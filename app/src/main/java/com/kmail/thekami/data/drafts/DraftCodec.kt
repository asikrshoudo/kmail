package com.kmail.thekami.data.drafts

import com.kmail.thekami.domain.model.Draft
import com.kmail.thekami.domain.model.DraftAttachment
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/** Binary format for a draft. Strings are length-prefixed UTF-8, so there is no 64 KB limit. */
object DraftCodec {
    private const val VERSION = 1
    private const val MAX_ITEMS = 1000

    fun encode(d: Draft): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(VERSION)
            out.str(d.id)
            out.optStr(d.accountId)
            out.str(d.to)
            out.str(d.cc)
            out.str(d.bcc)
            out.str(d.subject)
            out.str(d.body)
            out.optStr(d.inReplyTo)
            out.writeInt(d.references.size)
            d.references.forEach { out.str(it) }
            out.writeInt(d.attachments.size)
            d.attachments.forEach {
                out.str(it.name)
                out.str(it.mimeType)
                out.writeInt(it.bytes.size)
                out.write(it.bytes)
            }
            out.writeLong(d.updatedAtMillis)
        }
        return bytes.toByteArray()
    }

    /** Null for corrupt, truncated or unknown-version data. */
    fun decode(raw: ByteArray): Draft? = try {
        val input = DataInputStream(ByteArrayInputStream(raw))
        if (input.readInt() != VERSION) {
            null
        } else {
            val id = input.str()
            val accountId = input.optStr()
            val to = input.str()
            val cc = input.str()
            val bcc = input.str()
            val subject = input.str()
            val body = input.str()
            val inReplyTo = input.optStr()
            val references = List(input.count()) { input.str() }
            val attachments = List(input.count()) {
                val name = input.str()
                val type = input.str()
                DraftAttachment(name, type, input.blob())
            }
            Draft(id, accountId, to, cc, bcc, subject, body, inReplyTo, references, attachments, input.readLong())
        }
    } catch (e: IOException) {
        null
    }

    private fun DataOutputStream.str(s: String) {
        val b = s.toByteArray(Charsets.UTF_8)
        writeInt(b.size)
        write(b)
    }

    private fun DataOutputStream.optStr(s: String?) {
        writeBoolean(s != null)
        if (s != null) str(s)
    }

    private fun DataInputStream.blob(): ByteArray {
        val length = readInt()
        if (length < 0 || length > available()) throw IOException("bad length")
        return ByteArray(length).also { readFully(it) }
    }

    private fun DataInputStream.str(): String = String(blob(), Charsets.UTF_8)

    private fun DataInputStream.optStr(): String? = if (readBoolean()) str() else null

    private fun DataInputStream.count(): Int {
        val n = readInt()
        if (n < 0 || n > MAX_ITEMS) throw IOException("bad count")
        return n
    }
}
