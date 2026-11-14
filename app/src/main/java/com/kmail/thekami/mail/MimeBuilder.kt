package com.kmail.thekami.mail

import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MailDateFormat
import jakarta.mail.internet.MimeUtility
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.Date
import java.util.UUID

/**
 * Writes an RFC 5322 message by hand. It does not use MimeMessage.setContent(), which depends on
 * the activation mailcap registry. Bcc recipients are never written into the headers.
 */
object MimeBuilder {
    class Attachment(val fileName: String, val mimeType: String, val bytes: ByteArray)

    class Draft(
        val from: Mailbox,
        val to: List<String>,
        val cc: List<String>,
        val subject: String,
        val body: String,
        val attachments: List<Attachment>,
        val inReplyTo: String?,
        val references: List<String>,
    )

    private const val CRLF = "\r\n"

    fun build(
        draft: Draft,
        now: Date = Date(),
        messageIdSeed: String = UUID.randomUUID().toString(),
    ): ByteArray {
        val out = ByteArrayOutputStream()
        fun header(name: String, value: String) = out.write("$name: $value$CRLF".toByteArray(Charsets.UTF_8))

        val domain = draft.from.address.substringAfter('@', "localhost").ifEmpty { "localhost" }
        header("Date", MailDateFormat().format(now))
        header("From", address(draft.from.address, draft.from.name))
        if (draft.to.isNotEmpty()) header("To", MimeUtility.fold(4, draft.to.joinToString(", ") { address(it, null) }))
        if (draft.cc.isNotEmpty()) header("Cc", MimeUtility.fold(4, draft.cc.joinToString(", ") { address(it, null) }))
        header("Subject", MimeUtility.encodeText(draft.subject.replace(Regex("[\\r\\n]+"), " "), "UTF-8", null))
        header("Message-ID", "<$messageIdSeed@$domain>")
        draft.inReplyTo?.takeIf { it.isNotBlank() }?.let { header("In-Reply-To", it) }
        if (draft.references.isNotEmpty()) header("References", MimeUtility.fold(12, draft.references.joinToString(" ")))
        header("MIME-Version", "1.0")

        val bodyBytes = encode(normalizeNewlines(draft.body).toByteArray(Charsets.UTF_8), "quoted-printable")
        if (draft.attachments.isEmpty()) {
            header("Content-Type", "text/plain; charset=UTF-8")
            header("Content-Transfer-Encoding", "quoted-printable")
            out.write(CRLF.toByteArray())
            out.write(bodyBytes)
            out.write(CRLF.toByteArray())
        } else {
            val boundary = "=_kmail_" + UUID.randomUUID().toString().replace("-", "")
            header("Content-Type", "multipart/mixed; boundary=\"$boundary\"")
            out.write(CRLF.toByteArray())
            out.write("--$boundary$CRLF".toByteArray())
            out.write(("Content-Type: text/plain; charset=UTF-8$CRLF" +
                "Content-Transfer-Encoding: quoted-printable$CRLF$CRLF").toByteArray())
            out.write(bodyBytes)
            out.write(CRLF.toByteArray())
            for (a in draft.attachments) {
                val type = if (a.mimeType.contains('/')) a.mimeType else "application/octet-stream"
                out.write("--$boundary$CRLF".toByteArray())
                out.write(
                    ("Content-Type: $type; ${param("name", a.fileName)}$CRLF" +
                        "Content-Disposition: attachment; ${param("filename", a.fileName)}$CRLF" +
                        "Content-Transfer-Encoding: base64$CRLF$CRLF").toByteArray(),
                )
                out.write(encode(a.bytes, "base64"))
                out.write(CRLF.toByteArray())
            }
            out.write("--$boundary--$CRLF".toByteArray())
        }
        return out.toByteArray()
    }

    private fun address(address: String, name: String?): String =
        InternetAddress(address, name?.takeIf { it.isNotBlank() }, "UTF-8").toString()

    private fun normalizeNewlines(text: String) = text.replace("\r\n", "\n").replace("\r", "\n").replace("\n", CRLF)

    private fun encode(bytes: ByteArray, encoding: String): ByteArray {
        val sink = ByteArrayOutputStream()
        MimeUtility.encode(sink, encoding).use { it.write(bytes) }
        return sink.toByteArray()
    }

    /** RFC 2231 for names with non-ASCII characters. */
    private fun param(name: String, value: String): String {
        val plain = value.all { it.code in 32..126 && it != '"' && it != '\\' }
        return if (plain) {
            "$name=\"$value\""
        } else {
            "$name*=UTF-8''" + URLEncoder.encode(value, "UTF-8").replace("+", "%20")
        }
    }
}
