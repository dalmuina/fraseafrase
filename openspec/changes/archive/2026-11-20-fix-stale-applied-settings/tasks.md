# Tasks

## 1. `ObtainActiveBatch` registra los ajustes aplicados aunque no recomponga

- [x] 1.1 Actualizar el test `given no applied settings registered when obtaining the batch then it returns the existing batch as is` (`core/domain/src/test/.../practice/ObtainActiveBatchTest.kt`) para que, además de comprobar que se devuelve la tanda existente sin recomponer, compruebe que `settingsRepository.appliedSettings(targetLocale)` queda con los ajustes guardados vigentes tras la obtención. Renombrarlo a algo como `given no applied settings registered when obtaining the batch then it returns the existing batch as is and records the saved settings`. Ejecutar `:core:domain:test` y confirmar que falla por `appliedSettings` seguir siendo `null` (satisface el escenario de spec "Sin ajustes aplicados registrados todavía se registran sin recomponer").
- [x] 1.2 Añadir un test nuevo que obtenga la tanda activa dos veces: la primera sin ajustes aplicados registrados (lo que debe registrarlos) y, tras cambiar los ajustes guardados a un nivel CEFR distinto, la segunda debe devolver `ActiveBatchResult.Created` con frases del nuevo nivel. Ejecutar `:core:domain:test` y confirmar que falla porque la segunda obtención sigue devolviendo `Existing` (satisface el escenario de spec "Tras registrarse, un cambio posterior de ajustes ya recompone").
- [x] 1.3 En `ObtainActiveBatch.kt`, cuando `existing != null` y `appliedSettings(targetLocale)` es `null`, llamar a `settingsRepository.recordAppliedSettings(targetLocale, settings)` antes de devolver `ActiveBatchResult.Existing(existing)`, sin tocar el resto de la rama (sigue sin recomponer). Ejecutar `:core:domain:test` y confirmar que los dos tests de 1.1 y 1.2 pasan, junto con el resto de la suite de `ObtainActiveBatchTest`.

## 2. Verificación

- [x] 2.1 Ejecutar `./gradlew :core:domain:check` y confirmar que pasa (tests, ktlint, detekt) sin tocar ningún otro módulo.
- [x] 2.2 Ejecutar `./gradlew :core:domain:koverVerify` (o `koverVerify` global si no existe la tarea por módulo) y confirmar que la cobertura de dominio sigue en 90 % o más.

## Workflow follow-up

- Revisión opcional de `code-reviewer` (cambio ligero: un único método, sin lógica de dominio nueva más allá de reutilizar un puerto ya existente).
- `/opsx:verify` para comprobar la implementación contra los escenarios de `specs/practice-persistence/spec.md` de este cambio.
- `/opsx:archive` en la rama del cambio, antes de la pull request.
