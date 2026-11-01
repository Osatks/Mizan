#!/bin/bash
# لقطات App Store من محاكي iPhone بشاشة 6.9 بوصة (1320×2868): يستدعيه ios-screenshots.yml على macOS.
#   screenshots.sh <Mizan.app للمحاكي> <مجلد الإخراج> "<ar-SA en-US ...>"
#
# تجاوز جولة أول تشغيل واختيار المدينة بلا كود خاص: وسائط التشغيل «-مفتاح قيمة» تدخل NSArgumentDomain
# في NSUserDefaults، فيقرؤها Prefs (مفاتيح AppSettings بالبادئة «mizan.») كأنها محفوظة، ولا تُكتب في الجهاز.
# ثم تُفتح كل شاشة بروابط الأدوات نفسها: mizan://open?route=…
set -euo pipefail

APP="$1"
OUT="$2"
LOCALES="${3:-ar-SA en-US}"
BUNDLE=com.mizan.almuslim
LAUNCH_WAIT="${LAUNCH_WAIT:-10}"   # ثوانٍ بعد التشغيل حتى يستقر أول إطار
ROUTE_WAIT="${ROUTE_WAIT:-6}"      # ثوانٍ بعد فتح الشاشة (انتقال + تحميل النص)

log() { echo "▸ $*"; }

# ---------- المحاكي ----------
pick_device_type() {
  xcrun simctl list devicetypes -j | python3 -c '
import json, sys
types = {t["name"]: t["identifier"] for t in json.load(sys.stdin)["devicetypes"]}
for name in ("iPhone 17 Pro Max", "iPhone 16 Pro Max", "iPhone 16 Plus", "iPhone 15 Pro Max"):
    if name in types:
        print(types[name]); break
else:
    sys.exit("no 6.9-inch/6.7-inch iPhone device type")'
}
pick_runtime() {
  xcrun simctl list runtimes -j | python3 -c '
import json, sys
rs = [r for r in json.load(sys.stdin)["runtimes"] if r.get("isAvailable") and r.get("platform", "iOS") == "iOS" and "iOS" in r["name"]]
rs.sort(key=lambda r: [int(x) for x in r["version"].split(".")])
if not rs: sys.exit("no iOS simulator runtime")
print(rs[-1]["identifier"])'
}

TYPE=$(pick_device_type)
RUNTIME=$(pick_runtime)
log "Device type $TYPE, runtime $RUNTIME"
UDID=$(xcrun simctl create "Mizan Screenshots" "$TYPE" "$RUNTIME")
trap 'xcrun simctl shutdown "$UDID" >/dev/null 2>&1 || true; xcrun simctl delete "$UDID" >/dev/null 2>&1 || true' EXIT

boot() {
  xcrun simctl boot "$UDID" 2>/dev/null || true
  xcrun simctl bootstatus "$UDID" -b
  # شريط الحالة كما في صور Apple
  for i in 1 2 3; do
    xcrun simctl status_bar "$UDID" override --time "9:41" --dataNetwork wifi --wifiMode active --wifiBars 3 \
      --cellularMode active --cellularBars 4 --batteryState charged --batteryLevel 100 && break
    sleep 3
  done
  xcrun simctl ui "$UDID" appearance light || true
}

region_of() {
  case "$1" in
    ar-SA) echo ar_SA ;; en-US) echo en_US ;; fr-FR) echo fr_FR ;; de-DE) echo de_DE ;; es-ES) echo es_ES ;;
    tr) echo tr_TR ;; id) echo id_ID ;; ms) echo ms_MY ;; ru) echo ru_RU ;; uk) echo uk_UA ;;
    ur-PK) echo ur_PK ;; bn-BD) echo bn_BD ;; *) echo en_US ;;
  esac
}

shot() {
  local file="$1"
  for i in 1 2 3; do
    if xcrun simctl io "$UDID" screenshot --type=png "$file" >/dev/null 2>&1 && [ -s "$file" ]; then
      local w h
      w=$(sips -g pixelWidth "$file" | awk '/pixelWidth/ {print $2}')
      h=$(sips -g pixelHeight "$file" | awk '/pixelHeight/ {print $2}')
      log "  $(basename "$file") ${w}x${h}"
      return 0
    fi
    sleep 2
  done
  echo "::error::screenshot failed: $file"
  return 1
}

launch() {
  local lang="$1"
  xcrun simctl terminate "$UDID" "$BUNDLE" >/dev/null 2>&1 || true
  sleep 1
  # مكة المكرمة: مفتاح المدينة وإحداثياتها كما في PrayTimes.CITIES
  xcrun simctl launch "$UDID" "$BUNDLE" \
    -mizan.onboarded YES -mizan.lang "$lang" \
    -mizan.place_key makkah -mizan.place_lat 21.4225 -mizan.place_lng 39.8262 \
    -mizan.place_tz Asia/Riyadh -mizan.place_method makkah -mizan.place_gps NO -mizan.method makkah \
    -mizan.theme light >/dev/null
  sleep "$LAUNCH_WAIT"
}

open_route() {
  # الرابط مرمّز: surah/1?ayah=0 يمرّ قيمةً واحدة لـ route
  local enc
  enc=$(python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "$1")
  for i in 1 2 3; do
    xcrun simctl openurl "$UDID" "mizan://open?route=$enc" && break
    sleep 2
  done
  sleep "$ROUTE_WAIT"
}

# الترتيب = ترتيب ظهورها في المتجر (أول ثلاث هي الأهم)
SHOTS=(
  "01_home:home"
  "02_quran_page:surah/1?ayah=0&from=row"
  "03_prayer:prayer"
  "04_adhkar:adhkar"
  "05_quran_index:quran"
  "06_qibla:qibla"
  "07_zakat:zakat"
  "08_tasbih:tasbih"
)

boot
xcrun simctl install "$UDID" "$APP"
# إذن الموقع مسبقاً (لا نافذة إذن فوق القبلة)، وموقع المحاكي مكة
xcrun simctl privacy "$UDID" grant location "$BUNDLE" || true
xcrun simctl location "$UDID" set 21.4225,39.8262 || true

# تشغيل أول للتهيئة (نسخ الخطوط والقاعدة إلى الذاكرة) قبل أول لقطة
launch ar
sleep 5

for loc in $LOCALES; do
  lang="${loc%%-*}"
  region=$(region_of "$loc")
  log "Locale $loc (app language $lang)"
  # لغة النظام نفسها (أسماء الأيام وتنسيق الأرقام من NSLocale)، تتطلب إعادة تشغيل المحاكي
  xcrun simctl spawn "$UDID" defaults write -g AppleLanguages -array "$lang"
  xcrun simctl spawn "$UDID" defaults write -g AppleLocale -string "$region"
  xcrun simctl shutdown "$UDID" || true
  boot
  mkdir -p "$OUT/$loc"
  rm -f "$OUT/$loc"/*.png
  for s in "${SHOTS[@]}"; do
    name="${s%%:*}"
    route="${s#*:}"
    launch "$lang"
    [ "$route" = "home" ] || open_route "$route"
    shot "$OUT/$loc/$name.png"
  done
done

log "Done"
find "$OUT" -name '*.png' | sort
