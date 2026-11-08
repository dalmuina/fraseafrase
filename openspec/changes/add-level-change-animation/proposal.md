# Proposal

## Why

Cada autoevaluación resuelve el nivel de consolidación de la frase al instante (ADR-0015, ADR-0017): un
acierto que certifica la sube de insignia y un fallo en nivel ≥ 1 la baja, ambos en el mismo toque. Hoy la
pantalla de práctica no muestra ningún reflejo de ese cambio en el momento en que ocurre: solo el contador
"quedan N aciertos para la insignia X" (`practice-session`, "Aciertos para el próximo nivel visibles"), que
no distingue el instante en que ese cambio sucede. El usuario pierde el refuerzo visual del momento de mayor
carga del método de estudio.

## What Changes

- Al autoevaluar una frase, si su nivel de consolidación sube o baja como resultado de esa autoevaluación, la
  pantalla de práctica MUST mostrar una animación de insignia que comunique ese cambio antes de continuar con
  la frase siguiente de la tanda.
- La subida de nivel MUST mostrarse con una animación de énfasis (insignia con halo pulsante) y la bajada con
  una animación más sobria (sin halo, de menor duración), ambas con la etiqueta de la insignia resultante.
- Una autoevaluación que no cambia el nivel de consolidación (acierto que no certifica, o fallo en nivel 0)
  MUST NOT mostrar ninguna animación de insignia.
- `AnswerPhrase` (`:core:domain`) pasa a devolver el nivel de consolidación resultante de la frase respondida,
  en vez de `Unit`, para que la presentación pueda comparar el nivel anterior (ya visible en pantalla) con el
  resultante. No cambia ninguna regla de racha, certificación, bajada de nivel ni enfriamiento: solo expone un
  dato que `PracticeBatch.answer()` ya calculaba.
- Nuevo componente del design system, junto a `DsLevelBadge`, que anima la transición entre dos insignias de
  nivel (0 a 5).

## Capabilities

### New Capabilities

(ninguna)

### Modified Capabilities

- `practice-session`: nuevo requisito de animación de cambio de insignia al autoevaluar, y aclaración de que
  "Autoevaluación tras revelar" no se ve afectada en su resultado (sigue registrando la respuesta una sola vez
  y mostrando la frase siguiente); la animación es una capa adicional sobre ese mismo flujo, no lo sustituye.

## Impact

- **`:core:domain`**: `AnswerPhrase.invoke` cambia su tipo de retorno de `Unit` a `Level`. Sin cambios en
  `PracticeBatch`, `PhraseProgress` ni las reglas de ADR-0015/ADR-0017.
- **`:feature:session`**: `PracticeSessionViewModel` compara el nivel anterior (ya conocido antes de
  responder) con el nuevo nivel devuelto por `AnswerPhrase`, y expone un evento de un solo uso
  (`badgeChange`) en `PracticeSessionUiState.Practicing`; nueva acción para consumirlo. `PracticeSessionScreen`
  reproduce la animación y la consume.
- **`:core:designsystem`**: nuevo componente de animación de insignia junto a `DsLevelBadge`, construido con
  las utilidades de animación de Compose y los tokens ya existentes en `Motion` (sin añadir dependencias).
- **ADR afectados**: ninguno requiere un ADR nuevo. El cambio respeta el alcance del MVP de ADR-0012 y las
  reglas vigentes de ADR-0015/ADR-0017 (bajada instantánea de nivel y resolución al instante): no introduce
  ninguna regla de método nueva, solo hace visible en la UI una transición que el dominio ya resuelve.

## Fuera de alcance

- Cualquier animación o sonido en el cierre de tanda, la generación de sesión o la pantalla de Progreso.
- Haptics o sonido: solo animación visual.
- Cambiar los umbrales de certificación, enfriamiento o bajada de nivel.
- Añadir una librería de animación (confetti, Lottie u otra): el componente se construye con Compose nativo.
