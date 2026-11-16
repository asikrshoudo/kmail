package com.kmail.thekami.ui.reader

import com.kmail.thekami.data.db.MessageEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadModelsTest {
    @Test
    fun normalizeStripsReplyAndForwardPrefixes() {
        assertEquals("plan", ThreadSubject.normalize("Re: Re: Plan"))
        assertEquals("plan", ThreadSubject.normalize("RE:  FW: Plan "))
        assertEquals("plan", ThreadSubject.normalize("Fwd: Plan"))
        assertEquals("plan b", ThreadSubject.normalize("Plan   B"))
        assertEquals("report: q3", ThreadSubject.normalize("Report: Q3"))
        assertEquals("", ThreadSubject.normalize("  "))
    }

    private fun msg(id: Long, subject: String, from: String, time: Long) = MessageEntity(
        id = id, accountId = "a", folderId = 1, uidValidity = 1, uid = id, subject = subject,
        fromAddress = from, snippet = "", receivedAtMillis = time,
    )

    @Test
    fun threadHoldsOnlyTheSameSubjectOldestFirstAndMarksOwnMessages() {
        val opened = msg(2, "Re: Plan", "bob@x.com", 200)
        val candidates = listOf(
            msg(3, "Re: Re: Plan", "ME@y.com", 300),
            msg(1, "Plan", "bob@x.com", 100),
            msg(9, "Something else", "bob@x.com", 150),
        )
        val items = ThreadBuilder.build(opened, candidates, "me@y.com")
        assertEquals(listOf(1L, 2L, 3L), items.map { it.id })
        assertEquals(listOf(false, false, true), items.map { it.isOwn })
    }

    @Test
    fun aMessageWithoutSubjectStaysAlone() {
        val opened = msg(1, "", "a@x.com", 1)
        val items = ThreadBuilder.build(opened, listOf(msg(2, "", "b@x.com", 2)), "me@y.com")
        assertEquals(listOf(1L), items.map { it.id })
    }

    @Test
    fun longThreadsAreCappedButKeepTheOpenedMessage() {
        val opened = msg(1, "Plan", "a@x.com", 1)
        val many = (2L..60L).map { msg(it, "Plan", "a@x.com", it) }
        val items = ThreadBuilder.build(opened, many, "")
        assertEquals(40, items.size)
        assertTrue(items.any { it.id == 1L })
        assertFalse(items.any { it.id == 2L })
    }
}
