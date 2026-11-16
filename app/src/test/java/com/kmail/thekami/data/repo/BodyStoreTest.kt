package com.kmail.thekami.data.repo

import com.kmail.thekami.data.security.EncryptedBlob
import com.kmail.thekami.data.security.SecretCipher
import com.kmail.thekami.data.security.SecretUnavailableException
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Not secure: only for tests. It binds the ciphertext to the associated data like AES-GCM does. */
internal class FakeCipher : SecretCipher {
    override fun encrypt(plain: ByteArray, aad: ByteArray) = EncryptedBlob(byteArrayOf(1, 2, 3), aad + plain)

    override fun decrypt(blob: EncryptedBlob, aad: ByteArray): ByteArray {
        val c = blob.ciphertext
        if (c.size < aad.size || !c.copyOfRange(0, aad.size).contentEquals(aad)) throw SecretUnavailableException("aad")
        return c.copyOfRange(aad.size, c.size)
    }
}

class BodyStoreTest {
    private val dir: File = Files.createTempDirectory("bodies").toFile()
    private fun store(max: Long = 10_000_000) = BodyStore(dir, FakeCipher(), max)

    @Test
    fun roundTripAndMissing() = runTest {
        val s = store()
        assertNull(s.get(1))
        assertTrue(s.put(1, "hello".toByteArray()))
        assertTrue(s.has(1))
        assertArrayEquals("hello".toByteArray(), s.get(1))
    }

    @Test
    fun aFileSwappedToAnotherMessageIsRejected() = runTest {
        val s = store()
        s.put(1, "secret".toByteArray())
        s.fileFor(1).copyTo(s.fileFor(2))
        assertNull(s.get(2))
        assertFalse(s.has(2))
    }

    @Test
    fun corruptFilesReadAsMissing() = runTest {
        val s = store()
        s.put(1, "x".toByteArray())
        s.fileFor(1).writeBytes(byteArrayOf(9, 1))
        assertNull(s.get(1))
    }

    @Test
    fun oldestFilesAreEvictedWhenTheLimitIsPassed() = runTest {
        val s = store(max = 1000)
        val body = ByteArray(350) { 7 }
        s.put(1, body)
        s.fileFor(1).setLastModified(1_000)
        s.put(2, body)
        s.fileFor(2).setLastModified(2_000)
        s.put(3, body)
        assertFalse(s.has(1))
        assertTrue(s.has(2))
        assertTrue(s.has(3))
    }

    @Test
    fun hugeMessagesAreNotStoredAndClearRemovesEverything() = runTest {
        val s = store(max = 1000)
        assertFalse(s.put(1, ByteArray(600)))
        s.put(2, ByteArray(10))
        s.clear()
        assertFalse(s.has(2))
        assertNotNull(dir)
    }
}
