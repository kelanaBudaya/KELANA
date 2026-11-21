-- Product decision: guests (role anon, no login) may browse official data.
-- Visitor stories, accounts and everything personal stay authenticated-only.
-- Anon never writes. Existing migrations are untouched.

-- Privileges: start from nothing for anon, then grant exactly the browse set.
revoke all on all tables in schema public from anon;
grant select on public.regions, public.places, public.opening_hours,
  public.closures, public.ticket_prices, public.events to anon;
-- Explicitly no privileges on: profiles, stamps, postcards, videos, reports, subscriptions.

-- Policies (anon). Official facts: published rows only, same filter as authenticated.
create policy places_read_anon on public.places for select to anon using (true);
create policy opening_hours_read_anon on public.opening_hours for select to anon
  using (review_status = 'published');
create policy closures_read_anon on public.closures for select to anon
  using (review_status = 'published');
create policy ticket_prices_read_anon on public.ticket_prices for select to anon
  using (review_status = 'published');
create policy events_read_anon on public.events for select to anon
  using (review_status = 'published');
-- regions_read already covers anon.

-- place_facts is security invoker, so the policies above still hide pending rows.
grant execute on function public.place_facts(uuid) to anon;
-- stamp_distance stays service-role only; release_check unchanged.
