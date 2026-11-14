package com.kmail.thekami.mail

import com.kmail.thekami.data.repo.AccountRepository
import com.kmail.thekami.data.security.CredentialStore
import com.kmail.thekami.domain.mail.ConnectionTester
import com.kmail.thekami.domain.mail.MailProvider
import com.kmail.thekami.domain.mail.ProviderSource
import javax.inject.Inject
import javax.inject.Singleton

/** Builds the provider for an account. Today every account is IMAP/SMTP. */
@Singleton
class MailProviderFactory @Inject constructor(
    private val accounts: AccountRepository,
    private val credentials: CredentialStore,
    private val tester: ConnectionTester,
) : ProviderSource {
    override suspend fun forAccount(accountId: String): MailProvider? =
        accounts.getAccount(accountId)?.let {
            AngusMailProvider(it.id, it.settings, credentials, tester)
        }
}
