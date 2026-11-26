# ADR-0028: Resembrado del catálogo por huella de contenido

- **Estado:** Propuesto
- **Fecha:** 2026-11-26
- **Sustituye a:** ADR-0019 (solo la regla "la siembra se repite cuando cambia el `schemaVersion` de un fichero")

## Contexto

ADR-0019 repite la siembra de un idioma cuando cambia el `schemaVersion` de su documento. `schemaVersion` describe el
formato y hoy solo se admite el valor 1, así que las frases añadidas o corregidas no llegan a una instalación existente.

ADR-0022 mantiene `catalog_seed_version` como marca por idioma; esta decisión cambia lo que guarda (la huella en lugar
de `schemaVersion`), no su papel.

## Decisión

Un idioma se vuelve a sembrar cuando cambia la huella SHA-256 del texto de su documento de frases. La app la calcula al
arrancar y la guarda en `CatalogDatabase` junto a las frases. `schemaVersion` queda solo como versión del formato.

Sin cambios en el resto: catálogo en Room, documentos revisables en diffs, siembra por idioma que no toca el progreso
del usuario y reglas de permanencia de ids.

## Consecuencias

- **Positivas:** ampliar o corregir contenido no exige ningún paso manual ni cambiar el formato de los documentos.
- **Negativas:** cualquier cambio de bytes resiembra el idioma; un cambio en el parseo sin cambio en los documentos
  exige subir la versión de `CatalogDatabase`.
- **Descartado:** `contentVersion` manual en cada documento: se puede olvidar y obliga a un test que lo vigile.
