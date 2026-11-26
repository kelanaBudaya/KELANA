begin;
insert into public.place_videos (place_id, tiktok_url, tiktok_video_id, caption, sort_order)
select id, 'https://www.tiktok.com/@/video/7659382160377253140', '7659382160377253140',
       'htm 10rb udah bisa masuk museum se-cantik ini!! #placetogojogja #tempatwisatajogja #museumjogja #museumsonobudoyo #wisatajogja', 0
from public.places where slug = 'museum-sonobudoyo'
on conflict (place_id, tiktok_video_id) do update set caption = excluded.caption, tiktok_url = excluded.tiktok_url;
commit;
select p.slug, v.tiktok_video_id, v.is_published from public.place_videos v join public.places p on p.id = v.place_id;
