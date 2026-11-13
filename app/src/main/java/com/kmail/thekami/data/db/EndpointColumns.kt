package com.kmail.thekami.data.db

import com.kmail.thekami.domain.model.AuthMethod
import com.kmail.thekami.domain.model.ConnectionSecurity

/** Embedded server settings columns. Kept separate from the validated domain class. */
data class EndpointColumns(
    val host: String,
    val port: Int,
    val security: ConnectionSecurity,
    val authMethod: AuthMethod,
    val username: String,
)
