# Tasks

## 1. Dominio: ajustes aplicados y recomposición persistente de la tanda activa

- [x] 1.1 Test: `ObtainActiveBatch` sobre `FakeSettingsRepository`/`FakePracticeRepository` - devuelve
  la tanda existente sin recomponer si los ajustes aplicados coinciden con los guardados o no hay
  ninguno registrado; recompone sin tocar progreso ni el contador de sesiones si difieren; el caso
  recompuesto se sigue cumpliendo con una nueva instancia de `ObtainActiveBatch` sobre el mismo estado
  persistido (simula el reinicio de la app) - cubre los cuatro escenarios de "Recomposición de la
  tanda activa al detectar ajustes aplicados distintos" (`practice-persistence`). Falla porque
  `SettingsRepository` no expone ajustes aplicados.
- [x] 1.2 Añadir `appliedSettings(targetLocale): UserSettings?`/`recordAppliedSettings(targetLocale, settings)`
  a `SettingsRepository` (`:core:domain`) y a `FakeSettingsRepository` (`:core:testing`); reescribir
  `ObtainActiveBatch` para leer la tanda existente, comparar sus ajustes aplicados contra los
  guardados vigentes y delegar en `RegenerateActiveBatchForLevel` cuando procede (design D1/D2); ya no
  inyecta `ComposeNextBatch`/`ObtainCatalogPhrasesForActiveLanguage` directamente. Verificar
  `./gradlew :core:domain:test :core:testing:test`.
- [x] 1.3 Test: tras una composición por primer uso, por `GenerateSession` o por recomposición, los
  ajustes aplicados quedan registrados con los ajustes guardados vigentes usados - cubre "Registro de
  los ajustes aplicados al componer" (`practice-persistence`). Falla porque ninguna composición
  registra nada todavía.
- [x] 1.4 Registrar los ajustes aplicados en `ObtainActiveBatch` (tras `ActiveBatchResult.Created`) y
  en `GenerateSession` (tras componer con éxito). Verificar `./gradlew :core:domain:test`.

## 2. Datos: ajustes aplicados en DataStore

- [x] 2.1 Test: `DataStoreSettingsDataSource.appliedSettings(targetLocale)`/
  `recordAppliedSettings(targetLocale, settings)` - sin registro devuelve `null`, persiste y
  recupera, no mezcla idiomas destino, y sobrevive a una nueva instancia sobre el mismo `DataStore`
  (reinicio) - cubre "Ajustes aplicados a la tanda activa" (`user-settings`). Falla porque la
  implementación no tiene esos métodos.
- [x] 2.2 Implementar las claves `applied_cefr_level_<targetLocale>`/`applied_batch_size_<targetLocale>`
  en `DataStoreSettingsDataSource`, con el mismo patrón de lectura tolerante a valores ausentes o no
  válidos que las claves existentes (sin lanzar error). Verificar `./gradlew :core:data:test`.

## 3. Presentación: `SettingsViewModel` autoguarda sin acciones de salida

- [x] 3.1 Test: elegir un idioma destino lo activa de inmediato, sin ninguna acción adicional; elegir
  un nivel CEFR o escribir una N válida persiste de inmediato para el idioma destino mostrado; una N
  inválida no se autoguarda - cubre "Elegir un idioma destino lo activa de inmediato" y "Guardado de
  los ajustes editados" (`settings-screen`). Reescribe los tests de `SettingsViewModelTest` que hoy
  dependen de `SettingsAction.Save`. Falla porque `selectLanguage()` no activa el idioma de inmediato
  y no hay autoguardado de CEFR/N.
- [x] 3.2 En `SettingsViewModel`: `selectLanguage()` llama a `selectActiveLanguage(targetLocale)` de
  inmediato; `selectCefrLevel()`/`changeBatchSizeText()` llaman a `saveUserSettings` cuando el
  resultado es válido. Quitar `SettingsAction.Save`/`ConsumeSaveConfirmation`,
  `RegenerateActiveBatchForLevel` del constructor y los campos `saveConfirmationPending`/
  `navigateToPracticePending` de `SettingsUiState.Loaded` (design D3); actualizar las fixtures de
  `SettingsFixtures`/`SettingsViewModelTest`/`SettingsRouteTest` que construyen `SettingsViewModel`.
  Verificar `./gradlew :feature:settings:test`.
- [x] 3.3 Quitar `onShowSaveConfirmation`/`onNavigateToPractice` de `SettingsRoute` y sus
  `LaunchedEffect`; actualizar `SettingsRouteTest` en consecuencia (ya no hay ninguna señal que
  observar tras una edición, más allá del propio `uiState`). Verificar
  `./gradlew :feature:settings:check`.

## 4. Presentación: `SettingsScreen` sin botón "Guardar" y selector de idioma con estilo de tarjeta

- [x] 4.1 Test: `SettingsScreen` no muestra ningún botón "Guardar" ni su icono de check. Falla porque
  el botón existe.
- [x] 4.2 Quitar el botón "Guardar" de `SettingsScreen`; quitar los tests de `SettingsScreenTest` que
  dependían de él (habilitado/deshabilitado según `isBatchSizeValid`, icono de check, forma no
  totalmente redondeada): ninguno de los tres tiene ya requisito de spec que cubrir. Verificar
  `./gradlew :feature:settings:check`.
- [x] 4.3 Test instrumentado: el selector de idioma destino tiene el mismo fondo y forma (radio,
  color) que las tarjetas de nivel CEFR y tamaño de tanda - cubre "Estilo de tarjeta del selector de
  idioma destino" (`settings-screen`). Falla porque `DsLanguageOptions` está suelto sobre el fondo de
  la pantalla.
- [x] 4.4 Envolver `DsLanguageOptions` en un `Card` (`MaterialTheme.shapes.large`,
  `CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)`,
  `Modifier.padding(AppTheme.spacing.l)`), igual que `CefrLevelCard`/`BatchSizeCard` (design D4).
  Verificar `./gradlew :feature:settings:check`.

## 5. Integración en `:app`: la barra inferior vuelve a ser una asignación directa

- [x] 5.1 Test instrumentado: desde Ajustes, tras elegir un nivel CEFR distinto, pulsar la pestaña
  "Práctica" muestra la práctica con la tanda recompuesta, sin pasar por ningún botón "Guardar" ni
  mostrar confirmación alguna. Adapta los tests de `FraseAFraseAppTest` que hoy buscan el texto
  "Guardar" al entrar o salir de Ajustes. Falla mientras `FraseAFraseApp` siga esperando las señales
  de `SettingsRoute` que se retiraron en el grupo 3.
- [x] 5.2 En `FraseAFraseApp`: volver a asignar `selectedDestination` directamente en el `onClick` de
  los tres destinos de la barra inferior, sin ningún caso especial para Ajustes; quitar
  `onShowSaveConfirmation`/`onNavigateToPractice` de la llamada a `SettingsRoute`; quitar el
  `SnackbarHost`/`SnackbarHostState` de confirmación de guardado, sin otro uso en la app. Verificar
  `./gradlew :app:check`.

## 6. Verificación final

- [x] 6.1 Ejecutar `./gradlew check` completo (tests, ktlint, detekt) y corregir cualquier fallo.
- [x] 6.2 Ejecutar `./gradlew koverVerify` y ajustar cobertura si algún módulo queda por debajo de su
  mínimo (dominio 90 %, presentación 80 %, global 70 %).

## Workflow follow-up

- Revisar con `code-reviewer` antes de `/opsx:verify`.
- Ejecutar `/opsx:verify` contra las specs de este cambio.
- Archivar con `/opsx:archive` en la rama del cambio, antes de la PR.
