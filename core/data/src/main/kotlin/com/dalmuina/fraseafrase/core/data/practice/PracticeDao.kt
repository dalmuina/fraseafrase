package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update

@Dao
interface PhraseProgressDao {

    @Insert
    suspend fun insert(entity: PhraseProgressEntity)

    @Update
    suspend fun update(entity: PhraseProgressEntity)

    @Query("SELECT * FROM phrase_progress WHERE target_locale = :targetLocale")
    suspend fun all(targetLocale: String): List<PhraseProgressEntity>
}

@Dao
interface ActiveBatchDao {

    @Insert
    suspend fun insert(entity: ActiveBatchEntity)

    @Update
    suspend fun update(entity: ActiveBatchEntity)

    @Query("SELECT * FROM active_batch WHERE target_locale = :targetLocale")
    suspend fun byLocale(targetLocale: String): ActiveBatchEntity?

    @Query("DELETE FROM active_batch WHERE target_locale = :targetLocale")
    suspend fun clear(targetLocale: String)
}

@Dao
interface SessionCounterDao {

    @Insert
    suspend fun insert(entity: SessionCounterEntity)

    @Update
    suspend fun update(entity: SessionCounterEntity)

    @Query("SELECT * FROM session_counter WHERE target_locale = :targetLocale")
    suspend fun byLocale(targetLocale: String): SessionCounterEntity?
}
