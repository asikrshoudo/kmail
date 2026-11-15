package com.kmail.thekami.data.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.kmail.thekami.data.db.MessageDao
import com.kmail.thekami.domain.model.normalizeEmail
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Suggestion(val address: String, val name: String?)

/**
 * Suggests recipients from: addresses you sent to, phone contacts (only if the permission is
 * granted) and senders of cached mail. Nothing here leaves the device.
 */
@Singleton
class ContactSuggester @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageDao: MessageDao,
    private val recent: RecentRecipients,
) {
    suspend fun suggest(query: String, taken: Set<String>, limit: Int = 6): List<Suggestion> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            val out = LinkedHashMap<String, Suggestion>()
            fun add(address: String?, name: String?) {
                val email = normalizeEmail(address.orEmpty()) ?: return
                if (email in taken || email in out) return
                out[email] = Suggestion(email, name?.takeIf { it.isNotBlank() })
            }
            recent.matching(q, limit).forEach { add(it, null) }
            phoneContacts(q).forEach { add(it.address, it.name) }
            runCatching { messageDao.suggestSenders(q, limit) }.getOrDefault(emptyList())
                .forEach { add(it.address, it.name) }
            out.values.take(limit)
        }

    private fun phoneContacts(q: String): List<Suggestion> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        return runCatching {
            val like = "%$q%"
            val email = ContactsContract.CommonDataKinds.Email.ADDRESS
            val name = ContactsContract.Contacts.DISPLAY_NAME
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(email, name),
                "$email LIKE ? OR $name LIKE ?",
                arrayOf(like, like),
                null,
            )?.use { c ->
                buildList {
                    while (c.moveToNext() && size < 20) add(Suggestion(c.getString(0).orEmpty(), c.getString(1)))
                }
            } ?: emptyList()
        }.getOrDefault(emptyList())
    }
}
