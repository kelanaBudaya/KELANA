#!/usr/bin/env python3
"""T2 for the TikTok gallery: validate candidate videos with the TikTok oEmbed endpoint.

Reads docs/data/videos/tiktok_candidates.jsonl, keeps URLs matching /video/(\\d+),
calls https://www.tiktok.com/oembed?url=<url> one at a time (2 s pause), and writes
docs/data/videos/tiktok_validated.jsonl for responses that are HTTP 200 with a title
and author. No other tiktok.com page is fetched.

Stdlib only. Stops early after 5 consecutive 403/429 responses.
"""
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone

UA = "KelanaDataBot/0.1"
ENDPOINT = "https://www.tiktok.com/oembed?url="
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
CAND = os.path.join(ROOT, "docs", "data", "videos", "tiktok_candidates.jsonl")
OUT = os.path.join(ROOT, "docs", "data", "videos", "tiktok_validated.jsonl")
VIDEO_RE = re.compile(r"/video/(\d+)")
PAUSE = 2.0
BLOCK_LIMIT = 5


def read_candidates():
    rows, seen = [], set()
    with open(CAND, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            r = json.loads(line)
            m = VIDEO_RE.search(r.get("url", ""))
            if not m:
                continue
            key = (r["slug"], m.group(1))
            if key in seen:
                continue
            seen.add(key)
            rows.append(r)
    return rows


def oembed(url):
    req = urllib.request.Request(ENDPOINT + urllib.parse.quote(url, safe=""),
                                 headers={"User-Agent": UA})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, None
    except Exception as e:
        return None, type(e).__name__


def main():
    cands = read_candidates()
    print("candidate video URLs to check:", len(cands))
    status_counts = {}
    validated = []
    blocked_run = 0
    aborted = False
    now = datetime.now(timezone.utc).isoformat(timespec="seconds")
    for r in cands:
        vid = VIDEO_RE.search(r["url"]).group(1)
        status, body = oembed(r["url"])
        key = str(status) if status is not None else "error:%s" % body
        status_counts[key] = status_counts.get(key, 0) + 1
        if status in (403, 429):
            blocked_run += 1
            if blocked_run >= BLOCK_LIMIT:
                print("stop: %d consecutive %s responses" % (BLOCK_LIMIT, status))
                aborted = True
                break
        else:
            blocked_run = 0
        ok = False
        if status == 200 and body:
            try:
                j = json.loads(body.decode("utf-8"))
            except ValueError:
                j = {}
            title = (j.get("title") or "").strip()
            author = (j.get("author_name") or j.get("author_unique_id") or "").strip()
            if title and author:
                ok = True
                aid = (j.get("author_unique_id") or "").strip()
                canonical = ("https://www.tiktok.com/@%s/video/%s" % (aid, vid)
                             if aid else r["url"])
                validated.append({
                    "slug": r["slug"],
                    "id": vid,
                    "canonical_url": canonical,
                    "author": author,
                    "title": title,
                    "upload_date": datetime.fromtimestamp(int(vid) >> 32, tz=timezone.utc)
                                          .date().isoformat(),
                    "oembed_ok_at": now,
                })
        print("%-28s id=%s status=%s pass=%s" % (r["slug"], vid, status, ok))
        time.sleep(PAUSE)

    with open(OUT, "w", encoding="utf-8") as f:
        for v in validated:
            f.write(json.dumps(v, ensure_ascii=False) + "\n")
    print("status counts:", status_counts)
    print("validated:", len(validated), "aborted:", aborted)


if __name__ == "__main__":
    main()
