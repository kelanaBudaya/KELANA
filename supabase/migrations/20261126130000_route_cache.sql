-- Cache for the `route` Edge Function (road-following routes from the public Valhalla server).
-- Only the service role (used inside the function) touches this table: RLS is on with no policies,
-- so anon/authenticated clients can neither read nor write it.
create table public.route_cache (
  key        text primary key,            -- sha256 hex of profile|lat,lng(5 dp)|...
  profile    text not null,
  legs       jsonb not null,              -- [{seconds, meters, polyline}]
  created_at timestamptz not null default now()
);

create index route_cache_created_at_idx on public.route_cache (created_at);

alter table public.route_cache enable row level security;
-- Deliberately no policies. The service role bypasses RLS.
revoke all on public.route_cache from anon, authenticated;
