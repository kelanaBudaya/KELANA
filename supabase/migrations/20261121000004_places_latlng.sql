-- Plain numeric coordinates for clients (the Android app must not parse WKB).
-- Generated from places.location, so they can never drift; RLS on places applies as usual.
alter table public.places
  add column lat double precision generated always as (extensions.st_y(location::extensions.geometry)) stored,
  add column lng double precision generated always as (extensions.st_x(location::extensions.geometry)) stored;
comment on column public.places.lat is 'Latitude, WGS84, derived from location. Read-only.';
comment on column public.places.lng is 'Longitude, WGS84, derived from location. Read-only.';
