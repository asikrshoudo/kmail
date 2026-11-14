package com.kmail.thekami.mail

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MimeParserTest {
    private fun parse(raw: String) = MimeParser.parse(raw.trimIndent().replace("\n", "\r\n").toByteArray())

    @Test
    fun parsesAlternativeBodiesAddressesAndAttachment() {
        val m = parse(
            """
            From: A <a@x.com>
            To: b@y.com, "C D" <c@y.com>
            Subject: Test
            Date: Mon, 2 Mar 2026 10:00:00 +0000
            Message-ID: <id1@x.com>
            MIME-Version: 1.0
            Content-Type: multipart/mixed; boundary="b1"

            --b1
            Content-Type: multipart/alternative; boundary="b2"

            --b2
            Content-Type: text/plain; charset=utf-8

            Hello text
            --b2
            Content-Type: text/html; charset=utf-8

            <p>Hello <b>html</b></p>
            --b2--

            --b1
            Content-Type: application/octet-stream; name="a.bin"
            Content-Disposition: attachment; filename="a.bin"
            Content-Transfer-Encoding: base64

            aGVsbG8=
            --b1--
            """,
        )
        assertEquals("Test", m.subject)
        assertEquals("a@x.com", m.from?.address)
        assertEquals(2, m.to.size)
        assertEquals("C D", m.to[1].name)
        assertEquals("<id1@x.com>", m.messageId)
        assertTrue(m.htmlBody!!.contains("Hello"))
        assertTrue(m.textBody!!.startsWith("Hello text"))
        assertEquals(1, m.attachments.size)
        assertEquals("a.bin", m.attachments[0].fileName)
        assertArrayEquals("hello".toByteArray(), m.attachments[0].bytes)
    }

    @Test
    fun inlineImageIsCollectedByContentIdAndNotListedAsAttachment() {
        val m = parse(
            """
            From: a@x.com
            Subject: Inline
            MIME-Version: 1.0
            Content-Type: multipart/related; boundary="r1"

            --r1
            Content-Type: text/html; charset=utf-8

            <img src="cid:logo1">
            --r1
            Content-Type: image/png
            Content-ID: <logo1>
            Content-Transfer-Encoding: base64

            AQID
            --r1--
            """,
        )
        assertArrayEquals(byteArrayOf(1, 2, 3), m.inlineImages["logo1"]!!.bytes)
        assertTrue(m.attachments.isEmpty())
    }

    @Test
    fun decodesQuotedPrintableWithLatin1Charset() {
        val m = parse(
            """
            From: a@x.com
            Subject: =?UTF-8?Q?Caf=C3=A9?=
            Content-Type: text/plain; charset=iso-8859-1
            Content-Transfer-Encoding: quoted-printable

            Caf=E9
            """,
        )
        assertEquals("Caf\u00e9", m.subject)
        assertTrue(m.textBody!!.startsWith("Caf\u00e9"))
    }

    @Test
    fun previewCollapsesWhitespace() {
        val m = parse(
            """
            From: a@x.com
            Content-Type: text/plain

            Hello
               there

            world
            """,
        )
        assertEquals("Hello there world", m.previewText())
    }

    @Test
    fun messageWithoutBodyOrFromDoesNotCrash() {
        val m = parse("Subject: x\n\n")
        assertNull(m.from)
        assertEquals("x", m.subject)
    }
}
