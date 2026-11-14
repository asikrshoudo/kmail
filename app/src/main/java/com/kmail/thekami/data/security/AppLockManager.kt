package com.kmail.thekami.data.security

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Decides when the app is locked. A new process always starts locked when App Lock is on. */
@Singleton
class AppLockManager @Inject constructor(
    private val config: LockConfig,
) {
    private val _locked = MutableStateFlow(config.isEnabled)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()
    private var backgroundedAt: Long? = null

    fun onBackground(now: Long = SystemClock.elapsedRealtime()) {
        if (config.isEnabled) backgroundedAt = now
    }

    fun onForeground(now: Long = SystemClock.elapsedRealtime()) {
        val since = backgroundedAt
        backgroundedAt = null
        if (config.isEnabled && since != null && now - since >= config.autoLockSeconds * 1000L) {
            _locked.value = true
        }
    }

    fun unlock() {
        _locked.value = false
    }

    /** Called right after the user turned App Lock on inside the app. */
    fun onLockEnabled() {
        _locked.value = false
    }
}
