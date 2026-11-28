# Tasks

Escenarios: `openspec/changes/match-source-language/specs/phrase-catalog/spec.md`, requisito "Idioma de origen
según el dispositivo".

## 1. Regla de resolución en el dominio (`:core:domain`)

- [x] 1.1 Test rojo en `LanguageTagTest` (fichero nuevo junto a `ResolveSourceLocaleTest`): `language` devuelve la subetiqueta de idioma (`en-US` → `en`, `en` →
  `en`, `pt-BR` → `pt`). Base de los escenarios por idioma base. Verificar que falla porque `language` no existe.
- [x] 1.2 Añadir `LanguageTag.language` (design D3) y verificar el test de 1.1 en verde.
- [x] 1.3 Test rojo en `ResolveSourceLocaleTest` con la firma de lista (design D4), un test por escenario: "El
  idioma del dispositivo está disponible", "El idioma del dispositivo no está disponible", "Otra región del mismo
  idioma usa el idioma disponible", "Español de otra región usa es-ES", "Portugués de Brasil usa pt-PT", "Un idioma
  sin región usa la variante disponible", "La coincidencia exacta gana a la del mismo idioma", "Varias variantes del
  mismo idioma se desempatan por orden alfabético", "Se usa el siguiente idioma de la lista si el primero no casa",
  "El primer idioma de la lista gana aunque solo case por idioma base", "Ningún idioma de la lista casa" y "Una
  lista de idiomas vacía usa el idioma por defecto". Verificar que fallan por la firma o el resultado.
- [x] 1.4 Implementar `ResolveSourceLocale` con lista (design D4); en `ObtainSourceLanguage` pasar provisionalmente
  `listOf(deviceLocaleProvider.current())`. Verificar 1.3 en verde y `./gradlew :core:domain:check`.

## 2. Puerto con la lista de idiomas del dispositivo (`:core:domain`, `:core:testing`, `:core:platform`)

> Nota: cambiar la firma de `DeviceLocaleProvider` rompe a la vez la compilación del fake, de la implementación
> Android y de los dos objetos anónimos de los tests instrumentados; por eso este grupo toca varios módulos y va en
> un único commit.

- [x] 2.1 Test rojo en `ObtainSourceLanguageTest` (escenario "Se usa el siguiente idioma de la lista si el primero
  no casa"): con `FakeDeviceLocaleProvider(listOf(eu-ES, en-US))` y `en-GB` disponible, el origen es `en-GB`.
  Verificar que falla porque el fake no acepta una lista.
- [x] 2.2 Cambiar el puerto a `preferred(): List<LanguageTag>` (design D1), el fake a `var locales` con constructor
  secundario y `var locale` (design D5), y `ObtainSourceLanguage` para pasar `preferred()` entero. Verificar 2.1 en
  verde y que los escenarios existentes "El idioma de origen no depende del idioma destino activo" y "Un cambio del
  idioma del dispositivo cambia el origen" siguen en verde.
- [x] 2.3 Test rojo en `AndroidDeviceLocaleProviderTest`: con el seam devolviendo `[eu-ES, en-US]`, `preferred()`
  devuelve `[eu-ES, en-US]` en ese orden; y un `Locale` cuya etiqueta no cumple `LanguageTag.isWellFormed` no
  aparece en el resultado (design D2). Verificar que falla por compilación (seam y método nuevos).
- [x] 2.4 Implementar `AndroidDeviceLocaleProvider` con `LocaleList.getDefault()` y el seam `locales` (design D2).
  Verificar 2.3 en verde.
- [x] 2.5 Actualizar el objeto `TestDeviceLocaleProvider` de `PracticeSessionRouteTest` y el objeto anónimo de
  `SettingsRouteTest` a
  `preferred() = listOf(LanguageTag("es-ES"))`. Verificar que compilan con
  `./gradlew :feature:session:compileDebugAndroidTestKotlin :feature:settings:compileDebugAndroidTestKotlin`, y
  `./gradlew :core:domain:check :core:testing:check :core:platform:check :feature:session:check
  :feature:settings:check :feature:progress:check`.

## 3. Cierre y verificación (sesión principal)

- [ ] 3.1 Pasar ADR-0032 a `Aceptado`, cambiar la línea de estado de ADR-0023 a `Sustituido por ADR-0032 (solo la
  regla "Idioma de origen")` y actualizar `docs/adr/README.md` (fila 0032 y estado de 0023). Verificar que el índice
  enlaza el fichero.
- [ ] 3.2 Ejecutar `./gradlew check` y `./gradlew koverVerify` y verificar que pasan.

## Workflow follow-up

- `code-reviewer`, `/opsx:verify` y `/opsx:archive` en la rama `1.0.0/match-source-language`.
- Retomar `localize-interface` (A8) con la regla nueva, según el handoff.
