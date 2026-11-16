package com.kmail.thekami.ui.home

import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class DateBucket { TODAY, YESTERDAY, LAST_7_DAYS, EARLIER }

object DateBuckets {
    fun bucketOf(millis: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): DateBucket {
        if (millis <= 0) return DateBucket.EARLIER
        val day = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val days = ChronoUnit.DAYS.between(day, today)
        return when {
            days <= 0 -> DateBucket.TODAY
            days == 1L -> DateBucket.YESTERDAY
            days < 7 -> DateBucket.LAST_7_DAYS
            else -> DateBucket.EARLIER
        }
    }
}

sealed interface ListEntry {
    data class Header(val bucket: DateBucket) : ListEntry
    data class Row(val item: MessageItem) : ListEntry
}

/** Date headers only make sense when the list is ordered by date. */
fun buildEntries(
    items: List<MessageItem>,
    sort: SortOrder,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): List<ListEntry> {
    if (sort != SortOrder.NEWEST && sort != SortOrder.OLDEST) return items.map { ListEntry.Row(it) }
    val out = ArrayList<ListEntry>(items.size + 4)
    var last: DateBucket? = null
    for (item in items) {
        val bucket = DateBuckets.bucketOf(item.receivedAtMillis, nowMillis, zone)
        if (bucket != last) {
            out += ListEntry.Header(bucket)
            last = bucket
        }
        out += ListEntry.Row(item)
    }
    return out
}

/** The same sender always gets the same color. */
object AvatarColors {
    private val PALETTE = longArrayOf(
        0xFF5B6EE1, 0xFF0F9D8A, 0xFFD6477A, 0xFFE0762B, 0xFF3B8FD9,
        0xFF7C5CC4, 0xFF4FA05A, 0xFFB5651D, 0xFF2E8BA6, 0xFFA23E8A,
    )

    fun indexFor(key: String): Int = (key.trim().lowercase().hashCode() and Int.MAX_VALUE) % PALETTE.size

    fun colorFor(key: String): Color = Color(PALETTE[indexFor(key)])
}
