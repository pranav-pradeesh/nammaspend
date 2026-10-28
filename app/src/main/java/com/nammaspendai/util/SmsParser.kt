package com.nammaspendai.util

import com.nammaspendai.data.model.Transaction
import com.nammaspendai.data.model.TransactionSource
import com.nammaspendai.data.model.TransactionType
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.regex.Pattern

object SmsParser {

    // Patterns to detect transactional messages
    private val TRANSACTIONAL_KEYWORDS = listOf(
        "debited", "credited", "debit", "credit",
        "inr", "rs", "₹", "amount",
        "transaction", "payment", "purchase",
        "withdrawn", "deposit", "transfer",
        "upi", "neft", "rtgs", "imps"
    )

    // Patterns to ignore
    private val IGNORE_KEYWORDS = listOf(
        "otp", "verification", "code is", "password",
        "promo", "offer", "discount", "sale",
        "subscription", "renewal reminder",
        "do not share", "never share"
    )

    // Amount patterns - supports various formats
    private val AMOUNT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:inr|rs\\.?|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(?:amount|amt)\\s*(?:of|is)?\\s*[:\\s]*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(?:₹|inr|rs)\\s*([\\d,]+(?:\\.\\d{1,2})?)"),
        Pattern.compile("(?i)([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:inr|rs\\.?|₹)", Pattern.CASE_INSENSITIVE)
    )

    // Transaction type patterns
    private val DEBIT_PATTERNS = listOf(
        "debited", "debit", "withdrawn", "payment", "purchase",
        "spent", "paid", "deducted", "charged"
    )

    private val CREDIT_PATTERNS = listOf(
        "credited", "credit", "deposit", "received",
        "added", "refund", "cashback"
    )

    // Merchant extraction patterns
    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:at|from|to|via|using)\\s+([A-Za-z0-9\\s]+?)(?:\\s+on|\\s+for|\\.|\\,|$)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(?:merchant|payee|beneficiary)[:\\s]+([A-Za-z0-9\\s]+?)(?:\\.|\\,|$)", Pattern.CASE_INSENSITIVE)
    )

    // Date patterns
    private val DATE_PATTERNS = listOf(
        Pattern.compile("\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})\\b"), // DD/MM/YYYY or DD-MM-YYYY
        Pattern.compile("\\b(\\d{1,2})\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+(\\d{2,4})\\b", Pattern.CASE_INSENSITIVE), // 12 Jan 2024
        Pattern.compile("\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+(\\d{1,2})[,\\s]+(\\d{4})\\b", Pattern.CASE_INSENSITIVE) // Jan 12, 2024
    )

    /**
     * Check if SMS is a transactional message
     */
    fun isTransactional(smsBody: String): Boolean {
        val lowercaseBody = smsBody.lowercase()

        // Check ignore patterns first
        for (keyword in IGNORE_KEYWORDS) {
            if (lowercaseBody.contains(keyword)) {
                return false
            }
        }

        // Check for transactional keywords
        val transactionalMatches = TRANSACTIONAL_KEYWORDS.count { keyword ->
            lowercaseBody.contains(keyword)
        }

        // Must have at least 2 transactional keywords to be considered valid
        return transactionalMatches >= 2
    }

    /**
     * Parse SMS into Transaction object
     * Returns null if parsing fails
     */
    fun parse(smsBody: String, sender: String?, timestamp: Long): Transaction? {
        if (!isTransactional(smsBody)) {
            return null
        }

        val amount = extractAmount(smsBody) ?: return null
        val type = detectTransactionType(smsBody)
        val merchant = extractMerchant(smsBody)
        val dateTime = extractDateTime(smsBody, timestamp)
        val hash = generateHash(smsBody, amount, dateTime)

        return Transaction(
            amount = amount,
            type = type,
            category = "Other", // Will be categorized later
            merchant = merchant,
            description = "$sender: ${smsBody.take(100)}",
            timestamp = dateTime,
            source = TransactionSource.SMS,
            smsHash = hash
        )
    }

    private fun extractAmount(smsBody: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(smsBody)
            if (matcher.find()) {
                try {
                    val amountStr = matcher.group(1)?.replace(",", "")
                    return amountStr?.toDouble()
                } catch (_: NumberFormatException) {
                    continue
                }
            }
        }
        return null
    }

    private fun detectTransactionType(smsBody: String): TransactionType {
        val lowercaseBody = smsBody.lowercase()

        val debitScore = DEBIT_PATTERNS.count { lowercaseBody.contains(it) }
        val creditScore = CREDIT_PATTERNS.count { lowercaseBody.contains(it) }

        return if (creditScore > debitScore) {
            TransactionType.CREDIT
        } else {
            TransactionType.DEBIT
        }
    }

    private fun extractMerchant(smsBody: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(smsBody)
            if (matcher.find()) {
                return matcher.group(1)?.trim()?.takeIf { it.length > 2 }
            }
        }
        return null
    }

    private fun extractDateTime(smsBody: String, fallbackTimestamp: Long): LocalDateTime {
        val smsText = smsBody.replace(Regex("\\s+"), " ")

        for (pattern in DATE_PATTERNS) {
            val matcher = pattern.matcher(smsText)
            if (matcher.find()) {
                try {
                    return when {
                        pattern.pattern().contains("Jan|Feb") && !pattern.pattern().startsWith(".*Jan") -> {
                            // Format: 12 Jan 2024
                            val day = matcher.group(1)
                            val month = matcher.group(2)
                            val year = matcher.group(3)?.let { if (it.length == 2) "20$it" else it }
                            LocalDateTime.parse("$year-${parseMonth(month)}-${day}T00:00:00")
                        }
                        pattern.pattern().startsWith(".*Jan") -> {
                            // Format: Jan 12, 2024
                            val month = matcher.group(1)
                            val day = matcher.group(2)
                            val year = matcher.group(3)
                            LocalDateTime.parse("$year-${parseMonth(month)}-${day}T00:00:00")
                        }
                        else -> {
                            // Format: DD/MM/YYYY
                            val day = matcher.group(1)?.padStart(2, '0')
                            val month = matcher.group(2)?.padStart(2, '0')
                            val year = matcher.group(3)?.let { if (it.length == 2) "20$it" else it }
                            LocalDateTime.parse("$year-$month-${day}T00:00:00")
                        }
                    }
                } catch (_: DateTimeParseException) {
                    continue
                }
            }
        }

        // Fallback to SMS timestamp
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(fallbackTimestamp),
            java.time.ZoneId.systemDefault()
        )
    }

    private fun parseMonth(monthStr: String?): String {
        return when (monthStr?.lowercase()?.take(3)) {
            "jan" -> "01"
            "feb" -> "02"
            "mar" -> "03"
            "apr" -> "04"
            "may" -> "05"
            "jun" -> "06"
            "jul" -> "07"
            "aug" -> "08"
            "sep" -> "09"
            "oct" -> "10"
            "nov" -> "11"
            "dec" -> "12"
            else -> "01"
        }
    }

    private fun generateHash(smsBody: String, amount: Double, timestamp: LocalDateTime): String {
        val input = "$smsBody|$amount|${timestamp.toLocalDate()}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
