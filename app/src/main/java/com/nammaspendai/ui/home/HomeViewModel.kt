package com.nammaspendai.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.nammaspendai.data.model.Transaction
import com.nammaspendai.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _currentMonthExpenses = MutableLiveData<Double>(0.0)
    val currentMonthExpenses: LiveData<Double> = _currentMonthExpenses

    private val _currentMonthIncome = MutableLiveData<Double>(0.0)
    val currentMonthIncome: LiveData<Double> = _currentMonthIncome

    private val _categoryTotals = MutableLiveData<Map<String, Double>>()
    val categoryTotals: LiveData<Map<String, Double>> = _categoryTotals

    val recentTransactions: LiveData<List<Transaction>> =
        transactionRepository.getRecent(10).asLiveData()

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadCurrentMonthSummary()
    }

    private fun loadCurrentMonthSummary() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val now = LocalDateTime.now()
                val startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                val endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                    .withHour(23).withMinute(59).withSecond(59)

                val expenses = transactionRepository.getTotalExpenses(startOfMonth, endOfMonth)
                val income = transactionRepository.getTotalIncome(startOfMonth, endOfMonth)

                _currentMonthExpenses.value = expenses
                _currentMonthIncome.value = income

                val totals = transactionRepository.getCategoryTotals(startOfMonth, endOfMonth)
                _categoryTotals.value = totals.associate { it.category to it.total }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshData() {
        loadCurrentMonthSummary()
    }

    fun getBalance(): Double {
        return (_currentMonthIncome.value ?: 0.0) - (_currentMonthExpenses.value ?: 0.0)
    }
}
