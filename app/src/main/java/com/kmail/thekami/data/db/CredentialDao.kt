package com.kmail.thekami.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kmail.thekami.domain.model.CredentialPurpose

@Dao
interface CredentialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(credential: CredentialEntity)

    @Query("SELECT * FROM credentials WHERE accountId = :accountId AND purpose = :purpose")
    suspend fun find(accountId: String, purpose: CredentialPurpose): CredentialEntity?

    @Query("DELETE FROM credentials WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)
}
