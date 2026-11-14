package com.kmail.thekami.ui.compose

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.DateUtils
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmail.thekami.R
import com.kmail.thekami.data.db.FolderDao
import com.kmail.thekami.data.db.MessageDao
import com.kmail.thekami.data.db.MessageEntity
import com.kmail.thekami.data.repo.AccountRepository
import com.kmail.thekami.data.repo.MessageBodyCache
import com.kmail.thekami.domain.mail.MailError
import com.kmail.thekami.domain.mail.MailResult
import com.kmail.thekami.domain.mail.ProviderSource
import com.kmail.thekami.domain.model.Account
import com.kmail.thekami.domain.model.FolderRole
import com.kmail.thekami.domain.model.Recipients
import com.kmail.thekami.domain.model.RemoteFolder
import com.kmail.thekami.mail.Mailbox
import com.kmail.thekami.mail.MimeBuilder
import com.kmail.thekami.mail.MimeParser
import com.kmail.thekami.mail.ParsedMessage
import com.kmail.thekami.mail.ReplyBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ComposeAttachment(val name: String, val mimeType: String, val bytes: ByteArray)

sealed interface ComposeError {
    data object NoRecipient : ComposeError
    data class InvalidRecipient(val text: String) : ComposeError
    data object AttachmentTooLarge : ComposeError
    /** [error] is null when the account no longer exists. */
    data class SendFailed(val error: MailError?) : ComposeError
}

data class ComposeUiState(
    val accounts: List<Account> = emptyList(),
    val fromId: String? = null,
    val to: String = "",
    val cc: String = "",
    val bcc: String = "",
    val showCcBcc: Boolean = false,
    val subject: String = "",
    val body: String = "",
    val attachments: List<ComposeAttachment> = emptyList(),
    val loading: Boolean = true,
    val sending: Boolean = false,
    val dirty: Boolean = false,
    val error: ComposeError? = null,
    val sent: Boolean = false,
)

private const val MAX_ATTACHMENT_BYTES = 15L * 1024 * 1024

@HiltViewModel
class ComposeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val accountRepository: AccountRepository,
    private val messageDao: MessageDao,
    private val folderDao: FolderDao,
    private val providers: ProviderSource,
    private val cache: MessageBodyCache,
) : ViewModel() {

    val mode: String = savedStateHandle["mode"] ?: "new"
    private val sourceId: Long = savedStateHandle["messageId"] ?: -1L

    private val _state = MutableStateFlow(ComposeUiState())
    val state: StateFlow<ComposeUiState> = _state.asStateFlow()

    private var inReplyTo: String? = null
    private var references: List<String> = emptyList()

    init {
        viewModelScope.launch { prepare() }
    }

    // ---- prefill ----

    private suspend fun prepare() {
        val accounts = accountRepository.observeAccounts().first()
        var fromId = accounts.firstOrNull()?.id
        var to = ""
        var cc = ""
        var subject = ""
        var quote = ""
        var attachments = emptyList<ComposeAttachment>()

        val source = if (mode != "new") messageDao.getById(sourceId) else null
        if (source != null) {
            fromId = source.accountId
            val own = accounts.firstOrNull { it.id == fromId }?.emailAddress.orEmpty()
            val parsed = loadParsed(source)
            if (parsed != null) {
                val draft = if (mode == "forward") {
                    ReplyBuilder.forward(parsed, forwardHeader(parsed))
                } else {
                    ReplyBuilder.reply(parsed, own, mode == "replyall", quoteHeader(parsed))
                }
                to = draft.to.joinToString(", ")
                cc = draft.cc.joinToString(", ")
                subject = draft.subject
                quote = draft.body
                inReplyTo = draft.inReplyTo
                references = draft.references
                attachments = draft.attachments.map { ComposeAttachment(it.fileName, it.mimeType, it.bytes) }
            } else if (mode == "forward") {
                subject = ReplyBuilder.forwardSubject(source.subject)
            } else {
                // Offline fallback: the cached header is enough to address a reply.
                to = source.fromAddress
                subject = ReplyBuilder.replySubject(source.subject)
            }
        }

        val signature = context.getString(R.string.compose_signature)
        val body = "\n\n-- \n$signature" + if (quote.isNotBlank()) "\n\n$quote" else ""
        _state.value = ComposeUiState(
            accounts = accounts,
            fromId = fromId,
            to = to,
            cc = cc,
            showCcBcc = cc.isNotBlank(),
            subject = subject,
            body = body,
            attachments = attachments,
            loading = false,
        )
    }

    private suspend fun loadParsed(entity: MessageEntity): ParsedMessage? {
        val folder = folderDao.getById(entity.folderId) ?: return null
        val raw = cache.get(entity.id) ?: run {
            val provider = providers.forAccount(entity.accountId) ?: return null
            val remote = RemoteFolder(folder.remoteName, folder.displayName, folder.role)
            when (val r = provider.fetchRawMessage(remote, entity.uidValidity, entity.uid)) {
                is MailResult.Failure -> return null
                is MailResult.Success -> r.value.also { cache.put(entity.id, it) }
            }
        }
        return try {
            withContext(Dispatchers.Default) { MimeParser.parse(raw) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun formatDate(millis: Long?) = millis?.let {
        DateUtils.formatDateTime(
            context, it,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR,
        )
    }.orEmpty()

    private fun quoteHeader(m: ParsedMessage) = context.getString(
        R.string.compose_quote_header,
        formatDate(m.dateMillis),
        m.from?.display() ?: "",
    )

    private fun forwardHeader(m: ParsedMessage) = listOf(
        context.getString(R.string.compose_forward_header),
        context.getString(R.string.compose_forward_from, m.from?.display() ?: ""),
        context.getString(R.string.compose_forward_date, formatDate(m.dateMillis)),
        context.getString(R.string.compose_forward_subject, m.subject),
        context.getString(R.string.compose_forward_to, m.to.joinToString(", ") { it.display() }),
    ).joinToString("\n")

    // ---- editing ----

    private fun edit(transform: (ComposeUiState) -> ComposeUiState) {
        _state.update { transform(it).copy(dirty = true, error = null) }
    }

    fun onFrom(accountId: String) = edit { it.copy(fromId = accountId) }
    fun onTo(value: String) = edit { it.copy(to = value) }
    fun onCc(value: String) = edit { it.copy(cc = value) }
    fun onBcc(value: String) = edit { it.copy(bcc = value) }
    fun onSubject(value: String) = edit { it.copy(subject = value) }
    fun onBody(value: String) = edit { it.copy(body = value) }
    fun showCcBcc() = _state.update { it.copy(showCcBcc = true) }
    fun removeAttachment(attachment: ComposeAttachment) =
        edit { s -> s.copy(attachments = s.attachments.filterNot { it === attachment }) }

    fun addAttachments(uris: List<Uri>) {
        viewModelScope.launch {
            val added = mutableListOf<ComposeAttachment>()
            var total = _state.value.attachments.sumOf { it.bytes.size.toLong() }
            for (uri in uris) {
                val loaded = withContext(Dispatchers.IO) { readUri(uri, MAX_ATTACHMENT_BYTES - total) }
                if (loaded == null) {
                    _state.update { it.copy(error = ComposeError.AttachmentTooLarge) }
                    break
                }
                total += loaded.bytes.size
                added += loaded
            }
            if (added.isNotEmpty()) edit { it.copy(attachments = it.attachments + added) }
        }
    }

    /** Null if the file is bigger than [limit] bytes or cannot be read. */
    private fun readUri(uri: Uri, limit: Long): ComposeAttachment? = runCatching {
        val resolver = context.contentResolver
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "attachment"
        val type = resolver.getType(uri) ?: "application/octet-stream"
        val bytes = resolver.openInputStream(uri)?.use { input ->
            val buffer = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            while (true) {
                val n = input.read(chunk)
                if (n < 0) break
                buffer.write(chunk, 0, n)
                if (buffer.size() > limit) return@runCatching null
            }
            buffer.toByteArray()
        } ?: return@runCatching null
        ComposeAttachment(name, type, bytes)
    }.getOrNull()

    // ---- send ----

    fun send() {
        val s = _state.value
        if (s.sending || s.loading) return
        val to = Recipients.parse(s.to)
        val cc = Recipients.parse(s.cc)
        val bcc = Recipients.parse(s.bcc)
        val invalid = to.invalid + cc.invalid + bcc.invalid
        if (invalid.isNotEmpty()) {
            _state.update { it.copy(error = ComposeError.InvalidRecipient(invalid.first())) }
            return
        }
        if (to.valid.isEmpty() && cc.valid.isEmpty() && bcc.valid.isEmpty()) {
            _state.update { it.copy(error = ComposeError.NoRecipient) }
            return
        }
        val account = s.accounts.firstOrNull { it.id == s.fromId }
        if (account == null) {
            _state.update { it.copy(error = ComposeError.SendFailed(null)) }
            return
        }
        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            val provider = providers.forAccount(account.id)
            if (provider == null) {
                fail(ComposeError.SendFailed(null))
                return@launch
            }
            val name = account.displayName.takeIf { it.isNotBlank() && it != account.emailAddress }
            val raw = withContext(Dispatchers.Default) {
                MimeBuilder.build(
                    MimeBuilder.Draft(
                        from = Mailbox(name, account.emailAddress),
                        to = to.valid,
                        cc = cc.valid,
                        subject = s.subject,
                        body = s.body,
                        attachments = s.attachments.map { MimeBuilder.Attachment(it.name, it.mimeType, it.bytes) },
                        inReplyTo = inReplyTo,
                        references = references,
                    ),
                )
            }
            val recipients = (to.valid + cc.valid + bcc.valid).distinct()
            when (val r = provider.send(raw, account.emailAddress, recipients)) {
                is MailResult.Failure -> fail(ComposeError.SendFailed(r.error))
                is MailResult.Success -> {
                    // The message is already sent. Failing to copy it to Sent must not look like a failure.
                    folderDao.listByAccount(account.id).firstOrNull { it.role == FolderRole.SENT }?.let { f ->
                        provider.appendMessage(RemoteFolder(f.remoteName, f.displayName, f.role), raw, isRead = true)
                    }
                    _state.update { it.copy(sending = false, sent = true, dirty = false) }
                }
            }
        }
    }

    private fun fail(error: ComposeError) {
        _state.update { it.copy(sending = false, error = error) }
    }
}
