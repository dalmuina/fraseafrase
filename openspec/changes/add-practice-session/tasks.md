# Tasks

## 1. Dominio: lectura de la sesión de práctica

- [x] 1.1 Escribir tests de `ObtainPracticeSession` con los casos de uso reales (`ObtainActiveBatch` y `ComposeNextBatch(FixedClock)`) sobre `FakePracticeRepository`, `FakeCatalogRepository` y `FakeSettingsRepository`, para los escenarios "Al abrir la práctica se muestra la primera frase en español" (texto de origen y destino en orden de tanda), "Reanudar la tanda activa", "Primer uso muestra la primera frase del catálogo", "Cambio de día con tanda activa no altera la tanda", "Catálogo agotado tras cerrar" y "Frases con repaso futuro sin tanda" (`NothingToPractice`); verificar que fallan porque `ObtainPracticeSession` aún no existe.
- [x] 1.2 Implementar `ObtainPracticeSession`, `PracticeSessionResult` (`Ready`, `NothingToPractice`, `CatalogFailed`) y `PracticeItem` (`id`, `source`, `target`, `streak`, `isKnown`) en `core.domain.practice`; verificar que los tests de 1.1 pasan con `./gradlew :core:domain:test`.
- [x] 1.3 Escribir tests de la proyección para "Aviso con frases no certificadas", "Aviso con una sola frase no certificada", "Aviso sin frases no certificadas" (`uncertifiedCount`), "Frase con racha 3 marcada como sabida" e "Frase con racha 2 no se marca" (`isKnown`), y "Error de catálogo" (`CatalogFailed` con `AssetUnreadable` y con `MalformedDocument`); verificar que fallan porque los campos y el resultado `CatalogFailed` aún no se devuelven.
- [x] 1.4 Completar la proyección en `ObtainPracticeSession`: `uncertifiedCount` desde `PracticeBatch.uncertifiedCount()`, `isKnown` como `streak >= 3`, y `CatalogFailed` cuando la segunda lectura del catálogo falla; verificar que los tests de 1.3 pasan.
- [x] 1.5 Escribir test para "Frase de la tanda ausente del catálogo" (`IllegalArgumentException` con el id), que falla hasta que la unión lo valide; verificar que falla por el motivo esperado.
- [x] 1.6 Validar en la unión que cada id de la tanda existe en el catálogo y lanzar `IllegalArgumentException`; verificar que pasa el test de 1.5.
- [x] 1.7 Verificar cobertura del dominio con `./gradlew :core:domain:koverVerify` (mínimo 90 %).

## 2. Datos: catálogo en memoria

- [x] 2.1 Escribir tests de `AssetsCatalogDataSource` que comprueben que una segunda llamada a `phrases()` no vuelve a leer el asset (contador de lecturas en un asset de prueba) y que un `Failed` también se memoiza; verificar que fallan porque la memoización aún no existe.
- [x] 2.2 Implementar la memoización de la carga con un `Mutex` en `AssetsCatalogDataSource`, sin cambiar el puerto `CatalogRepository`; verificar que los tests de 2.1 pasan con `./gradlew :core:data:testDebugUnitTest` y que el contrato de ids sigue en verde.
- [x] 2.3 Escribir un test de `AssetsCatalogDataSource` para "cancelación durante la lectura": una `CancellationException` en la lectura se propaga y no se convierte en `Failed`, y la siguiente llamada vuelve a leer el asset (no se memoiza una cancelación); verificar que falla por el motivo esperado (el resultado es `Failed` en lugar de propagar la excepción).
- [x] 2.4 Implementar en `AssetsCatalogDataSource` que la `CancellationException` se relance sin capturarla como `AssetUnreadable`, sin memoizar la cancelación; verificar que pasa el test de 2.3 y que los tests de 2.1 siguen en verde con `./gradlew :core:data:testDebugUnitTest` y `./gradlew detekt`.

## 3. Presentación: estado y ViewModel

- [x] 3.1 Escribir tests de `PracticeSessionViewModel` con Turbine y `MainDispatcherExtension`, con los casos de uso reales sobre los fakes, para "Frase en español con traducción oculta" (estado `Practicing` sin revelar y con el texto de origen) y "Revelar muestra la traducción en portugués"; verificar que fallan porque el ViewModel aún no existe.
- [x] 3.2 Implementar `PracticeSessionViewModel`, `PracticeSessionUiState` (`Loading`, `Practicing`, `NothingToPractice`, `CatalogError`) y `PracticeSessionAction` con `Reveal`; cargar la sesión al iniciar con `ObtainPracticeSession`; verificar que los tests de 3.1 pasan.
- [x] 3.3 Escribir tests para "Acierto muestra la siguiente frase", "Fallo muestra la siguiente frase", "Sin revelar no hay autoevaluación", "La tanda da la vuelta sin desaparecer frases" y "Doble toque no registra dos respuestas"; verificar que fallan.
- [x] 3.4 Implementar `Answer(SelfAssessment)` con las reglas de D3 (ignorada sin revelar, con `busy` o con diálogo abierto), `busy` síncrono antes de `AnswerPhrase` y recarga de la sesión; verificar que los tests de 3.3 pasan.
- [x] 3.5 Escribir tests para "Racha visible tras un acierto", "Fallo reinicia la racha visible" y "Frase con racha 3 marcada como sabida" (campos `streak` e `isKnown` del estado); verificar que fallan. _Verificado por mutación (3.14): M1 mata «racha visible», M3 mata «fallo reinicia», M2 mata «sabida con racha 3», M4 mata «racha 2 no se marca».
- [x] 3.6 Exponer `streak` e `isKnown` en el estado sin cambiar el orden de la lista; verificar que pasan los tests de 3.5. _Verificado por mutación (3.14): M1 a M4 cambian `streak` o `isKnown` y lo detectan los tests de 3.5._
- [x] 3.7 Escribir tests para "Aviso con frases no certificadas", "Aviso sin frases no certificadas", "Cancelar el cierre no cambia nada", "Confirmar el cierre usa el número del aviso" y "Con el aviso abierto no se puede autoevaluar" (`RequestClose`, `CancelClose`, `ConfirmClose` con `CloseActiveBatch` y su expectativa); verificar que fallan.
- [x] 3.8 Implementar `RequestClose`, `CancelClose` y `ConfirmClose` con `closeWarning` en el estado; `ConfirmClose` sin aviso se ignora; tras confirmar, recargar la sesión, que compone la tanda siguiente; verificar que pasan los tests de 3.7.
- [x] 3.9 Escribir tests para "Cierre con arrastradas muestra la tanda siguiente", "Cierre con todas certificadas deja la tanda siguiente sin arrastradas", "Aviso desfasado recarga la tanda" y "Cierre sin tanda activa recarga la pantalla" (`Stale` y `NoActiveBatch` solo recargan sin error); verificar que fallan.
- [x] 3.10 Implementar el manejo de `Closed`, `Stale` y `NoActiveBatch`; verificar que pasan los tests de 3.9.
- [x] 3.11 Verificar cobertura de presentación con `./gradlew :feature:session:koverVerify` (mínimo 80 %).
- [x] 3.12 Escribir un test de `PracticeSessionViewModel` para "Cerrar mientras hay una respuesta en curso no abre el aviso": tras `Answer` y antes de avanzar el dispatcher, `RequestClose` no debe abrir el diálogo; verificar que falla por aserción, porque hoy `RequestClose` abre el diálogo sin mirar `busy`.
- [x] 3.13 Implementar en el ViewModel que `RequestClose` se ignora mientras `busy` es true, y que si no lo está lee la sesión con `ObtainPracticeSession` y pone en `closeWarning` su `uncertifiedCount` (sin contador privado). Ajustar los tests de 3.7 y 3.9 que pedían el aviso sin avanzar el dispatcher. Verificar que pasa el test de 3.12 y los de cierre con `./gradlew :feature:session:testDebugUnitTest`.
- [x] 3.14 Verificación por mutación de 3.5 y 3.6 (procedimiento de test nacido verde): mutar temporalmente el mapeo de `ObtainPracticeSession` a `streak = 0` e `isKnown = false`, comprobar que fallan los tests de racha de 3.5 y de sabida de 3.6, revertir sin dejar rastro y anotar el resultado en 3.5 y 3.6.

## 4. Presentación: pantalla

- [x] 4.1 Escribir previews `@PreviewLightDark` de `PracticeSessionScreen` para los estados `Loading`, `Practicing` oculto, `Practicing` revelado, `Practicing` con diálogo de cierre, `NothingToPractice` y `CatalogError`; verificar que compilan con `./gradlew :feature:session:compileDebugKotlin`. Las previews son verificación visual, no sustituyen a 4.2.
- [x] 4.2 Escribir Compose UI Tests en `androidTest` para "Sin revelar no hay autoevaluación", "Revelar muestra la traducción en portugués", "Aviso con frases no certificadas" (texto del diálogo con su número), "Aviso sin frases no certificadas" (texto con cero), "Frase con racha 3 marcada como sabida" y "Estado vacío sin frases disponibles"; verificar que fallan porque `PracticeSessionScreen` aún no existe.
- [x] 4.2a Fijar `espresso-core` 3.7.0 en `androidTest` de `:feature:session` (entrada en `gradle/libs.versions.toml`, design.md D10) y verificar que `./gradlew :feature:session:connectedDebugAndroidTest --no-configuration-cache` ejecuta los UI tests de 4.2 y 4.2b sin el fallo de `InputManager.getInstance`.
- [x] 4.2b Escribir un Compose UI Test en `androidTest` para "Cerrar mientras hay una respuesta en curso no abre el aviso": el botón de cerrar está deshabilitado mientras `busy` es true; verificar que falla porque la pantalla aún no existe.
- [x] 4.3 Implementar `PracticeSessionScreen` sin estado, con composables privados para la tarjeta de frase y los botones, usando `MaterialTheme` y `AppTheme.spacing`, sin colores ni dimensiones literales, con las cadenas en `feature/session/src/main/res/values/strings.xml` y el diálogo como `AlertDialog` modal con plural; verificar con `./gradlew :feature:session:assembleDebug` y, con emulador, `./gradlew :feature:session:connectedDebugAndroidTest`.
- [x] 4.4 Implementar `PracticeSessionRoute` que conecta el ViewModel con la pantalla (ADR-0006); verificar con `./gradlew :feature:session:compileDebugKotlin`.

## 5. Integración en :app

- [x] 5.1 Escribir un test unitario en `:app` que compruebe que `PRACTICE_START_DESTINATION` es la pantalla de práctica; verificar que falla porque la constante aún no existe.
- [x] 5.2 Declarar `PRACTICE_START_DESTINATION`, añadir `:feature:session` a `app/build.gradle.kts` y mostrar `PracticeSessionRoute` como destino inicial en `FraseAFraseApp`; verificar que pasa el test de 5.1.
- [x] 5.3 Verificar el cableado de Hilt con `./gradlew :app:assembleDebug` (el grafo debe resolver `ObtainPracticeSession`, `AnswerPhrase` y `CloseActiveBatch`).
- [ ] 5.4 Verificar a mano en el emulador con la skill `run`: abrir la app (primer uso), practicar tres frases, cerrar la tanda con aviso, cancelar, confirmar y reabrir la app con la tanda guardada.

## 6. Verificación final

- [ ] 6.1 Ejecutar `./gradlew detekt check koverVerify assembleDebug --rerun -PwarningsAsErrors=true` en verde. El proyecto no tiene ktlint propio: el formato lo aporta `detekt-formatting` (ADR-0011).
- [ ] 6.2 Ejecutar `./gradlew connectedDebugAndroidTest --no-configuration-cache` en verde en un emulador (los tests de 4.2, 4.2b y 4.2a).
