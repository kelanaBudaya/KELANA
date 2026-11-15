# BudayaGo — Rancangan Produk

Aplikasi mobile perencana perjalanan wisata budaya.

- **Konteks:** kompetisi / hackathon
- **Skala produk:** nasional (seluruh Indonesia), dibuka bertahap per daerah
- **Wilayah dengan data (MVP):** DI Yogyakarta; daerah lain tampil sebagai "Segera hadir"
- **Stack:** Flutter + Supabase
- **Tim & waktu:** 2–4 orang, kurang dari 2 minggu
- **Bentuk demo:** aplikasi berjalan di perangkat

## 1. Ringkasan produk

**Masalah.** Orang yang berkunjung ke suatu daerah dan penasaran dengan budaya lokal sulit menemukan informasi wisata budaya yang reliable; informasinya tersebar di banyak platform.

**Tujuan.** User dapat merencanakan kunjungan ke tempat wisata budaya dengan informasi yang mudah dijangkau, reliable, dan terintegrasi.

**Target user.** Gen Z, solo traveler, keluarga, pasangan.

**Metrik keberhasilan.** User memperoleh:

- itinerary lengkap berisi wisata budaya yang dituju,
- ringkasan tentang apa yang ada di dalam tiap atraksi,
- review dari orang sungguhan lewat video TikTok, Instagram, dan YouTube.

**Inovasi.**

- Desain menarik dengan onboarding yang engaging.
- Video media sosial sebagai review yang reliable.
- Peta interaktif untuk mencari atraksi berdasarkan lokasi.

**Batasan cakupan.** Hanya wisata budaya, dalam dua tipe:

| Tipe | Definisi | Contoh |
|---|---|---|
| Permanen | Satu kesatuan lokasi yang memang tempat wisata budaya | Museum Sonobudoyo |
| Pengalaman / pertunjukan | Tidak permanen, menumpang di lokasi wisata non-budaya | Tari kecak di pantai |

Data atraksi diambil dari situs-situs resmi.

## 2. Keputusan desain

| Topik | Keputusan | Alasan |
|---|---|---|
| Itinerary | Hybrid: draft otomatis, lalu diedit manual | Cepat menghasilkan rencana tanpa menghilangkan kendali user |
| Pengumpulan data atraksi | Model DeepSeek lewat opencode mengekstrak halaman resmi menjadi data terstruktur, lalu direview manual | Menghemat waktu menulis scraper per situs; review manual menjaga reliabilitas |
| Pencarian video | Otomatis di pipeline data dengan workflow skill `/last30days`, bukan saat app dibuka | Skill ini menarik posting TikTok, Instagram, dan YouTube 30 hari terakhir beserta angka engagement; platform tersebut tidak menyediakan pencarian publik yang bisa dipanggil langsung dari app |
| Tampilan video | Embed / oEmbed resmi | Patuh ketentuan platform, atribusi kreator terjaga |
| Kolaborasi | Berbagi lewat link (read-only + salin) | Edit bersama real-time tidak realistis dalam 2 minggu; masuk backlog |
| Backend | Supabase | Auth, database, dan query geospasial sudah tersedia tanpa server sendiri |
| Itinerary engine | Berjalan di perangkat | Data Yogyakarta hanya sekitar 50–100 atraksi |

## 3. Functional requirements

| ID | Requirement | Prioritas |
|---|---|---|
| FR-1 | Onboarding interaktif: pilih tipe traveler (solo/couple/family/teman), minat budaya, dan durasi kunjungan; tanggal boleh diisi belakangan. Ringkasan "Rencanaku sejauh ini" tampil selama pengisian | Must |
| FR-2 | Peta interaktif: pin atraksi, filter kategori, cari berdasarkan lokasi/radius, tap pin untuk kartu ringkas | Must |
| FR-3 | Katalog dan pencarian atraksi (nama, kategori, "buka sekarang") | Must |
| FR-4 | Detail atraksi: ringkasan, "apa yang ada di dalam", jam buka, harga tiket, durasi kunjungan, link sumber resmi dan tanggal verifikasi | Must |
| FR-5 | Dua tipe atraksi: permanen dan pengalaman/pertunjukan yang menumpang di lokasi lain, dengan jadwal tayang | Must |
| FR-6 | Video review per atraksi dari TikTok/Instagram/YouTube lewat embed resmi, dengan atribusi kreator dan label "Tren 30 hari terakhir" untuk video baru | Must |
| FR-7 | Buat draft itinerary otomatis dari preferensi, jarak, jam buka, dan jadwal pertunjukan; tampil per hari sebagai daftar bernomor dan garis rute di peta | Must |
| FR-8 | Edit itinerary manual: tambah, hapus, ubah urutan, pindah hari; peringatan jika bentrok jam buka | Must |
| FR-9 | Akun (Google sign-in) dan simpan itinerary; login baru diminta saat menyimpan | Must |
| FR-10 | Bagikan itinerary lewat link; penerima bisa menyalin | Should |
| FR-11 | Kalender pertunjukan/event mingguan | Should |
| FR-12 | Wishlist atraksi | Could |
| FR-13 | Laporkan info/video yang salah | Could |
| FR-14 | Jelajah tingkat nasional: peta Indonesia dengan penanda per daerah dan daftar daerah; daerah bertanda aktif/"Segera hadir" | Must |
| FR-15 | Daerah "Segera hadir" tidak bisa dibuka; user bisa menekan "Kabari saya" untuk diingatkan saat daerah tersedia | Should |
| FR-16 | Pemilihan daerah tujuan di onboarding; itinerary, peta, dan pencarian mengikuti daerah terpilih | Must |

## 4. Non-functional requirements

| ID | Aspek | Target |
|---|---|---|
| NFR-1 | Reliabilitas data | Setiap atraksi punya `source_url` resmi dan `last_verified_at`; data lolos review manual sebelum tayang |
| NFR-2 | Performa | Peta dan daftar tampil < 2 detik di 4G; itinerary dibuat < 1 detik |
| NFR-3 | Usability | Itinerary pertama jadi dalam ≤ 3 menit sejak install; onboarding ≤ 4 layar dan bisa dilewati |
| NFR-4 | Kepatuhan | Video hanya lewat embed/oEmbed resmi, tidak diunduh ulang; scraping menghormati robots.txt |
| NFR-5 | Keamanan | Row Level Security: itinerary hanya bisa diubah pemiliknya; link berbagi memakai token acak |
| NFR-6 | Ketahanan | Atraksi dan itinerary terakhir di-cache; jika embed video gagal, tampil link keluar |
| NFR-7 | Aksesibilitas | Kontras WCAG AA, target sentuh ≥ 48dp, mendukung pembesaran teks |
| NFR-8 | Skalabilitas | Skema memakai `region_id`; tabel `regions` punya status (`active` / `coming_soon`) sehingga daerah baru dibuka tanpa merilis ulang app |

## 5. Rencana fitur

### MVP (demo)

1. Onboarding preferensi
2. Peta interaktif dan katalog
3. Detail atraksi dengan sumber resmi
4. Video review ter-embed
5. Itinerary otomatis + editor manual
6. Akun dan simpan itinerary
7. Berbagi lewat link
8. Kalender pertunjukan

### Backlog (setelah kompetisi)

- Edit itinerary bersama secara real-time
- Mode offline penuh
- Kiriman video dari user dengan moderasi
- Perluasan ke daerah lain
- Wishlist dan pelaporan info yang salah (jika tidak sempat di MVP)

## 6. Arsitektur

```
Flutter app (Riverpod, go_router)
 ├─ flutter_map + tile OSM/MapTiler
 ├─ WebView untuk embed video
 └─ Itinerary engine (Dart, on-device)
        │
Supabase: Postgres + PostGIS · Auth (Google) · RLS
        ▲
Pipeline data (dijalankan tim, manual/berkala)
 ├─ opencode + DeepSeek: ekstrak situs resmi → validasi skema → review manual
 └─ /last30days: cari video terbaru → filter relevansi → simpan URL
```

### 6.1 Aplikasi

- **State management:** Riverpod
- **Navigasi:** go_router
- **Peta:** flutter_map dengan tile OSM atau MapTiler (tanpa kunci berbayar)
- **Video:** WebView yang memuat embed resmi; dimuat hanya saat kartu terlihat, dengan thumbnail sebagai placeholder

### 6.2 Itinerary engine

Berjalan di perangkat, dengan langkah:

1. Saring atraksi sesuai minat dan tipe traveler.
2. Pasang pertunjukan berjadwal sebagai jangkar waktu.
3. Kelompokkan atraksi per hari berdasarkan kedekatan.
4. Urutkan dengan nearest-neighbor.
5. Validasi terhadap jam buka dan durasi kunjungan.

### 6.3 Pipeline data

**Atraksi.** Sumber: situs resmi dinas pariwisata/kebudayaan DIY serta situs resmi tiap museum, keraton, dan candi. Daftar URL perlu ditetapkan tim.

Alur dengan DeepSeek lewat opencode:

1. Tim menyiapkan daftar URL resmi per atraksi.
2. opencode (model DeepSeek) membuka tiap halaman dan mengisi skema `attractions` dan `schedules` dalam format JSON.
3. Aturan ekstraksi: nilai yang tidak tertulis di halaman diisi `null`, tidak ditebak; setiap record membawa `source_url` halaman asalnya.
4. Skrip validasi memeriksa JSON terhadap skema (kolom wajib, format jam, koordinat di dalam wilayah DIY).
5. Anggota tim mencocokkan tiap record dengan halaman sumbernya, lalu mengisi `last_verified_at`.
6. Record yang lolos dimasukkan ke Supabase.

Langkah 5 tidak boleh dilewati: model bahasa bisa mengarang jam buka atau harga tiket, dan reliabilitas adalah nilai jual utama app ini.

**Video.** Ditemukan dengan workflow skill `/last30days`, dijalankan tim per atraksi (bukan oleh app):

1. Jalankan skill dengan nama atraksi sebagai topik, mis. "Museum Sonobudoyo".
2. Ambil hasil TikTok, Instagram, dan YouTube beserta angka engagement.
3. Saring: nama atraksi cocok, video memang direkam di lokasi, engagement di atas ambang.
4. Simpan URL ke tabel `videos` dengan `published_at`, lalu tandai `approved` setelah dicek.
5. Ulangi berkala (mis. mingguan) agar video tetap segar.

| Platform | Cara menampilkan |
|---|---|
| YouTube | Embed iframe |
| TikTok | oEmbed TikTok |
| Instagram | oEmbed Meta (butuh token app) |

Catatan:

- Sumber TikTok dan Instagram pada skill ini memakai layanan pihak ketiga yang butuh API key (`SCRAPECREATORS_API_KEY`); jalankan mode `doctor` pada skill untuk memastikan sumber mana yang aktif.
- Jendela 30 hari bisa kosong untuk atraksi yang kurang populer. Cadangannya: pencarian YouTube Data API tanpa batas tanggal, sehingga tiap atraksi tetap punya minimal satu video.
- Jika token Meta tidak sempat diurus, mulai dari TikTok dan YouTube.

### 6.4 Model data

| Tabel | Kolom utama |
|---|---|
| `regions` | id, nama, status (`active` / `coming_soon`), koordinat pusat, jumlah atraksi |
| `attractions` | id, region_id, type (`permanent` / `experience`), nama, kategori, ringkasan, isi, koordinat, host_place, jam buka, harga tiket, durasi kunjungan, source_url, last_verified_at |
| `schedules` | id, attraction_id, hari, jam mulai, jam selesai, source_url, last_verified_at |
| `videos` | id, attraction_id, platform, url, kreator, jumlah view, published_at, approved |
| `profiles` | id (auth user), tipe traveler, minat |
| `itineraries` | id, owner_id, judul, tanggal mulai, tanggal selesai, share_token |
| `itinerary_items` | id, itinerary_id, attraction_id, hari ke-, urutan, jam mulai |

`host_place` dipakai untuk atraksi tipe `experience`: nama dan koordinat lokasi yang ditumpangi.

## 7. Alur pengguna

```
Onboarding (termasuk pilih daerah) → Jelajah nasional → Peta daerah → Detail atraksi → Buat itinerary → Edit → Simpan (login) → Bagikan
```

1. **Onboarding (4 langkah):** tipe traveler → minat budaya (kartu: keraton, museum, seni pertunjukan, kerajinan, candi) → daerah tujuan (Yogyakarta aktif; lainnya "Segera hadir" dengan "Kabari saya") → durasi; tanggal opsional. Indikator langkah dan ringkasan pilihan selalu terlihat.
2. **Jelajah nasional:** peta Indonesia dengan penanda tiap daerah dan daftar daerah. Ketuk daerah aktif untuk masuk ke peta daerah; ketuk daerah "Segera hadir" untuk melihat statusnya. Tombol "Indonesia" di peta daerah kembali ke tampilan nasional.
3. **Peta daerah:** pin terfilter sesuai minat; geser peta atau "cari di area ini".
4. **Detail atraksi:** baca ringkasan, tonton video review, cek jadwal, lalu "Tambah ke rencana".
5. **Buat itinerary:** app menyusun draft per hari dengan estimasi waktu dan jarak.
6. **Edit:** drag untuk ubah urutan, tambah/hapus, peringatan bentrok.
7. **Simpan:** login Google diminta di titik ini.
8. **Bagikan:** link dibuka teman, lalu disalin ke akun mereka.

## 8. Rencana 2 minggu

| Hari | Hasil |
|---|---|
| 1–2 | Skema Supabase, design system, kerangka navigasi; scraping mulai paralel |
| 3–5 | Peta, katalog, detail atraksi; 30+ atraksi terverifikasi masuk |
| 6–8 | Itinerary engine dan editor; pipeline video dan embed |
| 9–10 | Auth, simpan, link berbagi, kalender pertunjukan |
| 11–12 | Onboarding final, polish, empty/error state |
| 13–14 | Uji di perangkat nyata, skenario demo, buffer |

## 9. Risiko

| Risiko | Mitigasi |
|---|---|
| Video hasil pencarian otomatis tidak relevan | Flag `approved` agar tim bisa menyaring cepat sebelum demo |
| DeepSeek mengarang data yang tidak ada di halaman sumber | Aturan `null` untuk nilai yang tidak ditemukan, validasi skema, dan pencocokan manual sebelum tayang |
| Sumber TikTok/Instagram di `/last30days` tidak aktif tanpa API key | Cek dengan mode `doctor` di hari pertama; cadangan YouTube Data API |
| Jadwal pertunjukan sering berubah | Tampilkan tanggal verifikasi dan link sumber di setiap jadwal |
| Embed TikTok di WebView lambat | Muat saat kartu terlihat, pakai thumbnail sebagai placeholder |
| Scope terlalu besar untuk 2 minggu | Fitur Should/Could dipotong lebih dulu; alur inti FR-1 sampai FR-9 diprioritaskan |

## 10. Referensi produk

Dua produk yang ditinjau pada 16 November 2026:

- **Traveloka Travel Planner** — https://www.traveloka.com/id-id/travelplanner
- **MaiA Trip Planner (Wonderful Indonesia)** — https://www.indonesia.travel/id/id/formai

| Yang diamati | Sumber | Penerapan di BudayaGo |
|---|---|---|
| Alur tiga langkah: ceritakan perjalanan → jelajahi hari demi hari → simpan | Traveloka | Menjadi kerangka alur utama (onboarding → itinerary → simpan) |
| Jumlah hari dipilih dulu, tanggal bisa menyusul | Traveloka | FR-1: durasi wajib, tanggal opsional |
| Itinerary per hari sebagai daftar bernomor, dengan peta yang menunjukkan hubungan antar-titik | Traveloka | FR-7: daftar bernomor per hari dan garis rute di peta |
| Simpan butuh login, ada konfirmasi "Disimpan" | Traveloka | FR-9: login diminta saat menyimpan |
| Form empat langkah dengan indikator "1 of 4" dan tombol Kembali/Selanjutnya | MaiA | Onboarding ≤ 4 layar dengan indikator langkah |
| Panel "Rencana saya sejauh ini" selama pengisian | MaiA | FR-1: ringkasan pilihan selalu terlihat |
| Pilihan rombongan: solo, pasangan, keluarga, teman | MaiA | Tipe traveler ditambah "teman" |
| Tag minat, termasuk ramah anak dan aksesibilitas disabilitas | MaiA | Kandidat filter atraksi untuk segmen keluarga (backlog) |

**Yang sengaja tidak diambil:** anggaran dan mata uang, pilihan akomodasi dan makanan, chatbot AI, serta integrasi pemesanan. Semuanya berada di luar cakupan wisata budaya dan tidak muat dalam 2 minggu.

**Pembeda BudayaGo.** Kedua referensi adalah perencana perjalanan umum. Dari halaman yang ditinjau, keduanya tidak menonjolkan hal berikut, yang menjadi posisi BudayaGo:

- fokus khusus wisata budaya, termasuk pertunjukan berjadwal yang menumpang di lokasi lain,
- sumber resmi dan tanggal verifikasi di setiap atraksi,
- video review dari orang sungguhan di halaman atraksi,
- berbagi itinerary lewat link.

Halaman hasil itinerary kedua produk tidak ikut ditinjau (hanya halaman depan dan formulirnya), jadi daftar pembeda ini perlu dicek ulang dengan mencoba keduanya langsung sebelum dipakai di presentasi.

## 11. Yang masih perlu diputuskan tim

- Daftar URL situs resmi yang menjadi sumber data.
- Siapa yang memegang API key untuk DeepSeek dan untuk sumber TikTok/Instagram di `/last30days`.
- Penyedia tile peta (OSM langsung atau MapTiler).
- Apakah Instagram masuk di MVP (bergantung pada token app Meta).
