package com.dalmuina.fraseafrase.core.domain.practice

/**
 * Puerto del estado del usuario: progreso, tanda activa y cola de arrastradas (ADR-0013). Una fuente: Room 3.
 *
 * [transact] ejecuta la transición dentro de una única transacción de escritura: la lectura, el cálculo y la
 * escritura ocurren juntos, así que el cierre es atómico y no hay lecturas obsoletas. [change] es una función pura
 * del dominio: no hace E/S ni suspende, y solo recibe y devuelve tipos del dominio.
 */
interface PracticeRepository {

    /** Lee una instantánea coherente del estado. */
    suspend fun read(): PracticeSnapshot

    /** Aplica [change] sobre la instantánea leída y persiste el resultado en la misma transacción. */
    suspend fun <T> transact(change: (PracticeSnapshot) -> PracticeChange<T>): T
}
