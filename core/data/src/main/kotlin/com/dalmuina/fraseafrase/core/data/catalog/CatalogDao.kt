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

    @Insert
    suspend fun insertTranslations(translations: List<CatalogTranslationEntity>)

    @Query("DELETE FROM catalog_phrase WHERE target_locale = :targetLocale")
    suspend fun deletePhrasesForTargetLocale(targetLocale: String)

    @Query("DELETE FROM catalog_translation WHERE target_locale = :targetLocale AND source_locale = :sourceLocale")
    suspend fun deleteTranslationsFor(targetLocale: String, sourceLocale: String)

    @Query(
        "SELECT p.*, t.source, t.source_locale AS source_locale FROM catalog_phrase p " +
            "JOIN catalog_translation t ON t.id = p.id AND t.target_locale = p.target_locale " +
            "WHERE p.target_locale = :targetLocale AND t.source_locale = :sourceLocale " +
            "ORDER BY p.position",
    )
    suspend fun phrasesFor(targetLocale: String, sourceLocale: String): List<CatalogPhraseWithSource>

    @Query("SELECT DISTINCT target_locale FROM catalog_phrase")
    suspend fun targetLocales(): List<String>

    @Query("SELECT DISTINCT source_locale FROM catalog_translation WHERE target_locale = :targetLocale")
    suspend fun sourceLocalesFor(targetLocale: String): List<String>

    @Query("SELECT id FROM catalog_phrase WHERE target_locale = :targetLocale")
    suspend fun phraseIdsFor(targetLocale: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeedVersion(entity: CatalogSeedVersionEntity)

    @Query("SELECT * FROM catalog_seed_version WHERE target_locale = :targetLocale AND source_locale = :sourceLocale")
    suspend fun seedVersionFor(targetLocale: String, sourceLocale: String): CatalogSeedVersionEntity?
}

/**
 * Fila de proyección del JOIN de [CatalogDao.phrasesFor]: una frase destino con el texto de origen y el
 * `sourceLocale` de la combinación consultada (design.md - "Esquema de Room").
 */
data class CatalogPhraseWithSource(
    @Embedded val phrase: CatalogPhraseEntity,
    val source: String,
    @ColumnInfo(name = "source_locale")
    val sourceLocale: String,
)
