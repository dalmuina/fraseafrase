package com.dalmuina.fraseafrase.core.domain.settings

import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize

/** Ajustes que afectan a la práctica: nivel CEFR de la tanda y tamaño de tanda N (ADR-0021, ADR-0026). */
data class UserSettings(val cefrLevel: CefrLevel, val batchSize: BatchSize) {

    companion object {
        /** Valores por defecto de la spec `user-settings`: A1 y N igual a 20. */
        val DEFAULT = UserSettings(cefrLevel = CefrLevel.A1, batchSize = BatchSize.DEFAULT)
    }
}
