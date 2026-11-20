package com.kmail.thekami.ui.home

import com.kmail.thekami.ui.components.AppIcons
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmail.thekami.R
import java.util.Date

private val StarAmber = Color(0xFFF2A900)

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
        modifier = modifier,
        enableDismissFromStartToEnd = !selecting,
        enableDismissFromEndToStart = !selecting,
        backgroundContent = {
            val archive = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val delete = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        when {
                            archive -> MaterialTheme.colorScheme.primaryContainer
                            delete -> MaterialTheme.colorScheme.errorContainer
                            else -> Color.Transparent
                        },
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = if (delete) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                if (archive) Icon(AppIcons.Archive, contentDescription = stringResource(R.string.home_archive))
                if (delete) Icon(AppIcons.Trash, contentDescription = stringResource(R.string.home_delete))
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
    val weight = if (unread) FontWeight.Bold else FontWeight.Normal
    val background = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    fontWeight = weight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (unread) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                }
            }
            Text(
                text = item.subject.ifBlank { stringResource(R.string.home_no_subject) },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.snippet.isNotBlank()) {
                Text(
                    text = item.snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp, top = 2.dp)) {
                if (item.hasAttachments) {
                    Icon(
                        AppIcons.Attachment,
                        contentDescription = stringResource(R.string.home_attachment),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = formatTime(item.receivedAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = weight,
                    color = if (unread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onToggleStar, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = if (item.isStarred) AppIcons.Star else AppIcons.StarOutline,
                    contentDescription = stringResource(R.string.home_toggle_star),
                    tint = if (item.isStarred) StarAmber else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun Avatar(item: MessageItem, selected: Boolean) {
    val initial = item.sender.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else AvatarColors.colorFor(item.senderKey.ifBlank { item.sender }),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(AppIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(initial, style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}

/** Placeholder rows shown while the first sync runs, so the screen never looks empty or frozen. */
@Composable
fun SkeletonList(count: Int = 9) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    val tint = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha)
    Column(Modifier.fillMaxSize()) {
        repeat(count) { index ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(tint))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.height(12.dp).fillMaxWidth(if (index % 2 == 0) 0.45f else 0.6f).clip(RoundedCornerShape(6.dp)).background(tint))
                    Box(Modifier.height(12.dp).fillMaxWidth(0.85f).clip(RoundedCornerShape(6.dp)).background(tint))
                    Box(Modifier.height(10.dp).fillMaxWidth(0.7f).clip(RoundedCornerShape(5.dp)).background(tint))
                }
            }
        }
    }
}
