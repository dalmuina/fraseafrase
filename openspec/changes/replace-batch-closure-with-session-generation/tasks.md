# Tasks

## 1. Dominio: composición de la tanda por cuotas

- [ ] 1.1 Test: `ComposeNextBatchTest` para el cálculo de cuotas con N=20 (8/8/4) y para el reparto de hueco
  cuando un bloque no tiene suficientes candidatos (reforzar corto → nuevas; dominadas corto → reforzar), según
  los escenarios de `specs/practice-batch/spec.md` ("Cuotas por defecto...", "Reforzar corto...", "Dominadas
  corto..."). Verificación: los tests fallan por ausencia de la lógica de cuotas, no por otro motivo.
- [ ] 1.2 Implementar las cuotas en `ComposeNextBatch` (bloques nuevas / a reforzar / dominadas, con el reparto
  de hueco en el orden a reforzar → nuevas → dominadas), conservando el orden interno ya existente dentro de "a
  reforzar" (arrastradas antes que pendientes; pendientes por nivel, fecha y catálogo). Verificación: los tests
  de 1.1 y los ya existentes de composición por prioridad que sigan aplicando están en verde (en particular:
  "Frase con repaso futuro no entra antes de su fecha", "El filtro CEFR no afecta a arrastradas ni pendientes",
  "Pendientes que no caben esperan sin perder nivel", "Catálogo agotado sin pendientes no crea tanda").
- [ ] 1.3 Test: cubrir "Dominada sin repaso pendiente entra en el bloque de dominadas" y "Dominada pendiente hoy
  cuenta como a reforzar" (frase en un único lugar). Verificación: falla antes de añadir el filtro de
  dominadas.
- [ ] 1.4 Implementar el filtro de "dominadas" (nivel de consolidación 4 o 5, excluyendo las que ya sean
  arrastradas o pendientes hoy). Verificación: tests de 1.3 en verde.

## 2. Dominio: generar sesión

- [ ] 2.1 Test: nuevo caso de uso `GenerateSession` resuelve la tanda activa (certifica, arrastra) y compone la
  siguiente en la misma transacción, sin `expectedUncertified`, según "Generar sesión resuelve y compone en un
  paso" y "La cola sobrante va antes que las arrastradas de este cierre" de `specs/practice-batch/spec.md`.
  Verificación: el test falla por ausencia de la clase.
- [ ] 2.2 Implementar `GenerateSession` en `core/domain/.../practice/`, extrayendo a una función interna
  compartida la parte común con `ForceCloseActiveBatch` (mover entradas a la cola, con o sin resolución previa
  de cierre; design D4). Retirar `CloseActiveBatch`. Verificación: tests de 2.1 en verde.
- [ ] 2.3 Test: "Tanda completa tras responder a todas sus frases" y "Tanda incompleta con una frase sin
  responder" de `specs/practice-batch/spec.md`, sobre el resultado de `ObtainPracticeSession`. Verificación:
  falla por ausencia del estado derivado.
- [ ] 2.4 Añadir a `PracticeSessionResult`/`ObtainPracticeSession` el estado derivado de tanda completa (design
  D7). Verificación: tests de 2.3 en verde.

## 3. Dominio: regeneración por cambio de nivel CEFR

- [ ] 3.1 Test: nuevo caso de uso `RegenerateActiveBatchForLevel` no certifica ni cambia nivel/`nextReview`, y
  mueve las entradas de la tanda activa a la cola de arrastradas delante de la cola sobrante, componiendo la
  siguiente con el nuevo CEFR y las mismas cuotas, según los escenarios de "Regeneración de la tanda activa por
  cambio de nivel CEFR" de `specs/practice-batch/spec.md`. Verificación: falla por ausencia de la clase.
- [ ] 3.2 Implementar `RegenerateActiveBatchForLevel` en `core/domain/.../practice/`, reutilizando la función
  interna compartida de 2.2 sin la resolución de cierre. Verificación: tests de 3.1 en verde.

## 4. Design system: nombre de insignia reutilizable

- [ ] 4.1 Test: `DsLevelBadgeTest` (o test de la nueva función) para `levelBadgeLabel(level)` con los cinco
  niveles y `null` en 0 y fuera de rango. Verificación: falla por no existir la función pública.
- [ ] 4.2 Extraer la tabla nivel→nombre de `levelBadgeSpec` a `levelBadgeLabel(level: Int): String?` pública en
  `DsLevelBadge.kt` (design D6), sin cambiar el comportamiento visual de `DsLevelBadge`. Verificación: tests de
  4.1 en verde; previews de `DsLevelBadge` sin cambios.

## 5. Presentación: práctica (feature:session)

- [ ] 5.1 Test: `PracticeSessionViewModelTest` para el nuevo estado de tanda completa y la acción
  `GenerateSession` (sustituye a `RequestClose`/`CancelClose`/`ConfirmClose`), según "Mensaje de tanda completada
  y generación de sesión" de `specs/practice-session/spec.md`. Verificación: falla por ausencia del estado y la
  acción.
- [ ] 5.2 Actualizar `PracticeSessionUiState` (sustituir `closeWarning: Int?`, design D7) y
  `PracticeSessionAction` (sustituir `RequestClose`/`CancelClose`/`ConfirmClose` por `GenerateSession`).
  Actualizar `PracticeSessionViewModel` para usar `GenerateSession` y el nuevo estado derivado. Verificación:
  tests de 5.1 en verde.
- [ ] 5.3 Test: contador "aciertos para el próximo nivel" en el `ViewModel` o en una función pura de
  presentación, para los escenarios de "Aciertos para el próximo nivel visibles" de
  `specs/practice-session/spec.md` (incluidos racha 3 y nivel máximo). Verificación: falla por ausencia del
  cálculo.
- [ ] 5.4 Implementar el cálculo `max(0, 3 - racha)` aciertos para la insignia `min(nivel + 1, 5)`, usando
  `levelBadgeLabel` de 4.2. Verificación: tests de 5.3 en verde.
- [ ] 5.5 Actualizar `PracticeSessionScreen.kt` (`PhraseCard`): quitar los textos "Racha %d" y "Sabida"; mostrar
  el mensaje de tanda completada con el botón de generar sesión en vez del diálogo de aviso; mostrar el nuevo
  contador. Actualizar `strings.xml` (retirar `practice_streak` y `practice_known`; añadir las cadenas nuevas).
  Verificación: `./gradlew :feature:session:testDebugUnitTest` en verde.
- [ ] 5.6 Actualizar `feature/session/src/test/.../PracticeSessionCloseTest.kt` para el nuevo `GenerateSession`
  (sustituye el cierre en dos pasos por el paso único; retirar lo que ya no aplique por la ausencia de
  `expectedUncertified`/`Stale`). Verificación: el test se ejecuta sobre `GenerateSession`, no sobre
  `CloseActiveBatch`, y pasa.
- [ ] 5.7 Test de pantalla (`androidTest`): actualizar `PracticeSessionScreenTest.kt` retirando los casos sobre
  "Racha N"/"Sabida" y el diálogo de cierre, y añadiendo los nuevos escenarios de
  `specs/practice-session/spec.md` ("Tanda completa muestra el mensaje positivo", "Generar sesión muestra la
  tanda siguiente con sus arrastradas primero", "Contador visible tras un acierto", "Racha de tres sigue
  mostrando el contador en 0", "Nivel máximo sigue mostrando el contador"). Verificación: los tests fallan antes
  de 5.5 (elementos de UI inexistentes) y pasan después.
- [ ] 5.8 Actualizar `PracticeSessionRouteTest.kt`, que hoy importa `CloseActiveBatch` (retirado en 2.2), para
  que compile y pase con el nuevo caso de uso. Verificación:
  `./gradlew :feature:session:connectedDebugAndroidTest` compila y pasa.

## 6. Presentación: ajustes (feature:settings)

- [ ] 6.1 Test: `SettingsViewModelTest` para distinguir cambio solo de CEFR (llama a
  `RegenerateActiveBatchForLevel`, sin cerrar) de cambio de N -con o sin cambio de CEFR a la vez- (llama a
  `ForceCloseActiveBatch`, con resolución completa), según los escenarios de `specs/settings-screen/spec.md`
  ("Cambiar solo el nivel CEFR no certifica ni cierra", "Cambiar N cierra la tanda activa sin aviso", "Cambiar N
  y el nivel CEFR a la vez cierra la tanda activa"). Verificación: falla por ausencia de la distinción.
- [ ] 6.2 Inyectar `RegenerateActiveBatchForLevel` en `SettingsViewModel` y actualizar `save()` para elegir
  entre los dos casos de uso: si N cambia (con o sin cambio de CEFR), `ForceCloseActiveBatch`; si solo cambia
  CEFR, `RegenerateActiveBatchForLevel` (design D5). Verificación: tests de 6.1 en verde.

## 7. Integración (:app)

- [ ] 7.1 Actualizar el grafo de Hilt en `:app` si hace falta exponer `GenerateSession` o
  `RegenerateActiveBatchForLevel` explícitamente (normalmente no, al llevar `@Inject constructor`).
  Verificación: `./gradlew assembleDebug` compila sin errores de inyección.

## 8. Verificación final

- [ ] 8.1 Ejecutar `./gradlew check` (tests, ktlint, detekt) en verde.
- [ ] 8.2 Ejecutar `./gradlew koverVerify` y comprobar los mínimos de cobertura (dominio 90 %, presentación
  80 %, global 70 %).
- [ ] 8.3 Prueba manual en el emulador: completar una vuelta a una tanda pequeña y generar sesión; cambiar el
  nivel CEFR en Ajustes con una tanda a medias y comprobar que el nivel de consolidación y el próximo repaso de
  sus frases no cambian; cambiar N (con o sin CEFR a la vez) y comprobar que sí certifica/cierra como hoy.

## Workflow follow-up

- Ejecutar `/opsx:verify` antes de archivar, para comprobar que la implementación cumple los escenarios de los
  deltas de `practice-batch`, `practice-session` y `settings-screen`.
- Tras el `APROBADO` de `android-architect` sobre esta propuesta (incluido `docs/adr/0016-generar-sesion-por-
  cuotas.md`), commitear los artefactos de OpenSpec y el ADR en su propio commit `docs(openspec): ...`, antes de
  `/opsx:apply`.
- Ejecutar `/opsx:archive` en la rama del cambio, antes de la pull request, y commitear el archivo junto con las
  specs sincronizadas.
