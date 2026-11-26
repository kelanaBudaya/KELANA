-- Generated from docs/data/videos/tiktok_selected.json. Idempotent. Rows stay unpublished until reviewed.
begin;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@museum.benteng.vredeburg/video/7670497585257270535', '7670497585257270535', 'Nikmati Sunset di Benteng Vredeburg Jogja', 1, false from public.places where slug = 'benteng-vredeburg'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@museum.benteng.vredeburg/video/7515758612686408966', '7515758612686408966', 'Museum Benteng Vredeburg: Sejarah dan Wisata Menarik', 2, false from public.places where slug = 'benteng-vredeburg'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@museum.benteng.vredeburg/video/7671967963771145490', '7671967963771145490', 'Spot Foto Terbaik di Benteng Vredeburg Jogja', 3, false from public.places where slug = 'benteng-vredeburg'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@dimasvlens/video/7635069864226393364', '7635069864226393364', 'Candi Prambanan Jogja: Pemandangan Drone & Suasana', 1, false from public.places where slug = 'candi-prambanan'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@ndddinda/video/7637169427057741064', '7637169427057741064', 'Living the Prayers: A Dream Visit to Candi Prambanan, Jogja', 2, false from public.places where slug = 'candi-prambanan'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@thisis_mahoyy/video/7641040316925054215', '7641040316925054215', 'Candi Prambanan Cakep Polll: Foto & Tips Wisata Jogja', 3, false from public.places where slug = 'candi-prambanan'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@rizkialazizz/video/7705962576479390996', '7705962576479390996', 'Bikin Gerabah di Desa Wisata Kasongan: Pengalaman Unik & Seru!', 1, false from public.places where slug = 'desa-wisata-kasongan'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@pemkabbantul/video/7573539929926421778', '7573539929926421778', 'Desa Wisata Krebet: Edukasi dan Kerajinan Batik Kayu', 1, false from public.places where slug = 'desa-wisata-krebet'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@tolewijaya19/video/7688982787042315540', '7688982787042315540', 'Desa Wisata Krebet: A Happy Escape Despite the Heat', 2, false from public.places where slug = 'desa-wisata-krebet'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@jalanjalanmen04/video/7579947151921679637', '7579947151921679637', 'Merapi dari dusun Tlatar,Wonokerto,Turi,Sleman #kaliurang #', 1, false from public.places where slug = 'desa-wisata-tlatar'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@gejapramono/video/7334311163297123589', '7334311163297123589', 'Sejarah Kapiten Tan Djin Sing: Rumah Peninggalan di Ketandan Jogja', 1, false from public.places where slug = 'kampung-ketandan'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@infonongkrongjogja/video/7611043149586173204', '7611043149586173204', 'Kuliner Pekan Budaya Tionghoa di Kampoeng Ketandan Malioboro', 2, false from public.places where slug = 'kampung-ketandan'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@museumkotagedeyk/video/7706717388741381384', '7706717388741381384', 'Museum Kotagede: Jelajahi Warisan Budaya Yogyakarta', 1, false from public.places where slug = 'kawasan-kotagede'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@museumkotagedeyk/video/7685630588992146695', '7685630588992146695', 'Museum Kotagede: Intro Living Museum Yogyakarta', 2, false from public.places where slug = 'kawasan-kotagede'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@museumkotagedeyk/video/7617269455013334292', '7617269455013334292', 'Museum Kotagede Yogyakarta', 3, false from public.places where slug = 'kawasan-kotagede'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@kratonjogja.id/video/7620840584550419733', '7620840584550419733', 'Wisata Keraton Yogyakarta: 3 Unit Kagungan Dalem yang Wajib', 1, false from public.places where slug = 'keraton-yogyakarta'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@hipeekaboo/video/7705067688099417345', '7705067688099417345', 'Keraton Jogja: Pesona Magis Keraton Ngayogyakarta Hadiningrat', 2, false from public.places where slug = 'keraton-yogyakarta'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@kratonjogja.id/video/7636953869922225429', '7636953869922225429', 'Permaten Pralada Sarahastrama Kraton Jogja: Sejarah & Makna', 3, false from public.places where slug = 'keraton-yogyakarta'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@kelayapanjogja/video/7659710531254619410', '7659710531254619410', 'Liburan di Jogja: Masjid Gedhe Kauman Saat Subuh', 1, false from public.places where slug = 'masjid-gedhe-kauman'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@kelayapanjogja/video/7567218156494720274', '7567218156494720274', 'Masjid Gedhe Kauman: Warisan Sejarah Yogyakarta', 2, false from public.places where slug = 'masjid-gedhe-kauman'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@tugujogja.id/video/7671538234093997332', '7671538234093997332', 'Banyak orang mengenal Masjid Gedhe Kauman sebagai masjid', 3, false from public.places where slug = 'masjid-gedhe-kauman'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@museumsonobudoyo/video/7680813608606928135', '7680813608606928135', 'Museum Negeri Sonobudoyo: Ulasan dan Cerita Pengunjung', 1, false from public.places where slug = 'museum-sonobudoyo'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@museumsonobudoyo/video/7707488318073605384', '7707488318073605384', 'Tampak kiri, tampak kanan, tapi yang paling pas ya tampak Museum', 2, false from public.places where slug = 'museum-sonobudoyo'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@pegikemana/video/7560554290645880076', '7560554290645880076', 'Museum Sonobudoyo: Pengalaman Belajar Budaya di Jogja', 3, false from public.places where slug = 'museum-sonobudoyo'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@jogjaexplore/video/7709041993535507720', '7709041993535507720', 'Museum Ullen Sentalu Jogja Saat Berkabut: Suasana Syahdu yang', 1, false from public.places where slug = 'museum-ullen-sentalu'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@c1adeexplorer/video/7705201484740201735', '7705201484740201735', 'Menjelajahi Museum Ullen Sentalu: Sejarah & Budaya Jawa', 2, false from public.places where slug = 'museum-ullen-sentalu'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@jogjakuofficial/video/7555315641280892167', '7555315641280892167', 'Ullen Sentalu Museum Jogja: Kesan Pertama & Cerita Kunjungan', 3, false from public.places where slug = 'museum-ullen-sentalu'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@manekodigital/video/7680475313251339541', '7680475313251339541', 'Kondisi terkini Plengkung Gading 1 Sep 2026 - Maneko Jogja', 1, false from public.places where slug = 'plengkung-gading'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@splitbill.aja/video/7682047794986650888', '7682047794986650888', 'Magical Sunset di Keraton Ratu Boko — Tips Menikmati & Pertunjukan', 1, false from public.places where slug = 'situs-ratu-boko'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@tugujogja.id/video/7710250509885656326', '7710250509885656326', 'Bukan sekadar kompleks candi, Ratu Boko di dekat Prambanan', 2, false from public.places where slug = 'situs-ratu-boko'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@loyaltrip/video/7335322885726260486', '7335322885726260486', 'Candi Ratu Boko: Keindahan Sejarah di Yogyakarta', 3, false from public.places where slug = 'situs-ratu-boko'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@tamanbudayayk/video/7656399249969646855', '7656399249969646855', 'Pentas Rebon TBY: Gratis Pertunjukan Seni 1 Juli 2026', 1, false from public.places where slug = 'taman-budaya-yogyakarta'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@tamanbudayayk/video/7690104365276826932', '7690104365276826932', 'Pembukaan TKTB XXV di Concert Hall TBY 28 September 2026', 2, false from public.places where slug = 'taman-budaya-yogyakarta'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@tamanbudayayk/video/7708257436884356370', '7708257436884356370', 'Pekan Seni Anak AFC 2026 Yogyakarta: Tari & Ketoprak', 3, false from public.places where slug = 'taman-budaya-yogyakarta'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@seringmampir_/video/7665939323430145300', '7665939323430145300', 'Tour Guide Taman Sari Jogja: Pengalaman Seru dengan Pak', 1, false from public.places where slug = 'taman-sari'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@novaursulashn/video/7617357343726030098', '7617357343726030098', 'Wisata Taman Sari Jogja: Tiket Murah dan Praktis', 2, false from public.places where slug = 'taman-sari'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@nessa.curates/video/7706425654207630600', '7706425654207630600', 'Taman Sari Yogyakarta: Cara ke Sana & Tips Beli Tiket Online', 3, false from public.places where slug = 'taman-sari'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@silversmith_workshop/video/7635135765969718549', '7635135765969718549', 'Silversmith Workshop Jogja: Ikut Workshop Perak di Kotagede', 1, false from public.places where slug = 'workshop-perak-kotagede'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@dayoh.id/video/7557701452626480395', '7557701452626480395', 'HS Silver: Pusat Kerajinan Perak di Yogyakarta', 2, false from public.places where slug = 'workshop-perak-kotagede'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@ferisulianta/video/7667410567140871445', '7667410567140871445', 'Candi Banyunibo Yogyakarta: Sejarah, Lokasi & Relief Hariti', 1, false from public.places where slug = 'candi-banyunibo'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@by_sunaryohosea/video/7633778768162622741', '7633778768162622741', 'Candi Banyunibo: Wisata Jogja Seribu Candi yang Wajib Dikunjungi', 2, false from public.places where slug = 'candi-banyunibo'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@tripsejarah/video/7478217280464211255', '7478217280464211255', 'Candi Banyunibo: Candi Buddha Era Medang di Sleman', 3, false from public.places where slug = 'candi-banyunibo'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@dimasvlens/video/7639194056500858133', '7639194056500858133', 'Candi Ijo Jogja: View Gunung Merapi di Pagi Hari Sleman', 1, false from public.places where slug = 'candi-ijo'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@dimasvlens/video/7630269659811417364', '7630269659811417364', 'Candi Ijo & Prambanan dalam Satu Frame: Spot Foto di Sleman Jogja', 2, false from public.places where slug = 'candi-ijo'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@jogjatravelid/video/7711598387161746740', '7711598387161746740', 'Candi Ijo, ada yang tau kenapa disebut candi ijo?? #jogjatravelid #can', 3, false from public.places where slug = 'candi-ijo'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@jogjaexplore/video/7702383380561759495', '7702383380561759495', 'Candi Sambisari: Candi Terdekat dari Malioboro — Panduan Singkat', 1, false from public.places where slug = 'candi-sambisari'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@gejapramono/video/7544347755196550406', '7544347755196550406', 'Candi Sambisari Jogja: Sejarah, Lokasi, dan Tips Kunjungan', 2, false from public.places where slug = 'candi-sambisari'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@jogjaexplore/video/7509428247948463366', '7509428247948463366', 'Candi Sambisari Yogyakarta: Candi Hijau di Sleman Jogja', 3, false from public.places where slug = 'candi-sambisari'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@sobat_mblayang/video/7570200342101544213', '7570200342101544213', 'Wisata Keliling Komplek Makam Raja-Raja Imogiri, Viewnya Cantik!', 1, false from public.places where slug = 'makam-raja-imogiri'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@_jogja/video/7568679830565260562', '7568679830565260562', 'Makam Raja Imogiri: Kompleks Pemakaman Keraton Yogyakarta', 2, false from public.places where slug = 'makam-raja-imogiri'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@jogjatreats/video/7638818801294871829', '7638818801294871829', 'Mitos atau Fakta Makam Raja-Raja Imogiri Jogja?', 3, false from public.places where slug = 'makam-raja-imogiri'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@jogjaexplore/video/7701167164903918855', '7701167164903918855', 'Suasana Pagi di Desa Nglanggeran Gunung Api Purba', 1, false from public.places where slug = 'desa-wisata-nglanggeran'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@gunungapipurba/video/7683384286292200712', '7683384286292200712', 'Wisata Desa Wisata Nglanggeran: Piknik di Gunung Api Purba Yogya', 2, false from public.places where slug = 'desa-wisata-nglanggeran'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@jhpict/video/7668253084446969106', '7668253084446969106', 'Desa Wisata Nglanggeran di Gunungkidul, Jogja', 3, false from public.places where slug = 'desa-wisata-nglanggeran'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@dewi_widosari/video/7622952567995256085', '7622952567995256085', 'Belajar Tari Tradisional di Desa Wisata Widosari Jogja', 1, false from public.places where slug = 'desa-wisata-widosari'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@dewi_widosari/video/7663669548373052693', '7663669548373052693', 'Desa Wisata Widosari: Pengalaman Tak Terlupakan di Jogja', 2, false from public.places where slug = 'desa-wisata-widosari'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@dewi_widosari/video/7691964789433715989', '7691964789433715989', 'Desa Wisata Widosari: Outbound Ceria di Kulon Progo', 3, false from public.places where slug = 'desa-wisata-widosari'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@affandimuseum/video/7627418670591003925', '7627418670591003925', 'Pengalaman Seru di Museum Affandi: Field Trip Kreatif', 1, false from public.places where slug = 'museum-affandi'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@alwijo/video/7508622810084887815', '7508622810084887815', 'Museum Affandi: Destinasi Wajib untuk Penggemar Seni', 2, false from public.places where slug = 'museum-affandi'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@enik.erna/video/7563568019834686728', '7563568019834686728', 'Museum Gunungapi Merapi: Wisata Edukatif Familier di Jogja', 1, false from public.places where slug = 'museum-gunungapi-merapi'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@mumtazuf/video/7608736695600696596', '7608736695600696596', 'Museum Gunung Merapi: Wisata Edukasi Menarik di Jogja', 2, false from public.places where slug = 'museum-gunungapi-merapi'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order, is_published)
select id, 'https://www.tiktok.com/@selmadena09/video/7661921959076171009', '7661921959076171009', 'Museum Gunungapi Merapi Jogja: Panduan Kunjungan & Info Penting', 3, false from public.places where slug = 'museum-gunungapi-merapi'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;

commit;
