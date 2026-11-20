package com.kmail.thekami.domain.transfer

import com.kmail.thekami.domain.model.AccountSettings
import com.kmail.thekami.domain.model.NewAccount

/** One mailbox inside a transfer. It holds both passwords, so call [wipe] when you are done. */
class TransferAccount(
    val emailAddress: String,
    val displayName: String,
    val colorArgb: Int,
    val settings: AccountSettings,
    val imapPassword: CharArray,
    val smtpPassword: CharArray,
) {
    /** Returns a draft with its own copies of the passwords, so both sides can be wiped separately. */
    fun toNewAccount() = NewAccount(
        emailAddress = emailAddress,
        displayName = displayName,
        colorArgb = colorArgb,
        settings = settings,
        imapPassword = imapPassword.copyOf(),
        smtpPassword = smtpPassword.copyOf(),
    )

    fun wipe() {
        imapPassword.fill('\u0000')
        smtpPassword.fill('\u0000')
    }
}

/** Everything needed to sign in to one or more accounts on another device. */
class TransferPackage(
    val createdAtMillis: Long,
    val accounts: List<TransferAccount>,
) {
    init {
        require(accounts.size in 1..MAX_ACCOUNTS) { "invalid account count" }
    }

    fun wipe() {
        accounts.forEach { it.wipe() }
    }

    companion object {
        const val MAX_ACCOUNTS = 32
    }
}
