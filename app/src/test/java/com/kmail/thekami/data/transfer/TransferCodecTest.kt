package com.kmail.thekami.data.transfer

import com.kmail.thekami.domain.model.AccountSettings
import com.kmail.thekami.domain.model.AuthMethod
import com.kmail.thekami.domain.model.ConnectionSecurity
import com.kmail.thekami.domain.model.ServerEndpoint
import com.kmail.thekami.domain.transfer.TransferPackage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferCodecTest {
    private val codec = TransferCodec(iterations = 1_000)
    private val passphrase = "correct horse battery".toCharArray()

    private fun sample() = TransferPackage(
        createdAtMillis = 1_700_000_000_000L,
        emailAddress = "user@example.com",
        displayName = "User Name",
        colorArgb = 0xFF6750A4.toInt(),
        settings = AccountSettings(
            imap = ServerEndpoint("imap.example.com", 993, ConnectionSecurity.SSL_TLS, AuthMethod.NORMAL_PASSWORD, "user@example.com"),
            smtp = ServerEndpoint("smtp.example.com", 587, ConnectionSecurity.STARTTLS, AuthMethod.NORMAL_PASSWORD, "user@example.com"),
        ),
        imapPassword = "imap-p\u00e4ss".toCharArray(),
        smtpPassword = "smtp-\u09ac\u09be\u0982".toCharArray(),
    )

    @Test
    fun roundTripKeepsEverything() {
        val original = sample()
        val result = codec.decode(codec.encode(original, passphrase), passphrase)
        assertTrue(result is TransferDecodeResult.Ok)
        val got = (result as TransferDecodeResult.Ok).data
        assertEquals(original.createdAtMillis, got.createdAtMillis)
        assertEquals(original.emailAddress, got.emailAddress)
        assertEquals(original.displayName, got.displayName)
        assertEquals(original.colorArgb, got.colorArgb)
        assertEquals(original.settings, got.settings)
        assertArrayEquals(original.imapPassword, got.imapPassword)
        assertArrayEquals(original.smtpPassword, got.smtpPassword)
    }

    @Test
    fun wrongPassphraseFails() {
        val blob = codec.encode(sample(), passphrase)
        assertEquals(TransferDecodeResult.Failed, codec.decode(blob, "wrong passphrase".toCharArray()))
    }

    @Test
    fun changedByteFails() {
        val blob = codec.encode(sample(), passphrase)
        blob[blob.size - 1] = (blob[blob.size - 1].toInt() xor 1).toByte()
        assertEquals(TransferDecodeResult.Failed, codec.decode(blob, passphrase))
    }

    @Test
    fun truncatedOrEmptyDataFails() {
        val blob = codec.encode(sample(), passphrase)
        assertEquals(TransferDecodeResult.Failed, codec.decode(blob.copyOf(20), passphrase))
        assertEquals(TransferDecodeResult.Failed, codec.decode(ByteArray(0), passphrase))
    }

    @Test
    fun outputIsDifferentEachTimeAndHidesContent() {
        val first = codec.encode(sample(), passphrase)
        val second = codec.encode(sample(), passphrase)
        assertFalse(first.contentEquals(second))
        assertFalse(String(first, Charsets.ISO_8859_1).contains("example.com"))
    }
}
