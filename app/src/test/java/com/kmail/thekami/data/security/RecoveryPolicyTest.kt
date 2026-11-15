package com.kmail.thekami.data.security

import com.kmail.thekami.data.security.RecoveryPolicy.Problem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecoveryPolicyTest {
    private val q1 = "What was the name of my first school?"
    private val q2 = "Which street did I grow up on?"

    @Test
    fun normalizeIgnoresCaseSpacesAndFullWidthForms() {
        assertEquals("blue car", RecoveryPolicy.normalize("  Blue   \uFF23ar "))
        assertEquals("abcd", RecoveryPolicy.normalize("\uFF21\uFF22\uFF23\uFF24"))
    }

    @Test
    fun acceptsReasonableQuestionsAndAnswers() {
        assertNull(RecoveryPolicy.checkAll(listOf(q1, q2), listOf("Greenfield Primary", "Maple Lane 12")))
    }

    @Test
    fun rejectsEasyOrBadInput() {
        assertEquals(Problem.QUESTION_SHORT, RecoveryPolicy.check("Pet?", "Fluffy the cat"))
        assertEquals(Problem.ANSWER_SHORT, RecoveryPolicy.check(q1, "abc"))
        assertEquals(Problem.ANSWER_COMMON, RecoveryPolicy.check(q1, "Password"))
        assertEquals(Problem.ANSWER_COMMON, RecoveryPolicy.check(q1, "aaaaaa"))
        assertEquals(Problem.ANSWER_REPEATS_QUESTION, RecoveryPolicy.check(q1, q1.uppercase()))
    }

    @Test
    fun rejectsDuplicatesAndWrongCounts() {
        assertEquals(Problem.DUPLICATE_QUESTIONS, RecoveryPolicy.checkAll(listOf(q1, " $q1 "), listOf("Alpha one", "Beta two")))
        assertEquals(Problem.DUPLICATE_ANSWERS, RecoveryPolicy.checkAll(listOf(q1, q2), listOf("Same answer", "same  ANSWER")))
        assertEquals(Problem.WRONG_COUNT, RecoveryPolicy.checkAll(listOf(q1), listOf("Alpha one")))
    }

    @Test
    fun lockoutStartsAtThreeFailuresAndIsCapped() {
        assertEquals(0L, RecoveryPolicy.lockoutSeconds(2))
        assertEquals(60L, RecoveryPolicy.lockoutSeconds(3))
        assertEquals(120L, RecoveryPolicy.lockoutSeconds(4))
        assertEquals(21_600L, RecoveryPolicy.lockoutSeconds(30))
    }
}
