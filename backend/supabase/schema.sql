-- Ningshing Che Dashboard — initial Supabase schema
-- Generated after a read-only schema probe on 2026-08-29 found none of the
-- required public tables. This migration is additive: it does not drop tables
-- or existing rows. Re-runs preserve migration 004 RBAC helpers and policies.
-- Review in a staging project before running in production.

begin;

create extension if not exists pgcrypto;

create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.updated_at = timezone('utc', now());
  return new;
end;
$$;

create table if not exists public.authors (
  id uuid primary key default gen_random_uuid(),
  title text not null check (length(btrim(title)) > 0),
  image text not null default '',
  imgbb_delete_url text not null default '',
  image_meta jsonb not null default '{}'::jsonb,
  designation text not null default '',
  description text not null default '',
  is_verified boolean not null default false,
  location text not null default '',
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.categories (
  id uuid primary key default gen_random_uuid(),
  title text not null check (length(btrim(title)) > 0),
  sub_title text not null default '',
  slug text not null check (length(btrim(slug)) > 0),
  icon_name text not null default 'layer-group',
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create unique index if not exists categories_slug_lower_idx
  on public.categories (lower(slug));
create index if not exists categories_created_at_idx
  on public.categories (created_at desc);

create table if not exists public.blogs (
  id uuid primary key default gen_random_uuid(),
  title text not null check (length(btrim(title)) > 0),
  sub_title text not null default '',
  image text not null default '',
  imgbb_delete_url text not null default '',
  image_meta jsonb not null default '{}'::jsonb,
  inline_media jsonb not null default '[]'::jsonb,
  content text not null default '',
  category_id uuid references public.categories(id) on update cascade on delete set null,
  author_id uuid references public.authors(id) on update cascade on delete set null,
  status text not null default 'Draft' check (status in ('Draft', 'Publish')),
  tags text[] not null default '{}'::text[],
  seo_title text not null default '',
  seo_description text not null default '',
  video_link text not null default '',
  pdf_book_link text not null default '',
  pdf_file_provider text not null default 'url' check (pdf_file_provider in ('url', 'supabase-storage')),
  pdf_storage_path text not null default '',
  pdf_file_size_mb numeric(8,2) not null default 0 check (pdf_file_size_mb >= 0),
  slug text not null check (length(btrim(slug)) > 0),
  is_slider boolean not null default false,
  is_feature boolean not null default false,
  is_special_article boolean not null default false,
  views_count bigint not null default 0 check (views_count >= 0),
  reading_time_minutes integer not null default 1 check (reading_time_minutes > 0),
  published_date date,
  -- Compatibility snapshots for the existing Android client. These are
  -- maintained by a trigger; dashboard code treats the foreign keys as truth.
  category_title text not null default '',
  category_slug text not null default '',
  author_name text not null default '',
  author_image text not null default '',
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create unique index if not exists blogs_slug_lower_idx on public.blogs (lower(slug));
create index if not exists blogs_author_id_idx on public.blogs (author_id);
create index if not exists blogs_category_id_idx on public.blogs (category_id);
create index if not exists blogs_status_created_idx on public.blogs (status, created_at desc);
create index if not exists blogs_tags_gin_idx on public.blogs using gin (tags);

create or replace function public.sync_blog_relationship_snapshots()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  if new.author_id is not null then
    select a.title, a.image into new.author_name, new.author_image
    from public.authors a where a.id = new.author_id;
  else
    new.author_name := '';
    new.author_image := '';
  end if;

  if new.category_id is not null then
    select c.title, c.slug into new.category_title, new.category_slug
    from public.categories c where c.id = new.category_id;
  else
    new.category_title := '';
    new.category_slug := '';
  end if;

  if new.status = 'Publish' and new.published_date is null then
    new.published_date := current_date;
  end if;

  new.reading_time_minutes := greatest(
    1,
    ceil(array_length(regexp_split_to_array(regexp_replace(new.content, '<[^>]+>', ' ', 'g'), '\s+'), 1) / 220.0)::integer
  );
  return new;
end;
$$;

drop trigger if exists blogs_sync_relationships on public.blogs;
create trigger blogs_sync_relationships
before insert or update of author_id, category_id, status, content
on public.blogs for each row execute function public.sync_blog_relationship_snapshots();

create table if not exists public.comments (
  id uuid primary key default gen_random_uuid(),
  blog_id uuid not null references public.blogs(id) on update cascade on delete cascade,
  blog_title text not null default '',
  name text not null check (length(btrim(name)) > 0),
  address text not null default '',
  phone text not null default '',
  email text not null default '',
  content text not null check (length(btrim(content)) > 0),
  status text not null default 'Unpublish' check (status in ('Publish', 'Unpublish')),
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create index if not exists comments_blog_id_idx on public.comments (blog_id);
create index if not exists comments_status_created_idx on public.comments (status, created_at desc);

create or replace function public.sync_comment_blog_title()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  select b.title into new.blog_title from public.blogs b where b.id = new.blog_id;
  return new;
end;
$$;

drop trigger if exists comments_sync_blog_title on public.comments;
create trigger comments_sync_blog_title
before insert or update of blog_id on public.comments
for each row execute function public.sync_comment_blog_title();

create table if not exists public.galleries (
  id uuid primary key default gen_random_uuid(),
  title text not null check (length(btrim(title)) > 0),
  description text not null default '',
  image text not null default '',
  imgbb_delete_url text not null default '',
  image_meta jsonb not null default '{}'::jsonb,
  category text not null default '',
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create index if not exists galleries_created_at_idx on public.galleries (created_at desc);

create table if not exists public.pdf_books (
  id uuid primary key default gen_random_uuid(),
  title text not null check (length(btrim(title)) > 0),
  image text not null default '',
  imgbb_delete_url text not null default '',
  image_meta jsonb not null default '{}'::jsonb,
  book_published_date date,
  link text not null default '',
  file_provider text not null default 'url' check (file_provider in ('url', 'supabase-storage')),
  file_storage_path text not null default '',
  author_or_editor text not null default '',
  edition text not null default '',
  category text not null default '',
  page_count integer not null default 0 check (page_count >= 0),
  file_size_mb numeric(8,2) not null default 0 check (file_size_mb >= 0),
  description text not null default '',
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create index if not exists pdf_books_published_idx
  on public.pdf_books (book_published_date desc nulls last, created_at desc);

create table if not exists public.submitted_blogs (
  id uuid primary key default gen_random_uuid(),
  title text not null check (length(btrim(title)) > 0),
  designation text not null default '',
  address text not null default '',
  phone text not null default '',
  thumbnail text not null default '',
  imgbb_delete_url text not null default '',
  thumbnail_meta jsonb not null default '{}'::jsonb,
  writer_name text not null check (length(btrim(writer_name)) > 0),
  writer_designation text not null default '',
  writer_profile_image text not null default '',
  writer_profile_delete_url text not null default '',
  writer_profile_meta jsonb not null default '{}'::jsonb,
  writer_email text not null default '',
  writer_facebook text not null default '',
  content_title text not null default '',
  content text not null check (length(btrim(content)) > 0),
  inline_media jsonb not null default '[]'::jsonb,
  status text not null default 'Pending'
    check (status in ('Pending', 'Reviewed', 'Approved', 'Rejected', 'Published')),
  reviewed_at timestamptz,
  converted_blog_id uuid references public.blogs(id) on update cascade on delete set null,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create index if not exists submitted_blogs_status_created_idx
  on public.submitted_blogs (status, created_at desc);
create index if not exists submitted_blogs_writer_name_lower_idx
  on public.submitted_blogs (lower(writer_name));
create index if not exists authors_title_lower_idx on public.authors (lower(title));

create table if not exists public.videos (
  id uuid primary key default gen_random_uuid(),
  title text not null check (length(btrim(title)) > 0),
  video_link text not null check (length(btrim(video_link)) > 0),
  platform text not null default 'Video Link',
  description text not null default '',
  thumbnail_url text not null default '',
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create index if not exists videos_created_at_idx on public.videos (created_at desc);

-- Kept as `settings` for compatibility with the existing Android client.
create table if not exists public.settings (
  id text primary key default 'site_settings',
  site_title text not null default 'Ningshing Che',
  site_description text not null default '',
  logo_url text not null default '',
  favicon_url text not null default '',
  default_seo_title text not null default '',
  default_seo_description text not null default '',
  contact_email text not null default '',
  contact_phone text not null default '',
  facebook_url text not null default '',
  youtube_url text not null default '',
  instagram_url text not null default '',
  hero_slider_enabled boolean not null default true,
  featured_articles_enabled boolean not null default true,
  special_articles_enabled boolean not null default true,
  allow_comments boolean not null default true,
  allow_user_submissions boolean not null default true,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

insert into public.settings (id, site_title, site_description)
values ('site_settings', 'Ningshing Che', 'Bishnupriya Manipuri Magazine')
on conflict (id) do nothing;

-- Apply a consistent updated_at trigger to every mutable entity.
do $$
declare
  table_name text;
begin
  foreach table_name in array array[
    'authors', 'categories', 'blogs', 'comments', 'galleries',
    'pdf_books', 'submitted_blogs', 'videos', 'settings'
  ]
  loop
    execute format('drop trigger if exists %I_set_updated_at on public.%I', table_name, table_name);
    execute format(
      'create trigger %I_set_updated_at before update on public.%I for each row execute function public.set_updated_at()',
      table_name,
      table_name
    );
  end loop;
end $$;

-- Dashboard request helper -------------------------------------------------
-- Fresh installations temporarily use the version-1 compatibility digest so
-- the static UI is not locked out before migration 004 is applied. If the RBAC
-- session helper already exists, a schema re-run must never overwrite it.
do $dashboard_auth_guard$
begin
  if to_regprocedure('public.dashboard_current_user_id()') is null then
    execute $dashboard_auth_function$
      create or replace function public.is_dashboard_request()
      returns boolean
      language sql
      stable
      security definer
      set search_path = public
      as $body$
        select coalesce(
          nullif(current_setting('request.headers', true), '')::jsonb ->> 'x-dashboard-token',
          ''
        ) = 'bf6b5bdb74c79ece9fc0ad0ac9fb0359f9555d4f35a83b2e6ec69ae99e09603d';
      $body$;
    $dashboard_auth_function$;
  end if;
end
$dashboard_auth_guard$;

-- Transactional submission-to-blog conversion. The relationship IDs remain
-- authoritative; snapshot fields are populated by the blog trigger.
create or replace function public.approve_submission(
  p_submission_id uuid,
  p_category_id uuid,
  p_status text default 'Draft',
  p_slug text default null
)
returns public.blogs
language plpgsql
set search_path = public
as $$
declare
  submission public.submitted_blogs;
  selected_author public.authors;
  created_blog public.blogs;
  final_slug text;
  authorized boolean;
begin
  if to_regprocedure('public.dashboard_has_permission(text)') is not null then
    execute 'select public.dashboard_has_permission($1)' into authorized using 'submissions';
  else
    authorized := public.is_dashboard_request();
  end if;
  if not coalesce(authorized, false) then
    raise exception 'Submit Blogs authorization required' using errcode = '42501';
  end if;

  select * into submission
  from public.submitted_blogs
  where id = p_submission_id
  for update;

  if not found then
    raise exception 'Submission not found' using errcode = 'P0002';
  end if;

  select * into selected_author
  from public.authors
  where lower(title) = lower(submission.writer_name)
  order by created_at asc
  limit 1;

  if selected_author.id is null then
    insert into public.authors (
      title, image, imgbb_delete_url, image_meta, designation, description, is_verified
    ) values (
      submission.writer_name,
      submission.writer_profile_image,
      submission.writer_profile_delete_url,
      submission.writer_profile_meta,
      submission.writer_designation,
      '',
      false
    ) returning * into selected_author;
  end if;

  final_slug := nullif(btrim(p_slug), '');
  if final_slug is null then
    final_slug := 'submission-' || replace(p_submission_id::text, '-', '');
  end if;

  insert into public.blogs (
    title, sub_title, image, imgbb_delete_url, image_meta,
    content, inline_media, category_id, author_id, status, slug
  ) values (
    submission.title,
    submission.content_title,
    submission.thumbnail,
    submission.imgbb_delete_url,
    submission.thumbnail_meta,
    submission.content,
    submission.inline_media,
    p_category_id,
    selected_author.id,
    case when p_status = 'Publish' then 'Publish' else 'Draft' end,
    final_slug
  ) returning * into created_blog;

  update public.submitted_blogs
  set status = 'Approved', reviewed_at = timezone('utc', now()), converted_blog_id = created_blog.id
  where id = p_submission_id;

  return created_blog;
end;
$$;

revoke all on function public.is_dashboard_request() from public;
grant execute on function public.is_dashboard_request() to anon, authenticated;
grant execute on function public.approve_submission(uuid, uuid, text, text) to anon, authenticated;

grant usage on schema public to anon, authenticated;
grant select, insert, update, delete on
  public.authors,
  public.categories,
  public.blogs,
  public.comments,
  public.galleries,
  public.pdf_books,
  public.submitted_blogs,
  public.videos,
  public.settings
to anon, authenticated;

-- Row Level Security -------------------------------------------------------
alter table public.authors enable row level security;
alter table public.categories enable row level security;
alter table public.blogs enable row level security;
alter table public.comments enable row level security;
alter table public.galleries enable row level security;
alter table public.pdf_books enable row level security;
alter table public.submitted_blogs enable row level security;
alter table public.videos enable row level security;
alter table public.settings enable row level security;

-- Re-running this script updates compatibility policies without touching
-- content. When migration 004 exists, remove (and never recreate) the broad
-- compatibility policy; migration 004's menu-specific policies stay intact.
do $$
declare
  t text;
  rbac_installed boolean := to_regprocedure('public.dashboard_has_permission(text)') is not null;
begin
  foreach t in array array[
    'authors','categories','blogs','comments','galleries',
    'pdf_books','submitted_blogs','videos','settings'
  ] loop
    execute format('drop policy if exists %I on public.%I', t || '_dashboard_all', t);
    if not rbac_installed then
      execute format(
        'create policy %I on public.%I for all to anon, authenticated using (public.is_dashboard_request()) with check (public.is_dashboard_request())',
        t || '_dashboard_all', t
      );
    end if;
  end loop;
end $$;

-- Public website read policies.
drop policy if exists authors_public_read on public.authors;
create policy authors_public_read on public.authors for select to anon, authenticated using (true);
drop policy if exists categories_public_read on public.categories;
create policy categories_public_read on public.categories for select to anon, authenticated using (true);
drop policy if exists blogs_public_read on public.blogs;
create policy blogs_public_read on public.blogs for select to anon, authenticated using (status = 'Publish');
drop policy if exists comments_public_read on public.comments;
create policy comments_public_read on public.comments for select to anon, authenticated using (status = 'Publish');
drop policy if exists galleries_public_read on public.galleries;
create policy galleries_public_read on public.galleries for select to anon, authenticated using (true);
drop policy if exists pdf_books_public_read on public.pdf_books;
create policy pdf_books_public_read on public.pdf_books for select to anon, authenticated using (true);
drop policy if exists videos_public_read on public.videos;
create policy videos_public_read on public.videos for select to anon, authenticated using (true);
drop policy if exists settings_public_read on public.settings;
create policy settings_public_read on public.settings for select to anon, authenticated using (true);

-- Public contribution policies expose INSERT only, never private submissions.
drop policy if exists comments_public_insert on public.comments;
create policy comments_public_insert on public.comments for insert to anon, authenticated
with check (status = 'Unpublish');
drop policy if exists submissions_public_insert on public.submitted_blogs;
create policy submissions_public_insert on public.submitted_blogs for insert to anon, authenticated
with check (status = 'Pending' and converted_blog_id is null);

-- Optional storage bucket used for PDF uploads. ImgBB remains the image host.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('pdf-books', 'pdf-books', true, 33554432, array['application/pdf'])
on conflict (id) do update
set public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists pdf_books_public_storage_read on storage.objects;
create policy pdf_books_public_storage_read on storage.objects
for select to anon, authenticated using (bucket_id = 'pdf-books');

-- Keep schema re-runs compatible with migration 004's menu-specific Storage
-- authorization instead of downgrading it to the broad setup helper.
do $storage_policy_guard$
declare
  rbac_installed boolean := to_regprocedure('public.dashboard_has_any_permission(text[])') is not null;
begin
  drop policy if exists pdf_books_dashboard_storage_insert on storage.objects;
  drop policy if exists pdf_books_dashboard_storage_update on storage.objects;
  drop policy if exists pdf_books_dashboard_storage_delete on storage.objects;

  if rbac_installed then
    execute 'create policy pdf_books_dashboard_storage_insert on storage.objects for insert to anon, authenticated with check (bucket_id = ''pdf-books'' and public.dashboard_has_any_permission(array[''blogs'',''books'']::text[]))';
    execute 'create policy pdf_books_dashboard_storage_update on storage.objects for update to anon, authenticated using (bucket_id = ''pdf-books'' and public.dashboard_has_any_permission(array[''blogs'',''books'']::text[])) with check (bucket_id = ''pdf-books'' and public.dashboard_has_any_permission(array[''blogs'',''books'']::text[]))';
    execute 'create policy pdf_books_dashboard_storage_delete on storage.objects for delete to anon, authenticated using (bucket_id = ''pdf-books'' and public.dashboard_has_any_permission(array[''blogs'',''books'']::text[]))';
  else
    execute 'create policy pdf_books_dashboard_storage_insert on storage.objects for insert to anon, authenticated with check (bucket_id = ''pdf-books'' and public.is_dashboard_request())';
    execute 'create policy pdf_books_dashboard_storage_update on storage.objects for update to anon, authenticated using (bucket_id = ''pdf-books'' and public.is_dashboard_request()) with check (bucket_id = ''pdf-books'' and public.is_dashboard_request())';
    execute 'create policy pdf_books_dashboard_storage_delete on storage.objects for delete to anon, authenticated using (bucket_id = ''pdf-books'' and public.is_dashboard_request())';
  end if;
end
$storage_policy_guard$;

commit;
