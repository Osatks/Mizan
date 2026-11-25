#!/usr/bin/env bash
# يلتقط صوراً لشاشات التطبيق على المحاكي (يُشغَّل من GitHub Actions)
set -u
PKG=com.mizan.almuslim
# وصف زر إعداد العرض في صفحة السورة بالعربية (s.displayTitle)
DISPLAY_DESC="العرض"
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

# shot <route> <name> [lang] [accent] [tone]: اللغة الافتراضية للقطات هي العربية، واللون الزمردي، ولون صفحة المصحف حسب المظهر.
# النشاط باسمه الكامل: معرّف التطبيق com.mizan.almuslim و MainActivity في الحزمة com.osatks.mizan
shot() {
  # المسار بين علامتي اقتباس: صدفة الجهاز تقطع النص عند "&"
  adb shell am start -W -S -n $PKG/com.osatks.mizan.MainActivity --es route "'$1'" --es lang "${3:-ar}" --es accent "${4:-emerald}" --es tone "${5:-}" --ez demo true > /dev/null
  sleep "${WAIT:-8}"
  adb exec-out screencap -p > "$OUT/$2.png"
  # نصوص الشاشة من شجرة الوصول: تبيّن أي شاشة ظهرت فعلاً
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1 && adb exec-out cat /sdcard/ui.xml > "$OUT/$2.xml" || true
  echo "captured $2 ($(adb shell dumpsys window | grep -m1 mCurrentFocus | tr -d '\r'))"
}

# يضغط وسط أول عنصر يحمل هذا الوصف (content-desc) من شجرة الوصول
tap_desc() {
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  b=$(adb exec-out cat /sdcard/ui.xml | grep -o "content-desc=\"$1\"[^>]*bounds=\"[^\"]*\"" | head -1 | grep -o 'bounds="[^"]*"' | grep -o '[0-9]\+')
  set -- $b
  [ $# -eq 4 ] && adb shell input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))
}

# يضغط وسط أول عنصر يحمل هذا النص (من شجرة الوصول)
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  b=$(adb exec-out cat /sdcard/ui.xml | grep -o "text=\"$1\"[^>]*bounds=\"[^\"]*\"" | head -1 | grep -o 'bounds="[^"]*"' | grep -o '[0-9]\+')
  set -- $b
  [ $# -eq 4 ] && adb shell input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))
}

# ONLY=new: لقطات ميزات 2.5.0 وحدها (Actions ← Android Screenshots ← only=new)
swipe_up() { adb shell input swipe 540 1900 540 700 500; sleep 2; }
cap() { adb exec-out screencap -p > "$OUT/$1.png"; echo "captured $1"; }
if [ "${ONLY:-}" = "new" ]; then
  adb shell cmd uimode night no || true
  # الترحيب التفاعلي: أول تشغيل بعد مسح بيانات التطبيق
  adb shell pm clear $PKG > /dev/null
  adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
  adb shell am start -W -n $PKG/com.osatks.mizan.MainActivity --es lang ar > /dev/null
  sleep 10; cap new-01-welcome-language
  tap_text "التالي"; sleep 4; cap new-02-welcome-location
  tap_text "التالي"; sleep 4; cap new-03-welcome-adhan
  tap_text "التالي"; sleep 4; cap new-04-welcome-style
  tap_text "التالي"; sleep 4; cap new-05-welcome-try
  for i in 1 2 3; do adb shell input tap 540 1200; sleep 1; done; sleep 2; cap new-06-welcome-try-done
  tap_text "التالي"; sleep 4; cap new-07-welcome-done
  shot tools        new-10-tools
  WAIT=16 shot mosques new-11-mosques
  shot fasting      new-12-fasting
  swipe_up; cap new-13-fasting-sunnah
  shot kids         new-14-kids
  swipe_up; cap new-15-kids-more
  shot kids         new-16a-kids-open
  tap_text "الإخلاص"; sleep 6; cap new-16-kids-surah
  shot adhan        new-17-adhan
  swipe_up; swipe_up; cap new-18-adhan-end-alerts
  shot settings     new-19-settings
  swipe_up; cap new-20-settings-backup
  swipe_up; cap new-21-settings-more
  shot downloads    new-22-downloads
  shot tafsirs      new-23-tafsir-library
  shot fasting      new-30-fasting-en en
  shot kids         new-31-kids-en en
  WAIT=16 shot mosques new-32-mosques-en en
  shot settings     new-33-settings-en en
  swipe_up; cap new-34-settings-backup-en
  adb shell cmd uimode night yes || true
  sleep 2
  shot fasting      new-40-fasting-dark
  shot kids         new-41-kids-dark
  adb logcat -d -b crash > "$OUT/logcat-crash.txt" || true
  ls -la "$OUT"
  exit 0
fi

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
# النمل (ص 377): «طسٓ» منفصلة عن «تِلۡكَ» والفراغ بين الكلمات، ثم الوضع ثلاثي الأبعاد (ظل الكلمات على الورقة)
shot "surah/27"     15b-naml-377
shot "surah/27"     15c-naml-377-depth ar emerald depth
shot "surah/27"     15d-naml-377-depth-night ar emerald depthNight
# إعداد العرض: ألوان الصفحة صفحاتٍ مصغّرة (زر الإعداد في رأس صفحة السورة)
tap_desc "$DISPLAY_DESC"; sleep 3
adb exec-out screencap -p > "$OUT/15e-display-options.png"; echo "captured 15e-display-options"
adb shell input keyevent KEYCODE_BACK; sleep 1
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
tap_text "قصة"
sleep 3
adb exec-out screencap -p > "$OUT/19h-share-story.png"; echo "captured 19h-share-story"
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

# صور Google Play (تُؤطَّر بعدها بـ iosApp/fastlane/frame/frame.py بمقاس 1080×1920): بالعربية والإنجليزية
adb shell cmd uimode night no || true; sleep 2
for lang in ar en; do
  WAIT=10 shot home                           "store-$lang-01_home"       "$lang"
  WAIT=16 shot "surah/18?ayah=0&from=row"     "store-$lang-02_quran_page" "$lang"
  WAIT=10 shot hadith                         "store-$lang-03_hadith"     "$lang"
  WAIT=10 shot prayer                         "store-$lang-04_prayer"     "$lang"
  WAIT=10 shot adhkar                         "store-$lang-05_adhkar"     "$lang"
  WAIT=10 shot tools                          "store-$lang-06_tools"      "$lang"
  WAIT=16 shot "surah/2?ayah=255&from=tafsir" "store-$lang-07_tafsir"     "$lang"
  WAIT=10 shot qibla                          "store-$lang-08_qibla"      "$lang"
done

# مصحف المدينة: الفاتحة، وأول البقرة، وأول المائدة (خط الصفحة يُنزَّل عند فتحها)
WAIT=14 shot "surah/1"          21-madinah-fatiha
WAIT=14 shot "surah/2"          21b-madinah-baqara
WAIT=14 shot "surah/5?ayah=1"   21c-madinah-maidah
adb shell cmd uimode night yes || true; sleep 2
WAIT=14 shot "surah/2"          21d-madinah-baqara-dark
adb shell cmd uimode night no || true; sleep 2
shot tools       22-tools

# حديث اليوم: الحديث وشرحه وفوائده، ثم صورة القصة
shot hadith      20-hadith
adb shell input swipe 540 1700 540 500 400; sleep 2
adb exec-out screencap -p > "$OUT/20b-hadith-benefits.png"; echo "captured 20b-hadith-benefits"
adb shell input swipe 540 1700 540 300 300; sleep 2
tap_text "مشاركة صورة"
sleep 4
adb exec-out screencap -p > "$OUT/20c-hadith-story.png"; echo "captured 20c-hadith-story"
adb shell input keyevent KEYCODE_BACK; sleep 1
shot hadith      20d-hadith-en en
shot home        20e-home-scrolled
adb shell input swipe 540 1800 540 400 500; sleep 2
adb exec-out screencap -p > "$OUT/20f-home-cards.png"; echo "captured 20f-home-cards"

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
