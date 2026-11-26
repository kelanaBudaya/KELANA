# TikTok candidates and oEmbed validation (DIY, 26 places)

Run date: 2026-11-26. Scripts: `scripts/media/tiktok_search.py` (T1), `scripts/media/tiktok_oembed.py` (T2).

## Result

**0 of 146 video candidates validated.** The oEmbed endpoint returned HTTP 200 for all 146 requests, but none of the responses contained a `title` or `author_name`. The validation rule (HTTP 200 plus title/author) therefore failed for every candidate. `tiktok_validated.jsonl` is empty. T3 (relevance review) cannot start because it needs the oEmbed title.

Example response (museum-sonobudoyo, id 7610998760574930194), abridged: `{"version":"1.0","type":"video","width":"325","height":"578","html":"<iframe ... src=\"https://www.tiktok.com/player/v1/7610998760574930194?...\"></iframe>","provider_name":"TikTok","embed_product_id":"7610998760574930194","embed_type":"video"}`. There is no `title` and no `author_name`.

The plan's looser T2 rule (HTTP 200 plus numeric id) would accept all 146, but those entries carry no title, so T3 relevance could not be judged. Decision needed: either accept id-only validation with a manual title check later, or find another source for titles.

## Credits and searches

- Credits remaining: 1000 before (credit-usage check), 998 after one test search, 946 after the T1 run. Total consumed: 54 of the 120 cap.
- Searches: 28 total (1 test, 27 in the T1 run). Each search costs 2 credits with limit 10.
- Second query ran only for plengkung-gading (2 video URLs after the first query). All other places had at least 3.
- The run did not hit the cap and was not aborted.

## Per-place counts

Candidates are TikTok-host search results. "Video" counts unique `/video/<id>` URLs. Validated is 0 for every place.

| slug | tiktok results | video candidates | validated |
|---|---|---|---|
| keraton-yogyakarta | 10 | 6 | 0 |
| museum-sonobudoyo | 10 | 5 | 0 |
| taman-sari | 10 | 4 | 0 |
| museum-ullen-sentalu | 10 | 5 | 0 |
| benteng-vredeburg | 10 | 7 | 0 |
| museum-puro-pakualaman | 10 | 3 | 0 |
| situs-ratu-boko | 10 | 5 | 0 |
| kawasan-kotagede | 10 | 9 | 0 |
| candi-prambanan | 10 | 6 | 0 |
| masjid-gedhe-kauman | 10 | 9 | 0 |
| plengkung-gading | 5 | 2 | 0 |
| kampung-ketandan | 10 | 5 | 0 |
| taman-budaya-yogyakarta | 10 | 7 | 0 |
| sekar-rinonce | 10 | 6 | 0 |
| workshop-perak-kotagede | 10 | 4 | 0 |
| desa-wisata-tlatar | 7 | 7 | 0 |
| desa-wisata-kasongan | 10 | 3 | 0 |
| desa-wisata-krebet | 10 | 7 | 0 |
| desa-wisata-nglanggeran | 10 | 7 | 0 |
| desa-wisata-widosari | 10 | 9 | 0 |
| makam-raja-imogiri | 3 | 3 | 0 |
| candi-sambisari | 10 | 5 | 0 |
| candi-ijo | 9 | 7 | 0 |
| candi-banyunibo | 10 | 8 | 0 |
| museum-affandi | 10 | 3 | 0 |
| museum-gunungapi-merapi | 9 | 4 | 0 |

Totals: 243 TikTok-host results, 146 video candidates, 0 validated. Places with no candidates: none. Places with fewer than 3 video candidates: plengkung-gading (2).

## oEmbed status codes

- 200: 146
- 403 or 429: 0 (the stop-after-5-blocked rule never triggered)
- Pacing: one request every 2 s, User-Agent `KelanaDataBot/0.1`.
- No tiktok.com page other than the oEmbed endpoint was fetched.

## Not verified

- T3 relevance review, `tiktok_selected.json`, and `supabase/data/place_videos_diy.sql` were not produced because nothing validated.
- The existing museum-sonobudoyo `tiktok_url` fix depends on T2 returning an author, which did not happen.
