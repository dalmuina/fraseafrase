# Proposal

## Why

El catálogo de frases (`core/data/src/main/assets/catalog/phrases.json`) solo tiene 2 frases, ambas
de nivel A1. Con ese contenido no se puede probar de forma realista el modelo de tandas del
ADR-0012: no hay frases suficientes para formar una tanda (N entre 10 y 30, por defecto 20), ni para
observar niveles de consolidación, racha, certificación, repetición espaciada o arrastre entre
tandas en los demás niveles CEFR. Hace falta ampliar el contenido del catálogo para poder probar el
MVP de principio a fin.

## What Changes

- Añadir ~148 frases nuevas al final de la lista `phrases` de
  `core/data/src/main/assets/catalog/phrases.json`, hasta alcanzar ≈30 frases por nivel
  (A1, A2, B1, B2, C1 — no existe C2), con variedad de temas por nivel (saludos, comida, familia,
  trabajo, viajes, tiempo/clima, compras, ocio, salud, casa, etc.).
- Cada frase nueva sigue el esquema ya vigente (`id`, `level`, `topic`, `target` en pt-PT, `source`
  en es-ES), con ids nuevos, únicos y no reutilizados, añadidos siempre al final de la lista.
- No se modifica `released-ids.txt`, el esquema del catálogo, ni ningún código Kotlin
  (`CatalogJson`, `CatalogPhrase`, `CefrLevel`, `Topic`, etc.).

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

Ninguna. El comportamiento y las reglas del catálogo (formato, locale, unicidad y permanencia de
ids) ya están definidos en `openspec/specs/phrase-catalog/spec.md` y no cambian: este cambio solo
añade contenido de datos que cumple esas reglas ya existentes. No se especifica en ningún requisito
el número de frases por nivel, así que no hay delta de spec que escribir (`skip_specs: true`).

## Impact

- **Archivo afectado:** `core/data/src/main/assets/catalog/phrases.json` (único archivo que cambia).
- **ADR relevantes:** ADR-0013 (catálogo JSON en assets; id permanente y append-only — este cambio
  respeta ambas reglas) y ADR-0012 (alcance del MVP; este cambio no amplía el alcance, solo rellena
  el catálogo dentro de los niveles A1-C1 ya definidos).
- **Tests existentes afectados:** `RealCatalogContractTest` (`core/data`) debe seguir pasando con el
  catálogo ampliado; no se modifica ningún test, solo se verifica en verde.

## Fuera de alcance

- Cambiar el esquema del catálogo o añadir campos nuevos (audio, dificultad, etc.).
- Tocar `released-ids.txt`.
- Cualquier cambio de código en `:core:domain`, `:core:data` o features.
