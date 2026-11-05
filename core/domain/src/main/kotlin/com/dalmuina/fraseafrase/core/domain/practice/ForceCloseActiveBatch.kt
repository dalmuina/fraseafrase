package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ResolveBatchClosure
import javax.inject.Inject

/**
 * Cierra la tanda activa (si hay alguna) sin comprobar ningún aviso de no certificadas (design D3 de
 * `add-settings-apply-flow`). A diferencia de [CloseActiveBatch], no recibe `expectedUncertified`: es un cierre
 * deliberado disparado desde ajustes, no un cierre iniciado desde un aviso mostrado en la práctica. Sin tanda
 * activa, no hace nada.
 */
class ForceCloseActiveBatch @Inject constructor(
    private val practiceRepository: PracticeRepository,
    private val resolveBatchClosure: ResolveBatchClosure,
) {

    suspend operator fun invoke() {
        practiceRepository.transact { snapshot ->
            val batch = snapshot.batch()
                ?: return@transact PracticeChange(snapshot, Unit)
            PracticeChange(next = closeBatch(snapshot, batch, resolveBatchClosure), result = Unit)
        }
    }
}
