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
- **Tests de cableado de Hilt: misma instancia de `RoomDatabase`, `DataStore` y `CatalogRepository`.** Origen: revisión de `add-practice-session`. Motivo: la memoización del catálogo depende de que `AssetsCatalogDataSource` sea `@Singleton` (se corrigió con la auditoría de ámbitos). El test exige `hilt-android-testing` con el runner instrumentado, que hoy está roto (entrada anterior), y un test de anotaciones sería tautológico. **Disparador cumplido:** runner arreglado en `1.0.0/fix-instrumented-test-runner`; pendiente de implementar. **Regla futura de Konsist:** los data sources con estado deben tener ámbito `@Singleton`.
- **Test automático de arranque de `FraseAFraseApp`.** Test de UI instrumentado con Hilt en `:app` que compruebe que la app abre la pantalla de práctica. Se eliminó `StartDestinationTest` (`add-practice-session`, tareas 5.1 y 5.2) por tautológico: comparaba una constante consigo misma. Mientras tanto, el escenario "Arranque muestra la práctica" se verifica de forma manual (5.4). **Disparador:** cambio de navegación con práctica y ajustes, junto con el arreglo del runner.

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
