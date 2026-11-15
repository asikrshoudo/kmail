package com.kmail.thekami.ui.compose

object ComposeBody {
    /** Empty lines for the cursor, then the signature, then the quoted text. */
    fun initial(signature: String, quote: String): String = buildString {
        append("\n\n")
        if (signature.isNotBlank()) append("-- \n").append(signature.trim())
        if (quote.isNotBlank()) {
            if (signature.isNotBlank()) append("\n\n")
            append(quote)
        }
    }
}
