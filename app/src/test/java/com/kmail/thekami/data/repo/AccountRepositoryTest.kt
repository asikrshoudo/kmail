package com.kmail.thekami.data.repo

import com.kmail.thekami.data.db.AccountDao
import com.kmail.thekami.data.db.AccountEntity
import com.kmail.thekami.data.db.CredentialDao
import com.kmail.thekami.data.db.CredentialEntity
import com.kmail.thekami.data.security.CredentialStore
import com.kmail.thekami.data.security.EncryptedBlob
import com.kmail.thekami.data.security.SecretCipher
import com.kmail.thekami.data.security.SecretUnavailableException
import com.kmail.thekami.domain.model.AccountSettings
import com.kmail.thekami.domain.model.AddAccountResult
import com.kmail.thekami.domain.model.AuthMethod
import com.kmail.thekami.domain.model.ConnectionSecurity
import com.kmail.thekami.domain.model.CredentialPurpose
import com.kmail.thekami.domain.model.NewAccount
import com.kmail.thekami.domain.model.ServerEndpoint
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uses in-memory fakes: it checks repository logic, not Room or Keystore. */
class AccountRepositoryTest {

    private class FakeAccountDao : AccountDao {
        val rows = linkedMapOf<String, AccountEntity>()
        override fun observeAll() = flowOf(rows.values.toList())
        override suspend fun getById(id: String) = rows[id]
        override suspend fun findByEmail(email: String) =
            rows.values.firstOrNull { it.emailAddress.equals(email, ignoreCase = true) }
        override suspend fun insert(account: AccountEntity) { rows[account.id] = account }
        override suspend fun deleteById(id: String) { rows.remove(id) }
    }

    private class FakeCredentialDao : CredentialDao {
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

    private class PassthroughCipher : SecretCipher {
        var failEncrypt = false
        override fun encrypt(plain: ByteArray, aad: ByteArray): EncryptedBlob {
            if (failEncrypt) throw SecretUnavailableException("fake failure")
            return EncryptedBlob(byteArrayOf(0), plain.copyOf())
        }
        override fun decrypt(blob: EncryptedBlob, aad: ByteArray) = blob.ciphertext.copyOf()
    }

    private val accountDao = FakeAccountDao()
    private val credentialDao = FakeCredentialDao()
    private val cipher = PassthroughCipher()
    private val store = CredentialStore(credentialDao, cipher)
    private val repository = AccountRepository(accountDao, store)

    private fun endpoint(host: String, port: Int, security: ConnectionSecurity) =
        ServerEndpoint(host, port, security, AuthMethod.NORMAL_PASSWORD, "me@example.com")

    private fun draft(email: String = "Me@Example.com") = NewAccount(
        emailAddress = email,
        displayName = " ",
        colorArgb = 0xFF4F5BD5.toInt(),
        settings = AccountSettings(
            imap = endpoint("imap.example.com", 993, ConnectionSecurity.SSL_TLS),
            smtp = endpoint("smtp.example.com", 587, ConnectionSecurity.STARTTLS),
        ),
        imapPassword = "imap-pass".toCharArray(),
        smtpPassword = "smtp-pass".toCharArray(),
    )

    @Test
    fun addsAccountWithNormalizedEmailAndEncryptedSecrets() = runBlocking {
        val result = repository.addAccount(draft())
        val account = (result as AddAccountResult.Added).account
        assertEquals("me@example.com", account.emailAddress)
        assertEquals("me@example.com", account.displayName)
        assertEquals(account, repository.getAccount(account.id))
        assertArrayEquals("imap-pass".toCharArray(), store.load(account.id, CredentialPurpose.IMAP))
        assertArrayEquals("smtp-pass".toCharArray(), store.load(account.id, CredentialPurpose.SMTP))
    }

    @Test
    fun rejectsInvalidEmailWithoutStoringAnything() = runBlocking {
        assertEquals(AddAccountResult.InvalidEmail, repository.addAccount(draft("not-an-email")))
        assertTrue(accountDao.rows.isEmpty())
        assertTrue(credentialDao.rows.isEmpty())
    }

    @Test
    fun rejectsDuplicateEmailIgnoringCase() = runBlocking {
        repository.addAccount(draft("me@example.com"))
        assertEquals(AddAccountResult.AlreadyExists, repository.addAccount(draft("ME@EXAMPLE.COM")))
        assertEquals(1, accountDao.rows.size)
    }

    @Test
    fun credentialFailureLeavesNoAccountBehind() = runBlocking {
        cipher.failEncrypt = true
        assertEquals(AddAccountResult.CredentialStorageFailed, repository.addAccount(draft()))
        assertTrue(accountDao.rows.isEmpty())
        assertTrue(credentialDao.rows.isEmpty())
    }

    @Test
    fun removeDeletesAccountAndSecrets() = runBlocking {
        val account = (repository.addAccount(draft()) as AddAccountResult.Added).account
        repository.removeAccount(account.id)
        assertNull(repository.getAccount(account.id))
        assertNull(store.load(account.id, CredentialPurpose.IMAP))
        assertNull(store.load(account.id, CredentialPurpose.SMTP))
    }
}
