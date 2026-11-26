# Photo review notes, set A (13 places)

Reviewer A looked at 107 preview thumbnails (shortlists of up to 12 per place, built from candidates.jsonl metadata) and selected photos in `selected_a.json`. 36 photos selected in total.

| place | selected | notes |
|---|---|---|
| keraton-yogyakarta | 4 | enough good Commons material |
| museum-sonobudoyo | 4 | all four are Openverse rows (actually Commons files); weaker quality (phone photos), one portrait object shot |
| taman-sari | 4 | good |
| museum-ullen-sentalu | 4 | dark forest setting, 1 portrait; quality is average |
| benteng-vredeburg | 4 | good |
| museum-puro-pakualaman | 1 | needs more searching |
| situs-ratu-boko | 4 | good |
| kawasan-kotagede | 0 | needs more searching |
| candi-prambanan | 4 | good, 1 portrait relief |
| masjid-gedhe-kauman | 4 | good |
| plengkung-gading | 2 | needs more searching |
| kampung-ketandan | 1 | needs more searching |
| taman-budaya-yogyakarta | 1 | needs more searching, identity of the building rests on the file title only |

## Places needing more searching

- **kawasan-kotagede** (0): only candidate was a gravestone close-up. Try Commons categories `Kotagede`, `Kotagede, Yogyakarta`, `Pasar Kotagede`, `Masjid Gedhe Mataram Kotagede`, `Makam Raja-raja Mataram Kotagede`, `Rumah Kalang Kotagede`, and search terms "Kotagede", "Kota Gede Yogyakarta", "Masjid Agung Mataram", "Pasar Gede Kotagede".
- **museum-puro-pakualaman** (1): candidates are mostly processions and dances. Try `Pura Pakualaman`, `Puro Pakualaman`, `Pakualaman Palace`, "Bangsal Sewatama", "Kadipaten Pakualaman", "Museum Pura Pakualaman".
- **plengkung-gading** (2): try `Plengkung Gading`, `Plengkung Nirbaya`, `Benteng Baluwarti`, "Baluwarti Yogyakarta", "Plengkung Wijilan", "Plengkung Tarunasura", "Alun-alun Kidul".
- **kampung-ketandan** (1): try `Kampung Ketandan`, `Pecinan Ketandan`, "Ketandan Malioboro", "Imlek Ketandan", "Pecinan Yogyakarta", "Jalan Pajeksan", "Gapura Ketandan".
- **taman-budaya-yogyakarta** (1): try `Taman Budaya Yogyakarta`, `Taman Budaya Yogyakarta Concert Hall`, "Societeit Militair Yogyakarta", "Concert Hall TBY", "Gedung Societet", "Jalan Sriwedani".
- **museum-sonobudoyo** (4, but weak): try `Museum Sonobudoyo` (Commons category), `Sonobudoyo Museum`, "Museum Negeri Sonobudoyo Unit I", "Kotagede Sonobudoyo".
- **museum-ullen-sentalu** (4, average): try `Museum Ullen Sentalu`, "Ullen Sentalu Kaliurang".

## Licence and data observations

- Public domain, generic: `taman-budaya-yogyakarta` photo (BP81Siti, "Public domain", no licence URL in metadata, Commons categories `PD Indonesian Government`). Mapped to `PDM` with the Creative Commons Public Domain Mark URL. The file title says "Gedung Taman Budaya, Yogyakarta", but the description is a generic family stroll note in Sundanese; verify the place before publishing.
- The four `museum-sonobudoyo` photos and the Ketandan duplicate are `source: openverse`, but their `original_url` points to upload.wikimedia.org, so they are Commons files that Openverse indexed. `file_title` was null in candidates, so I derived "File:<name>" from the original URL; `sha1` is null and P3 must compute it. `page_url` for these is a `commons.wikimedia.org/w/index.php?curid=...` link.
- Openverse thumbnails (`api.openverse.org/.../thumb/`) returned HTTP 400 for these files at review time (the proxy failed to render). I viewed them by downloading `url_1600` (a Commons original) and resizing locally. P3 should fetch the Commons original rather than rely on the Openverse `url_400`.
- CC0 license URLs in candidates end with `/deed.en`; I normalised `license_url` to the canonical `https://creativecommons.org/publicdomain/zero/1.0/`, and CC BY / BY-SA URLs to a trailing slash with the version (for example `https://creativecommons.org/licenses/by-sa/4.0/`).
- Authors with unusual author_text: `benteng-vredeburg` png file lists several names joined with spaces ("Erlyndita Setyawardani Fandy Aprianto Rohman Bernadetta Alice Caroline"); `keraton-yogyakarta` and `taman-sari` Uwe Aranas files list "CEphoto, Uwe Aranas". Kept as given.
- Several Commons uploaders have recent-dated files (for example "sep 2026" in the Sonobudoyo file name); nothing suspicious in licence terms.
- One Prambanan file (Aditya Prabaswara) carries a visible watermark and was rejected despite a valid licence.
