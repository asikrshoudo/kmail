package com.kmail.thekami

import android.app.Application
import com.kmail.thekami.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KmailApp : Application() {
    @Inject lateinit var syncScheduler: SyncScheduler

    override fun onCreate() {
        super.onCreate()
        syncScheduler.apply()
    }
}
