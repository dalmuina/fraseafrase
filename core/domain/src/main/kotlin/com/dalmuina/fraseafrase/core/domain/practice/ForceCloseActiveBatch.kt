package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PracticeBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ResolveBatchClosure
import javax.inject.Inject

/**
 * Cierra la tanda activa (si hay alguna) sin comprobar ningún aviso de no certificadas (design D3 de
 * `add-settings-apply-flow`). No recibe `expectedUncertified`: es un cierre deliberado disparado desde ajustes, no
 * un cierre iniciado desde un aviso mostrado en la práctica. Sin tanda activa, no hace nada. No compone la tanda
 * siguiente: esa composición la hace [ObtainActiveBatch] la próxima vez que se pida la tanda, o [GenerateSession]
 * en el flujo de práctica.
 */
class ForceCloseActiveBatch @Inject constructor(
    private val practiceRepository: PracticeRepository,
    private val resolveBatchClosure: ResolveBatchClosure,
) {

    suspend operator fun invoke() {
        practiceRepository.transact { snapshot ->
            val batch = snapshot.batch()
                ?: return@transact PracticeChange(snapshot, Unit)
            PracticeChange(next = advanceActiveBatch(snapshot, batch, resolveBatchClosure), result = Unit)
        }
    }
}

/**
 * Mueve las entradas de [batch] de la tanda activa a la cola de arrastradas, delante de la cola sobrante existente,
 * y vacía la tanda activa. Con [resolveBatchClosure] resuelve cada frase antes de encolarla (certifica, sube de
 * nivel, arrastra las no certificadas); sin él (`null`), las entradas pasan a la cola tal cual, sin certificar ni
 * cambiar nivel ni próximo repaso (design D3/D4 de `replace-batch-closure-with-session-generation`). Transición pura
 * compartida por [ForceCloseActiveBatch], [GenerateSession] y, en un cambio posterior, `RegenerateActiveBatchForLevel`.
 */
internal fun advanceActiveBatch(
    snapshot: PracticeSnapshot,
    batch: PracticeBatch,
    resolveBatchClosure: ResolveBatchClosure?,
): PracticeSnapshot {
    if (resolveBatchClosure == null) {
        return snapshot.copy(
            activeBatch = null,
            carry = CarryQueue(snapshot.carry.ids + batch.entries.map { it.id }),
        )
    }
    val outcomes = resolveBatchClosure(batch.entries)
    val resolved = snapshot.storing(PracticeBatch(outcomes.map { it.progress }, batch.capacity))
    return resolved.copy(
        activeBatch = null,
        carry = CarryQueue.afterClosure(snapshot.carry, outcomes),
    )
}
