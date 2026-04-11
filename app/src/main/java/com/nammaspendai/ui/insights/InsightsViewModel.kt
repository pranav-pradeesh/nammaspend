package com.nammaspendai.ui.insights

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nammaspendai.data.repository.TransactionRepository
import com.nammaspendai.util.PredictionEngine
import com.nammaspendai.util.PredictionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val predictionEngine: PredictionEngine
) : ViewModel() {

    private val _monthlyData = MutableLiveData<List<MonthlyChartData>>()
    val monthlyData: LiveData<List<MonthlyChartData>> = _monthlyData

    private val _categoryData = MutableLiveData<List<CategoryChartData>>()
    val categoryData: LiveData<List<CategoryChartData>> = _categoryData

    private val _topCategories = MutableLiveData<List<CategorySpending>>()
    val topCategories: LiveData<List<CategorySpending>> = _topCategories

    private val _predictions = MutableLiveData<List<PredictionResult>>()
    val predictions: LiveData<List<PredictionResult>> = _predictions

    private val _electricityPrediction = MutableLiveData<PredictionResult?>()
    val electricityPrediction: LiveData<PredictionResult?> = _electricityPrediction

    private val _lpgPrediction = MutableLiveData<PredictionResult?>()
    val lpgPrediction: LiveData<PredictionResult?> = _lpgPrediction

    private val _spendingTrend = MutableLiveData<Double>(0.0)
    val spendingTrend: LiveData<Double> = _spendingTrend

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadInsights()
    }

    fun loadInsights() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val now = LocalDateTime.now()
                val startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                val endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                    .withHour(23).withMinute(59).withSecond(59)

                // Monthly trends
                val monthlyTotals = transactionRepository.getMonthlyTotals()
                _monthlyData.value = monthlyTotals.map {
                    MonthlyChartData(month = it.month, expenses = it.expenses, income = it.income)
                }

                // Category totals
                val categoryTotals = transactionRepository.getCategoryTotals(startOfMonth, endOfMonth)
                val totalSpending = categoryTotals.sumOf { it.total }

                _categoryData.value = categoryTotals.map {
                    CategoryChartData(
                        category = it.category,
                        amount = it.total,
                        percentage = if (totalSpending > 0) (it.total / totalSpending * 100).toFloat() else 0f
                    )
                }

                _topCategories.value = categoryTotals
                    .sortedByDescending { it.total }
                    .take(5)
                    .map {
                        CategorySpending(
                            category = it.category,
                            amount = it.total,
                            percentage = if (totalSpending > 0) (it.total / totalSpending * 100) else 0.0
                        )
                    }

                // Predictions
                val allTransactions = transactionRepository.allTransactions.first()
                _predictions.value = predictionEngine.predictNextMonth(allTransactions)
                _electricityPrediction.value = predictionEngine.predictElectricityBill(allTransactions)
                _lpgPrediction.value = predictionEngine.predictLpgBill(allTransactions)
                _spendingTrend.value = predictionEngine.getSpendingTrend(allTransactions)

            } catch (e: Exception) {
                _error.value = "Failed to load insights: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

data class MonthlyChartData(
    val month: String,
    val expenses: Double,
    val income: Double
)

data class CategoryChartData(
    val category: String,
    val amount: Double,
    val percentage: Float
)

data class CategorySpending(
    val category: String,
    val amount: Double,
    val percentage: Double
)
