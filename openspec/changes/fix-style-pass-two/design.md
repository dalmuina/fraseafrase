# Design

## Context

`new-style` fijó paleta, tipografía y `Shapes` en `:core:designsystem/theme/*` y migró
`DsLevelBadge` a los 6 vectores de rango. Comparando las tres pantallas resultantes con los mocks
(`Practicar · traducción oculta.png`, `Practicar · traducción visible.png`, `Progreso.png`,
`Ajustes.png`) se encuentran seis desviaciones puramente de layout/tipografía y una de interacción,
descritas en `proposal.md - What Changes`. Ningún dato mostrado cambia (el texto "Frase X de Y", el
total disponible, el nivel CEFR, la insignia... ya están todos en pantalla); lo que cambia es dónde
y cómo se pintan.

Estado actual relevante:
- `PracticeSessionScreen.kt`: `Scaffold(topBar = { TopAppBar(title = {}) })` sin título; el
  indicador "X de Y" es un `Text` centrado dentro del `Column` del contenido, no una cabecera fija;
  el `Box` raíz usa `contentAlignment = Alignment.Center` y el `Column` interior
  `horizontalAlignment = CenterHorizontally`, por lo que `Button`/`OutlinedButton` se miden a su
  contenido (`wrapContentWidth`) en vez de a `fillMaxWidth()`.
- `ProgressScreen.kt`: la tarjeta destacada es un `Card` con los colores por defecto (`surface`) y
  dos `Text` con un formato cada uno (`progress_total_available_format`,
  `progress_cefr_level_format`), sin usar `primary`/`onPrimary` ni separar "Frases disponibles" del
  número grande.
- `DsLevelBadge.kt`: el `Text(text = spec.label)` no fija `style`, así que hereda `LocalTextStyle`
  (`bodyLarge` por defecto de `Text`), igual que `CefrLevelCard` en `SettingsScreen.kt`
  (`Text(text = level.name)` dentro del `FilterChip`) y el `Text(text = item.cefrLevel.name,
  style = MaterialTheme.typography.labelLarge)` de `PhraseCard`: tres orígenes distintos para lo
  que en el mock es el mismo tamaño de etiqueta pequeña.
- `SettingsScreen.kt` - `BatchSizeCard`: `OutlinedIconButton`/`FilledIconButton` usan su tamaño por
  defecto de Material 3 (40dp) junto a un `OutlinedTextField` cuya altura mínima por defecto es
  mayor (~56dp); `onBatchSizeTextChange(steppedBatchSizeText(...))` no comprueba límites, así que
  el botón "−" siempre está habilitado y puede dejar el texto en "9", "8"... (inválido, pero
  alcanzable).

## Goals / Non-Goals

**Goals:**
- Cabecera fija de Practicar con título, píldora de posición y barra de progreso, coherente con el
  mock, sin tocar la lógica de `PracticeSessionUiState` (todos los datos ya existen: `index`,
  `total`).
- Layout de ancho completo en los controles de Practicar que hoy se centran.
- Tarjeta destacada de Progreso con los roles `primary`/`onPrimary` y la jerarquía visual del mock.
- Un único estilo tipográfico de insignia/etiqueta pequeña, reutilizado por `DsLevelBadge` y por el
  nivel CEFR allí donde se muestra junto a una insignia.
- Botones +/- de Ajustes con la altura del campo de texto y centrados con él, y deshabilitados en
  los límites 10/30.

**Non-Goals:**
- Cambiar los valores de `LightColors`/`DarkColors`, `AppShapes` o las familias tipográficas
  (`Baloo2Family`/`PlusJakartaSansFamily`) fijados por `new-style`: este cambio los reutiliza, no
  los redefine.
- Rediseñar la navegación inferior, el flujo de confirmación de guardado de Ajustes o cualquier
  lógica de dominio.
- Añadir un nuevo componente `Ds*` de barra de progreso si `LinearProgressIndicator` de Material 3,
  con `MaterialTheme.colorScheme.primary`, ya cubre el mock sin personalización adicional.

## Decisions

### D1. Cabecera de Practicar: `Column` fijo fuera del contenido desplazable, con `LinearProgressIndicator`

`PracticeSessionScreen` cambia su `Scaffold` para que el `topBar` pinte el título "Practicar" con
`MaterialTheme.typography.headlineLarge` (ya mapeado a `Baloo2Family` por `new-style`) y, debajo
(dentro del mismo `topBar` o como `Column` fijo justo antes del contenido con estado), la píldora
"Frase X de Y" (`Text` sobre un `Surface`/`Box` con `shape = MaterialTheme.shapes.extraLarge`,
`color = MaterialTheme.colorScheme.primaryContainer`, texto en `onPrimaryContainer`) y un
`LinearProgressIndicator(progress = { index / total.toFloat() })` con
`color = MaterialTheme.colorScheme.primary`. Esta cabecera solo se muestra en el estado
`Practicing` (no en `Loading`, `NothingToPractice` ni `CatalogError`, que mantienen su mensaje
centrado, como en el estado actual): los otros tres estados no tienen posición que mostrar.
Alternativa descartada: mantener el indicador dentro del `Column` de contenido pero con
`Modifier.fillMaxWidth()` - se descarta porque el mock lo fija en la parte superior, fuera del área
que potencialmente se desplaza con tandas largas (hasta 30 frases, aunque hoy el contenido no
necesita scroll).

### D2. Contenido de Practicar a ancho completo

El `Box` raíz dentro del `Scaffold` deja `contentAlignment = Alignment.Center` solo para los tres
estados sin tanda (`Loading`/`NothingToPractice`/`CatalogError`, que siguen centrados como un
mensaje puntual). `PracticingContent` cambia su `Column` a `horizontalAlignment =
Alignment.Start` (o se quita el parámetro, por defecto) y añade `Modifier.fillMaxWidth()`; `Button`,
`OutlinedButton` y la `Row` de `AssessmentButtons` pasan a `Modifier.fillMaxWidth()` /
`Modifier.weight(1f)` respectivamente, igual que ya hace el botón "Guardar" de
`SettingsScreen.kt`. `PhraseCard`/`TranslationCard` ya usan `fillMaxWidth()`: no cambian.

### D3. Tarjeta destacada de Progreso con color primario

`ContentState` en `ProgressScreen.kt` cambia el `Card` de `state.totalAvailable`/`state.cefrLevel`
a `CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary, contentColor =
MaterialTheme.colorScheme.onPrimary)`, y separa el contenido en tres `Text` (etiqueta "Frases
disponibles", número grande con `MaterialTheme.typography.headlineLarge`, línea "Nivel actual:
<nivel>"), sustituyendo los dos `Text` con formato combinado actuales. No cambia `ProgressUiState`
ni las strings ya existentes más que, si hace falta, separar
`progress_total_available_format`/`progress_cefr_level_format` en recursos de texto más simples
(cambio de recursos, no de contrato del ViewModel).

### D4. Estilo de etiqueta pequeña compartido para insignias y nivel CEFR

Se fija `MaterialTheme.typography.labelMedium` (ya mapeado a `PlusJakartaSansFamily` por
`new-style`) como estilo explícito del `Text(text = spec.label)` en `DsLevelBadge`, y se reutiliza
el mismo estilo donde hoy se muestra el nivel CEFR junto a una insignia (`PhraseCard` en
`PracticeSessionScreen.kt`, que pasa de `labelLarge` a `labelMedium`) y donde se muestra dentro de
un chip de nivel (`CefrLevelCard` en `SettingsScreen.kt`, que fija `style = labelMedium` en su
`Text` en vez de heredar el del `FilterChip`). Alternativa descartada: introducir un nuevo token de
tipografía en `AppTheme` (p. ej. `AppTheme.typography.badge`) - se descarta porque
`MaterialTheme.typography.labelMedium` ya existe, ya está mapeado a la familia correcta por
`new-style`, y añadir un token nuevo solo para este caso contradice la regla de reutilizar lo ya
presente en el design system.

### D5. Stepper de N: límites en los botones, tamaño igualado al campo de texto

`BatchSizeCard` en `SettingsScreen.kt` ya recibe `batchSizeText: String` como parámetro propio; a
partir de él deriva internamente el entero actual para calcular `enabled` de cada
`OutlinedIconButton`/
`FilledIconButton`: el de "−" se deshabilita si el valor actual es `null` o `<= BatchSize.MIN`
(10); el de "+" si es `null` o `>= BatchSize.MAX` (30). `steppedBatchSizeText` no cambia (sigue sin
efecto si el texto no es un entero); el límite se aplica solo a si el botón está pulsable, no a la
edición libre del campo de texto, que sigue validándose como hoy. Ambos botones pasan a
`Modifier.size(...)` igual a la altura efectiva del `OutlinedTextField` (altura por defecto de
Material 3, ~56dp) en vez de su tamaño por defecto (40dp), y la `Row` que los contiene ya usa
`verticalAlignment = Alignment.CenterVertically` (sin cambios ahí): al igualar alturas, el centrado
existente ya los alinea con el campo de texto.

## Risks / Trade-offs

- [`LinearProgressIndicator` de Material 3 no admite fácilmente esquinas muy redondeadas como el
  resto de componentes (`AppShapes`)] → Se acepta su forma por defecto (recta o con el redondeo que
  traiga la versión de Material3 ya en el catálogo); no es un requisito de ninguna spec y el mock
  no exige un radio exacto.
- [Separar la tarjeta de Progreso en tres `Text` puede requerir nuevas claves de `strings.xml` en
  vez de reutilizar las combinadas] → Es un cambio de recursos interno a `:feature:progress`, sin
  impacto en `ProgressUiState` ni en ninguna spec.
- [Igualar el tamaño de los botones +/- al campo de texto es una medida aproximada (no hay un
  token compartido de altura entre `IconButton` y `TextField` en Material 3)] → Se fija el tamaño
  de los botones en implementación para que coincida visualmente con la altura real del
  `OutlinedTextField` renderizado; una revisión manual en la vista previa/emulador confirma el
  resultado, como ya se hace para el resto de detalles de `new-style`.

## Migration Plan

No aplica dato persistido ni migración de base de datos: cambio de presentación e interacción de
un control existente, sin tocar `UserSettings`, `ProgressUiState` más allá de sus recursos de
texto, ni `PracticeSessionUiState`.
