# Phase 7 · Agent A: Postcard (7.1 Tulis, 7.2 Bagikan)

Worktree `D:\PROJECT\BudayaGo\m\postcard` (branch `feat/postcard`). Read `docs/agents/phase7-common.md` first; it is binding.

## Screens
| No. | Screen | Artifact file |
|---|---|---|
| 7.1 | Tulis postcard | `docs/design/artifact/TulisPostcardIlustratif.dc.html` |
| 7.2 | Bagikan postcard | `docs/design/artifact/BagikanPostcardIlustratif.dc.html` |
| ref | The card as others see it | `docs/design/artifact/Postcard.dc.html` |

Video (7.3) is cancelled: no video button or route.

## Files you may touch outside `:feature:postcard`
- NEW `android/core/navigation/.../PostcardRoutes.kt` for any extra route (e.g. Bagikan). `PostcardRoute` itself stays as it is.
- `android/app/.../navigation/KelanaNavHost.kt`: only the `postcardScreen(...)` call (new parameters such as `navigate`, `onBack`).
- NEW files in `android/core/data/`: `SupabasePostcardRepository.kt` (real), `FakePostcardRepository.kt` (tests/previews, not bound), `di/PostcardDataModule.kt` (binds `PostcardRepository` to the Supabase one, which returns `NotConfigured` when `SupabaseClientProvider.client` is null), and their tests.
- NEW migration `supabase/migrations/20261123000001_postcard_fields.sql`, plus `supabase/tests/rls.test.sql`, `supabase/contract/*` and `supabase/README.md` where postcards appear.

## Backend change (the design needs fields the table lacks)
Add to `public.postcards`: `rating smallint null check (rating between 1 and 5)`, `card_style text not null default 'jurnal' check (card_style in ('jurnal','cap','peta'))`, `is_public boolean not null default true`. Replace policy `postcards_read` with `using (is_public or user_id = (select auth.uid()))` so a private postcard is visible to its author only. Insert still requires a stamp (keep `postcards_insert` as is). Add pgTAP cases: private postcard hidden from another user, visible to its author; insert without a stamp refused. Run the tests with the fallback in `supabase/README.md` if Docker works; if it does not, say plainly that the SQL was not executed.

## Behavior
- Login required. A guest who reaches the screen gets `NeedsLogin` handling (send to `MasukRoute`).
- `placeId == null`: let the user pick one of their stamped places (`StampRepository.stamps()` + `PlaceRepository`). No stamps: an empty state that leads to Paspor (`PasporRoute`). With a `placeId` the user holds no stamp for: explain a stamp is needed first. The client check is only a hint; the server (RLS) decides, and `NeedsStamp` from `publish` must be handled too.
- Photo: system Photo Picker (`PickVisualMedia`, images only) and, for "kamera", `ActivityResultContracts.TakePicture` into a FileProvider URI. Do not declare the CAMERA permission. Decode, downscale to at most 1600 px on the long side, and re-encode as JPEG (quality about 85, under 5 MB). Re-encoding drops EXIF, including any GPS tag: that is required, because location must never leave the device through a photo. Add a unit test that the encoder output carries no EXIF location.
- "Seberapa berkesan?": 1 to 5 stars, optional, never preselected. "Pesanmu": 1 to 500 characters after trimming, with a counter. "Gaya kartu": Jurnal / Cap / Peta. Switch "Tampilkan di halaman <nama tempat> ..." is on by default and maps to `isPublic`.
- "Lihat postcard" publishes through `PostcardRepository.publish`, then opens Bagikan. Map every `PostcardPublishResult` to a clear state (sending, error with retry, needs stamp, not configured). The storage path is `<user_id>/<place_id>/<uuid>.jpg` (the bucket policy requires that prefix).
- Bagikan: render the chosen card style as a Compose layout to a Bitmap at 1080x1920 ("9:16 Story") and 1080x1080 ("1:1 Feed"). Fraunces for the card headline, as the design does. "DIKUNJUNGI · <tanggal>" is the stamp date from `StampRepository`, never today's date by default. Share through a FileProvider (`${applicationId}.postcard.fileprovider`, files in `cacheDir/postcards/`, old files cleaned): Instagram Stories (`com.instagram.share.ADD_TO_STORY`) when installed, WhatsApp, and the system share sheet. No TikTok. "Simpan" writes to MediaStore `Pictures/KELANA` on API 29+; on API 26 to 28 hide it (PROVISIONAL), so no storage permission is declared.
- "Bagikan sekali, cap edisi Jurnal terbuka" has no backend: leave it out and list it as an open item.
- Published postcards will not show in Cerita yet (`EmptyStoryRepository` stays bound). That is known and out of scope; do not change it.

## Done means
`assembleDebug lintDebug testDebugUnitTest` pass. Tests cover: Tulis ViewModel (validation, each publish result, the stamped-place picker), Bagikan ViewModel, the Supabase repository's mapping of errors (RLS refusal becomes `NeedsStamp`, no client becomes `NotConfigured`, network becomes `NetworkError`) with a faked API layer and no network, and the EXIF strip. Report as in the common brief, including whether the SQL tests actually ran.
