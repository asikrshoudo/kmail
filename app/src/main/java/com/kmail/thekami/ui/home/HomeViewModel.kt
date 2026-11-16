package com.kmail.thekami.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmail.thekami.data.db.FolderDao
import com.kmail.thekami.data.db.FolderEntity
import com.kmail.thekami.data.db.MessageDao
import com.kmail.thekami.data.db.MessageEntity
import com.kmail.thekami.data.repo.AccountRepository
import com.kmail.thekami.data.repo.BodyPrefetcher
import com.kmail.thekami.data.repo.MessageActionResult
import com.kmail.thekami.data.repo.MessageActionsRepository
import com.kmail.thekami.data.repo.SyncRepository
import com.kmail.thekami.domain.mail.MailError
import com.kmail.thekami.domain.model.Account
import com.kmail.thekami.domain.model.FolderRole
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface MailboxSelection {
    data object Unified : MailboxSelection
    data object Starred : MailboxSelection
    data object Unread : MailboxSelection
    data class Folder(val id: Long) : MailboxSelection
}

data class MessageItem(
    val id: Long,
    val sender: String,
    val subject: String,
    val snippet: String,
    val receivedAtMillis: Long,
    val isRead: Boolean,
    val isStarred: Boolean,
    val hasAttachments: Boolean,
    val accountId: String = "",
    val accountColor: Int = 0,
    val senderKey: String = "",
)

data class SidebarAccount(val account: Account, val folders: List<FolderEntity>)

enum class UndoKind { ARCHIVE, DELETE }

data class UndoEvent(val token: Long, val kind: UndoKind, val count: Int)

data class HomeUiState(
    val selection: MailboxSelection = MailboxSelection.Unified,
    val sidebar: List<SidebarAccount> = emptyList(),
    val messages: List<MessageItem> = emptyList(),
    val query: String = "",
    val refreshing: Boolean = false,
    /** True only while the user pulled to refresh. Automatic syncs show the thin progress bar instead. */
    val manualRefresh: Boolean = false,
    val error: MailError? = null,
    val selectedIds: Set<Long> = emptySet(),
    val notice: MessageActionResult? = null,
    val options: ListOptions = ListOptions(),
    val unread: Map<Long, Int> = emptyMap(),
)

private data class Extras(
    val selected: Set<Long> = emptySet(),
    val notice: MessageActionResult? = null,
    val options: ListOptions = ListOptions(),
    /** Messages the user archived or deleted, hidden at once while the undo window is open. */
    val hidden: Set<Long> = emptySet(),
    val manual: Boolean = false,
)

private data class Chrome(
    val selection: MailboxSelection,
    val query: String,
    val refreshing: Boolean,
    val error: MailError?,
    val extras: Extras,
)

private data class Data(
    val accounts: List<Account>,
    val folders: List<FolderEntity>,
    val msgs: List<MessageEntity>,
    val unread: Map<Long, Int>,
)

private const val LIST_LIMIT = 500
private const val UNDO_MS = 4_500L

private fun roleOrder(role: FolderRole) = when (role) {
    FolderRole.INBOX -> 0
    FolderRole.SENT -> 1
    FolderRole.DRAFTS -> 2
    FolderRole.ARCHIVE -> 3
    FolderRole.SPAM -> 4
    FolderRole.TRASH -> 5
    FolderRole.CUSTOM -> 6
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    folderDao: FolderDao,
    messageDao: MessageDao,
    private val sync: SyncRepository,
    private val actions: MessageActionsRepository,
    private val prefetcher: BodyPrefetcher,
) : ViewModel() {

    private val selection = MutableStateFlow<MailboxSelection>(MailboxSelection.Unified)
    private val query = MutableStateFlow("")
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<MailError?>(null)
    private val extras = MutableStateFlow(Extras())

    private val messages = selection.flatMapLatest { sel ->
        when (sel) {
            MailboxSelection.Unified -> messageDao.observeUnifiedInbox(LIST_LIMIT)
            MailboxSelection.Starred -> messageDao.observeStarred(LIST_LIMIT)
            MailboxSelection.Unread -> messageDao.observeUnreadInbox(LIST_LIMIT)
            is MailboxSelection.Folder -> messageDao.observeInFolder(sel.id, LIST_LIMIT)
        }
    }

    private val data = combine(
        accountRepository.observeAccounts(),
        folderDao.observeAll(),
        messages,
        messageDao.observeUnreadCounts(),
    ) { accounts, folders, msgs, counts ->
        Data(accounts, folders, msgs, counts.associate { it.folderId to it.count })
    }
    private val chrome = combine(selection, query, refreshing, error, extras) { s, q, r, e, x ->
        Chrome(s, q, r, e, x)
    }

    val uiState: StateFlow<HomeUiState> = combine(data, chrome) { d, c ->
        val byAccount = d.folders.groupBy { it.accountId }
        val colors = d.accounts.associate { it.id to it.colorArgb }
        val sidebar = d.accounts.map { account ->
            SidebarAccount(
                account,
                (byAccount[account.id] ?: emptyList())
                    .sortedWith(compareBy({ roleOrder(it.role) }, { it.displayName.lowercase() })),
            )
        }
        val q = c.query.trim().lowercase()
        val items = ListOptions.apply(
            d.msgs.filter { it.id !in c.extras.hidden && it.matches(q) }.map { it.toItem(colors[it.accountId] ?: 0) },
            c.extras.options,
        )
        HomeUiState(
            selection = c.selection,
            sidebar = sidebar,
            messages = items,
            query = c.query,
            refreshing = c.refreshing,
            manualRefresh = c.extras.manual,
            error = c.error,
            // Messages that disappeared (moved, deleted) cannot stay selected.
            selectedIds = c.extras.selected intersect items.map { it.id }.toSet(),
            notice = c.extras.notice,
            options = c.extras.options,
            unread = d.unread,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _undo = MutableSharedFlow<UndoEvent>(extraBufferCapacity = 8)
    val undoEvents: SharedFlow<UndoEvent> = _undo.asSharedFlow()
    private val undoJobs = mutableMapOf<Long, Job>()
    private val undoIds = mutableMapOf<Long, List<Long>>()
    private var nextToken = 0L
    private val exhausted = mutableSetOf<Long>()

    init {
        refresh()
    }

    fun onQuery(value: String) {
        query.value = value
    }

    fun select(sel: MailboxSelection) {
        selection.value = sel
        extras.update { it.copy(selected = emptySet(), notice = null) }
        if (sel is MailboxSelection.Folder) {
            viewModelScope.launch {
                refreshing.value = true
                error.value = sync.syncFolder(sel.id)
                refreshing.value = false
            }
        }
    }

    fun refresh(manual: Boolean = false) {
        if (refreshing.value) return
        viewModelScope.launch {
            extras.update { it.copy(manual = manual) }
            refreshing.value = true
            error.value = null
            var accounts = emptyList<Account>()
            try {
                accounts = accountRepository.observeAccounts().first()
                val errors = coroutineScope {
                    accounts.map { async { sync.syncAccount(it.id) } }.awaitAll()
                }
                var failure = errors.firstNotNullOfOrNull { it }
                (selection.value as? MailboxSelection.Folder)?.let {
                    failure = failure ?: sync.syncFolder(it.id)
                }
                error.value = failure
            } finally {
                refreshing.value = false
                extras.update { it.copy(manual = false) }
            }
            // Newest messages are downloaded after the list is shown, so they open instantly later.
            accounts.forEach { prefetcher.prefetchInbox(it.id) }
        }
    }

    /** Loads the next older page of the shown folder, or of every inbox in the unified view. */
    fun loadMore() {
        if (refreshing.value) return
        val ids = when (val sel = selection.value) {
            MailboxSelection.Unified, MailboxSelection.Unread -> uiState.value.sidebar.flatMap { it.folders }
                .filter { it.role == FolderRole.INBOX }.map { it.id }
            is MailboxSelection.Folder -> listOf(sel.id)
            MailboxSelection.Starred -> emptyList()
        }.filter { it !in exhausted }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            refreshing.value = true
            val results = ids.map { it to sync.loadOlder(it) }
            results.filter { it.second.noMore }.forEach { exhausted += it.first }
            error.value = results.firstNotNullOfOrNull { it.second.error }
            refreshing.value = false
        }
    }

    // ---- selection mode ----

    fun toggleSelect(id: Long) = extras.update {
        it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id, notice = null)
    }

    fun clearSelection() = extras.update { it.copy(selected = emptySet()) }

    fun selectAll() = extras.update { it.copy(selected = uiState.value.messages.map { m -> m.id }.toSet()) }

    fun dismissNotice() = extras.update { it.copy(notice = null) }

    fun setSort(sort: SortOrder) = extras.update { it.copy(options = it.options.copy(sort = sort)) }

    fun setFilter(filter: ListFilter) = extras.update { it.copy(options = it.options.copy(filter = filter)) }

    private fun act(block: suspend (List<Long>) -> MessageActionResult) {
        val ids = extras.value.selected.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val result = block(ids)
            extras.update {
                it.copy(selected = emptySet(), notice = result.takeIf { r -> r != MessageActionResult.Ok })
            }
        }
    }

    fun markRead(read: Boolean) = act { actions.setRead(it, read) }
    fun setStarred(starred: Boolean) = act { actions.setStarred(it, starred) }
    fun moveSelected(target: FolderEntity) = act { actions.moveToFolder(it, target) }

    fun toggleStar(id: Long, starred: Boolean) {
        viewModelScope.launch {
            val result = actions.setStarred(listOf(id), starred)
            if (result != MessageActionResult.Ok) extras.update { it.copy(notice = result) }
        }
    }

    // ---- archive and delete, with undo ----

    fun archive() = moveSelectedWithUndo(UndoKind.ARCHIVE)
    fun delete() = moveSelectedWithUndo(UndoKind.DELETE)
    fun archiveOne(id: Long) = moveWithUndo(listOf(id), UndoKind.ARCHIVE)
    fun deleteOne(id: Long) = moveWithUndo(listOf(id), UndoKind.DELETE)

    private fun moveSelectedWithUndo(kind: UndoKind) {
        val ids = extras.value.selected.toList()
        extras.update { it.copy(selected = emptySet()) }
        moveWithUndo(ids, kind)
    }

    /** The rows disappear at once. The server is asked after the undo window, unless the user undoes it. */
    private fun moveWithUndo(ids: List<Long>, kind: UndoKind) {
        if (ids.isEmpty()) return
        val token = ++nextToken
        extras.update { it.copy(hidden = it.hidden + ids) }
        undoIds[token] = ids
        _undo.tryEmit(UndoEvent(token, kind, ids.size))
        undoJobs[token] = viewModelScope.launch {
            delay(UNDO_MS)
            val role = if (kind == UndoKind.ARCHIVE) FolderRole.ARCHIVE else FolderRole.TRASH
            val result = actions.moveToRole(ids, role)
            undoJobs.remove(token)
            undoIds.remove(token)
            // On success the rows stay hidden (they are gone locally too). On failure they come back.
            if (result != MessageActionResult.Ok) {
                extras.update { it.copy(hidden = it.hidden - ids.toSet(), notice = result) }
            }
        }
    }

    fun undo(token: Long) {
        undoJobs.remove(token)?.cancel()
        val ids = undoIds.remove(token) ?: return
        extras.update { it.copy(hidden = it.hidden - ids.toSet()) }
    }

    private fun MessageEntity.matches(q: String) = q.isEmpty() ||
        subject.lowercase().contains(q) ||
        fromAddress.lowercase().contains(q) ||
        (fromName?.lowercase()?.contains(q) == true) ||
        snippet.lowercase().contains(q)

    private fun MessageEntity.toItem(color: Int) = MessageItem(
        id = id,
        sender = fromName?.takeIf { it.isNotBlank() } ?: fromAddress,
        subject = subject,
        snippet = snippet,
        receivedAtMillis = receivedAtMillis,
        isRead = isRead,
        isStarred = isStarred,
        hasAttachments = hasAttachments,
        accountId = accountId,
        accountColor = color,
        senderKey = fromAddress,
    )
}
