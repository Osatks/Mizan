// صفحة التطبيق: تُبرز زر المتجر المناسب لجهاز الزائر
(function () {
  var ua = navigator.userAgent || "";
  var ios = /iPhone|iPad|iPod/.test(ua) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
  var android = /Android/i.test(ua);
  var note = document.querySelector(".device-note");
  var root = document.documentElement;
  if (android) {
    root.classList.add("is-android");
    if (note) { note.textContent = "جهازك يعمل بنظام Android: اضغط زر Google Play للتثبيت."; note.hidden = false; }
  } else if (ios) {
    root.classList.add("is-ios");
    if (note) { note.textContent = "نسخة iPhone وiPad قيد التطوير وستصدر قريبًا على App Store. يمكنك الآن استخدام أدوات ميزان من المتصفح."; note.hidden = false; }
  }
})();
