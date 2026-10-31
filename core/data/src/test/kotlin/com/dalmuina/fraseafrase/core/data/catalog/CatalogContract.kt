package com.dalmuina.fraseafrase.core.data.catalog

/**
 * Regla de permanencia de los ids del catálogo (ADR-0013): un id publicado no se retira ni cambia de posición, y las
 * frases nuevas van al final. Es una comprobación de CI sobre el fichero de ids publicados, no código de producción.
 */
object CatalogContract {

    /** Ids publicados que ya no aparecen en el catálogo actual. Una lista no vacía rompe la regla. */
    fun retiredIds(released: List<String>, current: List<String>): List<String> {
        val currentIds = current.toSet()
        return released.filterNot { it in currentIds }
    }

    /** Los ids publicados son el prefijo del catálogo actual, en el mismo orden. */
    fun keepsReleasedPrefix(released: List<String>, current: List<String>): Boolean =
        current.take(released.size) == released
}
