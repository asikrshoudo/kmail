package com.kmail.thekami.di

import com.kmail.thekami.data.repo.AccountRepository
import com.kmail.thekami.domain.mail.ProviderSource
import com.kmail.thekami.domain.model.AccountCreator
import com.kmail.thekami.mail.MailProviderFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindAccountCreator(impl: AccountRepository): AccountCreator

    @Binds
    abstract fun bindProviderSource(impl: MailProviderFactory): ProviderSource

    @Binds
    abstract fun bindLockConfig(impl: com.kmail.thekami.data.security.AppLockStore): com.kmail.thekami.data.security.LockConfig

    @Binds
    abstract fun bindNewMailNotifier(impl: com.kmail.thekami.sync.AndroidMailNotifier): com.kmail.thekami.sync.NewMailNotifier
}
