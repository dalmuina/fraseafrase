# Proposal

## Why

Las frases añadidas o corregidas en una versión nueva de la app no llegan a quien ya la tiene instalada. Un idioma solo
se vuelve a sembrar cuando cambia su `schemaVersion`, y el catálogo solo admite el valor `1`. Hay que arreglarlo antes
de publicar la 1.0.0 y antes de ampliar el catálogo (tarea A1 de `docs/plan-mvp.md`).

## What Changes

- Un idioma se vuelve a sembrar cuando cambia el **contenido** de su documento de frases, detectado con una huella
  calculada por la app. Los documentos no cambian.
- `schemaVersion` queda solo como versión del formato; ya no decide el resembrado.
- La caché del catálogo guarda la huella en lugar de `schemaVersion` (`CatalogDatabase` pasa a la versión 4, destructiva).
- Un documento nuevo que no valida conserva lo sembrado antes.
- ADR-0028 (propuesto): sustituye la regla de resembrado por `schemaVersion` del ADR-0019.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `phrase-catalog`: el requisito "Persistencia del catálogo de frases en Room" se sustituye por "Persistencia y resembrado del
  catálogo en Room" (cambia cuándo se vuelve a sembrar un idioma).

## Impact

- Código: solo `:core:data` (`catalog/`) y sus tests. Esquema exportado `CatalogDatabase/4.json`.
- Datos del usuario: sin cambios; `PracticeDatabase` no se toca.
- ADR: ADR-0028 nuevo. Respeta ADR-0010, ADR-0023 y el alcance del MVP (ADR-0018).
- Sin dependencias nuevas.

## Fuera de alcance

- Volver las bases a la versión 1 y la política de migraciones (A3, `reset-database-v1`).
- Cambiar el formato de los ids (A2) o el contenido del catálogo.
- Borrar de la caché un idioma cuyo documento se retira de la app.
- Paquetes de frases (MVP2).
