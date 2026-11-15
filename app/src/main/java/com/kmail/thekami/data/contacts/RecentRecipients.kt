package com.kmail.thekami.data.contacts

import android.content.Context
import com.kmail.thekami.domain.model.normalizeEmail
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

object RecentList {
    /** Newest first, no duplicates, at most [max] entries. */
    fun merge(existing: List<String>, added: List<String>, max: Int = 100): List<String> =
        (added.map { it.lowercase() } + existing).distinct().take(max)
}

/** Addresses the user has sent mail to. Kept on this device only. */
@Singleton
class RecentRecipients @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("kmail_recent", Context.MODE_PRIVATE)

    @Synchronized
    fun add(addresses: List<String>) {
        val clean = addresses.mapNotNull { normalizeEmail(it) }
        if (clean.isEmpty()) return
        prefs.edit().putString("list", RecentList.merge(read(), clean).joinToString("\n")).apply()
    }

    @Synchronized
    fun matching(query: String, limit: Int): List<String> {
        val q = query.lowercase()
        return read().filter { it.contains(q) }.take(limit)
    }

    private fun read(): List<String> = prefs.getString("list", "").orEmpty().split("\n").filter { it.isNotBlank() }
}
