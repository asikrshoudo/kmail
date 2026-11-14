package com.kmail.thekami.ui.home

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmail.thekami.R
import com.kmail.thekami.data.db.FolderEntity
import com.kmail.thekami.domain.model.FolderRole
import com.kmail.thekami.ui.setup.messageRes
import java.time.LocalTime
import java.util.Date
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddAccount: () -> Unit,
    onOpenMessage: (Long) -> Unit,
    onCompose: () -> Unit,
    modifier: Modifier = Modifier,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    val selecting = state.selectedIds.isNotEmpty()
    BackHandler(enabled = selecting) { vm.clearSelection() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier,
        gesturesEnabled = !selecting,
        drawerContent = {
            Sidebar(
                state = state,
                onSelect = {
                    vm.select(it)
                    scope.launch { drawerState.close() }
                },
                onAddAccount = {
                    scope.launch { drawerState.close() }
                    onAddAccount()
                },
            )
        },
    ) {
        Scaffold(
            floatingActionButton = {
                if (!selecting) {
                    FloatingActionButton(onClick = onCompose) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.home_compose))
                    }
                }
            },
            topBar = {
                if (selecting) {
                    SelectionBar(
                        count = state.selectedIds.size,
                        onCancel = vm::clearSelection,
                        onArchive = vm::archive,
                        onDelete = vm::delete,
                        onMarkRead = vm::markRead,
                        onStar = vm::setStarred,
                        onSelectAll = vm::selectAll,
                    )
                } else {
                    CenterAlignedTopAppBar(
                        title = { Text(greeting()) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.home_open_menu))
                            }
                        },
                        actions = {
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.home_more_options))
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.home_refresh)) },
                                        onClick = { menuOpen = false; vm.refresh() },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.home_select_all)) },
                                        onClick = { menuOpen = false; vm.selectAll() },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.home_add_account)) },
                                        onClick = { menuOpen = false; onAddAccount() },
                                    )
                                }
                            }
                        },
                    )
                }
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::onQuery,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.home_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                )
                state.error?.let { error ->
                    Banner(
                        text = stringResource(R.string.home_sync_error, stringResource(error.messageRes())),
                        onDismiss = null,
                    )
                }
                state.notice?.let { notice ->
                    actionNoticeText(notice)?.let { Banner(text = it, onDismiss = vm::dismissNotice) }
                }
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = vm::refresh,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        if (state.messages.isEmpty()) {
                            item {
                                Column(
                                    Modifier.fillParentMaxSize().padding(32.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        stringResource(R.string.home_empty_message),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            items(state.messages, key = { it.id }) { item ->
                                MessageRow(
                                    item = item,
                                    selected = item.id in state.selectedIds,
                                    onClick = { if (selecting) vm.toggleSelect(item.id) else onOpenMessage(item.id) },
                                    onLongClick = { vm.toggleSelect(item.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionBar(
    count: Int,
    onCancel: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onMarkRead: (Boolean) -> Unit,
    onStar: (Boolean) -> Unit,
    onSelectAll: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(stringResource(R.string.home_selected, count)) },
        navigationIcon = {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.home_cancel_selection))
            }
        },
        actions = {
            TextButton(onClick = onArchive) { Text(stringResource(R.string.home_archive)) }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.home_delete))
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.home_more_options))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.home_mark_read)) }, onClick = { menu = false; onMarkRead(true) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.home_mark_unread)) }, onClick = { menu = false; onMarkRead(false) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.home_star)) }, onClick = { menu = false; onStar(true) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.home_unstar)) }, onClick = { menu = false; onStar(false) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.home_select_all)) }, onClick = { menu = false; onSelectAll() })
                }
            }
        },
    )
}

@Composable
private fun Banner(text: String, onDismiss: (() -> Unit)?) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            )
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.compose_discard_keep))
                }
            }
        }
    }
}

@Composable
private fun greeting(): String {
    val hour = remember { LocalTime.now().hour }
    return stringResource(
        when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..16 -> R.string.greeting_afternoon
            in 17..20 -> R.string.greeting_evening
            else -> R.string.greeting_night
        },
    )
}

@Composable
private fun folderLabel(folder: FolderEntity): String = when (folder.role) {
    FolderRole.INBOX -> stringResource(R.string.role_inbox)
    FolderRole.SENT -> stringResource(R.string.role_sent)
    FolderRole.DRAFTS -> stringResource(R.string.role_drafts)
    FolderRole.ARCHIVE -> stringResource(R.string.role_archive)
    FolderRole.SPAM -> stringResource(R.string.role_spam)
    FolderRole.TRASH -> stringResource(R.string.role_trash)
    FolderRole.CUSTOM -> folder.displayName
}

@Composable
private fun Sidebar(
    state: HomeUiState,
    onSelect: (MailboxSelection) -> Unit,
    onAddAccount: () -> Unit,
) {
    ModalDrawerSheet {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.home_all_inboxes)) },
                selected = state.selection is MailboxSelection.Unified,
                onClick = { onSelect(MailboxSelection.Unified) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
            state.sidebar.forEach { entry ->
                Text(
                    text = entry.account.emailAddress,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
                )
                entry.folders.forEach { folder ->
                    NavigationDrawerItem(
                        label = { Text(folderLabel(folder)) },
                        selected = state.selection == MailboxSelection.Folder(folder.id),
                        onClick = { onSelect(MailboxSelection.Folder(folder.id)) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp, horizontal = 28.dp))
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.home_add_account)) },
                selected = false,
                onClick = onAddAccount,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }
    }
}

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    item: MessageItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val weight = if (item.isRead) FontWeight.Normal else FontWeight.Bold
    val initial = item.sender.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"
    val background = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(if (selected) "\u2713" else initial, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.sender.ifBlank { stringResource(R.string.home_unknown_sender) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = weight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatTime(item.receivedAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.subject.ifBlank { stringResource(R.string.home_no_subject) },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = weight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (item.hasAttachments) Text("\uD83D\uDCCE", modifier = Modifier.padding(start = 6.dp))
                if (item.isStarred) {
                    Text("\u2605", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp))
                }
            }
            if (item.snippet.isNotBlank()) {
                Text(
                    text = item.snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
