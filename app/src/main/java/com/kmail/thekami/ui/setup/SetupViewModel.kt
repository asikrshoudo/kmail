package com.kmail.thekami.ui.setup

import androidx.lifecycle.ViewModel
import com.kmail.thekami.domain.model.normalizeEmail
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CredentialsUiState(
    val email: String = "",
    val password: String = "",
    val emailError: Boolean = false,
    val passwordError: Boolean = false,
)

/**
 * State for the whole account setup flow. The password lives here only as long as the flow;
 * it is never saved to saved-instance state and is cleared in [onCleared].
 */
@HiltViewModel
class SetupViewModel @Inject constructor() : ViewModel() {

    private val _credentials = MutableStateFlow(CredentialsUiState())
    val credentials: StateFlow<CredentialsUiState> = _credentials.asStateFlow()

    fun onEmailChange(value: String) {
        _credentials.update { it.copy(email = value, emailError = false) }
    }

    fun onPasswordChange(value: String) {
        _credentials.update { it.copy(password = value, passwordError = false) }
    }

    /** Flags invalid fields. Returns true when the flow may continue. */
    fun submitCredentials(): Boolean {
        val current = _credentials.value
        val emailOk = normalizeEmail(current.email) != null
        val passwordOk = current.password.isNotEmpty()
        _credentials.update { it.copy(emailError = !emailOk, passwordError = !passwordOk) }
        return emailOk && passwordOk
    }

    override fun onCleared() {
        _credentials.value = CredentialsUiState()
    }
}
