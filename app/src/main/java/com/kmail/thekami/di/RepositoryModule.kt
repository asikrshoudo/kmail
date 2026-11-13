package com.kmail.thekami.di

import com.kmail.thekami.data.repo.AccountRepository
import com.kmail.thekami.domain.model.AccountCreator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindAccountCreator(impl: AccountRepository): AccountCreator
}
