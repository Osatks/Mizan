#!/bin/bash
# لقطات App Store من المحاكي (يستدعيه ios-screenshots.yml على macOS بصورة xcode-27):
#   iphone  iPhone بشاشة 6.9 بوصة (1320×2868)                       → <المجلد>/<اللغة>/NN_*.png
#   ipad    iPad Pro 13 بوصة (2064×2752) بالطول، ولقطة المصحف بالعرض صفحتين متقابلتين (2752×2064)
#                                                                      → <المجلد>/<اللغة>/ipad_NN_*.png
#   duo     iPhone Duo من محاكي iOS 27.1 (الشاشة الداخلية، والخارجية إن أمكن اختيارها)
#                                                                      → <المجلد>/<اللغة>/duo_*.png
#   watch   Apple Watch Ultra (410×502): المواقيت والمسبحة، بلقطة من حالات Kotlin (FIXTURES=live-fixtures.json)
#                                                                      → <المجلد>/<اللغة>/watch_*.png
#   screenshots.sh <Mizan.app للمحاكي> <مجلد الإخراج> "<ar-SA en-US ...>" ["iphone ipad duo"]
#
# أسماء الملفات تحدد ترتيبها في المتجر، ومقاسها يحدد نوع الشاشة عند deliver (fastlane/check_screenshots.rb يتحقق منه،
# وينقل ما لا يعرفه deliver إلى ملف مرفق فقط).
#
# تجاوز جولة أول تشغيل واختيار المدينة بلا كود خاص: وسائط التشغيل «-مفتاح قيمة» تدخل NSArgumentDomain
# في NSUserDefaults، فيقرؤها Prefs (مفاتيح AppSettings بالبادئة «mizan.») كأنها محفوظة، ولا تُكتب في الجهاز.
# وكل شاشة تُفتح عند التشغيل بالوسيط «-mizan.open_route <المسار>» (MainViewController.kt)، لا برابط mizan://
# يسأل النظام قبل فتحه («فتح في ميزان؟») فتظهر نافذته في اللقطة
set -euo pipefail

APP="$1"
OUT="$2"
LOCALES="${3:-ar-SA en-US}"
DEVICES="${4:-iphone ipad duo}"
BUNDLE=com.mizan.almuslim
LAUNCH_WAIT="${LAUNCH_WAIT:-10}"   # ثوانٍ بعد التشغيل حتى يستقر أول إطار
ROUTE_WAIT="${ROUTE_WAIT:-8}"      # ثوانٍ بعد فتح الشاشة (انتقال + تحميل النص)
LOGS="${LOGS:-$OUT/../screenshot-logs}"
mkdir -p "$OUT" "$LOGS"

log() { echo "▸ $*"; }

# الترتيب = ترتيب ظهورها في المتجر (أول ثلاث هي الأهم)
IPHONE_SHOTS=(
  "01_home:home"
  "02_quran_page:surah/1?ayah=0&from=row"
  "03_prayer:prayer"
  "04_adhkar:adhkar"
  "05_quran_index:quran"
  "06_qibla:qibla"
  "07_zakat:zakat"
  "08_tasbih:tasbih"
)
# iPad: القائمة الجانبية والأعمدة؛ والثانية (المصحف صفحتين متقابلتين) تُلتقط بالعرض
IPAD_SHOTS=(
  "ipad_01_home:home"
  "ipad_03_prayer:prayer"
  "ipad_04_quran_index:quran"
  "ipad_05_quran_page:surah/2?ayah=255&from=row"
  "ipad_06_adhkar:adhkar"
  "ipad_07_tasbih:tasbih"
)
IPAD_LANDSCAPE=(
  "ipad_02_quran_spread:surah/1?ayah=0&from=row"
)
DUO_SHOTS=(
  "01_home:home"
  "02_quran_page:surah/1?ayah=0&from=row"
  "03_prayer:prayer"
)

# ---------- المحاكي ----------
pick_device_type() {
  xcrun simctl list devicetypes -j | python3 -c '
import json, sys
types = {t["name"]: t["identifier"] for t in json.load(sys.stdin)["devicetypes"]}
for name in sys.argv[1:]:
    if name in types:
        print(types[name]); break
else:
    sys.exit("none of these device types: " + ", ".join(sys.argv[1:]))' "$@"
}

# أحدث نظام iOS في المحاكي يدعم نوع الجهاز (نظام 27.1 قد يكون لـ iPhone Duo وحده)
pick_runtime() {
  xcrun simctl list runtimes -j | python3 -c '
import json, sys
want = sys.argv[1]
rs = [r for r in json.load(sys.stdin)["runtimes"] if r.get("isAvailable") and r.get("platform", "iOS") == "iOS" and "iOS" in r["name"]]
def ok(r):
    types = r.get("supportedDeviceTypes")
    return types is None or any(t.get("identifier") == want for t in types)
rs = [r for r in rs if ok(r)]
rs.sort(key=lambda r: [int(x) for x in r["version"].split(".")])
if not rs: sys.exit("no iOS simulator runtime for " + want)
print(rs[-1]["identifier"])' "$1"
}

CREATED=()
cleanup() {
  for u in "${CREATED[@]:-}"; do
    [ -n "$u" ] || continue
    xcrun simctl shutdown "$u" >/dev/null 2>&1 || true
    xcrun simctl delete "$u" >/dev/null 2>&1 || true
  done
  osascript -e 'quit app "Simulator"' >/dev/null 2>&1 || true
}
trap cleanup EXIT

boot() {
  xcrun simctl boot "$UDID" 2>/dev/null || true
  xcrun simctl bootstatus "$UDID" -b
  # شريط الحالة كما في صور Apple
  for _ in 1 2 3; do
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

size_of() { echo "$(sips -g pixelWidth "$1" | awk '/pixelWidth/ {print $2}')x$(sips -g pixelHeight "$1" | awk '/pixelHeight/ {print $2}')"; }

# لقطة الشاشة؛ [$2] اسم شاشة الجهاز إن كان له أكثر من شاشة
shot() {
  local file="$1" display="${2:-}"
  for _ in 1 2 3; do
    if xcrun simctl io "$UDID" screenshot --type=png ${display:+--display="$display"} "$file" >/dev/null 2>&1 && [ -s "$file" ]; then
      log "  $(basename "$file") $(size_of "$file")${display:+ ($display)}"
      return 0
    fi
    sleep 2
  done
  echo "::error::screenshot failed: $file"
  return 1
}

launch() {
  local lang="$1" route="${2:-}"
  xcrun simctl terminate "$UDID" "$BUNDLE" >/dev/null 2>&1 || true
  sleep 1
  # مكة المكرمة: مفتاح المدينة وإحداثياتها كما في PrayTimes.CITIES
  xcrun simctl launch "$UDID" "$BUNDLE" \
    -mizan.onboarded YES -mizan.lang "$lang" \
    -mizan.place_key makkah -mizan.place_lat 21.4225 -mizan.place_lng 39.8262 \
    -mizan.place_tz Asia/Riyadh -mizan.place_method makkah -mizan.place_gps NO -mizan.method makkah \
    -mizan.theme light ${route:+-mizan.open_route "$route"} >/dev/null
  sleep "$LAUNCH_WAIT"
  [ -z "$route" ] || sleep "$ROUTE_WAIT"
}

set_locale() {
  local loc="$1"
  # لغة النظام نفسها (أسماء الأيام وتنسيق الأرقام من NSLocale)، تتطلب إعادة تشغيل المحاكي
  xcrun simctl spawn "$UDID" defaults write -g AppleLanguages -array "${loc%%-*}"
  xcrun simctl spawn "$UDID" defaults write -g AppleLocale -string "$(region_of "$loc")"
  xcrun simctl shutdown "$UDID" || true
  boot
}

# اللقطات [$3...] بأسمائها [prefix] في مجلد اللغة، كل واحدة من تشغيل جديد
take() {
  local loc="$1" prefix="$2" display="$3"; shift 3
  local s name route
  for s in "$@"; do
    name="${s%%:*}"
    route="${s#*:}"
    if [ "$route" = "home" ]; then launch "${loc%%-*}"; else launch "${loc%%-*}" "$route"; fi
    shot "$OUT/$loc/$prefix$name.png" "$display"
  done
}

prepare() {
  boot
  xcrun simctl install "$UDID" "$APP"
  # إذن الموقع مسبقاً (لا نافذة إذن فوق القبلة)، وموقع المحاكي مكة
  xcrun simctl privacy "$UDID" grant location "$BUNDLE" || true
  xcrun simctl location "$UDID" set 21.4225,39.8262 || true
  # تشغيل أول للتهيئة (نسخ الخطوط والقاعدة إلى الذاكرة) قبل أول لقطة
  launch ar
  sleep 5
}

new_device() {
  local kind="$1" type="$2" runtime
  runtime=$(pick_runtime "$type")
  log "$kind: device type $type, runtime $runtime"
  UDID=$(xcrun simctl create "Mizan $kind" "$type" "$runtime")
  CREATED+=("$UDID")
}

# ---------- اتجاه iPad ----------
# simctl لا يدير الجهاز في كل إصدار: أمر اتجاه إن وُجد في هذا الإصدار، وإلا تطبيق Simulator بتفضيل الاتجاه لهذا الجهاز،
# وإلا اختصار التدوير (يحتاج إذن الوصول). يُتحقق من النتيجة بمقاس اللقطة (العرض أكبر من الارتفاع).
SIMPREFS_DOMAIN=com.apple.iphonesimulator
simulator_orientation() {
  local orientation="$1" angle="$2" tmp
  tmp=$(mktemp -t simprefs)
  defaults export "$SIMPREFS_DOMAIN" "$tmp" 2>/dev/null || printf '%s\n' '<?xml version="1.0" encoding="UTF-8"?>' \
    '<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">' \
    '<plist version="1.0"><dict/></plist>' > "$tmp"
  /usr/libexec/PlistBuddy -c "Add :DevicePreferences dict" "$tmp" >/dev/null 2>&1 || true
  /usr/libexec/PlistBuddy -c "Add :DevicePreferences:$UDID dict" "$tmp" >/dev/null 2>&1 || true
  /usr/libexec/PlistBuddy -c "Delete :DevicePreferences:$UDID:SimulatorWindowOrientation" "$tmp" >/dev/null 2>&1 || true
  /usr/libexec/PlistBuddy -c "Add :DevicePreferences:$UDID:SimulatorWindowOrientation string $orientation" "$tmp"
  /usr/libexec/PlistBuddy -c "Delete :DevicePreferences:$UDID:SimulatorWindowRotationAngle" "$tmp" >/dev/null 2>&1 || true
  /usr/libexec/PlistBuddy -c "Add :DevicePreferences:$UDID:SimulatorWindowRotationAngle real $angle" "$tmp"
  defaults import "$SIMPREFS_DOMAIN" "$tmp"
  rm -f "$tmp"
}

is_landscape() {
  local probe="$LOGS/probe-$UDID.png" w h
  xcrun simctl io "$UDID" screenshot --type=png "$probe" >/dev/null 2>&1 || return 1
  w=$(sips -g pixelWidth "$probe" | awk '/pixelWidth/ {print $2}')
  h=$(sips -g pixelHeight "$probe" | awk '/pixelHeight/ {print $2}')
  rm -f "$probe"
  [ "$w" -gt "$h" ]
}

rotate_landscape() {
  local help cmd arg
  help=$(xcrun simctl help 2>&1 || true)
  printf '%s\n' "$help" > "$LOGS/simctl-help.txt"
  for cmd in $(printf '%s\n' "$help" | awk 'NF {print $1}' | grep -ixE 'orientation|rotate|rotation' || true); do
    log "simctl has '$cmd':"
    xcrun simctl help "$cmd" 2>&1 | sed 's/^/    /' || true
    for arg in landscape-left landscapeLeft LandscapeLeft landscape left; do
      if xcrun simctl "$cmd" "$UDID" "$arg" >/dev/null 2>&1; then sleep 3; is_landscape && { log "rotated with simctl $cmd $arg"; return 0; }; fi
    done
  done
  simulator_orientation LandscapeLeft 90
  open -a Simulator --args -CurrentDeviceUDID "$UDID" || true
  sleep 15
  if is_landscape; then log "rotated with Simulator.app preferences"; return 0; fi
  osascript -e 'tell application "Simulator" to activate' -e 'delay 1' \
    -e 'tell application "System Events" to key code 123 using command down' >/dev/null 2>&1 || true
  sleep 4
  if is_landscape; then log "rotated with the Simulator rotate shortcut"; return 0; fi
  return 1
}

rotate_portrait() {
  simulator_orientation Portrait 0
  osascript -e 'quit app "Simulator"' >/dev/null 2>&1 || true
  sleep 3
}

# ---------- iPhone Duo ----------
# حالة الطي من سطر الأوامر إن كان في simctl أمر لها (يُطبع نص المساعدة في السجل)، وإلا الحالة الافتراضية للمحاكي
DUO_FOLD="default"
duo_unfold() {
  local help cmd arg
  help=$(xcrun simctl help 2>&1 || true)
  printf '%s\n' "$help" > "$LOGS/simctl-help.txt"
  cmd=$(printf '%s\n' "$help" | awk 'NF {print $1}' | grep -ixE 'fold|unfold|posture|hinge|foldstate|fold-state|fold_state' | head -n1 || true)
  if [ -z "$cmd" ]; then
    echo "::notice title=iPhone Duo::simctl on $(xcodebuild -version | head -n1) has no fold or posture command (full help in screenshot-logs/simctl-help.txt); the Duo screenshots use the simulator's default display state"
    return 0
  fi
  log "simctl has '$cmd':"
  xcrun simctl help "$cmd" 2>&1 | tee "$LOGS/simctl-help-$cmd.txt" | sed 's/^/    /' || true
  for arg in unfolded open unfold inner flat expanded; do
    if xcrun simctl "$cmd" "$UDID" "$arg" >/dev/null 2>&1; then DUO_FOLD="$cmd $arg"; log "fold state: simctl $DUO_FOLD"; sleep 3; return 0; fi
  done
  echo "::notice title=iPhone Duo::could not set the fold state with 'simctl $cmd' (see screenshot-logs/simctl-help-$cmd.txt); the Duo screenshots use the simulator's default display state"
}

# شاشتا الجهاز القابل للطي إن ذكرهما «simctl io screenshot --display» (internal و external المعتادتان ليستا منهما)
duo_displays() {
  xcrun simctl help io > "$LOGS/simctl-help-io.txt" 2>&1 || true
  grep -oiE '\b(inner|outer|cover)\b' "$LOGS/simctl-help-io.txt" | tr 'A-Z' 'a-z' | sort -u | tr '\n' ' ' || true
}

# ---------- Apple Watch ----------
# تطبيق الساعة في Mizan.app/Watch، يُثبَّت في محاكي ساعة وحده. لا iPhone معه: تُمرَّر له لقطة المواقيت كما يرسلها
# التطبيق (fastlane/watch_payload.py من حالات Kotlin في FIXTURES) وسيطَ تشغيل، فيقرؤها من UserDefaults كما يحفظها.
# Apple Watch Ultra بشاشة 49 مم (410×502) أولاً: مقاس يعرفه deliver
WATCH_BUNDLE=com.mizan.almuslim.watchkitapp
WATCH_SHOTS=("watch_01_prayer:0" "watch_02_tasbih:1")

run_watch() {
  local watch_app="$APP/Watch/MizanWatch.app" type runtime loc s name page
  if [ ! -d "$watch_app" ]; then
    echo "::warning title=Apple Watch::no watch app in $APP/Watch"
    return 0
  fi
  if [ -z "${FIXTURES:-}" ] || [ ! -f "$FIXTURES" ]; then
    echo "::warning title=Apple Watch::FIXTURES (live-fixtures.json from LiveStatesTest) not given: watch screenshots skipped"
    return 0
  fi
  # محاكي watchOS ليس مثبّتاً دائماً في صورة المشغّل: يُنزَّل إن غاب
  if ! xcrun simctl list runtimes | grep -q watchOS; then
    log "downloading the watchOS simulator runtime"
    xcodebuild -downloadPlatform watchOS || { echo "::warning title=Apple Watch::could not download the watchOS simulator"; return 0; }
  fi
  # أحدث watchOS مثبّت وساعة يدعمها (Ultra 2 أولاً: 410×502)
  read -r runtime type < <(xcrun simctl list runtimes -j | python3 "$(dirname "$0")/pick_watch.py")
  log "watch: device type $type, runtime $runtime"
  UDID=$(xcrun simctl create "Mizan watch" "$type" "$runtime")
  CREATED+=("$UDID")
  xcrun simctl boot "$UDID" 2>/dev/null || true
  xcrun simctl bootstatus "$UDID" -b
  xcrun simctl status_bar "$UDID" override --time "9:41" >/dev/null 2>&1 || true
  xcrun simctl install "$UDID" "$watch_app"
  for loc in $LOCALES; do
    log "Apple Watch · $loc"
    mkdir -p "$OUT/$loc"
    local arg
    arg=$(python3 "$(dirname "$0")/watch_payload.py" "$FIXTURES" "${loc%%-*}")
    for s in "${WATCH_SHOTS[@]}"; do
      name="${s%%:*}"
      page="${s#*:}"
      xcrun simctl terminate "$UDID" "$WATCH_BUNDLE" >/dev/null 2>&1 || true
      sleep 1
      xcrun simctl launch "$UDID" "$WATCH_BUNDLE" -watch.json "$arg" -watch.page "$page" >/dev/null
      sleep 8
      shot "$OUT/$loc/$name.png"
    done
  done
}

# ---------- التشغيل ----------
run_iphone() {
  new_device iphone "$(pick_device_type "iPhone 17 Pro Max" "iPhone 16 Pro Max" "iPhone 18 Pro Max")"
  prepare
  for loc in $LOCALES; do
    log "iPhone · $loc"
    set_locale "$loc"
    mkdir -p "$OUT/$loc"
    take "$loc" "" "" "${IPHONE_SHOTS[@]}"
  done
}

run_ipad() {
  new_device ipad "$(pick_device_type "iPad Pro 13-inch (M5)" "iPad Pro 13-inch (M4)" "iPad Air 13-inch (M4)" "iPad Air 13-inch (M3)")"
  prepare
  for loc in $LOCALES; do
    log "iPad · $loc"
    set_locale "$loc"
    mkdir -p "$OUT/$loc"
    take "$loc" "" "" "${IPAD_SHOTS[@]}"
    if rotate_landscape; then
      take "$loc" "" "" "${IPAD_LANDSCAPE[@]}"
    else
      echo "::warning title=iPad landscape::could not rotate the iPad simulator from the command line on this runner; the landscape mushaf spread shot was skipped for $loc"
    fi
    rotate_portrait
    boot
  done
}

run_duo() {
  local type displays d
  if ! type=$(pick_device_type "iPhone Duo" 2>/dev/null); then
    echo "::warning title=iPhone Duo::no iPhone Duo device type in $(xcodebuild -version | head -n1)"
    return 0
  fi
  new_device duo "$type"
  prepare
  duo_unfold
  displays=$(duo_displays)
  log "Duo displays offered by simctl io: ${displays:-none listed} (fold state: $DUO_FOLD)"
  for loc in $LOCALES; do
    log "iPhone Duo · $loc"
    set_locale "$loc"
    [ "$DUO_FOLD" = "default" ] || duo_unfold
    mkdir -p "$OUT/$loc"
    if [ -n "$displays" ]; then
      for d in $displays; do take "$loc" "duo_${d}_" "$d" "${DUO_SHOTS[@]}" || true; done
    else
      take "$loc" "duo_" "" "${DUO_SHOTS[@]}"
    fi
  done
}

for device in $DEVICES; do
  case "$device" in
    iphone) run_iphone ;;
    ipad) run_ipad ;;
    duo) run_duo || echo "::warning title=iPhone Duo::Duo screenshots failed (see log); iPhone and iPad screenshots are unaffected" ;;
    watch) run_watch || echo "::warning title=Apple Watch::watch screenshots failed (see log); the other screenshots are unaffected" ;;
    *) echo "::error::unknown device $device"; exit 1 ;;
  esac
done

log "Done"
find "$OUT" -name '*.png' | sort
