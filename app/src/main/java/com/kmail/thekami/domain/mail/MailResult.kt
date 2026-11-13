package com.kmail.thekami.domain.mail

sealed interface MailResult<out T> {
    data class Success<out T>(val value: T) : MailResult<T>
    data class Failure(val error: MailError) : MailResult<Nothing>
}
