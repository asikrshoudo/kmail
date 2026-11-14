package com.kmail.thekami.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmail.thekami.data.repo.AccountRepository
import com.kmail.thekami.sync.SyncPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** Decides once at launch: Home if an account is already saved, otherwise Welcome. */
@HiltViewModel
class AppStartViewModel @Inject constructor(
    accounts: AccountRepository,
    private val syncPrefs: SyncPrefs,
) : ViewModel() {
    val startRoute: StateFlow<String?> = flow {
        emit(if (accounts.observeAccounts().first().isNotEmpty()) Routes.HOME else Routes.WELCOME)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** True only once, the first time Home is shown, so the notification permission is asked once. */
    fun takeNotificationPrompt(): Boolean {
        if (syncPrefs.askedNotificationPermission) return false
        syncPrefs.askedNotificationPermission = true
        return true
    }
}
