package com.kmail.thekami.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockManagerTest {
    private class Config(override var isEnabled: Boolean, override var autoLockSeconds: Int) : LockConfig

    @Test
    fun startsLockedOnlyWhenEnabled() {
        assertTrue(AppLockManager(Config(true, 60)).locked.value)
        assertFalse(AppLockManager(Config(false, 60)).locked.value)
    }

    @Test
    fun staysUnlockedForShortAbsence() {
        val m = AppLockManager(Config(true, 60))
        m.unlock()
        m.onBackground(now = 1_000)
        m.onForeground(now = 30_000)
        assertFalse(m.locked.value)
    }

    @Test
    fun locksAfterTheConfiguredTime() {
        val m = AppLockManager(Config(true, 60))
        m.unlock()
        m.onBackground(now = 1_000)
        m.onForeground(now = 61_000)
        assertTrue(m.locked.value)
    }

    @Test
    fun zeroSecondsLocksOnEveryReturn() {
        val m = AppLockManager(Config(true, 0))
        m.unlock()
        m.onBackground(now = 5_000)
        m.onForeground(now = 5_000)
        assertTrue(m.locked.value)
    }

    @Test
    fun neverLocksWhenDisabledOrWithoutBackgrounding() {
        val off = AppLockManager(Config(false, 0))
        off.onBackground(now = 0)
        off.onForeground(now = 999_999)
        assertFalse(off.locked.value)

        val on = AppLockManager(Config(true, 0))
        on.unlock()
        on.onForeground(now = 999_999)
        assertFalse(on.locked.value)
    }

    @Test
    fun enablingInsideTheAppDoesNotLockImmediately() {
        val config = Config(false, 60)
        val m = AppLockManager(config)
        config.isEnabled = true
        m.onLockEnabled()
        assertFalse(m.locked.value)
    }
}
