package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY publishedDate DESC")
    fun getAllArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    suspend fun getArticleById(id: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE id = :idOrSlug OR slug = :idOrSlug LIMIT 1")
    suspend fun getArticleByIdOrSlug(idOrSlug: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE isFeatured = 1 ORDER BY publishedDate DESC")
    fun getFeaturedArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE categorySlug = :categorySlug ORDER BY publishedDate DESC")
    fun getArticlesByCategory(categorySlug: String): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE authorId = :authorId ORDER BY publishedDate DESC")
    fun getArticlesByAuthor(authorId: String): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE year = :year ORDER BY publishedDate DESC")
    fun getArticlesByYear(year: Int): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR authorName LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun searchArticles(query: String): Flow<List<ArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleEntity)

    @Query("DELETE FROM articles WHERE id = :id")
    suspend fun deleteArticleById(id: String)

    @Query("DELETE FROM articles")
    suspend fun clearAll()

    @Query("DELETE FROM articles WHERE id LIKE 'art-%'")
    suspend fun deleteSeedArticles()

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun countArticles(): Int
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY savedAtTimestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE articleId = :articleId)")
    fun isBookmarked(articleId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE articleId = :articleId)")
    suspend fun isBookmarkedDirect(articleId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE articleId = :articleId")
    suspend fun deleteBookmark(articleId: String)

    @Query("DELETE FROM bookmarks")
    suspend fun clearAll()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM reading_history ORDER BY readAtTimestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM reading_history WHERE articleId = :articleId LIMIT 1")
    suspend fun getHistory(articleId: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Query("DELETE FROM reading_history WHERE articleId = :articleId")
    suspend fun deleteHistory(articleId: String)

    @Query("DELETE FROM reading_history")
    suspend fun clearAll()
}

@Dao
interface SearchDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 15")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearch(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}
