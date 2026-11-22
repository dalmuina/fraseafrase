package com.dalmuina.fraseafrase.core.data.catalog

/**
 * Regla de permanencia de los ids del catálogo (ADR-0013): un id publicado no se retira ni cambia de posición, y las
 * frases nuevas van al final. Es una comprobación de CI sobre el fichero de ids publicados, no código de producción.
 */
data class IdMismatch(val missing: List<String>, val extra: List<String>)

object CatalogContract {

    /** Ids publicados que ya no aparecen en el catálogo actual. Una lista no vacía rompe la regla. */
    fun retiredIds(released: List<String>, current: List<String>): List<String> {
        val currentIds = current.toSet()
        return released.filterNot { it in currentIds }
    }

    /**
     * Ids paralelos entre idiomas: todos los documentos deben tener el mismo conjunto de ids. Por cada idioma que no
     * lo cumple devuelve los ids que le faltan respecto a la unión de todos y los que solo él tiene (ningún otro
     * documento los contiene). Un mapa vacío significa que el contrato se cumple.
     */
    fun parallelIdMismatches(docs: Map<String, List<String>>): Map<String, IdMismatch> {
        val union = docs.values.flatten().toSet()
        return docs.mapNotNull { (locale, ids) ->
            val own = ids.toSet()
            val others = docs.filterKeys { it != locale }.values.flatten().toSet()
            val mismatch = IdMismatch(
                missing = union.filterNot { it in own },
                extra = ids.filterNot { it in others }.distinct(),
            )
            if (mismatch.missing.isEmpty() && mismatch.extra.isEmpty()) null else locale to mismatch
        }.toMap()
    }

    /** Los ids publicados son el prefijo del catálogo actual, en el mismo orden. */
    fun keepsReleasedPrefix(released: List<String>, current: List<String>): Boolean =
        current.take(released.size) == released
}
