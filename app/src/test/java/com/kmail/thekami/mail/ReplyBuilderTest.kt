package com.kmail.thekami.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyBuilderTest {
    private fun message(
        from: String = "alice@x.com",
        to: List<String> = listOf("me@y.com", "bob@z.com"),
        cc: List<String> = listOf("carol@z.com"),
        replyTo: List<String> = emptyList(),
        subject: String = "Plan",
    ) = ParsedMessage(
        subject = subject,
        from = Mailbox(null, from),
        to = to.map { Mailbox(null, it) },
        cc = cc.map { Mailbox(null, it) },
        replyTo = replyTo.map { Mailbox(null, it) },
        dateMillis = null,
        messageId = "<m3@x.com>",
        inReplyTo = null,
        references = listOf("<m1@x.com>", "<m2@x.com>"),
        htmlBody = null,
        textBody = "line one\nline two",
        inlineImages = emptyMap(),
        attachments = listOf(ParsedAttachment("a.txt", "text/plain", ByteArray(1))),
    )

    @Test
    fun replyGoesToSenderAndQuotesTheBody() {
        val d = ReplyBuilder.reply(message(), "me@y.com", replyAll = false, quoteHeader = "On x, Alice wrote:")
        assertEquals(listOf("alice@x.com"), d.to)
        assertTrue(d.cc.isEmpty())
        assertEquals("Re: Plan", d.subject)
        assertEquals("On x, Alice wrote:\n> line one\n> line two", d.body)
        assertEquals("<m3@x.com>", d.inReplyTo)
        assertEquals(listOf("<m1@x.com>", "<m2@x.com>", "<m3@x.com>"), d.references)
    }

    @Test
    fun replyPrefersReplyTo() {
        val d = ReplyBuilder.reply(message(replyTo = listOf("list@x.com")), "me@y.com", false, "h")
        assertEquals(listOf("list@x.com"), d.to)
    }

    @Test
    fun replyAllKeepsOthersButNotMeOrTheSender() {
        val d = ReplyBuilder.reply(message(to = listOf("ME@y.com", "bob@z.com", "alice@x.com")), "me@y.com", true, "h")
        assertEquals(listOf("alice@x.com"), d.to)
        assertEquals(listOf("bob@z.com", "carol@z.com"), d.cc)
    }

    @Test
    fun replyToOwnMessageGoesToItsRecipients() {
        val d = ReplyBuilder.reply(message(from = "me@y.com", to = listOf("bob@z.com")), "me@y.com", false, "h")
        assertEquals(listOf("bob@z.com"), d.to)
    }

    @Test
    fun subjectPrefixesAreNotDuplicated() {
        assertEquals("RE: Plan", ReplyBuilder.replySubject("RE: Plan"))
        assertEquals("Fwd: Plan", ReplyBuilder.forwardSubject("Fwd: Plan"))
        assertEquals("Fwd: Plan", ReplyBuilder.forwardSubject("Plan"))
    }

    @Test
    fun forwardKeepsAttachmentsAndHasNoRecipients() {
        val d = ReplyBuilder.forward(message(), "---- Forwarded ----")
        assertTrue(d.to.isEmpty())
        assertEquals(1, d.attachments.size)
        assertTrue(d.body.startsWith("---- Forwarded ----\n\nline one"))
        assertEquals(null, d.inReplyTo)
    }

    @Test
    fun htmlBodyBecomesPlainTextWithLineBreaks() {
        val m = message().copy(textBody = null, htmlBody = "<html><head><title>t</title></head><body><p>Hello</p><p>World<br>there</p></body></html>")
        val lines = m.plainBody().lines().map { it.trim() }
        assertTrue(lines.contains("Hello"))
        assertTrue(lines.contains("World"))
        assertTrue(lines.contains("there"))
        assertTrue(!m.plainBody().contains("t\n") || !lines.contains("t"))
    }
}
