package com.kmail.thekami.ui.nav

object Routes {
    const val HOME = "home"
    const val WELCOME = "welcome"
    const val CREDENTIALS = "credentials"
    const val INCOMING = "incoming"
    const val OUTGOING = "outgoing"
    const val CONNECTING = "connecting"
    const val CONNECTED = "connected"
    const val READER = "reader/{messageId}"
    const val COMPOSE = "compose/{mode}/{messageId}"

    fun reader(messageId: Long) = "reader/$messageId"

    /** [mode] is one of: new, reply, replyall, forward. [messageId] is -1 for a new message. */
    fun compose(mode: String, messageId: Long) = "compose/$mode/$messageId"
}
