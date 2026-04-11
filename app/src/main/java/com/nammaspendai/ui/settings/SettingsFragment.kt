package com.nammaspendai.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.nammaspendai.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            binding.btnExportCsv.isEnabled = transactions.isNotEmpty()
            binding.btnExportExcel.isEnabled = transactions.isNotEmpty()
        }

        viewModel.isBiometricAvailable.observe(viewLifecycleOwner) { available ->
            binding.switchBiometric.isEnabled = available
        }

        viewModel.isBiometricEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchBiometric.isChecked = enabled
        }

        viewModel.isPinSet.observe(viewLifecycleOwner) { isSet ->
            binding.btnSetPin.text = if (isSet) "Change PIN" else "Set PIN"
        }

        viewModel.exportResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                when (it) {
                    is ExportResult.Success -> viewModel.shareFile(it.uri, it.mimeType)
                    is ExportResult.Error -> Snackbar.make(binding.root, it.message, Snackbar.LENGTH_LONG).show()
                }
                viewModel.clearExportResult()
            }
        }

        viewModel.smsImportResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                val msg = "Imported ${it.imported} transactions (${it.duplicates} duplicates skipped)"
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                viewModel.clearImportResult()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnExportCsv.setOnClickListener {
            viewModel.allTransactions.value?.let { viewModel.exportToCsv(it) }
        }

        binding.btnExportExcel.setOnClickListener {
            viewModel.allTransactions.value?.let { viewModel.exportToExcel(it) }
        }

        binding.btnImportSms.setOnClickListener {
            if (hasSmsPermission()) {
                showImportConfirmationDialog()
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS),
                    SMS_PERMISSION_CODE
                )
            }
        }

        binding.btnSetPin.setOnClickListener { showPinDialog() }

        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBiometricEnabled(isChecked)
        }
    }

    private fun showImportConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Import SMS Transactions")
            .setMessage("This will scan your SMS inbox for bank messages and import transactions automatically. Continue?")
            .setPositiveButton("Import") { _, _ ->
                viewModel.importFromSms()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun showPinDialog() {
        val editText = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Enter 4-6 digit PIN"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Set PIN")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val pin = editText.text.toString()
                if (pin.length in 4..6) {
                    viewModel.setPin(pin)
                    Toast.makeText(requireContext(), "PIN set successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "PIN must be 4-6 digits", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            showImportConfirmationDialog()
        } else {
            Snackbar.make(binding.root, "SMS permission required for import", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val SMS_PERMISSION_CODE = 100
    }
}
