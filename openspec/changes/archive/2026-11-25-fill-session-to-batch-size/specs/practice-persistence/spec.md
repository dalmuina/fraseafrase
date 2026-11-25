## MODIFIED Requirements

### Requirement: Primer uso

Si la base de datos no tiene progreso ni tanda para un idioma destino, `ObtainActiveBatch` de ese
idioma destino MUST componer la primera tanda de ese idioma destino con las frases nuevas del nivel
elegido para ese idioma destino.

#### Scenario: Primer uso crea la primera tanda

- **GIVEN** una base de datos sin progreso ni tanda para el idioma destino `pt-PT`, un catálogo de
  `pt-PT` con quince frases A1 y el nivel elegido A1 con N igual a 20 para `pt-PT`
- **WHEN** se obtiene la tanda activa de `pt-PT`
- **THEN** el resultado es `Created` con las quince frases en orden de catálogo, todas de nivel 0
  (la primera tanda se llena hasta N o hasta agotar las frases del nivel, ADR-0026)

#### Scenario: El primer uso de un idioma destino no crea tanda para otro

- **GIVEN** `pt-PT` con progreso y tanda activa ya existentes, y `en-GB` sin progreso ni tanda
- **WHEN** se obtiene la tanda activa de `en-GB`
- **THEN** se compone la primera tanda de `en-GB` y la tanda activa de `pt-PT` no se modifica
