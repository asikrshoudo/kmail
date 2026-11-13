package com.kmail.thekami.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query(
        "SELECT * FROM messages " +
            "WHERE folderId = :folderId AND uidValidity = :uidValidity AND removedAtMillis IS NULL " +
            "ORDER BY receivedAtMillis DESC LIMIT :limit",
    )
    fun observeFolder(folderId: Long, uidValidity: Long, limit: Int): Flow<List<MessageEntity>>

    @Query(
        "SELECT * FROM messages " +
            "WHERE accountId = :accountId AND folderId = :folderId " +
            "AND uidValidity = :uidValidity AND uid = :uid",
    )
    suspend fun findByKey(
        accountId: String,
        folderId: Long,
        uidValidity: Long,
        uid: Long,
    ): MessageEntity?

    /** Duplicates (same account, folder, UIDVALIDITY, UID) are ignored by the unique index. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicates(messages: List<MessageEntity>): List<Long>

    @Query("UPDATE messages SET isRead = :isRead, isStarred = :isStarred WHERE id = :id")
    suspend fun updateFlags(id: Long, isRead: Boolean, isStarred: Boolean)

    @Query("UPDATE messages SET removedAtMillis = :nowMillis WHERE id IN (:ids)")
    suspend fun markRemoved(ids: List<Long>, nowMillis: Long)

    /** Called when the server reports a new UIDVALIDITY: old UIDs no longer identify anything. */
    @Query("DELETE FROM messages WHERE folderId = :folderId AND uidValidity != :currentUidValidity")
    suspend fun deleteStaleValidity(folderId: Long, currentUidValidity: Long)

    @Query("DELETE FROM messages WHERE folderId = :folderId")
    suspend fun deleteByFolder(folderId: Long)
}
