begin;
create extension if not exists pgtap with schema extensions;
select plan(81);

-- Panel verifikasi: staff roles, staff-only reads, four-eyes publishing and the append-only history
-- (migration 20261123090000_admin_panel.sql).

-- ---- fixtures (as postgres) -------------------------------------------------
-- d001 traveller (no staff row), d002 checker (DIY), d003 reviewer (DIY), d004 reviewer (Jawa Tengah only),
-- d005 admin (all regions).
insert into auth.users (instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
                        raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
                        confirmation_token, recovery_token, email_change_token_new, email_change)
select '00000000-0000-0000-0000-000000000000', v.id::uuid, 'authenticated', 'authenticated', v.email, '', now(),
       '{}', '{}', now(), now(), '', '', '', ''
from (values ('00000000-0000-0000-0000-00000000d001', 'traveller@test.invalid'),
             ('00000000-0000-0000-0000-00000000d002', 'checker@test.invalid'),
             ('00000000-0000-0000-0000-00000000d003', 'reviewer@test.invalid'),
             ('00000000-0000-0000-0000-00000000d004', 'reviewer-jtg@test.invalid'),
             ('00000000-0000-0000-0000-00000000d005', 'admin@test.invalid')) as v(id, email);

insert into public.staff (user_id, role, region_id, display_name) values
 ('00000000-0000-0000-0000-00000000d002', 'checker',  (select id from public.regions where code = 'DIY'), 'Pemeriksa Satu'),
 ('00000000-0000-0000-0000-00000000d003', 'reviewer', (select id from public.regions where code = 'DIY'), 'Peninjau Dua'),
 ('00000000-0000-0000-0000-00000000d004', 'reviewer', (select id from public.regions where code = 'JTG'), 'Peninjau Jateng'),
 ('00000000-0000-0000-0000-00000000d005', 'admin',    null,                                                'Admin');

create function pg_temp.as_user(uid uuid) returns void language plpgsql as $$
begin
  perform set_config('request.jwt.claims',
    json_build_object('sub', uid, 'role', 'authenticated')::text, true);
end $$;
create function pg_temp.as_nobody() returns void language plpgsql as $$
begin
  perform set_config('request.jwt.claims', '{}', true);
end $$;
create function pg_temp.affected(q text) returns int language plpgsql as $$
declare n int; begin execute q; get diagnostics n = row_count; return n; end $$;
-- security definer: the price is pending, so RLS would hide it from the roles the test switches to.
create function pg_temp.price_id() returns uuid language sql security definer as
  $$ select id from public.ticket_prices where label = 'Dewasa (usulan baru)' $$;
grant execute on function pg_temp.as_user(uuid), pg_temp.as_nobody(), pg_temp.price_id(), pg_temp.affected(text) to public;

-- A pending event (e001) with a source and a check date, as the scraper would insert it.
insert into public.events (id, place_id, title, recurrence, weekday, start_time, end_time, source_url, checked_at, review_status, is_sample)
select '00000000-0000-0000-0000-00000000e001', id, 'Uji pentas', 'weekly', 5, '19:30', '20:30',
       'https://example.org/uji', now(), 'pending', true
from public.places where slug = 'tamansari';
-- Reports: one by the traveller, one by the checker.
insert into public.reports (id, user_id, place_id, reason, details) values
 ('00000000-0000-0000-0000-00000000e101', '00000000-0000-0000-0000-00000000d001',
  (select id from public.places where slug = 'tamansari'), 'place_closed', 'Tutup'),
 ('00000000-0000-0000-0000-00000000e102', '00000000-0000-0000-0000-00000000d002',
  (select id from public.places where slug = 'kotagede'), 'other', 'Lainnya');

-- ---- structure ---------------------------------------------------------------
select is((select count(*)::int from pg_tables
            where schemaname = 'public' and tablename in ('staff', 'fact_reviews', 'fact_history') and rowsecurity),
          3, 'staff, fact_reviews and fact_history have RLS enabled');
select is((select count(*)::int from pg_trigger
            where not tgisinternal and tgname in ('opening_hours_four_eyes', 'closures_four_eyes',
                                                   'ticket_prices_four_eyes', 'events_four_eyes')),
          4, 'every fact table has the four-eyes trigger');
select is((select count(*)::int from public.fact_history
            where fact_id = '00000000-0000-0000-0000-00000000e001' and action = 'insert' and changed_by is null),
          1, 'inserting a fact writes one history row (no user for the scraper)');

-- ---- anon: nothing ----------------------------------------------------------------
set local role anon;
select throws_ok('select * from public.staff', '42501', null, 'anon cannot read staff');
select throws_ok('select * from public.fact_reviews', '42501', null, 'anon cannot read fact_reviews');
select throws_ok('select * from public.fact_history', '42501', null, 'anon cannot read fact_history');
select throws_ok('select * from public.staff_fact_queue()', '42501', null, 'anon cannot call staff_fact_queue');
select throws_ok($$select public.publish_fact('events', '00000000-0000-0000-0000-00000000e001')$$,
  '42501', null, 'anon cannot call publish_fact');
select is((select count(*)::int from public.events where id = '00000000-0000-0000-0000-00000000e001'), 0,
  'anon cannot see the pending event');
reset role;

-- ---- checker (d002) --------------------------------------------------------------
set local role authenticated;
select pg_temp.as_user('00000000-0000-0000-0000-00000000d002');
select ok(public.is_staff(), 'checker is staff');
select is(public.current_staff_role()::text, 'checker', 'checker role');
select is((select review_status::text from public.staff_fact_queue() where fact_id = '00000000-0000-0000-0000-00000000e001'),
  'pending', 'checker sees the pending event in the queue');
select ok((select count(*) from public.staff_fact_queue() where review_status = 'published') > 0,
  'the queue also lists published facts');
select is((select count(*)::int from public.events where review_status = 'pending'), 0,
  'staff still see only published events through the table (same as the Android app)');
select is((select jsonb_array_length(public.place_facts((select id from public.places where slug = 'candi-prambanan'))->'ticket_prices')), 2,
  'place_facts for a staff account still omits the pending Prambanan price');
select cmp_ok((select count(*)::int from public.reports), '>=', 2, 'staff can read other users'' reports in their region');
select is(pg_temp.affected($$update public.reports set details = 'x' where id = '00000000-0000-0000-0000-00000000e101'$$), 0,
  'reports stay read-only for staff (someone else''s report is not updated)');
select is((select count(*)::int from public.staff), 4, 'staff can read the staff list (names on checks)');
select throws_ok($$update public.staff set role = 'admin'$$, '42501', null, 'staff cannot change staff rows');
select throws_ok($$insert into public.staff (user_id, role, display_name) values ('00000000-0000-0000-0000-00000000d001', 'admin', 'x')$$,
  '42501', null, 'staff cannot add staff');

select lives_ok($$select public.staff_save_fact('events', '00000000-0000-0000-0000-00000000e001',
  '{"start_time": "19:30:00", "end_time": "20:30:00"}', 'Judul laman dan tabel berbeda', 'Pak Uji, WhatsApp', true, current_date + 3)$$,
  'checker saves a draft with an internal note');
select is((select internal_note || '|' || has_conflict::text from public.staff_fact_queue() where fact_id = '00000000-0000-0000-0000-00000000e001'),
  'Judul laman dan tabel berbeda|true', 'internal note and conflict flag are stored');
select throws_ok($$select public.staff_save_fact('events', '00000000-0000-0000-0000-00000000e001', '{"review_status": "published"}', null, null, false, null)$$,
  '42501', 'field_not_editable: review_status', 'review_status cannot be set through staff_save_fact');
select lives_ok($$select public.mark_fact_checked('events', '00000000-0000-0000-0000-00000000e001')$$,
  'checker marks the event as checked');
select is((select checker_id from public.fact_reviews where fact_id = '00000000-0000-0000-0000-00000000e001'),
  '00000000-0000-0000-0000-00000000d002'::uuid, 'checker recorded as the first pair of eyes');
select throws_ok($$select public.publish_fact('events', '00000000-0000-0000-0000-00000000e001')$$,
  '42501', 'reviewer_role_required', 'a checker cannot publish alone');
select throws_ok($$update public.events set review_status = 'published' where id = '00000000-0000-0000-0000-00000000e001'$$,
  '42501', null, 'a checker cannot publish with a direct update');
select throws_ok($$update public.fact_reviews set reviewer_id = '00000000-0000-0000-0000-00000000d003'$$,
  '42501', null, 'a checker cannot write reviewer_id directly');
reset role;

-- ---- skipping the review with a direct update fails for every role -------------
select pg_temp.as_nobody();
select throws_ok($$update public.events set review_status = 'published' where id = '00000000-0000-0000-0000-00000000e001'$$,
  'P0001', 'four_eyes_required', 'postgres cannot publish a checked-but-unreviewed fact');
set local role service_role;
select throws_ok($$update public.events set review_status = 'published' where id = '00000000-0000-0000-0000-00000000e001'$$,
  'P0001', 'four_eyes_required', 'service_role cannot publish a checked-but-unreviewed fact');
select throws_ok($$insert into public.events (place_id, title, recurrence, start_time, review_status)
  select id, 'Langsung terbit', 'daily', '10:00', 'published' from public.places limit 1$$,
  'P0001', 'four_eyes_required', 'service_role cannot insert an already published fact');
select throws_ok($$update public.fact_reviews set reviewer_id = '00000000-0000-0000-0000-00000000d003'
  where fact_id = '00000000-0000-0000-0000-00000000e001'$$,
  '42501', 'reviewer_must_be_self', 'service_role cannot forge a review');
select throws_ok($$update public.fact_reviews set checker_id = '00000000-0000-0000-0000-00000000d003'
  where fact_id = '00000000-0000-0000-0000-00000000e001'$$,
  '42501', 'checker_must_be_self', 'service_role cannot forge a check');
reset role;
select throws_ok($$update public.fact_reviews set reviewer_id = '00000000-0000-0000-0000-00000000d003'
  where fact_id = '00000000-0000-0000-0000-00000000e001'$$,
  '42501', 'reviewer_must_be_self', 'postgres cannot forge a review either');
select is((select review_status::text from public.events where id = '00000000-0000-0000-0000-00000000e001'),
  'pending', 'the event is still pending');

-- ---- same person cannot check and review ------------------------------------------
set local role authenticated;
select pg_temp.as_user('00000000-0000-0000-0000-00000000d003');
select lives_ok($$select public.mark_fact_checked('ticket_prices', pg_temp.price_id())$$,
  'reviewer checks the pending Prambanan price');
select throws_ok($$select public.publish_fact('ticket_prices', pg_temp.price_id())$$,
  'P0001', 'four_eyes_required', 'the same person cannot check and review');
select is((select review_status::text from public.staff_fact_queue() where fact_id = pg_temp.price_id()),
  'pending', 'the price stays pending');

-- ---- region scope ---------------------------------------------------------------------
select pg_temp.as_user('00000000-0000-0000-0000-00000000d004');
select is((select count(*)::int from public.staff_fact_queue() where fact_id = '00000000-0000-0000-0000-00000000e001'), 0,
  'a Jawa Tengah reviewer does not see DIY facts');
select throws_ok($$select public.publish_fact('events', '00000000-0000-0000-0000-00000000e001')$$,
  '42501', 'not_staff', 'a Jawa Tengah reviewer cannot publish a DIY fact');
select is((select count(*)::int from public.fact_reviews), 0, 'a Jawa Tengah reviewer reads no DIY review rows');
select is((select count(*)::int from public.reports), 0, 'a Jawa Tengah reviewer reads no DIY reports');
reset role;

-- ---- content changed after the check: the check is stale -------------------------------
select pg_temp.as_nobody();
select lives_ok($$update public.events set price_from_idr = 125000 where id = '00000000-0000-0000-0000-00000000e001'$$,
  'the scraper may change a pending fact');
set local role authenticated;
select pg_temp.as_user('00000000-0000-0000-0000-00000000d003');
select throws_ok($$select public.publish_fact('events', '00000000-0000-0000-0000-00000000e001')$$,
  'P0001', 'stale_check', 'a reviewer cannot approve content that changed after the check');
select is((select check_is_current from public.staff_fact_queue() where fact_id = '00000000-0000-0000-0000-00000000e001'),
  false, 'the queue flags the check as stale');
select pg_temp.as_user('00000000-0000-0000-0000-00000000d002');
select lives_ok($$select public.mark_fact_checked('events', '00000000-0000-0000-0000-00000000e001')$$,
  'checker checks the new content');

-- ---- two different people: publish ------------------------------------------------------
select pg_temp.as_user('00000000-0000-0000-0000-00000000d003');
select lives_ok($$select public.publish_fact('events', '00000000-0000-0000-0000-00000000e001')$$,
  'a different reviewer approves and publishes');
select is((select checker_id::text || '|' || reviewer_id::text from public.fact_reviews where fact_id = '00000000-0000-0000-0000-00000000e001'),
  '00000000-0000-0000-0000-00000000d002|00000000-0000-0000-0000-00000000d003', 'checker and reviewer are both recorded');
select pg_temp.as_user('00000000-0000-0000-0000-00000000d005');
select lives_ok($$select public.publish_fact('ticket_prices', pg_temp.price_id())$$,
  'the admin publishes the price checked by the reviewer');
reset role;
set local role anon;
select is((select count(*)::int from public.events where id = '00000000-0000-0000-0000-00000000e001'), 1,
  'guests see the event once it is published');
select is((select count(*)::int from public.ticket_prices where label = 'Dewasa (usulan baru)'), 1,
  'guests see the price once it is published');
reset role;

-- ---- published content is frozen without a new review ------------------------------------
select pg_temp.as_nobody();
select throws_ok($$update public.events set start_time = '20:00' where id = '00000000-0000-0000-0000-00000000e001'$$,
  'P0001', 'four_eyes_required', 'changing a published value without review fails (postgres)');
set local role service_role;
select throws_ok($$update public.events set start_time = '20:00' where id = '00000000-0000-0000-0000-00000000e001'$$,
  'P0001', 'four_eyes_required', 'changing a published value without review fails (service_role)');
select lives_ok($$update public.events set checked_at = now() where id = '00000000-0000-0000-0000-00000000e001'$$,
  'an unchanged re-check may refresh checked_at on a published fact');
reset role;

-- staff edit of a published fact sends it back to pending and clears check and review
set local role authenticated;
select pg_temp.as_user('00000000-0000-0000-0000-00000000d002');
select lives_ok($$select public.staff_save_fact('events', '00000000-0000-0000-0000-00000000e001',
  '{"start_time": "20:00:00"}', null, null, false, null)$$, 'checker edits a published event');
select is((select review_status::text || '|' || coalesce(checker_id::text, '-') || '|' || coalesce(reviewer_id::text, '-')
             from public.staff_fact_queue() where fact_id = '00000000-0000-0000-0000-00000000e001'),
  'pending|-|-', 'the edit makes the event pending again with no check or review');
select is((select count(*)::int from public.events where id = '00000000-0000-0000-0000-00000000e001'), 0,
  'the edited (pending) event is hidden from the table read again');

-- ---- history -------------------------------------------------------------------------------
select is((select count(*)::int from public.fact_history
            where fact_id = '00000000-0000-0000-0000-00000000e001' and action = 'checked'
              and changed_by = '00000000-0000-0000-0000-00000000d002'),
  2, 'both checks by the checker are in the history');
select is((select count(*)::int from public.fact_history
            where fact_id = '00000000-0000-0000-0000-00000000e001' and action = 'reviewed'
              and changed_by = '00000000-0000-0000-0000-00000000d003'),
  1, 'the review is in the history');
select is((select count(*)::int from public.fact_history
            where fact_id = '00000000-0000-0000-0000-00000000e001' and action = 'update'
              and before ->> 'review_status' = 'pending' and after ->> 'review_status' = 'published'
              and changed_by = '00000000-0000-0000-0000-00000000d003'),
  1, 'the publish is in the history with the reviewer as author');
select is((select before ->> 'start_time' || ' -> ' || (after ->> 'start_time') from public.fact_history
            where fact_id = '00000000-0000-0000-0000-00000000e001' and action = 'update'
              and changed_by = '00000000-0000-0000-0000-00000000d002'
            order by id desc limit 1),
  '19:30:00 -> 20:00:00', 'history keeps before and after values');
select is((select source_url from public.fact_history
            where fact_id = '00000000-0000-0000-0000-00000000e001' order by id desc limit 1),
  'https://example.org/uji', 'history keeps the source');
select throws_ok($$update public.fact_history set changed_by = null$$, '42501', null, 'staff cannot update history');
select throws_ok($$delete from public.fact_history$$, '42501', null, 'staff cannot delete history');
select throws_ok($$insert into public.fact_history (fact_table, fact_id, action) values ('events', gen_random_uuid(), 'insert')$$,
  '42501', null, 'staff cannot insert history directly');
select ok((public.staff_summary() ?& array['due_this_week', 'conflicts', 'pending', 'reports_7d', 'published_30d']),
  'staff_summary returns every card count');
reset role;
set local role service_role;
select throws_ok($$insert into public.fact_history (fact_table, fact_id, action) values ('events', gen_random_uuid(), 'insert')$$,
  '42501', null, 'service_role cannot insert history directly');
select throws_ok($$delete from public.fact_history$$, '42501', null, 'service_role cannot delete history');
reset role;
select throws_ok($$update public.fact_history set changed_by = null$$, 'P0001', 'fact_history_is_append_only',
  'postgres cannot update history');
select throws_ok($$delete from public.fact_history$$, 'P0001', 'fact_history_is_append_only',
  'postgres cannot delete history');
select throws_ok($$truncate public.fact_history$$, 'P0001', 'fact_history_is_append_only',
  'postgres cannot truncate history');

-- ---- traveller (d001): nothing of the staff surface ------------------------------------------
set local role authenticated;
select pg_temp.as_user('00000000-0000-0000-0000-00000000d001');
select ok(not public.is_staff(), 'a traveller is not staff');
select throws_ok('select * from public.staff_fact_queue()', '42501', 'not_staff', 'a traveller cannot read the queue');
select throws_ok('select public.staff_summary()', '42501', 'not_staff', 'a traveller cannot read the summary');
select throws_ok($$select public.mark_fact_checked('events', '00000000-0000-0000-0000-00000000e001')$$,
  '42501', 'not_staff', 'a traveller cannot check a fact');
select throws_ok($$select public.staff_save_fact('events', '00000000-0000-0000-0000-00000000e001', '{}', 'x', null, false, null)$$,
  '42501', 'not_staff', 'a traveller cannot edit a fact');
select is((select count(*)::int from public.events where review_status = 'pending'), 0, 'a traveller sees no pending events');
select is((select count(*)::int from public.fact_reviews), 0, 'a traveller reads no review rows (internal notes)');
select is((select count(*)::int from public.fact_history), 0, 'a traveller reads no history');
select is((select count(*)::int from public.staff), 0, 'a traveller reads no staff rows');
select is((select count(*)::int from public.reports), 1, 'a traveller still reads only their own report');
reset role;

select * from finish();
rollback;
