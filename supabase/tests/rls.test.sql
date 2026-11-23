begin;
create extension if not exists pgtap with schema extensions;
select plan(109);

-- ---- fixtures (as postgres) -------------------------------------------------
-- A holds a stamp for Tamansari only. B holds no stamps.
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

-- pending facts and a fact with no checked_at (as postgres)
insert into public.opening_hours (place_id, weekday, opens_at, closes_at, review_status, is_sample)
select id, 2, '08:00', '16:00', 'pending', true from public.places where slug = 'tamansari';
insert into public.closures (place_id, starts_on, ends_on, review_status, is_sample)
select id, '2026-12-01', '2026-12-02', 'pending', true from public.places where slug = 'tamansari';
insert into public.events (place_id, title, recurrence, weekday, start_time, review_status, is_sample)
select id, 'Pending pentas', 'weekly', 3, '10:00', 'pending', true from public.places where slug = 'tamansari';
insert into public.ticket_prices (place_id, label, price_idr, checked_at, review_status, is_sample)
select id, 'Tanpa tanggal', 12000, null, 'published', true from public.places where slug = 'kotagede';

-- ---- structure ---------------------------------------------------------------
select is((select count(*)::int from pg_tables t where schemaname = 'public' and not rowsecurity), 0,
          'every public table has RLS enabled');
select is((select count(*)::int from public.places), 10, 'seed has 10 places');
-- ---- lat/lng ------------------------------------------------------------------
select is((select count(*)::int from public.places where lat is null or lng is null), 0,
          'every place exposes numeric lat and lng');
select ok((select bool_and(lat between -11 and 6 and lng between 95 and 141) from public.places),
          'lat/lng fall inside Indonesia');
select is((select count(*)::int from public.places
            where abs(lat - extensions.st_y(location::extensions.geometry)) > 1e-9
               or abs(lng - extensions.st_x(location::extensions.geometry)) > 1e-9), 0,
          'lat/lng match the stored location');

select is((select string_agg(cap_code, ',' order by passport_order) from public.places),
          'YK-01,YK-02,YK-03,YK-04,YK-05,YK-06,YK-07,YK-08,YK-09,YK-10', 'cap codes in passport order');
select is((select count(*)::int from public.regions where status = 'upcoming'), 2, '2 upcoming regions');
select is((select count(*)::int from public.regions where status = 'not_open'), 7, '7 not_open regions');
select throws_ok($$insert into public.stamps (user_id, place_id)
  select '00000000-0000-0000-0000-00000000b001', id from public.places where slug = 'tamansari'$$,
  '23505', null, 'stamps are unique per (user, place)');
select throws_ok($$insert into public.subscriptions (user_id, category, place_id)
  select '00000000-0000-0000-0000-00000000b001', 'museum', id from public.places limit 1$$,
  '23514', null, 'subscription needs exactly one of category/place');
select throws_ok($$insert into public.events (place_id, title, recurrence, start_time)
  select id, 'x', 'weekly', '10:00' from public.places limit 1$$,
  '23514', null, 'weekly event requires weekday');
select throws_ok($$insert into public.events (place_id, title, recurrence, start_time, arrival_advice_min) select id, 'x', 'daily', '10:00', 30 from public.places limit 1$$,
  '23514', null, 'arrival advice needs both min and max (min only)');
select throws_ok($$insert into public.events (place_id, title, recurrence, start_time, arrival_advice_max) select id, 'x', 'daily', '10:00', 45 from public.places limit 1$$,
  '23514', null, 'arrival advice needs both min and max (max only)');
select throws_ok($$insert into public.events (place_id, title, recurrence, start_time, arrival_advice_min, arrival_advice_max) select id, 'x', 'daily', '10:00', 45, 30 from public.places limit 1$$,
  '23514', null, 'arrival advice min must be <= max');
select lives_ok($$insert into public.events (place_id, title, recurrence, start_time, arrival_advice_min, arrival_advice_max, performers, tags) select id, 'ok', 'daily', '10:00', 30, 30, '{A,B}', '{tari,wayang,ramah_anak}' from public.places limit 1$$,
  'valid advice, performers and all three tags are accepted');
select throws_ok($$insert into public.events (place_id, title, recurrence, start_time, tags) select id, 'x', 'daily', '10:00', '{tari,musik}' from public.places limit 1$$,
  '23514', null, 'unknown event tag is rejected');
select throws_ok($$insert into public.events (place_id, title, recurrence, start_time, performers) select id, 'x', 'daily', '10:00', null from public.places limit 1$$,
  '23502', null, 'performers cannot be null');

-- ---- anonymous (guest): may browse official data, nothing personal -------------
set local role anon;
select cmp_ok((select count(*)::int from public.regions), '>', 0, 'anon can read regions');
select is((select count(*)::int from public.places), 10, 'anon can read places');
select cmp_ok((select count(*)::int from public.places where lat is not null and lng is not null), '=', 10,
              'anon can read places.lat/lng');
select cmp_ok((select count(*)::int from public.opening_hours), '>', 0, 'anon can read published opening_hours');
select cmp_ok((select count(*)::int from public.ticket_prices), '>', 0, 'anon can read published ticket_prices');
select cmp_ok((select count(*)::int from public.events), '>', 0, 'anon can read published events');
select is((select performers from public.events where title = 'Sendratari Roro Jonggrang'), array['Lebih dari 200 penari dan pemusik lokal'], 'anon can read performers of a published event');
select is((select arrival_advice_min || '-' || arrival_advice_max from public.events where title = 'Sendratari Roro Jonggrang'), '30-45', 'anon can read arrival advice of a published event');
select is((select tags from public.events where title = 'Sendratari Roro Jonggrang'), array['tari'], 'anon can read tags of a published event');
select is((select tags from public.events where title like 'Fragmen wayang wong%'), array['wayang'], 'Fragmen wayang wong is tagged wayang');
select is((select count(*)::int from public.closures), (select count(*)::int from public.closures where review_status = 'published'),
          'anon closures are published only');
select is((select count(*)::int from public.opening_hours where review_status <> 'published'), 0, 'anon cannot see pending opening_hours');
select is((select count(*)::int from public.closures where review_status <> 'published'), 0, 'anon cannot see pending closures');
select is((select count(*)::int from public.ticket_prices where review_status <> 'published'), 0, 'anon cannot see pending ticket_prices');
select is((select count(*)::int from public.events where review_status <> 'published'), 0, 'anon cannot see pending events');
select is((select jsonb_array_length(public.place_facts((select id from public.places where slug = 'candi-prambanan'))->'ticket_prices')), 2,
          'anon place_facts works and omits the pending Prambanan price');
select throws_ok('select * from public.profiles', '42501', null, 'anon cannot read profiles');
select throws_ok('select * from public.stamps', '42501', null, 'anon cannot read stamps');
select throws_ok('select * from public.postcards', '42501', null, 'anon cannot read postcards');
select throws_ok('select * from public.videos', '42501', null, 'anon cannot read videos');
select throws_ok('select * from public.reports', '42501', null, 'anon cannot read reports');
select throws_ok('select * from public.subscriptions', '42501', null, 'anon cannot read subscriptions');
select throws_ok($$insert into public.regions (code, name) values ('X','X')$$, '42501', null, 'anon cannot insert regions');
select throws_ok($$insert into public.places (region_id, slug, name, category, cap_code, location)
  values (1,'x','x','museum','X-1', 'SRID=4326;POINT(110 -7)')$$, '42501', null, 'anon cannot insert places');
select throws_ok($$insert into public.opening_hours (place_id, weekday, is_closed) values (gen_random_uuid(),1,true)$$, '42501', null, 'anon cannot insert opening_hours');
select throws_ok($$insert into public.closures (place_id, starts_on, ends_on) values (gen_random_uuid(),'2026-01-01','2026-01-02')$$, '42501', null, 'anon cannot insert closures');
select throws_ok($$insert into public.ticket_prices (place_id, label) values (gen_random_uuid(),'x')$$, '42501', null, 'anon cannot insert ticket_prices');
select throws_ok($$insert into public.events (place_id, title, recurrence, start_time) values (gen_random_uuid(),'x','daily','10:00')$$, '42501', null, 'anon cannot insert events');
select throws_ok($$insert into public.profiles (id) values (gen_random_uuid())$$, '42501', null, 'anon cannot insert profiles');
select throws_ok($$insert into public.stamps (user_id, place_id) values (gen_random_uuid(), gen_random_uuid())$$, '42501', null, 'anon cannot insert stamps');
select throws_ok($$insert into public.postcards (user_id, place_id, message) values (gen_random_uuid(), gen_random_uuid(),'x')$$, '42501', null, 'anon cannot insert postcards');
select throws_ok($$insert into public.videos (user_id, place_id) values (gen_random_uuid(), gen_random_uuid())$$, '42501', null, 'anon cannot insert videos');
select throws_ok($$insert into public.reports (user_id, reason, place_id) values (gen_random_uuid(),'other', gen_random_uuid())$$, '42501', null, 'anon cannot insert reports');
select throws_ok($$insert into public.subscriptions (user_id, category) values (gen_random_uuid(),'museum')$$, '42501', null, 'anon cannot insert subscriptions');
select throws_ok($$update public.places set name = name$$, '42501', null, 'anon cannot update places');
select throws_ok($$delete from public.events$$, '42501', null, 'anon cannot delete events');
reset role;

-- ---- stamps: no client write ----------------------------------------------------
set local role authenticated;
select pg_temp.as_user('00000000-0000-0000-0000-00000000b002');
select throws_ok($$insert into public.stamps (user_id, place_id)
  select '00000000-0000-0000-0000-00000000b002', id from public.places where slug = 'kotagede'$$,
  '42501', null, 'client cannot insert a stamp (even own)');
select pg_temp.as_user('00000000-0000-0000-0000-00000000b001');
select throws_ok($$update public.stamps set stamped_at = now()$$, '42501', null, 'client cannot update a stamp');
select throws_ok($$delete from public.stamps$$, '42501', null, 'client cannot delete a stamp');
select is((select count(*)::int from public.stamps), 1, 'A sees own stamp only');
select pg_temp.as_user('00000000-0000-0000-0000-00000000b002');
select is((select count(*)::int from public.stamps), 0, 'B cannot see A''s stamp');

-- ---- postcards / videos need a stamp -------------------------------------------
select pg_temp.as_user('00000000-0000-0000-0000-00000000b002');
select throws_ok($$insert into public.postcards (user_id, place_id, message)
  select '00000000-0000-0000-0000-00000000b002', id, 'halo' from public.places where slug = 'tamansari'$$,
  '42501', null, 'B without stamp cannot post a postcard');
select throws_ok($$insert into public.videos (user_id, place_id, caption)
  select '00000000-0000-0000-0000-00000000b002', id, 'halo' from public.places where slug = 'tamansari'$$,
  '42501', null, 'B without stamp cannot post a video');
select throws_ok($$insert into public.postcards (user_id, place_id, message)
  select '00000000-0000-0000-0000-00000000b001', id, 'menyamar' from public.places where slug = 'tamansari'$$,
  '42501', null, 'B cannot post a postcard as A');
select throws_ok($$insert into public.videos (user_id, place_id, caption)
  select '00000000-0000-0000-0000-00000000b001', id, 'menyamar' from public.places where slug = 'tamansari'$$,
  '42501', null, 'B cannot post a video as A');

select pg_temp.as_user('00000000-0000-0000-0000-00000000b001');
select lives_ok($$insert into public.postcards (id, user_id, place_id, message)
  select '00000000-0000-0000-0000-00000000c001', '00000000-0000-0000-0000-00000000b001', id, 'Tamansari indah'
  from public.places where slug = 'tamansari'$$, 'A with stamp can post a postcard');
select lives_ok($$insert into public.videos (id, user_id, place_id, caption)
  select '00000000-0000-0000-0000-00000000d001', '00000000-0000-0000-0000-00000000b001', id, 'Lorong Tamansari'
  from public.places where slug = 'tamansari'$$, 'A with stamp can post a video');
select throws_ok($$insert into public.postcards (user_id, place_id, message)
  select '00000000-0000-0000-0000-00000000b001', id, 'belum cap' from public.places where slug = 'kotagede'$$,
  '42501', null, 'A cannot post a postcard for a place without own stamp');
select throws_ok($$insert into public.videos (user_id, place_id, caption)
  select '00000000-0000-0000-0000-00000000b001', id, 'belum cap' from public.places where slug = 'kotagede'$$,
  '42501', null, 'A cannot post a video for a place without own stamp');
select throws_ok($$update public.postcards set place_id = (select id from public.places where slug = 'kotagede')
  where id = '00000000-0000-0000-0000-00000000c001'$$,
  '42501', null, 'A cannot move a postcard to an unstamped place');
select throws_ok($$update public.videos set place_id = (select id from public.places where slug = 'kotagede')
  where id = '00000000-0000-0000-0000-00000000d001'$$,
  '42501', null, 'A cannot move a video to an unstamped place');

-- ---- postcard fields: rating, card style, private postcards ---------------------
select pg_temp.as_user('00000000-0000-0000-0000-00000000b001');
select lives_ok($$insert into public.postcards (id, user_id, place_id, message, rating, card_style, is_public)
  select '00000000-0000-0000-0000-00000000c002', '00000000-0000-0000-0000-00000000b001', id, 'Hanya aku', 5, 'cap', false
  from public.places where slug = 'tamansari'$$, 'A can post a private postcard with rating and style');
select throws_ok($$insert into public.postcards (user_id, place_id, message, rating, card_style, is_public)
  select '00000000-0000-0000-0000-00000000b001', id, 'belum cap', 4, 'peta', false from public.places where slug = 'kotagede'$$,
  '42501', null, 'insert without a stamp is refused even with the new fields');
select throws_ok($$insert into public.postcards (user_id, place_id, message, rating)
  select '00000000-0000-0000-0000-00000000b001', id, 'bintang 6', 6 from public.places where slug = 'tamansari'$$,
  '23514', null, 'rating above 5 is refused');
select throws_ok($$insert into public.postcards (user_id, place_id, message, card_style)
  select '00000000-0000-0000-0000-00000000b001', id, 'gaya salah', 'video' from public.places where slug = 'tamansari'$$,
  '23514', null, 'unknown card style is refused');
select is((select card_style from public.postcards where id = '00000000-0000-0000-0000-00000000c001'), 'jurnal',
          'card_style defaults to jurnal');
select is((select is_public from public.postcards where id = '00000000-0000-0000-0000-00000000c001'), true,
          'is_public defaults to true');
select is((select count(*)::int from public.postcards where id = '00000000-0000-0000-0000-00000000c002'), 1,
          'A sees own private postcard');
select pg_temp.as_user('00000000-0000-0000-0000-00000000b002');
select is((select count(*)::int from public.postcards where id = '00000000-0000-0000-0000-00000000c002'), 0,
          'B cannot see A''s private postcard');

-- ---- stories visible immediately ------------------------------------------------
select pg_temp.as_user('00000000-0000-0000-0000-00000000b002');
select is((select count(*)::int from public.postcards where id = '00000000-0000-0000-0000-00000000c001'), 1,
          'B sees A''s postcard immediately');
select is((select count(*)::int from public.videos where id = '00000000-0000-0000-0000-00000000d001'), 1,
          'B sees A''s video immediately');

-- ---- edit/delete only own -------------------------------------------------------
select is(pg_temp.affected($$update public.postcards set message = 'dibajak' where id = '00000000-0000-0000-0000-00000000c001'$$), 0,
          'B cannot edit A''s postcard');
select is(pg_temp.affected($$delete from public.postcards where id = '00000000-0000-0000-0000-00000000c001'$$), 0,
          'B cannot delete A''s postcard');
select is(pg_temp.affected($$update public.videos set caption = 'dibajak' where id = '00000000-0000-0000-0000-00000000d001'$$), 0,
          'B cannot edit A''s video');
select is(pg_temp.affected($$delete from public.videos where id = '00000000-0000-0000-0000-00000000d001'$$), 0,
          'B cannot delete A''s video');
select pg_temp.as_user('00000000-0000-0000-0000-00000000b001');
select is(pg_temp.affected($$update public.postcards set message = 'diedit' where id = '00000000-0000-0000-0000-00000000c001'$$), 1,
          'A can edit own postcard');
select is(pg_temp.affected($$update public.videos set caption = 'diedit' where id = '00000000-0000-0000-0000-00000000d001'$$), 1,
          'A can edit own video');

-- ---- reports -------------------------------------------------------------------
select lives_ok($$insert into public.reports (id, user_id, place_id, reason, details)
  select '00000000-0000-0000-0000-00000000e001', '00000000-0000-0000-0000-00000000b001', id, 'place_closed', 'Tutup'
  from public.places where slug = 'tamansari'$$, 'A can file a report as self');
select throws_ok($$insert into public.reports (user_id, place_id, reason)
  select '00000000-0000-0000-0000-00000000b002', id, 'other' from public.places where slug = 'tamansari'$$,
  '42501', null, 'A cannot file a report as B');
select pg_temp.as_user('00000000-0000-0000-0000-00000000b002');
select is((select count(*)::int from public.reports), 0, 'B cannot read A''s report');
select is(pg_temp.affected($$update public.reports set details = 'x' where id = '00000000-0000-0000-0000-00000000e001'$$), 0,
          'B cannot edit A''s report');
select is(pg_temp.affected($$delete from public.reports where id = '00000000-0000-0000-0000-00000000e001'$$), 0,
          'B cannot delete A''s report');

-- ---- subscriptions -------------------------------------------------------------
select pg_temp.as_user('00000000-0000-0000-0000-00000000b001');
select lives_ok($$insert into public.subscriptions (id, user_id, category)
  values ('00000000-0000-0000-0000-00000000f001', '00000000-0000-0000-0000-00000000b001', 'tari')$$,
  'A can subscribe to a category');
select throws_ok($$insert into public.subscriptions (user_id, category)
  values ('00000000-0000-0000-0000-00000000b002', 'candi')$$, '42501', null, 'A cannot subscribe as B');
select pg_temp.as_user('00000000-0000-0000-0000-00000000b002');
select is((select count(*)::int from public.subscriptions), 0, 'B cannot read A''s subscriptions');
select is(pg_temp.affected($$delete from public.subscriptions where id = '00000000-0000-0000-0000-00000000f001'$$), 0,
          'B cannot delete A''s subscription');

-- ---- profiles ------------------------------------------------------------------
select is(pg_temp.affected($$update public.profiles set display_name = 'dibajak' where id = '00000000-0000-0000-0000-00000000b001'$$), 0,
          'B cannot edit A''s profile');
select is(pg_temp.affected($$delete from public.profiles where id = '00000000-0000-0000-0000-00000000b001'$$), 0,
          'B cannot delete A''s profile');
select is(pg_temp.affected($$update public.profiles set display_name = 'B' where id = '00000000-0000-0000-0000-00000000b002'$$), 1,
          'B can edit own profile');
select throws_ok($$insert into public.profiles (id) values (gen_random_uuid())$$, '42501', null,
          'client cannot insert profiles directly');

-- ---- pending facts invisible ---------------------------------------------------
select is((select count(*)::int from public.opening_hours where review_status = 'pending'), 0, 'pending opening_hours hidden');
select is((select count(*)::int from public.closures where review_status = 'pending'), 0, 'pending closures hidden');
select is((select count(*)::int from public.ticket_prices where review_status = 'pending'), 0, 'pending ticket_prices hidden');
select is((select count(*)::int from public.events where review_status = 'pending'), 0, 'pending events hidden');
select is((select count(*)::int from public.ticket_prices where label = 'Tanpa tanggal' and checked_at is null), 1,
          'published fact with null checked_at is representable and visible');
select is((select jsonb_array_length(public.place_facts((select id from public.places where slug = 'candi-prambanan'))->'ticket_prices')), 2,
          'place_facts omits the pending Prambanan price');
select throws_ok($$update public.ticket_prices set price_idr = 1$$, '42501', null, 'client cannot write ticket_prices');
select throws_ok($$delete from public.places$$, '42501', null, 'client cannot delete places');
reset role;

-- ---- stamp_distance (service role only) -----------------------------------------
set local role authenticated;
select throws_ok($$select * from public.stamp_distance((select id from public.places where slug = 'tamansari'), -7.81, 110.359)$$,
  '42501', null, 'authenticated cannot call stamp_distance');
reset role;
set local role service_role;
select cmp_ok((select distance_m from public.stamp_distance((select id from public.places where slug = 'tamansari'), -7.8100, 110.3593)),
  '<', 1::double precision, 'stamp_distance is ~0 at the place itself');
reset role;

-- ---- release guard ----------------------------------------------------------------
select throws_ok($$select public.release_check()$$, 'P0001', null, 'release_check fails while sample rows exist');
delete from auth.users where id in (select id from public.profiles where is_sample);
delete from public.places where is_sample;
delete from public.regions where is_sample;
select lives_ok($$select public.release_check()$$, 'release_check passes once sample rows are removed');

select * from finish();
rollback;
