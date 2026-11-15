package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.practice.PracticeRepository
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Fake en memoria de [PracticeRepository], con un estado independiente por `targetLocale`. [transact] se
 * serializa con un [Mutex]: la transición se aplica sobre la instantánea actual de ese idioma destino y se
 * guarda solo si termina, igual que una transacción de escritura en la base real. Los métodos sin
 * `targetLocale` son un atajo para los tests que solo ejercitan [DEFAULT_LOCALE].
 */
class FakePracticeRepository(initial: PracticeSnapshot = emptySnapshot()) : PracticeRepository {

    private val mutex = Mutex()
    private val state = mutableMapOf(DEFAULT_LOCALE to initial)

    override suspend fun read(targetLocale: LanguageTag): PracticeSnapshot = mutex.withLock {
        state[targetLocale] ?: emptySnapshot()
    }

    override suspend fun <T> transact(
        targetLocale: LanguageTag,
        change: (PracticeSnapshot) -> PracticeChange<T>,
    ): T = mutex.withLock {
        val outcome = change(state[targetLocale] ?: emptySnapshot())
        state[targetLocale] = outcome.next
        outcome.result
    }

    /** Atajo para los tests que solo ejercitan [DEFAULT_LOCALE]. */
    suspend fun read(): PracticeSnapshot = read(DEFAULT_LOCALE)

    /** Atajo para los tests que solo ejercitan [DEFAULT_LOCALE]. */
    suspend fun <T> transact(change: (PracticeSnapshot) -> PracticeChange<T>): T = transact(DEFAULT_LOCALE, change)

    companion object {
        val DEFAULT_LOCALE = LanguageTag("pt-PT")

        private fun emptySnapshot() = aPracticeSnapshot()
    }
}
