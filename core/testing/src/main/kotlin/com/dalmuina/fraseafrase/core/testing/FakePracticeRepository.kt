package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.practice.PracticeRepository
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Fake en memoria de [PracticeRepository]. [transact] se serializa con un [Mutex]: la transición se aplica sobre la
 * instantánea actual y se guarda solo si termina, igual que una transacción de escritura en la base real.
 */
class FakePracticeRepository(initial: PracticeSnapshot = emptySnapshot()) : PracticeRepository {

    private val mutex = Mutex()
    private var state: PracticeSnapshot = initial

    override suspend fun read(): PracticeSnapshot = mutex.withLock { state }

    override suspend fun <T> transact(change: (PracticeSnapshot) -> PracticeChange<T>): T = mutex.withLock {
        val outcome = change(state)
        state = outcome.next
        outcome.result
    }

    private companion object {
        fun emptySnapshot() = PracticeSnapshot(
            progress = emptyList(),
            activeBatch = null,
            carry = CarryQueue(emptyList())
        )
    }
}
