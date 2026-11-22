# Design

## Context

La regla de fallo vive en `:core:domain`, paquete `spacedrepetition`:

- `PhraseProgress.recordMiss(currentSession)` baja el nivel en 1 si era ≥ 1, pone la racha a
  `streak.afterMiss()` y fija `dueAtSession = currentSession + 1`.
- `Streak.afterMiss()` devuelve `Streak(0)`.

Todo lo demás consume el `PhraseProgress` resultante y no necesita cambios: `AnswerPhrase` devuelve
`AnswerOutcome.resultingLevel` (de ahí sale el indicador de insignia del ViewModel), `UndoableAnswer` guarda
el progreso anterior completo para revertir, `BatchOutcome` cuenta aciertos, fallos y subidas sin mirar la
racha, y el contador `max(0, 3 - racha)` se calcula a partir del progreso persistido. Room guarda nivel y
racha como enteros: el esquema no cambia y los valores ya guardados siguen siendo válidos.

## Goals / Non-Goals

**Goals:**
- Un fallo resta un solo acierto en la escala continua `nivel × 3 + racha` (ADR-0024).
- Mantener intacto todo lo que no sea la transición de fallo.

**Non-Goals:**
- Cambiar la certificación, el enfriamiento, la composición de tandas, la persistencia o la UI.

## Decisions

### D1. La regla vive en `PhraseProgress.recordMiss`; `Streak.afterMiss` pasa a restar 1 con suelo en 0

Bajar de nivel depende de la racha y del nivel a la vez, así que la decisión completa solo puede tomarla
`PhraseProgress`, que tiene los dos. `Streak` sigue siendo responsable de su propia aritmética:

```kotlin
// Streak (:core:domain, spacedrepetition)
/** Un fallo resta 1 a la racha, sin bajar de 0 (ADR-0024). */
fun afterMiss(): Streak = Streak(maxOf(value - 1, 0))

// PhraseProgress (:core:domain, spacedrepetition)
private fun recordMiss(currentSession: Int): PhraseProgress {
    val dropsLevel = streak.value == 0 && level.value >= 1
    return copy(
        level = if (dropsLevel) Level(level.value - 1) else level,
        streak = if (dropsLevel) Streak(CERTIFICATION_STREAK - 1) else streak.afterMiss(),
        answeredInBatch = true,
        dueAtSession = currentSession + 1,
    )
}
```

La racha tras bajar de nivel es `CERTIFICATION_STREAK - 1` (2), no un literal: si el umbral cambiara, la
frase seguiría quedando a un acierto de recuperar el nivel.

Alternativas descartadas:
- Representar el progreso como un único entero `nivel × 3 + racha` y derivar nivel y racha. Exige migrar
  la persistencia y tocar todos los consumidores para un cambio de una sola transición.
- Mover la regla a un caso de uso. `PhraseProgress.record` ya es el punto único de resolución (ADR-0017) y
  lo usan `AnswerPhrase` y los tests de dominio.

### D2. Racha heredada mayor que 2

Las rachas en datos guardados pueden valer 3 (lo admiten los escenarios "Racha de tres sigue mostrando el
contador en 0"). Con D1, un fallo con racha 3 la deja en 2 sin bajar de nivel, que es coherente con
"restar un acierto". No se necesita migración.

### D3. Tests

- `:core:domain`, unitarios JUnit 6 + Kotest, sin fakes nuevos:
  - `StreakTest`: el escenario de fallo pasa de "racha 2 → 0" a "racha 2 → 1", más "racha 1 → 0" y
    "racha 0 → 0".
  - `PhraseProgressTest`: un test por escenario de "Resolución de la autoevaluación" sobre fallos
    (racha 0 en niveles 3, 1 y 5; racha > 0 en nivel 3; nivel 0 con racha 0 y con racha 2) y "Fallo resta
    uno a la racha" / "Fallo reinicia la racha" (nivel 2 se mantiene). Los tests de fallo existentes se renombran y
    ajustan para fijar la racha de partida explícitamente.
  - `AnswerPhraseTest`, `RevertLastAnswerTest`, `PracticeBatchTest`: revisar los tests de fallo; los que
    parten de racha 0 siguen valiendo, los que parten de racha > 0 se ajustan al nuevo resultado. El de
    reversión cubre "Revertir un fallo restaura el nivel, la racha y el vencimiento anteriores" con racha 0.
- `:feature:session`, `PracticeSessionViewModelTest` (usa el dominio real con `FakePracticeRepository`):
  - Sustituir `given a level 2 phrase with streak 2 when answering with a miss then it shows 3 remaining hits
    for Plata` por los tres escenarios del contador tras un fallo (racha 2 → 2 para "Oro"; racha 1 → 3 para
    "Oro"; racha 0 → 1 para "Plata").
  - Añadir "Un fallo con racha mayor que 0 no muestra el indicador" (`badgeChange` nulo) y "Un fallo con
    racha mayor que 0 mantiene la insignia".
  - Los tests de fallo en nivel 3 y nivel 0 parten de racha 0 (valor por defecto de `aPhraseProgress`) y
    solo cambian su comentario de escenario/ADR.
- Sin tests instrumentados nuevos: la pantalla recibe `uiState` y no contiene la regla. Se adapta el existente
  de `PracticeSessionScreenTest` que cita "Fallo reinicia el contador" al resultado nuevo (racha 1 → 3 para "Oro").

## Risks / Trade-offs

- [Una frase puede "rebotar" entre dos niveles con fallos y aciertos alternos] → Es el comportamiento
  buscado: cada fallo cuesta exactamente un acierto. El desglose de tanda ya cuenta una subida aunque la
  frase vuelva a bajar.
- [Rachas guardadas mayores que 2] → D2: siguen siendo válidas y un fallo las reduce en 1.
- [Texto de ADR-0017 queda en parte obsoleto] → ADR-0024 lo sustituye parcialmente y su línea de estado se
  actualiza en la tarea de cierre, como en ADR-0019.
