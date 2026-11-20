package com.kmail.thekami.data.transfer

import com.kmail.thekami.domain.model.AccountSettings
import com.kmail.thekami.domain.model.AuthMethod
import com.kmail.thekami.domain.model.ConnectionSecurity
import com.kmail.thekami.domain.model.ServerEndpoint
import com.kmail.thekami.domain.transfer.TransferAccount
import com.kmail.thekami.domain.transfer.TransferPackage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferCodecTest {
    private val codec = TransferCodec(iterations = 1_000)
    private val passphrase = "correct horse battery".toCharArray()

    private fun account(n: Int) = TransferAccount(
        emailAddress = "user$n@example.com",
        displayName = "User $n",
        colorArgb = 0xFF6750A4.toInt() + n,
        settings = AccountSettings(
            imap = ServerEndpoint("imap$n.example.com", 993, ConnectionSecurity.SSL_TLS, AuthMethod.NORMAL_PASSWORD, "user$n@example.com"),
            smtp = ServerEndpoint("smtp$n.example.com", 587, ConnectionSecurity.STARTTLS, AuthMethod.NORMAL_PASSWORD, "user$n@example.com"),
        ),
        imapPassword = "imap-p\u00e4ss$n".toCharArray(),
        smtpPassword = "smtp-\u09ac\u09be\u0982$n".toCharArray(),
    )

    private fun sample(count: Int = 2) = TransferPackage(1_700_000_000_000L, (1..count).map { account(it) })

    private fun assertSame(expected: TransferAccount, actual: TransferAccount) {
        assertEquals(expected.emailAddress, actual.emailAddress)
        assertEquals(expected.displayName, actual.displayName)
        assertEquals(expected.colorArgb, actual.colorArgb)
        assertEquals(expected.settings, actual.settings)
        assertArrayEquals(expected.imapPassword, actual.imapPassword)
        assertArrayEquals(expected.smtpPassword, actual.smtpPassword)
    }

    @Test
    fun roundTripKeepsEveryAccount() {
        val original = sample(3)
        val result = codec.decode(codec.encode(original, passphrase), passphrase)
        assertTrue(result is TransferDecodeResult.Ok)
        val got = (result as TransferDecodeResult.Ok).data
        assertEquals(original.createdAtMillis, got.createdAtMillis)
        assertEquals(3, got.accounts.size)
        original.accounts.indices.forEach { assertSame(original.accounts[it], got.accounts[it]) }
    }

    @Test
    fun singleAccountRoundTrips() {
        val original = sample(1)
        val got = (codec.decode(codec.encode(original, passphrase), passphrase) as TransferDecodeResult.Ok).data
        assertEquals(1, got.accounts.size)
        assertSame(original.accounts[0], got.accounts[0])
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyPackageIsRejected() {
        TransferPackage(0L, emptyList())
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
