package com.kmail.thekami.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmail.thekami.data.db.FolderDao
import com.kmail.thekami.data.db.MessageDao
import com.kmail.thekami.data.repo.MessageActionResult
import com.kmail.thekami.data.repo.MessageActionsRepository
import com.kmail.thekami.data.repo.MessageBodyCache
import com.kmail.thekami.domain.mail.MailError
import com.kmail.thekami.domain.mail.MailProvider
import com.kmail.thekami.domain.mail.MailResult
import com.kmail.thekami.domain.mail.ProviderSource
import com.kmail.thekami.domain.model.FolderRole
import com.kmail.thekami.domain.model.RemoteFolder
import com.kmail.thekami.mail.HtmlSanitizer
import com.kmail.thekami.mail.MimeParser
import com.kmail.thekami.mail.ParsedMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReaderUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val error: MailError? = null,
    val message: ParsedMessage? = null,
    val html: String = "",
    val blockedRemote: Int = 0,
    val allowRemote: Boolean = false,
    val starred: Boolean = false,
    /** Set after archive or delete succeeded: the screen should close. */
    val finished: Boolean = false,
    val notice: MessageActionResult? = null,
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageDao: MessageDao,
    private val folderDao: FolderDao,
    private val providers: ProviderSource,
    private val cache: MessageBodyCache,
    private val actions: MessageActionsRepository,
) : ViewModel() {

    val messageId: Long = savedStateHandle["messageId"] ?: -1L
    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()
    private var parsed: ParsedMessage? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = ReaderUiState()
            val entity = messageDao.getById(messageId)
            val folder = entity?.let { folderDao.getById(it.folderId) }
            val provider = entity?.let { providers.forAccount(it.accountId) }
            if (entity == null || folder == null || provider == null) {
                _state.value = ReaderUiState(loading = false, missing = true)
                return@launch
            }
            _state.update { it.copy(starred = entity.isStarred) }
            val remote = RemoteFolder(folder.remoteName, folder.displayName, folder.role)
            val raw = cache.get(entity.id) ?: when (val r = provider.fetchRawMessage(remote, entity.uidValidity, entity.uid)) {
                is MailResult.Failure -> {
                    _state.update { it.copy(loading = false, error = r.error) }
                    return@launch
                }
                is MailResult.Success -> r.value.also { cache.put(entity.id, it) }
            }
            val message = try {
                withContext(Dispatchers.Default) { MimeParser.parse(raw) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = MailError.Unknown("parse_failed")) }
                return@launch
            }
            parsed = message
            render(message, allowRemote = false)

            if (entity.snippet.isBlank()) messageDao.updateSnippet(entity.id, message.previewText())
            if (!entity.isRead) markRead(provider, remote, entity.id, entity.uidValidity, entity.uid, entity.isStarred)
        }
    }

    fun setAllowRemote(allow: Boolean) {
        val message = parsed ?: return
        viewModelScope.launch { render(message, allow) }
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

    private suspend fun render(message: ParsedMessage, allowRemote: Boolean) {
        val sanitized = withContext(Dispatchers.Default) { HtmlSanitizer.render(message, allowRemote) }
        _state.update {
            it.copy(
                loading = false, missing = false, error = null, message = message,
                html = sanitized.html, blockedRemote = sanitized.blockedRemoteCount, allowRemote = allowRemote,
            )
        }
    }

    private suspend fun markRead(
        provider: MailProvider,
        folder: RemoteFolder,
        id: Long,
        uidValidity: Long,
        uid: Long,
        starred: Boolean,
    ) {
        // The local flag changes only after the server accepted it.
        val result = provider.updateFlags(folder, uidValidity, listOf(uid), isRead = true, isStarred = null)
        if (result is MailResult.Success) messageDao.updateFlags(id, true, starred)
    }
}
