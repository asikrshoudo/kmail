package com.kmail.thekami.domain.model

/** Plaintext connections are intentionally not offered. */
enum class ConnectionSecurity { SSL_TLS, STARTTLS }

enum class AuthMethod { NORMAL_PASSWORD }

data class ServerEndpoint(
    val host: String,
    val port: Int,
    val security: ConnectionSecurity,
    val authMethod: AuthMethod,
    val username: String,
) {
    init {
        require(host.isNotBlank() && host.none { it.isWhitespace() }) { "invalid host" }
        require(!host.startsWith('.') && '/' !in host) { "invalid host" }
        require(port in 1..65535) { "invalid port" }
        require(username.isNotBlank()) { "username must not be blank" }
    }
}
