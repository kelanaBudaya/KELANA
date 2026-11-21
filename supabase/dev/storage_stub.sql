-- DEV/TEST ONLY. NOT a migration. Stand-in for the Storage schema created by the
-- storage-api service, used only where `supabase start` cannot run (this Windows
-- machine: storage-api and realtime containers segfault, exit 139). Mirrors the real
-- columns/RLS/foldername() closely enough to exercise policies. On a working stack the
-- real schema exists and this file must NOT be applied.
create table if not exists storage.buckets (
  id text primary key, name text not null unique, owner uuid, owner_id text,
  created_at timestamptz default now(), updated_at timestamptz default now(),
  public boolean default false, avif_autodetection boolean default false,
  file_size_limit bigint, allowed_mime_types text[]
);
create table if not exists storage.objects (
  id uuid primary key default gen_random_uuid(),
  bucket_id text references storage.buckets (id),
  name text, owner uuid, owner_id text,
  created_at timestamptz default now(), updated_at timestamptz default now(),
  last_accessed_at timestamptz default now(), metadata jsonb, version text, user_metadata jsonb
);
alter table storage.buckets enable row level security;
alter table storage.objects enable row level security;
create or replace function storage.foldername(name text) returns text[]
language plpgsql immutable as $$
declare _parts text[];
begin
  select string_to_array(name, '/') into _parts;
  return _parts[1:array_length(_parts, 1) - 1];
end $$;
-- real stack grants table privileges to API roles; RLS is what restricts them
grant all on storage.buckets, storage.objects to anon, authenticated, service_role;
alter table storage.buckets owner to postgres;
alter table storage.objects owner to postgres;
