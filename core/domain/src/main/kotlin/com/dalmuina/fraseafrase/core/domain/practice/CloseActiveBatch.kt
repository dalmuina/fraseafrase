package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PracticeBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ResolveBatchClosure
import javax.inject.Inject

/**
 * Cierra la tanda activa (ADR-0012, spec `practice-persistence`). Resuelve cada frase, guarda el progreso, la cola de
 * arrastradas y la desactivación de la tanda en una única transacción. **No compone la tanda siguiente**: esa
 * composición la hace [ObtainActiveBatch] la próxima vez que se pida la tanda.
 *
 * El orden de comprobación es: primero que haya tanda activa, y después que el número de no certificadas coincida con
 * el aviso que vio el usuario. Si no coincide, la tanda cambió desde el aviso y no se cierra.
 */
class CloseActiveBatch @Inject constructor(
    private val practiceRepository: PracticeRepository,
    private val resolveBatchClosure: ResolveBatchClosure,
) {

    suspend operator fun invoke(expectedUncertified: Int): CloseBatchResult =
        practiceRepository.transact { snapshot ->
            val batch = snapshot.batch()
                ?: return@transact PracticeChange(snapshot, CloseBatchResult.NoActiveBatch)
            if (batch.uncertifiedCount() != expectedUncertified) {
                return@transact PracticeChange(snapshot, CloseBatchResult.Stale)
            }
            val outcomes = resolveBatchClosure(batch.entries)
            val resolved = snapshot.storing(PracticeBatch(outcomes.map { it.progress }, batch.capacity))
            PracticeChange(
                next = resolved.copy(
                    activeBatch = null,
                    carry = CarryQueue.afterClosure(snapshot.carry, outcomes),
                ),
                result = CloseBatchResult.Closed,
            )
        }
}

/** Resultado de cerrar la tanda. `Closed` no incluye tanda nueva: la compone [ObtainActiveBatch]. */
sealed interface CloseBatchResult {

    data object Closed : CloseBatchResult

    /** El número de no certificadas cambió desde el aviso. No se modifica nada. */
    data object Stale : CloseBatchResult

    /** No hay tanda activa. Tiene prioridad sobre la expectativa. No se modifica nada. */
    data object NoActiveBatch : CloseBatchResult
}
