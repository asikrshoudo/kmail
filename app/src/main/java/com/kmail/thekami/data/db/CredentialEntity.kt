package com.kmail.thekami.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import com.kmail.thekami.domain.model.CredentialPurpose

/** Encrypted secret. Removed automatically when its account is deleted. */
@Entity(
    tableName = "credentials",
    primaryKeys = ["accountId", "purpose"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
class CredentialEntity(
    val accountId: String,
    val purpose: CredentialPurpose,
    val iv: ByteArray,
    val ciphertext: ByteArray,
    val updatedAtMillis: Long,
)
