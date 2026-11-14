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

    @Query("SELECT MAX(uid) FROM messages WHERE folderId = :folderId AND uidValidity = :uidValidity")
    suspend fun maxUid(folderId: Long, uidValidity: Long): Long?

    @Query(
        "SELECT m.* FROM messages m JOIN folders f ON f.id = m.folderId " +
            "WHERE m.folderId = :folderId AND m.uidValidity = f.uidValidity AND m.removedAtMillis IS NULL " +
            "ORDER BY m.receivedAtMillis DESC, m.uid DESC LIMIT :limit",
    )
    fun observeInFolder(folderId: Long, limit: Int): Flow<List<MessageEntity>>

    @Query(
        "SELECT m.* FROM messages m JOIN folders f ON f.id = m.folderId " +
            "WHERE f.role = 'INBOX' AND m.uidValidity = f.uidValidity AND m.removedAtMillis IS NULL " +
            "ORDER BY m.receivedAtMillis DESC, m.uid DESC LIMIT :limit",
    )
    fun observeUnifiedInbox(limit: Int): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: Long): MessageEntity?

    @Query("UPDATE messages SET snippet = :snippet WHERE id = :id")
    suspend fun updateSnippet(id: Long, snippet: String)

    @Query("SELECT * FROM messages WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<MessageEntity>
}
