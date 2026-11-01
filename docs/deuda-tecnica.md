# Deuda técnica

Registro de mejoras detectadas y aplazadas. Cada entrada indica su origen y el motivo por el que no se aplicó en su momento. Ninguna entrada autoriza a ampliar el alcance de un cambio en curso: se abordan en un cambio propio.

## Del cambio `add-practice-session`

- **`requireNotNull` en `AnswerPhrase`.** Origen: revisión del grupo 3. Motivo: la validación actual funciona; el cambio a `requireNotNull` mejora el mensaje de error, pero no altera el comportamiento y queda fuera de la tarea.
- **`inMemoryPracticeDatabase()` en `src/main`.** Origen: revisión de `:core:data`. Motivo: la función está en `core/data/src/main/.../practice/PracticeSchema.kt` con visibilidad `internal`, pero solo la usan tests JVM (`PracticeSchemaTest`, `PracticeTriggersTest`, `RoomPracticeCycleTest`, `RoomPracticeDataSourceReadTest`, `RoomPracticeDataSourceWriteTest`). Producción no la usa: el grafo de Hilt crea la base de datos en disco. Se debería mover a `src/testFixtures` o a `:core:testing` cuando haya un mecanismo que lo permita.
- **Comentario obsoleto de `MalformedDocument`.** Origen: revisión de `CatalogLoadResult.kt`. Motivo: el comentario describe un comportamiento anterior; corregirlo es un cambio de documentación sin impacto funcional.
- **Test "Lectura durante un cierre" secuencial.** Origen: revisión de los tests de cierre. Motivo: el test se ejecuta de forma secuencial y no fuerza la concurrencia que pretende verificar; reescribirlo requiere diseñar la sincronización con cuidado.
- **`writeSnapshot` sin validar filas de progreso.** Origen: revisión de `RoomPracticeDataSource`. Motivo: no se valida que las filas de progreso pertenezcan a la tanda escrita; la validación añade comportamiento nuevo y debe decidirse en su propio cambio.

## Runner de tests instrumentados (resuelto en `1.0.0/fix-instrumented-test-runner`)

- **Causa.** `build-logic/convention/.../KotlinAndroid.kt` fija `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` para todos los módulos Android, pero la dependencia `androidx.test:runner` solo está en `androidTestImplementation` de algunos módulos. Sin ella, la tarea `connectedDebugAndroidTest` falla al arrancar la instrumentación con `ClassNotFoundException: androidx.test.runner.AndroidJUnitRunner`, aunque el módulo no tenga tests.
- **Módulos afectados.** `:core:platform` y `:core:data` (no tienen `src/androidTest`). Se comprobó que el fallo ya ocurre en `develop` (commit `48d7a11`), así que es previo a `add-practice-session`.
- **Tests instrumentados sin ejecutar con el comando completo.** Ninguno de código: el único conjunto de tests instrumentados del repositorio es `feature/session/src/androidTest` (7 tests de `PracticeSessionScreenTest`), y ese módulo sí pasa en `Pixel_7a(AVD) - 17`. El riesgo es que `connectedDebugAndroidTest` completo no puede servir de puerta de calidad mientras falle.
- **Resuelto.** La dependencia `androidx.test:runner` se declara en el plugin común (`KotlinAndroid.kt`) para todos los módulos Android. `connectedDebugAndroidTest` completo queda en verde en `Pixel_7a(AVD) - 17`.
- **Pendiente.** Valorar ejecutar los tests instrumentados en la CI (`.github/workflows/ci.yml`) con un job de emulador (por ejemplo `reactivecircus/android-emulator-runner`), que hoy solo ejecuta `detekt`, `check`, `koverVerify` y `assembleDebug`.
- **Tests de cableado de Hilt: misma instancia de `CatalogRepository` y `PracticeDatabase`.** Origen: revisión de `add-practice-session`. Motivo: la memoización del catálogo depende de que `AssetsCatalogDataSource` sea `@Singleton` (se corrigió con la auditoría de ámbitos). **Resuelto en `add-hilt-wiring-tests`:** `PersistenceWiringTest` (`:app/src/androidTest`) comprueba con `assertSame` el grafo real de `PersistenceModule`, y se verificó con mutación de cada anotación. `DataStore` no es un binding de Hilt y no entra en esta prueba: su garantía de una sola instancia por fichero es la declaración única del delegado `preferencesDataStore`. Según la documentación de DataStore, tener varias instancias activas para el mismo fichero en el mismo proceso lanza `IllegalStateException` al leer o escribir.
  - **Regla futura de Konsist (sin implementar):** una única declaración de `preferencesDataStore` por nombre de fichero.
  - **Regla futura de Konsist (sin implementar):** los data sources con estado deben tener ámbito `@Singleton`. Un data source está *con estado* si guarda en sus propios campos una caché o un valor mutable en memoria. Clasificación actual: `AssetsCatalogDataSource` está con estado (memoización, `@Singleton`); `RoomPracticeDataSource` y `DataStoreSettingsDataSource` están sin estado.
- **Test automático de arranque de `FraseAFraseApp`.** Test de UI instrumentado con Hilt en `:app` que compruebe que la app abre la pantalla de práctica. Se eliminó `StartDestinationTest` (`add-practice-session`, tareas 5.1 y 5.2) por tautológico: comparaba una constante consigo misma. Mientras tanto, el escenario "Arranque muestra la práctica" se verifica de forma manual (5.4). `HiltTestRunner` ya existe (`add-hilt-wiring-tests`); falta solo el disparador de navegación. **Disparador:** cambio de navegación con práctica y ajustes.

## Del cambio `add-hilt-wiring-tests`

- **Job de tests instrumentados en la CI (prioridad alta).** Sin este job, los tests instrumentados solo protegen en local: un fallo de cableado solo se detecta si alguien ejecuta `connectedDebugAndroidTest` antes de integrar. Incluye los tests de UI de `:feature:session`, que nunca han pasado por detekt. Es el siguiente cambio de infraestructura.
- **`src/androidTest` fuera de detekt (prioridad alta).** Comprobado en `add-hilt-wiring-tests` (tarea 3.1): con una infracción temporal en `app/src/androidTest`, `./gradlew :app:detekt` termina en verde y no la reporta. Detekt no analiza ese source set. Su inclusión se añade en el mismo cambio de infraestructura que el job de CI. No se cambió la configuración de detekt en `add-hilt-wiring-tests`.
- **Runner genérico del convention plugin (deuda nueva, no arreglo pendiente).** `KotlinAndroid.kt` fija `AndroidJUnitRunner` para todos los módulos Android, aunque solo algunos tienen tests instrumentados. `:app` lo sobrescribe con `HiltTestRunner`. Revisar si el runner genérico debe seguir en el convention plugin.

## Del review de `add-practice-session`

Origen de todas: `code-reviewer add-practice-session` (revisión de `develop...HEAD`). Son sugerencias que no afectan a la spec ni al comportamiento verificado.

- **Estado `Practicing(current = null)` con lista vacía.** Origen: code-reviewer add-practice-session. Motivo: `Ready` con lista vacía produce `current = null`, y la pantalla tiene dos ramas para ese caso. Lo correcto es mapearlo a `NothingToPractice` y eliminar la rama, o usar un tipo que no admita nulo.
- **Guardas repetidas en el ViewModel.** Origen: code-reviewer add-practice-session. Motivo: cinco acciones repiten `_uiState.value as? Practicing ?: return`. Un helper como `updatePracticing { ... }` dejaría las reglas de D3 más legibles.
- **Aserción débil en "Confirmar el cierre usa el número del aviso".** Origen: code-reviewer add-practice-session. Motivo: solo comprueba el nivel de A. Falta comprobar explícitamente que la tanda quedó cerrada.
- **Rama de `withText` sin test.** Origen: code-reviewer add-practice-session. Motivo: `ObtainPracticeSession` devuelve `CatalogFailed` si falla la segunda lectura del catálogo (tarea 1.4), y no hay test de ese caso. Con la memoización bien cableada apenas es alcanzable.
- **Error de catálogo sin test de UI.** Origen: code-reviewer add-practice-session. Motivo: los tests de estado están en el ViewModel, pero ningún UI test comprueba el texto de `practice_catalog_error`.
- **`PracticeItem` en el mismo fichero que `PracticeSessionResult`.** Origen: code-reviewer add-practice-session. Motivo: conviene moverlo a `PracticeItem.kt`.
- **Test de UI que reimplementa el estado.** Origen: code-reviewer add-practice-session. Motivo: el test de "Revelar" reescribe a mano la reacción a `Reveal` en lugar de usar el ViewModel real. Es aceptable como fake, pero el flujo real no se prueba en UI.

## Transversal

- **`progressWith` duplicado en `ResolveBatchClosureTest`.** Origen: revisión de tests de dominio. Motivo: el helper está copiado en vez de compartido; moverlo a `:core:testing` no cambia la cobertura, pero toca un módulo fuera del cambio.
- **Mensajes de `require` en español en `Level.kt` y `PhraseProgress.kt`.** Origen: revisión de convenciones. Motivo: CLAUDE.md pide mensajes de código en inglés; el cambio es cosmético pero afecta a dos ficheros de dominio ya aprobados.
- **Builders de `:core:testing` con `Int` en lugar de value classes.** Origen: revisión de fakes y builders. Motivo: los builders aceptan enteros donde el dominio usa value classes; cambiarlo rompe la firma de muchos tests existentes.
- **Parametrizar los tests de `ReviewSchedule`.** Origen: revisión de tests de repetición espaciada. Motivo: hay casos repetidos que podrían ser `@ParameterizedTest`; es una mejora de legibilidad sin impacto funcional.
- **Reglas de arquitectura con Konsist (ADR-0010).** Origen: ADR-0010. Motivo: las dependencias entre módulos se revisan a mano (`android-architect`); automatizarlas con Konsist evitaría regresiones, pero requiere añadir la librería y justificarla en su `design.md`.
- **Migrar a detekt 2.0 cuando sea estable.** Origen: ADR-0011, que fija detekt 1.23.8 porque 2.0 estaba en alfa. Motivo: la migración sustituirá al ADR-0011 mediante un ADR nuevo cuando detekt 2.0 sea estable.
- **Proteger `develop` y `main` en GitHub.** Origen: flujo de ramas de CLAUDE.md. Motivo: la protección de ramas (PR obligatoria y CI en verde) se configura en GitHub, fuera del repositorio.
