package com.kmail.thekami.data.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface LockConfig {
    val isEnabled: Boolean
    val autoLockSeconds: Int
}

sealed interface PinVerify {
    data object Ok : PinVerify
    data object Wrong : PinVerify
    data class Locked(val seconds: Long) : PinVerify
}

/**
 * Stores the PIN as a salted PBKDF2 hash and counts failed attempts. The PIN is never stored.
 * Limitation: the hash is not bound to the Android Keystore yet.
 */
@Singleton
class AppLockStore @Inject constructor(
    @ApplicationContext context: Context,
) : LockConfig {
    private val prefs = context.getSharedPreferences("kmail_lock", Context.MODE_PRIVATE)

    override val isEnabled: Boolean get() = prefs.contains(KEY_HASH)
    override val autoLockSeconds: Int get() = prefs.getInt(KEY_AUTOLOCK, 60)
    val biometricEnabled: Boolean get() = prefs.getBoolean(KEY_BIOMETRIC, false)

    fun setAutoLockSeconds(seconds: Int) = prefs.edit().putInt(KEY_AUTOLOCK, seconds).apply()
    fun setBiometricEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()

    suspend fun setPin(pin: String): Boolean {
        if (PinPolicy.check(pin) != null) return false
        return withContext(Dispatchers.Default) {
            val salt = PinPolicy.newSalt()
            val hash = PinPolicy.hash(pin, salt)
            prefs.edit()
                .putString(KEY_SALT, Base64.getEncoder().encodeToString(salt))
                .putString(KEY_HASH, Base64.getEncoder().encodeToString(hash))
                .putInt(KEY_FAILS, 0)
                .putLong(KEY_UNTIL, 0L)
                .commit()
        }
    }

    fun clear() {
        prefs.edit().clear().commit()
    }

    suspend fun verify(pin: String, nowMillis: Long = System.currentTimeMillis()): PinVerify {
        val until = prefs.getLong(KEY_UNTIL, 0L)
        if (nowMillis < until) return PinVerify.Locked((until - nowMillis + 999) / 1000)
        val salt = prefs.getString(KEY_SALT, null)?.let { Base64.getDecoder().decode(it) } ?: return PinVerify.Wrong
        val expected = prefs.getString(KEY_HASH, null)?.let { Base64.getDecoder().decode(it) } ?: return PinVerify.Wrong
        val ok = withContext(Dispatchers.Default) { PinPolicy.matches(pin, salt, expected) }
        if (ok) {
            prefs.edit().putInt(KEY_FAILS, 0).putLong(KEY_UNTIL, 0L).commit()
            return PinVerify.Ok
        }
        val fails = prefs.getInt(KEY_FAILS, 0) + 1
        val wait = PinPolicy.lockoutSeconds(fails)
        prefs.edit().putInt(KEY_FAILS, fails).putLong(KEY_UNTIL, if (wait > 0) nowMillis + wait * 1000 else 0L).commit()
        return if (wait > 0) PinVerify.Locked(wait) else PinVerify.Wrong
    }

    private companion object {
        const val KEY_SALT = "salt"
        const val KEY_HASH = "hash"
        const val KEY_FAILS = "fails"
        const val KEY_UNTIL = "until"
        const val KEY_AUTOLOCK = "autolock"
        const val KEY_BIOMETRIC = "biometric"
    }
}
