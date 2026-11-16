package com.kmail.thekami.ui.reader

import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmail.thekami.data.db.FolderDao
import com.kmail.thekami.data.db.FolderEntity
import com.kmail.thekami.data.db.MessageDao
import com.kmail.thekami.data.prefs.ImagePrefs
import com.kmail.thekami.data.repo.AccountRepository
import com.kmail.thekami.data.repo.BodyStore
import com.kmail.thekami.data.repo.MessageActionResult
import com.kmail.thekami.data.repo.MessageActionsRepository
import com.kmail.thekami.data.repo.MessageBodyCache
import com.kmail.thekami.domain.mail.MailError
import com.kmail.thekami.domain.mail.MailResult
import com.kmail.thekami.domain.mail.ProviderSource
import com.kmail.thekami.domain.model.FolderRole
import com.kmail.thekami.domain.model.RemoteFolder
import com.kmail.thekami.mail.HtmlSanitizer
import com.kmail.thekami.mail.MimeParser
import com.kmail.thekami.mail.ParsedMessage
import com.kmail.thekami.ui.theme.AppearanceStore
import com.kmail.thekami.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LoadedBody(
    val message: ParsedMessage,
    val html: String,
    val blockedRemote: Int,
    val allowRemote: Boolean,
)

data class ReaderUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    /** Error of the opened message only. Other thread messages report through [failedBodies]. */
    val error: MailError? = null,
    val subject: String = "",
    val thread: List<ThreadItem> = emptyList(),
    val openedId: Long = -1,
    val openedFolder: FolderEntity? = null,
    val expanded: Set<Long> = emptySet(),
    val bodies: Map<Long, LoadedBody> = emptyMap(),
    val loadingBodies: Set<Long> = emptySet(),
    val failedBodies: Map<Long, MailError> = emptyMap(),
    val ownEmail: String = "",
    val darkView: Boolean = false,
    val starred: Boolean = false,
    /** Set after archive, delete or move succeeded: the screen should close. */
    val finished: Boolean = false,
    val notice: MessageActionResult? = null,
    val folders: List<FolderEntity> = emptyList(),
)

private sealed interface BodyLoad {
    data object Ok : BodyLoad
    data class Failed(val error: MailError) : BodyLoad
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val messageDao: MessageDao,
    private val folderDao: FolderDao,
    private val providers: ProviderSource,
    private val cache: MessageBodyCache,
    private val bodies: BodyStore,
    private val actions: MessageActionsRepository,
    private val appearanceStore: AppearanceStore,
    private val accountRepository: AccountRepository,
    private val imagePrefs: ImagePrefs,
) : ViewModel() {

    val messageId: Long = savedStateHandle["messageId"] ?: -1L

    /** Starts dark when the app is dark, so the email does not flash white first. */
    private var darkView = isDarkNow()
    private val parsed = mutableMapOf<Long, ParsedMessage>()

    private val _state = MutableStateFlow(ReaderUiState(darkView = darkView))
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun isDarkNow(): Boolean = when (appearanceStore.state.value.mode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System ->
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    fun load() {
        viewModelScope.launch {
            parsed.clear()
            _state.value = ReaderUiState(darkView = darkView)
            val entity = messageDao.getById(messageId)
            val folder = entity?.let { folderDao.getById(it.folderId) }
            if (entity == null || folder == null) {
                _state.update { it.copy(loading = false, missing = true) }
                return@launch
            }
            val own = accountRepository.getAccount(entity.accountId)?.emailAddress.orEmpty()
            val candidates = ThreadSubject.normalize(entity.subject).takeIf { it.isNotEmpty() }
                ?.let { messageDao.threadCandidates(entity.accountId, it) }.orEmpty()
            _state.update {
                it.copy(
                    subject = entity.subject,
                    thread = ThreadBuilder.build(entity, candidates, own),
                    openedId = entity.id,
                    openedFolder = folder,
                    expanded = setOf(entity.id),
                    loadingBodies = setOf(entity.id),
                    starred = entity.isStarred,
                    ownEmail = own,
                    folders = folderDao.listByAccount(entity.accountId),
                )
            }
            when (val r = loadBody(entity.id)) {
                is BodyLoad.Failed -> {
                    _state.update { it.copy(loading = false, error = r.error, loadingBodies = it.loadingBodies - entity.id) }
                    return@launch
                }
                BodyLoad.Ok -> _state.update { it.copy(loading = false) }
            }
            parsed[entity.id]?.let { message ->
                if (entity.snippet.isBlank()) messageDao.updateSnippet(entity.id, message.previewText())
            }
            if (!entity.isRead) actions.setRead(listOf(entity.id), true)
        }
    }

    private suspend fun loadBody(id: Long): BodyLoad {
        val entity = messageDao.getById(id) ?: return BodyLoad.Failed(MailError.Unknown("missing"))
        val folder = folderDao.getById(entity.folderId) ?: return BodyLoad.Failed(MailError.Unknown("missing"))
        val provider = providers.forAccount(entity.accountId) ?: return BodyLoad.Failed(MailError.Unknown("missing"))
        val remote = RemoteFolder(folder.remoteName, folder.displayName, folder.role)
        val raw = cache.get(id) ?: bodies.get(id)?.also { cache.put(id, it) }
            ?: when (val r = provider.fetchRawMessage(remote, entity.uidValidity, entity.uid)) {
                is MailResult.Failure -> return BodyLoad.Failed(r.error)
                is MailResult.Success -> r.value.also {
                    cache.put(id, it)
                    bodies.put(id, it)
                }
            }
        val message = try {
            withContext(Dispatchers.Default) { MimeParser.parse(raw) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return BodyLoad.Failed(MailError.Unknown("parse_failed"))
        }
        parsed[id] = message
        renderBody(id, message, allowRemote = imagePrefs.allowFor(message.from?.address))
        return BodyLoad.Ok
    }

    private suspend fun renderBody(id: Long, message: ParsedMessage, allowRemote: Boolean) {
        val dark = darkView
        val sanitized = withContext(Dispatchers.Default) { HtmlSanitizer.render(message, allowRemote, dark) }
        _state.update {
            it.copy(
                bodies = it.bodies + (id to LoadedBody(message, sanitized.html, sanitized.blockedRemoteCount, allowRemote)),
                loadingBodies = it.loadingBodies - id,
                failedBodies = it.failedBodies - id,
                darkView = dark,
            )
        }
    }

    /** Opens or closes a message of the thread. Opening loads its body and marks it read. */
    fun toggle(id: Long) {
        val s = _state.value
        if (id in s.expanded) {
            _state.update { it.copy(expanded = it.expanded - id) }
            return
        }
        _state.update { it.copy(expanded = it.expanded + id) }
        if (id !in s.bodies) retryBody(id)
    }

    fun retryBody(id: Long) {
        _state.update { it.copy(loadingBodies = it.loadingBodies + id, failedBodies = it.failedBodies - id) }
        viewModelScope.launch {
            when (val r = loadBody(id)) {
                is BodyLoad.Failed -> _state.update {
                    it.copy(loadingBodies = it.loadingBodies - id, failedBodies = it.failedBodies + (id to r.error))
                }
                BodyLoad.Ok -> if (_state.value.thread.firstOrNull { it.id == id }?.isRead == false) {
                    actions.setRead(listOf(id), true)
                }
            }
        }
    }

    fun setAllowRemote(id: Long, allow: Boolean) {
        val message = parsed[id] ?: return
        viewModelScope.launch { renderBody(id, message, allow) }
    }

    fun setDarkView(on: Boolean) {
        darkView = on
        viewModelScope.launch {
            parsed.forEach { (id, message) ->
                renderBody(id, message, _state.value.bodies[id]?.allowRemote ?: false)
            }
            _state.update { it.copy(darkView = on) }
        }
    }

    fun toggleStar() {
        val target = !_state.value.starred
        viewModelScope.launch {
            when (val r = actions.setStarred(listOf(messageId), target)) {
                MessageActionResult.Ok -> _state.update { it.copy(starred = target, notice = null) }
                else -> _state.update { it.copy(notice = r) }
            }
        }
    }

    fun markUnread() {
        viewModelScope.launch {
            when (val r = actions.setRead(listOf(messageId), false)) {
                MessageActionResult.Ok -> _state.update { it.copy(finished = true) }
                else -> _state.update { it.copy(notice = r) }
            }
        }
    }

    fun moveTo(target: FolderEntity) {
        viewModelScope.launch {
            when (val r = actions.moveToFolder(listOf(messageId), target)) {
                MessageActionResult.Ok -> _state.update { it.copy(finished = true) }
                else -> _state.update { it.copy(notice = r) }
            }
        }
    }

    fun archive() = move(FolderRole.ARCHIVE)
    fun delete() = move(FolderRole.TRASH)

    private fun move(role: FolderRole) {
        viewModelScope.launch {
            when (val r = actions.moveToRole(listOf(messageId), role)) {
                MessageActionResult.Ok -> _state.update { it.copy(finished = true) }
                else -> _state.update { it.copy(notice = r) }
            }
        }
    }
}
