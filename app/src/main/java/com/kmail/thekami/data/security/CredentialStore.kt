package com.kmail.thekami.data.security

import com.kmail.thekami.data.db.CredentialDao
import com.kmail.thekami.data.db.CredentialEntity
import com.kmail.thekami.domain.model.CredentialPurpose
import java.nio.ByteBuffer
import java.nio.CharBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Stores mailbox passwords encrypted. Callers must zero any returned [CharArray] after use. */
@Singleton
class CredentialStore @Inject constructor(
    private val dao: CredentialDao,
    private val cipher: SecretCipher,
) {
    suspend fun save(accountId: String, purpose: CredentialPurpose, secret: CharArray) =
        withContext(Dispatchers.IO) {
            val plain = secret.toUtf8Bytes()
            try {
                val blob = cipher.encrypt(plain, aad(accountId, purpose))
                dao.upsert(
                    CredentialEntity(
                        accountId = accountId,
                        purpose = purpose,
                        iv = blob.iv,
                        ciphertext = blob.ciphertext,
                        updatedAtMillis = System.currentTimeMillis(),
                    ),
                )
            } finally {
                plain.fill(0)
            }
        }

    /**
     * @return null if nothing is stored.
     * @throws SecretUnavailableException if the stored secret cannot be decrypted.
     */
    suspend fun load(accountId: String, purpose: CredentialPurpose): CharArray? =
        withContext(Dispatchers.IO) {
            val row = dao.find(accountId, purpose) ?: return@withContext null
            val plain = cipher.decrypt(EncryptedBlob(row.iv, row.ciphertext), aad(accountId, purpose))
            try {
                plain.toChars()
            } finally {
                plain.fill(0)
            }
        }

    suspend fun delete(accountId: String) = dao.deleteForAccount(accountId)

    private fun aad(accountId: String, purpose: CredentialPurpose): ByteArray =
        "$accountId|${purpose.name}".toByteArray(Charsets.UTF_8)

    private fun CharArray.toUtf8Bytes(): ByteArray {
        val buffer = Charsets.UTF_8.encode(CharBuffer.wrap(this))
        val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
        if (buffer.hasArray()) buffer.array().fill(0)
        return bytes
    }

    private fun ByteArray.toChars(): CharArray {
        val buffer = Charsets.UTF_8.decode(ByteBuffer.wrap(this))
        val chars = CharArray(buffer.remaining()).also { buffer.get(it) }
        if (buffer.hasArray()) buffer.array().fill('\u0000')
        return chars
    }
}
