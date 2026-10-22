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
  # معرّف التطبيق com.mizan.almuslim لكن صنف النشاط بقي في com.osatks.mizan، فنكتب اسمه كاملاً
  adb shell am start -W -S -n $PKG/com.osatks.mizan.MainActivity --es route "'$1'" --es lang "${3:-ar}" --es accent "${4:-emerald}" --ez demo true > /dev/null
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

# صور المتجر بالإنجليزية: كل الشاشات المعروضة، والتفسير من شريط خيارات الآية
adb shell cmd uimode night no || true
sleep 2
shot home        80-en-home        en
shot prayer      81-en-prayer      en
shot qibla       82-en-qibla       en
shot zakat       83-en-zakat       en
shot inheritance 84-en-inheritance en
shot adhkar      85-en-adhkar      en
shot quran       86-en-quran       en
shot "surah/18"  87-en-surah-kahf  en
adb shell input swipe 540 1250 540 1250 900
sleep 2
adb exec-out screencap -p > "$OUT/88-en-ayah-actions.png"; echo "captured 88-en-ayah-actions"
tap_text "Tafsir"
sleep 3
adb exec-out screencap -p > "$OUT/89-en-tafsir-panel.png"; echo "captured 89-en-tafsir-panel"

ls -la "$OUT"
adb logcat -d -b crash > "$OUT/logcat-crash.txt" || true
adb logcat -d > "$OUT/logcat.txt" || true

# لقطات متطابقة كلها تعني أن الشاشة لم تُرسم: نُفشل الخطوة بدل نتيجة خضراء مضلِّلة
if [ "$(md5sum "$OUT"/*.png | awk '{print $1}' | sort -u | wc -l)" -le 1 ]; then
  echo "All screenshots are identical; the display was not captured." >&2
  cat "$OUT/logcat-crash.txt" >&2
  exit 1
fi
