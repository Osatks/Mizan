/* ميزان — سكربت عام لكل الصفحات */
(function () {
  "use strict";

  /* ---------- أدوات مشتركة ---------- */
  const store = {
    get(k, d) { try { const v = localStorage.getItem("mizan:" + k); return v == null ? d : JSON.parse(v); } catch (e) { return d; } },
    set(k, v) { try { localStorage.setItem("mizan:" + k, JSON.stringify(v)); } catch (e) {} },
    del(k) { try { localStorage.removeItem("mizan:" + k); } catch (e) {} }
  };

  const nf = (opts) => new Intl.NumberFormat("ar-EG-u-nu-latn", opts);
  const fmt = (n, d = 2) => nf({ maximumFractionDigits: d, minimumFractionDigits: 0 }).format(n);
  const money = (n, cur) => fmt(n, 2) + (cur ? " " + cur : "");
  const num = (el) => {
    if (!el) return 0;
    const v = String(el.value || "").replace(/[٠-٩]/g, (d) => "٠١٢٣٤٥٦٧٨٩".indexOf(d)).replace(/[,،\s]/g, "").replace("٫", ".");
    const n = parseFloat(v);
    return isFinite(n) && n > 0 ? n : 0;
  };

  /* ---------- التقويم الهجري (أم القرى عبر Intl) ---------- */
  const HIJRI_MONTHS = ["محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"];
  const GREG_MONTHS = ["يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"];
  const WEEKDAYS = ["الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"];
  let hijriFmt = null;
  try {
    hijriFmt = new Intl.DateTimeFormat("en-u-ca-islamic-umalqura-nu-latn", { day: "numeric", month: "numeric", year: "numeric", timeZone: "UTC" });
    if (!/islamic/.test(hijriFmt.resolvedOptions().calendar)) hijriFmt = null;
  } catch (e) { hijriFmt = null; }

  // خوارزمية احتياطية (التقويم الهجري الحسابي) إن لم يدعم المتصفح أم القرى
  function jdFromDate(d) { return Math.floor(d.getTime() / 86400000) + 2440587.5; }
  function tabularFromJD(jd) {
    jd = Math.floor(jd) + 0.5;
    const y = Math.floor((30 * (jd - 1948439.5) + 10646) / 10631);
    const m = Math.min(12, Math.ceil((jd - (29 + tabularToJD(y, 1, 1))) / 29.5) + 1);
    const d = jd - tabularToJD(y, m, 1) + 1;
    return { y, m, d };
  }
  function tabularToJD(y, m, d) {
    return d + Math.ceil(29.5 * (m - 1)) + (y - 1) * 354 + Math.floor((3 + 11 * y) / 30) + 1948439.5 - 1;
  }

  /** يحوّل تاريخاً ميلادياً (UTC منتصف الليل) إلى هجري {y,m,d} */
  function toHijri(date) {
    const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    if (hijriFmt) {
      const parts = hijriFmt.formatToParts(d);
      const get = (t) => parseInt(parts.find((p) => p.type === t).value, 10);
      return { y: get("year"), m: get("month"), d: get("day") };
    }
    return tabularFromJD(jdFromDate(d));
  }

  /** يحوّل تاريخاً هجرياً إلى ميلادي (Date محلي) أو null إن كان اليوم غير موجود */
  function fromHijri(y, m, d) {
    // تقدير أولي ثم تصحيح بالبحث
    const approx = tabularToJD(y, m, d) - 2440587.5;
    let t = new Date(approx * 86400000);
    let guess = new Date(t.getUTCFullYear(), t.getUTCMonth(), t.getUTCDate());
    for (let i = 0; i < 6; i++) {
      const h = toHijri(guess);
      const diff = (y - h.y) * 354.37 + (m - h.m) * 29.53 + (d - h.d);
      const step = Math.round(diff);
      if (step === 0) break;
      guess = new Date(guess.getFullYear(), guess.getMonth(), guess.getDate() + step);
    }
    for (let k = -3; k <= 3; k++) {
      const c = new Date(guess.getFullYear(), guess.getMonth(), guess.getDate() + k);
      const h = toHijri(c);
      if (h.y === y && h.m === m && h.d === d) return c;
    }
    return null;
  }

  function hijriMonthLength(y, m) {
    const ny = m === 12 ? y + 1 : y, nm = m === 12 ? 1 : m + 1;
    const a = fromHijri(y, m, 1), b = fromHijri(ny, nm, 1);
    if (!a || !b) return 30;
    return Math.round((b - a) / 86400000);
  }

  const hijriStr = (h) => `${h.d} ${HIJRI_MONTHS[h.m - 1]} ${h.y} هـ`;
  const gregStr = (g) => `${g.getDate()} ${GREG_MONTHS[g.getMonth()]} ${g.getFullYear()} م`;

  function toast(msg) {
    let t = document.querySelector(".toast");
    if (!t) { t = document.createElement("div"); t.className = "toast"; t.setAttribute("role", "status"); document.body.appendChild(t); }
    t.textContent = msg;
    t.classList.add("show");
    clearTimeout(t._h);
    t._h = setTimeout(() => t.classList.remove("show"), 2200);
  }

  async function share(title, text) {
    const url = location.href;
    try {
      if (navigator.share) { await navigator.share({ title, text, url }); return; }
      await navigator.clipboard.writeText(text + "\n" + url);
      toast("تم نسخ النتيجة ✓");
    } catch (e) {}
  }

  window.Mizan = { store, fmt, money, num, toHijri, fromHijri, hijriMonthLength, hijriStr, gregStr, HIJRI_MONTHS, GREG_MONTHS, WEEKDAYS, toast, share };

  /* ---------- الواجهة ---------- */
  const root = document.documentElement;
  document.querySelector(".theme-toggle")?.addEventListener("click", () => {
    const dark = root.dataset.theme ? root.dataset.theme === "dark" : matchMedia("(prefers-color-scheme: dark)").matches;
    root.dataset.theme = dark ? "light" : "dark";
    try { localStorage.setItem("theme", root.dataset.theme); } catch (e) {}
  });

  const menuBtn = document.querySelector(".menu-toggle");
  menuBtn?.addEventListener("click", () => {
    const open = document.body.classList.toggle("nav-open");
    menuBtn.setAttribute("aria-expanded", open);
  });

  const more = document.querySelector(".nav-more");
  const moreBtn = more?.querySelector(".nav-more-btn");
  moreBtn?.addEventListener("click", (e) => {
    e.stopPropagation();
    const open = more.classList.toggle("open");
    moreBtn.setAttribute("aria-expanded", open);
  });
  document.addEventListener("click", (e) => {
    if (more && !more.contains(e.target)) { more.classList.remove("open"); moreBtn?.setAttribute("aria-expanded", "false"); }
  });
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") { more?.classList.remove("open"); document.body.classList.remove("nav-open"); }
  });

  const toTop = document.querySelector(".to-top");
  addEventListener("scroll", () => toTop?.classList.toggle("show", scrollY > 600), { passive: true });
  toTop?.addEventListener("click", () => scrollTo({ top: 0 }));

  // ظهور تدريجي للعناصر
  const revealEls = document.querySelectorAll(".tool-card, .article-card, .prose.card, .feature, .faq");
  if ("IntersectionObserver" in window) {
    const io = new IntersectionObserver((entries) => {
      entries.forEach((en) => { if (en.isIntersecting) { en.target.classList.add("in"); io.unobserve(en.target); } });
    }, { rootMargin: "0px 0px -40px 0px" });
    revealEls.forEach((el) => { el.classList.add("reveal"); io.observe(el); });
  }

  // نموذج الاتصال: يفتح برنامج البريد
  const cf = document.getElementById("contact-form");
  cf?.addEventListener("submit", (e) => {
    e.preventDefault();
    const status = cf.querySelector(".form-status");
    const name = cf.name.value.trim(), email = cf.email.value.trim(), msg = cf.message.value.trim();
    if (!name || !/^\S+@\S+\.\S+$/.test(email) || msg.length < 10) {
      status.textContent = "يرجى تعبئة الاسم وبريد صحيح ورسالة لا تقل عن 10 أحرف.";
      status.style.color = "var(--danger)";
      return;
    }
    const to = (document.querySelector("a[href^='mailto:']")?.getAttribute("href") || "mailto:").replace("mailto:", "").split("?")[0];
    const subject = encodeURIComponent(cf.subject.value + " - " + name);
    const body = encodeURIComponent(msg + "\n\n—\n" + name + "\n" + email);
    location.href = `mailto:${to}?subject=${subject}&body=${body}`;
    status.textContent = "تم فتح برنامج البريد لديك لإرسال الرسالة. شكراً لتواصلك!";
    status.style.color = "var(--primary)";
  });

  // تطبيق الويب: عامل الخدمة للعمل دون إنترنت + زر التثبيت
  if ("serviceWorker" in navigator && (location.protocol === "https:" || location.hostname === "localhost")) {
    addEventListener("load", () => navigator.serviceWorker.register("/sw.js").catch(() => {}));
  }
  let deferredPrompt = null;
  const installBtn = document.querySelector(".install-btn");
  addEventListener("beforeinstallprompt", (e) => {
    e.preventDefault();
    deferredPrompt = e;
    if (installBtn) installBtn.hidden = false;
  });
  installBtn?.addEventListener("click", async () => {
    if (!deferredPrompt) return;
    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;
    if (outcome === "accepted") { installBtn.hidden = true; toast("تم تثبيت التطبيق ✓"); }
    deferredPrompt = null;
  });
  addEventListener("appinstalled", () => { if (installBtn) installBtn.hidden = true; });
  addEventListener("offline", () => toast("أنت غير متصل — التطبيق يعمل من النسخة المحفوظة"));

  // أزرار + / − في حقول العدد
  document.addEventListener("click", (e) => {
    const b = e.target.closest(".stepper [data-step]");
    if (!b) return;
    const inp = b.parentElement.querySelector("input");
    const min = inp.min === "" ? -Infinity : +inp.min, max = inp.max === "" ? Infinity : +inp.max;
    inp.value = Math.min(max, Math.max(min, (parseInt(inp.value, 10) || 0) + +b.dataset.step));
    inp.dispatchEvent(new Event("input", { bubbles: true }));
  });

  // التاريخ الهجري في أي عنصر data-today-hijri
  document.querySelectorAll("[data-today-hijri]").forEach((el) => { el.textContent = hijriStr(toHijri(new Date())); });
  document.querySelectorAll("[data-today-greg]").forEach((el) => {
    const d = new Date(); el.textContent = WEEKDAYS[d.getDay()] + " " + gregStr(d);
  });
})();
