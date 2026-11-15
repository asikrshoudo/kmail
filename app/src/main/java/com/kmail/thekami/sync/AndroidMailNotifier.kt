package com.kmail.thekami.sync

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kmail.thekami.MainActivity
import com.kmail.thekami.R
import com.kmail.thekami.data.db.MessageEntity
import com.kmail.thekami.domain.model.Account
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidMailNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SyncPrefs,
) : NewMailNotifier {

    @SuppressLint("MissingPermission")
    override fun notifyNew(messages: List<MessageEntity>, accounts: List<Account>) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel()
        val hide = prefs.state.value.hideContent
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        messages.groupBy { it.accountId }.forEach { (accountId, list) ->
            val account = accounts.firstOrNull { it.id == accountId }?.emailAddress.orEmpty()
            list.sortedByDescending { it.receivedAtMillis }.take(MAX_PER_ACCOUNT).forEach { m ->
                val title = if (hide) {
                    context.getString(R.string.notif_new_mail)
                } else {
                    m.fromName?.takeIf { it.isNotBlank() } ?: m.fromAddress
                }
                val text = if (hide) account else m.subject.ifBlank { context.getString(R.string.home_no_subject) }
                val notification = NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSubText(account)
                    .setGroup(accountId)
                    .setContentIntent(openMessage(m.id))
                    .setAutoCancel(true)
                    .build()
                manager.notify(m.id.toInt(), notification)
            }
            if (list.size > 1) {
                val summary = NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.resources.getQuantityString(R.plurals.notif_new_messages, list.size, list.size))
                    .setSubText(account)
                    .setGroup(accountId)
                    .setGroupSummary(true)
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build()
                manager.notify(accountId.hashCode(), summary)
            }
        }
    }

    /** Opens this message in the reader. */
    private fun openMessage(id: Long): PendingIntent = PendingIntent.getActivity(
        context,
        id.toInt(),
        Intent(Intent.ACTION_VIEW, Uri.parse("kmail://message/$id"), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notif_channel_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL = "mail"
        const val MAX_PER_ACCOUNT = 5
    }
}
