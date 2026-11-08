#!/usr/bin/env python3
"""يرفع صور الهاتف في متجر Google Play (Android Publisher API v3) من google-play/screenshots/<لغة>/phone/*.png:
يحذف صور الهاتف القديمة لكل لغة موجودة هنا ثم يرفع الجديدة بترتيب أسمائها، ويثبّت التعديل (edit) مرة واحدة.
اللغات الأخرى في المتجر لا تُمس. لا يطبع أي سر.

    pip install google-auth requests
    PLAY_SERVICE_ACCOUNT_JSON='<json>' python3 google-play/upload_listing_images.py com.mizan.almuslim google-play/screenshots
"""
import json
import os
import sys
from pathlib import Path

import requests
from google.auth.transport.requests import Request
from google.oauth2 import service_account

API = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{pkg}"
UPLOAD = "https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/{pkg}"
# اسم المجلد ← رمز اللغة في Play
LANGS = {"ar": "ar", "en": "en-US", "en-US": "en-US"}


def main():
    pkg, root = sys.argv[1], Path(sys.argv[2])
    info = json.loads(os.environ["PLAY_SERVICE_ACCOUNT_JSON"])
    creds = service_account.Credentials.from_service_account_info(info, scopes=["https://www.googleapis.com/auth/androidpublisher"])
    creds.refresh(Request())
    h = {"Authorization": f"Bearer {creds.token}"}
    base, up = API.format(pkg=pkg), UPLOAD.format(pkg=pkg)

    r = requests.post(f"{base}/edits", headers=h, json={})
    r.raise_for_status()
    edit = r.json()["id"]
    try:
        for d in sorted(p for p in root.iterdir() if p.is_dir()):
            lang = LANGS.get(d.name, d.name)
            files = sorted((d / "phone").glob("*.png"))
            if not files:
                continue
            r = requests.delete(f"{base}/edits/{edit}/listings/{lang}/phoneScreenshots", headers=h)
            if r.status_code not in (200, 204):
                sys.exit(f"{lang}: delete failed HTTP {r.status_code}: {r.text[:300]}")
            for f in files:
                r = requests.post(f"{up}/edits/{edit}/listings/{lang}/phoneScreenshots", params={"uploadType": "media"},
                                  headers={**h, "Content-Type": "image/png"}, data=f.read_bytes())
                if r.status_code != 200:
                    sys.exit(f"{lang}/{f.name}: upload failed HTTP {r.status_code}: {r.text[:300]}")
                print(f"{lang}: {f.name}")
        r = requests.post(f"{base}/edits/{edit}:commit", headers=h)
        if r.status_code != 200:
            sys.exit(f"commit failed HTTP {r.status_code}: {r.text[:400]}")
        print("Committed: the store listing screenshots are updated (Google reviews listing changes).")
    except BaseException:
        requests.delete(f"{base}/edits/{edit}", headers=h)
        raise


if __name__ == "__main__":
    main()
