package com.kmail.thekami.mail

import com.kmail.thekami.domain.model.RemoteMessageHeader
import jakarta.mail.Flags
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage

internal object MessageMapper {
    /** Never throws on a malformed message: unreadable parts fall back to empty values. */
    fun toHeader(uid: Long, m: MimeMessage): RemoteMessageHeader {
        val from = runCatching { m.from?.firstOrNull() as? InternetAddress }.getOrNull()
        val flags = runCatching { m.flags }.getOrNull()
        val date = runCatching { m.receivedDate ?: m.sentDate }.getOrNull()
        return RemoteMessageHeader(
            uid = uid,
            messageIdHeader = runCatching { m.messageID }.getOrNull(),
            subject = runCatching { m.subject }.getOrNull().orEmpty(),
            fromName = from?.personal?.takeIf { it.isNotBlank() },
            fromAddress = from?.address.orEmpty(),
            snippet = "",
            receivedAtMillis = date?.time ?: 0L,
            isRead = flags?.contains(Flags.Flag.SEEN) == true,
            isStarred = flags?.contains(Flags.Flag.FLAGGED) == true,
            hasAttachments = runCatching { m.isMimeType("multipart/mixed") }.getOrDefault(false),
            sizeBytes = runCatching { m.size.toLong() }.getOrDefault(0L).coerceAtLeast(0L),
        )
    }
}
