# Tasks

## 1. Recursos de design system (sin lógica, base para el resto)

- [x] 1.1 Copiar los 6 vectores entregados (`ic_rango_sin_rango`, `ic_rango_bronce`,
      `ic_rango_plata`, `ic_rango_oro`, `ic_rango_platino`, `ic_rango_diamante`) de
      `android_drawable/` a `core/designsystem/src/main/res/drawable/` y verificar que el módulo
      compila con los nuevos recursos (`./gradlew :core:designsystem:assembleDebug`).
- [x] 1.2 Descargar los ficheros estáticos OFL de Baloo 2 (600/700) y Plus Jakarta Sans
      (400/500/600) desde Google Fonts y añadirlos a `core/designsystem/src/main/res/font/`
      (design D4); verificar que el módulo compila con los recursos de fuente añadidos.

## 2. Paleta y tipografía (`:core:designsystem`)

- [x] 2.1 Escribir el test de Compose (que debe fallar contra los valores por defecto de Material 3)
      que verifica que, dentro de `AppTheme(darkTheme = false)` y `AppTheme(darkTheme = true)`,
      `MaterialTheme.colorScheme.primary`/`tertiary`/`background`/`surface` son los valores fijados
      por la tabla de roles de design D1.
- [x] 2.2 Actualizar `theme/Color.kt` con `LightColors`/`DarkColors` explícitos según D1; verificar
      que el test de 2.1 pasa.
- [x] 2.3 Escribir el test de Compose (que debe fallar) que verifica que
      `MaterialTheme.typography.titleLarge.fontFamily` es la familia Baloo 2 esperada.
- [x] 2.4 Actualizar `theme/Type.kt` para declarar `FontFamily` de Baloo 2 y Plus Jakarta Sans a
      partir de los recursos de la tarea 1.2 y aplicarlas a la `Typography` de Material 3 (D4);
      verificar que el test de 2.3 pasa.
- [x] 2.5 Escribir el test de Compose (que debe fallar) que verifica que
      `MaterialTheme.shapes.large` tiene el radio esperado por D2.
- [x] 2.6 Crear `theme/Shape.kt` con un `Shapes` de esquinas grandes (D2) y pasarlo a
      `MaterialTheme` en `AppTheme.kt`; verificar que el test de 2.5 pasa.

## 3. `DsLevelBadge` con los 6 rangos

- [ ] 3.1 Escribir el test (que debe fallar) de `DsLevelBadgeTest` (unit, `core/designsystem/src/test`)
      para `levelBadgeLabel(0) == "Sin rango"` (spec `level-badges`, escenario "Nivel 0 no muestra
      insignia" → pasa a mostrar "Sin rango").
- [ ] 3.2 Actualizar `levelBadgeLabel`/`levelBadgeSpec` en `DsLevelBadge.kt` para que el nivel 0
      devuelva una insignia ("Sin rango", icono `ic_rango_sin_rango`) en vez de `null`, y que los
      niveles 1 a 5 usen los vectores de la tarea 1.1 en vez de `Icons.Filled.*` (D3); verificar que
      el test de 3.1 pasa.
- [ ] 3.3 Actualizar el test instrumentado `DsLevelBadgeTest` (`androidTest`) para el nuevo icono de
      nivel 0 (spec `level-badges`, escenario "Sin rango y Bronce no comparten representación");
      verificar en el emulador que las 6 insignias pintan icono y etiqueta distintos entre sí.
- [ ] 3.4 Actualizar las `@PreviewLightDark` de `DsLevelBadge.kt` para los 6 niveles (0 a 5) y
      verificar visualmente en el panel de previews que cada una usa su vector e incluye "Sin
      rango".

## 4. Barra de navegación inferior (`:app`)

- [ ] 4.1 Escribir el test (que debe fallar) en el test instrumentado de `FraseAFraseApp` que
      verifica que los tres `NavigationBarItem` muestran un icono (spec `app-navigation`, escenario
      "Los tres destinos están disponibles").
- [ ] 4.2 Añadir `implementation(libs.androidx.compose.material.icons.extended)` a
      `app/build.gradle.kts` (D5, hoy solo llega transitiva dentro de `:core:designsystem`) y
      `Icons.Default.AutoStories`, `Icons.Default.BarChart` y `Icons.Default.Tune` a los
      `icon = {}` vacíos de `FraseAFraseApp.kt`; verificar que el test de 4.1 pasa.

## 5. Pantalla de Progreso: total disponible y nivel CEFR

- [ ] 5.1 Escribir los tests (que deben fallar) de `ProgressViewModelTest` para los escenarios de la
      spec `progress-overview`: "El total coincide con la suma de los niveles", "El total se
      actualiza si cambia el catálogo disponible" y "Sin tanda activa muestra solo el total por
      nivel" (ahora con `totalAvailable`/`cefrLevel` en vez de `certifiedInBatch`).
- [ ] 5.2 Sustituir `certifiedInBatch: CertifiedCount?` por `totalAvailable: Int` y
      `cefrLevel: CefrLevel` en `ProgressUiState.Content` (eliminar `CertifiedCount`, sin otro uso
      en el proyecto); actualizar `ProgressViewModel.kt` para inyectar `ObtainUserSettings` y
      calcular `totalAvailable = byLevel.values.sum()` (D6); verificar que los tests de 5.1 pasan.
- [ ] 5.3 Actualizar `ProgressScreen.kt` para pintar la tarjeta superior con `totalAvailable` y
      `cefrLevel` en vez de la línea de certificadas, y actualizar `ProgressScreenPreviews.kt` con
      los nuevos campos; verificar con un test de Compose UI que la tarjeta muestra el texto
      esperado (p. ej. "120" y "B1").
- [ ] 5.4 Actualizar `ProgressScreenTest.kt` (androidTest) para los escenarios de `progress-overview`
      afectados: "La fila de nivel 0 no muestra insignia ni número de nivel" (ahora con etiqueta
      "Sin rango") y la tarjeta de total disponible; verificar en el emulador.

## 6. Pantalla de Práctica: insignia en nivel 0 y restilizado

- [ ] 6.1 Escribir el test (que debe fallar) de `PracticeSessionScreenTest` (androidTest) para el
      escenario "Sin insignia en nivel 0" de la spec `practice-session` (ahora espera la insignia
      "Sin rango" en vez de ausencia de insignia).
- [ ] 6.2 Verificar que `PracticeSessionScreen.kt` ya pinta `DsLevelBadge` sin condición especial
      para nivel 0 (la condición vivía en `DsLevelBadge`, ya corregida en 3.2); si hubiera lógica
      adicional en la pantalla que oculte la insignia en nivel 0, eliminarla. Verificar que el test
      de 6.1 pasa.
- [ ] 6.3 Aplicar los nuevos tokens de color/forma/tipografía (`MaterialTheme.colorScheme`,
      `MaterialTheme.shapes`, tipografía) a las tarjetas, al botón "Mostrar traducción" y a los
      botones "Fallo"/"Acierto" de `PracticeSessionScreen.kt`, sin introducir valores literales
      (regla de diseño del proyecto); verificar visualmente con `@PreviewLightDark` que coincide con
      los mocks "Practicar · traducción oculta" y "Practicar · traducción visible".

## 7. Pantalla de Ajustes: restilizado

- [ ] 7.1 Aplicar los nuevos tokens de color/forma/tipografía a las tarjetas de nivel CEFR, al
      stepper de tamaño de tanda y al botón "Guardar" de `SettingsScreen.kt`, sin cambiar su
      lógica ni introducir valores literales; verificar visualmente con `@PreviewLightDark` que
      coincide con el mock "Ajustes".

## 8. Verificación final

- [ ] 8.1 Ejecutar `./gradlew check` y confirmar que pasa en verde (tests unitarios, ktlint,
      detekt).
- [ ] 8.2 Ejecutar `./gradlew koverVerify` y confirmar que se cumplen los mínimos de cobertura
      (dominio 90 %, presentación 80 %, global 70 %).
- [ ] 8.3 Ejecutar los tests instrumentados afectados (`DsLevelBadgeTest`, `FraseAFraseApp`,
      `ProgressScreenTest`, `PracticeSessionScreenTest`) en un emulador y confirmar que pasan.

## Workflow follow-up

- Verificar el cambio con `/opsx:verify` antes de archivar.
- Archivar el cambio con `/opsx:archive` en la misma rama, antes de la pull request.
