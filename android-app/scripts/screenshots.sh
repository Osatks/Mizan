#!/usr/bin/env bash
# يلتقط صوراً لشاشات التطبيق على المحاكي (يُشغَّل من GitHub Actions)
set -u
PKG=com.mizan.almuslim
# اسم الملف يحمل رقم الإصدار (mizan-2.1.N-debug.apk)
APK=$(ls android-app/app/build/outputs/apk/debug/*.apk | head -1)
OUT=screenshots
mkdir -p "$OUT"

adb install -r "$APK"
adb logcat -c || true
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb shell settings put system screen_off_timeout 600000 || true
adb shell input keyevent KEYCODE_WAKEUP || true
# تنبيه «ملء الشاشة» الذي يظهر أول مرة يغطي لقطة وضع التركيز
adb shell settings put secure immersive_mode_confirmations confirmed || true

# shot <route> <name> [lang] [accent]: اللغة الافتراضية للقطات هي العربية، واللون الزمردي
shot() {
  # المسار بين علامتي اقتباس: صدفة الجهاز تقطع النص عند "&"
  adb shell am start -W -S -n $PKG/.MainActivity --es route "'$1'" --es lang "${3:-ar}" --es accent "${4:-emerald}" --ez demo true > /dev/null
  sleep "${WAIT:-8}"
  adb exec-out screencap -p > "$OUT/$2.png"
  # نصوص الشاشة من شجرة الوصول: تبيّن أي شاشة ظهرت فعلاً
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1 && adb exec-out cat /sdcard/ui.xml > "$OUT/$2.xml" || true
  echo "captured $2 ($(adb shell dumpsys window | grep -m1 mCurrentFocus | tr -d '\r'))"
}

# يضغط وسط أول عنصر يحمل هذا النص (من شجرة الوصول)
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  b=$(adb exec-out cat /sdcard/ui.xml | grep -o "text=\"$1\"[^>]*bounds=\"[^\"]*\"" | head -1 | grep -o 'bounds="[^"]*"' | grep -o '[0-9]\+')
  set -- $b
  [ $# -eq 4 ] && adb shell input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))
}

adb shell cmd uimode night no || true
shot home        01-home
shot prayer      02-prayer
shot qibla       03-qibla
shot tasbih      04-tasbih
shot zakat       05-zakat
shot inheritance 06-inheritance
shot hijri       07-hijri
shot events      08-events
shot age         09-age
shot settings    10-settings
shot adhkar      17-adhkar
shot "surah/2?ayah=255&from=focus" 18-focus-mode

adb shell cmd uimode night yes || true
sleep 2
shot home        11-home-dark
shot prayer      12-prayer-dark
shot inheritance 13-inheritance-dark

# القرآن الكريم: الفهرس وصفحة سورة
adb shell cmd uimode night no || true
sleep 2
shot quran          14-quran
shot "surah/18"     15-surah-kahf
# الضغط المطوّل على آية: شريط خياراتها، ثم التفسير في لوحته
adb shell input swipe 540 1250 540 1250 900
sleep 2
adb exec-out screencap -p > "$OUT/19-ayah-actions.png"; echo "captured 19-ayah-actions"
tap_text "التفسير"
sleep 3
adb exec-out screencap -p > "$OUT/19b-tafsir-panel.png"; echo "captured 19b-tafsir-panel"
# «استماع» من الشريط يفتح إعدادات التلاوة
adb shell input keyevent KEYCODE_BACK; sleep 1
adb shell input swipe 540 1250 540 1250 900; sleep 2
tap_text "استماع"
sleep 3
adb exec-out screencap -p > "$OUT/19c-listen-options.png"; echo "captured 19c-listen-options"
# مشاركة الآية صورة: لوحة البطاقة، ثم قائمة المشاركة بالصورة نفسها (PNG عبر FileProvider)
adb shell input keyevent KEYCODE_BACK; sleep 1
adb shell input swipe 540 1250 540 1250 900; sleep 2
tap_text "مشاركة"
sleep 4
adb exec-out screencap -p > "$OUT/19d-share-card.png"; echo "captured 19d-share-card"
tap_text "صورة"
sleep 5
adb exec-out screencap -p > "$OUT/19e-share-sheet.png"; echo "captured 19e-share-sheet"
adb shell input keyevent KEYCODE_BACK; sleep 1
# بالإنجليزية: البطاقة ومعنى الآية تحتها
shot "surah/2?ayah=255&from=row" 19f-surah-en en
adb shell input swipe 540 1250 540 1250 900; sleep 2
tap_text "Share"
sleep 4
adb exec-out screencap -p > "$OUT/19g-share-card-en.png"; echo "captured 19g-share-card-en"
adb shell input keyevent KEYCODE_BACK; sleep 1
shot quran          16-quran-en en

# ألوان التطبيق
adb shell cmd uimode night no || true
sleep 2
shot settings 70-accent-settings ar pink
shot home     71-accent-pink     ar pink
shot home     72-accent-purple   ar purple
shot home     73-accent-blue     ar blue
shot home     74-accent-honey    ar honey
shot tasbih   75-accent-maroon   ar maroon
adb shell cmd uimode night yes || true
sleep 2
shot prayer   76-accent-purple-dark ar purple
shot home     77-accent-teal-dark   ar teal

# لغات أخرى: الرئيسية والمواقيت والمسبحة
adb shell cmd uimode night no || true
sleep 2
n=20
for lang in en fr tr ur id ms fa bn ru de es uk uz kk; do
  shot home     "$n-$lang-home" "$lang"
  shot prayer   "$((n+1))-$lang-prayer" "$lang"
  shot tasbih   "$((n+2))-$lang-tasbih" "$lang"
  n=$((n+3))
done

ls -la "$OUT"
adb logcat -d -b crash > "$OUT/logcat-crash.txt" || true
adb logcat -d > "$OUT/logcat.txt" || true

# لقطات متطابقة كلها تعني أن الشاشة لم تُرسم: نُفشل الخطوة بدل نتيجة خضراء مضلِّلة
if [ "$(md5sum "$OUT"/*.png | awk '{print $1}' | sort -u | wc -l)" -le 1 ]; then
  echo "All screenshots are identical; the display was not captured." >&2
  cat "$OUT/logcat-crash.txt" >&2
  exit 1
fi
