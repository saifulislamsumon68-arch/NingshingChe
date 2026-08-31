-- Ningshing Che Dashboard — LEGACY Supabase Auth hardening (pre-RBAC)
-- Do NOT run this migration after 004_dashboard_access_control.sql: migration
-- 004 replaces this fixed-role helper with database users, dynamic roles, and
-- expiring dashboard sessions. This file is retained only for older installs.
--
-- Apply ONLY when intentionally using the older Supabase Auth design, after
-- creating Auth users and adding administrator, editor, or moderator to
-- app_metadata.role.
--
-- This removes the demo x-dashboard-token authorization path. Existing RLS
-- policies continue to call is_dashboard_request(), so no content is dropped.

begin;

do $$
begin
  if to_regprocedure('public.dashboard_current_user_id()') is not null then
    raise exception 'Migration 002 is legacy-only and cannot run after migration 004.'
      using errcode = '55000';
  end if;
end $$;

create or replace function public.is_dashboard_request()
returns boolean
language sql
stable
security definer
set search_path = public, auth
as $$
  select
    auth.uid() is not null
    and lower(coalesce(
      auth.jwt() -> 'app_metadata' ->> 'role',
      auth.jwt() -> 'user_metadata' ->> 'role',
      ''
    )) in ('administrator', 'admin', 'editor', 'moderator');
$$;

revoke all on function public.is_dashboard_request() from public;
grant execute on function public.is_dashboard_request() to anon, authenticated;

commit;
