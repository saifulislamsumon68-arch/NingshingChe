package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.ArticleEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.Article
import com.example.data.model.ArticleComment
import com.example.data.model.Author
import com.example.data.model.Bookmark
import com.example.data.model.Category
import com.example.data.model.PdfCategory
import com.example.data.model.PdfDocument
import com.example.data.model.ReadingHistory
import com.example.data.model.YearArchive
import com.example.data.remote.AuthorRecord
import com.example.data.remote.BlogRecord
import com.example.data.remote.CategoryRecord
import com.example.data.remote.CommentRecord
import com.example.data.remote.NingshingCheWebsiteClient
import com.example.data.remote.PdfBookRecord
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Log

data class WebsiteSyncState(
    val isSyncing: Boolean = false,
    val lastSuccessAt: Long = 0L,
    val lastMessage: String = "",
    val liveArticleCount: Int = 0,
    val usingLiveSite: Boolean = false
)

class ArticleRepository(
    private val database: AppDatabase,
    private val supabaseClient: SupabaseClient? = null,
    private val websiteClient: NingshingCheWebsiteClient = NingshingCheWebsiteClient()
) {

    private val articleDao = database.articleDao()
    private val bookmarkDao = database.bookmarkDao()
    private val historyDao = database.historyDao()
    private val searchDao = database.searchDao()
    /**
     * Startup and background work runs here. The [CoroutineExceptionHandler] is
     * essential: without it, any failure inside the `init` sync below propagates
     * to the process's uncaught-exception handler and Android kills the app on
     * launch, which is exactly what a cold offline start used to do.
     */
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Background article work failed", throwable)
        }
    )
    private val syncMutex = Mutex()

    companion object {
        private const val TAG = "ArticleRepository"
    }

    private val _categories = MutableStateFlow(NinghsingCheContentData.categories)
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _authors = MutableStateFlow(NinghsingCheContentData.authors)
    val authors: StateFlow<List<Author>> = _authors.asStateFlow()

    private val _yearArchives = MutableStateFlow(NinghsingCheContentData.yearArchives)
    val yearArchives: StateFlow<List<YearArchive>> = _yearArchives.asStateFlow()

    private val _pdfDocuments = MutableStateFlow(NinghsingCheContentData.pdfDocuments)
    val pdfDocuments: StateFlow<List<PdfDocument>> = _pdfDocuments.asStateFlow()

    private val _pdfCategories = MutableStateFlow(NinghsingCheContentData.pdfCategories)
    val pdfCategories: StateFlow<List<PdfCategory>> = _pdfCategories.asStateFlow()

    private val _syncState = MutableStateFlow(WebsiteSyncState())
    val syncState: StateFlow<WebsiteSyncState> = _syncState.asStateFlow()

    init {
        scope.launch {
            runCatching {
                val existing = articleDao.getAllArticles().first()
                if (existing.isEmpty()) {
                    seedInitialArticles()
                } else {
                    rebuildCatalogsFrom(existing.map { it.toModel() })
                }
                syncFromSupabaseOrWebsite()
            }.onFailure { error ->
                // Offline, empty cache, or an unexpected schema change must never
                // stop the app from opening.
                Log.e(TAG, "Startup sync skipped; continuing with local data.", error)
            }
        }
    }

    suspend fun seedInitialArticles() {
        val entities = NinghsingCheContentData.articles.map { it.toEntity() }
        articleDao.insertArticles(entities)
    }

    suspend fun syncFromSupabaseOrWebsite(): Result<Int> = syncMutex.withLock {
        _syncState.value = _syncState.value.copy(
            isSyncing = true,
            lastMessage = "সুপাবেজ (Supabase) এপিআই থেকে ডাটা লোড হচ্ছে..."
        )

        // 1. Try Supabase direct API first
        if (supabaseClient != null) {
            try {
                val blogsResult = supabaseClient.getBlogs(status = "Publish")
                if (blogsResult.isSuccess) {
                    val blogs = blogsResult.getOrNull().orEmpty()
                    if (blogs.isNotEmpty()) {
                        val articles = blogs.map { it.toArticle() }
                        upsertLiveArticles(articles)
                        articleDao.deleteSeedArticles()

                        // Categories from Supabase
                        val catResult = supabaseClient.getCategories()
                        if (catResult.isSuccess && !catResult.getOrNull().isNullOrEmpty()) {
                            _categories.value = catResult.getOrNull()!!.map { it.toCategory() }
                        } else {
                            _categories.value = buildCategories(articles)
                        }

                        // Authors from Supabase
                        val authorResult = supabaseClient.getAuthors()
                        if (authorResult.isSuccess && !authorResult.getOrNull().isNullOrEmpty()) {
                            _authors.value = authorResult.getOrNull()!!.map { it.toAuthor() }
                        } else {
                            _authors.value = NingshingCheWebsiteClient.buildAuthors(articles)
                        }

                        // PDF Books from Supabase
                        val pdfResult = supabaseClient.getPdfBooks()
                        if (pdfResult.isSuccess && !pdfResult.getOrNull().isNullOrEmpty()) {
                            val docs = pdfResult.getOrNull()!!.map { it.toPdfDocument() }
                            _pdfDocuments.value = docs
                            _pdfCategories.value = buildPdfCategories(docs)
                        }

                        _yearArchives.value = buildYearArchives(articles)

                        _syncState.value = WebsiteSyncState(
                            isSyncing = false,
                            lastSuccessAt = System.currentTimeMillis(),
                            lastMessage = "সুপাবেজ (Supabase) থেকে ${articles.size}টি প্রবন্ধ সিঙ্ক সম্পন্ন",
                            liveArticleCount = articles.size,
                            usingLiveSite = true
                        )
                        return Result.success(articles.size)
                    }
                }
            } catch (_: Exception) {
                // Fallthrough to website client or cache
            }
        }

        // 2. Fallback to website scraping if Supabase is unavailable or empty
        _syncState.value = _syncState.value.copy(
            isSyncing = true,
            lastMessage = "নিংশিংচে.কম থেকে হালনাগাদ হচ্ছে..."
        )
        val result = websiteClient.syncCatalog()
        return result.fold(
            onSuccess = { listing ->
                if (listing.articles.isNotEmpty()) {
                    upsertLiveArticles(listing.articles)
                    articleDao.deleteSeedArticles()
                    _categories.value = listing.categories.ifEmpty { buildCategories(listing.articles) }
                    _authors.value = listing.authors.ifEmpty { NingshingCheWebsiteClient.buildAuthors(listing.articles) }
                    _yearArchives.value = buildYearArchives(listing.articles)
                }
                if (listing.pdfDocuments.isNotEmpty()) {
                    _pdfDocuments.value = listing.pdfDocuments
                    _pdfCategories.value = buildPdfCategories(listing.pdfDocuments)
                }
                prefetchFeaturedBodies(listing.articles)
                _syncState.value = WebsiteSyncState(
                    isSyncing = false,
                    lastSuccessAt = System.currentTimeMillis(),
                    lastMessage = "নিংশিংচে.কম থেকে ${listing.articles.size}টি প্রবন্ধ হালনাগাদ হয়েছে",
                    liveArticleCount = listing.articles.size,
                    usingLiveSite = listing.articles.isNotEmpty()
                )
                Result.success(listing.articles.size)
            },
            onFailure = { error ->
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    lastMessage = "সিঙ্ক সম্পন্ন: অফলাইন আর্কাইভ প্রস্তুত।"
                )
                Result.failure(error)
            }
        )
    }

    fun syncFromWebsite() {
        refreshInBackground()
    }

    fun refreshInBackground() {
        scope.launch { syncFromSupabaseOrWebsite() }
    }

    fun getAllArticles(): Flow<List<Article>> {
        return articleDao.getAllArticles().map { entities ->
            val live = entities.filter { !it.id.startsWith("art-") }
            when {
                live.isNotEmpty() -> live.map { it.toModel() }
                entities.isNotEmpty() -> entities.map { it.toModel() }
                else -> NinghsingCheContentData.articles
            }
        }
    }

    fun getFeaturedArticles(): Flow<List<Article>> {
        return articleDao.getFeaturedArticles().map { entities ->
            val live = entities.filter { !it.id.startsWith("art-") }
            when {
                live.isNotEmpty() -> live.map { it.toModel() }
                entities.isNotEmpty() -> entities.map { it.toModel() }
                else -> NinghsingCheContentData.articles.filter { it.isFeatured }
            }
        }
    }

    suspend fun getArticleById(idOrSlug: String): Article? {
        val key = normalizeKey(idOrSlug)
        val entity = articleDao.getArticleByIdOrSlug(key)
            ?: articleDao.getArticleByIdOrSlug(idOrSlug)
            ?: articleDao.getArticleByIdOrSlug(idOrSlug.removeSuffix(".kehem"))

        val local = entity?.toModel()
            ?: NinghsingCheContentData.articles.find { it.id == key || it.slug == key }

        // If local article has full content, return immediately
        if (local != null && local.content.isNotBlank() && local.content.length > 100) {
            return local
        }

        // Try Supabase fetch
        if (supabaseClient != null) {
            val blogRes = supabaseClient.getBlogById(key)
            if (blogRes.isSuccess && blogRes.getOrNull() != null) {
                val art = blogRes.getOrNull()!!.toArticle()
                articleDao.insertArticle(art.toEntity())
                return art
            }
        }

        val dirty = local?.content?.contains("article-content") == true ||
            local?.content?.contains("id=\"") == true ||
            local?.content?.contains("<p") == true
        val needsBody = local == null || local.content.isBlank() || local.content.length < 80 || dirty
        if (needsBody) {
            val remote = websiteClient.fetchArticle(local?.sourceUrl?.takeIf { it.contains(".kehem") } ?: idOrSlug)
                ?: local?.sourceUrl?.let { websiteClient.fetchArticle(it) }
            if (remote != null) {
                val merged = mergeArticle(local, remote)
                articleDao.insertArticle(merged.toEntity())
                return merged
            }
        }
        return local
    }

    fun getArticlesByCategory(categorySlug: String): Flow<List<Article>> {
        return articleDao.getArticlesByCategory(categorySlug).map { entities ->
            val mapped = entities.map { it.toModel() }
            mapped.ifEmpty { NinghsingCheContentData.articles.filter { it.categorySlug == categorySlug } }
        }
    }

    fun getArticlesByAuthor(authorId: String): Flow<List<Article>> {
        return articleDao.getArticlesByAuthor(authorId).map { entities ->
            val mapped = entities.map { it.toModel() }
            mapped.ifEmpty { NinghsingCheContentData.articles.filter { it.authorId == authorId } }
        }
    }

    fun getArticlesByYear(year: Int): Flow<List<Article>> {
        return articleDao.getArticlesByYear(year).map { entities ->
            val mapped = entities.map { it.toModel() }
            mapped.ifEmpty { NinghsingCheContentData.articles.filter { it.year == year } }
        }
    }

    fun searchArticles(query: String, categorySlug: String? = null, year: Int? = null): Flow<List<Article>> {
        val trimmed = query.trim()
        return flow {
            val all = if (trimmed.isEmpty()) {
                articleDao.getAllArticles().first().map { it.toModel() }
                    .ifEmpty { NinghsingCheContentData.articles }
            } else {
                val dbResults = articleDao.searchArticles(trimmed).first().map { it.toModel() }
                dbResults.ifEmpty {
                    NinghsingCheContentData.articles.filter {
                        it.title.contains(trimmed, ignoreCase = true) ||
                            it.content.contains(trimmed, ignoreCase = true) ||
                            it.authorName.contains(trimmed, ignoreCase = true) ||
                            it.category.contains(trimmed, ignoreCase = true) ||
                            it.tags.any { tag -> tag.contains(trimmed, ignoreCase = true) }
                    }
                }
            }

            emit(
                all.filter { article ->
                    val matchCategory = categorySlug == null || article.categorySlug == categorySlug
                    val matchYear = year == null || article.year == year
                    matchCategory && matchYear
                }
            )
        }
    }

    suspend fun loadComments(articleUrlOrId: String): List<ArticleComment> {
        if (supabaseClient != null) {
            val key = normalizeKey(articleUrlOrId)
            val res = supabaseClient.getComments(blogId = key, status = "Publish")
            if (res.isSuccess && !res.getOrNull().isNullOrEmpty()) {
                return res.getOrNull()!!.map {
                    ArticleComment(
                        name = it.name.ifBlank { "পাঠক" },
                        content = it.content,
                        meta = it.createdAt.take(10).ifBlank { "আজ" }
                    )
                }
            }
        }
        return websiteClient.loadComments(articleUrlOrId)
    }

    suspend fun submitComment(
        articleUrlOrId: String,
        name: String,
        address: String,
        email: String,
        phone: String,
        content: String
    ): Result<String> {
        if (supabaseClient != null) {
            val key = normalizeKey(articleUrlOrId)
            val comment = CommentRecord(
                blogId = key,
                blogTitle = articleUrlOrId,
                name = name,
                address = address,
                email = email,
                phone = phone,
                content = content,
                status = "Publish"
            )
            val res = supabaseClient.upsertComment(comment)
            if (res.isSuccess) {
                return Result.success("আপনার মন্তব্য সফলভাবে প্রকাশিত হয়েছে!")
            }
        }
        return websiteClient.submitComment(articleUrlOrId, name, address, email, phone, content)
    }

    fun getCategories(): List<Category> = _categories.value

    fun getCategoryBySlug(slug: String): Category? =
        _categories.value.find { it.slug == slug } ?: NinghsingCheContentData.categories.find { it.slug == slug }

    fun getAuthors(): List<Author> = _authors.value

    fun getAuthorById(id: String): Author? =
        _authors.value.find { it.id == id } ?: NinghsingCheContentData.authors.find { it.id == id }

    fun getYearArchives(): List<YearArchive> = _yearArchives.value

    fun getYearArchiveByYear(year: Int): YearArchive? =
        _yearArchives.value.find { it.year == year } ?: NinghsingCheContentData.yearArchives.find { it.year == year }

    fun getPdfCategories(): List<PdfCategory> = _pdfCategories.value

    fun getPdfDocuments(): List<PdfDocument> = _pdfDocuments.value

    fun getPdfDocumentById(id: String): PdfDocument? = _pdfDocuments.value.find { it.id == id }

    fun getPdfDocumentsByCategory(categoryIdOrSlug: String): List<PdfDocument> {
        if (categoryIdOrSlug == "pdf-cat-all" || categoryIdOrSlug.isBlank()) return _pdfDocuments.value
        return _pdfDocuments.value.filter { it.categorySlug == categoryIdOrSlug || it.category == categoryIdOrSlug }
    }

    fun getAllBookmarks(): Flow<List<Bookmark>> {
        return bookmarkDao.getAllBookmarks().map { entities ->
            entities.map { Bookmark(it.articleId, it.savedAtTimestamp, it.folder, it.note) }
        }
    }

    fun isBookmarked(articleId: String): Flow<Boolean> = bookmarkDao.isBookmarked(articleId)

    suspend fun toggleBookmark(articleId: String) {
        val exists = bookmarkDao.isBookmarkedDirect(articleId)
        if (exists) {
            bookmarkDao.deleteBookmark(articleId)
        } else {
            bookmarkDao.insertBookmark(BookmarkEntity(articleId = articleId))
        }
    }

    fun getReadingHistory(): Flow<List<ReadingHistory>> {
        return historyDao.getAllHistory().map { entities ->
            entities.map { ReadingHistory(it.articleId, it.readAtTimestamp, it.scrollPosition, it.progressPercent) }
        }
    }

    suspend fun saveReadingProgress(articleId: String, scrollPos: Int, progress: Float) {
        historyDao.insertHistory(
            HistoryEntity(
                articleId = articleId,
                readAtTimestamp = System.currentTimeMillis(),
                scrollPosition = scrollPos,
                progressPercent = progress
            )
        )
    }

    suspend fun clearHistory() {
        historyDao.clearAll()
    }

    fun getRecentSearches(): Flow<List<String>> {
        return searchDao.getRecentSearches().map { entities ->
            entities.map { it.query }
        }
    }

    suspend fun recordSearch(query: String) {
        if (query.isNotBlank()) {
            searchDao.insertSearch(SearchHistoryEntity(query = query.trim()))
        }
    }

    suspend fun removeSearch(query: String) {
        searchDao.deleteSearch(query)
    }

    suspend fun clearSearchHistory() {
        searchDao.clearAll()
    }

    suspend fun clearAllCache() {
        articleDao.clearAll()
        seedInitialArticles()
        _categories.value = NinghsingCheContentData.categories
        _authors.value = NinghsingCheContentData.authors
        _yearArchives.value = NinghsingCheContentData.yearArchives
        _pdfDocuments.value = NinghsingCheContentData.pdfDocuments
        _pdfCategories.value = NinghsingCheContentData.pdfCategories
        syncFromSupabaseOrWebsite()
    }

    private suspend fun upsertLiveArticles(articles: List<Article>) {
        val existing = articleDao.getAllArticles().first().associateBy { it.id }
        val entities = articles.map { incoming ->
            val previous = existing[incoming.id] ?: existing.values.find { it.slug == incoming.slug }
            if (previous != null && previous.content.length > incoming.content.length) {
                incoming.copy(
                    content = previous.content,
                    excerpt = incoming.excerpt.ifBlank { previous.excerpt },
                    featuredImageUrl = incoming.featuredImageUrl.ifBlank { previous.featuredImageUrl },
                    authorAvatarUrl = incoming.authorAvatarUrl.ifBlank { previous.authorAvatarUrl }
                ).toEntity()
            } else {
                incoming.toEntity()
            }
        }
        articleDao.insertArticles(entities)
    }

    private suspend fun prefetchFeaturedBodies(articles: List<Article>) = coroutineScope {
        articles.filter { it.isFeatured || it.isEditorialPick }
            .distinctBy { it.id }
            .take(8)
            .map { article ->
                async {
                    if (article.content.length >= 80) return@async
                    val remote = websiteClient.fetchArticle(article.sourceUrl) ?: return@async
                    articleDao.insertArticle(mergeArticle(article, remote).toEntity())
                }
            }.awaitAll()
    }

    private fun mergeArticle(local: Article?, remote: Article): Article {
        if (local == null) return remote
        return remote.copy(
            isFeatured = local.isFeatured || remote.isFeatured,
            isEditorialPick = local.isEditorialPick || remote.isEditorialPick,
            category = remote.category.ifBlank { local.category },
            categorySlug = remote.categorySlug.ifBlank { local.categorySlug },
            authorName = remote.authorName.ifBlank { local.authorName },
            authorAvatarUrl = remote.authorAvatarUrl.ifBlank { local.authorAvatarUrl },
            featuredImageUrl = remote.featuredImageUrl.ifBlank { local.featuredImageUrl },
            excerpt = remote.excerpt.ifBlank { local.excerpt },
            content = remote.content.ifBlank { local.content },
            viewCount = maxOf(local.viewCount, remote.viewCount)
        )
    }

    private fun rebuildCatalogsFrom(articles: List<Article>) {
        val live = articles.filter { !it.id.startsWith("art-") }
        if (live.isEmpty()) return
        _categories.value = NingshingCheWebsiteClient.buildCategories(live)
        _authors.value = NingshingCheWebsiteClient.buildAuthors(live)
        _yearArchives.value = buildYearArchives(live)
        _syncState.value = _syncState.value.copy(
            liveArticleCount = live.size,
            usingLiveSite = true,
            lastMessage = "অফলাইন ক্যাশে ${live.size}টি প্রবন্ধ"
        )
    }

    private fun buildCategories(articles: List<Article>): List<Category> =
        NingshingCheWebsiteClient.buildCategories(articles)

    private fun buildYearArchives(articles: List<Article>): List<YearArchive> {
        return articles.groupBy { it.year }
            .toSortedMap(compareByDescending { it })
            .map { (year, list) ->
                val bn = NingshingCheWebsiteClient.toBengaliDigits(year)
                YearArchive(
                    year = year,
                    bengaliYearText = bn,
                    title = "নিংশিং চে — $bn",
                    description = "নিংশিং চে-তে $bn সালে প্রকাশিত প্রবন্ধ ও সংখ্যা।",
                    issueCount = 1,
                    articleCount = list.size
                )
            }
    }

    private fun buildPdfCategories(docs: List<PdfDocument>): List<PdfCategory> {
        val groups = docs.groupBy { it.categorySlug }
        return listOf(
            PdfCategory("pdf-cat-all", "সকল PDF", "নিংশিং চে-র সকল ডিজিটাল প্রকাশনা।", docs.size)
        ) + groups.map { (slug, list) ->
            PdfCategory(slug, list.first().category, list.first().category, list.size)
        }
    }

    private fun normalizeKey(raw: String): String {
        return raw.trim()
            .removePrefix("https://ningshingche.com/article/")
            .removePrefix("https://ningshingche.com/")
            .removePrefix("http://ningshingche.com/")
            .removePrefix("/")
            .removeSuffix(".kehem")
            .substringAfterLast('/')
    }

    private fun BlogRecord.toArticle(): Article {
        val pubYear = publishedDate.filter { it.isDigit() }.take(4).toIntOrNull()
            ?: createdAt.filter { it.isDigit() }.take(4).toIntOrNull()
            ?: 2026
        return Article(
            id = id,
            title = title,
            slug = slug.ifBlank { id },
            excerpt = subTitle.ifBlank {
                content.replace(Regex("<[^>]*>"), " ").take(160).trim()
            },
            content = content,
            featuredImageUrl = image,
            authorId = authorId,
            authorName = authorName.ifBlank { "নিংশিং চে লেখক" },
            authorAvatarUrl = authorImage,
            category = categoryTitle.ifBlank { "সাধারণ" },
            categorySlug = categorySlug.ifBlank { "general" },
            tags = tags,
            publishedDate = publishedDate.ifBlank { "২০২৬" },
            year = pubYear,
            readingTimeMinutes = readingTimeMinutes.coerceAtLeast(1),
            isFeatured = isFeature || isSlider,
            isEditorialPick = isSpecialArticle,
            viewCount = viewsCount,
            sourceUrl = "https://ningshingche.com/article/${slug.ifBlank { id }}",
            relatedArticleIds = emptyList()
        )
    }

    private fun CategoryRecord.toCategory(): Category {
        return Category(
            id = id,
            name = title,
            slug = slug,
            description = subTitle,
            articleCount = blogCount,
            iconName = iconName.ifBlank { "article" },
            imageUrl = ""
        )
    }

    private fun AuthorRecord.toAuthor(): Author {
        return Author(
            id = id,
            name = title,
            designation = designation,
            bio = description,
            avatarUrl = image,
            articleCount = articleCount,
            location = location.ifBlank { "বাংলাদেশ / ভারত" },
            topics = emptyList(),
            isVerified = isVerified
        )
    }

    private fun PdfBookRecord.toPdfDocument(): PdfDocument {
        val yr = bookPublishedDate.filter { it.isDigit() }.take(4).toIntOrNull() ?: 2026
        return PdfDocument(
            id = id,
            title = title,
            edition = edition,
            category = category,
            categorySlug = category.replace(Regex("[^a-zA-Z0-9]"), "-").lowercase().ifBlank { "archive" },
            year = yr,
            authorOrEditor = authorOrEditor,
            pageCount = pageCount,
            fileSizeMb = fileSizeMb,
            pdfUrl = link,
            coverImageUrl = image,
            description = description,
            tags = emptyList(),
            downloadUrl = link
        )
    }

    private fun Article.toEntity(): ArticleEntity {
        return ArticleEntity(
            id = id,
            title = title,
            slug = slug,
            excerpt = excerpt,
            content = content,
            featuredImageUrl = featuredImageUrl,
            authorId = authorId,
            authorName = authorName,
            authorAvatarUrl = authorAvatarUrl,
            category = category,
            categorySlug = categorySlug,
            tagsRaw = tags.joinToString(","),
            publishedDate = publishedDate,
            year = year,
            readingTimeMinutes = readingTimeMinutes,
            isFeatured = isFeatured,
            isEditorialPick = isEditorialPick,
            viewCount = viewCount,
            sourceUrl = sourceUrl,
            relatedArticleIdsRaw = relatedArticleIds.joinToString(",")
        )
    }

    private fun ArticleEntity.toModel(): Article {
        return Article(
            id = id,
            title = title,
            slug = slug,
            excerpt = excerpt,
            content = content,
            featuredImageUrl = featuredImageUrl,
            authorId = authorId,
            authorName = authorName,
            authorAvatarUrl = authorAvatarUrl,
            category = category,
            categorySlug = categorySlug,
            tags = if (tagsRaw.isNotBlank()) tagsRaw.split(",").map { it.trim() } else emptyList(),
            publishedDate = publishedDate,
            year = year,
            readingTimeMinutes = readingTimeMinutes,
            isFeatured = isFeatured,
            isEditorialPick = isEditorialPick,
            viewCount = viewCount,
            sourceUrl = sourceUrl,
            relatedArticleIds = if (relatedArticleIdsRaw.isNotBlank()) relatedArticleIdsRaw.split(",").map { it.trim() } else emptyList()
        )
    }
}

