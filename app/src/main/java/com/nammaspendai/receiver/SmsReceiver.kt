package com.nammaspendai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.nammaspendai.data.model.Transaction
import com.nammaspendai.data.repository.CategoryRepository
import com.nammaspendai.data.repository.TransactionRepository
import com.nammaspendai.util.SmsParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var categoryRepository: CategoryRepository

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        scope.launch {
            try {
                val defaultRules = categoryRepository.getDefaultRules()

                for (message in messages) {
                    val smsBody = message.messageBody ?: continue
                    val sender = message.displayOriginatingAddress
                    val timestamp = message.timestampMillis

                    // Parse transaction from SMS
                    val transaction = SmsParser.parse(smsBody, sender, timestamp)
                        ?: continue

                    // Check for duplicates
                    if (transaction.smsHash != null &&
                        transactionRepository.existsWithSmsHash(transaction.smsHash)
                    ) {
                        Log.d("SmsReceiver", "Duplicate transaction skipped")
                        continue
                    }

                    // Auto-categorize
                    val categorizedTransaction = transaction.copy(
                        category = categoryRepository.categorize(
                            "${transaction.merchant} ${transaction.description}",
                            defaultRules
                        )
                    )

                    // Save to database
                    val success = transactionRepository.insert(categorizedTransaction)
                    if (success) {
                        Log.d("SmsReceiver", "Transaction saved: ${categorizedTransaction.amount}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error processing SMS", e)
            }
        }
    }
}
