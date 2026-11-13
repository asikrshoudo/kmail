package com.kmail.thekami.data.security

import com.kmail.thekami.data.db.CredentialDao
import com.kmail.thekami.data.db.CredentialEntity
import com.kmail.thekami.domain.model.CredentialPurpose
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/** Uses a reversible fake cipher: it checks CredentialStore's logic, not Keystore encryption. */
class CredentialStoreTest {

    private class FakeCipher : SecretCipher {
        var failDecrypt = false
        private val boundAad = mutableMapOf<List<Byte>, List<Byte>>()

        override fun encrypt(plain: ByteArray, aad: ByteArray): EncryptedBlob {
            val cipherText = plain.reversedArray()
            boundAad[cipherText.toList()] = aad.toList()
            return EncryptedBlob(byteArrayOf(1), cipherText)
        }

        override fun decrypt(blob: EncryptedBlob, aad: ByteArray): ByteArray {
            if (failDecrypt || boundAad[blob.ciphertext.toList()] != aad.toList()) {
                throw SecretUnavailableException("fake failure")
            }
            return blob.ciphertext.reversedArray()
        }
    }

    private class FakeDao : CredentialDao {
        val rows = mutableMapOf<Pair<String, CredentialPurpose>, CredentialEntity>()

        override suspend fun upsert(credential: CredentialEntity) {
            rows[credential.accountId to credential.purpose] = credential
        }

        override suspend fun find(accountId: String, purpose: CredentialPurpose) =
            rows[accountId to purpose]

        override suspend fun deleteForAccount(accountId: String) {
            rows.keys.removeAll { it.first == accountId }
        }
    }

    private val cipher = FakeCipher()
    private val dao = FakeDao()
    private val store = CredentialStore(dao, cipher)

    @Test
    fun saveThenLoadRoundTripsNonAsciiPassword() = runBlocking {
        val secret = "pässwörd✓".toCharArray()
        store.save("a1", CredentialPurpose.IMAP, secret.copyOf())
        assertArrayEquals(secret, store.load("a1", CredentialPurpose.IMAP))
    }

    @Test
    fun loadReturnsNullWhenNothingStored() = runBlocking {
        assertNull(store.load("a1", CredentialPurpose.SMTP))
    }

    @Test
    fun imapAndSmtpSecretsAreIndependent() = runBlocking {
        store.save("a1", CredentialPurpose.IMAP, "one".toCharArray())
        store.save("a1", CredentialPurpose.SMTP, "two".toCharArray())
        assertArrayEquals("one".toCharArray(), store.load("a1", CredentialPurpose.IMAP))
        assertArrayEquals("two".toCharArray(), store.load("a1", CredentialPurpose.SMTP))
    }

    @Test
    fun blobCopiedToAnotherAccountCannotBeDecrypted() {
        runBlocking {
            store.save("a1", CredentialPurpose.IMAP, "secret".toCharArray())
            dao.rows["a2" to CredentialPurpose.IMAP] = dao.rows.getValue("a1" to CredentialPurpose.IMAP)
        }
        assertThrows(SecretUnavailableException::class.java) {
            runBlocking { store.load("a2", CredentialPurpose.IMAP) }
        }
    }

    @Test
    fun decryptFailureIsReportedNotHidden() {
        runBlocking { store.save("a1", CredentialPurpose.IMAP, "secret".toCharArray()) }
        cipher.failDecrypt = true
        assertThrows(SecretUnavailableException::class.java) {
            runBlocking { store.load("a1", CredentialPurpose.IMAP) }
        }
    }

    @Test
    fun deleteRemovesAllSecretsOfAccount() = runBlocking {
        store.save("a1", CredentialPurpose.IMAP, "one".toCharArray())
        store.save("a1", CredentialPurpose.SMTP, "two".toCharArray())
        store.delete("a1")
        assertNull(store.load("a1", CredentialPurpose.IMAP))
        assertNull(store.load("a1", CredentialPurpose.SMTP))
    }
}
