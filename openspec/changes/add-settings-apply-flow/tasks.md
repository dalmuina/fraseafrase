# Tasks

## 1. Dominio: cierre forzado sin aviso obsoleto

- [x] 1.1 Test: `CloseActiveBatch` sigue cerrando igual que antes (sin tanda activa, no certificadas,
      certificadas, cola de arrastradas) tras extraer la lógica compartida — verificar que
      `CloseActiveBatchTest` existente sigue en verde sin cambiar sus aserciones.
- [x] 1.2 Extraer la resolución y el guardado de una tanda al cerrarla (uso de
      `ResolveBatchClosure`, `PracticeSnapshot.storing`, `activeBatch = null` y
      `CarryQueue.afterClosure`) a una función compartida del paquete `practice` en `:core:domain`, y
      hacer que `CloseActiveBatch` la use — verificar que `CloseActiveBatchTest` sigue en verde (design
      D3).
- [x] 1.3 Test (rojo): `ForceCloseActiveBatchTest` — sin tanda activa no cambia el estado; con tanda
      activa con frases certificadas y no certificadas, cierra igual que `CloseActiveBatch` (nivel,
      racha, cola de arrastradas) sin recibir ningún `expectedUncertified` — spec `settings-screen`,
      escenarios "Cambiar el nivel CEFR cierra la tanda activa sin aviso" y "Sin tanda activa no hay
      nada que cerrar".
- [x] 1.4 Implementar `ForceCloseActiveBatch` en `:core:domain` (paquete `practice`) usando la función
      compartida de 1.2 — verificar que 1.3 pasa a verde.

## 2. Presentación: Ajustes detecta cambios, confirma y pide cerrar/navegar

- [ ] 2.1 Test (rojo): `SettingsViewModelTest` — guardar con el nivel CEFR o la N iguales a los
      persistidos deja `saveConfirmationPending = true` y `navigateToPracticePending = false` tras
      guardar — spec `settings-screen`, escenario "Guardar sin cambiar ningún valor se queda en
      ajustes".
- [ ] 2.2 Añadir `saveConfirmationPending: Boolean` y `navigateToPracticePending: Boolean` a
      `SettingsUiState.Loaded` (ambos a `false` por defecto) y las acciones `ConsumeSaveConfirmation`
      y `ConsumeNavigateToPractice` a `SettingsAction`; hacer que `save()` guarde el `UserSettings`
      cargado en `reload()` como base, compare con el guardado y active las dos banderas según
      corresponda, y que las acciones de consumo las vuelvan a `false` — verificar que 2.1 pasa a
      verde.
- [ ] 2.3 Test (rojo): `SettingsViewModelTest` — guardar con el nivel CEFR distinto del persistido deja
      `saveConfirmationPending = true` y `navigateToPracticePending = true`, y llama a
      `ForceCloseActiveBatch` — spec `settings-screen`, escenarios "Cambiar el nivel CEFR cierra la
      tanda activa sin aviso" y "Guardar con el nivel CEFR cambiado navega a práctica".
- [ ] 2.4 Inyectar `ForceCloseActiveBatch` en `SettingsViewModel` y llamarlo desde `save()` cuando hubo
      cambios, antes de activar las banderas — verificar que 2.3 pasa a verde.
- [ ] 2.5 Test (rojo): `SettingsViewModelTest` — guardar con N distinta de la persistida (mismo nivel
      CEFR) también activa `navigateToPracticePending` y llama a `ForceCloseActiveBatch` — spec
      `settings-screen`, escenario "Guardar con N cambiado navega a práctica".
- [ ] 2.6 Ajustar la comparación de cambios de 2.2 para que compruebe también la N — verificar que 2.5
      pasa a verde.
- [ ] 2.7 Test (rojo): `SettingsViewModelTest` — con el mismo `SettingsViewModel` (sin recrearlo), un
      primer guardado con N=15 (cambio) seguido de un segundo guardado con N=20 también se detecta
      como cambio (`navigateToPracticePending = true`, llama a `ForceCloseActiveBatch` otra vez); y un
      segundo guardado que repite el mismo valor ya guardado (N=15 dos veces seguidas) NO se detecta
      como cambio la segunda vez — design D2.
- [ ] 2.8 Hacer que `save()` actualice la base en memoria de 2.2 al `UserSettings` recién guardado tras
      cada guardado con éxito, en vez de dejar fija la de `reload()` — verificar que 2.7 pasa a verde.

## 3. Presentación: Práctica vuelve a leer la tanda al entrar en la pestaña, sin perder el estado si no cambió

- [ ] 3.1 Test (rojo): `PracticeSessionViewModelTest` — con un estado `Practicing` ya cargado y
      revelado (`revealed = true`, `index` distinto de 1), la acción `Resume` sin que la tanda haya
      cambiado en el repositorio conserva `revealed`, `index` y el resto del estado tal cual — design
      D4.
- [ ] 3.2 Test (rojo): `PracticeSessionViewModelTest` — con un estado ya cargado, la acción `Resume`
      vuelve a leer la sesión (igual que `reload()` sin `previous`) y refleja una tanda distinta
      (frase, orden o tamaño distintos) cuando sí cambió en el repositorio — design D4.
- [ ] 3.3 Añadir `PracticeSessionAction.Resume` a `PracticeSessionAction` y manejarlo en
      `PracticeSessionViewModel.onAction`: lee la sesión y, solo si la tanda difiere de la ya cargada
      (frase actual, orden o tamaño), sustituye el estado con el resultado equivalente a `reload()`;
      si es la misma tanda, no modifica el estado actual — verificar que 3.1 y 3.2 pasan a verde.

## 4. UI: confirmación visual y navegación

- [ ] 4.1 Test (rojo, `SettingsScreenTest` en `androidTest`): tras guardar, se muestra un snackbar con
      el texto de confirmación — spec `settings-screen`, escenario "Confirmación tras guardar".
- [ ] 4.2 Envolver `SettingsScreen` en su propio `Scaffold` con `snackbarHost`; `SettingsRoute` recuerda
      un `SnackbarHostState`, lo muestra en un `LaunchedEffect` cuando `saveConfirmationPending` es
      `true` y envía `ConsumeSaveConfirmation` al terminar (design D6) — verificar que 4.1 pasa a
      verde.
- [ ] 4.3 Test (rojo, `androidTest`): `SettingsRoute` con nivel CEFR cambiado llama a
      `onNavigateToPractice` tras guardar — spec `settings-screen`, escenario "Guardar con el nivel
      CEFR cambiado navega a práctica".
- [ ] 4.4 Añadir el parámetro `onNavigateToPractice: () -> Unit = {}` a `SettingsRoute`; en un
      `LaunchedEffect` lo invoca cuando `navigateToPracticePending` es `true` y envía
      `ConsumeNavigateToPractice` (design D5) — verificar que 4.3 pasa a verde.
- [ ] 4.5 Test (rojo, `androidTest`): `PracticeSessionScreenTest` (o uno nuevo) — al recomponer
      `PracticeSessionRoute` (simulando volver a la pestaña), se refleja una tanda distinta si cambió
      en el repositorio, y se conserva la frase revelada y la posición si no cambió — design D4.
- [ ] 4.6 Añadir `LaunchedEffect(Unit) { viewModel.onAction(PracticeSessionAction.Resume) }` en
      `PracticeSessionRoute` (design D4) — verificar que 4.5 pasa a verde.

## 5. Integración en `:app`

- [ ] 5.1 Pasar `onNavigateToPractice = { selectedDestination = BottomDestination.Practice }` a
      `SettingsRoute` desde `FraseAFraseApp` — verificar manualmente en el emulador: cambiar el nivel
      CEFR en Ajustes y guardar muestra el snackbar y cambia a la pestaña de práctica con una tanda
      nueva del nivel elegido (o con las arrastradas/pendientes primero, según ADR-0012).

## 6. Verificación final

- [ ] 6.1 Ejecutar `./gradlew check koverVerify` en verde (tests unitarios, instrumentados, detekt,
      ktlint y cobertura mínima).

## Workflow follow-up

- Ejecutar `/opsx:verify` antes de archivar.
- Archivar el cambio con `/opsx:archive` y sincronizar la spec `settings-screen`.
