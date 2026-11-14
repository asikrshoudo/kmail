package com.kmail.thekami.data.repo

import com.kmail.thekami.data.db.FolderEntity
import com.kmail.thekami.data.db.MessageEntity
import com.kmail.thekami.domain.mail.MailError
import com.kmail.thekami.domain.mail.MailProvider
import com.kmail.thekami.domain.mail.MailResult
import com.kmail.thekami.domain.mail.ProviderCapabilities
import com.kmail.thekami.domain.mail.ProviderSource
import com.kmail.thekami.domain.model.FolderRole
import com.kmail.thekami.domain.model.FolderSnapshot
import com.kmail.thekami.domain.model.RemoteFolder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class RecordingProvider : MailProvider {
    override val capabilities = ProviderCapabilities(supportsPush = false, supportsCategories = false)
    var flagResult: MailResult<Unit> = MailResult.Success(Unit)
    var moveResult: MailResult<Unit> = MailResult.Success(Unit)
    val flagCalls = mutableListOf<List<Long>>()
    val moveCalls = mutableListOf<Pair<List<Long>, String>>()

    override suspend fun testConnection(): MailResult<Unit> = MailResult.Success(Unit)
    override suspend fun listFolders(): MailResult<List<RemoteFolder>> = MailResult.Success(emptyList())
    override suspend fun fetchHeaders(folder: RemoteFolder, afterUid: Long?, limit: Int): MailResult<FolderSnapshot> =
        MailResult.Failure(MailError.Unsupported())
    override suspend fun fetchRawMessage(folder: RemoteFolder, uidValidity: Long, uid: Long): MailResult<ByteArray> =
        MailResult.Failure(MailError.Unsupported())
    override suspend fun updateFlags(
        folder: RemoteFolder, uidValidity: Long, uids: List<Long>, isRead: Boolean?, isStarred: Boolean?,
    ): MailResult<Unit> {
        flagCalls += uids
        return flagResult
    }
    override suspend fun moveMessages(
        folder: RemoteFolder, uidValidity: Long, uids: List<Long>, destination: RemoteFolder,
    ): MailResult<Unit> {
        moveCalls += uids to destination.remoteName
        return moveResult
    }
    override suspend fun send(rawMime: ByteArray, envelopeFrom: String, recipients: List<String>): MailResult<Unit> =
        MailResult.Success(Unit)
    override suspend fun appendMessage(folder: RemoteFolder, rawMime: ByteArray, isRead: Boolean): MailResult<Unit> =
        MailResult.Success(Unit)
}

class MessageActionsRepositoryTest {
    private val folders = FakeFolderDao()
    private val messages = FakeMessageDao()
    private val provider = RecordingProvider()
    private val repo = MessageActionsRepository(
        object : ProviderSource {
            override suspend fun forAccount(accountId: String): MailProvider = provider
        },
        folders,
        messages,
    )

    private suspend fun folder(name: String, role: FolderRole) =
        folders.insert(FolderEntity(accountId = "a", remoteName = name, displayName = name, role = role))

    private fun msg(folderId: Long, uid: Long) = MessageEntity(
        accountId = "a", folderId = folderId, uidValidity = 5, uid = uid, subject = "s",
        fromAddress = "x@y.com", snippet = "", receivedAtMillis = uid,
    )

    private suspend fun seed(): Triple<Long, Long, Long> {
        val inbox = folder("INBOX", FolderRole.INBOX)
        val archive = folder("Archive", FolderRole.ARCHIVE)
        val trash = folder("Trash", FolderRole.TRASH)
        messages.insertIgnoringDuplicates(listOf(msg(inbox, 10), msg(inbox, 11)))
        return Triple(inbox, archive, trash)
    }

    @Test
    fun markReadUpdatesServerThenLocalRows() = runTest {
        seed()
        assertEquals(MessageActionResult.Ok, repo.setRead(listOf(1, 2), true))
        assertEquals(listOf(listOf(10L, 11L)), provider.flagCalls)
        assertTrue(messages.rows.all { it.isRead })
    }

    @Test
    fun serverFailureLeavesLocalRowsUntouched() = runTest {
        seed()
        provider.flagResult = MailResult.Failure(MailError.Network("x"))
        assertEquals(MessageActionResult.Failed(MailError.Network("x")), repo.setStarred(listOf(1), true))
        assertTrue(messages.rows.none { it.isStarred })
    }

    @Test
    fun archiveMovesToArchiveFolderAndHidesMessages() = runTest {
        seed()
        assertEquals(MessageActionResult.Ok, repo.moveToRole(listOf(1, 2), FolderRole.ARCHIVE))
        assertEquals(listOf(listOf(10L, 11L) to "Archive"), provider.moveCalls)
        assertTrue(messages.rows.all { it.removedAtMillis != null })
    }

    @Test
    fun deleteMovesToTrash() = runTest {
        seed()
        repo.moveToRole(listOf(1), FolderRole.TRASH)
        assertEquals(listOf(listOf(10L) to "Trash"), provider.moveCalls)
        assertNotNull(messages.rows.first { it.id == 1L }.removedAtMillis)
        assertNull(messages.rows.first { it.id == 2L }.removedAtMillis)
    }

    @Test
    fun missingTargetFolderIsReportedWithoutTouchingTheServer() = runTest {
        val inbox = folder("INBOX", FolderRole.INBOX)
        messages.insertIgnoringDuplicates(listOf(msg(inbox, 10)))
        assertEquals(MessageActionResult.NoTargetFolder(FolderRole.ARCHIVE), repo.moveToRole(listOf(1), FolderRole.ARCHIVE))
        assertTrue(provider.moveCalls.isEmpty())
        assertNull(messages.rows[0].removedAtMillis)
    }

    @Test
    fun messagesAlreadyInTheTargetFolderAreSkipped() = runTest {
        val (_, archive, _) = seed()
        messages.insertIgnoringDuplicates(listOf(msg(archive, 20)))
        assertEquals(MessageActionResult.Ok, repo.moveToRole(listOf(3), FolderRole.ARCHIVE))
        assertTrue(provider.moveCalls.isEmpty())
    }
}
