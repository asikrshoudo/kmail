package com.kmail.thekami.mail

data class ReplyDraft(
    val to: List<String>,
    val cc: List<String>,
    val subject: String,
    val body: String,
    val inReplyTo: String?,
    val references: List<String>,
    val attachments: List<ParsedAttachment>,
)

object ReplyBuilder {
    private val RE = Regex("^\\s*re\\s*:", RegexOption.IGNORE_CASE)
    private val FWD = Regex("^\\s*fwd?\\s*:", RegexOption.IGNORE_CASE)
    private const val MAX_QUOTE = 20_000

    fun replySubject(subject: String) = if (RE.containsMatchIn(subject)) subject.trim() else "Re: ${subject.trim()}"

    fun forwardSubject(subject: String) = if (FWD.containsMatchIn(subject)) subject.trim() else "Fwd: ${subject.trim()}"

    fun reply(message: ParsedMessage, ownAddress: String, replyAll: Boolean, quoteHeader: String): ReplyDraft {
        val primary = (message.replyTo.firstOrNull() ?: message.from)?.address
        // Replying to a message you sent yourself goes to its original recipients.
        val to = if (primary != null && !primary.equals(ownAddress, ignoreCase = true)) {
            listOf(primary)
        } else {
            message.to.map { it.address }.ifEmpty { listOfNotNull(primary) }
        }
        val taken = to.map { it.lowercase() }.toSet()
        val cc = if (replyAll) {
            (message.to + message.cc).map { it.address }
                .filter { !it.equals(ownAddress, ignoreCase = true) && it.lowercase() !in taken }
                .distinctBy { it.lowercase() }
        } else {
            emptyList()
        }
        val quoted = message.plainBody().take(MAX_QUOTE).lines().joinToString("\n") { "> $it" }
        return ReplyDraft(
            to = to,
            cc = cc,
            subject = replySubject(message.subject),
            body = "$quoteHeader\n$quoted",
            inReplyTo = message.messageId,
            references = (message.references + listOfNotNull(message.messageId)).distinct().takeLast(20),
            attachments = emptyList(),
        )
    }

    fun forward(message: ParsedMessage, header: String): ReplyDraft = ReplyDraft(
        to = emptyList(),
        cc = emptyList(),
        subject = forwardSubject(message.subject),
        body = header + "\n\n" + message.plainBody().take(MAX_QUOTE),
        inReplyTo = null,
        references = emptyList(),
        attachments = message.attachments,
    )
}
