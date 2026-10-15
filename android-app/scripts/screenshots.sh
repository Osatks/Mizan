#!/usr/bin/env bash
# يلتقط صوراً لشاشات التطبيق على المحاكي (يُشغَّل من GitHub Actions)
set -u
PKG=com.osatks.mizan
APK=android-app/app/build/outputs/apk/debug/app-debug.apk
OUT=screenshots
mkdir -p "$OUT"

adb install -r "$APK"
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb shell settings put system screen_off_timeout 600000 || true
adb shell input keyevent KEYCODE_WAKEUP || true

# shot <route> <name> [lang]: اللغة الافتراضية للقطات هي العربية
shot() {
  adb shell am start -W -S -n $PKG/.MainActivity --es route "$1" --es lang "${3:-ar}" --ez demo true > /dev/null
  sleep 5
  adb exec-out screencap -p > "$OUT/$2.png"
  echo "captured $2"
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

adb shell cmd uimode night yes || true
sleep 2
shot home        11-home-dark
shot prayer      12-prayer-dark
shot inheritance 13-inheritance-dark

# لغات أخرى: الرئيسية والمواقيت والمسبحة
adb shell cmd uimode night no || true
sleep 2
n=20
for lang in en fr tr ur id ms fa bn ru de; do
  shot home     "$n-$lang-home" "$lang"
  shot prayer   "$((n+1))-$lang-prayer" "$lang"
  shot tasbih   "$((n+2))-$lang-tasbih" "$lang"
  n=$((n+3))
done

ls -la "$OUT"
