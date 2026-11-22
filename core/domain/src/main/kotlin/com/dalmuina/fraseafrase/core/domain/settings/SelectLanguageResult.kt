package com.dalmuina.fraseafrase.core.domain.settings

/** Resultado de [SelectActiveLanguage]: el idioma se guardó o se rechazó por ser el idioma de origen. */
sealed interface SelectLanguageResult {
    data object Selected : SelectLanguageResult
    data object RejectedSourceLocale : SelectLanguageResult
}
