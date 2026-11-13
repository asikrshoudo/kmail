package com.kmail.thekami.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AccountEntity::class,
        FolderEntity::class,
        MessageEntity::class,
        CredentialEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class KmailDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun folderDao(): FolderDao
    abstract fun messageDao(): MessageDao
    abstract fun credentialDao(): CredentialDao
}
