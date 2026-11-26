-- Tiket resmi: whether a ticket can be bought at the venue.
alter table public.ticket_prices
  add column on_site_available boolean not null default false;
alter table public.events
  add column on_site_available boolean not null default false;

comment on column public.ticket_prices.on_site_available is
  'True when this ticket tier can be bought at the venue ticket counter.';
comment on column public.events.on_site_available is
  'True when this show''s ticket can be bought at the venue ticket counter.';

-- Backfill assumption: a published price without any online seller is an entrance fee paid at the venue.
-- A human reviews these values in the Supabase dashboard.
update public.ticket_prices set on_site_available = true
  where seller_url is null and price_idr is not null;
update public.events set on_site_available = true
  where seller_url is null and price_from_idr is not null;
