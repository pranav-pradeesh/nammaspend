package com.nammaspendai.data.repository

import com.nammaspendai.data.dao.UsageRecordDao
import com.nammaspendai.data.model.ApiUsageStatus
import com.nammaspendai.data.model.UsageRecord
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepository @Inject constructor(
    private val usageRecordDao: UsageRecordDao
) {
    suspend fun canMakeApiCall(): ApiUsageStatus {
        val today = LocalDate.now()
        val record = usageRecordDao.getByDate(today) ?: UsageRecord(date = today)
        return record.canMakeCall()
    }

    suspend fun recordApiCall(): UsageRecord {
        return usageRecordDao.recordApiCall()
    }

    suspend fun getTodayStatus(): UsageRecord {
        return usageRecordDao.getOrCreateToday()
    }

    suspend fun cleanupOldRecords(daysToKeep: Int = 30) {
        val cutoffDate = LocalDate.now().minusDays(daysToKeep.toLong())
        usageRecordDao.deleteOlderThan(cutoffDate)
    }

    suspend fun clearAll() {
        usageRecordDao.deleteAll()
    }
}
