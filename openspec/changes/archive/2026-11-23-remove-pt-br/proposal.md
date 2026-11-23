# Proposal

## Why

El producto enseña portugués europeo (`pt-PT`). La variante brasileña (`pt-BR`) se añadió como segundo idioma
destino con contenido propio (ADR-0019), pero duplica el catálogo, el selector de ajustes y la superficie de
pruebas sin aportar al objetivo del producto, y mezclar variantes confunde al estudiante (ADR-0003). Se retira
para simplificar antes de seguir evolucionando la selección de idiomas. La rama `1.0.0/backup-pt-br-catalog`
conserva el estado previo (catálogo y esquemas) por si se recupera más adelante.

## What Changes

- Se elimina el documento de catálogo `core/data/src/main/assets/catalog/phrases-pt-BR.json`: `pt-BR` deja de
  estar disponible como idioma destino.
- Se retira el icono y la etiqueta de `pt-BR` del design system y de los ajustes
  (`DsLanguageBadge`, `LanguageTagDsLanguageOptionMapping`, previews, drawable `ic_flag_pt_br`).
- Los escenarios de las specs que usaban `pt-BR` como "segundo idioma destino" (aislamiento de progreso, tanda,
  contador, ajustes, icono) pasan a usar otro idioma destino que sigue existiendo (`en-GB`; no `es-ES`, que es el origen por defecto y no puede ser destino); no cambia el
  comportamiento, solo el idioma de ejemplo.
- Los tests existentes que usan `pt-BR` como segundo idioma pasan a usar `en-GB`; el test de contrato del
  catálogo deja de esperar `pt-BR`.
- **Sin migración de datos de usuario** (decisión del usuario: no hay datos que conservar). Un idioma destino
  activo guardado que ya no esté disponible ya cae al primero disponible (requisito "Idioma destino activo"
  existente), así que no hace falta código de migración ni cambio de esquema de Room.
- No se crea ADR nuevo: ADR-0003 sigue vigente (modelo multivariante con etiquetas BCP-47; solo cambia qué
  idiomas se distribuyen). ADR-0009, 0012 y 0015 ya listaban `pt-BR` como fuera de alcance; ADR-0019 y ADR-0020
  están sustituidos y no se editan.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `phrase-catalog`: los escenarios de coexistencia de idiomas destino y de origen independiente del destino usan
  `en-GB` en lugar de `pt-BR`.
- `user-settings`: los escenarios de idioma destino activo, ajustes por idioma y cambio de idioma usan `en-GB`
  como segundo idioma destino.
- `settings-screen`: los escenarios de selección, activación inmediata y guardado por idioma usan `en-GB`.
- `practice-persistence`: los escenarios de aislamiento de progreso, tanda activa, posición y contador usan
  `en-GB` como segundo idioma destino.
- `progress-overview`: el escenario de refresco tras cambiar el idioma destino usa `en-GB`.
- `language-indicator`: los escenarios del icono del idioma activo usan `en-GB`.

## Impact

- Código de producción: `:core:data` (asset), `:core:designsystem` (`DsLanguageBadge`, drawable),
  `:feature:settings` (mapeo de etiquetas, previews). Sin cambios en `:core:domain`, `:app` ni esquemas de Room.
- Tests: ajustes de fixtures en `:core:data`, `:core:domain`, `:core:designsystem`, `:feature:session`,
  `:feature:settings` y `:feature:progress`.
- ADR afectados: ADR-0003 (sin contradicción, no se edita). Respeta el alcance del ADR-0012.
- Usuarios con `pt-BR` guardado: caen al primer idioma destino disponible (comportamiento ya especificado);
  su progreso `pt-BR` queda huérfano en la BD y se ignora.

## Fuera de alcance

- El segundo cambio (`device-language-by-base-language`: resolver el origen solo por idioma base) y su posible
  ADR, que sustituiría la regla de ADR-0023.
- Migración o borrado de filas `pt-BR` en Room y cambios de esquema.
- Añadir, quitar o reordenar otros idiomas del catálogo.
- Cambios de comportamiento de práctica, niveles o repetición espaciada (ADR-0012).
