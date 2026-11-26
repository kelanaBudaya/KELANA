#!/usr/bin/env python3
"""P1 / P1b photo candidate harvest for the DIY galeri (KELANA).

Stdlib only. For every place in docs/data/diy_places.json:
  P1  Wikidata wbsearchentities (id, then en) -> QID -> P18 image + P373 category;
      Commons categorymembers (files) + search (namespace 6, "<name> Yogyakarta");
      Commons imageinfo (1600 px and 400 px thumbs).
  P1b Openverse images search, only for places with fewer than 6 Commons candidates.

Every successful raw API response is cached under docs/data/photos/.cache/ (not
committed), so re-runs only hit the network for missing responses.

Outputs:
  docs/data/photos/candidates.jsonl   one JSON line per candidate (license_ok is advisory)
  docs/data/photos/harvest_report.md  per-place counts and API call totals

Usage:
  python scripts/media/commons_harvest.py [--only SLUG] [--skip-openverse]
"""
import argparse
import hashlib
import html
import json
import math
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from collections import Counter
from datetime import date
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
PLACES_FILE = REPO / "docs/data/diy_places.json"
OUT_DIR = REPO / "docs/data/photos"
CACHE_DIR = OUT_DIR / ".cache"
CANDIDATES_FILE = OUT_DIR / "candidates.jsonl"
REPORT_FILE = OUT_DIR / "harvest_report.md"

USER_AGENT = ("KelanaDataBot/0.1 (https://github.com/kelanaBudaya/KELANA; "
              "cultural tourism data collection)")
WIKIDATA_API = "https://www.wikidata.org/w/api.php"
COMMONS_API = "https://commons.wikimedia.org/w/api.php"
OPENVERSE_API = "https://api.openverse.org/v1/images/"

MIN_INTERVAL = 1.0          # seconds between any two requests (all hosts)
MAXLAG = "5"
MIN_WIDTH = 1200
ALLOWED_MIME = {"image/jpeg", "image/png", "image/webp"}
OPENVERSE_THRESHOLD = 6     # P1b runs only for places with fewer Commons candidates
MAX_TITLES = 50
ACCEPT_CC_VERSIONS = {"2.0", "2.5", "3.0", "4.0"}
PLACE_DESC_RE = re.compile(r"yogyakarta|jogja|daerah istimewa|\bDIY\b", re.I)
CANDIDATE_FIELDS = [
    "slug", "source", "file_title", "page_url", "original_url", "url_1600",
    "url_400", "width", "height", "mime", "sha1", "author_text", "author_url",
    "license_short", "license_url", "usage_terms", "description", "categories",
    "gps", "license_ok", "license_reason",
]


class Api:
    """Single gate for all HTTP: throttles to MIN_INTERVAL, backs off, caches."""

    def __init__(self):
        self.last_request = 0.0
        self.http_requests = 0   # every request sent, retries included
        self.retries = 0
        self.cache_hits = 0
        self.failures = []      # (label, reason)

    def _throttle(self):
        wait = MIN_INTERVAL - (time.monotonic() - self.last_request)
        if wait > 0:
            time.sleep(wait)
        self.last_request = time.monotonic()

    def _back_off(self, attempt, retry_after, label, reason):
        self.retries += 1
        try:
            wait = float(retry_after)
        except (TypeError, ValueError):
            wait = (5.0 if reason == "maxlag" else 30.0) * attempt
        print(f"  back-off {wait:.0f}s ({reason}) on {label}", file=sys.stderr)
        time.sleep(wait)

    def _fetch(self, url, label):
        attempt = 0
        while True:
            attempt += 1
            self._throttle()
            self.http_requests += 1
            req = urllib.request.Request(
                url, headers={"User-Agent": USER_AGENT, "Accept": "application/json"})
            try:
                with urllib.request.urlopen(req, timeout=60) as resp:
                    body = resp.read().decode("utf-8")
            except urllib.error.HTTPError as exc:
                retry_after = exc.headers.get("Retry-After") if exc.headers else None
                if exc.code in (429, 503) and attempt <= 5:
                    self._back_off(attempt, retry_after, label, f"HTTP {exc.code}")
                    continue
                self.failures.append((label, f"HTTP {exc.code}"))
                return None
            except (urllib.error.URLError, OSError) as exc:
                if attempt <= 3:
                    self._back_off(attempt, None, label, type(exc).__name__)
                    continue
                self.failures.append((label, type(exc).__name__))
                return None
            try:
                data = json.loads(body)
            except ValueError:
                self.failures.append((label, "invalid JSON"))
                return None
            err = data.get("error") if isinstance(data, dict) else None
            if err and err.get("code") == "maxlag":
                if attempt <= 6:
                    retry_after = None
                    self._back_off(attempt, retry_after, label, "maxlag")
                    continue
                self.failures.append((label, "maxlag, gave up"))
                return None
            if err:
                self.failures.append((label, f"API error {err.get('code')}"))
                return None
            return body

    def call(self, base, params, label, kind):
        """kind: 'mw' (Commons), 'wd' (Wikidata), 'ov' (Openverse)."""
        p = dict(params)
        if kind == "mw":
            p.update(format="json", formatversion="2", maxlag=MAXLAG)
        elif kind == "wd":
            p.update(format="json", maxlag=MAXLAG)
        url = f"{base}?{urllib.parse.urlencode(sorted(p.items()))}"
        cache_file = CACHE_DIR / (hashlib.sha256(url.encode("utf-8")).hexdigest() + ".json")
        if cache_file.exists():
            self.cache_hits += 1
            body = json.loads(cache_file.read_text(encoding="utf-8"))["body"]
        else:
            body = self._fetch(url, label)
            if body is None:
                return None
            CACHE_DIR.mkdir(parents=True, exist_ok=True)
            cache_file.write_text(json.dumps({"url": url, "body": body}, ensure_ascii=False),
                                  encoding="utf-8")
        return json.loads(body)


# ---------- text helpers ----------

def strip_html(value):
    text = re.sub(r"<[^>]+>", " ", value or "")
    text = html.unescape(text)
    return re.sub(r"\s+", " ", text).strip()


def first_https_href(value):
    m = re.search(r'href="([^"]+)"', value or "")
    if not m:
        return None
    href = html.unescape(m.group(1))
    if href.startswith("//"):
        href = "https:" + href
    elif href.startswith("/"):
        href = "https://commons.wikimedia.org" + href
    return href if href.startswith("https://") else None


def https_url(url):
    if not url:
        return None
    return re.sub(r"^http://", "https://", url.strip())


# ---------- license ----------

def classify_license(short, url):
    """Return (license_code_or_None, accept_bool, reason). Accept list per plan."""
    s = (short or "").strip().lower().replace("_", " ")
    if not s:
        s = (url or "").strip().lower()
    if not s:
        return None, False, "license missing"
    if "fair use" in s or "fair-use" in s:
        return None, False, "fair use"
    if re.search(r"(^|[\s\-/])(nc|nd)($|[\s\-/])", s):
        return None, False, "NC or ND restriction"
    if re.search(r"(^|[\s\-/])igo($|[\s\-/])", s):
        return None, False, "IGO license"
    if "cc0" in s or "publicdomain/zero" in s:
        return "CC0-1.0", True, "accept: CC0"
    if "public domain mark" in s or re.search(r"\bpdm\b", s) or "publicdomain/mark" in s:
        return "PDM", True, "accept: public domain mark"
    m = re.search(r"\bpd-(old|self)\b", s)
    if m:
        return f"PD-{m.group(1)}", True, (
            f"accept: PD-{m.group(1)} (not in place_photos license_code list, needs mapping)")
    if re.search(r"\bpd\b|public domain", s):
        return "PD", True, "accept: public domain (generic, needs mapping)"
    m = re.search(r"(?:cc[ -]|licenses/)by(-sa)?[ /-]?(\d\.\d)", s)
    if m:
        family = "CC-BY-SA" if m.group(1) else "CC-BY"
        version = m.group(2)
        code = f"{family}-{version}"
        if version not in ACCEPT_CC_VERSIONS:
            return code, False, f"{family} {version} outside accepted 2.0-4.0"
        return code, True, f"accept: {code}"
    if "gfdl" in s:
        return None, False, "GFDL-only"
    return None, False, f"license not in accept list: {short or url}"


def finalize_license(cand):
    code, ok, reason = classify_license(cand["license_short"], cand["license_url"])
    if ok and not cand["author_text"]:
        ok, reason = False, "empty author"
    elif ok and cand["author_text"].lower() in ("unknown", "unknown author", "anonymous"):
        ok, reason = False, "author unknown"
    cand["license_ok"] = ok
    cand["license_reason"] = reason


# ---------- geo ----------

def haversine_km(lat1, lon1, lat2, lon2):
    r = 6371.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = p2 - p1
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


# ---------- Wikidata + Commons ----------

def find_wikidata(api, name):
    for lang in ("id", "en"):
        data = api.call(WIKIDATA_API, {
            "action": "wbsearchentities", "search": name, "language": lang,
            "uselang": lang, "type": "item", "limit": "7",
        }, f"wbsearch {lang} {name}", "wd") or {}
        for hit in data.get("search", []):
            desc = hit.get("description", "")
            if PLACE_DESC_RE.search(desc):
                return hit["id"], desc
    return None, None


def wikidata_files_and_categories(api, qid):
    data = api.call(WIKIDATA_API, {"action": "wbgetentities", "ids": qid, "props": "claims"},
                    f"wbgetentities {qid}", "wd") or {}
    claims = (data.get("entities", {}).get(qid, {}) or {}).get("claims", {})

    def values(prop):
        out = []
        for claim in claims.get(prop, []):
            v = claim.get("mainsnak", {}).get("datavalue", {}).get("value")
            if isinstance(v, str):
                out.append(v)
        return out
    return values("P18"), values("P373")


def fetch_commons_pages(api, titles, label, with_meta):
    """Return {title: page} for the given File titles (imageinfo at 1600 px)."""
    pages = {}
    for i in range(0, len(titles), MAX_TITLES):
        chunk = titles[i:i + MAX_TITLES]
        params = {"action": "query", "titles": "|".join(chunk), "prop": "imageinfo"}
        if with_meta:
            params.update(prop="imageinfo|coordinates|categories", cllimit="500",
                          iiprop="url|size|mime|sha1|extmetadata", iiurlwidth="1600")
        else:
            params.update(iiprop="url", iiurlwidth="400")
        data = api.call(COMMONS_API, params, f"{label} batch {i // MAX_TITLES + 1}", "mw") or {}
        for page in data.get("query", {}).get("pages", []):
            if not page.get("missing") and "title" in page:
                pages[page["title"]] = page
    return pages


def commons_candidate(slug, page, thumb400, counts):
    title = page["title"]
    infos = page.get("imageinfo") or []
    if not infos:
        counts["no imageinfo"] += 1
        return None
    ii = infos[0]
    mime = ii.get("mime")
    if mime not in ALLOWED_MIME:
        counts[f"mime {mime}"] += 1
        return None
    width, height = ii.get("width") or 0, ii.get("height") or 0
    if width < MIN_WIDTH:
        counts["width < 1200"] += 1
        return None
    ext = ii.get("extmetadata") or {}

    def ev(key):
        return (ext.get(key) or {}).get("value")

    coords = page.get("coordinates") or []
    cand = {
        "slug": slug,
        "source": "wikimedia_commons",
        "file_title": title,
        "page_url": "https://commons.wikimedia.org/wiki/"
                    + urllib.parse.quote(title.replace(" ", "_"), safe=":/"),
        "original_url": ii.get("url"),
        "url_1600": ii.get("thumburl") or ii.get("url"),
        "url_400": thumb400.get(title),
        "width": width,
        "height": height,
        "mime": mime,
        "sha1": ii.get("sha1"),
        "author_text": strip_html(ev("Artist")),
        "author_url": first_https_href(ev("Artist")),
        "license_short": (ev("LicenseShortName") or "").strip() or None,
        "license_url": https_url(ev("LicenseUrl")),
        "usage_terms": strip_html(ev("UsageTerms")) or None,
        "description": (strip_html(ev("ImageDescription"))[:500]) or None,
        "categories": [c["title"].removeprefix("Category:") for c in page.get("categories", [])],
        "gps": ({"lat": coords[0]["lat"], "lon": coords[0]["lon"]} if coords else None),
    }
    if not cand["sha1"]:
        counts["no sha1"] += 1
        return None
    return cand


GENERIC_PREFIX_RE = re.compile(
    r"^(desa wisata|museum negeri|museum|workshop|kawasan|kampung|situs|makam)\s+", re.I)


def core_name(name):
    """'Desa Wisata Kasongan' -> 'Kasongan' (fallback query only)."""
    core = GENERIC_PREFIX_RE.sub("", name).strip()
    core = re.sub(r"\s*\(.*?\)\s*", " ", core).strip()
    return core or name


def harvest_commons(api, place):
    name, slug = place["name"], place["slug"]
    counts = Counter()
    qid, desc = find_wikidata(api, name)
    titles, seen = [], set()

    def add(title):
        if title not in seen:
            seen.add(title)
            titles.append(title)

    p18, p373 = [], []
    if qid:
        p18, p373 = wikidata_files_and_categories(api, qid)
        for f in p18:
            add("File:" + f)
    for cat in p373:
        data = api.call(COMMONS_API, {"action": "query", "list": "categorymembers",
                                      "cmtitle": "Category:" + cat, "cmtype": "file",
                                      "cmlimit": "50"}, f"catmembers {cat}", "mw") or {}
        for m in data.get("query", {}).get("categorymembers", []):
            add(m["title"])
    data = api.call(COMMONS_API, {"action": "query", "list": "search",
                                  "srsearch": f"{name} Yogyakarta", "srnamespace": "6",
                                  "srlimit": "50"}, f"search {name}", "mw") or {}
    for hit in data.get("query", {}).get("search", []):
        add(hit["title"])

    fallback = False
    if len(titles) < 4:
        # Fallback (plan: Wikipedia article images when Wikidata/Commons give too little).
        # Titles that are not on Commons come back "missing" and are dropped.
        fallback = True
        for lang in ("id", "en"):
            data = api.call(f"https://{lang}.wikipedia.org/w/api.php", {
                "action": "query", "titles": name, "prop": "images", "imlimit": "max",
            }, f"wp images {lang} {name}", "mw") or {}
            for pg in data.get("query", {}).get("pages", []):
                for im in pg.get("images", []):
                    add(im["title"])
        for cat in (name, f"{name} Yogyakarta"):
            data = api.call(COMMONS_API, {"action": "query", "list": "categorymembers",
                                          "cmtitle": "Category:" + cat, "cmtype": "file",
                                          "cmlimit": "50"}, f"catmembers-name {cat}", "mw") or {}
            for m in data.get("query", {}).get("categorymembers", []):
                add(m["title"])
        data = api.call(COMMONS_API, {"action": "query", "list": "search", "srsearch": name,
                                      "srnamespace": "6", "srlimit": "50"},
                        f"search-bare {name}", "mw") or {}
        for hit in data.get("query", {}).get("search", []):
            add(hit["title"])
        core = core_name(name)
        if core != name and len(titles) < 4:
            data = api.call(COMMONS_API, {"action": "query", "list": "search", "srsearch": core,
                                          "srnamespace": "6", "srlimit": "50"},
                            f"search-core {core}", "mw") or {}
            for hit in data.get("query", {}).get("search", []):
                add(hit["title"])
    counts["discovered"] = len(titles)

    pages = fetch_commons_pages(api, titles, slug, with_meta=True)
    survivors = []
    for title in titles:
        page = pages.get(title)
        if page is None:
            counts["missing page"] += 1
            continue
        cand = commons_candidate(slug, page, {}, counts)
        if cand is not None:
            survivors.append(cand)

    by_sha = set()
    kept = []
    for cand in survivors:
        if cand["sha1"] in by_sha:
            counts["duplicate sha1"] += 1
            continue
        by_sha.add(cand["sha1"])
        kept.append(cand)

    thumbs = fetch_commons_pages(api, [c["file_title"] for c in kept], slug + " 400", with_meta=False)
    for cand in kept:
        page = thumbs.get(cand["file_title"], {})
        infos = page.get("imageinfo") or []
        cand["url_400"] = (infos[0].get("thumburl") if infos else None) or None
        finalize_license(cand)

    return {"place": place, "qid": qid, "wd_desc": desc, "p18": len(p18),
            "p373": p373, "fallback": fallback, "counts": counts, "candidates": kept,
            "openverse_ran": False, "openverse_added": 0}


# ---------- Openverse (P1b) ----------

OV_MIME = {"jpg": "image/jpeg", "jpeg": "image/jpeg", "png": "image/png", "webp": "image/webp"}


def harvest_openverse(api, place, res):
    slug = place["slug"]
    res["openverse_ran"] = True
    existing = {c["original_url"] for c in res["candidates"]}
    counts = res["counts"]
    results = []
    queries = [f"{place['name']} Yogyakarta", place["name"], core_name(place["name"])]
    for query in dict.fromkeys(queries):
        data = api.call(OPENVERSE_API, {"q": query, "license": "cc0,pdm,by,by-sa",
                                        "page_size": "20"},
                        f"openverse {slug} [{query}]", "ov") or {}
        results.extend(data.get("results", []))
    for r in results:
        mime = OV_MIME.get((r.get("filetype") or "").lower())
        if mime is None:
            counts["openverse mime"] += 1
            continue
        width, height = r.get("width") or 0, r.get("height") or 0
        if width < MIN_WIDTH:
            counts["openverse width < 1200"] += 1
            continue
        if r.get("url") in existing:
            counts["openverse duplicate"] += 1
            continue
        lic = (r.get("license") or "").lower()
        ver = r.get("license_version") or ""
        short = "CC0" if lic == "cc0" else "PDM" if lic == "pdm" else f"CC {lic.upper()} {ver}".strip()
        author = strip_html(r.get("creator") or "")
        cand = {
            "slug": slug,
            "source": "openverse",
            "file_title": None,
            "page_url": r.get("foreign_landing_url"),
            "original_url": r.get("url"),
            "url_1600": r.get("url"),
            "url_400": r.get("thumbnail"),
            "width": width,
            "height": height,
            "mime": mime,
            "sha1": None,
            "author_text": author,
            "author_url": https_url(r.get("creator_url")),
            "license_short": short,
            "license_url": https_url(r.get("license_url")),
            "usage_terms": None,
            "description": strip_html(r.get("title") or "")[:500] or None,
            "categories": [],
            "gps": None,
        }
        finalize_license(cand)
        existing.add(r.get("url"))
        res["candidates"].append(cand)
        res["openverse_added"] += 1


# ---------- output ----------

def write_outputs(results, api):
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    with CANDIDATES_FILE.open("w", encoding="utf-8", newline="\n") as fh:
        for res in results:
            for cand in res["candidates"]:
                row = {k: cand.get(k) for k in CANDIDATE_FIELDS}
                fh.write(json.dumps(row, ensure_ascii=False) + "\n")

    cache_files = len(list(CACHE_DIR.glob("*.json"))) if CACHE_DIR.exists() else 0
    lines = [
        "# Harvest report: galeri foto DIY (P1 + P1b)",
        "",
        f"Generated {date.today().isoformat()} by `scripts/media/commons_harvest.py` "
        "(stdlib only, no secrets). Candidates are NOT accepted yet; `license_ok` is advisory "
        "and the reviewer (P2) decides.",
        "",
        "## API usage",
        "",
        f"- HTTP requests sent in the last run (retries included): **{api.http_requests}**",
        f"- Requests served from `.cache/` in the last run: {api.cache_hits}",
        f"- Successful raw responses cached (= distinct API calls made so far): **{cache_files}**",
        f"- Back-offs (maxlag / 429 / 503 / network): {api.retries}",
        f"- Failed requests (not cached, skipped): {len(api.failures)}",
        "",
        "## Rules applied",
        "",
        "- Commons: files of type image/jpeg, image/png, image/webp with width >= 1200 px; "
        "duplicates by sha1 removed per place. Other files are counted below, not written.",
        "- Wikidata: first wbsearchentities hit (id, then en) whose description mentions "
        "Yogyakarta / Jogja / Daerah Istimewa / DIY; otherwise Commons search only.",
        "- Fallback when fewer than 4 titles were found: image list of the id and en Wikipedia "
        "article with the place name, and a Commons search on the bare name. Replaces the "
        "planned local Firecrawl scrape (stdlib only); titles not on Commons are dropped.",
        "- License accept list: CC0, PDM, PD (PD-old / PD-self / generic), CC BY and CC BY-SA "
        "2.0 to 4.0. Rejected: NC, ND, IGO, GFDL-only, fair use, unknown or missing, empty or "
        "unknown author.",
        "- Openverse (P1b) only for places with fewer than 6 Commons candidates; "
        "`sha1` and `file_title` are null there (P3 must compute sha1 after download), "
        "`url_400` is Openverse's own thumbnail (size not guaranteed).",
        "- `license_code` values PD-old / PD-self / PD are not in the place_photos check list; "
        "they are flagged in `license_reason` for the reviewer.",
        "",
        "## Per place",
        "",
        "| place | QID (wd desc ok) | discovered | rejected (reason: count) | candidates | "
        "commons | openverse | license_ok | GPS > 2 km from place |",
        "|---|---|---|---|---|---|---|---|---|",
    ]
    under4 = []
    with_rows_total = 0
    for res in results:
        place = res["place"]
        cands = res["candidates"]
        ok = sum(1 for c in cands if c["license_ok"])
        n_com = sum(1 for c in cands if c["source"] == "wikimedia_commons")
        n_ov = sum(1 for c in cands if c["source"] == "openverse")
        far = 0
        for c in cands:
            if c["gps"]:
                if haversine_km(c["gps"]["lat"], c["gps"]["lon"], place["lat"], place["lng"]) > 2.0:
                    far += 1
        rejects = {k: v for k, v in res["counts"].items() if k != "discovered" and v}
        rej_txt = ", ".join(f"{k}: {v}" for k, v in sorted(rejects.items())) or "none"
        qid = res["qid"] or "none"
        if res["fallback"]:
            qid += " +wp/bare-search fallback"
        lines.append(
            f"| {place['slug']} | {qid} | {res['counts'].get('discovered', 0)} | {rej_txt} | "
            f"{len(cands)} | {n_com} | {n_ov} | {ok} | {far} |")
        if ok < 4:
            under4.append((place["slug"], ok, len(cands)))
        if cands:
            with_rows_total += 1

    lines += ["", "## Places with fewer than 4 license_ok candidates", ""]
    if under4:
        lines += ["| place | license_ok | total candidates |", "|---|---|---|"]
        lines += [f"| {s} | {o} | {t} |" for s, o, t in under4]
    else:
        lines.append("None.")
    lines += ["", "## Failures", ""]
    if api.failures:
        lines += [f"- {label}: {reason}" for label, reason in api.failures]
    else:
        lines.append("None.")
    lines += ["", f"Places with at least one candidate: {with_rows_total} of {len(results)}.", ""]
    REPORT_FILE.write_text("\n".join(lines), encoding="utf-8")
    return with_rows_total


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--only", help="harvest a single place slug")
    parser.add_argument("--skip-openverse", action="store_true")
    args = parser.parse_args()
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    places = json.loads(PLACES_FILE.read_text(encoding="utf-8"))
    if args.only:
        places = [p for p in places if p["slug"] == args.only]

    api = Api()
    results = []
    for place in places:
        print(f"[commons] {place['slug']}", file=sys.stderr)
        results.append(harvest_commons(api, place))

    if not args.skip_openverse:
        for res in results:
            if len(res["candidates"]) < OPENVERSE_THRESHOLD:
                print(f"[openverse] {res['place']['slug']}", file=sys.stderr)
                harvest_openverse(api, res["place"], res)

    with_rows = write_outputs(results, api)
    print(f"places with >=1 candidate: {with_rows}/{len(results)}; "
          f"http requests: {api.http_requests}; cache hits: {api.cache_hits}; "
          f"failures: {len(api.failures)}", file=sys.stderr)


if __name__ == "__main__":
    main()
