-- KELANA: event yang digelar di tempat yang sudah ada (events.place_id = tempat tuan rumah).
-- Sumber: docs/data/tempat-diy.json (blok "event"). Jalankan SETELAH diy_places.sql. Aman dijalankan ulang (hapus lalu isi per tempat + judul).
-- Dua event lama (Saung Angklung Udjo, Wayang Orang Ngesti Pandowo) ada di provinsi_places.sql dan tidak disentuh di sini.
-- BYPASS PEMERIKSAAN (sementara, keputusan user 2026-11-26): checked_at = 2026-11-26 untuk jadwal/harga dari sumber tidak resmi; baris ditandai [bypass].
begin;

-- Sendratari Ramayana dulu tersimpan sebagai tempat tersendiri (YK-08); sekarang event di Candi Prambanan.
-- Dihapus hanya bila tidak ada yang merujuk (cap, postcard, laporan, langganan, video). Tiketnya ikut terhapus lewat cascade.
delete from public.places p
where p.slug = 'sendratari-ramayana'
  and not exists (select 1 from public.stamps x where x.place_id = p.id)
  and not exists (select 1 from public.postcards x where x.place_id = p.id)
  and not exists (select 1 from public.reports x where x.place_id = p.id)
  and not exists (select 1 from public.subscriptions x where x.place_id = p.id)
  and not exists (select 1 from public.videos x where x.place_id = p.id);

-- Candi Prambanan: Sendratari Ramayana, Selasa/Kamis/Sabtu 19.30-21.00 (panggung terbuka)
delete from public.events where place_id = (select id from public.places where slug = 'candi-prambanan') and title = 'Sendratari Ramayana Prambanan';
insert into public.events (place_id, title, description, category, recurrence, weekday, event_date, start_time, end_time, price_from_idr, seller_url, source_url, checked_at, review_status, is_sample, tags)
select (select id from public.places where slug = 'candi-prambanan'), 'Sendratari Ramayana Prambanan',
  'Sendratari Ramayana menampilkan kisah Ramayana lewat tari, drama, dan iringan gamelan dengan latar Candi Prambanan. Dipentaskan di panggung terbuka pada musim tertentu dan di gedung tertutup Trimurti pada musim lain. Jadwal berubah menurut musim, jadi periksa ulang sebelum berangkat.',
  'tari'::public.place_category, 'weekly'::public.recurrence_kind, d, null, '19:30', '21:00', 150000,
  'https://ticket.injourneydestination.id/en/ramayana-ballet-prambanan', 'https://ticket.injourneydestination.id/en/ramayana-ballet-prambanan',
  '2026-11-26T09:00:00+07', 'published', false, array['tari']
from unnest(array[2, 4, 6]) as d; -- [bypass] harga "mulai" Rp150.000 dari artikel 2023, bukan harga resmi

-- Museum Negeri Sonobudoyo: Pagelaran Wayang, Selasa-Sabtu 20.00-22.00 (Senin dilewati: museum tutup Senin menurut sumber)
delete from public.events where place_id = (select id from public.places where slug = 'museum-sonobudoyo') and title = 'Pagelaran Wayang Sonobudoyo';
insert into public.events (place_id, title, description, category, recurrence, weekday, event_date, start_time, end_time, price_from_idr, seller_url, source_url, checked_at, review_status, is_sample, tags)
select (select id from public.places where slug = 'museum-sonobudoyo'), 'Pagelaran Wayang Sonobudoyo',
  'Pagelaran wayang dan gamelan malam hari di Museum Negeri Sonobudoyo. Tiket pagelaran terpisah dari tiket masuk museum. Jadwal belum dikonfirmasi pengelola, jadi periksa ulang sebelum berangkat.',
  'lainnya'::public.place_category, 'weekly'::public.recurrence_kind, d, null, '20:00', '22:00', 20000,
  null, 'https://en.wikipedia.org/wiki/Sonobudoyo_Museum',
  '2026-11-26T09:00:00+07', 'published', false, array['wayang']
from unnest(array[2, 3, 4, 5, 6]) as d; -- [bypass] jadwal dari Wikipedia EN, harga domestik Rp20.000 dari Telkomsel Jelajah (asing Rp50.000)

commit;
