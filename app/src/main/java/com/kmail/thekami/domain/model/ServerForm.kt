package com.kmail.thekami.domain.model

enum class ServerFormError { HOST_INVALID, PORT_INVALID, USERNAME_EMPTY }

sealed interface ServerFormResult {
    data class Valid(val endpoint: ServerEndpoint) : ServerFormResult
    data class Invalid(val errors: Set<ServerFormError>) : ServerFormResult
}

/**
 * Raw text typed on a server screen. [validate] never throws, unlike the
 * [ServerEndpoint] constructor, which stays as a last line of defense.
 * The password is deliberately not part of this class.
 */
data class ServerForm(
    val host: String = "",
    val port: String = "",
    val security: ConnectionSecurity = ConnectionSecurity.SSL_TLS,
    val username: String = "",
) {
    fun validate(): ServerFormResult {
        val cleanHost = host.trim()
        val cleanUser = username.trim()
        val portNumber = port.trim().toIntOrNull()

        val errors = linkedSetOf<ServerFormError>()
        if (!isPlausibleHost(cleanHost)) errors += ServerFormError.HOST_INVALID
        if (portNumber == null || portNumber !in 1..65535) errors += ServerFormError.PORT_INVALID
        if (cleanUser.isEmpty()) errors += ServerFormError.USERNAME_EMPTY
        if (errors.isNotEmpty() || portNumber == null) return ServerFormResult.Invalid(errors)

        return ServerFormResult.Valid(
            ServerEndpoint(cleanHost, portNumber, security, AuthMethod.NORMAL_PASSWORD, cleanUser),
        )
    }

    private fun isPlausibleHost(h: String): Boolean =
        h.isNotEmpty() &&
            h.none { it.isWhitespace() || it == '/' || it == ':' || it == '@' } &&
            !h.startsWith('.') && !h.endsWith('.')
}
