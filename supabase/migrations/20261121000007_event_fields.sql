-- Event detail facts for the Agenda "Detail pentas" screen:
-- performers, arrival advice (minutes before the show) and filter tags.
alter table public.events
  add column performers          text[]  not null default '{}',
  add column arrival_advice_min  integer,
  add column arrival_advice_max  integer,
  add column tags                text[]  not null default '{}',
  add constraint events_arrival_advice_chk check (
    (arrival_advice_min is null and arrival_advice_max is null) or
    (arrival_advice_min is not null and arrival_advice_max is not null
      and arrival_advice_min >= 0 and arrival_advice_min <= arrival_advice_max)
  ),
  add constraint events_tags_chk check (tags <@ array['tari','wayang','ramah_anak']::text[]);
