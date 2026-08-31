package com.example.data.model

data class Article(
    val id: String,
    val title: String,
    val slug: String,
    val excerpt: String,
    val content: String,
    val featuredImageUrl: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String = "",
    val category: String,
    val categorySlug: String,
    val tags: List<String> = emptyList(),
    val publishedDate: String,
    val year: Int,
    val readingTimeMinutes: Int = 5,
    val isFeatured: Boolean = false,
    val isEditorialPick: Boolean = false,
    val viewCount: Int = 120,
    val sourceUrl: String = "https://ningshingche.com",
    val relatedArticleIds: List<String> = emptyList()
)

data class Category(
    val id: String,
    val name: String,
    val slug: String,
    val description: String,
    val articleCount: Int,
    val iconName: String = "article",
    val imageUrl: String = ""
)

data class Author(
    val id: String,
    val name: String,
    val designation: String,
    val bio: String,
    val avatarUrl: String,
    val articleCount: Int,
    val location: String = "বাংলাদেশ / ভারত",
    val topics: List<String> = emptyList(),
    val isVerified: Boolean = false
)

data class YearArchive(
    val year: Int,
    val bengaliYearText: String,
    val title: String,
    val description: String,
    val issueCount: Int,
    val articleCount: Int,
    val coverImageUrl: String = ""
)

data class PdfCategory(
    val id: String,
    val name: String,
    val description: String,
    val count: Int
)

data class PdfDocument(
    val id: String,
    val title: String,
    val edition: String,
    val category: String,
    val categorySlug: String,
    val year: Int,
    val authorOrEditor: String,
    val pageCount: Int,
    val fileSizeMb: Float,
    val pdfUrl: String,
    val coverImageUrl: String,
    val description: String,
    val tags: List<String> = emptyList(),
    val downloadUrl: String = pdfUrl
)

data class Bookmark(
    val articleId: String,
    val savedAtTimestamp: Long = System.currentTimeMillis(),
    val folder: String = "সব সংরক্ষিত",
    val note: String = ""
)

data class ReadingHistory(
    val articleId: String,
    val readAtTimestamp: Long = System.currentTimeMillis(),
    val scrollPosition: Int = 0,
    val progressPercent: Float = 0f
)

enum class ReaderThemeMode {
    PAPER, SEPIA, NIGHT, CRISP
}

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

data class ReaderPreferences(
    val fontSizeSp: Float = 18f,
    val lineSpacingMultiplier: Float = 1.6f,
    val themeMode: ReaderThemeMode = ReaderThemeMode.PAPER,
    val appThemeMode: AppThemeMode = AppThemeMode.LIGHT,
    val ttsSpeed: Float = 1.0f,
    val notificationNewArticles: Boolean = true,
    val notificationFeatured: Boolean = true
)

data class ArticleCitation(
    val articleId: String,
    val title: String,
    val author: String,
    val category: String,
    val snippet: String
)

data class ArticleComment(
    val name: String,
    val content: String,
    val meta: String = ""
)

data class AiChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val citations: List<ArticleCitation> = emptyList(),
    val isThinking: Boolean = false
)
