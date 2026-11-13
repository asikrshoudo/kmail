package com.kmail.thekami.domain.model

data class RemoteFolder(
    val remoteName: String,
    val displayName: String,
    val role: FolderRole,
)
