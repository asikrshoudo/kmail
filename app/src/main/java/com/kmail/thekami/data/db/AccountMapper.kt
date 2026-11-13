package com.kmail.thekami.data.db

import com.kmail.thekami.domain.model.Account
import com.kmail.thekami.domain.model.AccountSettings
import com.kmail.thekami.domain.model.ServerEndpoint

internal fun ServerEndpoint.toColumns() =
    EndpointColumns(host, port, security, authMethod, username)

internal fun EndpointColumns.toDomain() =
    ServerEndpoint(host, port, security, authMethod, username)

internal fun AccountEntity.toDomain() = Account(
    id = id,
    emailAddress = emailAddress,
    displayName = displayName,
    colorArgb = colorArgb,
    settings = AccountSettings(imap.toDomain(), smtp.toDomain()),
    createdAtMillis = createdAtMillis,
)

internal fun Account.toEntity() = AccountEntity(
    id = id,
    emailAddress = emailAddress,
    displayName = displayName,
    colorArgb = colorArgb,
    imap = settings.imap.toColumns(),
    smtp = settings.smtp.toColumns(),
    createdAtMillis = createdAtMillis,
)
