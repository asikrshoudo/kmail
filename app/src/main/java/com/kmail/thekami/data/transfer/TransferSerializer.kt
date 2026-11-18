package com.kmail.thekami.data.transfer

import com.kmail.thekami.domain.model.AccountSettings
import com.kmail.thekami.domain.model.AuthMethod
import com.kmail.thekami.domain.model.ConnectionSecurity
import com.kmail.thekami.domain.model.ServerEndpoint
import com.kmail.thekami.domain.transfer.TransferPackage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.CharBuffer

/** Plain binary layout of a [TransferPackage]. It is only ever stored or sent encrypted. */
internal object TransferSerializer {
    const val VERSION = 1
    private const val MAX_FIELD = 4096

    fun write(pkg: TransferPackage): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeByte(VERSION)
            out.writeLong(pkg.createdAtMillis)
            out.writeText(pkg.emailAddress)
            out.writeText(pkg.displayName)
            out.writeInt(pkg.colorArgb)
            out.writeEndpoint(pkg.settings.imap)
            out.writeEndpoint(pkg.settings.smtp)
            out.writeSecret(pkg.imapPassword)
            out.writeSecret(pkg.smtpPassword)
        }
        return bytes.toByteArray()
    }

    /** @return null if the version is not supported. @throws IOException or IllegalArgumentException if malformed. */
    fun read(plain: ByteArray): TransferPackage? =
        DataInputStream(ByteArrayInputStream(plain)).use { input ->
            if (input.readUnsignedByte() != VERSION) return null
            val createdAt = input.readLong()
            val email = input.readText()
            val name = input.readText()
            val color = input.readInt()
            val imap = input.readEndpoint()
            val smtp = input.readEndpoint()
            val imapPassword = input.readSecret()
            val smtpPassword = input.readSecret()
            TransferPackage(createdAt, email, name, color, AccountSettings(imap, smtp), imapPassword, smtpPassword)
        }

    private fun DataOutputStream.writeBlock(value: ByteArray) {
        writeInt(value.size)
        write(value)
    }

    private fun DataOutputStream.writeText(value: String) = writeBlock(value.toByteArray(Charsets.UTF_8))

    private fun DataOutputStream.writeSecret(value: CharArray) {
        val buffer = Charsets.UTF_8.encode(CharBuffer.wrap(value))
        val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
        if (buffer.hasArray()) buffer.array().fill(0)
        try {
            writeBlock(bytes)
        } finally {
            bytes.fill(0)
        }
    }

    private fun DataOutputStream.writeEndpoint(value: ServerEndpoint) {
        writeText(value.host)
        writeInt(value.port)
        writeText(value.security.name)
        writeText(value.authMethod.name)
        writeText(value.username)
    }

    private fun DataInputStream.readBlock(): ByteArray {
        val size = readInt()
        if (size < 0 || size > MAX_FIELD) throw IOException("bad field size")
        return ByteArray(size).also { readFully(it) }
    }

    private fun DataInputStream.readText() = String(readBlock(), Charsets.UTF_8)

    private fun DataInputStream.readSecret(): CharArray {
        val bytes = readBlock()
        try {
            val buffer = Charsets.UTF_8.decode(ByteBuffer.wrap(bytes))
            val chars = CharArray(buffer.remaining()).also { buffer.get(it) }
            if (buffer.hasArray()) buffer.array().fill('\u0000')
            return chars
        } finally {
            bytes.fill(0)
        }
    }

    private fun DataInputStream.readEndpoint() = ServerEndpoint(
        host = readText(),
        port = readInt(),
        security = ConnectionSecurity.valueOf(readText()),
        authMethod = AuthMethod.valueOf(readText()),
        username = readText(),
    )
}
