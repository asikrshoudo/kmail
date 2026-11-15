package com.kmail.thekami.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipientTokenTest {
    @Test
    fun currentIsTheTextAfterTheLastSeparator() {
        assertEquals("bo", RecipientToken.current("a@x.com, bo"))
        assertEquals("bo", RecipientToken.current("a@x.com,bo"))
        assertEquals("bo", RecipientToken.current("a@x.com; bo"))
        assertEquals("bo", RecipientToken.current("bo"))
        assertEquals("", RecipientToken.current(""))
        assertEquals("", RecipientToken.current("a@x.com, "))
    }

    @Test
    fun replaceSwapsOnlyTheCurrentToken() {
        assertEquals("a@x.com, bob@y.com, ", RecipientToken.replace("a@x.com, bo", "bob@y.com"))
        assertEquals("bob@y.com, ", RecipientToken.replace("bo", "bob@y.com"))
        assertEquals("a@x.com,bob@y.com, ", RecipientToken.replace("a@x.com,bo", "bob@y.com"))
    }
}
