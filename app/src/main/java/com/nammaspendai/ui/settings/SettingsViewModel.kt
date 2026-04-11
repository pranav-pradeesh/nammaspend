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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val transactionRepository: TransactionRepository,
    private val securityManager: SecurityManager
) : AndroidViewModel(application) {

    private val exportManager = ExportManager(application)

    private val _exportResult = MutableLiveData<ExportResult?>()
    val exportResult: LiveData<ExportResult?> = _exportResult

    private val _isBiometricAvailable = MutableLiveData<Boolean>()
    val isBiometricAvailable: LiveData<Boolean> = _isBiometricAvailable

    private val _isBiometricEnabled = MutableLiveData<Boolean>()
    val isBiometricEnabled: LiveData<Boolean> = _isBiometricEnabled

    private val _isPinSet = MutableLiveData<Boolean>()
    val isPinSet: LiveData<Boolean> = _isPinSet

    val allTransactions = transactionRepository.allTransactions.asLiveData()

    init {
        _isBiometricAvailable.value = securityManager.isBiometricAvailable()
        _isBiometricEnabled.value = securityManager.isBiometricEnabled()
        _isPinSet.value = securityManager.isPinSet()
    }

    fun exportToCsv(transactions: List<Transaction>) {
        viewModelScope.launch {
            val result = exportManager.exportToCsv(transactions)
            result.fold(
                onSuccess = { uri ->
                    _exportResult.value = ExportResult.Success(uri, "text/csv")
                },
                onFailure = { e ->
                    _exportResult.value = ExportResult.Error(e.message ?: "Export failed")
                }
            )
        }
    }

    fun exportToExcel(transactions: List<Transaction>) {
        viewModelScope.launch {
            val result = exportManager.exportToExcel(transactions)
            result.fold(
                onSuccess = { uri ->
                    _exportResult.value = ExportResult.Success(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                },
                onFailure = { e ->
                    _exportResult.value = ExportResult.Error(e.message ?: "Export failed")
                }
            )
        }
    }

    fun shareFile(uri: Uri, mimeType: String) {
        exportManager.shareFile(uri, mimeType)
    }

    fun setPin(pin: String) {
        securityManager.setPin(pin)
        _isPinSet.value = true
    }

    fun verifyPin(pin: String): Boolean {
        return securityManager.verifyPin(pin)
    }

    fun clearPin() {
        securityManager.clearPin()
        _isPinSet.value = false
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securityManager.setBiometricEnabled(enabled)
        _isBiometricEnabled.value = enabled
    }

    fun importFromSms() {
        // This would trigger SMS import - to be implemented with permission handling
    }

    fun clearExportResult() {
        _exportResult.value = null
    }
}

sealed class ExportResult {
    data class Success(val uri: Uri, val mimeType: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}
