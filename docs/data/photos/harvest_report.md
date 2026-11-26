# Harvest report: galeri foto DIY (P1 + P1b)

Generated 2026-11-26 by `scripts/media/commons_harvest.py` (stdlib only, no secrets). Candidates are NOT accepted yet; `license_ok` is advisory and the reviewer (P2) decides.

## API usage

- HTTP requests sent in the last run (retries included): **25**
- Requests served from `.cache/` in the last run: 197
- Successful raw responses cached (= distinct API calls made so far): **233**
- Back-offs (maxlag / 429 / 503 / network): 0
- Failed requests (not cached, skipped): 0

## Rules applied

- Commons: files of type image/jpeg, image/png, image/webp with width >= 1200 px; duplicates by sha1 removed per place. Other files are counted below, not written.
- Wikidata: first wbsearchentities hit (id, then en) whose description mentions Yogyakarta / Jogja / Daerah Istimewa / DIY; otherwise Commons search only.
- Fallback when fewer than 4 titles were found: image list of the id and en Wikipedia article with the place name, and a Commons search on the bare name. Replaces the planned local Firecrawl scrape (stdlib only); titles not on Commons are dropped.
- License accept list: CC0, PDM, PD (PD-old / PD-self / generic), CC BY and CC BY-SA 2.0 to 4.0. Rejected: NC, ND, IGO, GFDL-only, fair use, unknown or missing, empty or unknown author.
- Openverse (P1b) only for places with fewer than 6 Commons candidates; `sha1` and `file_title` are null there (P3 must compute sha1 after download), `url_400` is Openverse's own thumbnail (size not guaranteed).
- `license_code` values PD-old / PD-self / PD are not in the place_photos check list; they are flagged in `license_reason` for the reviewer.

## Per place

| place | QID (wd desc ok) | discovered | rejected (reason: count) | candidates | commons | openverse | license_ok | GPS > 2 km from place |
|---|---|---|---|---|---|---|---|---|
| keraton-yogyakarta | Q10778917 | 87 | mime application/ogg: 2, mime image/tiff: 10, mime video/webm: 1, width < 1200: 7 | 67 | 67 | 0 | 67 | 0 |
| museum-sonobudoyo | none | 7 | mime application/pdf: 7, openverse mime: 1, openverse width < 1200: 6 | 13 | 0 | 13 | 13 | 0 |
| taman-sari | none | 50 | none | 50 | 50 | 0 | 50 | 0 |
| museum-ullen-sentalu | Q3047067 | 18 | mime application/pdf: 3 | 15 | 15 | 0 | 15 | 0 |
| benteng-vredeburg | Q126126116 | 50 | mime application/pdf: 1, mime image/tiff: 4, width < 1200: 23 | 22 | 22 | 0 | 22 | 0 |
| museum-puro-pakualaman | Q109050118 | 50 | mime application/pdf: 4, mime image/svg+xml: 1, width < 1200: 9 | 36 | 36 | 0 | 36 | 0 |
| situs-ratu-boko | none | 10 | mime application/pdf: 2 | 8 | 8 | 0 | 8 | 0 |
| kawasan-kotagede | none +wp/bare-search fallback | 1 | none | 1 | 1 | 0 | 1 | 0 |
| candi-prambanan | Q47721 | 91 | width < 1200: 24 | 67 | 67 | 0 | 67 | 0 |
| masjid-gedhe-kauman | none | 43 | mime application/pdf: 3, mime image/tiff: 3, width < 1200: 11 | 26 | 26 | 0 | 26 | 0 |
| plengkung-gading | none | 10 | mime application/pdf: 2, mime image/svg+xml: 2 | 6 | 6 | 0 | 6 | 0 |
| kampung-ketandan | none +wp/bare-search fallback | 2 | mime application/pdf: 1, openverse duplicate: 2 | 2 | 1 | 1 | 2 | 0 |
| taman-budaya-yogyakarta | none +wp/bare-search fallback | 50 | mime application/pdf: 10, width < 1200: 16 | 24 | 24 | 0 | 24 | 0 |
| sekar-rinonce | none +wp/bare-search fallback | 4 | mime application/pdf: 4 | 0 | 0 | 0 | 0 | 0 |
| workshop-perak-kotagede | none +wp/bare-search fallback | 5 | mime application/pdf: 1, mime image/tiff: 1 | 5 | 3 | 2 | 5 | 0 |
| desa-wisata-tlatar | none +wp/bare-search fallback | 8 | mime application/pdf: 5, openverse mime: 20, width < 1200: 2 | 1 | 1 | 0 | 1 | 0 |
| desa-wisata-kasongan | none +wp/bare-search fallback | 51 | mime application/pdf: 1, mime video/webm: 1, width < 1200: 5 | 44 | 44 | 0 | 44 | 8 |
| desa-wisata-krebet | none +wp/bare-search fallback | 17 | none | 17 | 17 | 0 | 16 | 6 |
| desa-wisata-nglanggeran | none +wp/bare-search fallback | 18 | mime application/pdf: 1, missing page: 2, width < 1200: 3 | 12 | 12 | 0 | 12 | 5 |
| desa-wisata-widosari | none +wp/bare-search fallback | 2 | mime application/pdf: 2 | 0 | 0 | 0 | 0 | 0 |
| makam-raja-imogiri | none | 11 | mime application/pdf: 7, openverse duplicate: 12 | 12 | 4 | 8 | 12 | 0 |
| candi-sambisari | none | 39 | mime application/pdf: 5 | 34 | 34 | 0 | 34 | 0 |
| candi-ijo | Q9032333 | 49 | mime application/pdf: 9, width < 1200: 2 | 38 | 38 | 0 | 38 | 0 |
| candi-banyunibo | none | 28 | mime application/pdf: 1, width < 1200: 1 | 26 | 26 | 0 | 26 | 0 |
| museum-affandi | Q4688864 | 55 | width < 1200: 9 | 46 | 46 | 0 | 46 | 0 |
| museum-gunungapi-merapi | none | 8 | mime application/pdf: 1 | 7 | 7 | 0 | 7 | 0 |

## Places with fewer than 4 license_ok candidates

| place | license_ok | total candidates |
|---|---|---|
| kawasan-kotagede | 1 | 1 |
| kampung-ketandan | 2 | 2 |
| sekar-rinonce | 0 | 0 |
| desa-wisata-tlatar | 1 | 1 |
| desa-wisata-widosari | 0 | 0 |

## Failures

None.

Places with at least one candidate: 24 of 26.
