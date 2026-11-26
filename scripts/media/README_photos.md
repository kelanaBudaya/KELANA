# Photo upload pipeline (DIY)

Stdlib-only Python 3. Reads the selected photo JSON files and `docs/data/diy_places.json`.

Modes (first is the default):
- `python scripts/media/upload_photos.py --dry-run`: validates inputs, prints a per-place table. Downloads nothing.
- `--download`: downloads 1600px and 400px files (1 req/s, polite retry on 429/503), verifies type and size, writes `docs/data/photos/uploaded_manifest.json`.
- `--upload`: same as `--download`, then POSTs each file to Supabase Storage bucket `place-photos`. Needs env `SUPABASE_URL` and `SUPABASE_SERVICE_KEY`; values are never printed.
- `--sql supabase/data/place_photos_diy.sql`: writes the idempotent insert SQL from the manifest (refuses if the manifest has errors).

Options: `--workdir PATH` (default: system temp dir `kelana-photo-work`, outside the repo).
Missing selected files (e.g. `selected_gap.json`) are skipped. Exit code 1 means problems were found.
