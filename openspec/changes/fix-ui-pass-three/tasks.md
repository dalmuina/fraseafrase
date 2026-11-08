# Tasks

## 1. Progreso: filtrar el recuento por el nivel CEFR vigente (`:core:domain`)

- [x] 1.1 Añadir a `ObtainProgressOverviewTest` los escenarios "Las frases de otros niveles CEFR no se
      cuentan" (`byLevel`) y "El total no incluye frases de otros niveles CEFR" (total disponible) de
      `specs/progress-overview/spec.md`, usando `aCatalogPhrase(level = ...)` con frases de dos niveles
      CEFR distintos y un `FakeSettingsRepository`/`UserSettings` fijando el nivel CEFR vigente; y
      ajustar los tests existentes de `ObtainProgressOverviewTest` para que todas sus frases de catálogo
      compartan el nivel CEFR de los ajustes usados. Verificar que los dos tests nuevos fallan por el
      recuento actual (cuentan también las frases del otro nivel CEFR) antes de tocar la implementación.
- [x] 1.2 Inyectar `ObtainUserSettings` en `ObtainProgressOverview` y filtrar `loaded.phrases` por
      `CatalogPhrase.level == settings.cefrLevel` antes de calcular `byLevel` (design D3). Verificar que
      `./gradlew :core:domain:test` pasa, incluidos los tests de 1.1.
- [x] 1.3 Actualizar `ProgressFixtures.kt` (`:feature:progress`, test) para construir
      `ObtainProgressOverview` con el nuevo parámetro de ajustes. Verificar que
      `./gradlew :feature:progress:test` sigue en verde sin cambios de comportamiento en
      `ProgressViewModelTest`.

## 2. Ajustes: alinear el stepper con el campo de texto (`:feature:settings`)

- [x] 2.1 Añadir a `SettingsScreenTest` (`androidTest`) los dos escenarios de
      `specs/settings-screen/spec.md` ("Los controles se alinean con el campo de texto sin error
      visible" y "Los controles se mantienen alineados cuando el campo muestra el texto de error"),
      comprobando que el borde superior (`top`) de los nodos de disminuir y aumentar coincide con el
      del campo de texto, con y sin el texto de error visible. Verificar que ambos tests fallan por el
      desplazamiento actual antes de tocar `SettingsScreen.kt`.
- [x] 2.2 Cambiar `verticalAlignment` del `Row` de `BatchSizeCard` en `SettingsScreen.kt` de
      `Alignment.CenterVertically` a `Alignment.Top` (design D1). Verificar que
      `./gradlew :feature:settings:connectedAndroidTest` pasa, incluidos los tests de 2.1.

## 3. Practicar: contenido desplazable con los controles de acción siempre visibles (`:feature:session`)

- [x] 3.1 Añadir a `PracticeSessionScreenTest` (`androidTest`) los tres escenarios de
      `specs/practice-session/spec.md` ("El control de revelar es visible con una frase muy larga",
      "Los controles de acierto y fallo son visibles con una traducción muy larga" y "La frase y la
      traducción muy largas se pueden desplazar"), usando una `PracticeItem` con `source`/`target` de
      longitud suficiente para desbordar la pantalla del test, y comprobando `assertIsDisplayed()` del
      control de revelar o de los de acierto/fallo sin `performScrollTo()` previo. Verificar que los
      tres fallan por el recorte actual antes de tocar `PracticeSessionScreen.kt`.
- [x] 3.2 Reestructurar `PracticingContent` en `PracticeSessionScreen.kt`: una `Column` con
      `Modifier.weight(1f).verticalScroll(rememberScrollState())` para `PhraseCard`/`TranslationCard`, y
      el botón de revelar o `AssessmentButtons` fuera de esa zona de scroll, al final de la `Column`
      exterior (design D2). Verificar que `./gradlew :feature:session:connectedAndroidTest` pasa,
      incluidos los tests de 3.1.

## 4. Verificación final

- [ ] 4.1 Ejecutar `./gradlew check` y `./gradlew koverVerify` en verde, confirmando que no bajan los
      mínimos de cobertura (dominio 90 %, presentación 80 %, global 70 %).
