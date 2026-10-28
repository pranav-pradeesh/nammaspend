package com.nammaspendai.ui.addexpense

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.nammaspendai.data.model.Transaction
import com.nammaspendai.data.model.TransactionType
import com.nammaspendai.databinding.DialogManualEntryBinding
import com.nammaspendai.databinding.DialogVoiceResultBinding
import com.nammaspendai.databinding.FragmentAddExpenseBinding
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import java.util.Locale

@AndroidEntryPoint
class AddExpenseFragment : Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddExpenseViewModel by viewModels()
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
        setupObservers()
    }

    private fun setupButtons() {
        // Voice input button
        binding.btnVoiceInput.setOnClickListener {
            if (hasRecordPermission()) {
                startVoiceRecognition()
            } else {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            }
        }

        // Manual entry button
        binding.btnManualEntry.setOnClickListener {
            showManualEntryDialog()
        }
    }

    private fun setupObservers() {
        viewModel.voiceResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                when (it) {
                    is VoiceResult.Success -> showVoiceResultDialog(it.transactions)
                    is VoiceResult.Error -> showError(it.message)
                }
                viewModel.clearVoiceResult()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        viewModel.isProcessing.observe(viewLifecycleOwner) { isProcessing ->
            binding.progressBar.visibility = if (isProcessing) View.VISIBLE else View.GONE
            binding.btnVoiceInput.isEnabled = !isProcessing
        }

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            // Categories loaded
        }
    }

    private fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startVoiceRecognition() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your expense...")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                showError("Voice recognition error: $error")
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    viewModel.processVoiceInput(text)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private fun showVoiceResultDialog(transactions: List<Transaction>) {
        val dialogBinding = DialogVoiceResultBinding.inflate(layoutInflater)

        // Build summary text
        val summary = buildString {
            appendLine("Found ${transactions.size} expense(s):")
            transactions.forEach { t ->
                appendLine("• ${t.category}: ₹${t.amount}")
            }
        }
        dialogBinding.tvResultSummary.text = summary

        AlertDialog.Builder(requireContext())
            .setTitle("Review Expenses")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                viewModel.saveTransactions(transactions)
                Snackbar.make(binding.root, "Expenses saved!", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showManualEntryDialog() {
        val dialogBinding = DialogManualEntryBinding.inflate(layoutInflater)

        // Setup category dropdown
        viewModel.categories.value?.let { categories ->
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categories
            )
            dialogBinding.spinnerCategory.setAdapter(adapter)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Expense")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val amount = dialogBinding.etAmount.text.toString().toDoubleOrNull()
                val category = dialogBinding.spinnerCategory.text.toString()
                val merchant = dialogBinding.etMerchant.text.toString()
                val description = dialogBinding.etDescription.text.toString()

                if (amount == null || amount <= 0) {
                    showError("Please enter a valid amount")
                    return@setPositiveButton
                }

                val type = if (dialogBinding.radioIncome.isChecked) {
                    TransactionType.CREDIT
                } else {
                    TransactionType.DEBIT
                }

                viewModel.addManualTransaction(
                    amount = amount,
                    type = type,
                    category = category.ifEmpty { "Other" },
                    merchant = merchant,
                    description = description
                )

                Snackbar.make(binding.root, "Expense saved!", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _binding = null
    }
}
