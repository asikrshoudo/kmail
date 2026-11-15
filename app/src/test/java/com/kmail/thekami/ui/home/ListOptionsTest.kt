package com.kmail.thekami.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class ListOptionsTest {
    private fun item(id: Long, time: Long, read: Boolean = true, star: Boolean = false, att: Boolean = false) =
        MessageItem(id, "s", "subj", "", time, read, star, att)

    private val items = listOf(
        item(1, time = 100, read = true),
        item(2, time = 300, read = false, star = true),
        item(3, time = 200, read = false, att = true),
        item(4, time = 400, read = true, star = true, att = true),
    )

    private fun ids(o: ListOptions) = ListOptions.apply(items, o).map { it.id }

    @Test
    fun newestAndOldestFirst() {
        assertEquals(listOf(4L, 2L, 3L, 1L), ids(ListOptions()))
        assertEquals(listOf(1L, 3L, 2L, 4L), ids(ListOptions(sort = SortOrder.OLDEST)))
    }

    @Test
    fun unreadFirstKeepsNewestFirstInsideEachGroup() {
        assertEquals(listOf(2L, 3L, 4L, 1L), ids(ListOptions(sort = SortOrder.UNREAD_FIRST)))
        assertEquals(listOf(4L, 1L, 2L, 3L), ids(ListOptions(sort = SortOrder.READ_FIRST)))
    }

    @Test
    fun filters() {
        assertEquals(listOf(2L, 3L), ids(ListOptions(filter = ListFilter.UNREAD)))
        assertEquals(listOf(4L, 2L), ids(ListOptions(filter = ListFilter.STARRED)))
        assertEquals(listOf(4L, 3L), ids(ListOptions(filter = ListFilter.ATTACHMENTS)))
    }

    @Test
    fun filterAndSortCombine() {
        assertEquals(listOf(2L, 4L), ids(ListOptions(SortOrder.UNREAD_FIRST, ListFilter.STARRED)))
    }
}
