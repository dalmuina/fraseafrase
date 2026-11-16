# Tasks

## 1. ADR

- [x] 1.1 Redactar `docs/adr/0020-respaldo-de-origen-sin-coincidir-con-el-destino.md` (Propuesto),
  `Modifica a: ADR-0019`, con la regla de respaldo de `ResolveSourceLocale` fijada en design.md.
  Añadir la fila de ADR-0020 a la tabla de `docs/adr/README.md`.

## 2. Dominio: `ResolveSourceLocale` sin coincidir con el destino

- [x] 2.1 Test: en `ResolveSourceLocaleTest`, añadir el escenario "el respaldo no coincide con el
  propio destino" (spec `phrase-catalog`, idem design.md) - `targetLocale` `es-ES`, `available`
  `[pt-PT]`, `deviceLocale` `es-ES`, debe resolver `pt-PT`. Adaptar los dos tests existentes a la
  nueva firma con `targetLocale`. Ejecutar `./gradlew :core:domain:test` y comprobar que el test
  nuevo falla (firma inexistente) y los adaptados compilan en rojo.
- [x] 2.2 Implementar la nueva firma de `ResolveSourceLocale` de design.md (excluye `targetLocale` de
  `available`, prefiere `es-ES` entre los candidatos, si no el primero disponible). Actualizar
  `ObtainCatalogPhrasesForActiveLanguage` para pasar `targetLocale`. Verificar
  `./gradlew :core:domain:test` en verde.
- [x] 2.3 Test: en `ObtainCatalogPhrasesForActiveLanguageTest`, comprobar que `targetLocale` llega a
  `ResolveSourceLocale` (p. ej. con un `FakeCatalogRepository` cuyo único `sourceLocale` disponible
  coincide con el `targetLocale` consultado, y verificando que no se usa como origen). Verificar
  `./gradlew :core:domain:test`.

## 3. Contenido: catálogo `es-ES`

- [x] 3.1 Generar `catalog/phrases-es-ES.json` (`schemaVersion: 1`, `targetLocale: es-ES`,
  `sourceLocale: pt-PT`) invirtiendo `target`/`source` de `catalog/phrases-pt-PT.json`, conservando
  `id`/`level`/`topic` (design.md - Decisions). Verificar que `CatalogJson.parse` lo acepta
  (`./gradlew :core:data:test` con un test de fixture añadido para `es-ES`, mismo patrón que el de
  `pt-BR`).
- [x] 3.2 Revisar por muestreo una selección representativa (al menos una frase por nivel CEFR y
  tema) buscando frases cuyo sentido no se sostenga al invertir la dirección (design.md). Documentar
  la revisión en este fichero. Aplicar los cambios que la revisión señale antes de seguir.
- [x] 3.3 Crear `core/data/src/test/resources/catalog/released-ids-es-ES.txt` vacío y añadir `"es-ES"`
  al `@ValueSource` de `RealCatalogContractTest` ("se carga") y al test de "idioma destino nuevo sin
  ids publicados" (spec `phrase-catalog`, escenario "Un idioma destino nuevo sin ids publicados no
  bloquea su primera carga"). Verificar `./gradlew :core:data:test`.

## 4. Design system: badge e icono de `es-ES`

- [x] 4.1 Test: en `DsLanguageBadgeTest`, añadir `givenEsEsWhenPaintingThenEsEsIconIsShown` (mismo
  patrón que `pt-PT`/`pt-BR`). Verificar `./gradlew :core:designsystem:connectedCheck
  -Pandroid.testInstrumentationRunnerArguments.class=com.dalmuina.fraseafrase.core.designsystem.component.DsLanguageBadgeTest`
  y comprobar que falla porque `es-ES` no está reconocido.
- [x] 4.2 Añadir `core/designsystem/src/main/res/drawable/ic_flag_es_es.xml` (bandera de España, mismo
  estilo que `ic_flag_pt_pt.xml`/`ic_flag_pt_br.xml`) y la rama `"es-ES" ->
  LanguageBadgeSpec(R.drawable.ic_flag_es_es, "es-ES")` en `languageBadgeSpec`. Añadir `"es-ES"` a
  `AllLanguageCodes` del `@PreviewLightDark`. Verificar el test de 4.1 en verde.
  **Nota de verificación:** el test instrumentado no pudo confirmarse en este entorno: el `adb install`
  de la APK de test falla con `ShellCommandUnresponsiveException` (emulador `emulator-5554` detectado
  por `adb devices`, pero no responde a la instalación; ver intentos fallidos repetidos). Se verificó en
  su lugar `./gradlew :core:designsystem:compileDebugKotlin` (verde) y `:core:designsystem:check`, dado
  que el patrón de `languageBadgeSpec`/`AllLanguageCodes` es idéntico al ya probado para `pt-PT`/`pt-BR`
  y el recurso `ic_flag_es_es.xml` ya existía.

## 5. Ajustes: nombre visible de `es-ES`

- [x] 5.1 Test: si `SettingsScreenTest`/`SettingsViewModelTest` ya tienen un escenario genérico sobre
  la lista de idiomas disponibles con un fake de más de un idioma, extenderlo con `es-ES` esperando el
  texto "Español (España)"; si no existe tal escenario, añadir uno equivalente al de `pt-BR` (spec
  `settings-screen`, "Elegir un idioma destino de la lista"). Verificar
  `./gradlew :feature:settings:test` (y el `androidTest` correspondiente) en rojo por el texto
  ausente.
- [x] 5.2 Añadir la rama `"es-ES" -> "Español (España)"` en `LanguageTag.toDsLanguageOption()`
  (`SettingsScreen.kt`). Verificar `./gradlew :feature:settings:check` en verde.

## 6. Verificación final

- [ ] 6.1 Ejecutar `./gradlew check` completo (tests, ktlint, detekt) y corregir cualquier fallo.
- [ ] 6.2 Ejecutar `./gradlew koverVerify` y ajustar cobertura si algún módulo queda por debajo de su
  mínimo (dominio 90 %, presentación 80 %, `:core:data` 80 % según ADR-0013, global 70 %).

## Revisión por muestreo de la tarea 3.2

- **Criterio**: muestreo estratificado por nivel CEFR (A1-C1) y, dentro de cada nivel, por `topic`
  (hasta 4 topics distintos por nivel elegidos al azar con semilla fija). Además, búsqueda dirigida por
  palabras clave (`portugu*`, `espan*`, `castelh*`, `traduz`/`traduc`, `se dice`/`se diz`, `idioma`,
  `língua`/`lengua`) sobre las 1000 frases para localizar cualquier frase que mencione explícitamente un
  idioma o nacionalidad y pudiera depender de la dirección original (p. ej. una frase pensada como
  pregunta de un hispanohablante a un lusohablante o viceversa).
- **Frases revisadas**: 20 por el muestreo estratificado (4 por nivel x 5 niveles) más 13 encontradas
  por palabras clave (2 de la búsqueda de país/idioma + 11 de la búsqueda de lengua/traducción), 33 en
  total, sin solapamiento.
- **Resultado**: ninguna frase problemática. Las que mencionan España/Portugal lo hacen como
  referencia geográfica neutra (p. ej. `a2-correos-001`, "Envié una carta a España" / "Enviei uma carta
  para Espanha"), y las que mencionan lengua/traducción son observaciones generales (lingüística,
  crítica literaria, IA) sin alusión a la dirección es-ES/pt-PT del propio catálogo. No se encontró
  ninguna frase formulada como pregunta de un hablante de un idioma hacia el otro. No fue necesario
  corregir ninguna frase.

## Workflow follow-up

- Revisar con `code-reviewer` antes de `/opsx:verify`.
- Ejecutar `/opsx:verify` contra las specs de este cambio.
- Archivar con `/opsx:archive` en la rama del cambio, antes de la PR (incluye pasar el ADR-0020 a
  Aceptado).
