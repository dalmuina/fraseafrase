# Tasks

## 0. ADR-0014 en estado Propuesto (antes del grupo 1)

- [x] 0.1 Crear `docs/adr/0014-estrategia-de-testing-junit-6-y-junit-4-instrumentado.md` en estado `Propuesto`, con fecha y cabecera `Sustituye a: ADR-0007`, y las secciones Contexto, Decisión y Consecuencias de la plantilla de `docs/adr/README.md`. El contenido es el de design.md D7: todo lo vigente de ADR-0007, incluida la excepción acotada de DI y la declaración del cambio de JUnit 5 a JUnit 6.1.3. Verificar con la herramienta de búsqueda que el archivo contiene `Sustituye a: ADR-0007` y las tres secciones de la plantilla
- [x] 0.2 Añadir la fila de ADR-0014 en `docs/adr/README.md` con estado `Propuesto`; verificar con la herramienta de búsqueda que ADR-0007 sigue en `Aceptado`
- [x] 0.3 Commit del grupo 0 en verde (`docs(adr): propose ADR-0014`)

## 1. Dependencias y runner de Hilt en :app

- [x] 1.1 Añadir `hilt-android-testing` (con `version.ref = "hilt"`) a `gradle/libs.versions.toml`, en `androidTestImplementation` de `:app`, y `hilt-compiler` en `kspAndroidTest` de `:app`; verificar que `./gradlew :app:compileDebugAndroidTestKotlin` termina en verde
- [x] 1.2 Crear `app/src/androidTest/kotlin/com/dalmuina/fraseafrase/HiltTestRunner.kt` (extiende `AndroidJUnitRunner` y devuelve `HiltTestApplication` en `newApplication`) y fijar `testInstrumentationRunner = "com.dalmuina.fraseafrase.HiltTestRunner"` en el `defaultConfig` de `:app`; verificar que `./gradlew :app:compileDebugAndroidTestKotlin` termina en verde, que `git diff --stat build-logic/` está vacío y que el manifiesto fusionado del variant `debugAndroidTest` de `:app` (en `app/build/intermediates/merged_manifests/debugAndroidTest/`) declara el runner: buscar `android:name="com.dalmuina.fraseafrase.HiltTestRunner"` dentro de un elemento `<instrumentation`, sin asumir que los atributos van en una sola línea. Buscar en el manifiesto de test, no en el de `:app`
- [x] 1.3 Commit del grupo 1 en verde (`build(app): add hilt-android-testing and a Hilt test runner`), solo con esta configuración, sin tests

## 2. Tests de cableado (procedimiento de test nacido verde, design D5)

- [x] 2.1 Escribir `app/src/androidTest/kotlin/com/dalmuina/fraseafrase/PersistenceWiringTest.kt` (`@HiltAndroidTest`, `HiltAndroidRule`, `hiltRule.inject()`) con dos tests, cada uno con su escenario en un comentario. La comparación usa `org.junit.Assert.assertSame`:
  - `given the production graph when injecting the catalog twice then both references are the same instance`, requisito "Existe una única instancia de `CatalogRepository` en el grafo de la aplicación" de `phrase-catalog`, escenario "Dos obtenciones del catálogo desde el grafo de producción devuelven la misma instancia". Inyecta `CatalogRepository` dos veces y comprueba `assertSame`.
  - `given the production graph when obtaining the database twice then both references are the same instance`, requisito "Existe una única instancia de `PracticeDatabase` en el grafo de la aplicación" de `practice-persistence`, escenario "Dos obtenciones de la base de datos desde el grafo de producción devuelven la misma instancia". Inyecta `PracticeDatabase` dos veces y comprueba `assertSame`. No ejecuta consultas ni usa DAOs.
  Verificar que `./gradlew :app:connectedDebugAndroidTest` pasa en verde con el cableado real
- [x] 2.2 Mutación del catálogo: quitar `@Singleton` de `AssetsCatalogDataSource`, ejecutar los tests y comprobar que falla solo el de `CatalogRepository`, con un mensaje de `assertSame` que contiene `was not` (aserción, no compilación). Si el mensaje real difiere, parar y reportar; no ajustar la cadena esperada. Restaurar la anotación
- [x] 2.3 Mutación de la base de datos: quitar `@Singleton` del `@Provides` de `PracticeDatabase` en `PersistenceModule`, ejecutar los tests y comprobar que falla solo el de `PracticeDatabase`, con la misma aserción. Restaurar la anotación
- [x] 2.4 Ejecutar `./gradlew :app:connectedDebugAndroidTest --rerun` con ambas anotaciones restauradas y comprobar verde. Comprobar que `git diff` no contiene cambios en `core/data/` ni en `app/src/main/`. Commit del grupo 2 en verde (`test(app): verify singleton wiring of catalog and database`), sin rastro de la mutación

## 3. Comprobaciones previas y deuda técnica

- [x] 3.1 Comprobar si detekt analiza `src/androidTest`: añadir temporalmente en `app/src/androidTest` una infracción de regla de detekt, ejecutar `./gradlew :app:detekt` y anotar el resultado. Ejecutar además `./gradlew detekt` (raíz, lo que ejecuta la CI) y anotar si el código existente de `feature/session` falla. Borrar el fichero temporal y comprobar `git status` limpio. No cambiar la configuración de detekt. El resultado de esta tarea decide el punto de detekt de la tarea 3.2
- [x] 3.2 Corregir `docs/deuda-tecnica.md` según design.md D7:
  - Verificar en la documentación oficial de DataStore la afirmación de `IllegalStateException` que cita design.md D4, antes de escribirla en la deuda. Si no se confirma, quitarla.
  - El título de la entrada de Hilt pasa a "Tests de cableado de Hilt: misma instancia de `CatalogRepository` y `PracticeDatabase`"; la entrada deja de pedir identidad de DataStore y explica que no es un binding de Hilt; su disparador se marca como resuelto en este cambio.
  - Se añade la regla Konsist futura de DataStore (una única declaración de `preferencesDataStore` por nombre de fichero), sin implementar.
  - La regla Konsist futura sobre data sources con estado, de la línea 20, se mantiene sin implementar, con la definición y la clasificación de design.md D7.
  - La entrada de arranque de `FraseAFraseApp` anota que `HiltTestRunner` ya existe.
  - Se registra como siguiente cambio de infraestructura, con prioridad alta: el job de tests instrumentados en la CI, y los tests de UI de `:feature:session`, que nunca han pasado por detekt. Si 3.1 confirma que `src/androidTest` no se analiza, se añade la inclusión de `src/androidTest` en detekt; si 3.1 confirma que sí se analiza, no se añade.
  - Se añade la deuda nueva de revisar el runner genérico del convention plugin, sin presentarla como arreglo pendiente.
  Verificar con la herramienta de búsqueda que la entrada no exige identidad de DataStore
- [x] 3.3 Commit del grupo 3 (`docs(deuda): update Hilt wiring test debt`)

## 4. Verificación de integración

- [x] 4.1 Ejecutar `./gradlew connectedDebugAndroidTest` en un emulador y comprobar que todos los tests instrumentados pasan, incluidos los de `feature/session` y los nuevos de `:app`
- [x] 4.2 Ejecutar `./gradlew check koverVerify -PwarningsAsErrors=true` y comprobar que termina en verde

## 5. Revisión y cierre de decisiones de arquitectura

- [x] 5.1 Pasar `code-reviewer` sobre el cambio y `/opsx:verify`. Registrar en la descripción de la PR que no quedan hallazgos bloqueantes ni incidencias de verificación
- [x] 5.2 Pasar ADR-0014 a `Aceptado`, ADR-0007 a `Sustituido por ADR-0014` y actualizar sus filas en `docs/adr/README.md`. Solo después de 5.1, justo antes de `/opsx:archive`
- [x] 5.3 Verificar con la herramienta de búsqueda que los estados de ADR-0007 y ADR-0014 son los del punto anterior

## Workflow follow-up

- Archivar el cambio en su rama con `/opsx:archive`, con `openspec validate --specs` después, y hacer el último commit antes de la PR.
- Hacer push de la rama (`git push -u origin HEAD`) y parar. La PR a `develop` la abre el usuario.
