# Tasks

Cada tarea sigue TDD: primero el test, que debe fallar por el motivo esperado; después el código mínimo; después el refactor. Cada test cita en un comentario el escenario de `specs/practice-batch/spec.md` que cubre. Cada grupo termina en verde. Las tareas que nacen verde se anotan así en el informe de la tarea.

## 1. Tipos base y builders de pruebas

- [x] 1.1 Escribir `BatchSizeTest` con los escenarios "Valor por defecto", "Límites inferior y superior se aceptan" y "Valores fuera de rango se rechazan"; verificar que falla por clase inexistente
- [x] 1.2 Implementar `BatchSize` (`@JvmInline value class`, `require(value in 10..30)`, `MIN`, `MAX`, `DEFAULT = 20`); verificar que `BatchSizeTest` pasa
- [x] 1.3 Escribir `CefrLevelTest` con el escenario "Niveles CEFR válidos" en el paquete `core.domain.catalog`; verificar que falla por tipo inexistente
- [x] 1.4 Implementar `enum class CefrLevel { A1, A2, B1, B2, C1 }` en `core.domain.catalog`; verificar que `CefrLevelTest` pasa
- [x] 1.5 Escribir en `core/domain/src/test/kotlin/com/dalmuina/fraseafrase/core/domain/spacedrepetition/PhraseProgressBuildersTest.kt` un test que use `aPhraseProgress()` y espere que construya sin error, y que `aPhraseProgress(level = 1)` lance `IllegalArgumentException` por la invariante de repaso (cita el escenario existente "Nivel 1 sin próximo repaso se rechaza" de `spaced-repetition`; en el informe de la tarea, anotar que esa cobertura ya existe en `PhraseProgressTest` y no se cuenta dos veces); verificar que falla por función inexistente
- [x] 1.6 Implementar `aPhraseProgress(...)` en `:core:testing` con valores por defecto válidos; verificar que 1.5 pasa (la comprobación de combinaciones incoherentes ya está en 1.5)
- [x] 1.7 Escribir `CarryQueueTest` con el escenario "Cola de arrastradas con ids duplicados" de "Invariantes de la tanda y de la cola"; verificar que falla por clase inexistente
- [x] 1.8 Implementar `CarryQueue` (`data class`, `ids: List<PhraseId>`, ids únicos en `init`); verificar que `CarryQueueTest` pasa

## 2. Tanda activa como lista circular

- [ ] 2.1 Refactor en verde: extraer `PhraseProgress.record(assessment)` y hacer que `RecordSelfAssessment` delegue en él. Los tests existentes de `RecordSelfAssessmentTest` no se modifican; verificar con `./gradlew :core:domain:test --rerun` que siguen pasando
- [ ] 2.2 Escribir `PracticeBatchTest` con los escenarios "Acierto mueve la frase al final", "Fallo mueve la frase al final" y "Responder la última frase no cambia el orden", y con la aserción de capacidad de D1 (tanda de 30 entradas con `capacity = BatchSize(30)`; tras `answer`, `capacity == BatchSize(30)`; cita "Tamaño de tanda N"); verificar que falla por clase inexistente
- [ ] 2.3 Implementar `PracticeBatch(entries, capacity: BatchSize)` con `answer` que usa `PhraseProgress.record` y mueve la frase al final; verificar que 2.2 pasa
- [ ] 2.4 Escribir tests de "Varias respuestas mantienen las mismas entradas", "Responder una frase que no está en la tanda", "Tanda con ids duplicados" y "Tanda con más frases que su tamaño"; verificar que fallan por el motivo esperado, implementar las validaciones y verificar que pasan
- [ ] 2.5 Escribir, en el mismo rojo, los tests de `uncertifiedCount()` con "Aviso con frases no certificadas", "Aviso sin frases no certificadas", "Frase nunca respondida cuenta como no certificada" y "Calcular el aviso no modifica la tanda" (compara tanda, rachas y progreso antes y después); verificar que fallan por ausencia de `uncertifiedCount`; implementar `uncertifiedCount` como función pura y verificar que pasan

## 3. Composición de la tanda siguiente

- [ ] 3.1 Escribir `ComposeNextBatchTest` con "Arrastradas van antes que pendientes" y "Pendientes por nivel ascendente", usando `CatalogPhrase` y `aCatalogPhrase(...)` para el catálogo; verificar que falla por tipos y caso de uso inexistentes
- [ ] 3.2 Implementar `CatalogPhrase(id: PhraseId, level: CefrLevel)` en `core.domain.catalog`, `aCatalogPhrase(...)` en `:core:testing`, `ComposeNextBatch` (`@Inject constructor`, `Clock`, firma de D3) y `BatchComposition` (`Composed`, `data object NothingAvailable`) con la mínima lógica que exige 3.1; verificar que 3.1 pasa
- [ ] 3.3 Escribir "A igual nivel, la fecha de repaso más antigua primero" y "A igual nivel y fecha, orden de catálogo"; verificar que fallan, implementar y verificar que pasan
- [ ] 3.4 Escribir "Arrastrada entra primera en la siguiente tanda" y "Arrastrada que no cabe conserva su progreso"; verificar que fallan, implementar y verificar que pasan
- [ ] 3.5 Escribir "Nuevas solo si queda hueco", "Sin nuevas mientras queden pendientes sin caber" y "Pendientes que no caben esperan sin perder nivel"; verificar que fallan, implementar y verificar que pasan
- [ ] 3.6 Escribir "Arrastrada y pendiente aparece una sola vez" y "Frase programada no entra como nueva"; verificar que fallan, implementar la exclusión de todos los ids de `CarryQueue` y verificar que pasan
- [ ] 3.7 Escribir "El filtro CEFR no afecta a arrastradas ni pendientes"; verificar que falla, implementar y verificar que pasa
- [ ] 3.8 Escribir "Primera tanda con catálogo disponible", "Tanda menor que N si faltan frases", "Catálogo vacío sin progreso no crea tanda", "Catálogo sin frases del nivel elegido no crea tanda" y "Catálogo agotado sin pendientes no crea tanda"; estos escenarios son también la prueba de `CatalogPhrase` (tarea 3.2); verificar que fallan, implementar y verificar que pasan
- [ ] 3.9 Escribir "Solo nuevas del nivel elegido"; verificar que falla, implementar y verificar que pasa. Escribir "Certificada que vuelve como pendiente entra con racha 0" e implementar la normalización de racha de D3 paso 4: este test nace verde por ser guarda de robustez; anotarlo en el informe de la tarea
- [ ] 3.10 Escribir "Frase con repaso futuro no entra antes de su fecha" y "Frase con repaso de hoy entra en su fecha"; verificar que pasan con la implementación de 3.2 y anotar que nacen verdes (la fecha la fija la precondición de D3)
- [ ] 3.11 Escribir "Tanda activa conserva su tamaño al cambiar N" (requisito "Tamaño de tanda N"): cerrar una tanda de 30 frases, componer con `BatchSize(10)`, comprobar que la cerrada sigue siendo válida con 30 frases y que la siguiente tiene como máximo 10; como 3.2 ya existe, puede nacer verde: anotarlo en el informe de la tarea
- [ ] 3.12 Escribir los tests de precondiciones de "Composición con una arrastrada sin progreso", "Progreso sin entrada en el catálogo", "Progreso con ids duplicados" y "Catálogo con ids duplicados"; verificar que fallan, implementar los `require` y verificar que pasan

## 4. Ciclo completo: cierre y composición

- [ ] 4.1 Escribir `PracticeCycleTest` con "Arrastradas conservan el orden de la lista al cerrar", "La cola sobrante va antes que las arrastradas del cierre", "El aviso coincide con las arrastradas del cierre" y "Cierre con un id ya en la cola"; verificar que falla por ausencia de `CarryQueue.afterClosure`
- [ ] 4.2 Implementar `CarryQueue.afterClosure(leftover, outcomes)` como función de companion pura; verificar que 4.1 pasa
- [ ] 4.3 Escribir "La cola sobrante entra primera tras el cierre"; nace verde, porque lo cubren 3.4 y 4.1: anotarlo en el informe de la tarea

## 5. Verificación de integración

- [ ] 5.1 Ejecutar `./gradlew :core:domain:test` y verificar que todos los tests de `spacedrepetition` y `catalog` pasan, incluidos los existentes de `spaced-repetition`
- [ ] 5.2 Ejecutar `./gradlew detekt` y verificar que no hay incidencias nuevas en `:core:domain` ni en `:core:testing`
- [ ] 5.3 Ejecutar `./gradlew koverVerify` y verificar que el dominio mantiene al menos el 90 % de cobertura
- [ ] 5.4 Ejecutar `./gradlew check` y verificar que termina en verde

## Workflow follow-up

- Revisar el código con el subagente `code-reviewer` antes de verificar.
- Ejecutar `/opsx:verify` y, al completar el cambio, `/opsx:archive`.
