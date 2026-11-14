package com.kmail.thekami.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlSanitizerTest {
    private fun clean(html: String, inline: Map<String, InlineImage> = emptyMap(), allow: Boolean = false) =
        HtmlSanitizer.sanitize(html, inline, allow)

    @Test
    fun scriptsFramesAndHandlersAreRemoved() {
        val out = clean(
            "<p onclick=\"x()\">Hi</p><script>alert(1)</script><iframe src=\"https://e.com\"></iframe>" +
                "<form action=\"https://e.com\"><input name=a></form>",
        ).html
        assertFalse(out.contains("script", ignoreCase = true))
        assertFalse(out.contains("onclick"))
        assertFalse(out.contains("<iframe"))
        assertFalse(out.contains("<form"))
        assertTrue(out.contains("Hi"))
    }

    @Test
    fun javascriptLinksLoseTheirHref() {
        assertFalse(clean("<a href=\"javascript:alert(1)\">x</a>").html.contains("javascript"))
    }

    @Test
    fun webLinksKeepHrefAndGetRel() {
        val out = clean("<a href=\"https://e.com/a\">x</a>").html
        assertTrue(out.contains("href=\"https://e.com/a\""))
        assertTrue(out.contains("noopener"))
    }

    @Test
    fun remoteImagesAreBlockedByDefault() {
        val r = clean("<img src=\"https://t.example/p.gif\">")
        assertEquals(1, r.blockedRemoteCount)
        assertFalse(r.html.contains("t.example"))
    }

    @Test
    fun remoteImagesAreKeptWhenAllowed() {
        val r = clean("<img src=\"https://t.example/p.gif\">", allow = true)
        assertEquals(0, r.blockedRemoteCount)
        assertTrue(r.html.contains("https://t.example/p.gif"))
    }

    @Test
    fun cidImageBecomesDataUri() {
        val inline = mapOf("logo" to InlineImage("image/png", byteArrayOf(1, 2, 3)))
        assertTrue(clean("<img src=\"cid:logo\">", inline).html.contains("data:image/png;base64,AQID"))
    }

    @Test
    fun unknownCidIsDropped() {
        assertFalse(clean("<img src=\"cid:nope\">").html.contains("cid:"))
    }

    @Test
    fun svgDataImagesAreDropped() {
        assertFalse(clean("<img src=\"data:image/svg+xml;base64,AAAA\">").html.contains("svg"))
    }

    @Test
    fun inlineStyleIsKeptButRemoteUrlsAreNot() {
        val r = clean("<div style=\"color:red;background:url(https://t.example/x.png)\">x</div>")
        assertTrue(r.html.contains("color:red"))
        assertFalse(r.html.contains("t.example"))
        assertEquals(1, r.blockedRemoteCount)
    }

    @Test
    fun headStylesAreKeptAndCleaned() {
        val r = clean(
            "<html><head><style>@import url(https://e.com/a.css); .a{color:blue}</style></head>" +
                "<body><p class=\"a\">x</p></body></html>",
        )
        assertTrue(r.html.contains(".a{color:blue}"))
        assertFalse(r.html.contains("@import"))
        assertFalse(r.html.contains("e.com"))
    }

    @Test
    fun cspIsAlwaysPresent() {
        assertTrue(clean("<p>x</p>").html.contains("default-src 'none'"))
    }

    @Test
    fun plainTextIsEscapedAndLinked() {
        val html = HtmlSanitizer.textToHtml("a <b> & https://e.com/x?y=1&z=2.")
        assertTrue(html.contains("&lt;b&gt;"))
        assertTrue(html.contains("href=\"https://e.com/x?y=1&amp;z=2\""))
        assertTrue(html.endsWith(".</div>"))
    }
}
