# Proposal

## Why

`docs/deuda-tecnica.md` acumula seis limpiezas aceptadas en revisiones de código ya cerradas
(`add-practice-session`, revisión de tests de repetición espaciada, revisión de fakes y builders)
pero aplazadas por no tener impacto funcional. Son pequeñas, no relacionadas entre peers de código
pero sí homogéneas en naturaleza (legibilidad y mantenibilidad interna), y conviene resolverlas
juntas antes de abrir el siguiente cambio de producto (corrección por voz) para no acarrearlas
indefinidamente.

## What Changes

- Sustituir las seis comprobaciones repetidas `_uiState.value as? Practicing ?: return` del
  `PracticeSessionViewModel` por dos helpers (`currentPracticing()` y `updatePracticing { ... }`).
- Eliminar la duplicación de `progressWith` entre `RecordSelfAssessmentTest` y
  `ResolveBatchClosureTest` sustituyéndolo por `aPhraseProgress`, ya existente en `:core:testing`.
- Mover `inMemoryPracticeDatabase()` de `core/data/src/main` a `core/data/src/test`, dado que solo
  la usan tests JVM del propio módulo (no hace falta `:core:testing` ni `testFixtures`).
- Traducir al inglés los mensajes de `require` en `Level.kt` y `PhraseProgress.kt`, según la
  convención de CLAUDE.md.
- Cambiar los builders de `:core:testing` que aceptan `Int` donde el dominio usa value classes
  (p. ej. `PhraseProgressBuilders`) para que acepten el value class correspondiente.
- Parametrizar los casos repetidos de `ReviewScheduleTest` con `@ParameterizedTest`.

Ninguno de estos puntos altera comportamiento observable: son refactors internos, reorganización
de tests y cambios de mensajes de error no expuestos a la UI.

## Capabilities

### New Capabilities

(ninguna)

### Modified Capabilities

(ninguna — no cambia el comportamiento descrito en ninguna spec; cambio marcado `skip_specs: true`)

## Impact

- **Código:** `feature/session` (ViewModel), `core/domain` (`Level`, `PhraseProgress`),
  `core/testing` (builders, nuevo fixture `progressWith`, `inMemoryPracticeDatabase()`),
  `core/data` (elimina la función movida), tests de `core/domain/spacedrepetition`.
- **Tests:** ninguno nuevo desde el punto de vista de escenario; se reescriben o mueven los
  existentes. Sigue aplicando TDD donde cambia comportamiento interno verificable (p. ej. el
  helper del ViewModel se cubre con los tests ya existentes del ViewModel).
- **ADR:** no afecta a ningún ADR aceptado; respeta el alcance del ADR-0012 (no añade ni cambia
  funcionalidad de producto).
- **Fuera de alcance:** las reglas de Konsist (ADR-0010), la migración a detekt 2.0 (ADR-0011) y
  la protección de ramas en GitHub — son decisiones de infraestructura/ADR independientes, no
  limpiezas de código, y quedan en `docs/deuda-tecnica.md` para su propio cambio.
