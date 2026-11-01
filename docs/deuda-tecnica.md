# Deuda técnica

Registro de mejoras detectadas y aplazadas. Cada entrada indica su origen y el motivo por el que no se aplicó en su momento. Ninguna entrada autoriza a ampliar el alcance de un cambio en curso: se abordan en un cambio propio.

## Del cambio `add-practice-session`

- **`requireNotNull` en `AnswerPhrase`.** Origen: revisión del grupo 3. Motivo: la validación actual funciona; el cambio a `requireNotNull` mejora el mensaje de error, pero no altera el comportamiento y queda fuera de la tarea.
- **`inMemoryPracticeDatabase()` en `src/main`.** Origen: revisión de `:core:data`. Motivo: la función está en `core/data/src/main/.../practice/PracticeSchema.kt` con visibilidad `internal`, pero solo la usan tests JVM (`PracticeSchemaTest`, `PracticeTriggersTest`, `RoomPracticeCycleTest`, `RoomPracticeDataSourceReadTest`, `RoomPracticeDataSourceWriteTest`). Producción no la usa: el grafo de Hilt crea la base de datos en disco. Se debería mover a `src/testFixtures` o a `:core:testing` cuando haya un mecanismo que lo permita.
- **Comentario obsoleto de `MalformedDocument`.** Origen: revisión de `CatalogLoadResult.kt`. Motivo: el comentario describe un comportamiento anterior; corregirlo es un cambio de documentación sin impacto funcional.
- **Test "Lectura durante un cierre" secuencial.** Origen: revisión de los tests de cierre. Motivo: el test se ejecuta de forma secuencial y no fuerza la concurrencia que pretende verificar; reescribirlo requiere diseñar la sincronización con cuidado.
- **`writeSnapshot` sin validar filas de progreso.** Origen: revisión de `RoomPracticeDataSource`. Motivo: no se valida que las filas de progreso pertenezcan a la tanda escrita; la validación añade comportamiento nuevo y debe decidirse en su propio cambio.

## Prioridad alta: runner de tests instrumentados

- **Causa.** `build-logic/convention/.../KotlinAndroid.kt` fija `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` para todos los módulos Android, pero la dependencia `androidx.test:runner` solo está en `androidTestImplementation` de algunos módulos. Sin ella, la tarea `connectedDebugAndroidTest` falla al arrancar la instrumentación con `ClassNotFoundException: androidx.test.runner.AndroidJUnitRunner`, aunque el módulo no tenga tests.
- **Módulos afectados.** `:core:platform` y `:core:data` (no tienen `src/androidTest`). Se comprobó que el fallo ya ocurre en `develop` (commit `48d7a11`), así que es previo a `add-practice-session`.
- **Tests instrumentados sin ejecutar con el comando completo.** Ninguno de código: el único conjunto de tests instrumentados del repositorio es `feature/session/src/androidTest` (7 tests de `PracticeSessionScreenTest`), y ese módulo sí pasa en `Pixel_7a(AVD) - 17`. El riesgo es que `connectedDebugAndroidTest` completo no puede servir de puerta de calidad mientras falle.
- **Propuesta de cambio propio.** (1) Corregir la configuración del runner en `build-logic` para que solo se aplique cuando el módulo tiene tests instrumentados, o declarar `androidTestImplementation(libs.androidx.test.runner)` en el plugin, y verificar que `connectedDebugAndroidTest` completo termina en verde en el emulador. (2) Valorar ejecutar los tests instrumentados en la CI (`.github/workflows/ci.yml`) con un job de emulador (por ejemplo `reactivecircus/android-emulator-runner`), que hoy solo ejecuta `detekt`, `check`, `koverVerify` y `assembleDebug`.

## Transversal

- **`progressWith` duplicado en `ResolveBatchClosureTest`.** Origen: revisión de tests de dominio. Motivo: el helper está copiado en vez de compartido; moverlo a `:core:testing` no cambia la cobertura, pero toca un módulo fuera del cambio.
- **Mensajes de `require` en español en `Level.kt` y `PhraseProgress.kt`.** Origen: revisión de convenciones. Motivo: CLAUDE.md pide mensajes de código en inglés; el cambio es cosmético pero afecta a dos ficheros de dominio ya aprobados.
- **Builders de `:core:testing` con `Int` en lugar de value classes.** Origen: revisión de fakes y builders. Motivo: los builders aceptan enteros donde el dominio usa value classes; cambiarlo rompe la firma de muchos tests existentes.
- **Parametrizar los tests de `ReviewSchedule`.** Origen: revisión de tests de repetición espaciada. Motivo: hay casos repetidos que podrían ser `@ParameterizedTest`; es una mejora de legibilidad sin impacto funcional.
- **Reglas de arquitectura con Konsist (ADR-0010).** Origen: ADR-0010. Motivo: las dependencias entre módulos se revisan a mano (`android-architect`); automatizarlas con Konsist evitaría regresiones, pero requiere añadir la librería y justificarla en su `design.md`.
- **Migrar a detekt 2.0 cuando sea estable.** Origen: ADR-0011, que fija detekt 1.23.8 porque 2.0 estaba en alfa. Motivo: la migración sustituirá al ADR-0011 mediante un ADR nuevo cuando detekt 2.0 sea estable.
- **Proteger `develop` y `main` en GitHub.** Origen: flujo de ramas de CLAUDE.md. Motivo: la protección de ramas (PR obligatoria y CI en verde) se configura en GitHub, fuera del repositorio.
