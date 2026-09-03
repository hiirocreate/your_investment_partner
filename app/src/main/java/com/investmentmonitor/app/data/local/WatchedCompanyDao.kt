package com.investmentmonitor.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedCompanyDao {
    @Query("SELECT * FROM watched_companies ORDER BY addedAtEpochMillis DESC")
    fun observeAll(): Flow<List<WatchedCompanyEntity>>

    @Query("SELECT * FROM watched_companies WHERE companyId = :companyId")
    suspend fun findById(companyId: String): WatchedCompanyEntity?

    @Query("SELECT companyId FROM watched_companies")
    suspend fun allIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WatchedCompanyEntity)

    @Update
    suspend fun update(entity: WatchedCompanyEntity)

    @Query("DELETE FROM watched_companies WHERE companyId = :companyId")
    suspend fun deleteById(companyId: String)

    @Delete
    suspend fun delete(entity: WatchedCompanyEntity)
}
