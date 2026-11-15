package com.kmail.thekami.data.drafts

import com.kmail.thekami.domain.model.Draft
import com.kmail.thekami.domain.model.DraftAttachment
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DraftCodecTest {
    private fun sample(body: String = "hi", accountId: String? = "acc", inReplyTo: String? = "<r@x>") = Draft(
        id = "a1b2", accountId = accountId, to = "to@x.com", cc = "cc@x.com", bcc = "",
        subject = "Subj \u2615", body = body, inReplyTo = inReplyTo,
        references = listOf("<a>", "<b>"),
        attachments = listOf(DraftAttachment("f.bin", "application/octet-stream", byteArrayOf(1, 2, 3))),
        updatedAtMillis = 12345L,
    )

    @Test
    fun roundTripKeepsEveryField() {
        val d = DraftCodec.decode(DraftCodec.encode(sample()))!!
        assertEquals("a1b2", d.id)
        assertEquals("acc", d.accountId)
        assertEquals("to@x.com", d.to)
        assertEquals("cc@x.com", d.cc)
        assertEquals("", d.bcc)
        assertEquals("Subj \u2615", d.subject)
        assertEquals("hi", d.body)
        assertEquals("<r@x>", d.inReplyTo)
        assertEquals(listOf("<a>", "<b>"), d.references)
        assertEquals("f.bin", d.attachments[0].name)
        assertArrayEquals(byteArrayOf(1, 2, 3), d.attachments[0].bytes)
        assertEquals(12345L, d.updatedAtMillis)
    }

    @Test
    fun nullableFieldsAndHugeBodiesRoundTrip() {
        val body = "\u00e9\u2615 line\n".repeat(30_000)
        val d = DraftCodec.decode(DraftCodec.encode(sample(body, accountId = null, inReplyTo = null)))!!
        assertNull(d.accountId)
        assertNull(d.inReplyTo)
        assertEquals(body, d.body)
    }

    @Test
    fun truncatedCorruptOrForeignDataIsRejected() {
        val bytes = DraftCodec.encode(sample())
        assertNull(DraftCodec.decode(bytes.copyOf(10)))
        assertNull(DraftCodec.decode(bytes.copyOf(bytes.size - 5)))
        assertNull(DraftCodec.decode(ByteArray(0)))
        assertNull(DraftCodec.decode(ByteArray(64) { 0x7f }))
        val wrongVersion = bytes.copyOf().also { it[3] = 9 }
        assertNull(DraftCodec.decode(wrongVersion))
        assertNotNull(DraftCodec.decode(bytes))
    }
}
