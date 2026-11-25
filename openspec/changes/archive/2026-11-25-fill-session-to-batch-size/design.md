# Design

## Context

Motivación en `proposal.md`; reglas en ADR-0026 (Propuesto) y en los deltas de `specs/`.

- `ComposeNextBatch` (`:core:domain`, `spacedrepetition`) es una función pura que hoy compone pendientes por
  prioridad y `newCountOf(...)` nuevas, repartidas con `interleave`. Una frase con `dueAtSession` posterior a la
  sesión actual nunca entra. La usan `ObtainActiveBatch`, `GenerateSession` y `RegenerateActiveBatchForLevel`,
  así que el cambio llega a todos los caminos de composición sin tocarlos.
- `PhraseProgress.record(...)` resuelve cada autoevaluación al instante con la `currentSession` que recibe de
  `PracticeBatch.answer(...)`. El progreso de la entrada en ese momento es el último guardado: tras un acierto
  o un fallo en la misma sesión, `dueAtSession` ya es posterior a la sesión actual.
- Persistencia (Room), puertos y UI no cambian. No hay migración.

## Goals / Non-Goals

**Goals:**
- Tanda de `min(N, frases del nivel CEFR elegido)` frases en cualquier composición.
- Un acierto antes de vencer no acorta los intervalos.

**Non-Goals:**
- Distinguir en la UI las frases de repaso adelantado o cambiar el resumen de la tanda.
- Cambiar la fórmula de nuevas dentro del límite, la prioridad de las pendientes o la fórmula de posición.

## Decisions

### D1. Composición en cuatro tramos dentro de `ComposeNextBatch`

Se mantiene la clase y su firma. Tras calcular `pending` y `newCount` como hoy:

1. `taken = pending.take(N − newCount)`.
2. `early` = progreso del nivel CEFR elegido con `dueAtSession > currentSession`, ordenado por
   `level + (dueAtSession − currentSession)` y desempatado por índice de catálogo. Es la misma expresión de
   prioridad de las pendientes (`level − (currentSession − dueAtSession)`), así que se extrae a una función
   privada única y sirve para ambos tramos.
3. `earlyTaken = early.take(N − taken − newCount)`.
4. `extraNew = newCandidates.drop(newCount).take(N − taken − newCount − earlyTaken)`.

Las nuevas son `newCandidates.take(newCount + extraNew.size)` (en orden de catálogo) y las no nuevas son
`taken + earlyTaken`. `interleave` no cambia: recibe las no nuevas como `pending`. Las entradas de repaso
adelantado pasan por `restartedInBatch()` igual que las pendientes y conservan nivel, racha y `dueAtSession`.

La ausencia de tanda sigue saliendo de `entries.isEmpty()`, que ahora solo ocurre si el nivel no tiene frases.

*Alternativa descartada:* un caso de uso o clase nueva para el relleno. Separaría una regla que comparte datos y
orden con la composición, y duplicaría la preparación de las entradas.

### D2. El repaso adelantado se detecta al responder, no al componer

En `PhraseProgress.recordHit`, si `dueAtSession > currentSession`, se devuelve `copy(answeredInBatch = true)` sin
tocar nivel, racha ni `dueAtSession`. `recordMiss` no cambia.

*Alternativas descartadas:*
- Marcar la entrada como repaso adelantado al componer: exige una columna en `PhraseProgressEntity` o en la
  tanda activa y una migración de Room, contra el alcance "sin cambio de esquema".
- Comparar con el `dueAtSession` del momento de componer: no se guarda.

Consecuencia aceptada (ADR-0026 y escenario "Un segundo acierto en la misma sesión no suma"): un segundo acierto
sobre la misma frase en la misma sesión tampoco suma, porque el primero ya fijó un `dueAtSession` futuro.

La reversión no cambia: `revertLastAnswer` restaura el progreso previo, que en un acierto ignorado es el mismo.

### D3. El resumen de la tanda y las insignias no cambian

El desglose de la tanda completada cuenta autoevaluaciones con acierto, así que un acierto en repaso adelantado
cuenta como acierto y nunca como subida de nivel. El contador "faltan X aciertos" no baja, porque la racha no
cambia. No hace falta código en `:feature:session`.

### D4. Ajuste de los tests que usaban repasos futuros como "nada que practicar"

Varios tests de `:core:domain` y `:feature:session` construyen el estado vacío con frases de repaso futuro. Con
ADR-0026 esas frases forman tanda. Se cambian a un catálogo sin frases del nivel CEFR elegido (por ejemplo, el
mismo id con nivel CEFR distinto del elegido, o un catálogo vacío). Ese cambio es neutro: pasa con el código
actual y con el nuevo, así que va en un grupo previo para que el hook de commit siga en verde en cada grupo.

### Módulos y tests

| Pieza | Módulo | Cambio | Test |
|---|---|---|---|
| `PhraseProgress` | `:core:domain` | acierto antes de vencer | `PhraseProgressTest`, `PracticeBatchTest` (unitarios) |
| `ComposeNextBatch` | `:core:domain` | cuatro tramos | `ComposeNextBatchTest` (unitario) |
| Casos de uso | `:core:domain` | ninguno | `ObtainActiveBatchTest`, `ObtainPracticeSessionTest`, `CloseThenObtainTest` con los fakes de `:core:testing` |
| ViewModel de sesión | `:feature:session` | ninguno | `PracticeSessionViewModelTest`, `PracticeSessionCloseTest` |

Las dependencias entre módulos no cambian (ADR-0010). No hay librerías, puertos, `Clock` ni dispatchers nuevos:
la sesión actual sigue llegando como argumento.

## Risks / Trade-offs

- [Con pocas pendientes vuelven frases de nivel alto antes de tiempo] → El orden por `nivel + espera restante`
  pone primero las de nivel bajo y vencimiento cercano, y el acierto no suma.
- [Un usuario puede extrañarse de que un acierto no acerque la insignia] → Fuera de alcance; si molesta en pruebas,
  se anota en `docs/deuda-tecnica.md` para un cambio de UI.
- [Tests de otros módulos (`:core:data`, `:app`) con supuestos del tamaño de la primera tanda] → El grupo que
  cambia la composición ejecuta `check` de los módulos afectados y corrige solo datos de test, sin cambiar lo que
  verifican.
- [El fallo de composición concurrente del primer arranque puede seguir dando tandas raras] → Siguiente cambio;
  aquí no se toca `ObtainActiveBatch`.

## Migration Plan

Sin migración de datos. Al instalar la versión nueva, la siguiente composición ya usa las reglas de ADR-0026; la
tanda activa guardada no se recompone. Al archivar: ADR-0026 pasa a Aceptado y la línea de estado de ADR-0025 a
`Sustituido por ADR-0026` (el alcance parcial consta en el "Sustituye a" de ADR-0026).
