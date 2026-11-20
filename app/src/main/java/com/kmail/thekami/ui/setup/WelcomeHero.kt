package com.kmail.thekami.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/** A drawn preview of an inbox with a lock badge. It is decoration only, so screen readers skip it. */
@Composable
internal fun WelcomeHero(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = modifier.clearAndSetSemantics { }.padding(top = 20.dp, end = 12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surfaceContainer, RoundedCornerShape(28.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MailRowSketch(scheme.primaryContainer, 0.62f, 0.9f, unread = true, highlighted = true)
            MailRowSketch(scheme.tertiaryContainer, 0.48f, 0.78f, unread = true, highlighted = false)
            MailRowSketch(scheme.secondaryContainer, 0.7f, 0.55f, unread = false, highlighted = false)
            MailRowSketch(scheme.surfaceContainerHighest, 0.4f, 0.82f, unread = false, highlighted = false)
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(52.dp)
                .background(scheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun MailRowSketch(
    avatar: Color,
    titleFraction: Float,
    bodyFraction: Float,
    unread: Boolean,
    highlighted: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (highlighted) scheme.primaryContainer.copy(alpha = 0.45f) else Color.Transparent,
                RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).background(avatar, CircleShape))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .fillMaxWidth(titleFraction)
                    .height(10.dp)
                    .background(scheme.onSurface.copy(alpha = 0.78f), RoundedCornerShape(5.dp)),
            )
            Box(
                Modifier
                    .fillMaxWidth(bodyFraction)
                    .height(8.dp)
                    .background(scheme.onSurfaceVariant.copy(alpha = 0.32f), RoundedCornerShape(4.dp)),
            )
        }
        if (unread) Box(Modifier.size(10.dp).background(scheme.primary, CircleShape))
    }
}
