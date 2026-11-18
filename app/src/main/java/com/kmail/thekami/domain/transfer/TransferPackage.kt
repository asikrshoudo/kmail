package com.kmail.thekami.domain.transfer

import com.kmail.thekami.domain.model.AccountSettings
import com.kmail.thekami.domain.model.NewAccount

/**
 * Everything needed to sign in on another device. Holds both mailbox passwords,
 * so callers must call [wipe] as soon as they are done with it.
 */
class TransferPackage(
    val createdAtMillis: Long,
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
