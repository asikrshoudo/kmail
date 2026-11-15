package com.kmail.thekami.data.security

import java.text.Normalizer
import java.util.Locale

enum class PinChange { OK, FORMAT, WEAK, MISMATCH }

/** Rules for the recovery questions the user writes. Answers are compared after [normalize]. */
object RecoveryPolicy {
    enum class Problem {
        QUESTION_SHORT,
        ANSWER_SHORT,
        ANSWER_COMMON,
        ANSWER_REPEATS_QUESTION,
        DUPLICATE_QUESTIONS,
        DUPLICATE_ANSWERS,
        WRONG_COUNT,
    }

    const val REQUIRED = 2
    private const val MIN_QUESTION = 8
    private const val MIN_ANSWER = 4
    private val COMMON = setOf(
        "password", "qwerty", "123456", "1234", "12345678", "abcd", "abcdef", "admin",
        "none", "test", "yes", "no", "nothing", "iloveyou", "letmein", "secret",
    )

    /** Case, extra spaces and look-alike forms (such as full-width letters) must not matter. */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase(Locale.ROOT).trim().replace(Regex("\\s+"), " ")

    fun check(question: String, answer: String): Problem? {
        val q = normalize(question)
        val a = normalize(answer)
        if (q.length < MIN_QUESTION) return Problem.QUESTION_SHORT
        if (a.length < MIN_ANSWER) return Problem.ANSWER_SHORT
        if (a in COMMON || a.all { it == a[0] }) return Problem.ANSWER_COMMON
        if (a == q || (a.length >= 8 && q.contains(a))) return Problem.ANSWER_REPEATS_QUESTION
        return null
    }

    fun checkAll(questions: List<String>, answers: List<String>): Problem? {
        if (questions.size != REQUIRED || answers.size != REQUIRED) return Problem.WRONG_COUNT
        for (i in questions.indices) check(questions[i], answers[i])?.let { return it }
        if (normalize(questions[0]) == normalize(questions[1])) return Problem.DUPLICATE_QUESTIONS
        if (normalize(answers[0]) == normalize(answers[1])) return Problem.DUPLICATE_ANSWERS
        return null
    }

    /** Seconds to wait after [failures] wrong recovery attempts in a row. */
    fun lockoutSeconds(failures: Int): Long =
        if (failures < 3) 0L else minOf(60L shl (failures - 3).coerceAtMost(10), 21_600L)
}
