package com.kmail.thekami.mail

import android.os.Build
import com.kmail.thekami.domain.mail.ConnectionTester
import com.kmail.thekami.domain.mail.MailResult
import com.kmail.thekami.domain.model.ServerEndpoint
import jakarta.mail.MessagingException
import jakarta.mail.Service
import jakarta.mail.Session
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Note: Angus Mail's connect() only accepts a String password, so a short-lived
 * String copy exists while connecting. It cannot be zeroed.
 */
@Singleton
class AngusConnectionTester @Inject constructor() : ConnectionTester {

    override suspend fun testImap(endpoint: ServerEndpoint, password: CharArray) =
        withContext(Dispatchers.IO) {
            attempt {
                val session = Session.getInstance(AngusProperties.imap(endpoint, Build.VERSION.SDK_INT))
                connectAndClose(session.getStore("imap"), endpoint, password)
            }
        }

    override suspend fun testSmtp(endpoint: ServerEndpoint, password: CharArray) =
        withContext(Dispatchers.IO) {
            attempt {
                val session = Session.getInstance(AngusProperties.smtp(endpoint, Build.VERSION.SDK_INT))
                connectAndClose(session.getTransport("smtp"), endpoint, password)
            }
        }

    private fun connectAndClose(service: Service, endpoint: ServerEndpoint, password: CharArray) {
        try {
            service.connect(endpoint.host, endpoint.port, endpoint.username, String(password))
        } finally {
            try {
                service.close()
            } catch (ignored: MessagingException) {
                // nothing useful to do if closing fails
            }
        }
    }

    private inline fun attempt(block: () -> Unit): MailResult<Unit> = try {
        block()
        MailResult.Success(Unit)
    } catch (e: Exception) {
        MailResult.Failure(e.toMailError())
    }
}
