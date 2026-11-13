package com.kmail.thekami.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MessageKeyTest {
    private fun key(
        account: String = "a1",
        folder: Long = 1,
        validity: Long = 5,
        uid: Long = 10,
    ) = MessageKey(account, folder, validity, uid)

    @Test
    fun acceptsBoundaryValues() {
        key(validity = 1, uid = 1)
        key(validity = MAX_IMAP_UINT32, uid = MAX_IMAP_UINT32)
    }

    @Test
    fun rejectsBlankAccount() {
        assertThrows(IllegalArgumentException::class.java) { key(account = " ") }
    }

    @Test
    fun rejectsZeroUid() {
        assertThrows(IllegalArgumentException::class.java) { key(uid = 0) }
    }

    @Test
    fun rejectsUidAboveUnsigned32() {
        assertThrows(IllegalArgumentException::class.java) { key(uid = MAX_IMAP_UINT32 + 1) }
    }

    @Test
    fun rejectsZeroUidValidity() {
        assertThrows(IllegalArgumentException::class.java) { key(validity = 0) }
    }

    @Test
    fun sameUidInDifferentAccountsIsDifferentIdentity() {
        assertNotEquals(key(account = "a1"), key(account = "a2"))
    }

    @Test
    fun sameUidAfterUidValidityResetIsDifferentIdentity() {
        assertNotEquals(key(validity = 5), key(validity = 6))
    }

    @Test
    fun identicalKeysAreEqual() {
        assertEquals(key(), key())
    }
}
