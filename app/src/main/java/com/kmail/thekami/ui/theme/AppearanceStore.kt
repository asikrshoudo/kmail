package com.kmail.thekami.ui.theme

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Appearance(
    val mode: ThemeMode = ThemeMode.System,
    val amoled: Boolean = false,
    val dynamicColor: Boolean = true,
)

@Singleton
class AppearanceStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("kmail_appearance", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<Appearance> = _state.asStateFlow()

    fun update(transform: (Appearance) -> Appearance) {
        val next = transform(_state.value)
        prefs.edit()
            .putString("mode", next.mode.name)
            .putBoolean("amoled", next.amoled)
            .putBoolean("dynamic", next.dynamicColor)
            .apply()
        _state.value = next
    }

    private fun read() = Appearance(
        mode = runCatching { ThemeMode.valueOf(prefs.getString("mode", null) ?: "System") }.getOrDefault(ThemeMode.System),
        amoled = prefs.getBoolean("amoled", false),
        dynamicColor = prefs.getBoolean("dynamic", true),
    )
}
