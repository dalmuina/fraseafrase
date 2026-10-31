## Context

Ver `proposal.md` (motivación y alcance) y `specs/practice-batch/spec.md` (requisitos y escenarios). El cierre ya existe en `:core:domain` (`ResolveBatchClosure`, `BatchClosureOutcome`, del cambio archivado `add-spaced-repetition`) y se reutiliza sin cambios.

Restricciones del código actual:
- `PhraseProgress` ya valida sus invariantes en `init` y expone `isCertified` (racha ≥ 3) e `isPendingOn(today)`. Sobre un progreso ya resuelto `isCertified` es siempre `false`, porque el cierre pone la racha a 0.
- `RecordSelfAssessment` actúa sobre una sola frase y no conoce el orden de la tanda. Su lógica pasa a `PhraseProgress.record(assessment)`, y el caso de uso delega en ella.
- El tiempo llega por `Clock`; ningún cálculo lee el reloj del sistema.

## Goals / Non-Goals

**Goals**
- Modelar la tanda activa como lista con invariantes propias (ids únicos, tamaño fijado al crearla).
- Modelar la cola de arrastradas como estado explícito y ordenado.
- Componer la tanda siguiente como función pura, determinista y testeable.
- Garantizar que cada frase está en como máximo un sitio.

**Non-Goals**
- Persistencia, puertos de repositorio y casos de uso de aplicación con datos reales (cambio posterior).
- Cualquier cambio de comportamiento en `ResolveBatchClosure`, `RecordSelfAssessment` o `spaced-repetition`.

## Decisions

Todas las clases nuevas están en `:core:domain` (paquetes `spacedrepetition` y `catalog`), salvo los builders de `:core:testing`. No hay cambios de UI ni de `:core:designsystem`.

### D1. Entidad `PracticeBatch` y lista ordenada de `PhraseProgress`

`PracticeBatch` es una `data class` inmutable: `PracticeBatch(entries: List<PhraseProgress>, capacity: BatchSize)`.
- Su `init` exige ids únicos y `entries.size <= capacity.value`.
- `capacity` es el `BatchSize` con el que se creó la tanda. Un cambio posterior de N no la afecta: el ajuste solo entra en la composición siguiente (ADR-0012).
- `answer(id: PhraseId, assessment: SelfAssessment): PracticeBatch` aplica `PhraseProgress.record(assessment)` a la frase y la mueve al final. Si el id no existe, lanza `IllegalArgumentException`.
- `uncertifiedCount(): Int` cuenta las entradas con `!isCertified`. Es pura: no modifica la tanda, sus rachas ni el progreso. Se calcula antes de resolver el cierre.

Alternativa descartada: lista pura con funciones sueltas. Obliga a repetir las invariantes en cada llamador.

### D2. Cola de arrastradas explícita

`CarryQueue` es una `data class` con `ids: List<PhraseId>` y ids únicos en `init`. Es el único lugar que guarda el orden de las arrastradas entre tandas. Así, una frase arrastrada sin repaso (nivel 0) o con repaso (nivel ≥ 1) se distingue de una pendiente sin ambigüedad.

El orden de la cola es el de la lista de la tanda al cerrar, con la primera frase en la posición 0.

Regla de unicidad: el compositor excluye de las pendientes todos los ids de `CarryQueue`, no solo los que entran, para que una arrastrada que no cabe no se cuele como pendiente.

Alternativa descartada: marcar las arrastradas dentro del `PhraseProgress`. Mezcla el estado de repaso con el estado de la cola y no da un orden explícito.

### D3. Composición como caso de uso puro `ComposeNextBatch`

Se invoca solo cuando no hay tanda activa. Sin tanda activa, la unicidad de D2 se refiere a la cola, al progreso y al catálogo.

Firma: `operator fun invoke(carry: CarryQueue, progress: List<PhraseProgress>, catalog: List<CatalogPhrase>, cefrLevel: CefrLevel, size: BatchSize): BatchComposition`. El `Clock` se inyecta en el constructor; `today = clock.today()` se calcula una vez al inicio, como en `ResolveBatchClosure`.

Entrada:
- `carry: CarryQueue`
- `progress: List<PhraseProgress>` (todo el progreso conocido)
- `catalog: List<CatalogPhrase>` en orden de catálogo
- `cefrLevel: CefrLevel`
- `size: BatchSize`
- `Clock` inyectado, igual que `ResolveBatchClosure`

Tipos del paquete `core.domain.catalog`: `enum class CefrLevel { A1, A2, B1, B2, C1 }` y `data class CatalogPhrase(val id: PhraseId, val level: CefrLevel)`. No se llama `Level` al enum porque ese nombre ya es el nivel de consolidación 0–5.

Algoritmo:
1. `fromQueue = carry.ids.take(size)`; `leftover = carry.ids.drop(size)`.
2. `pending` = progreso con `isPendingOn(today)` y sin ids de `carry`, ordenado por (nivel ascendente, `nextReview` ascendente, índice en `catalog`). El índice de catálogo es solo un desempate de determinismo. Se toman hasta `size - fromQueue.size`.
3. `new` = frases de `catalog` con `level == cefrLevel` y sin progreso, en orden de catálogo, solo si queda hueco tras las pendientes. El filtro CEFR no se aplica a `fromQueue` ni a `pending`.
4. Cada entrada de `fromQueue + pending` se normaliza a racha 0 y `answeredInBatch = false`. Con progreso producido por el cierre es un no-op: es una guarda de robustez que nace verde, y el escenario "Certificada que vuelve como pendiente entra con racha 0" la fija. Las nuevas se construyen como `PhraseProgress(id, Level(0), Streak(0), answeredInBatch = false, nextReview = null)`.
5. Si `fromQueue + pending + new` está vacío → `BatchComposition.NothingAvailable`. Si no → `BatchComposition.Composed(PracticeBatch(entries, size), CarryQueue(leftover))`.

Precondiciones (`require`, `IllegalArgumentException`): cada id de `carry` tiene progreso; cada id de `progress` tiene entrada en `catalog`; no hay ids duplicados en `progress` ni en `catalog`.

Las frases nuevas no se persisten hasta que se responden o se cierran.

### D4. Encadenar cierre y composición

Flujo, sin orquestador en el dominio:

```
aviso        = batch.uncertifiedCount()                       (antes de resolver)
outcomes     = ResolveBatchClosure(batch.entries)             (orden de entrada)
newCarry     = CarryQueue.afterClosure(leftover, outcomes)    (cola sobrante + arrastradas del cierre)
progressSave = [o.progress for o in outcomes]                 (lo guarda el llamador)
next         = ComposeNextBatch(newCarry, progress actualizado, catalog, cefrLevel, size)
```

- `CarryQueue.afterClosure(leftover: CarryQueue, outcomes: List<BatchClosureOutcome>): CarryQueue` devuelve `leftover.ids + [o.progress.id for o in outcomes if o is Carried]`, con ids únicos. Es una función de companion, pura. Si la cola sobrante y una arrastrada comparten id, `CarryQueue.init` lanza `IllegalArgumentException` (escenario "Cierre con un id ya en la cola").
- `leftover` va primero porque son las arrastradas más antiguas.
- Las certificadas no entran en la cola: salen de la tanda y su repaso cae en días futuros, así que no son pendientes hoy.
- Coherencia del aviso: `uncertifiedCount()` debe igualar el número de `Carried` en los outcomes. Se cubre con el test de ciclo completo.

### D5. Tamaño de tanda `BatchSize`

`@JvmInline value class BatchSize(val value: Int)` con `require(value in 10..30)`, `MIN`, `MAX` y `DEFAULT = 20`. Sigue el patrón de `Level` y `Streak`. Si los ajustes guardan un valor corrupto, la capa de datos (cambio posterior) hace el fallback a `DEFAULT`; el dominio se queda estricto.

### D6. Resultado de la composición

`sealed interface BatchComposition` con `data class Composed(batch: PracticeBatch, leftover: CarryQueue)` y `data object NothingAvailable`. Es un resultado esperable (ADR-0012: "no se crea tanda y la app lo indica"), así que no es una excepción.

### D7. Errores: invariantes frente a resultados esperables

- Las violaciones de invariantes (ids duplicados, tamaño excedido, id inexistente en `answer`, precondiciones de D3) son errores de programación y lanzan `IllegalArgumentException` mediante `require`.
- Los resultados esperables (no hay nada que componer) son `BatchComposition.NothingAvailable`.
- El pedido de un tamaño fuera de 10..30 también es `IllegalArgumentException`, porque la UI solo ofrece valores válidos y la capa de datos hace el fallback (D5).

### D8. Builders de pruebas

En `:core:testing`:
- `aPhraseProgress(id: String = "phrase-1", level = 0, streak = 0, answeredInBatch = false, nextReview = null): PhraseProgress`, que envuelve `id` en `PhraseId`.
- `aCatalogPhrase(id: String = "phrase-1", level = CefrLevel.A1): CatalogPhrase`, que envuelve `id` en `PhraseId`.

Los valores por defecto cumplen las invariantes. Las combinaciones incoherentes siguen fallando en el `require` de `PhraseProgress`; los builders no las corrigen.

### D9. Estrategia de test

- Tests JVM puros en `:core:domain`: `BatchSizeTest`, `CefrLevelTest`, `CarryQueueTest`, `PracticeBatchTest`, `ComposeNextBatchTest` y `PracticeCycleTest`. `PhraseProgress.record` se prueba a través de los tests existentes de `RecordSelfAssessment`, que no se tocan, y de `PracticeBatchTest`. El test de builders (1.5) cita el escenario existente "Nivel 1 sin próximo repaso se rechaza" de `spaced-repetition` y vive en `core/domain/src/test/kotlin/com/dalmuina/fraseafrase/core/domain/spacedrepetition/PhraseProgressBuildersTest.kt`. No se crea `src/test` en `:core:testing`.
- Reloj: `FixedClock` existente de `:core:testing`, con fechas distintas por test cuando haga falta.
- Fakes: ninguno. No hay puertos nuevos; los datos entran como argumentos.
- Cada test cita en un comentario el escenario de la spec que cubre.
- `PracticeCycleTest` es el test de integración de dominio del ciclo cierre → composición.

## Risks / Trade-offs

- **[Cola que crece si N baja]** → Si N baja, las arrastradas que no caben se acumulan en la cola y las nuevas no entran hasta vaciarla. Es el comportamiento que pide la spec; no se limita en este cambio. Se observa en los tests de composición con cola mayor que N.
- **[Catálogo que cambia]** → Un progreso con id fuera del catálogo rompe la precondición. Mitigación: el catálogo es precargado e inmutable en el MVP; la precondición falla pronto y en test.
- **[Desfase entre aviso y cierre real]** → Mitigación: `uncertifiedCount` y el número de `Carried` se prueban juntos en `PracticeCycleTest`.
- **[Pendientes no caben]** → Quedan en progreso sin tocar, con su nivel y repaso, y se recogen en una composición posterior. Es el comportamiento que pide ADR-0012.
- **[Tests que nacen verdes]** → Los tests de la tarea 2.5 fallan por ausencia de `uncertifiedCount`, incluido "Calcular el aviso no modifica la tanda". Las guardas de 3.9, 3.10, 3.11 y 4.3 nacen verdes y se anotan en el informe de cada tarea.

## Notas: pendiente fuera de este cambio

- **Limpieza de `ResolveBatchClosureTest`:** la función privada `progressWith` duplica el nuevo builder `aPhraseProgress`. Migrarla es un refactor posterior, sin tarea en este cambio.

## Migration Plan

No hay persistencia ni datos existentes que migrar. Rollback: revertir la rama; no hay estado en disco afectado.

## Librerías

Ninguna nueva. Se usan JUnit 6, Kotest assertions y el `FixedClock` existente de `:core:testing`, tal como en los tests actuales de `:core:domain`.
