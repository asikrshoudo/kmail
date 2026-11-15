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

    // ---- recovery questions ----

    val hasRecovery: Boolean get() = prefs.getInt(KEY_RQ_COUNT, 0) > 0

    fun recoveryQuestions(): List<String> =
        (0 until prefs.getInt(KEY_RQ_COUNT, 0)).mapNotNull { prefs.getString("rq_q_$it", null) }

    /** Returns the first problem with the questions or answers, or null once they are saved. */
    suspend fun setRecovery(questions: List<String>, answers: List<String>): RecoveryPolicy.Problem? {
        RecoveryPolicy.checkAll(questions, answers)?.let { return it }
        withContext(Dispatchers.Default) {
            val edit = prefs.edit().putInt(KEY_RQ_COUNT, questions.size)
            questions.forEachIndexed { i, question ->
                val salt = PinPolicy.newSalt()
                val hash = PinPolicy.hash(RecoveryPolicy.normalize(answers[i]), salt)
                edit.putString("rq_q_$i", question.trim())
                    .putString("rq_s_$i", Base64.getEncoder().encodeToString(salt))
                    .putString("rq_h_$i", Base64.getEncoder().encodeToString(hash))
            }
            edit.putInt(KEY_RQ_FAILS, 0).putLong(KEY_RQ_UNTIL, 0L).commit()
        }
        return null
    }

    suspend fun verifyRecovery(answers: List<String>, nowMillis: Long = System.currentTimeMillis()): PinVerify {
        val until = prefs.getLong(KEY_RQ_UNTIL, 0L)
        if (nowMillis < until) return PinVerify.Locked((until - nowMillis + 999) / 1000)
        val count = prefs.getInt(KEY_RQ_COUNT, 0)
        if (count == 0 || answers.size != count) return PinVerify.Wrong
        val ok = withContext(Dispatchers.Default) {
            // Every answer is always checked, so the time taken does not reveal which one was wrong.
            (0 until count).map { i ->
                val salt = prefs.getString("rq_s_$i", null)?.let { Base64.getDecoder().decode(it) }
                val expected = prefs.getString("rq_h_$i", null)?.let { Base64.getDecoder().decode(it) }
                salt != null && expected != null &&
                    PinPolicy.matches(RecoveryPolicy.normalize(answers[i]), salt, expected)
            }.all { it }
        }
        if (ok) {
            prefs.edit().putInt(KEY_RQ_FAILS, 0).putLong(KEY_RQ_UNTIL, 0L).commit()
            return PinVerify.Ok
        }
        val fails = prefs.getInt(KEY_RQ_FAILS, 0) + 1
        val wait = RecoveryPolicy.lockoutSeconds(fails)
        prefs.edit().putInt(KEY_RQ_FAILS, fails).putLong(KEY_RQ_UNTIL, if (wait > 0) nowMillis + wait * 1000 else 0L).commit()
        return if (wait > 0) PinVerify.Locked(wait) else PinVerify.Wrong
    }

    /** Sets a new PIN after a successful recovery. */
    suspend fun changePin(pin: String, confirm: String): PinChange {
        when (PinPolicy.check(pin)) {
            PinPolicy.Problem.FORMAT -> return PinChange.FORMAT
            PinPolicy.Problem.WEAK -> return PinChange.WEAK
            null -> Unit
        }
        if (pin != confirm) return PinChange.MISMATCH
        setPin(pin)
        return PinChange.OK
    }

    private companion object {
        const val KEY_SALT = "salt"
        const val KEY_HASH = "hash"
        const val KEY_FAILS = "fails"
        const val KEY_UNTIL = "until"
        const val KEY_AUTOLOCK = "autolock"
        const val KEY_BIOMETRIC = "biometric"
        const val KEY_RQ_COUNT = "rq_count"
        const val KEY_RQ_FAILS = "rq_fails"
        const val KEY_RQ_UNTIL = "rq_until"
    }
}
