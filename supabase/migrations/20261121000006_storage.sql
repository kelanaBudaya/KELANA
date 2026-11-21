-- Supabase Storage, reproducible. Video is NOT stored here (Cloudflare Stream / Mux).
-- Both buckets private. Path conventions:
--   postcards:     <user_id>/<place_id>/<filename>
--   report-photos: <user_id>/<filename>
-- Safe on top of buckets/policies created by hand in the dashboard: the buckets are forced
-- to the private config above, and each policy is dropped (if present) before it is created,
-- so a same-named dashboard policy cannot break `db push`.
-- Default deny: storage.objects has RLS on and only the policies below exist, so anon
-- (guests) can neither read nor write any object. Every policy is scoped by bucket_id.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values
  ('postcards',     'postcards',     false, 5242880, array['image/jpeg','image/png','image/webp']),
  ('report-photos', 'report-photos', false, 5242880, array['image/jpeg','image/png','image/webp'])
on conflict (id) do update
  set public = false,
      file_size_limit = excluded.file_size_limit,
      allowed_mime_types = excluded.allowed_mime_types;

-- postcards ---------------------------------------------------------------------
-- Upload only into own folder AND only for a place the user holds a stamp for.
-- (storage.foldername(name))[2] is the place_id segment; compared as text.
drop policy if exists postcards_objects_insert on storage.objects;
create policy postcards_objects_insert on storage.objects for insert to authenticated
  with check (
    bucket_id = 'postcards'
    and (storage.foldername(name))[1] = (select auth.uid())::text
    and exists (select 1 from public.stamps s
                where s.user_id = (select auth.uid())
                  and s.place_id::text = (storage.foldername(name))[2])
  );
-- Any logged-in user can read postcard photos (stories are shared). Not guests.
drop policy if exists postcards_objects_select on storage.objects;
create policy postcards_objects_select on storage.objects for select to authenticated
  using (bucket_id = 'postcards');
-- Edit/delete own files only. The stamp rule is kept on update so a file cannot be
-- moved to an unstamped place.
drop policy if exists postcards_objects_update on storage.objects;
create policy postcards_objects_update on storage.objects for update to authenticated
  using (bucket_id = 'postcards'
         and (storage.foldername(name))[1] = (select auth.uid())::text)
  with check (
    bucket_id = 'postcards'
    and (storage.foldername(name))[1] = (select auth.uid())::text
    and exists (select 1 from public.stamps s
                where s.user_id = (select auth.uid())
                  and s.place_id::text = (storage.foldername(name))[2])
  );
drop policy if exists postcards_objects_delete on storage.objects;
create policy postcards_objects_delete on storage.objects for delete to authenticated
  using (bucket_id = 'postcards'
         and (storage.foldername(name))[1] = (select auth.uid())::text);

-- report-photos: private to the reporter (admins use the service role) -------------
drop policy if exists report_photos_objects_insert on storage.objects;
create policy report_photos_objects_insert on storage.objects for insert to authenticated
  with check (bucket_id = 'report-photos'
              and (storage.foldername(name))[1] = (select auth.uid())::text);
drop policy if exists report_photos_objects_select on storage.objects;
create policy report_photos_objects_select on storage.objects for select to authenticated
  using (bucket_id = 'report-photos'
         and (storage.foldername(name))[1] = (select auth.uid())::text);
drop policy if exists report_photos_objects_delete on storage.objects;
create policy report_photos_objects_delete on storage.objects for delete to authenticated
  using (bucket_id = 'report-photos'
         and (storage.foldername(name))[1] = (select auth.uid())::text);
