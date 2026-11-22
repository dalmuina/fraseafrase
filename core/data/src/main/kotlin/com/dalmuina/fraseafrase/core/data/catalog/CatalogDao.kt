package com.dalmuina.fraseafrase.core.data.catalog

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface CatalogDao {

    @Insert
    suspend fun insertPhrases(phrases: List<CatalogPhraseEntity>)

    @Query("DELETE FROM catalog_phrase WHERE target_locale = :targetLocale")
    suspend fun deletePhrasesForTargetLocale(targetLocale: String)

    @Query(
        "SELECT p.*, s.target AS source, s.target_locale AS source_locale FROM catalog_phrase p " +
            "JOIN catalog_phrase s ON s.id = p.id " +
            "WHERE p.target_locale = :targetLocale AND s.target_locale = :sourceLocale " +
            "ORDER BY p.position",
    )
    suspend fun phrasesFor(targetLocale: String, sourceLocale: String): List<CatalogPhraseWithSource>

    @Query("SELECT DISTINCT target_locale FROM catalog_phrase")
    suspend fun targetLocales(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeedVersion(entity: CatalogSeedVersionEntity)

    @Query("SELECT * FROM catalog_seed_version WHERE target_locale = :targetLocale")
    suspend fun seedVersionFor(targetLocale: String): CatalogSeedVersionEntity?
}

/**
 * Fila de proyección del self-JOIN de [CatalogDao.phrasesFor]: una frase del idioma destino con el texto de origen
 * (el `target` de la frase con el mismo id en el idioma de origen) y el `sourceLocale` consultado (ADR-0023).
 */
data class CatalogPhraseWithSource(
    @Embedded val phrase: CatalogPhraseEntity,
    val source: String,
    @ColumnInfo(name = "source_locale")
    val sourceLocale: String,
)
