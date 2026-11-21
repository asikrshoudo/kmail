package com.kmail.thekami.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmail.thekami.R
import com.kmail.thekami.data.db.FolderEntity
import com.kmail.thekami.ui.components.AppIcons
import com.kmail.thekami.ui.setup.messageRes
import kotlinx.coroutines.launch

/** What the list area is showing. Changes between these cross-fade. */
private enum class ListKind { Loading, Empty, Messages }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onAddAccount: () -> Unit,
    onOpenMessage: (Long) -> Unit,
    onCompose: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDrafts: () -> Unit,
    modifier: Modifier = Modifier,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var showMove by remember { mutableStateOf(false) }
    val selecting = state.selectedIds.isNotEmpty()
    BackHandler(enabled = selecting) { vm.clearSelection() }
    if (showMove) {
        MoveDialog(state, onPick = { vm.moveSelected(it); showMove = false }, onDismiss = { showMove = false })
    }

    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, state.messages.size) {
        if (nearEnd && state.messages.isNotEmpty()) vm.loadMore()
    }
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    val undoLabel = stringResource(R.string.home_undo)
    LaunchedEffect(Unit) {
        vm.undoEvents.collect { event ->
            val text = context.getString(
                if (event.kind == UndoKind.ARCHIVE) R.string.home_archived else R.string.home_deleted,
                event.count,
            )
            val result = snackbar.showSnackbar(text, actionLabel = undoLabel, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) vm.undo(event.token)
        }
    }

    val mailboxName = mailboxName(state)
    val listKind = when {
        state.messages.isEmpty() && state.refreshing -> ListKind.Loading
        state.messages.isEmpty() -> ListKind.Empty
        else -> ListKind.Messages
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier,
        gesturesEnabled = !selecting,
        drawerContent = {
            HomeDrawer(
                state = state,
                onSelect = {
                    vm.select(it)
                    scope.launch { drawerState.close() }
                },
                onDrafts = { scope.launch { drawerState.close() }; onOpenDrafts() },
                onSettings = { scope.launch { drawerState.close() }; onOpenSettings() },
                onAddAccount = { scope.launch { drawerState.close() }; onAddAccount() },
            )
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = !selecting,
                    enter = scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = scaleOut(tween(150)) + fadeOut(tween(150)),
                ) {
                    ExtendedFloatingActionButton(
                        onClick = onCompose,
                        expanded = fabExpanded,
                        icon = { Icon(AppIcons.Pencil, contentDescription = null) },
                        text = { Text(stringResource(R.string.home_compose)) },
                    )
                }
            },
            topBar = {
                Crossfade(targetState = selecting, animationSpec = tween(180), label = "top-bar") { isSelecting ->
                    if (isSelecting) {
                        SelectionBar(
                            count = state.selectedIds.size,
                            onCancel = vm::clearSelection,
                            onArchive = vm::archive,
                            onDelete = vm::delete,
                            onMarkRead = vm::markRead,
                            onStar = vm::setStarred,
                            onSelectAll = vm::selectAll,
                            onMove = { showMove = true },
                        )
                    } else {
                        Column(Modifier.statusBarsPadding()) {
                            SearchPill(
                                query = state.query,
                                placeholder = stringResource(R.string.home_search_in, mailboxName),
                                options = state.options,
                                onQuery = vm::onQuery,
                                onMenu = { scope.launch { drawerState.open() } },
                                onSort = vm::setSort,
                                onRefresh = { vm.refresh(manual = true) },
                                onSelectAll = vm::selectAll,
                                onAddAccount = onAddAccount,
                            )
                            FilterChips(state.options.filter, vm::setFilter)
                        }
                    }
                }
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                // Every sync, automatic or not, shows this thin bar. It fades in and out and never moves the list.
                Box(Modifier.fillMaxWidth().height(4.dp)) {
                    AnimatedVisibility(
                        visible = state.refreshing,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(300)),
                    ) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
                state.error?.let { error ->
                    Banner(stringResource(R.string.home_sync_error, stringResource(error.messageRes())), null)
                }
                state.notice?.let { notice ->
                    actionNoticeText(notice)?.let { Banner(it, vm::dismissNotice) }
                }
                PullToRefreshBox(
                    isRefreshing = state.manualRefresh,
                    onRefresh = { vm.refresh(manual = true) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    Crossfade(targetState = listKind, animationSpec = tween(220), label = "list-kind") { kind ->
                        when (kind) {
                            ListKind.Loading -> SkeletonList()
                            ListKind.Empty -> EmptyMailbox(state)
                            ListKind.Messages -> {
                                val entries = remember(state.messages, state.options.sort) {
                                    buildEntries(state.messages, state.options.sort, System.currentTimeMillis())
                                }
                                LazyColumn(
                                    Modifier.fillMaxSize(),
                                    state = listState,
                                    contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                                ) {
                                    items(
                                        entries,
                                        key = { if (it is ListEntry.Header) "h-${it.bucket.name}" else (it as ListEntry.Row).item.id },
                                    ) { entry ->
                                        val motion = Modifier.animateItem(
                                            fadeInSpec = tween(240),
                                            placementSpec = spring(
                                                stiffness = Spring.StiffnessMediumLow,
                                                visibilityThreshold = IntOffset.VisibilityThreshold,
                                            ),
                                            fadeOutSpec = tween(160),
                                        )
                                        when (entry) {
                                            is ListEntry.Header -> BucketHeader(entry.bucket, motion)
                                            is ListEntry.Row -> {
                                                val item = entry.item
                                                SwipeableMessageRow(
                                                    modifier = motion,
                                                    item = item,
                                                    selected = item.id in state.selectedIds,
                                                    selecting = selecting,
                                                    showAccountDot = state.sidebar.size > 1,
                                                    onClick = { if (selecting) vm.toggleSelect(item.id) else onOpenMessage(item.id) },
                                                    onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); vm.toggleSelect(item.id) },
                                                    onToggleStar = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); vm.toggleStar(item.id, !item.isStarred) },
                                                    onArchive = { vm.archiveOne(item.id) },
                                                    onDelete = { vm.deleteOne(item.id) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun mailboxName(state: HomeUiState): String = when (val sel = state.selection) {
    MailboxSelection.Unified -> stringResource(R.string.home_all_inboxes)
    MailboxSelection.Starred -> stringResource(R.string.sidebar_starred)
    MailboxSelection.Unread -> stringResource(R.string.sidebar_unread)
    is MailboxSelection.Folder ->
        state.sidebar.flatMap { it.folders }.firstOrNull { it.id == sel.id }?.let { folderLabel(it) }
            ?: stringResource(R.string.role_inbox)
}

@Composable
private fun SearchPill(
    query: String,
    placeholder: String,
    options: ListOptions,
    onQuery: (String) -> Unit,
    onMenu: () -> Unit,
    onSort: (SortOrder) -> Unit,
    onRefresh: () -> Unit,
    onSelectAll: () -> Unit,
    onAddAccount: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(56.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenu) {
                Icon(AppIcons.Menu, contentDescription = stringResource(R.string.home_open_menu))
            }
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        inner()
                    }
                },
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }) {
                    Icon(AppIcons.Close, contentDescription = stringResource(R.string.home_clear_search))
                }
            } else {
                Icon(AppIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(AppIcons.More, contentDescription = stringResource(R.string.home_more_options))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        SortOrder.entries.forEach { sort ->
                            val label = when (sort) {
                                SortOrder.NEWEST -> R.string.sort_newest
                                SortOrder.OLDEST -> R.string.sort_oldest
                                SortOrder.UNREAD_FIRST -> R.string.sort_unread_first
                                SortOrder.READ_FIRST -> R.string.sort_read_first
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(label)) },
                                trailingIcon = { if (options.sort == sort) Icon(AppIcons.Check, contentDescription = null) },
                                onClick = { onSort(sort); menuOpen = false },
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_refresh)) },
                            onClick = { menuOpen = false; onRefresh() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_select_all)) },
                            onClick = { menuOpen = false; onSelectAll() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_add_account)) },
                            onClick = { menuOpen = false; onAddAccount() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChips(active: ListFilter, onFilter: (ListFilter) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            ListFilter.UNREAD to R.string.sidebar_unread,
            ListFilter.STARRED to R.string.sidebar_starred,
            ListFilter.ATTACHMENTS to R.string.chip_attachments,
        ).forEach { (filter, label) ->
            val selected = active == filter
            FilterChip(
                selected = selected,
                onClick = { onFilter(if (selected) ListFilter.ALL else filter) },
                label = { Text(stringResource(label)) },
                leadingIcon = if (selected) {
                    { Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else {
                    null
                },
            )
        }
    }
}

/** A quiet date label above each group of messages. */
@Composable
private fun BucketHeader(bucket: DateBucket, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(
            when (bucket) {
                DateBucket.TODAY -> R.string.bucket_today
                DateBucket.YESTERDAY -> R.string.bucket_yesterday
                DateBucket.LAST_7_DAYS -> R.string.bucket_week
                DateBucket.EARLIER -> R.string.bucket_earlier
            },
        ),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 24.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
private fun EmptyMailbox(state: HomeUiState) {
    val filtering = state.query.isNotBlank() || state.options.filter != ListFilter.ALL
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = shown,
            enter = fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 10 },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (filtering) AppIcons.Search else AppIcons.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(if (filtering) R.string.empty_nomatch_title else R.string.empty_caught_up_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(if (filtering) R.string.empty_nomatch_text else R.string.empty_caught_up_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
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
                    Icon(AppIcons.Close, contentDescription = stringResource(R.string.compose_discard_keep))
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
    onMove: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(stringResource(R.string.home_selected, count)) },
        navigationIcon = {
            IconButton(onClick = onCancel) {
                Icon(AppIcons.Close, contentDescription = stringResource(R.string.home_cancel_selection))
            }
        },
        actions = {
            IconButton(onClick = onArchive) {
                Icon(AppIcons.Archive, contentDescription = stringResource(R.string.home_archive))
            }
            IconButton(onClick = onDelete) {
                Icon(AppIcons.Trash, contentDescription = stringResource(R.string.home_delete))
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(AppIcons.More, contentDescription = stringResource(R.string.home_more_options))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.home_mark_read)) }, onClick = { menu = false; onMarkRead(true) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.home_mark_unread)) }, onClick = { menu = false; onMarkRead(false) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.home_star)) }, onClick = { menu = false; onStar(true) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.home_unstar)) }, onClick = { menu = false; onStar(false) })
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move_to)) },
                        leadingIcon = { Icon(AppIcons.Folder, contentDescription = null) },
                        onClick = { menu = false; onMove() },
                    )
                    DropdownMenuItem(text = { Text(stringResource(R.string.home_select_all)) }, onClick = { menu = false; onSelectAll() })
                }
            }
        },
    )
}

@Composable
private fun MoveDialog(state: HomeUiState, onPick: (FolderEntity) -> Unit, onDismiss: () -> Unit) {
    val accounts = state.messages.filter { it.id in state.selectedIds }.map { it.accountId }.distinct()
    val folders = if (accounts.size == 1) {
        state.sidebar.firstOrNull { it.account.id == accounts[0] }?.folders.orEmpty()
    } else {
        emptyList()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.move_title)) },
        text = {
            if (folders.isEmpty()) {
                Text(stringResource(R.string.move_one_account))
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    folders.forEach { folder ->
                        TextButton(onClick = { onPick(folder) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(folderIcon(folder.role), contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.size(12.dp))
                            Text(folderLabel(folder), modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.reader_link_cancel)) } },
    )
}
