# Design

## Context

El mock de referencia vive hoy en `C:\Users\dalmu\OneDrive\Documentos\mock-ref\mock-ref` (fuera del
repo). Su propio `README.md` recomienda copiarlo a `design/mock/` dentro del proyecto para que sea la
fuente de verdad citable; este cambio lo copia tal cual (HTML de pantallas, PNG de referencia,
`design-tokens.md`, `icons/svg`, `icons/android-drawable`, `compose/DesignTokens.kt`) sin modificar su
contenido.

El estado actual (ver proposal.md - Why) ya tiene identidad visual propia: paleta índigo `#5B3DF5` en
`core/designsystem/.../theme/Color.kt`, tipografía Baloo 2 + Plus Jakarta Sans empaquetada como `.ttf`,
formas en `Shape.kt` (8/12/16/24dp + pill), `Dimensions.kt` con solo `primaryActionHeight = 56dp`, y los
componentes `Ds*`: `DsHeroStat`, `DsInfoBanner`/`DsInfoBannerSegment`, `DsLevelBadge`/
`DsLevelBadgeChange`, `DsProgressBar`, `DsStatTile`. La barra de navegación inferior usa
`NavigationBar`/`NavigationBarItem` de Material 3 sin overrides, en `app/.../ui/FraseAFraseApp.kt`. No
hay infraestructura de tests de captura (Roborazzi/Paparazzi) en el proyecto.

## Goals / Non-Goals

**Goals:**
- Que cada token (color, radio, dimensión, tipografía) y cada icono de las 5 pantallas del mock tenga un
  equivalente exacto en `:core:designsystem`, verificable contra `design/mock/design-tokens.md`.
- Reutilizar y ajustar los componentes `Ds*` existentes en vez de recrearlos cuando ya cubren la misma
  estructura (`DsHeroStat`, `DsInfoBanner`, `DsLevelBadge`, `DsProgressBar`, `DsStatTile`).
- Eliminar todo uso de `Icons.Filled`/`Icons.Outlined` (Material Icons) en las pantallas cubiertas por el
  mock, sustituyéndolos por los `VectorDrawable` de `design/mock/icons/android-drawable/`.

**Non-Goals:**
- No se añade infraestructura de tests de captura (Roborazzi/Paparazzi): el proyecto no la tiene hoy y
  añadirla es una decisión de tooling independiente, fuera del alcance de este cambio visual.
- No se introduce un tema oscuro específico del mock (no existe); el esquema oscuro se sigue derivando
  automáticamente del claro.
- No se cambia el modelo de dominio, casos de uso ni persistencia (ADR-0012, ADR-0013, ADR-0015 a 0018).

## Decisions

### 1. Tipografía: fuentes locales, no Google Fonts por red

El mock especifica Space Grotesk + Inter vía Google Fonts (apto para un HTML de referencia). El proyecto
es offline-first: ADR-0013 mantiene explícitamente la cláusula del ADR-0004 al que sustituye ("la app
funciona completamente sin conexión"), no es su propio contenido de persistencia el que aplica aquí.
Ya se empaqueta Baloo 2 + Plus Jakarta Sans como `.ttf` en
`core/designsystem/src/main/res/font/`. **Decisión**: descargar una sola vez los `.ttf` de Space Grotesk
(400/500/600/700) e Inter (400/500/600/700) y empaquetarlos igual que las fuentes actuales, sustituyendo
sus referencias en `Type.kt`. Alternativa descartada: mantener Baloo 2/Plus Jakarta Sans aproximando los
tamaños del mock — se descarta porque el mock pide explícitamente no sustituir valores por defecto y la
tipografía es uno de los elementos más reconocibles visualmente.

### 2. Tokens nuevos en objetos separados, no ampliar `Color`/`Shape`/`Dimensions` sin estructura

El archivo de referencia `compose/DesignTokens.kt` del mock organiza los tokens en `AppColors`,
`AppShapes`, `AppDimens`, `AppType` y un enum `Rank`. **Decisión**: fusionar esos valores dentro de los
objetos ya existentes del proyecto (`Color.kt`, `Shape.kt`, `tokens/Dimensions.kt`, `Type.kt`) en vez de
copiar el archivo del mock tal cual, para no duplicar la fuente de verdad de tokens ni crear un segundo
sistema de theming paralelo al de `AppTheme`. El enum `Rank` del mock (con sus colores exterior/interior
y, donde aplica, texto/fondo de chip) se añade como tabla de datos usada por `DsLevelBadge`, sustituyendo
o completando los vectores de insignia ya existentes si sus colores no coinciden exactamente con
`design-tokens.md` — verificar color por color en tareas, no asumir que ya coinciden solo porque el
orden de niveles (`SinRango`…`Diamante`) ya es correcto.
El círculo discontinuo de "Sin rango" se implementa con un `Canvas` + `Stroke(pathEffect =
PathEffect.dashPathEffect(...))` dentro de `DsLevelBadge` (equivalente a la función `SinRangoBadge` del
mock), porque un `VectorDrawable` no admite trazo discontinuo nativo.

### 3. Iconos: `VectorDrawable` importados, no `ImageVector` dibujados a mano

Los SVG del mock ya vienen convertidos a `VectorDrawable` en
`design/mock/icons/android-drawable/*.xml`. **Decisión**: copiarlos tal cual a
`core/designsystem/src/main/res/drawable/` (mismo nombre de archivo) y referenciarlos con
`ImageVector.vectorResource(R.drawable.ic_xxx)`, en vez de reescribirlos como `ImageVector` en Kotlin.
Los iconos de trazo (monocromos) se pintan con `Icon(tint = ...)`; las insignias de rango (multicolor) se
pintan con `Image()` o `Icon(tint = Color.Unspecified)`, nunca con el tint por defecto.

### 4. Selector de nivel CEFR: botones propios, no `SegmentedButtonRow`

`SingleChoiceSegmentedButtonRow`/`SegmentedButton` de Material 3 fuerza una forma de píldora conectada
que no coincide con la rejilla de 5 botones independientes (52dp, radio 14dp) del mock. **Decisión**:
sustituir por una `Row` de botones propios (`Button`/`OutlinedButton` con `shape = AppShapes.Hint` y
colores condicionales), igual que ya se hizo para el stepper de Ajustes. Esto es un cambio de widget
visual, no de comportamiento: sigue siendo una selección única A1-C1 con `onAction(SelectCefrLevel)`.

### 5. Cuadro de ayuda discontinuo: nuevo composable en `:feature:session`

Nace como composable privado de `PracticeSessionScreen.kt` (no hay una segunda feature que lo necesite
todavía, así que no se promueve a `:core:designsystem` por ahora, siguiendo la regla de promoción del
proyecto). Usa `Modifier.border(BorderStroke(2.dp, AppColors.DashedBorder), AppShapes.Panel)` con un
`drawBehind`/`Canvas` para el trazo discontinuo, visible solo cuando `uiState is Practicing &&
!revealed`. Alternativa descartada: generalizar `DsInfoBanner` para este caso — se descarta porque
`DsInfoBanner` no admite hoy un borde discontinuo sin icono y, como el cuadro de ayuda, solo lo usa
`:feature:session`, así que no hay hoy una segunda feature que justifique esa generalización.

### 6. Insignia compuesta de "sesión completada": nuevo composable en `:feature:session`

Igual que el punto 5: composición de 4 capas (hexágono exterior, hexágono interior, check, mini-hexágono
decorativo) como composable privado de `PracticeSessionScreen.kt`, usando `ic_hex_solid` y `ic_check_complete`
de `design/mock/icons/android-drawable/` con los tamaños y offsets del mock.

### 7. Barra de navegación inferior: reemplazar `NavigationBar`/`NavigationBarItem` por defecto

Hoy usa los valores por defecto de Material 3 en `app/.../ui/FraseAFraseApp.kt`. **Decisión**: mantener
`NavigationBar` de Material 3 pero sobreescribir explícitamente altura (80dp), colores del indicador
(`accentSoft`, 64×32dp), iconos (22dp, `ic_nav_*`) y color/peso de etiqueta (activa `textPrimary`/700,
inactiva `textSecondary` único valor `#5A5670` — se resuelve ahí mismo la inconsistencia del propio mock
entre `#5A5670` y `#5A6070`, documentada en su README), en vez de construir una barra de navegación desde
cero. Si Material 3 no permite igualar algún valor exacto (p. ej. la forma del indicador), se sustituye
por un componente propio solo en esa pieza, no en toda la barra.

### 8. Icono del mensaje "Te faltan N aciertos para X"

El mock requiere que el icono sea la insignia del **nivel siguiente**, no un icono genérico. Verificar en
tareas si `DsInfoBanner`/`PhraseCard` ya pasa `DsLevelBadge(nextLevel)` como icono; si pasa un icono
distinto, corregirlo como parte de este cambio (no es una capacidad nueva, es alinear la implementación
con el requisito ya existente de `practice-session` sobre el contador de aciertos para el próximo nivel).

## Risks / Trade-offs

- [Cambiar de tipografía puede desajustar alturas de línea ya ajustadas para Baloo 2/Plus Jakarta Sans en
  otras pantallas no cubiertas por el mock, si las hubiera] → Revisar cualquier pantalla fuera de las 5
  del mock tras el cambio de `Type.kt`; si existe alguna, documentarla como seguimiento, no romperla en
  silencio.
- [Sustituir Material Icons por `VectorDrawable` puede dejar íconos huérfanos sin usar en el código si se
  olvida limpiar algún import] → `detekt`/compilación fallan con imports no usados; `./gradlew check`
  antes de cada commit lo detecta.
- [Los colores de `DsLevelBadge` ya existentes pueden no coincidir exactamente con
  `design-tokens.md` aunque el orden de niveles ya sea correcto] → Verificar color por color contra la
  tabla de rangos del mock como primera tarea de icono, antes de dar por bueno el componente existente.
- [La rejilla de botones de nivel no es un componente de Material 3 estándar, lo que puede perder
  accesibilidad (semántica de grupo de radio) que `SegmentedButtonRow` ya daba gratis] → Mantener
  `Modifier.selectableGroup()`/`Role.RadioButton` semántico en los botones propios, verificado con un
  test de Compose UI Test de accesibilidad igual que el resto de controles existentes.

## Migration Plan

No aplica despliegue ni rollback de datos: es un cambio puramente de presentación sobre una app sin
backend. El "despliegue" es el propio flujo de PR a `develop` del proyecto. Si una pantalla queda a medio
migrar al final de una tanda de tareas, no debe mezclarse con la navegación real hasta que
`./gradlew check` esté en verde para ese grupo de tareas.
