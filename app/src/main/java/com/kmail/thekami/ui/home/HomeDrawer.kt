package com.kmail.thekami.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AllInbox
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Drafts
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kmail.thekami.R
import com.kmail.thekami.data.db.FolderEntity
import com.kmail.thekami.domain.model.FolderRole

fun folderIcon(role: FolderRole): ImageVector = when (role) {
    FolderRole.INBOX -> Icons.Rounded.Inbox
    FolderRole.SENT -> Icons.AutoMirrored.Rounded.Send
    FolderRole.DRAFTS -> Icons.Rounded.Drafts
    FolderRole.ARCHIVE -> Icons.Rounded.Archive
    FolderRole.SPAM -> Icons.Rounded.Report
    FolderRole.TRASH -> Icons.Rounded.Delete
    FolderRole.CUSTOM -> Icons.Rounded.Folder
}

@Composable
fun folderLabel(folder: FolderEntity): String = when (folder.role) {
    FolderRole.INBOX -> stringResource(R.string.role_inbox)
    FolderRole.SENT -> stringResource(R.string.role_sent)
    FolderRole.DRAFTS -> stringResource(R.string.role_drafts)
    FolderRole.ARCHIVE -> stringResource(R.string.role_archive)
    FolderRole.SPAM -> stringResource(R.string.role_spam)
    FolderRole.TRASH -> stringResource(R.string.role_trash)
    FolderRole.CUSTOM -> folder.displayName
}

private fun rolePriority(role: FolderRole): Int = when (role) {
    FolderRole.INBOX -> 0
    FolderRole.DRAFTS -> 1
    FolderRole.SENT -> 2
    FolderRole.ARCHIVE -> 3
    FolderRole.SPAM -> 4
    FolderRole.TRASH -> 5
    FolderRole.CUSTOM -> 6
}

/** Red pill: there is unread mail waiting in an inbox. */
@Composable
private fun UnreadPill(count: Int?) {
    if (count != null && count > 0) {
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.error)
                .padding(horizontal = 7.dp, vertical = 1.dp),
        ) {
            Text(
                if (count > 999) "999+" else count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onError,
            )
        }
    }
}

/** Quiet count for folders other than the inbox. */
@Composable
private fun CountText(count: Int?) {
    if (count != null && count > 0) {
        Text(
            if (count > 999) "999+" else count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccountAvatar(email: String, color: Color, size: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            email.take(1).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

@Composable
private fun DrawerHeader() {
    Row(
        Modifier.padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Top: the combined views (all inboxes, starred, unread).
 * Middle: an account switcher and the folders of the active account only.
 * Bottom (fixed): settings.
 */
@Composable
fun HomeDrawer(
    state: HomeUiState,
    onSelect: (MailboxSelection) -> Unit,
    onDrafts: () -> Unit,
    onSettings: () -> Unit,
    onAddAccount: () -> Unit,
) {
    val pad = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    var chosenId by rememberSaveable { mutableStateOf<String?>(null) }
    var switching by remember { mutableStateOf(false) }

    fun inboxUnread(folders: List<FolderEntity>): Int =
        folders.filter { it.role == FolderRole.INBOX }.sumOf { state.unread[it.id] ?: 0 }

    val allUnread = state.sidebar.sumOf { inboxUnread(it.folders) }
    val selectedAccountId = (state.selection as? MailboxSelection.Folder)?.let { sel ->
        state.sidebar.firstOrNull { e -> e.folders.any { it.id == sel.id } }?.account?.id
    }
    val activeId = selectedAccountId ?: chosenId
    val active = state.sidebar.firstOrNull { it.account.id == activeId } ?: state.sidebar.firstOrNull()

    ModalDrawerSheet {
        Column(Modifier.fillMaxHeight()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                DrawerHeader()

                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.AllInbox, contentDescription = null) },
                    label = { Text(stringResource(R.string.home_all_inboxes)) },
                    badge = { UnreadPill(allUnread) },
                    selected = state.selection is MailboxSelection.Unified,
                    onClick = { onSelect(MailboxSelection.Unified) },
                    modifier = pad,
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.StarBorder, contentDescription = null) },
                    label = { Text(stringResource(R.string.sidebar_starred)) },
                    selected = state.selection is MailboxSelection.Starred,
                    onClick = { onSelect(MailboxSelection.Starred) },
                    modifier = pad,
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.MarkEmailUnread, contentDescription = null) },
                    label = { Text(stringResource(R.string.sidebar_unread)) },
                    selected = state.selection is MailboxSelection.Unread,
                    onClick = { onSelect(MailboxSelection.Unread) },
                    modifier = pad,
                )

                HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 28.dp))

                if (active != null) {
                    val othersUnread = state.sidebar.any {
                        it.account.id != active.account.id && inboxUnread(it.folders) > 0
                    }

                    // Account switcher
                    Row(
                        pad
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { switching = !switching }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AccountAvatar(active.account.emailAddress, Color(active.account.colorArgb), 32.dp)
                        Text(
                            text = active.account.emailAddress,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (othersUnread && !switching) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                        }
                        Icon(
                            if (switching) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    AnimatedVisibility(visible = switching) {
                        Column {
                            state.sidebar.forEach { entry ->
                                val isActive = entry.account.id == active.account.id
                                Row(
                                    pad
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(28.dp))
                                        .clickable {
                                            chosenId = entry.account.id
                                            switching = false
                                            if (!isActive) {
                                                entry.folders.firstOrNull { it.role == FolderRole.INBOX }
                                                    ?.let { onSelect(MailboxSelection.Folder(it.id)) }
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    AccountAvatar(entry.account.emailAddress, Color(entry.account.colorArgb), 28.dp)
                                    Text(
                                        text = entry.account.emailAddress,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (isActive) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    } else {
                                        UnreadPill(inboxUnread(entry.folders))
                                    }
                                }
                            }
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) },
                                label = { Text(stringResource(R.string.home_add_account)) },
                                selected = false,
                                onClick = onAddAccount,
                                modifier = pad,
                            )
                        }
                    }

                    // Every folder of the active account, in one flat list.
                    AnimatedVisibility(visible = !switching) {
                        Column(Modifier.padding(top = 4.dp)) {
                            active.folders.sortedBy { rolePriority(it.role) }.forEach { folder ->
                                NavigationDrawerItem(
                                    icon = { Icon(folderIcon(folder.role), contentDescription = null) },
                                    label = { Text(folderLabel(folder)) },
                                    badge = {
                                        if (folder.role == FolderRole.INBOX) {
                                            UnreadPill(state.unread[folder.id])
                                        } else {
                                            CountText(state.unread[folder.id])
                                        }
                                    },
                                    selected = state.selection == MailboxSelection.Folder(folder.id),
                                    onClick = { onSelect(MailboxSelection.Folder(folder.id)) },
                                    modifier = pad,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 28.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.EditNote, contentDescription = null) },
                    label = { Text(stringResource(R.string.home_drafts)) },
                    selected = false,
                    onClick = onDrafts,
                    modifier = pad,
                )
            }

            HorizontalDivider(Modifier.padding(horizontal = 28.dp))
            Column(Modifier.padding(vertical = 8.dp)) {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.home_settings)) },
                    selected = false,
                    onClick = onSettings,
                    modifier = pad,
                )
            }
        }
    }
}
