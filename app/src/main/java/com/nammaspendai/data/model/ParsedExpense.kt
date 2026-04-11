package com.nammaspendai.data.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

/**
 * Data class for AI-parsed expenses from voice input
 */
data class ParsedExpense(
    @SerializedName("amount")
    val amount: Double,

    @SerializedName("type")
    val type: String, // "expense" or "income"

    @SerializedName("category")
    val category: String? = null,

    @SerializedName("merchant")
    val merchant: String? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("timestamp")
    val timestamp: String? = null // ISO-8601 format
) {
    fun toTransaction(source: TransactionSource = TransactionSource.VOICE): Transaction {
        return Transaction(
            amount = amount,
            type = if (type.lowercase() == "income") TransactionType.CREDIT else TransactionType.DEBIT,
            category = category ?: "Other",
            merchant = merchant,
            description = description,
            timestamp = timestamp?.let {
                try {
                    LocalDateTime.parse(it)
                } catch (e: Exception) {
                    LocalDateTime.now()
                }
            } ?: LocalDateTime.now(),
            source = source
        )
    }
}

data class ParsedExpensesResponse(
    @SerializedName("expenses")
    val expenses: List<ParsedExpense>
)
