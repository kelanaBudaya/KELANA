-- Release guard: raises if any sample row is left. Run before a public release.
create function public.release_check() returns void
language plpgsql security invoker set search_path = '' as $$
declare
  t text;
  n bigint;
  total bigint := 0;
  msg text := '';
begin
  foreach t in array array['regions','places','opening_hours','closures','ticket_prices',
                           'events','profiles','stamps','postcards','videos'] loop
    execute format('select count(*) from public.%I where is_sample', t) into n;
    if n > 0 then
      total := total + n;
      msg := msg || format(' %s=%s', t, n);
    end if;
  end loop;
  if total > 0 then
    raise exception 'release_check failed: % sample rows remain:%', total, msg
      using errcode = 'P0001';
  end if;
end $$;
revoke all on function public.release_check() from public, anon, authenticated;

-- Distance check for the verify-stamp Edge Function (service role only).
-- Stores nothing. Returns distance in metres and whether it is within the cap
-- radius. within_radius is null while the design leaves the radius open ([ISI]).
create function public.stamp_distance(p_place_id uuid, p_lat double precision, p_lng double precision)
returns table (distance_m double precision, cap_radius_m int, within_radius boolean)
language sql stable security invoker set search_path = '' as $$
  select
    extensions.st_distance(p.location,
      extensions.st_setsrid(extensions.st_makepoint(p_lng, p_lat), 4326)::extensions.geography),
    p.cap_radius_m,
    case when p.cap_radius_m is null then null
         else extensions.st_distance(p.location,
              extensions.st_setsrid(extensions.st_makepoint(p_lng, p_lat), 4326)::extensions.geography)
              <= p.cap_radius_m end
  from public.places p where p.id = p_place_id;
$$;
revoke all on function public.stamp_distance(uuid, double precision, double precision)
  from public, anon, authenticated;
grant execute on function public.stamp_distance(uuid, double precision, double precision) to service_role;

-- Convenience for the Android client: published facts for one place in one call.
-- security invoker: RLS still hides pending rows.
create function public.place_facts(p_place_id uuid) returns jsonb
language sql stable security invoker set search_path = '' as $$
  select jsonb_build_object(
    'opening_hours', coalesce((select jsonb_agg(to_jsonb(h) order by h.weekday, h.opens_at)
                               from public.opening_hours h where h.place_id = p_place_id), '[]'),
    'closures',      coalesce((select jsonb_agg(to_jsonb(c) order by c.starts_on)
                               from public.closures c where c.place_id = p_place_id), '[]'),
    'ticket_prices', coalesce((select jsonb_agg(to_jsonb(t) order by t.label)
                               from public.ticket_prices t where t.place_id = p_place_id), '[]'),
    'events',        coalesce((select jsonb_agg(to_jsonb(e) order by e.start_time)
                               from public.events e where e.place_id = p_place_id), '[]')
  );
$$;
revoke all on function public.place_facts(uuid) from public, anon;
grant execute on function public.place_facts(uuid) to authenticated;
