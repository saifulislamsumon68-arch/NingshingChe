# Ningshing Che — Editorial Dashboard

> **API documentation:** see [`API.md`](./API.md) for every REST endpoint, RPC function, Storage
> operation, table schema, error code, and client helper.

A responsive, no-build administration dashboard for **Ningshing Che — Bishnupriya Manipuri Magazine**. It is built with semantic HTML, Tailwind CSS via CDN, modular Vanilla JavaScript, Chart.js, Quill, DOMPurify, SheetJS, Font Awesome 6 Pro, Supabase REST, ImgBB, and Supabase Storage.

The dashboard is located entirely inside `backend/`, as requested.

## Highlights

- Dark-first editorial interface with a persistent light theme
- Database-backed dashboard users with bcrypt password hashes and expiring, revocable, SHA-256-hashed sessions
- Custom roles with menu-level authorization, direct-route denial, and permission-aware Supabase RLS
- Super Admin user/role management plus secure self-service username, display-name, and password updates
- Real Supabase CRUD for authors, blogs, categories, comments, galleries, PDF books, public submissions, videos, and site settings
- Relationship-aware blog editing with categories and authors
- WYSIWYG author biography, article, and submission editors with sanitized HTML/source mode
- Quill image insertion through either a local ImgBB upload or a direct image URL
- Slug generation and server-backed uniqueness checks
- Before-save article, gallery, book, submission, and video previews
- Reusable ImgBB image uploader with drag/drop, progress, validation, metadata, and delete URL preservation
- Blog hero images through local upload or direct URL
- Blog and library PDF fields through local upload or direct URL, backed by a 32 MB Supabase Storage bucket
- Validated CSV and genuine Excel import for all eight content sections, with preview, templates, relationship resolution, and duplicate skipping
- First-row CSV/Excel population in every Add workflow for manual review before save
- Transactional submission-to-blog conversion with author de-duplication
- Real Chart.js metrics and activity calculated from Supabase records
- Global `Ctrl + K` search
- Accessible modals, confirmations, toasts, loading states, empty states, and keyboard navigation
- Responsive behavior from 320 px through large desktop screens

## Project structure

```text
backend/
├── index.html
├── README.md
├── assets/
│   ├── css/
│   │   └── styles.css
│   └── js/
│       ├── config.js          # Central browser-safe configuration
│       ├── utils.js           # Formatting, validation, sanitization helpers
│       ├── auth.js            # Database-backed login/session/permission client
│       ├── api.js             # Central Supabase REST/Storage API layer
│       ├── components.js      # Modal, toast, table, badges, states
│       ├── media.js           # ImgBB, PDF, and safe video previews
│       ├── editor.js          # Quill/fallback rich-text abstraction
│       ├── crud.js            # Shared list and CRUD behavior
│       ├── importer.js        # CSV/XLS/XLSX templates, validation, preview, import
│       ├── dashboard.js       # Metrics, charts, activity
│       ├── authors.js
│       ├── blogs.js
│       ├── categories.js
│       ├── comments.js
│       ├── galleries.js
│       ├── books.js
│       ├── submissions.js
│       ├── videos.js
│       ├── settings.js
│       ├── access-control.js  # Super Admin users, roles, and own credentials
│       └── app.js             # Authorized shell, routing, theme, search
├── API.md                     # API documentation (REST, RPC, Storage, schema, errors)
├── imports/
│   ├── ningshing-che-authors.csv             # Ready-to-import 27-author dataset
│   ├── ningshing-che-author-sources.csv      # Complete profile/image provenance audit
│   └── ningshing-che-categories-bangla.csv   # Ready-to-import Bengali categories
└── supabase/
    ├── schema.sql
    └── migrations/
        ├── 002_production_rls.sql          # Legacy only; never run after 004
        ├── 003_blog_media_uploads.sql
        └── 004_dashboard_access_control.sql
```

## Database setup

### What was inspected

The initial read-only request to the configured Supabase REST API on **2026-08-29** found that none of these required public tables existed in the PostgREST schema cache:

- `authors`
- `categories`
- `blogs`
- `comments`
- `galleries`
- `pdf_books`
- `submitted_blogs`
- `videos`
- `settings`

The project owner subsequently installed `schema.sql`, and a second read-only REST check returned HTTP 200 for all nine tables. No data was deleted or modified by either inspection. The supplied publishable key cannot run SQL migrations, so schema changes must be performed in the Supabase SQL Editor by a project owner.

### Install the schema

1. Sign in to the correct [Supabase dashboard](https://supabase.com/dashboard).
2. Open **SQL Editor** for the project.
3. Review and run the complete `supabase/schema.sql` file.
4. Review and run `supabase/migrations/004_dashboard_access_control.sql` (the current base schema already contains migration 003's media columns).
5. Sign in, replace the initial Super Admin password, and open **Settings → Authentication & database → Run check**.

### Upgrade an existing dashboard database

If `schema.sql` was installed before Blog media uploads were added, run the complete `supabase/migrations/003_blog_media_uploads.sql` file once in Supabase SQL Editor. It additively adds:

- ImgBB metadata and deletion URL columns for Blog hero images
- JSON metadata for images inserted through the Blog/submission Quill editors
- Supabase Storage provider, object path, and size fields for Blog PDF attachments
- Updated submission conversion logic that transfers hero and inline-image metadata to the Blog

The dashboard database check also verifies these columns and reports **Migration needed** until the migration is installed.

Then run the complete `supabase/migrations/004_dashboard_access_control.sql` file. Migration 004 is the current authentication and authorization layer. It additively creates:

- `dashboard_roles`, `dashboard_users`, and `dashboard_sessions`
- bcrypt password hashing inside PostgreSQL
- random browser session tokens whose SHA-256 hashes alone are retained in Supabase
- 8-hour standard and 7-day remembered sessions, revocation, and five-attempt temporary lockout
- customizable menu permissions with a protected Super Admin role
- Super Admin-only user/role RPCs and current-password-protected self-credential updates
- menu-specific write and private-read RLS policies plus permission-aware PDF Storage policies
- safeguards against deleting, disabling, or demoting the final active Super Admin

For an existing installation, use this order:

1. Back up the Supabase database and Storage objects.
2. Run `003_blog_media_uploads.sql` if it has not already been installed.
3. Run `004_dashboard_access_control.sql`.
4. Sign in as `admin` / `admin123` only on a fresh access-control installation and replace that password immediately.
5. Open **Settings → Authentication & database → Run check**.

`002_production_rls.sql` belongs to the older fixed Supabase Auth design. **Do not run migration 002 after migration 004**; the legacy migration now detects migration 004 and aborts before it can replace the dashboard-session helper. The current full `schema.sql` includes downgrade guards and was rerun safely in QA, but numbered migrations remain the preferred upgrade path; never paste an older standalone demo-auth policy snippet over migration 004.

The initial schema is additive and uses `CREATE TABLE IF NOT EXISTS`; it does not intentionally drop content tables or rows. It creates:

- UUID primary keys
- Foreign keys for blog author/category and comment/blog relationships
- Status and value constraints
- Case-insensitive unique slug indexes
- Search and relationship indexes
- `created_at` / `updated_at` fields and update triggers
- Compatibility snapshot fields maintained by database triggers for the existing Android client
- RLS policies
- A `pdf-books` Storage bucket (PDF only, 32 MB)
- A transactional `approve_submission(...)` RPC

Always review and back up an established production database before running a migration.

## Supabase configuration

Browser-safe configuration is centralized in `assets/js/config.js`:

```js
supabase: {
  url: 'https://YOUR_PROJECT.supabase.co',
  publishableKey: 'YOUR_PUBLISHABLE_KEY',
  pdfBucket: 'pdf-books',
  pdfMaxBytes: 32 * 1024 * 1024
}
```

Only a **publishable browser key** belongs here. Never add any of the following to frontend files:

- `service_role` key
- database password
- Supabase management token
- private server credential

`api.js` centralizes REST headers, filters, CRUD, RPC calls, Storage uploads, timeouts, and user-friendly API errors.

## Authentication and access control

### Initial Super Admin

On the first run of migration 004, and only when `dashboard_users` is empty, Supabase creates:

```text
Username: admin
Password: admin123
Role: Super Admin
```

The account is marked `must_change_password`. The dashboard opens a non-dismissible credential dialog after login until a new password of at least eight characters is saved. Change the initial password before sharing the dashboard URL. Re-running migration 004 does not recreate or reset existing users.

### Roles and menu permissions

Migration 004 seeds five customizable role choices:

| Role | Initial menus |
| --- | --- |
| Super Admin | Every dashboard menu, including Users & Roles |
| Administrator | All editorial, Analytics, and Settings menus |
| Editor | Dashboard plus editorial content menus |
| Moderator | Dashboard, Comments, and Submit Blogs |
| Analyzer | Dashboard and Analytics |

A Super Admin can create additional roles, rename/edit all non-Super-Admin roles, assign visible menus, create or disable login users, assign roles, and reset another user's password. Dashboard is always included in a role; Users & Roles is reserved for the protected Super Admin role. System roles may be customized but not deleted. Custom roles can be deleted only when no users are assigned.

Menu restrictions are not cosmetic:

- the sidebar, global search, dashboard metrics/actions, and landing-page choices are filtered;
- a direct `#/route` URL renders an Access denied state;
- Supabase RLS requires the matching menu permission for inserts, updates, and deletes;
- private reads require the matching menu or Analytics permission (public-site read policies remain intentionally public);
- PDF Storage writes require Blogs or PDF Books access;
- role edits revoke every active session assigned to that role so stale permissions stop at the database immediately.

The final active Super Admin cannot be demoted, disabled, or deleted. Those checks are serialized in PostgreSQL to remain safe across concurrent requests.

### Session and password design

`dashboard_login(...)` verifies bcrypt password hashes inside PostgreSQL. After a successful login it returns a one-time random token; only the token's SHA-256 hash is stored in `dashboard_sessions`. The raw token is held in:

- `sessionStorage` for the standard 8-hour session, or
- `localStorage` for the optional 7-day remembered session.

Authenticated REST and RPC requests send the token in `x-dashboard-session`. Supabase validates expiration, revocation, account status, temporary lock state, role, and menu permission. Five failed login attempts lock the account for 15 minutes. Logout revokes the server session before clearing the browser copy.

Any signed-in user can select **My login** to update their display name, username, or password after confirming the current password. Username/password changes revoke that user's other active sessions. Password and session hashes are never returned to the browser.

### Compatibility upgrade behavior

Before migration 004 exists, the client recognizes the missing `dashboard_login` RPC and temporarily uses the former version-1 demo login so an existing installation is not locked out during deployment. The **Users & Roles** page then displays the exact migration path. As soon as `dashboard_session` is available, legacy sessions are rejected and the fallback cannot be used to bypass database login.

This fallback is migration compatibility, not the target security model. Install migration 004 promptly. Do not expose a service-role key, database password, or Supabase management token in this static application.

## ImgBB setup

The ImgBB client configuration is centralized in `assets/js/config.js`:

```js
imgbb: {
  endpoint: 'https://api.imgbb.com/1/upload',
  apiKey: 'YOUR_IMGBB_UPLOAD_KEY',
  maxBytes: 32 * 1024 * 1024
}
```

The reusable uploader captures and preserves:

- `url`
- `display_url`
- `delete_url`
- `filename`
- `size`
- `mime`
- provider and upload timestamp

The same uploader is used for Blog hero images and inside Quill’s image action. In both places an editor may either choose a local image file for ImgBB upload or enter a direct public image URL. Blog hero metadata is stored in `imgbb_delete_url` / `image_meta`; Quill image metadata is stored in `inline_media` so deletion information is not discarded.

When replacing media, the dashboard uploads and saves the new image first, then attempts to remove the old image. Abandoned uploads created during an editor session are also tracked for cleanup. Images shared by a converted submission and its Blog are preserved instead of being remotely deleted while the related record still relies on them.

### ImgBB deletion behavior

ImgBB commonly supplies a human deletion page rather than a documented cross-origin deletion API. The dashboard:

1. Deletes the database record/reference.
2. Attempts a remote `DELETE` using the saved deletion URL.
3. Reports failures honestly.
4. Offers the saved ImgBB deletion page for manual completion.

It never silently claims remote deletion succeeded when ImgBB or browser CORS does not confirm it.

## PDF uploads

ImgBB’s upload API is for images, not PDF documents. Local PDF files therefore use the `pdf-books` Supabase Storage bucket created by `schema.sql`; the user-facing controls still provide the requested file-or-direct-URL workflow for both Blog attachments and PDF Books.

- Accepted local file: PDF
- Maximum size: 32 MB
- Real upload progress is shown
- Direct public PDF links remain supported
- Storage provider, object path, and size are preserved for later cleanup
- Old stored files are deleted only after the replacement record saves successfully

## CSV and Excel imports

Spreadsheet import is available for **Authors, Blogs, Categories, Comments, Galleries, PDF Books, Submit Blogs, and Videos**. Settings are intentionally excluded.

Ready-to-import UTF-8 CSV datasets are included in `imports/`:

- `ningshing-che-authors.csv` contains the 27 authors from the public **আমার লেখক পারেঙ** carousel in the requested order. Each individual profile was retrieved on 2026-08-30; names, designation/byline text, full available biography, verification state, and the profile page's original image URL were preserved. Biography markup is compact Quill-compatible HTML. The source site exposes no separate location field, so `location` remains blank rather than being inferred from biography or designation text.
- `ningshing-che-author-sources.csv` is the comprehensive provenance copy: it repeats every author's designation, location, full available description, verification state, and profile-image URL, then adds the exact profile URL, image host, HTTP validation status, biography availability, and source exceptions. Sixteen profile images are Blogger-hosted, ten are served by the site's GitHub Pages repository, and the **নিংশিং চে** logo is served by GitHub raw content. Those 11 non-Blogger URLs are the addresses exposed by the live profiles and were not replaced with invented Blogger URLs. **কুঙ্গ থাঙ** uses the Blogger-hosted placeholder supplied by the live profile. Five profiles render no biography (`None`), so their import descriptions are intentionally blank.
- `ningshing-che-categories-bangla.csv` contains the supplied Bengali category list. Its slugs use URL-safe hyphens for spaces, and the shared slug normalizer preserves Bengali combining marks.

All 27 author profile pages and all 27 distinct profile-image URLs returned HTTP 200 during validation. The primary author CSV uses only the six supported Author headers, so it imports without ignored-column warnings; the separate source audit retains provenance that is not part of the Supabase Author schema.

Two workflows are provided:

1. **Bulk create:** select **Import CSV / Excel** in a list-page header, upload a file, select a worksheet when necessary, inspect the validation preview, and import all valid rows.
2. **Fill one Add form:** select **Choose file** in the import card at the top of an Add workflow. The first data row fills the form but is **not saved automatically**; review and edit it before using the normal Save/Publish action.

Both import surfaces provide downloadable CSV and Excel templates. Excel templates include an **Import Data** worksheet and a separate **Instructions** worksheet with required-field guidance. The implementation accepts:

- `.csv` (UTF-8, UTF-8 BOM, UTF-16 LE, or UTF-16 BE)
- genuine `.xlsx`
- legacy `.xls` input
- up to 10 MB and 5,000 data rows per selected sheet

The pinned standalone SheetJS browser build parses and generates Excel workbooks without npm, a compiler, or a build step. CSV parsing also has a local fallback so CSV remains usable if SheetJS is unavailable.

### Import columns

Headers are case-insensitive, and common labels such as `name`, `image_url`, `subtitle`, and `pdf_url` are recognized as aliases. The safest option is to retain the downloaded template headers.

| Section | Supported template columns |
| --- | --- |
| Authors | `title`, `designation`, `location`, `image`, `description`, `is_verified` |
| Blogs | `title`, `slug`, `sub_title`, `content`, `category_slug`, `category_id`, `author_name`, `author_id`, `status`, `tags`, `image`, `video_link`, `pdf_book_link`, `seo_title`, `seo_description`, `is_slider`, `is_feature`, `is_special_article`, `published_date` |
| Categories | `title`, `sub_title`, `slug`, `icon_name` |
| Comments | `blog_slug`, `blog_id`, `name`, `email`, `phone`, `address`, `content`, `status` |
| Galleries | `title`, `image`, `category`, `description` |
| PDF Books | `title`, `book_published_date`, `image`, `link`, `author_or_editor`, `edition`, `category`, `page_count`, `file_size_mb`, `description` |
| Submit Blogs | `title`, `designation`, `address`, `phone`, `thumbnail`, `writer_name`, `writer_designation`, `writer_profile_image`, `writer_email`, `writer_facebook`, `content_title`, `content`, `status` |
| Videos | `title`, `video_link`, `thumbnail_url`, `description` |

### Relationships, media, statuses, and dates

- Blog categories resolve against an existing category by `category_id`, slug, or exact case-insensitive title.
- Blog authors resolve against an existing author by `author_id` or exact case-insensitive name.
- Comment blogs resolve against an existing Blog by `blog_id`, slug, or exact case-insensitive title.
- An unresolved required relationship marks the bulk row invalid. First-row form population reports a warning and leaves the relationship for manual selection.
- Spreadsheet image and PDF cells accept **direct public `http://` or `https://` URLs only**. Embedded Excel images, attachments, and local file paths are not extracted. Direct-URL media metadata is retained with provider `url`; no fabricated ImgBB deletion URL or Storage object path is created.
- Blog and submission content is sanitized before it reaches either the form or Supabase. Plain text is converted to basic paragraphs; supported HTML is preserved after sanitization.
- Blog status accepts Draft or Publish; Comment status accepts Publish or Unpublish. Submission import accepts Pending, Reviewed, or Rejected. Approval/Published transitions remain reserved for the dashboard conversion workflow.
- Dates may use `YYYY-MM-DD` or another browser-parseable date. New-record `created_at` and `updated_at` values remain server-managed.

### Validation and duplicate policy

Nothing is written during parsing or preview. Each row is labeled **Ready**, **Duplicate**, or **Invalid**, and unrecognized columns are explicitly reported. Invalid rows and duplicates are skipped; imports never overwrite existing records.

Duplicate identity is evaluated as follows:

| Section | Duplicate identity |
| --- | --- |
| Authors | case-insensitive title/name |
| Blogs | case-insensitive slug |
| Categories | case-insensitive slug |
| Comments | Blog + email/name + exact normalized comment content |
| Galleries | image URL, falling back to title + category |
| PDF Books | title + edition |
| Submit Blogs | title + writer email/name |
| Videos | normalized video URL |

Duplicates are checked against current Supabase records and against earlier rows in the selected worksheet. Valid rows are inserted individually with bounded concurrency, so one rejected row does not discard successful rows. The result screen reports imported, skipped, and failed totals and preserves row-specific API errors for correction.

> **Migration note:** Blog and Submit Blog imports that include the newer media metadata rely on `supabase/migrations/003_blog_media_uploads.sql`. Install that migration on an older database before testing those imports.

## Running locally

A local HTTP server is recommended so browser security APIs and CDN assets work consistently.

```bash
cd backend
python3 -m http.server 8080
```

Open:

```text
http://localhost:8080
```

No `npm install`, compiler, bundler, or build step is required.

Opening `index.html` directly may work in some browsers, but a local server avoids browser restrictions around secure hashing, modules/CDNs, and cross-origin requests.

## CDN dependencies

- Tailwind CSS Play CDN
- Font Awesome 6 Pro from the provided project URL
- Chart.js `4.4.7`
- Quill `2.0.3`
- DOMPurify `3.2.4`
- SheetJS `0.20.3` from the authoritative pinned browser CDN

Pinned versions are used where the CDN package supports it. If a CDN is blocked, core error and empty states remain readable; rich-text editing falls back to a basic contenteditable editor.

## Rich text and XSS safety

Author biography, Blog, and submission HTML is treated as untrusted. The shared Quill editor abstraction sanitizes content with DOMPurify:

- before returning editor content
- before preview rendering
- before injecting stored content into dashboard previews

The Author Description field uses Quill for headings, emphasis, links, lists, quotations, alignment, tables, and sanitized HTML source. Its image-upload toolbar action is intentionally disabled because the Author schema has no inline-media metadata or deletion fields; the separate profile-image uploader retains its existing managed-media workflow.

External URLs are validated as `http:` or `https:`. Video previews use provider-specific, encoded iframe URLs; arbitrary embed HTML is never accepted.

For defense in depth, the public website should sanitize content again on render and deploy a restrictive Content Security Policy.

## Submission moderation

Supported states:

- Pending
- Reviewed
- Approved
- Rejected

Approval calls the transactional `approve_submission(...)` database function. It:

1. Locks and reads the submission.
2. Finds an author by case-insensitive writer name.
3. Creates an unverified author only if no match exists.
4. Creates the related Blog as Draft or Publish.
5. Preserves thumbnail, content title, and article content.
6. Marks the submission Approved and stores `converted_blog_id`.

Migration 004 requires **Submit Blogs** permission before the RPC can run, and its Author/Blog/Submission writes still pass the matching RLS policies. Assign all relevant editorial menus to roles that perform approvals. The frontend has a compatibility fallback if the RPC has not yet been installed, but the SQL RPC is preferred because it is atomic.

## Theme and dashboard preferences

- Dark is the default.
- The topbar theme button and Settings page both update the theme.
- Theme, landing page, page size, and table density are stored as browser preferences.
- Public-site settings are stored in the Supabase `settings` table.

## Deployment

The dashboard is static and can be deployed to:

- GitHub Pages
- Cloudflare Pages
- Netlify
- Vercel static hosting
- Any HTTPS web server

Deploy the contents of `backend/` as the site root. Then:

1. Verify Supabase project CORS/origin settings.
2. Use HTTPS.
3. Configure a strict Content Security Policy that permits only the required CDNs, Supabase project, ImgBB, and supported video providers.
4. Install migrations 003 and 004, change the initial Super Admin password, and test each role before exposing the dashboard publicly.
5. Keep the dashboard on a restricted admin subdomain where practical.

## Data handling guarantees

The implementation follows these rules:

- PATCH updates only known form fields rather than replacing entire records.
- Existing optional values and relationships remain unless explicitly changed.
- Old ImgBB media is not deleted before a replacement upload and save succeeds.
- Foreign keys, not duplicated labels, are authoritative relationships.
- Compatibility labels for the Android client are maintained by database triggers.
- Spreadsheet imports perform create-only operations: existing duplicate identities are skipped and never patched.
- Spreadsheet relationship values must resolve to existing Supabase records before a bulk row is accepted.
- API failures remain visible and do not break the entire dashboard shell.
- No fake dashboard statistics are used; charts display zero/empty states until real records exist.

## QA performed

The implementation was checked with a local HTTP server and headless Chromium at desktop and mobile widths.

Completed checks:

- All JavaScript files pass `node --check`.
- All schema and migration SQL files parse successfully with PostgreSQL's grammar.
- `schema.sql`, migration 003, and migration 004 executed successfully on a clean PostgreSQL 17 database; migration 004 and the full guarded base schema were then rerun after populated/customized RBAC data without resetting users, roles, helpers, or policies.
- PostgreSQL integration assertions covered bcrypt login, hashed session tokens, session validation/revocation, five-attempt lockout, role allow-listing, self-credential changes, protected private tables, menu-specific RLS, Analytics reads, PDF Storage writes, role-session revocation, and submission-approval authorization.
- A concurrent two-Super-Admin cross-disable test forced both requests to queue on the advisory guard; one succeeded, the waiting request re-authorized and failed, and exactly one active Super Admin remained.
- Fixture-backed Chromium checks covered Super Admin login/user/role CRUD, the mandatory password-change dialog, role-filtered navigation/search/dashboard requests, direct-route denial, Analyzer restrictions, cross-menu moderation links/actions, Categories-only request filtering, legacy migration guidance, and Access Control layouts at desktop and 375 px without horizontal overflow.
- Automated axe scans reported zero violations for both the mandatory credential dialog and the populated Access Control page; the configured Supabase gateway's live CORS preflight explicitly accepted `x-dashboard-session`.
- Login, logout/session transition, dark/light switching, global search dialog, schema warning, and mobile sidebar were exercised.
- Dashboard rendering was checked at 320, 375, 414, 768, 1024, 1280, and 1440 px; no document-level horizontal overflow was detected.
- Every entity list and add/edit form was rendered against intercepted Supabase-shaped fixtures.
- The 27-author CSV was exercised through the Author bulk importer: all 27 rows were ready, with zero invalid rows, zero in-file duplicates, and no ignored headers. The complete runner produced 27 intercepted Author inserts, and first-row Add-form population preserved Sukanta Singha's Blogger image URL and formatted Quill biography without sending production requests.
- All 27 source profile pages and all 27 distinct source profile images were retrieved successfully; image payloads were decoded as valid PNG or JPEG files.
- Author and Category POST payloads, Blog draft POST payload, Comment status PATCH, and Video DELETE confirmation were exercised end to end through the API abstraction.
- Blog preview, rich editor, two-image submission form, submission approval form, PDF uploader, and provider-safe video preview were opened successfully.
- Blog hero and PDF file-or-URL controls, the nested Quill image chooser, and 375 px responsive layout were exercised in Chromium.
- Fixture-backed ImgBB and Supabase Storage uploads produced a Blog payload containing hero, inline-image, PDF provider/path/size, and deletion metadata.
- CSV bulk validation correctly classified ready, within-file duplicate, and invalid rows; an intercepted create request verified the normalized Supabase payload without modifying production.
- A genuine `.xlsx` workbook populated the first Author Add form row, including text, boolean, direct image URL, and Quill biography values.
- The Author Description Quill editor preserved formatted, sanitized HTML in its live preview and intercepted Supabase payload; its 375 px toolbar stayed inside the modal with internal scrolling.
- CSV and genuine Excel template downloads were generated successfully, and all eight list/Add workflows exposed their correct import controls while Settings exposed none.
- All eight entity transformers passed valid schema-shaped rows; Blog relation lookup, tag parsing, direct-media metadata, reading time, and HTML sanitization were verified.
- The import modal was checked at 375 px: it retained 10 px viewport margins, caused no document-level overflow, and confined its wide preview table to an internal horizontal scroller.
- Automated axe accessibility checks reported zero violations for the login and dashboard views at desktop and mobile sizes.
- No uncaught browser page errors occurred in the fixture-backed navigation pass or spreadsheet-import pass.

The configured project returns HTTP 200 for all nine required tables after `supabase/schema.sql` was installed. On that existing installation, the project owner must still apply migration 003 (if pending) and migration 004 in Supabase SQL Editor; a publishable browser key cannot execute DDL. Destructive live CRUD, live credential creation, and third-party ImgBB uploads were intentionally not run against production. Perform the final add/edit/delete/upload and multi-role checklist in a staging project after installing both migrations.

## Troubleshooting

### “Database setup required”

Run `supabase/schema.sql` in the project SQL Editor and then use the Settings database check.

### “Migration needed” or missing Blog media columns

Run `supabase/migrations/003_blog_media_uploads.sql` in Supabase SQL Editor, then reload the dashboard and run the database check again.

### “Security migration required” or Users & Roles shows setup instructions

Run `supabase/migrations/004_dashboard_access_control.sql` in Supabase SQL Editor after migration 003, then sign out and sign in again.

### `401` / `403` or RLS error while saving records

- Sign out and sign in again so a revoked or changed role gets a fresh session.
- Ask a Super Admin to confirm that the account is active and its role includes the relevant menu.
- Confirm migration 004 was run completely and that migration 002 was not run afterward.
- For submission approval, remember that conversion writes a submission, Blog, and potentially Author record; assign the relevant editorial menus.

### Initial `admin` login does not work

The `admin` / `admin123` account is inserted only when migration 004 finds no dashboard users. It is not recreated on reruns. Use an existing Super Admin account; if all credentials are lost, recover access directly through a carefully reviewed Supabase SQL Editor operation rather than placing a privileged key in the browser.

### Image upload fails

- Confirm the file is an accepted image type.
- Confirm it is below 32 MB.
- Check ImgBB key restrictions and browser network logs.

### PDF upload fails

- Confirm `schema.sql` created the `pdf-books` bucket and Storage policies.
- Use a PDF below 32 MB.
- Use a direct public PDF URL as a fallback.

### Spreadsheet cannot be read or rows are invalid

- Download a fresh template from the same section and preserve its first header row.
- Keep the file at or below 10 MB and 5,000 data rows per selected worksheet.
- For Blog/Comment relations, confirm the referenced author, category, or Blog already exists and that its ID, slug, or title matches.
- Use direct public URLs for spreadsheet media fields; do not paste local paths or embed files inside Excel.
- If `.xlsx` parsing or Excel template generation is unavailable, check whether `cdn.sheetjs.com` is blocked, or use CSV.
- Run migration 003 before importing Blog/submission media metadata into an older database.

### Charts, editor, or Excel parser do not load

Check whether the browser or content blocker is blocking the pinned CDN URLs.

## Production recommendations

- Install migration 004 and replace the seeded password before public use; never rely on compatibility login.
- Add MFA through a trusted authentication tier if the deployment's risk profile requires it.
- Keep role/user changes inside the protected database RPCs; never make private access tables browser-readable.
- Add audit logs for login, role changes, destructive actions, and publishing actions.
- Add rate limits and CAPTCHA to public comment/submission forms.
- Run ImgBB deletion through a trusted server/Edge Function if automatic deletion is essential.
- Add automated browser tests against a staging Supabase project.
- Back up content and Storage objects on a schedule.
