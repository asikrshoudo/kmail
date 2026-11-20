package com.kmail.thekami.data.transfer

import com.kmail.thekami.data.repo.AccountRepository
import com.kmail.thekami.data.security.CredentialStore
import com.kmail.thekami.data.security.SecretUnavailableException
import com.kmail.thekami.domain.model.CredentialPurpose
import com.kmail.thekami.domain.transfer.TransferAccount
import com.kmail.thekami.domain.transfer.TransferPackage
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface ExportResult {
    class Ready(val data: ByteArray) : ExportResult

    /** An account or one of its passwords could not be read. */
    data object Unavailable : ExportResult
}

/** Builds the encrypted transfer for one or more accounts. Key derivation is slow, so it runs off the main thread. */
class TransferExporter @Inject constructor(
    private val accounts: AccountRepository,
    private val credentials: CredentialStore,
) {
    private val codec = TransferCodec()

    suspend fun export(accountIds: List<String>, passphrase: CharArray): ExportResult =
        withContext(Dispatchers.Default) {
            if (accountIds.isEmpty()) return@withContext ExportResult.Unavailable
            val loaded = ArrayList<TransferAccount>()
            try {
                for (id in accountIds) {
                    loaded += load(id) ?: return@withContext ExportResult.Unavailable
                }
                ExportResult.Ready(codec.encode(TransferPackage(System.currentTimeMillis(), loaded), passphrase))
            } finally {
                loaded.forEach { it.wipe() }
            }
        }

    private suspend fun load(id: String): TransferAccount? {
        val account = accounts.getAccount(id) ?: return null
        var imap: CharArray? = null
        var smtp: CharArray? = null
        try {
            imap = credentials.load(id, CredentialPurpose.IMAP)
            smtp = credentials.load(id, CredentialPurpose.SMTP)
            if (imap != null && smtp != null) {
                return TransferAccount(account.emailAddress, account.displayName, account.colorArgb, account.settings, imap, smtp)
            }
        } catch (e: SecretUnavailableException) {
            // Fall through: the passwords could not be read.
        }
        imap?.fill('\u0000')
        smtp?.fill('\u0000')
        return null
    }
}
