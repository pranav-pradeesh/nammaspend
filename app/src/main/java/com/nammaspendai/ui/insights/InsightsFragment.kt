package com.nammaspendai.ui.insights

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.nammaspendai.R
import com.nammaspendai.databinding.FragmentInsightsBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Currency

@AndroidEntryPoint
class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InsightsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupSwipeRefresh()
    }

    private fun setupObservers() {
        viewModel.monthlyData.observe(viewLifecycleOwner) { data ->
            updateMonthlyChart(data)
        }

        viewModel.categoryData.observe(viewLifecycleOwner) { data ->
            updateCategoryChart(data)
        }

        viewModel.topCategories.observe(viewLifecycleOwner) { categories ->
            updateTopCategories(categories)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadInsights()
        }
    }

    private fun updateMonthlyChart(data: List<MonthlyChartData>) {
        if (data.isEmpty()) {
            binding.barChartMonthly.visibility = View.GONE
            return
        }

        binding.barChartMonthly.visibility = View.VISIBLE

        val entriesExpense = ArrayList<BarEntry>()
        val entriesIncome = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        data.forEachIndexed { index, item ->
            entriesExpense.add(BarEntry(index.toFloat(), item.expenses.toFloat()))
            entriesIncome.add(BarEntry(index.toFloat(), item.income.toFloat()))
            labels.add(item.month)
        }

        val expenseDataSet = BarDataSet(entriesExpense, "Expenses").apply {
            color = Color.rgb(244, 67, 54)
            valueTextSize = 10f
        }

        val incomeDataSet = BarDataSet(entriesIncome, "Income").apply {
            color = Color.rgb(76, 175, 80)
            valueTextSize = 10f
        }

        val barData = BarData(expenseDataSet, incomeDataSet).apply {
            barWidth = 0.35f
        }

        binding.barChartMonthly.apply {
            this.data = barData
            description.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                labelRotationAngle = -45f
            }
            axisRight.isEnabled = false
            groupBars(0f, 0.2f, 0.05f)
            animateY(1000)
            invalidate()
        }
    }

    private fun updateCategoryChart(data: List<CategoryChartData>) {
        if (data.isEmpty()) {
            binding.horizontalBarChart.visibility = View.GONE
            return
        }

        binding.horizontalBarChart.visibility = View.VISIBLE

        val entries = data.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.amount.toFloat())
        }

        val labels = data.map { it.category }

        val dataSet = BarDataSet(entries, "Spending by Category").apply {
            colors = getChartColors(data.size)
            valueTextSize = 10f
        }

        binding.horizontalBarChart.apply {
            this.data = BarData(dataSet)
            description.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
            }
            axisRight.isEnabled = false
            animateX(1000)
            invalidate()
        }
    }

    private fun updateTopCategories(categories: List<CategorySpending>) {
        if (categories.isEmpty()) {
            binding.layoutTopCategories.visibility = View.GONE
            return
        }

        binding.layoutTopCategories.visibility = View.VISIBLE
        binding.layoutTopCategories.removeAllViews()

        val currencyFormat = NumberFormat.getCurrencyInstance()
        currencyFormat.currency = Currency.getInstance("INR")

        categories.forEach { item ->
            val view = layoutInflater.inflate(R.layout.item_top_category, binding.layoutTopCategories, false)
            // Note: In actual implementation, bind the views here
            binding.layoutTopCategories.addView(view)
        }
    }

    private fun getChartColors(count: Int): List<Int> {
        val baseColors = listOf(
            Color.rgb(255, 102, 0),
            Color.rgb(245, 199, 0),
            Color.rgb(106, 150, 31),
            Color.rgb(179, 100, 53),
            Color.rgb(64, 89, 128),
            Color.rgb(149, 165, 124),
            Color.rgb(217, 184, 162),
            Color.rgb(191, 134, 134)
        )
        return List(count) { index -> baseColors[index % baseColors.size] }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
