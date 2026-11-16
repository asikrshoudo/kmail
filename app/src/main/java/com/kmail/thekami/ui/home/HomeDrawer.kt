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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

@Composable
private fun Badge(count: Int?) {
    if (count != null && count > 0) {
        Text(
            if (count > 999) "999+" else count.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
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
            Icon(
                Icons.Rounded.Email,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The mailbox list scrolls. Settings and Add account stay fixed at the bottom. */
@Composable
fun HomeDrawer(
    state: HomeUiState,
    onSelect: (MailboxSelection) -> Unit,
    onDrafts: () -> Unit,
    onSettings: () -> Unit,
    onAddAccount: () -> Unit,
) {
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val pad = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    val allUnread = state.sidebar.flatMap { it.folders }.filter { it.role == FolderRole.INBOX }
        .sumOf { state.unread[it.id] ?: 0 }

    ModalDrawerSheet {
        Column(Modifier.fillMaxHeight()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                DrawerHeader()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.AllInbox, contentDescription = null) },
                    label = { Text(stringResource(R.string.home_all_inboxes)) },
                    badge = { Badge(allUnread) },
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

                state.sidebar.forEachIndexed { index, entry ->
                    val holdsSelection = (state.selection as? MailboxSelection.Folder)
                        ?.let { sel -> entry.folders.any { it.id == sel.id } } == true
                    val open = expanded[entry.account.id] ?: (index == 0 || holdsSelection)
                    val inboxUnread = entry.folders.filter { it.role == FolderRole.INBOX }
                        .sumOf { state.unread[it.id] ?: 0 }
                    Row(
                        pad
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .clickable { expanded[entry.account.id] = !open }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.size(28.dp).clip(CircleShape).background(Color(entry.account.colorArgb)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                entry.account.emailAddress.take(1).uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                            )
                        }
                        Text(
                            text = entry.account.emailAddress,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (!open) Badge(inboxUnread)
                        Icon(
                            if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AnimatedVisibility(visible = open) {
                        Column {
                            entry.folders.forEach { folder ->
                                NavigationDrawerItem(
                                    icon = { Icon(folderIcon(folder.role), contentDescription = null) },
                                    label = { Text(folderLabel(folder)) },
                                    badge = { Badge(state.unread[folder.id]) },
                                    selected = state.selection == MailboxSelection.Folder(folder.id),
                                    onClick = { onSelect(MailboxSelection.Folder(folder.id)) },
                                    modifier = pad.padding(start = 12.dp),
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
                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) },
                    label = { Text(stringResource(R.string.home_add_account)) },
                    selected = false,
                    onClick = onAddAccount,
                    modifier = pad,
                )
            }
        }
    }
}
