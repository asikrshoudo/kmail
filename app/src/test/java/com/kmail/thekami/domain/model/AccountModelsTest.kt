package com.kmail.thekami.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class AccountModelsTest {
    private fun endpoint(
        host: String = "imap.example.com",
        port: Int = 993,
        username: String = "me@example.com",
    ) = ServerEndpoint(host, port, ConnectionSecurity.SSL_TLS, AuthMethod.NORMAL_PASSWORD, username)

    @Test
    fun normalizesEmail() {
        assertEquals("user@example.com", normalizeEmail("  User@Example.COM "))
    }

    @Test
    fun rejectsImplausibleEmails() {
        listOf("", "user", "@example.com", "a@@example.com", "a b@example.com", "a@localhost", "a@.com", "a@example.")
            .forEach { assertNull(it, normalizeEmail(it)) }
    }

    @Test
    fun endpointAcceptsValidValues() {
        endpoint()
        endpoint(port = 1)
        endpoint(port = 65535)
    }

    @Test
    fun endpointRejectsBadHosts() {
        listOf("", " ", ".example.com", "imap example.com", "imap.example.com/path").forEach {
            assertThrows(it, IllegalArgumentException::class.java) { endpoint(host = it) }
        }
    }

    @Test
    fun endpointRejectsBadPortsAndUsername() {
        assertThrows(IllegalArgumentException::class.java) { endpoint(port = 0) }
        assertThrows(IllegalArgumentException::class.java) { endpoint(port = 65536) }
        assertThrows(IllegalArgumentException::class.java) { endpoint(username = " ") }
    }
}
