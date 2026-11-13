package com.kmail.thekami.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerFormTest {
    private fun valid() = ServerForm("imap.example.com", "993", ConnectionSecurity.SSL_TLS, "me@example.com")

    private fun errorsOf(form: ServerForm): Set<ServerFormError> =
        (form.validate() as ServerFormResult.Invalid).errors

    @Test
    fun validFormGivesTrimmedEndpoint() {
        val form = ServerForm("  imap.example.com ", " 993 ", ConnectionSecurity.STARTTLS, " me@example.com ")
        val endpoint = (form.validate() as ServerFormResult.Valid).endpoint
        assertEquals("imap.example.com", endpoint.host)
        assertEquals(993, endpoint.port)
        assertEquals(ConnectionSecurity.STARTTLS, endpoint.security)
        assertEquals("me@example.com", endpoint.username)
    }

    @Test
    fun blankHostIsInvalid() {
        assertEquals(setOf(ServerFormError.HOST_INVALID), errorsOf(valid().copy(host = "  ")))
    }

    @Test
    fun badHostsAreInvalid() {
        listOf("a b.com", ".example.com", "example.com.", "ex/ample.com", "host:993", "a@b.com").forEach {
            assertEquals(it, setOf(ServerFormError.HOST_INVALID), errorsOf(valid().copy(host = it)))
        }
    }

    @Test
    fun badPortsAreInvalid() {
        listOf("", "0", "65536", "-1", "abc", "99999999999").forEach {
            assertEquals(it, setOf(ServerFormError.PORT_INVALID), errorsOf(valid().copy(port = it)))
        }
    }

    @Test
    fun portBoundsAreAccepted() {
        listOf("1", "65535").forEach {
            assertTrue(it, valid().copy(port = it).validate() is ServerFormResult.Valid)
        }
    }

    @Test
    fun blankUsernameIsInvalid() {
        assertEquals(setOf(ServerFormError.USERNAME_EMPTY), errorsOf(valid().copy(username = " ")))
    }

    @Test
    fun emptyFormReportsEveryError() {
        assertEquals(
            setOf(ServerFormError.HOST_INVALID, ServerFormError.PORT_INVALID, ServerFormError.USERNAME_EMPTY),
            errorsOf(ServerForm()),
        )
    }
}
