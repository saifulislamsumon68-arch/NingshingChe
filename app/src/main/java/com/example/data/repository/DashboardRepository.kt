package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.ArticleEntity
import com.example.data.model.Article
import com.example.data.remote.AuthorRecord
import com.example.data.remote.BlogRecord
import com.example.data.remote.CategoryRecord
import com.example.data.remote.CommentRecord
import com.example.data.remote.DashboardSummaryStats
import com.example.data.remote.GalleryRecord
import com.example.data.remote.PdfBookRecord
import com.example.data.remote.RecentActivityItem
import com.example.data.remote.SiteSettingsRecord
import com.example.data.remote.SubmittedBlogRecord
import com.example.data.remote.SupabaseClient
import com.example.data.remote.UserProfile
import com.example.data.remote.UserRole
import com.example.data.remote.VideoRecord
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import java.util.UUID

class DashboardRepository(
    private val context: Context,
    val supabaseClient: SupabaseClient,
    private val database: AppDatabase
) {
    companion object {
        private const val TAG = "DashboardRepository"
    }

    private val scope = CoroutineScope(
        Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Background dashboard work failed", throwable)
        }
    )

    private val _authors = MutableStateFlow<List<AuthorRecord>>(emptyList())
    val authors: StateFlow<List<AuthorRecord>> = _authors.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryRecord>>(emptyList())
    val categories: StateFlow<List<CategoryRecord>> = _categories.asStateFlow()

    private val _blogs = MutableStateFlow<List<BlogRecord>>(emptyList())
    val blogs: StateFlow<List<BlogRecord>> = _blogs.asStateFlow()

    private val _comments = MutableStateFlow<List<CommentRecord>>(emptyList())
    val comments: StateFlow<List<CommentRecord>> = _comments.asStateFlow()

    private val _galleries = MutableStateFlow<List<GalleryRecord>>(emptyList())
    val galleries: StateFlow<List<GalleryRecord>> = _galleries.asStateFlow()

    private val _pdfBooks = MutableStateFlow<List<PdfBookRecord>>(emptyList())
    val pdfBooks: StateFlow<List<PdfBookRecord>> = _pdfBooks.asStateFlow()

    private val _submittedBlogs = MutableStateFlow<List<SubmittedBlogRecord>>(emptyList())
    val submittedBlogs: StateFlow<List<SubmittedBlogRecord>> = _submittedBlogs.asStateFlow()

    private val _videos = MutableStateFlow<List<VideoRecord>>(emptyList())
    val videos: StateFlow<List<VideoRecord>> = _videos.asStateFlow()

    private val _settings = MutableStateFlow(SiteSettingsRecord())
    val settings: StateFlow<SiteSettingsRecord> = _settings.asStateFlow()

    private val _summaryStats = MutableStateFlow(DashboardSummaryStats())
    val summaryStats: StateFlow<DashboardSummaryStats> = _summaryStats.asStateFlow()

    private val _recentActivities = MutableStateFlow<List<RecentActivityItem>>(emptyList())
    val recentActivities: StateFlow<List<RecentActivityItem>> = _recentActivities.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        scope.launch {
            runCatching {
                initializeDefaultsFromSeed()
                refreshAll()
            }.onFailure { error ->
                Log.e(TAG, "Startup refresh skipped; continuing with seeded data.", error)
            }
        }
    }

    private suspend fun initializeDefaultsFromSeed() = withContext(Dispatchers.IO) {
        // Seed default initial categories from NinghsingCheContentData
        val initialCats = NinghsingCheContentData.categories.map {
            CategoryRecord(
                id = it.id,
                title = it.name,
                subTitle = it.description,
                slug = it.slug,
                iconName = it.iconName,
                blogCount = it.articleCount
            )
        }
        _categories.value = initialCats

        // Seed default authors
        val initialAuthors = NinghsingCheContentData.authors.map {
            AuthorRecord(
                id = it.id,
                title = it.name,
                image = it.avatarUrl,
                designation = it.designation,
                description = it.bio,
                isVerified = true,
                location = it.location,
                articleCount = it.articleCount
            )
        }
        _authors.value = initialAuthors

        // Seed default blogs
        val initialBlogs = NinghsingCheContentData.articles.mapIndexed { index, art ->
            BlogRecord(
                id = art.id,
                title = art.title,
                subTitle = art.excerpt.take(90),
                image = art.featuredImageUrl,
                content = art.content,
                categoryId = NinghsingCheContentData.categories.find { it.slug == art.categorySlug }?.id ?: "cat-1",
                categoryTitle = art.category,
                categorySlug = art.categorySlug,
                status = "Publish",
                tags = art.tags,
                seoTitle = art.title,
                slug = art.slug,
                authorId = art.authorId,
                authorName = art.authorName,
                authorImage = art.authorAvatarUrl,
                isSlider = index < 3, // first 3 in slider carousel
                isFeature = art.isFeatured,
                isSpecialArticle = art.isEditorialPick,
                viewsCount = art.viewCount,
                readingTimeMinutes = art.readingTimeMinutes,
                publishedDate = art.publishedDate
            )
        }
        _blogs.value = initialBlogs

        // Seed default PDF books
        val initialPdfs = NinghsingCheContentData.pdfDocuments.map {
            PdfBookRecord(
                id = it.id,
                title = it.title,
                image = it.coverImageUrl,
                bookPublishedDate = "${it.year}",
                link = it.pdfUrl,
                authorOrEditor = it.authorOrEditor,
                edition = it.edition,
                category = it.category,
                pageCount = it.pageCount,
                fileSizeMb = it.fileSizeMb,
                description = it.description
            )
        }
        _pdfBooks.value = initialPdfs

        // Seed sample galleries
        val initialGalleries = listOf(
            GalleryRecord(
                id = "gal-1",
                title = "মহারাসলীলা উৎসব ও রাখাল নৃত্য",
                description = "কমলগঞ্জের মাধবপুর জোড়ামণ্ডপে শতবর্ষের ঐতিহ্যবাহী রাসলীলা ও নৃত্যানুষ্ঠান",
                image = "https://images.unsplash.com/photo-1544717305-2782549b5136?w=1000&auto=format&fit=crop&q=80",
                category = "রাসোৎসব ও মেলা"
            ),
            GalleryRecord(
                id = "gal-2",
                title = "ঐতিহ্যবাহী বিষ্ণুপ্রিয়া মণিপুরি তাঁতশিল্প",
                description = "হাতে বোনা ইনফি ও খাম্বানাউ নকশার অনবদ্য হস্তশিল্প নিদর্শন",
                image = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=1000&auto=format&fit=crop&q=80",
                category = "তাঁত ও হস্তশিল্প"
            ),
            GalleryRecord(
                id = "gal-3",
                title = "নিংশিং চে ঐতিহাসিক সাময়িকী সংকলন",
                description = "সাহিত্য ও ভাষা আন্দোলনের মূল্যবান ঐতিহাসিক দুর্লভ আলোকচিত্র",
                image = "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=1000&auto=format&fit=crop&q=80",
                category = "সাহিত্য ও দলিল"
            )
        )
        _galleries.value = initialGalleries

        // Seed sample videos
        val initialVideos = listOf(
            VideoRecord(
                id = "vid-1",
                title = "বিষ্ণুপ্রিয়া মণিপুরি রাস নৃত্য ও লোকসংস্কৃতি ডকুমেন্টারি",
                videoLink = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                platform = "YouTube",
                description = "ঐতিহ্যবাহী রাস নৃত্যের গঠনরীতি ও ঐতিহাসিক গুরুত্ব সম্পর্কিত প্রামাণ্যচিত্র"
            ),
            VideoRecord(
                id = "vid-2",
                title = "নিংশিং চে প্রকাশনা উৎসব ও কবি সম্মেলন",
                videoLink = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                platform = "YouTube",
                description = "বার্ষিক সাহিত্য অধিবেশন ও গুণীজন সম্মাননা প্রদান অনুষ্ঠান"
            )
        )
        _videos.value = initialVideos

        // Seed sample comments
        val initialComments = listOf(
            CommentRecord(
                id = "comm-1",
                blogId = "art-1",
                blogTitle = "বিষ্ণুপ্রিয়া মণিপুরি ভাষার উৎপত্তি ও প্রাচীন সাহিত্য",
                name = "সুনীল সিংহ",
                address = "সিলেট, বাংলাদেশ",
                phone = "+880 1711-223344",
                email = "sunil.singha@gmail.com",
                content = "অত্যন্ত তথ্যবহুল ও গবেষণাধর্মী একটি প্রবন্ধ। নতুন প্রজন্মের জন্য এই ধরণের লেখা বেশি বেশি প্রকাশ হওয়া জরুরি।",
                status = "Publish"
            ),
            CommentRecord(
                id = "comm-2",
                blogId = "art-2",
                blogTitle = "মহারাসলীলার আধ্যাত্মিক ও নৃতাত্ত্বিক গুরুত্ব",
                name = "অনিতা দেবী",
                address = "মৌলভীবাজার",
                phone = "+880 1819-556677",
                email = "anita.devi@yahoo.com",
                content = "রাস নৃত্যের প্রতিটি মুদ্রা ও ভাবার্থ চমৎকারভাবে ফুটে উঠেছে। নিংশিং চে কে ধন্যবাদ।",
                status = "Publish"
            )
        )
        _comments.value = initialComments

        // Seed sample submitted blogs
        val initialSubs = listOf(
            SubmittedBlogRecord(
                id = "sub-1",
                title = "মণিপুরি লোকসংগীত ও ইশেই ধারার বিবর্তন",
                designation = "গবেষক ও সংগীতশিল্পী",
                address = "কমলগঞ্জ, মৌলভীবাজার",
                phone = "+880 1722-334455",
                thumbnail = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&auto=format&fit=crop&q=80",
                writerName = "সঞ্জয় শর্মা",
                writerDesignation = "এম.ফিল গবেষক, ঢাকা বিশ্ববিদ্যালয়",
                writerEmail = "sanjay.sharma@du.ac.bd",
                contentTitle = "মণিপুরি লোকসংগীতের শিকড় অন্বেষণ",
                content = "<p>বিষ্ণুপ্রিয়া মণিপুরি লোকসংগীতের ঐতিহ্য কয়েক শতাব্দী প্রাচীন। প্রাচীন বৈষ্ণব পদাবলী এবং লোকজীবনের সুখ-দুঃখের রূপায়ণে এই গানগুলো লোকসমাজে আজও সমান জীবন্ত।</p>",
                status = "Pending"
            )
        )
        _submittedBlogs.value = initialSubs

        recalculateStatsAndActivities()
    }

    suspend fun refreshAll() = withContext(Dispatchers.IO) {
        _isLoading.value = true
        try {
            // Authors
            supabaseClient.getAuthors().onSuccess { if (it.isNotEmpty()) _authors.value = it }
            // Categories
            supabaseClient.getCategories().onSuccess { if (it.isNotEmpty()) _categories.value = it }
            // Blogs
            supabaseClient.getBlogs().onSuccess { if (it.isNotEmpty()) {
                _blogs.value = it
                syncBlogsToLocalRoom(it)
            }}
            // Comments
            supabaseClient.getComments().onSuccess { if (it.isNotEmpty()) _comments.value = it }
            // Galleries
            supabaseClient.getGalleries().onSuccess { if (it.isNotEmpty()) _galleries.value = it }
            // PDF Books
            supabaseClient.getPdfBooks().onSuccess { if (it.isNotEmpty()) _pdfBooks.value = it }
            // Submitted Blogs
            supabaseClient.getSubmittedBlogs().onSuccess { if (it.isNotEmpty()) _submittedBlogs.value = it }
            // Videos
            supabaseClient.getVideos().onSuccess { if (it.isNotEmpty()) _videos.value = it }
            // Settings
            supabaseClient.getSettings().onSuccess { _settings.value = it }
        } catch (_: Exception) {
            // Fallback continues with current state
        } finally {
            recalculateStatsAndActivities()
            _isLoading.value = false
        }
    }

    private suspend fun syncBlogsToLocalRoom(blogs: List<BlogRecord>) = withContext(Dispatchers.IO) {
        try {
            val entities = blogs.filter { it.isPublished }.map { b ->
                ArticleEntity(
                    id = b.id,
                    title = b.title,
                    slug = b.slug,
                    excerpt = b.subTitle.ifBlank { b.content.take(120) },
                    content = b.content,
                    featuredImageUrl = b.image,
                    authorId = b.authorId,
                    authorName = b.authorName,
                    authorAvatarUrl = b.authorImage,
                    category = b.categoryTitle,
                    categorySlug = b.categorySlug,
                    tagsRaw = b.tags.joinToString(","),
                    publishedDate = b.publishedDate.ifBlank { "২০২৬" },
                    year = 2026,
                    readingTimeMinutes = b.readingTimeMinutes,
                    isFeatured = b.isFeature || b.isSlider,
                    isEditorialPick = b.isSpecialArticle,
                    viewCount = b.viewsCount,
                    sourceUrl = "https://ningshingche.com/article/${b.slug}",
                    relatedArticleIdsRaw = ""
                )
            }
            if (entities.isNotEmpty()) {
                database.articleDao().insertArticles(entities)
            }
        } catch (_: Exception) { }
    }

    private fun recalculateStatsAndActivities() {
        val totalBlogs = _blogs.value.size
        val pubBlogs = _blogs.value.count { it.isPublished }
        val draftBlogs = _blogs.value.count { !it.isPublished }
        val pendingSubs = _submittedBlogs.value.count { it.status.equals("Pending", ignoreCase = true) }
        val totalComments = _comments.value.size
        val pubComments = _comments.value.count { it.isPublished }
        val views = _blogs.value.sumOf { it.viewsCount }

        _summaryStats.value = DashboardSummaryStats(
            totalAuthors = _authors.value.size,
            totalBlogs = totalBlogs,
            publishedBlogs = pubBlogs,
            draftBlogs = draftBlogs,
            pendingSubmittedBlogs = pendingSubs,
            totalCategories = _categories.value.size,
            totalComments = totalComments,
            publishedComments = pubComments,
            totalGalleries = _galleries.value.size,
            totalPdfBooks = _pdfBooks.value.size,
            totalVideos = _videos.value.size,
            totalViews = views
        )

        // Build Recent Activity Feed
        val activities = mutableListOf<RecentActivityItem>()
        _blogs.value.take(4).forEach {
            activities.add(
                RecentActivityItem(
                    id = it.id,
                    title = it.title,
                    subtitle = "লেখক: ${it.authorName.ifBlank { "সম্পাদক" }} • ${it.categoryTitle}",
                    type = "blog",
                    timestamp = it.publishedDate.ifBlank { "সম্প্রতি" },
                    status = it.status
                )
            )
        }
        _submittedBlogs.value.take(3).forEach {
            activities.add(
                RecentActivityItem(
                    id = it.id,
                    title = it.title,
                    subtitle = "পাঠক জমা দিয়েছেন: ${it.writerName}",
                    type = "submission",
                    timestamp = "পর্যালোচনাধীন",
                    status = it.status
                )
            )
        }
        _comments.value.take(3).forEach {
            activities.add(
                RecentActivityItem(
                    id = it.id,
                    title = "মন্তব্য: ${it.name}",
                    subtitle = it.content.take(60),
                    type = "comment",
                    timestamp = "প্রবন্ধে",
                    status = it.status
                )
            )
        }
        _pdfBooks.value.take(2).forEach {
            activities.add(
                RecentActivityItem(
                    id = it.id,
                    title = it.title,
                    subtitle = "প্রকাশনা: ${it.edition} • ${it.authorOrEditor}",
                    type = "book",
                    timestamp = it.bookPublishedDate,
                    status = "সক্রিয়"
                )
            )
        }
        _recentActivities.value = activities
    }

    // ==========================================
    // AUTHORS ACTIONS
    // ==========================================

    suspend fun saveAuthor(author: AuthorRecord): Result<AuthorRecord> = withContext(Dispatchers.IO) {
        val current = _authors.value.toMutableList()
        val index = current.indexOfFirst { it.id == author.id }
        if (index >= 0) {
            current[index] = author
        } else {
            current.add(0, author)
        }
        _authors.value = current
        recalculateStatsAndActivities()

        // Background Supabase Sync
        supabaseClient.upsertAuthor(author)
        Result.success(author)
    }

    suspend fun deleteAuthor(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val author = _authors.value.find { it.id == id }
        _authors.value = _authors.value.filter { it.id != id }
        recalculateStatsAndActivities()

        supabaseClient.deleteAuthor(id, author?.imgbbDeleteUrl.orEmpty())
        Result.success(true)
    }

    // ==========================================
    // CATEGORIES ACTIONS
    // ==========================================

    suspend fun saveCategory(category: CategoryRecord): Result<CategoryRecord> = withContext(Dispatchers.IO) {
        val current = _categories.value.toMutableList()
        val index = current.indexOfFirst { it.id == category.id }
        if (index >= 0) {
            current[index] = category
        } else {
            current.add(category)
        }
        _categories.value = current
        recalculateStatsAndActivities()

        supabaseClient.upsertCategory(category)
        Result.success(category)
    }

    suspend fun deleteCategory(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        _categories.value = _categories.value.filter { it.id != id }
        recalculateStatsAndActivities()

        supabaseClient.deleteCategory(id)
        Result.success(true)
    }

    // ==========================================
    // BLOGS ACTIONS
    // ==========================================

    suspend fun saveBlog(blog: BlogRecord): Result<BlogRecord> = withContext(Dispatchers.IO) {
        val current = _blogs.value.toMutableList()
        val index = current.indexOfFirst { it.id == blog.id }
        if (index >= 0) {
            current[index] = blog
        } else {
            current.add(0, blog)
        }
        _blogs.value = current
        recalculateStatsAndActivities()

        // Sync directly with Room and Supabase
        syncBlogsToLocalRoom(_blogs.value)
        supabaseClient.upsertBlog(blog)
        Result.success(blog)
    }

    suspend fun deleteBlog(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        _blogs.value = _blogs.value.filter { it.id != id }
        recalculateStatsAndActivities()

        database.articleDao().deleteArticleById(id)
        supabaseClient.deleteBlog(id)
        Result.success(true)
    }

    // ==========================================
    // COMMENTS ACTIONS
    // ==========================================

    suspend fun saveComment(comment: CommentRecord): Result<CommentRecord> = withContext(Dispatchers.IO) {
        val current = _comments.value.toMutableList()
        val index = current.indexOfFirst { it.id == comment.id }
        if (index >= 0) {
            current[index] = comment
        } else {
            current.add(0, comment)
        }
        _comments.value = current
        recalculateStatsAndActivities()

        supabaseClient.upsertComment(comment)
        Result.success(comment)
    }

    suspend fun toggleCommentStatus(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val comment = _comments.value.find { it.id == id } ?: return@withContext Result.failure(Exception("Comment not found"))
        val newStatus = if (comment.status == "Publish") "Unpublish" else "Publish"
        val updated = comment.copy(status = newStatus)

        val current = _comments.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) current[index] = updated
        _comments.value = current
        recalculateStatsAndActivities()

        supabaseClient.updateCommentStatus(id, newStatus)
        Result.success(true)
    }

    suspend fun deleteComment(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        _comments.value = _comments.value.filter { it.id != id }
        recalculateStatsAndActivities()

        supabaseClient.deleteComment(id)
        Result.success(true)
    }

    // ==========================================
    // GALLERIES ACTIONS
    // ==========================================

    suspend fun saveGallery(gallery: GalleryRecord): Result<GalleryRecord> = withContext(Dispatchers.IO) {
        val current = _galleries.value.toMutableList()
        val index = current.indexOfFirst { it.id == gallery.id }
        if (index >= 0) {
            current[index] = gallery
        } else {
            current.add(0, gallery)
        }
        _galleries.value = current
        recalculateStatsAndActivities()

        supabaseClient.upsertGallery(gallery)
        Result.success(gallery)
    }

    suspend fun deleteGallery(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val gallery = _galleries.value.find { it.id == id }
        _galleries.value = _galleries.value.filter { it.id != id }
        recalculateStatsAndActivities()

        supabaseClient.deleteGallery(id, gallery?.imgbbDeleteUrl.orEmpty())
        Result.success(true)
    }

    // ==========================================
    // PDF BOOKS ACTIONS
    // ==========================================

    suspend fun savePdfBook(book: PdfBookRecord): Result<PdfBookRecord> = withContext(Dispatchers.IO) {
        val current = _pdfBooks.value.toMutableList()
        val index = current.indexOfFirst { it.id == book.id }
        if (index >= 0) {
            current[index] = book
        } else {
            current.add(0, book)
        }
        _pdfBooks.value = current
        recalculateStatsAndActivities()

        supabaseClient.upsertPdfBook(book)
        Result.success(book)
    }

    suspend fun deletePdfBook(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val book = _pdfBooks.value.find { it.id == id }
        _pdfBooks.value = _pdfBooks.value.filter { it.id != id }
        recalculateStatsAndActivities()

        supabaseClient.deletePdfBook(id, book?.imgbbDeleteUrl.orEmpty())
        Result.success(true)
    }

    // ==========================================
    // SUBMITTED BLOGS ACTIONS
    // ==========================================

    suspend fun saveSubmittedBlog(sub: SubmittedBlogRecord): Result<SubmittedBlogRecord> = withContext(Dispatchers.IO) {
        val current = _submittedBlogs.value.toMutableList()
        val index = current.indexOfFirst { it.id == sub.id }
        if (index >= 0) {
            current[index] = sub
        } else {
            current.add(0, sub)
        }
        _submittedBlogs.value = current
        recalculateStatsAndActivities()

        supabaseClient.upsertSubmittedBlog(sub)
        Result.success(sub)
    }

    suspend fun updateSubmissionStatus(id: String, status: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val current = _submittedBlogs.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(status = status)
            _submittedBlogs.value = current
            recalculateStatsAndActivities()
        }
        supabaseClient.updateSubmittedBlogStatus(id, status)
        Result.success(true)
    }

    suspend fun approveAndPublishSubmission(sub: SubmittedBlogRecord): Result<BlogRecord> = withContext(Dispatchers.IO) {
        // 1. Mark submission as Published
        updateSubmissionStatus(sub.id, "Published")

        // 2. Create or find Author
        val authorName = sub.writerName.ifBlank { "অতিথি লেখক" }
        var author = _authors.value.find { it.title.equals(authorName, ignoreCase = true) }
        if (author == null) {
            author = AuthorRecord(
                id = UUID.randomUUID().toString(),
                title = authorName,
                image = sub.writerProfileImage,
                designation = sub.writerDesignation.ifBlank { sub.designation },
                description = "নিংশিং চেতে প্রকাশিত অতিথি লেখক",
                isVerified = false
            )
            saveAuthor(author)
        }

        // 3. Convert to published Blog
        val slug = sub.title
            .lowercase()
            .replace(Regex("[^a-zA-Z0-9\\u0980-\\u09FF]+"), "-")
            .trim('-') + "-${System.currentTimeMillis() % 10000}"

        val newBlog = BlogRecord(
            id = UUID.randomUUID().toString(),
            title = sub.title,
            subTitle = sub.designation,
            image = sub.thumbnail,
            content = sub.content,
            categoryId = _categories.value.firstOrNull()?.id ?: "cat-1",
            categoryTitle = _categories.value.firstOrNull()?.title ?: "সাহিত্য",
            categorySlug = _categories.value.firstOrNull()?.slug ?: "literature",
            status = "Publish",
            tags = listOf("পাঠক রচনা", "সাহিত্য"),
            slug = slug,
            authorId = author.id,
            authorName = author.title,
            authorImage = author.image,
            isSlider = false,
            isFeature = true,
            publishedDate = "২০২৬"
        )
        saveBlog(newBlog)
        Result.success(newBlog)
    }

    suspend fun deleteSubmittedBlog(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val sub = _submittedBlogs.value.find { it.id == id }
        _submittedBlogs.value = _submittedBlogs.value.filter { it.id != id }
        recalculateStatsAndActivities()

        supabaseClient.deleteSubmittedBlog(id, sub?.imgbbDeleteUrl.orEmpty())
        Result.success(true)
    }

    // ==========================================
    // VIDEOS ACTIONS
    // ==========================================

    suspend fun saveVideo(video: VideoRecord): Result<VideoRecord> = withContext(Dispatchers.IO) {
        val current = _videos.value.toMutableList()
        val index = current.indexOfFirst { it.id == video.id }
        if (index >= 0) {
            current[index] = video
        } else {
            current.add(0, video)
        }
        _videos.value = current
        recalculateStatsAndActivities()

        supabaseClient.upsertVideo(video)
        Result.success(video)
    }

    suspend fun deleteVideo(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        _videos.value = _videos.value.filter { it.id != id }
        recalculateStatsAndActivities()

        supabaseClient.deleteVideo(id)
        Result.success(true)
    }

    // ==========================================
    // SETTINGS ACTIONS
    // ==========================================

    suspend fun saveSettings(settings: SiteSettingsRecord): Result<SiteSettingsRecord> = withContext(Dispatchers.IO) {
        _settings.value = settings
        supabaseClient.updateSettings(settings)
        Result.success(settings)
    }
}
