package com.kmail.thekami.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kmail.thekami.R

/** Language tag to the name written in that language itself, so it is always readable. */
private val LANGUAGES = listOf(
    "en" to "English",
    "bn" to "\u09AC\u09BE\u0982\u09B2\u09BE",
    "hi" to "\u0939\u093F\u0928\u094D\u0926\u0940",
    "ar" to "\u0627\u0644\u0639\u0631\u0628\u064A\u0629",
    "de" to "Deutsch",
    "ja" to "\u65E5\u672C\u8A9E",
    "ko" to "\uD55C\uAD6D\uC5B4",
)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun appLanguageTag(context: Context): String {
    val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
    return if (locales.isEmpty()) "" else locales.get(0).language
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun setAppLanguage(context: Context, tag: String) {
    context.getSystemService(LocaleManager::class.java).applicationLocales =
        if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
}

/** Name of the language in use, for the subtitle in the settings list. */
@Composable
fun currentLanguageLabel(): String {
    val context = LocalContext.current
    val systemLabel = stringResource(R.string.settings_language_system)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return systemLabel
    val tag = appLanguageTag(context)
    return LANGUAGES.firstOrNull { it.first == tag }?.second ?: systemLabel
}

@Composable
fun LanguagePage() {
    val context = LocalContext.current
    val systemLabel = stringResource(R.string.settings_language_system)
    val current = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) appLanguageTag(context) else ""
    val rows = listOf("" to systemLabel) + LANGUAGES
    Column(Modifier.fillMaxWidth()) {
        rows.forEach { (tag, name) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && tag != current) {
                            setAppLanguage(context, tag)
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (tag == current) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
