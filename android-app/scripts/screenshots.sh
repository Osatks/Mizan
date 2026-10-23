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

# فرع التقاط فقط: شاشات صور المتجر بالروسية (لا يُدمج)
adb shell cmd uimode night no || true
shot home        01-ru-home        ru
shot prayer      02-ru-prayer      ru
shot qibla       03-ru-qibla       ru
shot tasbih      04-ru-tasbih      ru
shot zakat       05-ru-zakat       ru
shot inheritance 06-ru-inheritance ru
shot quran       14-ru-quran       ru
shot adhkar      17-ru-adhkar      ru
shot "surah/18"  15-ru-surah-kahf  ru
# الضغط المطوّل على آية: شريط خياراتها، ثم التفسير في لوحته
adb shell input swipe 540 1250 540 1250 900
sleep 2
adb exec-out screencap -p > "$OUT/19-ru-ayah-actions.png"; echo "captured 19-ru-ayah-actions"
tap_text "Тафсир"
sleep 3
adb exec-out screencap -p > "$OUT/19b-ru-tafsir-panel.png"; echo "captured 19b-ru-tafsir-panel"

ls -la "$OUT"
adb logcat -d -b crash > "$OUT/logcat-crash.txt" || true
adb logcat -d > "$OUT/logcat.txt" || true

# لقطات متطابقة كلها تعني أن الشاشة لم تُرسم: نُفشل الخطوة بدل نتيجة خضراء مضلِّلة
if [ "$(md5sum "$OUT"/*.png | awk '{print $1}' | sort -u | wc -l)" -le 1 ]; then
  echo "All screenshots are identical; the display was not captured." >&2
  cat "$OUT/logcat-crash.txt" >&2
  exit 1
fi
