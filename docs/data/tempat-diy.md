# Data tempat budaya DIY (draft, 29 tempat + 2 event)

Dikumpulkan lewat pencarian web (2026-11-25 dan 2026-11-26; pembacaan halaman putaran 2 memakai Firecrawl self-host). Semua entri **pending**: belum disetujui manusia. Entri **ditahan** tidak boleh dimuat. `checked_at` kosong berarti sumbernya bertanggal lebih dari 2 tahun atau tidak terbaca, sehingga di app tampil "sedang dicek". Data per fakta dengan `source_url` ada di `tempat-diy.json`.

| Tempat | Jenis | Kategori (enum) | Kabupaten | Jam | Tiket | Koordinat | Status |
|---|---|---|---|---|---|---|---|
| Keraton Ngayogyakarta Hadiningrat | permanen | lainnya | Kota Yogyakarta | ya | ya | ya | pending |
| Taman Sari | permanen | lainnya | Kota Yogyakarta | ya | ya | ya | pending |
| Museum Negeri Sonobudoyo | permanen | museum | Kota Yogyakarta | ya | ya | ya | pending |
| Candi Prambanan | permanen | candi | Sleman | sedang dicek | sedang dicek | ya | pending |
| Museum Benteng Vredeburg | permanen | museum | Kota Yogyakarta | ya | ya | ya | pending |
| Museum Ullen Sentalu | permanen | museum | Sleman | ya | ya | ya | pending |
| Situs Ratu Boko | permanen | lainnya | Sleman | sedang dicek | sedang dicek | ya | pending |
| Kampung Batik Giriloyo | pengalaman | kerajinan | Bantul | sedang dicek | sedang dicek | belum | pending |
| Jamu Ginggang | permanen | lainnya | Kota Yogyakarta | sedang dicek | sedang dicek | belum | pending |
| Masjid Gedhe Kauman | permanen | lainnya | Kota Yogyakarta | sedang dicek | sedang dicek | ya | pending |
| Plengkung Gading | permanen | lainnya | Kota Yogyakarta | sedang dicek | sedang dicek | ya | pending |
| Kampung Pecinan Ketandan | permanen | lainnya | Kota Yogyakarta | sedang dicek | sedang dicek | ya | pending |
| Living Museum Kotagede | permanen | lainnya | Kota Yogyakarta | ya | ya | ya | pending |
| Taman Budaya Yogyakarta (TBY) | permanen | tari | Kota Yogyakarta | sedang dicek | sedang dicek | ya | pending |
| Sekar Rinonce | pengalaman | tari | Kota Yogyakarta | sedang dicek | sedang dicek | ya | pending |
| Workshop Perak Kotagede | pengalaman | kerajinan | Kota Yogyakarta | sedang dicek | ya | ya | pending |
| Desa Wisata Tlatar | pengalaman | gamelan | Sleman | sedang dicek | sedang dicek | ya | pending |
| Desa Wisata Kasongan | pengalaman | kerajinan | Bantul | sedang dicek | sedang dicek | ya | pending |
| Desa Wisata Krebet | pengalaman | kerajinan | Bantul | sedang dicek | sedang dicek | ya | pending |
| Desa Wisata Nglanggeran | pengalaman | lainnya | Gunungkidul | sedang dicek | ya | ya | pending |
| Desa Wisata Widosari | pengalaman | lainnya | Kulon Progo | sedang dicek | sedang dicek | ya | pending |
| Makam Raja-raja Imogiri | permanen | lainnya | Bantul | ya | sedang dicek | ya | pending |
| Rumah Sandi Purwoharjo | permanen | lainnya | Kulon Progo | sedang dicek | sedang dicek | belum | ditahan |
| Candi Sambisari | permanen | candi | Sleman | sedang dicek | ya | ya | pending |
| Candi Ijo | permanen | candi | Sleman | sedang dicek | ya | ya | pending |
| Candi Banyunibo | permanen | candi | Sleman | sedang dicek | ya | ya | pending |
| Museum Affandi | permanen | museum | Sleman | ya | ya | ya | pending |
| Museum Gunungapi Merapi | permanen | museum | Sleman | sedang dicek | ya | belum | pending |
| Museum Puro Pakualaman | permanen | museum | Kota Yogyakarta | sedang dicek | sedang dicek | ya | pending |

## Perlu tindak lanjut

### Keraton Ngayogyakarta Hadiningrat
- Jam dan tarif dari indonesia.travel, bukan kratonjogja.id. Senin tidak tercantum, belum dikonfirmasi tutup.

### Taman Sari
- Tarif berlaku 1 Maret 2023 (pengumuman Dinas Pariwisata DIY); cek apakah masih berlaku.

### Museum Negeri Sonobudoyo
- Jam dan tiket dari Telkomsel Jelajah (bukan situs resmi); situs resmi tidak memuatnya. Konfirmasi ke pengelola.

### Candi Prambanan
- Jam 06.30-17.00 dari artikel 2022, checked_at dikosongkan. Tarif resmi tidak ditemukan (borobudurpark.com error).

### Museum Benteng Vredeburg
- Tiket dari halaman resmi yang bertanda 26 Mei 2019; konfirmasi masih berlaku. Koordinat dari Wikipedia.

### Museum Ullen Sentalu
- Koordinat Wikipedia id. Belum jelas apakah pemandu sudah termasuk tiket.

### Situs Ratu Boko
- Jam dan tiket KOSONG: sumber resmi PT TWC gagal dibuka. Perlu dicari ulang atau dikonfirmasi pengelola.

### Kampung Batik Giriloyo
- Tarif dari artikel 2020-2023 dan skemanya tidak konsisten (per peserta vs per paket), checked_at dikosongkan. Jam workshop, kontak, dan koordinat belum ada; situs batikgiriloyo.co.id error.

### Event di tempat yang ada
- Sendratari Ramayana Prambanan (dulu tempat YK-08) kini event di Candi Prambanan: Selasa, Kamis, Sabtu 19.30-21.00. Harga mulai Rp150.000 hanya dari artikel 2023; jadwal bertentangan antara header dan tabel halaman tiket; konfirmasi ke pengelola.
- Pagelaran Wayang Sonobudoyo: event di Museum Negeri Sonobudoyo, Selasa-Sabtu 20.00-22.00 (Wikipedia EN), mulai Rp20.000 (Telkomsel); belum diverifikasi resmi.

### Jamu Ginggang
- Tidak ada bukti program pengalaman/workshop, jadi dicatat sebagai tempat permanen. Jam dari artikel 2021 (checked_at dikosongkan). Nomor alamat hanya satu sumber. Koordinat belum ditemukan.

### Masjid Gedhe Kauman
- Jam kunjungan dan tarif tidak ditemukan; masjidgedhe.or.id memberi 403.

### Plengkung Gading
- Nama resmi Plengkung Nirbaya; Gading nama populer (pengetahuan umum, belum diverifikasi sumber). Tidak ada jam/tiket. Koordinat perkiraan dari OSM; sumber artikel 2022.

### Kampung Pecinan Ketandan
- Hanya artikel 2017 (diubah 2020), semua fakta checked_at null. Koordinat titik tengah node OSM, bukan batas kawasan.

### Living Museum Kotagede
- Jam (Selasa-Minggu 08.00-15.30, Jumat sampai 14.00) dan gratis adalah milik Museum Kotagede (pusat informasi), bukan seluruh kawasan. Koordinat titik pusat kecamatan, bukan batas kawasan.

### Taman Budaya Yogyakarta (TBY)
- Jam 07.30-16.00 dan gratis hanya dari situs travel pihak ketiga, tidak dimasukkan. Program rutin tidak ditemukan di sumber resmi.

### Sekar Rinonce
- Jadwal (Selasa dan Sabtu, tiga lokasi) hanya dari Antara 9 Sep 2022, tidak terkonfirmasi 2026. Koordinat titik jalan Malioboro, bukan panggung.

### Workshop Perak Kotagede
- Rp850.000 per tamu, sekitar 2 jam, hanya dari listing Airbnb. Alamat pasti tidak ada; situs HS Silver dan Tom's Silver diblokir (403). Koordinat perkiraan.

### Desa Wisata Tlatar
- Lokasi di Wonokerto, Turi, Sleman (bukan Tlatar Boyolali). Harga paket dan jam tidak ditemukan. Koordinat titik permukiman, perkiraan.

### Desa Wisata Kasongan
- Jam 09-21 dan tanpa tarif masuk dari indonesia.travel tanpa tanggal (checked_at null); satu direktori menyebut 09-16. Harga workshop gerabah tidak ditemukan. Koordinat perkiraan.

### Desa Wisata Krebet
- Halaman visitingjogja (diperbarui 31 Des 2025) memuat paket tanpa harga. Kontak admin ada di catatan mentah. Koordinat dari sekretariat desa wisata.

### Desa Wisata Nglanggeran
- Harga paket dari indonesia.travel, 'mulai dari', per orang kecuali Live-In (satuan tidak tertulis). Penghargaan (UNWTO 2021 vs ASEAN CBT 2017) bertentangan antarsumber. Koordinat titik tengah desa.

### Desa Wisata Widosari
- Harga paket dan jam tidak ada di sumber resmi. Koordinat memakai pusat Kalurahan Ngargosari (perkiraan).

### Makam Raja-raja Imogiri
- Jam (Senin 10-13, Jumat 13-16, Minggu 10-13) dan aturan busana dari detik (Nov 2025), bukan sumber resmi; visitingjogja 2020 menyebut buka setiap hari. Tarif resmi tidak ditemukan. Koordinat perkiraan.

### Rumah Sandi Purwoharjo
- DITAHAN: tidak terbukti dibuka untuk umum (Metro TV Apr 2026: bangunan ditinggalkan penghuninya). Lokasi bertentangan (Kaliwunglon/Banjarsari vs Purwoharjo). Konfirmasi ke BSSN/Dispar Kulon Progo sebelum dipakai.

### Candi Sambisari
- Tiket dari Perbup Sleman No. 119/2025 (resmi). Jam bertentangan antar-GNFI (2024 vs 2025), tidak ada sumber resmi, checked_at null.

### Candi Ijo
- Tiket dari Perbup Sleman No. 119/2025 (resmi). Jam 06.00-17.30 hanya dari Hipwee 2017 dan Viva 2022; sumber lain 08.00-17.00. Koordinat centroid kawasan.

### Candi Banyunibo
- Jam buka tidak ditemukan. Tiket Rp5.000 dan parkir Rp2.000 hanya dari satu artikel GNFI Juni 2026, bukan resmi.

### Museum Affandi
- Data dari situs resmi affandi.org. Koordinat dari OSM, belum dicek ke sumber resmi.

### Museum Gunungapi Merapi
- Tiket dari ANTARA 27 Des 2024. Jam bertentangan/lama, dikosongkan. Koordinat tidak ditemukan di OSM. BPPTKG dan Dispar Sleman belum dibuka.

### Museum Puro Pakualaman
- Harga dan jam bertentangan antarsumber dan sudah lebih dari 2 tahun (checked_at null). Koordinat dari titik Pura Pakualaman di OSM (perkiraan).
