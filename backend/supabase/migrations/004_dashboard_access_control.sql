-- Ningshing Che Dashboard — database-backed users, roles, and menu access
-- Run after schema.sql (and after 003_blog_media_uploads.sql on older installs).
--
-- This migration replaces the discoverable demo request digest with expiring,
-- server-validated dashboard sessions. Passwords are hashed with bcrypt inside
-- PostgreSQL and raw session tokens are stored only in the signed-in browser.
-- It is additive and does not delete editorial content.

begin;

create extension if not exists pgcrypto;
set local search_path = public, extensions, pg_temp;

create table if not exists public.dashboard_roles (
  id uuid primary key default gen_random_uuid(),
  name text not null check (char_length(btrim(name)) between 2 and 80),
  slug text not null check (char_length(btrim(slug)) between 2 and 80),
  description text not null default '',
  menu_permissions text[] not null default '{}'::text[],
  is_system boolean not null default false,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create unique index if not exists dashboard_roles_name_lower_idx
  on public.dashboard_roles (lower(name));
create unique index if not exists dashboard_roles_slug_lower_idx
  on public.dashboard_roles (lower(slug));

create table if not exists public.dashboard_users (
  id uuid primary key default gen_random_uuid(),
  username text not null check (char_length(btrim(username)) between 3 and 40),
  display_name text not null check (char_length(btrim(display_name)) between 2 and 80),
  password_hash text not null,
  role_id uuid not null references public.dashboard_roles(id) on update cascade on delete restrict,
  is_active boolean not null default true,
  must_change_password boolean not null default true,
  failed_attempts integer not null default 0 check (failed_attempts >= 0),
  locked_until timestamptz,
  last_login_at timestamptz,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create unique index if not exists dashboard_users_username_lower_idx
  on public.dashboard_users (lower(username));
create index if not exists dashboard_users_role_idx on public.dashboard_users (role_id);

create table if not exists public.dashboard_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.dashboard_users(id) on update cascade on delete cascade,
  token_hash text not null,
  expires_at timestamptz not null,
  last_seen_at timestamptz not null default timezone('utc', now()),
  revoked_at timestamptz,
  user_agent text not null default '',
  created_at timestamptz not null default timezone('utc', now())
);

create unique index if not exists dashboard_sessions_token_hash_idx
  on public.dashboard_sessions (token_hash);
create index if not exists dashboard_sessions_user_expiry_idx
  on public.dashboard_sessions (user_id, expires_at desc);

-- The browser must never read password hashes or session hashes directly.
alter table public.dashboard_roles enable row level security;
alter table public.dashboard_users enable row level security;
alter table public.dashboard_sessions enable row level security;
revoke all on public.dashboard_roles, public.dashboard_users, public.dashboard_sessions from anon, authenticated;

-- Reuse the dashboard-wide updated_at helper without altering content tables.
drop trigger if exists dashboard_roles_set_updated_at on public.dashboard_roles;
create trigger dashboard_roles_set_updated_at
before update on public.dashboard_roles
for each row execute function public.set_updated_at();

drop trigger if exists dashboard_users_set_updated_at on public.dashboard_users;
create trigger dashboard_users_set_updated_at
before update on public.dashboard_users
for each row execute function public.set_updated_at();

-- Menu IDs are deliberately centralized so arbitrary values cannot be stored.
create or replace function public.dashboard_valid_permissions()
returns text[]
language sql
immutable
set search_path = public, pg_temp
as $$
  select array[
    'dashboard', 'authors', 'blogs', 'categories', 'comments', 'galleries',
    'books', 'submissions', 'videos', 'analytics', 'settings', 'access-control'
  ]::text[];
$$;

revoke all on function public.dashboard_valid_permissions() from public;

-- System roles are created once. Their menu selections can be customized by a
-- Super Admin except for the protected Super Admin role itself.
insert into public.dashboard_roles (name, slug, description, menu_permissions, is_system)
values
  (
    'Super Admin', 'super-admin',
    'Full dashboard, user, role, credential, and configuration access.',
    public.dashboard_valid_permissions(), true
  ),
  (
    'Administrator', 'administrator',
    'All editorial and settings menus except user and role administration.',
    array['dashboard','authors','blogs','categories','comments','galleries','books','submissions','videos','analytics','settings'], true
  ),
  (
    'Editor', 'editor',
    'Editorial content creation and publishing menus.',
    array['dashboard','authors','blogs','categories','galleries','books','submissions','videos'], true
  ),
  (
    'Moderator', 'moderator',
    'Comment and submitted-article moderation menus.',
    array['dashboard','comments','submissions'], true
  ),
  (
    'Analyzer', 'analyzer',
    'Dashboard and analytics visibility without editorial write menus.',
    array['dashboard','analytics'], true
  )
on conflict do nothing;

-- Preserve existing access-control users on re-run. A fresh installation gets
-- the same one-time credentials as the former demo login and immediately asks
-- the Super Admin to choose a private password.
insert into public.dashboard_users (
  username, display_name, password_hash, role_id, is_active, must_change_password
)
select
  'admin', 'Chief Editor', crypt('admin123', gen_salt('bf', 12)), r.id, true, true
from public.dashboard_roles r
where r.slug = 'super-admin'
  and not exists (select 1 from public.dashboard_users)
limit 1;

-- Safely read the custom session header supplied by the static dashboard.
create or replace function public.dashboard_request_session_token()
returns text
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
  request_headers jsonb;
begin
  begin
    request_headers := nullif(current_setting('request.headers', true), '')::jsonb;
  exception when others then
    request_headers := '{}'::jsonb;
  end;
  return coalesce(request_headers ->> 'x-dashboard-session', '');
end;
$$;

create or replace function public.dashboard_current_user_id()
returns uuid
language sql
stable
security definer
set search_path = public, extensions, pg_temp
as $$
  select s.user_id
  from public.dashboard_sessions s
  join public.dashboard_users u on u.id = s.user_id
  where s.token_hash = encode(digest(public.dashboard_request_session_token(), 'sha256'), 'hex')
    and s.revoked_at is null
    and s.expires_at > timezone('utc', now())
    and u.is_active
    and (u.locked_until is null or u.locked_until <= timezone('utc', now()))
  order by s.created_at desc
  limit 1;
$$;

create or replace function public.dashboard_has_any_permission(p_permissions text[])
returns boolean
language sql
stable
security definer
set search_path = public, extensions, pg_temp
as $$
  select exists (
    select 1
    from public.dashboard_sessions s
    join public.dashboard_users u on u.id = s.user_id
    join public.dashboard_roles r on r.id = u.role_id
    where s.token_hash = encode(digest(public.dashboard_request_session_token(), 'sha256'), 'hex')
      and s.revoked_at is null
      and s.expires_at > timezone('utc', now())
      and u.is_active
      and (u.locked_until is null or u.locked_until <= timezone('utc', now()))
      and (
        r.slug = 'super-admin'
        or r.menu_permissions && coalesce(p_permissions, '{}'::text[])
      )
  );
$$;

create or replace function public.dashboard_has_permission(p_permission text)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select public.dashboard_has_any_permission(array[coalesce(p_permission, '')]::text[]);
$$;

create or replace function public.dashboard_is_super_admin()
returns boolean
language sql
stable
security definer
set search_path = public, extensions, pg_temp
as $$
  select exists (
    select 1
    from public.dashboard_sessions s
    join public.dashboard_users u on u.id = s.user_id
    join public.dashboard_roles r on r.id = u.role_id
    where s.token_hash = encode(digest(public.dashboard_request_session_token(), 'sha256'), 'hex')
      and s.revoked_at is null
      and s.expires_at > timezone('utc', now())
      and u.is_active
      and (u.locked_until is null or u.locked_until <= timezone('utc', now()))
      and r.slug = 'super-admin'
  );
$$;

-- Existing content functions and Storage policies call this name. Replacing it
-- upgrades them from the public demo digest to a real expiring session.
create or replace function public.is_dashboard_request()
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select public.dashboard_current_user_id() is not null;
$$;

revoke all on function public.dashboard_request_session_token() from public;
revoke all on function public.dashboard_current_user_id() from public;
revoke all on function public.dashboard_has_any_permission(text[]) from public;
revoke all on function public.dashboard_has_permission(text) from public;
revoke all on function public.dashboard_is_super_admin() from public;
revoke all on function public.is_dashboard_request() from public;
grant execute on function public.dashboard_has_any_permission(text[]) to anon, authenticated;
grant execute on function public.dashboard_has_permission(text) to anon, authenticated;
grant execute on function public.is_dashboard_request() to anon, authenticated;

-- Upgrade the submission conversion RPC installed by schema.sql/migration 003
-- so opening the dashboard alone cannot authorize a direct approval call. The
-- function remains invoker-security; underlying Author, Blog, and Submission
-- writes must also pass their menu-specific RLS policies.
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
begin
  if not public.dashboard_has_permission('submissions') then
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

-- Return only browser-safe account data. Hashes never leave PostgreSQL.
create or replace function public.dashboard_user_payload(p_user_id uuid)
returns jsonb
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select jsonb_build_object(
    'id', u.id,
    'username', u.username,
    'name', u.display_name,
    'role_id', r.id,
    'role', r.name,
    'role_slug', r.slug,
    'permissions', case
      when r.slug = 'super-admin' then public.dashboard_valid_permissions()
      else r.menu_permissions
    end,
    'must_change_password', u.must_change_password,
    'is_active', u.is_active
  )
  from public.dashboard_users u
  join public.dashboard_roles r on r.id = u.role_id
  where u.id = p_user_id;
$$;

create or replace function public.dashboard_role_payload(p_role_id uuid)
returns jsonb
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select jsonb_build_object(
    'id', r.id,
    'name', r.name,
    'slug', r.slug,
    'description', r.description,
    'permissions', case
      when r.slug = 'super-admin' then public.dashboard_valid_permissions()
      else r.menu_permissions
    end,
    'is_system', r.is_system,
    'user_count', (select count(*) from public.dashboard_users u where u.role_id = r.id)
  )
  from public.dashboard_roles r
  where r.id = p_role_id;
$$;

revoke all on function public.dashboard_user_payload(uuid) from public;
revoke all on function public.dashboard_role_payload(uuid) from public;

-- Public login RPC. It returns a one-time raw token while only its SHA-256 hash
-- is retained in dashboard_sessions. Five failed attempts lock an account for
-- fifteen minutes; the failure update is returned rather than raised so it is
-- not rolled back by Postgres.
create or replace function public.dashboard_login(
  p_username text,
  p_password text,
  p_remember boolean,
  p_user_agent text
)
returns jsonb
language plpgsql
security definer
set search_path = public, extensions, pg_temp
as $$
declare
  selected_user public.dashboard_users%rowtype;
  raw_token text;
  token_digest text;
  session_expiry timestamptz;
  failures integer;
begin
  select * into selected_user
  from public.dashboard_users
  where lower(username) = lower(btrim(coalesce(p_username, '')))
  for update;

  if not found then
    -- Perform a comparable bcrypt operation to reduce username timing leaks.
    perform crypt(coalesce(p_password, ''), gen_salt('bf', 10));
    return jsonb_build_object('ok', false, 'error', 'The username or password is incorrect.');
  end if;

  if selected_user.locked_until is not null
     and selected_user.locked_until > timezone('utc', now()) then
    return jsonb_build_object(
      'ok', false,
      'error', 'Too many failed attempts. Try again after the temporary lock expires.',
      'locked_until', selected_user.locked_until
    );
  end if;

  if selected_user.password_hash <> crypt(coalesce(p_password, ''), selected_user.password_hash) then
    failures := case
      when selected_user.locked_until is not null
       and selected_user.locked_until <= timezone('utc', now()) then 1
      else selected_user.failed_attempts + 1
    end;
    update public.dashboard_users
    set failed_attempts = failures,
        locked_until = case
          when failures >= 5 then timezone('utc', now()) + interval '15 minutes'
          else null
        end
    where id = selected_user.id;
    return jsonb_build_object('ok', false, 'error', 'The username or password is incorrect.');
  end if;

  if not selected_user.is_active then
    return jsonb_build_object('ok', false, 'error', 'This dashboard account is disabled.');
  end if;

  raw_token := encode(gen_random_bytes(32), 'hex');
  token_digest := encode(digest(raw_token, 'sha256'), 'hex');
  session_expiry := timezone('utc', now()) + case
    when coalesce(p_remember, false) then interval '7 days'
    else interval '8 hours'
  end;

  delete from public.dashboard_sessions
  where user_id = selected_user.id
    and (
      expires_at <= timezone('utc', now())
      or (revoked_at is not null and revoked_at < timezone('utc', now()) - interval '7 days')
    );

  insert into public.dashboard_sessions (
    user_id, token_hash, expires_at, user_agent
  ) values (
    selected_user.id,
    token_digest,
    session_expiry,
    left(coalesce(p_user_agent, ''), 300)
  );

  update public.dashboard_users
  set failed_attempts = 0,
      locked_until = null,
      last_login_at = timezone('utc', now())
  where id = selected_user.id;

  return jsonb_build_object(
    'ok', true,
    'token', raw_token,
    'expires_at', session_expiry,
    'user', public.dashboard_user_payload(selected_user.id)
  );
end;
$$;

create or replace function public.dashboard_session()
returns jsonb
language plpgsql
security definer
set search_path = public, extensions, pg_temp
as $$
declare
  current_user_id uuid;
  current_token_hash text;
  session_expiry timestamptz;
begin
  current_user_id := public.dashboard_current_user_id();
  if current_user_id is null then
    return jsonb_build_object('ok', false, 'error', 'Session is invalid or expired.');
  end if;

  current_token_hash := encode(digest(public.dashboard_request_session_token(), 'sha256'), 'hex');
  update public.dashboard_sessions
  set last_seen_at = timezone('utc', now())
  where token_hash = current_token_hash
    and revoked_at is null
    and expires_at > timezone('utc', now())
    and exists (
      select 1 from public.dashboard_users u
      where u.id = dashboard_sessions.user_id
        and u.is_active
        and (u.locked_until is null or u.locked_until <= timezone('utc', now()))
    )
  returning expires_at into session_expiry;
  if not found then
    return jsonb_build_object('ok', false, 'error', 'Session is invalid or expired.');
  end if;

  return jsonb_build_object(
    'ok', true,
    'expires_at', session_expiry,
    'user', public.dashboard_user_payload(current_user_id)
  );
end;
$$;

create or replace function public.dashboard_logout()
returns boolean
language plpgsql
security definer
set search_path = public, extensions, pg_temp
as $$
begin
  update public.dashboard_sessions
  set revoked_at = timezone('utc', now())
  where token_hash = encode(digest(public.dashboard_request_session_token(), 'sha256'), 'hex')
    and revoked_at is null;
  return found;
end;
$$;

-- Super Admin snapshot for the new Users & Roles menu.
create or replace function public.dashboard_access_snapshot()
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  role_rows jsonb;
  user_rows jsonb;
  active_sessions bigint;
begin
  if not public.dashboard_is_super_admin() then
    raise exception 'Super Admin authorization required.' using errcode = '42501';
  end if;

  select coalesce(jsonb_agg(item order by is_system desc, name), '[]'::jsonb)
  into role_rows
  from (
    select
      r.is_system,
      r.name,
      public.dashboard_role_payload(r.id) as item
    from public.dashboard_roles r
  ) roles;

  select coalesce(jsonb_agg(item order by created_at, username), '[]'::jsonb)
  into user_rows
  from (
    select
      u.created_at,
      u.username,
      jsonb_build_object(
        'id', u.id,
        'username', u.username,
        'name', u.display_name,
        'role_id', r.id,
        'role', r.name,
        'role_slug', r.slug,
        'is_active', u.is_active,
        'must_change_password', u.must_change_password,
        'last_login_at', u.last_login_at,
        'created_at', u.created_at
      ) as item
    from public.dashboard_users u
    join public.dashboard_roles r on r.id = u.role_id
  ) users;

  select count(*) into active_sessions
  from public.dashboard_sessions s
  join public.dashboard_users u on u.id = s.user_id
  where s.revoked_at is null
    and s.expires_at > timezone('utc', now())
    and u.is_active
    and (u.locked_until is null or u.locked_until <= timezone('utc', now()));

  return jsonb_build_object(
    'roles', role_rows,
    'users', user_rows,
    'active_sessions', active_sessions,
    'valid_permissions', public.dashboard_valid_permissions()
  );
end;
$$;

create or replace function public.dashboard_save_role(
  p_role_id uuid,
  p_name text,
  p_description text,
  p_menu_permissions text[]
)
returns jsonb
language plpgsql
security definer
set search_path = public, extensions, pg_temp
as $$
declare
  selected_role public.dashboard_roles%rowtype;
  clean_name text;
  clean_slug text;
  clean_permissions text[];
  saved_id uuid;
begin
  if not public.dashboard_is_super_admin() then
    raise exception 'Super Admin authorization required.' using errcode = '42501';
  end if;

  clean_name := btrim(coalesce(p_name, ''));
  if char_length(clean_name) < 2 or char_length(clean_name) > 80 then
    raise exception 'Role name must contain between 2 and 80 characters.' using errcode = '22023';
  end if;

  select coalesce(array_agg(permission order by first_position), '{}'::text[])
  into clean_permissions
  from (
    select permission, min(position) as first_position
    from unnest(coalesce(p_menu_permissions, '{}'::text[])) with ordinality requested(permission, position)
    where permission = any(public.dashboard_valid_permissions())
      and permission <> 'access-control'
    group by permission
  ) allowed;

  if not ('dashboard' = any(clean_permissions)) then
    clean_permissions := array_prepend('dashboard', clean_permissions);
  end if;

  if p_role_id is null then
    clean_slug := trim(both '-' from regexp_replace(lower(clean_name), '[^a-z0-9]+', '-', 'g'));
    if clean_slug = '' then
      clean_slug := 'role-' || left(replace(gen_random_uuid()::text, '-', ''), 10);
    end if;
    insert into public.dashboard_roles (
      name, slug, description, menu_permissions, is_system
    ) values (
      clean_name, clean_slug, btrim(coalesce(p_description, '')), clean_permissions, false
    ) returning id into saved_id;
  else
    select * into selected_role
    from public.dashboard_roles
    where id = p_role_id
    for update;

    if not found then
      raise exception 'Role not found.' using errcode = 'P0002';
    end if;
    if selected_role.slug = 'super-admin' then
      raise exception 'The Super Admin role is protected and always has full access.' using errcode = '42501';
    end if;

    update public.dashboard_roles
    set name = clean_name,
        description = btrim(coalesce(p_description, '')),
        menu_permissions = clean_permissions
    where id = selected_role.id
    returning id into saved_id;

    -- Permission changes take effect immediately at the database and force
    -- affected users to sign in again so their visible menus cannot stay stale.
    update public.dashboard_sessions
    set revoked_at = timezone('utc', now())
    where user_id in (
      select u.id from public.dashboard_users u where u.role_id = selected_role.id
    ) and revoked_at is null;
  end if;

  return public.dashboard_role_payload(saved_id);
exception
  when unique_violation then
    raise exception 'A role with this name already exists.' using errcode = '23505';
end;
$$;

create or replace function public.dashboard_delete_role(p_role_id uuid)
returns boolean
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  selected_role public.dashboard_roles%rowtype;
begin
  if not public.dashboard_is_super_admin() then
    raise exception 'Super Admin authorization required.' using errcode = '42501';
  end if;

  select * into selected_role from public.dashboard_roles where id = p_role_id for update;
  if not found then
    raise exception 'Role not found.' using errcode = 'P0002';
  end if;
  if selected_role.is_system then
    raise exception 'System roles cannot be deleted.' using errcode = '42501';
  end if;
  if exists (select 1 from public.dashboard_users where role_id = selected_role.id) then
    raise exception 'Move users to another role before deleting this role.' using errcode = '23503';
  end if;

  delete from public.dashboard_roles where id = selected_role.id;
  return true;
end;
$$;

create or replace function public.dashboard_save_user(
  p_user_id uuid,
  p_username text,
  p_display_name text,
  p_role_id uuid,
  p_password text,
  p_is_active boolean
)
returns jsonb
language plpgsql
security definer
set search_path = public, extensions, pg_temp
as $$
declare
  actor_id uuid;
  selected_user public.dashboard_users%rowtype;
  selected_role public.dashboard_roles%rowtype;
  old_role_slug text;
  clean_username text;
  clean_name text;
  clean_password text;
  saved_id uuid;
  security_changed boolean;
  active_super_admins bigint;
begin
  if not public.dashboard_is_super_admin() then
    raise exception 'Super Admin authorization required.' using errcode = '42501';
  end if;

  -- Serialize role/status changes so concurrent requests cannot remove every
  -- active Super Admin after both observe the same pre-change count. Recheck
  -- authorization after waiting because another request may have disabled or
  -- demoted this actor while it was blocked on the advisory lock.
  perform pg_advisory_xact_lock(hashtext('ningshing-dashboard-super-admin-guard'));
  if not public.dashboard_is_super_admin() then
    raise exception 'Super Admin authorization expired while waiting.' using errcode = '42501';
  end if;

  actor_id := public.dashboard_current_user_id();
  clean_username := lower(btrim(coalesce(p_username, '')));
  clean_name := btrim(coalesce(p_display_name, ''));
  clean_password := coalesce(p_password, '');

  if char_length(clean_username) < 3 or char_length(clean_username) > 40
     or clean_username ~ '[[:space:]/:]' then
    raise exception 'Username must be 3–40 characters without spaces, slashes, or colons.' using errcode = '22023';
  end if;
  if char_length(clean_name) < 2 or char_length(clean_name) > 80 then
    raise exception 'Display name must contain between 2 and 80 characters.' using errcode = '22023';
  end if;

  select * into selected_role from public.dashboard_roles where id = p_role_id;
  if not found then
    raise exception 'Choose a valid role.' using errcode = '22023';
  end if;

  if p_user_id is null then
    if char_length(clean_password) < 8 then
      raise exception 'A new password must contain at least 8 characters.' using errcode = '22023';
    end if;
    insert into public.dashboard_users (
      username, display_name, password_hash, role_id, is_active, must_change_password
    ) values (
      clean_username, clean_name, crypt(clean_password, gen_salt('bf', 12)),
      selected_role.id, coalesce(p_is_active, true), true
    ) returning id into saved_id;
  else
    select * into selected_user
    from public.dashboard_users
    where id = p_user_id
    for update;

    if not found then
      raise exception 'Dashboard user not found.' using errcode = 'P0002';
    end if;
    if selected_user.id = actor_id then
      raise exception 'Use My login to update your own username or password.' using errcode = '42501';
    end if;
    if clean_password <> '' and char_length(clean_password) < 8 then
      raise exception 'A replacement password must contain at least 8 characters.' using errcode = '22023';
    end if;

    select r.slug into old_role_slug from public.dashboard_roles r where r.id = selected_user.role_id;
    if old_role_slug = 'super-admin'
       and (selected_role.slug <> 'super-admin' or not coalesce(p_is_active, true)) then
      select count(*) into active_super_admins
      from public.dashboard_users u
      join public.dashboard_roles r on r.id = u.role_id
      where u.is_active and r.slug = 'super-admin';
      if active_super_admins <= 1 then
        raise exception 'At least one active Super Admin must remain.' using errcode = '42501';
      end if;
    end if;

    security_changed :=
      lower(selected_user.username) is distinct from clean_username
      or selected_user.role_id is distinct from selected_role.id
      or selected_user.is_active is distinct from coalesce(p_is_active, true)
      or clean_password <> '';

    update public.dashboard_users
    set username = clean_username,
        display_name = clean_name,
        role_id = selected_role.id,
        is_active = coalesce(p_is_active, true),
        password_hash = case
          when clean_password <> '' then crypt(clean_password, gen_salt('bf', 12))
          else password_hash
        end,
        must_change_password = case
          when clean_password <> '' then true
          else must_change_password
        end,
        failed_attempts = case when clean_password <> '' then 0 else failed_attempts end,
        locked_until = case when clean_password <> '' then null else locked_until end
    where id = selected_user.id
    returning id into saved_id;

    if security_changed then
      update public.dashboard_sessions
      set revoked_at = timezone('utc', now())
      where user_id = selected_user.id and revoked_at is null;
    end if;
  end if;

  return public.dashboard_user_payload(saved_id);
exception
  when unique_violation then
    raise exception 'That username is already in use.' using errcode = '23505';
end;
$$;

create or replace function public.dashboard_delete_user(p_user_id uuid)
returns boolean
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  actor_id uuid;
  selected_user public.dashboard_users%rowtype;
  role_slug text;
  active_super_admins bigint;
begin
  if not public.dashboard_is_super_admin() then
    raise exception 'Super Admin authorization required.' using errcode = '42501';
  end if;

  perform pg_advisory_xact_lock(hashtext('ningshing-dashboard-super-admin-guard'));
  if not public.dashboard_is_super_admin() then
    raise exception 'Super Admin authorization expired while waiting.' using errcode = '42501';
  end if;
  actor_id := public.dashboard_current_user_id();
  select * into selected_user from public.dashboard_users where id = p_user_id for update;
  if not found then
    raise exception 'Dashboard user not found.' using errcode = 'P0002';
  end if;
  if selected_user.id = actor_id then
    raise exception 'You cannot delete your own signed-in account.' using errcode = '42501';
  end if;

  select r.slug into role_slug from public.dashboard_roles r where r.id = selected_user.role_id;
  if role_slug = 'super-admin' and selected_user.is_active then
    select count(*) into active_super_admins
    from public.dashboard_users u
    join public.dashboard_roles r on r.id = u.role_id
    where u.is_active and r.slug = 'super-admin';
    if active_super_admins <= 1 then
      raise exception 'At least one active Super Admin must remain.' using errcode = '42501';
    end if;
  end if;

  delete from public.dashboard_users where id = selected_user.id;
  return true;
end;
$$;

-- Any signed-in user can securely change their own username/display name and
-- password. Super Admins reach this from both the user menu and Access Control.
create or replace function public.dashboard_update_own_credentials(
  p_current_password text,
  p_username text,
  p_display_name text,
  p_new_password text
)
returns jsonb
language plpgsql
security definer
set search_path = public, extensions, pg_temp
as $$
declare
  current_user_id uuid;
  selected_user public.dashboard_users%rowtype;
  clean_username text;
  clean_name text;
  clean_password text;
  current_token_hash text;
begin
  current_user_id := public.dashboard_current_user_id();
  if current_user_id is null then
    raise exception 'Your session is invalid or expired.' using errcode = '42501';
  end if;

  select * into selected_user
  from public.dashboard_users
  where id = current_user_id
  for update;
  if not found or not selected_user.is_active
     or (selected_user.locked_until is not null and selected_user.locked_until > timezone('utc', now())) then
    raise exception 'Your dashboard account is no longer active.' using errcode = '42501';
  end if;

  if selected_user.password_hash <> crypt(coalesce(p_current_password, ''), selected_user.password_hash) then
    raise exception 'Current password is incorrect.' using errcode = '42501';
  end if;

  clean_username := lower(btrim(coalesce(p_username, '')));
  clean_name := btrim(coalesce(p_display_name, ''));
  clean_password := coalesce(p_new_password, '');

  if char_length(clean_username) < 3 or char_length(clean_username) > 40
     or clean_username ~ '[[:space:]/:]' then
    raise exception 'Username must be 3–40 characters without spaces, slashes, or colons.' using errcode = '22023';
  end if;
  if char_length(clean_name) < 2 or char_length(clean_name) > 80 then
    raise exception 'Display name must contain between 2 and 80 characters.' using errcode = '22023';
  end if;
  if selected_user.must_change_password and char_length(clean_password) < 8 then
    raise exception 'Choose a new password containing at least 8 characters.' using errcode = '22023';
  end if;
  if clean_password <> '' and char_length(clean_password) < 8 then
    raise exception 'New password must contain at least 8 characters.' using errcode = '22023';
  end if;

  update public.dashboard_users
  set username = clean_username,
      display_name = clean_name,
      password_hash = case
        when clean_password <> '' then crypt(clean_password, gen_salt('bf', 12))
        else password_hash
      end,
      must_change_password = case
        when clean_password <> '' then false
        else must_change_password
      end,
      failed_attempts = 0,
      locked_until = null
  where id = selected_user.id;

  if lower(selected_user.username) is distinct from clean_username or clean_password <> '' then
    current_token_hash := encode(digest(public.dashboard_request_session_token(), 'sha256'), 'hex');
    update public.dashboard_sessions
    set revoked_at = timezone('utc', now())
    where user_id = selected_user.id
      and token_hash <> current_token_hash
      and revoked_at is null;
  end if;

  return jsonb_build_object(
    'ok', true,
    'user', public.dashboard_user_payload(selected_user.id)
  );
exception
  when unique_violation then
    raise exception 'That username is already in use.' using errcode = '23505';
end;
$$;

-- RPC functions are the only browser entry points to private access data.
revoke all on function public.dashboard_login(text, text, boolean, text) from public;
revoke all on function public.dashboard_session() from public;
revoke all on function public.dashboard_logout() from public;
revoke all on function public.dashboard_access_snapshot() from public;
revoke all on function public.dashboard_save_role(uuid, text, text, text[]) from public;
revoke all on function public.dashboard_delete_role(uuid) from public;
revoke all on function public.dashboard_save_user(uuid, text, text, uuid, text, boolean) from public;
revoke all on function public.dashboard_delete_user(uuid) from public;
revoke all on function public.dashboard_update_own_credentials(text, text, text, text) from public;

grant execute on function public.dashboard_login(text, text, boolean, text) to anon, authenticated;
grant execute on function public.dashboard_session() to anon, authenticated;
grant execute on function public.dashboard_logout() to anon, authenticated;
grant execute on function public.dashboard_access_snapshot() to anon, authenticated;
grant execute on function public.dashboard_save_role(uuid, text, text, text[]) to anon, authenticated;
grant execute on function public.dashboard_delete_role(uuid) to anon, authenticated;
grant execute on function public.dashboard_save_user(uuid, text, text, uuid, text, boolean) to anon, authenticated;
grant execute on function public.dashboard_delete_user(uuid) to anon, authenticated;
grant execute on function public.dashboard_update_own_credentials(text, text, text, text) to anon, authenticated;

-- Replace broad demo dashboard policies with permission-aware policies.
-- A matching menu or Analytics can read private rows. Writes require the
-- matching visible menu, and public website policies remain unchanged.
do $$
declare
  item record;
begin
  for item in
    select * from (values
      ('authors', 'authors'),
      ('categories', 'categories'),
      ('blogs', 'blogs'),
      ('comments', 'comments'),
      ('galleries', 'galleries'),
      ('pdf_books', 'books'),
      ('submitted_blogs', 'submissions'),
      ('videos', 'videos'),
      ('settings', 'settings')
    ) as routes(table_name, permission_name)
  loop
    execute format('drop policy if exists %I on public.%I', item.table_name || '_dashboard_all', item.table_name);
    execute format('drop policy if exists %I on public.%I', item.table_name || '_dashboard_select', item.table_name);
    execute format('drop policy if exists %I on public.%I', item.table_name || '_dashboard_insert', item.table_name);
    execute format('drop policy if exists %I on public.%I', item.table_name || '_dashboard_update', item.table_name);
    execute format('drop policy if exists %I on public.%I', item.table_name || '_dashboard_delete', item.table_name);

    execute format(
      'create policy %I on public.%I for select to anon, authenticated using (public.dashboard_has_any_permission(array[%L,%L]::text[]))',
      item.table_name || '_dashboard_select', item.table_name,
      item.permission_name, 'analytics'
    );
    execute format(
      'create policy %I on public.%I for insert to anon, authenticated with check (public.dashboard_has_permission(%L))',
      item.table_name || '_dashboard_insert', item.table_name, item.permission_name
    );
    execute format(
      'create policy %I on public.%I for update to anon, authenticated using (public.dashboard_has_permission(%L)) with check (public.dashboard_has_permission(%L))',
      item.table_name || '_dashboard_update', item.table_name,
      item.permission_name, item.permission_name
    );
    execute format(
      'create policy %I on public.%I for delete to anon, authenticated using (public.dashboard_has_permission(%L))',
      item.table_name || '_dashboard_delete', item.table_name, item.permission_name
    );
  end loop;
end $$;

-- PDF uploads belong only to roles with the Blogs or PDF Books menu.
drop policy if exists pdf_books_dashboard_storage_insert on storage.objects;
create policy pdf_books_dashboard_storage_insert on storage.objects
for insert to anon, authenticated
with check (
  bucket_id = 'pdf-books'
  and public.dashboard_has_any_permission(array['blogs','books']::text[])
);

drop policy if exists pdf_books_dashboard_storage_update on storage.objects;
create policy pdf_books_dashboard_storage_update on storage.objects
for update to anon, authenticated
using (
  bucket_id = 'pdf-books'
  and public.dashboard_has_any_permission(array['blogs','books']::text[])
)
with check (
  bucket_id = 'pdf-books'
  and public.dashboard_has_any_permission(array['blogs','books']::text[])
);

drop policy if exists pdf_books_dashboard_storage_delete on storage.objects;
create policy pdf_books_dashboard_storage_delete on storage.objects
for delete to anon, authenticated
using (
  bucket_id = 'pdf-books'
  and public.dashboard_has_any_permission(array['blogs','books']::text[])
);

notify pgrst, 'reload schema';

commit;
