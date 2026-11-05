# Proposal

## Why

ADR-0012 solo resetea la racha de una frase al fallar dentro de la tanda; su nivel de consolidación no
cambia hasta que la tanda se cierra, y solo si la frase no llega a certificarse antes de ese cierre. Una
frase de nivel alto puede fallar una vez y, si el usuario la acierta 3 veces seguidas antes de cerrar,
sube de nivel sin que el fallo deje ningún rastro. ADR-0015 sustituye esa regla: un fallo en una frase
con nivel ≥ 1 debe bajar su nivel al instante, no solo al cerrar la tanda. Este cambio implementa esa
regla en el dominio; es bloqueante para las insignias de progreso (P4), que se derivan del nivel.

## What Changes

- `PhraseProgress.record()` pasa a recibir la fecha de la respuesta. Un fallo (`SelfAssessment.MISS`) en
  una frase con nivel ≥ 1 baja su nivel en 1 (`max(nivel - 1, 0)`) y recalcula su próximo repaso con
  `ReviewSchedule.nextReviewAfter()` sobre el nivel resultante y esa fecha. Una frase de nivel 0 que falla
  no cambia de nivel (regla sin cambios: solo resetea su racha).
- `PracticeBatch.answer()` y `RecordSelfAssessment` pasan a recibir esa misma fecha y la propagan a
  `PhraseProgress.record()`.
- `AnswerPhrase` (caso de uso real que invoca `PracticeBatch.answer()`) inyecta `Clock` y le pasa
  `clock.today()`.
- `ResolveBatchClosure.carried()` deja de forzar nivel 0 a las frases respondidas y no certificadas: su
  nivel y su próximo repaso ya quedaron fijados, fallo a fallo, durante la práctica. Solo resetea racha y
  `answeredInBatch`.
- **BREAKING** (solo a nivel de dominio, sin dato persistido que migrar): una frase de nivel alto que
  falla y luego no certifica ya no vuelve a nivel 0 al cerrar la tanda; queda en su nivel decrementado.

## Fuera de alcance

- UI de `:feature:session` y `:feature:settings`: ya muestran nivel y racha leyendo `PhraseProgress`, sin
  cambios de forma.
- Persistencia (`:core:data`, Room): el esquema de `PhraseProgress` no cambia.
- Insignias de progreso (P4): este cambio solo deja el nivel correcto para que P4 las derive de él.
- Limpieza de `RecordSelfAssessment` como caso de uso huérfano (no se usa en producción); se actualiza
  solo para seguir compilando con la nueva firma de `PhraseProgress.record()`.

## Capabilities

### New Capabilities

(ninguna)

### Modified Capabilities

- `spaced-repetition`: añade el requisito "Bajada instantánea de nivel ante un fallo" y modifica dos
  requisitos existentes para que dejen de asumir que un fallo solo resetea la racha hasta el cierre:
  "Resolución de frase respondida no certificada al cerrar la tanda" (ya no fuerza nivel 0 al cerrar; el
  nivel y el repaso ya se fijaron durante la práctica) y "Frase respondida en la tanda" (su escenario
  "Frase con un fallo cuenta como respondida" decía que la frase sale arrastrada en nivel 0; con ADR-0015
  sale en el nivel que le fijó ese fallo). Detectado durante la implementación (grupo 4 de tasks.md), no
  previsto en la propuesta inicial.

## Impact

- `:core:domain`: `PhraseProgress.record()`, `PracticeBatch.answer()`, `RecordSelfAssessment`,
  `ResolveBatchClosure.carried()`, y el caso de uso `AnswerPhrase` (añade dependencia de `Clock`).
- Tests de dominio afectados: `PhraseProgressTest`, `PracticeBatchTest`, `RecordSelfAssessmentTest`,
  `ResolveBatchClosureTest`, `AnswerPhraseTest`.
- Sin cambios en `:core:data`, `:core:platform`, `:feature:settings` ni `:app`: la UI ya muestra nivel y
  racha según el estado de `PhraseProgress`, que sigue siendo la misma entidad.
- `:feature:session`: sin cambios de comportamiento ni de UI, pero el nuevo constructor de `AnswerPhrase`
  (con `Clock`) exige un ajuste mecánico de cableado en dos ficheros de test que lo construían a mano sin
  `Clock` (`PracticeSessionFixtures.kt`, `PracticeSessionRouteTest.kt`); ambos ya tienen un `Clock` en
  scope. Detectado durante la implementación, no previsto al proponer el cambio; ver design.md - Risks /
  Trade-offs.
- Sin migración de datos: el esquema de `PhraseProgress` (nivel, racha, próximo repaso) no cambia; solo
  cambia cuándo se recalculan sus valores.
