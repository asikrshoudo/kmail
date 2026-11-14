package com.kmail.thekami.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmail.thekami.data.repo.AccountRepository
import com.kmail.thekami.data.repo.MessageBodyCache
import com.kmail.thekami.data.security.AppLockManager
import com.kmail.thekami.data.security.AppLockStore
import com.kmail.thekami.data.security.PinPolicy
import com.kmail.thekami.data.security.PinVerify
import com.kmail.thekami.domain.model.Account
import com.kmail.thekami.ui.lock.Biometric
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class PinSetResult { OK, FORMAT, WEAK, MISMATCH }

data class LockUi(
    val enabled: Boolean,
    val biometric: Boolean,
    val biometricAvailable: Boolean,
    val autoLockSeconds: Int,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountRepository: AccountRepository,
    private val store: AppLockStore,
    private val manager: AppLockManager,
    private val cache: MessageBodyCache,
) : ViewModel() {

    /** Null until the first load, so "no accounts" can be told apart from "not loaded yet". */
    val accounts: StateFlow<List<Account>?> = accountRepository.observeAccounts()
        .map<List<Account>, List<Account>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _lock = MutableStateFlow(readLock())
    val lock: StateFlow<LockUi> = _lock.asStateFlow()

    val versionName: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

    private fun readLock() = LockUi(
        enabled = store.isEnabled,
        biometric = store.biometricEnabled,
        biometricAvailable = Biometric.isAvailable(context),
        autoLockSeconds = store.autoLockSeconds,
    )

    suspend fun enableLock(pin: String, confirm: String): PinSetResult {
        when (PinPolicy.check(pin)) {
            PinPolicy.Problem.FORMAT -> return PinSetResult.FORMAT
            PinPolicy.Problem.WEAK -> return PinSetResult.WEAK
            null -> Unit
        }
        if (pin != confirm) return PinSetResult.MISMATCH
        store.setPin(pin)
        manager.onLockEnabled()
        _lock.value = readLock()
        return PinSetResult.OK
    }

    suspend fun disableLock(pin: String): PinVerify {
        val result = store.verify(pin)
        if (result is PinVerify.Ok) {
            store.clear()
            _lock.value = readLock()
        }
        return result
    }

    fun setBiometric(enabled: Boolean) {
        store.setBiometricEnabled(enabled)
        _lock.value = readLock()
    }

    fun setAutoLock(seconds: Int) {
        store.setAutoLockSeconds(seconds)
        _lock.value = readLock()
    }

    /** With App Lock on, the PIN must be right before anything is deleted. */
    suspend fun removeAccount(id: String, pin: String?): PinVerify {
        if (store.isEnabled) {
            val check = store.verify(pin.orEmpty())
            if (check !is PinVerify.Ok) return check
        }
        accountRepository.removeAccount(id)
        cache.clear()
        return PinVerify.Ok
    }
}
