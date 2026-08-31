package com.example.data.portal

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * PostgREST surface used by the public reader.
 *
 * Notes on the query parameters
 * -----------------------------
 * - `select` projects columns. The reader never pulls `content` into a list; it
 *   is requested only for the single-article call, which keeps feed responses at
 *   roughly 2 KB per row instead of 20 KB.
 * - Filters use PostgREST's `column=operator.value` syntax. `ilike` and `or`
 *   carry `*`, `(`, `)` and `,` characters, so those parameters are annotated
 *   `encoded = true` and are pre-encoded by [PortalRepository.filters] helpers
 *   where needed.
 * - `limit`/`offset` drive paging; the exact total comes back in the
 *   `Content-Range` response header when `Prefer: count=exact` is sent.
 * - `nullslast` keeps rows without a published date at the end rather than the
 *   top of a descending feed.
 */
interface PortalApi {

    companion object {
        /** Feed/list projection — deliberately excludes `content`. */
        const val BLOG_LIST_COLUMNS =
            "id,title,sub_title,slug,image,category_id,category_title,category_slug," +
                "author_id,author_name,author_image,tags,status,is_slider,is_feature," +
                "is_special_article,views_count,reading_time_minutes,published_date,created_at"

        /** Single-article projection — adds the body and related media. */
        const val BLOG_DETAIL_COLUMNS =
            "$BLOG_LIST_COLUMNS,content,seo_title,seo_description,video_link,pdf_book_link"

        const val CATEGORY_COLUMNS = "id,title,sub_title,slug,icon_name"
        const val AUTHOR_COLUMNS = "id,title,image,designation,description,is_verified,location"
        const val GALLERY_COLUMNS = "id,title,description,image,category,created_at"
        const val PDF_COLUMNS =
            "id,title,image,book_published_date,link,file_provider,author_or_editor," +
                "edition,category,page_count,file_size_mb,description"
        const val VIDEO_COLUMNS = "id,title,video_link,platform,description,thumbnail_url,created_at"
        const val COMMENT_COLUMNS = "id,blog_id,blog_title,name,address,content,status,created_at"
        const val SETTINGS_COLUMNS =
            "id,site_title,site_description,logo_url,contact_email,contact_phone," +
                "facebook_url,youtube_url,instagram_url,hero_slider_enabled," +
                "featured_articles_enabled,special_articles_enabled,allow_comments," +
                "allow_user_submissions"

        const val FEED_ORDER = "published_date.desc.nullslast,created_at.desc"
    }

    // ------------------------------------------------------------------ blogs

    @Headers("Prefer: count=exact")
    @GET("blogs")
    suspend fun blogs(
        @Query("select") select: String = BLOG_LIST_COLUMNS,
        @Query("id") id: String? = null,
        @Query("slug", encoded = true) slug: String? = null,
        @Query("status") status: String? = null,
        @Query("category_id") categoryId: String? = null,
        @Query("author_id") authorId: String? = null,
        @Query("is_slider") isSlider: String? = null,
        @Query("is_feature") isFeature: String? = null,
        @Query("is_special_article") isSpecialArticle: String? = null,
        @Query("title", encoded = true) title: String? = null,
        @Query("or", encoded = true) or: String? = null,
        @Query("order") order: String? = FEED_ORDER,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<List<BlogDto>>

    /** Lookup by UUID **or** slug — used by deep links and the reader. */
    @GET("blogs")
    suspend fun blogByIdOrSlug(
        @Query("select") select: String = BLOG_DETAIL_COLUMNS,
        @Query("or", encoded = true) or: String,
        @Query("limit") limit: Int = 1
    ): Response<List<BlogDto>>

    // ------------------------------------------------------------- categories

    @GET("categories")
    suspend fun categories(
        @Query("select") select: String = CATEGORY_COLUMNS,
        @Query("order") order: String = "title.asc",
        @Query("limit") limit: Int? = null
    ): Response<List<CategoryDto>>

    @GET("categories")
    suspend fun categoryBySlug(
        @Query("select") select: String = CATEGORY_COLUMNS,
        @Query("slug", encoded = true) slug: String,
        @Query("limit") limit: Int = 1
    ): Response<List<CategoryDto>>

    // ---------------------------------------------------------------- authors

    @GET("authors")
    suspend fun authors(
        @Query("select") select: String = AUTHOR_COLUMNS,
        @Query("order") order: String = "title.asc",
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<List<AuthorDto>>

    @GET("authors")
    suspend fun authorById(
        @Query("select") select: String = AUTHOR_COLUMNS,
        @Query("id") id: String,
        @Query("limit") limit: Int = 1
    ): Response<List<AuthorDto>>

    // -------------------------------------------------------------- galleries

    @Headers("Prefer: count=exact")
    @GET("galleries")
    suspend fun galleries(
        @Query("select") select: String = GALLERY_COLUMNS,
        @Query("category", encoded = true) category: String? = null,
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<List<GalleryDto>>

    // -------------------------------------------------------------- pdf books

    @GET("pdf_books")
    suspend fun pdfBooks(
        @Query("select") select: String = PDF_COLUMNS,
        @Query("order") order: String = "book_published_date.desc.nullslast,created_at.desc",
        @Query("limit") limit: Int? = null
    ): Response<List<PdfBookDto>>

    // ----------------------------------------------------------------- videos

    @GET("videos")
    suspend fun videos(
        @Query("select") select: String = VIDEO_COLUMNS,
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int? = null
    ): Response<List<VideoDto>>

    // --------------------------------------------------------------- comments

    @GET("comments")
    suspend fun comments(
        @Query("select") select: String = COMMENT_COLUMNS,
        @Query("blog_id") blogId: String? = null,
        @Query("status") status: String? = null,
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int? = null
    ): Response<List<CommentDto>>

    /**
     * Anonymous comment submission. Allowed by the `comments_public_insert`
     * RLS policy; the row lands as `Unpublish` until a moderator approves it.
     */
    @Headers("Prefer: return=representation")
    @POST("comments")
    suspend fun postComment(
        @Body comment: NewCommentDto
    ): Response<List<CommentDto>>

    // --------------------------------------------------------------- settings

    @GET("settings")
    suspend fun settings(
        @Query("select") select: String = SETTINGS_COLUMNS,
        @Query("id") id: String = "eq.site_settings",
        @Query("limit") limit: Int = 1
    ): Response<List<SettingsDto>>
}
