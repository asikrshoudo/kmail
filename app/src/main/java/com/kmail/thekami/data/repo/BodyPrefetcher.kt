package com.kmail.thekami.data.repo

import com.kmail.thekami.data.db.FolderDao
import com.kmail.thekami.data.db.MessageDao
import com.kmail.thekami.domain.mail.MailResult
import com.kmail.thekami.domain.mail.ProviderSource
import com.kmail.thekami.domain.model.FolderRole
import com.kmail.thekami.domain.model.RemoteFolder
import com.kmail.thekami.mail.MimeParser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Downloads the newest inbox messages in one connection, so they open instantly. Also fills list previews. */
@Singleton
class BodyPrefetcher @Inject constructor(
    private val providers: ProviderSource,
    private val folderDao: FolderDao,
    private val messageDao: MessageDao,
    private val bodies: BodyStore,
) {
    private val mutex = Mutex()

    suspend fun prefetchInbox(accountId: String) = mutex.withLock {
        val folder = folderDao.listByAccount(accountId).firstOrNull { it.role == FolderRole.INBOX } ?: return@withLock
        val validity = folder.uidValidity ?: return@withLock
        val wanted = messageDao.listLive(folder.id, validity)
            .sortedByDescending { it.receivedAtMillis }
            .take(LIMIT)
            .filter { !bodies.has(it.id) }
        if (wanted.isEmpty()) return@withLock
        val provider = providers.forAccount(accountId) ?: return@withLock
        val remote = RemoteFolder(folder.remoteName, folder.displayName, folder.role)
        val fetched = (provider.fetchRawBatch(remote, validity, wanted.map { it.uid }, MAX_EACH) as? MailResult.Success)
            ?.value ?: return@withLock
        for (message in wanted) {
            val raw = fetched[message.uid] ?: continue
            if (!bodies.put(message.id, raw)) continue
            if (message.snippet.isBlank()) {
                val preview = withContext(Dispatchers.Default) {
                    runCatching { MimeParser.parse(raw).previewText() }.getOrDefault("")
                }
                if (preview.isNotBlank()) messageDao.updateSnippet(message.id, preview)
            }
        }
    }

    private companion object {
        const val LIMIT = 15
        const val MAX_EACH = 2L * 1024 * 1024
    }
}
