# Design

## Context

Ver `proposal.md` - Why. Estado actual relevante:

- `RoomCatalogDataSource` (`:core:data`) siembra `CatalogDatabase` leyendo todos los `.json` de
  `assets/catalog/`, sin lista cerrada de ficheros: añadir un documento nuevo basta para que se
  siembre y aparezca en `availableTargetLocales()`/`availableSourceLocales()`. No hace falta tocar
  esta clase.
- `ResolveSourceLocale` (`:core:domain`) es una función pura:
  ```kotlin
  object ResolveSourceLocale {
      operator fun invoke(deviceLocale: LanguageTag, available: List<LanguageTag>): LanguageTag =
          available.firstOrNull { it == deviceLocale } ?: LanguageTag("es-ES")
  }
  ```
  El respaldo `LanguageTag("es-ES")` es un literal pensado para cuando `es-ES` solo podía ser origen.
  `ObtainCatalogPhrasesForActiveLanguage` la invoca como
  `ResolveSourceLocale(deviceLocaleProvider.current(), catalogRepository.availableSourceLocales(targetLocale))`:
  `available` nunca está vacío para un `targetLocale` que ya pasó `availableTargetLocales()` (un
  idioma destino sin ningún `sourceLocale` no se consideraría sembrado), así que un respaldo tomado de
  `available` siempre tiene un valor que devolver.
- `DsLanguageBadge`/`languageBadgeSpec` (`:core:designsystem`) y
  `LanguageTag.toDsLanguageOption()` (`:feature:settings`, `SettingsScreen.kt`) son los dos únicos
  sitios con un `when` cerrado sobre el código de idioma (`pt-PT`, `pt-BR`); ambos ya tienen una rama
  `else` segura (no pintar nada / usar el propio código), así que añadir `es-ES` es añadir una rama más
  en cada uno, sin tocar su estructura.
- `RealCatalogContractTest` (`:core:data`) tiene la lista de idiomas destino cubiertos como
  `@ValueSource(strings = ["pt-PT", "pt-BR"])` en dos sitios: hay que añadir `"es-ES"` y su fichero
  `released-ids-es-ES.txt` (vacío), igual que se hizo con `pt-BR`.

## Goals / Non-Goals

**Goals:**
- `es-ES` disponible como idioma destino con contenido propio (`sourceLocale: pt-PT`), sin tocar el
  contenido ni el progreso de `pt-PT`/`pt-BR`.
- Eliminar el literal `es-ES` como respaldo de `ResolveSourceLocale`, de forma que el respaldo nunca
  pueda coincidir con el idioma destino que se está resolviendo, para cualquier idioma destino
  presente y futuro.
- `es-ES` reconocido en los dos puntos de la UI que hoy enumeran idiomas por código (badge, nombre
  visible en Ajustes).

**Non-Goals:**
- Un segundo origen (`pt-BR`) para el catálogo `es-ES` (ver proposal.md - Fuera de alcance).
- Cambiar `CatalogRepository`, `RoomCatalogDataSource` o el esquema de `CatalogDatabase`: ya soportan
  N idiomas destino sin migración.
- Cambiar `SettingsRepository`, `PracticeRepository` o cualquier independencia por idioma destino ya
  resuelta por ADR-0019.

## Decisions

### Contenido: `catalog/phrases-es-ES.json`, `sourceLocale: pt-PT`

Mismo esquema que `phrases-pt-PT.json` (`schemaVersion: 1`, `targetLocale: es-ES`,
`sourceLocale: pt-PT`, `phrases[]`), revisado como diff de texto (ADR-0004/ADR-0013) y con ids propios
e independientes de los de `pt-PT`/`pt-BR` (la permanencia de ids ya se verifica por idioma destino,
spec `phrase-catalog`). Se añade `core/data/src/test/resources/catalog/released-ids-es-ES.txt` vacío,
siguiendo el patrón que `pt-BR` ya estableció para un idioma destino nuevo sin ids publicados.

El contenido se genera **invirtiendo las columnas `target`/`source` de `phrases-pt-PT.json`**, en vez
de traducir de cero: cada frase de `phrases-pt-PT.json` ya es un par (es-ES, pt-PT) revisado, así que
`phrases-es-ES.json` reutiliza ese mismo texto con los campos `target` y `source` intercambiados
(`id`/`level`/`topic` se conservan igual). Esto evita el riesgo de traducción automática que sí existe
para `pt-BR` (donde `target` se tradujo de cero): aquí no hay texto nuevo, solo una relectura para
confirmar que el intercambio no deja ninguna frase con matices que solo tengan sentido en una
dirección (p. ej. una frase pensada como pregunta de un hispanohablante a un lusohablante). Se revisa
por muestreo igual que `pt-BR`, pero buscando ese caso concreto en vez de errores de traducción.

**Alternativa descartada**: generar también `sourceLocale: pt-BR` para `es-ES` en este cambio. Dos
documentos de catálogo con el mismo `targetLocale` chocarían en el nombre de fichero
`phrases-<targetLocale>.json` que usa `RealCatalogContractTest`; decidir ese nombrado (p. ej.
`phrases-es-ES-from-pt-BR.json`) sin tener aún el contenido es prematuro y se deja para cuando se
aborde ese idioma de origen.

### `ResolveSourceLocale`: el respaldo se toma de `available`, nunca del propio destino

```kotlin
// :core:domain
object ResolveSourceLocale {
    operator fun invoke(
        deviceLocale: LanguageTag,
        targetLocale: LanguageTag,
        available: List<LanguageTag>,
    ): LanguageTag {
        val candidates = available.filterNot { it == targetLocale }
        return candidates.firstOrNull { it == deviceLocale }
            ?: candidates.firstOrNull { it == LanguageTag("es-ES") }
            ?: candidates.first()
    }
}
```

- `candidates` excluye explícitamente `targetLocale` de `available` antes de elegir: aunque algún
  idioma destino futuro llegara a tener su propio código entre sus orígenes disponibles (no ocurre
  hoy: cada documento de catálogo declara un `sourceLocale` distinto de su `targetLocale`), el
  resultado nunca podría ser "un idioma desde sí mismo".
- Se sigue preferiendo `es-ES` como respaldo cuando está disponible y no es el propio destino, para no
  cambiar el comportamiento observable de `pt-PT`/`pt-BR` (su único origen disponible sigue siendo
  `es-ES`).
- `candidates.first()` no lanza `NoSuchElementException` en la práctica: `available` para un
  `targetLocale` ya sembrado nunca está vacío (ver Context), y le basta con tener un elemento distinto
  de `targetLocale` porque ningún documento de catálogo declara su propio `targetLocale` como
  `sourceLocale`. Queda cubierto por un test unitario que lo fija como invariante, no por manejo de
  error: si algún día se incumpliera, es un error de contenido del catálogo, no un caso a silenciar.

**Alternativa descartada**: excluir `es-ES` de `availableTargetLocales()` cuando el origen resuelto
coincidiera con él, o excluirlo de la lista de Ajustes. Se descarta porque el problema no es que
`es-ES` no deba ser destino, sino que el respaldo estaba mal calculado (un literal, no una función del
destino); corrigiendo `ResolveSourceLocale` el conflicto desaparece para `es-ES` sin reducir qué
idiomas destino puede ofrecer la app, y sin necesitar lógica adicional en `ObtainAvailableLanguages` ni
en la pantalla de Ajustes.

`ObtainCatalogPhrasesForActiveLanguage` pasa `targetLocale` a la llamada:
```kotlin
val sourceLocale = ResolveSourceLocale(deviceLocaleProvider.current(), targetLocale, available)
```

### ADR nuevo: ADR-0020, modifica la regla de respaldo de ADR-0019

ADR-0019 ya fija "el origen se deriva del dispositivo, con `es-ES` por defecto" como decisión de
arquitectura; cambiar esa regla de respaldo es una decisión de arquitectura, no un detalle de
implementación (ADR-0001). Se propone `docs/adr/0020-respaldo-de-origen-sin-coincidir-con-el-destino.md`
que:
- Referencia el contexto: `es-ES` deja de ser solo origen y el respaldo literal de ADR-0019 queda
  incorrecto para ese caso.
- Decide la regla general de este documento (respaldo tomado de los orígenes disponibles del propio
  destino, nunca el propio destino, con `es-ES` como preferencia dentro de esos candidatos).
- Encabezado `Modifica a: ADR-0019` (siguiendo el mismo patrón que ADR-0019 usó con ADR-0003): no
  sustituye el ADR completo, solo esa regla puntual. No toca ADR-0003 porque ADR-0019 ya lo modificó
  en lo referente a variantes/orígenes; este ADR no introduce una decisión nueva sobre ADR-0003.

### Design system y Ajustes: nueva rama por código, mismo patrón

- `languageBadgeSpec`: nueva rama `"es-ES" -> LanguageBadgeSpec(R.drawable.ic_flag_es_es, "es-ES")`.
  Nuevo drawable vectorial `ic_flag_es_es.xml` en `:core:designsystem`, mismo estilo que
  `ic_flag_pt_pt.xml`/`ic_flag_pt_br.xml` (bandera de España). Se añade a `AllLanguageCodes` del
  `@PreviewLightDark` existente.
- `LanguageTag.toDsLanguageOption()` (`SettingsScreen.kt`): nueva rama `"es-ES" -> "Español (España)"`,
  mismo criterio que las existentes (nombre nativo con la variante entre paréntesis).
- Ninguno de los dos introduce dependencias ni tipos nuevos: ambos ya reciben `String`, no
  `LanguageTag`, cumpliendo ADR-0010.

### Estrategia de test

- `ResolveSourceLocaleTest` (`:core:domain`, unitario): casos existentes adaptados a la nueva firma
  (con `targetLocale`), más el escenario nuevo "el respaldo no coincide con el propio destino"
  (`es-ES` destino, `pt-PT` disponible, dispositivo `es-ES` → resuelve `pt-PT`).
- `ObtainCatalogPhrasesForActiveLanguageTest`: verificar que pasa `targetLocale` a
  `ResolveSourceLocale` sin cambiar su contrato con `CatalogRepository`.
- `RealCatalogContractTest` (`:core:data`, integración sobre el asset real): `"es-ES"` añadido al
  `@ValueSource` de "se carga" y al test de "idioma destino nuevo sin ids publicados".
- `DsLanguageBadgeTest` (`:core:designsystem`, UI): nuevo test `givenEsEsWhenPaintingThenEsEsIconIsShown`,
  mismo patrón que `pt-PT`/`pt-BR`.
- `SettingsScreenTest`/`SettingsViewModelTest` (`:feature:settings`): si ya cubren la lista de nombres
  visibles con un fake de tres idiomas, se extiende con `es-ES`; no se necesita un test nuevo si el
  escenario existente ya es genérico sobre "idiomas disponibles".
- No hace falta ningún fake ni cambio en `:core:testing` (`FakeCatalogRepository`,
  `FakeDeviceLocaleProvider`): ya aceptan locales arbitrarios.

## Risks / Trade-offs

- [Riesgo] Cambiar la firma de `ResolveSourceLocale` (añade `targetLocale`) rompe su único punto de
  llamada fuera de los tests → Mitigación: un único caller (`ObtainCatalogPhrasesForActiveLanguage`),
  se actualiza en la misma tarea.
- [Riesgo] Traducir frases de `pt-PT` a `es-ES` puede introducir errores que la validación estructural
  no detecta → Mitigación: igual que con `pt-BR`, revisión manual por muestreo documentada en
  `tasks.md`, más el test de contrato de ids.
- [Riesgo] Olvidar añadir `es-ES` a `RealCatalogContractTest` deja el idioma sin cobertura de
  contrato → Mitigación: tarea explícita con el mismo patrón que `pt-BR`, revisada en `code-reviewer`.

## Migration Plan

Se publica como contenido nuevo en una versión de la app: el sembrado de `CatalogDatabase` detecta el
documento nuevo en el primer arranque tras actualizar (mismo mecanismo que `pt-BR`), sin migración de
Room ni pérdida de progreso de ningún idioma destino existente.
