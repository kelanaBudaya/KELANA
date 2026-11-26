begin;
create extension if not exists pgtap with schema extensions;
select plan(13);

insert into public.place_photos (place_id, storage_path, thumb_path, width, height, sha1, source, source_page_url,
                                 author, license_code, license_url, attribution_text, is_published)
select id, 'test/a.jpg', 'test/a_t.jpg', 1600, 1000, 'sha-a', 'wikimedia_commons',
       'https://commons.wikimedia.org/wiki/File:A.jpg', 'Uji', 'CC-BY-SA-4.0',
       'https://creativecommons.org/licenses/by-sa/4.0/', 'Foto: Uji · CC BY-SA 4.0 · Wikimedia Commons', true
from public.places where slug = 'tamansari';
insert into public.place_photos (place_id, storage_path, thumb_path, width, height, sha1, source, source_page_url,
                                 author, license_code, license_url, attribution_text, is_published)
select id, 'test/b.jpg', 'test/b_t.jpg', 1600, 1000, 'sha-b', 'openverse',
       'https://openverse.org/image/b', 'Uji', 'CC0-1.0',
       'https://creativecommons.org/publicdomain/zero/1.0/', 'Foto: Uji · CC0 · Openverse', false
from public.places where slug = 'tamansari';

select throws_ok($$insert into public.place_photos (place_id, storage_path, thumb_path, width, height, sha1, source,
    source_page_url, author, license_code, license_url, attribution_text)
  select id, 'test/c.jpg', 'test/c_t.jpg', 1600, 1000, 'sha-a', 'openverse', 'https://x.test/c', 'Uji', 'CC0-1.0',
         'https://creativecommons.org/publicdomain/zero/1.0/', 'x' from public.places where slug = 'tamansari'$$,
  '23505', null, 'a sha1 is unique per place');
select throws_ok($$insert into public.place_photos (place_id, storage_path, thumb_path, width, height, sha1, source,
    source_page_url, author, license_code, license_url, attribution_text)
  select id, 'test/d.jpg', 'test/d_t.jpg', 1600, 1000, 'sha-d', 'openverse', 'https://x.test/d', 'Uji', 'CC-BY-NC-4.0',
         'https://creativecommons.org/licenses/by-nc/4.0/', 'x' from public.places where slug = 'tamansari'$$,
  '23514', null, 'a non-open license is rejected');
select throws_ok($$insert into public.place_photos (place_id, storage_path, thumb_path, width, height, sha1, source,
    source_page_url, author, license_code, license_url, attribution_text)
  select id, 'test/e.jpg', 'test/e_t.jpg', 1600, 1000, 'sha-e', 'openverse', 'https://x.test/e', '   ', 'CC0-1.0',
         'https://creativecommons.org/publicdomain/zero/1.0/', 'x' from public.places where slug = 'tamansari'$$,
  '23514', null, 'a blank author is rejected');
select throws_ok($$insert into public.place_photos (place_id, storage_path, thumb_path, width, height, sha1, source,
    source_page_url, author, license_code, license_url, attribution_text)
  select id, 'test/f.jpg', 'test/f_t.jpg', 1600, 1000, 'sha-f', 'flickr', 'https://x.test/f', 'Uji', 'CC0-1.0',
         'https://creativecommons.org/publicdomain/zero/1.0/', 'x' from public.places where slug = 'tamansari'$$,
  '23514', null, 'an unknown source is rejected');

set local role anon;
select is((select count(*)::int from public.place_photos), 1, 'anon reads published photos only');
select throws_ok($$insert into public.place_photos (place_id, storage_path, thumb_path, width, height, sha1, source,
    source_page_url, author, license_code, license_url, attribution_text)
  select id, 'test/g.jpg', 'test/g_t.jpg', 1600, 1000, 'sha-g', 'openverse', 'https://x.test/g', 'Uji', 'CC0-1.0',
         'https://creativecommons.org/publicdomain/zero/1.0/', 'x' from public.places limit 1$$,
  '42501', null, 'anon cannot insert');
reset role;

set local role authenticated;
select set_config('request.jwt.claims',
  json_build_object('sub', '00000000-0000-0000-0000-00000000b003', 'role', 'authenticated')::text, true);
select is((select count(*)::int from public.place_photos), 1, 'authenticated reads published photos only');
select throws_ok($$insert into public.place_photos (place_id, storage_path, thumb_path, width, height, sha1, source,
    source_page_url, author, license_code, license_url, attribution_text)
  select id, 'test/g.jpg', 'test/g_t.jpg', 1600, 1000, 'sha-g', 'openverse', 'https://x.test/g', 'Uji', 'CC0-1.0',
         'https://creativecommons.org/publicdomain/zero/1.0/', 'x' from public.places limit 1$$,
  '42501', null, 'authenticated cannot insert');
select throws_ok($$update public.place_photos set caption = 'x'$$, '42501', null, 'authenticated cannot update');
select throws_ok($$delete from public.place_photos$$, '42501', null, 'authenticated cannot delete');
reset role;

select is((select count(*)::int from public.place_photos), 2, 'postgres still sees the unpublished row');
select is((select count(*)::int from pg_policies where tablename = 'place_photos' and cmd <> 'SELECT'), 0,
          'no write policy exists on place_photos');
select is((select count(*)::int from storage.buckets
            where id = 'place-photos' and public and file_size_limit = 2097152), 1,
          'place-photos bucket is public with a 2 MB limit');

select * from finish();
rollback;
