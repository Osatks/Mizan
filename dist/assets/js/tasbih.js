(function () {
  "use strict";
  const M = window.Mizan;
  const $ = (id) => document.getElementById(id);
  if (!$("tasbih-app")) return;

  const DHIKR = [
    { t: "سُبْحَانَ اللَّهِ", s: "سبحان الله" },
    { t: "الْحَمْدُ لِلَّهِ", s: "الحمد لله" },
    { t: "اللَّهُ أَكْبَرُ", s: "الله أكبر" },
    { t: "لَا إِلَهَ إِلَّا اللَّهُ", s: "لا إله إلا الله" },
    { t: "أَسْتَغْفِرُ اللَّهَ", s: "أستغفر الله" },
    { t: "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", s: "سبحان الله وبحمده" },
    { t: "سُبْحَانَ اللَّهِ الْعَظِيمِ", s: "سبحان الله العظيم" },
    { t: "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", s: "لا حول ولا قوة إلا بالله" },
    { t: "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ", s: "الصلاة على النبي ﷺ" },
  ];
  const SALAH = [{ i: 0, n: 33 }, { i: 1, n: 33 }, { i: 2, n: 33 }, { t: "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ", s: "تمام المائة", n: 1 }];

  const todayKey = new Date().toISOString().slice(0, 10);
  const st = Object.assign({ mode: "free", idx: 0, count: 0, target: 33, rounds: 0, total: 0, step: 0, day: todayKey, log: {}, vibe: true }, M.store.get("tasbih", {}));
  if (st.day !== todayKey) { st.day = todayKey; st.log = {}; }

  const save = () => M.store.set("tasbih", st);
  const C = 2 * Math.PI * 47;

  function current() {
    if (st.mode === "salah") { const s = SALAH[st.step]; return s.t ? { t: s.t, s: s.s, n: s.n } : { ...DHIKR[s.i], n: s.n }; }
    return { ...DHIKR[st.idx], n: st.target };
  }

  function render(pop) {
    const d = current();
    $("ts-text").textContent = d.t;
    $("ts-text").style.fontSize = d.t.length > 40 ? "1.3rem" : "";
    $("ts-count").textContent = st.count;
    $("ts-of").textContent = d.n ? `من ${d.n}` : "بلا حد";
    const frac = d.n ? Math.min(1, st.count / d.n) : (st.count % 100) / 100;
    $("ts-ring").style.strokeDashoffset = C * (1 - frac);
    $("ts-mode").querySelectorAll("button").forEach((b) => b.setAttribute("aria-selected", b.dataset.mode === st.mode));
    $("ts-chips").hidden = st.mode !== "free";
    $("ts-chips").innerHTML = DHIKR.map((x, i) => `<button type="button" class="chip${i === st.idx ? " active" : ""}" data-i="${i}">${x.s}</button>`).join("");
    if (st.mode === "salah") {
      $("ts-chips").hidden = false;
      $("ts-chips").innerHTML = SALAH.map((x, i) => `<span class="chip${i === st.step ? " active" : ""}" style="cursor:default">${x.s || DHIKR[x.i].s} ${x.n}${i < st.step ? " ✓" : ""}</span>`).join("");
    }
    $("ts-target").value = String(st.target);
    $("ts-target").disabled = st.mode === "salah";
    $("ts-rounds").textContent = M.fmt(st.rounds, 0);
    $("ts-total").textContent = M.fmt(st.total, 0);
    const todaySum = Object.values(st.log).reduce((a, b) => a + b, 0);
    $("ts-today").textContent = M.fmt(todaySum, 0);
    const entries = Object.entries(st.log).sort((a, b) => b[1] - a[1]);
    $("ts-log").innerHTML = entries.length ? entries.map(([k, v]) => `<li><span>${k}</span><span>${M.fmt(v, 0)}</span></li>`).join("") : '<li><span class="muted">لم تسجّل أذكاراً اليوم بعد</span></li>';
    $("ts-vibe").checked = st.vibe;
    if (pop) { const c = $("ts-count"); c.classList.remove("pop"); void c.offsetWidth; c.classList.add("pop"); }
  }

  function vibrate(p) { if (st.vibe && navigator.vibrate) navigator.vibrate(p); }

  function inc() {
    const d = current();
    st.count++; st.total++;
    st.log[d.s] = (st.log[d.s] || 0) + 1;
    vibrate(15);
    if (d.n && st.count >= d.n) {
      if (st.mode === "salah") {
        if (st.step < SALAH.length - 1) { st.step++; st.count = 0; vibrate([80, 60, 80]); M.toast("انتقل إلى: " + (SALAH[st.step].s || DHIKR[SALAH[st.step].i].s)); }
        else { st.rounds++; st.step = 0; st.count = 0; vibrate([100, 60, 100, 60, 100]); M.toast("تقبّل الله منك ✓ أتممت المائة"); }
      } else { st.rounds++; st.count = 0; vibrate([80, 60, 80]); M.toast(`أتممت ${d.n} ✓`); }
    }
    save(); render(true);
  }

  $("ts-btn").addEventListener("click", inc);
  document.addEventListener("keydown", (e) => {
    if ((e.code === "Space" || e.key === "Enter") && !e.target.closest("input,select,textarea,button,a")) { e.preventDefault(); inc(); }
  });
  $("ts-chips").addEventListener("click", (e) => {
    const b = e.target.closest("[data-i]"); if (!b) return;
    st.idx = +b.dataset.i; st.count = 0; save(); render();
  });
  $("ts-mode").addEventListener("click", (e) => {
    const b = e.target.closest("button"); if (!b) return;
    st.mode = b.dataset.mode; st.count = 0; st.step = 0; save(); render();
  });
  $("ts-target").addEventListener("change", () => { st.target = +$("ts-target").value; st.count = 0; save(); render(); });
  $("ts-reset").addEventListener("click", () => { st.count = 0; st.step = 0; save(); render(); });
  $("ts-undo").addEventListener("click", () => {
    if (st.count > 0) { const d = current(); st.count--; st.total = Math.max(0, st.total - 1); if (st.log[d.s]) st.log[d.s]--; if (!st.log[d.s]) delete st.log[d.s]; save(); render(); }
  });
  $("ts-vibe").addEventListener("change", () => { st.vibe = $("ts-vibe").checked; save(); });
  render();
})();
