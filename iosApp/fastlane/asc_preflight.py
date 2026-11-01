#!/usr/bin/env python3
"""فحص مفتاح App Store Connect API قبل أي بناء على macOS (أول مرحلة في ios-release.yml، على لينكس في ثوانٍ).

يوقّع JWT (ES256) بالمفتاح ويطلب سجل التطبيق، فيخبر بالسبب الحقيقي إن رُفض:
  200 وفيه التطبيق  → صالح
  200 بلا تطبيق     → لا سجل للتطبيق في App Store Connect
  403               → المفتاح مقبول بلا صلاحية (دور Admin واتفاقيات معلّقة)
  401               → يُعاد بصيغة مفتاح فردي (Individual key): إن قُبل فهو مفتاح فردي، وإلا فالمعرّفات والملف لا تتطابق
لا يطبع أي قيمة سرية.

    pip install pyjwt cryptography
    ASC_KEY_ID=… ASC_ISSUER_ID=… ASC_KEY_P8=<base64 of the .p8> python3 iosApp/fastlane/asc_preflight.py com.mizan.almuslim
"""
import base64
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

import jwt  # PyJWT

API = "https://api.appstoreconnect.apple.com/v1/apps"


def fail(title, message):
    print(f"::error title={title}::{message}")
    sys.exit(1)


def token(key_id, issuer, pem, individual):
    now = int(time.time())
    payload = {"aud": "appstoreconnect-v1", "iat": now, "exp": now + 15 * 60}
    if individual:
        payload["sub"] = "user"
    else:
        payload["iss"] = issuer
    return jwt.encode(payload, pem, algorithm="ES256", headers={"kid": key_id, "typ": "JWT"})


def call(bundle_id, bearer):
    query = urllib.parse.urlencode({"filter[bundleId]": bundle_id, "fields[apps]": "bundleId,name"})
    req = urllib.request.Request(f"{API}?{query}", headers={"Authorization": f"Bearer {bearer}"})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, json.loads(r.read().decode() or "{}")
    except urllib.error.HTTPError as e:
        body = e.read().decode(errors="replace")
        try:
            return e.code, json.loads(body)
        except ValueError:
            return e.code, {"errors": [{"detail": body[:300]}]}


def details(body):
    return "; ".join(
        " — ".join(x for x in (err.get("code"), err.get("title"), err.get("detail")) if x)
        for err in body.get("errors", [])
    ) or "no details"


def main():
    bundle_id = sys.argv[1] if len(sys.argv) > 1 else "com.mizan.almuslim"
    key_id = "".join(os.environ.get("ASC_KEY_ID", "").split())
    issuer = "".join(os.environ.get("ASC_ISSUER_ID", "").split())
    raw = "".join(os.environ.get("ASC_KEY_P8", "").split())
    try:
        pem = base64.b64decode(raw, validate=True).decode()
    except Exception:
        fail("ASC_KEY_P8", "ASC_KEY_P8 is not valid base64 (it must be the base64 of the whole AuthKey_XXXX.p8 file)")
    if "BEGIN PRIVATE KEY" not in pem:
        fail("ASC_KEY_P8", "ASC_KEY_P8 does not decode to a .p8 private key")

    try:
        status, body = call(bundle_id, token(key_id, issuer, pem, individual=False))
    except Exception as e:  # مفتاح لا يُقرأ، أو لا اتصال
        fail("App Store Connect API", f"could not sign or send the request: {type(e).__name__}: {e}")

    if status == 200:
        apps = [a for a in body.get("data", []) if a.get("attributes", {}).get("bundleId") == bundle_id]
        if not apps:
            fail("App record missing", f"The key works, but there is no App Store Connect app for {bundle_id}: "
                 f"create the App Store Connect app record for {bundle_id} (APP_STORE.md step c)")
        name = apps[0].get("attributes", {}).get("name", "")
        print(f"App Store Connect API key OK (Team key) — app {bundle_id} found: {name}")
        return
    if status == 403:
        fail("ASC key permission", "The key was accepted but lacks permission: give it the Admin role "
             "(Users and Access → Integrations → Team Keys) and accept pending agreements (Business). "
             f"API: {details(body)}")
    if status == 401:
        try:
            status2, body2 = call(bundle_id, token(key_id, issuer, pem, individual=True))
        except Exception:
            status2, body2 = 0, {}
        if status2 in (200, 403):
            fail("Individual key", "This is an Individual key: create a Team key with Admin access "
                 "(Users and Access → Integrations → App Store Connect API → Team Keys) and update ASC_KEY_ID and ASC_KEY_P8")
        fail("ASC key rejected", "Key ID / Issuer ID / .p8 don't match, or the key was revoked "
             f"(check ASC_KEY_ID, ASC_ISSUER_ID and ASC_KEY_P8 belong to the same Team key). API: {details(body)}")
    fail("App Store Connect API", f"Unexpected HTTP {status}: {details(body)}")


if __name__ == "__main__":
    main()
