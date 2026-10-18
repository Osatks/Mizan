#!/usr/bin/env bash
# يلتقط صوراً لشاشات التطبيق على المحاكي (يُشغَّل من GitHub Actions)
set -u
PKG=com.osatks.mizan
# اسم الملف يحمل رقم الإصدار (mizan-2.1.N-debug.apk)
APK=$(ls android-app/app/build/outputs/apk/debug/*.apk | head -1)
OUT=screenshots
mkdir -p "$OUT"

adb install -r "$APK"
adb logcat -c || true
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb shell settings put system screen_off_timeout 600000 || true
adb shell input keyevent KEYCODE_WAKEUP || true

# shot <route> <name> [lang]: اللغة الافتراضية للقطات هي العربية
shot() {
  # المسار بين علامتي اقتباس: صدفة الجهاز تقطع النص عند "&"
  adb shell am start -W -S -n $PKG/.MainActivity --es route "'$1'" --es lang "${3:-ar}" --ez demo true > /dev/null
  sleep 5
  adb exec-out screencap -p > "$OUT/$2.png"
  echo "captured $2 ($(adb shell dumpsys window | grep -m1 mCurrentFocus | tr -d '\r'))"
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
shot quran          16-quran-en en

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
