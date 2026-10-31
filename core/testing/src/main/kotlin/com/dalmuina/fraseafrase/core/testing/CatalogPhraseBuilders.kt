package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId

/**
 * Crea una [CatalogPhrase] de prueba con nivel CEFR A1 por defecto, como fijan los escenarios de composición.
 */
fun aCatalogPhrase(id: String = "phrase-1", level: CefrLevel = CefrLevel.A1): CatalogPhrase =
    CatalogPhrase(id = PhraseId(id), level = level)
