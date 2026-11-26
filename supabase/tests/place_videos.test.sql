begin;
create extension if not exists pgtap with schema extensions;
select plan(10);

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, is_published)
select id, 'https://www.tiktok.com/@uji/video/111', '111', true from public.places where slug = 'tamansari';
insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, is_published)
select id, 'https://www.tiktok.com/@uji/video/222', '222', false from public.places where slug = 'tamansari';

select throws_ok($$insert into public.place_videos (place_id, tiktok_url, tiktok_video_id)
  select id, 'https://www.tiktok.com/@uji/video/111', '111' from public.places where slug = 'tamansari'$$,
  '23505', null, 'a video id is unique per place');
select throws_ok($$insert into public.place_videos (place_id, tiktok_url, tiktok_video_id)
  select id, 'https://www.tiktok.com/@uji/video/abc', 'abc' from public.places where slug = 'tamansari'$$,
  '23514', null, 'tiktok_video_id must be digits');

set local role anon;
select is((select count(*)::int from public.place_videos), 1, 'anon reads published videos only');
select throws_ok($$insert into public.place_videos (place_id, tiktok_url, tiktok_video_id)
  select id, 'https://www.tiktok.com/@uji/video/333', '333' from public.places limit 1$$,
  '42501', null, 'anon cannot insert');
reset role;

set local role authenticated;
select set_config('request.jwt.claims',
  json_build_object('sub', '00000000-0000-0000-0000-00000000b002', 'role', 'authenticated')::text, true);
select is((select count(*)::int from public.place_videos), 1, 'authenticated reads published videos only');
select throws_ok($$insert into public.place_videos (place_id, tiktok_url, tiktok_video_id)
  select id, 'https://www.tiktok.com/@uji/video/333', '333' from public.places limit 1$$,
  '42501', null, 'authenticated cannot insert');
select throws_ok($$update public.place_videos set caption = 'x'$$, '42501', null, 'authenticated cannot update');
select throws_ok($$delete from public.place_videos$$, '42501', null, 'authenticated cannot delete');
reset role;

select is((select count(*)::int from public.place_videos), 2, 'postgres still sees the unpublished row');
select is((select count(*)::int from pg_policies where tablename = 'place_videos' and cmd <> 'SELECT'), 0,
          'no write policy exists on place_videos');

select * from finish();
rollback;
