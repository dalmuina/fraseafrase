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

    @Query("SELECT * FROM phrase_progress")
    suspend fun all(): List<PhraseProgressEntity>
}

@Dao
interface ActiveBatchDao {

    @Insert
    suspend fun insert(entity: ActiveBatchEntity)

    @Update
    suspend fun update(entity: ActiveBatchEntity)

    @Query("SELECT * FROM active_batch")
    suspend fun all(): List<ActiveBatchEntity>

    @Query("DELETE FROM active_batch")
    suspend fun clear()
}
