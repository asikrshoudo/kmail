package com.kmail.thekami.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmail.thekami.data.security.AppLockStore
import com.kmail.thekami.data.security.PinVerify
import com.kmail.thekami.data.transfer.ExportResult
import com.kmail.thekami.data.transfer.TransferExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data object Working : ExportUiState
    class Ready(val data: ByteArray) : ExportUiState
    data object Failed : ExportUiState
}

@HiltViewModel
class TransferExportViewModel @Inject constructor(
    private val exporter: TransferExporter,
    private val lockStore: AppLockStore,
) : ViewModel() {
    private val _state = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    val lockEnabled: Boolean get() = lockStore.isEnabled

    suspend fun verifyPin(pin: String): PinVerify = lockStore.verify(pin)

    fun prepare(accountIds: List<String>, passphrase: String) {
        if (_state.value is ExportUiState.Working) return
        _state.value = ExportUiState.Working
        viewModelScope.launch {
            val chars = passphrase.toCharArray()
            try {
                _state.value = when (val result = exporter.export(accountIds, chars)) {
                    is ExportResult.Ready -> ExportUiState.Ready(result.data)
                    else -> ExportUiState.Failed
                }
            } finally {
                chars.fill('\u0000')
            }
        }
    }

    fun reset() {
        _state.value = ExportUiState.Idle
    }

    override fun onCleared() {
        _state.value = ExportUiState.Idle
    }
}
