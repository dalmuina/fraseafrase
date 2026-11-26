# Tasks

## 1. :core:data — resembrado por contenido

- [ ] 1.1 Test en `RoomCatalogDataSourceTest`: con `pt-PT` y `es-ES` ya sembrados, documentos con una frase más y el
  mismo `schemaVersion` devuelven las dos frases en orden. Verificar que falla porque solo devuelve la primera.
  (Scenario: Una frase añadida llega a una instalación ya sembrada)
- [ ] 1.2 Guardar y comparar la huella SHA-256: `content_hash` en `CatalogSeedVersionEntity`, `CatalogDatabase` versión
  4 y `RoomCatalogDataSource`; adaptar los tests que usan `schemaVersion` (`CatalogDaoTest`, `CatalogDatabaseTest`,
  `RoomCatalogDataSourceTest`), actualizar en ellos las referencias de escenario al requisito "Persistencia y
  resembrado del catálogo en Room" y renombrar el test `the latest schemaVersion wins` a
  `the latest content hash wins`. Verificar que 1.1 pasa. (Scenarios: Una frase añadida…; Primer arranque siembra el
  catálogo)
- [ ] 1.3 Test: un texto corregido en `pt-PT` sustituye al anterior y `it-IT` no se vuelve a sembrar (borrar a mano
  las filas de `it-IT` antes del segundo arranque y comprobar que siguen vacías); verificar con una mutación (resembrar
  siempre) que el test detecta el fallo. (Scenario: Un texto corregido sustituye al anterior sin afectar a otros
  idiomas)
- [ ] 1.4 Test con `CatalogDatabase` y `PracticeDatabase` de test: guardar progreso (nivel 2, 1 acierto), resembrar
  `pt-PT` con una frase más mediante `RoomCatalogDataSource` y comprobar que el progreso no cambia; verificar con una
  mutación (alterar el progreso guardado antes de la comprobación) que el test detecta el fallo. (Scenario: Resembrar un
  idioma no cambia el progreso guardado)
- [ ] 1.5 Adaptar el test de "sin cambio" a documentos idénticos y añadir el de documento nuevo inválido; ambos en
  verde. Verificar el segundo con una mutación (borrar las filas del idioma antes de validar el documento). (Scenarios:
  Sin cambio de contenido no se vuelve a sembrar; Un documento nuevo inválido conserva lo sembrado)
- [ ] 1.6 Quitar `schemaVersion` de `CatalogHeader`, actualizar los KDoc afectados y versionar el esquema exportado
  `CatalogDatabase/4.json`. Verificar con `./gradlew :core:data:check`.

## 2. Documentación (sesión principal)

- [ ] 2.1 Aceptar ADR-0028: estado en el ADR y en `docs/adr/README.md`; línea de estado de ADR-0019: "Sustituido por
  ADR-0022 (formato de documento), ADR-0023 (regla de origen) y ADR-0028 (regla de resembrado)"; su fila del índice:
  "Sustituido por 0022, 0023 y 0028". Verificar que el índice y los ficheros coinciden.

## 3. Verificación (sesión principal)

- [ ] 3.1 `./gradlew check` y `./gradlew koverVerify` en verde.
