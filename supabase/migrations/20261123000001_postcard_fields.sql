-- Tulis postcard (7.1): rating, card style and visibility.
-- A private postcard (is_public = false) is visible to its author only; it can still be shared outside the app.
-- Insert policy (needs a stamp for the place) is unchanged.

alter table public.postcards
  add column if not exists rating     smallint check (rating between 1 and 5),
  add column if not exists card_style text not null default 'jurnal' check (card_style in ('jurnal', 'cap', 'peta')),
  add column if not exists is_public  boolean not null default true;

drop policy if exists postcards_read on public.postcards;
create policy postcards_read on public.postcards for select to authenticated
  using (is_public or user_id = (select auth.uid()));

-- The photo of a private postcard must be private too. Before this migration any logged-in user could read
-- (and list) every object in the `postcards` bucket. Now a file is readable by its owner (own folder), or by
-- others only while a PUBLIC postcard row points at it. A file not yet linked to a row (upload in progress,
-- or left behind by a failed insert) is visible to its owner only.
create index if not exists postcards_photo_path_idx on public.postcards (photo_path) where photo_path is not null;

drop policy if exists postcards_objects_select on storage.objects;
create policy postcards_objects_select on storage.objects for select to authenticated
  using (
    bucket_id = 'postcards'
    and (
      (storage.foldername(name))[1] = (select auth.uid())::text
      or exists (select 1 from public.postcards p
                 where p.photo_path = storage.objects.name and p.is_public)
    )
  );
