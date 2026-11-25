# Design

## Context

Ver proposal.md, "Why". Estado actual relevante:

- `PracticeSessionViewModel` lanza `reload()` en `init`, y `PracticeSessionRoute` envía `Resume` en un
  `LaunchedEffect(Unit)`, que lanza otro `reload(onlyIfChanged = true)`. Las dos llamadas a
  `ObtainPracticeSession` → `ObtainActiveBatch` pueden coincidir.
- `ObtainActiveBatch` decide con un `practiceRepository.read()` fuera de la transacción ("sin tanda", o "ajustes
  aplicados distintos") y después llama a `RegenerateActiveBatchForLevel`.
- `RegenerateActiveBatchForLevel` hace `transact { snapshot.copy(activeBatch = null) → composeNextBatchInto(...) }`:
  vacía la tanda que encuentre, sin comprobar si es la que se observó. `RoomPracticeDataSource.transact` usa una
  transacción inmediata, así que las dos transacciones se serializan, y la segunda vacía la tanda de la primera y
  recompone con su progreso ya creado.
- `RegenerateActiveBatchForLevel` solo lo invoca `ObtainActiveBatch` (el `androidTest` de `PracticeSessionRoute`
  solo lo construye).
- `PracticeSessionViewModel.hasSameBatchAs` compara `LoadedItem(id, source)`; los ids son comunes a todos los
  catálogos de destino (ADR-0013).

## Goals / Non-Goals

**Goals:**
- Que la decisión de recomponer y la recomposición formen una única operación atómica respecto a la tanda activa,
  sin mecanismos de exclusión nuevos fuera de la transacción ya existente (ADR-0013).
- Pruebas que fuerzan la intercalación de forma determinista, sin depender de la planificación de hilos.

**Non-Goals:**
- Cambiar cuántas veces lee la sesión el ViewModel.
- Cambiar `PracticeRepository`, el esquema de Room o `composeNextBatchInto`.

## Decisions

### D1. Recomposición condicionada a la tanda observada (compare-and-set dentro de `transact`)

`RegenerateActiveBatchForLevel.invoke` recibe un parámetro nuevo y obligatorio, `expected: ActiveBatch?`: la tanda
activa que el llamador observó al decidir recomponer (`null` si no había). Dentro de la transacción:

| `snapshot.activeBatch` | Resultado | Escritura |
|---|---|---|
| `== expected` | vacía y compone como hoy (`Created` o `NothingAvailable`) | sí |
| `!= expected` y no `null` | `Existing(snapshot.batch())` | sin cambios: `PracticeChange(snapshot, …)` reescribe la misma instantánea |
| `!= expected` y `null` | `NothingAvailable` | sin cambios: `PracticeChange(snapshot, …)` reescribe la misma instantánea |

La comparación es la igualdad de `ActiveBatch` (`data class` con orden y capacidad), que identifica la tanda
persistida. Vive en `:core:domain` (`practice/RegenerateActiveBatchForLevel.kt`), como función pura dentro del
`change` de `transact`; no hay E/S nueva.

`ObtainActiveBatch` guarda la instantánea leída y pasa `snapshot.activeBatch` (`ActiveBatch?`) como `expected` en las
dos ramas que recomponen (sin tanda y ajustes aplicados distintos); `existing` sigue siendo `snapshot.batch()` para
la rama `Existing`. Solo registra los ajustes aplicados cuando el resultado es
`Created`, como hoy: si otra obtención ganó la carrera, es ella quien los registra.

Alternativas descartadas:
- `Mutex` en el caso de uso o en el repositorio: depende del ámbito de DI (singleton) para funcionar, no cubre dos
  procesos ni instancias distintas, y añade un mecanismo paralelo a la transacción, que ya serializa.
- Serializar `reload()` en el ViewModel: solo arregla esta pantalla y deja la carrera en el dominio (decisión del
  usuario en explore).
- Parámetro opcional con valor por defecto: permitiría llamar sin la tanda observada y volver a la carrera; con un
  único llamador, obligatorio no cuesta nada.

### D2. `hasSameBatchAs` incluye el idioma destino

`LoadedItem` pasa a ser `LoadedItem(id, source, targetLocale)`, tomado de `PracticeItem.targetLocale`. Con un
idioma destino distinto, la tanda se considera cambiada y `reload` reemplaza el estado (frase sin revelar, texto e
idioma nuevos). Vive en `:feature:session` (`PracticeSessionViewModel.kt`). Alternativa descartada: guardar
`activeTargetLocale` aparte; el idioma va con cada elemento, igual que el origen (D7 de `origin-language-selection`).

### D3. Estrategia de test

- **Unitario de dominio** (`:core:domain`, `RegenerateActiveBatchForLevelTest`, `ObtainActiveBatchTest`), con
  `FakePracticeRepository` de `:core:testing`:
  - `RegenerateActiveBatchForLevel` con `expected` distinto de la tanda guardada → `Existing` y la instantánea del
    fake sin cambios (tanda, progreso, contador).
  - Intercalación forzada en `ObtainActiveBatch`: un decorador local del test (`InterleavingPracticeRepository`)
    envuelve el fake y, en la primera llamada a `read()`, lee la instantánea del fake, ejecuta la acción inyectada
    (otra obtención completa) y devuelve la instantánea leída antes de la acción. Así la primera obtención decide
    con "sin tanda" (o con la tanda A1) y escribe cuando la otra ya guardó la suya. Cubre los escenarios sin tanda
    activa y de cambio de ajustes, con los datos de partida de la spec (4 frases de nivel 1 con `dueAtSession` 5):
    sin ese progreso previo, ADR-0026 recompone la misma tanda y el test no podría fallar en rojo. El THEN que lo
    hace fallar hoy es "exactamente una `Created` y otra `Existing`" (hoy las dos devuelven `Created`), además del
    progreso (14 frases en vez de 10).
- **Integración con Room real** (`:core:data`, nuevo `ConcurrentObtainPracticeSessionTest` en
  `src/test/.../practice`, con `inMemoryPracticeDatabase()` como `RoomPracticeCycleTest`): dos
  `async { obtainPracticeSession(ptPt) }` sobre `RoomPracticeDataSource`, a través de un decorador de test cuyo
  `read()` espera a una barrera (`CompletableDeferred` liberado cuando han leído las dos). Así las dos leen "sin
  tanda" antes de que ninguna transacción escriba, sea cual sea el dispatcher. Parte del progreso previo del
  escenario (4 frases A1 de nivel 1 con `dueAtSession` 5, sembradas en Room). Comprueba que los dos resultados
  muestran la misma tanda de 10 en el mismo orden, que la tanda guardada coincide con ella, que el progreso tiene
  exactamente 10 frases (las 4 previas y 6 nuevas) y que el contador no cambia. Con el código actual falla: la
  segunda composición deja una tanda de 6 pendientes y 4 nuevas distinta de la primera y 14 frases con progreso.
- **ViewModel** (`:feature:session`, `PracticeSessionViewModelTest` o un `PracticeSessionTargetLanguageTest`
  nuevo junto a `PracticeSessionSourceLanguageTest`), con `MainDispatcherExtension` y los fixtures de
  `PracticeSessionFixtures`: tanda `pt-PT` cargada y revelada; se cambia el idioma destino activo a `de-DE` con una
  tanda de los mismos ids y origen; tras `Resume`, `current.target` es el de `de-DE`, `revealed == false` y
  `activeTargetLocale == de-DE`.

No hay fakes nuevos en `:core:testing`: los decoradores son locales de cada test.

## Risks / Trade-offs

- [Una autoevaluación entre la lectura y la transacción rota el orden y la recomposición por ajustes se salta en esa
  obtención] → Se devuelve `Existing` con la tanda vigente; la comparación de ajustes se repite en cada obtención
  (requisito vigente), así que la siguiente obtención recompone.
- [Instalaciones que ya sufrieron la carrera conservan progreso de frases no vistas] → Fuera de alcance; sin
  migración (proposal).
- [La barrera del test de integración bloquea si solo una obtención llega a leer] → La barrera se libera al
  segundo `read()` y el test usa el `timeout` de `runTest`; un fallo se ve como timeout, no como cuelgue silencioso.

## Migration Plan

Sin migración de datos ni de esquema.
