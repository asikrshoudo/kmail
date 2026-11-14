package com.kmail.thekami.data.repo

import com.kmail.thekami.data.db.FolderDao
import com.kmail.thekami.data.db.FolderEntity
import com.kmail.thekami.data.db.MessageDao
import com.kmail.thekami.data.db.MessageEntity
import com.kmail.thekami.domain.mail.MailError
import com.kmail.thekami.domain.mail.MailProvider
import com.kmail.thekami.domain.mail.MailResult
import com.kmail.thekami.domain.mail.ProviderCapabilities
import com.kmail.thekami.domain.mail.ProviderSource
import com.kmail.thekami.domain.model.FolderRole
import com.kmail.thekami.domain.model.FolderSnapshot
import com.kmail.thekami.domain.model.RemoteFolder
import com.kmail.thekami.domain.model.RemoteMessageHeader
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeFolderDao : FolderDao {
    val rows = mutableListOf<FolderEntity>()
    private var nextId = 1L
    override fun observeByAccount(accountId: String) = flowOf(rows.filter { it.accountId == accountId })
    override suspend fun find(accountId: String, remoteName: String) =
        rows.firstOrNull { it.accountId == accountId && it.remoteName == remoteName }
    override suspend fun insert(folder: FolderEntity): Long {
        val id = nextId++
        rows += folder.copy(id = id)
        return id
    }
    override suspend fun update(folder: FolderEntity) {
        rows[rows.indexOfFirst { it.id == folder.id }] = folder
    }
    override suspend fun getById(id: Long) = rows.firstOrNull { it.id == id }
    override fun observeAll() = flowOf(rows.toList())
    override suspend fun listByAccount(accountId: String) = rows.filter { it.accountId == accountId }
}

private class FakeMessageDao : MessageDao {
    val rows = mutableListOf<MessageEntity>()
    private var nextId = 1L
    private fun same(a: MessageEntity, b: MessageEntity) =
        a.accountId == b.accountId && a.folderId == b.folderId && a.uidValidity == b.uidValidity && a.uid == b.uid

    override fun observeFolder(folderId: Long, uidValidity: Long, limit: Int) =
        flowOf(rows.filter { it.folderId == folderId && it.uidValidity == uidValidity })
    override suspend fun findByKey(accountId: String, folderId: Long, uidValidity: Long, uid: Long) =
        rows.firstOrNull {
            it.accountId == accountId && it.folderId == folderId && it.uidValidity == uidValidity && it.uid == uid
        }
    override suspend fun insertIgnoringDuplicates(messages: List<MessageEntity>): List<Long> =
        messages.map { m ->
            if (rows.any { same(it, m) }) {
                -1L
            } else {
                val id = nextId++
                rows += m.copy(id = id)
                id
            }
        }
    override suspend fun updateFlags(id: Long, isRead: Boolean, isStarred: Boolean) {}
    override suspend fun markRemoved(ids: List<Long>, nowMillis: Long) {}
    override suspend fun deleteStaleValidity(folderId: Long, currentUidValidity: Long) {
        rows.removeAll { it.folderId == folderId && it.uidValidity != currentUidValidity }
    }
    override suspend fun deleteByFolder(folderId: Long) {
        rows.removeAll { it.folderId == folderId }
    }
    override suspend fun maxUid(folderId: Long, uidValidity: Long): Long? =
        rows.filter { it.folderId == folderId && it.uidValidity == uidValidity }.maxOfOrNull { it.uid }
    override fun observeInFolder(folderId: Long, limit: Int) = flowOf(emptyList<MessageEntity>())
    override fun observeUnifiedInbox(limit: Int) = flowOf(emptyList<MessageEntity>())
    override suspend fun getById(id: Long): MessageEntity? = rows.firstOrNull { it.id == id }
    override suspend fun updateSnippet(id: Long, snippet: String) {}
}

private class FakeProvider : MailProvider {
    override val capabilities = ProviderCapabilities(supportsPush = false, supportsCategories = false)
    var folders: MailResult<List<RemoteFolder>> = MailResult.Success(emptyList())
    val fetchCalls = mutableListOf<Long?>()
    var onFetch: (Long?) -> MailResult<FolderSnapshot> = { MailResult.Success(FolderSnapshot(1, 1, emptyList())) }

    override suspend fun testConnection() = MailResult.Success(Unit)
    override suspend fun listFolders() = folders
    override suspend fun fetchHeaders(folder: RemoteFolder, afterUid: Long?, limit: Int): MailResult<FolderSnapshot> {
        fetchCalls += afterUid
        return onFetch(afterUid)
    }
    override suspend fun fetchRawMessage(folder: RemoteFolder, uidValidity: Long, uid: Long) =
        MailResult.Failure(MailError.Unsupported())
    override suspend fun updateFlags(
        folder: RemoteFolder, uidValidity: Long, uids: List<Long>, isRead: Boolean?, isStarred: Boolean?,
    ) = MailResult.Success(Unit)
    override suspend fun moveMessages(
        folder: RemoteFolder, uidValidity: Long, uids: List<Long>, destination: RemoteFolder,
    ) = MailResult.Success(Unit)
    override suspend fun send(rawMime: ByteArray, envelopeFrom: String, recipients: List<String>) =
        MailResult.Success(Unit)
}

class SyncRepositoryTest {
    private val folders = FakeFolderDao()
    private val messages = FakeMessageDao()
    private val provider = FakeProvider()
    private val repo = SyncRepository(
        object : ProviderSource {
            override suspend fun forAccount(accountId: String): MailProvider = provider
        },
        folders,
        messages,
    )

    private fun h(uid: Long) = RemoteMessageHeader(
        uid = uid, messageIdHeader = null, subject = "s$uid", fromName = null, fromAddress = "a@b.com",
        snippet = "", receivedAtMillis = uid * 1000, isRead = false, isStarred = false,
        hasAttachments = false, sizeBytes = 0,
    )

    private suspend fun addInbox() = folders.insert(
        FolderEntity(accountId = "a", remoteName = "INBOX", displayName = "INBOX", role = FolderRole.INBOX),
    )

    @Test
    fun firstSyncFetchesNewestAndStoresValidity() = runTest {
        val id = addInbox()
        provider.onFetch = { MailResult.Success(FolderSnapshot(5, 4, listOf(h(1), h(2), h(3)))) }
        assertNull(repo.syncFolder(id))
        assertEquals(listOf<Long?>(null), provider.fetchCalls)
        assertEquals(3, messages.rows.size)
        val f = folders.getById(id)!!
        assertEquals(5L, f.uidValidity)
        assertEquals(4L, f.uidNext)
        assertNotNull(f.lastSyncedAtMillis)
    }

    @Test
    fun secondSyncOnlyAsksForNewerUids() = runTest {
        val id = addInbox()
        provider.onFetch = { MailResult.Success(FolderSnapshot(5, 4, listOf(h(1), h(2), h(3)))) }
        repo.syncFolder(id)
        provider.fetchCalls.clear()
        provider.onFetch = { MailResult.Success(FolderSnapshot(5, 6, listOf(h(4), h(5)))) }
        assertNull(repo.syncFolder(id))
        assertEquals(listOf<Long?>(3L), provider.fetchCalls)
        assertEquals(5, messages.rows.size)
    }

    @Test
    fun changedUidValidityDropsOldRowsAndRefetches() = runTest {
        val id = addInbox()
        provider.onFetch = { MailResult.Success(FolderSnapshot(5, 4, listOf(h(1), h(2), h(3)))) }
        repo.syncFolder(id)
        provider.fetchCalls.clear()
        provider.onFetch = { after ->
            if (after != null) {
                MailResult.Success(FolderSnapshot(9, 3, emptyList()))
            } else {
                MailResult.Success(FolderSnapshot(9, 3, listOf(h(1), h(2))))
            }
        }
        assertNull(repo.syncFolder(id))
        assertEquals(listOf<Long?>(3L, null), provider.fetchCalls)
        assertEquals(2, messages.rows.size)
        assertEquals(setOf(9L), messages.rows.map { it.uidValidity }.toSet())
        assertEquals(9L, folders.getById(id)!!.uidValidity)
    }

    @Test
    fun failureIsReturnedAndNothingIsStored() = runTest {
        val id = addInbox()
        provider.onFetch = { MailResult.Failure(MailError.Network("x")) }
        assertEquals(MailError.Network("x"), repo.syncFolder(id))
        assertEquals(0, messages.rows.size)
    }

    @Test
    fun duplicateHeadersAreStoredOnce() = runTest {
        val id = addInbox()
        provider.onFetch = { MailResult.Success(FolderSnapshot(5, 2, listOf(h(1), h(1)))) }
        repo.syncFolder(id)
        assertEquals(1, messages.rows.size)
    }

    @Test
    fun syncAccountCreatesFoldersOnceAndSyncsOnlyTheInbox() = runTest {
        provider.folders = MailResult.Success(
            listOf(
                RemoteFolder("INBOX", "INBOX", FolderRole.INBOX),
                RemoteFolder("INBOX.Sent", "Sent", FolderRole.SENT),
            ),
        )
        provider.onFetch = { MailResult.Success(FolderSnapshot(5, 2, listOf(h(1)))) }
        assertNull(repo.syncAccount("a"))
        assertNull(repo.syncAccount("a"))
        assertEquals(2, folders.rows.size)
        assertEquals(1, messages.rows.size)
        assertEquals(listOf<Long?>(null, 1L), provider.fetchCalls)
    }
}
