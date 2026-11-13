package com.kmail.thekami.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kmail.thekami.domain.model.MessageKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["accountId", "folderId", "uidValidity", "uid"], unique = true),
        Index(value = ["folderId", "receivedAtMillis"]),
        Index(value = ["messageIdHeader"]),
    ],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: String,
    val folderId: Long,
    val uidValidity: Long,
    val uid: Long,
    val messageIdHeader: String? = null,
    val subject: String,
    val fromName: String? = null,
    val fromAddress: String,
    val snippet: String,
    val receivedAtMillis: Long,
    val isRead: Boolean = false,
    val isStarred: Boolean = false,
    val hasAttachments: Boolean = false,
    val sizeBytes: Long = 0,
    /** Set when the message left this folder (deleted or moved); row is purged after sync confirms. */
    val removedAtMillis: Long? = null,
) {
    fun key() = MessageKey(accountId, folderId, uidValidity, uid)
}
