package com.nammaspendai.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nammaspendai.data.model.Transaction
import com.nammaspendai.data.model.TransactionType
import com.nammaspendai.databinding.ItemTransactionBinding
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Currency

class TransactionAdapter(
    private val onItemClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(TransactionDiffCallback()) {

    private val currencyFormat = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance("INR")
    }
    private val dateFormat = DateTimeFormatter.ofPattern("dd MMM, hh:mm a")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.apply {
                tvCategory.text = transaction.category
                tvMerchant.text = transaction.merchant ?: "Unknown"
                tvDate.text = dateFormat.format(transaction.timestamp)
                tvAmount.text = currencyFormat.format(transaction.amount)

                // Set color based on transaction type
                val amountColor = when (transaction.type) {
                    TransactionType.DEBIT ->
                        androidx.core.content.ContextCompat.getColor(root.context, android.R.color.holo_red_dark)
                    TransactionType.CREDIT ->
                        androidx.core.content.ContextCompat.getColor(root.context, android.R.color.holo_green_dark)
                }
                tvAmount.setTextColor(amountColor)

                // Show +/- prefix
                val prefix = when (transaction.type) {
                    TransactionType.DEBIT -> "-"
                    TransactionType.CREDIT -> "+"
                }
                tvAmount.text = "$prefix${currencyFormat.format(transaction.amount)}"

                // Source indicator
                tvSource.text = when (transaction.source) {
                    com.nammaspendai.data.model.TransactionSource.SMS -> "S"
                    com.nammaspendai.data.model.TransactionSource.VOICE -> "V"
                    com.nammaspendai.data.model.TransactionSource.MANUAL -> "M"
                }

                root.setOnClickListener { onItemClick(transaction) }
            }
        }
    }
}

class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
    override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem == newItem
    }
}
