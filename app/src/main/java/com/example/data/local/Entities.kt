package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val slug: String,
    val excerpt: String,
    val content: String,
    val featuredImageUrl: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val category: String,
    val categorySlug: String,
    val tagsRaw: String, // comma-separated
    val publishedDate: String,
    val year: Int,
    val readingTimeMinutes: Int,
    val isFeatured: Boolean,
    val isEditorialPick: Boolean,
    val viewCount: Int,
    val sourceUrl: String,
    val relatedArticleIdsRaw: String, // comma-separated
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val articleId: String,
    val savedAtTimestamp: Long = System.currentTimeMillis(),
    val folder: String = "সব সংরক্ষিত",
    val note: String = ""
)

@Entity(tableName = "reading_history")
data class HistoryEntity(
    @PrimaryKey val articleId: String,
    val readAtTimestamp: Long = System.currentTimeMillis(),
    val scrollPosition: Int = 0,
    val progressPercent: Float = 0f
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
