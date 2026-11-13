package com.kmail.thekami.mail

import com.kmail.thekami.domain.model.ConnectionSecurity
import com.kmail.thekami.domain.model.ServerEndpoint
import java.util.Properties

/**
 * Angus Mail session properties. Certificate and hostname checks are never relaxed:
 * no ssl.trust, no custom socket factory, no debug output.
 */
internal object AngusProperties {
    private const val TIMEOUT_MS = "20000"
    private const val TLS13_MIN_SDK = 29

    fun imap(endpoint: ServerEndpoint, sdkInt: Int): Properties =
        base("imap", endpoint, sdkInt).apply {
            setProperty("mail.imap.auth.mechanisms", "PLAIN LOGIN")
        }

    fun smtp(endpoint: ServerEndpoint, sdkInt: Int): Properties =
        base("smtp", endpoint, sdkInt).apply {
            setProperty("mail.smtp.auth", "true")
            setProperty("mail.smtp.auth.mechanisms", "PLAIN LOGIN")
        }

    private fun base(protocol: String, endpoint: ServerEndpoint, sdkInt: Int) =
        Properties().apply {
            val p = "mail.$protocol"
            setProperty("$p.host", endpoint.host)
            setProperty("$p.port", endpoint.port.toString())
            setProperty("$p.connectiontimeout", TIMEOUT_MS)
            setProperty("$p.timeout", TIMEOUT_MS)
            setProperty("$p.writetimeout", TIMEOUT_MS)
            setProperty("$p.ssl.checkserveridentity", "true")
            // Naming an unsupported protocol throws, and TLS 1.3 needs Android 10+.
            setProperty(
                "$p.ssl.protocols",
                if (sdkInt >= TLS13_MIN_SDK) "TLSv1.2 TLSv1.3" else "TLSv1.2",
            )
            when (endpoint.security) {
                ConnectionSecurity.SSL_TLS -> setProperty("$p.ssl.enable", "true")
                ConnectionSecurity.STARTTLS -> {
                    setProperty("$p.starttls.enable", "true")
                    setProperty("$p.starttls.required", "true")
                }
            }
        }
}
