package com.dalmuina.fraseafrase.core.domain.catalog

/**
 * Resolves the source language (ADR-0003, ADR-0032) from the ordered [device] languages. For each one, in order,
 * it picks the identical tag in [available] or, failing that, the alphabetically first one with the same base
 * language. If none matches, it returns [default]. Pure function, with no language literals.
 */
object ResolveSourceLocale {

    operator fun invoke(device: List<LanguageTag>, default: LanguageTag, available: List<LanguageTag>): LanguageTag =
        device.firstNotNullOfOrNull { match(it, available) } ?: default

    private fun match(candidate: LanguageTag, available: List<LanguageTag>): LanguageTag? =
        candidate.takeIf { it in available }
            ?: available.filter { it.language == candidate.language }.minByOrNull { it.value }
}
