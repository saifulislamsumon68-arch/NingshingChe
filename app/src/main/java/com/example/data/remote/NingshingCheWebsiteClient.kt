package com.example.data.remote

import android.net.Uri
import com.example.data.model.Article
import com.example.data.model.Author
import com.example.data.model.Category
import com.example.data.model.PdfDocument
import com.example.data.repository.NinghsingCheContentData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import com.example.data.model.ArticleComment
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class WebsiteListing(
    val articles: List<Article>,
    val categories: List<Category>,
    val authors: List<Author>,
    val pdfDocuments: List<PdfDocument>
)

/**
 * Live reader for ningshingche.com — the public KEHEM site that publishes
 * articles at /YYYY/MM/{slug}.kehem, categories at /category/{name},
 * yearly issues at /tag/নিংশিং_চে_-_{year}, and flipbook PDFs at /read_pdf/{id}.
 */
class NingshingCheWebsiteClient {

    private val cookieStore = ConcurrentHashMap<String, List<Cookie>>()
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host].orEmpty()
            }
        })
        .build()

    suspend fun syncCatalog(): Result<WebsiteListing> = withContext(Dispatchers.IO) {
        try {
            val merged = linkedMapOf<String, Article>()
            val featuredIds = mutableSetOf<String>()

            fetchPaginated("$SITE/") { html ->
                parseListing(html, isFeatured = false).also { page ->
                    page.take(3).forEach { featuredIds.add(it.id) }
                }
            }.forEach { merged[it.id] = it }

            fetchPaginated("$SITE/${encodePath("ফিচার্ড")}") { html ->
                parseListing(html, isFeatured = true)
            }.forEach {
                featuredIds.add(it.id)
                merged[it.id] = it.copy(isFeatured = true, isEditorialPick = true)
            }

            SITE_CATEGORIES.forEach { (name, slug) ->
                fetchPaginated("$SITE/category/${encodePath(name)}") { html ->
                    parseListing(html, categoryName = name, categorySlug = slug)
                }.forEach { article ->
                    val previous = merged[article.id]
                    merged[article.id] = (previous ?: article).copy(
                        category = name,
                        categorySlug = slug,
                        isFeatured = previous?.isFeatured == true || article.id in featuredIds,
                        isEditorialPick = previous?.isEditorialPick == true || article.id in featuredIds
                    )
                }
            }

            (2014..2026).forEach { year ->
                val tag = "নিংশিং_চে_-_${toBengaliDigits(year)}"
                fetchPaginated("$SITE/tag/${encodePath(tag)}", maxPages = 4) { html ->
                    parseListing(html, yearHint = year)
                }.forEach { article ->
                    val previous = merged[article.id]
                    merged[article.id] = (previous ?: article).copy(
                        year = year,
                        isFeatured = previous?.isFeatured == true || article.id in featuredIds
                    )
                }
            }

            val articles = merged.values
                .map { if (it.id in featuredIds) it.copy(isFeatured = true, isEditorialPick = true) else it }
                .sortedWith(compareByDescending<Article> { it.year }.thenByDescending { it.publishedDate })

            val pdfs = fetchPdfDocuments()
            Result.success(
                WebsiteListing(
                    articles = articles,
                    categories = buildCategories(articles),
                    authors = buildAuthors(articles),
                    pdfDocuments = pdfs
                )
            )
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun fetchArticle(idOrSlugOrUrl: String): Article? = withContext(Dispatchers.IO) {
        val candidates = buildList {
            resolveArticleUrl(idOrSlugOrUrl)?.let { add(it) }
            if (idOrSlugOrUrl.startsWith("http")) add(idOrSlugOrUrl)
        }.distinct()
        for (url in candidates) {
            val html = get(url) ?: continue
            parseArticlePage(html, url)?.let { return@withContext it }
        }
        null
    }

    private suspend fun fetchPaginated(
        baseUrl: String,
        maxPages: Int = 8,
        parse: (String) -> List<Article>
    ): List<Article> {
        val collected = linkedMapOf<String, Article>()
        for (page in 1..maxPages) {
            val url = if (page == 1) baseUrl else joinPage(baseUrl, page)
            val html = get(url) ?: break
            val items = parse(html)
            val before = collected.size
            items.forEach { collected[it.id] = it }
            val hasNext = html.contains("?page=${page + 1}") || html.contains("page=${page + 1}")
            if (items.isEmpty() || collected.size == before || !hasNext) break
        }
        return collected.values.toList()
    }

    private suspend fun fetchPdfDocuments(): List<PdfDocument> = coroutineScope {
        (1..6).map { id ->
            async {
                val html = get("$SITE/read_pdf/$id") ?: return@async null
                parsePdfPage(html, id)
            }
        }.awaitAll().filterNotNull()
    }

    private fun parseListing(
        html: String,
        categoryName: String = "",
        categorySlug: String = "",
        yearHint: Int? = null,
        isFeatured: Boolean = false
    ): List<Article> {
        val byId = linkedMapOf<String, Article>()

        html.split("article-card").drop(1).forEach { chunk ->
            parseCardChunk(chunk, categoryName, categorySlug, yearHint, isFeatured)
                ?.let { byId[it.id] = it }
        }

        val titleAnchor = Pattern.compile(
            """<a[^>]*class="[^"]*line-clamp-3[^"]*"[^>]*href="([^"]+\.kehem)"[^>]*>(.*?)</a>""",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        ).matcher(html)
        while (titleAnchor.find()) {
            val href = titleAnchor.group(1).orEmpty()
            val title = decode(stripTags(titleAnchor.group(2).orEmpty()))
            val parsed = parseHref(href) ?: continue
            if (title.isBlank() || title == "তামকরিক") continue
            if (byId.containsKey(parsed.slug)) continue
            val windowStart = (titleAnchor.start() - 900).coerceAtLeast(0)
            val windowEnd = (titleAnchor.end() + 700).coerceAtMost(html.length)
            byId[parsed.slug] = articleFromContext(
                html.substring(windowStart, windowEnd),
                parsed,
                title,
                categoryName,
                categorySlug,
                yearHint,
                isFeatured
            )
        }

        return byId.values.toList()
    }

    private fun parseCardChunk(
        chunk: String,
        categoryName: String,
        categorySlug: String,
        yearHint: Int?,
        isFeatured: Boolean
    ): Article? {
        val href = ARTICLE_HREF.find(chunk)?.groupValues?.get(1) ?: return null
        val parsed = parseHref(href) ?: return null
        val title = decode(
            TITLE_IN_CARD.find(chunk)?.groupValues?.get(1)
                ?: TITLE_IN_ANCHOR.find(chunk)?.groupValues?.get(1)
                ?: parsed.slug.replace("-", " ")
        ).trim()
        if (title.isBlank() || title == "তামকরিক") return null
        return articleFromContext(chunk, parsed, title, categoryName, categorySlug, yearHint, isFeatured)
    }

    private fun articleFromContext(
        ctx: String,
        parsed: ParsedPath,
        title: String,
        categoryName: String,
        categorySlug: String,
        yearHint: Int?,
        isFeatured: Boolean
    ): Article {
        val images = IMG_SRC.findAll(ctx).map { it.groupValues[1] }.toList()
        val profileImage = images.firstOrNull { it.contains("/profiles/", ignoreCase = true) }.orEmpty()
        val image = images.firstOrNull { src ->
            val lower = src.lowercase()
            !lower.contains("logo") && !lower.contains("profile") && !lower.contains("avatar")
        } ?: images.firstOrNull().orEmpty()

        val date = DATE_BN.find(ctx)?.value.orEmpty()
        val authorName = decode(AUTHOR_NAME.find(ctx)?.groupValues?.get(1).orEmpty()).ifBlank { "নিংশিং চে" }
        val authorHref = AUTHOR_HREF.find(ctx)?.groupValues?.get(1).orEmpty()
        val authorId = authorIdFromName(Uri.decode(authorHref.substringAfterLast('/').ifBlank { authorName }))
        val excerpt = decode(
            EXCERPT.find(ctx)?.groupValues?.get(1)
                ?: title
        ).trim().take(220)
        val year = yearHint ?: parsed.year
        val resolvedCategory = categoryName.ifBlank { guessCategory(title, ctx) }
        val resolvedSlug = categorySlug.ifBlank { categorySlugFromName(resolvedCategory) }

        return Article(
            id = parsed.slug,
            title = title,
            slug = parsed.slug,
            excerpt = excerpt,
            content = "",
            featuredImageUrl = image,
            authorId = authorId,
            authorName = authorName,
            authorAvatarUrl = AuthorProfiles.resolve(authorName, profileImage),
            category = resolvedCategory.ifBlank { "সাহিত্য" },
            categorySlug = resolvedSlug.ifBlank { "literature" },
            tags = listOfNotNull(resolvedCategory.takeIf { it.isNotBlank() }, "নিংশিং চে ${toBengaliDigits(year)}"),
            publishedDate = date.ifBlank { toBengaliDigits(year) },
            year = year,
            readingTimeMinutes = 5,
            isFeatured = isFeatured,
            isEditorialPick = isFeatured,
            viewCount = 0,
            sourceUrl = parsed.absoluteUrl,
            relatedArticleIds = emptyList()
        )
    }

    private fun parseArticlePage(html: String, sourceUrl: String): Article? {
        val parsed = parseHref(sourceUrl) ?: parseHref(
            OG_URL.find(html)?.groupValues?.get(1).orEmpty()
        ) ?: return null

        val title = decode(
            firstGroup(OG_TITLE.find(html))
                ?: H1.find(html)?.groupValues?.get(1)
                ?: parsed.slug.replace("-", " ")
        ).trim()
        if (title.isBlank()) return null

        val description = decode(OG_DESC.find(html)?.groupValues?.get(1).orEmpty())
        val image = OG_IMAGE.find(html)?.groupValues?.get(1).orEmpty()
        val categoryHref = CATEGORY_HREF.find(html)?.groupValues?.get(1).orEmpty()
        val categoryName = decode(
            CATEGORY_LABEL.find(html)?.groupValues?.get(1)
                ?: Uri.decode(categoryHref.substringAfterLast('/'))
        ).trim()
        val authorName = decode(AUTHOR_NAME.find(html)?.groupValues?.get(1).orEmpty()).ifBlank { "নিংশিং চে" }
        val authorHref = AUTHOR_HREF.find(html)?.groupValues?.get(1).orEmpty()
        val authorId = authorIdFromName(Uri.decode(authorHref.substringAfterLast('/').ifBlank { authorName }))
        val authorAvatar = AuthorProfiles.resolve(
            authorName,
            IMG_SRC.findAll(html).map { it.groupValues[1] }
                .firstOrNull { it.contains("/profiles/", ignoreCase = true) }
                .orEmpty()
        )
        val date = DATE_BN.find(html)?.value.orEmpty()
        val content = htmlToPortalContent(html)
        val excerpt = description.ifBlank {
            contentBlocks(content).filter { it.first == "p" }.joinToString(" ") { it.second }.take(180)
        }
        val minutes = ((content.length / 900) + 1).coerceIn(3, 25)

        return Article(
            id = parsed.slug,
            title = title,
            slug = parsed.slug,
            excerpt = excerpt,
            content = content,
            featuredImageUrl = image,
            authorId = authorId,
            authorName = authorName,
            authorAvatarUrl = authorAvatar,
            category = categoryName.ifBlank { "সাহিত্য" },
            categorySlug = categorySlugFromName(categoryName),
            tags = listOfNotNull(categoryName.takeIf { it.isNotBlank() }),
            publishedDate = date.ifBlank { toBengaliDigits(parsed.year) },
            year = parsed.year,
            readingTimeMinutes = minutes,
            isFeatured = false,
            isEditorialPick = false,
            viewCount = 0,
            sourceUrl = parsed.absoluteUrl,
            relatedArticleIds = emptyList()
        )
    }

    suspend fun loadComments(articleUrl: String): List<ArticleComment> = withContext(Dispatchers.IO) {
        val html = get(articleUrl) ?: return@withContext emptyList()
        parseComments(html)
    }

    suspend fun submitComment(
        articleUrl: String,
        name: String,
        address: String,
        email: String,
        phone: String,
        content: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val page = get(articleUrl) ?: return@withContext Result.failure(Exception("পৃষ্ঠা খোলা যায়নি"))
            val csrf = CSRF_TOKEN.find(page)?.groupValues?.get(1).orEmpty()
            if (csrf.isBlank()) return@withContext Result.failure(Exception("ফর্ম টোকেন পাওয়া যায়নি"))
            val body = FormBody.Builder()
                .add("csrfmiddlewaretoken", csrf)
                .add("name", name.trim())
                .add("address", address.trim().ifBlank { "বাংলাদেশ" })
                .add("email", email.trim())
                .add("phone", phone.trim())
                .add("content", content.trim())
                .build()
            val request = Request.Builder()
                .url(articleUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", articleUrl)
                .header("Origin", SITE)
                .post(body)
                .build()
            http.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success("মন্তব্য পাঠানি ইল। পর্যালোচনার পর প্রকাশ অইতই।")
                else Result.failure(Exception("সার্ভার জবাব: ${response.code}"))
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun parsePdfPage(html: String, id: Int): PdfDocument? {
        val title = decode(TITLE_TAG.find(html)?.groupValues?.get(1).orEmpty()).ifBlank { "নিংশিং চে PDF $id" }
        val pdfUrl = PDF_JS.find(html)?.groupValues?.get(1) ?: return null
        val year = Regex("""(20\d{2}|২০\d{2})""").find(title)?.value?.let { fromBengaliDigits(it).toIntOrNull() } ?: 0
        val category = when {
            title.contains("যুব") || title.contains("কুমেই") -> "স্মারক ও উৎসব"
            else -> "বার্ষিক ও উৎসব সংখ্যা"
        }
        return PdfDocument(
            id = "pdf-live-$id",
            title = title,
            edition = title,
            category = category,
            categorySlug = if (category.startsWith("স্মারক")) "pdf-cat-jubilee" else "pdf-cat-annual",
            year = if (year in 1990..2035) year else 0,
            authorOrEditor = "নিংশিং চে প্রকাশনা পর্ষদ",
            pageCount = 0,
            fileSizeMb = 0f,
            pdfUrl = pdfUrl,
            coverImageUrl = NinghsingCheContentData.APP_LOGO_URL,
            description = "নিংশিংচে.কম থেকে সংগৃহীত মূল ডিজিটাল সংস্করণ।",
            tags = listOf("নিংশিং চে", "PDF", title),
            downloadUrl = pdfUrl
        )
    }

    private fun get(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "bn-BD,bn,en;q=0.8")
                .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            }
        } catch (_: Exception) {
            null
        }
    }

    private data class ParsedPath(val year: Int, val month: String, val slug: String, val absoluteUrl: String)

    private fun parseHref(raw: String): ParsedPath? {
        val decoded = Uri.decode(raw.trim())
        val match = PATH_PATTERN.find(decoded) ?: return null
        val year = match.groupValues[1].toInt()
        val month = match.groupValues[2]
        val slug = match.groupValues[3].removeSuffix(".kehem")
        if (slug.isBlank()) return null
        return ParsedPath(year, month, slug, "$SITE/$year/$month/${encodePath(slug)}.kehem")
    }

    private fun resolveArticleUrl(idOrSlugOrUrl: String): String? {
        val trimmed = idOrSlugOrUrl.trim()
        parseHref(trimmed)?.let { return it.absoluteUrl }
        val slug = trimmed.removePrefix("/").removeSuffix(".kehem").substringAfterLast('/')
        if (slug.isBlank()) return null
        val yearMonth = Regex("""^(20\d{2})[/~-](\d{2})[/~-]""").find(trimmed)
        return if (yearMonth != null) {
            "$SITE/${yearMonth.groupValues[1]}/${yearMonth.groupValues[2]}/${encodePath(slug)}.kehem"
        } else {
            // slug-only: try common recent years/months via listing cache is preferred;
            // fall back to a search-less guess is not reliable, so return null here.
            null
        }
    }

    companion object {
        const val SITE = "https://ningshingche.com"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 NingshingCheApp/1.0"

        val SITE_CATEGORIES: List<Pair<String, String>> = listOf(
            "ইমার ঠারর এলা" to "language",
            "পৌ" to "news",
            "ভুমিকা" to "preface",
            "সম্পাদকীয়" to "editorial",
            "ইতিহাস" to "history",
            "সাহিত্য" to "literature",
            "সমাজ ও সংস্কৃতি" to "society-culture",
            "পর্যালোচনা" to "reviews",
            "জীবনী" to "biography",
            "স্মৃতিচারণ" to "reminiscence",
            "পৌরাণিক কাহিনী" to "mythology",
            "বিজ্ঞান ও প্রযুক্তি" to "science-technology",
            "সংস্কৃতি" to "culture",
            "রকমারি" to "misc",
            "ধর্ম" to "religion",
            "কবিতা" to "poetry"
        )

        private val ARTICLE_HREF = Regex("""href=["']([^"']+\.kehem)["']""")
        private val TITLE_IN_CARD = Regex("""line-clamp-3[^>]*>([^<]+)""")
        private val TITLE_IN_ANCHOR = Regex("""href=["'][^"']+\.kehem["'][^>]*>([^<]+)""")
        private val IMG_SRC = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        private val DATE_BN = Regex("""(?:জানুয়ারি|ফেব্রুয়ারি|মার্চ|এপ্রিল|মে|জুন|জুলাই|আগস্ট|সেপ্টেম্বর|অক্টোবর|নভেম্বর|ডিসেম্বর)[^<]{0,18}২০\d{2}""")
        private val AUTHOR_NAME = Regex("""/author/[^"']+["'][^>]*>([^<]+)""")
        private val AUTHOR_HREF = Regex("""href=["']([^"']*/author/[^"']+)["']""")
        private val EXCERPT = Regex("""<(?:p|span)[^>]*line-clamp-[23][^>]*>([^<]{12,})""")
        private val OG_TITLE = Regex("""property=["']og:title["'][^>]*content=["']([^"']+)["']|content=["']([^"']+)["'][^>]*property=["']og:title["']""", RegexOption.IGNORE_CASE)
        private val OG_DESC = Regex("""property=["']og:description["'][^>]*content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        private val OG_IMAGE = Regex("""property=["']og:image["'][^>]*content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        private val OG_URL = Regex("""property=["']og:url["'][^>]*content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        private val H1 = Regex("""<h1[^>]*>([^<]+)</h1>""", RegexOption.IGNORE_CASE)
        private val CATEGORY_HREF = Regex("""href=["']([^"']*/category/[^"']+)["']""")
        private val CATEGORY_LABEL = Regex("""/category/[^"']+["'][^>]*>([^<]{2,40})</a>""")
        private val ARTICLE_BLOCK = Regex("""<article[^>]*>([\s\S]*?)</article>""", RegexOption.IGNORE_CASE)
        private val TITLE_TAG = Regex("""<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE)
        private val PDF_JS = Regex("""var\s+pdf\s*=\s*'([^']+)'""")
        private val PATH_PATTERN = Regex("""(?:https?://ningshingche\.com)?/?(20\d{2})/(\d{2})/([^/?#]+?\.kehem|[^/?#]+)""")
        private val CSRF_TOKEN = Regex("""name=["']csrfmiddlewaretoken["'][^>]*value=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        private val BODY_TOKEN = Regex("""<(p)(\s[^>]*)?>([\s\S]*?)</p>|<(img)\b([^>]*)>""", RegexOption.IGNORE_CASE)
        private val COMMENT_CARD = Regex("""<(?:div|article)[^>]*\bcomments\b[^>]*>([\s\S]*?)</(?:div|article)>""", RegexOption.IGNORE_CASE)

        fun encodePath(value: String): String =
            URLEncoder.encode(value, "UTF-8").replace("+", "%20")

        fun toBengaliDigits(number: Int): String {
            val digits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
            return number.toString().map { ch -> if (ch.isDigit()) digits[ch - '0'] else ch }.joinToString("")
        }

        fun fromBengaliDigits(raw: String): String {
            val map = mapOf(
                '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
                '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
            )
            return raw.map { map[it] ?: it }.joinToString("")
        }

        fun authorIdFromName(name: String): String {
            val cleaned = name.trim().lowercase().replace(Regex("""\s+"""), "-")
            return if (cleaned.isBlank()) "author-ningshingche" else "author-$cleaned"
        }

        fun authorAvatarFromName(name: String): String {
            val encoded = encodePath(name.trim())
            return "https://surajit-singha-sisir.github.io/NingshingCheNew/profiles/$encoded Profile.png"
        }

        fun categorySlugFromName(name: String): String {
            val n = name.trim()
            return when {
                n.contains("ইতিহাস") -> "history"
                n.contains("সাহিত্য") -> "literature"
                n.contains("সমাজ") -> "society-culture"
                n.contains("জীবনী") -> "biography"
                n.contains("স্মৃতি") -> "reminiscence"
                n.contains("পৌরাণিক") -> "mythology"
                n.contains("সম্পাদক") -> "editorial"
                n.contains("পর্যালোচনা") -> "reviews"
                n.contains("বিজ্ঞান") -> "science-technology"
                n.contains("ভূমিকা") || n.contains("ভুমিকা") -> "preface"
                n.contains("কবিতা") -> "poetry"
                n.contains("ধর্ম") -> "religion"
                n.contains("ইমার") || n.contains("ঠার") -> "language"
                n.contains("পৌ") && !n.contains("পৌরাণিক") -> "news"
                n.contains("রকমারি") -> "misc"
                n.contains("সংস্কৃতি") -> "culture"
                n.isBlank() -> "literature"
                else -> n.lowercase().replace(Regex("""\s+"""), "-")
            }
        }

        fun guessCategory(title: String, ctx: String): String {
            val hay = "$title $ctx"
            return SITE_CATEGORIES.firstOrNull { hay.contains(it.first) }?.first.orEmpty()
        }

        fun firstGroup(match: MatchResult?): String? =
            match?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }

        fun decode(raw: String): String {
            if (raw.isBlank()) return ""
            var value = raw.replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&lt;", "<").replace("&gt;", ">")
                .replace("&#x27;", "'").replace("&apos;", "'")
            value = Uri.decode(value)
            return value.replace(Regex("""\s+"""), " ").trim()
        }

        fun stripTags(html: String): String = html.replace(Regex("""<[^>]+>"""), " ")

        fun htmlToParagraphs(html: String): String = htmlToPortalContent(html)

        fun htmlToPortalContent(fullHtml: String): String {
            val article = ARTICLE_BLOCK.find(fullHtml)?.groupValues?.get(1) ?: fullHtml
            val cutAt = listOf("হাব্বি মন্তব্যহানি", "মন্তব্য করিক", "id=\"contactForm\"", "নুয়া লেখা")
                .map { article.indexOf(it) }.filter { it > 80 }.minOrNull()
            val body = if (cutAt != null) article.substring(0, cutAt) else article
            val out = StringBuilder()
            BODY_TOKEN.findAll(body).forEach { match ->
                val isImage = match.groupValues[4].equals("img", ignoreCase = true)
                if (isImage) {
                    val attrs = match.groupValues[5]
                    val src = Regex("""src=["']([^"']+)""").find(attrs)?.groupValues?.get(1).orEmpty()
                    val lower = src.lowercase()
                    if (src.isNotBlank() && !lower.contains("profile") && !lower.contains("logo") && !lower.contains("avatar")) {
                        out.append("▣").append(src).append("\n\n")
                    }
                } else {
                    val attrs = match.groupValues[2]
                    if (attrs.contains("article-content")) return@forEach
                    val inner = match.groupValues[3]
                        .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
                    val text = decode(stripTags(inner))
                    if (text.isNotBlank()) out.append("¶").append(text).append("\n\n")
                }
            }
            return out.toString().trim()
        }

        fun contentBlocks(content: String): List<Pair<String, String>> {
            if (content.contains("article-content") || content.contains("<p") || content.contains("id=")) {
                val cleaned = decode(stripTags(content.replace(Regex("""id=["']article-content["'][^<]*"""), "")))
                return cleaned.split(Regex("""\n{2,}""")).map { it.trim() }.filter { it.isNotBlank() }.map { "p" to it }
            }
            if (content.contains("¶") || content.contains("▣")) {
                return content.split(Regex("""\n{2,}""")).mapNotNull { chunk ->
                    val line = chunk.trim()
                    when {
                        line.startsWith("▣") -> "img" to line.removePrefix("▣").trim()
                        line.startsWith("¶") -> "p" to line.removePrefix("¶").trim()
                        line.isNotBlank() -> "p" to line
                        else -> null
                    }
                }
            }
            return content.split(Regex("""\n{2,}""")).map { it.trim() }.filter { it.isNotBlank() }.map { "p" to it }
        }

        fun parseComments(html: String): List<ArticleComment> {
            val cards = COMMENT_CARD.findAll(html).map { it.groupValues[1] }.toList()
            return cards.mapNotNull { card ->
                val text = decode(stripTags(Regex("""class=["'][^"']*comment-text[^"']*["'][^>]*>([\s\S]*?)<""", RegexOption.IGNORE_CASE).find(card)?.groupValues?.get(1) ?: ""))
                    .ifBlank { decode(stripTags(card)) }
                val name = decode(
                    Regex("""class=["'][^"']*(?:font-bold|font-semibold|author)[^"']*["'][^>]*>([^<]+)""", RegexOption.IGNORE_CASE)
                        .find(card)?.groupValues?.get(1).orEmpty()
                ).ifBlank { "পাঠক" }
                if (text.isBlank() || text.contains("কোন মন্তব্য")) null
                else ArticleComment(name = name, content = text)
            }
        }

        fun joinPage(baseUrl: String, page: Int): String {
            val clean = baseUrl.trimEnd('/')
            return if (clean.contains("?")) "$clean&page=$page" else "$clean?page=$page"
        }

        fun buildCategories(articles: List<Article>): List<Category> {
            val counts = articles.groupingBy { it.categorySlug }.eachCount()
            val fromSite = SITE_CATEGORIES.map { (name, slug) ->
                Category(
                    id = "cat-$slug",
                    name = name,
                    slug = slug,
                    description = "$name বিষয়ে নিংশিং চে-তে প্রকাশিত প্রবন্ধসমূহ।",
                    articleCount = counts[slug] ?: 0,
                    iconName = slug
                )
            }
            val extras = articles
                .filter { article -> SITE_CATEGORIES.none { it.second == article.categorySlug } }
                .groupBy { it.categorySlug }
                .map { (slug, list) ->
                    Category(
                        id = "cat-$slug",
                        name = list.first().category,
                        slug = slug,
                        description = "${list.first().category} বিষয়ে নিংশিং চে-তে প্রকাশিত প্রবন্ধসমূহ।",
                        articleCount = list.size,
                        iconName = slug
                    )
                }
            return (fromSite + extras).filter { it.articleCount > 0 || SITE_CATEGORIES.any { known -> known.second == it.slug } }
                .sortedByDescending { it.articleCount }
        }

        fun buildAuthors(articles: List<Article>): List<Author> {
            return articles.groupBy { it.authorId }.map { (id, list) ->
                val first = list.first()
                Author(
                    id = id,
                    name = first.authorName,
                    designation = "নিংশিং চে লেখক",
                    bio = "নিংশিং চে তথ্যকোষে প্রকাশিত লেখক। ${first.authorName} এর রচনাসমূহ ইতিহাস, সাহিত্য ও সংস্কৃতি বিষয়ে পাওয়া যায়।",
                    avatarUrl = AuthorProfiles.resolve(first.authorName, first.authorAvatarUrl),
                    articleCount = list.size,
                    location = "বাংলাদেশ / ভারত",
                    topics = list.map { it.category }.distinct().take(4),
                    isVerified = AuthorProfiles.isOfficial(
                        AuthorProfiles.resolve(first.authorName, first.authorAvatarUrl)
                    )
                )
            }.sortedByDescending { it.articleCount }
        }
    }
}
