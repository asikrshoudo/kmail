package com.kmail.thekami.mail

import java.util.Base64
import java.util.Locale
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Element
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist

data class SanitizedHtml(val html: String, val blockedRemoteCount: Int)

/**
 * Email HTML is untrusted. Scripts, forms, frames, event handlers and unsafe URLs are removed,
 * remote content is blocked unless allowed, and a Content-Security-Policy is added as a second layer.
 * Quoted earlier messages are folded behind a small "..." button.
 */
object HtmlSanitizer {
    private val safelist: Safelist = Safelist.relaxed()
        .addTags(
            "style", "font", "center", "hr", "section", "article", "header", "footer", "main", "nav",
            "figure", "figcaption", "abbr", "address", "big", "tt", "s", "del", "ins", "mark", "wbr",
        )
        .addAttributes(
            ":all", "style", "class", "dir", "lang", "align", "valign", "width", "height", "bgcolor",
            "border", "cellpadding", "cellspacing", "colspan", "rowspan", "color", "face", "size", "title",
        )
        .removeProtocols("a", "href", "ftp")
        .addProtocols("a", "href", "mailto", "tel")
        .addProtocols("img", "src", "cid", "data")
        .addEnforcedAttribute("a", "rel", "noopener noreferrer")

    private const val BASE_CSS =
        "body{margin:0;padding:12px 16px 96px;overflow-wrap:anywhere;word-wrap:break-word;color:#202124;background:#fff}" +
            "img{max-width:100% !important;height:auto}table{max-width:100%}" +
            "blockquote{margin:8px 0;padding-left:12px;border-left:3px solid #c4c7c5;color:#5f6368}" +
            "details.kmail-quote{margin:8px 0}" +
            "details.kmail-quote>summary{display:inline-block;list-style:none;cursor:pointer;padding:0 10px;" +
            "border-radius:12px;background:#e8eaed;color:#5f6368;font-size:13px;line-height:20px}" +
            "details.kmail-quote>summary::-webkit-details-marker{display:none}"

    /** Inverts the page and then inverts pictures back, the usual way to get a dark email without scripts. */
    private const val DARK_CSS =
        "html{filter:invert(1) hue-rotate(180deg)}" +
            "img,picture,video,svg,[style*=\"background-image\"]{filter:invert(1) hue-rotate(180deg)}"

    private val IMPORT = Regex("@import[^;]*;?", RegexOption.IGNORE_CASE)
    private val EXPRESSION = Regex("expression\\s*\\(", RegexOption.IGNORE_CASE)
    private val BINDING = Regex("(-moz-binding|behavior)\\s*:[^;}]*", RegexOption.IGNORE_CASE)
    private val FIXED = Regex("position\\s*:\\s*fixed", RegexOption.IGNORE_CASE)
    private val URL_FN = Regex("url\\(\\s*(['\"]?)([^)'\"]*)\\1\\s*\\)", RegexOption.IGNORE_CASE)
    private val URL_IN_TEXT = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)
    private val SAFE_IMAGE_TYPES = setOf("image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp", "image/bmp")
    private const val QUOTE_SELECTOR = "blockquote, div.gmail_quote, div.protonmail_quote, div.yahoo_quoted"

    fun render(message: ParsedMessage, allowRemote: Boolean, darkView: Boolean = false): SanitizedHtml {
        val html = message.htmlBody ?: textToHtml(message.textBody.orEmpty())
        return sanitize(html, message.inlineImages, allowRemote, darkView)
    }

    fun sanitize(
        html: String,
        inlineImages: Map<String, InlineImage>,
        allowRemote: Boolean,
        darkView: Boolean = false,
    ): SanitizedHtml {
        val dirty = Jsoup.parse(html)
        // The cleaner keeps only the body, so move head styles in first (keeping their order).
        dirty.head().select("style").reversed().forEach { dirty.body().prependChild(it) }
        val doc = Cleaner(safelist).clean(dirty)
        doc.outputSettings().prettyPrint(false)
        var blocked = 0

        doc.select("style").forEach { el ->
            val css = el.data()
            el.empty()
            el.appendChild(DataNode(cleanCss(css, allowRemote) { blocked++ }))
        }
        doc.select("[style]").forEach { el ->
            el.attr("style", cleanCss(el.attr("style"), allowRemote) { blocked++ })
        }
        doc.select("img").forEach { img ->
            val src = img.attr("src").trim()
            val lower = src.lowercase(Locale.ROOT)
            when {
                lower.startsWith("cid:") -> {
                    val image = inlineImages[src.substring(4).trim().removePrefix("<").removeSuffix(">")]
                    if (image != null && image.mimeType.lowercase(Locale.ROOT) in SAFE_IMAGE_TYPES) {
                        img.attr("src", "data:${image.mimeType};base64," + Base64.getEncoder().encodeToString(image.bytes))
                    } else {
                        img.removeAttr("src")
                    }
                }
                lower.startsWith("data:image/") -> {
                    val type = lower.substringAfter("data:").substringBefore(";").substringBefore(",")
                    if (type !in SAFE_IMAGE_TYPES) img.removeAttr("src")
                }
                lower.startsWith("http://") || lower.startsWith("https://") -> {
                    if (!allowRemote) {
                        blocked++
                        img.removeAttr("src")
                    }
                }
                else -> img.removeAttr("src")
            }
        }

        collapseQuotes(doc.body())

        val csp = "default-src 'none'; img-src data:" + (if (allowRemote) " http: https:" else "") +
            "; style-src 'unsafe-inline'"
        doc.head().prepend("<meta http-equiv=\"Content-Security-Policy\" content=\"$csp\">")
        doc.head().prepend("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
        doc.head().append("<style>$BASE_CSS${if (darkView) DARK_CSS else ""}</style>")
        return SanitizedHtml(doc.outerHtml(), blocked)
    }

    /** Only the outermost quote is folded, and only if the message has other text besides the quote. */
    private fun collapseQuotes(body: Element) {
        val bodyTextLength = body.text().length
        for (quote in body.select(QUOTE_SELECTOR)) {
            if (quote.parents().any { it.tagName() == "details" || it.tagName() == "blockquote" }) continue
            if (bodyTextLength <= quote.text().length + 10) continue
            val details = Element("details").addClass("kmail-quote")
            details.appendElement("summary").text("\u2022\u2022\u2022")
            quote.before(details)
            details.appendChild(quote)
        }
    }

    internal fun cleanCss(css: String, allowRemote: Boolean, onBlocked: () -> Unit): String {
        var s = IMPORT.replace(css, "")
        s = EXPRESSION.replace(s, "blocked(")
        s = BINDING.replace(s, "")
        s = FIXED.replace(s, "position:static")
        return URL_FN.replace(s) { m ->
            val url = m.groupValues[2].trim().lowercase(Locale.ROOT)
            val remote = url.startsWith("http://") || url.startsWith("https://")
            when {
                url.startsWith("data:image/") && !url.startsWith("data:image/svg") -> m.value
                allowRemote && remote -> m.value
                else -> {
                    if (remote) onBlocked()
                    "none"
                }
            }
        }
    }

    private fun escape(s: String) =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    /** Escapes [text] and turns web addresses into links. */
    private fun linkify(text: String): String {
        val out = StringBuilder()
        var last = 0
        for (m in URL_IN_TEXT.findAll(text)) {
            out.append(escape(text.substring(last, m.range.first)))
            var url = m.value
            var trailing = ""
            while (url.isNotEmpty() && url.last() in ".,;:!?)]}") {
                trailing = url.last() + trailing
                url = url.dropLast(1)
            }
            val safe = escape(url)
            out.append("<a href=\"").append(safe).append("\">").append(safe).append("</a>").append(escape(trailing))
            last = m.range.last + 1
        }
        out.append(escape(text.substring(last)))
        return out.toString()
    }

    /** Lines starting with ">" become a quote block, which the reader then folds like an HTML quote. */
    fun textToHtml(text: String): String {
        val out = StringBuilder("<div style=\"white-space:pre-wrap\">")
        val normal = StringBuilder()
        val quote = StringBuilder()
        fun flushNormal() {
            if (normal.isNotEmpty()) out.append(linkify(normal.toString()))
            normal.setLength(0)
        }
        fun flushQuote() {
            if (quote.isNotEmpty()) out.append("<blockquote>").append(linkify(quote.toString().trimEnd('\n'))).append("</blockquote>")
            quote.setLength(0)
        }
        val lines = text.replace("\r\n", "\n").split("\n")
        lines.forEachIndexed { index, line ->
            if (line.startsWith(">")) {
                flushNormal()
                quote.append(line.removePrefix(">").removePrefix(" ")).append('\n')
            } else {
                flushQuote()
                normal.append(line)
                if (index != lines.lastIndex) normal.append('\n')
            }
        }
        flushQuote()
        flushNormal()
        return out.append("</div>").toString()
    }
}
