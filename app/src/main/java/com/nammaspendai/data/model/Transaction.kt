package com.nammaspendai.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["category"]),
        Index(value = ["smsHash"], unique = true)
    ]
)
data class Transaction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val merchant: String?,
    val description: String?,
    val timestamp: LocalDateTime,
    val source: TransactionSource,
    val smsHash: String? = null, // For deduplication
    val isSynced: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    val isExpense: Boolean
        get() = type == TransactionType.DEBIT

    val isIncome: Boolean
        get() = type == TransactionType.CREDIT
}

enum class TransactionType {
    DEBIT,      // Money out
    CREDIT      // Money in
}

enum class TransactionSource {
    SMS,        // Auto-detected from SMS
    VOICE,      // Voice input
    MANUAL      // Manual entry
}
