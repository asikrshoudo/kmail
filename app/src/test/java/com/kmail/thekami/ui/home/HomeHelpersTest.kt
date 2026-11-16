package com.kmail.thekami.ui.home

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeHelpersTest {
    private val now = Instant.parse("2026-11-16T12:00:00Z").toEpochMilli()
    private fun at(iso: String) = Instant.parse(iso).toEpochMilli()
    private fun bucket(iso: String) = DateBuckets.bucketOf(at(iso), now, ZoneOffset.UTC)

    @Test
    fun bucketsFollowCalendarDays() {
        assertEquals(DateBucket.TODAY, bucket("2026-11-16T00:00:01Z"))
        assertEquals(DateBucket.YESTERDAY, bucket("2026-11-15T23:59:59Z"))
        assertEquals(DateBucket.LAST_7_DAYS, bucket("2026-11-12T10:00:00Z"))
        assertEquals(DateBucket.EARLIER, bucket("2026-11-09T10:00:00Z"))
        assertEquals(DateBucket.TODAY, bucket("2026-11-17T10:00:00Z"))
        assertEquals(DateBucket.EARLIER, DateBuckets.bucketOf(0, now, ZoneOffset.UTC))
    }

    private fun item(id: Long, iso: String) = MessageItem(id, "s", "subj", "", at(iso), true, false, false)

    @Test
    fun headersAreInsertedWhenTheBucketChanges() {
        val items = listOf(
            item(1, "2026-11-16T09:00:00Z"), item(2, "2026-11-16T08:00:00Z"),
            item(3, "2026-11-15T09:00:00Z"), item(4, "2026-10-01T09:00:00Z"),
        )
        val entries = buildEntries(items, SortOrder.NEWEST, now, ZoneOffset.UTC)
        assertEquals(
            listOf("H:TODAY", "R:1", "R:2", "H:YESTERDAY", "R:3", "H:EARLIER", "R:4"),
            entries.map { if (it is ListEntry.Header) "H:${it.bucket}" else "R:${(it as ListEntry.Row).item.id}" },
        )
    }

    @Test
    fun noHeadersWhenNotSortedByDate() {
        val entries = buildEntries(listOf(item(1, "2026-11-16T09:00:00Z")), SortOrder.UNREAD_FIRST, now, ZoneOffset.UTC)
        assertTrue(entries.all { it is ListEntry.Row })
    }

    @Test
    fun avatarColorIsStableCaseInsensitiveAndSpread() {
        assertEquals(AvatarColors.indexFor("Bob@X.com"), AvatarColors.indexFor(" bob@x.com "))
        val indexes = (1..30).map { AvatarColors.indexFor("user$it@example.com") }
        assertTrue(indexes.all { it in 0..9 })
        assertTrue(indexes.toSet().size >= 5)
    }
}
