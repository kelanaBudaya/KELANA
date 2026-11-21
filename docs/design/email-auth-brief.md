# Brief desain: masuk dan daftar dengan email + password

Untuk dibuat di Claude Design nanti (token sama dengan layar Masuk `MasukIlustratif`). Layar sementara sudah dibangun dari token yang ada; setelah desain jadi, salin ke `docs/design/artifact/` supaya bisa dicocokkan.

## Aturan alur (sudah diputuskan)
- Daftar: email + password, akun langsung aktif. **Tanpa verifikasi kode email** (rencana itu ditunda sebagai pekerjaan mendatang, karena butuh SMTP sendiri).
- Masuk: email + password. Google tetap ada. Tamu tetap boleh "Jelajah dulu tanpa akun".
- **Tidak ada** "Lupa password" untuk sekarang (butuh pengiriman email). Konsekuensinya, pengguna yang lupa password tidak bisa memulihkan akun lewat aplikasi.
- Password: minimal 8 karakter, harus ada huruf dan angka.
- Email yang sama dengan akun Google: Supabase hanya menautkan jika email terverifikasi. Karena email daftar tidak diverifikasi, akun email dan akun Google dengan alamat sama bisa menjadi dua akun terpisah. Tidak ada layar khusus.

## Layar yang dibutuhkan
1. **Masuk (diperbarui):** tombol Google yang ada + pemisah "atau" + opsi masuk dengan email. Tautan ke Daftar dan "Jelajah dulu tanpa akun".
2. **Masuk dengan email:** kolom email, password (tampilkan/sembunyikan), tombol "Masuk", tautan ke Daftar.
3. **Daftar:** kolom email, password, ulangi password, petunjuk aturan password, tombol "Daftar", tautan ke Masuk.

## State yang harus digambar
- Kolom: normal, fokus, error (teks di bawah kolom), terisi.
- Tombol: normal, memuat (spinner), nonaktif.
- Error: email tidak valid, password terlalu lemah, email sudah terdaftar, email atau password salah, tidak ada sinyal, server belum dikonfigurasi.

## Catatan copy
- "Email atau password salah" tidak boleh membedakan email yang tidak ada dari password yang salah.
- Semua teks Bahasa Indonesia, nanti ditambah terjemahan Inggris.
