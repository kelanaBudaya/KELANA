begin;
create extension if not exists pgtap with schema extensions;
select plan(24);

-- Hapus akun: what deleting an auth user (auth.admin.deleteUser in the `delete-account` Edge Function)
-- does to the user's rows. Storage objects are removed by the function through the Storage API and are
-- covered by functions/delete-account/delete_account_test.ts, not here.

-- ---- fixtures (as postgres) -------------------------------------------------
-- C (c001) is deleted. D (c002) stays and must be untouched.
insert into auth.users (instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
                        raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
                        confirmation_token, recovery_token, email_change_token_new, email_change)
values
 ('00000000-0000-0000-0000-000000000000','00000000-0000-0000-0000-00000000c001','authenticated','authenticated','c@test.invalid','',now(),'{}','{}',now(),now(),'','','',''),
 ('00000000-0000-0000-0000-000000000000','00000000-0000-0000-0000-00000000c002','authenticated','authenticated','d@test.invalid','',now(),'{}','{}',now(),now(),'','','','');

create function pg_temp.pid(s text) returns uuid language sql as
  $$ select id from public.places where slug = s $$;
create function pg_temp.as_user(uid uuid) returns void language plpgsql as $$
begin
  perform set_config('request.jwt.claims',
    json_build_object('sub', uid, 'role', 'authenticated')::text, true);
end $$;
grant execute on function pg_temp.pid(text), pg_temp.as_user(uuid) to public;

-- C: two stamps, a public and a private postcard, a video, a subscription and a report.
insert into public.stamps (user_id, place_id) values
 ('00000000-0000-0000-0000-00000000c001', pg_temp.pid('tamansari')),
 ('00000000-0000-0000-0000-00000000c001', pg_temp.pid('kotagede'));
insert into public.postcards (id, user_id, place_id, message, is_public) values
 ('00000000-0000-0000-0000-00000000f001', '00000000-0000-0000-0000-00000000c001', pg_temp.pid('tamansari'), 'publik', true),
 ('00000000-0000-0000-0000-00000000f002', '00000000-0000-0000-0000-00000000c001', pg_temp.pid('tamansari'), 'privat', false);
insert into public.videos (id, user_id, place_id, caption) values
 ('00000000-0000-0000-0000-00000000f003', '00000000-0000-0000-0000-00000000c001', pg_temp.pid('tamansari'), 'video C');
insert into public.subscriptions (user_id, category) values ('00000000-0000-0000-0000-00000000c001', 'museum');
insert into public.reports (id, user_id, place_id, reason, details) values
 ('00000000-0000-0000-0000-00000000f004', '00000000-0000-0000-0000-00000000c001', pg_temp.pid('tamansari'), 'place_closed', 'Tutup');

-- D: a stamp, a postcard, a video, a subscription and a report of their own.
insert into public.stamps (user_id, place_id) values ('00000000-0000-0000-0000-00000000c002', pg_temp.pid('tamansari'));
insert into public.postcards (id, user_id, place_id, message) values
 ('00000000-0000-0000-0000-00000000f101', '00000000-0000-0000-0000-00000000c002', pg_temp.pid('tamansari'), 'punya D');
insert into public.videos (id, user_id, place_id) values
 ('00000000-0000-0000-0000-00000000f102', '00000000-0000-0000-0000-00000000c002', pg_temp.pid('tamansari'));
insert into public.subscriptions (user_id, category) values ('00000000-0000-0000-0000-00000000c002', 'museum');
insert into public.reports (id, user_id, place_id, reason) values
 ('00000000-0000-0000-0000-00000000f104', '00000000-0000-0000-0000-00000000c002', pg_temp.pid('kotagede'), 'other');

-- ---- foreign keys ---------------------------------------------------------------
select is((select confdeltype::text from pg_constraint
            where conrelid = 'public.reports'::regclass and contype = 'f' and confrelid = 'auth.users'::regclass),
          'n', 'reports.user_id is ON DELETE SET NULL');
select is((select attnotnull from pg_attribute
            where attrelid = 'public.reports'::regclass and attname = 'user_id'),
          false, 'reports.user_id is nullable');
select is((select count(*)::int from pg_constraint
            where conrelid = 'public.reports'::regclass and contype = 'f' and confrelid = 'auth.users'::regclass),
          1, 'exactly one FK from reports to auth.users');
select is((select string_agg(conrelid::regclass::text || '=' || confdeltype::text, ',' order by conrelid::regclass::text)
             from pg_constraint
            where contype = 'f' and confrelid = 'auth.users'::regclass
              and conrelid in ('public.profiles'::regclass, 'public.stamps'::regclass, 'public.postcards'::regclass,
                               'public.videos'::regclass, 'public.subscriptions'::regclass)),
          'postcards=c,profiles=c,stamps=c,subscriptions=c,videos=c',
          'profiles, stamps, postcards, videos and subscriptions cascade from auth.users');

-- ---- RLS still holds for a null user_id (before deletion) -----------------------
set local role authenticated;
select pg_temp.as_user('00000000-0000-0000-0000-00000000c001');
select throws_ok($$insert into public.reports (user_id, place_id, reason)
  values (null, pg_temp.pid('tamansari'), 'other')$$,
  '42501', null, 'a client cannot file a report without a user');
reset role;

-- ---- delete C (what auth.admin.deleteUser does in the database) ----------------
select lives_ok($$delete from auth.users where id = '00000000-0000-0000-0000-00000000c001'$$,
                'deleting the auth user succeeds with all of C''s rows present');

select is((select count(*)::int from public.profiles where id = '00000000-0000-0000-0000-00000000c001'), 0,
          'C''s profile is deleted');
select is((select count(*)::int from public.stamps where user_id = '00000000-0000-0000-0000-00000000c001'), 0,
          'C''s stamps are deleted');
select is((select count(*)::int from public.postcards
            where id in ('00000000-0000-0000-0000-00000000f001', '00000000-0000-0000-0000-00000000f002')), 0,
          'C''s postcards are deleted, public and private');
select is((select count(*)::int from public.videos where id = '00000000-0000-0000-0000-00000000f003'), 0,
          'C''s video is deleted');
select is((select count(*)::int from public.subscriptions where user_id = '00000000-0000-0000-0000-00000000c001'), 0,
          'C''s subscriptions are deleted');
select is((select count(*)::int from public.reports where id = '00000000-0000-0000-0000-00000000f004'), 1,
          'C''s report is kept');
select ok((select user_id is null from public.reports where id = '00000000-0000-0000-0000-00000000f004'),
          'C''s report no longer points at the user');
select is((select details from public.reports where id = '00000000-0000-0000-0000-00000000f004'), 'Tutup',
          'C''s report content is unchanged');

-- D is untouched.
select is((select count(*)::int from auth.users where id = '00000000-0000-0000-0000-00000000c002'), 1, 'D still exists');
select is((select count(*)::int from public.profiles where id = '00000000-0000-0000-0000-00000000c002'), 1, 'D''s profile stays');
select is((select count(*)::int from public.stamps where user_id = '00000000-0000-0000-0000-00000000c002'), 1, 'D''s stamp stays');
select is((select count(*)::int from public.postcards where id = '00000000-0000-0000-0000-00000000f101'), 1, 'D''s postcard stays');
select is((select count(*)::int from public.videos where id = '00000000-0000-0000-0000-00000000f102'), 1, 'D''s video stays');
select is((select count(*)::int from public.subscriptions where user_id = '00000000-0000-0000-0000-00000000c002'), 1,
          'D''s subscription stays');
select is((select user_id::text from public.reports where id = '00000000-0000-0000-0000-00000000f104'),
          '00000000-0000-0000-0000-00000000c002', 'D''s report keeps its user');

-- ---- the orphaned report is invisible to every client --------------------------
set local role authenticated;
select pg_temp.as_user('00000000-0000-0000-0000-00000000c002');
select is((select count(*)::int from public.reports where id = '00000000-0000-0000-0000-00000000f004'), 0,
          'another user cannot read the orphaned report');
select is((select count(*)::int from public.reports), 1, 'D still reads exactly their own report');
reset role;

set local role anon;
select throws_ok('select * from public.reports', '42501', null, 'anon still cannot read reports');
reset role;

select * from finish();
rollback;
