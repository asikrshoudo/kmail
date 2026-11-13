package com.kmail.thekami.di

import android.content.Context
import androidx.room.Room
import com.kmail.thekami.data.db.AccountDao
import com.kmail.thekami.data.db.FolderDao
import com.kmail.thekami.data.db.KmailDatabase
import com.kmail.thekami.data.db.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KmailDatabase =
        Room.databaseBuilder(context, KmailDatabase::class.java, "kmail.db").build()

    @Provides
    fun provideAccountDao(db: KmailDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideFolderDao(db: KmailDatabase): FolderDao = db.folderDao()

    @Provides
    fun provideMessageDao(db: KmailDatabase): MessageDao = db.messageDao()
}
