package com.kmail.thekami.data.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinPolicyTest {
    @Test
    fun acceptsFourToEightDigitPinsThatAreNotEasy() {
        assertNull(PinPolicy.check("4829"))
        assertNull(PinPolicy.check("73920614"))
    }

    @Test
    fun rejectsWrongFormat() {
        listOf("", "123", "123456789", "48a29", "48 29", "٤٨٢٩").forEach {
            assertEquals(it, PinPolicy.Problem.FORMAT, PinPolicy.check(it))
        }
    }

    @Test
    fun rejectsEasyPins() {
        listOf("0000", "1111", "1234", "4321", "123456", "9876", "1212", "2580").forEach {
            assertEquals(it, PinPolicy.Problem.WEAK, PinPolicy.check(it))
        }
    }

    @Test
    fun hashIsDeterministicPerSaltAndDiffersAcrossSaltsAndPins() {
        val salt = ByteArray(16) { it.toByte() }
        val a = PinPolicy.hash("4829", salt, 1000)
        assertArrayEquals(a, PinPolicy.hash("4829", salt, 1000))
        assertNotEquals(a.toList(), PinPolicy.hash("4830", salt, 1000).toList())
        assertNotEquals(a.toList(), PinPolicy.hash("4829", ByteArray(16) { 9 }, 1000).toList())
        assertEquals(32, a.size)
    }

    @Test
    fun matchesAcceptsOnlyTheRightPin() {
        val salt = PinPolicy.newSalt()
        val expected = PinPolicy.hash("4829", salt, 1000)
        assertTrue(PinPolicy.matches("4829", salt, expected, 1000))
        assertFalse(PinPolicy.matches("0482", salt, expected, 1000))
    }

    @Test
    fun lockoutGrowsAfterFiveFailuresAndIsCapped() {
        assertEquals(0L, PinPolicy.lockoutSeconds(4))
        assertEquals(30L, PinPolicy.lockoutSeconds(5))
        assertEquals(60L, PinPolicy.lockoutSeconds(6))
        assertEquals(120L, PinPolicy.lockoutSeconds(7))
        assertEquals(3600L, PinPolicy.lockoutSeconds(30))
    }
}
