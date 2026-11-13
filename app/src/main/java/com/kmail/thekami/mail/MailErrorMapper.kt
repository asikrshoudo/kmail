package com.kmail.thekami.mail

import com.kmail.thekami.domain.mail.MailError
import jakarta.mail.AuthenticationFailedException
import jakarta.mail.MessagingException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException

/**
 * Maps an exception (and its causes) to a provider-neutral error.
 * Only the exception class name is kept: messages can contain hosts, users or addresses.
 */
internal fun Throwable.toMailError(): MailError {
    val detail = javaClass.simpleName
    val chain = generateSequence(this) { it.cause }.take(8).toList()
    return when {
        chain.any { it is AuthenticationFailedException } -> MailError.Authentication(detail)
        chain.any { it is SSLException } -> MailError.Tls(detail)
        chain.any { it is SocketTimeoutException } -> MailError.Timeout(detail)
        chain.any { it is IOException } -> MailError.Network(detail)
        chain.any { it is MessagingException } -> MailError.Protocol(detail)
        else -> MailError.Unknown(detail)
    }
}
