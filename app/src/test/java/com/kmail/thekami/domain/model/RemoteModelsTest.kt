package com.kmail.thekami.domain.model

import org.junit.Assert.assertThrows
import org.junit.Test

class RemoteModelsTest {
    private fun header(uid: Long) = RemoteMessageHeader(
        uid = uid,
        messageIdHeader = null,
        subject = "s",
        fromName = null,
        fromAddress = "a@example.com",
        snippet = "",
        receivedAtMillis = 0,
        isRead = false,
        isStarred = false,
        hasAttachments = false,
        sizeBytes = 0,
    )

    @Test
    fun headerRejectsZeroUid() {
        assertThrows(IllegalArgumentException::class.java) { header(0) }
    }

    @Test
    fun headerAcceptsMaxUid() {
        header(MAX_IMAP_UINT32)
    }

    @Test
    fun snapshotRejectsZeroUidValidity() {
        assertThrows(IllegalArgumentException::class.java) {
            FolderSnapshot(uidValidity = 0, uidNext = null, headers = emptyList())
        }
    }

    @Test
    fun snapshotAcceptsMissingUidNext() {
        FolderSnapshot(uidValidity = 7, uidNext = null, headers = listOf(header(1)))
    }

    @Test
    fun snapshotRejectsUidNextOutOfRange() {
        assertThrows(IllegalArgumentException::class.java) {
            FolderSnapshot(uidValidity = 7, uidNext = MAX_IMAP_UINT32 + 1, headers = emptyList())
        }
    }
}
