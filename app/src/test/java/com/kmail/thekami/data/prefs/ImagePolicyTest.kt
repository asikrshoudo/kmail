package com.kmail.thekami.data.prefs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagePolicyTest {
    private val trusted = setOf("news@shop.com")

    @Test
    fun alwaysAndNeverIgnoreTheSender() {
        assertTrue(ImagePolicy.initialAllow(ImageMode.ALWAYS, emptySet(), null))
        assertFalse(ImagePolicy.initialAllow(ImageMode.NEVER, trusted, "news@shop.com"))
    }

    @Test
    fun askLoadsOnlyForTrustedSendersIgnoringCaseAndSpaces() {
        assertTrue(ImagePolicy.initialAllow(ImageMode.ASK, trusted, " News@Shop.com "))
        assertFalse(ImagePolicy.initialAllow(ImageMode.ASK, trusted, "other@x.com"))
        assertFalse(ImagePolicy.initialAllow(ImageMode.ASK, trusted, null))
    }
}
