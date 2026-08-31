package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Explore : Screen("explore")
    data object Search : Screen("search")
    data object Bookmarks : Screen("bookmarks")
    data object History : Screen("history")
    data object AiAssistant : Screen("ai_assistant")
    data object Settings : Screen("settings")
    data object Dashboard : Screen("dashboard")
    data object PdfArchive : Screen("pdf_archive")
    data object Featured : Screen("featured")
    data object About : Screen("about")
    data object SocialActivities : Screen("social_activities")
    data object AuthorsDirectory : Screen("authors_directory")

    data object PdfViewer : Screen("pdf_viewer/{pdfId}") {
        fun createRoute(pdfId: String) = "pdf_viewer/$pdfId"
    }

    data object ArticleDetail : Screen("article/{articleId}") {
        fun createRoute(articleId: String) = "article/$articleId"
    }

    data object CategoryDetail : Screen("category/{categorySlug}") {
        fun createRoute(categorySlug: String) = "category/$categorySlug"
    }

    data object AuthorDetail : Screen("author/{authorId}") {
        fun createRoute(authorId: String) = "author/$authorId"
    }

    data object ArchiveDetail : Screen("archive/{year}") {
        fun createRoute(year: Int) = "archive/$year"
    }
}

data class PortalNavItem(
    val id: String,
    val label: String,
    val route: String? = null,
    val categorySlug: String? = null,
    val year: Int? = null,
    val externalUrl: String? = null
)

object PortalNavigation {
    val primary = listOf(
        PortalNavItem("home", "ঘর", route = Screen.Home.route),
        PortalNavItem("latest", "সাম্প্রতিক", route = Screen.Home.route),
        PortalNavItem("featured", "ফিচার্ড", route = Screen.Featured.route),
        PortalNavItem("pdf", "PDF আর্কাইভ", route = Screen.PdfArchive.route),
        PortalNavItem("search", "অনুসন্ধান", route = Screen.Search.route),
        PortalNavItem("bookmarks", "সংরক্ষিত", route = Screen.Bookmarks.route)
    )

    val years = (2025 downTo 2014).map { year ->
        PortalNavItem(
            id = "year-$year",
            label = "নিংশিং চে-$year",
            year = year
        )
    }

    val categories = listOf(
        "ইমার ঠারর এলা" to "language",
        "পৌ" to "news",
        "ভুমিকা" to "preface",
        "সম্পাদকীয়" to "editorial",
        "ইতিহাস" to "history",
        "সাহিত্য" to "literature",
        "সমাজ ও সংস্কৃতি" to "society-culture",
        "পর্যালোচনা" to "reviews",
        "জীবনী" to "biography",
        "স্মৃতিচারণ" to "reminiscence",
        "পৌরাণিক কাহিনী" to "mythology",
        "বিজ্ঞান ও প্রযুক্তি" to "science-technology",
        "সংস্কৃতি" to "culture",
        "রকমারি" to "misc",
        "ধর্ম" to "religion",
        "কবিতা" to "poetry"
    ).map { (name, slug) ->
        PortalNavItem(id = "cat-$slug", label = name, categorySlug = slug)
    }

    val portal = listOf(
        PortalNavItem("about", "আমার সম্পর্কে", route = Screen.About.route),
        PortalNavItem("authors", "লেখক", route = Screen.AuthorsDirectory.route),
        PortalNavItem("social", "সামাজিক কার্যকলাপ", route = Screen.SocialActivities.route),
        PortalNavItem(
            "submit",
            "লেখা জমাদান",
            externalUrl = "https://ningshingche.com/blog_submission"
        )
    )
}
