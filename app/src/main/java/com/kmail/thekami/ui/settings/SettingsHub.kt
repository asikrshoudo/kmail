package com.kmail.thekami.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmail.thekami.R
import com.kmail.thekami.ui.components.AppIcons
import com.kmail.thekami.ui.support.SupportSection

/** One tappable line inside a settings group. */
data class SettingsEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    /** True for rows that leave the app (web page, store), so they get an "open" arrow. */
    val external: Boolean = false,
    val onClick: () -> Unit,
)

/** The main settings page: grouped cards, one topic per card. */
@Composable
fun SettingsHub(
    accountsSummary: String?,
    themeSummary: String,
    versionName: String,
    onAccounts: () -> Unit,
    onSignature: () -> Unit,
    onAppearance: () -> Unit,
    onLanguage: () -> Unit,
    onSync: () -> Unit,
    onPrivacy: () -> Unit,
    onAbout: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        SettingsGroup(
            title = null,
            entries = listOf(
                SettingsEntry(AppIcons.AccountCircle, stringResource(R.string.settings_accounts), accountsSummary, onClick = onAccounts),
            ),
        )
        SettingsGroup(
            title = stringResource(R.string.settings_group_mail),
            entries = listOf(
                SettingsEntry(AppIcons.NoteEdit, stringResource(R.string.settings_signature_title), onClick = onSignature),
                SettingsEntry(AppIcons.Sync, stringResource(R.string.settings_sync), onClick = onSync),
            ),
        )
        SettingsGroup(
            title = stringResource(R.string.settings_group_personalize),
            entries = buildList {
                add(SettingsEntry(AppIcons.Palette, stringResource(R.string.settings_appearance), themeSummary, onClick = onAppearance))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(SettingsEntry(AppIcons.Language, stringResource(R.string.settings_language), currentLanguageLabel(), onClick = onLanguage))
                }
            },
        )
        SettingsGroup(
            title = stringResource(R.string.settings_group_security),
            entries = listOf(
                SettingsEntry(AppIcons.Lock, stringResource(R.string.settings_privacy), onClick = onPrivacy),
            ),
        )
        SupportSection()
        SettingsGroup(
            title = stringResource(R.string.settings_group_about),
            entries = listOf(
                SettingsEntry(AppIcons.Info, stringResource(R.string.settings_about), onClick = onAbout),
            ),
        )
        HubFooter(versionName)
    }
}

/** A titled, rounded card holding a list of rows. A null title leaves only a small gap. */
@Composable
fun SettingsGroup(title: String?, entries: List<SettingsEntry>) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, top = 20.dp, bottom = 8.dp),
            )
        } else {
            Spacer(Modifier.height(12.dp))
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column {
                entries.forEachIndexed { index, entry ->
                    SettingsRow(entry)
                    if (index < entries.lastIndex) {
                        HorizontalDivider(
                            Modifier.padding(start = 72.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(entry: SettingsEntry) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = entry.onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                entry.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.bodyLarge)
            if (entry.subtitle != null) {
                Text(
                    entry.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            if (entry.external) AppIcons.OpenInNew else AppIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HubFooter(versionName: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.settings_brand), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.settings_version, versionName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
