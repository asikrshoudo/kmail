package com.kmail.thekami.ui.compose

enum class RecipientKind { TO, CC, BCC }

/** The address being typed is the text after the last separator in the field. */
object RecipientToken {
    private val SEPARATORS = charArrayOf(',', ';', ' ', '\t', '\n')

    fun current(text: String): String = text.substring(text.lastIndexOfAny(SEPARATORS) + 1).trim()

    fun replace(text: String, address: String): String =
        text.substring(0, text.lastIndexOfAny(SEPARATORS) + 1) + address + ", "
}
