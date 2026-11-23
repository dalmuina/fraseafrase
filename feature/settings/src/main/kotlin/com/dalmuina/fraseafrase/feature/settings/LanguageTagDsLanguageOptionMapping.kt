package com.dalmuina.fraseafrase.feature.settings

import com.dalmuina.fraseafrase.core.designsystem.component.DsLanguageOption
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag

/**
 * Mapea un [LanguageTag] del dominio a una [DsLanguageOption] del design system (ADR-0010: `DsLanguageOptions`
 * no conoce `LanguageTag`). El nombre a mostrar no está fijado en ninguna spec ni mock: se elige el nombre
 * nativo del idioma con su variante entre paréntesis, igual que lo haría cualquier selector de idioma/región.
 * Un código no reconocido usa el propio valor de [LanguageTag] como nombre, para no perder la opción.
 *
 * Vive en su propio fichero, separado de `SettingsScreen.kt`, para poder probarla con un test unitario de JVM
 * sin cargar el resto de la pantalla (sus funciones `@Composable`).
 */
internal fun LanguageTag.toDsLanguageOption(): DsLanguageOption = DsLanguageOption(
    code = value,
    displayName = when (value) {
        "pt-PT" -> "Português (Portugal)"
        "es-ES" -> "Español (España)"
        "it-IT" -> "Italiano (Italia)"
        "en-GB" -> "English (UK)"
        "de-DE" -> "Deutsch (Deutschland)"
        "fr-FR" -> "Français (France)"
        "ru-RU" -> "Русский (Россия)"
        else -> value
    },
)
