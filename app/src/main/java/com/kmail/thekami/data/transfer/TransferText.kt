package com.kmail.thekami.data.transfer

import java.util.Base64

/** Text form of an encrypted transfer, used inside QR codes. Files use the raw bytes. */
object TransferText {
    fun encode(data: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(data)

    /** @return null if [text] is not valid Base64. */
    fun decode(text: String): ByteArray? = try {
        Base64.getUrlDecoder().decode(text.trim())
    } catch (e: IllegalArgumentException) {
        null
    }
}
