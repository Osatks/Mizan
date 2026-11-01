#!/usr/bin/env python3
"""يتحقق من حدود App Store لكل نصوص fastlane/metadata قبل الرفع (يستعمله ios-release.yml).

python3 iosApp/fastlane/check_metadata.py
"""
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent / "metadata"
LIMITS = {
    "name.txt": (2, 30),
    "subtitle.txt": (0, 30),
    "promotional_text.txt": (0, 170),
    "description.txt": (10, 4000),
    "keywords.txt": (0, 100),
    "release_notes.txt": (0, 4000),
}
REQUIRED = ["name.txt", "description.txt", "keywords.txt", "support_url.txt", "privacy_url.txt"]
# ما يقبله App Store Connect (FastlaneCore::Languages::ALL_LANGUAGES)
LOCALES = set("""ar-SA bn-BD ca cs da de-DE el en-AU en-CA en-GB en-US es-ES es-MX fi fr-CA fr-FR gu-IN he hi hr hu
id it ja kn-IN ko ml-IN mr-IN ms nl-NL no or-IN pa-IN pl pt-BR pt-PT ro ru sk sl-SI sv ta-IN te-IN th tr uk ur-PK vi
zh-Hans zh-Hant""".split())

errors = []
report = []
for d in sorted(p for p in ROOT.iterdir() if p.is_dir() and p.name != "review_information"):
    if d.name not in LOCALES:
        errors.append(f"{d.name}: not an App Store locale")
    for req in REQUIRED:
        if not (d / req).exists():
            errors.append(f"{d.name}/{req}: missing")
    for f in sorted(d.glob("*.txt")):
        text = f.read_text(encoding="utf-8").strip()
        if f.name in LIMITS:
            lo, hi = LIMITS[f.name]
            n = len(text)
            report.append(f"{d.name:6} {f.name:22} {n:5}/{hi}")
            if not lo <= n <= hi:
                errors.append(f"{d.name}/{f.name}: {n} chars (limit {lo}..{hi})")
        if f.name == "keywords.txt":
            parts = text.split(",")
            if any(p != p.strip() or not p for p in parts):
                errors.append(f"{d.name}/keywords.txt: no spaces around commas, no empty keywords")
            low = [p.lower() for p in parts]
            if len(set(low)) != len(low):
                errors.append(f"{d.name}/keywords.txt: duplicate keyword")
        if f.name.endswith("_url.txt") and not text.startswith("https://"):
            errors.append(f"{d.name}/{f.name}: must be an https URL")
        if "\t" in text or "<" in text:
            errors.append(f"{d.name}/{f.name}: tabs or HTML are not allowed")

notes = ROOT / "review_information" / "notes.txt"
if notes.exists() and len(notes.read_text(encoding="utf-8").strip()) > 4000:
    errors.append("review_information/notes.txt: over 4000 chars")
for f in ["copyright.txt", "primary_category.txt"]:
    if not (ROOT / f).exists():
        errors.append(f"{f}: missing")

print("\n".join(report))
if errors:
    print("\nERRORS:\n" + "\n".join(errors))
    sys.exit(1)
print(f"\nOK: {len(report)} fields checked")
