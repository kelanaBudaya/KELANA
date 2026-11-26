-- Free entry (price 0) is not bought at the ticket counter, so it is never "on site".
-- The earlier backfill (20261126150000_ticket_on_site) also flagged price 0 rows. Idempotent.
update public.ticket_prices set on_site_available = false where price_idr = 0;
update public.events set on_site_available = false where price_from_idr = 0;
