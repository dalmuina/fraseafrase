# Tasks

## 1. Design system: token de altura y color de `DsStatTile`

- [x] 1.1 Test (`AppThemeTest` o similar en `:core:designsystem`): un nuevo token
  `AppTheme.dimensions.primaryActionHeight` existe y vale 56.dp (design D8). Verificación: el test
  falla porque el token no existe, luego pasa al añadirlo.
- [x] 1.2 Añadir `AppTheme.dimensions.primaryActionHeight` (y la clase `Dimensions`/`Dimens` que lo
  contenga) en `:core:designsystem`, análogo a `AppTheme.spacing`. Verificación: test de 1.1 en
  verde, `./gradlew :core:designsystem:testDebugUnitTest`.
- [x] 1.3 Test (`DsStatTileTest`): `DsStatTile` acepta un color de contenedor/contenido opcional y,
  sin indicarlo, sigue usando `primaryContainer`/`onPrimaryContainer` (compatibilidad); indicando
  `tertiaryContainer`, el `Surface` resultante usa ese color (design D4). Verificación: el test
  falla antes de añadir el parámetro.
- [x] 1.4 Añadir el parámetro de color a `DsStatTile` con el valor por defecto actual. Verificación:
  tests de 1.3 en verde, `./gradlew :core:designsystem:testDebugUnitTest` y `connectedAndroidTest`
  de `DsStatTileTest` en verde.

## 2. Design system: `DsInfoBanner`

- [x] 2.1 Test (`DsInfoBannerTest`, `:core:designsystem`): dado un icono, un texto con tramos en
  negrita y un color de contenedor, el componente pinta el icono, el texto completo y respeta el
  color de fondo indicado. Verificación: el test falla porque `DsInfoBanner` no existe todavía.
- [x] 2.2 Implementar `DsInfoBanner` en `:core:designsystem` (design D2): icono + texto con
  parámetros de tramo normal/negrita, sin tipos del dominio, `modifier: Modifier = Modifier` primero,
  con `@PreviewLightDark`. Verificación: test de 2.1 en verde.

## 3. Design system: `DsHeroStat`

- [x] 3.1 Test (`DsHeroStatTest`, `:core:designsystem`): dados un valor, una etiqueta, una línea
  secundaria y un color de fondo, el componente los pinta alineados a la izquierda con ese fondo.
  Verificación: el test falla porque `DsHeroStat` no existe todavía.
- [x] 3.2 Implementar `DsHeroStat` en `:core:designsystem` (design D3): tarjeta de ancho completo,
  fondo de color configurable, slot para icono(s) decorativo(s) detrás del texto, contenido
  alineado a la izquierda. Verificación: test de 3.1 en verde.

## 4. Practicar: caja de "aciertos para el siguiente nivel" (punto 2)

- [x] 4.1 Test (`PracticeSessionScreenTest`, `androidTest`): el mensaje "Te faltan N aciertos para
  subir a X" se muestra dentro de un nodo con el icono de insignia visible (spec:
  `practice-session`, "Aciertos para el próximo nivel visibles"). Verificación: el test falla con
  la implementación actual (`Text` suelto sin icono).
- [x] 4.2 Sustituir el `Text` de `PhraseCard` (líneas 65-72 de `PracticeSessionPhraseCard.kt`) por
  `DsInfoBanner`, con el icono de insignia del siguiente nivel y el número de aciertos/nombre de
  insignia en negrita, color `secondaryContainer`. Verificación: test de 4.1 en verde,
  `./gradlew :feature:session:testDebugUnitTest`.

## 5. Practicar: control de escuchar como icono simple (punto 3, delta `phrase-audio`)

- [x] 5.1 Test (`PracticeSessionScreenTest`, `androidTest`): el control de escuchar no muestra
  ningún texto visible y expone `contentDescription` con el texto equivalente (spec: `phrase-audio`,
  "El control de escuchar muestra un icono de audio"). Verificación: el test falla con
  `ListenPill` actual (icono + texto visible).
- [x] 5.2 Sustituir `ListenPill` por un `IconButton`/`FilledIconButton` de icono simple
  (`Icons.Default.VolumeUp`) en `PracticeSessionPhraseCard.kt`, manteniendo `revealed`/`enabled` y
  añadiendo `contentDescription`. Retirar el string `practice_listen` visible y añadir el de
  descripción accesible si no existe ya uno equivalente. Verificación: test de 5.1 en verde.

## 6. Practicar: altura, icono y margen de los botones inferiores (punto 1, parte de Practicar)

- [x] 6.1 Test (`PracticeSessionScreenTest`, `androidTest`): los botones "Mostrar traducción",
  "Fallo", "Acierto" y "Generar sesión nueva" muestran un icono junto al texto (spec:
  `practice-session`, escenarios de "Controles de acción siempre visibles y pulsables" y de
  "Mensaje de tanda completada"). Verificación: el test falla porque ninguno de los cuatro botones
  tiene icono hoy.
- [x] 6.2 Añadir icono a cada uno de los cuatro botones en `PracticeSessionScreen.kt` (ojo para
  revelar, X para fallo, check para acierto, flecha circular para generar sesión) y aplicar
  `Modifier.height(AppTheme.dimensions.primaryActionHeight)` a los cuatro. Verificación: test de
  6.1 en verde.

  Nota de implementación: `Icons.Default.Visibility` (el "ojo" previsto para revelar) solo existe
  en `material-icons-extended`, no en `material-icons-core` (comprobado inspeccionando el aar),
  que es la única dependencia de iconos que declara `:feature:session`; se usó
  `Icons.Default.Search` como alternativa disponible más cercana a "revelar/ver contenido oculto",
  documentado con un comentario en `RevealButton` (`PracticeSessionScreen.kt`), igual que el grupo
  5 hizo con `VolumeUp` → `PlayArrow`. `Icons.Default.Close`, `Check` y `Refresh` sí están en
  `material-icons-core`, como esperaba la tarea.
- [x] 6.3 Reducir el padding inferior del `Box` de contenido de `PracticeSessionScreen` (hoy
  `AppTheme.spacing.m` sobre el `innerPadding` del `Scaffold`) a `AppTheme.spacing.s`, para acercar
  el botón principal a la barra de navegación como en el mock. Verificación de dispositivo/emulador
  (no asertable por test: es una distancia visual), comparando contra `Main.dc.html`/
  `AudioCompacto.dc.html` del mock.

  Aplicado sin verificación visual propia en este grupo (el agente no tiene forma de comparar
  contra el mock); pendiente de la verificación conjunta de dispositivo/emulador de la tarea 11.3.

## 7. Practicar: colores y altura de los chips de resultado (punto 6)

- [x] 7.1 Test (`PracticeSessionScreenTest`, `androidTest`): los tres `DsStatTile` de
  `CompletedMessage` usan colores distintos entre sí (verde para aciertos, rojo para fallos, naranja
  para "sube de rango") y las tres columnas miden la misma altura aunque la etiqueta de una ocupe
  dos líneas (spec: `practice-session`, "El mensaje muestra el desglose de aciertos, fallos y
  subidas de nivel"). Verificación: el test falla porque hoy las tres usan el mismo color y pueden
  medir alturas distintas.
- [x] 7.2 Pasar `tertiaryContainer`/`onTertiaryContainer`, `errorContainer`/`onErrorContainer` y
  `secondaryContainer`/`onSecondaryContainer` a los tres `DsStatTile` de `CompletedMessage`, y
  envolver la `Row` en `Modifier.height(IntrinsicSize.Max)` con cada `DsStatTile.fillMaxHeight()`.
  Verificación: test de 7.1 en verde.

## 8. Practicar: botón de deshacer (punto 7, delta `practice-session`)

- [x] 8.1 Test (`PracticeSessionViewModelTest`): al revelar la frase actual con
  `canRevertLastAnswer` en `true`, el estado resultante tiene `canRevertLastAnswer` en `false`
  (spec: `practice-session`, "El botón desaparece al revelar la frase siguiente"). Verificación: el
  test falla porque `reveal()` hoy solo cambia `revealed`.
- [x] 8.2 Añadir `canRevertLastAnswer = false` a la copia de estado que produce
  `PracticeSessionViewModel.reveal()`. Verificación: test de 8.1 en verde,
  `./gradlew :feature:session:testDebugUnitTest`.
- [x] 8.3 Test (`PracticeSessionScreenTest`, `androidTest`): el botón flotante de deshacer muestra
  un icono de recarga/deshacer, no una flecha de "volver atrás" (mismo escenario de spec que 8.1,
  comprobado a nivel de pantalla). Verificación: el test falla con el icono actual.

  Nota de implementación: compara píxel a píxel el icono realmente pintado dentro del
  `FloatingActionButton` con un icono de referencia (`Icons.AutoMirrored.Filled.Undo`) pintado en el
  mismo árbol con el mismo tinte y fondo, con una tolerancia del 10 % de píxeles distintos (margen de
  antialiasing medido empíricamente: ~3 % entre el mismo icono en dos contextos de render, ~21 % entre
  `Undo` y `ArrowBack`). Falla con el icono actual (`ArrowBack`, ~21 % de diferencia) antes de 8.4.
- [x] 8.4 Cambiar el icono de `PracticeSessionRevertButton.kt` de
  `Icons.AutoMirrored.Filled.ArrowBack` a `Icons.AutoMirrored.Filled.Undo`. Verificación: test de
  8.3 en verde.

  Nota de implementación: `Icons.AutoMirrored.Filled.Undo` solo existe en `material-icons-extended`
  (comprobado inspeccionando el aar de `material-icons-core`, que no la incluye), que
  `:feature:session` no declaraba como dependencia. A diferencia de los grupos 5/6 (que evitaron
  `material-icons-extended` eligiendo un icono equivalente ya en `material-icons-core`), aquí no hay
  ninguna alternativa en `material-icons-core` que no sea otra vez una flecha de "volver atrás" (la
  propia `ArrowBack`), así que se añade `implementation(libs.androidx.compose.material.icons.extended)`
  a `feature/session/build.gradle.kts`, igual que ya hacen `:core:designsystem` y `:app` (justificado
  por design D6, que ya nombraba esa dependencia explícitamente para este icono).
- [x] 8.5 Sacar el botón de deshacer del slot `floatingActionButton` del `Scaffold` de
  `PracticeSessionScreen.kt` y pintarlo dentro de la `Column` de contenido, antes del botón
  principal/los botones de autoevaluación, con su propio espaciado (design D6), añadiendo
  `AnimatedVisibility` (u otra transición equivalente) para no perder la animación de
  aparición/desaparición que tenía como `floatingActionButton`. Verificación de
  dispositivo/emulador (posición relativa y transición, no asertable por test), comparando contra
  el mock.

  Aplicado sin verificación visual propia en este grupo (el agente no tiene forma de comparar contra
  el mock), igual que la nota de la tarea 6.3; pendiente de la verificación conjunta de
  dispositivo/emulador de la tarea 11.3, citada explícitamente en esa tarea como uno de los dos
  puntos que solo se verifican allí.

## 9. Ajustes: icono del botón "Guardar" y cabecera (punto 1 parte de Ajustes, punto 4 parte)

- [ ] 9.1 Test (`SettingsScreenTest`, `androidTest`): el botón "Guardar" muestra un icono de check
  junto al texto (spec: `settings-screen`, "Confirmación visual al guardar" sigue cumpliéndose; el
  icono es un detalle de presentación cubierto por el mock, no un escenario nuevo de spec).
  Verificación: el test falla porque el botón no tiene icono hoy.
- [ ] 9.2 Añadir icono de check al `Button` de "Guardar" en `SettingsScreen.kt` y aplicarle
  `Modifier.height(AppTheme.dimensions.primaryActionHeight)`. Verificación: test de 9.1 en verde.
- [ ] 9.3 Test (`SettingsScreenTest`, `androidTest`): el título "Ajustes" se pinta con
  `MaterialTheme.typography.headlineLarge` dentro del contenido, sin `TopAppBar`. Verificación: el
  test falla con la `TopAppBar` actual.
- [ ] 9.4 Sustituir el `Scaffold(topBar = { TopAppBar(...) })` de `SettingsScreen.kt` por el mismo
  patrón de título grande sin barra propia que usa `PracticeHeader` (design D7). Verificación: test
  de 9.3 en verde, `./gradlew :feature:settings:testDebugUnitTest`.

## 10. Progreso: cabecera de título y tarjeta destacada (punto 4 parte, punto 5)

- [ ] 10.1 Test (`ProgressScreenTest`, `androidTest`): el título "Progreso" se pinta con
  `MaterialTheme.typography.headlineLarge` dentro del contenido, sin `TopAppBar` (mismo patrón que
  9.3). Verificación: el test falla con la `TopAppBar` actual.
- [ ] 10.2 Sustituir el `Scaffold(topBar = { TopAppBar(...) })` de `ProgressScreen.kt` por el mismo
  patrón de título grande (design D7). Verificación: test de 10.1 en verde.
- [ ] 10.3 Test (`ProgressScreenTest`, `androidTest`): la tarjeta de "Frases disponibles" pinta el
  total y el nivel CEFR vigente dentro de un nodo con fondo de color distinto del fondo general de
  la pantalla, con el texto alineado a la izquierda (spec: `progress-overview`, "Total de frases
  disponibles y nivel CEFR vigente"). Verificación: el test falla con el `DsStatTile` centrado
  actual.
- [ ] 10.4 Sustituir el `DsStatTile(Large)` + `Text` de nivel CEFR en `ContentState` de
  `ProgressScreen.kt` por `DsHeroStat`, con color `primary`/`onPrimary` (design D3). Verificación:
  test de 10.3 en verde, `./gradlew :feature:progress:testDebugUnitTest`.

## 11. Verificación final

- [ ] 11.1 Ejecutar `./gradlew check` (tests, ktlint, detekt) en verde.
- [ ] 11.2 Ejecutar `./gradlew koverVerify` en verde (cobertura dominio 90 %, presentación 80 %,
  global 70 % sin bajar por los componentes nuevos de `:core:designsystem` ni por
  `PracticeSessionViewModel`).
- [ ] 11.3 Verificación de dispositivo/emulador de los 7 puntos del mock a la vez (altura y margen
  de botones, caja de aviso, icono de escuchar, cabeceras, tarjeta de Progreso, chips de resultado,
  posición e icono del botón de deshacer), citando explícitamente las tareas 6.3 (margen inferior)
  y 8.5 (posición y transición del botón de deshacer) como los dos puntos que solo se verifican
  aquí, con capturas o nota de qué se comparó contra el mock del artifact, en este propio
  `tasks.md`.

## Workflow follow-up

- Revisar con `android-architect` antes de `/opsx:apply` (presupuesto de revisión: cambio normal,
  una ronda salvo bloqueantes).
- Revisar con `code-reviewer` al terminar la implementación.
- `/opsx:verify` antes de `/opsx:archive`.
- Archivar el cambio y sincronizar specs antes de abrir la pull request, en la misma rama.
