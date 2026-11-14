package com.kmail.thekami.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmail.thekami.data.db.FolderDao
import com.kmail.thekami.data.db.FolderEntity
import com.kmail.thekami.data.db.MessageDao
import com.kmail.thekami.data.db.MessageEntity
import com.kmail.thekami.data.repo.AccountRepository
import com.kmail.thekami.data.repo.MessageActionResult
import com.kmail.thekami.data.repo.MessageActionsRepository
import com.kmail.thekami.data.repo.SyncRepository
import com.kmail.thekami.domain.mail.MailError
import com.kmail.thekami.domain.model.Account
import com.kmail.thekami.domain.model.FolderRole
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface MailboxSelection {
    data object Unified : MailboxSelection
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
)

data class SidebarAccount(val account: Account, val folders: List<FolderEntity>)

data class HomeUiState(
    val selection: MailboxSelection = MailboxSelection.Unified,
    val sidebar: List<SidebarAccount> = emptyList(),
    val messages: List<MessageItem> = emptyList(),
    val query: String = "",
    val refreshing: Boolean = false,
    val error: MailError? = null,
    val selectedIds: Set<Long> = emptySet(),
    val notice: MessageActionResult? = null,
)

private data class Extras(val selected: Set<Long> = emptySet(), val notice: MessageActionResult? = null)

private data class Chrome(
    val selection: MailboxSelection,
    val query: String,
    val refreshing: Boolean,
    val error: MailError?,
    val extras: Extras,
)

private const val LIST_LIMIT = 500

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
) : ViewModel() {

    private val selection = MutableStateFlow<MailboxSelection>(MailboxSelection.Unified)
    private val query = MutableStateFlow("")
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<MailError?>(null)
    private val extras = MutableStateFlow(Extras())

    private val messages = selection.flatMapLatest { sel ->
        when (sel) {
            MailboxSelection.Unified -> messageDao.observeUnifiedInbox(LIST_LIMIT)
            is MailboxSelection.Folder -> messageDao.observeInFolder(sel.id, LIST_LIMIT)
        }
    }

    private val data = combine(accountRepository.observeAccounts(), folderDao.observeAll(), messages) { a, f, m ->
        Triple(a, f, m)
    }
    private val chrome = combine(selection, query, refreshing, error, extras) { s, q, r, e, x ->
        Chrome(s, q, r, e, x)
    }

    val uiState: StateFlow<HomeUiState> = combine(data, chrome) { (accounts, folders, msgs), c ->
        val byAccount = folders.groupBy { it.accountId }
        val sidebar = accounts.map { account ->
            SidebarAccount(
                account,
                (byAccount[account.id] ?: emptyList())
                    .sortedWith(compareBy({ roleOrder(it.role) }, { it.displayName.lowercase() })),
            )
        }
        val q = c.query.trim().lowercase()
        val items = msgs.filter { it.matches(q) }.map { it.toItem() }
        HomeUiState(
            selection = c.selection,
            sidebar = sidebar,
            messages = items,
            query = c.query,
            refreshing = c.refreshing,
            error = c.error,
            // Messages that disappeared (moved, deleted) cannot stay selected.
            selectedIds = c.extras.selected intersect items.map { it.id }.toSet(),
            notice = c.extras.notice,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refresh()
    }

    fun onQuery(value: String) {
        query.value = value
    }

    fun select(sel: MailboxSelection) {
        selection.value = sel
        extras.value = Extras()
        if (sel is MailboxSelection.Folder) {
            viewModelScope.launch {
                refreshing.value = true
                error.value = sync.syncFolder(sel.id)
                refreshing.value = false
            }
        }
    }

    fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            error.value = null
            try {
                val accounts = accountRepository.observeAccounts().first()
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
            }
        }
    }

    // ---- selection mode ----

    fun toggleSelect(id: Long) = extras.update {
        it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id, notice = null)
    }

    fun clearSelection() = extras.update { it.copy(selected = emptySet()) }

    fun selectAll() = extras.update { it.copy(selected = uiState.value.messages.map { m -> m.id }.toSet()) }

    fun dismissNotice() = extras.update { it.copy(notice = null) }

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
    fun archive() = act { actions.moveToRole(it, FolderRole.ARCHIVE) }
    fun delete() = act { actions.moveToRole(it, FolderRole.TRASH) }

    private fun MessageEntity.matches(q: String) = q.isEmpty() ||
        subject.lowercase().contains(q) ||
        fromAddress.lowercase().contains(q) ||
        (fromName?.lowercase()?.contains(q) == true) ||
        snippet.lowercase().contains(q)

    private fun MessageEntity.toItem() = MessageItem(
        id = id,
        sender = fromName?.takeIf { it.isNotBlank() } ?: fromAddress,
        subject = subject,
        snippet = snippet,
        receivedAtMillis = receivedAtMillis,
        isRead = isRead,
        isStarred = isStarred,
        hasAttachments = hasAttachments,
    )
}
