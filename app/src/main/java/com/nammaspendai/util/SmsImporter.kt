package com.nammaspendai.util

import android.content.Context
import android.provider.Telephony
import com.nammaspendai.data.model.Transaction
import com.nammaspendai.data.repository.CategoryRepository
import com.nammaspendai.data.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class SmsImportResult(
    val total: Int,
    val imported: Int,
    val duplicates: Int,
    val failed: Int
)

@Singleton
class SmsImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend fun importFromInbox(maxMessages: Int = 500): SmsImportResult {
        var total = 0
        var imported = 0
        var duplicates = 0
        var failed = 0

        try {
            val defaultRules = categoryRepository.getDefaultRules()
            val uri = Telephony.Sms.CONTENT_URI
            val projection = arrayOf(
                Telephony.Sms.BODY,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.DATE
            )

            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT $maxMessages"
            ) ?: return SmsImportResult(0, 0, 0, 0)

            cursor.use {
                val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    total++
                    try {
                        val body = it.getString(bodyIndex) ?: continue
                        val address = it.getString(addressIndex)
                        val date = it.getLong(dateIndex)

                        val transaction = SmsParser.parse(body, address, date) ?: continue

                        // Check duplicate
                        if (transaction.smsHash != null &&
                            transactionRepository.existsWithSmsHash(transaction.smsHash)) {
                            duplicates++
                            continue
                        }

                        // Categorize
                        val categorized = transaction.copy(
                            category = categoryRepository.categorize(
                                "${transaction.merchant} ${transaction.description}",
                                defaultRules
                            )
                        )

                        val success = transactionRepository.insert(categorized)
                        if (success) imported++ else failed++

                    } catch (e: Exception) {
                        failed++
                    }
                }
            }
        } catch (e: Exception) {
            failed++
        }

        return SmsImportResult(total, imported, duplicates, failed)
    }
}
