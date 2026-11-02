#!/usr/bin/env python3
"""محاكي Apple Watch لالتقاط الصور: أحدث نظام watchOS مثبّت، وأول طراز يدعمه من القائمة المفضّلة
(Ultra 2 أولاً: مقاس 410×502 يعرفه deliver)، وإلا آخر ساعة يدعمها. يطبع «<runtime> <device type>».

  xcrun simctl list runtimes -j | pick_watch.py
"""
import json
import sys

PREFERRED = [
    "Apple Watch Ultra 2 (49mm)", "Apple Watch Ultra 3 (49mm)", "Apple Watch Series 9 (45mm)",
    "Apple Watch Series 11 (46mm)", "Apple Watch Series 10 (46mm)",
]

runtimes = [r for r in json.load(sys.stdin)["runtimes"] if r.get("isAvailable") and "watchOS" in r.get("name", "")]
runtimes.sort(key=lambda r: [int(x) for x in r["version"].split(".")])
for r in reversed(runtimes):
    supported = r.get("supportedDeviceTypes") or []
    names = {t["name"]: t["identifier"] for t in supported}
    for name in PREFERRED:
        if name in names:
            print(r["identifier"], names[name])
            sys.exit(0)
    watches = [t for t in supported if t["name"].startswith("Apple Watch")]
    if watches:
        print(r["identifier"], watches[-1]["identifier"])
        sys.exit(0)
sys.exit("no watchOS simulator runtime with a supported Apple Watch")
