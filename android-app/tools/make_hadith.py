#!/usr/bin/env python3
"""
Builds the "hadith of the day" data from the HadeethEnc.com encyclopedia
(https://hadeethenc.com), one file per language, into shared/src/commonMain/assets/hadith.

The text, explanation, benefits, grade and takhrij are copied verbatim, as the
publisher's terms require ("no modification, addition, or deletion of the content");
the app shows the source and a link to each hadith on HadeethEnc.com.

Usage:
    python3 make_hadith.py <dir with {lang}.csv>      # CSVs already downloaded
    python3 make_hadith.py --download <cache dir>     # fetch the CSVs first

Selection (366, one per day of a year, in a fixed shuffled order):
  - present with an explanation in every widely translated language
    (ar en fr tr ur id fa bn ru es), and graded authentic or good (no mawquf);
  - a short title, so it fits in a widget;
  - preferring hadiths that German, Ukrainian, Uzbek and Malay also have,
    then the shorter ones.
Languages without a translation of a hadith fall back at run time (see Hadith.kt).
"""
import csv, json, os, random, sys, urllib.request

csv.field_size_limit(10 ** 8)

LANGS = ["ar", "en", "fr", "tr", "ur", "id", "fa", "bn", "ru", "es", "de", "uk", "uz", "ms"]
CORE = ["ar", "en", "fr", "tr", "ur", "id", "fa", "bn", "ru", "es"]
SMALL = ["de", "uk", "uz", "ms"]
COUNT = 366
TITLE_MAX = 120
URL = "https://hadeethenc.com/{lang}/browse/download/{lang}/csv"

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "shared", "src", "commonMain", "assets", "hadith")


def load(path):
    rows = list(csv.reader(open(path, encoding="utf-8-sig")))
    head = rows[0][0]
    version = next((l.split("(")[-1].rstrip(")") for l in head.splitlines() if "Last update" in l), "")
    return version, {r["id"]: r for r in (dict(zip(rows[1], x)) for x in rows[2:])}


def lines(s):
    return [l.strip() for l in s.replace("\r", "").split("\n") if l.strip()]


def paragraphs(s):
    return "\n\n".join(lines(s))


def bracket(s):
    s = s.strip()
    return s[1:-1].strip() if s.startswith("[") and s.endswith("]") else s


def main():
    args = sys.argv[1:]
    if args and args[0] == "--download":
        src = args[1]
        os.makedirs(src, exist_ok=True)
        for l in LANGS:
            with urllib.request.urlopen(URL.format(lang=l)) as r, open(os.path.join(src, f"{l}.csv"), "wb") as f:
                f.write(r.read())
    else:
        src = args[0]

    data, versions = {}, {}
    for l in LANGS:
        versions[l], data[l] = load(os.path.join(src, f"{l}.csv"))

    ar = data["ar"]

    def has(i, l):
        x = data[l].get(i)
        if not x:
            return False
        if l == "ar":
            return bool(x["explanation"].strip() and x["hadith_text"].strip())
        return bool(x["explanation"].strip() and x["hadith_text"].strip() and x["title"].strip())

    cand = [
        i for i in ar
        if all(has(i, l) for l in CORE)
        and len(ar[i]["title"].strip()) <= TITLE_MAX
        and "موقوف" not in ar[i]["grade"]
    ]
    cand.sort(key=lambda i: (-sum(has(i, l) for l in SMALL), len(ar[i]["hadith_text"]), int(i)))
    chosen = cand[:COUNT]
    random.Random(1446).shuffle(chosen)

    os.makedirs(OUT, exist_ok=True)
    for l in LANGS:
        items = {}
        for i in chosen:
            if not has(i, l):
                continue
            x = data[l][i]
            items[i] = {
                "title": x["title"].strip(),
                "text": paragraphs(x["hadith_text"]),
                "expl": paragraphs(x["explanation"]),
                "benefits": lines(x["benefits"]),
                "grade": bracket(x["grade"]),
                "takhrij": bracket(x["takhrij"]),
            }
        doc = {"source": f"https://hadeethenc.com/{l}", "version": versions[l], "items": items}
        if l == "ar":
            doc["order"] = chosen
        with open(os.path.join(OUT, f"{l}.json"), "w", encoding="utf-8") as f:
            json.dump(doc, f, ensure_ascii=False, separators=(",", ":"))
        print(l, len(items), os.path.getsize(os.path.join(OUT, f"{l}.json")) // 1024, "KB")


if __name__ == "__main__":
    main()
