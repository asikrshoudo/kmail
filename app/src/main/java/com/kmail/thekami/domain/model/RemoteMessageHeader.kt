package com.kmail.thekami.domain.model

/** Provider-independent message summary. [snippet] may be empty if the provider has none. */
data class RemoteMessageHeader(
    val uid: Long,
    val messageIdHeader: String?,
    val subject: String,
    val fromName: String?,
    val fromAddress: String,
    val snippet: String,
    val receivedAtMillis: Long,
    val isRead: Boolean,
    val isStarred: Boolean,
    val hasAttachments: Boolean,
    val sizeBytes: Long,
) {
    init {
        require(uid in 1..MAX_IMAP_UINT32) { "uid out of range" }
    }
}
