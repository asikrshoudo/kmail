package com.kmail.thekami.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SyncSettings(
    val backgroundSync: Boolean = true,
    val intervalMinutes: Int = 15,
    val notifications: Boolean = true,
    val hideContent: Boolean = false,
)

@Singleton
class SyncPrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("kmail_sync", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<SyncSettings> = _state.asStateFlow()

    var askedNotificationPermission: Boolean
        get() = prefs.getBoolean("asked_permission", false)
        set(value) = prefs.edit().putBoolean("asked_permission", value).apply()

    fun update(transform: (SyncSettings) -> SyncSettings) {
        val next = transform(_state.value)
        prefs.edit()
            .putBoolean("bg", next.backgroundSync)
            .putInt("interval", next.intervalMinutes)
            .putBoolean("notify", next.notifications)
            .putBoolean("hide", next.hideContent)
            .apply()
        _state.value = next
    }

    private fun read() = SyncSettings(
        backgroundSync = prefs.getBoolean("bg", true),
        intervalMinutes = prefs.getInt("interval", 15),
        notifications = prefs.getBoolean("notify", true),
        hideContent = prefs.getBoolean("hide", false),
    )
}
