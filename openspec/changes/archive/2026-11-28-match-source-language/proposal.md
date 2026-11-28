## Why

El idioma de origen solo se detecta si la etiqueta del dispositivo coincide exactamente con un idioma del catálogo
(`ResolveSourceLocale` compara la etiqueta entera). Un móvil en `en-US`, `es-MX` o `pt-BR` ve las frases en `es-ES`
aunque el catálogo tenga `en-GB`, `es-ES` o `pt-PT`. Además solo se mira el primer idioma del dispositivo, mientras
que Android elige los textos con toda la lista: el cambio `localize-interface` (A8) necesita que ambos criterios
coincidan. Es la decisión D2 de `docs/plan-mvp.md` (fila A6) y la decisión del 2026-11-28 sobre la lista de idiomas.

## What Changes

- El idioma de origen se resuelve recorriendo **la lista ordenada de idiomas del dispositivo**. Para cada idioma,
  en orden: coincidencia exacta con un idioma disponible y, si no, uno disponible con el mismo idioma base
  (subetiqueta de idioma: `en-US` → `en-GB`, `es-MX` → `es-ES`, `pt-BR` → `pt-PT`). El primero que case gana. Si
  ninguno casa, el idioma de origen por defecto (`es-ES`, inyectado desde `:app`).
- Si varios idiomas disponibles comparten el idioma base, gana el primero por orden alfabético de etiqueta (regla
  determinista; hoy el catálogo no tiene dos variantes del mismo idioma).
- **BREAKING (API interna):** el puerto `DeviceLocaleProvider` pasa de `current(): LanguageTag` a
  `preferred(): List<LanguageTag>`. `AndroidDeviceLocaleProvider` lee `LocaleList.getDefault()`. Se actualizan el
  fake de `:core:testing` y los objetos anónimos de los tests instrumentados.
- **ADR-0032** (nuevo): sustituye la regla "Idioma de origen" de ADR-0023; el resto de ADR-0023 sigue vigente.

## Capabilities

### New Capabilities

_Ninguna._

### Modified Capabilities

- `phrase-catalog`: cambia el requisito "Idioma de origen según el dispositivo" (lista de idiomas y coincidencia
  por idioma base).

## ADR afectados y alcance

- **ADR-0023:** se sustituye solo su regla "Idioma de origen"; texto de origen desde frases, ids paralelos y
  "origen ≠ destino" no cambian. Su estado pasa a `Sustituido por ADR-0032` en la parte correspondiente, según
  `docs/adr/README.md`.
- **ADR-0020:** ya sustituido por ADR-0023; el ancla "origen ≠ destino" (el destino se ajusta al origen) sigue
  igual, y no se reintroduce ninguna dependencia del origen respecto al destino.
- **ADR-0003:** el dominio sigue sin idiomas fijos; el valor por defecto se inyecta desde `:app`.
- **ADR-0010:** el puerto sigue en `:core:domain` y su implementación en `:core:platform`.
- **ADR-0018:** dentro del alcance del MVP (fila A6 de la MVP1); no añade pantallas ni ajustes.

## Fuera de alcance

- Elegir o guardar el idioma de origen en Ajustes (descartado en ADR-0023).
- Traducir la interfaz y escuchar los cambios de idioma de la app (`localize-interface`, A8).
- Añadir catálogos nuevos (p. ej. `pt-BR`) o cambiar el valor por defecto `es-ES`.
- Coincidencia por script o variantes (`zh-Hans`, `sr-Latn`): solo se compara la subetiqueta de idioma.

## Impact

- `:core:domain`: `ResolveSourceLocale`, `ObtainSourceLanguage`, `DeviceLocaleProvider`, `LanguageTag` (idioma base).
- `:core:platform`: `AndroidDeviceLocaleProvider`.
- `:core:testing`: `FakeDeviceLocaleProvider`.
- Tests que construyen el fake o implementan el puerto: `:core:domain`, `:feature:session`, `:feature:settings`,
  `:feature:progress` y los instrumentados de `:feature:session`, `:feature:settings` y `:app`.
- `docs/adr/0032-...md` nuevo, línea de estado de ADR-0023 e índice `docs/adr/README.md`.
- Sin dependencias nuevas ni cambios de persistencia.
