package com.kmail.thekami.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Pure rules and hashing for the App Lock PIN. */
object PinPolicy {
    enum class Problem { FORMAT, WEAK }

    const val ITERATIONS = 120_000
    private val COMMON = setOf("1212", "1004", "2000", "2001", "1122", "6969", "1313", "2580", "0852", "5683")

    fun check(pin: String): Problem? {
        if (pin.length !in 4..8 || !pin.all { it in '0'..'9' }) return Problem.FORMAT
        if (pin.all { it == pin[0] }) return Problem.WEAK
        val steps = pin.zipWithNext { a, b -> b - a }
        if (steps.all { it == 1 } || steps.all { it == -1 }) return Problem.WEAK
        if (pin in COMMON) return Problem.WEAK
        return null
    }

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hash(pin: String, salt: ByteArray, iterations: Int = ITERATIONS): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun matches(pin: String, salt: ByteArray, expected: ByteArray, iterations: Int = ITERATIONS): Boolean =
        MessageDigest.isEqual(hash(pin, salt, iterations), expected)

    /** Seconds to wait after [failures] wrong attempts in a row. */
    fun lockoutSeconds(failures: Int): Long =
        if (failures < 5) 0L else minOf(30L shl (failures - 5).coerceAtMost(10), 3600L)
}
