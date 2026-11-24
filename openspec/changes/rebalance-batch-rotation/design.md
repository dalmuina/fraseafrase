# Design

## Context

- `ComposeNextBatch` (`:core:domain`, `spacedrepetition/`) es una función pura. La llaman, a través de
  `composeNextBatchInto` (`practice/PracticeTransitions.kt`), tres casos de uso:
  - `GenerateSession`, que suma uno al contador de sesiones.
  - `RegenerateActiveBatchForLevel`, que no lo suma.
  - `ObtainActiveBatch`.

  Su firma (`progress`, `catalog`, `cefrLevel`, `size`, `currentSession`) basta para las reglas nuevas, así que
  los casos de uso no cambian.
- `PhraseProgress.recordHit` fija hoy `dueAtSession = sesión + 1` si el acierto no certifica.
  `ReviewSchedule.cooldownSessionsFor` tiene la tabla 1/2/4/8/16.
- Todo el estado necesario ya se guarda: nivel, racha, `dueAtSession` y el contador de sesiones. No hace falta
  ningún campo nuevo ni migración de Room.
- **Hallazgo durante el diseño (corrige el plan inicial):** se había planteado limitar las autoevaluaciones a
  una por frase y sesión. No hace falta:
  - `PracticeBatch.answer` mueve la frase respondida al final, y la pantalla muestra siempre la primera
    entrada, que no está respondida hasta que todas lo están.
  - Al completar la tanda, la pantalla deja de ofrecer frases (`PracticeSessionScreen`, `completed`).
  - Una frase solo vuelve a autoevaluarse en la misma sesión tras revertirla, y la reversión restaura su
    progreso anterior.
  - La regeneración por ajustes no reintroduce frases ya respondidas en la sesión: toda autoevaluación fija
    `dueAtSession ≥ sesión + 1` y, sin bloque de dominadas, una frase no pendiente no entra.

  Por eso el enfriamiento no se pisa dentro de una sesión, y esa regla queda fuera de alcance.

## Goals / Non-Goals

**Goals:**
- Que los niveles altos se vean menos, sin dejar de exigir 3 aciertos por nivel (decisión del usuario,
  2026-11-24).
- Que las frases roten: las pendientes se ordenan por prioridad, no por catálogo, y ninguna espera sin límite.
- Que entren frases nuevas según el aprendizaje en curso, no con un porcentaje fijo, y repartidas por la
  tanda.
- Que la primera tanda de un nivel CEFR se llene hasta la mitad de N (decisión del usuario, 2026-11-24).

**Non-Goals:**
- Contador de errores persistido, proporciones configurables o cambios de interfaz.
- Cambiar la certificación (3 aciertos), el enfriamiento tras certificar o la regla de fallo (ADR-0024).

## Decisions

### D1. Espera intra-nivel en `ReviewSchedule`

Se añade `ReviewSchedule.withinLevelWaitFor(level: Level): Int = max(1, (COOLDOWN_SESSIONS[level.value] ?: 0) / 2)`. El
nivel 0, que no tiene enfriamiento, da 1. Resultado: 1, 1, 1, 2, 4 y 8 para los niveles 0 a 5.

`PhraseProgress.recordHit` la usa en la rama que no certifica. El KDoc de `record` se actualiza con la
referencia a ADR-0025.

- *Alternativa descartada:* una tabla independiente. Derivarla del enfriamiento mantiene una sola fuente de
  verdad y escala con ella.

### D2. Composición sin bloques fijos (`ComposeNextBatch`)

Pasos:

1. **Candidatas.** Solo se consideran frases con nivel CEFR igual al elegido (ADR-0021):
   - **Pendientes:** `dueAtSession ≤ sesión actual`.
   - **Nuevas:** frases del catálogo sin progreso, en orden de catálogo.
   - **`enAprendizaje`:** frases con progreso de nivel 0 de ese nivel CEFR, estén o no pendientes. Las de
     nivel 0 respondidas en la sesión actual no están pendientes durante una regeneración por ajustes, pero
     cuentan.
2. **Prioridad de las pendientes:** `nivel − (sesión actual − dueAtSession)`, de menor a mayor. Desempata el
   índice de catálogo.
3. **Número de nuevas:**
   - `cap = N / 2` y `max40 = (N * 2) / 5`, con división entera.

     ⚠️ Se usa aritmética entera en lugar de `N * 0.4` en `Double`: `17 * 0.4` da `6.8000…01` y el redondeo a
     la baja sería frágil para otros valores.
   - `techo = if (pendientes == 0) cap else max40`
   - `hueco = max(0, cap − enAprendizaje)`
   - `nuevas = min(disponibles, techo, max(hueco, N − pendientes))`
   - `pendientesTomadas = min(pendientes, N − nuevas)`
4. **Reparto de las nuevas.** Si `p > 0`, la nueva `i` va en la posición `(i + 1) × (p + k) / (k + 1)`, con
   división entera. Las posiciones son distintas. Con `p ≥ 2` nunca es la última, así que una pendiente cierra la tanda; con `p == 1` la última puede ser nueva (p. ej. P, N, N, N), y es aceptable.
   Las pendientes ocupan el resto en su orden. Si `p == 0`, las nuevas van en orden de catálogo.
5. **Resultado.** Las pendientes se reinician en la tanda (`answeredInBatch = false`, igual que hoy). Las
   nuevas se crean con nivel 0, racha 0 y `dueAtSession` 0. Si no hay entradas, `NothingAvailable`.

Se eliminan `Quotas`, `Take`, `MASTERED_LEVEL`, `NEW_RATIO` y `REINFORCE_RATIO`. Las constantes nuevas
(`LEARNING_CAP_NUMERATOR`/`DENOMINATOR` o equivalentes) quedan en el `companion object` con KDoc.
`requireConsistentInputs` no cambia.

Por qué `max(hueco, N − pendientes)`:
- Si las pendientes no llenan la tanda, las nuevas ocupan el sitio libre, hasta el techo.
- Si la llenan, las nuevas solo entran mientras el grupo de nivel 0 tenga hueco.

Así el número de nuevas reacciona al aprendizaje y no es un porcentaje fijo.

- *Alternativa descartada:* ordenar las pendientes solo por retraso. Una frase recién fallada (retraso 0)
  quedaría la última cuando hay acumulación, y los fallos perderían prioridad.

### D3. Ubicación y dependencias

Todo vive en `:core:domain` (Kotlin puro). No hay puertos, data sources, componentes de interfaz ni
librerías nuevas. ADR-0010 se respeta sin cambios.

### D4. Tests

- **Unitarios en `:core:domain`:**
  - `ReviewScheduleTest`: espera de cada nivel.
  - `PhraseProgressTest`: escenarios de acierto sin certificar en los niveles 1, 3 y 5.
  - `ComposeNextBatchTest`: se reescriben los tests de cuotas y dominadas con los escenarios nuevos de
    `practice-batch`. Los de CEFR, invariantes y ausencia de tanda se mantienen o se ajustan a la nueva
    redacción.
- **Tests de casos de uso con el dominio real** (`GenerateSessionTest`, `ObtainActiveBatchTest`,
  `RegenerateActiveBatchForLevelTest`, `CloseThenObtainTest`, `ObtainPracticeSessionTest`): se ajustan los que
  fijan el tamaño o el orden de la tanda compuesta. `RegenerateActiveBatchForLevelTest` reescribe el escenario
  de cuotas ("La tanda siguiente se compone por cuotas con el nuevo nivel": 8 nuevas y 12 pendientes) y
  `ObtainActiveBatchTest` cubre `practice-persistence` "Primer uso crea la primera tanda" con 10 frases.
- **`:feature:session`** (`PracticeSessionViewModelTest`, `PracticeSessionCloseTest`,
  `PracticeSessionRevertTest`) y **`:core:data`** (`RoomPracticeCycleTest`): solo se ajustan si fallan por el
  tamaño o el orden de la tanda. El escenario de `practice-session` "Generar sesión muestra la tanda siguiente
  con sus arrastradas primero" (resultado nuevo) se cubre en `PracticeSessionCloseTest` o en el test del ViewModel que hoy cubre el
  orden tras generar sesión.
- **Instrumentados** (`PracticeSessionRouteTest`, `PracticeSessionCompletedScreenTest`): se revisan con Grep y
  se ajustan si fijan el orden. Se ejecutan filtrados por clase si hay dispositivo.

Fakes: los existentes de `:core:testing` (`FakePracticeRepository` y builders). No hacen falta nuevos.

## Risks / Trade-offs

- **Tandas menores que N cuando hay pocas pendientes** (por ejemplo, 3 pendientes y 4 nuevas con N = 10).
  Es aceptable (ADR-0012) y es lo que evita meter muchas nuevas de golpe. Se mitiga porque al bajar las
  pendientes también suben las nuevas, hasta el 40 %.
- **El grupo de nivel 0 puede superar la mitad de N** cuando hay pocas pendientes, porque `N − pendientes`
  supera al `hueco`. Se autorregula: en cuanto las pendientes llenan la tanda, las nuevas se paran hasta que
  el grupo baja.
- **Cambia el orden y el tamaño de las tandas,** así que varios tests de otros módulos que fijaban la tanda
  por cuotas tendrán que ajustarse. No hay cambio de esquema: el progreso guardado sigue siendo válido.
- **Starvation teórica:** con la prioridad `nivel − retraso`, una pendiente gana un puesto por cada sesión que
  espera. Ninguna espera sin límite mientras se generen sesiones.
