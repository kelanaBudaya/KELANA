-- Casual short name for running text ("stamps in Jogja"); null falls back to name in the clients.
alter table public.regions add column short_name text;
comment on column public.regions.short_name is 'Casual short name used in running text, e.g. "stamps in Jogja". Null falls back to name.';

update public.regions set short_name = 'Jogja' where code = 'DIY';
update public.regions set short_name = 'Jateng' where code = 'JTG';
update public.regions set short_name = 'Jatim' where code = 'JTM';
update public.regions set short_name = 'Jabar' where code = 'JBR';
update public.regions set short_name = 'Jakarta' where code = 'JKT';
update public.regions set short_name = 'Banten' where code = 'BTN';
update public.regions set short_name = 'Bali' where code = 'BALI';
update public.regions set short_name = 'Sumbar' where code = 'SUMBAR';
update public.regions set short_name = 'Sulsel' where code = 'SULSEL';
update public.regions set short_name = 'NTT' where code = 'NTT';
update public.regions set short_name = 'Kaltim' where code = 'KALTIM';
update public.regions set short_name = 'Papua' where code = 'PAPUA';
