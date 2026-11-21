-- KELANA schema. Identifiers: snake_case English. Content values: Bahasa Indonesia.
create extension if not exists postgis with schema extensions;

-- ---------------------------------------------------------------- enums
create type public.region_status as enum ('available', 'upcoming', 'not_open');
create type public.place_category as enum
  ('museum', 'candi', 'tari', 'topeng', 'gamelan', 'kerajinan', 'lainnya');
create type public.review_status as enum ('pending', 'published');
create type public.recurrence_kind as enum ('once', 'weekly', 'daily');
-- Mirrors frame 02.07 "Laporkan".
create type public.report_reason as enum (
  'hours_or_price_changed',            -- Jam atau harga berubah
  'place_closed',                      -- Tempat tutup
  'ceremony_shown_without_permission', -- Upacara ditampilkan tanpa izin
  'inappropriate_media',               -- Foto atau video tidak pantas
  'other'                              -- free text only
);

-- ---------------------------------------------------------------- regions
create table public.regions (
  id         bigint generated always as identity primary key,
  code       text not null unique,
  name       text not null,
  status     public.region_status not null default 'not_open',
  sort_order int  not null default 0,
  is_sample  boolean not null default false,
  created_at timestamptz not null default now()
);

-- ---------------------------------------------------------------- places
create table public.places (
  id             uuid primary key default gen_random_uuid(),
  region_id      bigint not null references public.regions(id),
  slug           text not null unique,
  name           text not null,
  category       public.place_category not null,
  cap_code       text not null unique check (cap_code ~ '^[A-Z]{2,3}-[0-9]{2,3}$'),
  passport_order int,
  -- Stamp radius is still [ISI] in the design: nullable until decided.
  cap_radius_m   int check (cap_radius_m is null or cap_radius_m > 0),
  location       extensions.geography(Point, 4326) not null,
  address        text,
  description    text,
  cover_path     text,
  is_sample      boolean not null default false,
  created_at     timestamptz not null default now()
);
create index places_location_gix on public.places using gist (location);
create index places_region_idx on public.places (region_id, category);

-- ------------------------------------------------- official fact tables
-- Common columns: source_url, checked_at (nullable: the app then shows
-- "Harga sedang dicek"), review_status (clients only see 'published'), is_sample.

create table public.opening_hours (
  id            uuid primary key default gen_random_uuid(),
  place_id      uuid not null references public.places(id) on delete cascade,
  weekday       smallint not null check (weekday between 1 and 7), -- ISO: 1=Senin .. 7=Minggu
  is_closed     boolean not null default false,
  opens_at      time,
  closes_at     time,
  note          text,
  source_url    text,
  checked_at    timestamptz,
  review_status public.review_status not null default 'pending',
  is_sample     boolean not null default false,
  check (is_closed or (opens_at is not null and closes_at is not null))
);
create index opening_hours_place_idx on public.opening_hours (place_id, weekday);

create table public.closures (
  id            uuid primary key default gen_random_uuid(),
  place_id      uuid not null references public.places(id) on delete cascade,
  starts_on     date not null,
  ends_on       date not null,
  reason        text,
  source_url    text,
  checked_at    timestamptz,
  review_status public.review_status not null default 'pending',
  is_sample     boolean not null default false,
  check (ends_on >= starts_on)
);
create index closures_place_idx on public.closures (place_id, starts_on);

create table public.ticket_prices (
  id            uuid primary key default gen_random_uuid(),
  place_id      uuid not null references public.places(id) on delete cascade,
  label         text not null,
  price_idr     integer check (price_idr is null or price_idr >= 0), -- null = unknown
  seller_url    text,   -- "Buka situs" opens this in a Custom Tab. Never a checkout.
  source_url    text,
  checked_at    timestamptz,
  review_status public.review_status not null default 'pending',
  is_sample     boolean not null default false
);
create index ticket_prices_place_idx on public.ticket_prices (place_id);

-- Event recurrence model (deliberately simple):
--   once   -> event_date set, weekday null
--   weekly -> weekday set (ISO 1..7), event_date null; repeats every week
--   daily  -> weekday and event_date both null
-- start_time/end_time are local Asia/Jakarta wall-clock times.
-- Cancelled dates are expressed with `closures` on the place.
create table public.events (
  id             uuid primary key default gen_random_uuid(),
  place_id       uuid not null references public.places(id) on delete cascade,
  title          text not null,
  description    text,
  category       public.place_category not null default 'tari',
  recurrence     public.recurrence_kind not null,
  weekday        smallint check (weekday between 1 and 7),
  event_date     date,
  start_time     time not null,
  end_time       time,
  price_from_idr integer check (price_from_idr is null or price_from_idr >= 0),
  seller_url     text,
  source_url     text,
  checked_at     timestamptz,
  review_status  public.review_status not null default 'pending',
  is_sample      boolean not null default false,
  check (
    (recurrence = 'once'   and event_date is not null and weekday is null) or
    (recurrence = 'weekly' and weekday is not null and event_date is null) or
    (recurrence = 'daily'  and weekday is null and event_date is null)
  )
);
create index events_place_idx on public.events (place_id);

-- ---------------------------------------------------------------- users
-- The app's "users" table: 1:1 with auth.users.
create table public.profiles (
  id             uuid primary key references auth.users(id) on delete cascade,
  display_name   text,
  avatar_path    text,
  home_region_id bigint references public.regions(id),
  is_sample      boolean not null default false,
  created_at     timestamptz not null default now()
);

create function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  insert into public.profiles (id, display_name)
  values (new.id, coalesce(new.raw_user_meta_data ->> 'full_name', new.raw_user_meta_data ->> 'name'));
  return new;
end $$;
revoke all on function public.handle_new_user() from public, anon, authenticated;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------- stamps
-- Written only by a service-role Edge Function (verify-stamp) after the
-- PostGIS distance check. No location is stored, only the verified result.
create table public.stamps (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references auth.users(id) on delete cascade,
  place_id   uuid not null references public.places(id) on delete cascade,
  stamped_at timestamptz not null default now(),
  is_sample  boolean not null default false,
  unique (user_id, place_id)
);
create index stamps_place_idx on public.stamps (place_id);

-- ---------------------------------------------------------------- stories
create table public.postcards (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references auth.users(id) on delete cascade,
  place_id   uuid not null references public.places(id) on delete cascade,
  message    text not null check (char_length(message) between 1 and 500),
  photo_path text,
  is_sample  boolean not null default false,
  created_at timestamptz not null default now()
);
create index postcards_place_idx on public.postcards (place_id, created_at desc);
create index postcards_feed_idx on public.postcards (created_at desc);

create table public.videos (
  id             uuid primary key default gen_random_uuid(),
  user_id        uuid not null references auth.users(id) on delete cascade,
  place_id       uuid not null references public.places(id) on delete cascade,
  caption        text check (caption is null or char_length(caption) <= 300),
  stream_uid     text,   -- Cloudflare Stream / Mux asset id
  thumbnail_path text,
  duration_s     int check (duration_s is null or duration_s > 0),
  is_sample      boolean not null default false,
  created_at     timestamptz not null default now()
);
create index videos_place_idx on public.videos (place_id, created_at desc);
create index videos_feed_idx on public.videos (created_at desc);

-- ---------------------------------------------------------------- reports
-- Read by an admin later (service role / dashboard). Clients see only their own.
create table public.reports (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null references auth.users(id) on delete cascade,
  place_id    uuid references public.places(id) on delete cascade,
  postcard_id uuid references public.postcards(id) on delete cascade,
  video_id    uuid references public.videos(id) on delete cascade,
  reason      public.report_reason not null,
  details     text check (details is null or char_length(details) <= 1000),
  photo_path  text,
  created_at  timestamptz not null default now(),
  check (place_id is not null or postcard_id is not null or video_id is not null)
);
create index reports_user_idx on public.reports (user_id);

-- ---------------------------------------------------------------- subscriptions
-- "Kabari aku": exactly one of category / place.
create table public.subscriptions (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references auth.users(id) on delete cascade,
  category   public.place_category,
  place_id   uuid references public.places(id) on delete cascade,
  created_at timestamptz not null default now(),
  check ((category is not null)::int + (place_id is not null)::int = 1)
);
create unique index subscriptions_user_category_uq on public.subscriptions (user_id, category) where category is not null;
create unique index subscriptions_user_place_uq on public.subscriptions (user_id, place_id) where place_id is not null;

-- ---------------------------------------------------------------- realtime
-- Stories go live immediately; clients subscribe to inserts.
alter publication supabase_realtime add table public.postcards, public.videos;
