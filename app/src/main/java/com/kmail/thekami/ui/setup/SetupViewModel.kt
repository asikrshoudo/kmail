package com.kmail.thekami.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmail.thekami.domain.mail.ConnectionTester
import com.kmail.thekami.domain.mail.MailError
import com.kmail.thekami.domain.mail.MailResult
import com.kmail.thekami.domain.model.AccountCreator
import com.kmail.thekami.domain.model.AccountSettings
import com.kmail.thekami.domain.model.AddAccountResult
import com.kmail.thekami.domain.model.ConnectionSecurity
import com.kmail.thekami.domain.model.NewAccount
import com.kmail.thekami.domain.model.ProviderPresets
import com.kmail.thekami.domain.model.ServerForm
import com.kmail.thekami.domain.model.ServerFormError
import com.kmail.thekami.domain.model.ServerFormResult
import com.kmail.thekami.domain.model.SuggestionNote
import com.kmail.thekami.domain.model.normalizeEmail
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CredentialsUiState(
    val email: String = "",
    val password: String = "",
    val emailError: Boolean = false,
    val passwordError: Boolean = false,
)

data class ServerUiState(
    val form: ServerForm = ServerForm(),
    val password: String = "",
    val errors: Set<ServerFormError> = emptySet(),
    val passwordError: Boolean = false,
)

enum class ServerSide { INCOMING, OUTGOING }

enum class ConnectStage { INCOMING, OUTGOING, SAVING }

sealed interface ConnectFailure {
    data class Incoming(val error: MailError) : ConnectFailure
    data class Outgoing(val error: MailError) : ConnectFailure
    data object InvalidInput : ConnectFailure
    data object AlreadyExists : ConnectFailure
    data object StorageFailed : ConnectFailure
}

sealed interface ConnectState {
    data object Idle : ConnectState
    data class Testing(val stage: ConnectStage) : ConnectState
    data class Failed(val failure: ConnectFailure) : ConnectState
    data class Connected(val emailAddress: String) : ConnectState
}

private val ACCOUNT_COLORS = intArrayOf(
    0xFF6750A4.toInt(), 0xFF00796B.toInt(), 0xFFC2185B.toInt(),
    0xFFEF6C00.toInt(), 0xFF1565C0.toInt(), 0xFF558B2F.toInt(),
)

private fun defaultPort(side: ServerSide, security: ConnectionSecurity): Int = when (side) {
    ServerSide.INCOMING -> if (security == ConnectionSecurity.SSL_TLS) 993 else 143
    ServerSide.OUTGOING -> if (security == ConnectionSecurity.SSL_TLS) 465 else 587
}

/**
 * State for the whole account setup flow. Passwords live here only while the flow runs;
 * they are never saved to saved-instance state and are cleared in [onCleared].
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val tester: ConnectionTester,
    private val accounts: AccountCreator,
) : ViewModel() {

    private val _credentials = MutableStateFlow(CredentialsUiState())
    val credentials: StateFlow<CredentialsUiState> = _credentials.asStateFlow()

    private val _incoming = MutableStateFlow(ServerUiState())
    val incoming: StateFlow<ServerUiState> = _incoming.asStateFlow()

    private val _outgoing = MutableStateFlow(ServerUiState())
    val outgoing: StateFlow<ServerUiState> = _outgoing.asStateFlow()

    private val _note = MutableStateFlow<SuggestionNote?>(null)
    val note: StateFlow<SuggestionNote?> = _note.asStateFlow()

    private val _connectState = MutableStateFlow<ConnectState>(ConnectState.Idle)
    val connectState: StateFlow<ConnectState> = _connectState.asStateFlow()

    private var connectJob: Job? = null

    // ---- credentials ----

    fun onEmailChange(value: String) {
        _credentials.update { it.copy(email = value, emailError = false) }
    }

    fun onPasswordChange(value: String) {
        _credentials.update { it.copy(password = value, passwordError = false) }
    }

    /** Flags invalid fields and, when valid, pre-fills both server forms. */
    fun submitCredentials(): Boolean {
        val current = _credentials.value
        val email = normalizeEmail(current.email)
        val passwordOk = current.password.isNotEmpty()
        _credentials.update { it.copy(emailError = email == null, passwordError = !passwordOk) }
        if (email == null || !passwordOk) return false
        prefill(email, current.password)
        return true
    }

    private fun prefill(email: String, password: String) {
        val suggestion = ProviderPresets.suggest(email)
        _note.value = suggestion?.note
        _incoming.value = ServerUiState(
            form = suggestion?.imap?.toForm(email) ?: ServerForm(username = email),
            password = password,
        )
        _outgoing.value = ServerUiState(
            form = suggestion?.smtp?.toForm(email) ?: ServerForm(username = email),
            password = password,
        )
    }

    // ---- server forms ----

    private fun flow(side: ServerSide) = if (side == ServerSide.INCOMING) _incoming else _outgoing

    private fun edit(side: ServerSide, transform: (ServerUiState) -> ServerUiState) {
        flow(side).update(transform)
    }

    fun onServerHost(side: ServerSide, value: String) = edit(side) {
        it.copy(form = it.form.copy(host = value), errors = it.errors - ServerFormError.HOST_INVALID)
    }

    fun onServerPort(side: ServerSide, value: String) = edit(side) {
        it.copy(form = it.form.copy(port = value), errors = it.errors - ServerFormError.PORT_INVALID)
    }

    fun onServerUsername(side: ServerSide, value: String) = edit(side) {
        it.copy(form = it.form.copy(username = value), errors = it.errors - ServerFormError.USERNAME_EMPTY)
    }

    fun onServerPassword(side: ServerSide, value: String) = edit(side) {
        it.copy(password = value, passwordError = false)
    }

    /** Switching security also switches the port, but only if the port was still the default. */
    fun onServerSecurity(side: ServerSide, security: ConnectionSecurity) = edit(side) {
        val oldDefault = defaultPort(side, it.form.security).toString()
        val port = if (it.form.port.trim() == oldDefault) {
            defaultPort(side, security).toString()
        } else {
            it.form.port
        }
        it.copy(
            form = it.form.copy(security = security, port = port),
            errors = it.errors - ServerFormError.PORT_INVALID,
        )
    }

    fun submitServer(side: ServerSide): Boolean {
        val state = flow(side).value
        val errors = (state.form.validate() as? ServerFormResult.Invalid)?.errors ?: emptySet()
        val passwordOk = state.password.isNotEmpty()
        edit(side) { it.copy(errors = errors, passwordError = !passwordOk) }
        return errors.isEmpty() && passwordOk
    }

    // ---- connect ----

    /** Ignored while a test is running or after success. Allowed again after a failure. */
    fun connect() {
        val state = _connectState.value
        if (state is ConnectState.Testing || state is ConnectState.Connected) return
        connectJob = viewModelScope.launch { runConnect() }
    }

    fun cancelConnect() {
        connectJob?.cancel()
        if (_connectState.value is ConnectState.Testing) _connectState.value = ConnectState.Idle
    }

    fun startNewAccount() {
        cancelConnect()
        clearAll()
    }

    private fun fail(failure: ConnectFailure) {
        _connectState.value = ConnectState.Failed(failure)
    }

    private suspend fun runConnect() {
        val imap = (_incoming.value.form.validate() as? ServerFormResult.Valid)?.endpoint
        val smtp = (_outgoing.value.form.validate() as? ServerFormResult.Valid)?.endpoint
        val email = normalizeEmail(_credentials.value.email)
        if (imap == null || smtp == null || email == null) {
            fail(ConnectFailure.InvalidInput)
            return
        }
        val imapPassword = _incoming.value.password.toCharArray()
        val smtpPassword = _outgoing.value.password.toCharArray()
        try {
            _connectState.value = ConnectState.Testing(ConnectStage.INCOMING)
            val imapResult = tester.testImap(imap, imapPassword)
            if (imapResult is MailResult.Failure) {
                fail(ConnectFailure.Incoming(imapResult.error))
                return
            }

            _connectState.value = ConnectState.Testing(ConnectStage.OUTGOING)
            val smtpResult = tester.testSmtp(smtp, smtpPassword)
            if (smtpResult is MailResult.Failure) {
                fail(ConnectFailure.Outgoing(smtpResult.error))
                return
            }

            _connectState.value = ConnectState.Testing(ConnectStage.SAVING)
            val draft = NewAccount(
                emailAddress = email,
                displayName = "",
                colorArgb = ACCOUNT_COLORS[(email.hashCode() and Int.MAX_VALUE) % ACCOUNT_COLORS.size],
                settings = AccountSettings(imap, smtp),
                imapPassword = imapPassword,
                smtpPassword = smtpPassword,
            )
            when (val result = accounts.addAccount(draft)) {
                is AddAccountResult.Added -> {
                    clearForms()
                    _connectState.value = ConnectState.Connected(result.account.emailAddress)
                }
                AddAccountResult.AlreadyExists -> fail(ConnectFailure.AlreadyExists)
                AddAccountResult.InvalidEmail -> fail(ConnectFailure.InvalidInput)
                AddAccountResult.CredentialStorageFailed -> fail(ConnectFailure.StorageFailed)
            }
        } finally {
            imapPassword.fill('\u0000')
            smtpPassword.fill('\u0000')
        }
    }

    private fun clearForms() {
        _credentials.value = CredentialsUiState()
        _incoming.value = ServerUiState()
        _outgoing.value = ServerUiState()
        _note.value = null
    }

    private fun clearAll() {
        clearForms()
        _connectState.value = ConnectState.Idle
    }

    override fun onCleared() {
        connectJob?.cancel()
        clearAll()
    }
}
