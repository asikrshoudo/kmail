package com.kmail.thekami.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.AllInbox
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Drafts
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmail.thekami.R
import com.kmail.thekami.data.db.FolderEntity
import com.kmail.thekami.domain.model.FolderRole

fun folderIcon(role: FolderRole): ImageVector = when (role) {
    FolderRole.INBOX -> Icons.Outlined.Inbox
    FolderRole.SENT -> Icons.AutoMirrored.Outlined.Send
    FolderRole.DRAFTS -> Icons.Outlined.Drafts
    FolderRole.ARCHIVE -> Icons.Outlined.Archive
    FolderRole.SPAM -> Icons.Outlined.Report
    FolderRole.TRASH -> Icons.Outlined.Delete
    FolderRole.CUSTOM -> Icons.Outlined.Folder
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
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

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
        Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Outlined.AllInbox, contentDescription = null) },
                label = { Text(stringResource(R.string.home_all_inboxes)) },
                badge = { Badge(allUnread) },
                selected = state.selection is MailboxSelection.Unified,
                onClick = { onSelect(MailboxSelection.Unified) },
                modifier = pad,
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Outlined.StarBorder, contentDescription = null) },
                label = { Text(stringResource(R.string.sidebar_starred)) },
                selected = state.selection is MailboxSelection.Starred,
                onClick = { onSelect(MailboxSelection.Starred) },
                modifier = pad,
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Outlined.MarkEmailUnread, contentDescription = null) },
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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded[entry.account.id] = !open }
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color(entry.account.colorArgb)))
                    Text(
                        text = entry.account.emailAddress,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (open) {
                    entry.folders.forEach { folder ->
                        NavigationDrawerItem(
                            icon = { Icon(folderIcon(folder.role), contentDescription = null) },
                            label = { Text(folderLabel(folder)) },
                            badge = { Badge(state.unread[folder.id]) },
                            selected = state.selection == MailboxSelection.Folder(folder.id),
                            onClick = { onSelect(MailboxSelection.Folder(folder.id)) },
                            modifier = pad,
                        )
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 28.dp))

            NavigationDrawerItem(
                icon = { Icon(Icons.Outlined.EditNote, contentDescription = null) },
                label = { Text(stringResource(R.string.home_drafts)) },
                selected = false,
                onClick = onDrafts,
                modifier = pad,
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                label = { Text(stringResource(R.string.home_settings)) },
                selected = false,
                onClick = onSettings,
                modifier = pad,
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
                label = { Text(stringResource(R.string.home_add_account)) },
                selected = false,
                onClick = onAddAccount,
                modifier = pad,
            )
        }
    }
}
