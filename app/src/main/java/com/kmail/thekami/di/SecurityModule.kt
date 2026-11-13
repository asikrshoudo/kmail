package com.kmail.thekami.di

import com.kmail.thekami.data.security.AndroidKeystoreCipher
import com.kmail.thekami.data.security.SecretCipher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {
    @Binds
    abstract fun bindSecretCipher(impl: AndroidKeystoreCipher): SecretCipher
}
