#!/usr/bin/env python3
"""تقارير الأعطال والأداء من App Store Connect API، في مجلد crash-reports/ (لا يطبع أي سر):

  testflight/      أعطال TestFlight التي أرسلها المختبرون (betaFeedbackCrashSubmissions): التعليق والجهاز
                   والنظام والبناء، ونص سجل العطل كاملاً (crashLog)
  diagnostics/     توقيعات التشخيص التي تجمعها Apple من أجهزة المستخدمين لآخر البنايات (diagnosticSignatures):
                   التعليق (HANGS)، والإقلاع البطيء (LAUNCHES)، والكتابة الكثيرة على القرص (DISK_WRITES)،
                   مع وزن كل توقيع وسجلاته (مكدّس الاستدعاءات)
  summary.md       ملخص ما وُجد

أعطال مستخدمي App Store نفسها لا يتيحها الـ API (تظهر في Xcode › Organizer › Crashes فقط).

    pip install pyjwt cryptography
    ASC_KEY_ID=… ASC_ISSUER_ID=… ASC_KEY_P8=<base64 of the .p8> python3 iosApp/fastlane/asc_crashes.py com.mizan.almuslim
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
OUT = "crash-reports"


class Api:
    def __init__(self, key_id, issuer, pem):
        self.key_id, self.issuer, self.pem = key_id, issuer, pem
        self._tok, self._exp = None, 0

    def token(self):
        now = int(time.time())
        if not self._tok or now > self._exp - 60:
            self._exp = now + 15 * 60
            self._tok = jwt.encode({"iss": self.issuer, "aud": "appstoreconnect-v1", "iat": now, "exp": self._exp},
                                   self.pem, algorithm="ES256", headers={"kid": self.key_id, "typ": "JWT"})
        return self._tok

    def get(self, path, params=None, accept="application/json"):
        url = path if path.startswith("http") else BASE + path
        if params:
            url += ("&" if "?" in url else "?") + urllib.parse.urlencode(params)
        req = urllib.request.Request(url, headers={"Authorization": f"Bearer {self.token()}", "Accept": accept})
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                raw = r.read()
                return r.status, (json.loads(raw) if raw and "json" in r.headers.get("Content-Type", "") else raw)
        except urllib.error.HTTPError as e:
            body = e.read().decode(errors="replace")
            try:
                return e.code, json.loads(body)
            except ValueError:
                return e.code, {"errors": [{"detail": body[:300]}]}

    def all(self, path, params=None, limit=200):
        """كل صفحات القائمة (حتى limit عنصراً)"""
        items, included, url, p = [], [], path, dict(params or {})
        while url and len(items) < limit:
            status, body = self.get(url, p)
            if status != 200:
                return status, body, items, included
            items += body.get("data", [])
            included += body.get("included", [])
            url, p = body.get("links", {}).get("next"), None
        return 200, None, items, included


def err(body):
    if isinstance(body, dict):
        return "; ".join(" — ".join(x for x in (e.get("code"), e.get("title"), e.get("detail")) if x) for e in body.get("errors", [])) or "?"
    return "?"


def main():
    bundle = sys.argv[1] if len(sys.argv) > 1 else "com.mizan.almuslim"
    key_id = "".join(os.environ.get("ASC_KEY_ID", "").split())
    issuer = "".join(os.environ.get("ASC_ISSUER_ID", "").split())
    pem = base64.b64decode("".join(os.environ.get("ASC_KEY_P8", "").split())).decode()
    api = Api(key_id, issuer, pem)
    os.makedirs(f"{OUT}/testflight", exist_ok=True)
    os.makedirs(f"{OUT}/diagnostics", exist_ok=True)
    summary = [f"# Crash and diagnostics reports — {bundle}", ""]

    status, body = api.get("/v1/apps", {"filter[bundleId]": bundle, "fields[apps]": "bundleId,name"})
    if status != 200 or not body.get("data"):
        print(f"::error title=App Store Connect::cannot find the app ({status}: {err(body)})")
        sys.exit(1)
    app = body["data"][0]["id"]

    # ---------- أعطال TestFlight ----------
    summary += ["## TestFlight crash reports", ""]
    status, body, crashes, inc = api.all(f"/v1/apps/{app}/betaFeedbackCrashSubmissions",
                                         {"limit": 50, "include": "build", "sort": "-createdDate"})
    builds = {b["id"]: b.get("attributes", {}) for b in inc if b.get("type") == "builds"}
    if status != 200:
        summary.append(f"Could not read them: HTTP {status} — {err(body)}")
    elif not crashes:
        summary.append("None.")
    for c in crashes:
        a = c.get("attributes", {})
        bid = c.get("relationships", {}).get("build", {}).get("data", {}) or {}
        ver = builds.get(bid.get("id"), {}).get("version", "?")
        name = f"{a.get('createdDate', '')[:19].replace(':', '-')}_{c['id'][:8]}"
        st, log = api.get(f"/v1/betaFeedbackCrashSubmissions/{c['id']}/crashLog")
        text = ""
        if st == 200 and isinstance(log, dict):
            text = log.get("data", {}).get("attributes", {}).get("logText", "") or ""
        meta = {k: a.get(k) for k in ("createdDate", "comment", "deviceModel", "osVersion", "appPlatform", "devicePlatform",
                                       "deviceFamily", "locale", "batteryPercentage", "appUptimeInMilliseconds", "connectionType")}
        meta["build"] = ver
        with open(f"{OUT}/testflight/{name}.txt", "w") as f:
            f.write(json.dumps(meta, ensure_ascii=False, indent=2) + "\n\n" + (text or f"(no crash log: HTTP {st})"))
        first = next((l.strip() for l in text.splitlines() if l.startswith(("Exception Type", "Termination Reason", "Crashed Thread"))), "")
        summary.append(f"- {a.get('createdDate', '')[:16]} · build {ver} · {a.get('deviceModel')} iOS {a.get('osVersion')} · "
                       f"{(a.get('comment') or '').strip()[:80]} · {first}")
    summary.append("")

    # ---------- توقيعات التشخيص لآخر البنايات ----------
    summary += ["## Diagnostic signatures (last builds)", ""]
    status, body, blist, _ = api.all("/v1/builds", {"filter[app]": app, "sort": "-uploadedDate", "limit": 5,
                                                     "fields[builds]": "version,uploadedDate"}, limit=5)
    if status != 200:
        summary.append(f"Could not read builds: HTTP {status} — {err(body)}")
    for b in blist[:5]:
        ver = b.get("attributes", {}).get("version")
        st, sbody, sigs, _ = api.all(f"/v1/builds/{b['id']}/diagnosticSignatures", {"limit": 50})
        if st != 200:
            summary.append(f"- build {ver}: HTTP {st} — {err(sbody)}")
            continue
        if not sigs:
            summary.append(f"- build {ver}: no signatures yet")
            continue
        for s in sigs:
            sa = s.get("attributes", {})
            line = f"- build {ver} · {sa.get('diagnosticType')} · weight {sa.get('weight')} · {sa.get('signature', '')[:140]}"
            summary.append(line)
            st2, logs = api.get(f"/v1/diagnosticSignatures/{s['id']}/logs", accept="application/vnd.apple.diagnostic-logs+json")
            with open(f"{OUT}/diagnostics/build{ver}_{sa.get('diagnosticType')}_{s['id'][:8]}.json", "w") as f:
                json.dump({"signature": sa, "logs": logs if st2 == 200 and not isinstance(logs, bytes) else
                           (logs.decode(errors='replace') if isinstance(logs, bytes) else logs)}, f, ensure_ascii=False, indent=2)
    summary += ["", "App Store (customer) crash reports are not in the API: Xcode › Window › Organizer › Crashes."]
    open(f"{OUT}/summary.md", "w").write("\n".join(summary) + "\n")
    print("\n".join(summary))


if __name__ == "__main__":
    main()
