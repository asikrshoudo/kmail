package com.kmail.thekami.domain.model

interface AccountCreator {
    suspend fun addAccount(draft: NewAccount): AddAccountResult
}
