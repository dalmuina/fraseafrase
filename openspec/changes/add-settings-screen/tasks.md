# Tasks

## 1. Dependencia de Navigation 3

- [x] 1.1 Comprobar la última versión estable (no `alpha`/`beta`) de
  `androidx.navigation3:navigation3-runtime` y `navigation3-ui`; si solo hay prerelease, anotar la
  versión fijada en `design.md` (Risks / Trade-offs) como riesgo aceptado. Añadirlas a
  `gradle/libs.versions.toml` y como dependencia de `:app` (design.md - Decisiones, navegación básica
  sin *back stack* guardable); verificar con `./gradlew :app:assembleDebug`.

## 2. Dominio: escritura de ajustes y casos de uso

- [x] 2.1 Test: `SettingsRepository` (puerto) expone `save(settings: UserSettings)`; escribir un test en
  `FakeSettingsRepository`-based scenario que falle al compilar porque el método no existe, luego
  añadir la firma a la interfaz en `:core:domain` (spec `user-settings`, "Guardar sustituye los
  ajustes anteriores").
- [x] 2.2 Test y caso de uso `ObtainUserSettings` (`:core:domain`, `given no settings saved when
  invoked then returns the repository's settings`); hacerlo fallar antes de implementar la clase con
  `operator fun invoke`.
- [x] 2.3 Test y caso de uso `SaveUserSettings` (`:core:domain`, `given a new cefr level and batch size
  when invoked then the repository receives them`); hacerlo fallar antes de implementar.
- [x] 2.4 Actualizar `FakeSettingsRepository` (`:core:testing`) para que `save()` reemplace el valor
  devuelto por `settings()`; verificar que los tests de 2.2 y 2.3 pasan con el fake real usado en los
  tests de dominio.

## 3. Datos: persistencia de la escritura

- [x] 3.1 Test en `DataStoreSettingsDataSourceTest` (`:core:data`): `given cefr level and batch size
  saved when settings are read then they match` (spec `user-settings`, "Guardar sustituye los ajustes
  anteriores"); debe fallar porque `save()` no existe todavía.
- [x] 3.2 Implementar `DataStoreSettingsDataSource.save()` con un único `dataStore.edit { ... }` que
  escriba `cefr_level` y `batch_size`; verificar que el test de 3.1 pasa.
- [x] 3.3 Test: `given a settings store that throws IOException on write when save is called then the
  exception propagates and a subsequent read still returns the previous settings` (spec
  `user-settings`, "Fallo de escritura no mezcla ajustes antiguos y nuevos"); implementar o ajustar
  solo si el test falla con el código de 3.2.

## 4. Presentación: pantalla de ajustes (`:feature:settings`)

- [x] 4.1 Test de `SettingsViewModel` (`:core:testing`'s `MainDispatcherExtension`): `given settings
  stored when the screen opens then the ui state shows them` (spec `settings-screen`, "Se muestran los
  ajustes guardados"); implementar `SettingsUiState` (`Loading` / `Loaded`), `SettingsAction` y el
  ViewModel con `ObtainUserSettings` hasta que pase.
- [x] 4.2 Test: `given a cefr level selected action when dispatched then the ui state shows the new
  level` (spec `settings-screen`, "Elegir un nivel CEFR de la lista"); implementar el manejo de
  `SelectCefrLevel`.
- [x] 4.3 Tests: `given a batch size text out of range or non numeric when changed then the ui state is
  invalid and saving is disabled` y `given a batch size text in range when changed then saving is
  enabled` (spec `settings-screen`, "N fuera de rango impide guardar", "N no numérico impide guardar",
  "N dentro de rango permite guardar"); implementar la validación de `ChangeBatchSizeText`.
- [x] 4.4 Test: `given a valid cefr level and batch size when Save is dispatched then SaveUserSettings
  is invoked with them and a later read reflects them` (spec `settings-screen`, "Guardar persiste el
  nuevo nivel CEFR y N"); implementar el manejo de `Save`.
- [x] 4.5 Compose UI tests de `SettingsScreen` (sin ViewModel, con `@PreviewLightDark` y
  `ui-test-junit4`): muestra el nivel y la N recibidos, deshabilita guardar con un valor inválido, y
  expone una acción de volver que invoca `onBack` sin pasar por el ViewModel (spec `settings-screen`,
  "Volver tras guardar" y "Volver sin guardar"); implementar `SettingsScreen` siguiendo el patrón de
  `PracticeSessionScreen` (Material3 directo, tokens de `AppTheme`, sin literales).
- [x] 4.6 Implementar `SettingsRoute` (conecta `SettingsViewModel` con `SettingsScreen`, recibe
  `onBack: () -> Unit`) y los strings de `feature/settings/src/main/res/values/strings.xml`; verificar
  con `./gradlew :feature:settings:test`.

## 5. Punto de entrada desde la práctica

- [x] 5.1 Test de `PracticeSessionScreen`: con un `onOpenSettings` dado, pulsar la acción de ajustes del
  `TopAppBar` lo invoca (spec `settings-screen`, "Abrir ajustes desde la práctica"); hacerlo fallar
  antes de añadir el `TopAppBar` con un `TextButton` de texto "Ajustes" (sin `material-icons-core`,
  sin componente `Ds*`; design.md - Decisiones, "Acceso a ajustes: texto, no icono") al `Scaffold`
  existente.
- [x] 5.2 Añadir el parámetro `onOpenSettings: () -> Unit` a `PracticeSessionRoute` y reenviarlo a
  `PracticeSessionScreen`; verificar con `./gradlew :feature:session:test`.

## 6. Integración en `:app`: navegación

- [ ] 6.1 Definir las claves de ruta `Practice` y `Settings` (`data object`) en un paquete de
  navegación de `:app` (design.md - Decisiones, Navigation 3 básico).
- [ ] 6.2 Sustituir el `PracticeSessionRoute` directo de `FraseAFraseApp` por un `NavDisplay` con
  `backStack` en memoria (`mutableStateListOf`) que muestre `Practice` al abrir, navegue a `Settings`
  al pulsar el icono de ajustes y vuelva a `Practice` con `onBack` o el botón de volver de la pantalla
  de ajustes.
- [ ] 6.3 Test instrumentado en `:app` (`androidTest`, nombre `givenAppStart_whenLaunched_thenShowsPracticeThenSettings`):
  arranca mostrando la práctica (spec `practice-session`, "Pantalla de práctica como inicio"), navega a
  ajustes con el icono (spec `settings-screen`, "Abrir ajustes desde la práctica") y vuelve a la
  práctica (spec `settings-screen`, "Volver sin guardar"); verificar con
  `./gradlew :app:connectedDebugAndroidTest` o el emulador disponible.

## 7. Verificación final

- [ ] 7.1 Ejecutar `./gradlew check` (tests, ktlint, detekt) en verde.
- [ ] 7.2 Ejecutar `./gradlew koverVerify` y confirmar los mínimos de cobertura (dominio 90 %,
  presentación 80 %, global 70 %) en los módulos tocados.

## Workflow follow-up

- Ejecutar `/opsx:verify` para comprobar la implementación contra `proposal.md`, las specs y
  `tasks.md`.
- Ejecutar `/opsx:archive` en la rama del cambio, antes de la pull request.
