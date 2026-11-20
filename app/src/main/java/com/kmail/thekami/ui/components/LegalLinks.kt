package com.kmail.thekami.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.kmail.thekami.R

internal const val PRIVACY_URL = "https://blog.thekami.tech/posts/kmail-privacy-policy/"
internal const val HOW_TO_USE_URL = "https://blog.thekami.tech/posts/kmail-how-to-use/"

/** Returns a function that opens a link, and does nothing if the phone has no browser. */
@Composable
internal fun openLinkAction(): (String) -> Unit {
    val handler = LocalUriHandler.current
    return { url -> runCatching { handler.openUri(url) } }
}

/** Privacy policy and usage guide, as two quiet text links. */
@Composable
internal fun LegalLinks(modifier: Modifier = Modifier) {
    val open = openLinkAction()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LinkButton(R.string.legal_privacy_policy) { open(PRIVACY_URL) }
        Text(
            text = "·",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics { },
        )
        LinkButton(R.string.legal_how_to_use) { open(HOW_TO_USE_URL) }
    }
}

@Composable
private fun LinkButton(@StringRes label: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
