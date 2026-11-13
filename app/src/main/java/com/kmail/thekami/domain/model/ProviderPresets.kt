package com.kmail.thekami.domain.model

/** Extra action the user may need for a suggested server. */
enum class SuggestionNote { NONE, APP_PASSWORD_REQUIRED, OAUTH_MAY_BE_REQUIRED, GUESSED }

data class ServerTemplate(
    val host: String,
    val port: Int,
    val security: ConnectionSecurity,
) {
    fun toForm(username: String) = ServerForm(host, port.toString(), security, username)
}

data class ServerSuggestion(
    val imap: ServerTemplate,
    val smtp: ServerTemplate,
    val note: SuggestionNote,
)

/**
 * Built-in server settings for common providers. No network lookup happens here,
 * because sending the user's email domain to a third party is intentionally avoided.
 * Values are not yet verified against live servers.
 */
object ProviderPresets {
    private fun tls(host: String, port: Int) = ServerTemplate(host, port, ConnectionSecurity.SSL_TLS)
    private fun starttls(host: String, port: Int) = ServerTemplate(host, port, ConnectionSecurity.STARTTLS)

    private val gmail = ServerSuggestion(
        tls("imap.gmail.com", 993), tls("smtp.gmail.com", 465), SuggestionNote.APP_PASSWORD_REQUIRED,
    )
    private val outlook = ServerSuggestion(
        tls("outlook.office365.com", 993), starttls("smtp-mail.outlook.com", 587), SuggestionNote.OAUTH_MAY_BE_REQUIRED,
    )
    private val yahoo = ServerSuggestion(
        tls("imap.mail.yahoo.com", 993), tls("smtp.mail.yahoo.com", 465), SuggestionNote.APP_PASSWORD_REQUIRED,
    )
    private val icloud = ServerSuggestion(
        tls("imap.mail.me.com", 993), starttls("smtp.mail.me.com", 587), SuggestionNote.APP_PASSWORD_REQUIRED,
    )
    private val zoho = ServerSuggestion(
        tls("imap.zoho.com", 993), tls("smtp.zoho.com", 465), SuggestionNote.NONE,
    )
    private val fastmail = ServerSuggestion(
        tls("imap.fastmail.com", 993), tls("smtp.fastmail.com", 465), SuggestionNote.APP_PASSWORD_REQUIRED,
    )
    private val aol = ServerSuggestion(
        tls("imap.aol.com", 993), tls("smtp.aol.com", 465), SuggestionNote.APP_PASSWORD_REQUIRED,
    )

    private val byDomain: Map<String, ServerSuggestion> = mapOf(
        "gmail.com" to gmail, "googlemail.com" to gmail,
        "outlook.com" to outlook, "hotmail.com" to outlook, "live.com" to outlook, "msn.com" to outlook,
        "yahoo.com" to yahoo, "ymail.com" to yahoo, "rocketmail.com" to yahoo,
        "icloud.com" to icloud, "me.com" to icloud, "mac.com" to icloud,
        "zoho.com" to zoho,
        "fastmail.com" to fastmail,
        "aol.com" to aol,
    )

    val knownDomains: Set<String> get() = byDomain.keys

    /** Null if [emailAddress] is not plausibly an address. Unknown domains get a marked guess. */
    fun suggest(emailAddress: String): ServerSuggestion? {
        val email = normalizeEmail(emailAddress) ?: return null
        val domain = email.substringAfter('@')
        return byDomain[domain] ?: guess(domain)
    }

    private fun guess(domain: String) = ServerSuggestion(
        tls("imap.$domain", 993), starttls("smtp.$domain", 587), SuggestionNote.GUESSED,
    )
}
