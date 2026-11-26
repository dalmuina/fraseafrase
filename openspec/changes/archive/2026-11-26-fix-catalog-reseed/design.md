# Design

## Context

Ver `proposal.md`. `RoomCatalogDataSource.seedDocument` lee el texto completo de cada documento en cada arranque,
obtiene su cabecera con `CatalogJson.headerOf` y solo lo parsea si `schemaVersion` difiere del guardado en
`catalog_seed_version`. `CatalogJson` rechaza cualquier `schemaVersion` distinto de 1.

## Goals / Non-Goals

**Goals:** que cualquier cambio de un documento llegue a una instalación existente sin pasos manuales.

**Non-Goals:** actualizar frase a frase, optimizar el arranque y tocar `PracticeDatabase`.

## Decisions

1. **Huella SHA-256 del texto del documento**, en hexadecimal, calculada con `java.security.MessageDigest` en
   `:core:data` (paquete `catalog`). Sin librerías nuevas.
   - Alternativa descartada (decisión del usuario): `contentVersion` escrito a mano en cada documento. Hay que subirlo
     en siete ficheros y necesita un test que detecte el olvido.
2. **`catalog_seed_version.content_hash` (TEXT) sustituye a `schema_version`.** La entidad mantiene su nombre.
   `schemaVersion` sigue validándose en `parsePhrases`; `CatalogHeader` pierde esa propiedad, que quedaría sin uso.
3. **`CatalogDatabase` 3 → 4 con la migración destructiva ya configurada.** Es una caché sembrada desde assets.
4. **La huella se guarda solo tras sembrar**, en la misma transacción que las frases (`replaceLanguage`). Un documento
   inválido conserva las filas anteriores y se vuelve a intentar en el siguiente arranque.

No hay puertos ni clases de dominio nuevos: `CatalogRepository` no cambia y sigue teniendo una única fuente
(`RoomCatalogDataSource`, ADR-0010).

**Tests:** unitarios JVM en `:core:data` con la base de test existente (`CatalogTestDatabase`) y documentos en memoria.
No hacen falta fakes nuevos.

## Risks / Trade-offs

- [Un cambio sin efecto en el contenido (espacios, fin de línea) provoca un resembrado] → Es inofensivo: 1000 filas por
  idioma, una vez.
- [Calcular siete huellas en cada arranque] → Son ~1,2 MB que ya se leen hoy. No se ha medido en dispositivo.
- [Un documento que retire un id dejaría progreso huérfano, que `ComposeNextBatch` rechaza con excepción] → Ahora los
  cambios de contenido sí llegan. Lo impide el contrato de permanencia de ids (ADR-0013), que solo protege los ids
  listados en `released-ids-*.txt`; A11 los completa antes de publicar.
  A2 (`renumber-phrase-ids`) lo provoca a propósito: tras A2 y A3 hay que desinstalar o borrar datos
  (`docs/plan-mvp.md`, avisos).
- [Cambiar el parseo sin cambiar los documentos no resiembra] → Subir la versión de `CatalogDatabase`.
