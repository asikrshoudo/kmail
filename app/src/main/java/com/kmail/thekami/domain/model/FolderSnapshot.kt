package com.kmail.thekami.domain.model

/** Result of a header fetch. [uidValidity] lets the caller detect a server-side UID reset. */
data class FolderSnapshot(
    val uidValidity: Long,
    val uidNext: Long?,
    val headers: List<RemoteMessageHeader>,
) {
    init {
        require(uidValidity in 1..MAX_IMAP_UINT32) { "uidValidity out of range" }
        require(uidNext == null || uidNext in 1..MAX_IMAP_UINT32) { "uidNext out of range" }
    }
}
