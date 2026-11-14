# Design

## Context

`FraseAFraseBottomBar` (en `app/src/main/kotlin/.../ui/FraseAFraseApp.kt`) es hoy un `NavigationBar` de
Material 3 dentro del slot `bottomBar` del `Scaffold` raíz, con `Modifier.height(AppTheme.dimensions.navHeight)`
(80dp fijo). `NavigationBar` aplica por dentro `Modifier.windowInsetsPadding(NavigationBarDefaults.windowInsets)`
antes de su alto mínimo (`defaultMinSize(minHeight = 80dp)`): con una altura fija impuesta desde fuera,
el contenido que ese padding desplaza hacia arriba no cabe en los 80dp del `Surface` y la parte inferior
de los tres destinos queda recortada tras la barra del sistema. El `Scaffold` raíz reparte el espacio
restante como `innerPadding` a las tres pantallas (Practicar, Progreso, Ajustes).

`RevertLastAnswerButton` (botón flotante naranja de deshacer, en `:feature:session`) ya NO usa el slot
`floatingActionButton` del `Scaffold`: vive como hermano del contenido en un `Box`, posicionado con
`Modifier.offset(x = ..., y = ...)` fijo en dp desde las esquinas de la pantalla
(`revertFabHorizontalOffset` = 20dp, `revertFabBottomOffset` = 172dp). Ese offset se calculó para que
el FAB quedara justo encima de la barra a sangre anterior; con la barra flotante nueva deja de ser
correcto.

No existe en la versión de Material 3 del proyecto (BOM `2026.09.00`) un componente "barra de
navegación flotante" equivalente a `NavigationBar`: la guía M3 de barras/toolbars flotantes (mínimo
16dp de margen respecto al borde de la ventana) se aplica construyendo la superficie a mano con
`Surface` + `Row`, igual que `HorizontalFloatingToolbar` hace internamente para su propio caso de uso
(ese componente no encaja aquí: está pensado para una fila de acciones con un FAB adjunto, no para 3-5
destinos de navegación con icono+etiqueta y estado seleccionado).

## Goals / Non-Goals

**Goals:**
- La barra de navegación inferior nunca queda recortada por los insets del sistema,
  independientemente de su alto.
- La barra pasa a ser una superficie flotante (separada de los tres bordes inferiores de la pantalla),
  con el mismo comportamiento de selección de pestaña que ya tiene.
- El FAB de deshacer queda a una distancia fija y moderada por encima de la barra flotante, sin
  solaparse con ella ni dejar un hueco grande.

**Non-Goals:**
- No se cambia la lógica de selección de pestaña, la pestaña de inicio, ni la lógica de visibilidad o
  comportamiento del FAB de deshacer (ya cubiertas por `app-navigation` y `practice-session`).
- No se introduce una variante de la barra para pantallas medianas/grandes (`ShortNavigationBar`,
  `NavigationRail`): el proyecto solo soporta teléfono en vertical.
- No se añade ninguna dependencia nueva: se reutiliza Material 3, ya presente en el catálogo.

## Decisions

### D1 — Construir la barra flotante con `Surface` + `Row`, no con `NavigationBar`

**Decisión**: `FraseAFraseBottomBar` deja de envolver un `NavigationBar` de Material 3 y pasa a ser un
`Surface` con forma de cápsula completa (`RoundedCornerShape(percent = 50)`, token nuevo
`AppCornerShapes.FloatingNav`; mismo patrón de redondeo total que ya usa el indicador de pestaña
seleccionada en `FraseAFraseBottomBarItem`) y elevación, conteniendo una `Row` con los tres
`FraseAFraseBottomBarItem` (sin cambios en su contenido interno: icono, indicador de píldora,
etiqueta). Referencia visual: imagen de ejemplo de barra flotante tipo cápsula aportada por el
usuario (14/11/2026) — fondo de color sólido, radio total, separada de los tres bordes, con los tres
iconos repartidos en una sola fila. La superficie aplica ella misma
`Modifier.windowInsetsPadding(WindowInsets.navigationBars)` seguido del margen flotante (token nuevo,
16dp), en vez de depender de los insets internos de `NavigationBar` combinados con una altura externa
fija — eso es precisamente lo que causaba el recorte.

**Alternativas consideradas**:
- Mantener `NavigationBar` y solo cambiar `Modifier.height(...)` por `Modifier.heightIn(min = ...)`:
  corrige el recorte pero no permite el margen y la forma redondeada separada de los bordes que pide el
  rediseño (`NavigationBar` siempre ocupa el ancho completo de la ventana). Se descarta porque no
  cumple el pedido de barra flotante.
- Usar `HorizontalFloatingToolbar`: pensado para una fila de acciones con un FAB adjunto
  (`floatingActionButton` obligatorio), no para destinos de navegación con estado seleccionado
  persistente; forzarlo distorsiona su semántica y su spec de medidas. Se descarta.

### D2 — La barra flotante pasa a ser hermana del contenido, no slot `bottomBar` del `Scaffold`

**Decisión**: `FraseAFraseApp` deja de pasar `FraseAFraseBottomBar` como `bottomBar` del `Scaffold` raíz
y la coloca como overlay (`Box` con `Modifier.align(Alignment.BottomCenter)`) sobre el contenido,
igual que `PracticeSessionScreen` ya hace con `RevertLastAnswerButton`. Cada pantalla reserva el hueco
inferior de la barra con `contentPadding`/`Modifier.padding` calculado a partir de la altura de la
barra flotante más su margen (token de `Dimensions`), en vez del `innerPadding` automático de
`Scaffold`.

**Alternativas consideradas**:
- Mantener el slot `bottomBar` del `Scaffold` pasándole la nueva barra flotante: el slot de
  `Scaffold` sigue reservando un rectángulo a sangre completa para `bottomBar` y coloca la barra
  pegada al borde, incompatible con el margen flotante. Se descarta.

### D3 — Nuevo offset del FAB de deshacer relativo a la barra flotante

**Decisión**: `revertFabBottomOffset` se redefine como la distancia entre el borde superior de la
barra flotante (no el borde de la pantalla) y el borde inferior del FAB, con el valor del margen
flotante (16dp, mismo token que D1) como separación — evita tanto el solape que la guía M3 de FAB
prohíbe ("Don't cover the navigation bar with a FAB") como el hueco grande detectado en la revisión
visual. `revertFabHorizontalOffset` (20dp desde la izquierda) no cambia: la alineación a la izquierda
es una decisión de mock ya tomada en `adopt-reference-mock-ui`, ajena a este cambio.

**Alternativas consideradas**:
- Mantener 172dp fijo desde el borde de pantalla: ese valor se calculó para la altura y posición de la
  barra a sangre anterior; con la barra flotante (menor huella vertical total, separada del borde) deja
  de corresponder a ninguna distancia con sentido. Se descarta.

### D4 — Tokens nuevos en `:core:designsystem`, sin literales en `:app` ni en `:feature:session`

**Decisión**: se añaden a `Dimensions` (`core/designsystem/.../tokens/Dimensions.kt`) el token
`navFloatingMargin` (16dp, margen respecto a los tres bordes inferiores y separación con el FAB de
deshacer) y a `AppCornerShapes` (`core/designsystem/.../theme/Shape.kt`) el token `FloatingNav`
(`RoundedCornerShape(percent = 50)`, cápsula completa, D1). `navHeight` se mantiene como alto interno
de la `Row` de destinos (ya no como alto fijo del `Surface`, que ahora crece con los insets).
`revertFabBottomOffset` cambia de valor pero no de nombre ni de tipo.

## Risks / Trade-offs

- **[Riesgo]** Los tests instrumentados que miden el alto exacto de `bottom-nav-bar`
  (`FraseAFraseAppTest`, 80dp ± 1dp) y el offset exacto del FAB (`PracticeSessionRevertButtonScreenTest`,
  172dp desde el borde de pantalla) dejan de ser válidos con la nueva geometría.
  → **Mitigación**: se reescriben en las tareas de este cambio para medir la nueva geometría (margen
  flotante, offset relativo a la barra) en vez de eliminarse, preservando cobertura sobre la posición
  exacta.
- **[Riesgo]** El emulador de CI no tiene una barra de gestos real, así que un test que solo
  comprobara "no hay recorte visible" en ese emulador podría no reproducir el defecto original.
  → **Mitigación**: el emulador sí reporta un inset de `WindowInsets.navigationBars` real y distinto de
  cero (barra de navegación de 3 botones del AVD por defecto, igual en desarrollo y en CI). El nuevo
  escenario de `app-navigation` no simula ningún inset: lee ese inset real con
  `ViewCompat.getRootWindowInsets(activity.window.decorView)` (tipo `Type.navigationBars()`) y comprueba
  que los límites de cada destino (icono + etiqueta) quedan íntegramente por encima de él, igual que ya
  hace el resto de tests instrumentados de la app con `getUnclippedBoundsInRoot()`.
- **[Riesgo]** Mover la barra fuera del slot `bottomBar` del `Scaffold` podría cambiar el orden de
  dibujado/elevación respecto al `SnackbarHost`, que también vive en ese `Scaffold`.
  → **Mitigación**: el `SnackbarHost` no cambia de slot; se verifica manualmente (o con un test de UI)
  que la confirmación de guardado de Ajustes se sigue viendo por encima de la barra flotante tras el
  cambio.
