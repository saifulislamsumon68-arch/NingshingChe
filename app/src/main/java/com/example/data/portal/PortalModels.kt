package com.example.data.portal

import java.io.IOException

/**
 * UI-facing models for the reader. They are intentionally flat, immutable and
 * free of JSON annotations so a screen never depends on a wire-format detail.
 *
 * Everything here is derived from [BlogDto] & friends by the mappers at the
 * bottom of this file, which is also where all of the "real data is messy"
 * decisions live (blank slugs, missing dates, HTML excerpts, ...).
 */

/** Result of a paged PostgREST query. */
data class Page<out T>(
    val items: List<T>,
    /** Exact total reported by `Content-Range`, or `null` when not requested. */
    val total: Int? = null,
    val offset: Int = 0,
    val limit: Int = PortalConfig.PAGE_SIZE
) {
    val hasMore: Boolean
        get() = total?.let { offset + items.size < it } ?: (items.size == limit)

    val nextOffset: Int get() = offset + items.size
}

data class ArticleSummary(
    val id: String,
    val title: String,
    val subTitle: String,
    val slug: String,
    val imageUrl: String,
    val categoryId: String?,
    val categoryTitle: String,
    val categorySlug: String,
    val authorId: String?,
    val authorName: String,
    val authorImageUrl: String,
    val tags: List<String>,
    val readingTimeMinutes: Int,
    val viewsCount: Long,
    /** ISO `yyyy-MM-dd`, or `""` when the row has no date. */
    val publishedDate: String,
    val year: Int,
    val isSlider: Boolean,
    val isFeature: Boolean,
    val isSpecial: Boolean
)

data class ArticleDetail(
    val summary: ArticleSummary,
    /** Sanitised-for-display HTML body, exactly as stored by the editor. */
    val html: String,
    val seoTitle: String,
    val seoDescription: String,
    val videoLink: String,
    val pdfLink: String
) {
    val id: String get() = summary.id
    val title: String get() = summary.title
}

data class CategoryRef(
    val id: String,
    val title: String,
    val subTitle: String,
    val slug: String,
    /** Font Awesome icon name from the dashboard, e.g. `"book-open"`. */
    val iconName: String
)

data class AuthorRef(
    val id: String,
    val name: String,
    val designation: String,
    val bio: String,
    val imageUrl: String,
    val location: String,
    val isVerified: Boolean
)

data class GalleryItem(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val category: String
)

data class PdfBook(
    val id: String,
    val title: String,
    val coverUrl: String,
    val authorOrEditor: String,
    val edition: String,
    val category: String,
    val pageCount: Int,
    val fileSizeMb: Double,
    val publishedDate: String,
    val year: Int,
    val fileUrl: String,
    val description: String,
    val isHostedInStorage: Boolean
)

data class VideoItem(
    val id: String,
    val title: String,
    val url: String,
    val platform: String,
    val description: String,
    val thumbnailUrl: String
)

data class CommentItem(
    val id: String,
    val name: String,
    val content: String,
    val createdAt: String,
    val address: String
)

data class SiteSettings(
    val title: String,
    val description: String,
    val logoUrl: String,
    val contactEmail: String,
    val facebookUrl: String,
    val youtubeUrl: String,
    val instagramUrl: String,
    val heroSliderEnabled: Boolean,
    val featuredEnabled: Boolean,
    val specialEnabled: Boolean,
    val allowComments: Boolean,
    val allowSubmissions: Boolean
) {
    companion object {
        val DEFAULT = SiteSettings(
            title = "নিংশিং চে",
            description = "বিষ্ণুপ্রিয়া মণিপুরি ডিজিটাল সাংস্কৃতিক আর্কাইভ ও সাহিত্য পত্রিকা",
            logoUrl = "",
            contactEmail = "",
            facebookUrl = "",
            youtubeUrl = "",
            instagramUrl = "",
            heroSliderEnabled = true,
            featuredEnabled = true,
            specialEnabled = true,
            allowComments = true,
            allowSubmissions = true
        )
    }
}

/** Everything the home feed needs, fetched in one parallel batch. */
data class HomeFeed(
    val hero: List<ArticleSummary>,
    val featured: List<ArticleSummary>,
    val special: List<ArticleSummary>,
    val latest: List<ArticleSummary>,
    val categories: List<CategoryRef>,
    val authors: List<AuthorRef>,
    val gallery: List<GalleryItem>,
    val pdfBooks: List<PdfBook>,
    val videos: List<VideoItem>,
    val settings: SiteSettings
)

// ---------------------------------------------------------------------------
// Errors
// ---------------------------------------------------------------------------

sealed class PortalError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** No connectivity, DNS failure, TLS failure or timeout. */
    class Offline(cause: Throwable? = null) :
        PortalError("ইন্টারনেট সংযোগ নেই। সংরক্ষিত আর্কাইভ দেখানো হচ্ছে।", cause)

    /** HTTP failure. `code` is the PostgREST/Supabase status. */
    class Http(val code: Int, override val message: String) : PortalError(message)

    /** A table or column is missing — `schema.sql` or migration 003 has not run. */
    class SchemaMissing(override val message: String) : PortalError(message)

    /** Successful request, empty result. */
    object NotFound : PortalError("কোনো তথ্য পাওয়া যায়নি।")

    class Unknown(cause: Throwable? = null) :
        PortalError("তথ্য লোড করতে সমস্যা হয়েছে। অনুগ্রহ করে আবার চেষ্টা করুন।", cause)
}

internal fun Throwable.toPortalError(): Throwable = when (this) {
    is PortalError -> this
    is IOException -> PortalError.Offline(this)
    else -> PortalError.Unknown(this)
}

// ---------------------------------------------------------------------------
// Mappers
// ---------------------------------------------------------------------------

internal fun BlogDto.toSummary(): ArticleSummary {
    val date = publishedDate.orEmpty()
    return ArticleSummary(
        id = id,
        title = title.trim(),
        subTitle = subTitle.orEmpty().trim(),
        // Slugs in this database are Bengali; fall back to the UUID so deep
        // links and share URLs always resolve to something.
        slug = slug.orEmpty().ifBlank { id },
        imageUrl = image.orEmpty(),
        categoryId = categoryId,
        categoryTitle = categoryTitle.orEmpty().ifBlank { "সাধারণ" },
        categorySlug = categorySlug.orEmpty(),
        authorId = authorId,
        authorName = authorName.orEmpty().ifBlank { "নিংশিং চে" },
        authorImageUrl = authorImage.orEmpty(),
        tags = tags?.filter { it.isNotBlank() }.orEmpty(),
        readingTimeMinutes = (readingTimeMinutes ?: 0).coerceAtLeast(1),
        viewsCount = viewsCount ?: 0L,
        publishedDate = date,
        year = yearOf(date, createdAt),
        isSlider = isSlider ?: false,
        isFeature = isFeature ?: false,
        isSpecial = isSpecialArticle ?: false
    )
}

internal fun BlogDto.toDetail(): ArticleDetail = ArticleDetail(
    summary = toSummary(),
    html = content.orEmpty(),
    seoTitle = seoTitle.orEmpty(),
    seoDescription = seoDescription.orEmpty(),
    videoLink = videoLink.orEmpty(),
    pdfLink = pdfBookLink.orEmpty()
)

internal fun CategoryDto.toRef(): CategoryRef = CategoryRef(
    id = id,
    title = title.trim(),
    subTitle = subTitle.orEmpty().trim(),
    slug = slug.orEmpty().ifBlank { id },
    iconName = iconName.orEmpty().ifBlank { "layer-group" }
)

internal fun AuthorDto.toRef(): AuthorRef = AuthorRef(
    id = id,
    name = title.trim(),
    designation = designation.orEmpty().trim(),
    bio = description.orEmpty(),
    imageUrl = image.orEmpty(),
    location = location.orEmpty(),
    isVerified = isVerified ?: false
)

internal fun GalleryDto.toItem(): GalleryItem = GalleryItem(
    id = id,
    title = title.trim(),
    description = description.orEmpty(),
    imageUrl = image.orEmpty(),
    category = category.orEmpty().ifBlank { "সাধারণ" }
)

internal fun PdfBookDto.toModel(): PdfBook {
    val date = bookPublishedDate.orEmpty()
    return PdfBook(
        id = id,
        title = title.trim(),
        coverUrl = image.orEmpty(),
        authorOrEditor = authorOrEditor.orEmpty(),
        edition = edition.orEmpty(),
        category = category.orEmpty(),
        pageCount = pageCount ?: 0,
        fileSizeMb = fileSizeMb ?: 0.0,
        publishedDate = date,
        year = yearOf(date, createdAt),
        fileUrl = link.orEmpty(),
        description = description.orEmpty(),
        isHostedInStorage = fileProvider == "supabase-storage"
    )
}

internal fun VideoDto.toItem(): VideoItem = VideoItem(
    id = id,
    title = title.trim(),
    url = videoLink,
    platform = platform.orEmpty().ifBlank { platformOf(videoLink) },
    description = description.orEmpty(),
    thumbnailUrl = thumbnailUrl.orEmpty().ifBlank { thumbnailOf(videoLink) }
)

internal fun CommentDto.toItem(): CommentItem = CommentItem(
    id = id,
    name = name.trim(),
    content = content.trim(),
    createdAt = createdAt.orEmpty(),
    address = address.orEmpty()
)

internal fun SettingsDto.toModel(): SiteSettings = SiteSettings(
    title = siteTitle.orEmpty().ifBlank { SiteSettings.DEFAULT.title },
    description = siteDescription.orEmpty().ifBlank { SiteSettings.DEFAULT.description },
    logoUrl = logoUrl.orEmpty(),
    contactEmail = contactEmail.orEmpty(),
    facebookUrl = facebookUrl.orEmpty(),
    youtubeUrl = youtubeUrl.orEmpty(),
    instagramUrl = instagramUrl.orEmpty(),
    heroSliderEnabled = heroSliderEnabled ?: true,
    featuredEnabled = featuredArticlesEnabled ?: true,
    specialEnabled = specialArticlesEnabled ?: true,
    allowComments = allowComments ?: true,
    allowSubmissions = allowUserSubmissions ?: true
)

// ---------------------------------------------------------------------------
// Small helpers shared by the mappers and the UI
// ---------------------------------------------------------------------------

/** `2025-06-17` → `2025`. Falls back to `created_at`, then to the current year. */
internal fun yearOf(publishedDate: String, createdAt: String?): Int {
    val fromPublished = publishedDate.filter { it.isDigit() }.take(4).toIntOrNull()
    if (fromPublished != null) return fromPublished
    val fromCreated = createdAt?.filter { it.isDigit() }?.take(4)?.toIntOrNull()
    if (fromCreated != null) return fromCreated
    return 2025
}

/** Plain-text teaser for the HTML body stored by the dashboard editor. */
fun excerptOf(html: String, maxChars: Int = 160): String {
    val text = stripHtml(html)
    return if (text.length <= maxChars) text else text.take(maxChars).trimEnd() + "…"
}

fun stripHtml(html: String): String = html
    .replace(Regex("(?is)<(script|style).*?</\\1>"), " ")
    .replace(Regex("(?is)<br\\s*/?>"), "\n")
    .replace(Regex("(?is)</p\\s*>"), "\n\n")
    .replace(Regex("(?s)<[^>]+>"), " ")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace(Regex("[ \\t]+"), " ")
    .replace(Regex("\n{3,}"), "\n\n")
    .trim()

/** Best-effort platform label for a video URL. */
fun platformOf(url: String): String = when {
    url.contains("youtube.com", true) || url.contains("youtu.be", true) -> "YouTube"
    url.contains("facebook.com", true) || url.contains("fb.watch", true) -> "Facebook"
    url.contains("instagram.com", true) -> "Instagram"
    url.contains("vimeo.com", true) -> "Vimeo"
    url.contains("dailymotion.com", true) -> "Dailymotion"
    else -> "Video Link"
}

/** YouTube thumbnails are derivable; Facebook reels are not. */
fun thumbnailOf(url: String): String {
    val id = Regex("(?:v=|youtu\\.be/|shorts/|embed/)([A-Za-z0-9_-]{11})").find(url)
        ?.groupValues?.getOrNull(1)
    return if (id != null) "https://i.ytimg.com/vi/$id/hqdefault.jpg" else ""
}

/** Public permalink for a blog. Bengali slugs must be percent-encoded. */
fun permalinkOf(slug: String): String {
    val encoded = java.net.URLEncoder.encode(slug, "UTF-8").replace("+", "%20")
    return "https://ningshingche.com/article/$encoded"
}
