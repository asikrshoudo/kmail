package com.kmail.thekami.domain.model

/** Parses the text of a To, Cc or Bcc field. Separators are commas, semicolons and whitespace. */
object Recipients {
    data class Result(val valid: List<String>, val invalid: List<String>)

    fun parse(text: String): Result {
        val valid = LinkedHashSet<String>()
        val invalid = mutableListOf<String>()
        text.split(Regex("[,;\\s]+"))
            .map { it.trim().trim('<', '>') }
            .filter { it.isNotEmpty() }
            .forEach { token ->
                val email = normalizeEmail(token)
                if (email != null) valid.add(email) else invalid.add(token)
            }
        return Result(valid.toList(), invalid)
    }
}
