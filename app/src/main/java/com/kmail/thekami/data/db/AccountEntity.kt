package com.kmail.thekami.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Account metadata only. Credentials are never stored in this database. */
@Entity(
    tableName = "accounts",
    indices = [Index(value = ["emailAddress"], unique = true)],
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val emailAddress: String,
    val displayName: String,
    val colorArgb: Int,
    val createdAtMillis: Long,
)
