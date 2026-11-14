package com.kmail.thekami.domain.mail

import com.kmail.thekami.domain.model.FolderSnapshot
import com.kmail.thekami.domain.model.RemoteFolder

/**
 * Protocol boundary. IMAP/SMTP implements it first; Gmail API and Microsoft Graph can follow.
 * Implementations must never log credentials or message content.
 * Message payloads are raw RFC 822 bytes; MIME parsing lives in a separate layer.
 */
interface MailProvider {
    val capabilities: ProviderCapabilities

    suspend fun testConnection(): MailResult<Unit>

    suspend fun listFolders(): MailResult<List<RemoteFolder>>

    /**
     * Up to [limit] headers with UID greater than [afterUid], or the newest [limit]
     * when [afterUid] is null. Order is unspecified; callers sort.
     */
    suspend fun fetchHeaders(
        folder: RemoteFolder,
        afterUid: Long?,
        limit: Int,
    ): MailResult<FolderSnapshot>

    suspend fun fetchRawMessage(
        folder: RemoteFolder,
        uidValidity: Long,
        uid: Long,
    ): MailResult<ByteArray>

    /** Null flag values are left unchanged. */
    suspend fun updateFlags(
        folder: RemoteFolder,
        uidValidity: Long,
        uids: List<Long>,
        isRead: Boolean?,
        isStarred: Boolean?,
    ): MailResult<Unit>

    suspend fun moveMessages(
        folder: RemoteFolder,
        uidValidity: Long,
        uids: List<Long>,
        destination: RemoteFolder,
    ): MailResult<Unit>

    suspend fun send(
        rawMime: ByteArray,
        envelopeFrom: String,
        recipients: List<String>,
    ): MailResult<Unit>

    /** Stores a copy of a message in [folder], for example a sent message in the Sent folder. */
    suspend fun appendMessage(folder: RemoteFolder, rawMime: ByteArray, isRead: Boolean): MailResult<Unit>
}
