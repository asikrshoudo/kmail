package com.kmail.thekami.ui.nav

object Routes {
    const val HOME = "home"
    const val WELCOME = "welcome"
    const val CREDENTIALS = "credentials"
    const val SCAN = "scan"
    const val INCOMING = "incoming"
    const val OUTGOING = "outgoing"
    const val CONNECTING = "connecting"
    const val CONNECTED = "connected"
    const val SETTINGS = "settings"
    const val DRAFTS = "drafts"
    const val READER = "reader/{messageId}"
    const val COMPOSE = "compose/{mode}/{messageId}?draft={draft}"

    fun reader(messageId: Long) = "reader/$messageId"

    /** [mode] is one of: new, reply, replyall, forward, draft. [messageId] is -1 unless replying or forwarding. */
    fun compose(mode: String, messageId: Long, draftId: String? = null) =
        "compose/$mode/$messageId" + (draftId?.let { "?draft=$it" } ?: "")
}
