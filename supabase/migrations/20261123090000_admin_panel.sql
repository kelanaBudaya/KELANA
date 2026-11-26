-- Panel verifikasi (admin web, admin/): staff roles, four-eyes publishing and an append-only change history.
--
-- What this migration adds, and what it deliberately leaves alone:
--   * public.staff: who may use the admin panel (role checker | reviewer | admin, optional region).
--     No self-service signup: rows are inserted by the project owner (SQL editor / psql as postgres).
--   * public.fact_reviews: internal review data for one official fact (internal note, manager confirmation,
--     conflict flag, due date, who checked, who reviewed). Kept OUT of the fact tables on purpose: travellers
--     read published fact rows (and place_facts returns whole rows), so an internal note or a staff id in
--     those rows would leak to the app.
--   * public.fact_history: append-only log of every change to a fact (before -> after, source, who, when),
--     written by triggers only.
--   * Four-eyes rule, enforced by triggers on the fact tables and on fact_reviews (so it holds for every
--     caller: the admin app, the service role, the dashboard and psql): a fact becomes or stays
--     `published` only when a checker and a DIFFERENT reviewer both signed off on exactly its current content.
--   * RPCs for the admin app (security definer, each checks the caller's staff row).
--
-- Unchanged: every existing policy and grant on the fact tables. Staff do NOT get a select policy on the
-- fact tables: they read pending facts only through staff_fact_queue(). So a staff member who also uses the
-- Android app with the same account still sees published facts only, exactly like any traveller.
-- The only existing table that gets a new policy is reports (staff may read reports, read-only).

-- ---------------------------------------------------------------- staff
create type public.staff_role as enum ('checker', 'reviewer', 'admin');

create table public.staff (
  user_id      uuid primary key references auth.users(id) on delete cascade,
  role         public.staff_role not null,
  -- null = every region; otherwise the staff member only sees facts of places in this region.
  region_id    bigint references public.regions(id),
  display_name text not null check (char_length(display_name) between 1 and 80),
  created_at   timestamptz not null default now()
);
alter table public.staff enable row level security;

-- Helpers. security definer so policies can call them without recursing into staff RLS.
create function public.is_staff() returns boolean
language sql stable security definer set search_path = '' as $$
  select exists (select 1 from public.staff s where s.user_id = (select auth.uid()));
$$;

create function public.current_staff_role() returns public.staff_role
language sql stable security definer set search_path = '' as $$
  select s.role from public.staff s where s.user_id = (select auth.uid());
$$;

-- True when the caller is staff and the place is inside the caller's region (or the caller has no region).
create function public.is_staff_for_place(p_place_id uuid) returns boolean
language sql stable security definer set search_path = '' as $$
  select exists (
    select 1
      from public.staff s
      join public.places p on p.id = p_place_id
     where s.user_id = (select auth.uid())
       and (s.region_id is null or s.region_id = p.region_id)
  );
$$;

-- ---------------------------------------------------------------- fact content
-- The part of a fact that a review signs off on: every column except review_status and checked_at.
-- (checked_at alone may be refreshed on a published fact: "an unchanged re-check just refreshes checkedAt".)
create function public.fact_content(p_row jsonb) returns jsonb
language sql immutable set search_path = '' as $$
  select p_row - 'review_status' - 'checked_at';
$$;

create function public.is_fact_kind(p_kind text) returns boolean
language sql immutable set search_path = '' as $$
  select p_kind in ('opening_hours', 'closures', 'ticket_prices', 'events');
$$;

-- ---------------------------------------------------------------- fact_reviews
create table public.fact_reviews (
  fact_table           text not null check (public.is_fact_kind(fact_table)),
  fact_id              uuid not null,
  place_id             uuid not null references public.places(id) on delete cascade,
  internal_note        text check (internal_note is null or char_length(internal_note) <= 2000),
  manager_confirmation text check (manager_confirmation is null or char_length(manager_confirmation) <= 300),
  has_conflict         boolean not null default false,
  due_on               date,
  -- "Diperiksa": the first person. Set only by the guard trigger rules below.
  checker_id           uuid references auth.users(id) on delete set null,
  checker_marked_at    timestamptz,
  checked_snapshot     jsonb,
  -- "Ditinjau pemeriksa kedua": must be a different person.
  reviewer_id          uuid references auth.users(id) on delete set null,
  reviewed_at          timestamptz,
  reviewed_snapshot    jsonb,
  updated_at           timestamptz not null default now(),
  primary key (fact_table, fact_id),
  check (checker_id is null or reviewer_id is null or checker_id <> reviewer_id)
);
create index fact_reviews_place_idx on public.fact_reviews (place_id);
alter table public.fact_reviews enable row level security;

-- Current content of a fact, or null when it does not exist.
create function public.fact_row(p_kind text, p_id uuid) returns jsonb
language plpgsql stable security definer set search_path = '' as $$
declare r jsonb;
begin
  if not public.is_fact_kind(p_kind) then
    raise exception 'unknown_fact_kind' using errcode = '22023';
  end if;
  execute format('select to_jsonb(t) from public.%I t where t.id = $1', p_kind) into r using p_id;
  return r;
end $$;

-- Guard: nobody (not even service_role or postgres) can forge a check or a review.
--   * checker_id can only be set to the caller (auth.uid()), who must be staff for the place.
--   * reviewer_id can only be set to the caller, who must be reviewer or admin for the place, must differ from
--     the checker, and the content must still be exactly what the checker signed off on.
--   * Snapshots and timestamps are computed here; values sent by the caller are ignored.
--   * A new or cleared check also clears the review.
create function public.guard_fact_review() returns trigger
language plpgsql security definer set search_path = '' as $$
declare
  v_row     jsonb;
  v_content jsonb;
  v_uid     uuid := (select auth.uid());
  v_role    public.staff_role;
  v_checker_changed  boolean;
  v_reviewer_changed boolean;
begin
  v_row := public.fact_row(new.fact_table, new.fact_id);
  if v_row is null then
    raise exception 'fact_not_found' using errcode = 'P0002';
  end if;
  new.place_id := (v_row ->> 'place_id')::uuid;
  v_content := public.fact_content(v_row);
  new.updated_at := now();

  if tg_op = 'INSERT' then
    v_checker_changed  := new.checker_id is not null;
    v_reviewer_changed := new.reviewer_id is not null;
    new.checked_snapshot := null; new.checker_marked_at := null;
    new.reviewed_snapshot := null; new.reviewed_at := null;
  else
    if new.fact_table <> old.fact_table or new.fact_id <> old.fact_id then
      raise exception 'fact_review_key_is_fixed' using errcode = '42501';
    end if;
    v_checker_changed  := new.checker_id is distinct from old.checker_id;
    v_reviewer_changed := new.reviewer_id is distinct from old.reviewer_id;
    new.checked_snapshot := old.checked_snapshot; new.checker_marked_at := old.checker_marked_at;
    new.reviewed_snapshot := old.reviewed_snapshot; new.reviewed_at := old.reviewed_at;
  end if;

  if v_checker_changed then
    if new.checker_id is not null then
      if v_uid is null or new.checker_id <> v_uid or not public.is_staff_for_place(new.place_id) then
        raise exception 'checker_must_be_self' using errcode = '42501';
      end if;
      new.checked_snapshot := v_content;
      new.checker_marked_at := now();
    else
      new.checked_snapshot := null;
      new.checker_marked_at := null;
    end if;
    -- A new (or removed) check invalidates any earlier review, unless this same statement sets a reviewer.
    if not v_reviewer_changed then
      new.reviewer_id := null;
      if tg_op = 'UPDATE' then
        v_reviewer_changed := old.reviewer_id is not null;
      end if;
    end if;
  end if;

  if v_reviewer_changed then
    if new.reviewer_id is not null then
      select s.role into v_role from public.staff s where s.user_id = v_uid;
      if v_uid is null or new.reviewer_id <> v_uid or v_role is null or v_role = 'checker'
         or not public.is_staff_for_place(new.place_id) then
        raise exception 'reviewer_must_be_self' using errcode = '42501';
      end if;
      if new.checker_id is null or new.checker_id = new.reviewer_id then
        raise exception 'four_eyes_required'
          using errcode = 'P0001', detail = 'The reviewer must be a different person from the checker.';
      end if;
      if new.checked_snapshot is distinct from v_content then
        raise exception 'stale_check'
          using errcode = 'P0001', detail = 'The fact changed after it was checked; check it again.';
      end if;
      new.reviewed_snapshot := v_content;
      new.reviewed_at := now();
    else
      new.reviewed_snapshot := null;
      new.reviewed_at := null;
    end if;
  end if;
  return new;
end $$;

create trigger fact_reviews_guard
  before insert or update on public.fact_reviews
  for each row execute function public.guard_fact_review();

-- ---------------------------------------------------------------- four-eyes on the fact tables
-- BEFORE INSERT OR UPDATE on opening_hours, closures, ticket_prices, events.
-- security invoker on purpose: current_user is the real caller, so only the owner roles (seed, migrations)
-- may insert a row that is already published. Everyone else, service_role included, inserts `pending`.
create function public.enforce_fact_four_eyes() returns trigger
language plpgsql security invoker set search_path = '' as $$
declare
  v_content jsonb;
  r record;
begin
  if new.review_status <> 'published' then
    return new;
  end if;

  if tg_op = 'INSERT' then
    if current_user in ('postgres', 'supabase_admin') then
      return new;
    end if;
    raise exception 'four_eyes_required'
      using errcode = 'P0001', detail = 'A new fact starts as pending and is published through review.';
  end if;

  v_content := public.fact_content(to_jsonb(new));
  -- Already published and the content is unchanged (for example only checked_at refreshed): allowed.
  if old.review_status = 'published' and public.fact_content(to_jsonb(old)) = v_content then
    return new;
  end if;

  select fr.checker_id, fr.reviewer_id, fr.checked_snapshot, fr.reviewed_snapshot
    into r
    from public.fact_reviews fr
   where fr.fact_table = tg_table_name and fr.fact_id = new.id;
  if not found
     or r.checker_id is null or r.reviewer_id is null or r.checker_id = r.reviewer_id
     or r.checked_snapshot is distinct from v_content
     or r.reviewed_snapshot is distinct from v_content then
    raise exception 'four_eyes_required'
      using errcode = 'P0001',
            detail = 'Publishing needs a check and a review by two different staff members of this exact content.';
  end if;
  return new;
end $$;

-- ---------------------------------------------------------------- fact_history (append-only)
create table public.fact_history (
  id          bigint generated always as identity primary key,
  fact_table  text not null check (public.is_fact_kind(fact_table)),
  fact_id     uuid not null,
  place_id    uuid,          -- no FK: history outlives deleted places
  action      text not null check (action in ('insert', 'update', 'delete', 'checked', 'reviewed')),
  before      jsonb,
  after       jsonb,
  source_url  text,
  changed_by  uuid,          -- auth.uid() of the caller; null for service role, scraper or SQL editor. No FK.
  changed_at  timestamptz not null default now()
);
create index fact_history_fact_idx on public.fact_history (fact_table, fact_id, changed_at desc);
create index fact_history_place_idx on public.fact_history (place_id, changed_at desc);
alter table public.fact_history enable row level security;

create function public.log_fact_change() returns trigger
language plpgsql security definer set search_path = '' as $$
declare
  v_before jsonb;
  v_after  jsonb;
  v_row    jsonb;
begin
  if tg_op <> 'INSERT' then v_before := to_jsonb(old); end if;
  if tg_op <> 'DELETE' then v_after := to_jsonb(new); end if;
  if v_before = v_after then
    return null;
  end if;
  v_row := coalesce(v_after, v_before);
  insert into public.fact_history (fact_table, fact_id, place_id, action, before, after, source_url, changed_by)
  values (tg_table_name, (v_row ->> 'id')::uuid, (v_row ->> 'place_id')::uuid, lower(tg_op),
          v_before, v_after, v_row ->> 'source_url', (select auth.uid()));
  if tg_op = 'DELETE' then
    delete from public.fact_reviews where fact_table = tg_table_name and fact_id = (v_before ->> 'id')::uuid;
  end if;
  return null;
end $$;

create function public.log_fact_review() returns trigger
language plpgsql security definer set search_path = '' as $$
declare
  v_old_checked_at  timestamptz;
  v_old_reviewed_at timestamptz;
begin
  if tg_op = 'UPDATE' then
    v_old_checked_at := old.checker_marked_at;
    v_old_reviewed_at := old.reviewed_at;
  end if;
  -- checker_marked_at / reviewed_at are set by the guard on every new check or review.
  if new.checker_id is not null and new.checker_marked_at is distinct from v_old_checked_at then
    insert into public.fact_history (fact_table, fact_id, place_id, action, after, changed_by)
    values (new.fact_table, new.fact_id, new.place_id, 'checked', new.checked_snapshot, new.checker_id);
  end if;
  if new.reviewer_id is not null and new.reviewed_at is distinct from v_old_reviewed_at then
    insert into public.fact_history (fact_table, fact_id, place_id, action, after, changed_by)
    values (new.fact_table, new.fact_id, new.place_id, 'reviewed', new.reviewed_snapshot, new.reviewer_id);
  end if;
  return null;
end $$;

create trigger fact_reviews_log
  after insert or update on public.fact_reviews
  for each row execute function public.log_fact_review();

create function public.reject_history_change() returns trigger
language plpgsql set search_path = '' as $$
begin
  raise exception 'fact_history_is_append_only' using errcode = 'P0001';
end $$;

create trigger fact_history_no_update_delete
  before update or delete on public.fact_history
  for each row execute function public.reject_history_change();
create trigger fact_history_no_truncate
  before truncate on public.fact_history
  for each statement execute function public.reject_history_change();

-- Attach the fact-table triggers.
do $$
declare t text;
begin
  foreach t in array array['opening_hours', 'closures', 'ticket_prices', 'events'] loop
    execute format('create trigger %I before insert or update on public.%I
                      for each row execute function public.enforce_fact_four_eyes()', t || '_four_eyes', t);
    execute format('create trigger %I after insert or update or delete on public.%I
                      for each row execute function public.log_fact_change()', t || '_history', t);
  end loop;
end $$;

-- ---------------------------------------------------------------- privileges and RLS
-- Supabase's default privileges grant everything on new tables to anon/authenticated: undo that first.
revoke all on public.staff, public.fact_reviews, public.fact_history from anon, authenticated;
grant select on public.staff, public.fact_reviews, public.fact_history to authenticated;
-- History is append-only for every API role, service_role included (triggers also block owner roles).
revoke insert, update, delete, truncate on public.fact_history from service_role;

create policy staff_read on public.staff for select to authenticated
  using (user_id = (select auth.uid()) or public.is_staff());
-- No insert/update/delete policy on staff: rows are managed by the project owner only.

create policy fact_reviews_read_staff on public.fact_reviews for select to authenticated
  using (public.is_staff_for_place(place_id));
create policy fact_history_read_staff on public.fact_history for select to authenticated
  using (public.is_staff_for_place(place_id));

-- reports: staff may read reports (read-only), scoped by the reported place's region. The existing
-- own-only policies stay; this one is OR-ed with them and gives travellers nothing new.
create function public.report_place_id(p_place_id uuid, p_postcard_id uuid, p_video_id uuid) returns uuid
language sql stable security definer set search_path = '' as $$
  select coalesce(
    p_place_id,
    (select pc.place_id from public.postcards pc where pc.id = p_postcard_id),
    (select v.place_id from public.videos v where v.id = p_video_id));
$$;

create policy reports_read_staff on public.reports for select to authenticated
  using (public.is_staff_for_place(public.report_place_id(place_id, postcard_id, video_id)));

-- ---------------------------------------------------------------- RPCs for the admin app
create function public.require_staff_for_place(p_place_id uuid) returns void
language plpgsql stable security definer set search_path = '' as $$
begin
  if not public.is_staff_for_place(p_place_id) then
    raise exception 'not_staff' using errcode = '42501';
  end if;
end $$;

-- Queue: every fact (pending and published) of the places the caller may see, with its review data.
create function public.staff_fact_queue()
returns table (
  fact_table text, fact_id uuid, place_id uuid, place_name text, place_category public.place_category,
  data jsonb, review_status public.review_status, checked_at timestamptz, source_url text,
  internal_note text, manager_confirmation text, has_conflict boolean, due_on date,
  checker_id uuid, checker_name text, checker_marked_at timestamptz,
  reviewer_id uuid, reviewer_name text, reviewed_at timestamptz,
  check_is_current boolean
)
language plpgsql stable security definer set search_path = '' as $$
begin
  if not public.is_staff() then
    raise exception 'not_staff' using errcode = '42501';
  end if;
  return query
  with facts as (
    select 'opening_hours'::text k, h.id, h.place_id, to_jsonb(h) d, h.review_status rs, h.checked_at ca, h.source_url su from public.opening_hours h
    union all
    select 'closures', c.id, c.place_id, to_jsonb(c), c.review_status, c.checked_at, c.source_url from public.closures c
    union all
    select 'ticket_prices', t.id, t.place_id, to_jsonb(t), t.review_status, t.checked_at, t.source_url from public.ticket_prices t
    union all
    select 'events', e.id, e.place_id, to_jsonb(e), e.review_status, e.checked_at, e.source_url from public.events e
  )
  select f.k, f.id, f.place_id, p.name, p.category, f.d, f.rs, f.ca, f.su,
         fr.internal_note, fr.manager_confirmation, coalesce(fr.has_conflict, false), fr.due_on,
         fr.checker_id, sc.display_name, fr.checker_marked_at,
         fr.reviewer_id, sr.display_name, fr.reviewed_at,
         fr.checker_id is not null and fr.checked_snapshot = public.fact_content(f.d)
    from facts f
    join public.places p on p.id = f.place_id
    left join public.fact_reviews fr on fr.fact_table = f.k and fr.fact_id = f.id
    left join public.staff sc on sc.user_id = fr.checker_id
    left join public.staff sr on sr.user_id = fr.reviewer_id
   where public.is_staff_for_place(f.place_id)
   order by (f.rs = 'published'), coalesce(fr.has_conflict, false) desc, fr.due_on nulls last, p.name, f.k;
end $$;

create function public.ensure_fact_review(p_kind text, p_id uuid) returns uuid
language plpgsql security definer set search_path = '' as $$
declare v_row jsonb;
begin
  v_row := public.fact_row(p_kind, p_id);
  if v_row is null then
    raise exception 'fact_not_found' using errcode = 'P0002';
  end if;
  perform public.require_staff_for_place((v_row ->> 'place_id')::uuid);
  insert into public.fact_reviews (fact_table, fact_id, place_id)
  values (p_kind, p_id, (v_row ->> 'place_id')::uuid)
  on conflict (fact_table, fact_id) do nothing;
  return (v_row ->> 'place_id')::uuid;
end $$;

-- "Simpan draf": update the fact's values (p_patch, column -> value) and the internal review fields.
-- A change to the content sends the fact back to pending and clears its check and review.
-- id, place_id, review_status and is_sample cannot be changed here.
create function public.staff_save_fact(
  p_kind text, p_id uuid, p_patch jsonb,
  p_internal_note text, p_manager_confirmation text, p_has_conflict boolean, p_due_on date
) returns void
language plpgsql security definer set search_path = '' as $$
declare
  v_old jsonb;
  v_new jsonb;
  v_key text;
  v_set text;
begin
  perform public.ensure_fact_review(p_kind, p_id);
  p_patch := coalesce(p_patch, '{}'::jsonb);
  if jsonb_typeof(p_patch) <> 'object' then
    raise exception 'patch_must_be_object' using errcode = '22023';
  end if;

  for v_key in select jsonb_object_keys(p_patch) loop
    if v_key in ('id', 'place_id', 'review_status', 'is_sample')
       or not exists (select 1 from pg_attribute a
                       where a.attrelid = format('public.%I', p_kind)::regclass
                         and a.attname = v_key and a.attnum > 0 and not a.attisdropped
                         and a.attgenerated = '') then
      raise exception 'field_not_editable: %', v_key using errcode = '42501';
    end if;
  end loop;

  if p_patch <> '{}'::jsonb then
    v_old := public.fact_row(p_kind, p_id);
    execute format('select to_jsonb(r) from jsonb_populate_record(null::public.%I, $1) r', p_kind)
      into v_new using v_old || p_patch;
    if v_new is distinct from v_old then
      select string_agg(format('%I = r.%I', k, k), ', ') into v_set from jsonb_object_keys(p_patch) k;
      if public.fact_content(v_new) is distinct from public.fact_content(v_old) then
        v_set := v_set || ', review_status = ''pending''';
        update public.fact_reviews set checker_id = null, reviewer_id = null
         where fact_table = p_kind and fact_id = p_id;
      end if;
      execute format('update public.%I t set %s from jsonb_populate_record(null::public.%I, $1) r where t.id = $2',
                     p_kind, v_set, p_kind)
        using p_patch, p_id;
    end if;
  end if;

  update public.fact_reviews
     set internal_note = nullif(btrim(p_internal_note), ''),
         manager_confirmation = nullif(btrim(p_manager_confirmation), ''),
         has_conflict = coalesce(p_has_conflict, false),
         due_on = p_due_on
   where fact_table = p_kind and fact_id = p_id;
end $$;

-- "Minta tinjauan": the caller marks the current content as checked (first pair of eyes).
create function public.mark_fact_checked(p_kind text, p_id uuid) returns void
language plpgsql security definer set search_path = '' as $$
declare v_row jsonb;
begin
  perform public.ensure_fact_review(p_kind, p_id);
  v_row := public.fact_row(p_kind, p_id);
  if v_row ->> 'review_status' <> 'pending' then
    raise exception 'fact_not_pending' using errcode = 'P0001';
  end if;
  if v_row ->> 'checked_at' is null or v_row ->> 'source_url' is null then
    raise exception 'check_needs_source_and_date'
      using errcode = 'P0001', detail = 'Fill source_url and checked_at before asking for a review.';
  end if;
  -- Re-marking by the same person refreshes the snapshot: clear first so the guard treats it as a new check.
  update public.fact_reviews set checker_id = null
   where fact_table = p_kind and fact_id = p_id and checker_id = (select auth.uid());
  update public.fact_reviews set checker_id = (select auth.uid())
   where fact_table = p_kind and fact_id = p_id;
end $$;

-- "Ditinjau pemeriksa kedua" + "Terbit ke aplikasi": the caller (reviewer or admin, not the checker)
-- approves the checked content and publishes it in one transaction.
create function public.publish_fact(p_kind text, p_id uuid) returns void
language plpgsql security definer set search_path = '' as $$
declare v_role public.staff_role;
begin
  perform public.ensure_fact_review(p_kind, p_id);
  v_role := public.current_staff_role();
  if v_role is null or v_role = 'checker' then
    raise exception 'reviewer_role_required' using errcode = '42501';
  end if;
  if public.fact_row(p_kind, p_id) ->> 'review_status' <> 'pending' then
    raise exception 'fact_not_pending' using errcode = 'P0001';
  end if;
  update public.fact_reviews set reviewer_id = (select auth.uid())
   where fact_table = p_kind and fact_id = p_id;
  execute format('update public.%I set review_status = ''published'' where id = $1', p_kind) using p_id;
end $$;

-- Summary cards on the queue page. Counts only what the caller may see.
create function public.staff_summary() returns jsonb
language plpgsql stable security definer set search_path = '' as $$
begin
  if not public.is_staff() then
    raise exception 'not_staff' using errcode = '42501';
  end if;
  return jsonb_build_object(
    'due_this_week', (select count(*) from public.staff_fact_queue() q
                       where q.due_on is not null and q.due_on <= (now() at time zone 'Asia/Jakarta')::date + 7),
    'conflicts',     (select count(*) from public.staff_fact_queue() q
                       where q.has_conflict and q.review_status = 'pending'),
    'pending',       (select count(*) from public.staff_fact_queue() q where q.review_status = 'pending'),
    'reports_7d',    (select count(*) from public.reports r
                       where r.created_at >= now() - interval '7 days'
                         and public.is_staff_for_place(public.report_place_id(r.place_id, r.postcard_id, r.video_id))),
    'published_30d', (select count(*) from public.fact_history h
                       where h.action = 'update'
                         and h.before ->> 'review_status' = 'pending'
                         and h.after ->> 'review_status' = 'published'
                         and h.changed_at >= now() - interval '30 days'
                         and public.is_staff_for_place(h.place_id))
  );
end $$;

-- Function privileges. Trigger and internal functions: nobody calls them directly.
revoke all on function public.guard_fact_review(), public.enforce_fact_four_eyes(), public.log_fact_change(),
  public.log_fact_review(), public.reject_history_change(), public.fact_row(text, uuid),
  public.ensure_fact_review(text, uuid), public.require_staff_for_place(uuid)
  from public, anon, authenticated;
-- Helpers used inside RLS policies and RPCs for logged-in users.
revoke all on function public.is_staff(), public.current_staff_role(), public.is_staff_for_place(uuid),
  public.report_place_id(uuid, uuid, uuid), public.fact_content(jsonb), public.is_fact_kind(text),
  public.staff_fact_queue(), public.staff_save_fact(text, uuid, jsonb, text, text, boolean, date),
  public.mark_fact_checked(text, uuid), public.publish_fact(text, uuid), public.staff_summary()
  from public, anon;
grant execute on function public.is_staff(), public.current_staff_role(), public.is_staff_for_place(uuid),
  public.report_place_id(uuid, uuid, uuid), public.fact_content(jsonb), public.is_fact_kind(text),
  public.staff_fact_queue(), public.staff_save_fact(text, uuid, jsonb, text, text, boolean, date),
  public.mark_fact_checked(text, uuid), public.publish_fact(text, uuid), public.staff_summary()
  to authenticated;
