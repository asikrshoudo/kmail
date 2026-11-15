package com.kmail.thekami.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class ComposeBodyTest {
    @Test
    fun buildsBodyFromSignatureAndQuote() {
        assertEquals("\n\n", ComposeBody.initial("", ""))
        assertEquals("\n\n-- \nSent", ComposeBody.initial(" Sent ", ""))
        assertEquals("\n\n> q", ComposeBody.initial("", "> q"))
        assertEquals("\n\n-- \nS\n\nQ", ComposeBody.initial("S", "Q"))
    }
}
