-- Stamp name: a catchy per-place name shown instead of the code (cap_code stays as the small passport number).
alter table public.places add column stamp_name text;

comment on column public.places.stamp_name is
  'Short catchy name for the stamp the visitor collects at this place; shown instead of the code. Null falls back to cap_code.';
