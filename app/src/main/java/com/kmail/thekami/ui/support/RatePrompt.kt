package com.kmail.thekami.ui.support

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.kmail.thekami.R
import kotlinx.coroutines.delay

private const val TICK_SECONDS = 15
private const val MIN_USAGE_SECONDS = 10 * 60
private const val MIN_USAGE_AFTER_DAYS_SECONDS = 3 * 60
private const val DAYS_BEFORE_ASKING = 2
private const val DAY_MS = 24L * 60 * 60 * 1000
private const val ASK_AGAIN_MS = 14 * DAY_MS

/** Remembers how long the app was used and whether the person was already asked. */
private class RatePromptStore(context: Context) {
    private val prefs = context.getSharedPreferences("rate_prompt", Context.MODE_PRIVATE)

    fun markFirstOpen() {
        if (prefs.getLong(FIRST_OPEN, 0L) == 0L) {
            prefs.edit().putLong(FIRST_OPEN, System.currentTimeMillis()).apply()
        }
    }

    fun addUsage(seconds: Int) {
        prefs.edit().putLong(USAGE, prefs.getLong(USAGE, 0L) + seconds).apply()
    }

    fun shouldAsk(): Boolean {
        if (prefs.getBoolean(DONE, false)) return false
        val now = System.currentTimeMillis()
        val lastAsked = prefs.getLong(LAST_ASKED, 0L)
        if (lastAsked != 0L && now - lastAsked < ASK_AGAIN_MS) return false
        val usage = prefs.getLong(USAGE, 0L)
        val days = (now - prefs.getLong(FIRST_OPEN, now)) / DAY_MS
        return usage >= MIN_USAGE_SECONDS ||
            (days >= DAYS_BEFORE_ASKING && usage >= MIN_USAGE_AFTER_DAYS_SECONDS)
    }

    fun later() {
        prefs.edit().putLong(LAST_ASKED, System.currentTimeMillis()).apply()
    }

    fun done() {
        prefs.edit().putBoolean(DONE, true).apply()
    }

    private companion object {
        const val FIRST_OPEN = "first_open"
        const val USAGE = "usage_seconds"
        const val LAST_ASKED = "last_asked"
        const val DONE = "done"
    }
}

/**
 * Put this once inside the app's main content. It does nothing unless Google Play installed the app.
 * It counts the time the app is on screen and asks for a Play Store rating once, politely.
 */
@Composable
fun RatePromptHost() {
    val context = LocalContext.current
    val fromPlay = remember { isInstalledFromPlayStore(context) }
    if (fromPlay) RatePromptContent()
}

@Composable
private fun RatePromptContent() {
    val context = LocalContext.current
    val store = remember { RatePromptStore(context.applicationContext) }
    var show by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            store.markFirstOpen()
            while (!show) {
                delay(TICK_SECONDS * 1000L)
                store.addUsage(TICK_SECONDS)
                if (store.shouldAsk()) show = true
            }
        }
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { store.later(); show = false },
            title = { Text(stringResource(R.string.rate_title)) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.rate_message))
                    TextButton(
                        onClick = { store.done(); show = false },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text(stringResource(R.string.rate_never)) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    store.done()
                    show = false
                    openPlayListing(context)
                }) { Text(stringResource(R.string.rate_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { store.later(); show = false }) {
                    Text(stringResource(R.string.rate_later))
                }
            },
        )
    }
}
