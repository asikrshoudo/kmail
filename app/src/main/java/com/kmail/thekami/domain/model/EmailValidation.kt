package com.kmail.thekami.domain.model

import java.util.Locale

/** Trims and lowercases an address. Returns null if it is not plausibly an email address. */
fun normalizeEmail(raw: String): String? {
    val email = raw.trim().lowercase(Locale.ROOT)
    val at = email.indexOf('@')
    if (at <= 0 || at != email.lastIndexOf('@')) return null
    if (email.any { it.isWhitespace() }) return null
    val domain = email.substring(at + 1)
    if ('.' !in domain || domain.startsWith('.') || domain.endsWith('.')) return null
    return email
}
