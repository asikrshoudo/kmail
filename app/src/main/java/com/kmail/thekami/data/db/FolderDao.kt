package com.kmail.thekami.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE accountId = :accountId ORDER BY role, displayName")
    fun observeByAccount(accountId: String): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE accountId = :accountId AND remoteName = :remoteName")
    suspend fun find(accountId: String, remoteName: String): FolderEntity?

    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Update
    suspend fun update(folder: FolderEntity)

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getById(id: Long): FolderEntity?

    @Query("SELECT * FROM folders")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE accountId = :accountId")
    suspend fun listByAccount(accountId: String): List<FolderEntity>
}
