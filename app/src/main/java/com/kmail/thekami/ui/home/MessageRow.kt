package com.kmail.thekami.ui.home

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmail.thekami.R
import com.kmail.thekami.ui.components.AppIcons
import java.util.Date

private val StarAmber = Color(0xFFF2A900)
private val RowShape = RoundedCornerShape(20.dp)

@Composable
private fun formatTime(millis: Long): String {
    if (millis <= 0) return ""
    val context = LocalContext.current
    return if (DateUtils.isToday(millis)) {
        DateFormat.getTimeFormat(context).format(Date(millis))
    } else {
        DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
    }
}

/** Swipe right to archive, left to delete. The row snaps back and disappears through the list update. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SwipeableMessageRow(
    item: MessageItem,
    selected: Boolean,
    selecting: Boolean,
    showAccountDot: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleStar: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onArchive()
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 1.dp).clip(RowShape),
        enableDismissFromStartToEnd = !selecting,
        enableDismissFromEndToStart = !selecting,
        backgroundContent = {
            val archive = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val delete = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            val armed = state.targetValue != SwipeToDismissBoxValue.Settled
            val scheme = MaterialTheme.colorScheme
            val container by animateColorAsState(
                targetValue = when {
                    archive && armed -> scheme.primaryContainer
                    delete && armed -> scheme.errorContainer
                    else -> scheme.surfaceContainerHigh
                },
                label = "swipe-container",
            )
            val iconScale by animateFloatAsState(
                targetValue = if (armed) 1.1f else 0.75f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label = "swipe-icon",
            )
            val iconTint = if (delete) scheme.onErrorContainer else scheme.onPrimaryContainer
            Box(
                Modifier.fillMaxSize().background(container).padding(horizontal = 28.dp),
                contentAlignment = if (delete) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                if (archive) {
                    Icon(
                        AppIcons.Archive,
                        contentDescription = stringResource(R.string.home_archive),
                        tint = iconTint,
                        modifier = Modifier.scale(iconScale),
                    )
                }
                if (delete) {
                    Icon(
                        AppIcons.Trash,
                        contentDescription = stringResource(R.string.home_delete),
                        tint = iconTint,
                        modifier = Modifier.scale(iconScale),
                    )
                }
            }
        },
    ) {
        MessageRow(item, selected, showAccountDot, onClick, onLongClick, onToggleStar)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    item: MessageItem,
    selected: Boolean,
    showAccountDot: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleStar: () -> Unit,
) {
    val unread = !item.isRead
    val scheme = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (selected) scheme.secondaryContainer else scheme.surface,
        animationSpec = tween(180),
        label = "row-background",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Avatar(item, selected)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showAccountDot && item.accountColor != 0) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(item.accountColor)))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = item.sender.ifBlank { stringResource(R.string.home_unknown_sender) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.subject.ifBlank { stringResource(R.string.home_no_subject) },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.snippet.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 12.dp, top = 2.dp),
            ) {
                if (item.hasAttachments) {
                    Icon(
                        AppIcons.Attachment,
                        contentDescription = stringResource(R.string.home_attachment),
                        modifier = Modifier.size(14.dp),
                        tint = scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = formatTime(item.receivedAtMillis),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (unread) scheme.onSurface else scheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onToggleStar, modifier = Modifier.size(44.dp)) {
                AnimatedContent(
                    targetState = item.isStarred,
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center,
                    transitionSpec = {
                        (
                            scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), initialScale = 0.4f) +
                                fadeIn(tween(100))
                            ) togetherWith fadeOut(tween(80))
                    },
                    label = "star",
                ) { starred ->
                    Icon(
                        imageVector = if (starred) AppIcons.Star else AppIcons.StarOutline,
                        contentDescription = stringResource(R.string.home_toggle_star),
                        tint = if (starred) StarAmber else scheme.outline,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

/** A soft tinted circle with the sender's initial. Selecting turns it into a check mark. */
@Composable
private fun Avatar(item: MessageItem, selected: Boolean) {
    val initial = item.sender.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"
    val tone = AvatarColors.colorFor(item.senderKey.ifBlank { item.sender })
    val scheme = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (selected) scheme.primary else tone.copy(alpha = 0.16f),
        animationSpec = tween(180),
        label = "avatar-background",
    )
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                (fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.6f)) togetherWith
                    (fadeOut(tween(100)) + scaleOut(tween(100), targetScale = 0.6f))
            },
            label = "avatar-content",
        ) { isSelected ->
            if (isSelected) {
                Icon(AppIcons.Check, contentDescription = null, tint = scheme.onPrimary)
            } else {
                Text(initial, style = MaterialTheme.typography.titleMedium, color = tone)
            }
        }
    }
}

/** Placeholder rows with a soft moving highlight, shown while the first sync runs. */
@Composable
fun SkeletonList(count: Int = 9) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shift",
    )
    val ink = MaterialTheme.colorScheme.onSurface
    val base = ink.copy(alpha = 0.06f)
    val glow = ink.copy(alpha = 0.13f)
    val x = -500f + shift * 2400f
    val brush = Brush.linearGradient(
        colors = listOf(base, glow, base),
        start = Offset(x, 0f),
        end = Offset(x + 500f, 0f),
    )
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        repeat(count) { index ->
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(brush))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(
                        Modifier.height(13.dp).fillMaxWidth(if (index % 2 == 0) 0.4f else 0.52f)
                            .clip(RoundedCornerShape(7.dp)).background(brush),
                    )
                    Box(
                        Modifier.height(12.dp).fillMaxWidth(0.82f)
                            .clip(RoundedCornerShape(6.dp)).background(brush),
                    )
                    Box(
                        Modifier.height(12.dp).fillMaxWidth(if (index % 3 == 0) 0.6f else 0.7f)
                            .clip(RoundedCornerShape(6.dp)).background(brush),
                    )
                }
            }
        }
    }
}
