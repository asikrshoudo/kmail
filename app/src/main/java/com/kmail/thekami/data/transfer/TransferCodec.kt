package com.kmail.thekami.data.transfer

import com.kmail.thekami.domain.transfer.TransferPackage
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

sealed interface TransferDecodeResult {
    class Ok(val data: TransferPackage) : TransferDecodeResult

    /** Wrong passphrase, or the data was damaged or changed. These cannot be told apart. */
    data object Failed : TransferDecodeResult

    /** The data was made by a newer version of the app. */
    data object Unsupported : TransferDecodeResult
}

/**
 * Seals a [TransferPackage] with a passphrase. Output layout: salt(16) + iv(12) + AES-256-GCM data.
 * There is no readable header, so the result looks like random bytes. The key comes from
 * PBKDF2-HMAC-SHA256 over the passphrase, a random salt and an app-specific constant.
 * The constant only keeps other tools from decrypting by accident. The passphrase is the real protection.
 */
class TransferCodec(
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val random: SecureRandom = SecureRandom(),
) {
    fun encode(pkg: TransferPackage, passphrase: CharArray): ByteArray {
        require(passphrase.isNotEmpty()) { "passphrase must not be empty" }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val plain = TransferSerializer.write(pkg)
        try {
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(AAD)
            return salt + iv + cipher.doFinal(plain)
        } finally {
            plain.fill(0)
        }
    }

    fun decode(data: ByteArray, passphrase: CharArray): TransferDecodeResult {
        if (passphrase.isEmpty() || data.size <= SALT_BYTES + IV_BYTES + TAG_BITS / 8) {
            return TransferDecodeResult.Failed
        }
        val salt = data.copyOfRange(0, SALT_BYTES)
        val iv = data.copyOfRange(SALT_BYTES, SALT_BYTES + IV_BYTES)
        val sealed = data.copyOfRange(SALT_BYTES + IV_BYTES, data.size)
        val plain = try {
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(AAD)
            cipher.doFinal(sealed)
        } catch (e: GeneralSecurityException) {
            return TransferDecodeResult.Failed
        }
        try {
            val pkg = TransferSerializer.read(plain) ?: return TransferDecodeResult.Unsupported
            return TransferDecodeResult.Ok(pkg)
        } catch (e: IOException) {
            return TransferDecodeResult.Failed
        } catch (e: IllegalArgumentException) {
            return TransferDecodeResult.Failed
        } finally {
            plain.fill(0)
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt + PEPPER, iterations, KEY_BITS)
        try {
            val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(raw, "AES").also { raw.fill(0) }
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        const val DEFAULT_ITERATIONS = 600_000
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private const val KEY_BITS = 256
        private const val TRANSFORM = "AES/GCM/NoPadding"
        private val AAD = "kmail-transfer".toByteArray(Charsets.US_ASCII)
        private val PEPPER = "bbc177323b9b24fc03b4e5785f14ae1d868fb3585a7010bd".toByteArray(Charsets.US_ASCII)
    }
}
