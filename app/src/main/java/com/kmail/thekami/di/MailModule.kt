package com.kmail.thekami.di

import com.kmail.thekami.domain.mail.ConnectionTester
import com.kmail.thekami.mail.AngusConnectionTester
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MailModule {
    @Binds
    abstract fun bindConnectionTester(impl: AngusConnectionTester): ConnectionTester
}
