package com.nammaspendai.util

import com.nammaspendai.data.model.Transaction
import com.nammaspendai.data.model.TransactionType
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

data class PredictionResult(
    val category: String,
    val predictedAmount: Double,
    val confidence: Float, // 0.0 - 1.0
    val basedOnMonths: Int
)

@Singleton
class PredictionEngine @Inject constructor() {

    /**
     * Predict next month's spending for each category
     * Uses weighted average of last 3-6 months with recent months weighted higher
     */
    fun predictNextMonth(transactions: List<Transaction>): List<PredictionResult> {
        val now = LocalDateTime.now()
        val debits = transactions.filter { it.type == TransactionType.DEBIT }

        // Group by category and month
        val categoryMonthlyTotals = mutableMapOf<String, MutableMap<YearMonth, Double>>()

        for (tx in debits) {
            val month = YearMonth.from(tx.timestamp)
            val categoryMap = categoryMonthlyTotals.getOrPut(tx.category) { mutableMapOf() }
            categoryMap[month] = (categoryMap[month] ?: 0.0) + tx.amount
        }

        val predictions = mutableListOf<PredictionResult>()
        val currentMonth = YearMonth.now()

        for ((category, monthlyTotals) in categoryMonthlyTotals) {
            // Get last 6 months excluding current month
            val historicalMonths = (1..6).map { currentMonth.minusMonths(it.toLong()) }
            val historicalData = historicalMonths.mapNotNull { month ->
                monthlyTotals[month]?.let { month to it }
            }

            if (historicalData.isEmpty()) continue

            // Weighted average: recent months get higher weight
            val totalWeight = historicalData.indices.sumOf { (historicalData.size - it).toDouble() }
            val weightedSum = historicalData.indices.sumOf { i ->
                val weight = (historicalData.size - i).toDouble()
                historicalData[i].second * weight
            }

            val predicted = weightedSum / totalWeight
            val confidence = when (historicalData.size) {
                1 -> 0.3f
                2 -> 0.5f
                3 -> 0.65f
                4 -> 0.75f
                5 -> 0.85f
                else -> 0.9f
            }

            predictions.add(
                PredictionResult(
                    category = category,
                    predictedAmount = predicted,
                    confidence = confidence,
                    basedOnMonths = historicalData.size
                )
            )
        }

        return predictions.sortedByDescending { it.predictedAmount }
    }

    /**
     * Predict electricity bill based on Utilities category history
     */
    fun predictElectricityBill(transactions: List<Transaction>): PredictionResult? {
        val utilityKeywords = listOf("electricity", "bescom", "kseb", "mseb", "tata power", "reliance energy")
        val utilityTx = transactions.filter { tx ->
            tx.type == TransactionType.DEBIT &&
            (tx.category == "Utilities" ||
             utilityKeywords.any { kw -> (tx.merchant ?: "").contains(kw, ignoreCase = true) ||
                                         (tx.description ?: "").contains(kw, ignoreCase = true) })
        }
        return predictForCategory("Electricity", utilityTx)
    }

    /**
     * Predict LPG refill cost
     */
    fun predictLpgBill(transactions: List<Transaction>): PredictionResult? {
        val lpgKeywords = listOf("lpg", "gas", "indane", "hp gas", "bharat gas")
        val lpgTx = transactions.filter { tx ->
            tx.type == TransactionType.DEBIT &&
            lpgKeywords.any { kw -> (tx.merchant ?: "").contains(kw, ignoreCase = true) ||
                                     (tx.description ?: "").contains(kw, ignoreCase = true) }
        }
        return predictForCategory("LPG", lpgTx)
    }

    private fun predictForCategory(label: String, transactions: List<Transaction>): PredictionResult? {
        if (transactions.isEmpty()) return null

        val monthlyTotals = transactions.groupBy { YearMonth.from(it.timestamp) }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }

        val currentMonth = YearMonth.now()
        val historicalMonths = (1..6).mapNotNull { i ->
            val month = currentMonth.minusMonths(i.toLong())
            monthlyTotals[month]?.let { month to it }
        }

        if (historicalMonths.isEmpty()) return null

        val avg = historicalMonths.map { it.second }.average()
        val confidence = minOf(0.3f + (historicalMonths.size * 0.1f), 0.9f)

        return PredictionResult(
            category = label,
            predictedAmount = avg,
            confidence = confidence,
            basedOnMonths = historicalMonths.size
        )
    }

    /**
     * Get spending trend: positive = spending more, negative = spending less
     */
    fun getSpendingTrend(transactions: List<Transaction>): Double {
        val currentMonth = YearMonth.now()
        val lastMonth = currentMonth.minusMonths(1)
        val twoMonthsAgo = currentMonth.minusMonths(2)

        val debits = transactions.filter { it.type == TransactionType.DEBIT }

        val currentTotal = debits.filter { YearMonth.from(it.timestamp) == currentMonth }.sumOf { it.amount }
        val lastTotal = debits.filter { YearMonth.from(it.timestamp) == lastMonth }.sumOf { it.amount }
        val twoMonthTotal = debits.filter { YearMonth.from(it.timestamp) == twoMonthsAgo }.sumOf { it.amount }

        val avg = (lastTotal + twoMonthTotal) / 2
        return if (avg == 0.0) 0.0 else ((currentTotal - avg) / avg) * 100
    }
}
