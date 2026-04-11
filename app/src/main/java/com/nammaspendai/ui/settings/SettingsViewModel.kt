package com.nammaspendai.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.nammaspendai.data.model.Transaction
import com.nammaspendai.data.repository.TransactionRepository
import com.nammaspendai.util.ExportManager
import com.nammaspendai.util.SecurityManager
import com.nammaspendai.util.SmsImportResult
import com.nammaspendai.util.SmsImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val transactionRepository: TransactionRepository,
    private val securityManager: SecurityManager,
    private val smsImporter: SmsImporter
) : AndroidViewModel(application) {

    private val exportManager = ExportManager(application)

    val allTransactions = transactionRepository.allTransactions.asLiveData()

    private val _exportResult = MutableLiveData<ExportResult?>()
    val exportResult: LiveData<ExportResult?> = _exportResult

    private val _smsImportResult = MutableLiveData<SmsImportResult?>()
    val smsImportResult: LiveData<SmsImportResult?> = _smsImportResult

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isBiometricAvailable = MutableLiveData(securityManager.isBiometricAvailable())
    val isBiometricAvailable: LiveData<Boolean> = _isBiometricAvailable

    private val _isBiometricEnabled = MutableLiveData(securityManager.isBiometricEnabled())
    val isBiometricEnabled: LiveData<Boolean> = _isBiometricEnabled

    private val _isPinSet = MutableLiveData(securityManager.isPinSet())
    val isPinSet: LiveData<Boolean> = _isPinSet

    fun exportToCsv(transactions: List<Transaction>) {
        viewModelScope.launch {
            val result = exportManager.exportToCsv(transactions)
            result.fold(
                onSuccess = { uri -> _exportResult.value = ExportResult.Success(uri, "text/csv") },
                onFailure = { e -> _exportResult.value = ExportResult.Error(e.message ?: "Export failed") }
            )
        }
    }

    fun exportToExcel(transactions: List<Transaction>) {
        viewModelScope.launch {
            val result = exportManager.exportToExcel(transactions)
            result.fold(
                onSuccess = { uri -> _exportResult.value = ExportResult.Success(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
                onFailure = { e -> _exportResult.value = ExportResult.Error(e.message ?: "Export failed") }
            )
        }
    }

    fun shareFile(uri: Uri, mimeType: String) {
        exportManager.shareFile(uri, mimeType)
    }

    fun importFromSms() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = smsImporter.importFromInbox()
                _smsImportResult.value = result
            } catch (e: Exception) {
                _smsImportResult.value = SmsImportResult(0, 0, 0, 1)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setPin(pin: String) {
        securityManager.setPin(pin)
        _isPinSet.value = true
    }

    fun verifyPin(pin: String): Boolean = securityManager.verifyPin(pin)

    fun clearPin() {
        securityManager.clearPin()
        _isPinSet.value = false
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securityManager.setBiometricEnabled(enabled)
        _isBiometricEnabled.value = enabled
    }

    fun clearExportResult() { _exportResult.value = null }
    fun clearImportResult() { _smsImportResult.value = null }
}

sealed class ExportResult {
    data class Success(val uri: Uri, val mimeType: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}
