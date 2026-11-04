# Design

## Context

Ver `proposal.md` - Why. Los tres puntos vienen de `docs/deuda-tecnica.md` (sección "Transversal" y "Del review
de `add-practice-session`") y no cambian comportamiento observable: `skip_specs: true`.

## Goals / Non-Goals

**Goals:**
- Corregir el comentario obsoleto de `CatalogLoadError.MalformedDocument`.
- Eliminar la rama `current == null` de la pantalla de práctica, estrechando el tipo de `Practicing.current`
  a no nulable.
- Automatizar con Konsist las dos reglas de ADR-0010 ya documentadas como deuda (una `preferencesDataStore`
  por fichero; data sources con estado en `@Singleton`).

**Non-Goals:**
- Migrar a detekt 2.0 (sigue en alfa, `2.0.0-alpha.4` sin fecha de estable) y proteger `develop`/`master` en
  GitHub: quedan en `docs/pendientes-mvp.md`, punto 8, pendientes de otro cambio.
- Ninguna regla de Konsist más allá de las dos ya identificadas en la deuda técnica; no se auditan de nuevo
  todas las reglas del ADR-0010 (eso ya lo hace `android-architect` a mano en cada propuesta).
- No se toca `ActiveBatchResult.NothingAvailable` ni el resto de `PracticeSessionResult`; solo el caso
  `Ready` con `items` vacío.

## Decisions

### 1. `Ready(items vacío)` se mapea a `NothingToPractice` en `ObtainPracticeSession`, no en el ViewModel

`ObtainPracticeSession.withText` (`:core:domain`) pasa a comprobar `loaded.phrases` ya mapeado: si
`items(batch, loaded.phrases)` resulta vacío, devuelve `PracticeSessionResult.NothingToPractice` en vez de
`Ready(items = emptyList(), ...)`. Alternativa descartada: dejar `Ready` tal cual y que
`PracticeSessionViewModel.toScreenState()` decida cuándo `items.isEmpty()` mapear a `NothingToPractice`. Se
descarta porque movería una decisión de dominio (qué cuenta como "nada que practicar") a presentación;
`PracticeSessionResult` ya es el contrato que distingue ambos casos y `ActiveBatchResult.NothingAvailable` se
resuelve en el mismo punto.

Con esto, `PracticeSessionUiState.Practicing.current` pasa de `PracticeItem?` a `PracticeItem`, y
`PracticeSessionViewModel.toScreenState()` usa `items.first()` en vez de `items.firstOrNull()` (ya no hay rama
vacía que alcance ese punto). `PracticingContent` elimina el `if (current == null)` y pinta siempre
`PhraseCard`; las comprobaciones `current != null &&` en `answer()` y `playAudio()` se simplifican a la
condición de `isAnswerable()` sola.

### 2. Las reglas de Konsist viven en `:app/src/test`, sin módulo nuevo

Konsist resuelve los ficheros Kotlin del proyecto a partir de la raíz de Gradle (`Konsist.scopeFromProject()`),
no necesita que el módulo que ejecuta el test dependa en tiempo de compilación de los módulos que inspecciona.
Por ADR-0010, `:app` es el único módulo que ya conoce la existencia de todos los demás (navegación y grafo de
DI), así que es el lugar natural para un test que vigila la arquitectura completa: evita crear un módulo nuevo
solo para dos reglas, y mantiene la regla "las features nunca se conocen entre sí" sin tocar.

Alternativa descartada: `:core:testing`. Se descarta porque ese módulo expone fakes y builders que consumen
las features y `:core:data`, no herramientas de verificación de arquitectura; mezclar ambas cosas confundiría
su propósito.

`Konsist` se añade como `testImplementation` solo de `:app` (no es una librería de producción ni se expone a
otros módulos), versión `0.17.3` (última estable), declarada en `gradle/libs.versions.toml`.

### 3. Alcance de las dos reglas de Konsist

- **Una única declaración de `preferencesDataStore` por nombre de fichero.** Se busca con Konsist las
  propiedades delegadas por `preferencesDataStore(name = "...")` en todo el proyecto y se falla si dos
  declaraciones usan el mismo `name`. Hoy solo existe una (ajustes), así que el test pasa en verde y queda
  como guarda de regresión.
- **Los data sources con estado tienen ámbito `@Singleton`.** "Con estado" se define, como en la deuda técnica,
  por tener un campo mutable propio o una caché en memoria (constructor properties no cuentan). Se audita a
  mano el conjunto de clases `*DataSource` (`:core:data`) y se decide cuáles están con estado (hoy,
  `AssetsCatalogDataSource`); la regla de Konsist comprueba que esas clases, y solo esas, declaran `@Singleton`.
  No se intenta inferir "con estado" automáticamente desde el AST: Konsist puede listar propiedades `var` o
  `val` mutables de un tipo conocido (p. ej. `MutableStateFlow`, caché), pero clasificarlo de forma genérica
  excede el alcance de esta tarea y no lo pide la deuda técnica.

## Risks / Trade-offs

- [Konsist 0.17.3 es una librería joven (breaking changes entre minors, ver 0.17.0)] → fijar la versión en el
  catálogo y no actualizar sin revisar el changelog; el test falla en rojo si una API cambia, no en silencio.
- [Una regla de Konsist demasiado genérica podría dar falsos positivos con código futuro legítimo] → las dos
  reglas se limitan exactamente a lo que pide `docs/deuda-tecnica.md`; cualquier regla nueva se decide en su
  propio cambio.

## Migration Plan

No aplica: sin datos ni usuarios afectados, cambio de código y una librería de test nueva.
