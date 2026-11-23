-- Hapus akun (account deletion, Edge Function `delete-account`).
--
-- The function deletes the auth user with the service role. What happens to the user's rows is decided here,
-- in the foreign keys, so it holds whoever deletes the user (function, dashboard or admin API):
--   profiles, stamps, postcards (public and private), videos, subscriptions: already `on delete cascade`
--     from auth.users (migration 20261121000001), nothing to change.
--   reports the user sent: KEPT for the admin, without the user ("Tetap tersimpan, tanpa namamu").
--     Until now `reports.user_id` was `not null ... on delete cascade`, which deleted them. This migration
--     makes it nullable with `on delete set null`.
-- Plans and saved needs are not stored on the server (the app keeps them on the device), so there is nothing
-- to delete for them here. Postcard photos in Storage are removed by the function through the Storage API.
--
-- RLS is unchanged: an orphaned report (user_id null) matches no `user_id = auth.uid()` policy, so no client
-- can read, edit or delete it, and a client still cannot insert a report with a null user_id.

do $$
declare c record;
begin
  -- Drop the existing FK from reports.user_id to auth.users, whatever it was named.
  for c in
    select con.conname
      from pg_constraint con
     where con.conrelid = 'public.reports'::regclass
       and con.contype = 'f'
       and con.confrelid = 'auth.users'::regclass
  loop
    execute format('alter table public.reports drop constraint %I', c.conname);
  end loop;
end $$;

alter table public.reports alter column user_id drop not null;

alter table public.reports
  add constraint reports_user_id_fkey
  foreign key (user_id) references auth.users (id) on delete set null;
