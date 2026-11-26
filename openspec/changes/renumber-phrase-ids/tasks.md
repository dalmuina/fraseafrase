# Tasks

## 1. :core:data: reglas del contrato de ids (tdd-implementer)

Ficheros: `core/data/src/test/kotlin/com/dalmuina/fraseafrase/core/data/catalog/CatalogContract.kt` y
`CatalogContractTest.kt`. Diseño: D1. Datos sintéticos, sin leer los assets.

- [x] 1.1 Tests en `CatalogContractTest` para `idFormatViolations`, con registro de bloques `{0000}` y temas
  `{saludos}`: `0000-a1-saludos-001` no devuelve nada; `a1-saludos-001` y `0000-a1-saludos-000` devuelven `MALFORMED`;
  `1000-a1-saludos-001` devuelve `UNKNOWN_BLOCK`; `0000-a1-viajes-001` devuelve `UNKNOWN_TOPIC`. Comprobar que fallan
  porque la función no existe. (Scenarios: Un id con formato válido cumple el contrato; Un id sin bloque incumple el
  contrato; Un id con número 000 incumple el contrato; Un bloque fuera del registro incumple el contrato; Un tema
  fuera de la lista incumple el contrato)
- [x] 1.2 Implementar `IdViolation`, `IdViolationReason` e `idFormatViolations` en `CatalogContract`, con la expresión
  de D1, y actualizar el KDoc del objeto para citar ADR-0029. Verificar que los tests de 1.1 pasan.
- [x] 1.3 Tests en `CatalogContractTest` para `birthLabelMismatches`, con `TargetPhrase` construidas en el test:
  `0000-a1-saludos-003` de nivel A2 y no publicada se devuelve; la misma frase de nivel A1 y tema `comida` se devuelve;
  `0000-a1-saludos-001` de nivel A2 y tema `comida`, publicada, no se devuelve. Comprobar que fallan porque la función
  no existe. (Scenarios: Una frase nueva cuyo nivel no coincide con su id incumple el contrato; Una frase nueva cuyo
  tema no coincide con su id incumple el contrato; Una frase publicada reclasificada conserva su id)
- [x] 1.4 Implementar `birthLabelMismatches` en `CatalogContract`. Verificar que los tests de 1.3 pasan y que
  `./gradlew :core:data:check` está en verde.

## 2. :core:data: registros y renombrado de los assets (sesión principal, contenido)

Sin `tdd-implementer`. No se leen los assets enteros. Diseño: D2, D3 y D4. El script vive fuera del repositorio.
Nota: el grupo incluye los dos tests del contrato sobre los assets reales, porque su rojo depende de los ids antiguos y
su verde, del renombrado.

- [x] 2.1 Generar con un script `core/data/src/test/resources/catalog/topics.txt`, con los temas distintos de
  `phrases-pt-PT.json` en orden alfabético, y crear `blocks.txt` con `0000`. Verificar que `topics.txt` tiene 114
  líneas y que los temas de los otros 6 assets son los mismos.
- [x] 2.2 En `RealCatalogContractTest`, añadir dos tests parametrizados por los 7 idiomas. El primero aplica
  `idFormatViolations` con los dos registros y el segundo, `birthLabelMismatches` con `released-ids-<locale>.txt`.
  Actualizar el comentario de cabecera de la clase para que cite también ADR-0029. Comprobar que el primero falla
  con `MALFORMED` en los ids antiguos. (Scenarios: Los documentos empaquetados cumplen
  el formato; Las frases empaquetadas no publicadas coinciden con su id)
- [x] 2.3 Renombrar los ids de los 7 assets con el script de D3, que antepone `0000-` y exige 1000 sustituciones por
  fichero. Verificar que `git diff --stat` muestra 1000 líneas cambiadas por asset y que un Grep de
  `"id": "[abc][12]-` sobre los assets no devuelve nada.
- [x] 2.4 Cambiar los 2 ids de `released-ids-pt-PT.txt` a `0000-a1-saludos-001` y `0000-a1-comida-001`. Verificar que
  todo `RealCatalogContractTest` pasa (formato, etiqueta, paralelismo y prefijo publicado) con
  `./gradlew :core:data:check`.

## 3. :core:data: ids literales en los tests (tdd-implementer, haiku)

- [x] 3.1 Cambiar los ids literales de formato antiguo por su versión con `0000-` en `CatalogDaoTest`,
  `CatalogDatabaseTest`, `CatalogJsonTest`, `RoomCatalogDataSourceTest` y `RoomPracticeDataSourceReadTest`. No se cambia
  ninguna aserción más. Verificar con un Grep de `"[abc][12]-[a-z_]+-[0-9]{3}` en `core/data/src/test`, que solo debe
  devolver los ids sin bloque que `CatalogContractTest` usa a propósito (caso `MALFORMED`), y con `./gradlew :core:data:check`. (Scenarios: El origen sale del texto destino del otro idioma; Ids
  duplicados rechazan la carga; Resembrar un idioma no cambia el progreso guardado; El progreso de dos idiomas destino
  para la misma frase no se mezcla)

## 4. :core:domain: ids literales en los tests (tdd-implementer, haiku)

- [x] 4.1 Cambiar los ids literales de formato antiguo por su versión con `0000-` en `CatalogPhraseTest` y
  `PracticeRepositoryTest`. Verificar con el mismo Grep en `core/domain/src/test` y con `./gradlew :core:domain:check`. (Scenarios: Campos obligatorios de cada frase, en
  `CatalogPhraseTest`; El progreso de dos idiomas destino para la misma frase no se mezcla, en `PracticeRepositoryTest`)

## 5. Documentación (sesión principal)

- [ ] 5.1 Aceptar ADR-0029: cambiar su estado a `Aceptado` en el ADR y en `docs/adr/README.md`. Verificar que el índice
  y el fichero coinciden.
- [ ] 5.2 `CLAUDE.md`: añadir "Ids de frases: ADR-0029" a la regla de oro 4. En "No hacer", ampliar la regla de ids
  para que cite el formato `<bloque>-<nivel>-<tema>-<nnn>`, el registro de bloques (`blocks.txt`) y la lista de temas
  (`topics.txt`) de `core/data/src/test/resources/catalog/`, además de ADR-0013 y ADR-0029. Verificar que las rutas
  citadas existen.

## 6. Verificación (sesión principal)

- [ ] 6.1 `./gradlew check` y `./gradlew koverVerify` en verde.

## Workflow follow-up

- Tras la PR: borrar los datos de la app (o desinstalarla) en los dispositivos de desarrollo, porque el progreso
  guardado usa los ids antiguos. A3 (`reset-database-v1`) va justo después.
- Code review, verify y archive según `CLAUDE.md`.
