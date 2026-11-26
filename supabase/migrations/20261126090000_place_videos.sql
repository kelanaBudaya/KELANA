-- Visitor videos on Detail tempat: curated TikTok links embedded in the app.
-- Rows are added by the team from the Supabase dashboard / admin (service role). Clients can only read.
-- Guests may read: this is curated public content, not user data.

create table public.place_videos (
  id              uuid primary key default gen_random_uuid(),
  place_id        uuid not null references public.places(id) on delete cascade,
  tiktok_url      text not null,
  tiktok_video_id text not null check (tiktok_video_id ~ '^[0-9]+$'),
  caption         text,
  sort_order      integer not null default 0,
  is_published    boolean not null default true,
  created_at      timestamptz not null default now(),
  constraint place_videos_place_video_key unique (place_id, tiktok_video_id)
);

create index place_videos_place_idx on public.place_videos (place_id, sort_order);

alter table public.place_videos enable row level security;

-- Supabase's default privileges grant everything on new tables to anon/authenticated: undo that first.
revoke all on public.place_videos from anon, authenticated;
grant select on public.place_videos to anon, authenticated;

-- Read: published rows only. No insert/update/delete policy exists, so clients can never write.
create policy place_videos_read on public.place_videos for select to anon, authenticated
  using (is_published);
