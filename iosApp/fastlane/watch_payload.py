#!/usr/bin/env python3
"""ما يرسله iPhone إلى ميزان على Apple Watch (WatchSync.swift)، من حالات Kotlin (live-fixtures.json من LiveStatesTest):
لقطة المواقيت والأذكار (نصوصها العربية وعددها ومصدرها فقط، بلا آية اليوم) وأوقاتها منقولة إلى الآن، ونصوص المسبحة.
يُطبع وسيطَ تشغيل للمحاكي: بيانات plist «<hex>» تقرؤها الساعة من UserDefaults (watch.json) كما تقرأ ما يصلها من iPhone
بعد فكّ ضغطه.

  watch_payload.py <live-fixtures.json> <lang>
"""
import json
import sys
import time


def payload(fixtures: dict, lang: str) -> bytes:
    fx = fixtures.get(lang) or fixtures["en"]
    snap = fx["snapshot"]
    d = time.time() - snap["at"]
    snap["at"] += d
    for day in snap["days"]:
        day["start"] += d
        day["ayah"] = None
        for t in day["times"]:
            t["at"] += d
    for w in snap["windows"]:
        w["s"] += d
        w["e"] += d
    snap["items"] = {k: {"ar": v["ar"], "n": v["n"], "c": v.get("c"), "src": v.get("src", "")} for k, v in snap["items"].items()}
    t = fx["tasbih"]
    goal = str(t["goal"])
    of = t["goalText"].replace(goal, "%d") if t["goal"] > 0 and goal in t["goalText"] else None
    out = {
        "snapshot": snap,
        "tasbih": {"title": t["title"], "reset": t["reset"], "of": of, "today": t["todayLabel"], "rounds": t["roundsLabel"]},
    }
    return json.dumps(out, ensure_ascii=False).encode("utf-8")


if __name__ == "__main__":
    data = payload(json.load(open(sys.argv[1], encoding="utf-8")), sys.argv[2])
    sys.stdout.write("<" + data.hex() + ">")
