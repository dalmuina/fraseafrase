# Design

## Context

Ver `proposal.md` - "Why" para la motivación. Puntos de partida en el código actual:

- `ComposeNextBatch` (`core/domain/.../spacedrepetition/ComposeNextBatch.kt`) llena la tanda hasta N por
  prioridad estricta (arrastradas → pendientes → nuevas), sin cuotas ni bloque de "dominadas".
- `CloseActiveBatch`/`ForceCloseActiveBatch` resuelven la tanda activa con `ResolveBatchClosure` (certifica,
  sube nivel, arrastra) y la dejan vacía; `ObtainActiveBatch` compone la siguiente de forma perezosa, en la
  próxima lectura. Son dos pasos separados.
- `SettingsViewModel.save()` llama a `forceCloseActiveBatch()` igual si cambia el CEFR o solo N: ambos casos
  certifican y resetean racha hoy.
- `PracticeSessionViewModel` expone `RequestClose`/`CancelClose`/`ConfirmClose` con un `closeWarning: Int?` en
  `PracticeSessionUiState.Practicing`, calculado leyendo la tanda antes de cerrar.
- `DsLevelBadge` (`core/designsystem/.../component/DsLevelBadge.kt`) mapea nivel 1-5 a icono, color y nombre
  ("Bronce".."Diamante"), todo en una función privada `levelBadgeSpec`.

## Goals / Non-Goals

**Goals:**
- Un único caso de uso de dominio para "generar sesión" que une resolución de cierre y composición.
- Composición por cuotas con un bloque nuevo de "dominadas", reutilizando el orden interno ya existente dentro
  de "a reforzar".
- Un camino de recomposición por cambio de CEFR que no pase por `ResolveBatchClosure`.
- Reutilizar el nombre de insignia existente en `DsLevelBadge` desde `feature/session`, sin duplicar el mapeo
  nivel → nombre.

**Non-Goals:**
- No se cambia `PhraseProgress.record()` ni `ResolveBatchClosure` (la lógica de racha/nivel al responder y al
  cerrar se mantiene intacta); solo cambia cuándo se invoca `ResolveBatchClosure`.
- No se cambia el comportamiento de `ForceCloseActiveBatch` para el caso de "solo cambia N": sigue
  certificando y reseteando racha como hoy.
- No se toca `:core:data` ni el esquema de Room: ni la tanda activa ni la cola de arrastradas cambian de forma
  de persistirse, solo cómo y cuándo se recomponen.

## Decisions

### D1. Cuotas calculadas sobre N, no proporciones de catálogo

Las cuotas (nuevas ≈40 %, a reforzar ≈40 %, dominadas ≈20 %) se calculan sobre el tamaño de tanda N, no sobre
el tamaño del catálogo ni del progreso total. `newQuota = floor(N * 0.4)`, `reinforceQuota = floor(N * 0.4)`,
`masteredQuota = N - newQuota - reinforceQuota` (el resto, para que la suma sea siempre N). Con N = 20:
8/8/4. Alternativa descartada: porcentajes exactos con redondeo "al más cercano" en los tres bloques a la vez,
que puede sumar más o menos que N y obliga a un ajuste posterior; `floor` en los dos primeros y "resto" en el
último evita esa inconsistencia con una sola regla.

### D2. Relleno con cuotas como objetivo, no como tope duro

Cada bloque se llena primero con sus propios candidatos hasta su cuota. Si un bloque no tiene suficientes
candidatos, el hueco pasa al siguiente bloque en el orden a reforzar → nuevas → dominadas, que puede superar su
propia cuota con sus propios candidatos sobrantes. Esto preserva la regla ya existente de `practice-batch`
("Frase en un único lugar", "arrastrada que no quepa conserva su progreso"): un candidato que no entra por falta
de hueco en ningún bloque simplemente no entra en esta tanda, igual que hoy con N. Alternativa descartada: topes
duros por bloque (nunca superar la cuota) — se descarta porque dejaría tandas más pequeñas que N incluso
habiendo material disponible, lo que contradice el requisito ya vigente "Composición de la tanda o ausencia de
tanda" (tanda `Composed` con hasta N frases cuando haya al menos una disponible).

### D3. Un nuevo caso de uso `GenerateSession` sustituye la combinación `CloseActiveBatch` + lectura perezosa

`GenerateSession` (`:core:domain`, paquete `practice`) hace, en una transacción de `PracticeRepository`: (1)
resuelve la tanda activa con `ResolveBatchClosure`, igual que hoy; (2) compone inmediatamente la siguiente con
la nueva composición por cuotas. No recibe `expectedUncertified`: al eliminarse el aviso previo, no hay
expectativa que comprobar, así que tampoco hay resultado `Stale`. `CloseActiveBatch` se retira (ya no tiene
llamador); `ForceCloseActiveBatch` se mantiene para el caso de "solo cambia N" en Ajustes. Alternativa
descartada: mantener `CloseActiveBatch` y llamar a `ObtainActiveBatch` justo después desde el ViewModel — se
descarta porque dejaría la resolución y la composición en dos transacciones distintas, con una ventana en la
que la tanda activa queda vacía.

### D4. Un nuevo caso de uso `RegenerateActiveBatchForLevel` para el cambio de CEFR

`RegenerateActiveBatchForLevel` (`:core:domain`, paquete `practice`) hace, en una transacción: si hay tanda
activa, mueve sus entradas tal cual (sin pasar por `ResolveBatchClosure`) a la cola de arrastradas, en su orden
de tanda, delante de la cola sobrante existente (reutilizando la función `CarryQueue.afterClosure`-equivalente,
pero a partir de las entradas sin resolver, no de `BatchClosureOutcome`); después compone la siguiente con las
cuotas y el nuevo CEFR. El nivel y el `nextReview` de cada frase no se tocan: solo cambia su membership en la
tanda activa. La racha de una frase que vuelva a entrar en la tanda recompuesta sí parte de 0, porque "racha
inicial 0 en cada tanda nueva" es una regla general de composición (`practice-batch`), no algo que este caso de
uso añada. Alternativa descartada: limpiar `activeBatch` sin pasar las entradas a la cola de arrastradas — se
descarta porque perdería la posición en cola de las frases que estaban a medio practicar, que no volverían a
aparecer hasta que su `nextReview` las hiciera pendientes (y las de nivel 0 nunca lo harían).

### D5. `SettingsViewModel` elige entre `RegenerateActiveBatchForLevel` y `ForceCloseActiveBatch` según qué campo cambió

En `save()`: si cambia N (con o sin cambio de CEFR a la vez), se llama a `ForceCloseActiveBatch` como hoy: es
un cierre deliberado desde ajustes, con resolución completa (certifica, resetea racha), y la composición que
sigue ya usa el nuevo CEFR y la nueva N por cuotas. Si cambia **solo** el nivel CEFR (N igual al persistido), se
llama a `RegenerateActiveBatchForLevel`, que no resuelve el cierre. Esto coincide con
`specs/settings-screen/spec.md` ("Cierre de la tanda activa al cambiar N", escenario "Cambiar N y el nivel CEFR
a la vez cierra la tanda activa"): cambiar N nunca evita el cierre, cambiar CEFR en solitario sí. Alternativa
descartada: que el cambio de CEFR "ganara" siempre, aunque N cambiara a la vez (sin cierre ni certificación en
ningún caso) — se descarta porque mezclaría, en un solo guardado, una resolución parcial (nada certificado) con
un cambio de tamaño de tanda que normalmente sí la lleva, más difícil de razonar para el usuario que la regla
simple "cambiar N siempre cierra" (ver ADR-0016).

### D6. Nombre de insignia público en `DsLevelBadge`

Se añade `fun levelBadgeLabel(level: Int): String?` pública en `DsLevelBadge.kt`, construida a partir de la
misma tabla que ya usa `levelBadgeSpec` (se extrae la tabla nivel→nombre a una función compartida para no
duplicarla). `feature/session` la llama con `Int` (el mismo tipo que ya usa para `DsLevelBadge`), sin que
`:core:designsystem` pase a depender de ningún módulo del proyecto (ADR-0010).

### D7. "Tanda completa" como estado derivado, expuesto en `PracticeSessionResult`

Se añade un campo o variante (a decidir en la implementación, p. ej. `PracticeSessionResult.Ready.isComplete:
Boolean`) calculado como "todas las entradas de la tanda tienen `answeredInBatch = true`". `ObtainPracticeSession`
ya lee la tanda activa completa, así que puede calcularlo sin una lectura adicional.
`PracticeSessionUiState.Practicing` sustituye `closeWarning: Int?` por `completed: Boolean`;
`PracticeSessionAction` sustituye `RequestClose`/`CancelClose`/`ConfirmClose` por `GenerateSession`.

## Risks / Trade-offs

- [Riesgo] Cambiar el reparto de arrastradas/pendientes de "todo lo que quepa en N" a "como máximo la cuota de
  a reforzar" puede dejar más frases en la cola de arrastradas de las que quedarían hoy, alargando cuánto tarda
  una frase muy atrasada en volver a aparecer. → Mitigación: la cola de arrastradas ya conserva la posición y
  el progreso de lo que no cupo (regla existente); con cuotas del ~40 % sobre N (10-30), el bloque de a reforzar
  nunca baja de 4 frases, un mínimo razonable por tanda.
- [Riesgo] `RegenerateActiveBatchForLevel` y `GenerateSession` comparten casi toda la lógica de "mover entradas
  a la cola y componer de nuevo" salvo si pasan por `ResolveBatchClosure`. → Mitigación: extraer esa parte común
  a una función interna compartida (análoga a la actual `closeBatch(...)` compartida por `CloseActiveBatch` y
  `ForceCloseActiveBatch`), parametrizada por si resuelve o no antes de encolar.
- [Riesgo] El ADR-0016 propuesto sustituye parcialmente a ADR-0015; si `android-architect` no lo aprueba en la
  revisión de esta propuesta, los deltas de especificación quedarían sin una decisión de arquitectura que los
  respalde. → Mitigación: `docs/adr/0016-generar-sesion-por-cuotas.md` se redacta y se revisa junto con esta
  propuesta, antes de `/opsx:apply` (no como tarea de implementación).

## Migration Plan

Sin datos que migrar: la tanda activa, la cola de arrastradas y el progreso por frase mantienen su forma de
persistencia (Room). El cambio es solo de comportamiento (qué casos de uso se invocan y qué ve la UI), así que
no hace falta migración de esquema ni de datos existentes. Una tanda activa guardada con la forma actual sigue
siendo válida: la primera vez que se lea tras actualizar la app, se sigue mostrando igual hasta la primera
generación de sesión o cambio de ajustes.

## Open Questions

- Forma exacta del nuevo estado "tanda completa" en `PracticeSessionUiState` (variante hermana de `Practicing`
  vs. flag `completed` dentro de `Practicing`): no cambia las specs ni el enfoque, se resuelve en `tasks.md`.
- Texto exacto de las cadenas de recursos nuevas (contador de aciertos, mensaje positivo de tanda completada):
  no cambia el comportamiento especificado, se resuelve durante la implementación.
