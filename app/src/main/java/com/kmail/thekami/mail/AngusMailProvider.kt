package com.kmail.thekami.mail

import android.os.Build
import com.kmail.thekami.data.security.CredentialStore
import com.kmail.thekami.data.security.SecretUnavailableException
import com.kmail.thekami.domain.mail.ConnectionTester
import com.kmail.thekami.domain.mail.MailError
import com.kmail.thekami.domain.mail.MailProvider
import com.kmail.thekami.domain.mail.MailResult
import com.kmail.thekami.domain.mail.ProviderCapabilities
import com.kmail.thekami.domain.model.AccountSettings
import com.kmail.thekami.domain.model.CredentialPurpose
import com.kmail.thekami.domain.model.FolderRole
import com.kmail.thekami.domain.model.FolderSnapshot
import com.kmail.thekami.domain.model.MAX_IMAP_UINT32
import com.kmail.thekami.domain.model.RemoteFolder
import jakarta.mail.Address
import jakarta.mail.FetchProfile
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.UIDFolder
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.angus.mail.imap.IMAPFolder
import org.eclipse.angus.mail.imap.IMAPStore

/**
 * Standard IMAP/SMTP provider. Every operation opens its own connection and closes it again;
 * passwords are loaded from the credential store per operation and zeroed afterwards.
 * No credentials or message content are logged.
 */
class AngusMailProvider(
    private val accountId: String,
    private val settings: AccountSettings,
    private val credentials: CredentialStore,
    private val tester: ConnectionTester,
) : MailProvider {

    override val capabilities = ProviderCapabilities(supportsPush = false, supportsCategories = false)

    private class Failed(val error: MailError) : Exception()

    // ---- connection helpers ----

    private suspend fun <T> withPassword(
        purpose: CredentialPurpose,
        block: suspend (CharArray) -> MailResult<T>,
    ): MailResult<T> {
        val password = try {
            credentials.load(accountId, purpose)
        } catch (e: SecretUnavailableException) {
            null
        } ?: return MailResult.Failure(MailError.Authentication("credentials_unavailable"))
        return try {
            block(password)
        } finally {
            password.fill('\u0000')
        }
    }

    private suspend fun <T> withStore(block: (Store) -> T): MailResult<T> =
        withPassword(CredentialPurpose.IMAP) { password ->
            withContext<MailResult<T>>(Dispatchers.IO) {
                try {
                    val props = AngusProperties.imap(settings.imap, Build.VERSION.SDK_INT)
                    // Reading a message must not mark it as read.
                    props.setProperty("mail.imap.peek", "true")
                    val store = Session.getInstance(props).getStore("imap")
                    try {
                        store.connect(
                            settings.imap.host,
                            settings.imap.port,
                            settings.imap.username,
                            String(password),
                        )
                        MailResult.Success(block(store))
                    } finally {
                        try {
                            store.close()
                        } catch (ignored: MessagingException) {
                            // nothing useful to do if closing fails
                        }
                    }
                } catch (e: Failed) {
                    MailResult.Failure(e.error)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    MailResult.Failure(e.toMailError())
                }
            }
        }

    private fun <T> Store.withFolder(name: String, mode: Int, block: (Folder, UIDFolder) -> T): T {
        val folder = getFolder(name)
        if (!folder.exists()) throw Failed(MailError.Protocol("folder_not_found"))
        val uidFolder = folder as? UIDFolder ?: throw Failed(MailError.Unsupported("no_uid_support"))
        folder.open(mode)
        try {
            return block(folder, uidFolder)
        } finally {
            try {
                folder.close(false)
            } catch (ignored: MessagingException) {
                // nothing useful to do if closing fails
            }
        }
    }

    private fun checkValidity(folder: UIDFolder, expected: Long) {
        if (folder.uidValidity != expected) throw Failed(MailError.Protocol("uidvalidity_changed"))
    }

    // ---- MailProvider ----

    override suspend fun testConnection(): MailResult<Unit> {
        val imap = withPassword(CredentialPurpose.IMAP) { tester.testImap(settings.imap, it) }
        if (imap is MailResult.Failure) return imap
        return withPassword(CredentialPurpose.SMTP) { tester.testSmtp(settings.smtp, it) }
    }

    override suspend fun listFolders(): MailResult<List<RemoteFolder>> = withStore { store ->
        val found = store.defaultFolder.list("*").mapNotNull { f ->
            if ((f.type and Folder.HOLDS_MESSAGES) == 0) return@mapNotNull null
            val attributes = (f as? IMAPFolder)?.attributes?.toList().orEmpty()
            if (attributes.any { it.equals("\\Noselect", ignoreCase = true) }) return@mapNotNull null
            RemoteFolder(
                remoteName = f.fullName,
                displayName = f.name,
                role = FolderRoles.detect(f.fullName, f.separator, attributes),
            )
        }
        if (found.any { it.role == FolderRole.INBOX }) {
            found
        } else {
            listOf(RemoteFolder("INBOX", "INBOX", FolderRole.INBOX)) + found
        }
    }

    override suspend fun fetchHeaders(
        folder: RemoteFolder,
        afterUid: Long?,
        limit: Int,
    ): MailResult<FolderSnapshot> {
        require(limit > 0) { "limit must be positive" }
        return withStore { store ->
            store.withFolder(folder.remoteName, Folder.READ_ONLY) { f, uf ->
                val validity = uf.uidValidity
                if (validity !in 1..MAX_IMAP_UINT32) throw Failed(MailError.Unsupported("no_uidvalidity"))
                val uidNext = uf.uidNext.takeIf { it in 1..MAX_IMAP_UINT32 }

                val messages: Array<Message> = if (afterUid == null) {
                    val count = f.messageCount
                    if (count == 0) emptyArray() else f.getMessages(maxOf(1, count - limit + 1), count)
                } else {
                    uf.getMessagesByUID(afterUid + 1, UIDFolder.LASTUID)
                }

                val profile = FetchProfile().apply {
                    add(FetchProfile.Item.ENVELOPE)
                    add(FetchProfile.Item.FLAGS)
                    add(FetchProfile.Item.CONTENT_INFO)
                    add(UIDFolder.FetchProfileItem.UID)
                }
                f.fetch(messages, profile)

                val headers = messages.mapNotNull { m ->
                    val uid = uf.getUID(m)
                    // "N:*" always includes the last message, even if its UID is below N.
                    if (uid !in 1..MAX_IMAP_UINT32 || (afterUid != null && uid <= afterUid)) {
                        null
                    } else {
                        (m as? MimeMessage)?.let { MessageMapper.toHeader(uid, it) }
                    }
                }.sortedBy { it.uid }

                FolderSnapshot(
                    uidValidity = validity,
                    uidNext = uidNext,
                    headers = if (afterUid != null) headers.take(limit) else headers,
                )
            }
        }
    }

    override suspend fun fetchRawMessage(
        folder: RemoteFolder,
        uidValidity: Long,
        uid: Long,
    ): MailResult<ByteArray> = withStore { store ->
        store.withFolder(folder.remoteName, Folder.READ_ONLY) { _, uf ->
            checkValidity(uf, uidValidity)
            val message = uf.getMessageByUID(uid) ?: throw Failed(MailError.Protocol("message_not_found"))
            if (message.size > MAX_RAW_BYTES) throw Failed(MailError.Unsupported("message_too_large"))
            val out = ByteArrayOutputStream()
            message.writeTo(out)
            out.toByteArray()
        }
    }

    override suspend fun updateFlags(
        folder: RemoteFolder,
        uidValidity: Long,
        uids: List<Long>,
        isRead: Boolean?,
        isStarred: Boolean?,
    ): MailResult<Unit> {
        if (uids.isEmpty()) return MailResult.Success(Unit)
        return withStore { store ->
            store.withFolder(folder.remoteName, Folder.READ_WRITE) { f, uf ->
                checkValidity(uf, uidValidity)
                val messages = uf.getMessagesByUID(uids.toLongArray()).filterNotNull().toTypedArray()
                if (messages.isNotEmpty()) {
                    isRead?.let { f.setFlags(messages, Flags(Flags.Flag.SEEN), it) }
                    isStarred?.let { f.setFlags(messages, Flags(Flags.Flag.FLAGGED), it) }
                }
            }
        }
    }

    override suspend fun moveMessages(
        folder: RemoteFolder,
        uidValidity: Long,
        uids: List<Long>,
        destination: RemoteFolder,
    ): MailResult<Unit> {
        if (uids.isEmpty()) return MailResult.Success(Unit)
        return withStore { store ->
            store.withFolder(folder.remoteName, Folder.READ_WRITE) { f, uf ->
                checkValidity(uf, uidValidity)
                val messages = uf.getMessagesByUID(uids.toLongArray()).filterNotNull().toTypedArray()
                if (messages.isNotEmpty()) {
                    val target = store.getFolder(destination.remoteName)
                    if (!target.exists()) throw Failed(MailError.Protocol("folder_not_found"))
                    val source = f as IMAPFolder
                    if ((store as IMAPStore).hasCapability("MOVE")) {
                        source.moveMessages(messages, target)
                    } else {
                        source.copyMessages(messages, target)
                        source.setFlags(messages, Flags(Flags.Flag.DELETED), true)
                        source.expunge(messages)
                    }
                }
            }
        }
    }

    override suspend fun send(
        rawMime: ByteArray,
        envelopeFrom: String,
        recipients: List<String>,
    ): MailResult<Unit> {
        if (recipients.isEmpty()) return MailResult.Failure(MailError.Protocol("no_recipients"))
        return withPassword(CredentialPurpose.SMTP) { password ->
            withContext<MailResult<Unit>>(Dispatchers.IO) {
                try {
                    val props = AngusProperties.smtp(settings.smtp, Build.VERSION.SDK_INT)
                    props.setProperty("mail.smtp.from", envelopeFrom)
                    val session = Session.getInstance(props)
                    val transport = session.getTransport("smtp")
                    try {
                        transport.connect(
                            settings.smtp.host,
                            settings.smtp.port,
                            settings.smtp.username,
                            String(password),
                        )
                        val message = MimeMessage(session, ByteArrayInputStream(rawMime))
                        val to = recipients.map<String, Address> { InternetAddress(it) }.toTypedArray()
                        transport.sendMessage(message, to)
                        MailResult.Success(Unit)
                    } finally {
                        try {
                            transport.close()
                        } catch (ignored: MessagingException) {
                            // nothing useful to do if closing fails
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    MailResult.Failure(e.toMailError())
                }
            }
        }
    }

    override suspend fun appendMessage(
        folder: RemoteFolder,
        rawMime: ByteArray,
        isRead: Boolean,
    ): MailResult<Unit> = withStore { store ->
        val target = store.getFolder(folder.remoteName)
        if (!target.exists()) throw Failed(MailError.Protocol("folder_not_found"))
        val message = MimeMessage(Session.getInstance(java.util.Properties()), ByteArrayInputStream(rawMime))
        if (isRead) message.setFlag(Flags.Flag.SEEN, true)
        target.appendMessages(arrayOf<Message>(message))
    }

    private companion object {
        const val MAX_RAW_BYTES = 25L * 1024 * 1024
    }
}
