package com.kmail.thekami.domain.model

/** Input for creating an account. The caller zeroes both password arrays afterwards. */
class NewAccount(
    val emailAddress: String,
    val displayName: String,
    val colorArgb: Int,
    val settings: AccountSettings,
    val imapPassword: CharArray,
    val smtpPassword: CharArray,
)

sealed interface AddAccountResult {
    data class Added(val account: Account) : AddAccountResult
    data object InvalidEmail : AddAccountResult
    data object AlreadyExists : AddAccountResult
    data object CredentialStorageFailed : AddAccountResult
}
