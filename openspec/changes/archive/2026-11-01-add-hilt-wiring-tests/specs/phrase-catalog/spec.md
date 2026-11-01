## ADDED Requirements

### Requirement: Existe una única instancia de `CatalogRepository` en el grafo de la aplicación

En el grafo de la aplicación, toda obtención de `CatalogRepository` MUST devolver la misma instancia. La carga única del
asset la cubre `AssetsCatalogDataSourceTest` con contadores de lecturas, no este requisito.

#### Scenario: Dos obtenciones del catálogo desde el grafo de producción devuelven la misma instancia

- **GIVEN** la aplicación con su grafo de Hilt de producción (`PersistenceModule`)
- **WHEN** se obtiene `CatalogRepository` dos veces
- **THEN** ambas referencias son la misma instancia
