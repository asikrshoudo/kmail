package com.kmail.thekami.data.repo

import com.kmail.thekami.data.db.FolderEntity
import com.kmail.thekami.data.db.MessageEntity
import com.kmail.thekami.domain.mail.MailError
import com.kmail.thekami.domain.mail.MailProvider
import com.kmail.thekami.domain.mail.MailResult
import com.kmail.thekami.domain.mail.ProviderSource
import com.kmail.thekami.domain.model.FolderRole
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyPrefetcherTest {
    private val folders = FakeFolderDao()
    private val messages = FakeMessageDao()
    private val provider = FakeProvider()
    private val store = BodyStore(Files.createTempDirectory("bodies").toFile(), FakeCipher(), 10_000_000)
    private val prefetcher = BodyPrefetcher(
        object : ProviderSource {
            override suspend fun forAccount(accountId: String): MailProvider = provider
        },
        folders,
        messages,
        store,
    )

    private fun raw(text: String) = "From: a@b.com\r\nContent-Type: text/plain\r\n\r\n$text\r\n".toByteArray()

    private suspend fun seed(): Long {
        val inbox = folders.insert(
            FolderEntity(accountId = "a", remoteName = "INBOX", displayName = "INBOX", role = FolderRole.INBOX, uidValidity = 5),
        )
        messages.insertIgnoringDuplicates(
            (1L..3L).map {
                MessageEntity(
                    accountId = "a", folderId = inbox, uidValidity = 5, uid = it * 10, subject = "s",
                    fromAddress = "a@b.com", snippet = "", receivedAtMillis = it,
                )
            },
        )
        return inbox
    }

    @Test
    fun storesBodiesAndFillsPreviews() = runTest {
        seed()
        provider.batch = MailResult.Success(mapOf(10L to raw("one"), 20L to raw("two  words"), 30L to raw("three")))
        prefetcher.prefetchInbox("a")
        assertTrue((1L..3L).all { store.has(it) })
        assertEquals(listOf("one", "two words", "three"), messages.rows.sortedBy { it.id }.map { it.snippet })
    }

    @Test
    fun skipsMessagesThatAreAlreadyStored() = runTest {
        seed()
        store.put(3, raw("old"))
        provider.batch = MailResult.Success(mapOf(10L to raw("one"), 20L to raw("two")))
        prefetcher.prefetchInbox("a")
        assertEquals(1, provider.batchCalls.size)
        assertEquals(setOf(10L, 20L), provider.batchCalls[0].toSet())
    }

    @Test
    fun aFailedDownloadStoresNothing() = runTest {
        seed()
        provider.batch = MailResult.Failure(MailError.Network("x"))
        prefetcher.prefetchInbox("a")
        assertFalse(store.has(1))
        assertTrue(messages.rows.all { it.snippet.isEmpty() })
    }
}
