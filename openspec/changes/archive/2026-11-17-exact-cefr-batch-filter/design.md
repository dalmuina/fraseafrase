# Design

## Context

`ComposeNextBatch` (`:core:domain`, `core/domain/src/main/kotlin/com/dalmuina/fraseafrase/core/domain/spacedrepetition/ComposeNextBatch.kt`)
compone la tanda siguiente a partir del progreso guardado (`PhraseProgress`) y el catálogo
(`CatalogPhrase`, con un `CefrLevel` inmutable por frase). Hoy filtra así:

- `reinforceCandidates`: `it.dueAtSession <= currentSession && catalogById.getValue(it.id).level >= cefrLevel`
- `masteredCandidates`: nivel de consolidación ≥ 4, `dueAtSession > currentSession` y `catalogById.getValue(it.id).level >= cefrLevel`
- `newCandidates`: `it.level == cefrLevel` (ya exacto)

`PhraseProgress` no guarda el nivel CEFR: se obtiene siempre en vivo del catálogo (`catalogById.getValue(it.id).level`),
porque el CEFR de una frase del catálogo no cambia. El cambio de nivel CEFR en Ajustes dispara
`RegenerateActiveBatchForLevel` desde `ObtainActiveBatch` cuando detecta que los "ajustes aplicados"
difieren de los ajustes vigentes (ver proposal.md - Impact). Ver ADR-0021 para la motivación completa.

## Goals / Non-Goals

**Goals:**
- Que `reinforceCandidates` y `masteredCandidates` exijan `catalogById.getValue(it.id).level == cefrLevel`,
  igual que `newCandidates`.
- Mantener intacta cualquier otra regla de cuotas, reparto de huecos, `dueAtSession` o nivel de
  consolidación.

**Non-Goals:**
- No cambiar cómo se almacena `PhraseProgress` ni añadir un campo de nivel CEFR en el progreso: la
  comparación en vivo contra el catálogo ya es suficiente y evita duplicar un dato derivable.
- No cambiar `RegenerateActiveBatchForLevel` ni `ObtainActiveBatch`: siguen disparando la misma
  recomposición; solo cambia qué candidatos pasan el filtro dentro de `ComposeNextBatch`.
- No tocar la pantalla de ajustes ni añadir ningún aviso o mensaje nuevo sobre frases "ocultas" de
  otro nivel CEFR (fuera de alcance, ver proposal.md).

## Decisions

1. **Cambiar `>=` por `==` en las dos condiciones de filtro CEFR de `ComposeNextBatch`.**
   Alternativa descartada: añadir un parámetro o modo para elegir entre "estricto" y "con repaso
   superior" — se descarta por la misma razón que ADR-0021 descarta el ajuste explícito: aumenta la
   complejidad para un matiz que el uso real no pide.
2. **No tocar `PhraseProgress` ni persistencia.** El nivel CEFR de una frase ya es una propiedad
   inmutable del catálogo, no del progreso; seguir resolviéndolo por `catalogById.getValue(it.id).level`
   en el momento de componer la tanda es más simple que duplicarlo y mantiene una única fuente de
   verdad. Esto también es lo que garantiza, sin trabajo adicional, que el progreso de un nivel CEFR
   abandonado se conserve intacto y vuelva a ofrecerse al recuperar ese nivel (ver spec delta,
   requisito "Regeneración de la tanda activa por cambio de nivel CEFR sin resolución").
3. **Actualizar los tests existentes que verifican "igual o superior" en vez de añadir una bandera
   de compatibilidad.** El test `given a reinforce and mastered phrase of a higher level when
   composing then both enter` (`ComposeNextBatchTest`) deja de ser válido con el resultado que
   afirma hoy; se corrige para esperar la exclusión, tal como recoge el escenario actualizado "El
   filtro CEFR admite niveles iguales o superiores al elegido en reforzar y dominadas" en la spec
   delta.

## Risks / Trade-offs

- [Un usuario que dominaba un nivel alto y baja de nivel CEFR para practicar algo más fácil deja de
  ver esas frases dominadas como repaso ligero] → Mitigación: su progreso no se pierde (ADR-0021);
  basta con volver a elegir ese nivel CEFR para que reaparezcan exactamente igual. Riesgo aceptado
  explícitamente en ADR-0021 como consecuencia negativa conocida.
- [Cambio de comportamiento sin aviso en la UI] → Fuera de alcance de este cambio (ver proposal.md -
  Fuera de alcance); si se considera necesario explicarlo al usuario, sería un cambio posterior sobre
  `settings-screen`.

## Migración de datos

Ninguna. El filtro se recalcula en memoria a partir de datos ya existentes (catálogo y progreso);
no cambia ningún esquema de Room ni DataStore.
