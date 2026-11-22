# Tasks

## 1. Relectura de Progreso al volver a mostrarse (`:feature:progress`)

- [x] 1.1 En `ProgressFixtures`, añadir una sobrecarga de `progressViewModel` que reciba `FakeSettingsRepository`,
  `FakeCatalogRepository` y `FakePracticeRepository` ya creados (design, "Estrategia de test"). La función actual
  delega en ella. Verificación: los tests existentes de `ProgressViewModelTest` siguen en verde.
- [x] 1.2 Test rojo en `ProgressViewModelTest`: `pt-PT` con 6 frases y `pt-BR` con 4. Tras la carga inicial, el
  test cambia el idioma activo a `pt-BR` y envía `ProgressAction.Resume`. Espera `totalAvailable == 4`,
  `byLevel[0] == 4` y `activeTargetLocale == pt-BR` (el icono se deriva de `activeTargetLocale`; la ruta no tiene test propio). Escenario: `specs/progress-overview/spec.md`, "Volver a
  Progreso tras cambiar el idioma destino activo". Debe fallar porque `Resume` no existe o no relee.
- [x] 1.3 Test rojo: B1 vigente, con 6 frases B1 y 4 frases C1. Tras la carga inicial, el test guarda C1 y envía
  `Resume`. Espera `totalAvailable == 4` y `cefrLevel == C1`. Escenario: "Volver a Progreso tras aplicar otro
  nivel CEFR".
- [x] 1.4 Test con Turbine: tras el primer `Content`, `Resume` sin cambios no emite ningún estado nuevo
  (`expectNoEvents`), tampoco `Loading`. Escenario: "Volver a Progreso sin cambios no pasa por el estado de
  carga".
- [x] 1.5 Implementar:
  - `data object Resume` en `ProgressAction`, actualizando su KDoc.
  - En `ProgressViewModel`, `onAction(Resume)` relanza `reload()` y cancela el `Job` anterior (design D2 y D3): se guarda el `Job` devuelto por
    `launch` en un campo `private var reloadJob: Job?`. La cancelación es por robustez; ningún escenario la exige.
    Se elimina el `@Suppress`.
  - Verificación: los tests 1.2 a 1.4 pasan en verde.
- [x] 1.6 En `ProgressRoute`, añadir `LaunchedEffect(Unit) { viewModel.onAction(ProgressAction.Resume) }` y
  actualizar su KDoc (design D1). Verificación: `./gradlew :feature:progress:check -q --console=plain` en verde.

## 2. Verificación final (sesión principal)

- [ ] 2.1 `./gradlew check` y `./gradlew koverVerify` en verde (tests, ktlint/detekt, lint y cobertura).
- [ ] 2.2 Comprobación manual en la app:
  - Cambiar el idioma destino activo en Ajustes y volver a Progreso: cambian el icono y los recuentos.
  - Aplicar otro nivel CEFR y volver a Progreso: cambian el nivel y el total.

## Workflow follow-up

- `code-reviewer` (una ronda, cambio ligero), `/opsx:archive` y PR mediante el hook.
