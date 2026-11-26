-- Rename two place categories. Existing rows (places, events, subscriptions) keep their value under the new name.
--   topeng  -> pentas      (performances; the app label is "Pentas")
--   gamelan -> seni_musik  (music arts; the app label is "Seni Musik")
-- Older app builds that still send or cache the old keys are mapped by PlaceCategory.fromKey on the client.
alter type public.place_category rename value 'topeng' to 'pentas';
alter type public.place_category rename value 'gamelan' to 'seni_musik';
