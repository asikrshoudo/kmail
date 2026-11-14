package com.kmail.thekami.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmail.thekami.data.repo.AccountRepository
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
) : ViewModel() {
    val startRoute: StateFlow<String?> = flow {
        emit(if (accounts.observeAccounts().first().isNotEmpty()) Routes.HOME else Routes.WELCOME)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
