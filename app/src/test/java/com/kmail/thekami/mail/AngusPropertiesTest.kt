package com.kmail.thekami.mail

import com.kmail.thekami.domain.model.AuthMethod
import com.kmail.thekami.domain.model.ConnectionSecurity
import com.kmail.thekami.domain.model.ServerEndpoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AngusPropertiesTest {
    private fun endpoint(security: ConnectionSecurity, port: Int = 993) =
        ServerEndpoint("mail.example.com", port, security, AuthMethod.NORMAL_PASSWORD, "me@example.com")

    @Test
    fun implicitTlsEnablesSslAndChecksHostname() {
        val p = AngusProperties.imap(endpoint(ConnectionSecurity.SSL_TLS), 34)
        assertEquals("true", p.getProperty("mail.imap.ssl.enable"))
        assertEquals("true", p.getProperty("mail.imap.ssl.checkserveridentity"))
        assertNull(p.getProperty("mail.imap.starttls.enable"))
        assertEquals("mail.example.com", p.getProperty("mail.imap.host"))
        assertEquals("993", p.getProperty("mail.imap.port"))
    }

    @Test
    fun starttlsIsRequiredNotOptional() {
        val p = AngusProperties.smtp(endpoint(ConnectionSecurity.STARTTLS, 587), 34)
        assertEquals("true", p.getProperty("mail.smtp.starttls.enable"))
        assertEquals("true", p.getProperty("mail.smtp.starttls.required"))
        assertNull(p.getProperty("mail.smtp.ssl.enable"))
        assertEquals("true", p.getProperty("mail.smtp.ssl.checkserveridentity"))
    }

    @Test
    fun neverRelaxesCertificateChecksOrEnablesDebug() {
        for (security in ConnectionSecurity.entries) {
            val imap = AngusProperties.imap(endpoint(security), 34)
            val smtp = AngusProperties.smtp(endpoint(security), 34)
            assertNull(imap.getProperty("mail.imap.ssl.trust"))
            assertNull(smtp.getProperty("mail.smtp.ssl.trust"))
            assertNull(imap.getProperty("mail.imap.ssl.socketFactory.class"))
            assertNull(imap.getProperty("mail.debug"))
            assertNull(smtp.getProperty("mail.debug"))
        }
    }

    @Test
    fun tls13OnlyOnAndroid10AndUp() {
        val old = AngusProperties.imap(endpoint(ConnectionSecurity.SSL_TLS), 28)
        val modern = AngusProperties.imap(endpoint(ConnectionSecurity.SSL_TLS), 29)
        assertEquals("TLSv1.2", old.getProperty("mail.imap.ssl.protocols"))
        assertEquals("TLSv1.2 TLSv1.3", modern.getProperty("mail.imap.ssl.protocols"))
    }

    @Test
    fun setsTimeoutsAndPasswordMechanismsOnly() {
        val imap = AngusProperties.imap(endpoint(ConnectionSecurity.SSL_TLS), 34)
        val smtp = AngusProperties.smtp(endpoint(ConnectionSecurity.SSL_TLS, 465), 34)
        assertEquals("20000", imap.getProperty("mail.imap.connectiontimeout"))
        assertEquals("20000", imap.getProperty("mail.imap.timeout"))
        assertEquals("20000", imap.getProperty("mail.imap.writetimeout"))
        assertEquals("PLAIN LOGIN", imap.getProperty("mail.imap.auth.mechanisms"))
        assertEquals("true", smtp.getProperty("mail.smtp.auth"))
        assertEquals("PLAIN LOGIN", smtp.getProperty("mail.smtp.auth.mechanisms"))
    }
}
