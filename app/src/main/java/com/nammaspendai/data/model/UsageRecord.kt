package com.nammaspendai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "usage_records")
data class UsageRecord(
    @PrimaryKey
    val date: LocalDate,
    val hourlyCalls: MutableMap<Int, Int> = mutableMapOf(), // hour -> count
    val dailyCallCount: Int = 0,
    val lastCallTimestamp: LocalDateTime? = null
) {
    fun canMakeCall(): ApiUsageStatus {
        val now = LocalDateTime.now()
        val currentHour = now.hour

        // Check daily limit (20 calls/day)
        if (dailyCallCount >= DAILY_LIMIT) {
            return ApiUsageStatus.Blocked(
                reason = "Daily limit reached ($DAILY_LIMIT calls/day)",
                retryAfter = date.plusDays(1).atStartOfDay()
            )
        }

        // Check hourly limit (10 calls/hour)
        val hourlyCount = hourlyCalls[currentHour] ?: 0
        if (hourlyCount >= HOURLY_LIMIT) {
            return ApiUsageStatus.Blocked(
                reason = "Hourly limit reached ($HOURLY_LIMIT calls/hour)",
                retryAfter = now.withHour(currentHour + 1).withMinute(0).withSecond(0)
            )
        }

        // Check cooldown (10 seconds between calls)
        lastCallTimestamp?.let { lastCall ->
            val secondsSinceLastCall = java.time.Duration.between(lastCall, now).seconds
            if (secondsSinceLastCall < COOLDOWN_SECONDS) {
                return ApiUsageStatus.Blocked(
                    reason = "Please wait ${COOLDOWN_SECONDS - secondsSinceLastCall}s before next call",
                    retryAfter = lastCall.plusSeconds(COOLDOWN_SECONDS.toLong())
                )
            }
        }

        return ApiUsageStatus.Allowed(
            remainingHourly = HOURLY_LIMIT - hourlyCount,
            remainingDaily = DAILY_LIMIT - dailyCallCount
        )
    }

    fun recordCall(): UsageRecord {
        val now = LocalDateTime.now()
        val currentHour = now.hour
        val newHourlyCalls = hourlyCalls.toMutableMap()
        newHourlyCalls[currentHour] = (newHourlyCalls[currentHour] ?: 0) + 1
        return copy(
            hourlyCalls = newHourlyCalls,
            dailyCallCount = dailyCallCount + 1,
            lastCallTimestamp = now
        )
    }

    companion object {
        const val HOURLY_LIMIT = 10
        const val DAILY_LIMIT = 20
        const val COOLDOWN_SECONDS = 10
    }
}

sealed class ApiUsageStatus {
    data class Allowed(
        val remainingHourly: Int,
        val remainingDaily: Int
    ) : ApiUsageStatus()

    data class Blocked(
        val reason: String,
        val retryAfter: LocalDateTime
    ) : ApiUsageStatus()
}
