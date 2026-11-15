package com.kmail.thekami.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android runs periodic work at most every 15 minutes. For shorter intervals each run schedules
 * the next one. Both are best effort: Android may delay them (Doze), and this is not push.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SyncPrefs,
) {
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun apply() {
        val manager = WorkManager.getInstance(context)
        val settings = prefs.state.value
        manager.cancelAllWorkByTag(CHAIN_TAG)
        if (!settings.backgroundSync) {
            manager.cancelUniqueWork(PERIODIC)
            return
        }
        if (settings.intervalMinutes >= MIN_PERIODIC) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(settings.intervalMinutes.toLong(), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            manager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
        } else {
            manager.cancelUniqueWork(PERIODIC)
            scheduleNext()
        }
    }

    /** Used by short intervals only. */
    fun scheduleNext() {
        val settings = prefs.state.value
        if (!settings.backgroundSync || settings.intervalMinutes >= MIN_PERIODIC) return
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInitialDelay(settings.intervalMinutes.toLong(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag(CHAIN_TAG)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    private companion object {
        const val PERIODIC = "kmail-background-sync"
        const val CHAIN_TAG = "kmail-sync-chain"
        const val MIN_PERIODIC = 15
    }
}
