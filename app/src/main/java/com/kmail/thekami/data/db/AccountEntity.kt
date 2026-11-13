package com.kmail.thekami.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Account metadata and server settings. Passwords are never stored here. */
@Entity(
    tableName = "accounts",
    indices = [Index(value = ["emailAddress"], unique = true)],
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val emailAddress: String,
    val displayName: String,
    val colorArgb: Int,
    @Embedded(prefix = "imap_") val imap: EndpointColumns,
    @Embedded(prefix = "smtp_") val smtp: EndpointColumns,
    val createdAtMillis: Long,
)
