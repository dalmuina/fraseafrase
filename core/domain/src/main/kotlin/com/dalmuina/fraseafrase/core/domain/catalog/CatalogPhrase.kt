package com.dalmuina.fraseafrase.core.domain.catalog

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId

/**
 * Frase del catálogo precargado: su identificador y su nivel CEFR (ADR-0012). El orden de la lista de
 * catálogo se usa solo como desempate determinista en la composición de la tanda.
 */
data class CatalogPhrase(val id: PhraseId, val level: CefrLevel)
