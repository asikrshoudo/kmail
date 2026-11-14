package com.kmail.thekami.sync

import com.kmail.thekami.data.db.FolderDao
import com.kmail.thekami.data.db.MessageDao
import com.kmail.thekami.data.repo.SyncRepository
import com.kmail.thekami.domain.model.Account
import com.kmail.thekami.domain.model.FolderRole
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Singleton
class BackgroundSync @Inject constructor(
    private val sync: SyncRepository,
    private val folderDao: FolderDao,
    private val messageDao: MessageDao,
    private val notifier: NewMailNotifier,
) {
    /**
     * Syncs every account. Notifies only about unread inbox messages that arrived during this run,
     * and never for an inbox that had not been synced before, so a new account does not flood.
     * Returns false if any account failed.
     */
    suspend fun run(accounts: List<Account>, notificationsEnabled: Boolean): Boolean {
        val baseline = messageDao.maxId()
        val warm = accounts.filter { account ->
            folderDao.listByAccount(account.id).any { it.role == FolderRole.INBOX && it.lastSyncedAtMillis != null }
        }.map { it.id }.toSet()

        val errors = coroutineScope { accounts.map { async { sync.syncAccount(it.id, reconcile = false) } }.awaitAll() }

        if (notificationsEnabled) {
            val fresh = messageDao.newUnreadInbox(baseline).filter { it.accountId in warm }
            if (fresh.isNotEmpty()) notifier.notifyNew(fresh, accounts)
        }
        return errors.all { it == null }
    }
}
