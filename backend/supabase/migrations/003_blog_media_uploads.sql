-- Ningshing Che Dashboard — Blog and Quill media metadata
-- Run this once after the initial schema.sql on existing projects.
-- This migration is additive and preserves all existing Blog/submission data.

begin;

alter table public.blogs
  add column if not exists imgbb_delete_url text not null default '',
  add column if not exists image_meta jsonb not null default '{}'::jsonb,
  add column if not exists inline_media jsonb not null default '[]'::jsonb,
  add column if not exists pdf_file_provider text not null default 'url',
  add column if not exists pdf_storage_path text not null default '',
  add column if not exists pdf_file_size_mb numeric(8,2) not null default 0;

alter table public.submitted_blogs
  add column if not exists inline_media jsonb not null default '[]'::jsonb;

-- Add the constraints separately so this remains safe when columns already exist.
do $$
begin
  if not exists (
    select 1 from pg_constraint
    where conname = 'blogs_pdf_file_provider_check'
      and conrelid = 'public.blogs'::regclass
  ) then
    alter table public.blogs
      add constraint blogs_pdf_file_provider_check
      check (pdf_file_provider in ('url', 'supabase-storage'));
  end if;

  if not exists (
    select 1 from pg_constraint
    where conname = 'blogs_pdf_file_size_mb_check'
      and conrelid = 'public.blogs'::regclass
  ) then
    alter table public.blogs
      add constraint blogs_pdf_file_size_mb_check
      check (pdf_file_size_mb >= 0);
  end if;
end $$;

-- Keep ImgBB metadata for the submission hero and inline images when an
-- approved submission is converted to a Blog. Relationship IDs remain authoritative.
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
  set status = 'Approved',
      reviewed_at = timezone('utc', now()),
      converted_blog_id = created_blog.id
  where id = p_submission_id;

  return created_blog;
end;
$$;

revoke all on function public.approve_submission(uuid, uuid, text, text) from public;
grant execute on function public.approve_submission(uuid, uuid, text, text) to anon, authenticated;

-- Ask PostgREST to refresh its exposed column list immediately.
notify pgrst, 'reload schema';

commit;
