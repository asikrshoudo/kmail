package com.kmail.thekami.domain.mail

data class ProviderCapabilities(
    val supportsPush: Boolean,
    val supportsCategories: Boolean,
)
