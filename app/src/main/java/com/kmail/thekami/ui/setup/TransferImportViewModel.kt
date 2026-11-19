package com.kmail.thekami.ui.setup

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmail.thekami.data.transfer.TransferCodec
import com.kmail.thekami.data.transfer.TransferDecodeResult
import com.kmail.thekami.domain.transfer.TransferPackage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ImportError { WRONG_OR_DAMAGED, UNSUPPORTED }

sealed interface ImportUiState {
    data object Idle : ImportUiState
    class AskPassphrase(val error: ImportError?) : ImportUiState
    data object Working : ImportUiState
    class Unlocked(val data: TransferPackage) : ImportUiState
    data object Rejected : ImportUiState
}

/** Holds the encrypted bytes of a scanned or picked transfer until the passphrase is entered. */
@HiltViewModel
class TransferImportViewModel @Inject constructor() : ViewModel() {
    private val codec = TransferCodec()
    private var pending: ByteArray? = null

    private val _state = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    /** @return false if [bytes] cannot be a transfer. */
    fun offer(bytes: ByteArray): Boolean {
        if (bytes.size !in MIN_BYTES..MAX_BYTES) return false
        pending = bytes
        _state.value = ImportUiState.AskPassphrase(null)
        return true
    }

    fun loadFile(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) { readLimited(resolver, uri) }
            if (bytes == null || !offer(bytes)) _state.value = ImportUiState.Rejected
        }
    }

    fun unlock(passphrase: String) {
        val data = pending ?: return
        if (_state.value is ImportUiState.Working) return
        _state.value = ImportUiState.Working
        viewModelScope.launch {
            val chars = passphrase.toCharArray()
            try {
                val result = withContext(Dispatchers.Default) { codec.decode(data, chars) }
                _state.value = when (result) {
                    is TransferDecodeResult.Ok -> ImportUiState.Unlocked(result.data)
                    TransferDecodeResult.Failed -> ImportUiState.AskPassphrase(ImportError.WRONG_OR_DAMAGED)
                    TransferDecodeResult.Unsupported -> ImportUiState.AskPassphrase(ImportError.UNSUPPORTED)
                }
            } finally {
                chars.fill('\u0000')
            }
        }
    }

    /** Call after an unlocked transfer has been handed over, or when the user gives up. */
    fun clear() {
        pending?.fill(0)
        pending = null
        _state.value = ImportUiState.Idle
    }

    override fun onCleared() {
        clear()
    }

    private fun readLimited(resolver: ContentResolver, uri: Uri): ByteArray? = try {
        resolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (out.size() + read > MAX_BYTES) return@use null
                out.write(buffer, 0, read)
            }
            out.toByteArray()
        }
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        null
    }

    private companion object {
        const val MIN_BYTES = 45
        const val MAX_BYTES = 32 * 1024
    }
}
