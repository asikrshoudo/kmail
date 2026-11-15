package com.kmail.thekami.data.drafts

import android.content.Context
import com.kmail.thekami.data.security.EncryptedBlob
import com.kmail.thekami.data.security.SecretCipher
import com.kmail.thekami.data.security.SecretUnavailableException
import com.kmail.thekami.domain.model.Draft
import com.kmail.thekami.domain.model.DraftSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Drafts are stored on this device only, one file each, encrypted with the Keystore key
 * (AES-GCM, the draft id as associated data). They are not uploaded to the mail server.
 */
@Singleton
class DraftStore @Inject constructor(
    @ApplicationContext context: Context,
    private val cipher: SecretCipher,
) {
    private val dir = File(context.filesDir, "drafts")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private val _summaries = MutableStateFlow<List<DraftSummary>>(emptyList())
    val summaries: StateFlow<List<DraftSummary>> = _summaries.asStateFlow()

    init {
        scope.launch { refresh() }
    }

    /** False if the draft could not be written, for example when the Keystore key is unavailable. */
    suspend fun save(draft: Draft): Boolean = withContext(Dispatchers.IO) {
        val ok = write(draft)
        if (ok) refresh()
        ok
    }

    /** Survives the screen that started it, so leaving the composer never loses the last edit. */
    fun saveDetached(draft: Draft) {
        scope.launch { save(draft) }
    }

    suspend fun load(id: String): Draft? = withContext(Dispatchers.IO) { read(id) }

    fun deleteDetached(id: String) {
        scope.launch {
            synchronized(lock) { if (isValidId(id)) file(id).delete() }
            refresh()
        }
    }

    private fun file(id: String) = File(dir, "$id.draft")

    private fun write(draft: Draft): Boolean = synchronized(lock) {
        if (!isValidId(draft.id)) return false
        try {
            dir.mkdirs()
            val blob = cipher.encrypt(DraftCodec.encode(draft), draft.id.toByteArray())
            val out = ByteArrayOutputStream()
            out.write(blob.iv.size)
            out.write(blob.iv)
            out.write(blob.ciphertext)
            val tmp = File(dir, "${draft.id}.tmp")
            tmp.writeBytes(out.toByteArray())
            tmp.renameTo(file(draft.id))
        } catch (e: SecretUnavailableException) {
            false
        } catch (e: IOException) {
            false
        }
    }

    private fun read(id: String): Draft? = synchronized(lock) {
        if (!isValidId(id)) return null
        val f = file(id)
        try {
            if (!f.exists()) return null
            val raw = f.readBytes()
            if (raw.isEmpty()) return null
            val ivLength = raw[0].toInt() and 0xFF
            if (raw.size <= 1 + ivLength) return null
            val blob = EncryptedBlob(raw.copyOfRange(1, 1 + ivLength), raw.copyOfRange(1 + ivLength, raw.size))
            DraftCodec.decode(cipher.decrypt(blob, id.toByteArray()))
        } catch (e: SecretUnavailableException) {
            null
        } catch (e: IOException) {
            null
        }
    }

    private fun refresh() {
        val ids = dir.listFiles { f -> f.name.endsWith(".draft") }
            ?.map { it.name.removeSuffix(".draft") }.orEmpty()
        _summaries.value = ids.mapNotNull { read(it) }
            .sortedByDescending { it.updatedAtMillis }
            .map { d ->
                DraftSummary(
                    id = d.id,
                    to = d.to.trim(),
                    subject = d.subject.trim(),
                    preview = d.body.lines().firstOrNull { it.isNotBlank() && !it.startsWith("-- ") }.orEmpty().take(100),
                    updatedAtMillis = d.updatedAtMillis,
                )
            }
    }

    companion object {
        private val ID = Regex("[0-9a-f]{32}")
        fun isValidId(id: String) = ID.matches(id)
    }
}
