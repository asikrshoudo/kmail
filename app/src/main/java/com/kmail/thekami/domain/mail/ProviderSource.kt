package com.kmail.thekami.domain.mail

interface ProviderSource {
    /** Null if the account no longer exists. */
    suspend fun forAccount(accountId: String): MailProvider?
}
