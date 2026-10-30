package com.dalmuina.fraseafrase.core.domain.time

import java.time.LocalDate

/**
 * Puerto de tiempo del dominio (ADR-0010). Devuelve la fecha local del dispositivo.
 *
 * Nombre distinto del tipo homónimo de la biblioteca estándar, a propósito: el código del dominio
 * nunca importa esa clase ni lee el reloj del sistema directamente.
 * La implementación de sistema vive en `:core:platform` (cambio posterior).
 */
interface Clock {
    fun today(): LocalDate
}
