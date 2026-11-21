> **Superseded in part:** the design source is now the artifact in `docs/design/artifact/` (see its README). Fonts there are Plus Jakarta Sans + Fraunces, not Inter. The colors below stay valid; use the artifact for everything else.

# Figma frame map

File: https://www.figma.com/design/S740rinPV6M9cUYpHzALtv/KELANA (page `UI Design`, node `42:2`).
All app frames are 390 px wide (mobile). Node ids below are `fileKey S740rinPV6M9cUYpHzALtv`.
Extracted by the planner on 2026-11-21. Coding agents cannot call Figma, so ask the planner for a screenshot or design context by node id.

## Frames by flow

| Flow | Frames (node id) |
|---|---|
| 01 New user | 01.01 Mulai `43:32848`, 01.02 Kenalan 1 `43:32922`, 01.03 Kenalan 2 `43:33060`, 01.04 Kenalan 3 `43:33237`, 01.05 Pilih daerah `43:33430`, 01.06 Jelajah `43:48794` |
| 02 Cari dan nilai tempat | 02.01 Cari `43:34692`, 02.02 Kategori Museum `43:35089`, 02.03 Detail tempat `43:35680`, 02.04 Cerita pengunjung `43:36569`, 02.05 Foto resmi `43:51554`, 02.06 Video pengunjung `43:51215`, 02.07 Laporkan `43:51322` |
| 03 Pertunjukan | 03.01 Agenda `43:53743`, 03.02 Detail pentas `43:38625`, 03.03 Atur pengingat `43:38447`, 03.04 Ke situs tiket resmi `43:39080` |
| 04 Menyusun rencana | 04.01 Rencana kosong `43:39194`, 04.02 1-6 Tanggal `43:39499`, 04.03 2-6 Teman jalan `43:39703`, 04.05 4-6 Waktu `43:40181`, 04.06 5-6 Kendaraan `43:40358`, 04.07 6-6 Kebutuhan `43:40511`, 04.08 Menyusun `43:40721`, 04.09 Izin notifikasi `43:40894`, 04.10 Rencana hari ini `43:41066` |
| 05 Berangkat dan perubahan | 05.01 Notifikasi `43:41567`, 05.02 Peta rute `43:41868`, 05.03 Tempat tutup mendadak `43:42065`, 05.04 Tanpa internet `43:42320` |
| 06 Ambil cap | 06.01 Izin lokasi `43:42509`, 06.02 Ambil cap `43:42695`, 06.03 Cap berhasil `43:42937`, 06.04 Gagal di luar area `43:43199`, 06.05 Gagal lokasi ditolak `43:43348` |
| 07 Postcard, video, paspor | 07.01 Tulis postcard `43:43493`, 07.02 Bagikan postcard `43:43728`, 07.03 Unggah video `43:44037`, 07.04 Paspor budaya `43:44208` |
| 08 Akun | 08.01 Masuk `43:44720`, 08.02 Profil `43:44850` |
| 09 Kategori jelajah | 09.01 Candi `43:45336`, 09.02 Tari `43:46015`, 09.03 Topeng kosong `43:45677`, 09.04 Gamelan `43:46605`, 09.05 Tiket resmi `43:47048`, 09.06 Peta `43:47570`, 09.07 Semua tempat `43:47958` |
| 10 Web pengelola | 10.01 Panel verifikasi `43:15949` (1280 px wide, admin web, not Android) |

Note: 03.xx has no "03.05"; 04.04 is absent from the file (numbering gap, not a missing screen we know of).

## Bottom navigation (from 01.06 Jelajah, node `43:49659`)

Four tabs, in order: **Jelajah, Agenda, Rencana, Cerita**. Paspor is not a tab: it is reached from the Jelajah header card ("Paspor budayamu") and the avatar (Profil). Active label is bold navy, inactive regular muted. Bar height 84 px.

## Design tokens

Confirmed from Figma node styles:

| Token | Hex | Use |
|---|---|---|
| `ink` | `#243047` | primary text |
| `muted` | `#566072` | secondary text, inactive tab |
| `navy` | `#1B3671` | active tab, stamp text |
| `skyText` | `#DCE6F7` | secondary text on dark header |
| `white` | `#FFFFFF` | text on dark |

Font: **Inter** (Regular 400, Bold 700). Sizes seen: 9, 11, 12 (caption/tab), 14 (card title), 17 (header value). Layout: 16 dp side gutter, 358 dp card width.

Read from Figma fills on 7 key frames (Jelajah, Detail tempat, Ambil cap, Cap berhasil, Rencana hari ini, Mulai, Peta), most used first. Treat as confirmed unless marked guess:

| Token | Hex | Where seen |
|---|---|---|
| `surface` | `#FFFFFF` | cards, sheets |
| `background` | `#FFFCF6` | screen background (cream) |
| `backgroundWarm` | `#F8F3E8` | warm panels |
| `primary` | `#24458F` | buttons, links ("Lihat semua"), header blue |
| `primaryDeep` | `#133476` and `#1A3885` | deep navy shapes and illustrations |
| `primaryBright` | `#1D499A`, `#2D52A9` | illustration accents |
| `accentOrange` | `#C94D1A` | CTA buttons ("Lihat kisahnya", Ambil cap), orange card |
| `accentOrangeBright` | `#F5831B`, `#E5591A` | pins, illustration |
| `stampBrown` | `#A84E2B` | Cap card background |
| `stampYellow` | `#F4B52B`, `#FDB42C` | stamp, highlights, "Ayo berkelana" button |
| `creamYellow` | `#FFF0C8`, `#FFF6DC` | info banners |
| `warnText` | `#8A5A00`, `#5C3D00` | text on yellow banners |
| `successText` | `#35745A`, `#1F4D3A` | "Diperiksa", "Kamu ada di area" |
| `success` | `#468A5E` | check icons |
| `successSurface` | `#E3F1EA` | success banner background |
| `iconMuted` | `#8C93A0` | inactive icons |

Fonts (confirmed): **Inter** Regular and Bold for UI; **Fraunces** Regular, Bold and Italic for headlines, titles on dark screens, postcard and "KELANA" wordmark. Text sizes in use: 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 20, 22, 26, 30. Most common are 12 (body small), 13, 14 (card title), 11 (button label). Danger red, card corner radius, button height and exact spacing were not readable from fills (vectors); keep PROVISIONAL.

Bottom-bar icons (frame 01.06, node `43:49659`): Jelajah = compass (`docs/assets/Discover.svg`), Agenda = calendar (`docs/assets/Jadwal.svg`), Rencana = route with pin (`docs/assets/Tujuan.svg`), Cerita = camera (`docs/assets/Kamera.svg`). Inactive icons use `iconMuted`. The active tab icon sits on a light-blue pill (~`#DCE6F7`) about 64 x 32 dp, label bold `navy`. Verify the icon-to-file mapping by looking at the SVGs before using them.
