package com.nammaspendai.data.dao

import androidx.room.*
import com.nammaspendai.data.model.UsageRecord
import java.time.LocalDate

@Dao
interface UsageRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: UsageRecord)

    @Update
    suspend fun update(record: UsageRecord)

    @Query("SELECT * FROM usage_records WHERE date = :date")
    suspend fun getByDate(date: LocalDate): UsageRecord?

    @Query("SELECT * FROM usage_records ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): UsageRecord?

    @Query("DELETE FROM usage_records WHERE date < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: LocalDate)

    @Query("DELETE FROM usage_records")
    suspend fun deleteAll()

    @Transaction
    open suspend fun recordApiCall(): UsageRecord {
        val today = LocalDate.now()
        val record = getByDate(today) ?: UsageRecord(date = today)
        val updated = record.recordCall()
        insert(updated)
        return updated
    }

    @Transaction
    open suspend fun getOrCreateToday(): UsageRecord {
        val today = LocalDate.now()
        return getByDate(today) ?: UsageRecord(date = today).also { insert(it) }
    }
}
