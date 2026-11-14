package com.kmail.thekami.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipientsTest {
    @Test
    fun splitsOnCommasSemicolonsAndSpacesAndNormalizes() {
        val r = Recipients.parse("A@x.com, b@y.com; <c@z.com>  a@X.com")
        assertEquals(listOf("a@x.com", "b@y.com", "c@z.com"), r.valid)
        assertEquals(emptyList<String>(), r.invalid)
    }

    @Test
    fun reportsInvalidTokens() {
        val r = Recipients.parse("good@x.com, nonsense, a@b")
        assertEquals(listOf("good@x.com"), r.valid)
        assertEquals(listOf("nonsense", "a@b"), r.invalid)
    }

    @Test
    fun emptyTextHasNoRecipients() {
        val r = Recipients.parse("  ,; ")
        assertEquals(emptyList<String>(), r.valid)
        assertEquals(emptyList<String>(), r.invalid)
    }
}
