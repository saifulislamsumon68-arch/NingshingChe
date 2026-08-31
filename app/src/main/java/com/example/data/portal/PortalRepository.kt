package com.example.data.portal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Read-only repository over the live Supabase REST API.
 *
 * Design rules
 * ------------
 * 1. **The reader never authenticates.** Public RLS policies already restrict
 *    `blogs` and `comments` to `status = 'Publish'`, so the anonymous
 *    publishable key is both sufficient and correct. If you later front the API
 *    with a proxy that needs a token, add it in [PortalConfig.okHttpClient] —
 *    no call site has to change.
 * 2. **Every call returns a `Result`.** `PortalError` already carries a
 *    Bengali, user-presentable message.
 * 3. **Last-good-wins caching.** Reference data (categories, authors, PDFs,
 *    videos, settings) is cached in memory with a TTL; if the network fails the
 *    cached value is returned so the UI degrades instead of blanking out.
 * 4. **Paging is explicit.** Callers receive a [Page] with the exact total from
 *    `Content-Range`, which is what drives "load more" and the result counter in
 *    search.
 */
class PortalRepository(
    private val api: PortalApi
) {

    // ------------------------------------------------------------------ cache

    private class CacheEntry<T>(val value: T, val storedAtMillis: Long) {
        fun isFresh(ttlMillis: Long) = System.currentTimeMillis() - storedAtMillis < ttlMillis
    }

    private val cacheMutex = Mutex()
    private val categoriesCache = mutableMapOf<String, CacheEntry<List<CategoryRef>>>()
    private val authorsCache = mutableMapOf<String, CacheEntry<List<AuthorRef>>>()
    private val pdfCache = mutableMapOf<String, CacheEntry<List<PdfBook>>>()
    private val videoCache = mutableMapOf<String, CacheEntry<List<VideoItem>>>()
    private val settingsCache = mutableMapOf<String, CacheEntry<SiteSettings>>()

    private companion object {
        val TTL_REFERENCE = TimeUnit.MINUTES.toMillis(10)
        val TTL_SETTINGS = TimeUnit.HOURS.toMillis(1)
    }

    // ------------------------------------------------------------ home feed

    /**
     * One batched load for the home screen. Each section is fetched in parallel
     * and a failure in any single section degrades to an empty list rather than
     * failing the whole screen.
     */
    suspend fun homeFeed(): Result<HomeFeed> = withContext(Dispatchers.IO) {
        runCatching {
            coroutineScope {
                val hero = async { heroArticles().getOrNull().orEmpty() }
                val featured = async { featuredArticles().getOrNull().orEmpty() }
                val special = async { specialArticles().getOrNull().orEmpty() }
                val latest = async { latestArticles(limit = 12).getOrNull() }
                val categories = async { categories().getOrNull().orEmpty() }
                val authors = async { authors(limit = 16).getOrNull().orEmpty() }
                val gallery = async { galleries(limit = 12).getOrNull()?.items.orEmpty() }
                val pdfs = async { pdfBooks().getOrNull().orEmpty() }
                val videos = async { videos(limit = 8).getOrNull().orEmpty() }
                val settings = async { settings().getOrNull() ?: SiteSettings.DEFAULT }

                val latestPage = latest.await()
                if (latestPage == null && hero.await().isEmpty()) {
                    // Nothing at all came back — surface a real error instead of
                    // rendering an empty home screen.
                    throw PortalError.Unknown()
                }

                HomeFeed(
                    // With only 3 slider rows in the database the hero can look
                    // thin; top up from the newest articles so the carousel
                    // always has at least three panels.
                    hero = (hero.await().takeIf { it.size >= 3 } ?: (hero.await() + latestPage?.items.orEmpty()).distinctBy { it.id }.take(5)),
                    featured = featured.await().ifEmpty { latestPage?.items.orEmpty().take(6) },
                    special = special.await(),
                    latest = latestPage?.items.orEmpty(),
                    categories = categories.await(),
                    authors = authors.await(),
                    gallery = gallery.await(),
                    pdfBooks = pdfs.await(),
                    videos = videos.await(),
                    settings = settings.await()
                )
            }
        }.recoverCatching { throw it.toPortalError() }
    }

    // ----------------------------------------------------------------- blogs

    suspend fun heroArticles(): Result<List<ArticleSummary>> = withContext(Dispatchers.IO) {
        callList {
            api.blogs(
                status = "eq.Publish",
                isSlider = "eq.true",
                order = PortalApi.FEED_ORDER,
                limit = 6
            )
        }.map { list -> list.map { it.toSummary() } }
    }

    suspend fun featuredArticles(): Result<List<ArticleSummary>> = withContext(Dispatchers.IO) {
        callList {
            api.blogs(
                status = "eq.Publish",
                isFeature = "eq.true",
                order = PortalApi.FEED_ORDER,
                limit = 10
            )
        }.map { list -> list.map { it.toSummary() } }
    }

    suspend fun specialArticles(): Result<List<ArticleSummary>> = withContext(Dispatchers.IO) {
        callList {
            api.blogs(
                status = "eq.Publish",
                isSpecialArticle = "eq.true",
                order = PortalApi.FEED_ORDER,
                limit = 10
            )
        }.map { list -> list.map { it.toSummary() } }
    }

    suspend fun latestArticles(limit: Int = PortalConfig.PAGE_SIZE): Result<Page<ArticleSummary>> =
        withContext(Dispatchers.IO) {
            callPage {
                api.blogs(status = "eq.Publish", order = PortalApi.FEED_ORDER, limit = limit)
            }.map { page -> page.mapItems { it.toSummary() } }
        }

    /** Page through a category. `categoryId` is the UUID from [CategoryRef.id]. */
    suspend fun articlesByCategory(
        categoryId: String,
        limit: Int = PortalConfig.PAGE_SIZE,
        offset: Int = 0
    ): Result<Page<ArticleSummary>> = withContext(Dispatchers.IO) {
        callPage {
            api.blogs(
                status = "eq.Publish",
                categoryId = "eq.$categoryId",
                order = PortalApi.FEED_ORDER,
                limit = limit,
                offset = offset
            )
        }.map { page -> page.mapItems { it.toSummary() } }
    }

    suspend fun articlesByAuthor(
        authorId: String,
        limit: Int = PortalConfig.PAGE_SIZE,
        offset: Int = 0
    ): Result<Page<ArticleSummary>> = withContext(Dispatchers.IO) {
        callPage {
            api.blogs(
                status = "eq.Publish",
                authorId = "eq.$authorId",
                order = PortalApi.FEED_ORDER,
                limit = limit,
                offset = offset
            )
        }.map { page -> page.mapItems { it.toSummary() } }
    }

    /**
     * Server-side search. PostgREST ORs an `ilike` across title, subtitle and
     * slug; the term is escaped so a stray comma or parenthesis cannot break the
     * filter expression.
     */
    suspend fun searchArticles(
        query: String,
        limit: Int = PortalConfig.PAGE_SIZE,
        offset: Int = 0
    ): Result<Page<ArticleSummary>> = withContext(Dispatchers.IO) {
        val term = query.trim()
        if (term.length < 2) return@withContext Result.success(Page(emptyList(), total = 0, offset = offset, limit = limit))

        val pattern = "*${escapeFilterValue(term)}*"
        callPage {
            api.blogs(
                status = "eq.Publish",
                or = "(title.ilike.$pattern,sub_title.ilike.$pattern,slug.ilike.$pattern)",
                order = PortalApi.FEED_ORDER,
                limit = limit,
                offset = offset
            )
        }.map { page -> page.mapItems { it.toSummary() } }
    }

    /** Fetch a single article by UUID or slug (deep links use the slug). */
    suspend fun article(idOrSlug: String): Result<ArticleDetail> = withContext(Dispatchers.IO) {
        val trimmed = idOrSlug.trim()
        if (trimmed.isBlank()) return@withContext Result.failure(PortalError.NotFound)

        val isUuid = runCatching { java.util.UUID.fromString(trimmed) }.isSuccess

        callList {
            if (isUuid) {
                api.blogs(
                    select = PortalApi.BLOG_DETAIL_COLUMNS,
                    id = "eq.$trimmed",
                    limit = 1
                )
            } else {
                api.blogs(
                    select = PortalApi.BLOG_DETAIL_COLUMNS,
                    slug = "eq.${escapeFilterValue(trimmed)}",
                    limit = 1
                )
            }
        }.mapCatching { list ->
            val first = list.firstOrNull()
            if (first != null) {
                first.toDetail()
            } else if (isUuid) {
                val fallback = api.blogs(
                    select = PortalApi.BLOG_DETAIL_COLUMNS,
                    slug = "eq.${escapeFilterValue(trimmed)}",
                    limit = 1
                ).body()?.firstOrNull()
                fallback?.toDetail() ?: throw PortalError.NotFound
            } else {
                throw PortalError.NotFound
            }
        }
    }

    // ------------------------------------------------------------ reference

    suspend fun categories(forceRefresh: Boolean = false): Result<List<CategoryRef>> =
        withContext(Dispatchers.IO) {
            cached("all", categoriesCache, TTL_REFERENCE, forceRefresh) {
                callList { api.categories(limit = 100) }.getOrThrow().map { it.toRef() }
            }
        }

    suspend fun categoryBySlug(slug: String): Result<CategoryRef> = withContext(Dispatchers.IO) {
        val trimmed = slug.trim()
        if (trimmed.isBlank()) return@withContext Result.failure(PortalError.NotFound)
        callList { api.categoryBySlug(slug = "eq.${escapeFilterValue(trimmed)}") }.mapCatching { list ->
            list.firstOrNull()?.toRef() ?: throw PortalError.NotFound
        }
    }

    suspend fun authors(
        limit: Int = 50,
        offset: Int = 0,
        forceRefresh: Boolean = false
    ): Result<List<AuthorRef>> = withContext(Dispatchers.IO) {
        cached("authors-$limit-$offset", authorsCache, TTL_REFERENCE, forceRefresh) {
            callList { api.authors(limit = limit, offset = offset) }.getOrThrow().map { it.toRef() }
        }
    }

    suspend fun author(id: String): Result<AuthorRef> = withContext(Dispatchers.IO) {
        val trimmed = id.trim()
        if (trimmed.isBlank()) return@withContext Result.failure(PortalError.NotFound)
        callList { api.authorById(id = "eq.$trimmed") }.mapCatching { list ->
            list.firstOrNull()?.toRef() ?: throw PortalError.NotFound
        }
    }

    suspend fun galleries(
        category: String? = null,
        limit: Int = 24,
        offset: Int = 0
    ): Result<Page<GalleryItem>> = withContext(Dispatchers.IO) {
        val catFilter = category?.trim()?.takeIf { it.isNotBlank() }?.let { "eq.${escapeFilterValue(it)}" }
        callPage {
            api.galleries(category = catFilter, limit = limit, offset = offset)
        }.map { page -> page.mapItems { it.toItem() } }
    }

    suspend fun pdfBooks(forceRefresh: Boolean = false): Result<List<PdfBook>> =
        withContext(Dispatchers.IO) {
            cached("all", pdfCache, TTL_REFERENCE, forceRefresh) {
                callList { api.pdfBooks(limit = 100) }.getOrThrow().map { it.toModel() }
            }
        }

    suspend fun videos(limit: Int = 20, forceRefresh: Boolean = false): Result<List<VideoItem>> =
        withContext(Dispatchers.IO) {
            cached("videos-$limit", videoCache, TTL_REFERENCE, forceRefresh) {
                callList { api.videos(limit = limit) }.getOrThrow().map { it.toItem() }
            }
        }

    suspend fun settings(forceRefresh: Boolean = false): Result<SiteSettings> =
        withContext(Dispatchers.IO) {
            cached("site_settings", settingsCache, TTL_SETTINGS, forceRefresh) {
                callList { api.settings() }.getOrThrow().firstOrNull()?.toModel()
                    ?: SiteSettings.DEFAULT
            }
        }

    // ------------------------------------------------------------- comments

    suspend fun comments(blogId: String): Result<List<CommentItem>> = withContext(Dispatchers.IO) {
        callList {
            api.comments(blogId = "eq.$blogId", status = "eq.Publish", limit = 100)
        }.map { list -> list.map { it.toItem() } }
    }

    /**
     * Public comment submission. RLS inserts the row as `Unpublish`, so it is
     * invisible until a moderator approves it in the dashboard.
     */
    suspend fun postComment(
        blogId: String,
        blogTitle: String,
        name: String,
        email: String?,
        content: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.postComment(
                NewCommentDto(
                    blogId = blogId,
                    blogTitle = blogTitle,
                    name = name.trim(),
                    email = email?.trim()?.takeIf { it.isNotBlank() },
                    content = content.trim()
                )
            )
            if (!response.isSuccessful) throw PortalError.Http(response.code(), response.message())
            Unit
        }.recoverCatching { throw it.toPortalError() }
    }

    // -------------------------------------------------------------- plumbing

    /** Unwraps a list response, mapping HTTP/transport failures to [PortalError]. */
    private suspend fun <T> callList(block: suspend () -> Response<List<T>>): Result<List<T>> =
        runCatching {
            val response = block()
            if (!response.isSuccessful) throw httpError(response.code(), response)
            response.body().orEmpty()
        }.recoverCatching { throw it.toPortalError() }

    /** Same as [callList] but also parses `Content-Range` into [Page.total]. */
    private suspend fun <T> callPage(block: suspend () -> Response<List<T>>): Result<Page<T>> =
        runCatching {
            val response = block()
            if (!response.isSuccessful) throw httpError(response.code(), response)
            val body = response.body().orEmpty()
            Page(
                items = body,
                total = parseContentRangeTotal(response.headers()["Content-Range"]),
                offset = 0,
                limit = body.size
            )
        }.recoverCatching { throw it.toPortalError() }

    private fun httpError(code: Int, response: Response<*>): PortalError {
        val raw = response.errorBody()?.string().orEmpty()
        val message = runCatching {
            val obj = org.json.JSONObject(raw)
            obj.optString("message")
                .takeIf { it.isNotBlank() }
                ?: obj.optString("error_description")
        }.getOrNull()
        return when {
            code == 404 || raw.contains("PGRST205") ->
                PortalError.SchemaMissing("ডেটাবেজ টেবিল পাওয়া যায়নি। অনুগ্রহ করে সার্ভার কনফিগারেশন যাচাই করুন।")
            raw.contains("PGRST204") || raw.contains("42703") ->
                PortalError.SchemaMissing("ডেটাবেজ আপডেট প্রয়োজন।")
            else -> PortalError.Http(code, message?.takeIf { it.isNotBlank() } ?: "সার্ভার ত্রুটি ($code)")
        }
    }

    /** Example header "0-19/50" yields 50; "0-19/star" or malformed yields null. */
    internal fun parseContentRangeTotal(header: String?): Int? {
        if (header.isNullOrBlank()) return null
        val total = header.substringAfterLast('/', "").trim()
        return total.toIntOrNull()
    }

    /**
     * Percent-encode a value that is interpolated into a PostgREST filter.
     * Commas, parentheses and asterisks are structural in `or=`/`ilike`, so they
     * must never arrive unescaped from user input or a Bengali slug.
     */
    private fun escapeFilterValue(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private suspend fun <T> cached(
        key: String,
        store: MutableMap<String, CacheEntry<T>>,
        ttlMillis: Long,
        forceRefresh: Boolean,
        fetch: suspend () -> T
    ): Result<T> {
        if (!forceRefresh) {
            val hit = cacheMutex.withLock { store[key]?.takeIf { it.isFresh(ttlMillis) } }
            if (hit != null) return Result.success(hit.value)
        }
        return runCatching { fetch() }
            .onSuccess { value ->
                cacheMutex.withLock { store[key] = CacheEntry(value, System.currentTimeMillis()) }
            }
            .recoverCatching { error ->
                val stale = cacheMutex.withLock { store[key]?.value }
                if (stale != null) return@recoverCatching stale
                throw error.toPortalError()
            }
    }

    private inline fun <T, R> Page<T>.mapItems(transform: (T) -> R): Page<R> = Page(
        items = items.map(transform),
        total = total,
        offset = offset,
        limit = limit
    )
}
