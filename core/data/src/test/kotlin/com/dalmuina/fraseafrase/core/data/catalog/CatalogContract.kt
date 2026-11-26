package com.dalmuina.fraseafrase.core.data.catalog

data class IdMismatch(val missing: List<String>, val extra: List<String>)

data class IdViolation(val id: String, val reason: IdViolationReason)

enum class IdViolationReason { MALFORMED, UNKNOWN_BLOCK, UNKNOWN_TOPIC }

/**
 * Reglas de los ids del catálogo. Permanencia (ADR-0013): un id publicado no se retira ni cambia de posición, y las
 * frases nuevas van al final. Formato (ADR-0029): `<bloque>-<nivel>-<tema>-<nnn>`, con el bloque y el tema registrados.
 * Son comprobaciones de CI, no código de producción.
 */
object CatalogContract {

    private val idFormat = Regex("^([0-9]{4})-(a1|a2|b1|b2|c1)-([a-z]+(?:_[a-z]+)*)-(?!000)([0-9]{3})$")

    /** Ids que no cumplen el formato o usan un bloque o un tema sin registrar, con el primer motivo encontrado. */
    fun idFormatViolations(ids: List<String>, blocks: Set<String>, topics: Set<String>): List<IdViolation> =
        ids.mapNotNull { id ->
            val parts = idFormat.matchEntire(id)?.groupValues
            val reason = when {
                parts == null -> IdViolationReason.MALFORMED
                parts[1] !in blocks -> IdViolationReason.UNKNOWN_BLOCK
                parts[3] !in topics -> IdViolationReason.UNKNOWN_TOPIC
                else -> null
            }
            reason?.let { IdViolation(id, it) }
        }

    /**
     * Ids no publicados cuyo nivel o tema no coinciden con los de la frase. Las publicadas pueden reclasificarse sin
     * cambiar de id, y los ids mal formados ya los detecta [idFormatViolations].
     */
    fun birthLabelMismatches(phrases: List<TargetPhrase>, released: Set<String>): List<String> =
        phrases.filter { phrase ->
            val parts = idFormat.matchEntire(phrase.id.value)?.groupValues
            phrase.id.value !in released &&
                parts != null &&
                (parts[2] != phrase.level.name.lowercase() || parts[3] != phrase.topic.value)
        }.map { it.id.value }

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
