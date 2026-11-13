package com.kmail.thekami

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kmail.thekami.ui.nav.KmailNavHost
import com.kmail.thekami.ui.theme.KmailTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KmailTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    KmailNavHost()
                }
            }
        }
    }
}
