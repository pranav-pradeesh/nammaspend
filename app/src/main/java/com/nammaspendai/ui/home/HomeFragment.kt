package com.nammaspendai.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.nammaspendai.R
import com.nammaspendai.databinding.FragmentHomeBinding
import com.nammaspendai.ui.common.TransactionAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Currency

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter { transaction ->
            // Handle transaction click
        }
        binding.recyclerRecentTransactions.adapter = transactionAdapter
    }

    private fun setupObservers() {
        viewModel.currentMonthExpenses.observe(viewLifecycleOwner) { expenses ->
            val currencyFormat = NumberFormat.getCurrencyInstance()
            currencyFormat.currency = Currency.getInstance("INR")
            binding.tvTotalSpent.text = currencyFormat.format(expenses)
        }

        viewModel.currentMonthIncome.observe(viewLifecycleOwner) { income ->
            val currencyFormat = NumberFormat.getCurrencyInstance()
            currencyFormat.currency = Currency.getInstance("INR")
            binding.tvTotalIncome.text = currencyFormat.format(income)
        }

        viewModel.categoryTotals.observe(viewLifecycleOwner) { totals ->
            updatePieChart(totals)
        }

        viewModel.recentTransactions.observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.submitList(transactions)
            binding.tvEmptyTransactions.visibility =
                if (transactions.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
        }
    }

    private fun updatePieChart(categoryTotals: Map<String, Double>) {
        if (categoryTotals.isEmpty()) {
            binding.pieChart.visibility = View.GONE
            return
        }

        binding.pieChart.visibility = View.VISIBLE

        val entries = categoryTotals.map { (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }

        val dataSet = PieDataSet(entries, "Expenses").apply {
            colors = getChartColors()
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(binding.pieChart))
        }

        binding.pieChart.apply {
            this.data = data
            description.isEnabled = false
            isRotationEnabled = true
            setUsePercentValues(true)
            setEntryLabelTextSize(10f)
            setEntryLabelColor(Color.BLACK)
            legend.isEnabled = true
            animateY(1000)
            invalidate()
        }
    }

    private fun getChartColors(): List<Int> {
        return listOf(
            Color.rgb(64, 89, 128),
            Color.rgb(149, 165, 124),
            Color.rgb(217, 184, 162),
            Color.rgb(191, 134, 134),
            Color.rgb(179, 48, 80),
            Color.rgb(193, 37, 82),
            Color.rgb(255, 102, 0),
            Color.rgb(245, 199, 0),
            Color.rgb(106, 150, 31),
            Color.rgb(179, 100, 53)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
