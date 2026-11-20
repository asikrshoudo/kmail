package com.kmail.thekami.ui.setup

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmail.thekami.data.transfer.TransferCodec
import com.kmail.thekami.data.transfer.TransferDecodeResult
import com.kmail.thekami.domain.mail.ConnectionTester
import com.kmail.thekami.domain.mail.MailResult
import com.kmail.thekami.domain.model.AccountCreator
import com.kmail.thekami.domain.model.AddAccountResult
import com.kmail.thekami.domain.transfer.TransferAccount
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

enum class ImportOutcome { ADDED, ALREADY_EXISTS, INCOMING_FAILED, OUTGOING_FAILED, FAILED }

class ImportResult(val email: String, val outcome: ImportOutcome)

sealed interface ImportUiState {
    data object Idle : ImportUiState
    class AskPassphrase(val error: ImportError?) : ImportUiState
    data object Decrypting : ImportUiState
    class Importing(val index: Int, val total: Int, val email: String) : ImportUiState
    class Finished(val results: List<ImportResult>) : ImportUiState
    data object Rejected : ImportUiState
}

/**
 * Holds the encrypted bytes of a scanned or picked transfer until the passphrase is entered,
 * then tests and adds every account in it, one after the other.
 */
@HiltViewModel
class TransferImportViewModel @Inject constructor(
    private val tester: ConnectionTester,
    private val accounts: AccountCreator,
) : ViewModel() {
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
        if (_state.value !is ImportUiState.AskPassphrase) return
        _state.value = ImportUiState.Decrypting
        viewModelScope.launch {
            val chars = passphrase.toCharArray()
            val result = try {
                withContext(Dispatchers.Default) { codec.decode(data, chars) }
            } finally {
                chars.fill('\u0000')
            }
            when (result) {
                is TransferDecodeResult.Ok -> {
                    discardPending()
                    runImport(result.data)
                }
                TransferDecodeResult.Failed -> _state.value = ImportUiState.AskPassphrase(ImportError.WRONG_OR_DAMAGED)
                TransferDecodeResult.Unsupported -> _state.value = ImportUiState.AskPassphrase(ImportError.UNSUPPORTED)
            }
        }
    }

    /** Call when the user gives up, or after the results have been shown. */
    fun clear() {
        discardPending()
        _state.value = ImportUiState.Idle
    }

    override fun onCleared() {
        discardPending()
    }

    private fun discardPending() {
        pending?.fill(0)
        pending = null
    }

    private suspend fun runImport(pkg: TransferPackage) {
        val results = ArrayList<ImportResult>()
        try {
            pkg.accounts.forEachIndexed { index, account ->
                _state.value = ImportUiState.Importing(index + 1, pkg.accounts.size, account.emailAddress)
                results += ImportResult(account.emailAddress, importOne(account))
            }
        } finally {
            pkg.wipe()
        }
        _state.value = ImportUiState.Finished(results)
    }

    private suspend fun importOne(account: TransferAccount): ImportOutcome {
        val draft = account.toNewAccount()
        try {
            if (tester.testImap(account.settings.imap, draft.imapPassword) is MailResult.Failure) {
                return ImportOutcome.INCOMING_FAILED
            }
            if (tester.testSmtp(account.settings.smtp, draft.smtpPassword) is MailResult.Failure) {
                return ImportOutcome.OUTGOING_FAILED
            }
            return when (accounts.addAccount(draft)) {
                is AddAccountResult.Added -> ImportOutcome.ADDED
                AddAccountResult.AlreadyExists -> ImportOutcome.ALREADY_EXISTS
                AddAccountResult.InvalidEmail, AddAccountResult.CredentialStorageFailed -> ImportOutcome.FAILED
            }
        } finally {
            draft.imapPassword.fill('\u0000')
            draft.smtpPassword.fill('\u0000')
        }
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
