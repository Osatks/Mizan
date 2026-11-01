// صفحة التطبيق: تُبرز زر المتجر المناسب لجهاز الزائر
(function () {
  var ua = navigator.userAgent || "";
  var ios = /iPhone|iPad|iPod/.test(ua) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
  var android = /Android/i.test(ua);
  var note = document.querySelector(".device-note");
  var root = document.documentElement;
  if (android) {
    root.classList.add("is-android");
    if (note) { note.textContent = t("جهازك يعمل بنظام Android: اضغط زر Google Play للتثبيت."); note.hidden = false; }
  } else if (ios) {
    root.classList.add("is-ios");
    if (note) { note.textContent = t("نسخة iPhone وiPad قيد التطوير وستصدر قريبًا على App Store. يمكنك الآن استخدام أدوات ميزان من المتصفح."); note.hidden = false; }
  }
})();

// فيديو التطبيق: صورة وزر تشغيل، ولا يُحمَّل شيء من YouTube قبل الضغط (youtube-nocookie)
(function () {
  "use strict";
  document.querySelectorAll(".video-facade[data-yt]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const f = document.createElement("iframe");
      f.src = `https://www.youtube-nocookie.com/embed/${btn.dataset.yt}?autoplay=1&playsinline=1&rel=0`;
      f.title = btn.getAttribute("aria-label") || "";
      f.allow = "autoplay; encrypted-media; picture-in-picture; fullscreen";
      f.allowFullscreen = true;
      f.className = "video-frame";
      btn.replaceWith(f);
    }, { once: true });
  });
})();
