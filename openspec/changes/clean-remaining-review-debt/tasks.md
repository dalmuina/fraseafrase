# Tasks

## 1. Dominio: comentario obsoleto y `Ready` sin `items` vacío

- [x] 1.1 Corregir el comentario de `CatalogLoadError.MalformedDocument` en
      `core/domain/.../catalog/CatalogLoadResult.kt`: ya aparece en la spec (`phrase-catalog`, líneas 135-141);
      describir el error como distinto de `AssetUnreadable` sin la frase "No aparece en la spec". Verificar
      leyendo el fichero tras el cambio; no hay comportamiento que testear.
- [x] 1.2 Escribir un test en `ObtainPracticeSessionTest` (`core/domain`) que, con una tanda cuya composición de
      frase/catálogo produzca `items` vacío, espere `PracticeSessionResult.NothingToPractice` en vez de
      `Ready(items = emptyList(), ...)`. Ejecutarlo y comprobar que falla (hoy devuelve `Ready`).
- [x] 1.3 Implementar el cambio en `ObtainPracticeSession.withText`: si `items(batch, loaded.phrases)` es
      vacío, devolver `PracticeSessionResult.NothingToPractice`. Verificar que el test de 1.2 pasa en verde y
      que `ObtainPracticeSessionTest` completo sigue en verde.
      (Deuda técnica: "Estado `Practicing(current = null)` con lista vacía", sección "Del review de
      `add-practice-session`".)

## 2. Presentación: `current` no nulable y sin rama vacía

- [ ] 2.1 Actualizar `PracticeSessionUiState.Practicing.current` de `PracticeItem?` a `PracticeItem` y el
      KDoc que lo describe. En el mismo paso, cambiar `PracticeSessionViewModel.toScreenState()` de
      `items.firstOrNull()` a `items.first()` (ya no alcanzable vacío tras la tarea 1.3) — es el cambio mínimo
      para que `:feature:session` siga compilando con el tipo no nulable. Verificar con
      `./gradlew :feature:session:compileDebugKotlin` en verde.
- [ ] 2.2 Actualizar `PracticeSessionViewModelTest` y `PracticeSessionFixtures` (`feature/session`) para
      construir `Practicing` solo con `current` no nulo; eliminar cualquier fixture o caso que construyera
      `Practicing(current = null, ...)`. Ejecutar `:feature:session:test` y comprobar que compila y pasa en
      verde.
- [ ] 2.3 Simplificar `answer()` y `playAudio()`: la comprobación `current != null && state.isAnswerable()`
      pasa a `state.isAnswerable()`, usando `state.current` directamente. Verificar con
      `PracticeSessionViewModelTest` en verde.
- [ ] 2.4 Eliminar la rama `if (current == null)` de `PracticingContent` (`PracticeSessionScreen.kt`): pintar
      siempre `PhraseCard` con `state.current`. Actualizar o eliminar el test de UI instrumentado que cubría el
      mensaje vacío dentro de `Practicing` si existe, y confirmar que el caso `NothingToPractice` (estado de
      nivel superior) sigue cubierto. Verificar con `connectedDebugAndroidTest` de `:feature:session` o, si no
      es viable en este paso, con una revisión manual registrada en el propio `tasks.md`.

## 3. Arquitectura: reglas de Konsist (ADR-0010)

- [ ] 3.1 Añadir Konsist al catálogo de versiones (`gradle/libs.versions.toml`, versión `0.17.3`) como
      `testImplementation` de `:app` únicamente. Verificar con `./gradlew :app:dependencies --configuration testImplementation`
      que la librería aparece y con `./gradlew :app:compileTestKotlin` que compila.
- [ ] 3.2 Escribir `ArchitectureTest` (`app/src/test/.../architecture/ArchitectureTest.kt`) con la regla "una
      única declaración de `preferencesDataStore` por nombre de fichero". Comprobar en rojo duplicando
      temporalmente una declaración de `preferencesDataStore` con el mismo nombre en un fichero de prueba
      (o con un test que llame directamente a la función de comprobación con una lista de declaraciones
      duplicada) y revertir; en verde con el código real, que hoy solo tiene una declaración (ajustes).
- [ ] 3.3 Añadir a `ArchitectureTest` la regla "los data sources con estado tienen ámbito `@Singleton`":
      lista explícita de clases con estado (`AssetsCatalogDataSource`, por su caché en memoria) y comprobación
      de que cada una, y solo esas, declara `@Singleton`. Comprobar en rojo quitando temporalmente la
      anotación de `AssetsCatalogDataSource` y revertir; en verde con el código real.
      (Deuda técnica: dos entradas "Regla futura de Konsist (sin implementar)", sección "Transversal".)

## 4. Verificación final

- [ ] 4.1 Ejecutar `./gradlew check` (tests, ktlint, detekt) en verde.
- [ ] 4.2 Ejecutar `./gradlew koverVerify` en verde (cobertura dominio 90 %, presentación 80 %, global 70 %).
- [ ] 4.3 Actualizar `docs/deuda-tecnica.md`: marcar como resueltas las tres entradas de este cambio
      (comentario de `CatalogLoadResult`, `Practicing(current = null)`, las dos reglas de Konsist) con una
      referencia a `clean-remaining-review-debt`. Actualizar `docs/pendientes-mvp.md`: el punto 8 conserva solo
      detekt 2.0 y la protección de ramas en GitHub.

## Workflow follow-up

- Archivar el cambio (`/opsx:archive`) una vez aprobado por `android-architect` y `code-reviewer`, en esta
  misma rama y antes de la pull request.
- Verificar el archivo con `/opsx:verify` antes de archivar.
