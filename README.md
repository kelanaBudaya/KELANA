# KELANA

**Kawan Eksplorasi Budaya Nusantara** — pemandu wisata budaya kita semua.

KELANA membantu wisatawan menjelajahi tempat dan pertunjukan budaya dengan informasi yang jujur: setiap fakta resmi (jam buka, harga tiket, hari tutup, jadwal pentas) selalu menampilkan **tanggal pemeriksaannya**.

> Status: **pengembangan awal** (versi 0.1.0). 

---

## 1. Fitur Utama

- **Jelajah** tempat budaya (museum, candi, keraton, kerajinan) dan kategori pertunjukan (tari, wayang/topeng, gamelan).
- **Info resmi + tanggal periksa**: tanpa tanggal periksa, aplikasi menampilkan "sedang dicek", bukan angka yang belum diverifikasi.
- **Agenda budaya**: jadwal pentas per hari, dengan detail pentas dan tombol ke situs tiket resmi.
- **Rencana harian**: pertanyaan singkat (6 langkah) menghasilkan urutan kunjungan yang sesuai jam buka resmi.
- **Paspor budaya**: kumpulkan cap di setiap tempat. Cap diverifikasi **di server**, bukan oleh aplikasi.
- **Cerita pengunjung**: postcard dan video, hanya dari akun yang sudah mengambil cap di tempat tersebut.

---

## 2. Cara penggunaan


**Memasang**

1. Dapatkan berkas APK dari tim (mis. `KELANA-budaya.apk`).
2. Di HP Android 8.0 atau lebih baru, izinkan *Pasang aplikasi tak dikenal* untuk aplikasi yang membuka berkas (File saya / Chrome).
3. Buka berkas APK dan ketuk **Pasang**. Jika muncul peringatan Play Protect, ketuk **Tetap pasang**.

**Memakai aplikasi**

| Langkah | Yang dilakukan |
|---|---|
| 1. Mulai | Buka aplikasi → lewati 3 halaman perkenalan → **Pilih wilayah** → ketuk **Berkelana di DI Yogyakarta**. |
| 2. Masuk (opsional) | Ketuk **Masuk** lalu pilih Google atau email + password (min. 8 karakter, huruf dan angka). Atau pilih **Jelajah dulu tanpa akun**. |
| 3. Jelajah | Tab **Jelajah**: pilih kategori (Museum, Candi, Tari, Topeng, Gamelan) atau cari tempat. Ketuk kartu tempat untuk melihat **Info resmi** beserta tanggal diperiksa. |
| 4. Agenda | Tab **Agenda**: pilih tanggal, saring (Tari, Wayang, Ramah anak), ketuk pentas untuk detail. **Beli tiket resmi** membuka situs penjual resmi. |
| 5. Rencana | Tab **Rencana** → tombol untuk menyusun rencana pertama → jawab 6 pertanyaan (tanggal, teman jalan, minat, jam dan tempo, kendaraan, kebutuhan khusus) → **Susun rencanaku**. |
| 6. Ambil cap | Datang ke tempatnya → buka halaman tempat → ketuk banner cap → izinkan lokasi → **Cap sekarang**. Wajib login. |
| 7. Paspor | Kartu **Paspor budayamu** di Jelajah, atau lewat profil. Menampilkan cap yang sudah dikonfirmasi server. |

---

## Teknologi

Kotlin 2.2, Jetpack Compose + Material 3, Navigation Compose (rute bertipe), MVVM + Clean Architecture, Coroutines + Flow, Hilt, Room, DataStore, WorkManager, Retrofit + OkHttp + kotlinx.serialization, supabase-kt (Auth, Postgrest, Functions), Credential Manager (Google), Play Integrity, Google Play Services Location. Backend: Supabase (Postgres + PostGIS, RLS, Storage, Edge Functions/Deno). CI: GitHub Actions.

## Struktur repositori

```
android/    Proyek Gradle (app, core, feature)
supabase/   migrasi, kebijakan RLS, Edge Function, seed, tes
docs/       desain (artifact), brief agen, dokumen teknis
```
