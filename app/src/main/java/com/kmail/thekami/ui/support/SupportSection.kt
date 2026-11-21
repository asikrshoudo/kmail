package com.kmail.thekami.ui.support

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kmail.thekami.R
import com.kmail.thekami.ui.components.AppIcons
import com.kmail.thekami.ui.components.ExtraIcons
import com.kmail.thekami.ui.components.openLinkAction

/**
 * Settings block that depends on where the app came from:
 * Play Store installs get a rating row, GitHub and other installs get donate and star rows.
 */
@Composable
fun SupportSection() {
    val context = LocalContext.current
    val openLink = openLinkAction()
    val fromPlay = remember { isInstalledFromPlayStore(context) }

    HorizontalDivider(
        Modifier.padding(top = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
    Text(
        stringResource(R.string.support_header),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
    )

    if (fromPlay) {
        SupportRow(AppIcons.StarOutline, R.string.support_rate, R.string.support_rate_desc) {
            openPlayListing(context)
        }
    } else {
        if (DONATE_URL.isNotBlank()) {
            SupportRow(ExtraIcons.Heart, R.string.support_donate, R.string.support_donate_desc) {
                openLink(DONATE_URL)
            }
        }
        SupportRow(ExtraIcons.GitHub, R.string.support_star, R.string.support_star_desc) {
            openLink(GITHUB_URL)
        }
    }
}

@Composable
private fun SupportRow(
    icon: ImageVector,
    @StringRes title: Int,
    @StringRes subtitle: Int,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(AppIcons.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
