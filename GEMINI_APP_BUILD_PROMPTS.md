# Gemini prompt pack — Ningshing Che reader app

Copy-paste prompts for building the **public reader app** in Kotlin, phased so you can
verify each stage before continuing.

**How to use this file**

1. Work in one chat per phase. Start a fresh chat for each phase and paste that phase's
   prompt — context windows degrade, and a fresh chat forces Gemini to re-read the repo
   instead of trusting a stale summary.
2. Every prompt is self-contained: it re-states the repo, the API and the constraints, so
   it works in a brand-new chat.
3. Gemini cannot compile or run the app. Each phase therefore ends by asking it to state
   which files it changed and to flag any API it is not certain exists. **You build.**
   Paste build errors back into the chat and continue.
4. Nothing here asks for the CMS/dashboard, login, or any write endpoint. Scope is the
   anonymous public reader.

---

## Shared facts (pour-les into any prompt if you trim one down)

### Repo

- `https://github.com/surajit-singha-sisir/NingshingCheApp`, branch `main`
- `ANDROID_API.md` at the repo root is the API/UI spec — **read it before writing code**
- `backend/API.md` describes the dashboard side; useful for background, out of scope

### Live API (anonymous read — no login anywhere in the reader)

| | |
|---|---|
| Base | `https://slcpvmpsynkqdozvlsii.supabase.co/rest/v1` |
| Auth | the Supabase **publishable** key, sent twice: `apikey: <key>` and `Authorization: Bearer <key>` |
| Key | already in `app/.../data/portal/PortalConfig.kt` as `FALLBACK_KEY` |

Row counts as of the last check: `blogs` 50 · `categories` 15 · `authors` 27 ·
`galleries` 78 · `pdf_books` 6 · `videos` 9 · `settings` 1 · `comments` 30 ·
`submitted_blogs` 0.

The publishable key is the correct and sufficient credential — RLS grants the `anon` role
read access to published content. **Never put the `service_role` key in the app.**

### PostgREST gotchas (each of these has already cost a debugging cycle — do not re-learn them)

1. **There is no implicit `eq`.** `?id=site_settings` returns HTTP 400
   `PGRST100 unexpected "s" expecting "not" or operator`. It must be `?id=eq.site_settings`.
2. **`encoded = true` is not enough on its own.** It only stops double-encoding; you still
   need the `eq.` prefix. `?slug=<percent-encoded-bengali>` alone is a 400.
   Correct: `?slug=eq.<percent-encoded-bengali>`.
3. Slugs and category slugs are **Bengali**. Every route, permalink and filter value must
   be percent-encoded (`URLEncoder.encode(v, "UTF-8").replace("+", "%20")`).
4. `or=` for id-or-slug lookups: `?or=(id.eq.X,slug.eq.Y)`.
5. Exact counts: send `Prefer: count=exact` and read `Content-Range`, don't `select(*)` and
   count the rows.

Verify any query you plan to ship with curl first:

```bash
U="https://slcpvmpsynkqdozvlsii.supabase.co/rest/v1"
K="<publishable key>"
curl -s "$U/settings?id=eq.site_settings" -H "apikey: $K" -H "Authorization: Bearer $K"
```

### Build constraints (the project is on these versions — target them, don't upgrade)

| Constraint | Consequence |
|---|---|
| `minSdk 24`, **no** coreLibraryDesugaring | **No `java.time.*`** — use `SimpleDateFormat`, or `NoClassDefFoundError` on Android 7 |
| Moshi via `KotlinJsonAdapterFactory`, KSP codegen **not** applied | Don't assume generated adapters exist |
| Compose BOM `2024.09.00` (= Compose 1.7.0) | Pager/`onRelease`/`boundsInRoot` are available; APIs added after 1.7 are not |
| Coil `2.7.0` | `AsyncImage`, `rememberAsyncImagePainter`, `SubcomposeAsyncImage` |
| Cleartext disabled globally (`res/xml/network_security_config.xml`) | HTTPS only |
| No certificate pinning, deliberately | Supabase edge certs rotate; a pin would brick installed apps |
| `enableEdgeToEdge()` in `MainActivity` | The app must handle its own insets; `adjustResize` is ignored from API 30 — use `WindowInsets.ime` |

### Design language — "modern editorial"

| Token | Value |
|---|---|
| paper (background) | `#FDFBF7` |
| ink (text) | `#1A1512` |
| maroon (accent) | `#7A2E1E` |
| saffron (secondary accent) | `#D97706` |
| rule (hairlines) | `#E3DACD` |
| sunken (recessed surfaces) | `#F6F1E8` |

Defined in `app/.../ui/editorial/EditorialTheme.kt` as `LocalEditorialTokens` +
`EditorialSpace` / `EditorialShape` / `EditorialType`.

`Kalpurush` exists in `com.example.ui.theme` but is now `FontFamily.Serif` — the TTF was
removed to cut binary size. Keep using the `Kalpurush` symbol; don't reintroduce a font file.

### Content notes that change UI decisions

- `settings` row: `site_title` "Ningshing Che", but logo/favicon/contact/social columns are
  **empty strings**. Every consumer must fall back to the values published on
  ningshingche.com. All four feature toggles are `true`.
- `blogs.content` is **raw HTML** — render it, don't strip it to plain text.
- `published_date` spans 2025-06-17 → 2025-08-11 only. Don't hard-code a year list.
- `pdf_books`: all `file_provider="url"`, `link` points at GitHub Pages → open externally
  via `ACTION_VIEW`, don't try to render in-app. Sizes 2.68–54.01 MB, 8–119 pages.
- `videos`: 4 YouTube + 5 Facebook. `thumbnail_url` is populated **for YouTube only** —
  derive Facebook thumbnails or use a placeholder: `https://i.ytimg.com/vi/<id>/hqdefault.jpg`.
- `categories.icon_name` values are Font Awesome names (landmark, language, feather,
  address-card, hands-praying, magnifying-glass, dragon, microchip, circle-info, shapes,
  people-group, pen-nib, masks-theater, book-open, clock-rotate-left).
- Anonymous comment insert is permitted, but RLS forces status `Unpublish` — the UI must
  say "pending moderation" rather than pretending the comment is live.
- Deep links to accept: `https://ningshingche.com/article/{slug}` (and `http://`), plus
  `https://ningshingche.com/{id}`.

---

## Phase 0 — Orientation (no code)

> **Goal:** make sure the model has actually read the repo before it starts typing.

**Paste:**

```
You are working on the Android app at
https://github.com/surajit-singha-sisir/NingshingCheApp (branch main).

Read these before anything else:
- ANDROID_API.md (repo root) — the API and UI spec
- app/src/main/java/com/example/ui/reader/  (ReaderNavHost.kt, HomeScreen.kt,
  ArticleScreen.kt, ListScreens.kt, ReaderViewModels.kt)
- app/src/main/java/com/example/data/portal/  (PortalApi, PortalRepository, PortalModels)
- app/src/main/java/com/example/ui/editorial/  (EditorialTheme, EditorialComponents,
  LazyImage, VideoPlayer, SiteFooter)

Do not write or modify any code in this phase. Report back:

1. What already exists and works, screen by screen.
2. What is stubbed, empty, or clearly unfinished.
3. Anything that looks like it will not compile, and why.
4. Anything that duplicates something else (orphaned or dead code is expected — the repo
   still carries an older dashboard implementation that the reader no longer reaches).

Be specific: file paths and function names. If you are unsure whether something works,
say so rather than guessing. Keep it under 600 words.
```

**Verify before moving on:** the report should mention the 18 routes in `ReaderNavHost`,
the portal data layer, and the editorial theme. If it invents files that don't exist, stop
and correct it — don't let it build on a hallucinated map.

---

## Phase 1 — Data layer: finish and harden

> **Goal:** every read path the reader needs exists, is Result-based, and is verified
> against the live API.

**Paste:**

```
Repo: https://github.com/surajit-singha-sisir/NingshingCheApp (branch main).
Read ANDROID_API.md and app/src/main/java/com/example/data/portal/ first.

Build out the reader's data layer. Scope: READ ONLY — the anonymous public reader. Do not
add login, the dashboard, or any write/insert path other than the existing public comment
insert.

Work through this list:

1. Audit PortalApi / PortalRepository against ANDROID_API.md section 13 (API coverage
   matrix). List every endpoint the reader needs that is missing, then implement it.
2. Every repository call must return a Result<T> and must never throw out of a coroutine.
   Repository `init {}` blocks and any `scope.launch` must be guarded — this app previously
   crashed on cold start because an unguarded init coroutine let an exception reach the
   process handler.
3. Add offline-first behaviour: read from the Room cache first, refresh from the network,
   and expose an "offline" notice the UI can surface as a snackbar.
4. Add pagination or at least a sane limit/order on every list query. No unbounded selects.
5. Verify every query shape against the live API before shipping it. Base URL
   https://slcpvmpsynkqdozvlsii.supabase.co/rest/v1 with headers `apikey: <key>` and
   `Authorization: Bearer <key>`. Remember: PostgREST has NO implicit `eq` —
   `?id=site_settings` is a 400, `?id=eq.site_settings` is a 200. Bengali values must be
   percent-encoded AND carry the `eq.` prefix.

Constraints: minSdk 24 with no desugaring, so no java.time — use SimpleDateFormat. Moshi
uses KotlinJsonAdapterFactory (reflection); KSP codegen is not enabled. Do not upgrade any
dependency version. Do not add certificate pinning. Never reference the service_role key.

When you are done, list every file you changed, and separately list any Compose, Coil or
Android API you used that you are not certain exists in Compose 1.7.0 / Coil 2.7.0.
```

**Verify:** build it, run it, confirm the home feed loads on a cold start **and** with the
network off, and that it does not crash when the app is killed mid-load.

---

## Phase 2 — Theme and shared components

> **Goal:** one canonical set of UI primitives so screens stop re-inventing them.

**Paste:**

```
Repo: https://github.com/surajit-singha-sisir/NingshingCheApp (branch main).
Read app/src/main/java/com/example/ui/editorial/EditorialTheme.kt and
EditorialComponents.kt first.

Consolidate the reader's shared UI primitives. Design language is "modern editorial":
paper #FDFBF7, ink #1A1512, maroon #7A2E1E, saffron #D97706, rule #E3DACD,
sunken #F6F1E8. Tokens live in LocalEditorialTokens with EditorialSpace / EditorialShape /
EditorialType alongside them.

Deliverables:

1. Every image in the app goes through LazyImage (already exists in this package). It does
   two things: it does not start a network fetch until the composable is near the viewport,
   and it shows a shimmer skeleton over the reserved box until the bitmap is fully loaded.
   Find any remaining raw AsyncImage / SubcomposeAsyncImage usage in the reader and route it
   through LazyImage. Exception: images that need ContentScale.FillWidth with an intrinsic
   height may keep SubcomposeAsyncImage, but must use the shimmer skeleton, not a spinner.
2. One canonical loading skeleton, empty state and error state. Screens should not hand-roll
   these.
3. A consistent section header with an optional "see all" action.
4. Confirm the theme reads correctly in both light and dark, and that nothing hard-codes a
   colour that should come from the tokens.

Constraints: Compose 1.7.0, no dependency upgrades. Keep the Bengali font as the existing
Kalpurush symbol (it is FontFamily.Serif — do not add a font file).

Do not change screen-level layout in this phase. Only extract and unify primitives, then
list every file you touched and every call site you re-pointed.
```

**Verify:** scroll the home feed on a throttled connection — every image should show a
skeleton then fade in, with no layout jump and no spinner.

---

## Phase 3 — Home

> **Goal:** the home feed is the shop window. Make it complete and correct.

**Paste:**

```
Repo: https://github.com/surajit-singha-sisir/NingshingCheApp (branch main).
Read app/src/main/java/com/example/ui/reader/HomeScreen.kt and ANDROID_API.md first.

Finish the home feed. It is a LazyColumn and already has: hero carousel, category strip,
featured rail, special articles, photo gallery, books/PDFs, video rail, authors rail,
latest feed, footer.

Fix and complete:

1. Each section must honour its settings toggle (hero_slider_enabled,
   featured_articles_enabled, special_articles_enabled) and disappear when its data is
   empty. No empty headers.
2. Hero = blogs where is_slider; Featured = is_feature; Special = is_special_article.
   There are 3 slider, 8 featured and 5 special items right now — the filters must actually
   use these flags rather than just taking the first N rows.
3. Video rail opens the in-app player (VideoPlayer.kt already exists: YouTube iframe embed,
   Facebook video plugin, in a WebView). Do not open an external browser on tap.
4. Photo gallery opens the swipeable pager viewer (already implemented with
   HorizontalPager) at the tapped index, paging through the whole gallery — not a
   single-image dialog.
5. Footer is EditorialFooter (already implemented, content mirrors ningshingche.com).
6. Pull-to-refresh must work and must surface an offline notice when it fails.

Constraints: Compose 1.7.0, Coil 2.7.0. All images via LazyImage. No dependency upgrades.
Section ordering should follow the website's priorities.

List every file you changed. Flag anything you could not verify against live data.
```

**Verify:** counts on screen should match the API (3 hero, 8 featured, 5 special), videos
play in-app, gallery swipes.

---

## Phase 4 — Article reader

> **Goal:** the article screen is where readers spend their time.

**Paste:**

```
Repo: https://github.com/surajit-singha-sisir/NingshingCheApp (branch main).
Read app/src/main/java/com/example/ui/reader/ArticleScreen.kt,
HtmlArticleRenderer.kt and ArticleTtsPlayer.kt first.

Finish the article screen.

1. Body content is raw HTML from the blogs table. HtmlArticleRenderer.kt already renders it
   in Compose. Complete it: headings, blockquotes, lists, links (LinkAnnotation, not the
   deprecated ClickableText), inline images with the shimmer skeleton, captions.
2. Inline images should be tappable to open full-screen with pinch-zoom.
3. Comments: show approved comments. Anonymous posting is allowed but RLS forces status
   Unpublish — after posting, tell the user it is pending moderation. Never show a
   newly-posted comment as if it were live. Respect the allow_comments setting.
4. Related articles, author byline, category, reading time, published date.
   Dates use SimpleDateFormat (minSdk 24, no java.time).
5. TTS playback (ArticleTtsPlayer.kt) must keep working and must stop when leaving the
   screen or when the app is backgrounded.
6. Share, bookmark and reading-history actions must persist.

Constraints: Compose 1.7.0, no java.time, no dependency upgrades. Images via LazyImage or
SubcomposeAsyncImage-with-skeleton.

List every file you changed and every API you are unsure exists.
```

**Verify:** open several articles including the longest one; check Bengali shaping, inline
image loading, comment posting, and that audio stops when you back out.

---

## Phase 5 — Secondary screens

> **Goal:** every route in ReaderNavHost leads somewhere real.

**Paste:**

```
Repo: https://github.com/surajit-singha-sisir/NingshingCheApp (branch main).
Read app/src/main/java/com/example/ui/reader/ReaderNavHost.kt and ListScreens.kt first.

There are 18 routes. Make sure each one leads to a real, finished screen. Handle these
specifically:

- Category and Author screens: take a Bengali slug / id, so percent-encoding matters.
  Paginate; sort sensibly; show an empty state rather than a blank page.
- Search: query across title and content, debounced, with recent searches and a clear
  empty/results state.
- Bookmarks and History: persisted, with an empty state, and removable entries.
- PDF archive and PDF viewer: pdf_books rows all have file_provider="url" pointing at
  GitHub Pages. Show cover, title, pages, size, then open externally via ACTION_VIEW.
  Files run 2.68–54.01 MB — warn before downloading on metered connections.
- Explore, Featured, AuthorsDirectory, SocialActivities: these are the thinnest. Give each
  real data and a real layout, or remove the route and its drawer entry if it genuinely has
  no data source.
- About: static content about the publication — match the tone of ningshingche.com.
- Settings: theme (system/light/dark), reading preferences, clear cache.

Rules: no placeholder "TODO" screens. If a screen has no data source, say so and propose
removing it rather than faking it with seed data — this app previously shipped seed data
masquerading as real content and it must not happen again.

Constraints: Compose 1.7.0, no dependency upgrades, images via LazyImage.

List every route and what you did with it.
```

**Verify:** tap every drawer item and every in-app link. No route should dead-end.

---

## Phase 6 — Polish, resilience, release

> **Goal:** make it shippable.

**Paste:**

```
Repo: https://github.com/surajit-singha-sisir/NingshingCheApp (branch main).

Final pass before release. Work through:

1. Crash-safety: no unguarded coroutine anywhere. Every repository init, every scope.launch,
   every LaunchedEffect that can fail must handle its own exceptions. The app previously
   died on cold start from exactly this — treat it as a regression test.
2. Lifecycle: media (TTS, WebView video) must stop when the screen or app goes to the
   background. WebViews must be destroyed on release.
3. Keyboard: any screen with a text input must handle WindowInsets.ime. The app is
   edge-to-edge (enableEdgeToEdge), so adjustResize does not apply from API 30 — the input
   must move above the keyboard itself.
4. Offline: every screen degrades gracefully with cached data and a visible offline notice.
   No stack traces, no infinite spinners.
5. Performance: verify images are not fetched until they are near the viewport, lists are
   keyed and paginated, and scrolling the home feed is smooth on a low-end device.
6. Accessibility: content descriptions on images and icon buttons, touch targets >= 48dp,
   text scales with system font size.
7. Privacy/security: HTTPS only (already enforced by network_security_config.xml), no
   service_role key anywhere in the source, no credentials logged. Confirm the Supabase
   publishable key is read from BuildConfig/local.properties rather than hard-coded.
8. Dead code: the repo still carries an older dashboard implementation and legacy files the
   reader no longer reaches. List what is genuinely unreachable so it can be deleted in a
   separate commit — do not delete it in this pass.

Constraints: minSdk 24, no java.time, Compose 1.7.0, no dependency upgrades.

Finish with a release checklist: what you changed, what you could not verify without a
device, and anything you would want a human to test by hand.
```

**Verify:** this is the phase to actually test on a real low-end device, offline, and with
a large system font.

---

## Rules to keep in every chat

Add this paragraph to any prompt you trim down:

```
Constraints that are non-negotiable: minSdk 24 with no core library desugaring (so no
java.time — use SimpleDateFormat); Moshi with KotlinJsonAdapterFactory (KSP codegen is not
enabled); Compose BOM 2024.09.00 / Compose 1.7.0; Coil 2.7.0; HTTPS only with no
certificate pinning; never reference the Supabase service_role key; do not upgrade any
dependency version without asking first. PostgREST has no implicit `eq` operator — always
write `column=eq.value`, and percent-encode Bengali values.

You cannot compile or run this project. Do not claim code works. When you finish, list the
files you changed and separately list any API you used whose existence you are not certain
of, so a human can check those first.
```
