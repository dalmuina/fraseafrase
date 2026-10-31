package com.dalmuina.fraseafrase.core.domain.settings

/**
 * Puerto de los ajustes del usuario que afectan a la práctica (spec `user-settings`). Una fuente: DataStore.
 * Nunca falla: un valor ausente, corrupto o ilegible devuelve los valores por defecto.
 */
interface SettingsRepository {

    suspend fun settings(): UserSettings
}
