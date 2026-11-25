#!/usr/bin/env python3
"""شهادات التطوير التي تُنشئها بنايات CI في حساب Apple (App Store Connect API). لا يطبع أي سر ولا اسم شخص.

كل بناء موقَّع في ios-release.yml يجري على جهاز macOS مؤقت لا شهادة فيه، فيُنشئ Xcode (-allowProvisioningUpdates
بمفتاح الـ API) شهادة «Apple Development» جديدة اسمها «Created via API»، ومفتاحها الخاص يذهب مع الجهاز. وللحساب حدّ
من الشهادات، فإذا بلغه رفض Xcode الأرشفة («Your account has reached the maximum number of certificates»).

    --list                         يعدّ الشهادات بأنواعها (الافتراضي)
    --revoke-ci-development        يلغي شهادات التطوير التي أنشأتها بنايات CI وحدها: نوعها DEVELOPMENT أو IOS_DEVELOPMENT
                                   واسمها «Created via API». لا تُمس شهادة التوزيع، ولا شهادات أجهزة صاحب الحساب.

    pip install pyjwt cryptography
    ASC_KEY_ID=… ASC_ISSUER_ID=… ASC_KEY_P8=<base64 of the .p8> python3 iosApp/fastlane/asc_certs.py --revoke-ci-development
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

BASE = "https://api.appstoreconnect.apple.com"
DEVELOPMENT = {"DEVELOPMENT", "IOS_DEVELOPMENT"}
CI_NAME = "Created via API"


def token(key_id, issuer, pem, individual):
    now = int(time.time())
    payload = {"aud": "appstoreconnect-v1", "iat": now, "exp": now + 15 * 60}
    if individual:
        payload["sub"] = "user"
    else:
        payload["iss"] = issuer
    return jwt.encode(payload, pem, algorithm="ES256", headers={"kid": key_id, "typ": "JWT"})


def request(method, url, bearer):
    req = urllib.request.Request(url, method=method, headers={"Authorization": f"Bearer {bearer}"})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            raw = r.read().decode()
            return r.status, (json.loads(raw) if raw else {})
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


def certificates(bearer):
    url = BASE + "/v1/certificates?" + urllib.parse.urlencode(
        {"limit": 200, "fields[certificates]": "name,displayName,certificateType,expirationDate"})
    out = []
    while url:
        status, body = request("GET", url, bearer)
        if status != 200:
            return status, body
        out += body.get("data", [])
        url = body.get("links", {}).get("next")
    return 200, out


def main():
    revoke = "--revoke-ci-development" in sys.argv[1:]
    key_id = os.environ.get("ASC_KEY_ID", "").strip()
    issuer = os.environ.get("ASC_ISSUER_ID", "").strip()
    pem = base64.b64decode("".join(os.environ.get("ASC_KEY_P8", "").split())).decode()

    # مفتاح الفريق أولاً، ثم المفتاح الفردي إن رُفض الأول (كما في asc_preflight.py)
    bearer = token(key_id, issuer, pem, individual=False)
    status, certs = certificates(bearer)
    if status == 401:
        bearer = token(key_id, issuer, pem, individual=True)
        status, certs = certificates(bearer)
    if status != 200:
        print(f"::warning title=Certificates::could not list certificates (HTTP {status}): {details(certs)}")
        return 0

    counts = {}
    ci = []
    for c in certs:
        a = c.get("attributes", {})
        kind = a.get("certificateType", "?")
        counts[kind] = counts.get(kind, 0) + 1
        if kind in DEVELOPMENT and (CI_NAME in (a.get("name") or "") or CI_NAME in (a.get("displayName") or "")):
            ci.append((c["id"], kind, (a.get("expirationDate") or "")[:10]))
    print("Certificates by type: " + (", ".join(f"{k} {v}" for k, v in sorted(counts.items())) or "none"))
    print(f"Development certificates created by CI builds ({CI_NAME!r}): {len(ci)}")

    if not revoke:
        return 0
    revoked = 0
    for cid, kind, expires in ci:
        status, body = request("DELETE", f"{BASE}/v1/certificates/{cid}", bearer)
        if status in (200, 204):
            revoked += 1
            print(f"revoked {kind} (expires {expires})")
        else:
            print(f"::warning title=Certificates::could not revoke a {kind} certificate (HTTP {status}): {details(body)}")
    print(f"Revoked {revoked} of {len(ci)} CI development certificates; distribution and personal certificates untouched.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
