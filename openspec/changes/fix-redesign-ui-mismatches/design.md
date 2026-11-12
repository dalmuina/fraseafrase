# Design

## Context

`redesign-app-ui` dejó 7 incongruencias visuales y una de comportamiento respecto al mock de
diseño (ver `proposal.md`). El mock define 5 pantallas (`Main`, `AudioCompacto`, `Progreso`,
`Ajustes`, `SesionCompletada`); los colores que usa para las cajas e iconos a corregir
(`#FFF1EA`, `#E3F6EC`, `#FFECEE`) coinciden exactamente con los roles ya definidos en
`core/designsystem/theme/Color.kt` (`secondaryContainer`, `tertiaryContainer`, `errorContainer`),
así que no se necesita ningún color nuevo. No hay un componente `Ds` compartido hoy para una caja
con icono y texto parcialmente en negrita, ni para una tarjeta destacada con fondo de color y
decoración.

## Goals / Non-Goals

**Goals:**
- Igualar los 7 puntos de `proposal.md` al mock, reutilizando roles de color y espaciado ya
  existentes.
- Promocionar a `:core:designsystem` los patrones visuales que ya se repiten o que empiezan a
  repetirse entre features (caja con icono, tarjeta destacada, altura de control primario), en vez
  de duplicarlos con valores literales en cada pantalla.

**Non-Goals:**
- No se añade ningún color, ni modo oscuro nuevo (los componentes nuevos usan `LightColors`/
  `DarkColors` ya existentes).
- No se toca ninguna regla de `practice-batch` o `spaced-repetition`; el único cambio de
  comportamiento (ocultar el botón de deshacer al revelar) es puramente de presentación del
  estado ya existente, no de las reglas de racha o nivel.
- No se sustituye `DsStatTile` donde ya funciona bien (solo gana un parámetro de color).

## Decisions

**D1 — Reutilizar roles de color existentes, sin tokens nuevos.** Los colores del mock para la
caja "te faltan N aciertos" y los tres chips de resultado ya existen en el tema
(`secondaryContainer`/`onSecondaryContainer` para aviso y "sube de rango",
`tertiaryContainer`/`onTertiaryContainer` para aciertos, `errorContainer`/`onErrorContainer` para
fallos). Alternativa descartada: definir colores literales en las features, prohibido por
convención del proyecto (sin colores literales fuera de `:core:designsystem`).

**D2 — Nuevo componente `DsInfoBanner` en `:core:designsystem`.** Icono + texto con tramos en
negrita (vía parámetros de texto normal/negrita, no `AnnotatedString` libre, para mantenerlo sin
tipos del dominio), fondo de color configurable por parámetro de rol de color. Sustituye el
`Text` suelto de "te faltan N aciertos para subir a X" en `PhraseCard`. Se crea en
`:core:designsystem` directamente, no en la feature, porque el patrón "pastilla/caja con icono"
ya se repite ad-hoc en `CefrLevelPill`/`ListenPill` de `PracticeSessionPhraseCard.kt`: nace
compartido en vez de duplicarse una tercera vez.

**D3 — Nuevo componente `DsHeroStat` en `:core:designsystem`.** Tarjeta de ancho completo con
fondo de color, un valor grande, una etiqueta y una línea secundaria, todos alineados a la
izquierda, con espacio para un icono decorativo detrás del texto. Sustituye el `DsStatTile`
(`Large`) + `Text` suelto de la cabecera de `ProgressScreen`. No sustituye a `DsStatTile`, que
sigue usándose para los chips compactos de resultado (D4).

**D4 — `DsStatTile` gana un parámetro de rol de color, y sus usos en fila igualan altura.** Se
añade un parámetro (p. ej. `containerColor`/`contentColor`, con el valor por defecto actual de
`primaryContainer`/`onPrimaryContainer` para no romper otros usos) para que
`CompletedMessage` pueda pasar `tertiaryContainer` (aciertos), `errorContainer` (fallos) y
`secondaryContainer` (sube de rango). La fila que las contiene pasa a medirse con
`Modifier.height(IntrinsicSize.Max)` y cada `DsStatTile` a `fillMaxHeight()`, para que las tres
columnas midan lo mismo aunque su etiqueta ocupe una o dos líneas. Alternativa descartada: crear
una variante `DsOutcomeTile` nueva — se descarta porque solo cambiaría el color, no la estructura,
y duplicaría el componente.

**D5 — Control de escuchar como icono simple.** `ListenPill` (icono + texto en una `Surface`
pulsable) se sustituye por un `IconButton`/`FilledIconButton` con un único icono de altavoz
(`Icons.Default.VolumeUp`, ya en `material-icons-core`), manteniendo las condiciones de
visibilidad (`revealed`) y habilitación (`enabled`) actuales y añadiendo `contentDescription`
como equivalente accesible del texto que deja de mostrarse (delta de `phrase-audio`).

**D6 — Botón de deshacer: icono, posición y ocultación al revelar.** Se cambia
`Icons.AutoMirrored.Filled.ArrowBack` por un icono de recarga/deshacer
(`Icons.AutoMirrored.Filled.Undo`, ya en `material-icons-extended`, declarada en el catálogo).
Para que quede apilado encima del botón principal en vez de superpuesto, deja el slot
`floatingActionButton` del `Scaffold` (pensado para flotar sobre el contenido, no para reservar su
propio espacio) y pasa a pintarse dentro de la misma `Column` que los botones inferiores, antes del
botón principal, con su propio espaciado (`AppTheme.spacing`). `PracticeSessionViewModel.reveal()`
añade `canRevertLastAnswer = false` a la copia del estado que ya produce (delta de
`practice-session`), para que la regla de visibilidad viva en el `UiState` (ADR-0006) y no en una
condición ad-hoc de la pantalla. El slot `floatingActionButton` del `Scaffold` anima su
aparición/desaparición automáticamente; al sacar el botón de ese slot hay que añadir explícitamente
una animación equivalente (p. ej. `AnimatedVisibility`) en su nueva ubicación, para no perder esa
transición (revisión `android-architect`).

**D7 — Cabeceras de Progreso y Ajustes sin `TopAppBar`.** Se elimina el `Scaffold(topBar = {
TopAppBar(...) })` de ambas pantallas y se sustituye por el mismo patrón que ya usa
`PracticeHeader` en `PracticeSessionScreen.kt`: un `Text` con `MaterialTheme.typography.headlineLarge`
dentro de la `Column` de contenido, con el mismo fondo que el resto de la pantalla. No se promueve
`PracticeHeader` entero a `:core:designsystem` porque también pinta la posición en la tanda y la
barra de progreso, específicas de Practicar; solo se replica el patrón de título.

**D8 — Altura de controles primarios como token nuevo de `:core:designsystem`.** Hoy
`SettingsScreen.kt` ya fija `TEXT_FIELD_HEIGHT = 56.dp` como constante privada, con un comentario
explicando que no existe un token compartido. Los botones del punto 1 (revelar, fallo, acierto,
generar sesión, guardar) deben medir 56dp como en el mock: se añade un token de altura de control
primario a `AppTheme` (p. ej. `AppTheme.dimensions.primaryActionHeight`) y los cinco botones lo
usan vía `Modifier.height(...)`, en vez de repetir `56.dp` como literal en cada feature. El botón
de aumentar/disminuir N de Ajustes puede migrar al mismo token en un cambio posterior si conviene;
no es parte de este cambio.

## Risks / Trade-offs

- [Cambiar `DsStatTile` con un parámetro de color opcional] → mantiene compatibilidad con los usos
  existentes (valor por defecto igual al actual); se verifica con los tests ya existentes de
  `DsStatTileTest` más los nuevos casos de color.
- [Mover el botón de deshacer fuera del slot `floatingActionButton`] → pierde la elevación/sombra
  por defecto de `FloatingActionButton`; se mitiga dándole la misma forma y color mediante un
  `Surface`/`IconButton` con elevación equivalente, verificado visualmente contra el mock.
- [Ocultar el botón de deshacer al revelar reduce la ventana en la que el usuario puede deshacer]
  → es el comportamiento pedido explícitamente por el usuario; se documenta en el delta de
  `practice-session` para que quede claro que es intencional, no una regresión.
