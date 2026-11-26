package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag

/**
 * Puerto del estado del usuario: progreso, tanda activa y contador de sesiones, independiente por idioma
 * destino (ADR-0013, ADR-0019). Una fuente: Room 3.
 *
 * [transact] ejecuta la transición dentro de una única transacción de escritura: la lectura, el cálculo y la
 * escritura ocurren juntos, así que el cierre es atómico y no hay lecturas obsoletas. [change] es una función pura
 * del dominio: no hace E/S ni suspende, y solo recibe y devuelve tipos del dominio.
 */
interface PracticeRepository {

    /** Lee una instantánea coherente del estado de [targetLocale]. */
    suspend fun read(targetLocale: LanguageTag): PracticeSnapshot

    /** Aplica [change] sobre la instantánea de [targetLocale] y persiste el resultado en la misma transacción. */
    suspend fun <T> transact(targetLocale: LanguageTag, change: (PracticeSnapshot) -> PracticeChange<T>): T
}
