# Proposal

## Why

Una revisión visual en dispositivo real de la pantalla de Practicar detecta dos defectos y motiva un
rediseño: (1) el botón flotante para deshacer la última autoevaluación (`RevertLastAnswerButton`)
queda visualmente demasiado elevado sobre la barra de navegación inferior; (2) la propia barra de
navegación inferior (`FraseAFraseBottomBar`) queda tapada por los controles del sistema (barra de
gestos o botones de navegación) en vez de quedar por encima de ellos. La causa de (2) es que
`FraseAFraseBottomBar` fija la altura de su `NavigationBar` con `Modifier.height(AppTheme.dimensions.navHeight)`
(80dp): el `NavigationBar` de Material 3 ya añade por dentro el padding de `WindowInsets.navigationBars`
antes de aplicar su alto mínimo, así que en dispositivos con barra de gestos o de 3 botones el
contenido necesita más de 80dp totales y la altura fija recorta la parte inferior de los tres
destinos. Aprovechando la corrección, el usuario pide además modernizar la barra al estilo "flotante"
de Material 3 Expressive (superficie separada de los bordes de la pantalla, no a sangre) en vez de la
barra a sangre actual.

## What Changes

- **Fix**: la barra de navegación inferior deja de recortarse en dispositivos con barra de gestos o de
  3 botones: su contenido ya no depende de una altura fija que ignore `WindowInsets.navigationBars`.
- **Rediseño**: `FraseAFraseBottomBar` sustituye la barra a sangre (`NavigationBar` dentro del slot
  `bottomBar` del `Scaffold`, con fondo y ancho completos) por una barra flotante — superficie con
  forma redondeada, elevación y margen respecto a los tres bordes inferiores de la pantalla (margen
  mínimo 16dp, guía M3 de barras/toolbars flotantes) — posicionada como overlay sobre el contenido, con
  el mismo patrón que ya usa `RevertLastAnswerButton` (hermano del contenido con offsets absolutos, no
  slot del `Scaffold`), para poder controlar directamente su padding de `WindowInsets.navigationBars`
  sin el recorte de altura fija que causa el fallo anterior.
- **Fix**: el botón flotante de deshacer se reposiciona para quedar a una distancia fija y moderada
  (no superpuesto, tampoco con un hueco grande) por encima de la nueva barra flotante, siguiendo la
  guía de M3 de posicionar un control flotante por encima de la barra de navegación sin taparla; el
  valor exacto de separación es una decisión de `design.md` (sustituye el offset fijo de 172dp desde
  el borde de pantalla, calculado para la barra a sangre anterior, que ya no aplica con la barra
  flotante).
- El contenido de cada pantalla (Practicar, Progreso, Ajustes) reserva el espacio inferior de la nueva
  barra flotante igual que antes reservaba el de la barra a sangre, para que ningún control quede
  oculto detrás de ella.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `app-navigation`: nuevo requisito — la barra de navegación inferior MUST permanecer completamente
  visible por encima de las barras del sistema (barra de gestos o de navegación de 3 botones),
  cualquiera que sea su alto. Es un comportamiento observable y verificable (el defecto reportado), no
  solo un detalle de estilo; el resto de requisitos de `app-navigation` (tres destinos, cambio de
  pestaña, pestaña de inicio) no cambia.

## Impact

- **Código**: `:app` (`FraseAFraseApp.kt` — `FraseAFraseBottomBar`/`FraseAFraseBottomBarItem`),
  `:feature:session` (`PracticeSessionScreen.kt`, `PracticeSessionRevertButton.kt`, offset del FAB de
  deshacer), `:core:designsystem` (`Dimensions.kt` — tokens de alto/margen/forma de la barra flotante y
  de separación del FAB de deshacer; posible token de elevación/forma si no existe ya uno reutilizable
  en `AppCornerShapes`).
- **Tests**: `app/src/androidTest/.../FraseAFraseAppTest.kt` (las aserciones de alto exacto de
  `bottom-nav-bar` cambian de forma — superficie flotante en vez de barra a sangre — y se añade un
  escenario de visibilidad completa sobre los insets del sistema);
  `feature/session/src/androidTest/.../PracticeSessionRevertButtonScreenTest.kt` (el offset fijo de
  172dp desde el borde de pantalla se sustituye por el nuevo valor relativo a la barra flotante).
- **Dependencias**: ninguna nueva; Material 3 (`androidx.compose.material3`, BOM `2026.09.00`) ya está
  en el catálogo de versiones y no existe un componente dedicado "barra de navegación flotante" en esa
  versión, así que la superficie flotante se construye con primitivas ya usadas en el proyecto
  (`Surface`/`Row` con forma, elevación y `windowInsetsPadding`), igual que ya hace
  `RevertLastAnswerButton` con `FloatingActionButton`.
- **ADR**: ninguno queda contradicho. Respeta ADR-0006 (Compose + MVVM) y ADR-0010 (el componente sigue
  viviendo en `:app`, único módulo que compone las tres features; no se añade dependencia de proyecto
  nueva). No se propone ningún ADR nuevo.

## Fuera de alcance

- Cualquier cambio en la lógica de selección de pestaña, pestaña de inicio o en la lógica de
  visibilidad del botón de deshacer (`revert-selection`, `practice-session`): solo cambia su
  presentación y posición.
- Adaptar la barra a anchos de pantalla medianos/grandes (tablet, `ShortNavigationBar` u otra variante
  de M3 para esos tamaños): el proyecto no soporta hoy ese rango de pantallas y no se introduce aquí.
- Tema oscuro: se deriva automáticamente del esquema actual, igual que el resto de componentes; no se
  ajusta a mano en este cambio salvo que algún color de la barra flotante no se vea bien derivado
  (se señalará en `design.md` si ocurre).
- Cualquier otro control flotante o barra de la app (cabecera de Practicar, barra de progreso,
  `SnackbarHost`): no se tocan.
