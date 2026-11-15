# Proposal

## Why

El catálogo actual tiene 150 frases (30 por nivel CEFR), insuficiente para probar la app con continuidad:
un usuario agota pronto las frases nuevas de su nivel y la tanda deja de incorporar contenido fresco.
Antes de decidir el modelo de negocio (packs de pago, idiomas adicionales), hace falta un catálogo más
amplio en `pt-PT`/`es-ES` para poder probar el método de práctica con un volumen realista.

## What Changes

- Ampliar `core/data/src/main/assets/catalog/phrases.json` de 150 a 1000 frases: 200 por nivel CEFR
  (A1, A2, B1, B2, C1), manteniendo el esquema y las reglas vigentes de ADR-0013.
- Las 150 frases actuales conservan su `id` y su posición; las ~850 frases nuevas se añaden al final de
  la lista `phrases`, con ids nuevos que siguen el patrón existente (`<nivel>-<topic>-<secuencial>`).
- Diversificar los `topic` (texto libre, sin cambios de esquema) dentro de cada nivel para que las 200
  frases no se concentren en los 8 temas actuales por nivel.
- Sin cambios de código de producción: ni `CatalogPhrase`, ni `CatalogRepository`, ni los data sources,
  ni ningún caso de uso cambian de comportamiento. Es una ampliación de contenido sobre un esquema que
  ya la soporta (ADR-0013).

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

Ninguna. El esquema, los campos obligatorios, las reglas de permanencia de ids y el resto de requisitos
de `phrase-catalog` no cambian: solo crece el contenido que ya cubren. Este cambio declara
`skip_specs: true` en `.openspec.yaml`.

## Impact

- **Código:** ninguno. Solo se sustituye el asset `core/data/src/main/assets/catalog/phrases.json`.
- **Tests:** ninguno nuevo. Los tests existentes de `:core:data` (`AssetsCatalogDataSourceTest`,
  `CatalogJsonTest`, `CatalogContractTest`) ya verifican el formato y ejecutan contra el asset real; se
  vuelven a ejecutar contra el catálogo ampliado sin modificarlos.
- **Tamaño de la app:** 1000 frases de texto en JSON es un incremento despreciable (unos cientos de KB).
- **Memoria en tiempo de ejecución:** ADR-0013 ya advierte que la carga en memoria del catálogo es
  "despreciable" para el tamaño previsto del MVP; 1000 frases de texto corto sigue siendo un volumen
  pequeño (no se espera impacto medible).
- **Progreso de usuario en Room:** no se toca. Como se acordó, no es necesario preservar ni migrar el
  estado de práctica en esta fase de debug; cualquier progreso existente en un dispositivo de desarrollo
  puede perderse al reinstalar sin que sea un problema para este cambio.

## Fuera de alcance

- Cualquier concepto nuevo de categoría o pack de frases ligado al futuro modelo de negocio (descarga
  con N frases preinstaladas + packs de pago). El modelo de negocio todavía está en marcha y no se
  decide en este cambio; se usa el campo `topic` (texto libre) ya existente.
- Traducción del catálogo a otros idiomas además de `pt-PT`/`es-ES`.
- Herramientas de autoría o validación adicionales más allá de los tests de contrato ya existentes
  (`CatalogContractTest`, `CatalogJsonTest`).
- Cualquier cambio de ADR: este cambio no contradice ni amplía ADR-0003, ADR-0012 ni ADR-0013; respeta
  el alcance del ADR-0012 (catálogo precargado, clasificado por nivel) y las reglas de permanencia de
  ids de ADR-0013.
