#!/usr/bin/env python3
"""Targeted gap harvest for DIY places with too few photos (KELANA galeri).

Reuses the API gate, license and geo helpers of commons_harvest.py (stdlib only,
1 request/second, maxlag=5, KelanaDataBot User-Agent). For each gap place it tries
Commons categories (files + one level of subcategories), namespace-6 searches with
name variants, and the id/en Wikipedia article image lists. Candidates already in
candidates.jsonl are skipped. Output: docs/data/photos/candidates_extra.jsonl.

Usage: python scripts/media/commons_extra.py [--cache DIR] [--only SLUG]
"""
import argparse
import json
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import commons_harvest as H  # noqa: E402

OUT = H.OUT_DIR / "candidates_extra.jsonl"

# slug -> (categories, search terms, wikipedia titles, max km for GPS,
#          tokens required in title/description/categories when there is no GPS)
PLACES = {
    "kawasan-kotagede": (
        ["Kotagede", "Kotagede, Yogyakarta", "Pasar Kotagede", "Masjid Gedhe Mataram Kotagede",
         "Makam Raja-raja Mataram Kotagede", "Rumah Kalang Kotagede", "Kotagede Yogyakarta"],
        ["Kotagede", "Kota Gede Yogyakarta", "Kotagedhe", "Masjid Agung Mataram", "Pasar Gede Kotagede",
         "Living Museum Kotagede"],
        ["Kotagede", "Kotagede, Yogyakarta"], 3, ["kotagede", "kota gede", "kotagedhe"]),
    "museum-puro-pakualaman": (
        ["Pura Pakualaman", "Puro Pakualaman", "Pakualaman Palace", "Pakualaman"],
        ["Pura Pakualaman", "Puro Pakualaman", "Pakualaman Palace", "Bangsal Sewatama",
         "Kadipaten Pakualaman", "Museum Pura Pakualaman", "Pakualaman Yogyakarta"],
        ["Pakualaman", "Pura Pakualaman"], 3, ["pakualaman"]),
    "plengkung-gading": (
        ["Plengkung Gading", "Plengkung Nirbaya", "Benteng Baluwarti", "Plengkung Wijilan",
         "Plengkung Tarunasura", "Alun-alun Kidul"],
        ["Plengkung Gading", "Plengkung Nirbaya", "Gading gate Yogyakarta", "Baluwarti Yogyakarta",
         "Plengkung Wijilan", "Plengkung Tarunasura", "Alun-alun Kidul Yogyakarta"],
        ["Plengkung Gading", "Plengkung Nirbaya"], 2, ["plengkung", "baluwarti", "nirbaya"]),
    "kampung-ketandan": (
        ["Kampung Ketandan", "Pecinan Ketandan", "Ketandan", "Chinatown, Yogyakarta"],
        ["Kampung Ketandan", "Pecinan Ketandan", "Ketandan Malioboro", "Imlek Ketandan",
         "Pecinan Yogyakarta", "Gapura Ketandan", "Jalan Pajeksan"],
        ["Ketandan", "Kampung Ketandan"], 2, ["ketandan", "pecinan", "pajeksan"]),
    "taman-budaya-yogyakarta": (
        ["Taman Budaya Yogyakarta", "Taman Budaya Yogyakarta Concert Hall"],
        ["Taman Budaya Yogyakarta", "Taman Budaya Yogyakarta Cultural Park", "Societeit Militair Yogyakarta",
         "Concert Hall TBY", "Gedung Societet", "Jalan Sriwedani"],
        ["Taman Budaya Yogyakarta"], 2, ["taman budaya yogyakarta", "societeit", "societet", "sriwedani",
                                          "concert hall", "taman budaya, yogyakarta"]),
    "sekar-rinonce": (
        ["Sekar Rinonce"],
        ["Sekar Rinonce", "Sekar Rinonce Yogyakarta", "Sekar Rinonce Jogja"],
        [], 2, ["sekar rinonce"]),
    "desa-wisata-widosari": (
        ["Widosari", "Samigaluh", "Ngargosari"],
        ["Widosari", "Desa Wisata Widosari", "Widosari Kulon Progo", "Widosari Samigaluh",
         "Ngargosari Samigaluh", "Samigaluh Kulon Progo"],
        ["Samigaluh"], 4, ["widosari", "samigaluh", "ngargosari"]),
    "desa-wisata-krebet": (
        ["Krebet", "Pajangan", "Pajangan, Bantul", "Batik kayu"],
        ["Krebet", "Krebet Bantul", "Krebet Pajangan", "Batik kayu Krebet", "Topeng kayu Krebet",
         "Desa Wisata Krebet", "Pajangan Bantul", "Sendangsari Pajangan"],
        ["Pajangan, Bantul"], 4, ["krebet", "pajangan", "sendangsari"]),
    "desa-wisata-tlatar": (
        ["Tlatar", "Turi, Sleman", "Wonokerto, Turi"],
        ["Tlatar", "Tlatar Turi Sleman", "Umbul Tlatar", "Wonokerto Turi", "Desa Wisata Tlatar",
         "Tlatar Kandangan Turi"],
        ["Turi, Sleman"], 4, ["tlatar", "wonokerto", "turi"]),
    "desa-wisata-kasongan": (
        ["Kasongan", "Kasongan, Bantul", "Kasongan (tourist attraction)", "Gerabah Kasongan",
         "Pottery in Yogyakarta"],
        ["Kasongan pottery", "Kasongan Bantul", "Gerabah Kasongan", "Kasongan Yogyakarta",
         "Desa Wisata Kasongan", "Kasihan Bantul gerabah", "Kajen Bangunjiwo", "Kasongan"],
        ["Kasongan", "Kasongan, Bantul"], 4, ["kasongan", "bangunjiwo", "kasihan"]),
    "makam-raja-imogiri": (
        ["Imogiri Royal Cemetery", "Makam Raja-raja Imogiri", "Imogiri", "Pajimatan",
         "Imogiri, Bantul", "Imogiri Royal Mausoleum"],
        ["Imogiri", "Pajimatan", "Pemakaman Imogiri", "Makam Sultan Agung", "Makam Raja Imogiri",
         "Imogiri royal cemetery", "Kompleks Makam Imogiri", "Imogiri Bantul"],
        ["Makam Raja-raja Imogiri", "Imogiri"], 3, ["imogiri", "pajimatan"]),
    "workshop-perak-kotagede": (
        ["Silver of Kotagede", "Kotagede silver", "Silver in Indonesia", "Silversmiths in Indonesia"],
        ["Kotagede silver", "Kerajinan perak Kotagede", "Perak Kotagede", "silversmith Kotagede",
         "Kotagede silver workshop", "Kotagede perak Yogyakarta", "Kotagede silverware"],
        ["Kotagede"], 3, ["kotagede", "kota gede"]),
}
# Namesake guards: reject if any of these appear (other regions).
BAD = ["katingan", "kalimantan", "boyolali", "klaten", "magelang", "pulosari"]


def text_of(c):
    return " ".join([c["file_title"] or "", c["description"] or "", " ".join(c["categories"])]).lower()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--cache")
    ap.add_argument("--only")
    args = ap.parse_args()
    if args.cache:
        H.CACHE_DIR = Path(args.cache)
    sys.stdout.reconfigure(encoding="utf-8")
    places = {p["slug"]: p for p in json.loads(H.PLACES_FILE.read_text(encoding="utf-8"))}
    known = set()
    for line in (H.OUT_DIR / "candidates.jsonl").read_text(encoding="utf-8").splitlines():
        r = json.loads(line)
        known.add((r["slug"], r["sha1"] or r["original_url"]))
    api = H.Api()
    rows = []
    for slug, (cats, terms, wps, maxkm, tokens) in PLACES.items():
        if args.only and slug != args.only:
            continue
        place = places[slug]
        counts = Counter()
        titles, seen = [], set()

        def add(t):
            if t not in seen:
                seen.add(t)
                titles.append(t)
        for cat in cats:
            data = api.call(H.COMMONS_API, {"action": "query", "list": "categorymembers",
                            "cmtitle": "Category:" + cat, "cmtype": "file|subcat", "cmlimit": "100"},
                            f"cat {cat}", "mw") or {}
            subs = []
            for m in data.get("query", {}).get("categorymembers", []):
                if m["title"].startswith("Category:"):
                    subs.append(m["title"])
                else:
                    add(m["title"])
            for sub in subs[:8]:
                d2 = api.call(H.COMMONS_API, {"action": "query", "list": "categorymembers",
                              "cmtitle": sub, "cmtype": "file", "cmlimit": "50"}, f"subcat {sub}", "mw") or {}
                for m in d2.get("query", {}).get("categorymembers", []):
                    add(m["title"])
        for term in terms:
            data = api.call(H.COMMONS_API, {"action": "query", "list": "search", "srsearch": term,
                            "srnamespace": "6", "srlimit": "50"}, f"search {term}", "mw") or {}
            for hit in data.get("query", {}).get("search", []):
                add(hit["title"])
        for lang in ("id", "en"):
            for wp in wps:
                data = api.call(f"https://{lang}.wikipedia.org/w/api.php", {
                    "action": "query", "titles": wp, "prop": "images", "imlimit": "max"},
                    f"wp {lang} {wp}", "mw") or {}
                for pg in data.get("query", {}).get("pages", []):
                    for im in pg.get("images", []):
                        add(im["title"])
        counts["discovered"] = len(titles)
        pages = H.fetch_commons_pages(api, titles, slug, with_meta=True)
        kept, shas = [], set()
        for t in titles:
            pg = pages.get(t)
            if pg is None:
                counts["missing page"] += 1
                continue
            c = H.commons_candidate(slug, pg, {}, counts)
            if c is None:
                continue
            if (slug, c["sha1"]) in known:
                counts["already in candidates.jsonl"] += 1
                continue
            if c["sha1"] in shas:
                counts["duplicate sha1"] += 1
                continue
            txt = text_of(c)
            if any(b in txt for b in BAD):
                counts["namesake keyword"] += 1
                continue
            g = c["gps"]
            if g:
                km = H.haversine_km(g["lat"], g["lon"], place["lat"], place["lng"])
                if km > maxkm:
                    counts[f"GPS > {maxkm} km"] += 1
                    continue
            elif not any(tok in txt for tok in tokens):
                counts["no GPS and no place token"] += 1
                continue
            shas.add(c["sha1"])
            kept.append(c)
        thumbs = H.fetch_commons_pages(api, [c["file_title"] for c in kept], slug + " 400", with_meta=False)
        for c in kept:
            ii = (thumbs.get(c["file_title"], {}).get("imageinfo") or [{}])[0]
            c["url_400"] = ii.get("thumburl")
            H.finalize_license(c)
            rows.append(c)
        ok = sum(1 for c in kept if c["license_ok"])
        print(f"{slug}: discovered {counts['discovered']}, kept {len(kept)}, license_ok {ok}, "
              f"dropped {dict((k, v) for k, v in counts.items() if k != 'discovered')}")
    with OUT.open("w", encoding="utf-8", newline="\n") as fh:
        for c in rows:
            fh.write(json.dumps({k: c.get(k) for k in H.CANDIDATE_FIELDS}, ensure_ascii=False) + "\n")
    print("requests", api.http_requests, "failures", api.failures)


if __name__ == "__main__":
    main()
