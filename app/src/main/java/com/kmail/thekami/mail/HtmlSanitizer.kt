package com.kmail.thekami.mail

import java.util.Base64
import java.util.Locale
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist

data class SanitizedHtml(val html: String, val blockedRemoteCount: Int)

/**
 * Email HTML is untrusted. Scripts, forms, frames, event handlers and unsafe URLs are removed,
 * remote content is blocked unless allowed, and a Content-Security-Policy is added as a second layer.
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
        "body{margin:0;padding:12px 12px 96px;overflow-wrap:anywhere;word-wrap:break-word;color:#202124;background:#fff}" +
            "img{max-width:100% !important;height:auto}table{max-width:100%}"

    private val IMPORT = Regex("@import[^;]*;?", RegexOption.IGNORE_CASE)
    private val EXPRESSION = Regex("expression\\s*\\(", RegexOption.IGNORE_CASE)
    private val BINDING = Regex("(-moz-binding|behavior)\\s*:[^;}]*", RegexOption.IGNORE_CASE)
    private val FIXED = Regex("position\\s*:\\s*fixed", RegexOption.IGNORE_CASE)
    private val URL_FN = Regex("url\\(\\s*(['\"]?)([^)'\"]*)\\1\\s*\\)", RegexOption.IGNORE_CASE)
    private val URL_IN_TEXT = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)
    private val SAFE_IMAGE_TYPES = setOf("image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp", "image/bmp")

    fun render(message: ParsedMessage, allowRemote: Boolean): SanitizedHtml {
        val html = message.htmlBody ?: textToHtml(message.textBody.orEmpty())
        return sanitize(html, message.inlineImages, allowRemote)
    }

    fun sanitize(html: String, inlineImages: Map<String, InlineImage>, allowRemote: Boolean): SanitizedHtml {
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

        val csp = "default-src 'none'; img-src data:" + (if (allowRemote) " http: https:" else "") +
            "; style-src 'unsafe-inline'"
        doc.head().prepend("<meta http-equiv=\"Content-Security-Policy\" content=\"$csp\">")
        doc.head().prepend("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
        doc.head().append("<style>$BASE_CSS</style>")
        return SanitizedHtml(doc.outerHtml(), blocked)
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

    fun textToHtml(text: String): String {
        val out = StringBuilder("<div style=\"white-space:pre-wrap\">")
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
        return out.append("</div>").toString()
    }
}
