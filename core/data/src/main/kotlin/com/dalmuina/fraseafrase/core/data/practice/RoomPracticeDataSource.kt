package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import androidx.room3.withReadTransaction
import com.dalmuina.fraseafrase.core.data.di.IoDispatcher
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.BatchOutcome
import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.practice.PracticeRepository
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Fuente Room del estado de práctica, independiente por idioma destino (ADR-0013, ADR-0019). La lectura va en una
 * transacción de lectura. La escritura va en una transacción IMMEDIATE: la transición se calcula dentro, con la
 * instantánea que se acaba de leer, y se escribe en dos fases para no chocar con los UNIQUE de posición. Ambas
 * transacciones están acotadas a [LanguageTag] recibido: ningún paso lee ni escribe filas de otro idioma destino.
 */
class RoomPracticeDataSource @Inject constructor(
    private val database: PracticeDatabase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : PracticeRepository {

    override suspend fun read(targetLocale: LanguageTag): PracticeSnapshot = withContext(dispatcher) {
        database.withReadTransaction { readSnapshot(targetLocale.value) }
    }

    override suspend fun <T> transact(
        targetLocale: LanguageTag,
        change: (PracticeSnapshot) -> PracticeChange<T>,
    ): T =
        withContext(dispatcher) {
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    val locale = targetLocale.value
                    val current = readSnapshot(locale)
                    val outcome = change(current)
                    writeSnapshot(locale, next = outcome.next)
                    outcome.result
                }
            }
        }

    private suspend fun readSnapshot(locale: String): PracticeSnapshot {
        val rows = database.phraseProgressDao().all(locale)
        val activeRow = database.activeBatchDao().byLocale(locale)
        val activeBatch = activeRow?.let { row ->
            ActiveBatch(
                order = orderedIds(rows) { it.batchPosition },
                capacity = row.capacity(),
            )
        }
        val sessionCount = database.sessionCounterDao().byLocale(locale)?.sessionCount ?: 0
        return PracticeSnapshot(
            progress = rows.map { it.toDomain() },
            activeBatch = activeBatch,
            sessionCount = sessionCount,
            batchOutcome = activeRow?.toBatchOutcome() ?: BatchOutcome(),
        )
    }

    /** Ids de las filas que ocupan una posición, en el orden de esa posición. */
    private fun orderedIds(rows: List<PhraseProgressEntity>, position: (PhraseProgressEntity) -> Int?): List<PhraseId> =
        rows
            .filter { position(it) != null }
            .sortedBy(position)
            .map { PhraseId(it.phraseId) }

    /**
     * Escribe [next] en dos fases para el idioma destino [locale]. Fase 1: cada fila que deja la tanda activa recibe
     * una posición temporal negativa y única, así ninguna posición final queda ocupada por otra fila. Fase 2: cada
     * fila recibe su posición final. Una fila nunca pierde y gana la posición a la vez, así que los triggers de cada
     * sentencia se cumplen.
     */
    private suspend fun writeSnapshot(locale: String, next: PracticeSnapshot) {
        val finalBatch = next.activeBatch?.order.orEmpty().withIndex().associate { it.value to it.index }
        val phrases = database.phraseProgressDao()
        val current = phrases.all(locale).associateBy { PhraseId(it.phraseId) }

        var temporary = -1
        current.values.forEach { row ->
            val id = PhraseId(row.phraseId)
            val leavesBatch = row.batchPosition != null && finalBatch[id] != row.batchPosition
            if (leavesBatch) {
                phrases.update(row.copy(batchPosition = temporary--))
            }
        }

        // Solo se escriben las filas que cambian (design D3). Una fila movida en la fase 1 siempre difiere de su
        // original, así que nunca se omite por error.
        next.progress.forEach { progress ->
            val entity = progress.toEntity(targetLocale = locale, batchPosition = finalBatch[progress.id])
            val row = current[progress.id]
            when {
                row == null -> phrases.insert(entity)
                row != entity -> phrases.update(entity)
            }
        }

        writeActiveBatch(locale, next.activeBatch?.capacity?.value, next.batchOutcome)
        writeSessionCounter(locale, next.sessionCount)
    }

    private suspend fun writeActiveBatch(locale: String, capacity: Int?, batchOutcome: BatchOutcome) {
        val activeBatches = database.activeBatchDao()
        val existing = activeBatches.byLocale(locale)
        when {
            capacity == null -> if (existing != null) activeBatches.clear(locale)
            else -> {
                val entity = ActiveBatchEntity(
                    targetLocale = locale,
                    capacity = capacity,
                    hits = batchOutcome.hits,
                    misses = batchOutcome.misses,
                    levelUps = batchOutcome.levelUps,
                )
                when {
                    existing == null -> activeBatches.insert(entity)
                    existing != entity -> activeBatches.update(entity)
                }
            }
        }
    }

    private suspend fun writeSessionCounter(locale: String, sessionCount: Int) {
        val sessionCounters = database.sessionCounterDao()
        val existing = sessionCounters.byLocale(locale)
        when {
            existing == null ->
                sessionCounters.insert(SessionCounterEntity(targetLocale = locale, sessionCount = sessionCount))
            existing.sessionCount != sessionCount ->
                sessionCounters.update(SessionCounterEntity(targetLocale = locale, sessionCount = sessionCount))
        }
    }
}
