package com.example.data.portal

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Wire models for the Supabase PostgREST API.
 *
 * Field names mirror the columns in `backend/supabase/schema.sql` exactly, so
 * Moshi needs no custom naming strategy. Unknown columns (for example
 * `image_meta`, `inline_media`, `pdf_storage_path`) are ignored on read and are
 * never sent by the reader, which only performs GET requests.
 *
 * Every nullable field is nullable on purpose: PostgREST omits `null` columns
 * from JSON, and Bengali content frequently leaves optional columns empty.
 */

@JsonClass(generateAdapter = true)
data class BlogDto(
    val id: String,
    val title: String,
    @Json(name = "sub_title") val subTitle: String? = null,
    val slug: String? = null,
    val image: String? = null,
    val content: String? = null,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "category_title") val categoryTitle: String? = null,
    @Json(name = "category_slug") val categorySlug: String? = null,
    @Json(name = "author_id") val authorId: String? = null,
    @Json(name = "author_name") val authorName: String? = null,
    @Json(name = "author_image") val authorImage: String? = null,
    val status: String? = null,
    val tags: List<String>? = null,
    @Json(name = "seo_title") val seoTitle: String? = null,
    @Json(name = "seo_description") val seoDescription: String? = null,
    @Json(name = "video_link") val videoLink: String? = null,
    @Json(name = "pdf_book_link") val pdfBookLink: String? = null,
    @Json(name = "is_slider") val isSlider: Boolean? = null,
    @Json(name = "is_feature") val isFeature: Boolean? = null,
    @Json(name = "is_special_article") val isSpecialArticle: Boolean? = null,
    @Json(name = "views_count") val viewsCount: Long? = null,
    @Json(name = "reading_time_minutes") val readingTimeMinutes: Int? = null,
    @Json(name = "published_date") val publishedDate: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CategoryDto(
    val id: String,
    val title: String,
    @Json(name = "sub_title") val subTitle: String? = null,
    val slug: String? = null,
    @Json(name = "icon_name") val iconName: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthorDto(
    val id: String,
    val title: String,
    val image: String? = null,
    val designation: String? = null,
    val description: String? = null,
    @Json(name = "is_verified") val isVerified: Boolean? = null,
    val location: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class GalleryDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val image: String? = null,
    val category: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class PdfBookDto(
    val id: String,
    val title: String,
    val image: String? = null,
    @Json(name = "book_published_date") val bookPublishedDate: String? = null,
    val link: String? = null,
    @Json(name = "file_provider") val fileProvider: String? = null,
    @Json(name = "author_or_editor") val authorOrEditor: String? = null,
    val edition: String? = null,
    val category: String? = null,
    @Json(name = "page_count") val pageCount: Int? = null,
    @Json(name = "file_size_mb") val fileSizeMb: Double? = null,
    val description: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class VideoDto(
    val id: String,
    val title: String,
    @Json(name = "video_link") val videoLink: String,
    val platform: String? = null,
    val description: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CommentDto(
    val id: String,
    @Json(name = "blog_id") val blogId: String,
    @Json(name = "blog_title") val blogTitle: String? = null,
    val name: String,
    val address: String? = null,
    val content: String,
    val status: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SettingsDto(
    val id: String,
    @Json(name = "site_title") val siteTitle: String? = null,
    @Json(name = "site_description") val siteDescription: String? = null,
    @Json(name = "logo_url") val logoUrl: String? = null,
    @Json(name = "contact_email") val contactEmail: String? = null,
    @Json(name = "contact_phone") val contactPhone: String? = null,
    @Json(name = "facebook_url") val facebookUrl: String? = null,
    @Json(name = "youtube_url") val youtubeUrl: String? = null,
    @Json(name = "instagram_url") val instagramUrl: String? = null,
    @Json(name = "hero_slider_enabled") val heroSliderEnabled: Boolean? = null,
    @Json(name = "featured_articles_enabled") val featuredArticlesEnabled: Boolean? = null,
    @Json(name = "special_articles_enabled") val specialArticlesEnabled: Boolean? = null,
    @Json(name = "allow_comments") val allowComments: Boolean? = null,
    @Json(name = "allow_user_submissions") val allowUserSubmissions: Boolean? = null
)

/**
 * Payload for the public comment insert. RLS grants `INSERT` to anonymous users
 * for `comments` only, and the default status is `Unpublish`, so anything posted
 * here waits for moderation in the dashboard.
 */
@JsonClass(generateAdapter = true)
data class NewCommentDto(
    @Json(name = "blog_id") val blogId: String,
    @Json(name = "blog_title") val blogTitle: String,
    val name: String,
    val address: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val content: String,
    val status: String = "Unpublish"
)
