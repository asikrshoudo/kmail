package com.kmail.thekami.ui.reader

import com.kmail.thekami.data.db.MessageEntity

/** Subject-based threading: "Re: Plan" and "Fwd: Plan" belong to the conversation "plan". */
object ThreadSubject {
    private val PREFIX = Regex("^\\s*(re|fwd?|aw|wg|sv|vs|antw|rv|tr|enc)\\s*:\\s*", RegexOption.IGNORE_CASE)

    fun normalize(subject: String): String {
        var s = subject.trim()
        while (true) {
            val stripped = PREFIX.replace(s, "")
            if (stripped == s) break
            s = stripped
        }
        return s.replace(Regex("\\s+"), " ").trim().lowercase()
    }
}

data class ThreadItem(
    val id: Long,
    val senderName: String,
    val senderAddress: String,
    val isOwn: Boolean,
    val receivedAtMillis: Long,
    val snippet: String,
    val isRead: Boolean,
)

object ThreadBuilder {
    private const val MAX = 40

    /** The opened message is always in the result. Oldest first. */
    fun build(opened: MessageEntity, candidates: List<MessageEntity>, ownEmail: String): List<ThreadItem> {
        val core = ThreadSubject.normalize(opened.subject)
        val same = if (core.isEmpty()) emptyList() else candidates.filter { ThreadSubject.normalize(it.subject) == core }
        var all = (same + opened).distinctBy { it.id }.sortedBy { it.receivedAtMillis }
        if (all.size > MAX) {
            val others = all.filter { it.id != opened.id }.takeLast(MAX - 1)
            all = (others + opened).sortedBy { it.receivedAtMillis }
        }
        return all.map {
            ThreadItem(
                id = it.id,
                senderName = it.fromName?.takeIf { n -> n.isNotBlank() } ?: it.fromAddress,
                senderAddress = it.fromAddress,
                isOwn = ownEmail.isNotBlank() && it.fromAddress.equals(ownEmail, ignoreCase = true),
                receivedAtMillis = it.receivedAtMillis,
                snippet = it.snippet,
                isRead = it.isRead,
            )
        }
    }
}
