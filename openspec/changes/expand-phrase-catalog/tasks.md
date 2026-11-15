# Tasks

## 1. Preparación

- [x] 1.1 Guardar los 150 ids actuales de `phrases.json`, en orden, en un fichero temporal fuera del
      repo (no se commitea) para poder comprobar al final que siguen presentes y en el mismo orden
      (prefijo exacto de la lista final). Verificación: el fichero temporal contiene 150 líneas.

## 2. Nivel A1 (30 -> 200 frases)

- [x] 2.1 Añadir 170 frases nuevas de nivel A1 al final del bloque A1 en `phrases.json`, repartidas en
      varios `topic` (reutilizando los 8 actuales y añadiendo nuevos de vocabulario cotidiano), con ids
      `a1-<topic>-<secuencial>` que continúan la numeración existente por topic. Verificación:
      `./gradlew :core:data:test --tests "*Catalog*"` pasa y el nivel A1 tiene exactamente 200 frases.

## 3. Nivel A2 (30 -> 200 frases)

- [x] 3.1 Añadir 170 frases nuevas de nivel A2 al final del bloque A2, repartidas en varios `topic`, con
      ids `a2-<topic>-<secuencial>` continuando la numeración existente. Verificación:
      `./gradlew :core:data:test --tests "*Catalog*"` pasa y el nivel A2 tiene exactamente 200 frases.

## 4. Nivel B1 (30 -> 200 frases)

- [x] 4.1 Añadir 170 frases nuevas de nivel B1 al final del bloque B1, repartidas en varios `topic`, con
      ids `b1-<topic>-<secuencial>` continuando la numeración existente. Verificación:
      `./gradlew :core:data:test --tests "*Catalog*"` pasa y el nivel B1 tiene exactamente 200 frases.

## 5. Nivel B2 (30 -> 200 frases)

- [x] 5.1 Añadir 170 frases nuevas de nivel B2 al final del bloque B2, repartidas en varios `topic`, con
      ids `b2-<topic>-<secuencial>` continuando la numeración existente. Verificación:
      `./gradlew :core:data:test --tests "*Catalog*"` pasa y el nivel B2 tiene exactamente 200 frases.

## 6. Nivel C1 (30 -> 200 frases)

- [x] 6.1 Añadir 170 frases nuevas de nivel C1 al final del bloque C1, repartidas en varios `topic`, con
      ids `c1-<topic>-<secuencial>` continuando la numeración existente. Verificación:
      `./gradlew :core:data:test --tests "*Catalog*"` pasa y el nivel C1 tiene exactamente 200 frases.

## 7. Verificación final

- [x] 7.1 Comprobar que el catálogo final tiene 1000 frases (200 por nivel) y que los 150 ids guardados
      en la tarea 1.1 son un prefijo exacto de la lista `phrases` final, en el mismo orden (regla de
      permanencia de ADR-0013). Verificación: comparación del fichero temporal contra el catálogo final,
      sin diferencias de orden ni ids que falten.
- [x] 7.2 Ejecutar `./gradlew :core:data:check` en verde (incluye `AssetsCatalogDataSourceTest`,
      `CatalogJsonTest` y `CatalogContractTest` contra el catálogo ampliado).
