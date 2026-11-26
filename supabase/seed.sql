-- SAMPLE DATA ONLY. Every row has is_sample = true.
-- None of this is verified. Run `select public.release_check();` before a public
-- release: it fails while any sample row remains.
-- Sample checked_at = 2026-11-20. Values marked (figma) come from the Figma
-- overviews; the rest are invented but plausible placeholders.

insert into public.regions (code, name, status, sort_order, is_sample) values
  ('DIY', 'DI Yogyakarta',       'available', 1, true),
  ('JTG', 'Jawa Tengah',         'upcoming',  2, true),
  ('BALI','Bali',                'upcoming',  3, true),
  ('JTM', 'Jawa Timur',          'not_open',  4, true),
  ('JBR', 'Jawa Barat',          'not_open',  5, true),
  ('SUMBAR','Sumatera Barat',    'not_open',  6, true),
  ('SULSEL','Sulawesi Selatan',  'not_open',  7, true),
  ('NTT', 'Nusa Tenggara Timur', 'not_open',  8, true),
  ('KALTIM','Kalimantan Timur',  'not_open',  9, true),
  ('PAPUA','Papua',              'not_open', 10, true);

-- Passport order: YK-01 .. YK-10. Coordinates are approximate.
insert into public.places (region_id, slug, name, category, cap_code, passport_order, location, address, description, is_sample)
select r.id, v.slug, v.name, v.category::public.place_category, v.cap_code, v.ord,
       extensions.st_setsrid(extensions.st_makepoint(v.lng, v.lat), 4326)::extensions.geography,
       v.address, v.description, true
from public.regions r,
(values
  ('keraton-yogyakarta',     'Keraton Yogyakarta',          'lainnya', 'YK-01',  1, -7.8053, 110.3642, 'Jl. Rotowijayan Blok No. 1, Panembahan, Kraton', 'Istana Kesultanan Yogyakarta dengan bangsal, museum, dan pentas budaya rutin.'),
  ('museum-sonobudoyo',      'Museum Sonobudoyo',           'museum',  'YK-02',  2, -7.8013, 110.3641, 'Jl. Pangurakan No. 6, Ngupasan, Gondomanan',     'Museum budaya Jawa: wayang, keris, topeng, dan batik.'),
  ('tamansari',              'Tamansari',                   'lainnya', 'YK-03',  3, -7.8100, 110.3593, 'Patehan, Kraton',                                'Bekas taman istirahat sultan dengan kolam, lorong, dan Sumur Gumuling.'),
  ('museum-kereta-wahanarata','Museum Kereta Wahanarata',   'museum',  'YK-04',  4, -7.8036, 110.3567, 'Jl. Rotowijayan No. 5, Panembahan, Kraton',      'Koleksi kereta kencana Keraton Yogyakarta.'),
  ('museum-benteng-vredeburg','Museum Benteng Vredeburg',   'museum',  'YK-05',  5, -7.8002, 110.3664, 'Jl. Margo Mulyo No. 6, Ngupasan, Gondomanan',    'Benteng kolonial yang kini menjadi museum sejarah perjuangan.'),
  ('puro-pakualaman',        'Puro Pakualaman',             'lainnya', 'YK-06',  6, -7.8007, 110.3779, 'Jl. Sultan Agung, Gunungketur, Pakualaman',      'Istana Kadipaten Pakualaman dengan museum dan pendapa.'),
  ('museum-batik-yogyakarta','Museum Batik Yogyakarta',     'museum',  'YK-07',  7, -7.7878, 110.3766, 'Jl. Dr. Sutomo No. 13A, Bausasran, Danurejan',   'Museum batik dengan koleksi kain dan alat membatik.'),
  ('tugu-yogyakarta',        'Tugu Yogyakarta',             'lainnya', 'YK-08',  8, -7.7829, 110.3671, 'Jl. Jenderal Sudirman, Gowongan, Jetis',         'Tugu penanda sumbu filosofi Yogyakarta.'),
  ('kotagede',               'Kotagede',                    'kerajinan','YK-09', 9, -7.8279, 110.3987, 'Kotagede, Kota Yogyakarta',                      'Kawasan kota lama dengan kerajinan perak dan makam raja Mataram.'),
  ('candi-prambanan',        'Candi Prambanan',             'candi',   'YK-10', 10, -7.7520, 110.4914, 'Jl. Raya Solo - Yogyakarta km 16, Prambanan, Sleman', 'Kompleks candi Hindu abad ke-9 dengan relief Ramayana.')
) as v(slug, name, category, cap_code, ord, lat, lng, address, description)
where r.code = 'DIY';

-- PLACEHOLDER dev cap radius (metres) for the sample places only, so Ambil cap can be tried. The real radius
-- per place is a team decision and is NOT decided here. Same change as dev/sample_cap_radius.sql.
update public.places set cap_radius_m = 150 where is_sample = true and cap_radius_m is null;

-- Opening hours, all published (sample). 1=Senin .. 7=Minggu.
-- Sonobudoyo (figma): Selasa-Minggu 08.00-21.00, Senin tutup.
insert into public.opening_hours (place_id, weekday, is_closed, opens_at, closes_at, source_url, checked_at, review_status, is_sample)
select p.id, d, d = 1, case when d = 1 then null else '08:00'::time end, case when d = 1 then null else '21:00'::time end,
       'https://example.org/sample/sonobudoyo', '2026-11-20T09:00:00+07', 'published', true
from public.places p, generate_series(1, 7) d where p.slug = 'museum-sonobudoyo';

-- Prambanan (figma): 06.30-17.30 every day.
insert into public.opening_hours (place_id, weekday, is_closed, opens_at, closes_at, source_url, checked_at, review_status, is_sample)
select p.id, d, false, '06:30', '17:30',
       'https://example.org/sample/prambanan', '2026-11-20T09:00:00+07', 'published', true
from public.places p, generate_series(1, 7) d where p.slug = 'candi-prambanan';

-- Invented hours for the others: open 09.00-15.00, closed on one invented day.
insert into public.opening_hours (place_id, weekday, is_closed, opens_at, closes_at, source_url, checked_at, review_status, is_sample)
select p.id, d, d = 1, case when d = 1 then null else '09:00'::time end, case when d = 1 then null else '15:00'::time end,
       'https://example.org/sample/' || p.slug, '2026-11-20T09:00:00+07', 'published', true
from public.places p, generate_series(1, 7) d
where p.slug not in ('museum-sonobudoyo', 'candi-prambanan', 'tugu-yogyakarta');

-- Tugu is an open monument: open all day.
insert into public.opening_hours (place_id, weekday, is_closed, opens_at, closes_at, note, source_url, checked_at, review_status, is_sample)
select p.id, d, false, '00:00', '23:59', 'Ruang terbuka',
       'https://example.org/sample/tugu', '2026-11-20T09:00:00+07', 'published', true
from public.places p, generate_series(1, 7) d where p.slug = 'tugu-yogyakarta';

-- Ticket prices. Sonobudoyo (figma): dewasa Rp10.000, anak Rp5.000.
insert into public.ticket_prices (place_id, label, price_idr, seller_url, source_url, checked_at, review_status, is_sample)
select p.id, v.label, v.price, v.url, 'https://example.org/sample/' || p.slug, '2026-11-20T09:00:00+07', 'published', true
from public.places p
join (values
  ('museum-sonobudoyo',       'Dewasa',    10000, 'https://example.org/sample/tiket/sonobudoyo'),
  ('museum-sonobudoyo',       'Anak-anak',  5000, 'https://example.org/sample/tiket/sonobudoyo'),
  ('candi-prambanan',         'Dewasa',    50000, 'https://example.org/sample/tiket/prambanan'),
  ('candi-prambanan',         'Anak-anak', 25000, 'https://example.org/sample/tiket/prambanan'),
  ('keraton-yogyakarta',      'Dewasa',    25000, 'https://example.org/sample/tiket/keraton'),
  ('tamansari',               'Dewasa',    15000, 'https://example.org/sample/tiket/tamansari'),
  ('museum-kereta-wahanarata','Dewasa',    10000, 'https://example.org/sample/tiket/kereta'),
  ('museum-benteng-vredeburg','Dewasa',     5000, 'https://example.org/sample/tiket/vredeburg'),
  ('museum-batik-yogyakarta', 'Dewasa',    20000, 'https://example.org/sample/tiket/batik')
) as v(slug, label, price, url) on v.slug = p.slug;

-- Pakualaman: price intentionally unknown (checked_at null -> "Harga sedang dicek"). Published.
insert into public.ticket_prices (place_id, label, price_idr, source_url, checked_at, review_status, is_sample)
select p.id, 'Dewasa', null, null, null, 'published', true from public.places p where p.slug = 'puro-pakualaman';

-- Pending example: must stay invisible to clients.
insert into public.ticket_prices (place_id, label, price_idr, source_url, checked_at, review_status, is_sample)
select p.id, 'Dewasa (usulan baru)', 60000, 'https://example.org/sample/prambanan-baru', '2026-11-20T09:00:00+07', 'pending', true
from public.places p where p.slug = 'candi-prambanan';

-- Events (recurrence: weekly uses ISO weekday).
insert into public.events (place_id, title, description, category, recurrence, weekday, start_time, end_time, price_from_idr, seller_url, source_url, checked_at, review_status, is_sample, performers, arrival_advice_min, arrival_advice_max, tags)
select p.id, v.title, v.descr, v.cat::public.place_category, 'weekly', v.wd, v.st::time, v.et::time, v.price, v.url,
       'https://example.org/sample/events/' || p.slug, '2026-11-20T09:00:00+07', 'published', true,
       v.performers::text[], v.amin::int, v.amax::int, v.tags::text[]
from public.places p
join (values
  ('candi-prambanan',    'Sendratari Roro Jonggrang', 'Pentas sendratari kisah Roro Jonggrang. (figma)', 'tari', 5, '19:30', '20:30', 150000, 'https://example.org/sample/tiket/roro-jonggrang', '{Lebih dari 200 penari dan pemusik lokal}', 30, 45, '{tari}'),
  ('candi-prambanan',    'Sendratari Ramayana',       'Pentas sendratari Ramayana. Hari tayang invented.', 'tari', 2, '19:30', '21:00', 150000, 'https://example.org/sample/tiket/ramayana', '{}', null, null, '{tari}'),
  ('candi-prambanan',    'Sendratari Ramayana',       'Pentas sendratari Ramayana. Hari tayang invented.', 'tari', 4, '19:30', '21:00', 150000, 'https://example.org/sample/tiket/ramayana', '{}', null, null, '{tari}'),
  ('keraton-yogyakarta', 'Macapat, Bangsal Srimanganti', 'Tembang macapat di Bangsal Srimanganti. Hari invented.', 'seni_musik', 6, '09:00', '11:00', null, null, '{}', null, null, '{}'),
  ('keraton-yogyakarta', 'Fragmen wayang wong, Bangsal Srimanganti', 'Fragmen wayang wong di Bangsal Srimanganti. Hari invented.', 'tari', 7, '09:00', '11:00', null, null, '{}', null, null, '{wayang}')
) as v(slug, title, descr, cat, wd, st, et, price, url, performers, amin, amax, tags) on v.slug = p.slug;

-- Sample user, stamp, and two postcards so story screens have data.
insert into auth.users (instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
                        raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
                        confirmation_token, recovery_token, email_change_token_new, email_change)
values ('00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-00000000a001',
        'authenticated', 'authenticated', 'sample.user@example.invalid', '', now(),
        '{"provider":"email","providers":["email"]}', '{"full_name":"Sari (contoh)"}', now(), now(),
        '', '', '', '');
update public.profiles
   set is_sample = true, display_name = 'Sari (contoh)',
       home_region_id = (select id from public.regions where code = 'DIY')
 where id = '00000000-0000-0000-0000-00000000a001';

insert into public.stamps (user_id, place_id, stamped_at, is_sample)
select '00000000-0000-0000-0000-00000000a001', id, '2026-11-20T10:15:00+07', true
from public.places where slug in ('museum-sonobudoyo', 'candi-prambanan');

insert into public.postcards (user_id, place_id, message, is_sample, created_at)
select '00000000-0000-0000-0000-00000000a001', id,
       'Koleksi wayangnya bikin betah berlama-lama. Contoh data.', true, '2026-11-20T11:00:00+07'
from public.places where slug = 'museum-sonobudoyo';
insert into public.postcards (user_id, place_id, message, is_sample, created_at)
select '00000000-0000-0000-0000-00000000a001', id,
       'Relief Ramayana di dinding candi detail sekali. Contoh data.', true, '2026-11-20T12:00:00+07'
from public.places where slug = 'candi-prambanan';
