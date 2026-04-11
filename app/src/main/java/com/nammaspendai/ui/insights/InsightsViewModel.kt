package com.nammaspendai.ui.insights

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nammaspendai.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _monthlyData = MutableLiveData<List<MonthlyChartData>>()
    val monthlyData: LiveData<List<MonthlyChartData>> = _monthlyData

    private val _categoryData = MutableLiveData<List<CategoryChartData>>()
    val categoryData: LiveData<List<CategoryChartData>> = _categoryData

    private val _topCategories = MutableLiveData<List<CategorySpending>>()
    val topCategories: LiveData<List<CategorySpending>> = _topCategories

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadInsights()
    }

    fun loadInsights() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load monthly trends
                val monthlyTotals = transactionRepository.getMonthlyTotals()
                _monthlyData.value = monthlyTotals.map {
                    MonthlyChartData(
                        month = it.month,
                        expenses = it.expenses,
                        income = it.income
                    )
                }

                // Load current month category breakdown
                val now = LocalDateTime.now()
                val startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                val endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                    .withHour(23).withMinute(59).withSecond(59)

                val categoryTotals = transactionRepository.getCategoryTotals(startOfMonth, endOfMonth)
                _categoryData.value = categoryTotals.map {
                    CategoryChartData(
                        category = it.category,
                        amount = it.total,
                        percentage = 0f // Calculated below
                    )
                }

                // Calculate percentages and top categories
                val totalSpending = categoryTotals.sumOf { it.total }
                if (totalSpending > 0) {
                    _categoryData.value = categoryTotals.map {
                        CategoryChartData(
                            category = it.category,
                            amount = it.total,
                            percentage = (it.total / totalSpending * 100).toFloat()
                        )
                    }

                    _topCategories.value = categoryTotals
                        .sortedByDescending { it.total }
                        .take(5)
                        .map {
                            CategorySpending(
                                category = it.category,
                                amount = it.total,
                                percentage = (it.total / totalSpending * 100)
                            )
                        }
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Prediction methods
    fun predictElectricityBill(): Double? {
        // Simple average of last 3-6 months for utilities category
        return calculateCategoryAverage("Utilities", 6)
    }

    fun predictLpgBill(): Double? {
        // Simple average for LPG-related expenses
        return calculateCategoryAverage("Utilities", 6)
    }

    private fun calculateCategoryAverage(category: String, months: Int): Double? {
        // This would require historical data query - simplified for now
        return null
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
