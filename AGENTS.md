# KELANA — Agent Guide

KELANA ("Kawan Eksplorasi Budaya Nusantara") is a native Android travel guide for cultural tourism across Indonesia. It launched with DI Yogyakarta. A region opens in the app (status `available`) as soon as its data exists in the database: places with their official facts and, where the schema allows, events. Regions without data yet show as "coming soon".

Design source of truth: the Claude Design artifact https://claude.ai/artifact/9CFY5pcyzvTJi9rVu49FN8 (same design as the Figma file, same screen numbers). Local copies and the screen map are in `docs/design/artifact/` (read its README first). Tokens there override `docs/design/FIGMA-FRAMES.md`: font is Plus Jakarta Sans (+ Fraunces for postcard and highlights), not Inter. Figma (`UI Design` page, node `42:2`) is a fallback only; its MCP exports are rate-limited. The PNGs in `docs/design/` are overview slides for context only.

## Product rules (never break)

- **No payments.** No payment SDK, no checkout. Ticket prices are shown as information, and "Buka situs" opens the official seller in a Custom Tab.
- **Location is read once, never tracked.** Call `FusedLocationProviderClient.getCurrentLocation()` only from the "Ambil cap" button handler. No background location, no `ACCESS_BACKGROUND_LOCATION`, no location stored on-device or sent anywhere except the single stamp-verification request. Block mock locations and use Play Integrity.
- **Stamp is verified on the server** (PostGIS distance check). The client never decides that a stamp was earned.
- **Stamping works offline by queueing.** With weak signal, "Ambil cap" takes the single location fix and queues one pending stamp request locally (fix, accuracy, capture time), then sends it when online. The fix lives only in that pending request and is deleted as soon as the server answers. The server still decides. A queued request expires after **30 minutes**: the client drops it and asks the user to try again, and the server rejects any request whose capture time is older than that. The Play Integrity token is requested at **send time**, with a nonce bound to a hash of the request payload, never stored in the queue. The UI shows a "menunggu sinyal" state until the server confirms; the stamp never appears in the passport before that.
- **Place photos are open-licensed** (Wikimedia Commons CC0/PD/CC BY/CC BY-SA), curated into the public `place-photos` bucket and the `place_photos` table (no client writes), and always shown with their attribution; never images with an unclear license.
- **Stories (postcards, videos) only from accounts that hold a stamp for that place.** Enforced by Postgres Row Level Security, not just by the app.
- **Stories go live immediately.** No pre-moderation: an upload is visible to other users in real time (Supabase Realtime). Reports and flagged stories are read by an admin later (low priority), so the story feed must keep working with no admin action.
- **Official info is gated by its check date, but the date and the source are not shown.** Any official fact (hours, price, closed day, show time) without a `checkedAt` date must not be rendered; use the "Harga sedang dicek" state instead. Every fact still stores `checkedAt` and `sourceUrl`, but the place detail does not display the "Diperiksa ..." label or the source (product decision, Nov 2026; the label may return later).
- **Place detail shows admission prices only** ("Tiket masuk"): other products (workshop, parking, costume rental, guide, performance tickets) are not stored in `ticket_prices`. With several admission categories the prices are listed one per line, label left and price right.
- **Performances and experiences hosted at a place are events linked to it** (`events.place_id`), shown on that place's detail ("Pertunjukan di sini") and in Agenda. They are not separate places.
- **Visitor videos on place detail come from TikTok embeds** (`place_videos`, curated by the team, readable by guests, no client writes). They are separate from stories: stories stay stamp-gated and RLS-enforced. The player is TikTok's Embed Player (`player/v1/{id}`, the older `embed/v2` is being deprecated), a third-party WebView restricted to tiktok.com that loads only when its card is shown and loads TikTok's own trackers (mention it in the privacy policy and the Play Data Safety form). TikTok may rate-limit a domain, so a failing player shows "Buka di TikTok" instead of breaking the screen.
- **Addresses are plain text**: no source suffix such as "(alamat dari OpenStreetMap)".
- **Official info is scraped by an AI agent** for the first 10 places. Every scraped fact stores its `sourceUrl` and `checkedAt`, and `checkedAt` is set only when the source was actually fetched and parsed. A failed fetch keeps the old date and never refreshes it. The first scrape of a place and any value that differs from the published one go to a pending state and are published only after a human approves them (via the Supabase dashboard until the admin exists). An unchanged re-check just refreshes `checkedAt`.
- **Offline planner:** the cached plan is readable and usable offline. Building a plan and live re-planning run on the device from the cached places and events, so they also work without signal; the Rencana tab only shows a non-blocking offline status. A server planner (Edge Function) may replace the on-device builder later behind the same `PlanRepository`. Without any cached place data, building shows an error with retry.
- **Guests can browse; an account is needed to act.** Figma frame 08.01 has "Jelajah dulu tanpa akun". Without login a guest can read regions, places, published official facts and events (Jelajah, Detail tempat, Agenda, Peta, Rencana). Ambil cap, Postcard, video, Laporkan, Kabari aku and visitor stories need a login (stories are not readable by guests). Login is Google (Credential Manager, ID token exchanged with Supabase Auth) or email + password (min 8 characters, letters and digits). Email sign-up is **not verified** for now: the account is active immediately (Supabase "Confirm email" off). Email code verification, password reset by email and phone OTP are future work and need a custom SMTP provider first. Auth and session use the supabase-kt client.

## Stack

- Kotlin, Jetpack Compose + Material 3, Navigation Compose (type-safe routes), minSdk 26, applicationId `id.kelana.app`
- MVVM + Clean Architecture; Coroutines + Flow; Hilt
- Retrofit + OkHttp + kotlinx.serialization; Room + DataStore (offline cache for places, today's plan, passport)
- Coil (images), Media3 ExoPlayer (visitor video), CameraX or Photo Picker (postcards)
- Maps: **MapLibre + OpenStreetMap**, styled to match the Figma illustrated look. Do not add the Google Maps SDK. Place detail may link out to Google Maps (icon in the header, `geo:` intent with a web fallback) to check a place's location; this is a plain link with the place's coordinates, not an SDK and not the user's location. Show the "© OpenStreetMap contributors" attribution on every map. The public OSM tile server is for development only: use a tile provider or own tile server before public launch.
- Notifications: FCM for server pushes, WorkManager/AlarmManager for local "time to leave" reminders
- Postcard: render a Compose layout to Bitmap, share via FileProvider (IG Stories intent, TikTok Share Kit, WhatsApp `ACTION_SEND`)
- Backend (same repo): Supabase (Postgres + PostGIS, Auth, Storage, Edge Functions); video on Cloudflare Stream or Mux; admin CMS in Next.js
- Quality: Firebase Crashlytics + Analytics, JUnit, Turbine, Compose UI Test, Maestro; GitHub Actions + Gradle Version Catalogs, release to Play Internal Testing

## Repository layout

```
android/      Gradle project (app, core, feature modules)
supabase/     migrations, RLS policies, Edge Functions
admin/        Next.js admin (hours, tickets, "tanggal periksa", user reports)
docs/design/  overview PNGs (context only)
```

## Android module rules

- **One feature = one Gradle module** (`:feature:<name>`). **Features never import each other.**
- Allowed dependencies: `:feature:*` → `:core:*`; `:app` → everything. `:core:*` never depends on a feature.
- Shared modules: `:core:model`, `:core:data`, `:core:ui`, `:core:designsystem`, `:core:navigation`.
- **Cross-feature navigation**: typed routes live in `:core:navigation`. A feature navigates by route object only. `:app` owns the `NavHost` and registers every destination. If feature A needs something from feature B, move the shared piece into `:core:*`.
- Inside a feature: `data` / `domain` / `ui` packages. UI talks to a ViewModel, the ViewModel to use cases, use cases to repository interfaces (defined in `domain`, implemented in `:core:data`).
- Feature modules (one per design flow): `onboarding` (intro + Pilih wilayah), `akun` (masuk, profil), `jelajah`, `tempat` (detail, galeri foto, laporkan), `cerita` (visitor stories feed), `agenda`, `rencana` (6-step planner + live re-plan), `peta`, `paspor` (Ambil cap + passport), `postcard`, `notifikasi`, `tiket`.
- Bottom tabs, in order: Jelajah, Agenda, Rencana, Cerita. Paspor is reached from the Jelajah header card and from Profil (the avatar opens Profil, per the artifact), not from a tab.
- Frame map and design tokens: `docs/design/FIGMA-FRAMES.md`. Agent briefs: `docs/agents/`.

## Conventions

- **All user-facing text lives in `strings.xml`, never hardcoded.** Bahasa Indonesia in `values/`, English in `values-en/`. A new string needs both.
- **Every ViewModel has a unit test** (JUnit + Turbine for Flows).
- UI follows the Figma frame; use design tokens from `:core:designsystem`, no ad-hoc colors, sizes or fonts in feature code.
- Dates and prices are formatted for `id-ID` (e.g. "Jumat, 20 November", "Rp150.000").
- Keep secrets (Supabase keys, `google-services.json`, signing keys) out of git.

## Definition of done

A screen or feature is done only when all three hold:

1. It **builds** (`./gradlew assembleDebug`).
2. Tests **pass** (`./gradlew test`, plus relevant instrumented/Compose tests).
3. The screen **matches its artifact screen** (`docs/design/artifact/`, same numbering as Figma): copy, layout, spacing, type, colors, states (empty, loading, error, offline). Where the artifact shows `[ISI]`, the app shows real data with its check date or "sedang dicek".

Report which of the three were actually run. If the artifact screen was not compared or the app was not run on a device/emulator, say that plainly. Do not claim a visual match you did not check.

## Build order

1. DI Yogyakarta only. Jelajah + Detail tempat + Agenda
2. Ambil cap + Paspor
3. Postcard + sharing
4. Planner and live re-planning last (most complex)

## Open items (decide before building the related feature)

- Cap radius per place and acceptable GPS accuracy threshold (the design shows `[ISI]` placeholders)
- Pending-stamp state ("menunggu sinyal") is not in Figma yet; design it before building Ambil cap
- Admin review of reports and flagged stories is deferred. Decide before public launch whether a flagged story stays visible or is hidden after N flags
- Planner error states (build failed, no place data, plan not loaded, re-plan not saved) are not in Figma yet; the app reuses the offline screen layout for them. The wheel time picker (Susun 4/6) and the Ojek online option (Susun 5/6) deviate from the artifact by user decision.
