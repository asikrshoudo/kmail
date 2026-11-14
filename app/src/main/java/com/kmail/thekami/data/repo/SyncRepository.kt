package com.kmail.thekami.data.repo

import com.kmail.thekami.data.db.FolderDao
import com.kmail.thekami.data.db.FolderEntity
import com.kmail.thekami.data.db.MessageDao
import com.kmail.thekami.data.db.MessageEntity
import com.kmail.thekami.domain.mail.MailError
import com.kmail.thekami.domain.mail.MailResult
import com.kmail.thekami.domain.mail.ProviderSource
import com.kmail.thekami.domain.model.FolderRole
import com.kmail.thekami.domain.model.RemoteFolder
import com.kmail.thekami.domain.model.RemoteMessageHeader
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Header sync with a local cache. Both functions return null on success or the failure.
 * Per-account and per-folder locks stop two syncs of the same thing from racing.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val providers: ProviderSource,
    private val folderDao: FolderDao,
    private val messageDao: MessageDao,
) {
    private val accountLocks = ConcurrentHashMap<String, Mutex>()
    private val folderLocks = ConcurrentHashMap<Long, Mutex>()

    /** Updates the folder list, then syncs the inbox. */
    suspend fun syncAccount(accountId: String): MailError? =
        accountLocks.getOrPut(accountId) { Mutex() }.withLock {
            val provider = providers.forAccount(accountId) ?: return@withLock null
            val remote = when (val result = provider.listFolders()) {
                is MailResult.Failure -> return@withLock result.error
                is MailResult.Success -> result.value
            }
            for (rf in remote) {
                val existing = folderDao.find(accountId, rf.remoteName)
                if (existing == null) {
                    folderDao.insert(
                        FolderEntity(
                            accountId = accountId,
                            remoteName = rf.remoteName,
                            displayName = rf.displayName,
                            role = rf.role,
                        ),
                    )
                } else if (existing.displayName != rf.displayName || existing.role != rf.role) {
                    folderDao.update(existing.copy(displayName = rf.displayName, role = rf.role))
                }
            }
            val inbox = folderDao.listByAccount(accountId).firstOrNull { it.role == FolderRole.INBOX }
            if (inbox == null) null else syncFolder(inbox.id)
        }

    suspend fun syncFolder(folderId: Long): MailError? =
        folderLocks.getOrPut(folderId) { Mutex() }.withLock {
            val folder = folderDao.getById(folderId) ?: return@withLock null
            val provider = providers.forAccount(folder.accountId) ?: return@withLock null
            val remote = RemoteFolder(folder.remoteName, folder.displayName, folder.role)

            var known = folder.uidValidity
            var rounds = 0
            while (true) {
                var fetchedNewest = false
                var afterUid = known?.let { messageDao.maxUid(folder.id, it) }
                if (afterUid == null) fetchedNewest = true

                var snapshot = when (val r = provider.fetchHeaders(remote, afterUid, PAGE)) {
                    is MailResult.Failure -> return@withLock r.error
                    is MailResult.Success -> r.value
                }
                if (known != null && snapshot.uidValidity != known) {
                    // The server reset its UIDs: cached rows no longer identify anything.
                    messageDao.deleteStaleValidity(folder.id, snapshot.uidValidity)
                    afterUid = null
                    fetchedNewest = true
                    snapshot = when (val r = provider.fetchHeaders(remote, null, PAGE)) {
                        is MailResult.Failure -> return@withLock r.error
                        is MailResult.Success -> r.value
                    }
                }

                messageDao.insertIgnoringDuplicates(
                    snapshot.headers.map { it.toEntity(folder, snapshot.uidValidity) },
                )
                known = snapshot.uidValidity
                folderDao.update(
                    folder.copy(
                        uidValidity = snapshot.uidValidity,
                        uidNext = snapshot.uidNext,
                        lastSyncedAtMillis = System.currentTimeMillis(),
                    ),
                )
                rounds++
                if (fetchedNewest || snapshot.headers.size < PAGE || rounds >= MAX_ROUNDS) break
            }
            null
        }

    private fun RemoteMessageHeader.toEntity(folder: FolderEntity, uidValidity: Long) = MessageEntity(
        accountId = folder.accountId,
        folderId = folder.id,
        uidValidity = uidValidity,
        uid = uid,
        messageIdHeader = messageIdHeader,
        subject = subject,
        fromName = fromName,
        fromAddress = fromAddress,
        snippet = snippet,
        receivedAtMillis = receivedAtMillis,
        isRead = isRead,
        isStarred = isStarred,
        hasAttachments = hasAttachments,
        sizeBytes = sizeBytes,
    )

    private companion object {
        const val PAGE = 50
        const val MAX_ROUNDS = 10
    }
}
