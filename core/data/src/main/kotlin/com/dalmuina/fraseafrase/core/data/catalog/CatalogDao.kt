package com.dalmuina.fraseafrase.core.data.catalog

import androidx.room3.Dao
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
        "SELECT * FROM catalog_phrase WHERE target_locale = :targetLocale AND source_locale = :sourceLocale " +
            "ORDER BY position",
    )
    suspend fun phrasesFor(targetLocale: String, sourceLocale: String): List<CatalogPhraseEntity>

    @Query("SELECT DISTINCT target_locale FROM catalog_phrase")
    suspend fun targetLocales(): List<String>

    @Query("SELECT DISTINCT source_locale FROM catalog_phrase WHERE target_locale = :targetLocale")
    suspend fun sourceLocalesFor(targetLocale: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeedVersion(entity: CatalogSeedVersionEntity)

    @Query("SELECT * FROM catalog_seed_version WHERE target_locale = :targetLocale")
    suspend fun seedVersionFor(targetLocale: String): CatalogSeedVersionEntity?
}
