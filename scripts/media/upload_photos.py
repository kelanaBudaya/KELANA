#!/usr/bin/env python3
"""Validate, download, upload and SQL-generate DIY place photos (stdlib only).

Modes: --dry-run (default), --download, --upload, and --sql PATH (from manifest).
Secrets: SUPABASE_URL and SUPABASE_SERVICE_KEY are read only from the environment.
"""
import argparse
import hashlib
import json
import os
import re
import struct
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
PLACES_JSON = REPO / "docs/data/diy_places.json"
SELECTED_JSON = [
    REPO / "docs/data/photos/selected_a.json",
    REPO / "docs/data/photos/selected_b.json",
    REPO / "docs/data/photos/selected_gap.json",
]
MANIFEST = REPO / "docs/data/photos/uploaded_manifest.json"
BUCKET = "place-photos"
UA = "KelanaDataBot/0.1 (https://github.com/kelanaBudaya/KELANA; cultural tourism data collection)"
LICENSES = ["CC0-1.0", "PDM", "CC-BY-2.0", "CC-BY-2.5", "CC-BY-3.0", "CC-BY-4.0",
            "CC-BY-SA-2.0", "CC-BY-SA-2.5", "CC-BY-SA-3.0", "CC-BY-SA-4.0"]
SOURCES = ("wikimedia_commons", "openverse")
REQUIRED = ["sort_order", "caption_id", "license_code", "attribution_text", "source",
            "file_title", "page_url", "original_url", "url_1600", "url_400",
            "author_text", "license_url"]
URL_FIELDS = ["page_url", "original_url", "url_1600", "url_400", "license_url"]
MAX_BYTES = 2_000_000
PREFERRED_MAX = 1_900_000
MIN_INTERVAL = 1.0
EXT = {"image/jpeg": "jpg", "image/png": "png", "image/webp": "webp"}
SOF = {0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF}
_last_request = [0.0]


class PhotoError(Exception):
    pass


def secrets():
    vals = [os.environ.get("SUPABASE_SERVICE_KEY", "").strip(),
            os.environ.get("SUPABASE_URL", "").strip().rstrip("/")]
    return [v for v in vals if v]


def scrub(text):
    for s in secrets():
        text = text.replace(s, "***")
    return text


def throttle():
    wait = MIN_INTERVAL - (time.monotonic() - _last_request[0])
    if wait > 0:
        time.sleep(wait)
    _last_request[0] = time.monotonic()


def retry_after(err, default):
    raw = err.headers.get("Retry-After") if err.headers else None
    try:
        return min(int(raw), 300)
    except (TypeError, ValueError):
        return default


def http_get(url):
    """GET with throttle and polite retry on 429/503. Returns (content_type, bytes)."""
    for attempt in range(6):
        throttle()
        req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "image/*"})
        try:
            with urllib.request.urlopen(req, timeout=60) as resp:
                ctype = resp.headers.get("Content-Type", "").split(";")[0].strip().lower()
                return ctype, resp.read(MAX_BYTES + 1)
        except urllib.error.HTTPError as err:
            if err.code in (429, 503) and attempt < 5:
                time.sleep(retry_after(err, 10 * 2 ** attempt))
                continue
            raise PhotoError(f"HTTP {err.code} from source")
        except OSError as err:
            if attempt < 5:
                time.sleep(5 * 2 ** attempt)
                continue
            raise PhotoError(f"network error: {type(err).__name__}")
    raise PhotoError("gave up after retries")


def _jpeg_size(d):
    i, n = 2, len(d)
    while i + 4 <= n:
        if d[i] != 0xFF:
            break
        m = d[i + 1]
        if m == 0xFF:
            i += 1
            continue
        if m in (0xD8, 0x01) or 0xD0 <= m <= 0xD7:
            i += 2
            continue
        if m in (0xD9, 0xDA):
            break
        if m in SOF:
            if i + 9 > n:
                break
            h, w = struct.unpack(">HH", d[i + 5:i + 9])
            return w, h
        i += 2 + struct.unpack(">H", d[i + 2:i + 4])[0]
    raise PhotoError("jpeg dimensions not found")


def _webp_size(d):
    chunk = d[12:16]
    if chunk == b"VP8 " and len(d) >= 30:
        w = struct.unpack("<H", d[26:28])[0] & 0x3FFF
        h = struct.unpack("<H", d[28:30])[0] & 0x3FFF
    elif chunk == b"VP8L" and len(d) >= 25 and d[20] == 0x2F:
        b = int.from_bytes(d[21:25], "little")
        w, h = (b & 0x3FFF) + 1, ((b >> 14) & 0x3FFF) + 1
    elif chunk == b"VP8X" and len(d) >= 30:
        w = int.from_bytes(d[24:27], "little") + 1
        h = int.from_bytes(d[27:30], "little") + 1
    else:
        raise PhotoError("webp dimensions not found")
    return w, h


def sniff(data, ctype):
    """Check magic bytes and declared content-type; return (mime, width, height) from the header."""
    if data[:3] == b"\xff\xd8\xff":
        mime, (w, h) = "image/jpeg", _jpeg_size(data)
    elif data[:8] == b"\x89PNG\r\n\x1a\n" and len(data) >= 24:
        mime = "image/png"
        w, h = struct.unpack(">II", data[16:24])
    elif data[:4] == b"RIFF" and data[8:12] == b"WEBP":
        mime, (w, h) = "image/webp", _webp_size(data)
    else:
        raise PhotoError("file is not jpeg, png or webp")
    if ctype != mime:
        raise PhotoError(f"content-type {ctype or 'missing'} does not match {mime}")
    if w <= 0 or h <= 0:
        raise PhotoError("invalid image dimensions")
    return mime, w, h


def downsized(url):
    """Commons thumb URLs carry the width in the file name: .../1920px-Name.jpg."""
    parts = urllib.parse.urlsplit(url)
    segs = parts.path.split("/")
    m = re.match(r"(\d+)px-", segs[-1])
    if not m:
        return []
    out = []
    for width in (1280, 1024):
        segs2 = segs[:-1] + [f"{width}px-" + segs[-1][m.end():]]
        out.append(urllib.parse.urlunsplit(parts._replace(path="/".join(segs2))))
    return out


def load_places():
    rows = json.loads(PLACES_JSON.read_text(encoding="utf-8"))
    return {r["slug"]: r for r in rows}


def load_selected():
    merged, skipped = defaultdict(list), []
    for path in SELECTED_JSON:
        if not path.exists():
            skipped.append(path.name)
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        for slug, block in data.items():
            merged[slug].extend(block.get("selected", []))
    return merged, skipped


def validate(selected, places):
    rows, problems = [], []
    for slug in sorted(selected):
        items = selected[slug]
        counts = Counter(missing=0, blank_author=0, bad_license=0, bad_url_src=0,
                         null_sha1=0, dup_sha1=0)
        seen = Counter()
        for idx, p in enumerate(items):
            tag = f"{slug}[{idx}]"
            for f in REQUIRED:
                v = p.get(f)
                if v is None or (isinstance(v, str) and not v.strip() and f != "author_text"):
                    counts["missing"] += 1
                    problems.append(f"{tag} missing {f}")
            if not str(p.get("author_text") or "").strip():
                counts["blank_author"] += 1
                problems.append(f"{tag} blank author_text")
            if p.get("license_code") not in LICENSES:
                counts["bad_license"] += 1
                problems.append(f"{tag} license outside DB list: {p.get('license_code')}")
            bad = [f for f in URL_FIELDS if p.get(f) and not str(p[f]).startswith("https://")]
            if p.get("source") not in SOURCES:
                bad.append("source")
            if bad:
                counts["bad_url_src"] += 1
                problems.append(f"{tag} not https / bad source: {', '.join(bad)}")
            sha = p.get("sha1")
            if not sha:
                counts["null_sha1"] += 1
            else:
                seen[str(sha).lower()] += 1
        counts["dup_sha1"] = sum(c - 1 for c in seen.values() if c > 1)
        if counts["dup_sha1"]:
            problems.append(f"{slug} has {counts['dup_sha1']} duplicate sha1 (same photo twice)")
        in_db = slug in places
        if not in_db:
            problems.append(f"{slug} not found in diy_places.json")
        status = "OK" if in_db and not any(
            counts[k] for k in ("missing", "blank_author", "bad_license", "bad_url_src", "dup_sha1")
        ) else "FIX"
        rows.append([slug, "yes" if in_db else "NO", len(items), counts["missing"],
                     counts["blank_author"], counts["bad_license"], counts["bad_url_src"],
                     counts["dup_sha1"], counts["null_sha1"], status])
    return rows, problems


def print_table(rows):
    headers = ["slug", "in_db", "count", "missing", "blank_author", "bad_license",
               "bad_url_src", "dup_sha1", "null_sha1", "status"]
    widths = [max(len(str(x)) for x in col) for col in zip(headers, *rows)] if rows else [len(h) for h in headers]
    fmt = "  ".join("{:<%d}" % w for w in widths)
    print(fmt.format(*headers))
    print(fmt.format(*["-" * w for w in widths]))
    for r in rows:
        print(fmt.format(*[str(x) for x in r]))
    total = sum(r[2] for r in rows)
    print(f"places with photos: {len(rows)}, photos: {total}")


def fetch_photo(slug, place, p):
    """Download and verify one photo. Returns (entry, [(storage_path, bytes), (thumb_path, bytes)])."""
    cands = [p["url_1600"]] + downsized(p["url_1600"])
    chosen = fallback = None
    for url in cands:
        ctype, data = http_get(url)
        if len(data) > MAX_BYTES:
            continue
        if len(data) <= PREFERRED_MAX:
            chosen = (url, ctype, data)
            break
        fallback = (url, ctype, data)
    chosen = chosen or fallback
    if chosen is None:
        raise PhotoError("no 1600px variant within 2 MB")
    url, ctype, big = chosen
    mime, w, h = sniff(big, ctype)
    if p.get("sha1"):
        sha, sha_src = str(p["sha1"]).lower(), "commons"
    else:
        sha, sha_src = hashlib.sha1(big).hexdigest(), "computed"
    if not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise PhotoError("invalid sha1")
    ctype2, small = http_get(p["url_400"])
    if len(small) > MAX_BYTES:
        raise PhotoError("400px thumb larger than 2 MB")
    mime2, _, _ = sniff(small, ctype2)
    sha8 = sha[:8]
    storage_path = f"diy/{slug}/{sha8}.{EXT[mime]}"
    thumb_path = f"diy/{slug}/{sha8}_t.{EXT[mime2]}"
    entry = {
        "slug": slug, "place_id": place["id"], "sort_order": int(p["sort_order"]),
        "storage_path": storage_path, "thumb_path": thumb_path,
        "width": w, "height": h, "bytes": len(big), "thumb_bytes": len(small),
        "sha1": sha, "sha1_source": sha_src, "served_url": url,
        "source": p["source"], "source_page_url": p["page_url"],
        "original_url": p.get("original_url"), "caption": p.get("caption_id"),
        "author": p["author_text"], "author_url": p.get("author_url") or None,
        "license_code": p["license_code"], "license_url": p["license_url"],
        "attribution_text": p["attribution_text"],
    }
    return entry, [(storage_path, big), (thumb_path, small)]


def fetch_all(selected, places, workdir):
    entries, errors = [], []
    for slug in sorted(selected):
        used_sha, used_sha8 = set(), set()
        for p in sorted(selected[slug], key=lambda x: int(x.get("sort_order") or 0)):
            label = f"{slug} sort {p.get('sort_order')}"
            try:
                entry, files = fetch_photo(slug, places[slug], p)
                if entry["sha1"] in used_sha or entry["sha1"][:8] in used_sha8:
                    raise PhotoError("duplicate sha1 within place, skipped")
                used_sha.add(entry["sha1"])
                used_sha8.add(entry["sha1"][:8])
                for rel, data in files:
                    dest = workdir / rel
                    dest.parent.mkdir(parents=True, exist_ok=True)
                    dest.write_bytes(data)
                entries.append(entry)
                print(f"ok   {label}  {entry['storage_path']}  {entry['width']}x{entry['height']}")
            except Exception as err:  # keep going; failures are reported and recorded
                msg = scrub(f"{type(err).__name__}: {err}" if not isinstance(err, PhotoError) else str(err))
                errors.append(f"{label}: {msg}")
                print(f"FAIL {label}: {msg}")
    return entries, errors


def upload_file(base, key, rel, data, mime):
    url = f"{base}/storage/v1/object/{BUCKET}/{urllib.parse.quote(rel, safe='/')}"
    for attempt in range(6):
        throttle()
        req = urllib.request.Request(url, data=data, method="POST", headers={
            "apikey": key, "Authorization": f"Bearer {key}", "x-upsert": "true",
            "Content-Type": mime, "User-Agent": UA})
        try:
            with urllib.request.urlopen(req, timeout=120) as resp:
                resp.read()
                return
        except urllib.error.HTTPError as err:
            if err.code in (429, 503) and attempt < 5:
                time.sleep(retry_after(err, 10 * 2 ** attempt))
                continue
            raise PhotoError(f"upload HTTP {err.code} for {rel}")
        except OSError as err:
            if attempt < 5:
                time.sleep(5 * 2 ** attempt)
                continue
            raise PhotoError(f"upload network error for {rel}: {type(err).__name__}")


def upload_all(entries, workdir, errors):
    base = os.environ.get("SUPABASE_URL", "").strip().rstrip("/")
    key = os.environ.get("SUPABASE_SERVICE_KEY", "").strip()
    for e in entries:
        for rel, mime_key in ((e["storage_path"], EXT_MIME(e["storage_path"])),
                              (e["thumb_path"], EXT_MIME(e["thumb_path"]))):
            try:
                upload_file(base, key, rel, (workdir / rel).read_bytes(), mime_key)
                print(f"uploaded {rel}")
            except Exception as err:
                msg = scrub(str(err) if isinstance(err, PhotoError) else type(err).__name__)
                errors.append(f"upload {rel}: {msg}")
                print(f"FAIL upload {rel}: {msg}")


def EXT_MIME(path):
    ext = path.rsplit(".", 1)[-1]
    return {"jpg": "image/jpeg", "png": "image/png", "webp": "image/webp"}[ext]


def sql_lit(v):
    if v is None:
        return "NULL"
    if isinstance(v, int):
        return str(v)
    return "'" + str(v).replace("\x00", "").replace("'", "''") + "'"


def write_sql(path):
    data = json.loads(MANIFEST.read_text(encoding="utf-8"))
    if data.get("errors"):
        sys.exit(f"manifest has {len(data['errors'])} failed photos; fix and re-run --download before --sql")
    lines = ["-- Generated by scripts/media/upload_photos.py --sql. Do not edit by hand.",
             "begin;"]
    for slug in sorted(data["photos"]):
        for e in sorted(data["photos"][slug], key=lambda x: x["sort_order"]):
            cols = ["storage_path", "thumb_path", "width", "height", "sha1", "source",
                    "source_page_url", "original_url", "caption", "author", "author_url",
                    "license_code", "license_url", "attribution_text", "sort_order"]
            vals = [e["storage_path"], e["thumb_path"], e["width"], e["height"], e["sha1"],
                    e["source"], e["source_page_url"], e["original_url"], e["caption"],
                    e["author"], e["author_url"], e["license_code"], e["license_url"],
                    e["attribution_text"], e["sort_order"]]
            lines.append(
                "insert into public.place_photos (place_id, " + ", ".join(cols) + ", is_published)\n"
                "select id, " + ", ".join(sql_lit(v) for v in vals) + ", false\n"
                f"from public.places where slug = {sql_lit(slug)}\n"
                "on conflict (place_id, sha1) do update set\n"
                "  storage_path = excluded.storage_path,\n"
                "  thumb_path = excluded.thumb_path,\n"
                "  caption = excluded.caption,\n"
                "  attribution_text = excluded.attribution_text,\n"
                "  sort_order = excluded.sort_order,\n"
                "  license_code = excluded.license_code,\n"
                "  license_url = excluded.license_url;"
            )
    lines.append("commit;")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
    print(f"wrote {path} ({len(lines) - 3} inserts)")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    g = ap.add_mutually_exclusive_group()
    g.add_argument("--dry-run", action="store_true", help="validate only (default)")
    g.add_argument("--download", action="store_true", help="download and verify, write manifest")
    g.add_argument("--upload", action="store_true", help="download, verify, upload to Supabase Storage")
    ap.add_argument("--sql", metavar="PATH", help="write idempotent SQL from the manifest")
    ap.add_argument("--workdir", default=None, help="work folder (default: temp dir outside repo)")
    args = ap.parse_args()
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

    if args.upload:
        base = os.environ.get("SUPABASE_URL", "").strip()
        key = os.environ.get("SUPABASE_SERVICE_KEY", "").strip()
        if not base or not key:
            sys.exit("aborting: set SUPABASE_URL and SUPABASE_SERVICE_KEY in the environment first")
        if not base.startswith("https://"):
            sys.exit("aborting: SUPABASE_URL must be an https URL")

    places = load_places()
    selected, skipped = load_selected()
    for name in skipped:
        print(f"skipped missing input: {name}")
    rows, problems = validate(selected, places)
    print_table(rows)
    for p in problems:
        print(f"  ! {p}")

    if args.download or args.upload:
        if problems:
            sys.exit("aborting: fix the validation problems above before downloading")
        workdir = Path(args.workdir) if args.workdir else Path(tempfile.gettempdir()) / "kelana-photo-work"
        workdir.mkdir(parents=True, exist_ok=True)
        print(f"work folder: {workdir}")
        entries, errors = fetch_all(selected, places, workdir)
        if args.upload:
            upload_all(entries, workdir, errors)
        for e in entries:
            e.pop("served_url", None)
        manifest = {
            "generated_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
            "bucket": BUCKET,
            "uploaded": bool(args.upload),
            "photos": defaultdict(list),
            "errors": errors,
        }
        for e in entries:
            manifest["photos"][e["slug"]].append(e)
        manifest["photos"] = dict(manifest["photos"])
        MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
                            encoding="utf-8", newline="\n")
        print(f"manifest: {MANIFEST} ({len(entries)} photos, {len(errors)} errors)")

    if args.sql:
        write_sql(Path(args.sql))

    if args.dry_run or not (args.download or args.upload or args.sql):
        sys.exit(1 if problems else 0)
    sys.exit(1 if (args.download or args.upload) and errors else 0)


if __name__ == "__main__":
    main()
