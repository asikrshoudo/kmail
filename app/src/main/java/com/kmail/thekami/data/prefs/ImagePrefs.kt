package com.kmail.thekami.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ImageMode { ALWAYS, ASK, NEVER }

object ImagePolicy {
    /** Whether pictures from the internet load as soon as a message opens. */
    fun initialAllow(mode: ImageMode, trusted: Set<String>, sender: String?): Boolean = when (mode) {
        ImageMode.ALWAYS -> true
        ImageMode.NEVER -> false
        ImageMode.ASK -> sender != null && sender.trim().lowercase() in trusted
    }
}

/** Remote picture preference. Loading them tells the sender that and when you opened the message. */
@Singleton
class ImagePrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("kmail_images", Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(readMode())
    val mode: StateFlow<ImageMode> = _mode.asStateFlow()

    fun setMode(mode: ImageMode) {
        prefs.edit().putString("mode", mode.name).apply()
        _mode.value = mode
    }

    fun trust(sender: String) {
        prefs.edit().putStringSet("trusted", trusted() + sender.trim().lowercase()).apply()
    }

    fun allowFor(sender: String?): Boolean = ImagePolicy.initialAllow(_mode.value, trusted(), sender)

    private fun trusted(): Set<String> = prefs.getStringSet("trusted", emptySet()).orEmpty().toSet()

    private fun readMode(): ImageMode =
        runCatching { ImageMode.valueOf(prefs.getString("mode", null) ?: "ALWAYS") }.getOrDefault(ImageMode.ALWAYS)
}
