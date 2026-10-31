package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import androidx.room3.withReadTransaction
import com.dalmuina.fraseafrase.core.data.di.IoDispatcher
import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.practice.PracticeRepository
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Fuente Room del estado de práctica (ADR-0013). La lectura va en una transacción de lectura. La escritura va en una
 * transacción IMMEDIATE: la transición se calcula dentro, con la instantánea que se acaba de leer, y se escribe en
 * dos fases para no chocar con los UNIQUE de posición.
 */
class RoomPracticeDataSource @Inject constructor(
    private val database: PracticeDatabase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : PracticeRepository {

    override suspend fun read(): PracticeSnapshot = withContext(dispatcher) {
        database.withReadTransaction { readSnapshot() }
    }

    override suspend fun <T> transact(change: (PracticeSnapshot) -> PracticeChange<T>): T =
        withContext(dispatcher) {
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    val current = readSnapshot()
                    val outcome = change(current)
                    writeSnapshot(next = outcome.next)
                    outcome.result
                }
            }
        }

    private suspend fun readSnapshot(): PracticeSnapshot {
        val rows = database.phraseProgressDao().all()
        val activeRow = database.activeBatchDao().all().singleOrNull()
        val activeBatch = activeRow?.let { row ->
            ActiveBatch(
                order = orderedIds(rows) { it.batchPosition },
                capacity = row.capacity(),
            )
        }
        return PracticeSnapshot(
            progress = rows.map { it.toDomain() },
            activeBatch = activeBatch,
            carry = CarryQueue(orderedIds(rows) { it.carryPosition }),
        )
    }

    /** Ids de las filas que ocupan una posición, en el orden de esa posición. */
    private fun orderedIds(rows: List<PhraseProgressEntity>, position: (PhraseProgressEntity) -> Int?): List<PhraseId> =
        rows
            .filter { position(it) != null }
            .sortedBy(position)
            .map { PhraseId(it.phraseId) }

    /**
     * Escribe [next] en dos fases. Fase 1: cada fila que deja una posición ocupada recibe una posición temporal
     * negativa y única, así ninguna posición final queda ocupada por otra fila. Fase 2: cada fila recibe su posición
     * final. Una fila nunca pierde sus dos posiciones a la vez, así que los triggers de cada sentencia se cumplen.
     */
    private suspend fun writeSnapshot(next: PracticeSnapshot) {
        val finalBatch = next.activeBatch?.order.orEmpty().withIndex().associate { it.value to it.index }
        val finalCarry = next.carry.ids.withIndex().associate { it.value to it.index }
        val phrases = database.phraseProgressDao()
        val current = phrases.all().associateBy { PhraseId(it.phraseId) }

        var temporary = -1
        current.values.forEach { row ->
            val id = PhraseId(row.phraseId)
            val leavesBatch = row.batchPosition != null && finalBatch[id] != row.batchPosition
            val leavesCarry = row.carryPosition != null && finalCarry[id] != row.carryPosition
            if (leavesBatch || leavesCarry) {
                phrases.update(
                    row.copy(
                        batchPosition = if (leavesBatch) temporary-- else row.batchPosition,
                        carryPosition = if (leavesCarry) temporary-- else row.carryPosition,
                    ),
                )
            }
        }

        // Solo se escriben las filas que cambian (design D3). Una fila movida en la fase 1 siempre difiere de su
        // original, así que nunca se omite por error.
        next.progress.forEach { progress ->
            val entity = progress.toEntity(
                batchPosition = finalBatch[progress.id],
                carryPosition = finalCarry[progress.id],
            )
            val row = current[progress.id]
            when {
                row == null -> phrases.insert(entity)
                row != entity -> phrases.update(entity)
            }
        }

        writeActiveBatch(next.activeBatch?.capacity?.value)
    }

    private suspend fun writeActiveBatch(capacity: Int?) {
        val activeBatches = database.activeBatchDao()
        val existing = activeBatches.all().firstOrNull()
        when {
            capacity == null -> if (existing != null) activeBatches.clear()
            existing == null -> activeBatches.insert(ActiveBatchEntity(id = 1, capacity = capacity))
            existing.capacity != capacity -> activeBatches.update(ActiveBatchEntity(id = 1, capacity = capacity))
        }
    }
}
