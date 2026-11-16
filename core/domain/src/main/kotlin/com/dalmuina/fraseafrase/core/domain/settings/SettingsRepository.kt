package com.dalmuina.fraseafrase.core.domain.settings

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag

/**
 * Puerto de los ajustes del usuario que afectan a la práctica, independientes por idioma destino, y del
 * idioma destino activo (spec `user-settings`, ADR-0019). Una fuente: DataStore. Nunca falla: un valor
 * ausente, corrupto o ilegible devuelve los valores por defecto.
 */
interface SettingsRepository {

    suspend fun settings(targetLocale: LanguageTag): UserSettings

    /** Sustituye los ajustes guardados de [targetLocale] (spec `user-settings`, "Guardar sustituye..."). */
    suspend fun save(targetLocale: LanguageTag, settings: UserSettings)

    /** El idioma destino activo; el primero disponible si no hay ninguno guardado o ya no está disponible. */
    suspend fun activeTargetLocale(): LanguageTag

    /** Cambia el idioma destino activo, sin modificar los ajustes de ningún idioma destino. */
    suspend fun setActiveTargetLocale(targetLocale: LanguageTag)
}
