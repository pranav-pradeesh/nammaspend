package com.nammaspendai.ui.addexpense

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nammaspendai.data.model.*
import com.nammaspendai.data.remote.GroqClient
import com.nammaspendai.data.repository.CategoryRepository
import com.nammaspendai.data.repository.TransactionRepository
import com.nammaspendai.data.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    application: Application,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val usageRepository: UsageRepository,
    private val groqClient: GroqClient
) : AndroidViewModel(application) {

    private val _voiceResult = MutableLiveData<VoiceResult?>()
    val voiceResult: LiveData<VoiceResult?> = _voiceResult

    private val _isProcessing = MutableLiveData<Boolean>(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories

    init {
        viewModelScope.launch {
            _categories.value = CategoryRule.DEFAULT_CATEGORIES
        }
    }

    fun processVoiceInput(text: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null

            try {
                // Check rate limiting
                val usageStatus = usageRepository.canMakeApiCall()
                if (usageStatus is ApiUsageStatus.Blocked) {
                    _error.value = usageStatus.reason
                    _isProcessing.value = false
                    return@launch
                }

                // Call Groq API
                val result = groqClient.parseVoiceInput(text)

                // Record usage
                usageRepository.recordApiCall()

                result.fold(
                    onSuccess = { expenses ->
                        if (expenses.isNotEmpty()) {
                            val defaultRules = categoryRepository.getDefaultRules()
                            val categorized = expenses.map { expense ->
                                val transaction = expense.toTransaction()
                                val category = categoryRepository.categorize(
                                    "${expense.merchant} ${expense.description} ${expense.category}",
                                    defaultRules
                                )
                                transaction.copy(category = category)
                            }
                            _voiceResult.value = VoiceResult.Success(categorized)
                        } else {
                            _error.value = "No expenses found in the input"
                        }
                    },
                    onFailure = { e ->
                        _error.value = e.message ?: "Failed to process voice input"
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun saveTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.insert(transaction)
        }
    }

    fun saveTransactions(transactions: List<Transaction>) {
        viewModelScope.launch {
            transactionRepository.insertAll(transactions)
        }
    }

    fun addManualTransaction(
        amount: Double,
        type: TransactionType,
        category: String,
        merchant: String?,
        description: String?,
        timestamp: LocalDateTime = LocalDateTime.now()
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                amount = amount,
                type = type,
                category = category,
                merchant = merchant?.takeIf { it.isNotBlank() },
                description = description?.takeIf { it.isNotBlank() },
                timestamp = timestamp,
                source = TransactionSource.MANUAL
            )
            transactionRepository.insert(transaction)
        }
    }

    fun clearVoiceResult() {
        _voiceResult.value = null
    }

    fun clearError() {
        _error.value = null
    }
}

sealed class VoiceResult {
    data class Success(val transactions: List<Transaction>) : VoiceResult()
    data class Error(val message: String) : VoiceResult()
}
