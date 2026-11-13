package com.kmail.thekami.mail

import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import java.util.Properties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageMapperTest {
    private fun parse(raw: String) =
        MimeMessage(Session.getInstance(Properties()), raw.replace("\n", "\r\n").byteInputStream())

    @Test
    fun mapsBasicHeaders() {
        val m = parse(
            "From: \"Jane Doe\" <jane@example.com>\n" +
                "Subject: Hello\n" +
                "Message-ID: <abc@example.com>\n" +
                "Date: Mon, 2 Mar 2026 10:00:00 +0000\n" +
                "Content-Type: text/plain\n\nbody\n",
        )
        val h = MessageMapper.toHeader(7, m)
        assertEquals(7L, h.uid)
        assertEquals("Jane Doe", h.fromName)
        assertEquals("jane@example.com", h.fromAddress)
        assertEquals("Hello", h.subject)
        assertEquals("<abc@example.com>", h.messageIdHeader)
        assertTrue(h.receivedAtMillis > 0)
        assertFalse(h.hasAttachments)
        assertFalse(h.isRead)
    }

    @Test
    fun decodesEncodedSubject() {
        val m = parse("From: a@b.com\nSubject: =?UTF-8?Q?Caf=C3=A9?=\n\nx\n")
        assertEquals("Caf\u00e9", MessageMapper.toHeader(1, m).subject)
    }

    @Test
    fun missingFromAndSubjectFallBackToEmpty() {
        val h = MessageMapper.toHeader(1, parse("To: a@b.com\n\nx\n"))
        assertEquals("", h.fromAddress)
        assertNull(h.fromName)
        assertEquals("", h.subject)
        assertEquals(0L, h.receivedAtMillis)
    }

    @Test
    fun multipartMixedCountsAsAttachments() {
        val m = parse("From: a@b.com\nContent-Type: multipart/mixed; boundary=zz\n\n--zz\n\nx\n--zz--\n")
        assertTrue(MessageMapper.toHeader(1, m).hasAttachments)
    }
}
