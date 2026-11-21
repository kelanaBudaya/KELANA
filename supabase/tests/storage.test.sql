begin;
create extension if not exists pgtap with schema extensions;
select plan(35);

-- ---- fixtures (as postgres) -------------------------------------------------
-- A (b001) holds a stamp for Tamansari only. B (b002) holds none.
insert into auth.users (instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
                        raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
                        confirmation_token, recovery_token, email_change_token_new, email_change)
values
 ('00000000-0000-0000-0000-000000000000','00000000-0000-0000-0000-00000000b001','authenticated','authenticated','a@test.invalid','',now(),'{}','{}',now(),now(),'','','',''),
 ('00000000-0000-0000-0000-000000000000','00000000-0000-0000-0000-00000000b002','authenticated','authenticated','b@test.invalid','',now(),'{}','{}',now(),now(),'','','','');
insert into public.stamps (user_id, place_id)
select '00000000-0000-0000-0000-00000000b001', id from public.places where slug = 'tamansari';

create function pg_temp.affected(q text) returns int language plpgsql as $$
declare n int; begin execute q; get diagnostics n = row_count; return n; end $$;
create function pg_temp.as_user(uid uuid) returns void language plpgsql as $$
begin
  perform set_config('request.jwt.claims',
    json_build_object('sub', uid, 'role', 'authenticated')::text, true);
end $$;
create function pg_temp.pid(s text) returns text language sql as
  $$ select id::text from public.places where slug = s $$;
grant execute on function pg_temp.affected(text), pg_temp.as_user(uuid), pg_temp.pid(text) to public;

-- existing objects (as postgres): A's postcards, A's report photo
insert into storage.objects (bucket_id, name, owner)
values ('postcards',     '00000000-0000-0000-0000-00000000b001/' || pg_temp.pid('tamansari') || '/seed.jpg', '00000000-0000-0000-0000-00000000b001'),
       ('postcards',     '00000000-0000-0000-0000-00000000b001/' || pg_temp.pid('tamansari') || '/todelete.jpg', '00000000-0000-0000-0000-00000000b001'),
       ('report-photos', '00000000-0000-0000-0000-00000000b001/seed.png', '00000000-0000-0000-0000-00000000b001');

-- ---- buckets ------------------------------------------------------------------
select is((select count(*)::int from storage.buckets where id in ('postcards','report-photos')), 2, 'both buckets exist');
select is((select count(*)::int from storage.buckets where id in ('postcards','report-photos') and public = false), 2, 'both buckets are private');
select is((select count(*)::int from storage.buckets where id in ('postcards','report-photos')
            and file_size_limit = 5242880
            and allowed_mime_types = array['image/jpeg','image/png','image/webp']), 2, 'limit 5 MB and jpeg/png/webp only');
select is((select count(*)::int from storage.buckets where name ilike '%video%'), 0, 'no video bucket');

-- ---- postcards: insert --------------------------------------------------------
set local role authenticated;
select pg_temp.as_user('00000000-0000-0000-0000-00000000b001');
select lives_ok($$insert into storage.objects (bucket_id, name, owner) values ('postcards',
  '00000000-0000-0000-0000-00000000b001/' || pg_temp.pid('tamansari') || '/new.jpg', '00000000-0000-0000-0000-00000000b001')$$,
  'postcards: upload into own folder with a stamp for the place');
select throws_ok($$insert into storage.objects (bucket_id, name, owner) values ('postcards',
  '00000000-0000-0000-0000-00000000b001/' || pg_temp.pid('kotagede') || '/x.jpg', '00000000-0000-0000-0000-00000000b001')$$,
  '42501', null, 'postcards: denied for a place without a stamp (own folder)');
select throws_ok($$insert into storage.objects (bucket_id, name, owner) values ('postcards',
  '00000000-0000-0000-0000-00000000b002/' || pg_temp.pid('tamansari') || '/x.jpg', '00000000-0000-0000-0000-00000000b001')$$,
  '42501', null, 'postcards: denied into another user''s folder');
select throws_ok($$insert into storage.objects (bucket_id, name, owner) values ('postcards',
  '00000000-0000-0000-0000-00000000b001/x.jpg', '00000000-0000-0000-0000-00000000b001')$$,
  '42501', null, 'postcards: denied without a place segment');
select throws_ok($$insert into storage.objects (bucket_id, name, owner) values ('postcards',
  'x.jpg', '00000000-0000-0000-0000-00000000b001')$$,
  '42501', null, 'postcards: denied at bucket root');
select throws_ok($$insert into storage.objects (bucket_id, name, owner) values ('postcards',
  '00000000-0000-0000-0000-00000000b001/not-a-place/x.jpg', '00000000-0000-0000-0000-00000000b001')$$,
  '42501', null, 'postcards: denied for a place id that does not exist');
select throws_ok($$insert into storage.objects (bucket_id, name, owner) values ('somewhere-else',
  '00000000-0000-0000-0000-00000000b001/' || pg_temp.pid('tamansari') || '/x.jpg', '00000000-0000-0000-0000-00000000b001')$$,
  '42501', null, 'unknown bucket: no policy applies, denied');
select pg_temp.as_user('00000000-0000-0000-0000-00000000b002');
select throws_ok($$insert into storage.objects (bucket_id, name, owner) values ('postcards',
  '00000000-0000-0000-0000-00000000b002/' || pg_temp.pid('tamansari') || '/x.jpg', '00000000-0000-0000-0000-00000000b002')$$,
  '42501', null, 'postcards: denied for a user with no stamp at all');

-- ---- postcards: read, update, delete -------------------------------------------
select is((select count(*)::int from storage.objects where bucket_id = 'postcards' and name like '%/seed.jpg'), 1,
          'postcards: another logged-in user can read A''s file');
select is(pg_temp.affected($$update storage.objects set metadata = '{"x":1}' where bucket_id = 'postcards' and name like '%/seed.jpg'$$), 0,
          'postcards: another user cannot update A''s file');
select is(pg_temp.affected($$delete from storage.objects where bucket_id = 'postcards' and name like '%/seed.jpg'$$), 0,
          'postcards: another user cannot delete A''s file');
select pg_temp.as_user('00000000-0000-0000-0000-00000000b001');
select is(pg_temp.affected($$update storage.objects set metadata = '{"x":1}' where bucket_id = 'postcards' and name like '%/seed.jpg'$$), 1,
          'postcards: owner can update own file');
select throws_ok($$update storage.objects set name = '00000000-0000-0000-0000-00000000b001/' || pg_temp.pid('kotagede') || '/seed.jpg'
  where bucket_id = 'postcards' and name like '%/seed.jpg'$$, '42501', null,
  'postcards: owner cannot move a file to an unstamped place');
select throws_ok($$update storage.objects set name = '00000000-0000-0000-0000-00000000b002/' || pg_temp.pid('tamansari') || '/seed.jpg'
  where bucket_id = 'postcards' and name like '%/seed.jpg'$$, '42501', null,
  'postcards: owner cannot move a file into another user''s folder');
select is(pg_temp.affected($$delete from storage.objects where bucket_id = 'postcards' and name like '%/todelete.jpg'$$), 1,
          'postcards: owner can delete own file');

-- ---- report-photos ---------------------------------------------------------------
select lives_ok($$insert into storage.objects (bucket_id, name, owner) values ('report-photos',
  '00000000-0000-0000-0000-00000000b001/new.png', '00000000-0000-0000-0000-00000000b001')$$,
  'report-photos: owner can insert into own folder');
select is((select count(*)::int from storage.objects where bucket_id = 'report-photos'), 2, 'report-photos: owner reads own files');
select throws_ok($$insert into storage.objects (bucket_id, name, owner) values ('report-photos',
  '00000000-0000-0000-0000-00000000b002/x.png', '00000000-0000-0000-0000-00000000b001')$$,
  '42501', null, 'report-photos: cannot insert into another user''s folder');
select throws_ok($$insert into storage.objects (bucket_id, name, owner) values ('report-photos',
  'x.png', '00000000-0000-0000-0000-00000000b001')$$,
  '42501', null, 'report-photos: cannot insert at bucket root');
select is(pg_temp.affected($$update storage.objects set metadata = '{"x":1}' where bucket_id = 'report-photos'$$), 0,
          'report-photos: no update policy, owner cannot update');
select is(pg_temp.affected($$delete from storage.objects where bucket_id = 'report-photos' and name like '%/new.png'$$), 1,
          'report-photos: owner can delete own file');
select pg_temp.as_user('00000000-0000-0000-0000-00000000b002');
select is((select count(*)::int from storage.objects where bucket_id = 'report-photos'), 0, 'report-photos: another user cannot read A''s files');
select is(pg_temp.affected($$delete from storage.objects where bucket_id = 'report-photos'$$), 0, 'report-photos: another user cannot delete A''s files');
select lives_ok($$insert into storage.objects (bucket_id, name, owner) values ('report-photos',
  '00000000-0000-0000-0000-00000000b002/mine.png', '00000000-0000-0000-0000-00000000b002')$$,
  'report-photos: B can insert into own folder (no stamp needed)');
select pg_temp.as_user('00000000-0000-0000-0000-00000000b001');
select is((select count(*)::int from storage.objects where bucket_id = 'report-photos' and name like '%b002%'), 0,
          'report-photos: A cannot read B''s files');
reset role;

-- ---- anonymous (guest) -----------------------------------------------------------
set local role anon;
select is((select count(*)::int from storage.objects), 0, 'anon cannot read any object');
select is((select count(*)::int from storage.objects where bucket_id = 'postcards'), 0, 'anon cannot read postcards');
select throws_ok($$insert into storage.objects (bucket_id, name) values ('postcards',
  '00000000-0000-0000-0000-00000000b001/' || pg_temp.pid('tamansari') || '/anon.jpg')$$,
  '42501', null, 'anon cannot upload to postcards');
select throws_ok($$insert into storage.objects (bucket_id, name) values ('report-photos',
  '00000000-0000-0000-0000-00000000b001/anon.png')$$,
  '42501', null, 'anon cannot upload to report-photos');
select is(pg_temp.affected($$delete from storage.objects$$), 0, 'anon cannot delete objects');
select is(pg_temp.affected($$update storage.objects set metadata = '{}'$$), 0, 'anon cannot update objects');
reset role;

select * from finish();
rollback;
