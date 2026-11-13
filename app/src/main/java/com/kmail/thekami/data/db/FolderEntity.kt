package com.kmail.thekami.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kmail.thekami.domain.model.FolderRole

@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["accountId", "remoteName"], unique = true)],
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: String,
    val remoteName: String,
    val displayName: String,
    val role: FolderRole,
    val uidValidity: Long? = null,
    val uidNext: Long? = null,
    val lastSyncedAtMillis: Long? = null,
)
