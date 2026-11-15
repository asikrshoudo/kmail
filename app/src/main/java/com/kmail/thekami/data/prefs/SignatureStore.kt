package com.kmail.thekami.data.prefs

import android.content.Context
import com.kmail.thekami.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SignatureState(
    val enabled: Boolean,
    val defaultText: String,
    val perAccount: Map<String, String>,
)

/** Plain-text signatures: a default one, and an optional different one per account. */
@Singleton
class SignatureStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("kmail_signature", Context.MODE_PRIVATE)
    private val fallback = context.getString(R.string.compose_signature)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<SignatureState> = _state.asStateFlow()

    fun textFor(accountId: String?): String {
        val s = _state.value
        if (!s.enabled) return ""
        val own = accountId?.let { s.perAccount[it] }
        return own?.takeIf { it.isNotBlank() } ?: s.defaultText
    }

    fun setEnabled(on: Boolean) {
        prefs.edit().putBoolean("enabled", on).apply()
        _state.value = _state.value.copy(enabled = on)
    }

    fun setDefault(text: String) {
        prefs.edit().putString("default", text).apply()
        _state.value = _state.value.copy(defaultText = text)
    }

    /** An empty [text] removes the override, so the default signature is used again. */
    fun setForAccount(accountId: String, text: String) {
        val trimmed = text.trim()
        val edit = prefs.edit()
        if (trimmed.isEmpty()) edit.remove("acc_$accountId") else edit.putString("acc_$accountId", trimmed)
        edit.apply()
        val map = _state.value.perAccount.toMutableMap()
        if (trimmed.isEmpty()) map.remove(accountId) else map[accountId] = trimmed
        _state.value = _state.value.copy(perAccount = map)
    }

    private fun read(): SignatureState {
        val per = prefs.all.filterKeys { it.startsWith("acc_") }
            .mapNotNull { (key, value) -> (value as? String)?.let { key.removePrefix("acc_") to it } }
            .toMap()
        return SignatureState(
            enabled = prefs.getBoolean("enabled", true),
            defaultText = prefs.getString("default", null) ?: fallback,
            perAccount = per,
        )
    }
}
