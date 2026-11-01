# Proposal

## Why

El grafo de Hilt liga `CatalogRepository` a `AssetsCatalogDataSource` y `PracticeDatabase` a un único `@Provides`.
Ambas garantías (una sola instancia del catálogo y una sola base de datos Room por proceso) dependen de anotaciones
`@Singleton` que hoy no comprueba ningún test. La revisión de `add-practice-session` encontró que faltaba `@Singleton` en
`AssetsCatalogDataSource` y ningún test lo detectó. Ahora que el runner instrumentado funciona
(`fix-instrumented-test-runner`), un test de cableado es viable. Las dos garantías son consistentes con ADR-0013
(catálogo cargado en memoria, estado del usuario en Room), pero hoy no están formalizadas en `openspec/specs/`: este
cambio las añade como requisitos verificables.

## What Changes

- Añadir requisitos a `phrase-catalog` y `practice-persistence` (ver Capabilities) cuyo escenario observable es que dos
  obtenciones desde el grafo de producción devuelven la misma instancia.
- Añadir un test instrumentado de cableado en `:app/src/androidTest` que cubre esos escenarios con `assertSame` de JUnit 4
  (sin dependencia nueva de aserciones), usando el grafo real de `PersistenceModule` y la base de datos real, sin
  `@TestInstallIn`.
- Añadir un runner propio de Hilt (que devuelve `HiltTestApplication`) solo en `:app`, sobrescribiendo el runner que fija
  el convention plugin en el `defaultConfig` de `:app`. El convention plugin y el runner de los demás módulos no cambian.
- Añadir `hilt-android-testing` a `androidTestImplementation` de `:app` y `hilt-compiler` a `kspAndroidTest`, ambos en la
  versión de Hilt ya fijada en `gradle/libs.versions.toml`.
- Crear ADR-0014 (estado Propuesto) que sustituye por completo al ADR-0007. Repite la estrategia vigente y fija:
  JUnit 6 (Jupiter) para tests JVM y JUnit 4 para tests instrumentados. ADR-0007 nombra JUnit 5; el proyecto usa JUnit 6
  (`junit = "6.1.3"`), que mantiene la API de Jupiter y el mismo modelo de programación, y este ADR lo formaliza. JUnit 4
  para instrumentados se mantiene porque `AndroidJUnitRunner`, `createComposeRule` y `HiltAndroidRule` lo requieren.
  La tarea 5.2 pasa 0014 a Aceptado y 0007 a `Sustituido por ADR-0014`, y actualiza la tabla de `docs/adr/README.md`,
  después de la revisión de código y de `/opsx:verify` (tarea 5.1).
- Corregir `docs/deuda-tecnica.md`: el título y la entrada de tests de cableado de Hilt pasan a referirse a
  `CatalogRepository` y `PracticeDatabase`, y dejan de pedir identidad de DataStore (DataStore no es un binding de Hilt;
  su instancia única la garantiza una sola declaración del delegado `preferencesDataStore`). Se registra el siguiente
  cambio de infraestructura: un job de tests instrumentados con emulador en la CI, y los tests de UI de `:feature:session`, que nunca han pasado por detekt, la inclusión de `src/androidTest` en
  detekt si la tarea 3.1 lo confirma, y una deuda nueva para revisar el runner genérico que el convention plugin aplica a
  módulos sin tests instrumentados.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `phrase-catalog`: añade el requisito "Existe una única instancia de `CatalogRepository` en el grafo de la aplicación", con el escenario
  "Dos obtenciones del catálogo desde el grafo de producción devuelven la misma instancia". Ver
  `specs/phrase-catalog/spec.md`.
- `practice-persistence`: añade el requisito "Existe una única instancia de `PracticeDatabase` en el grafo de la aplicación", con el
  escenario "Dos obtenciones de la base de datos desde el grafo de producción devuelven la misma instancia". Ver
  `specs/practice-persistence/spec.md`.

Estos requisitos formalizan garantías consistentes con ADR-0013. No cambian ningún comportamiento de producto existente.

## Impact

- **Código:** `app/build.gradle.kts` (dependencias y runner de tests), nuevo `app/src/androidTest` con el test de cableado y
  el runner. Ningún cambio en `src/main` ni en el convention plugin.
- **Dependencias:** `hilt-android-testing` (nueva en el proyecto) y `hilt-compiler` en `kspAndroidTest`. Justificación en
  `design.md`.
- **Especificaciones:** deltas ADDED en `phrase-catalog` y `practice-persistence`.
- **Documentación:** `docs/adr/0014-*.md` (nuevo), `docs/adr/0007-*.md` (solo la línea de estado al aceptar 0014),
  `docs/adr/README.md`, `docs/deuda-tecnica.md`.
- **ADR afectados:** ADR-0007 (sustituido por ADR-0014), ADR-0010 (`:app` es el único módulo que conoce las
  implementaciones; el test se ubica ahí por ese motivo), ADR-0011 (detekt 1.23.8 con `detekt-formatting`, que es el
  análisis estático real del proyecto), ADR-0013 (catálogo en memoria y estado en Room).
- **ADR-0012:** no se toca el método de práctica ni el alcance del MVP.
- **Ejecución:** los tests son instrumentados y solo corren en emulador o dispositivo. La CI actual no ejecuta
  `connectedDebugAndroidTest`, así que hasta el cambio siguiente estos tests protegen solo en local.

## Fuera de alcance

- Cambiar el convention plugin ni el runner genérico de los módulos. Es una deuda nueva para el siguiente cambio de
  infraestructura, no un arreglo pendiente de este.
- Añadir un job de tests instrumentados con emulador en `.github/workflows/ci.yml`. Es el siguiente cambio de infraestructura.
- Incluir `src/androidTest` en el análisis de detekt. Si la tarea 3.1 confirma que no se analiza, queda en la deuda técnica
  con prioridad alta; no se cambia aquí la configuración de detekt.
- Las reglas Konsist futuras de DataStore (una única declaración de `preferencesDataStore`) y sobre data sources con
  estado, registradas en `docs/deuda-tecnica.md` sin implementar. Cambio aparte.
- Probar la memoización del catálogo dentro de `:app`. Ya la cubre `AssetsCatalogDataSourceTest` con contadores de lecturas
  del asset (`reads shouldBe 1`).
- Test de comportamiento con dos `DataStoreSettingsDataSource`. Se descarta: no falla nunca.
- Test de arranque de `FraseAFraseApp` y navegación. Su disparador es otro cambio de navegación.
