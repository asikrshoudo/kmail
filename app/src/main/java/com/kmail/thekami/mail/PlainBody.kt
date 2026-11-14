package com.kmail.thekami.mail

import org.jsoup.Jsoup

/** Plain-text version of a message, used for quoting in replies and forwards. */
fun ParsedMessage.plainBody(): String = textBody ?: htmlBody?.let(::htmlToText) ?: ""

private fun htmlToText(html: String): String {
    val doc = Jsoup.parse(html.replace("\r\n", " ").replace("\n", " "))
    doc.select("style, script, head").remove()
    doc.select("br").before("\\n")
    doc.select("p, div, tr, li, h1, h2, h3, h4, h5, h6, blockquote").before("\\n")
    return doc.body().wholeText()
        .replace("\\n", "\n")
        .replace(Regex("[ \\t\\u00A0]+"), " ")
        .replace(Regex(" ?\\n ?"), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}
