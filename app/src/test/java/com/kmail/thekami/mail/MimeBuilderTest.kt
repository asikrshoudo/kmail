package com.kmail.thekami.mail

import java.util.Date
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MimeBuilderTest {
    private fun draft(attachments: List<MimeBuilder.Attachment> = emptyList()) = MimeBuilder.Draft(
        from = Mailbox("Zo\u00eb \u00dcnal", "zoe@example.com"),
        to = listOf("a@x.com", "b@y.com"),
        cc = listOf("c@z.com"),
        subject = "Caf\u00e9 \u2615 meeting",
        body = "Hello\nWorld \u2014 \u2713",
        attachments = attachments,
        inReplyTo = "<orig@x.com>",
        references = listOf("<r1@x.com>", "<orig@x.com>"),
    )

    @Test
    fun plainMessageRoundTripsThroughTheParser() {
        val raw = MimeBuilder.build(draft(), now = Date(0), messageIdSeed = "seed")
        val m = MimeParser.parse(raw)
        assertEquals("Caf\u00e9 \u2615 meeting", m.subject)
        assertEquals("zoe@example.com", m.from?.address)
        assertEquals("Zo\u00eb \u00dcnal", m.from?.name)
        assertEquals(listOf("a@x.com", "b@y.com"), m.to.map { it.address })
        assertEquals(listOf("c@z.com"), m.cc.map { it.address })
        assertEquals("<seed@example.com>", m.messageId)
        assertEquals("<orig@x.com>", m.inReplyTo)
        assertEquals(listOf("<r1@x.com>", "<orig@x.com>"), m.references)
        assertEquals(0L, m.dateMillis)
        assertTrue(m.textBody!!.replace("\r\n", "\n").contains("Hello\nWorld \u2014 \u2713"))
        assertNull(m.htmlBody)
        assertTrue(m.attachments.isEmpty())
    }

    @Test
    fun attachmentsRoundTripWithNonAsciiName() {
        val bytes = ByteArray(3000) { (it * 7).toByte() }
        val raw = MimeBuilder.build(draft(listOf(MimeBuilder.Attachment("r\u00e9sum\u00e9.pdf", "application/pdf", bytes))))
        val m = MimeParser.parse(raw)
        assertEquals(1, m.attachments.size)
        assertEquals("r\u00e9sum\u00e9.pdf", m.attachments[0].fileName)
        assertArrayEquals(bytes, m.attachments[0].bytes)
        assertTrue(m.textBody!!.contains("Hello"))
    }

    @Test
    fun asciiAttachmentNameIsKept() {
        val raw = MimeBuilder.build(draft(listOf(MimeBuilder.Attachment("report.txt", "text/plain", "hi".toByteArray()))))
        assertEquals("report.txt", MimeParser.parse(raw).attachments[0].fileName)
    }

    @Test
    fun subjectNewlinesCannotInjectHeaders() {
        val d = MimeBuilder.Draft(
            Mailbox(null, "a@x.com"), listOf("b@y.com"), emptyList(),
            "hi\r\nBcc: evil@x.com", "x", emptyList(), null, emptyList(),
        )
        val text = String(MimeBuilder.build(d))
        assertFalse(text.contains("\r\nBcc:"))
    }
}
