package com.kmail.thekami.data.contacts

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentListTest {
    @Test
    fun newestFirstWithoutDuplicates() {
        assertEquals(listOf("c@x.com", "a@x.com", "b@x.com"), RecentList.merge(listOf("a@x.com", "b@x.com"), listOf("C@x.com", "a@x.com")))
    }

    @Test
    fun capsTheList() {
        assertEquals(listOf("a", "b"), RecentList.merge(listOf("c", "d"), listOf("a", "b"), max = 2))
    }
}
