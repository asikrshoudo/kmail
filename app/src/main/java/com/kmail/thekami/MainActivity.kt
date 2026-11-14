package com.kmail.thekami

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmail.thekami.data.security.AppLockManager
import com.kmail.thekami.data.security.AppLockStore
import com.kmail.thekami.data.security.PinVerify
import com.kmail.thekami.ui.lock.Biometric
import com.kmail.thekami.ui.lock.LockScreen
import com.kmail.thekami.ui.nav.KmailNavHost
import com.kmail.thekami.ui.theme.KmailTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var lockManager: AppLockManager
    @Inject lateinit var lockStore: AppLockStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KmailTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val locked by lockManager.locked.collectAsStateWithLifecycle()
                    Box(Modifier.fillMaxSize()) {
                        KmailNavHost()
                        if (locked) {
                            LockScreen(
                                biometricAvailable = lockStore.biometricEnabled && Biometric.isAvailable(this@MainActivity),
                                onVerify = { pin ->
                                    lockStore.verify(pin).also { if (it is PinVerify.Ok) lockManager.unlock() }
                                },
                                onBiometric = {
                                    Biometric.authenticate(
                                        this@MainActivity,
                                        getString(R.string.lock_biometric_title),
                                        getString(R.string.lock_biometric_cancel),
                                    ) { lockManager.unlock() }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lockManager.onForeground()
    }

    override fun onStop() {
        // A rotation or theme change restarts the activity and must not count as leaving the app.
        if (!isChangingConfigurations) lockManager.onBackground()
        super.onStop()
    }
}
