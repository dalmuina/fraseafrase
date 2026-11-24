# Tasks

Escenarios: `specs/<capacidad>/spec.md` de este cambio. Decisiones: `design.md` (D1–D4). ADR-0025 ya está
redactado en `docs/adr/0025-rotacion-de-tanda-por-prioridad-y-aprendizaje.md` (Propuesto).

## 1. Espera intra-nivel tras un acierto que no certifica (`:core:domain`)

> Nota: si el hook de commit (`./gradlew check` global) detecta tests de otros módulos que fijan
> `dueAtSession = sesión + 1` tras un acierto sin certificar en nivel ≥ 3, se adaptan en este mismo grupo,
> sin tocar código de producción fuera de `:core:domain`.

- [ ] 1.1 Test: `ReviewScheduleTest` - `given levels 0 to 5 when computing the within level wait then it is 1 1 1
  2 4 and 8` (`spaced-repetition`: "Espera intra-nivel de cada nivel"). Debe fallar por compilación o aserción
  (no existe `withinLevelWaitFor`).
- [ ] 1.2 Implementación: `ReviewSchedule.withinLevelWaitFor(level)` = `max(1, enfriamiento / 2)`, con 1 para el
  nivel 0, más KDoc con ADR-0025 (design D1). Verificación: 1.1 en verde.
- [ ] 1.3 Test: `PhraseProgressTest` - `given a level 3 phrase with streak 1 when a hit is recorded at session
  10 then dueAtSession is 12` y `given a level 5 phrase with streak 0 when a hit is recorded at session 10 then
  dueAtSession is 18` (`spaced-repetition`: "Acierto que no certifica en nivel 3 espera dos sesiones" y "… en
  nivel 5 espera ocho sesiones"). Se mantiene el test de nivel 1 → 11 ("Acierto que no certifica fija la sesión
  siguiente"). Debe fallar por `dueAtSession` 11 en los dos nuevos.
- [ ] 1.4 Implementación: `PhraseProgress.recordHit` usa `ReviewSchedule.withinLevelWaitFor(level)` en la rama
  que no certifica; se actualiza el KDoc de `record` (ADR-0025). Verificación: 1.3 en verde y
  `./gradlew :core:domain:check`.

## 2. Composición de la tanda por prioridad y aprendizaje (`:core:domain`, más ajustes de tests en `:feature:session` y `:core:data`)

> Nota: el cambio de composición altera el tamaño y el orden de las tandas que fijan los tests de casos de uso
> y de otros módulos, y el hook ejecuta `./gradlew check` global al commitear. Por eso 2.5 adapta en este mismo
> grupo los tests de `:feature:session` y `:core:data`, sin cambiar su código de producción.

- [ ] 2.1 Test: `ComposeNextBatchTest`, candidatas y prioridad. Se sustituyen los tests de cuotas y dominadas.
  Un test por escenario de `practice-batch`:
  - "Candidatas a la tanda nueva": los seis escenarios.
  - "Prioridad de las pendientes": "Menor nivel va antes a igual retraso", "El retraso adelanta a una frase de
    nivel alto", "Empate de prioridad se resuelve por catálogo" y "Las pendientes que no caben esperan".

  Se conservan los tests de invariantes y de ausencia de tanda. Los escenarios que ya pasan con el código
  actual (frase vencida, frase de sesión futura de nivel menor que 4, CEFR) se dejan como regresión. Los que
  deben fallar son los de orden por prioridad y "Una frase de nivel alto sin vencer no entra".
- [ ] 2.2 Test: `ComposeNextBatchTest`, número y reparto de nuevas. Un test por escenario de `practice-batch`:
  - "Número de frases nuevas según el aprendizaje": los siete escenarios.
  - "Reparto de las nuevas en la tanda": los tres escenarios.
  - "Composición de la tanda o ausencia de tanda": "Primera tanda con catálogo disponible" (10 de 15) y "Tanda
    menor que N si faltan frases".
  - "Frases nuevas por nivel CEFR": "Solo nuevas del nivel elegido".

  Deben fallar por el número de nuevas y por su posición.
- [ ] 2.3 Implementación: reescribir `ComposeNextBatch` según design D2. Incluye: candidatas, prioridad
  `nivel − retraso`, `techo`/`hueco` con aritmética entera, reparto de las nuevas, y eliminar `Quotas`, `Take`
  y las constantes de cuotas y dominadas. KDoc de `BatchComposition` sin "dominadas". Verificación: 2.1 y 2.2
  en verde.
- [ ] 2.4 Tests de casos de uso de `:core:domain` que fijan el tamaño o el orden de la tanda compuesta:
  `GenerateSessionTest`, `ObtainActiveBatchTest`, `CloseThenObtainTest`, `ObtainPracticeSessionTest` y
  `RegenerateActiveBatchForLevelTest`. Se ajustan al resultado nuevo con el comentario de escenario o
  ADR-0025. En concreto:
  - `RegenerateActiveBatchForLevelTest` reescribe el escenario de `practice-batch` "La tanda siguiente se
    compone por cuotas con el nuevo nivel" (resultado nuevo: 8 nuevas y 12 pendientes) y ajusta "El progreso
    de un nivel CEFR abandonado…" (vuelve como pendiente).
  - `ObtainActiveBatchTest` cubre `practice-persistence` "Primer uso crea la primera tanda" con las 10
    primeras frases (hoy afirma 15, `ObtainActiveBatchTest.kt:82-83`).
  - `GenerateSessionTest` cubre "Generar sesión incrementa el contador y compone en un paso". Verificación: `./gradlew :core:domain:check`.
- [ ] 2.5 Tests de otros módulos:
  - `PracticeSessionCloseTest` o `PracticeSessionViewModelTest`: cubrir el escenario de `practice-session`
    "Generar sesión muestra la tanda siguiente con sus arrastradas primero" (nuevo resultado: A, N1–N4, C) y ajustar los que
    fijan el orden o el tamaño anterior.
  - Ajustar `PracticeSessionRevertTest` y `RoomPracticeCycleTest` si fallan.
  - Revisar con Grep los instrumentados `PracticeSessionRouteTest` y `PracticeSessionCompletedScreenTest`, y
    ajustarlos si fijan el orden. Ejecutarlos filtrados por clase si hay dispositivo; si no lo hay, indicarlo
    en la tarea.

  Verificación: `./gradlew :feature:session:check :core:data:check`.

## 3. Cierre y verificación (sesión principal)

- [ ] 3.1 Líneas de estado (ADR-0001):
  - ADR-0025 → `Aceptado`.
  - ADR-0017 → `Sustituido por ADR-0018 (alcance del MVP), ADR-0021 (filtro CEFR), ADR-0024 (regla de fallo) y
    ADR-0025 (acierto sin certificar y composición)`.
  - Añadir 0025 a la tabla de `docs/adr/README.md` y actualizar la fila de 0017.
- [ ] 3.2 `./gradlew check` y `./gradlew koverVerify` en verde (tests, detekt/ktlint, lint y cobertura: dominio
  90 %, presentación 80 %, global 70 %).
- [ ] 3.3 Comprobación manual con `assembleDebug`, N = 10 y 4 o 5 sesiones generadas:
  - las frases rotan;
  - las nuevas aparecen repartidas;
  - una frase Oro acertada sin certificar no reaparece en la sesión siguiente.

  Si no hay dispositivo, indicarlo en la tarea.

## Workflow follow-up

- `code-reviewer` (hasta dos rondas), `/opsx:verify`, `/opsx:archive` y commit de archive (abre la PR por hook).
