package com.kmail.thekami.domain.mail

import com.kmail.thekami.domain.model.ServerEndpoint

/** Connects and authenticates against a server without sending or reading any mail. */
interface ConnectionTester {
    suspend fun testImap(endpoint: ServerEndpoint, password: CharArray): MailResult<Unit>
    suspend fun testSmtp(endpoint: ServerEndpoint, password: CharArray): MailResult<Unit>
}
