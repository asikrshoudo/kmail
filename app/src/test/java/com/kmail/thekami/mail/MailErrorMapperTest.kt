package com.kmail.thekami.mail

import com.kmail.thekami.domain.mail.MailError
import jakarta.mail.AuthenticationFailedException
import jakarta.mail.MessagingException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MailErrorMapperTest {
    @Test
    fun authenticationFailure() {
        assertTrue(AuthenticationFailedException("x").toMailError() is MailError.Authentication)
    }

    @Test
    fun timeoutWrappedInMessagingException() {
        val e = MessagingException("connect failed", SocketTimeoutException("t"))
        assertTrue(e.toMailError() is MailError.Timeout)
    }

    @Test
    fun unknownHostIsNetwork() {
        val e = MessagingException("connect failed", UnknownHostException("h"))
        assertTrue(e.toMailError() is MailError.Network)
    }

    @Test
    fun tlsProblemIsTls() {
        val e = MessagingException("connect failed", SSLHandshakeException("cert"))
        assertTrue(e.toMailError() is MailError.Tls)
    }

    @Test
    fun plainMessagingExceptionIsProtocol() {
        assertTrue(MessagingException("bad").toMailError() is MailError.Protocol)
    }

    @Test
    fun otherExceptionIsUnknown() {
        assertTrue(IllegalStateException("x").toMailError() is MailError.Unknown)
    }

    @Test
    fun detailNeverContainsTheExceptionMessage() {
        val error = MessagingException("login failed for secret@example.com").toMailError()
        assertFalse(error.detail.orEmpty().contains("secret@example.com"))
    }
}
