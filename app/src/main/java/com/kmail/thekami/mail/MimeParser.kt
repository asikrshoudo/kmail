package com.kmail.thekami.mail

import jakarta.mail.Address
import jakarta.mail.Message
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.internet.ContentType
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.internet.MimePart
import jakarta.mail.internet.MimePartDataSource
import jakarta.mail.internet.MimeUtility
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.util.Locale
import java.util.Properties
import org.jsoup.Jsoup

data class Mailbox(val name: String?, val address: String) {
    fun display() = if (name != null) "$name <$address>" else address
}

class ParsedAttachment(val fileName: String, val mimeType: String, val bytes: ByteArray) {
    val sizeBytes: Long get() = bytes.size.toLong()
}

class InlineImage(val mimeType: String, val bytes: ByteArray)

data class ParsedMessage(
    val subject: String,
    val from: Mailbox?,
    val to: List<Mailbox>,
    val cc: List<Mailbox>,
    val replyTo: List<Mailbox>,
    val dateMillis: Long?,
    val messageId: String?,
    val inReplyTo: String?,
    val references: List<String>,
    val htmlBody: String?,
    val textBody: String?,
    val inlineImages: Map<String, InlineImage>,
    val attachments: List<ParsedAttachment>,
) {
    /** Short single-line preview for the message list. */
    fun previewText(): String {
        val source = textBody ?: htmlBody?.let { Jsoup.parse(it).text() } ?: ""
        return source.replace(Regex("\\s+"), " ").trim().take(160)
    }
}

/**
 * Parses raw RFC 822 bytes. It never calls Part.getContent(), which needs the activation
 * mailcap registry; it reads decoded streams and picks the charset itself.
 */
object MimeParser {
    private const val MAX_DEPTH = 12
    private const val MAX_PARTS = 200

    private class Collector {
        var html: String? = null
        var text: String? = null
        val attachments = mutableListOf<ParsedAttachment>()
        val inline = LinkedHashMap<String, InlineImage>()
        var parts = 0
    }

    fun parse(raw: ByteArray): ParsedMessage {
        val message = MimeMessage(Session.getInstance(Properties()), ByteArrayInputStream(raw))
        val collector = Collector()
        walk(message, collector, 0)
        return ParsedMessage(
            subject = runCatching { message.subject }.getOrNull().orEmpty(),
            from = addresses { message.from }.firstOrNull(),
            to = addresses { message.getRecipients(Message.RecipientType.TO) },
            cc = addresses { message.getRecipients(Message.RecipientType.CC) },
            replyTo = addresses { message.replyTo },
            dateMillis = runCatching { message.sentDate?.time }.getOrNull(),
            messageId = runCatching { message.messageID }.getOrNull(),
            inReplyTo = runCatching { message.getHeader("In-Reply-To", null) }.getOrNull()
                ?.trim()?.ifEmpty { null },
            references = runCatching { message.getHeader("References", null) }.getOrNull().orEmpty()
                .split(Regex("\\s+")).filter { it.isNotBlank() },
            htmlBody = collector.html,
            textBody = collector.text,
            inlineImages = collector.inline,
            attachments = collector.attachments,
        )
    }

    private fun addresses(block: () -> Array<Address>?): List<Mailbox> =
        runCatching { block() }.getOrNull().orEmpty().mapNotNull { a ->
            val ia = a as? InternetAddress ?: return@mapNotNull null
            val address = ia.address?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Mailbox(ia.personal?.takeIf { it.isNotBlank() }, address)
        }

    private fun walk(part: MimePart, c: Collector, depth: Int) {
        if (depth > MAX_DEPTH || ++c.parts > MAX_PARTS) return
        if (part.isMimeType("multipart/*")) {
            val multipart = MimeMultipart(MimePartDataSource(part))
            for (i in 0 until multipart.count) {
                // One broken part must not hide the rest of the message.
                runCatching { walk(multipart.getBodyPart(i) as MimePart, c, depth + 1) }
            }
            return
        }
        val disposition = runCatching { part.disposition }.getOrNull()?.lowercase(Locale.ROOT)
        val fileName = runCatching { part.fileName }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { MimeUtility.decodeText(it) }.getOrDefault(it) }
        val type = baseType(part)
        val isAttachment = disposition == Part.ATTACHMENT || fileName != null
        when {
            !isAttachment && type == "text/html" && c.html == null -> c.html = readText(part)
            !isAttachment && type == "text/plain" && c.text == null -> c.text = readText(part)
            else -> {
                val cid = (part as? MimeBodyPart)?.contentID?.trim()
                    ?.removePrefix("<")?.removeSuffix(">")?.takeIf { it.isNotEmpty() }
                val bytes = part.inputStream.use { it.readBytes() }
                if (cid != null && type.startsWith("image/")) c.inline[cid] = InlineImage(type, bytes)
                if (cid == null || disposition == Part.ATTACHMENT) {
                    c.attachments += ParsedAttachment(fileName ?: defaultName(type), type, bytes)
                }
            }
        }
    }

    private fun baseType(part: MimePart): String =
        runCatching { ContentType(part.contentType).baseType.lowercase(Locale.ROOT) }
            .getOrDefault("application/octet-stream")

    private fun readText(part: MimePart): String {
        val bytes = part.inputStream.use { it.readBytes() }
        val name = runCatching { ContentType(part.contentType).getParameter("charset") }.getOrNull()
        val charset = name?.let { runCatching { Charset.forName(MimeUtility.javaCharset(it)) }.getOrNull() }
            ?: Charsets.UTF_8
        return String(bytes, charset)
    }

    private fun defaultName(type: String) = when (type) {
        "text/calendar" -> "invite.ics"
        "message/rfc822" -> "message.eml"
        "application/pdf" -> "document.pdf"
        "image/png" -> "image.png"
        "image/jpeg" -> "image.jpg"
        "text/plain" -> "text.txt"
        else -> "attachment"
    }
}
