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
import javax.inject.Inject
import javax.inject.Singleton

sealed interface MessageActionResult {
    data object Ok : MessageActionResult
    data class NoTargetFolder(val role: FolderRole) : MessageActionResult
    /** [error] is null when the account no longer exists. */
    data class Failed(val error: MailError?) : MessageActionResult
}

/**
 * Applies an action on the server first and changes the local cache only after the server accepted it.
 * Moved messages are hidden locally. They appear in the target folder at its next sync.
 */
@Singleton
class MessageActionsRepository @Inject constructor(
    private val providers: ProviderSource,
    private val folderDao: FolderDao,
    private val messageDao: MessageDao,
) {
    private class Batch(val folder: FolderEntity, val uidValidity: Long, val messages: List<MessageEntity>)

    suspend fun setRead(ids: List<Long>, read: Boolean) = flags(ids, isRead = read, isStarred = null)

    suspend fun setStarred(ids: List<Long>, starred: Boolean) = flags(ids, isRead = null, isStarred = starred)

    suspend fun moveToRole(ids: List<Long>, role: FolderRole): MessageActionResult {
        val now = System.currentTimeMillis()
        for (batch in batches(ids)) {
            if (batch.folder.role == role) continue
            val target = folderDao.listByAccount(batch.folder.accountId).firstOrNull { it.role == role }
                ?: return MessageActionResult.NoTargetFolder(role)
            val provider = providers.forAccount(batch.folder.accountId) ?: return MessageActionResult.Failed(null)
            when (
                val r = provider.moveMessages(
                    remote(batch.folder), batch.uidValidity, batch.messages.map { it.uid }, remote(target),
                )
            ) {
                is MailResult.Failure -> return MessageActionResult.Failed(r.error)
                is MailResult.Success -> messageDao.markRemoved(batch.messages.map { it.id }, now)
            }
        }
        return MessageActionResult.Ok
    }

    private suspend fun flags(ids: List<Long>, isRead: Boolean?, isStarred: Boolean?): MessageActionResult {
        for (batch in batches(ids)) {
            val provider = providers.forAccount(batch.folder.accountId) ?: return MessageActionResult.Failed(null)
            when (
                val r = provider.updateFlags(
                    remote(batch.folder), batch.uidValidity, batch.messages.map { it.uid }, isRead, isStarred,
                )
            ) {
                is MailResult.Failure -> return MessageActionResult.Failed(r.error)
                is MailResult.Success -> batch.messages.forEach {
                    messageDao.updateFlags(it.id, isRead ?: it.isRead, isStarred ?: it.isStarred)
                }
            }
        }
        return MessageActionResult.Ok
    }

    private suspend fun batches(ids: List<Long>): List<Batch> =
        messageDao.getByIds(ids).groupBy { it.folderId to it.uidValidity }.mapNotNull { (key, list) ->
            folderDao.getById(key.first)?.let { Batch(it, key.second, list) }
        }

    private fun remote(f: FolderEntity) = RemoteFolder(f.remoteName, f.displayName, f.role)
}
