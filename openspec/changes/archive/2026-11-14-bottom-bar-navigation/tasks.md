# Tasks

## 1. Tokens de design system

- [x] 1.1 Actualizar `DimensionsTest` (`core/designsystem/src/test/.../tokens/DimensionsTest.kt`) para
      esperar el token nuevo `navFloatingMargin` (16dp, design D1/D3) y ejecutar el test, comprobando
      que falla porque el token todavía no existe.
- [x] 1.2 Añadir `navFloatingMargin` a `Dimensions` (`core/designsystem/.../tokens/Dimensions.kt`) y
      `FloatingNav` (`RoundedCornerShape(percent = 50)`, cápsula completa, design D1) a
      `AppCornerShapes` (`core/designsystem/.../theme/Shape.kt`); verificar que `DimensionsTest` pasa.
- [x] 1.3 Actualizar `DimensionsTest` para esperar el nuevo valor de `revertFabBottomOffset` (design
      D3: distancia = `navFloatingMargin` en vez de 172dp) y ejecutar el test, comprobando que falla
      por el valor antiguo.
- [x] 1.4 Cambiar el valor de `revertFabBottomOffset` en `Dimensions` al nuevo token de margen; verificar
      que `DimensionsTest` pasa.

## 2. Barra de navegación flotante en `:app`

- [x] 2.1 Actualizar `FraseAFraseAppTest` (`app/src/androidTest/.../ui/FraseAFraseAppTest.kt`):
      (a) reescribir `givenAppLaunched_whenLookingAtTheBottomBar_thenItsHeightMatchesTheMock` para medir,
      en vez del alto fijo de 80dp del `Surface`, que la barra queda separada de los tres bordes
      inferiores de la pantalla por `navFloatingMargin` (más el inset de `WindowInsets.navigationBars`
      en el borde inferior); (b) añadir el escenario de la especificación "Barra de navegación visible
      sobre las barras del sistema" (`openspec/changes/bottom-bar-navigation/specs/app-navigation/spec.md`):
      leer el inset real de `WindowInsets.navigationBars` con
      `ViewCompat.getRootWindowInsets(activity.window.decorView)` y comprobar que los límites
      (`getUnclippedBoundsInRoot()`) de los tres destinos quedan íntegramente por encima de ese inset;
      como el mecanismo bajo prueba no distingue el tipo de barra del sistema, un comentario en el test
      deja constancia explícita de que este único test cubre los dos GIVEN de la spec (barra de gestos y
      barra de 3 botones). Ejecutar ambos tests y comprobar que fallan contra la barra a sangre actual
      (altura fija, sin margen, contenido recortado por el inset).
- [x] 2.2 Reescribir `FraseAFraseBottomBar` (`app/src/main/.../ui/FraseAFraseApp.kt`) como `Surface` con
      forma `AppCornerShapes.FloatingNav` (cápsula completa) y elevación en vez de `NavigationBar`,
      conteniendo la `Row` existente de `FraseAFraseBottomBarItem` sin cambios en su contenido interno
      (icono, indicador de píldora, etiqueta); aplicar `AppTheme.dimensions.navHeight` como alto de esa
      `Row` interna (ya no del `Surface`, que debe poder crecer con los insets, design D4) y
      `Modifier.windowInsetsPadding(WindowInsets.navigationBars)` seguido del margen flotante al
      `Surface` (design D1); sustituir también el literal preexistente `RoundedCornerShape(percent = 50)`
      del indicador de pestaña seleccionada en `FraseAFraseBottomBarItem` por el mismo token
      `AppCornerShapes.FloatingNav`, ya que ambos coinciden en valor y el token ya existe; verificar que
      los dos tests de 2.1 pasan.
- [x] 2.3 Actualizar `FraseAFraseApp` para dejar de pasar `FraseAFraseBottomBar` como `bottomBar` del
      `Scaffold` raíz y colocarla como overlay (`Box` con `Modifier.align(Alignment.BottomCenter)`)
      sobre el contenido (design D2); cada pantalla (Practicar, Progreso, Ajustes) recibe el padding
      inferior necesario para reservar el hueco de la barra flotante en vez del `innerPadding`
      automático del `Scaffold`; verificar manualmente (o con el test existente de selección de pestaña)
      que ningún contenido de las tres pantallas queda oculto tras la barra.
- [x] 2.4 Verificar que el resto de tests de `FraseAFraseAppTest` (tres destinos visibles, cambio de
      pestaña, pestaña de inicio, indicador del tab activo, color de etiquetas inactivas) sigue pasando
      con la barra flotante; ajustar únicamente las aserciones de geometría que dependan de la forma
      anterior (a sangre) sin cambiar su intención.

## 3. Offset del FAB de deshacer en `:feature:session`

- [x] 3.1 Actualizar `PracticeSessionRevertButtonScreenTest`
      (`feature/session/src/androidTest/.../PracticeSessionRevertButtonScreenTest.kt`,
      `givenCanRevertLastAnswerTrue_whenScreenShown_thenRevertButtonIsOffsetFromScreenEdgesPerMock`)
      para esperar el nuevo `REVERT_FAB_BOTTOM_OFFSET` (igual a `AppTheme.dimensions.navFloatingMargin`,
      design D3) en vez de 172dp, y ejecutar el test, comprobando que falla contra el offset antiguo.
- [x] 3.2 Verificar que `PracticeSessionScreen` ya usa `AppTheme.dimensions.revertFabBottomOffset` (sin
      cambios de código, solo del valor del token cambiado en la tarea 1.4) y que el test de 3.1 pasa.

## 4. Verificación final

- [x] 4.1 Ejecutar `./gradlew check` (tests, ktlint, detekt) y `./gradlew koverVerify`, y confirmar que
      ambos terminan en verde.
