# Design

## Context

Ver `proposal.md` (Why). Estado actual:

- `ResolveSourceLocale(device, default, available)` (`:core:domain`, objeto puro) devuelve `device` si está en
  `available` (igualdad de etiqueta) y si no `default`.
- `ObtainSourceLanguage` es su único llamador: le pasa `DeviceLocaleProvider.current()`, `DefaultSourceLocale.value`
  (inyectado desde `:app`, hoy `es-ES`) y `CatalogRepository.availableTargetLocales()`.
- `DeviceLocaleProvider` (puerto en `:core:domain`, una fuente) lo implementa `AndroidDeviceLocaleProvider` en
  `:core:platform` con `Locale.getDefault()`, a través de un seam `locale: () -> Locale` para el test JVM.
- `FakeDeviceLocaleProvider(var locale)` en `:core:testing` lo usan fixtures de `:core:domain`, `:feature:session`,
  `:feature:settings` y `:feature:progress`; tres tests reasignan `device.locale` para simular un cambio de idioma.
  Dos tests instrumentados implementan el puerto: el objeto `TestDeviceLocaleProvider` de `PracticeSessionRouteTest`
  y un objeto anónimo en `SettingsRouteTest`; `PersistenceWiringTest` (`:app`) solo lo inyecta.
- `availableTargetLocales()` sale de `SELECT DISTINCT` sin `ORDER BY`: su orden no está garantizado.
- El catálogo empaquetado tiene `de-DE`, `en-GB`, `es-ES`, `fr-FR`, `it-IT`, `pt-PT` y `ru-RU`: una variante por
  idioma base.

## Goals / Non-Goals

**Goals:**
- La regla del requisito "Idioma de origen según el dispositivo" (delta de `phrase-catalog`) vive entera en
  `ResolveSourceLocale`, función pura y testeable sin Android.
- El puerto expone la lista de idiomas del dispositivo en el orden de preferencia del sistema.

**Non-Goals:**
- Reaccionar a cambios de idioma en caliente: el origen ya se recalcula en cada resolución (escenario "Un cambio
  del idioma del dispositivo cambia el origen"), y eso no cambia.
- Ordenar `availableTargetLocales()` en `:core:data`: el desempate no depende de ese orden.

## Decisions

### D1. Puerto `DeviceLocaleProvider.preferred(): List<LanguageTag>` en lugar de `current()`

Se sustituye el método (no se añade uno al lado): con dos métodos, un llamador podría seguir usando solo el
primero y reabrir la discrepancia con los textos de Android (A8). `current()` solo lo llama `ObtainSourceLanguage`.
Sigue siendo una única fuente, así que la implementación conserva su nombre `AndroidDeviceLocaleProvider` (ADR-0010).

- *Alternativa descartada:* que el puerto devuelva el idioma ya resuelto. Mueve la regla de dominio a
  `:core:platform` y obliga a pasarle los idiomas disponibles.

### D2. `AndroidDeviceLocaleProvider` lee `LocaleList.getDefault()`

`LocaleList` existe desde API 24 (minSdk 26). El seam pasa a `locales: () -> List<Locale>`; el constructor
`@Inject` convierte `LocaleList.getDefault()` en lista conservando el orden. El test JVM de `:core:platform` usa el
seam y no toca `LocaleList` (clase Android no disponible en tests JVM). Cada `Locale` se convierte con
`toLanguageTag()`, como hoy; las etiquetas que no cumplen `LanguageTag.isWellFormed` se descartan de la lista, para
que ningún idioma del sistema pueda cerrar la app al construir `LanguageTag` (requisito: resolver no lanza error).

- *Alternativa descartada:* `Resources.getSystem().configuration.locales`. Mismo resultado, pero necesita
  `Resources` y complica el seam sin ganancia.

### D3. Idioma base como propiedad de `LanguageTag`

`LanguageTag.language: String` devuelve la subetiqueta de idioma (lo anterior al primer `-`): `en-US` → `en`,
`en` → `en`. La forma ya validada por `LanguageTag` (idioma en minúsculas de 2-3 letras) hace la comparación
directa, sin normalizar mayúsculas. Vive en `:core:domain` sin dependencias.

- *Alternativa descartada:* `java.util.Locale` dentro del dominio. Funciona en JVM, pero el dominio trata los
  idiomas como etiquetas propias (ADR-0003) y no necesita más que la subetiqueta.

### D4. Algoritmo de `ResolveSourceLocale`

Firma: `invoke(device: List<LanguageTag>, default: LanguageTag, available: List<LanguageTag>): LanguageTag`.
Recorre `device` en orden; para cada etiqueta devuelve la de `available` idéntica o, si no hay, la de
`available` con el mismo `language` de menor `value` alfabético. Si ninguna casa, `default`. Una lista vacía da
`default`. Sigue sin lanzar errores.

- *Desempate alfabético:* `available` no tiene orden garantizado (ver Context), así que "el primero de la lista"
  no sería determinista. Hoy no hay dos variantes del mismo idioma; la regla solo fija el resultado si un día las hay.
- *Alternativa descartada:* dos pasadas (primero exacta en toda la lista, luego por idioma base). Con `[en-US,
  es-ES]` daría `es-ES`, contra la decisión del 2026-11-28 de seguir el orden del usuario como Android.

### D5. `FakeDeviceLocaleProvider(var locales: List<LanguageTag>)`

Constructor secundario `FakeDeviceLocaleProvider(locale: LanguageTag)` y propiedad `var locale` (lee el primero,
escribe una lista de uno) para que las fixtures y los tres tests que simulan un cambio de idioma de
`:feature:session`, `:feature:settings` y `:feature:progress` no cambien. El objeto `TestDeviceLocaleProvider` de
`PracticeSessionRouteTest` y el objeto anónimo de `SettingsRouteTest` pasan a `preferred() = listOf(LanguageTag("es-ES"))`.

### Módulos y dependencias (ADR-0010)

Ninguna clase nueva. Todo queda en su módulo actual: `LanguageTag`, `ResolveSourceLocale`, `ObtainSourceLanguage`
y `DeviceLocaleProvider` en `:core:domain`; `AndroidDeviceLocaleProvider` en `:core:platform`; el fake en
`:core:testing`. Sin dependencias ni librerías nuevas, y sin cambios en `:app` (el binding de Hilt no cambia).

### Estrategia de test

- `LanguageTagTest` y `ResolveSourceLocaleTest` (`:core:domain`, unitarios): todos los escenarios del delta salvo
  los dos de `ObtainSourceLanguage`.
- `ObtainSourceLanguageTest`: el caso de uso pasa la lista del puerto entera (escenario "Se usa el siguiente idioma
  de la lista si el primero no casa") y los escenarios existentes de independencia del destino y cambio de idioma.
- `AndroidDeviceLocaleProviderTest` (`:core:platform`, JVM con el seam): conserva el orden de la lista.
- Fakes: solo `FakeDeviceLocaleProvider`.

## Risks / Trade-offs

- [El idioma base no distingue scripts (`sr-Latn` frente a `sr-Cyrl`, `zh-Hans` frente a `zh-Hant`)] → Ningún
  idioma con scripts distintos está en el catálogo; queda fuera de alcance en la propuesta.
- [Si A8 aplica un idioma por app, `LocaleList.getDefault()` podría devolver ese idioma y no los del sistema] →
  Es lo que se quiere (interfaz y origen alineados); `localize-interface` lo revisará al retomarse contra esta regla.
- [Cambiar el puerto rompe la compilación de varios módulos a la vez] → El grupo 2 de `tasks.md` reúne todos los
  puntos afectados en un único commit.

## Migration Plan

Sin datos persistidos: el origen no se guarda. Al actualizar, el origen puede cambiar (p. ej. de `es-ES` a `en-GB`
en un móvil `en-US`), y con él el texto de origen mostrado; el progreso es por idioma destino y no se ve afectado.
Si el origen nuevo coincide con el destino guardado, se aplica la regla "origen ≠ destino" de ADR-0023, ya vigente.
Rollback: revertir los commits.
