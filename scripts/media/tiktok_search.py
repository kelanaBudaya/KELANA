#!/usr/bin/env python3
"""T1 for the TikTok gallery: Firecrawl cloud search for candidate videos per place.

Modes:
  credits  print remaining credits (team credit-usage)
  test     one sample search, print the response shape and credits consumed
  run      T1 for all places in docs/data/diy_places.json -> tiktok_candidates.jsonl

Stdlib only. The API key is read from FIRECRAWL_API_KEY in the project .env file
and is sent only in the Authorization header. It is never printed or written.
"""
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

ENV_PATH = "D:/project/budayago/.env"
API = "https://api.firecrawl.dev"
UA = "KelanaDataBot/0.1"
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
PLACES = os.path.join(ROOT, "docs", "data", "diy_places.json")
CANDIDATES = os.path.join(ROOT, "docs", "data", "videos", "tiktok_candidates.jsonl")
CREDIT_CAP = 120
MIN_VIDEO_HITS = 3
PAUSE = 1.0
VIDEO_RE = re.compile(r"/video/(\d+)")

_KEY = None


def load_key():
    global _KEY
    try:
        with open(ENV_PATH, encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line.startswith("FIRECRAWL_API_KEY="):
                    _KEY = line.split("=", 1)[1].strip().strip('"').strip("'")
                    break
    except OSError:
        raise SystemExit("cannot read .env")
    if not _KEY:
        raise SystemExit("FIRECRAWL_API_KEY missing in .env")


def scrub(text):
    s = str(text)
    if _KEY:
        s = s.replace(_KEY, "***")
    return s


def api(method, path, body=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(
        API + path,
        data=data,
        method=method,
        headers={
            "Authorization": "Bearer " + _KEY,
            "Content-Type": "application/json",
            "User-Agent": UA,
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=90) as r:
            return json.loads(r.read().decode("utf-8") or "{}")
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", "replace")[:300]
        raise RuntimeError("HTTP %d on %s: %s" % (e.code, path, scrub(detail)))
    except Exception as e:  # scrub anything that could carry the key
        raise RuntimeError("%s on %s: %s" % (type(e).__name__, path, scrub(e)))


def remaining_credits():
    j = api("GET", "/v2/team/credit-usage")
    d = j.get("data", j)
    return d.get("remainingCredits")


def search(query):
    return api("POST", "/v2/search", {"query": query, "limit": 10, "sources": ["web"]})


def web_items(j):
    """Accept {"data": {"web": [...]}}, {"data": [...]} or {"web": [...]}."""
    d = j.get("data", j)
    if isinstance(d, dict):
        items = d.get("web") or []
    elif isinstance(d, list):
        items = d
    else:
        items = j.get("web") or []
    return [x for x in items if isinstance(x, dict)]


def is_tiktok(url):
    host = urllib.parse.urlparse(url).hostname or ""
    return host == "tiktok.com" or host.endswith(".tiktok.com")


def mode_credits():
    d = api("GET", "/v2/team/credit-usage").get("data", {})
    print("credit-usage fields:", sorted(d.keys()) if isinstance(d, dict) else type(d).__name__)
    print("remainingCredits:", d.get("remainingCredits"))


def mode_test():
    before = remaining_credits()
    j = search("museum sonobudoyo jogja site:tiktok.com")
    after = remaining_credits()
    print("top-level keys:", sorted(j.keys()))
    if isinstance(j.get("data"), dict):
        print("data keys:", sorted(j["data"].keys()))
    items = web_items(j)
    print("web items:", len(items))
    if items:
        print("item keys:", sorted(items[0].keys()))
    for it in items:
        print("  -", it.get("url"), "| tiktok:", is_tiktok(it.get("url", "")))
    print("credits before:", before, "after:", after, "consumed:", (before or 0) - (after or 0))


def mode_run():
    with open(PLACES, encoding="utf-8") as f:
        places = json.load(f)
    start = remaining_credits()
    print("credits remaining at start:", start)
    if start is None or start < 150:
        raise SystemExit("remaining credits %s < 150, stop" % start)

    searches = 0
    aborted = False
    total_rows = 0
    os.makedirs(os.path.dirname(CANDIDATES), exist_ok=True)
    with open(CANDIDATES, "w", encoding="utf-8") as out:
        for p in places:
            slug, name = p["slug"], p["name"]
            queries = [
                "%s jogja site:tiktok.com" % name,
                "%s yogyakarta tiktok" % name,
            ]
            seen = set()
            video_urls = set()
            rows = []
            for qi, q in enumerate(queries):
                if qi == 1 and len(video_urls) >= MIN_VIDEO_HITS:
                    break
                consumed = start - (remaining_credits() or 0)
                if consumed >= CREDIT_CAP:
                    aborted = True
                    break
                try:
                    j = search(q)
                except RuntimeError as e:
                    print("ERROR", slug, "query", qi + 1, scrub(e))
                    time.sleep(PAUSE)
                    continue
                searches += 1
                for it in web_items(j):
                    url = it.get("url") or ""
                    if not is_tiktok(url) or url in seen:
                        continue
                    seen.add(url)
                    if VIDEO_RE.search(url):
                        video_urls.add(url)
                    rows.append({
                        "slug": slug,
                        "query": q,
                        "url": url,
                        "title": it.get("title"),
                        "description": it.get("description"),
                    })
                time.sleep(PAUSE)
            for r in rows:
                out.write(json.dumps(r, ensure_ascii=False) + "\n")
            out.flush()
            total_rows += len(rows)
            print("%-28s tiktok_results=%2d video_urls=%2d" % (slug, len(rows), len(video_urls)))
            if aborted:
                break

    end = remaining_credits()
    print("searches:", searches, "rows:", total_rows, "aborted:", aborted)
    print("credits remaining at end:", end, "consumed:", (start or 0) - (end or 0))


def main():
    load_key()
    modes = {"credits": mode_credits, "test": mode_test, "run": mode_run}
    if len(sys.argv) != 2 or sys.argv[1] not in modes:
        raise SystemExit("usage: tiktok_search.py credits|test|run")
    try:
        modes[sys.argv[1]]()
    except RuntimeError as e:
        raise SystemExit("ERROR: " + scrub(e))


if __name__ == "__main__":
    main()
