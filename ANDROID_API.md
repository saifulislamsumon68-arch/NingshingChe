# Ningshing Che — Android App: UI Foundation & API Integration Guide

**Audience:** the engineer/AI agent implementing the app UI against the live API.
**Companion:** [`backend/API.md`](./backend/API.md) — the server-side contract (REST, RPC, Storage, RLS,
error codes). Read that first; this document explains *how the Kotlin app consumes it today* and
*what must change* so every screen is functional.

Everything below was read from `app/src/main/java/com/example/**`, `app/build.gradle.kts`, and
`gradle/libs.versions.toml`. Line references are to the current `main`.

---

## Table of contents

1. [Project facts](#1-project-facts)
2. [Architecture](#2-architecture)
3. [Composition root and DI](#3-composition-root-and-di)
4. [Navigation](#4-navigation)
5. [Theme and design tokens](#5-theme-and-design-tokens)
6. [Data layer — Supabase client](#6-data-layer--supabase-client)
7. [Data layer — models and column mapping](#7-data-layer--models-and-column-mapping)
8. [Data layer — Room cache](#8-data-layer--room-cache)
9. [Data layer — repositories](#9-data-layer--repositories)
10. [Image and PDF media](#10-image-and-pdf-media)
11. [ViewModel contracts](#11-viewmodel-contracts)
12. [UI inventory — screens and components](#12-ui-inventory--screens-and-components)
13. [API coverage matrix (implemented vs available)](#13-api-coverage-matrix-implemented-vs-available)
14. [Critical gaps and required changes](#14-critical-gaps-and-required-changes)
15. [Implementation recipes](#15-implementation-recipes)
16. [Definition of done](#16-definition-of-done)

---

## 1. Project facts

| Item | Value |
| --- | --- |
| Module | `app/` (single-module Gradle project, root `settings.gradle.kts` includes `:app`) |
| Namespace / packages | `com.example` (root), `com.example.data.*`, `com.example.ui.*`, `com.example.util.*` |
| `applicationId` | `com.aistudio.ningshingche.bcdppr` |
| `minSdk` / `targetSdk` / `compileSdk` | 24 / 36 / 36.1 |
| Kotlin | 2.2.10, JVM target 11 |
| Compose | Material 3, BOM `2024.09.00`, `compose.compiler` via `org.jetbrains.kotlin.plugin.compose` |
| Navigation | `androidx.navigation:navigation-compose` 2.8.9 |
| Local DB | Room 2.7.0 (KSP) |
| Images | Coil 2.7.0 (`coil-compose`) |
| Networking | OkHttp 4.10.0 (used directly); **Retrofit 2.12.0 + Moshi 1.15.2 + logging-interceptor are already dependencies but unused** |
| Prefs | `androidx.datastore:datastore-preferences` 1.1.7 |
| AI | `firebase-ai` (Firebase AI Logic / Gemini), Firebase App Check reCAPTCHA |
| Secrets | `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` — reads `.env`, falls back to `.env.example`, injects `BuildConfig.*` |
| Screenshots tests | Roborazzi 1.59.0 + Robolectric 4.16.1 |
| App entry | `NinghsingCheApp : Application` → `MainActivity : ComponentActivity` |
| Permissions | `INTERNET`, `ACCESS_NETWORK_STATE` |
| Fonts | `app/src/main/res/font/kalpurush.ttf` (Bengali) |

BuildConfig keys injected from `.env`: `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, `IMGBB_API_KEY`,
`GEMINI_API_KEY`. Values starting with `MY_` are treated as unset and fall back to
`SupabaseConfig.DEFAULT_*`.

---

## 2. Architecture

```
ui/                         Jetpack Compose screens + reusable components
  ├── MainActivity.kt       NavHost, drawer, top bar, bottom bar, ViewModel wiring
  ├── screens/              Reader-side screens (Home, Explore, Search, Reader, ...)
  ├── dashboard/            In-app CMS (mirrors backend/ web dashboard)
  ├── components/           Shared editorial/portal components
  ├── viewmodel/            HomeViewModel, ExploreViewModel, ... + ViewModelFactory
  └── theme/                Color.kt, Type.kt, Theme.kt
        │
        ▼  (StateFlow / suspend calls)
data/
  ├── repository/
  │   ├── ArticleRepository     reader side: Room cache + Supabase + website fallback
  │   └── DashboardRepository   CMS side: in-memory StateFlows + Supabase writes
  ├── remote/
  │   ├── SupabaseConfig        base URLs + keys
  │   ├── SupabaseClient        OkHttp PostgREST CRUD + Supabase Auth
  │   ├── SupabaseModels        *Record data classes + UserProfile/UserRole
  │   ├── ImgBbUploader         multipart image upload
  │   └── NingshingCheWebsiteClient / WebsiteHtml / AuthorProfiles
  ├── local/                    Room entities, DAOs, AppDatabase
  ├── preferences/              UserPreferencesRepository (DataStore)
  ├── ai/                       NinghsingCheAiAssistant
  └── model/                    UI-facing domain models (Article, Author, Category, ...)
```

**Reader flow:** Room is the single source of truth for article lists (offline-first).
`ArticleRepository.syncFromSupabaseOrWebsite()` refreshes Room from Supabase, falling back to
scraping ningshingche.com, falling back to the shipped seed data in `NinghsingCheContentData`.

**CMS flow:** `DashboardRepository` keeps each entity in a `MutableStateFlow`, seeds it from
`NinghsingCheContentData`, then swaps in Supabase data. Writes are **optimistic**: the local
`StateFlow` is updated first, then the network call is fired, and the result is **discarded**
(`Result.success(...)` is returned regardless of the HTTP outcome).

---

## 3. Composition root and DI

`NinghsingCheApp.onCreate()` (`app/src/main/java/com/example/NinghsingCheApp.kt`):

```kotlin
database            = AppDatabase.getInstance(this)      // Room: "ningshingche_database"
websiteClient       = NingshingCheWebsiteClient()
supabaseClient      = SupabaseClient(this)               // SharedPrefs session restore
dashboardRepository = DashboardRepository(this, supabaseClient, database)
articleRepository   = ArticleRepository(database, supabaseClient, websiteClient)
preferencesRepository = UserPreferencesRepository(this)
aiAssistant         = NinghsingCheAiAssistant(articleRepository)
```

`MainActivity` builds `ViewModelFactory(repository, preferencesRepository, aiAssistant,
dashboardRepository, applicationContext)` and passes it to every `viewModel(factory = factory)`
call. **There is no Hilt/Koin** — to add a new ViewModel, add a branch to
`ViewModelFactory.create()` (`ui/viewmodel/MainViewModels.kt:610`).

> If you add a dependency (e.g. a `SessionManager`), construct it in `NinghsingCheApp.onCreate()`
> and add it to the `ViewModelFactory` constructor.

---

## 4. Navigation

`ui/navigation/Screen.kt` — sealed class, one `data object` per destination:

| Route | Args | Notes |
| --- | --- | --- |
| `home` | — | start destination |
| `explore` | — | bottom bar |
| `search` | — | |
| `bookmarks` | — | bottom bar |
| `history` | — | |
| `ai_assistant` | — | |
| `settings` | — | |
| `dashboard` | — | In-app CMS. **Not** in the bottom bar; reachable from the drawer ("ড্যাশবোর্ড (CMS)") |
| `pdf_archive` | — | bottom bar |
| `featured` | — | bottom bar |
| `about`, `social_activities`, `authors_directory` | — | portal pages |
| `pdf_viewer/{pdfId}` | `pdfId: String` | |
| `article/{articleId}` | `articleId: String` | deep links: `ningshingche.com/{articleId}`, `ningshingche.com/article/{articleId}` |
| `category/{categorySlug}` | `categorySlug: String` | |
| `author/{authorId}` | `authorId: String` | |
| `archive/{year}` | `year: Int` | |
| `web_article/{year}/{month}/{slug}` | three strings | deep link `ningshingche.com/{year}/{month}/{slug}`; `.kehem` suffix stripped |

Chrome is set in `MainActivity`:

- **Drawer** (`ModalNavigationDrawer` + `PortalDrawerContent`) on *all* routes.
- **Top bar** (`PortalTopBar`) on `home, explore, bookmarks, pdf_archive, featured, about,
  social_activities, authors_directory`.
- **Bottom bar** (`EditorialBottomNavBar`) on `home, explore, bookmarks, pdf_archive, featured`.
- Transitions: 280 ms slide+fade in, 240 ms out, both directions.
- Drawer navigation uses `popUpTo(home) { saveState = true }`, `launchSingleTop`, `restoreState`.

`PortalNavigation` (same file) holds the drawer's static Bengali menu: `primary` (ঘর, সাম্প্রতিক,
ফিচার্ড, PDF আর্কাইভ, অনুসন্ধান, সংরক্ষিত), `years` (2025 → 2014), `categories` (16 hard-coded
Bengali name → slug pairs), `portal` (আমার সম্পর্কে, লেখক, সামাজিক কার্যকলাপ, লেখা জমাদান → external URL).

> **Task for the UI work:** `PortalNavigation.categories` is hard-coded. Replace it with
> `GET /rest/v1/categories?select=*&order=title.asc` so new categories appear automatically.
> The same applies to `years` — derive from `blogs.published_date`.

---

## 5. Theme and design tokens

`ui/theme/Color.kt` — portal palette:

| Token | Value |
| --- | --- |
| `PortalMaroon` | `0xFF6E2B19` |
| `PortalSaffron` | `0xFFFF8C00` |
| `PortalDeepBrown` | `0xFF4B2E2B` |
| `PortalCream1` | `0xFFF9F5E9` |
| `PortalCream2` | `0xFFFFFAF2` |
| `PortalGold` | `0xFFFFD700` |
| `PortalWhite` | `0xFFFFFFFF` |
| `PortalDarkBg` / `Surface` / `Variant` / `Border` / `Text` | `0xFF111827` / `0xFF1F2937` / `0xFF374151` / `0xFF4B5563` / `0xFFF9FAFB` |

Plus an amber ramp (`Amber50` … `Amber950`) and reader-only palettes: `SepiaCanvas`, `SepiaSurface`,
`SepiaText`, `PaperCanvasLight`, `PaperSurfaceLight`, `PaperSurfaceVariantLight`, `PaperCardBorderLight`.

`ui/theme/Theme.kt` — `MyApplicationTheme(darkTheme: Boolean, content)` builds
`LightPortalScheme` / `DarkPortalScheme` (`lightColorScheme` / `darkColorScheme`), applies
`MaterialTheme`, and uses `SideEffect` + `WindowCompat` for edge-to-edge status bar colour.
`MainActivity` selects dark mode from `AppThemeMode.SYSTEM | LIGHT | DARK` in DataStore.

Reader comfort modes (`ReaderThemeMode.PAPER | SEPIA | NIGHT | CRISP`) are separate from the app
theme and are driven by `ReaderPreferences` (font 12–26 sp, line spacing 1.2–2.4).

---

## 6. Data layer — Supabase client

`data/remote/SupabaseConfig.kt`:

```kotlin
restBaseUrl  = "$supabaseUrl/rest/v1"    // https://slcpvmpsynkqdozvlsii.supabase.co/rest/v1
authBaseUrl  = "$supabaseUrl/auth/v1"
imgbbUploadUrl = "https://api.imgbb.com/1/upload"
```

`data/remote/SupabaseClient.kt` — one `OkHttpClient` (connect 20 s, read/write 30 s), hand-built
`org.json` payloads, `Dispatchers.IO`, `Result<T>` returns.

### 6.1 Base headers (`createBaseRequestBuilder`)

```
apikey: <publishable key>
Content-Type: application/json
Prefer: return=representation
Authorization: Bearer <authToken ?: publishableKey>
```

⚠️ **The app never sends `x-dashboard-session`.** See [§14.1](#141-writes-will-be-rejected-by-rls-critical).

### 6.2 Method → HTTP call

| Kotlin method | HTTP | Query / body |
| --- | --- | --- |
| `signIn(email, pass)` | `POST /auth/v1/token?grant_type=password` | `{email, password}`; also accepts hard-coded `admin@ningshingche.com`/`admin123` or stored admin creds |
| `signUp(email, pass, name, role)` | `POST /auth/v1/signup` | `{email, password, data:{full_name, role}}`; falls back to a fake local session on failure |
| `signOut()` | — | clears SharedPrefs |
| `getAuthors()` | `GET /authors?select=*&order=created_at.desc` | no limit |
| `upsertAuthor(a)` | `POST /authors` | full row, `Prefer: resolution=merge-duplicates` |
| `deleteAuthor(id, imgbbDeleteUrl)` | `DELETE /authors?id=eq.{id}` | best-effort ImgBB delete first |
| `getCategories()` | `GET /categories?select=*&order=created_at.asc` | |
| `upsertCategory(c)` | `POST /categories` | merge-duplicates |
| `deleteCategory(id)` | `DELETE /categories?id=eq.{id}` | |
| `getBlogs(query, categoryId, authorId, status, limit=100, offset=0)` | `GET /blogs?select=*&order=created_at.desc&limit&offset` | optional `category_id=eq.`, `author_id=eq.`, `status=eq.`, `title=ilike.*q*` |
| `getBlogById(idOrSlug)` | `GET /blogs?or=(id.eq.X,slug.eq.X)&limit=1` | |
| `upsertBlog(b)` | `POST /blogs` | merge-duplicates |
| `deleteBlog(id)` | `DELETE /blogs?id=eq.{id}` | |
| `getComments(blogId, status)` | `GET /comments?select=*&order=created_at.desc` | optional `blog_id=eq.`, `status=eq.` |
| `upsertComment(c)` | `POST /comments` | merge-duplicates |
| `updateCommentStatus(id, status)` | `PATCH /comments?id=eq.{id}` | `{status}` |
| `deleteComment(id)` | `DELETE /comments?id=eq.{id}` | |
| `getGalleries(category)` | `GET /galleries?select=*&order=created_at.desc` | optional `category=eq.` |
| `upsertGallery(g)` / `deleteGallery(id, del)` | `POST` / `DELETE /galleries?id=eq.{id}` | |
| `getPdfBooks()` | `GET /pdf_books?select=*&order=created_at.desc` | |
| `upsertPdfBook(b)` / `deletePdfBook(id, del)` | `POST` / `DELETE /pdf_books?id=eq.{id}` | |
| `getSubmittedBlogs(status)` | `GET /submitted_blogs?select=*&order=created_at.desc` | optional `status=eq.` |
| `upsertSubmittedBlog(s)` | `POST /submitted_blogs` | |
| `updateSubmittedBlogStatus(id, status)` | `PATCH /submitted_blogs?id=eq.{id}` | `{status}` |
| `deleteSubmittedBlog(id, del)` | `DELETE /submitted_blogs?id=eq.{id}` | |
| `getVideos()` | `GET /videos?select=*&order=created_at.desc` | |
| `upsertVideo(v)` / `deleteVideo(id)` | `POST` / `DELETE /videos?id=eq.{id}` | |
| `getSettings()` | `GET /settings?id=eq.site_settings&limit=1` | returns defaults on any failure |
| `updateSettings(s)` | `POST /settings` | merge-duplicates |

Session persistence: `SharedPreferences("supabase_auth_session")` → `access_token`, `user_profile`,
`admin_email`, `admin_password`. Exposed as `currentUser: StateFlow<UserProfile?>`.

**Not implemented in `SupabaseClient` at all:** every RPC (`dashboard_login`, `dashboard_session`,
`approve_submission`, …), Storage upload/delete, `count`/`Content-Range` totals, `select`
projection, `imgbb_delete_url`/`image_meta`/`inline_media`/`pdf_storage_path` columns.

---

## 7. Data layer — models and column mapping

`data/remote/SupabaseModels.kt` holds the wire records (each with `toJson()` / `fromJson()`), and
`data/model/Models.kt` holds the UI domain models. `ArticleRepository` converts between them.

### 7.1 `BlogRecord` ↔ `blogs`

| `BlogRecord` field | `blogs` column | Notes |
| --- | --- | --- |
| `id` | `id` | defaults to a random UUID client-side |
| `title`, `subTitle` | `title`, `sub_title` | |
| `image` | `image` | |
| `content` | `content` | HTML |
| `categoryId`, `categoryTitle`, `categorySlug` | `category_id`, `category_title`, `category_slug` | snapshots are trigger-maintained server-side |
| `status` | `status` | `"Draft"` \| `"Publish"` |
| `tags: List<String>` | `tags text[]` | |
| `seoTitle` | `seo_title` | **`seo_description` is missing** |
| `videoLink`, `pdfBookLink` | `video_link`, `pdf_book_link` | |
| `slug` | `slug` | |
| `authorId`, `authorName`, `authorImage` | `author_id`, `author_name`, `author_image` | |
| `isSlider`, `isFeature`, `isSpecialArticle` | `is_slider`, `is_feature`, `is_special_article` | |
| `viewsCount`, `readingTimeMinutes` | `views_count`, `reading_time_minutes` | reading time is recomputed by trigger |
| `publishedDate` | `published_date` | |
| — | `imgbb_delete_url`, `image_meta`, `inline_media`, `pdf_file_provider`, `pdf_storage_path`, `pdf_file_size_mb` | **not modelled** |

`BlogRecord.fromJson` has tolerant fallbacks (`featured_image_url`, `category`, `author_avatar_url`,
`is_featured`, `view_count`) so older payloads still parse.

### 7.2 Other records

| Record | Table | Missing columns vs. schema |
| --- | --- | --- |
| `AuthorRecord` | `authors` | `image_meta` |
| `CategoryRecord` | `categories` | — |
| `CommentRecord` | `comments` | — |
| `GalleryRecord` | `galleries` | `image_meta` |
| `PdfBookRecord` | `pdf_books` | `image_meta`, `file_provider`, `file_storage_path` |
| `SubmittedBlogRecord` | `submitted_blogs` | `thumbnail_meta`, `writer_designation` ✅ / `writer_profile_delete_url`, `writer_profile_meta`, `writer_facebook` ✅, `inline_media`, `reviewed_at`, `converted_blog_id` |
| `VideoRecord` | `videos` | — (`platform` inferred from URL when absent) |
| `SiteSettingsRecord` | `settings` | `favicon_url`, `default_seo_title`, `default_seo_description` |
| `UserProfile` / `UserRole` | — | **not backed by any table**; the real model is `dashboard_users` + `dashboard_roles` |
| `DashboardSummaryStats`, `RecentActivityItem` | — | computed locally, not from an RPC |

### 7.3 Mapping to UI models (in `ArticleRepository`, private)

| Source | Target | Key rules |
| --- | --- | --- |
| `BlogRecord.toArticle()` | `Article` | `slug` falls back to `id`; `excerpt` from `subTitle`, else first 160 chars of HTML-stripped `content`; `year` from digits in `publishedDate` → `createdAt` → 2026; `isFeatured = isFeature \|\| isSlider`; `sourceUrl = https://ningshingche.com/article/{slug}` |
| `CategoryRecord.toCategory()` | `Category` | `name = title`, `description = subTitle` |
| `AuthorRecord.toAuthor()` | `Author` | `name = title`, `bio = description`, `avatarUrl = image` |
| `PdfBookRecord.toPdfDocument()` | `PdfDocument` | `categorySlug` = slugified `category`; `year` from `bookPublishedDate`; `downloadUrl = pdfUrl = link` |
| `ArticleEntity.toModel()` / `Article.toEntity()` | Room ↔ domain | `tags` and `relatedArticleIds` stored comma-separated |

⚠️ `DashboardRepository.syncBlogsToLocalRoom()` hard-codes `year = 2026` and
`publishedDate = b.publishedDate.ifBlank { "২০২৬" }`, so year/archive filtering breaks for older
posts. Fix by parsing `published_date` properly.

---

## 8. Data layer — Room cache

`AppDatabase` — `ningshingche_database`, **version 1**, `fallbackToDestructiveMigration()`.

| Entity / table | Columns |
| --- | --- |
| `ArticleEntity` / `articles` | `id` PK, `title`, `slug`, `excerpt`, `content`, `featuredImageUrl`, `authorId`, `authorName`, `authorAvatarUrl`, `category`, `categorySlug`, `tagsRaw`, `publishedDate`, `year`, `readingTimeMinutes`, `isFeatured`, `isEditorialPick`, `viewCount`, `sourceUrl`, `relatedArticleIdsRaw`, `cachedAt` |
| `BookmarkEntity` / `bookmarks` | `articleId` PK, `savedAtTimestamp`, `folder`, `note` |
| `HistoryEntity` / `reading_history` | `articleId` PK, `readAtTimestamp`, `scrollPosition`, `progressPercent` |
| `SearchHistoryEntity` / `search_history` | `id` auto PK, `query`, `timestamp` |

DAOs return `Flow<List<…>>` for lists and `suspend` for one-shots:
`ArticleDao.getAllArticles()`, `getArticleById`, `getArticleByIdOrSlug` (matches `id` **or** `slug`),
`getFeaturedArticles`, `getArticlesByCategory`, `getArticlesByAuthor`, `getArticlesByYear`,
`searchArticles` (LIKE over title/content/authorName/category), `insertArticles(REPLACE)`,
`deleteArticleById`, `clearAll`, `deleteSeedArticles()` (deletes ids `LIKE 'art-%'`), `countArticles`.
`BookmarkDao`, `HistoryDao`, `SearchDao` follow the same shape.

> Adding a column/entity requires bumping the DB version and providing a migration, otherwise
> `fallbackToDestructiveMigration()` wipes the offline cache on update.

---

## 9. Data layer — repositories

### 9.1 `ArticleRepository` (reader side)

```kotlin
class ArticleRepository(database, supabaseClient?, websiteClient)
```

Public surface:

| Member | Type | Purpose |
| --- | --- | --- |
| `categories`, `authors`, `yearArchives`, `pdfDocuments`, `pdfCategories` | `StateFlow<List<…>>` | seeded from `NinghsingCheContentData`, replaced after sync |
| `syncState` | `StateFlow<WebsiteSyncState>` | `{isSyncing, lastSuccessAt, lastMessage, liveArticleCount, usingLiveSite}` |
| `syncFromSupabaseOrWebsite()` | `suspend → Result<Int>` | **the** sync entry point |
| `syncFromWebsite()` / `refreshInBackground()` | fire-and-forget | launch the sync on the repo scope |
| `seedInitialArticles()` | `suspend` | load bundled seed data into Room |
| `getAllArticles()` / `getFeaturedArticles()` / `getArticlesByCategory` / `getArticlesByAuthor` / `getArticlesByYear` | `Flow<List<Article>>` | Room-backed |
| `getArticleById(idOrSlug)` | `suspend → Article?` | matches `id` or `slug` |
| `searchArticles(query, categorySlug, year)` | `Flow<List<Article>>` | Room LIKE + in-memory filters |
| `loadComments(articleUrlOrId)` | `suspend → List<ArticleComment>` | |
| `submitComment(urlOrId, name, address, email, phone, content)` | `suspend → Result<…>` | |
| `getCategories()`, `getCategoryBySlug()`, `getAuthors()`, `getAuthorById()`, `getYearArchives()`, `getYearArchiveByYear()` | sync snapshot | current `StateFlow` value |
| `getPdfCategories()`, `getPdfDocuments()`, `getPdfDocumentById()`, `getPdfDocumentsByCategory()` | sync snapshot | |
| `getAllBookmarks()`, `isBookmarked(id)`, `toggleBookmark(id)` | Flow / suspend | |
| `getReadingHistory()`, `saveReadingProgress(id, pos, pct)`, `clearHistory()` | Flow / suspend | |
| `getRecentSearches()`, `recordSearch(q)`, `removeSearch(q)`, `clearSearchHistory()` | Flow / suspend | |
| `clearAllCache()` | `suspend` | |

**Sync strategy** (`syncFromSupabaseOrWebsite`, guarded by a `Mutex`):
1. `supabaseClient.getBlogs(status = "Publish")` → map to `Article` → `upsertLiveArticles()` →
   `deleteSeedArticles()`; then refresh `categories`, `authors`, `pdf_books` from Supabase;
   rebuild `yearArchives`.
2. If Supabase fails or returns empty → `websiteClient.syncCatalog()` (scrape ningshingche.com) and
   use its categories/authors/pdfDocuments.
3. If both fail → keep the Room cache and set `lastMessage = "সিঙ্ক সম্পন্ন: অফলাইন আর্কাইভ প্রস্তুত।"`.

### 9.2 `DashboardRepository` (CMS side)

Holds `MutableStateFlow` for `authors, categories, blogs, comments, galleries, pdfBooks,
submittedBlogs, videos, settings, summaryStats, recentActivities, isLoading`. On `init` it seeds
everything from `NinghsingCheContentData` (+ hard-coded sample galleries, videos, comments, and one
submission) then calls `refreshAll()`.

`refreshAll()` pulls each table with the "ignore empty result" pattern:
`supabaseClient.getAuthors().onSuccess { if (it.isNotEmpty()) _authors.value = it }`.

Mutation pattern (**optimistic, error-swallowing**):

```kotlin
suspend fun saveAuthor(author: AuthorRecord): Result<AuthorRecord> {
    /* update StateFlow, recalculate stats */
    supabaseClient.upsertAuthor(author)     // result discarded
    Result.success(author)                  // always success
}
```

Actions: `saveAuthor/deleteAuthor`, `saveCategory/deleteCategory`, `saveBlog/deleteBlog`,
`saveComment/toggleCommentStatus/deleteComment`, `saveGallery/deleteGallery`,
`savePdfBook/deletePdfBook`, `saveSubmittedBlog/updateSubmissionStatus/deleteSubmittedBlog`,
`approveAndPublishSubmission`, `saveVideo/deleteVideo`, `saveSettings`.

`approveAndPublishSubmission()` is a **local** 3-step conversion (set status → find-or-create author
→ insert blog). It does **not** call `POST /rest/v1/rpc/approve_submission`, so it is not
transactional and will not set `converted_blog_id`.

Stats: `DashboardSummaryStats` is computed by counting the in-memory lists — because `refreshAll()`
is unpaginated this is only correct while every table is small.

### 9.3 `UserPreferencesRepository` (DataStore)

`readerPreferences: Flow<ReaderPreferences>` plus `updateFontSize`, `updateLineSpacing`,
`updateThemeMode`, `updateAppThemeMode`, `updateTtsSpeed`, `updateNotificationNew`,
`updateNotificationFeatured`.

### 9.4 AI

`NinghsingCheAiAssistant(repository).answerQuestion(q): AiChatMessage` — used by `AiViewModel`.
UI: `AiAssistantScreen` with `suggestedQuestions` and `AiSourceCitationCard`.

---

## 10. Image and PDF media

### Images — `ImgBbUploader`

```kotlin
data class ImgBbUploadResult(url, displayUrl, deleteUrl, title, sizeBytes, mimeType)

suspend fun uploadFromUri(context, uri, customName?): Result<ImgBbUploadResult>
suspend fun uploadBitmap(bitmap, customName?, format = JPEG, quality = 90): Result<ImgBbUploadResult>
suspend fun uploadBytes(bytes, fileName, mimeType): Result<ImgBbUploadResult>
suspend fun attemptDeleteImage(deleteUrl: String): Boolean
```

Multipart `POST https://api.imgbb.com/1/upload`, field `image` = base64, key in the query string.
Validates `image/*` MIME and the 32 MB cap. Error strings are Bengali.

UI: `DashboardImageUploader` composable (gallery picker + URL entry + progress + preview).

### PDFs — `util/PdfHelper.kt`

`getOrGeneratePdfFile(context, PdfDocument)`, `renderPdfPages(file): List<Bitmap>`,
`savePdfToDownloads(context, doc)`, `sharePdfFile(context, doc, file)`; `util/ApkManager.kt` for
app-update handling. PDFs are opened via the `link`/`pdfUrl` field; **the app never touches the
Supabase Storage `pdf-books` bucket**.

---

## 11. ViewModel contracts

All use `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), …)`.

| ViewModel | Exposed state | Actions |
| --- | --- | --- |
| `HomeViewModel` | `allArticles`, `featuredArticles`, `categories`, `authors`, `yearArchives`, `pdfDocuments`, `syncState`, `readingHistory`, `scrollToTop: SharedFlow<Unit>` | `requestScrollToTop()`, `refreshFromWebsite()` |
| `ExploreViewModel` | `categories`, `authors`, `yearArchives`, `allArticles`, `selectedTab: Int` (0 Categories, 1 Authors, 2 Archives, 3 Popular) | `selectTab()` |
| `SearchViewModel` | `searchQuery`, `selectedCategorySlug`, `selectedYear`, `categories`, `yearArchives`, `recentSearches`, `searchResults`, `popularSuggestions` | `onQueryChange`, `setCategoryFilter`, `setYearFilter`, `executeSearch`, `removeRecentSearch`, `clearAllRecentSearches` |
| `ReaderViewModel` | `currentArticle`, `relatedArticles`, `isBookmarked`, `comments`, `commentStatus`, `isSubmittingComment`, `readerPreferences`, `isTtsPlaying`, `ttsProgressText` | `loadArticle(id)`, `submitComment(...)`, `toggleBookmark()`, `updateReadingProgress()`, `updateFontSize/LineSpacing/ThemeMode`, `toggleTts()`, `stopTts()` |
| `BookmarksViewModel` | `searchSavedQuery`, `selectedCategoryFilter`, `bookmarkedArticles` | `onSearchQueryChange`, `setCategoryFilter`, `removeBookmark` |
| `HistoryViewModel` | `historyItems: List<Pair<ReadingHistory, Article>>` | `clearAllHistory()` |
| `AiViewModel` | `messages: List<AiChatMessage>`, `isLoading`, `suggestedQuestions` | `sendQuestion()` |
| `SettingsViewModel` | `preferences` | `updateAppThemeMode`, `toggleNewArticlesNotif`, `toggleFeaturedNotif`, `clearCache`, `clearHistory` |
| `PdfArchiveViewModel` | `categories`, `allPdfDocuments`, `selectedCategoryId`, `filteredPdfs`, `isLoading` | `selectCategory()` |
| `PdfViewerViewModel` | `pdfDocument`, `pages: List<Bitmap>`, `currentPage`, `isLoading`, `downloadStatus` | `loadPdf(id)`, `setPage`, `downloadPdf`, `sharePdf`, `clearStatus` |
| `DashboardViewModel` | `currentSection: DashboardSection`, `isInitialSkeletonLoading`, all entity flows, `summaryStats`, `recentActivities`, `isLoading`, `currentUser` | `setSection`, `refreshData`, `save*/delete*` per entity, `toggleCommentStatus`, `approveAndPublishSubmission`, `rejectSubmission`, `saveSettings`, `updateCurrentUser`, `updateAdminCredentials`, `signOut` |

`DashboardSection` (in `ui/dashboard/components/DashboardSidebarDrawer.kt`):
`HOME, AUTHORS, BLOGS, CATEGORIES, COMMENTS, GALLERIES, PDF_BOOKS, SUBMIT_BLOGS, VIDEOS, SETTINGS`.

---

## 12. UI inventory — screens and components

**Reader screens** (`ui/screens/`): `HomeScreen`, `FeaturedScreen`, `ExploreScreen`, `SearchScreen`,
`BookmarksScreen`, `HistoryScreen`, `ArticleReaderScreen`, `PdfArchiveScreen`, `PdfViewerScreen`,
`AiAssistantScreen`, `SettingsScreen`, and in `PortalPages.kt`: `AboutScreen`,
`AuthorsDirectoryScreen`, `SocialActivitiesScreen`; in `DetailListScreens.kt`:
`CategoryDetailScreen`, `AuthorDetailScreen`, `ArchiveYearDetailScreen`.

**CMS** (`ui/dashboard/`): `DashboardScreen` (shell + login gate) and
`views/`: `DashboardHomeView`, `DashboardLoginView`, `DashboardSettingsView`,
`AuthorsManagementView`, `BlogsManagementView`, `CategoriesManagementView`,
`CommentsManagementView`, `GalleriesManagementView`, `PdfBooksManagementView`,
`SubmittedBlogsManagementView`, `VideosManagementView`, `UserProfileAuthDialog`.

**Reusable components** — reuse these instead of writing new ones:

`ui/components/EditorialComponents.kt` (1,627 lines): `NingshingCheBrandLogo`, `EditorialTopHeader`,
`EditorialBottomNavBar`, `HeroArticleCarousel`, `FeaturedArticleHeroCard`, `ArticleListItemCard`,
`CategoryFilterChip`, `AuthorCardItem`, `YearArchiveTimelineCard`, `PdfDocumentCard`,
`PdfDocumentCardItem`, `PdfCategoryFilterChip`, `AiSourceCitationCard`,
`EditorialNavigationDrawerContent`, `getCategoryIcon(slug)`.

`ui/components/PortalHomeSections.kt`: `PortalSectionHeader`, `FeaturedPortalCard`,
`SelectedEssayCard`, `CategoryImageTile`, `AuthorRailCard`, `PdfBookRailCard`, `HorizontalCardsRow`,
`SubmitWritingBanner`.

Also: `PortalDrawerContent`, `PortalTopBar`, `MarkdownFormattedText`, `SkeletonShimmer`,
`VerifiedBadge`, and dashboard kit `DashboardStatCard`, `StatusBadge`, `ConfirmDeleteDialog`,
`DashboardHeaderBar`, `EmptyStateView`, `BlogPreviewDialog`, `DashboardImageUploader`,
`DashboardRichTextEditor`, `DashboardTagInput`, `DashboardSidebarContent`.

---

## 13. API coverage matrix (implemented vs available)

Legend: ✅ used · ⚠️ partially / incorrectly used · ❌ not used

| Capability | Web dashboard | Android app |
| --- | --- | --- |
| Read `authors`, `categories`, `blogs`, `comments`, `galleries`, `pdf_books`, `submitted_blogs`, `videos`, `settings` | ✅ | ✅ |
| Filter by `category_id` / `author_id` / `status` / `title ilike` | ✅ | ✅ (blogs only) |
| Pagination (`limit`/`offset` + `Content-Range` totals) | ✅ | ⚠️ `offset` param exists, never exercised; no counts |
| Column projection (`select=`) | ✅ | ❌ always `select=*` |
| Create / update (upsert) | ✅ | ⚠️ always upsert-with-client-UUID; no real `PATCH` except comment/submission status |
| Delete | ✅ | ✅ (+ ImgBB delete attempt) |
| Dashboard session RPCs (`dashboard_login`, `dashboard_session`, `dashboard_logout`) | ✅ | ❌ |
| Access-control RPCs (users/roles/credentials) | ✅ | ❌ |
| `approve_submission` RPC | ✅ | ❌ (local 3-step fallback only) |
| Supabase Storage PDF upload/delete | ✅ | ❌ |
| ImgBB upload | ✅ | ✅ |
| ImgBB delete | ✅ best-effort | ✅ best-effort |
| Global search across tables | ✅ | ❌ (Room-only LIKE search) |
| Schema/health probe | ✅ | ❌ |
| Realtime subscriptions | ❌ | ❌ |

**Consequence:** today the app is a *reader* with a partially-wired CMS. Reads work because the
public `SELECT` policies allow anonymous access; **writes only work on installs that still have the
pre-migration-004 `is_dashboard_request()` digest path**, and never on a properly secured install.

---

## 14. Critical gaps and required changes

### 14.1 Writes will be rejected by RLS (critical)

`schema.sql` grants anonymous `SELECT` on content, but migration 004 replaces the write policies
with menu-permission checks driven by `public.dashboard_current_user_id()`, which reads the
`x-dashboard-session` header. `SupabaseClient` never sends it, so every `POST`/`PATCH`/`DELETE`
from the app returns `401`/`403` (`42501`).

**Fix:** add a session manager that stores the raw token from `dashboard_login` and injects
`x-dashboard-session: <token>` into `createBaseRequestBuilder()`. See [§15.1](#151-session-manager-and-auth-header).

### 14.2 Authentication is fake

`signIn()` accepts `admin@ningshingche.com` / `admin123` locally and stores the password in
plaintext SharedPrefs (`admin_password`). `UserRole` (`ADMINISTRATOR/EDITOR/MODERATOR/AUTHOR`) does
not exist in the database — the real roles are `dashboard_roles` rows with `menu_permissions`.

**Fix:** call `POST /rest/v1/rpc/dashboard_login` with `{p_username, p_password, p_remember,
p_user_agent}`, persist `{token, expires_at, user}` in **EncryptedSharedPreferences**, delete
`admin_password` storage, and gate UI with `user.permissions` instead of `UserRole`.
Persist sessions in `EncryptedSharedPreferences` or `crypto-ktx`-backed storage, not plain
SharedPreferences.

### 14.3 No pagination, no counts

Every list call is unpaginated (`limit=100` for blogs, unlimited elsewhere). `DashboardSummaryStats`
is therefore computed from a partial list, and large archives will OOM.

**Fix:** read `Content-Range` for exact totals (`Prefer: count=exact`, parse `*/N` or `a-b/N`) and
page with `limit`/`offset`; add a Paging 3 `PagingSource` for `blogs` and `comments`.

### 14.4 Missing columns

Add to the records and `toJson()`/`fromJson()`:
`BlogRecord` → `imgbb_delete_url`, `image_meta`, `inline_media`, `pdf_file_provider`,
`pdf_storage_path`, `pdf_file_size_mb`, `seo_description`.
`AuthorRecord`/`GalleryRecord` → `image_meta`. `PdfBookRecord` → `image_meta`, `file_provider`,
`file_storage_path`. `SubmittedBlogRecord` → `thumbnail_meta`, `writer_profile_delete_url`,
`writer_profile_meta`, `inline_media`, `reviewed_at`, `converted_blog_id`.

### 14.5 Hard-coded content

`NinghsingCheContentData` (753 lines) seeds articles, authors, categories, and PDF books;
`DashboardRepository.initializeDefaultsFromSeed()` also injects fake galleries, videos, comments,
and a submission with Unsplash URLs and a Rickroll YouTube link. `PortalNavigation.categories` and
`.years` are hard-coded; `year` is hard-coded to 2026 on sync.

**Fix:** treat seed data as an offline-only fallback (behind a flag), and drive the drawer,
archives, and filters from live API data.

### 14.6 Optimistic writes swallow errors

Every `DashboardRepository.save*/delete*` returns `Result.success` regardless of the HTTP result, so
the UI shows success for failed writes.

**Fix:** propagate the `Result` to the ViewModel and expose `error: StateFlow<String?>`; roll back
the `StateFlow` on failure.

### 14.7 Other items

- Room DB is version 1 with `fallbackToDestructiveMigration()` — any schema change wipes the cache.
- No HTTP error taxonomy: failures surface as `Exception("Supabase error: 403 …")`. Map
  PostgREST codes as `backend/API.md` §13 does.
- No retry/backoff, no connectivity observer, no `Cache-Control`.
- `Retrofit` + `Moshi` are already on the classpath and unused — migrating off hand-rolled
  `org.json` would remove ~400 lines of boilerplate and give typed responses.
- No Supabase Realtime; new articles only appear after a manual pull-to-refresh.

---

## 15. Implementation recipes

### 15.1 Session manager and auth header

```kotlin
// data/remote/DashboardSession.kt
data class DashboardSession(
    val token: String,
    val expiresAtMillis: Long,
    val user: UserProfile,
    val permissions: List<String>
) {
    val isExpired get() = System.currentTimeMillis() >= expiresAtMillis
}

// data/remote/SessionManager.kt  (construct in NinghsingCheApp.onCreate)
class SessionManager(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        "nc_dashboard_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var session: DashboardSession?
        get() = prefs.getString("session", null)?.let { Json.decodeFromString(it) }
        private set(value) = prefs.edit {
            if (value == null) remove("session") else putString("session", Json.encodeToString(value))
        }

    fun hasPermission(menu: String) = session?.permissions?.contains(menu) == true
}
```

```kotlin
// patch SupabaseClient.createBaseRequestBuilder()
private fun createBaseRequestBuilder(url: String): Request.Builder {
    val key = SupabaseConfig.supabaseKey
    val builder = Request.Builder()
        .url(url)
        .addHeader("apikey", key)
        .addHeader("Content-Type", "application/json")
        .addHeader("Prefer", "return=representation")

    val dashboardToken = sessionManager?.session?.takeUnless { it.isExpired }?.token
    val token = authToken          // Supabase Auth JWT, if you keep it
    builder.addHeader("Authorization", "Bearer ${token ?: key}")
    if (dashboardToken != null) builder.addHeader("x-dashboard-session", dashboardToken)
    return builder
}
```

### 15.2 Login against the real RPC

```kotlin
suspend fun dashboardLogin(username: String, password: String, remember: Boolean): Result<UserProfile> =
    withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("p_username", username.trim())
            put("p_password", password)
            put("p_remember", remember)
            put("p_user_agent", "NingshingCheAndroid/${BuildConfig.VERSION_NAME}")
        }
        val request = createBaseRequestBuilder("${SupabaseConfig.restBaseUrl}/rpc/dashboard_login")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        val body = httpClient.newCall(request).execute().use { it.body?.string().orEmpty() }
        val result = JSONArray(body).optJSONObject(0)
            ?: return@withContext Result.failure(Exception(body))
        if (!result.optBoolean("ok")) {
            return@withContext Result.failure(Exception(result.optString("error", "Login failed.")))
        }
        val session = DashboardSession(
            token = result.getString("token"),
            expiresAtMillis = Instant.parse(result.getString("expires_at")).toEpochMilli(),
            user = UserProfile.fromRpcUser(result.getJSONObject("user")),
            permissions = result.getJSONObject("user")
                .optJSONArray("permissions")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } }.orEmpty()
        )
        sessionManager.save(session)
        Result.success(session.user)
    }
```

Validate on cold start with `POST /rest/v1/rpc/dashboard_session` (no body) and log out with
`POST /rest/v1/rpc/dashboard_logout`. Full request/response shapes: `backend/API.md` §7.1–7.4.

### 15.3 Exact counts via `Content-Range`

```kotlin
suspend fun countBlogs(status: String?): Int = withContext(Dispatchers.IO) {
    val q = buildString {
        append("${SupabaseConfig.restBaseUrl}/blogs?select=id&limit=1")
        if (!status.isNullOrBlank()) append("&status=eq.$status")
    }
    val request = createBaseRequestBuilder(q)
        .addHeader("Prefer", "count=exact")
        .addHeader("Range", "0-0")
        .build()
    val response = httpClient.newCall(request).execute()
    val range = response.header("Content-Range") ?: return@withContext 0
    Regex("/(\\d+)$").find(range)?.groupValues?.get(1)?.toIntOrNull() ?: 0
}
```

### 15.4 Transactional submission approval

```kotlin
suspend fun approveSubmission(submissionId: String, categoryId: String, status: String = "Draft") =
    withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("p_submission_id", submissionId)
            put("p_category_id", categoryId)
            put("p_status", status)
            put("p_slug", JSONObject.NULL)
        }
        val request = createBaseRequestBuilder("${SupabaseConfig.restBaseUrl}/rpc/approve_submission")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        val body = httpClient.newCall(request).execute().use { it.body?.string().orEmpty() }
        runCatching { BlogRecord.fromJson(JSONArray(body).getJSONObject(0)) }
    }
```

Replace the local `DashboardRepository.approveAndPublishSubmission()` with this; it creates the
missing author, de-duplicates the slug, and sets `converted_blog_id` in one transaction.

### 15.5 PDF upload to Storage

```kotlin
suspend fun uploadPdf(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
    val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
    val path = "${Calendar.getInstance().get(Calendar.YEAR)}/${UUID.randomUUID()}.pdf"
    val url = "${SupabaseConfig.supabaseUrl}/storage/v1/object/pdf-books/$path"
    val request = createBaseRequestBuilder(url)
        .addHeader("Content-Type", "application/pdf")
        .addHeader("x-upsert", "false")
        .post(bytes.toRequestBody("application/pdf".toMediaType()))
        .build()
    httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
        Result.success("${SupabaseConfig.supabaseUrl}/storage/v1/object/public/pdf-books/$path")
    }
}
```

Store the returned **path** in `pdf_books.file_storage_path` / `blogs.pdf_storage_path` and set
`file_provider` / `pdf_file_provider` to `"supabase-storage"`, otherwise the file becomes
undeletable. Requires the `books` or `blogs` menu permission.

---

## 16. Definition of done

Ordered task list for making every screen functional against the API:

1. **Auth** — `SessionManager` + `x-dashboard-session` header + `dashboard_login` /
   `dashboard_session` / `dashboard_logout`; encrypted token storage; delete the hard-coded
   `admin123` path and plaintext password storage.
2. **Permissions** — drive `DashboardSection` and drawer visibility from `user.permissions`
   (`dashboard, authors, blogs, categories, comments, galleries, books, submissions, videos,
   analytics, settings, access-control`); replace the local `UserRole` enum.
3. **Error handling** — map PostgREST codes to Bengali/English messages; surface failures from
   `DashboardRepository` to the UI; roll back optimistic updates.
4. **Pagination + counts** — `Content-Range` totals everywhere; Paging 3 for `blogs`/`comments`;
   fix `DashboardSummaryStats`.
5. **Model parity** — add the missing columns listed in [§14.4](#144-missing-columns); stop
   hard-coding `year = 2026`.
6. **Live navigation data** — drawer categories and years from the API; seed data demoted to an
   offline fallback.
7. **Submission workflow** — call `approve_submission`; render `reviewed_at` / `converted_blog_id`.
8. **Media** — PDF upload to Supabase Storage from `PdfBooksManagementView` and blog PDF fields;
   preserve `imgbb_delete_url` and `inline_media`.
9. **Refresh UX** — pull-to-refresh on Home/Explore/Dashboard, retry with backoff, offline banner
   driven by `syncState`.
10. **Optional hardening** — migrate `SupabaseClient` to Retrofit + Moshi (already on the
    classpath), add a Room migration instead of destructive migration, add Realtime for new
    articles.

---

## 17. The rebuilt public reader (current implementation)

The reader UI was rewritten against the live API. It lives in three new packages
and replaces the old reader navigation in `MainActivity`:

```
data/portal/     PortalConfig · PortalApi (Retrofit) · PortalDtos (Moshi)
                 PortalModels · PortalRepository · PortalProvider
ui/editorial/    EditorialTheme (tokens, type scale, spacing) · EditorialComponents
ui/reader/       HomeScreen · ArticleScreen · ListScreens (search/category/author)
                 ReaderViewModels · ReaderNavHost
```

| Concern | Where | Notes |
| --- | --- | --- |
| Transport | `PortalConfig.okHttpClient()` | TLS 1.2+, modern ciphers only, no cleartext (`res/xml/network_security_config.xml`), logging redacted and debug-only |
| Auth | none | Public reader uses only the publishable anon key. See [§17.1](#171-why-there-is-no-bearer-token) |
| Paging | `Page<T>` + `Content-Range` | Exact totals drive "load more" and the search result counter |
| Caching | `PortalRepository` | TTL in-memory cache for categories/authors/PDFs/videos/settings; last-good-wins on failure |
| Errors | `PortalError` | Bengali messages; `SchemaMissing` maps `PGRST205`/`PGRST204` to "run the migrations" |
| Deep links | `ReaderNavHost` | `ningshingche.com/article/{slug}` and `ningshingche.com/{id}`; Bengali slugs are percent-encoded |

### 17.1 Why there is no bearer token

`apikey` + `Authorization: Bearer <publishable key>` is *required* by PostgREST for
every request, including anonymous ones — but it is **not** a secret and **not** a
credential. It selects the `anon` Postgres role, whose powers are entirely defined
by the RLS policies in `schema.sql`: read published blogs, read reference tables,
insert comments as `Unpublish`. Adding a bearer token or a login to a public reader
would add attack surface without adding protection.

What was hardened instead:

1. **TLS-only transport** — `network_security_config.xml` sets
   `cleartextTrafficPermitted="false"` globally and again for the Supabase,
   ningshingche.com and ImgBB hosts, so no code path can accidentally use HTTP.
2. **Modern TLS only** — `ConnectionSpec.MODERN_TLS` restricted to TLS 1.2/1.3.
3. **Keys injected at build time** — `BuildConfig.SUPABASE_PUBLISHABLE_KEY` from
   `.env` via the secrets plugin; staging and production can differ.
4. **Header redaction** in the debug logging interceptor.
5. **No `service_role` key, ever.** The dashboard-session header
   (`x-dashboard-session`) stays out of the reader entirely — it belongs to the CMS.

Certificate pinning was deliberately **not** added: Supabase serves projects from
behind a managed edge whose leaf certificates rotate, so a pin would brick every
installed app at the next rotation.

### 17.2 Screens still to build

Gallery viewer, PDF archive + viewer, bookmarks, reading history, settings, and the
Room-backed offline cache are the next increment. The components in
`ui/editorial/EditorialComponents.kt` and the paging contract in
`ui/reader/ReaderViewModels.kt` are already shared, so those screens are mostly
layout.

---

*Generated from `app/` on branch `main`. Server contract: [`backend/API.md`](./backend/API.md).
Database schema: `backend/supabase/schema.sql` and
`backend/supabase/migrations/004_dashboard_access_control.sql`.*
