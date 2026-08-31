# Ningshing Che — Dashboard API Documentation

Reference for every network endpoint, database table, RPC function, and client helper used by the
editorial dashboard in `backend/`. Everything here was read from the shipped source
(`assets/js/*.js`) and the SQL in `supabase/`, not from assumptions.

- **Dashboard version:** 1.3.0 (`assets/js/config.js`)
- **Backend:** Supabase (PostgREST + Storage), ImgBB for raster images
- **Client:** no-build static app; all calls are made from the browser with a publishable key
- **Companion documents:** [`README.md`](./README.md) (setup, migrations, feature tour) ·
  [`ANDROID_API.md`](../ANDROID_API.md) (Kotlin/Compose UI foundation and how the Android app
  consumes these endpoints)

---

## Table of contents

1. [Architecture at a glance](#1-architecture-at-a-glance)
2. [Configuration](#2-configuration)
3. [Authentication and sessions](#3-authentication-and-sessions)
4. [REST conventions](#4-rest-conventions)
5. [Data model](#5-data-model)
6. [Resource endpoints (CRUD)](#6-resource-endpoints-crud)
7. [RPC reference](#7-rpc-reference)
8. [Storage API (PDF)](#8-storage-api-pdf)
9. [ImgBB image API](#9-imgbb-image-api)
10. [Global search](#10-global-search)
11. [Schema / health probe](#11-schema--health-probe)
12. [Import pipeline](#12-import-pipeline)
13. [Error reference](#13-error-reference)
14. [JavaScript client reference](#14-javascript-client-reference)
15. [Security model and hardening](#15-security-model-and-hardening)
16. [cURL cheat sheet](#16-curl-cheat-sheet)

---

## 1. Architecture at a glance

```
Browser (backend/index.html)
│
├── NC.api        → Supabase PostgREST   /rest/v1/<table>          CRUD on 9 content tables
│                 → Supabase PostgREST   /rest/v1/rpc/<fn>         10 RPC functions
│                 → Supabase Storage     /storage/v1/object/...    PDF upload/delete (bucket: pdf-books)
│
├── NC.media      → ImgBB                /1/upload                 Image hosting (hero, avatar, gallery)
│
└── NC.auth       → session in sessionStorage / localStorage, validated by dashboard_session()
```

All traffic is HTTPS `fetch` (or `XMLHttpRequest` where upload progress is needed). There is no
custom server: **authorization is enforced entirely by PostgreSQL RLS policies and by
`SECURITY DEFINER` RPC functions** created in `supabase/migrations/004_dashboard_access_control.sql`.

Module map:

| File | Responsibility |
| --- | --- |
| `config.js` | Endpoints, keys, table map, route/permission catalogue |
| `api.js` | REST/Storage transport, filters, pagination, RPC, error normalization |
| `auth.js` | Login, session persistence, expiry, permission checks |
| `crud.js` | Shared list state, save, cascading media deletion |
| `media.js` | ImgBB uploader, PDF uploader, video preview |
| `editor.js` | Quill rich-text abstraction |
| `importer.js` | CSV/XLSX templates, validation, preview, batch insert |
| `dashboard.js` | Metrics, Chart.js data, activity feed |
| `access-control.js` | Users & Roles UI (Super Admin RPCs) |
| `app.js` | Shell, hash routing, theme, global search wiring |

---

## 2. Configuration

Source of truth: `assets/js/config.js` → `window.NC_CONFIG`.

| Key | Value | Notes |
| --- | --- | --- |
| `supabase.url` | `https://slcpvmpsynkqdozvlsii.supabase.co` | Project REST host |
| `supabase.publishableKey` | `sb_publishable_…` | Browser-safe; **never** swap in `service_role` |
| `supabase.restPath` | `/rest/v1` | PostgREST base |
| `supabase.storagePath` | `/storage/v1` | Storage base |
| `supabase.pdfBucket` | `pdf-books` | Public bucket, PDF only |
| `supabase.pdfMaxBytes` | `33554432` (32 MB) | Enforced client-side and by bucket policy |
| `imgbb.endpoint` | `https://api.imgbb.com/1/upload` | Multipart `POST` |
| `imgbb.apiKey` | `576f6549…` | ⚠️ Secret, shipped client-side — see [§15](#15-security-model-and-hardening) |
| `imgbb.maxBytes` | `33554432` | Applies to every hero/avatar/gallery image |
| `app.requestTimeoutMs` | `20000` | `AbortController` timeout for REST calls |
| `app.sessionHours` | `8` | Standard session lifetime |
| `app.rememberedSessionDays` | `7` | "Remember me" lifetime |
| `app.defaultPageSize` | `10` | Table page size (10/20/30/50 selectable) |
| `app.locale` / `timeZone` | `en-BD` / `Asia/Dhaka` | Formatting only |

Derived base URLs (`api.js`):

```
REST_BASE    = https://slcpvmpsynkqdozvlsii.supabase.co/rest/v1
STORAGE_BASE = https://slcpvmpsynkqdozvlsii.supabase.co/storage/v1
```

> Changing project? Edit `config.js` only. Every module reads from `NC_CONFIG`.

---

## 3. Authentication and sessions

Migration 004 replaces the old static demo credential with database-backed accounts.

### 3.1 Request identity

Every REST/RPC/Storage request sends (`api.js → authHeaders()`):

| Header | Value |
| --- | --- |
| `apikey` | `NC_CONFIG.supabase.publishableKey` |
| `Authorization` | `Bearer <accessToken || publishableKey>` |
| `x-dashboard-session` | Raw 32-byte hex session token — **only** for `mode: 'supabase-rbac'` sessions |
| `x-dashboard-token` | SHA-256 demo digest — **only** for legacy `mode: 'legacy-demo'` sessions |

The server reads these headers through `public.dashboard_request_session_token()`.

### 3.2 Session object (browser)

Stored under the `nc:admin-session` key in `sessionStorage` (standard) or `localStorage`
(remember me). Shape:

```jsonc
{
  "version": 2,                     // 2 = supabase-rbac, 1 = legacy-demo
  "id": "uuid",                     // client-side only
  "mode": "supabase-rbac",
  "token": "64-hex-characters",     // raw token; only the SHA-256 hash is stored server-side
  "user": {
    "id": "uuid",
    "username": "admin",
    "name": "Chief Editor",
    "roleId": "uuid",
    "role": "Super Admin",
    "roleSlug": "super-admin",
    "permissions": ["dashboard", "blogs", "..."],
    "mustChangePassword": true,
    "isActive": true
  },
  "issuedAt": 1756000000000,
  "expiresAt": 1756028800000,
  "remember": false
}
```

Lifecycle helpers in `auth.js`: `restore()`, `login()`, `logout()`, `validateSession()`,
`isAuthenticated()`, `canAccess(route)`, `isSuperAdmin()`, `remainingMilliseconds()`, `isLegacy()`.

### 3.3 Login flow

1. `POST /rest/v1/rpc/dashboard_login` → returns `{ ok, token, expires_at, user }`.
2. If the RPC is missing (`PGRST202`), the client transparently falls back to the local
   demo credential so you cannot be locked out mid-migration (see [§7.1](#71-dashboard_login)).
3. Session written to storage; an expiry timer logs out automatically.
4. On app boot and on route changes, `dashboard_session()` re-validates and refreshes `expiresAt`.

### 3.4 Permissions and roles

Valid menu permissions (`dashboard_valid_permissions()`), also the route IDs:

```
dashboard  authors  blogs  categories  comments  galleries
books      submissions  videos  analytics  settings  access-control
```

Seeded roles:

| Role | Slug | Permissions | System |
| --- | --- | --- | --- |
| Super Admin | `super-admin` | All 12 (always recomputed server-side) | ✅ protected |
| Administrator | `administrator` | All except `access-control` | ✅ |
| Editor | `editor` | `dashboard, authors, blogs, categories, galleries, books, submissions, videos` | ✅ |
| Moderator | `moderator` | `dashboard, comments, submissions` | ✅ |
| Analyzer | `analyzer` | `dashboard, analytics` | ✅ |

Rules enforced in SQL and mirrored in the UI:

- `dashboard` is force-prepended to every custom role.
- `access-control` is stripped from every role payload — it is implicit for `super-admin` only.
- `super-admin` cannot be renamed, edited, or deleted; at least one active Super Admin must remain.
- Editing a role or a user's role/password/status **revokes that user's sessions immediately**.
- `must_change_password` opens a non-dismissible credential dialog until an 8+ character password is set.

---

## 4. REST conventions

### 4.1 Standard headers

```
Accept: application/json
apikey: <publishable key>
Authorization: Bearer <access token or publishable key>
x-dashboard-session: <raw session token>
Content-Type: application/json        (added automatically for JSON bodies)
Prefer: return=representation         (INSERT/PATCH/POST-upsert)
Prefer: count=exact                   (when a total count is requested)
Prefer: resolution=merge-duplicates,return=representation   (upsert)
Prefer: return=minimal                (DELETE)
```

`FormData`/`Blob` bodies skip `Content-Type` so the browser sets the multipart boundary.

### 4.2 Selecting

```
GET /rest/v1/blogs?select=id,title,status
```

The dashboard never requests article bodies for list views. Per-view `select` values:

| View | `select` |
| --- | --- |
| Dashboard / Analytics | `id,title,status,author_name,published_date,created_at` (blogs) and equivalents |
| Blogs | `*` |
| Authors | `*` |
| Categories | `*` (+ `id,category_id` from blogs for usage counts) |
| Comments | `*` (+ `id,title,status` from blogs) |
| Galleries | `*` |
| PDF Books | `*` |
| Submissions | `*` (+ authors, categories) |
| Videos | `*` |
| Settings | `*` |

### 4.3 Filtering

Filters are serialized as PostgREST query parameters. Pass a scalar for `eq`, or an object for
other operators (`api.js → filterExpression`):

```js
{ status: 'Publish' }                      // → status=eq.Publish
{ status: { op: 'neq', value: 'Draft' } }  // → status=neq.Draft
{ id:    { op: 'in',  value: [a, b] } }    // → id=in.(a,b)
{ deleted_at: { op: 'is', value: 'null' }} // → deleted_at=is.null
{ tags:  { op: 'cs',  value: ['x'] } }     // → tags=cs.{x}
```

`undefined`, `null`, and `''` filters are dropped. Any PostgREST operator (`gte`, `lte`, `like`,
`ilike`, `fts`, …) works by setting `op`.

### 4.4 Ordering, paging, counting

```
GET /rest/v1/blogs?select=*&order=created_at.desc&limit=20&offset=40
```

- `order` accepts any PostgREST expression, e.g. `book_published_date.desc.nullslast,created_at.desc`.
- Paging is `limit` + `offset` (no `Range` header for list reads).
- Exact totals come from the `Content-Range` response header (`*/N` or `0-19/N`) when
  `Prefer: count=exact` is sent. `NC.api.list()` parses this and returns `{ data, count }`.
- `NC.api.count()` uses `select=id&limit=1` + `Range: 0-0` + `Prefer: count=exact` for a
  header-only total.

Hard caps used by the UI: authors 1000, blogs 2000, categories 1000, comments 3000,
galleries 2000, books 2000, submissions 3000, videos 2000, importer context 5000.

### 4.5 Writes

| Operation | Method + path | Prefer | Returns |
| --- | --- | --- | --- |
| Create | `POST /rest/v1/<table>` | `return=representation` | Array → first element |
| Update | `PATCH /rest/v1/<table>?id=eq.<uuid>` | `return=representation` | Array → first element |
| Upsert | `POST /rest/v1/<table>?on_conflict=id` | `resolution=merge-duplicates,return=representation` | Array → first element |
| Delete | `DELETE /rest/v1/<table>?id=eq.<uuid>` | `return=minimal` | `true` |

`cleanPayload()` strips `undefined` values before serialization, so partial `PATCH` bodies are safe.

---

## 5. Data model

Nine content tables (`settings` uses a fixed text primary key; the rest use UUIDs). All tables have
`created_at`/`updated_at` (`timestamptz`, UTC) maintained by `public.set_updated_at()`.

### `authors`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | uuid PK | |
| `title` | text | Required, non-blank — author display name |
| `image` | text | ImgBB/direct image URL |
| `imgbb_delete_url` | text | Manual deletion page |
| `image_meta` | jsonb | `{ display_url, filename, size, mime, provider, uploaded_at }` |
| `designation` | text | |
| `description` | text | Rich-text biography |
| `is_verified` | boolean | Verified badge |
| `location` | text | |

### `categories`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | uuid PK | |
| `title` | text | Required |
| `sub_title` | text | |
| `slug` | text | Required; unique index on `lower(slug)` |
| `icon_name` | text | Font Awesome name without the `fa-` prefix, default `layer-group` |

### `blogs`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | uuid PK | |
| `title`, `sub_title` | text | `title` required |
| `image`, `imgbb_delete_url`, `image_meta` | text / jsonb | Hero image |
| `inline_media` | jsonb | `[]` — images inserted via the Quill editor |
| `content` | text | Sanitized HTML |
| `category_id` | uuid FK → `categories(id)` | `on delete set null` |
| `author_id` | uuid FK → `authors(id)` | `on delete set null` |
| `status` | text | `Draft` \| `Publish`, default `Draft` |
| `tags` | text[] | GIN indexed |
| `seo_title`, `seo_description` | text | |
| `video_link`, `pdf_book_link` | text | |
| `pdf_file_provider` | text | `url` \| `supabase-storage`, default `url` |
| `pdf_storage_path` | text | Set when uploaded to Storage |
| `pdf_file_size_mb` | numeric(8,2) | |
| `slug` | text | Unique on `lower(slug)` |
| `is_slider`, `is_feature`, `is_special_article` | boolean | |
| `views_count` | bigint | |
| `reading_time_minutes` | integer | Auto-computed, ≥ 1 |
| `published_date` | date | Auto-set when status becomes `Publish` |
| `category_title`, `category_slug`, `author_name`, `author_image` | text | Trigger-maintained snapshots for the Android client |

**Trigger `blogs_sync_relationships`** (before insert/update of `author_id`, `category_id`,
`status`, `content`) refreshes the four snapshot columns, stamps `published_date`, and recomputes
reading time as `ceil(words / 220)`.

### `comments`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | uuid PK | |
| `blog_id` | uuid FK → `blogs(id)` | `on delete cascade` |
| `blog_title` | text | Trigger-maintained snapshot |
| `name` | text | Required |
| `address`, `phone`, `email` | text | |
| `content` | text | Required |
| `status` | text | `Publish` \| `Unpublish`, default `Unpublish` |

### `galleries`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | uuid PK | |
| `title` | text | Required |
| `description` | text | |
| `image`, `imgbb_delete_url`, `image_meta` | text / jsonb | |
| `category` | text | Free-text grouping |

### `pdf_books` (alias `books`)
| Column | Type | Notes |
| --- | --- | --- |
| `id` | uuid PK | |
| `title` | text | Required |
| `image`, `imgbb_delete_url`, `image_meta` | text / jsonb | Cover |
| `book_published_date` | date | |
| `link` | text | External PDF URL |
| `file_provider` | text | `url` \| `supabase-storage` |
| `file_storage_path` | text | |
| `author_or_editor`, `edition`, `category` | text | |
| `page_count` | integer | |
| `file_size_mb` | numeric(8,2) | |
| `description` | text | |

### `submitted_blogs` (alias `submissions`)
| Column | Type | Notes |
| --- | --- | --- |
| `id` | uuid PK | |
| `title` | text | Required |
| `designation`, `address`, `phone` | text | |
| `thumbnail`, `imgbb_delete_url`, `thumbnail_meta` | text / jsonb | |
| `writer_name` | text | Required |
| `writer_designation`, `writer_profile_image`, `writer_profile_delete_url`, `writer_profile_meta` | text / jsonb | |
| `writer_email`, `writer_facebook` | text | |
| `content_title` | text | |
| `content` | text | Required, sanitized HTML |
| `inline_media` | jsonb | `[]` |
| `status` | text | `Pending` \| `Reviewed` \| `Approved` \| `Rejected` \| `Published`, default `Pending` |
| `reviewed_at` | timestamptz | |
| `converted_blog_id` | uuid FK → `blogs(id)` | `on delete set null` |

### `videos`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | uuid PK | |
| `title` | text | Required |
| `video_link` | text | Required |
| `platform` | text | Auto-detected (YouTube, Facebook, Dailymotion, Vimeo, direct file, …) |
| `description` | text | |
| `thumbnail_url` | text | Derived for known platforms |

### `settings`
Single row, `id = 'site_settings'`.

| Column | Type | Default |
| --- | --- | --- |
| `id` | text PK | `site_settings` |
| `site_title` | text | `Ningshing Che` |
| `site_description` | text | `Bishnupriya Manipuri Magazine` |
| `logo_url`, `favicon_url` | text | `''` |
| `default_seo_title`, `default_seo_description` | text | `''` |
| `contact_email`, `contact_phone` | text | `''` |
| `facebook_url`, `youtube_url`, `instagram_url` | text | `''` |
| `hero_slider_enabled` | boolean | `true` |
| `featured_articles_enabled` | boolean | `true` |
| `special_articles_enabled` | boolean | `true` |
| `allow_comments` | boolean | `true` |
| `allow_user_submissions` | boolean | `true` |

### Access-control tables (never readable from the browser)

| Table | Columns |
| --- | --- |
| `dashboard_roles` | `id`, `name`, `slug`, `description`, `menu_permissions text[]`, `is_system`, timestamps |
| `dashboard_users` | `id`, `username`, `display_name`, `password_hash` (bcrypt bf-12), `role_id`, `is_active`, `must_change_password`, `failed_attempts`, `locked_until`, `last_login_at`, timestamps |
| `dashboard_sessions` | `id`, `user_id`, `token_hash` (SHA-256), `expires_at`, `last_seen_at`, `revoked_at`, `user_agent`, `created_at` |

All three have RLS enabled and `revoke all … from anon, authenticated` — they are reachable only
through the SECURITY DEFINER RPCs in [§7](#7-rpc-reference).

---

## 6. Resource endpoints (CRUD)

Table name mapping lives in `NC_CONFIG.tables`:

| Logical key | Table | Default order | UI page size |
| --- | --- | --- | --- |
| `authors` | `authors` | `created_at.desc` | 10 |
| `categories` | `categories` | `title.asc` | 10 |
| `blogs` | `blogs` | `created_at.desc` | 10 |
| `comments` | `comments` | `created_at.desc` | 10 |
| `galleries` | `galleries` | `created_at.desc` | 10 |
| `books` | `pdf_books` | `book_published_date.desc.nullslast,created_at.desc` | 10 |
| `submissions` | `submitted_blogs` | `created_at.desc` | 10 |
| `videos` | `videos` | `created_at.desc` | 10 |
| `settings` | `settings` | `id=eq.site_settings` | 1 |

Generic operations — substitute `<table>` from the mapping above:

```
GET    /rest/v1/<table>?select=*&order=…&limit=…&offset=…
POST   /rest/v1/<table>
PATCH  /rest/v1/<table>?id=eq.<uuid>
DELETE /rest/v1/<table>?id=eq.<uuid>
```

### 6.1 Public (anonymous, no dashboard session) allowances

Granted by `schema.sql` so the website and Android app can read content directly:

| Table | Public policy |
| --- | --- |
| `authors`, `categories`, `galleries`, `pdf_books`, `videos`, `settings` | `SELECT` — all rows |
| `blogs` | `SELECT` — only `status = 'Publish'` |
| `comments` | `SELECT` — only `status = 'Publish'`; `INSERT` allowed (visitor comments) |
| `submitted_blogs` | `INSERT` allowed (public article submission) |
| `storage.objects` (`pdf-books`) | `SELECT` — public read |

Write access to everything else requires a dashboard session whose role carries the matching menu
permission (migration 004 creates per-menu policies).

### 6.2 Resource-specific behaviour

**Blogs**
- Slug uniqueness is checked before save: `count('blogs', { slug, id: { op:'neq', value: id } }) > 0`
  (`NC.api.slugExists`).
- Deleting a blog offers to remove its hero image and every inline image from ImgBB using the
  stored `imgbb_delete_url` / `inline_media[].delete_url`.
- If `pdf_storage_path` is set, the stored PDF is deleted from Storage after the row is removed.

**Comments**
- Quick moderation is a single column patch: `PATCH /rest/v1/comments?id=eq.<id>` with
  `{ "status": "Publish" }` or `{ "status": "Unpublish" }`.

**Categories**
- Duplicate detection before insert: `count('categories', { title, slug })`; usage counts come from
  `GET /rest/v1/blogs?select=id,category_id&limit=5000`.

**Submissions → Blogs**
- Preferred path is the transactional RPC `approve_submission` ([§7.2](#72-approve_submission)).
- Fallback path (`NC.views.submissions` when the RPC is unavailable) does, in order:
  1. `POST /rest/v1/authors` (author de-duplicated by lower-cased name),
  2. `POST /rest/v1/blogs` with `status: 'Draft'`,
  3. `PATCH /rest/v1/submitted_blogs?id=eq.<id>` → `{ status: 'Approved', reviewed_at, converted_blog_id }`.
- Rejection: `PATCH /rest/v1/submitted_blogs?id=eq.<id>` → `{ status: 'Rejected', reviewed_at }`.

**Settings**
- Always an upsert on the fixed key: `POST /rest/v1/settings?on_conflict=id` with `id: 'site_settings'`.
- View-only preferences (`landing_page`, `items-per-page`, `table-density`, theme) are stored in
  `localStorage`, never in Supabase.

---

## 7. RPC reference

All RPCs are `POST /rest/v1/rpc/<name>` with a JSON object body. PostgREST returns a JSON array for
`Prefer: return=representation`; the client unwraps element 0.

### 7.1 `dashboard_login`

Authenticates a dashboard user and issues a session token.

**Body**

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `p_username` | text | ✅ | Case-insensitive, trimmed |
| `p_password` | text | ✅ | Compared with `crypt()` against the bcrypt hash |
| `p_remember` | boolean | ✅ | `true` → 7-day session, `false` → 8-hour |
| `p_user_agent` | text | ✅ | Truncated to 300 chars |

**Success**

```json
{
  "ok": true,
  "token": "6f2b…",                    // 64 hex chars; only the SHA-256 hash is persisted
  "expires_at": "2026-08-31T12:00:00+00:00",
  "user": { "id": "uuid", "username": "admin", "name": "Chief Editor",
            "role_id": "uuid", "role": "Super Admin", "role_slug": "super-admin",
            "permissions": ["dashboard", "…"], "must_change_password": true, "is_active": true }
}
```

**Failure** (HTTP 200 with `ok: false` — the client turns this into a thrown error)

| `error` | Condition |
| --- | --- |
| `The username or password is incorrect.` | Unknown username (a dummy bcrypt round is performed to reduce timing leaks) or wrong password |
| `Too many failed attempts. Try again after the temporary lock expires.` | 5 failures → `locked_until` set for 15 minutes; `locked_until` is also returned |
| `This dashboard account is disabled.` | `is_active = false` |

**Side effects:** expired/revoked sessions purged, `failed_attempts` reset to 0, `last_login_at` updated.

---

### 7.2 `approve_submission`

Converts a submitted article into a blog post transactionally (author de-duplication included).

**Body**

| Field | Type | Default | Notes |
| --- | --- | --- | --- |
| `p_submission_id` | uuid | — | Row in `submitted_blogs` |
| `p_category_id` | uuid | — | Target category |
| `p_status` | text | `'Draft'` | Passed straight to the new blog |
| `p_slug` | text | `null` | Generated from the title when null; uniqueness guaranteed |

**Returns:** the created `public.blogs` row.

**Requires:** the `submissions` menu permission. Raises `42501` otherwise.

---

### 7.3 `dashboard_session`

Validates the `x-dashboard-session` token and refreshes `last_seen_at`.

**Body:** none (`{}`).

**Returns**

```json
{ "ok": true, "expires_at": "2026-08-31T12:00:00+00:00", "user": { …user payload… } }
```

or `{ "ok": false, "error": "Session is invalid or expired." }`.

Client behaviour: on `NETWORK_ERROR`/`TIMEOUT` the session is **kept** (every protected request is
still checked server-side); on any other failure the session is ended.

---

### 7.4 `dashboard_logout`

Revokes the current session (`revoked_at = now()`).

**Body:** none. **Returns:** `true` if a row was revoked, otherwise `false`.
The client fires it and ignores the result so logout is instant even offline.

---

### 7.5 `dashboard_access_snapshot`

Super Admin only — everything the Users & Roles screen needs in one call.

**Body:** none.

**Returns**

```json
{
  "roles": [ { "id": "uuid", "name": "Editor", "slug": "editor", "description": "…",
               "permissions": ["dashboard", "…"], "is_system": true, "user_count": 3 } ],
  "users": [ { "id": "uuid", "username": "admin", "name": "Chief Editor", "role_id": "uuid",
               "role": "Super Admin", "role_slug": "super-admin", "is_active": true,
               "must_change_password": true, "last_login_at": "…", "created_at": "…" } ],
  "active_sessions": 2,
  "valid_permissions": ["dashboard", "authors", "…", "access-control"]
}
```

**Errors:** `42501 Super Admin authorization required.`

---

### 7.6 `dashboard_save_role`

Creates or updates a role. Super Admin only.

**Body**

| Field | Type | Notes |
| --- | --- | --- |
| `p_role_id` | uuid \| null | `null` creates a new custom role |
| `p_name` | text | 2–80 chars, unique (case-insensitive) |
| `p_description` | text | Trimmed |
| `p_menu_permissions` | text[] | Filtered against valid permissions; `access-control` stripped; `dashboard` force-added |

**Returns:** `dashboard_role_payload` — `{ id, name, slug, description, permissions, is_system, user_count }`.

**Errors**

| Code | Message |
| --- | --- |
| `42501` | `Super Admin authorization required.` |
| `22023` | `Role name must contain between 2 and 80 characters.` |
| `P0002` | `Role not found.` |
| `42501` | `The Super Admin role is protected and always has full access.` |
| `23505` | `A role with this name already exists.` |

**Side effect:** updating a role revokes all sessions of users assigned to it.

---

### 7.7 `dashboard_delete_role`

Super Admin only. **Body:** `{ "p_role_id": "uuid" }`. **Returns:** `true`.

| Code | Message |
| --- | --- |
| `42501` | `Super Admin authorization required.` / `System roles cannot be deleted.` |
| `P0002` | `Role not found.` |
| `23503` | `Move users to another role before deleting this role.` |

---

### 7.8 `dashboard_save_user`

Creates or updates a dashboard user. Super Admin only. Runs under the advisory lock
`ningshing-dashboard-super-admin-guard` and re-checks authorization after acquiring it.

**Body**

| Field | Type | Notes |
| --- | --- | --- |
| `p_user_id` | uuid \| null | `null` creates |
| `p_username` | text | Lower-cased/trimmed; 3–40 chars; no spaces, `/`, or `:` |
| `p_display_name` | text | 2–80 chars |
| `p_role_id` | uuid | Must exist |
| `p_password` | text | `''` = keep current; otherwise ≥ 8 chars (bcrypt bf-12) |
| `p_is_active` | boolean | Default `true` |

**Returns:** `dashboard_user_payload` — `{ id, username, name, role_id, role, role_slug, permissions, must_change_password, is_active }`.

**Errors**

| Code | Message |
| --- | --- |
| `42501` | `Super Admin authorization required.` / `Super Admin authorization expired while waiting.` |
| `42501` | `Use My login to update your own username or password.` (editing yourself) |
| `42501` | `At least one active Super Admin must remain.` |
| `22023` | `Username must be 3–40 characters without spaces, slashes, or colons.` |
| `22023` | `Display name must contain between 2 and 80 characters.` |
| `22023` | `Choose a valid role.` / `A new password must contain at least 8 characters.` / `A replacement password must contain at least 8 characters.` |
| `P0002` | `Dashboard user not found.` |
| `23505` | `That username is already in use.` |

**Side effects:** setting a password sets `must_change_password = true`, resets `failed_attempts`,
clears `locked_until`, and revokes the user's other sessions.

---

### 7.9 `dashboard_delete_user`

Super Admin only. **Body:** `{ "p_user_id": "uuid" }`. **Returns:** `true`.

| Code | Message |
| --- | --- |
| `42501` | `Super Admin authorization required.` |
| `42501` | `You cannot delete your own signed-in account.` |
| `42501` | `At least one active Super Admin must remain.` |
| `P0002` | `Dashboard user not found.` |

---

### 7.10 `dashboard_update_own_credentials`

Self-service username / display name / password change. Requires the current password.

**Body**

| Field | Type | Notes |
| --- | --- | --- |
| `p_current_password` | text | Verified with `crypt()` |
| `p_username` | text | 3–40 chars, lower-cased |
| `p_display_name` | text | 2–80 chars |
| `p_new_password` | text | `''` = keep; otherwise ≥ 8 chars |

**Returns:** `{ "ok": true, "user": { …user payload… } }`.

**Errors**

| Code | Message |
| --- | --- |
| `42501` | `Your session is invalid or expired.` / `Your dashboard account is no longer active.` / `Current password is incorrect.` |
| `22023` | `Username must be 3–40 characters without spaces, slashes, or colons.` |
| `22023` | `Display name must contain between 2 and 80 characters.` |
| `22023` | `Choose a new password containing at least 8 characters.` (when `must_change_password`) |
| `22023` | `New password must contain at least 8 characters.` |
| `23505` | `That username is already in use.` |

**Side effect:** if the username or password changed, every *other* session for that user is revoked.

---

### 7.11 Internal helpers (not callable from the browser)

`dashboard_valid_permissions()`, `dashboard_request_session_token()`, `dashboard_current_user_id()`,
`dashboard_has_permission(text)`, `dashboard_has_any_permission(text[])`, `dashboard_is_super_admin()`,
`is_dashboard_request()`, `dashboard_user_payload(uuid)`, `dashboard_role_payload(uuid)`.
`dashboard_user_payload` and `dashboard_role_payload` are explicitly revoked from `public`.

---

## 8. Storage API (PDF)

Bucket: **`pdf-books`** — public, `allowed_mime_types = ['application/pdf']`, 32 MB limit.

### Upload

```
POST /storage/v1/object/pdf-books/{yyyy}/{uuid}-{safeName}
Content-Type: application/pdf
x-upsert: false
apikey / Authorization / x-dashboard-session
```

- Performed with `XMLHttpRequest` so `upload.onprogress` can drive the progress bar (60 s timeout).
- Filename is NFKD-normalized; non `[a-zA-Z0-9._-]` characters become `-`.
- Client-side guards: must be a `File`, must be `application/pdf` or end in `.pdf`, must be ≤ 32 MB.

**Returns**

```json
{ "url": "…/storage/v1/object/public/pdf-books/2026/<uuid>-file.pdf",
  "path": "2026/<uuid>-file.pdf",
  "provider": "supabase-storage",
  "size": 1234567,
  "filename": "file.pdf",
  "mime": "application/pdf",
  "response": { …raw Storage response… } }
```

Store `path` in `blogs.pdf_storage_path` / `pdf_books.file_storage_path` and set the provider column
to `supabase-storage` — that is what makes deletion possible later.

### Public URL

```
GET /storage/v1/object/public/pdf-books/{path}
```

`NC.api.storagePublicUrl(bucket, path)` builds this with each path segment percent-encoded.

### Delete

```
DELETE /storage/v1/object/pdf-books
Content-Type: application/json
Body: { "prefixes": ["2026/<uuid>-file.pdf"] }
```

Never throws: `deleteStorageObject()` resolves `{ ok: false, error }` and the UI warns that the file
must be removed manually from Supabase Storage.

**Storage RLS:** insert/update/delete require the `blogs` **or** `books` menu permission
(`dashboard_has_any_permission(array['blogs','books'])`), falling back to `is_dashboard_request()`
on pre-004 installs. Public `SELECT` is always allowed.

---

## 9. ImgBB image API

Used for author avatars, blog hero images, gallery images, book covers, and submission thumbnails.

```
POST https://api.imgbb.com/1/upload?key=<IMGBB_API_KEY>
Content-Type: multipart/form-data

image: <binary file>
name:  <filename without extension, max 100 chars>
```

**Client validation:** MIME must be one of `image/jpeg, png, webp, gif, bmp, avif, heic, heif`;
size ≤ 32 MB; 60 s timeout; progress via `XMLHttpRequest`.

**Response (normalized by `NC.media.uploadImage`)**

```json
{
  "url": "https://i.ibb.co/…/image.jpg",
  "display_url": "https://i.ibb.co/…/image.jpg",
  "delete_url": "https://ibb.co/…/delete/…",
  "filename": "image.jpg",
  "size": 123456,
  "mime": "image/jpeg",
  "provider": "imgbb",
  "uploaded_at": "2026-08-31T06:00:00.000Z"
}
```

Persisted into the row as (`NC.crud.imagePayload`):

```json
{
  "image": "<url>",
  "imgbb_delete_url": "<delete_url>",
  "image_meta": { "display_url": "…", "filename": "…", "size": 0,
                  "mime": "…", "provider": "imgbb", "uploaded_at": "…" }
}
```

**Deletion:** ImgBB exposes a human confirmation page rather than a public delete API. The dashboard
sends `DELETE <delete_url>` (`mode: cors`, `credentials: omit`, 8 s timeout) and honestly reports
`{ ok: false, requiresManual: true, deleteUrl }` when it fails, prompting manual cleanup.

---

## 10. Global search

Triggered by `Ctrl/⌘ + K` or the top-bar field; implemented client-side in `NC.api.searchAll(query)`.

- Term is trimmed, stripped of `, * ( )`, collapsed to a single space, capped at 80 chars, and
  ignored below 2 characters.
- For each **accessible** table it issues one request:

```
GET /rest/v1/<table>?select=*&or=(<f1>.ilike.*term*,<f2>.ilike.*term*)&order=created_at.desc&limit=5
```

| Table | Searched columns |
| --- | --- |
| `blogs` | `title`, `sub_title`, `slug` |
| `authors` | `title`, `designation` |
| `categories` | `title`, `sub_title` |
| `comments` | `name`, `content`, `blog_title` |
| `galleries` | `title`, `description` |
| `pdf_books` | `title`, `author_or_editor` |
| `submitted_blogs` | `title`, `writer_name`, `content_title` |
| `videos` | `title`, `description` |

Requests run in parallel via `Promise.allSettled`; tables the user may not access are skipped and
individual failures are dropped. Results are flattened to
`{ table, label, icon, item }` and rendered grouped by section.

---

## 11. Schema / health probe

`NC.api.schemaProbe()` (wired to **Settings → Authentication & database → Run check**) issues one
lightweight `GET …?select=…&limit=1` per table and reports:

| Check | Request | Meaning when it fails |
| --- | --- | --- |
| Content tables | `select=id` | `PGRST205` → table missing → run `schema.sql` |
| `blogs` media columns | `select=id,imgbb_delete_url,image_meta,inline_media,pdf_file_provider,pdf_storage_path,pdf_file_size_mb` | `PGRST204`/`42703` → run migration `003` |
| `submitted_blogs.inline_media` | `select=id,inline_media` | run migration `003` |
| Access control | presence of the `dashboard_login` RPC (client-side `isLegacy()` check) | run migration `004` |

Returns `{ ok, results[], missing[], mismatched[], accessControlMissing }`.

---

## 12. Import pipeline

`assets/js/importer.js` imports CSV and genuine Excel (`.xls`/`.xlsx`, via SheetJS) into all eight
content tables. It is a client-side workflow, but it is worth documenting because it is a bulk
writer against the REST API.

**Sequence**

1. **Template download** — `ningshing-che-<entity>-template.csv` or `.xlsx`, headers = column keys.
2. **Parse** — first row is the header; headers are matched case-insensitively against aliases.
   Unknown headers are reported, not silently dropped.
3. **Validate** — required fields, URLs, booleans (`yes/no`, `true/false`, `1/0`), rich text, slugs.
   Each row becomes `valid`, `warning`, `duplicate`, or `invalid`.
4. **Preview** — a table showing row number, record label, status, and issues.
5. **Import** — `POST /rest/v1/<table>` per row, sequentially, with duplicate skipping resolved
   against up to 5000 existing rows (`GET …?select=<duplicateSelect>&order=id.asc`, paged at 1000).

**Field maps**

| Entity | Table | Required | Columns |
| --- | --- | --- | --- |
| Authors | `authors` | `title` | `title`\*, `designation`, `location`, `image`, `description`, `is_verified` |
| Categories | `categories` | `title` | `title`\*, `sub_title`, `slug`, `icon_name` |
| Blogs | `blogs` | `title` (+ relationships) | `title`\*, category/author resolution, `content`, `status`, `tags`, `slug`, flags |
| Comments | `comments` | `name`, `content`, blog reference | resolved via `blog_id` |
| Galleries | `galleries` | `title` | `title`\*, `description`, `image`, `category` |
| PDF Books | `pdf_books` | `title` | `title`\*, `author_or_editor`, `edition`, `category`, `page_count`, `file_size_mb`, `link`, `image`, `description`, `book_published_date` |
| Submit Blogs | `submitted_blogs` | `title`, `content`, `writer_name` | `title`\*, `content_title`, `content`\*, `writer_name`\*, contact fields |
| Videos | `videos` | `title`, `video_link` | `title`\*, `video_link`\*, `platform`, `description` |

\* required

Duplicate keys: authors/categories/blogs by lower-cased `title` (and `slug`), comments by
`blog_id + name + email + content`, galleries by `title + image + category`, books by
`title + edition`, submissions by `title + writer_email + writer_name`, videos by `video_link`.

Ready-to-import datasets live in `backend/imports/`.

---

## 13. Error reference

Every failure is normalized into an `ApiError` with `{ message, status, code, details, hint, body }`
plus convenience flags. `NC.api.userMessage(error)` maps them to UI copy.

### 13.1 PostgREST / Postgres codes

| Code | Meaning | Dashboard message |
| --- | --- | --- |
| `PGRST205` | Table not in schema cache | *"The Supabase tables are not installed yet. Run backend/supabase/schema.sql first."* |
| `PGRST204`, `42703` | Column not in schema cache | *"The media columns are not installed yet. Run migration 003."* |
| `PGRST202` | Function not in schema cache | Treated as "migration 004 missing" → legacy login fallback |
| `23505` | Unique violation | *"A record with this unique value already exists."* |
| `23503` | Foreign key violation | *"This record is still used by related content and cannot be deleted."* |
| `42501` | Insufficient privilege | *"You do not have permission to perform this action…"* |
| `22023` | Invalid parameter value | Message from the raised exception |
| `22001` | String data right truncation | Raw message |
| `23502` | Not-null violation | Raw message |

### 13.2 Client-generated codes

| Code | Cause |
| --- | --- |
| `TIMEOUT` | `AbortController` fired after 20 s (REST) / 60 s (upload) |
| `NETWORK_ERROR` | `fetch` rejected — offline, DNS, CORS |
| `NO_FILE` | PDF upload called without a `File` |
| `INVALID_FILE` | Non-PDF selected for the book library |
| `FILE_TOO_LARGE` | Exceeds the 32 MB limit |
| `STORAGE_UPLOAD_ERROR` | Supabase Storage rejected the upload |

### 13.3 HTTP status mapping

| Status | Typical cause |
| --- | --- |
| `400` | Constraint or validation failure |
| `401` / `403` | Missing/invalid session, or RLS denied the operation |
| `404` | Table/RPC missing (`PGRST205`/`PGRST202`) |
| `406` | Malformed `select`/`Accept` |
| `409` | Unique or foreign-key conflict |
| `413` | Payload or file too large |
| `5xx` | Supabase-side incident — retry later |

---

## 14. JavaScript client reference

All helpers live on the `window.NC` namespace. Load order matters — see `index.html`.

### `NC.api`

| Method | Signature | Purpose |
| --- | --- | --- |
| `request(url, options)` | `(string, { method, body, headers, timeout, cache })` → `{ data, response }` | Raw authenticated fetch |
| `list(key, options)` | `({ select, order, limit, offset, filters, or, count })` → `{ data[], count }` | Paged read with total |
| `getById(key, id, select)` | → object \| `null` | Single record by `id` |
| `count(key, filters)` | → number | Header-only exact count |
| `insert(key, payload)` | → created row | `POST` |
| `update(key, id, payload)` | → updated row | `PATCH` by `id` |
| `upsert(key, payload, conflict = 'id')` | → row | Merge-duplicates upsert |
| `remove(key, id)` | → `true` | `DELETE` by `id` |
| `rpc(name, payload)` | → unwrapped result | `POST /rpc/<name>` |
| `slugExists(slug, excludeId)` | → boolean | Blog slug uniqueness |
| `searchAll(query)` | → `[{ table, label, icon, item }]` | Global search |
| `schemaProbe()` | → `{ ok, results, missing, mismatched, accessControlMissing }` | Health check |
| `uploadPdf(file, onProgress)` | → `{ url, path, provider, size, filename, mime }` | Storage upload |
| `deleteStorageObject(bucket, path)` | → `{ ok }` | Storage delete |
| `storagePublicUrl(bucket, path)` | → string | Public URL builder |
| `attemptImgBBDelete(deleteUrl)` | → `{ ok, requiresManual?, deleteUrl }` | Best-effort image removal |
| `userMessage(error, fallback)` | → string | Human-readable error |
| `tableName(key)` | → string | Logical key → physical table |

### `NC.auth`

`restore()`, `login(username, password, remember)`, `logout(reason)`, `isAuthenticated()`,
`validateSession()`, `updateUser(user)`, `getDashboardToken()`, `getSessionToken()`,
`getAccessToken()`, `canAccess(route)`, `isSuperAdmin()`, `firstAccessibleRoute(preferred)`,
`remainingMilliseconds()`, `isLegacy()`.

### `NC.media`

`uploadImage(file, onProgress)`, `imageUploaderHTML(opts)`, `mountImageUploader(el, opts)`,
`pdfUploaderHTML(opts)`, `mountPdfUploader(el, opts)`, `detectVideoProvider(url)`,
`videoPreviewHTML(url)`.

### Other namespaces

`NC.crud` (`ListState`, `save`, `deleteRecord`, `imagePayload`, `handleLoadError`, …),
`NC.components` (`toast`, `openModal`, `confirm`, `tableShell`, `pagination`, …),
`NC.editor` (`editorHTML`, `mountEditor`),
`NC.utils` (formatting, `slugify`, `sanitizeHTML`, `sha256`, `uuid`, `sortRecords`, …),
`NC.views` (one `render(container, context)` per route).

### Events

| Event | Detail | Fired when |
| --- | --- | --- |
| `nc:auth-change` | `{ authenticated, reason }` | Login, logout, expiry |
| `nc:session-change` | `{ user }` | Session refresh, credential update |

---

## 15. Security model and hardening

**What is already solid**

- Passwords: bcrypt (`gen_salt('bf', 12)`) inside PostgreSQL; never compared or stored in the browser.
- Sessions: 32 random bytes; **only the SHA-256 hash** is stored in `dashboard_sessions`;
  expiring (8 h / 7 days), revocable, with a 5-attempt / 15-minute lockout.
- Authorization: RLS on every table; write policies require the matching menu permission;
  `dashboard_users`, `dashboard_roles`, and `dashboard_sessions` are `revoke`d from `anon` and
  `authenticated` and reachable only via SECURITY DEFINER RPCs.
- Super Admin lockout protection via a transaction advisory lock plus a "last active Super Admin" guard.
- Content snapshots (`author_name`, `category_title`, …) are trigger-maintained, so the Android
  client keeps working without joins.

**Action items before this goes to production**

| Issue | Why it matters | Fix |
| --- | --- | --- |
| **ImgBB API key hard-coded in `config.js`** | Anyone viewing source can upload/consume your ImgBB quota | Rotate the key; proxy uploads through a Supabase Edge Function, or move to Supabase Storage |
| **`is_dashboard_request()` shared digest** (pre-004 fallback) | A static bearer value grants write access on installs that never ran migration 004 | Install migration 004 immediately; confirm `Settings → Run check` reports access control ready |
| **Legacy demo login fallback** (`admin123`, hash in `config.js`) | Used whenever `dashboard_login` is missing | Remove `NC_CONFIG.auth` once migration 004 is confirmed |
| Public `INSERT` on `comments` and `submitted_blogs` | Open to spam/abuse | Add rate limiting, a captcha, or a moderation queue with `status = 'Unpublish'` by default (already the default for comments) |
| Broad `SELECT` on content tables | Intentional (website + Android app read anonymously) | Keep, but never add a `service_role` key to the frontend |
| No server-side rate limiting on `dashboard_login` | Credential stuffing | Supabase Auth protections / WAF rules, or add an RPC-level throttle |

**Rules that must never be broken:** only a **publishable** key belongs in `backend/assets/js/`.
Never add a `service_role` key, database password, or management token to any frontend file.

---

## 16. cURL cheat sheet

Export your configuration first (values are in `assets/js/config.js`):

```bash
export SUPABASE_URL="https://slcpvmpsynkqdozvlsii.supabase.co"
export SUPABASE_KEY="sb_publishable_…"      # publishable key only
export SESSION=""                            # raw token from dashboard_login
```

**Login**

```bash
curl -s "$SUPABASE_URL/rest/v1/rpc/dashboard_login" \
  -H "apikey: $SUPABASE_KEY" -H "Authorization: Bearer $SUPABASE_KEY" \
  -H "Content-Type: application/json" -H "Prefer: return=representation" \
  -d '{"p_username":"admin","p_password":"admin123","p_remember":false,"p_user_agent":"curl"}'
```

**Use the session**

```bash
export SESSION="<token from login>"
H=(-H "apikey: $SUPABASE_KEY" -H "Authorization: Bearer $SUPABASE_KEY" -H "x-dashboard-session: $SESSION")
```

**Read published blogs (public, no session)**

```bash
curl -s "$SUPABASE_URL/rest/v1/blogs?select=id,title,slug,status&status=eq.Publish&order=created_at.desc&limit=20" \
  -H "apikey: $SUPABASE_KEY" -H "Authorization: Bearer $SUPABASE_KEY"
```

**Read with an exact total**

```bash
curl -s -D - -o /dev/null "$SUPABASE_URL/rest/v1/blogs?select=id&limit=1" \
  "${H[@]}" -H "Prefer: count=exact" -H "Range: 0-0"
# → content-range: 0-0/1234
```

**Create an author**

```bash
curl -s "$SUPABASE_URL/rest/v1/authors" "${H[@]}" \
  -H "Content-Type: application/json" -H "Prefer: return=representation" \
  -d '{"title":"Anupama Singha","designation":"Writer","location":"Sylhet, Bangladesh","is_verified":false}'
```

**Publish a comment (moderation toggle)**

```bash
curl -s -X PATCH "$SUPABASE_URL/rest/v1/comments?id=eq.<uuid>" "${H[@]}" \
  -H "Content-Type: application/json" -H "Prefer: return=representation" \
  -d '{"status":"Publish"}'
```

**Delete a blog**

```bash
curl -s -X DELETE "$SUPABASE_URL/rest/v1/blogs?id=eq.<uuid>" "${H[@]}" -H "Prefer: return=minimal"
```

**Approve a submission**

```bash
curl -s "$SUPABASE_URL/rest/v1/rpc/approve_submission" "${H[@]}" \
  -H "Content-Type: application/json" -H "Prefer: return=representation" \
  -d '{"p_submission_id":"<uuid>","p_category_id":"<uuid>","p_status":"Draft","p_slug":null}'
```

**Upload a PDF to Storage**

```bash
curl -s "$SUPABASE_URL/storage/v1/object/pdf-books/2026/my-book.pdf" "${H[@]}" \
  -H "Content-Type: application/pdf" -H "x-upsert: false" \
  --data-binary @my-book.pdf
# Public: $SUPABASE_URL/storage/v1/object/public/pdf-books/2026/my-book.pdf
```

**Delete a stored PDF**

```bash
curl -s -X DELETE "$SUPABASE_URL/storage/v1/object/pdf-books" "${H[@]}" \
  -H "Content-Type: application/json" -d '{"prefixes":["2026/my-book.pdf"]}'
```

**Validate / log out**

```bash
curl -s "$SUPABASE_URL/rest/v1/rpc/dashboard_session" "${H[@]}" \
  -H "Content-Type: application/json" -H "Prefer: return=representation" -d '{}'

curl -s "$SUPABASE_URL/rest/v1/rpc/dashboard_logout" "${H[@]}" \
  -H "Content-Type: application/json" -H "Prefer: return=representation" -d '{}'
```

---

## 17. Android client (Kotlin / Jetpack Compose)

The Android app in `app/` is a **second consumer of this same API**. Full UI-foundation and
integration guide: [`ANDROID_API.md`](../ANDROID_API.md). Summary:

| Layer | Class | What it calls |
| --- | --- | --- |
| Transport | `data/remote/SupabaseClient` (OkHttp) | `/rest/v1/<table>` CRUD for all nine content tables |
| Config | `data/remote/SupabaseConfig` | `restBaseUrl`, `authBaseUrl`, `imgbbUploadUrl`; keys from `BuildConfig` → `.env` |
| Reader cache | `data/local/AppDatabase` (Room) + `ArticleRepository` | `GET /blogs?status=eq.Publish`, then `categories`, `authors`, `pdf_books` |
| CMS state | `DashboardRepository` | Same endpoints, optimistic writes |
| Media | `ImgBbUploader` | `POST https://api.imgbb.com/1/upload` |

### Differences that matter when debugging the app

1. **No dashboard session header.** `SupabaseClient` sends only `apikey` and
   `Authorization`. It never sends `x-dashboard-session`, so every write is rejected with
   `42501` on any install where migration 004 is active. Reads still work because the public
   `SELECT` policies allow anonymous access.
2. **Auth is local, not the RPCs.** `signIn()` accepts `admin@ningshingche.com` / `admin123`
   in-process and stores credentials in plain `SharedPreferences`; `dashboard_login`,
   `dashboard_session`, and `dashboard_logout` are never called. `UserRole` in the app is an
   enum that does not exist in the database — the real roles live in `dashboard_roles`.
3. **Always `select=*`, never paginated.** `offset` exists as a parameter but is unused; there
   are no `Content-Range` counts, so statistics are computed from a partial list.
4. **`approve_submission` is not used.** `DashboardRepository.approveAndPublishSubmission()`
   performs a non-transactional local conversion and never sets `converted_blog_id`.
5. **Storage is not used.** PDFs are consumed from the `link` column; the `pdf-books` bucket is
   never written to from Android.
6. **Missing columns** in the app's `SupabaseModels.kt`: `image_meta`, `inline_media`,
   `imgbb_delete_url` (blogs), `pdf_file_provider` / `pdf_storage_path` / `pdf_file_size_mb`,
   `seo_description`, `reviewed_at`, `converted_blog_id`, `favicon_url`.

Fix order and code recipes: [`ANDROID_API.md` §14–§16](../ANDROID_API.md#14-critical-gaps-and-required-changes).

---

*Generated from the source in `backend/` — schema `supabase/schema.sql`, migrations
`003_blog_media_uploads.sql` and `004_dashboard_access_control.sql`, client modules
`assets/js/*.js` (dashboard v1.3.0).*
