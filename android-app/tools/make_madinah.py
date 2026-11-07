#!/usr/bin/env python3
"""
Builds the Madinah Mushaf page data (King Fahd Complex print, 1421 AH, 15 lines, QPC V2 glyphs)
into shared/src/commonMain/assets/madinah/p{N}.json, one small file per page.

Sources:
  - line layout (line type, centred lines, surah of each header) from the QUL layout database
    qpc-v2-15-lines.db (https://qul.tarteel.ai/resources/mushaf-layout), passed as the first argument;
  - each word's QPC V2 glyph code, its line and its ayah from the Quran.com API
    (api.quran.com/api/v4/verses/by_page), cached in the second argument.

The glyphs are drawn at run time with the page's own KFGQPC font (p{N}.ttf, downloaded on demand
from the QUL CDN), so the page is exactly the printed one. Fonts and glyph codes are not modified.

Page file: {"l": [line, ...]} with 15 entries (missing lines are null):
  ["s", surah]                      surah header
  ["b"]                             basmala
  ["a", centred, glyphs, ayahs, ends]
      glyphs: the line's word glyphs (a glyph code can hold a space)
      ayahs:  for each glyph, the ayah's index in the whole Quran (0..6235)
      ends:   positions of the ayah-end markers in the line

Usage: python3 make_madinah.py qpc-v2-15-lines.db <cache dir>
"""
import concurrent.futures as cf, json, os, sqlite3, sys, time, urllib.request

API = "https://api.quran.com/api/v4/verses/by_page/{p}?words=true&word_fields=code_v2,line_number,page_number&per_page=60"
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "shared", "src", "commonMain", "assets", "madinah")
# ayahs per surah (to turn "s:a" into a global index)
COUNTS = [7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,13,14,11,11,18,12,12,30,52,52,44,28,28,20,56,40,31,50,40,46,42,29,19,36,25,22,17,19,26,30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6]
START = [sum(COUNTS[:i]) for i in range(114)]


def fetch(p, cache):
    path = os.path.join(cache, f"p{p}.json")
    if os.path.exists(path):
        return json.load(open(path))
    for attempt in range(5):
        try:
            req = urllib.request.Request(API.format(p=p), headers={"User-Agent": "mizan-build/1.0", "Accept": "application/json"})
            with urllib.request.urlopen(req, timeout=60) as r:
                data = json.loads(r.read())
            json.dump(data, open(path, "w"))
            return data
        except Exception:
            time.sleep(2 ** attempt)
    raise RuntimeError(f"page {p}")


def main():
    db, cache = sys.argv[1], sys.argv[2]
    os.makedirs(cache, exist_ok=True)
    os.makedirs(OUT, exist_ok=True)
    con = sqlite3.connect(db)
    layout = {}
    for p, ln, kind, centred, surah in con.execute("select page_number, line_number, line_type, is_centered, surah_number from pages"):
        layout.setdefault(p, {})[ln] = (kind, int(centred or 0), surah)

    with cf.ThreadPoolExecutor(8) as ex:
        pages = dict(zip(range(1, 605), ex.map(lambda p: fetch(p, cache), range(1, 605))))

    # a verse that runs onto the next page is listed under one of the pages only: group the words by their own page
    by_page, seen = {}, set()
    for p in range(1, 605):
        for v in pages[p]["verses"]:
            s, a = map(int, v["verse_key"].split(":"))
            idx = START[s - 1] + a - 1
            for w in v["words"]:
                key = (v["verse_key"], w["position"])
                if key in seen:
                    continue
                seen.add(key)
                by_page.setdefault(w["page_number"], {}).setdefault(w["line_number"], []).append((w["position"], idx, w["code_v2"], w["char_type_name"] == "end"))

    total_words = 0
    for p in range(1, 605):
        words = {ln: [(g, i, e) for _, i, g, e in sorted(ws, key=lambda t: (t[1], t[0]))] for ln, ws in by_page.get(p, {}).items()}
        lines = []
        for ln in range(1, 16):
            kind, centred, surah = layout[p].get(ln, (None, 0, None))
            if kind == "surah_name":
                lines.append(["s", int(surah)])
            elif kind == "basmallah":
                lines.append(["b"])
            elif kind == "ayah":
                ws = words.get(ln)
                assert ws, (p, ln)
                total_words += len(ws)
                lines.append(["a", centred, [g for g, _, _ in ws], [i for _, i, _ in ws], [k for k, (_, _, e) in enumerate(ws) if e]])
            else:
                assert ln not in words, (p, ln)
                lines.append(None)
        assert sum(len(v) for v in words.values()) == sum(len(l[3]) for l in lines if l and l[0] == "a"), p
        with open(os.path.join(OUT, f"p{p}.json"), "w", encoding="utf-8") as f:
            json.dump({"l": lines}, f, ensure_ascii=False, separators=(",", ":"))
    print("pages", 604, "words", total_words)


if __name__ == "__main__":
    main()
