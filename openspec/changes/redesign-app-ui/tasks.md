# Tasks

## 1. Dominio - desglose de la tanda completada

- [x] 1.1 Test: `BatchOutcome` empieza en (0,0,0) y un acierto que no sube de nivel incrementa solo
      `hits`; falla antes de crear el tipo. Verificar: el test falla por "unresolved reference
      BatchOutcome" y pasa tras crear `BatchOutcome(hits, misses, levelUps)` en `:core:domain`
      (`practice`), cubre el escenario "El mensaje muestra el desglose..." del delta de
      `practice-session`.
- [x] 1.2 Test: un fallo incrementa solo `misses`; un acierto que sube de nivel incrementa `hits` y
      `levelUps`. Verificar: `./gradlew :core:domain:test` en verde para ambos casos, mismo
      escenario que 1.1.
- [x] 1.3 Test: una frase que sube y luego baja de nivel en la misma tanda conserva `levelUps` en 1 (no
      baja). Verificar: test en verde; cubre el escenario "Una frase que sube y vuelve a bajar de
      nivel en la misma tanda cuenta como subida".
- [x] 1.4 Implementar la acumulación de `BatchOutcome` en `AnswerPhrase`, comparando el nivel de la
      frase inmediatamente antes y después de cada respuesta. Verificar: tests de 1.1-1.3 en verde.
- [x] 1.5 Test: `RevertLastAnswer` deshace exactamente el último incremento de `BatchOutcome`
      (resta 1 de `hits` o `misses`, y de `levelUps` si esa respuesta había contado como subida).
      Verificar: test falla antes de tocar `RevertLastAnswer`, pasa después; cubre el escenario "Una
      frase revertida no cuenta en el desglose".
- [x] 1.6 Implementar la reversión de `BatchOutcome` en `RevertLastAnswer`. Verificar: test de 1.5 en
      verde y los tests existentes de `RevertLastAnswer` siguen en verde.
- [x] 1.7 Test: `composeNextBatchInto` (`PracticeTransitions.kt`, compartido por `GenerateSession`,
      `RegenerateActiveBatchForLevel` y `ObtainActiveBatch`) reinicia `BatchOutcome` a (0,0,0) al
      componer una tanda nueva. Verificar: test falla antes, pasa después de reiniciar el contador
      ahí; cubre el escenario "Generar sesión reinicia el desglose para la tanda siguiente" y,
      explícitamente, el camino de cambio de nivel CEFR/N desde Ajustes (`RegenerateActiveBatchForLevel`,
      ADR-0017), que comparte la misma función y debe reiniciar el contador igual.
- [x] 1.8 Implementar el reinicio de `BatchOutcome` en `composeNextBatchInto`. Verificar: test de 1.7
      en verde para los tres llamantes (`GenerateSession`, `RegenerateActiveBatchForLevel`,
      `ObtainActiveBatch`).
- [x] 1.9 Test: `PracticeSessionResult` (tipo que representa la tanda completada) expone el
      `BatchOutcome` de la tanda que se acaba de completar. Verificar: test falla antes de ampliar el
      tipo, pasa después; cubre el escenario "Tanda completa muestra el mensaje positivo" junto con
      el desglose.
- [x] 1.10 Ampliar `PracticeSessionResult`/el caso de uso que detecta tanda completada para incluir el
      `BatchOutcome`. Verificar: test de 1.9 en verde; `./gradlew :core:domain:test` completo en
      verde; cobertura de dominio de Kover sigue ≥ 90 %.

## 2. Dominio - corrección de `progress-overview`

- [x] 2.1 Test: `ObtainProgressOverview` devuelve el total disponible del nivel CEFR vigente y ese
      nivel CEFR, sin depender de si hay tanda activa. Verificar: test falla antes del cambio
      (comprueba la forma antigua `BatchProgress`), pasa después; cubre el escenario "Sin tanda
      activa muestra el total disponible, el nivel CEFR y el total por nivel" de `progress-overview`
      (spec ya vigente, sin delta en este cambio).
- [x] 2.2 Sustituir `ProgressOverviewResult.Available.batchProgress` por el total disponible + nivel
      CEFR vigente, y actualizar `ObtainProgressOverview` para calcularlo a partir de `byLevel`.
      Verificar: test de 2.1 en verde; tests existentes de `ObtainProgressOverview` actualizados y en
      verde.

## 3. Datos - persistencia del desglose de la tanda

- [x] 3.1 Test: tras subir la versión de `PracticeDatabase` con las columnas nuevas, una tanda activa
      leída (en una base recién creada, acorde al `fallbackToDestructiveMigration()` ya usado por el
      único precedente real de esta base de datos) siempre tiene `BatchOutcome` en (0,0,0).
      Verificar: test falla antes de añadir las columnas (no compila o el campo no existe), pasa
      después. No se preserva una tanda persistida con el esquema anterior: ese precedente ya está
      documentado en `design.md` - Decisiones y Risks.
- [x] 3.2 Añadir las columnas de `BatchOutcome` a la entidad Room de la tanda activa, subir la
      versión de `PracticeDatabase` y mantener `fallbackToDestructiveMigration()`. Verificar: test de
      3.1 en verde; `./gradlew :core:data:test` en verde.
- [x] 3.3 `FakePracticeRepository` en `:core:testing` es genérico sobre `PracticeSnapshot` y no
      necesita cambios de compilación para el campo nuevo de `BatchOutcome` si este tiene un valor
      por defecto (0,0,0). Añadir solo un builder de conveniencia para construir snapshots con un
      `BatchOutcome` concreto, usado por los tests del grupo 1 que verifican acumulación/reversión.
      Verificar: los tests de dominio del grupo 1 que usan el builder compilan y pasan sin tocar el
      resto del fake.

## 4. Design system - tokens visuales

- [x] 4.1 Test de snapshot/preview: `AppTheme` expone el color primario, los radios de tarjeta y los
      tamaños tipográficos actualizados en claro y oscuro. Verificar: test de tokens falla antes del
      cambio (valores antiguos), pasa después.
- [x] 4.2 Actualizar `theme/Color.kt` (primario índigo, contenedores, éxito/fallo), `theme/AppShapes.kt`
      (radio de tarjeta ~24dp) y `theme/Type.kt` (tamaños/peso/`letterSpacing` de titulares y cuerpo)
      según `design.md` - Decisiones. Verificar: test de 4.1 en verde; contraste 4.5:1 comprobado
      para cada combinación de color tocada (texto sobre `primaryContainer`, etiquetas de 11-13px).

## 5. Design system - componentes `Ds*` nuevos

- [x] 5.1 Test: `DsStatTile` renderiza el valor y la etiqueta recibidos, en variante `large` y
      `compact`. Verificar: test falla antes de crear el componente, pasa después; añadir
      `@PreviewLightDark`.
- [x] 5.2 Implementar `DsStatTile` en `:core:designsystem` sin dependencias del dominio, usando
      `AppTheme.spacing`/tokens de color. Verificar: test de 5.1 en verde; `detekt`/`ktlint` sin
      errores.
- [x] 5.3 Test: `DsProgressBar` renderiza la proporción `progress` recibida con la forma/color del
      tema. Verificar: test falla antes de crear el componente, pasa después; añadir
      `@PreviewLightDark`.
- [x] 5.4 Implementar `DsProgressBar` envolviendo `LinearProgressIndicator`. Verificar: test de 5.3 en
      verde; `detekt`/`ktlint` sin errores.

## 6. Presentación - `feature:session`

- [x] 6.1 Test: `PracticeSessionUiState.Completed` (o equivalente) expone aciertos, fallos y subidas
      de rango del `BatchOutcome` recibido del dominio. Verificar: test de ViewModel con
      `MainDispatcherExtension` + Turbine falla antes del cambio, pasa después; cubre "El mensaje
      muestra el desglose de aciertos, fallos y subidas de nivel".
- [x] 6.2 Ampliar `PracticeSessionUiState`/`PracticeSessionViewModel` para exponer el desglose desde
      el `BatchOutcome` del caso de uso. Verificar: test de 6.1 en verde.
- [x] 6.3 Test de Compose UI: la pantalla de sesión completada muestra tres `DsStatTile`
      (aciertos/fallos/subidas) y un icono de éxito. Verificar: test falla antes de construir la
      pantalla, pasa después; cubre el mismo escenario que 6.1 a nivel de UI.
- [x] 6.4 Reemplazar `CompletedMessage` por la pantalla de sesión completada del mock (icono de
      éxito, tres `DsStatTile`, botón "Generar sesión nueva", enlace "Ver mi progreso"), con los
      tokens de `AppTheme` del grupo 4. Verificar: test de 6.3 en verde.
- [x] 6.5 Test de Compose UI: activar el enlace "Ver mi progreso" navega a Progreso sin generar
      sesión ni cambiar la tanda activa. Verificar: test falla antes de conectar el enlace, pasa
      después; cubre el escenario "El mensaje ofrece ir a Progreso sin generar sesión".
- [x] 6.6 Conectar el enlace "Ver mi progreso" a la navegación del destino Progreso ya existente en
      `:app`. Verificar: test de 6.5 en verde.
- [x] 6.7 Test de Compose UI: la cabecera de `PhraseCard` muestra el nivel CEFR y la insignia de rango
      como pastillas, y el control de escuchar muestra icono y una etiqueta de texto visible en la
      cabecera cuando la frase está revelada. Verificar: test falla antes de mover el control, pasa
      después; confirma que `phrase-audio` sigue cumpliéndose (icono + texto visibles) tal como
      exige su spec, sin delta.
- [x] 6.8 Reestilizar `PracticeSessionScreen`/`PhraseCard`/`RevertLastAnswerButton` con los tokens y
      componentes de los grupos 4 y 5 (pastillas de cabecera, `DsProgressBar` en el indicador de
      posición, color del botón de revertir), sin tocar la lógica de revelar/autoevaluar/revertir ya
      cubierta por tests existentes. Verificar: test de 6.7 en verde; tests de Compose UI existentes
      de `PracticeSessionScreen` siguen en verde sin modificarlos.

## 7. Presentación - `feature:progress`

- [x] 7.1 Test de Compose UI: la tarjeta destacada de Progreso usa `DsStatTile` (variante `large`)
      con el total disponible y el nivel CEFR vigente, leyendo la forma corregida de
      `ProgressOverviewResult` del grupo 2. Verificar: test falla antes del cambio (forma antigua),
      pasa después; cubre los escenarios de "Total de frases disponibles y nivel CEFR vigente".
- [x] 7.2 Conectar `ProgressScreen` a la forma corregida de `ProgressOverviewResult` y reestilizar la
      tarjeta destacada con `DsStatTile`. Verificar: test de 7.1 en verde.
- [x] 7.3 Test de Compose UI: cada fila de nivel muestra `DsLevelBadge`, su recuento y una
      `DsProgressBar` proporcional al total de esa fila frente al total disponible, sin mostrar el
      número de nivel como texto. Verificar: test falla antes de añadir la barra, pasa después;
      cubre "Total de frases por nivel" (ya vigente, sin delta).
- [x] 7.4 Añadir `DsProgressBar` a cada fila de `ProgressScreen` y reestilizar con los tokens del
      grupo 4. Verificar: test de 7.3 en verde; tests de Compose UI existentes de `ProgressScreen`
      (recuento por nivel) siguen en verde sin modificarlos.

## 8. Presentación - `feature:settings`

- [x] 8.1 Test de Compose UI: el selector de nivel CEFR se muestra como grupo de cinco botones
      segmentados con selección única, accesible como grupo de radio. Verificar: test falla antes de
      sustituir los `FilterChip`, pasa después; cubre "Selección del nivel CEFR" (ya vigente, sin
      delta).
- [x] 8.2 Reemplazar `CefrLevelCard` (chips) por el selector segmentado, con
      `Modifier.selectableGroup()` y rol de radio por botón, reestilizado con los tokens del grupo 4.
      Verificar: test de 8.1 en verde.
- [x] 8.3 Test de Compose UI: el stepper de tamaño de tanda sigue deshabilitando disminuir/aumentar en
      los límites 10/30 y mantiene su borde superior alineado con el campo de texto, incluido con el
      texto de error visible. Verificar: test existente adaptado al nuevo estilo sigue en verde tras
      el reestilizado; cubre "Edición del tamaño de tanda N con validación" y "Alineación visual del
      stepper" (ambos ya vigentes, sin delta).
- [x] 8.4 Reestilizar `BatchSizeCard` con los tokens del grupo 4 sin tocar su lógica de validación.
      Verificar: test de 8.3 en verde.

## 9. Integración en `:app`

- [ ] 9.1 Test de Compose UI: la bottom bar (`FraseAFraseApp.kt`) muestra los tres destinos con los
      colores actualizados del tema, sin cambiar sus rutas ni comportamiento. Verificar: test de
      navegación existente sigue en verde tras aplicar los tokens del grupo 4.
- [ ] 9.2 Verificación manual en el emulador/dispositivo: recorrer Practicar → revelar → acierto/fallo
      → tanda completada → "Ver mi progreso" → Ajustes → guardar cambio de nivel CEFR, comprobando
      que el aspecto coincide con el mock y que ninguna regla de negocio existente cambió. Verificar:
      captura o nota de la comprobación en la PR.

## 10. Verificación final

- [ ] 10.1 Ejecutar `./gradlew check` (tests + detekt + ktlint) en verde.
- [ ] 10.2 Ejecutar `./gradlew koverVerify` y confirmar los mínimos de cobertura (dominio 90 %,
      presentación 80 %, global 70 %).

## Workflow follow-up

- Revisar la propuesta y el diseño con el subagente `android-architect` antes de `/opsx:apply`.
- Implementar los grupos de tareas con el subagente `tdd-implementer`, un grupo pequeño de tareas
  relacionadas por invocación.
- Revisar el código con el subagente `code-reviewer` antes de `/opsx:verify`.
- Ejecutar `/opsx:verify` y, si corresponde, `/opsx:archive` en la rama del cambio antes de la PR.
