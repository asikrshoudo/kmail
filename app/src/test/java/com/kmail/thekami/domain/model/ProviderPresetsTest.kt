package com.kmail.thekami.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderPresetsTest {
    @Test
    fun gmailPresetIgnoresCaseAndRequiresAppPassword() {
        val s = ProviderPresets.suggest("  Someone@GMAIL.com ")!!
        assertEquals("imap.gmail.com", s.imap.host)
        assertEquals(993, s.imap.port)
        assertEquals(ConnectionSecurity.SSL_TLS, s.imap.security)
        assertEquals("smtp.gmail.com", s.smtp.host)
        assertEquals(SuggestionNote.APP_PASSWORD_REQUIRED, s.note)
    }

    @Test
    fun unknownDomainIsGuessedAndMarked() {
        val s = ProviderPresets.suggest("me@example.org")!!
        assertEquals("imap.example.org", s.imap.host)
        assertEquals("smtp.example.org", s.smtp.host)
        assertEquals(587, s.smtp.port)
        assertEquals(ConnectionSecurity.STARTTLS, s.smtp.security)
        assertEquals(SuggestionNote.GUESSED, s.note)
    }

    @Test
    fun invalidEmailGivesNoSuggestion() {
        listOf("", "no-at-sign", "a@b", "a b@c.com").forEach {
            assertNull(it, ProviderPresets.suggest(it))
        }
    }

    @Test
    fun everyPresetAndGuessPassesFormValidation() {
        (ProviderPresets.knownDomains + "example.org").forEach { domain ->
            val email = "user@$domain"
            val s = ProviderPresets.suggest(email)!!
            assertTrue(domain, s.imap.toForm(email).validate() is ServerFormResult.Valid)
            assertTrue(domain, s.smtp.toForm(email).validate() is ServerFormResult.Valid)
        }
    }
}
