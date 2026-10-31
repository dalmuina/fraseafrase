# Proposal

## Why

El ADR-0012 define el método de práctica por tandas, pero el dominio solo sabe resolver el cierre de una tanda (`ResolveBatchClosure`). Faltan las reglas que dan vida a la tanda: la lista circular, el aviso antes de cerrar, la cola de frases arrastradas, la composición de la tanda siguiente con prioridades y el caso "sin tanda". Sin ellas no hay forma de decidir qué practica el usuario después de cerrar. Este cambio añade esas reglas al dominio, sin persistencia ni interfaz, para poder probarlas de forma determinista.

## What Changes

- Nueva entidad `PracticeBatch`: tanda activa, lista de `PhraseProgress` con ids únicos y un tamaño máximo fijado al crearla (`BatchSize`). Responder una frase la mueve al final sin eliminarla. Un cambio posterior de N no afecta a una tanda ya creada.
- Nueva cola de arrastradas explícita y ordenada (`CarryQueue`, ids `PhraseId`), parte del estado entre tandas. Las arrastradas que no caben en una tanda conservan su orden y entran primero en la siguiente.
- Aviso previo al cierre: número de frases no certificadas que se arrastrarían. Calcularlo no modifica la tanda, sus rachas ni el progreso.
- Composición de la tanda nueva, hasta N (`BatchSize`, 10..30, por defecto 20), en este orden:
  1. Arrastradas, en su orden de cola.
  2. Pendientes, según la definición de `spaced-repetition`: por nivel ascendente, a igual nivel por fecha de repaso más antigua y después por orden de catálogo.
  3. Nuevas del nivel CEFR elegido, en orden de catálogo, solo si queda hueco. El nivel CEFR filtra solo las nuevas.
- Toda entrada de la tanda nueva empieza con racha 0 y sin responder.
- Invariante de unicidad: una frase está como máximo en un sitio (tanda activa, cola de arrastradas, programada o nueva sin progreso). Si es arrastrada y pendiente a la vez, cuenta como arrastrada y no se duplica.
- Errores: las violaciones de invariantes lanzan `IllegalArgumentException` (errores de programación). Los resultados esperables son un `sealed interface`: `BatchComposition.Composed` (puede tener menos de N frases) o `BatchComposition.NothingAvailable` (no se crea tanda).
- Método `PhraseProgress.record(assessment)`, extraído de `RecordSelfAssessment`, que delega en él sin cambiar su comportamiento.
- Tipos nuevos: `BatchSize` (value class validada) en `core.domain.spacedrepetition`; `CefrLevel` y `CatalogPhrase` en el paquete `core.domain.catalog`.
- `:core:testing`: builders `aPhraseProgress(...)` y `aCatalogPhrase(...)` con valores por defecto válidos. Las combinaciones incoherentes fallan en el `require` de `PhraseProgress`; el builder no las corrige.
- Sin cambios en el cierre: `ResolveBatchClosure` y `BatchClosureOutcome` se reutilizan tal cual.

## Capabilities

### New Capabilities
- `practice-batch`: composición y ciclo de la tanda activa (lista circular, aviso de cierre, cola de arrastradas, composición de la tanda nueva y caso sin tanda). Es la especificación que el ADR-0012 deja fuera de `spaced-repetition`.

### Modified Capabilities
- Ninguna. Las reglas de racha, certificación, cierre de frases, niveles e intervalos ya están en `spaced-repetition` y no cambian. La racha inicial de la tanda nueva, que `spaced-repetition` delega en `practice-batch`, se especifica aquí.

## Impact

- `:core:domain`, paquete `spacedrepetition`: `PracticeBatch`, `CarryQueue`, `ComposeNextBatch`, `BatchComposition`, `BatchSize`, `PhraseProgress.record` y sus tests.
- `:core:domain`, paquete `catalog` (nuevo): `CefrLevel` y `CatalogPhrase`.
- `:core:testing`: builders `aPhraseProgress(...)` y `aCatalogPhrase(...)`.
- Sin dependencias nuevas: no se añade ninguna librería ni cambia `gradle/libs.versions.toml`.
- ADR: ninguno nuevo. Respeta ADR-0012 (alcance y método), ADR-0010 (dependencias hacia dentro) y ADR-0003 (sin valores de idioma fijos en el dominio).

## Fuera de alcance

- Persistencia (Room, DataStore) y puertos `XxxRepository` de progreso, catálogo y tanda activa: cambio posterior.
- Interfaz de usuario y ViewModels, incluido el aviso visual de cierre.
- `:core:platform`: adaptador de `Clock` y reconocimiento de voz.
- Casos de uso de aplicación que orquesten cierre y composición con datos reales.
- Primera tanda automática (disparador de la app).
- Corrección por voz y dirección inversa (ADR-0012).
