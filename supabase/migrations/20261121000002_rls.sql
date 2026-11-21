-- Row Level Security. Default deny; every table has RLS enabled.

alter table public.regions        enable row level security;
alter table public.places         enable row level security;
alter table public.opening_hours  enable row level security;
alter table public.closures       enable row level security;
alter table public.ticket_prices  enable row level security;
alter table public.events         enable row level security;
alter table public.profiles       enable row level security;
alter table public.stamps         enable row level security;
alter table public.postcards      enable row level security;
alter table public.videos         enable row level security;
alter table public.reports        enable row level security;
alter table public.subscriptions  enable row level security;

-- Table privileges: anon gets nothing except reading regions.
revoke all on all tables in schema public from anon;
grant select on public.regions to anon;

-- Clients can never write stamps (privilege level AND no policy).
revoke insert, update, delete, truncate on public.stamps from authenticated;
-- Official data is written only by service role / dashboard.
revoke insert, update, delete, truncate on public.regions, public.places,
  public.opening_hours, public.closures, public.ticket_prices, public.events
  from authenticated;
-- Profiles are created by trigger only.
revoke insert on public.profiles from authenticated;

-- regions: the only thing readable without login (onboarding region picker).
create policy regions_read on public.regions for select to anon, authenticated using (true);

-- places: any logged-in user.
create policy places_read on public.places for select to authenticated using (true);

-- official facts: only published rows are visible.
create policy opening_hours_read on public.opening_hours for select to authenticated
  using (review_status = 'published');
create policy closures_read on public.closures for select to authenticated
  using (review_status = 'published');
create policy ticket_prices_read on public.ticket_prices for select to authenticated
  using (review_status = 'published');
create policy events_read on public.events for select to authenticated
  using (review_status = 'published');

-- profiles: readable by any logged-in user (names on stories); edit own only.
create policy profiles_read on public.profiles for select to authenticated using (true);
create policy profiles_update_own on public.profiles for update to authenticated
  using (id = (select auth.uid())) with check (id = (select auth.uid()));
create policy profiles_delete_own on public.profiles for delete to authenticated
  using (id = (select auth.uid()));

-- stamps: read own only. No insert/update/delete policy exists on purpose.
create policy stamps_read_own on public.stamps for select to authenticated
  using (user_id = (select auth.uid()));

-- postcards: visible immediately to everyone logged in; write needs a stamp.
create policy postcards_read on public.postcards for select to authenticated using (true);
create policy postcards_insert on public.postcards for insert to authenticated
  with check (
    user_id = (select auth.uid())
    and exists (select 1 from public.stamps s
                where s.user_id = (select auth.uid()) and s.place_id = postcards.place_id)
  );
create policy postcards_update_own on public.postcards for update to authenticated
  using (user_id = (select auth.uid()))
  with check (
    user_id = (select auth.uid())
    and exists (select 1 from public.stamps s
                where s.user_id = (select auth.uid()) and s.place_id = postcards.place_id)
  );
create policy postcards_delete_own on public.postcards for delete to authenticated
  using (user_id = (select auth.uid()));

-- videos: same rules as postcards.
create policy videos_read on public.videos for select to authenticated using (true);
create policy videos_insert on public.videos for insert to authenticated
  with check (
    user_id = (select auth.uid())
    and exists (select 1 from public.stamps s
                where s.user_id = (select auth.uid()) and s.place_id = videos.place_id)
  );
create policy videos_update_own on public.videos for update to authenticated
  using (user_id = (select auth.uid()))
  with check (
    user_id = (select auth.uid())
    and exists (select 1 from public.stamps s
                where s.user_id = (select auth.uid()) and s.place_id = videos.place_id)
  );
create policy videos_delete_own on public.videos for delete to authenticated
  using (user_id = (select auth.uid()));

-- reports: own only (admin reads via service role).
create policy reports_read_own on public.reports for select to authenticated
  using (user_id = (select auth.uid()));
create policy reports_insert_own on public.reports for insert to authenticated
  with check (user_id = (select auth.uid()));
create policy reports_update_own on public.reports for update to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy reports_delete_own on public.reports for delete to authenticated
  using (user_id = (select auth.uid()));

-- subscriptions: own only.
create policy subscriptions_all_own on public.subscriptions for all to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
