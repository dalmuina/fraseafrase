## ADDED Requirements

### Requirement: Existe una única instancia de `PracticeDatabase` en el grafo de la aplicación

En el grafo de la aplicación, toda obtención de `PracticeDatabase` MUST devolver la misma instancia.

#### Scenario: Dos obtenciones de la base de datos desde el grafo de producción devuelven la misma instancia

- **GIVEN** la aplicación con su grafo de Hilt de producción (`PersistenceModule`)
- **WHEN** se obtiene `PracticeDatabase` dos veces
- **THEN** ambas referencias son la misma instancia
