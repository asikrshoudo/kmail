package com.kmail.thekami.data.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** AES-256-GCM with a non-exportable Android Keystore key. Never creates a key while decrypting. */
@Singleton
class AndroidKeystoreCipher @Inject constructor() : SecretCipher {

    override fun encrypt(plain: ByteArray, aad: ByteArray): EncryptedBlob =
        guarded("Encryption failed") {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            cipher.updateAAD(aad)
            EncryptedBlob(iv = cipher.iv, ciphertext = cipher.doFinal(plain))
        }

    override fun decrypt(blob: EncryptedBlob, aad: ByteArray): ByteArray =
        guarded("Decryption failed") {
            val key = existingKey() ?: throw SecretUnavailableException("Encryption key is missing")
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, blob.iv))
            cipher.updateAAD(aad)
            cipher.doFinal(blob.ciphertext)
        }

    private inline fun <T> guarded(message: String, block: () -> T): T = try {
        block()
    } catch (e: SecretUnavailableException) {
        throw e
    } catch (e: GeneralSecurityException) {
        throw SecretUnavailableException(message, e)
    } catch (e: ProviderException) {
        throw SecretUnavailableException(message, e)
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    private fun existingKey(): SecretKey? = keyStore().getKey(KEY_ALIAS, null) as? SecretKey

    @Synchronized
    private fun getOrCreateKey(): SecretKey = existingKey() ?: createKey()

    private fun createKey(): SecretKey {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return generate(strongBox = true)
            } catch (ignored: StrongBoxUnavailableException) {
                // fall back to the regular hardware-backed Keystore
            } catch (ignored: ProviderException) {
                // some devices report StrongBox problems this way
            }
        }
        return generate(strongBox = false)
    }

    private fun generate(strongBox: Boolean): SecretKey {
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_BITS)
        if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
            .apply { init(builder.build()) }
            .generateKey()
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "kmail.secrets.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val TAG_BITS = 128
    }
}
