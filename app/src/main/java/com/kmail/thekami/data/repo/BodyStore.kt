package com.kmail.thekami.data.repo

import android.content.Context
import com.kmail.thekami.data.security.EncryptedBlob
import com.kmail.thekami.data.security.SecretCipher
import com.kmail.thekami.data.security.SecretUnavailableException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Raw messages on disk, so a message opens instantly and offline. Every file is encrypted with the
 * Keystore key (AES-GCM, the message id as associated data). The oldest files are removed when the
 * folder grows past [maxBytes].
 */
@Singleton
class BodyStore(
    private val dir: File,
    private val cipher: SecretCipher,
    private val maxBytes: Long,
) {
    @Inject
    constructor(@ApplicationContext context: Context, cipher: SecretCipher) :
        this(File(context.filesDir, "bodies"), cipher, DEFAULT_MAX_BYTES)

    private val lock = Any()

    internal fun fileFor(id: Long) = File(dir, "$id.msg")

    fun has(id: Long): Boolean = fileFor(id).exists()

    fun sizeBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L

    suspend fun get(id: Long): ByteArray? = withContext(Dispatchers.IO) { read(id) }

    /** False if the message was too large or could not be written. */
    suspend fun put(id: Long, raw: ByteArray): Boolean = withContext(Dispatchers.IO) { write(id, raw) }

    suspend fun clear() = withContext(Dispatchers.IO) {
        synchronized(lock) { dir.listFiles()?.forEach { it.delete() } }
        Unit
    }

    private fun aad(id: Long) = "body:$id".toByteArray()

    private fun write(id: Long, raw: ByteArray): Boolean = synchronized(lock) {
        if (raw.size > maxBytes / 2) return false
        try {
            dir.mkdirs()
            val blob = cipher.encrypt(raw, aad(id))
            val out = ByteArrayOutputStream()
            out.write(blob.iv.size)
            out.write(blob.iv)
            out.write(blob.ciphertext)
            val tmp = File(dir, "$id.tmp")
            tmp.writeBytes(out.toByteArray())
            val ok = tmp.renameTo(fileFor(id))
            trim()
            ok
        } catch (e: SecretUnavailableException) {
            false
        } catch (e: IOException) {
            false
        }
    }

    private fun read(id: Long): ByteArray? = synchronized(lock) {
        val f = fileFor(id)
        if (!f.exists()) return null
        try {
            val raw = f.readBytes()
            val ivLength = if (raw.isEmpty()) 0 else raw[0].toInt() and 0xFF
            if (raw.size <= 1 + ivLength) {
                f.delete()
                return null
            }
            val blob = EncryptedBlob(raw.copyOfRange(1, 1 + ivLength), raw.copyOfRange(1 + ivLength, raw.size))
            val plain = cipher.decrypt(blob, aad(id))
            f.setLastModified(System.currentTimeMillis())
            plain
        } catch (e: SecretUnavailableException) {
            f.delete()
            null
        } catch (e: IOException) {
            null
        }
    }

    private fun trim() {
        val files = dir.listFiles { f -> f.name.endsWith(".msg") } ?: return
        var total = files.sumOf { it.length() }
        if (total <= maxBytes) return
        for (f in files.sortedBy { it.lastModified() }) {
            if (total <= maxBytes * 8 / 10) break
            total -= f.length()
            f.delete()
        }
    }

    companion object {
        const val DEFAULT_MAX_BYTES = 200L * 1024 * 1024
    }
}
