package com.kmail.thekami.domain.mail

/**
 * Provider-independent failure categories.
 * [detail] must never contain credentials, tokens or message content.
 */
sealed interface MailError {
    val detail: String?

    data class Network(override val detail: String? = null) : MailError
    data class Timeout(override val detail: String? = null) : MailError
    data class Authentication(override val detail: String? = null) : MailError
    data class Tls(override val detail: String? = null) : MailError
    data class Protocol(override val detail: String? = null) : MailError
    data class Unsupported(override val detail: String? = null) : MailError
    data class Unknown(override val detail: String? = null) : MailError
}
