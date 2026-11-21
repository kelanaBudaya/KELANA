-- DEV ONLY. NOT a migration: run it by hand on the cloud project (SQL editor) so stamping can be tried
-- on the sample places. 150 m is a PLACEHOLDER dev value. The real cap radius per place is still a team
-- decision; before a public release set the real values and make sure no sample row remains
-- (select public.release_check();). It touches sample rows only (is_sample = true).
update public.places
   set cap_radius_m = 150
 where is_sample = true
   and cap_radius_m is null;
