package com.kmail.thekami.domain.model

/** Largest value of an IMAP UID or UIDVALIDITY (unsigned 32-bit), hence Long. */
const val MAX_IMAP_UINT32: Long = 4_294_967_295L

/** Identity of a cached message: account, folder, UIDVALIDITY and UID together. */
data class MessageKey(
    val accountId: String,
    val folderId: Long,
    val uidValidity: Long,
    val uid: Long,
) {
    init {
        require(accountId.isNotBlank()) { "accountId must not be blank" }
        require(uidValidity in 1..MAX_IMAP_UINT32) { "uidValidity out of range" }
        require(uid in 1..MAX_IMAP_UINT32) { "uid out of range" }
    }
}
