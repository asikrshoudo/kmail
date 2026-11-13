package com.kmail.thekami.domain.model

data class AccountSettings(
    val imap: ServerEndpoint,
    val smtp: ServerEndpoint,
)

data class Account(
    val id: String,
    val emailAddress: String,
    val displayName: String,
    val colorArgb: Int,
    val settings: AccountSettings,
    val createdAtMillis: Long,
)
