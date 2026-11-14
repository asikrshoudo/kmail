package com.kmail.thekami.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kmail.thekami.data.repo.AccountRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Entry {
        fun backgroundSync(): BackgroundSync
        fun accounts(): AccountRepository
        fun prefs(): SyncPrefs
    }

    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(applicationContext, Entry::class.java)
        val accounts = entry.accounts().observeAccounts().first()
        if (accounts.isEmpty()) return Result.success()
        val ok = entry.backgroundSync().run(accounts, entry.prefs().state.value.notifications)
        return if (ok) Result.success() else Result.retry()
    }
}
