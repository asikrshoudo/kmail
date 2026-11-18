package com.kmail.thekami.data.transfer

import com.kmail.thekami.data.repo.AccountRepository
import com.kmail.thekami.data.security.CredentialStore
import com.kmail.thekami.data.security.SecretUnavailableException
import com.kmail.thekami.domain.model.CredentialPurpose
import com.kmail.thekami.domain.transfer.TransferPackage
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface ExportResult {
    class Ready(val data: ByteArray) : ExportResult
    data object AccountMissing : ExportResult
    data object PasswordUnavailable : ExportResult
}

/** Builds the encrypted transfer for one account. Key derivation is slow, so it runs off the main thread. */
class TransferExporter @Inject constructor(
    private val accounts: AccountRepository,
    private val credentials: CredentialStore,
) {
    private val codec = TransferCodec()

    suspend fun export(accountId: String, passphrase: CharArray): ExportResult =
        withContext(Dispatchers.Default) {
            val account = accounts.getAccount(accountId) ?: return@withContext ExportResult.AccountMissing
            var imap: CharArray? = null
            var smtp: CharArray? = null
            try {
                imap = credentials.load(accountId, CredentialPurpose.IMAP)
                smtp = credentials.load(accountId, CredentialPurpose.SMTP)
                if (imap == null || smtp == null) return@withContext ExportResult.PasswordUnavailable
                val pkg = TransferPackage(
                    createdAtMillis = System.currentTimeMillis(),
                    emailAddress = account.emailAddress,
                    displayName = account.displayName,
                    colorArgb = account.colorArgb,
                    settings = account.settings,
                    imapPassword = imap,
                    smtpPassword = smtp,
                )
                ExportResult.Ready(codec.encode(pkg, passphrase))
            } catch (e: SecretUnavailableException) {
                ExportResult.PasswordUnavailable
            } finally {
                imap?.fill('\u0000')
                smtp?.fill('\u0000')
            }
        }
}
