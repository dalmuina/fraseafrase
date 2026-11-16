# Proposal

## Why

ADR-0019 ya permite más de un idioma destino (hoy `pt-PT`, `pt-BR`) y deriva el idioma de origen del
locale del dispositivo, con `es-ES` como valor por defecto cuando no hay contenido para ese locale.
Añadir `es-ES` como idioma destino aprendible desde portugués sirve a un hispanohablante que ya sabe
portugués, o a un lusohablante que quiere aprender español, sin tocar el contenido existente: es
"añadir contenido, no modificar código", la promesa que ADR-0003 hizo y ADR-0019 mantuvo.

Al convertir `es-ES` en idioma destino aparece un conflicto que el código actual no cubre: la función
`ResolveSourceLocale` usa `es-ES` como origen de respaldo **codificado como literal**, pensado para
cuando `es-ES` solo podía ser origen. Si el destino elegido es `es-ES` y el dispositivo no tiene
contenido de origen distinto disponible, ese literal resolvería el origen a `es-ES` igual que el
destino - "español desde español". Este cambio corrige esa resolución para que el valor de respaldo
nunca pueda coincidir con el idioma destino que se está resolviendo.

## What Changes

- Nuevo catálogo `catalog/phrases-es-ES.json` (`targetLocale: es-ES`, `sourceLocale: pt-PT`), con el
  mismo esquema y las mismas reglas de permanencia de ids que `phrases-pt-PT.json`/`phrases-pt-BR.json`
  (ADR-0013, ADR-0019). Se siembra igual que los idiomas existentes, sin migración de Room: aparece en
  `availableTargetLocales()` y queda seleccionable en Ajustes en cuanto se añade el fichero.
- **Corrección de `ResolveSourceLocale` (comportamiento observable)**: el valor de respaldo deja de ser
  el literal `es-ES` y pasa a resolverse dinámicamente a partir de los orígenes disponibles para el
  idioma destino que se está resolviendo, de forma que nunca coincide con ese destino. Para `pt-PT` y
  `pt-BR` el resultado observable no cambia (su único origen disponible sigue siendo `es-ES`). Para
  `es-ES`, el origen resuelto pasa a ser `pt-PT` en vez de `es-ES`.
- Ajustes: el desplegable de idioma destino muestra "Español (España)" junto a los nombres existentes.
- Design system: `DsLanguageBadge` reconoce el código `es-ES` con una bandera nueva (España), siguiendo
  el mismo patrón que `pt-PT`/`pt-BR`.
- Contrato de catálogo: `es-ES` se añade a la lista de idiomas destino cubiertos por
  `RealCatalogContractTest`, con su propio fichero de ids publicados (vacío al principio, igual que se
  hizo con `pt-BR`).
- **Nuevo ADR** que modifica puntualmente la regla de respaldo de `ResolveSourceLocale` fijada en
  ADR-0019, dejando constancia de por qué un literal de idioma fijo deja de ser válido ahora que
  `es-ES` también puede ser destino.

## Fuera de alcance

- Un segundo catálogo `es-ES` con `sourceLocale: pt-BR`. El catálogo de `pt-BR` existente no tiene
  reglas de nombrado ni de siembra definidas para más de un origen por idioma destino (la clave
  primaria de `catalog_phrase` ya lo admite, pero el nombrado de ficheros no está decidido). Se deja
  para un cambio posterior si hace falta.
- Traducir el catálogo de `es-ES` a otro idioma de origen distinto de `pt-PT`.
- Cualquier cambio en las reglas de tanda, racha, certificación o repetición espaciada: son agnósticas
  del idioma y no cambian.
- Monetización o cualquier otra superficie fuera del catálogo, la resolución de origen, Ajustes y el
  design system.

## Capabilities

### Modified Capabilities
- `phrase-catalog`: nuevo idioma destino `es-ES` disponible; la resolución del idioma de origen por
  defecto deja de usar un literal fijo como respaldo y pasa a excluir el propio idioma destino del
  resultado.

`settings-screen` ya exige mostrar un desplegable con "los idiomas destino disponibles" de forma
genérica (sin fijar cuáles ni sus nombres): `es-ES` aparece ahí sin cambiar ningún requisito. El
nombre visible ("Español (España)") es un detalle de implementación de esa pantalla, no un requisito
nuevo, igual que los nombres de `pt-PT`/`pt-BR` no están en la spec hoy.

## Impact

- **Contenido**: `core/data/src/main/assets/catalog/phrases-es-ES.json` (nuevo),
  `core/data/src/test/resources/catalog/released-ids-es-ES.txt` (nuevo, vacío).
- **`:core:domain`**: `ResolveSourceLocale` (cambia su firma o su lógica de respaldo),
  `ResolveSourceLocaleTest`, `ObtainCatalogPhrasesForActiveLanguage` si cambia la firma.
- **`:core:data`**: `RealCatalogContractTest` (añade `es-ES` a los idiomas destino cubiertos).
- **`:core:designsystem`**: `DsLanguageBadge` (nuevo caso `es-ES`), nuevo drawable de bandera,
  `DsLanguageBadgeTest`.
- **`:feature:settings`**: `SettingsScreen` (nuevo nombre visible para `es-ES`),
  `SettingsScreenTest`/`SettingsScreenPreviews` si cubren la lista de idiomas.
- **Documentación**: ADR nuevo que modifica la regla de respaldo de origen de ADR-0019.
