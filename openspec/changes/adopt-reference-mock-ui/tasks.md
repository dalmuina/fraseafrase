# Tasks

## 1. Mock en el repositorio

- [ ] 1.1 Copiar `C:\Users\dalmu\OneDrive\Documentos\mock-ref\mock-ref` a `design/mock/` en el repo
  (README, `design-tokens.md`, `screens/`, `reference-png/`, `icons/svg/`, `icons/android-drawable/`,
  `compose/DesignTokens.kt`) sin modificar su contenido, y verificar que los 5 HTML abren igual en un
  navegador desde su nueva ruta.

## 2. Tokens de color, forma, dimensión y tipografía (`:core:designsystem`)

- [ ] 2.1 Añadir un test de `AppTheme`/tokens (unitario, sin Robolectric si es posible) que compruebe
  que cada color nuevo listado en `design.md` (decisión 2) existe con el valor hexadecimal exacto de
  `design/mock/design-tokens.md`; verificar que el test falla por los colores ausentes antes de
  implementarlos.
- [ ] 2.2 Añadir en `Color.kt` los tokens que faltan (`accentStrong`, `accentSoft`, `textSecondary`
  unificado a `#5A5670`, `textOnLevel`, `track`, `trackSoft`, `dashedBorder`, `disabled`, `hintBg`,
  `hintText`, `rankUpText`, `fabContent`, los 3 tonos de éxito y de error) y corregir `background` a
  `#F7F5FF`; verificar que el test de 2.1 pasa.
- [ ] 2.3 Añadir en `Shape.kt` los radios que faltan (panel/zona discontinua 20dp, FAB 18dp, 14dp para
  mensaje de rango/nivel/stepper/campo) y en `tokens/Dimensions.kt` las dimensiones del mock (nav 80dp,
  indicador 64×32dp, iconos de nav 22dp, icono de botón 22dp + gap 10dp, FAB 56dp + offset 20/172dp +
  icono 24dp, botón Escuchar 44dp + icono 22dp, badge de chip 18dp, icono de mensaje de rango 24/22dp,
  controles de ajustes 52dp, icono de fila de rango 40dp, barra de rango 6dp); verificar con un test de
  tokens que cada constante nueva tiene el valor exacto documentado. Sin escenario de spec propio: son
  tokens de forma/dimensión puramente visuales, sin comportamiento observable distinto del ya cubierto
  por las specs de pantalla existentes.
- [ ] 2.4 Empaquetar las fuentes Space Grotesk e Inter (400/500/600/700) como `.ttf` en
  `core/designsystem/src/main/res/font/` y actualizar `Type.kt` con los tamaños/pesos/tracking exactos
  de `design-tokens.md` (ver design.md decisión 1); verificar con una previsualización (`@PreviewLightDark`)
  que el texto de ejemplo usa la fuente correcta. Sin escenario de spec propio: cambio de tipografía
  puramente visual.

## 3. Iconografía (`:core:designsystem`)

- [ ] 3.1 Copiar los `VectorDrawable` de `design/mock/icons/android-drawable/` a
  `core/designsystem/src/main/res/drawable/` con el mismo nombre de archivo; verificar que el proyecto
  compila y que no hay recursos duplicados.
- [ ] 3.2 Añadir un test de `DsLevelBadge` que compruebe, para cada uno de los 6 niveles (Sin
  rango…Diamante), el color exterior/interior exacto de `design-tokens.md`; verificar que el test falla
  para cualquier nivel cuyo color actual no coincida antes de corregirlo.
- [ ] 3.3 Ajustar `DsLevelBadge` para que los 6 niveles usen los colores exactos del mock, implementando
  "Sin rango" con un `Canvas`/`Stroke(pathEffect = dashPathEffect(...))` en vez de un `VectorDrawable`
  (no admite trazo discontinuo); verificar que el test de 3.2 pasa.

## 4. Practicar · traducción oculta (`:feature:session`)

- [ ] 4.1 Añadir un escenario de test (Compose UI Test o de `PracticeSessionUiState`, según dónde viva
  la lógica de visibilidad) que compruebe que el mensaje de ayuda "Traduce la frase mentalmente y luego
  comprueba la traducción" se muestra cuando la frase no está revelada y desaparece al revelarla
  (escenarios de `openspec/changes/adopt-reference-mock-ui/specs/practice-session/spec.md`); verificar
  que el test falla porque el mensaje no existe todavía.
- [ ] 4.2 Implementar el cuadro de borde discontinuo con ese mensaje como composable privado de
  `PracticeSessionScreen.kt` (`AppColors.DashedBorder`, radio `AppShapes.Panel` de 20dp, ver design.md
  decisión 5); verificar que el test de 4.1 pasa.
- [ ] 4.3 Ajustar la cabecera de práctica (paddings 24/20/12, gap 14, barra 8dp, píldora del contador en
  `accentSoft`/`accentStrong`) y verificar visualmente contra `design/mock/reference-png/01-practicar-oculta.png`.
  Sin escenario de spec propio: el contador "X de Y" y la barra ya están cubiertos por "Posición en la
  tanda visible" en `practice-session`; este ajuste es solo de color/espaciado.
- [ ] 4.4 Corregir el icono del `DsInfoBanner` de "Te faltan N aciertos para X" para que muestre la
  insignia del nivel siguiente (ver design.md decisión 8); añadir o ajustar el test que verifica qué
  insignia se pasa como icono, comprobando que falla antes de la corrección. Escenario de referencia:
  "Aciertos para el próximo nivel visibles" en `practice-session`.
- [ ] 4.5 Sustituir el icono del botón "Mostrar traducción" (`Icons.Filled.Search`) por `ic_eye`;
  verificar que no queda ningún import de Material Icons en este archivo tras el cambio. Sin escenario de
  spec propio: cambio puramente de icono, sin alterar el comportamiento de "Revelar muestra la traducción
  en portugués".

## 5. Practicar · traducción revelada (`:feature:session`)

- [ ] 5.1 Ajustar el panel de traducción (`TranslationCard`): fondo `accentSoft`, radio 20dp, etiqueta
  "TRADUCCIÓN" en mayúsculas (12sp/700/tracking +0.8/`accentStrong`); verificar contra
  `design/mock/reference-png/02-practicar-traduccion.png`. Escenario de referencia: "Revelar muestra la
  traducción en portugués" en `practice-session`; este ajuste no cambia qué texto se muestra, solo su
  presentación.
- [ ] 5.2 Sustituir los iconos de los botones Fallo/Acierto (`Icons.Filled.Close`/`Check`) por
  `ic_close`/`ic_check` y ajustar sus colores (borde 2dp, fondos `errorBg`/`success`) a los valores
  exactos del mock; verificar que el test de colores existente (o uno nuevo) pasa.
- [ ] 5.3 Verificar que el botón Escuchar ya es circular 44dp con icono único; si no, ajustarlo a
  `ic_volume` con fondo `accent` y sombra, y documentar el resultado en esta tarea. Escenario de
  referencia: los de `phrase-audio` sobre el control icono-solo; este ajuste solo confirma/corrige
  dimensión y color, no la condición de disponibilidad del control.

## 6. Botón flotante de revertir (`:feature:session`)

- [ ] 6.1 Sustituir el icono (`Icons.Filled.Undo`) por `ic_undo_previous`, ajustar el color de contenido
  a `fabContent` (`#2B0E02`), la posición (left 20dp, bottom 172dp) y el radio (18dp); verificar contra
  `design/mock/reference-png/01-practicar-oculta.png` (el FAB es visible en esa pantalla tras la primera
  autoevaluación) sin alterar ninguno de los escenarios ya cubiertos por la spec de "Botón flotante para
  revertir la última autoevaluación" (`practice-session`), ejecutando sus tests existentes. Sin
  escenario de spec propio adicional: solo cambia icono/color/posición, no la lógica de visibilidad ya
  cubierta por esos tests.

## 7. Sesión completada (`:feature:session`)

- [ ] 7.1 Añadir un test de Compose UI (o snapshot si existiera infraestructura; en su defecto, un test
  de presencia del composable) que compruebe que la insignia compuesta de 4 capas se muestra en el
  mensaje de tanda completada; verificar que falla porque hoy solo hay un icono simple.
- [ ] 7.2 Implementar la insignia compuesta (hexágono exterior 128dp `accentSoft`, hexágono interior
  92dp `accent`, check blanco 44dp con `ic_check_complete`, mini-hexágono decorativo 28dp `fab` con
  `ic_hex_solid`) como composable privado de `PracticeSessionScreen.kt`, sustituyendo el icono
  `CheckCircle`; verificar que el test de 7.1 pasa y que el desglose de `DsStatTile` sigue mostrando los
  colores correctos (`successBg`/`errorBg`/`hintBg`+`rankUpText`).
- [ ] 7.3 Sustituir el icono de "Generar sesión nueva" (`Icons.Filled.Refresh`) por `ic_new_session`;
  verificar que no queda ningún import de Material Icons en este archivo. Sin escenario de spec propio:
  cambio puramente de icono sobre el botón de "Mensaje de tanda completada y generación de sesión".

## 8. Progreso

- [ ] 8.1 Localizar el composable de la pantalla de Progreso y verificar que `DsHeroStat` usa su slot
  `decoration` para los hexágonos de fondo con opacidad del mock; si no, implementarlo, y verificar
  contra `design/mock/reference-png/04-progreso.png`. Escenario de referencia: la tarjeta destacada con
  total disponible y nivel CEFR de `progress-overview`; este ajuste no cambia qué dato se muestra
  (`value`/`label`/`secondaryLine`), solo su decoración.
- [ ] 8.2 Ajustar cada fila de rango (icono 40dp, barra `DsProgressBar` 6dp con el color exacto del
  rango, cifra alineada a la derecha 18sp/700) a los valores del mock; verificar con un test existente
  de la pantalla de Progreso que las filas siguen mostrando el recuento correcto tras el cambio visual.
  Escenario de referencia: recuento por nivel de `progress-overview` y "insignias visualmente
  distinguibles" de `level-badges`; el dato mostrado no cambia, solo su presentación.

## 9. Ajustes (`:feature:settings`)

- [ ] 9.1 Añadir un test de Compose UI que compruebe que el selector de nivel CEFR expone 5 botones
  seleccionables con semántica de grupo de radio (en vez de `SegmentedButtonRow`); verificar que falla
  mientras exista el segmented control.
- [ ] 9.2 Sustituir `SingleChoiceSegmentedButtonRow`/`SegmentedButton` por una rejilla de botones propios
  (52dp alto, radio 14dp, borde 2dp `track`, seleccionado con fondo/borde `accent` y texto blanco, con
  `Modifier.selectableGroup()`/`Role.RadioButton`), manteniendo `onAction(SelectCefrLevel)` sin cambios;
  verificar que el test de 9.1 pasa y que los tests existentes de selección de nivel siguen en verde.
- [ ] 9.3 Ajustar el stepper +/- a 52dp con los colores exactos (deshabilitado `trackSoft`/`disabled`,
  habilitado `accentSoft`/`accentStrong`) y sustituir el icono de "Guardar" (`Icons.Filled.Check`) por
  `ic_check_save`; verificar contra `design/mock/reference-png/05-ajustes.png`. Escenario de referencia:
  "Edición de N con validación y stepper +/- deshabilitado en límites" de `settings-screen`; este ajuste
  no cambia los límites ni la validación, solo color/tamaño/icono.
- [ ] 9.4 Verificar el color exacto (`successStatus` `#1B6B44`) del mensaje "Ajustes guardados" con el
  test existente de confirmación de guardado, ajustándolo si no coincide. Escenario de referencia:
  "confirmación visual al guardar" de `settings-screen`; el test existente ya prueba que el mensaje se
  muestra, aquí solo se verifica/corrige su color exacto.

## 10. Barra de navegación inferior (`:app`)

- [ ] 10.1 Añadir un test de Compose UI sobre `FraseAFraseApp.kt` que compruebe la altura (80dp), el
  tamaño del indicador de tab activo (64×32dp) y que la etiqueta inactiva usa un único color
  `textSecondary` (`#5A5670`) en las 3 pestañas; verificar que falla con la configuración actual de
  `NavigationBar`/`NavigationBarItem` por defecto.
- [ ] 10.2 Sobreescribir los colores/formas/alturas de `NavigationBar`/`NavigationBarItem` según
  design.md decisión 7 y sustituir los iconos por `ic_nav_practicar`/`ic_nav_progreso`/`ic_nav_ajustes`;
  verificar que el test de 10.1 pasa en las 5 pantallas que incluyen la barra.

## 11. Verificación final

- [ ] 11.1 Ejecutar `./gradlew check` (tests + detekt + lint) en verde.
- [ ] 11.2 Ejecutar `./gradlew koverVerify` y confirmar que se mantienen los mínimos de cobertura
  (dominio 90 %, presentación 80 %, global 70 %).
- [ ] 11.3 Comparar manualmente cada una de las 5 pantallas contra su PNG de referencia en
  `design/mock/reference-png/` (no hay infraestructura de tests de captura en el proyecto; ver design.md
  - Non-Goals) y anotar en esta tarea cualquier discrepancia residual encontrada.

## Workflow follow-up

- Revisar con `android-architect` el diseño antes de implementar (si no se ha hecho ya en esta sesión).
- Revisar el código con `code-reviewer` al terminar la implementación.
- Ejecutar `/opsx:verify` antes de `/opsx:archive`.
- Archivar el cambio con `/opsx:archive` en la rama del cambio, antes de abrir la PR.
