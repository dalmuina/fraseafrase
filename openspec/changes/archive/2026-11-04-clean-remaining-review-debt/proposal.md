# Proposal

## Why

`docs/deuda-tecnica.md` acumula tres pendientes de revisión sin impacto en el comportamiento observable
(punto 8 de `docs/pendientes-mvp.md`): un comentario obsoleto, una rama defensiva innecesaria y la falta
de una regla automática que proteja el ADR-0010. Resolverlos ahora, antes de la revisión manual y las
mejoras posteriores, evita que se acumulen más cambios sin pasar por ellos primero.

## What Changes

- Corrige el comentario de `CatalogLoadError.MalformedDocument` (`CatalogLoadResult.kt`): decía "no aparece
  en la spec", pero el escenario ya está en `openspec/specs/phrase-catalog/spec.md` (línea 135-141).
- Elimina la rama `current == null` de `PracticingContent` (`PracticeSessionScreen.kt`): `ObtainPracticeSession`
  deja de poder producir `PracticeSessionResult.Ready` con `items` vacío — ese caso se mapea a
  `PracticeSessionResult.NothingToPractice`, igual que `ActiveBatchResult.NothingAvailable`. `PracticeItem` pasa de
  nulable a no nulable en `PracticeSessionUiState.Practicing.current`.
- Añade Konsist (`core/testing`, convention plugin) con dos reglas de arquitectura que automatizan el ADR-0010:
  una única declaración de `preferencesDataStore` por nombre de fichero, y los data sources con estado
  (campo mutable propio o caché en memoria) deben tener ámbito `@Singleton`.

## Capabilities

Ninguna: los tres puntos son correcciones de documentación interna y endurecimiento de un invariante de tipos
y de arquitectura ya vigente (ADR-0010); no cambian ningún comportamiento observable del sistema ni añaden
comportamiento nuevo. `PracticeSessionResult.Ready` con `items` vacío no es alcanzable hoy en producción
(`ObtainActiveBatch` solo produce `Ready` a partir de una tanda existente o recién compuesta, ninguna vacía);
la pantalla mostraba el mismo mensaje (`practice_nothing_to_practice`) en ambos casos. Este cambio declara
`skip_specs: true`.

### New Capabilities

Ninguna.

### Modified Capabilities

Ninguna.

## Impact

- `core/domain/.../catalog/CatalogLoadResult.kt`: comentario.
- `core/domain/.../practice/ObtainPracticeSession.kt`, `PracticeSessionResult.kt`: `items` vacío se mapea a
  `NothingToPractice`.
- `feature/session/.../PracticeSessionUiState.kt`, `PracticeSessionViewModel.kt`, `PracticeSessionScreen.kt`:
  `current` pasa a no nulable; se elimina la rama y sus comprobaciones `current != null`.
- `gradle/libs.versions.toml`, `build-logic/`: añade la librería Konsist y el test de arquitectura (módulo a
  decidir en `design.md`).
- Fuera de alcance de este cambio (quedan en `docs/pendientes-mvp.md`, punto 8): migrar a detekt 2.0 (sigue en
  alfa — `2.0.0-alpha.4`, sin fecha de estable) y proteger `develop`/`master` en GitHub (configuración del
  repositorio, no verificable con un test automático del propio repositorio).
