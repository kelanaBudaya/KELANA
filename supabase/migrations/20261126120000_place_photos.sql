-- Photo gallery on Detail tempat: open-licensed photos (Wikimedia Commons / Openverse) with
-- license metadata, files in the public Storage bucket `place-photos`.
-- Rows and files are written by the team with the service role (upload script / dashboard).
-- Clients can only read published rows. Guests may read: curated public content.

create table public.place_photos (
  id               uuid primary key default gen_random_uuid(),
  place_id         uuid not null references public.places(id) on delete cascade,
  storage_path     text not null unique,   -- e.g. diy/museum-sonobudoyo/3f9a1c2e.jpg
  thumb_path       text not null unique,   -- e.g. diy/museum-sonobudoyo/3f9a1c2e_t.jpg
  width            integer not null check (width > 0),
  height           integer not null check (height > 0),
  sha1             text not null,
  source           text not null check (source in ('wikimedia_commons', 'openverse')),
  source_page_url  text not null check (source_page_url ~ '^https://'),
  original_url     text,
  caption          text,
  author           text not null check (length(btrim(author)) > 0),
  author_url       text,
  license_code     text not null check (license_code in (
                     'CC0-1.0', 'PDM',
                     'CC-BY-2.0', 'CC-BY-2.5', 'CC-BY-3.0', 'CC-BY-4.0',
                     'CC-BY-SA-2.0', 'CC-BY-SA-2.5', 'CC-BY-SA-3.0', 'CC-BY-SA-4.0')),
  license_url      text not null check (license_url ~ '^https://'),
  attribution_text text not null,          -- "Foto: <author> · CC BY-SA 3.0 · Wikimedia Commons"
  sort_order       integer not null default 0,
  is_published     boolean not null default false,
  checked_at       timestamptz not null default now(),
  created_at       timestamptz not null default now(),
  constraint place_photos_place_sha1_key unique (place_id, sha1)
);

create index place_photos_place_idx on public.place_photos (place_id, sort_order);

alter table public.place_photos enable row level security;

-- Supabase's default privileges grant everything on new tables to anon/authenticated: undo that first.
revoke all on public.place_photos from anon, authenticated;
grant select on public.place_photos to anon, authenticated;

-- Read: published rows only. No insert/update/delete policy exists, so clients can never write.
create policy place_photos_read on public.place_photos for select to anon, authenticated
  using (is_published);

-- Storage bucket: public read (served by URL), 2 MB per file. No storage.objects policies are
-- added: with RLS on and no policy, only the service role can write.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('place-photos', 'place-photos', true, 2097152, array['image/jpeg','image/png','image/webp'])
on conflict (id) do update
  set public = true,
      file_size_limit = excluded.file_size_limit,
      allowed_mime_types = excluded.allowed_mime_types;

-- Rollback (manual; remove the bucket's files first, e.g. via the dashboard):
--   delete from storage.buckets where id = 'place-photos';
--   drop policy if exists place_photos_read on public.place_photos;
--   drop table if exists public.place_photos;
