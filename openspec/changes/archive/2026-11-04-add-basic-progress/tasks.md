# Tasks

## 1. Dominio: resumen de progreso

- [x] 1.1 Escribir el test de `ObtainProgressOverview` para "Tanda con certificadas y no
  certificadas" y "Tanda sin ninguna certificada" (spec `progress-overview`) y comprobar que falla
  porque la clase no existe todavía
- [x] 1.2 Implementar `ObtainProgressOverview`, `ProgressOverviewResult` y `BatchProgress` en
  `:core:domain` (paquete `progress`) hasta que los tests de 1.1 pasen
- [x] 1.3 Escribir el test de "Catálogo con frases en varios niveles" y "Ninguna frase con progreso"
  para el cálculo de `byLevel`, comprobar que falla y luego implementar hasta que pase
- [x] 1.4 Escribir el test de "Frase de la tanda activa cuenta en nivel 0", comprobar que falla y
  luego implementar hasta que pase
- [x] 1.5 Escribir el test de "Sin tanda activa muestra solo el total por nivel" (`batchProgress`
  nulo en `Available`), comprobar que falla y luego implementar hasta que pase
- [x] 1.6 Escribir el test de "Catálogo ilegible al abrir el progreso" (`CatalogUnavailable`),
  comprobar que falla y luego implementar hasta que pase

## 2. Presentación: `:feature:progress`

- [x] 2.1 Añadir el módulo `:feature:progress` a `settings.gradle.kts` con el plugin
  `fraseafrase.android.feature`, crear su `build.gradle.kts` (plugin `fraseafrase.android.feature`
  y `namespace`) y comprobar que `./gradlew :feature:progress:check` se ejecuta (módulo vacío, sin
  tests todavía)
- [x] 2.2 Escribir el test de `ProgressViewModel` para las tres variantes de `ProgressUiState`
  (`Content` con `certifiedInBatch`, `Content` sin `certifiedInBatch`, `CatalogError`) con un fake
  de `ObtainProgressOverview`, y comprobar que falla porque `ProgressViewModel` no existe
- [x] 2.3 Implementar `ProgressUiState`, `ProgressAction` y `ProgressViewModel` hasta que los tests
  de 2.2 pasen
- [x] 2.4 Escribir el test de Compose UI para `ProgressScreen` cubriendo "Tanda con certificadas y
  no certificadas", "Sin tanda activa muestra solo el total por nivel" (`Content` sin
  `certifiedInBatch`) y "Catálogo ilegible al abrir el progreso", y comprobar que falla
- [x] 2.5 Implementar `ProgressScreen` (con tokens de `:core:designsystem`, sin literales) y
  `ProgressRoute` hasta que los tests de 2.4 pasen

## 3. Retirar la navegación por `back stack` de las features existentes

- [x] 3.1 Quitar el botón de ajustes y el parámetro `onOpenSettings` de `PracticeSessionScreen` y
  `PracticeSessionRoute`; actualizar `PracticeSessionScreenTest` y los `@Preview` para que compilen
  sin él, y comprobar que `./gradlew :feature:session:check` pasa
- [x] 3.2 Quitar el botón de volver y el parámetro `onBack` de `SettingsScreen` y `SettingsRoute`;
  mover el escenario "Salir sin guardar no persiste la edición" (spec `settings-screen`, delta) a un
  test de `SettingsViewModelTest` que no dependa de `onBack`, y comprobar que
  `./gradlew :feature:settings:check` pasa

## 4. Integración en `:app`: barra de navegación inferior

- [x] 4.1 Escribir el test instrumentado de `FraseAFraseApp` para "Los tres destinos están
  disponibles", "Seleccionar progreso desde la práctica", "Seleccionar ajustes desde el progreso",
  "Volver a la práctica desde cualquier destino" y "Arranque muestra la práctica seleccionada" (spec
  `app-navigation`), y comprobar que falla porque la barra de navegación no existe
- [x] 4.2 Sustituir `Practice`/`Settings` en `Routes.kt` por un `BottomDestination` sellado con
  `Practice`, `Progress` y `Settings`
- [x] 4.3 Reemplazar el `NavDisplay` de `FraseAFraseApp.kt` por un `Scaffold` con `NavigationBar` de
  tres `NavigationBarItem`, que compone `PracticeSessionRoute`, `ProgressRoute` o `SettingsRoute`
  según el destino seleccionado, hasta que los tests de 4.1 pasen
- [x] 4.4 Añadir `ProgressRoute` al grafo de Hilt de `:app` (ningún módulo de DI nuevo: usa el mismo
  patrón que `SettingsRoute`)

## 5. Verificación final

- [x] 5.1 Ejecutar `./gradlew check` y confirmar que pasa (tests, detekt y ktlint de todos los
  módulos, incluido el nuevo `:feature:progress`)
- [x] 5.2 Ejecutar `./gradlew koverVerify` y confirmar los mínimos de cobertura (dominio 90 %,
  presentación 80 %, global 70 %) con `ObtainProgressOverview` y `ProgressViewModel` incluidos
