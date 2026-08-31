package com.example.data.remote

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class UserRole(val displayName: String, val bengaliName: String, val level: Int, val description: String) {
    ADMINISTRATOR("Administrator", "প্রধান প্রশাসক", 4, "সকল ক্ষমতা, সেটিংস ও মুছে ফেলার পূর্ণ নিয়ন্ত্রণ"),
    EDITOR("Editor", "সম্পাদক", 3, "সকল কন্টেন্ট তৈরি ও সম্পাদনার অনুমতি"),
    MODERATOR("Moderator", "মডারেটর", 2, "মন্তব্য ও পাঠক রচনা পর্যালোচনা"),
    AUTHOR("Author", "লেখক", 1, "নিজস্ব প্রবন্ধ রচনা ও খসড়া সংরক্ষণ");

    companion object {
        fun fromString(value: String?): UserRole {
            return entries.find {
                it.name.equals(value, ignoreCase = true) ||
                it.displayName.equals(value, ignoreCase = true) ||
                it.bengaliName.equals(value, ignoreCase = true)
            } ?: ADMINISTRATOR
        }
    }
}

data class UserProfile(
    val id: String,
    val email: String,
    val fullName: String,
    val role: UserRole = UserRole.ADMINISTRATOR,
    val avatarUrl: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("email", email)
            put("full_name", fullName)
            put("role", role.name)
            put("avatar_url", avatarUrl)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): UserProfile {
            return UserProfile(
                id = json.optString("id", UUID.randomUUID().toString()),
                email = json.optString("email", ""),
                fullName = json.optString("full_name", json.optString("name", "Administrator")),
                role = UserRole.fromString(json.optString("role", "ADMINISTRATOR")),
                avatarUrl = json.optString("avatar_url", ""),
                createdAt = json.optString("created_at", ""),
                updatedAt = json.optString("updated_at", "")
            )
        }
    }
}

data class AuthorRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val image: String = "",
    val imgbbDeleteUrl: String = "",
    val designation: String = "",
    val description: String = "",
    val isVerified: Boolean = true,
    val location: String = "বাংলাদেশ / ভারত",
    val articleCount: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("image", image)
            put("imgbb_delete_url", imgbbDeleteUrl)
            put("designation", designation)
            put("description", description)
            put("is_verified", isVerified)
            put("location", location)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): AuthorRecord {
            return AuthorRecord(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", json.optString("name", "")),
                image = json.optString("image", json.optString("avatar_url", "")),
                imgbbDeleteUrl = json.optString("imgbb_delete_url", ""),
                designation = json.optString("designation", ""),
                description = json.optString("description", json.optString("bio", "")),
                isVerified = json.optBoolean("is_verified", true),
                location = json.optString("location", "বাংলাদেশ / ভারত"),
                articleCount = json.optInt("article_count", 0),
                createdAt = json.optString("created_at", ""),
                updatedAt = json.optString("updated_at", "")
            )
        }
    }
}

data class CategoryRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subTitle: String = "",
    val slug: String,
    val iconName: String = "article",
    val blogCount: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("sub_title", subTitle)
            put("slug", slug)
            put("icon_name", iconName)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): CategoryRecord {
            return CategoryRecord(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", json.optString("name", "")),
                subTitle = json.optString("sub_title", json.optString("description", "")),
                slug = json.optString("slug", ""),
                iconName = json.optString("icon_name", "article"),
                blogCount = json.optInt("blog_count", 0),
                createdAt = json.optString("created_at", ""),
                updatedAt = json.optString("updated_at", "")
            )
        }
    }
}

data class BlogRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subTitle: String = "",
    val image: String = "",
    val content: String,
    val categoryId: String = "",
    val categoryTitle: String = "",
    val categorySlug: String = "",
    val status: String = "Publish", // "Draft" or "Publish"
    val tags: List<String> = emptyList(),
    val seoTitle: String = "",
    val videoLink: String = "",
    val pdfBookLink: String = "",
    val slug: String,
    val authorId: String = "",
    val authorName: String = "",
    val authorImage: String = "",
    val isSlider: Boolean = false, // Hero Carousel
    val isFeature: Boolean = false, // Featured Articles
    val isSpecialArticle: Boolean = false, // Special Articles
    val viewsCount: Int = 0,
    val readingTimeMinutes: Int = 5,
    val publishedDate: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    val isPublished: Boolean get() = status.equals("Publish", ignoreCase = true)

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("sub_title", subTitle)
            put("image", image)
            put("content", content)
            put("category_id", categoryId)
            put("category_title", categoryTitle)
            put("category_slug", categorySlug)
            put("status", status)
            put("tags", JSONArray(tags))
            put("seo_title", seoTitle)
            put("video_link", videoLink)
            put("pdf_book_link", pdfBookLink)
            put("slug", slug)
            put("author_id", authorId)
            put("author_name", authorName)
            put("author_image", authorImage)
            put("is_slider", isSlider)
            put("is_feature", isFeature)
            put("is_special_article", isSpecialArticle)
            put("views_count", viewsCount)
            put("reading_time_minutes", readingTimeMinutes)
            put("published_date", publishedDate)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): BlogRecord {
            val tagsList = mutableListOf<String>()
            val tagsArr = json.optJSONArray("tags")
            if (tagsArr != null) {
                for (i in 0 until tagsArr.length()) {
                    tagsList.add(tagsArr.optString(i))
                }
            } else {
                val tagsStr = json.optString("tags", "")
                if (tagsStr.isNotBlank()) {
                    tagsList.addAll(tagsStr.split(",").map { it.trim() })
                }
            }

            return BlogRecord(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", ""),
                subTitle = json.optString("sub_title", ""),
                image = json.optString("image", json.optString("featured_image_url", "")),
                content = json.optString("content", ""),
                categoryId = json.optString("category_id", ""),
                categoryTitle = json.optString("category_title", json.optString("category", "")),
                categorySlug = json.optString("category_slug", ""),
                status = json.optString("status", "Publish"),
                tags = tagsList,
                seoTitle = json.optString("seo_title", ""),
                videoLink = json.optString("video_link", ""),
                pdfBookLink = json.optString("pdf_book_link", ""),
                slug = json.optString("slug", ""),
                authorId = json.optString("author_id", ""),
                authorName = json.optString("author_name", ""),
                authorImage = json.optString("author_image", json.optString("author_avatar_url", "")),
                isSlider = json.optBoolean("is_slider", false),
                isFeature = json.optBoolean("is_feature", json.optBoolean("is_featured", false)),
                isSpecialArticle = json.optBoolean("is_special_article", false),
                viewsCount = json.optInt("views_count", json.optInt("view_count", 0)),
                readingTimeMinutes = json.optInt("reading_time_minutes", 5),
                publishedDate = json.optString("published_date", ""),
                createdAt = json.optString("created_at", ""),
                updatedAt = json.optString("updated_at", "")
            )
        }
    }
}

data class CommentRecord(
    val id: String = UUID.randomUUID().toString(),
    val blogId: String,
    val blogTitle: String = "",
    val name: String,
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val content: String,
    val status: String = "Publish", // "Publish" or "Unpublish"
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    val isPublished: Boolean get() = status.equals("Publish", ignoreCase = true)

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("blog_id", blogId)
            put("blog_title", blogTitle)
            put("name", name)
            put("address", address)
            put("phone", phone)
            put("email", email)
            put("content", content)
            put("status", status)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): CommentRecord {
            return CommentRecord(
                id = json.optString("id", UUID.randomUUID().toString()),
                blogId = json.optString("blog_id", ""),
                blogTitle = json.optString("blog_title", ""),
                name = json.optString("name", ""),
                address = json.optString("address", ""),
                phone = json.optString("phone", ""),
                email = json.optString("email", ""),
                content = json.optString("content", ""),
                status = json.optString("status", "Publish"),
                createdAt = json.optString("created_at", ""),
                updatedAt = json.optString("updated_at", "")
            )
        }
    }
}

data class GalleryRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val image: String,
    val imgbbDeleteUrl: String = "",
    val category: String = "সংস্কৃতি ও উৎসব",
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("description", description)
            put("image", image)
            put("imgbb_delete_url", imgbbDeleteUrl)
            put("category", category)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): GalleryRecord {
            return GalleryRecord(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", ""),
                description = json.optString("description", ""),
                image = json.optString("image", ""),
                imgbbDeleteUrl = json.optString("imgbb_delete_url", ""),
                category = json.optString("category", "সংস্কৃতি ও উৎসব"),
                createdAt = json.optString("created_at", ""),
                updatedAt = json.optString("updated_at", "")
            )
        }
    }
}

data class PdfBookRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val image: String = "", // Recommended ratio 2:1
    val imgbbDeleteUrl: String = "",
    val bookPublishedDate: String = "",
    val link: String = "", // Download or view URL
    val authorOrEditor: String = "",
    val edition: String = "",
    val category: String = "বার্ষিক সাহিত্য সংকলন",
    val pageCount: Int = 0,
    val fileSizeMb: Float = 0f,
    val description: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("image", image)
            put("imgbb_delete_url", imgbbDeleteUrl)
            put("book_published_date", bookPublishedDate)
            put("link", link)
            put("author_or_editor", authorOrEditor)
            put("edition", edition)
            put("category", category)
            put("page_count", pageCount)
            put("file_size_mb", fileSizeMb.toDouble())
            put("description", description)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): PdfBookRecord {
            return PdfBookRecord(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", ""),
                image = json.optString("image", json.optString("cover_image_url", "")),
                imgbbDeleteUrl = json.optString("imgbb_delete_url", ""),
                bookPublishedDate = json.optString("book_published_date", json.optString("year", "")),
                link = json.optString("link", json.optString("pdf_url", "")),
                authorOrEditor = json.optString("author_or_editor", ""),
                edition = json.optString("edition", ""),
                category = json.optString("category", "বার্ষিক সাহিত্য সংকলন"),
                pageCount = json.optInt("page_count", 0),
                fileSizeMb = json.optDouble("file_size_mb", 0.0).toFloat(),
                description = json.optString("description", ""),
                createdAt = json.optString("created_at", ""),
                updatedAt = json.optString("updated_at", "")
            )
        }
    }
}

data class SubmittedBlogRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val designation: String = "",
    val address: String = "",
    val phone: String = "",
    val thumbnail: String = "",
    val imgbbDeleteUrl: String = "",
    val writerName: String,
    val writerDesignation: String = "",
    val writerProfileImage: String = "",
    val writerEmail: String = "",
    val writerFacebook: String = "",
    val contentTitle: String = "",
    val content: String,
    val status: String = "Pending", // "Pending", "Published", "Rejected"
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("designation", designation)
            put("address", address)
            put("phone", phone)
            put("thumbnail", thumbnail)
            put("imgbb_delete_url", imgbbDeleteUrl)
            put("writer_name", writerName)
            put("writer_designation", writerDesignation)
            put("writer_profile_image", writerProfileImage)
            put("writer_email", writerEmail)
            put("writer_facebook", writerFacebook)
            put("content_title", contentTitle)
            put("content", content)
            put("status", status)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): SubmittedBlogRecord {
            return SubmittedBlogRecord(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", ""),
                designation = json.optString("designation", ""),
                address = json.optString("address", ""),
                phone = json.optString("phone", ""),
                thumbnail = json.optString("thumbnail", ""),
                imgbbDeleteUrl = json.optString("imgbb_delete_url", ""),
                writerName = json.optString("writer_name", ""),
                writerDesignation = json.optString("writer_designation", ""),
                writerProfileImage = json.optString("writer_profile_image", ""),
                writerEmail = json.optString("writer_email", ""),
                writerFacebook = json.optString("writer_facebook", ""),
                contentTitle = json.optString("content_title", ""),
                content = json.optString("content", ""),
                status = json.optString("status", "Pending"),
                createdAt = json.optString("created_at", ""),
                updatedAt = json.optString("updated_at", "")
            )
        }
    }
}

data class VideoRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val videoLink: String,
    val platform: String = "YouTube",
    val description: String = "",
    val thumbnailUrl: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("video_link", videoLink)
            put("platform", platform)
            put("description", description)
            put("thumbnail_url", thumbnailUrl)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): VideoRecord {
            val link = json.optString("video_link", "")
            val inferredPlatform = when {
                link.contains("youtube.com") || link.contains("youtu.be") -> "YouTube"
                link.contains("facebook.com") || link.contains("fb.watch") -> "Facebook"
                link.contains("instagram.com") -> "Instagram"
                link.contains("vimeo.com") -> "Vimeo"
                else -> "Video Link"
            }

            return VideoRecord(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", ""),
                videoLink = link,
                platform = json.optString("platform", inferredPlatform),
                description = json.optString("description", ""),
                thumbnailUrl = json.optString("thumbnail_url", ""),
                createdAt = json.optString("created_at", ""),
                updatedAt = json.optString("updated_at", "")
            )
        }
    }
}

data class SiteSettingsRecord(
    val id: String = "site_settings",
    val siteTitle: String = "নিংশিং চে",
    val siteDescription: String = "বিষ্ণুপ্রিয়া মণিপুরি ডিজিটাল সাংস্কৃতিক আর্কাইভ ও সাহিত্য পত্রিকা",
    val logoUrl: String = "",
    val contactEmail: String = "editorial@ningshingche.com",
    val contactPhone: String = "+880 1712-345678",
    val facebookUrl: String = "https://facebook.com/ningshingche",
    val youtubeUrl: String = "https://youtube.com/@ningshingche",
    val instagramUrl: String = "https://instagram.com/ningshingche",
    val heroSliderEnabled: Boolean = true,
    val featuredArticlesEnabled: Boolean = true,
    val specialArticlesEnabled: Boolean = true,
    val allowComments: Boolean = true,
    val allowUserSubmissions: Boolean = true,
    val updatedAt: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("site_title", siteTitle)
            put("site_description", siteDescription)
            put("logo_url", logoUrl)
            put("contact_email", contactEmail)
            put("contact_phone", contactPhone)
            put("facebook_url", facebookUrl)
            put("youtube_url", youtubeUrl)
            put("instagram_url", instagramUrl)
            put("hero_slider_enabled", heroSliderEnabled)
            put("featured_articles_enabled", featuredArticlesEnabled)
            put("special_articles_enabled", specialArticlesEnabled)
            put("allow_comments", allowComments)
            put("allow_user_submissions", allowUserSubmissions)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): SiteSettingsRecord {
            return SiteSettingsRecord(
                id = json.optString("id", "site_settings"),
                siteTitle = json.optString("site_title", "নিংশিং চে"),
                siteDescription = json.optString("site_description", "বিষ্ণুপ্রিয়া মণিপুরি ডিজিটাল সাংস্কৃতিক আর্কাইভ ও সাহিত্য পত্রিকা"),
                logoUrl = json.optString("logo_url", ""),
                contactEmail = json.optString("contact_email", "editorial@ningshingche.com"),
                contactPhone = json.optString("contact_phone", "+880 1712-345678"),
                facebookUrl = json.optString("facebook_url", "https://facebook.com/ningshingche"),
                youtubeUrl = json.optString("youtube_url", "https://youtube.com/@ningshingche"),
                instagramUrl = json.optString("instagram_url", "https://instagram.com/ningshingche"),
                heroSliderEnabled = json.optBoolean("hero_slider_enabled", true),
                featuredArticlesEnabled = json.optBoolean("featured_articles_enabled", true),
                specialArticlesEnabled = json.optBoolean("special_articles_enabled", true),
                allowComments = json.optBoolean("allow_comments", true),
                allowUserSubmissions = json.optBoolean("allow_user_submissions", true),
                updatedAt = json.optString("updated_at", "")
            )
        }
    }
}

data class DashboardSummaryStats(
    val totalAuthors: Int = 0,
    val totalBlogs: Int = 0,
    val publishedBlogs: Int = 0,
    val draftBlogs: Int = 0,
    val pendingSubmittedBlogs: Int = 0,
    val totalCategories: Int = 0,
    val totalComments: Int = 0,
    val publishedComments: Int = 0,
    val totalGalleries: Int = 0,
    val totalPdfBooks: Int = 0,
    val totalVideos: Int = 0,
    val totalViews: Int = 0
)

data class RecentActivityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: String, // "blog", "submission", "comment", "book", "gallery"
    val timestamp: String,
    val status: String = ""
)
