/* عامل الخدمة لتطبيق ميزان — يُولَّد تلقائياً بواسطة build.py */
const VERSION = "mizan-2d85c2f800";
const PRECACHE = ["/", "/404.html", "/about/", "/age-calculator/", "/app/", "/articles/after-prayer-adhkar/", "/articles/faraid-basics/", "/articles/gold-jewelry-zakat/", "/articles/hijri-months/", "/articles/", "/articles/prayer-times-calculation/", "/articles/zakat-nisab/", "/contact/", "/disclaimer/", "/fidya-kaffarah/", "/hijri-converter/", "/inheritance-calculator/", "/islamic-events/", "/prayer-times/", "/privacy-policy/", "/qibla-direction/", "/tasbih/", "/terms/", "/zakat-al-fitr/", "/zakat-calculator/", "/assets/css/style.css", "/assets/img/app/actions.webp", "/assets/img/app/adhkar.webp", "/assets/img/app/colors.webp", "/assets/img/app/dark.webp", "/assets/img/app/focus.webp", "/assets/img/app/home.webp", "/assets/img/app/inheritance.webp", "/assets/img/app/listen.webp", "/assets/img/app/mushaf.webp", "/assets/img/app/play-qr.svg", "/assets/img/app/prayer.webp", "/assets/img/app/qibla.webp", "/assets/img/app/quran.webp", "/assets/img/app/tafsir.webp", "/assets/img/app/tasbih.webp", "/assets/img/favicon.svg", "/assets/img/icon-180.png", "/assets/img/icon-192.png", "/assets/img/icon-512.png", "/assets/img/icon-maskable-512.png", "/assets/js/age.js", "/assets/js/app.js", "/assets/js/events.js", "/assets/js/fidya.js", "/assets/js/fitr.js", "/assets/js/hijri.js", "/assets/js/home.js", "/assets/js/inheritance.js", "/assets/js/main.js", "/assets/js/prayer.js", "/assets/js/praytimes.js", "/assets/js/qibla.js", "/assets/js/tasbih.js", "/assets/js/zakat.js"];
const FONT_CACHE = "mizan-fonts";

self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(VERSION).then((c) => c.addAll(PRECACHE)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== VERSION && k !== FONT_CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (e) => {
  const req = e.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);

  // خطوط Google: من التخزين أولاً
  if (url.hostname === "fonts.googleapis.com" || url.hostname === "fonts.gstatic.com") {
    e.respondWith(
      caches.open(FONT_CACHE).then((c) =>
        c.match(req).then((hit) => hit || fetch(req).then((res) => { c.put(req, res.clone()); return res; }))
      )
    );
    return;
  }
  if (url.origin !== location.origin) return; // الإعلانات والتحليلات تمر مباشرة

  // الصفحات: الشبكة أولاً (لأحدث محتوى) ثم النسخة المخزنة عند انقطاع الإنترنت
  if (req.mode === "navigate") {
    e.respondWith(
      fetch(req)
        .then((res) => {
          if (res.ok) { const copy = res.clone(); caches.open(VERSION).then((c) => c.put(req, copy)); }
          return res;
        })
        .catch(() =>
          caches.match(req, { ignoreSearch: true })
            .then((hit) => hit || caches.match(url.pathname.endsWith("/") ? url.pathname : url.pathname + "/"))
            .then((hit) => hit || caches.match("/404.html"))
        )
    );
    return;
  }

  // الملفات الثابتة: من التخزين فوراً مع تحديثها في الخلفية
  e.respondWith(
    caches.open(VERSION).then((c) =>
      c.match(req, { ignoreSearch: true }).then((hit) => {
        const net = fetch(req).then((res) => { if (res.ok) c.put(req, res.clone()); return res; }).catch(() => hit);
        return hit || net;
      })
    )
  );
});
