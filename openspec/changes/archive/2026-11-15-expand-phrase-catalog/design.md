# Design

## Context

Ver proposal.md - Why. El catálogo es un único JSON en `core/data/src/main/assets/catalog/phrases.json`
(ADR-0013), validado al cargarse por `AssetsCatalogDataSource` contra las reglas de la spec
`phrase-catalog` (formato, locales heredados, campos obligatorios, ids únicos). No hay base de datos ni
migraciones que afecten al catálogo: es un asset estático, cargado en memoria.

Hoy: 150 frases, 30 por nivel CEFR, 8 `topic` por nivel, ids con el patrón
`<nivel-minúsculas>-<topic>-<secuencial de 3 cifras>` (p. ej. `a1-saludos-001`).

## Goals / Non-Goals

**Goals:**
- Llegar a 1000 frases (200 por nivel CEFR: A1, A2, B1, B2, C1).
- Mantener los 150 ids actuales sin cambios, en su posición actual.
- Repartir las frases nuevas de cada nivel entre varios `topic` para que la práctica no se sienta
  repetitiva ni desequilibrada.

**Non-Goals:**
- No se introduce ningún concepto nuevo de categoría, pack o agrupación de monetización (proposal.md -
  Fuera de alcance).
- No se cambia el esquema del catálogo (`CatalogPhrase`, `LanguageTag`, `Topic`, `CefrLevel` quedan
  igual) ni el pipeline de carga.
- No se añade tooling de autoría nuevo: la validación existente (`AssetsCatalogDataSourceTest`,
  `CatalogJsonTest`) ya cubre duplicados, campos vacíos y nivel inválido.

## Decisions

**Esquema de ids para las frases nuevas.** Se sigue el patrón existente
`<nivel>-<topic>-<secuencial>`, con el secuencial continuando por `topic` donde ya hay frases (p. ej. si
`a1-saludos` llega hasta `003`, la siguiente es `a1-saludos-004`) y empezando en `001` para un `topic`
nuevo dentro de un nivel. Alternativa descartada: un contador global por nivel (`a1-0151`...) - se
descarta porque pierde la trazabilidad por tema que ya tiene el catálogo actual y complica revisar el
contenido por bloques temáticos.

**Diversificación de `topic` por nivel.** Cada nivel pasa de 8 a aproximadamente 16-20 `topic`,
reutilizando los temas actuales (p. ej. `trabajo`, `viajes`) y añadiendo nuevos coherentes con el nivel
CEFR (p. ej. B2/C1 pueden cubrir temas más abstractos; A1/A2 se mantienen en vocabulario cotidiano). El
número exacto de temas y su reparto se decide al redactar el contenido, no aquí: es un detalle de
contenido, no una decisión de arquitectura.

**Orden de la lista `phrases`.** Se mantiene el agrupamiento actual por nivel (todas las A1 antes que
las A2, etc.) y dentro de cada nivel, por `topic`. Las ~850 frases nuevas se insertan de forma que las
150 actuales queden como prefijo exacto de la lista final (lo exige la regla de permanencia de ids); el
resto del contenido nuevo se añade después, agrupado por nivel y tema para facilidad de revisión.

**Generación del contenido.** Las frases se redactan como parte de este cambio (sin pipeline externo,
según la respuesta ya dada). Cada frase pt-PT con su traducción es-ES se revisa a mano antes de
incluirse, como exige ADR-0013 ("el catálogo se elabora fuera de la app y se revisa manualmente").

## Risks / Trade-offs

- [Calidad del contenido a este volumen] → Redactar en bloques por nivel/tema y revisar cada bloque
  antes de pasar al siguiente, en vez de generar las 1000 frases de una sola pasada sin revisión.
- [Un id nuevo choca por error con uno existente o se reutiliza un hueco] → El test de contrato
  (`CatalogContractTest`) y `AssetsCatalogDataSourceTest` ya fallan ante duplicados; se ejecutan antes de
  cerrar el cambio.
- [Fatiga de temas repetidos dentro de un nivel con 200 frases] → Mitigado por la diversificación de
  `topic` de la decisión anterior.
