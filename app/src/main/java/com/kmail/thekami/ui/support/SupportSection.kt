package com.kmail.thekami.ui.support

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kmail.thekami.R
import com.kmail.thekami.ui.components.AppIcons
import com.kmail.thekami.ui.components.ExtraIcons
import com.kmail.thekami.ui.components.openLinkAction
import com.kmail.thekami.ui.settings.SettingsEntry
import com.kmail.thekami.ui.settings.SettingsGroup

/**
 * Settings card that depends on where the app came from:
 * Play Store installs get a rating row, GitHub and other installs get donate and star rows.
 */
@Composable
fun SupportSection() {
    val context = LocalContext.current
    val openLink = openLinkAction()
    val fromPlay = remember { isInstalledFromPlayStore(context) }

    val rateTitle = stringResource(R.string.support_rate)
    val rateDesc = stringResource(R.string.support_rate_desc)
    val donateTitle = stringResource(R.string.support_donate)
    val donateDesc = stringResource(R.string.support_donate_desc)
    val starTitle = stringResource(R.string.support_star)
    val starDesc = stringResource(R.string.support_star_desc)

    val entries = if (fromPlay) {
        listOf(
            SettingsEntry(AppIcons.StarOutline, rateTitle, rateDesc, external = true) { openPlayListing(context) },
        )
    } else {
        buildList {
            if (DONATE_URL.isNotBlank()) {
                add(SettingsEntry(ExtraIcons.Heart, donateTitle, donateDesc, external = true) { openLink(DONATE_URL) })
            }
            add(SettingsEntry(ExtraIcons.GitHub, starTitle, starDesc, external = true) { openLink(GITHUB_URL) })
        }
    }

    SettingsGroup(stringResource(R.string.support_header), entries)
}
