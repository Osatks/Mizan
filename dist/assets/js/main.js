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

  /* ---------- أرقام متدحرجة (مطابقة لـ RollingText في التطبيق) ---------- */
  const reducedMotion = () => matchMedia("(prefers-reduced-motion: reduce)").matches;
  const NUM_RE = /^[0-9.,:%°\s+\-]+$/;
  let inRoll = false;

  function roll(el, nextText) {
    if (!el || inRoll) return;
    const prev = el._rollPrev;
    const next = String(nextText != null ? nextText : el.textContent || "");
    if (prev === next) return;
    el._rollPrev = next;
    if (prev == null || reducedMotion() || !next || !NUM_RE.test(next) || !NUM_RE.test(prev)) {
      if (prev != null && !reducedMotion()) { el.classList.remove("swap"); void el.offsetWidth; el.classList.add("swap"); }
      return;
    }
    inRoll = true;
    try {
      const n = next.length, pLen = prev.length;
      const wrap = document.createElement("span");
      wrap.className = "roll";
      for (let i = 0; i < n; i++) {
        const ch = next[i];
        const pi = pLen - (n - i); // مطابقة الخانات من اليمين (الآحاد مع الآحاد)
        const pch = pi >= 0 ? prev[pi] : "";
        const s = document.createElement("span");
        const inner = document.createElement("i");
        inner.textContent = ch;
        if (pch && pch !== ch && /\d/.test(ch) && /\d/.test(pch)) {
          const up = ch > pch || (pch === "9" && ch === "0");
          s.className = "rd " + (up ? "up" : "dn");
          s.setAttribute("data-p", pch);
        } else {
          s.className = "rd";
        }
        s.appendChild(inner);
        wrap.appendChild(s);
      }
      el.textContent = "";
      el.appendChild(wrap);
    } finally {
      inRoll = false;
    }
  }

  window.Mizan = { store, fmt, money, num, toHijri, fromHijri, hijriMonthLength, hijriStr, gregStr, HIJRI_MONTHS, GREG_MONTHS, WEEKDAYS, toast, share, roll };

  /* ---------- المظهر ولون التطبيق (مطابق لـ Theme.kt و SettingsScreen.kt) ---------- */
  const root = document.documentElement;
  const ap = document.getElementById("appearance");
  const apBtn = document.querySelector(".ap-open, .theme-toggle");
  const metaTheme = document.querySelector('meta[name="theme-color"]');

  function syncThemeColor() {
    if (!metaTheme) return;
    const c = getComputedStyle(root).getPropertyValue("--hero-1").trim();
    if (c) metaTheme.setAttribute("content", c);
  }

  function currentMode() {
    try {
      const t = localStorage.getItem("theme");
      return t === "light" || t === "dark" ? t : "system";
    } catch (e) { return "system"; }
  }

  function currentAccent() {
    try { return localStorage.getItem("accent") || root.dataset.accent || "emerald"; }
    catch (e) { return root.dataset.accent || "emerald"; }
  }

  function syncAppearanceUI() {
    if (!ap) return;
    const mode = currentMode();
    const acc = currentAccent();
    ap.querySelectorAll("[data-mode]").forEach((b) => {
      const on = b.dataset.mode === mode;
      b.setAttribute("aria-checked", on);
      b.classList.toggle("active", on);
    });
    let activeName = "زمردي";
    ap.querySelectorAll(".sw[data-accent]").forEach((b) => {
      const on = b.dataset.accent === acc;
      b.setAttribute("aria-checked", on);
      if (on && b.dataset.name) activeName = b.dataset.name;
    });
    const nameEl = ap.querySelector("[data-accent-name]");
    if (nameEl) nameEl.textContent = activeName;
    syncThemeColor();
  }

  function animateThemeChange(originEl, mutateFn) {
    if (document.startViewTransition && !reducedMotion() && originEl) {
      const r = originEl.getBoundingClientRect();
      const x = r.left + r.width / 2, y = r.top + r.height / 2;
      const maxR = Math.hypot(Math.max(x, innerWidth - x), Math.max(y, innerHeight - y));
      root.classList.add("theme-vt");
      const vt = document.startViewTransition(() => { mutateFn(); syncAppearanceUI(); });
      vt.ready.then(() => {
        root.animate(
          { clipPath: [`circle(0px at ${x}px ${y}px)`, `circle(${maxR}px at ${x}px ${y}px)`] },
          { duration: 420, easing: "cubic-bezier(.2, 0, 0, 1)", pseudoElement: "::view-transition-new(root)" }
        );
      }).catch(() => {});
      vt.finished.finally(() => root.classList.remove("theme-vt"));
      return;
    }
    root.classList.add("theme-anim");
    mutateFn();
    syncAppearanceUI();
    clearTimeout(root._ta);
    root._ta = setTimeout(() => root.classList.remove("theme-anim"), 360);
  }

  function openAppearance() {
    if (!ap) return;
    syncAppearanceUI();
    ap.hidden = false;
    requestAnimationFrame(() => ap.classList.add("open"));
  }

  function closeAppearance() {
    if (!ap || ap.hidden) return;
    ap.classList.remove("open");
    setTimeout(() => { if (!ap.classList.contains("open")) ap.hidden = true; }, 320);
  }

  apBtn?.addEventListener("click", (e) => {
    if (e.shiftKey || !ap) {
      const dark = root.dataset.theme ? root.dataset.theme === "dark" : matchMedia("(prefers-color-scheme: dark)").matches;
      animateThemeChange(apBtn, () => {
        root.dataset.theme = dark ? "light" : "dark";
        try { localStorage.setItem("theme", root.dataset.theme); } catch (err) {}
      });
      return;
    }
    if (ap.classList.contains("open")) closeAppearance(); else openAppearance();
  });

  ap?.addEventListener("click", (e) => {
    if (e.target.closest("[data-ap-close]")) { closeAppearance(); return; }
    const mb = e.target.closest("[data-mode]");
    if (mb) {
      const m = mb.dataset.mode;
      navigator.vibrate?.(8);
      animateThemeChange(mb, () => {
        if (m === "system") { delete root.dataset.theme; try { localStorage.removeItem("theme"); } catch (err) {} }
        else { root.dataset.theme = m; try { localStorage.setItem("theme", m); } catch (err) {} }
      });
      return;
    }
    const sw = e.target.closest(".sw[data-accent]");
    if (sw) {
      const a = sw.dataset.accent;
      navigator.vibrate?.(10);
      sw.classList.remove("picked"); void sw.offsetWidth; sw.classList.add("picked");
      animateThemeChange(sw, () => {
        if (a === "emerald") { delete root.dataset.accent; try { localStorage.removeItem("accent"); } catch (err) {} }
        else { root.dataset.accent = a; try { localStorage.setItem("accent", a); } catch (err) {} }
      });
    }
  });

  syncAppearanceUI();
  matchMedia("(prefers-color-scheme: dark)").addEventListener?.("change", syncThemeColor);

  /* ---------- القوائم والتنقل ---------- */
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
    if (e.key === "Escape") { more?.classList.remove("open"); document.body.classList.remove("nav-open"); closeAppearance(); }
  });

  // اهتزاز خفيف عند لمس شريط التبويبات السفلي أو الشرائح
  document.querySelector(".app-tabbar")?.addEventListener("click", (e) => {
    const a = e.target.closest("a");
    if (a && a.getAttribute("aria-current") !== "page") navigator.vibrate?.(8);
  });

  // انتقالات الصفحات المشتركة (Shared Card -> Hero كما في Motion.kt و App.kt)
  document.addEventListener("click", (e) => {
    const card = e.target.closest(".tool-card[href]");
    if (card) {
      document.querySelectorAll(".tool-card").forEach((c) => (c.style.viewTransitionName = ""));
      card.style.viewTransitionName = "tool-hero";
      try { sessionStorage.setItem("mizan:vt", "card:" + (card.dataset.tool || card.getAttribute("href"))); } catch (err) {}
      return;
    }
    const tab = e.target.closest(".app-tabbar a[href]");
    if (tab) {
      try { sessionStorage.setItem("mizan:vt", "tab"); } catch (err) {}
      return;
    }
    const back = e.target.closest(".breadcrumb a[href], .brand[href]");
    if (back) {
      try { sessionStorage.setItem("mizan:vt", "back"); } catch (err) {}
    }
  });

  addEventListener("pageswap", (e) => {
    if (!e.viewTransition) return;
    try {
      const vt = sessionStorage.getItem("mizan:vt") || "";
      root.classList.toggle("vt-tab", vt === "tab");
      root.classList.toggle("vt-back", vt === "back");
    } catch (err) {}
  });

  addEventListener("pagereveal", (e) => {
    if (!e.viewTransition) return;
    try {
      const vt = sessionStorage.getItem("mizan:vt") || "";
      sessionStorage.removeItem("mizan:vt");
      root.classList.toggle("vt-tab", vt === "tab");
      root.classList.toggle("vt-back", vt === "back" || (location.pathname === "/" && !vt));
      if (location.pathname === "/" && vt.startsWith("card:")) {
        const key = vt.slice(5);
        const card = document.querySelector(`.tool-card[data-tool="${key}"], .tool-card[href="${key}"]`);
        if (card) card.style.viewTransitionName = "tool-hero";
      }
      e.viewTransition.finished.finally(() => {
        root.classList.remove("vt-tab", "vt-back");
        document.querySelectorAll(".tool-card").forEach((c) => (c.style.viewTransitionName = ""));
      });
    } catch (err) {}
  });

  const toTop = document.querySelector(".to-top");
  addEventListener("scroll", () => toTop?.classList.toggle("show", scrollY > 600), { passive: true });
  toTop?.addEventListener("click", () => scrollTo({ top: 0 }));

  /* ---------- ظهور متتابع لعناصر الصفحة (RevealState في Motion.kt) ---------- */
  // العناصر المرئية خلال أول 450ms تظهر متتابعة بفارق 40ms (حتى 10 عناصر)، وما يظهر لاحقاً بالتمرير يظهر فوراً
  const pageStart = performance.now();
  let revealIdx = 0;
  const revealEls = document.querySelectorAll(".tool-card, .article-card, .prose.card, .feature, .faq, .tool-layout > .card");
  if ("IntersectionObserver" in window && !reducedMotion()) {
    const io = new IntersectionObserver((entries) => {
      const initial = performance.now() - pageStart < 450;
      entries.forEach((en) => {
        if (!en.isIntersecting) return;
        const el = en.target;
        if (initial && revealIdx < 10) {
          el.style.setProperty("--d", `${revealIdx * 40}ms`);
          revealIdx++;
        } else {
          el.classList.add("quick");
        }
        el.classList.add("in");
        io.unobserve(el);
      });
    }, { rootMargin: "0px 0px -32px 0px" });
    revealEls.forEach((el) => { el.classList.add("reveal"); io.observe(el); });
  } else {
    revealEls.forEach((el) => el.classList.add("in"));
  }

  /* ---------- مراقبة الأرقام المتدحرجة في الأدوات والعدّادات ---------- */
  const rollTargets = document.querySelectorAll(".tasbih-btn .count, .prayer-next .countdown, .tile b");
  if ("MutationObserver" in window && rollTargets.length && !reducedMotion()) {
    rollTargets.forEach((el) => {
      el._rollPrev = el.textContent || "";
      const mo = new MutationObserver(() => { if (!inRoll) roll(el, el.textContent); });
      mo.observe(el, { childList: true, characterData: true });
    });
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
    navigator.vibrate?.(6);
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
