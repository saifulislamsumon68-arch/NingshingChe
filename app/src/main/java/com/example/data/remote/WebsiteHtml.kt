package com.example.data.remote

import android.net.Uri
import java.util.regex.Pattern

/**
 * Cleans ningshingche.com article HTML into portal blocks.
 * Markers: ¶ paragraph, ▣ image URL. Never leave raw tags in stored content.
 */
object WebsiteHtml {
    private val ARTICLE_BLOCK = Regex("""<article[^>]*>([\s\S]*?)</article>""", RegexOption.IGNORE_CASE)
    private val TOKEN = Regex("""<(p)(\s[^>]*)?>([\s\S]*?)</p>|<(img)\b([^>]*)>""", RegexOption.IGNORE_CASE)
    private val IMG_SRC = Regex("""src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val LEAK = Regex(
        """id\s*=\s*["']?article-content["']?[^<\n]{0,40}>?|class\s*=\s*["'][^"']*text-justify[^"']*["']""",
        RegexOption.IGNORE_CASE
    )

    private val CUTS = listOf(
        "হাব্বি মন্তব্যহানি",
        "মন্তব্য করিক",
        "id=\"contactForm\"",
        "নুয়া লেখা",
        "মান্নাপা লেখা",
        "শেয়ার করিক"
    )

    fun looksDirty(content: String): Boolean {
        val t = content.trim()
        if (t.isEmpty()) return false
        return t.contains("article-content", ignoreCase = true) ||
            t.contains("<p") ||
            t.contains("<img") ||
            t.contains("<div") ||
            t.contains("</") ||
            t.startsWith("id=") ||
            t.contains("id=\"article") ||
            LEAK.containsMatchIn(t)
    }

    fun htmlToPortalContent(fullHtml: String): String {
        val article = ARTICLE_BLOCK.find(fullHtml)?.groupValues?.get(1) ?: fullHtml
        val cutAt = CUTS.map { article.indexOf(it) }.filter { it > 80 }.minOrNull()
        val sliced = if (cutAt != null) article.substring(0, cutAt) else article

        val emptyMarker = Regex(
            """<p[^>]*id=["']article-content["'][^>]*>\s*</p>""",
            RegexOption.IGNORE_CASE
        )
        val marker = emptyMarker.find(sliced)
        val before = if (marker != null) sliced.substring(0, marker.range.first) else ""
        val after = if (marker != null) sliced.substring(marker.range.last + 1) else sliced

        val out = StringBuilder()
        collectImages(before, out)
        TOKEN.findAll(after).forEach { match ->
            val isImage = match.groupValues[4].equals("img", ignoreCase = true)
            if (isImage) {
                appendImage(match.groupValues[5], out)
            } else {
                val attrs = match.groupValues[2]
                if (attrs.contains("article-content", ignoreCase = true)) return@forEach
                val inner = match.groupValues[3]
                    .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
                val text = visibleText(inner)
                if (text.isNotBlank()) out.append("¶").append(text).append("\n\n")
            }
        }
        if (out.isBlank()) {
            collectImages(sliced, out)
            val fallback = visibleText(
                sliced.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
                    .replace(Regex("""</p>""", RegexOption.IGNORE_CASE), "\n\n")
            )
            fallback.split(Regex("""\n{2,}""")).map { it.trim() }.filter { it.length > 1 }.forEach {
                out.append("¶").append(it).append("\n\n")
            }
        }
        return out.toString().trim()
    }

    fun contentBlocks(content: String, featuredImageUrl: String = ""): List<Pair<String, String>> {
        val source = when {
            content.isBlank() -> ""
            looksDirty(content) -> htmlToPortalContent(content)
            else -> content
        }
        val parsed = if (source.contains("¶") || source.contains("▣")) {
            source.split(Regex("""\n{2,}""")).mapNotNull { chunk ->
                val line = chunk.trim()
                when {
                    line.startsWith("▣") -> "img" to line.removePrefix("▣").trim()
                    line.startsWith("¶") -> "p" to visibleText(line.removePrefix("¶"))
                    line.isNotBlank() -> "p" to visibleText(line)
                    else -> null
                }
            }
        } else {
            visibleText(source)
                .split(Regex("""\n{2,}"""))
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { "p" to it }
        }

        val cleaned = parsed.mapNotNull { (kind, value) ->
            if (kind == "img") {
                val src = value.trim()
                if (src.isBlank() || isNonArticleImage(src)) null else "img" to src
            } else {
                val text = visibleText(value)
                if (text.isBlank() || looksLikeGarbage(text)) null else "p" to text
            }
        }

        if (featuredImageUrl.isBlank()) return cleaned
        val firstImg = cleaned.indexOfFirst { it.first == "img" }
        return if (firstImg >= 0 && sameImage(cleaned[firstImg].second, featuredImageUrl)) {
            cleaned.filterIndexed { index, _ -> index != firstImg }
        } else {
            cleaned
        }
    }

    fun sameImage(a: String, b: String): Boolean {
        if (a.isBlank() || b.isBlank()) return false
        if (a == b) return true
        fun key(url: String): String =
            url.substringAfterLast('/')
                .substringBefore('?')
                .lowercase()
                .replace("hyphenhyphen", "-")
                .replace("%20", " ")
        val ka = key(a)
        val kb = key(b)
        return ka.isNotBlank() && ka == kb
    }

    fun visibleText(raw: String): String {
        if (raw.isBlank()) return ""
        var value = raw.replace(LEAK, " ")
        value = value.replace(Regex("""<[^>]+>"""), " ")
        value = value.replace("&nbsp;", " ").replace("&amp;", "&")
            .replace("&quot;", "\"").replace("&#39;", "'")
            .replace("&lt;", " ").replace("&gt;", " ")
            .replace("&#x27;", "'").replace("&apos;", "'")
        value = try {
            Uri.decode(value)
        } catch (_: Exception) {
            value
        }
        value = value.replace(Regex("""id\s*=\s*["']?article-content["']?"""), " ")
        value = value.replace(Regex("""[<>]"""), " ")
        return value.replace(Regex("""\s+"""), " ").trim()
    }

    private fun looksLikeGarbage(text: String): Boolean {
        val t = text.lowercase()
        return t == "id=\"article-content\">" ||
            t.startsWith("id=") ||
            t.contains("article-content") ||
            (t.contains("class=") && t.length < 40)
    }

    private fun isNonArticleImage(src: String): Boolean {
        val lower = src.lowercase()
        return lower.contains("logo") ||
            lower.contains("profile") ||
            lower.contains("avatar") ||
            lower.contains("/profiles/")
    }

    private fun collectImages(html: String, out: StringBuilder) {
        val matcher = Pattern.compile("""<img[^>]+src=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE).matcher(html)
        while (matcher.find()) {
            val src = matcher.group(1).orEmpty()
            if (src.isNotBlank() && !isNonArticleImage(src)) {
                out.append("▣").append(src).append("\n\n")
            }
        }
    }

    private fun appendImage(attrs: String, out: StringBuilder) {
        val src = IMG_SRC.find(attrs)?.groupValues?.get(1).orEmpty()
        if (src.isNotBlank() && !isNonArticleImage(src)) {
            out.append("▣").append(src).append("\n\n")
        }
    }
}
