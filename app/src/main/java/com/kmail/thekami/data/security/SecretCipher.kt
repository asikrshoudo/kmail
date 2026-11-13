package com.kmail.thekami.data.security

/** Encrypts small secrets. [aad] binds a ciphertext to its owner so blobs cannot be swapped. */
interface SecretCipher {
    /** @throws SecretUnavailableException if the key cannot be created or used. */
    fun encrypt(plain: ByteArray, aad: ByteArray): EncryptedBlob

    /** @throws SecretUnavailableException if the key is missing or invalidated, or the data is corrupt. */
    fun decrypt(blob: EncryptedBlob, aad: ByteArray): ByteArray
}

class EncryptedBlob(val iv: ByteArray, val ciphertext: ByteArray)

/** The secret cannot be recovered and the user must re-enter it. Never carries secret data. */
class SecretUnavailableException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
