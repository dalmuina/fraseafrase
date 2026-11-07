# Design

## Context

Ver `proposal.md` - "Why" para la motivación. Puntos de partida en el código actual (post
`replace-batch-closure-with-session-generation`):

- `PhraseProgress` (`core/domain/.../spacedrepetition/PhraseProgress.kt`): `nextReview: LocalDate?`, racha
  reiniciada a 0 por `ComposeNextBatch.restartedInBatch()` en cada composición; certificación resuelta por
  `ResolveBatchClosure` al "cerrar" (hoy, al generar sesión).
- `ComposeNextBatch` (`core/domain/.../spacedrepetition/ComposeNextBatch.kt`): toma `CarryQueue`, `progress`,
  catálogo, CEFR y N; cuotas 40/40/20; CEFR solo filtra nuevas.
- `CarryQueue` (`core/domain/.../spacedrepetition/CarryQueue.kt`): cola FIFO de ids arrastrados, con
  `afterClosure(leftover, outcomes)`.
- `GenerateSession`/`RegenerateActiveBatchForLevel`/`ForceCloseActiveBatch`
  (`core/domain/.../practice/*.kt`): comparten `advanceActiveBatch(snapshot, batch, resolveBatchClosure)`
  (mueve entradas a la cola, con o sin `ResolveBatchClosure`) y `composeNextBatchInto(...)`.
- `PracticeSnapshot` (`core/domain/.../practice/PracticeSnapshot.kt`): `progress`, `activeBatch`, `carry`.
- `SettingsViewModel.save()`: `batchSizeChanged` → `ForceCloseActiveBatch`; `cefrLevelChanged` (sin N) →
  `RegenerateActiveBatchForLevel`.

## Goals / Non-Goals

**Goals:**
- Que una frase pueda certificar y subir de nivel a lo largo de varias sesiones generadas, no solo dentro de
  una.
- Que el repaso espaciado se mida en sesiones generadas, no en días naturales.
- Que las frases a reforzar y dominadas respeten el nivel CEFR elegido (nunca por debajo).
- Simplificar el modelo retirando la cola de arrastradas y la resolución de cierre, que ya no hacen falta.

**Non-Goals:**
- No cambia la certificación por racha ≥ 3 ni la bajada instantánea de nivel ante un fallo (ADR-0015):
  siguen igual, solo cambia la base temporal de lo que pasa después.
- No cambia el modelo de "generar sesión" en un paso, el mensaje de tanda completada, ni el indicador "N
  aciertos para `<insignia>`" de `feature:session`.
- No migra datos de usuarios reales (proyecto pre-1.0.0, sin usuarios en producción).

## Decisions

### D0. `answeredInBatch` no cambia

`PhraseProgress.answeredInBatch` sigue existiendo y sigue siendo por-tanda (se resetea a `false` al entrar en
una tanda recién compuesta): es lo que usa "Tanda completa" (`practice-batch`, no tocada por este cambio) para
saber si ya se dio una vuelta completa. Lo único que deja de resetearse por tanda es la racha (D2); ambos
campos son independientes entre sí.

### D1. `dueAtSession: Int` no nulo, desligado del nivel

Hoy `nextReview` es nulo si y solo si el nivel es 0. Eso impide marcar "disponible pronto" a una frase de
nivel 0 recién fallada, que es justo lo que se pide (D2 del proposal). Se sustituye por `dueAtSession: Int`
no nulo en toda `PhraseProgress` con progreso: una frase nunca vista no tiene entrada en `progress` (sigue
siendo candidata a "nuevas"), y cualquier frase con entrada, incluida una de nivel 0, tiene su propio
`dueAtSession`. Se retira la invariante "nivel 0 ⇔ sin próximo repaso". Alternativa descartada: mantener
`dueAtSession` nulo para nivel 0 y añadir un campo aparte "disponible desde" solo para ese caso — se descarta
por duplicar el mismo concepto en dos campos.

### D2. Resolución al instante de la autoevaluación, no al generar sesión

Hoy `ResolveBatchClosure` resuelve certificación/arrastre al "cerrar" (generar sesión), porque antes una
frase podía fallar y acertar varias veces dentro de la misma tanda antes de cerrarla. Con "tanda completa"
definida como una sola vuelta (`replace-batch-closure-with-session-generation`), una frase recibe como mucho
una autoevaluación por tanda generada: resolver en la autoevaluación misma (`PhraseProgress.record()`) es
equivalente y más simple. Se retiran `ResolveBatchClosure` y `BatchClosureOutcome`: no hay nada que resolver
en lote al generar sesión. `record(assessment, currentSession)`:
- Acierto que lleva la racha a 3: certifica (`level = min(level+1,5)`, `streak = 0`,
  `dueAtSession = currentSession + enfriamiento(nuevoNivel)`).
- Acierto que no certifica: nivel sin cambios, `dueAtSession = currentSession + 1`.
- Fallo: nivel baja (ADR-0015, sin cambios), `dueAtSession = currentSession + 1`, en cualquier nivel.

Alternativa descartada: mantener la resolución por lote al generar sesión, recorriendo las entradas de la
tanda que se cierra. Se descarta porque ya no aporta nada (cada entrada solo tuvo una autoevaluación) y
obligaría a seguir pasando la tanda completa a una función de resolución en vez de resolver donde ya se tiene
el dato (`AnswerPhrase`).

### D3. Se retira `CarryQueue`; "a reforzar" se calcula sobre `progress`

Sin resolución por lote, no hace falta mover entradas a una cola al cerrar: el propio `dueAtSession` de cada
frase, ya actualizado por D2, basta para saber si debe reaparecer. `ComposeNextBatch` pasa a recibir
`progress`, catálogo, CEFR, N y la sesión actual (sin `CarryQueue`):
- `reinforceCandidates = progress.filter { it.dueAtSession <= currentSession && nivelCatalogo(it.id) >= cefrLevel }`
- `masteredCandidates = progress.filter { it.level.value >= 4 && it.dueAtSession > currentSession && nivelCatalogo(it.id) >= cefrLevel }`
- `newCandidates = catalog.filter { it.level == cefrLevel && it.id !in progressIds }` (sin cambios)

Mutuamente excluyentes por construcción: una dominada con `dueAtSession` vencido cae en `reinforceCandidates`,
no en `masteredCandidates`. Alternativa descartada: mantener `CarryQueue` solo para las arrastradas "puras"
(nunca certificadas) y usar `dueAtSession` solo para las que sí certificaron una vez — se descarta por
mantener dos mecanismos para la misma idea ("cuándo debe reaparecer esta frase").

### D4. `GenerateSession` y `RegenerateActiveBatchForLevel` se simplifican; `ForceCloseActiveBatch` se retira

Sin resolución por lote, la única diferencia real entre "generar sesión" (desde la práctica) y "recomponer por
ajustes" es si avanza el contador de sesiones generadas: generar sesión sí (fue una ronda real de práctica);
cambiar ajustes no (no se practicó nada). `GenerateSession`: `sessionCount += 1`, vacía la tanda activa,
compone con el CEFR y N persistidos y la nueva sesión. `RegenerateActiveBatchForLevel` (invocado por
`SettingsViewModel` para cualquier cambio de CEFR y/o N, ver D5): vacía la tanda activa, compone con el CEFR y
N recién guardados, sin tocar `sessionCount`. `ForceCloseActiveBatch` se retira: ya no hay un "cierre con
resolución" que lo distinga de recomponer. `advanceActiveBatch`/`composeNextBatchInto` (funciones internas
compartidas de `replace-batch-closure-with-session-generation`) se simplifican a una sola función "vaciar y
recomponer", sin el parámetro `resolveBatchClosure` (ya no existe esa clase).

### D5. Ajustes: CEFR y N se comportan igual

Consecuencia directa de D2/D4: ya no hay una "resolución de cierre" de la que cambiar N debiera formar parte y
cambiar CEFR no. `SettingsViewModel.save()` deja de distinguir `batchSizeChanged` de `cefrLevelChanged`: si
algo cambió, llama a `RegenerateActiveBatchForLevel` (generalizado para recibir también N, o renombrado si se
prefiere un nombre más neutro, p. ej. `RegenerateActiveBatch`) con los valores recién guardados. Alternativa
descartada: mantener la distinción actual por inercia con la propuesta anterior — se descarta porque ya no
refleja ninguna diferencia real de comportamiento, y mantenerla sería una complejidad sin motivo.

### D6. Enfriamiento en sesiones: 1, 2, 4, 8, 16

Progresión que dobla en cada nivel, análoga en espíritu a los días de ADR-0015 (1, 3, 7, 14, 30) pero mucho
más corta, porque el usuario puede generar varias sesiones en un mismo día. Valor de partida para revisar en
la implementación si la prueba manual muestra que dominadas tarda demasiado o muy poco en volver. Alternativa
descartada: reutilizar los mismos números que en días (1, 3, 7, 14, 30 sesiones) — se descarta porque, para un
uso de varias sesiones diarias, haría que una frase de nivel 5 tardase semanas en volver a repasarse ligero.

### D7. Migración de Room: destructiva

El proyecto está en desarrollo, versión 1.0.0 todavía no publicada, sin usuarios reales. Las tablas afectadas
(cola de arrastradas, columnas ligadas a `nextReview`) se pueden recrear sin migración de datos: la primera
apertura tras la actualización simplemente empieza con el progreso vacío, igual que una instalación nueva.
Alternativa descartada: escribir una migración que traduzca `nextReview` (fecha) a `dueAtSession` (sesión) —
se descarta porque no hay una correspondencia con sentido entre una fecha pasada y un número de sesiones
generadas que nunca se contaron, y el coste de escribirla no se justifica sin usuarios a los que afecte.

## Risks / Trade-offs

- [Riesgo] Retirar `ForceCloseActiveBatch` y unificar el comportamiento de ajustes (D5) cambia lo que
  `settings-screen` especifica desde la propuesta anterior, apenas archivada. → Mitigación: el cambio está
  motivado por una consecuencia directa de D2 (ya no existe nada que "cerrar con resolución"), no por un
  cambio de opinión sobre el producto; se documenta explícitamente en `proposal.md` y en el delta de
  `settings-screen`.
- [Riesgo] Sin `CarryQueue`, una frase que nunca llega a responderse en una tanda (caso raro, ya que "tanda
  completa" exige responder todas) no tiene ningún mecanismo de arrastre explícito. → Mitigación: su
  `dueAtSession` ya persistido no cambia hasta que se le responda; sigue siendo candidata a "a reforzar" en
  cuanto venza, sin necesidad de una cola.
- [Riesgo] Los números de enfriamiento (D6) son una propuesta de partida, no validada con datos reales de
  uso. → Mitigación: viven en una sola función (`ReviewSchedule`), fáciles de ajustar en un cambio posterior
  sin tocar el resto del algoritmo.

## Migration Plan

Migración destructiva de Room (D7): se elimina la tabla/columna de la cola de arrastradas y se añade la
columna del contador de sesiones generadas (por defecto 0) y la columna `due_at_session` (sustituye a la de
`next_review`). Al actualizar desde una versión anterior, el progreso existente se pierde (vuelve a cero),
aceptable dado que no hay usuarios en producción. Sin plan de "rollback" de datos: si hiciera falta revertir
la app a una versión anterior, también se perdería el progreso acumulado con el nuevo esquema.

## Open Questions

- Nombre final de `RegenerateActiveBatchForLevel` tras generalizarse a N y CEFR (¿se renombra a
  `RegenerateActiveBatch`, o se mantiene el nombre actual con una firma más amplia?): no cambia las specs ni
  el comportamiento, se resuelve en `tasks.md`.
- Si el enfriamiento (D6) debería tener un tope máximo distinto de 16, o si conviene hacerlo configurable: no
  cambia el enfoque de este cambio, se puede ajustar sin un ADR nuevo si la prueba manual lo pide.
