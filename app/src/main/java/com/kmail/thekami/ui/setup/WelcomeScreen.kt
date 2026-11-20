package com.kmail.thekami.ui.setup

import com.kmail.thekami.ui.components.AppIcons
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kmail.thekami.R
import com.kmail.thekami.ui.components.KmailWordmark
import com.kmail.thekami.ui.components.LegalLinks
import com.kmail.thekami.ui.theme.rememberReducedMotion

private class Feature(val icon: ImageVector, @StringRes val title: Int, @StringRes val body: Int)

private val features = listOf(
    Feature(AppIcons.At, R.string.welcome_feature_any_title, R.string.welcome_feature_any_body),
    Feature(AppIcons.Lock, R.string.welcome_feature_private_title, R.string.welcome_feature_private_body),
    Feature(AppIcons.QrCode, R.string.welcome_feature_move_title, R.string.welcome_feature_move_body),
)

/**
 * The first screen. Phones get one column with the button pinned at the bottom; wide or landscape
 * screens get the inbox sketch on one side and the text on the other. Content eases in one piece
 * after another, unless the system animation scale is off.
 *
 * QR and file import now live behind "Other ways to log in" on the login screen, so [onScanQr]
 * and [onImportFile] are no longer used here. They stay so existing callers still compile.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
    onScanQr: () -> Unit = {},
    onImportFile: () -> Unit = {},
) {
    val progress = rememberEntrance(rememberReducedMotion())
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val topEnd = Offset(size.width, 0f)
                val bottomStart = Offset(0f, size.height)
                drawCircle(
                    brush = Brush.radialGradient(listOf(primary.copy(alpha = 0.16f), Color.Transparent), topEnd, size.maxDimension * 0.7f),
                    radius = size.maxDimension * 0.7f,
                    center = topEnd,
                )
                drawCircle(
                    brush = Brush.radialGradient(listOf(tertiary.copy(alpha = 0.12f), Color.Transparent), bottomStart, size.maxDimension * 0.6f),
                    radius = size.maxDimension * 0.6f,
                    center = bottomStart,
                )
            }
            .safeDrawingPadding(),
    ) {
        if (maxWidth >= 600.dp && maxWidth > maxHeight) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    WelcomeHero(Modifier.widthIn(max = 380.dp).reveal(progress, 1))
                }
                WelcomeColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    progress = progress,
                    showHero = false,
                    onGetStarted = onGetStarted,
                )
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                WelcomeColumn(
                    modifier = Modifier.widthIn(max = 520.dp).fillMaxHeight(),
                    progress = progress,
                    showHero = this@BoxWithConstraints.maxHeight >= 700.dp,
                    onGetStarted = onGetStarted,
                )
            }
        }
    }
}

@Composable
private fun WelcomeColumn(
    progress: State<Float>,
    showHero: Boolean,
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        // Only this part scrolls, so the button below never moves off screen.
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (showHero) WelcomeHero(Modifier.fillMaxWidth().reveal(progress, 0))
            KmailWordmark(
                modifier = Modifier.reveal(progress, 1),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.welcome_headline),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.reveal(progress, 2),
            )
            Text(
                text = stringResource(R.string.welcome_description),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.reveal(progress, 3),
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                features.forEachIndexed { index, feature ->
                    FeatureRow(feature, Modifier.reveal(progress, 4 + index))
                }
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).reveal(progress, 7),
            ) {
                Text(stringResource(R.string.welcome_get_started), style = MaterialTheme.typography.titleMedium)
            }
            LegalLinks(Modifier.reveal(progress, 8))
        }
    }
}

@Composable
private fun FeatureRow(feature: Feature, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier.size(44.dp).background(scheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(feature.icon, contentDescription = null, tint = scheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(feature.title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(feature.body), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
    }
}

/** 0 to 1 over about a second. It starts at 1 when animations are off, so nothing waits. */
@Composable
private fun rememberEntrance(reduceMotion: Boolean): State<Float> {
    var started by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(Unit) { started = true }
    return animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(900, easing = LinearOutSlowInEasing),
        label = "welcomeEntrance",
    )
}

/** Fades and lifts one element in, offset by [index]. It only touches the draw layer, so it never recomposes. */
private fun Modifier.reveal(progress: State<Float>, index: Int): Modifier = graphicsLayer {
    val t = ((progress.value - index * 0.08f) / 0.4f).coerceIn(0f, 1f)
    alpha = t
    translationY = (1f - t) * 20.dp.toPx()
}
