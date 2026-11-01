(function () {
  "use strict";
  const M = window.Mizan, P = window.PrayTimes;
  const $ = (id) => document.getElementById(id);
  if (!$("prayer-app")) return;

  const NAMES = [["fajr", t("الفجر")], ["sunrise", t("الشروق")], ["dhuhr", t("الظهر")], ["asr", t("العصر")], ["maghrib", t("المغرب")], ["isha", t("العشاء")]];
  let loc = P.getLocation();
  const prefs = M.store.get("prayer-prefs", { asr: "std", fmt: "12" });
  let day = new Date(); day.setHours(0, 0, 0, 0);

  $("pt-city").innerHTML = (loc.cityIndex === -1 ? `<option value="-1" selected>📍 ${loc.name}</option>` : "") + P.citySelectHTML(loc.cityIndex);
  $("pt-method").innerHTML = Object.entries(P.METHODS).map(([k, m]) => `<option value="${k}">${m.name}</option>`).join("");
  $("pt-method").value = loc.method;
  $("pt-asr").value = prefs.asr; $("pt-fmt").value = prefs.fmt;

  const opts = (d) => ({ lat: loc.lat, lng: loc.lng, tz: loc.tz, elev: loc.elev || 0, method: $("pt-method").value, asr: $("pt-asr").value, hijriMonth: M.toHijri(d).m });
  const h12 = () => $("pt-fmt").value === "12";

  // الوقت الحالي في منطقة المدينة الزمنية
  function nowInCity() {
    const now = new Date();
    const diff = P.tzOffset(loc.tz, now) - (-now.getTimezoneOffset() / 60);
    return new Date(now.getTime() + diff * 3600000);
  }

  function render() {
    const times = P.compute(day, opts(day));
    const dow = M.WEEKDAYS[day.getDay()];
    $("pt-date").innerHTML = `${dow} ${M.gregStr(day)}<br><small class="muted">${M.hijriStr(M.toHijri(day))}</small>`;
    $("pt-city-title").textContent = t("مواقيت الصلاة في {city}", { city: loc.name });
    $("pt-loc").innerHTML = `${loc.country ? loc.country + " · " : ""}${t("خط العرض {lat}° · خط الطول {lng}°", { lat: loc.lat.toFixed(2), lng: loc.lng.toFixed(2) })} ${loc.gps ? '<span class="badge ok">' + t("موقعك الحالي") + '</span>' : ""}`;

    const now = nowInCity();
    const isToday = now.toDateString() === day.toDateString();
    const nowH = now.getHours() + now.getMinutes() / 60 + now.getSeconds() / 3600;
    let current = null, next = null;
    if (isToday) {
      const seq = NAMES.filter(([k]) => k !== "sunrise");
      for (const [k] of seq) if (times[k] <= nowH) current = k;
      next = (seq.find(([k]) => times[k] > nowH) || [null])[0];
    }
    $("pt-list").innerHTML = NAMES.map(([k, n]) =>
      `<li class="${k === "sunrise" ? "sun " : ""}${k === current ? "current " : ""}${k === next ? "next" : ""}"><span class="pname">${n}${k === next ? ' <span class="badge gold">' + t("التالية") + '</span>' : ""}</span><span class="time">${P.fmtTime(times[k], h12())}</span></li>`).join("");

    // منتصف الليل والثلث الأخير (من المغرب إلى فجر الغد)
    const tomorrow = new Date(day); tomorrow.setDate(day.getDate() + 1);
    const tf = P.compute(tomorrow, opts(tomorrow)).fajr + 24;
    const nightLen = tf - times.maghrib;
    $("pt-midnight").textContent = P.fmtTime(times.maghrib + nightLen / 2, h12());
    $("pt-lastthird").textContent = P.fmtTime(times.maghrib + (nightLen * 2) / 3, h12());
    $("pt-qibla").textContent = t("{deg}° من الشمال", { deg: P.qibla(loc.lat, loc.lng).toFixed(1) });
    app.times = times;
  }

  // العد التنازلي (يعمل دائماً على اليوم الحالي)
  function tick() {
    const now = nowInCity();
    const today = new Date(now); today.setHours(0, 0, 0, 0);
    const nowH = now.getHours() + now.getMinutes() / 60 + now.getSeconds() / 3600;
    const times = P.compute(today, opts(today));
    const seq = NAMES.filter(([k]) => k !== "sunrise");
    let nx = seq.find(([k]) => times[k] > nowH), target, name;
    if (nx) { target = times[nx[0]]; name = nx[1]; }
    else { const tm = new Date(today); tm.setDate(today.getDate() + 1); target = P.compute(tm, opts(tm)).fajr + 24; name = t("الفجر"); }
    let secs = Math.max(0, Math.round((target - nowH) * 3600));
    const hh = String(Math.floor(secs / 3600)).padStart(2, "0"), mm = String(Math.floor((secs % 3600) / 60)).padStart(2, "0"), ss = String(secs % 60).padStart(2, "0");
    $("pt-next-name").textContent = name;
    $("pt-countdown").textContent = `${hh}:${mm}:${ss}`;
    $("pt-next-time").textContent = t("عند {time}", { time: P.fmtTime(target, h12()) });
    if (secs === 0) render();
  }

  function renderMonth() {
    const y = day.getFullYear(), m = day.getMonth();
    const n = new Date(y, m + 1, 0).getDate();
    $("pt-month-title").textContent = t("جدول مواقيت الصلاة — {month} {y} — {city}", { month: M.GREG_MONTHS[m], y, city: loc.name });
    const today = new Date(); today.setHours(0, 0, 0, 0);
    let html = "";
    for (let d = 1; d <= n; d++) {
      const dt = new Date(y, m, d), t = P.compute(dt, opts(dt)), h = M.toHijri(dt);
      const cls = dt.getTime() === today.getTime() ? ' style="background:var(--primary-soft);font-weight:700"' : "";
      html += `<tr${cls}><td>${M.WEEKDAYS[dt.getDay()]}</td><td>${d}/${m + 1}</td><td>${h.d} ${M.HIJRI_MONTHS[h.m - 1]}</td>` +
        ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"].map((k) => `<td dir="ltr" style="text-align:end">${P.fmtTime(t[k], h12())}</td>`).join("") + "</tr>";
    }
    $("pt-month-body").innerHTML = html;
  }

  function savePrefs() {
    M.store.set("prayer-prefs", { asr: $("pt-asr").value, fmt: $("pt-fmt").value });
    loc.method = $("pt-method").value; P.setLocation(loc);
  }

  const app = $("prayer-app");
  function refresh() { savePrefs(); render(); tick(); if (!$("pt-month").hidden) renderMonth(); }

  $("pt-city").addEventListener("change", () => {
    const i = +$("pt-city").value;
    if (i < 0) return;
    loc = { ...P.CITIES[i], cityIndex: i, rv: 2 };
    $("pt-method").value = loc.method;
    refresh();
  });
  ["pt-method", "pt-asr", "pt-fmt"].forEach((id) => $(id).addEventListener("change", refresh));
  $("pt-prev").addEventListener("click", () => { day.setDate(day.getDate() - 1); render(); });
  $("pt-next").addEventListener("click", () => { day.setDate(day.getDate() + 1); render(); });
  $("pt-today").addEventListener("click", () => { day = new Date(); day.setHours(0, 0, 0, 0); render(); });
  $("pt-month-btn").addEventListener("click", () => {
    const s = $("pt-month"); s.hidden = !s.hidden;
    $("pt-month-btn").textContent = s.hidden ? t("عرض جدول الشهر") : t("إخفاء جدول الشهر");
    if (!s.hidden) { renderMonth(); s.scrollIntoView({ block: "start" }); }
  });
  $("pt-gps").addEventListener("click", async () => {
    const b = $("pt-gps"); b.disabled = true; b.textContent = t("جارٍ تحديد الموقع…");
    try {
      loc = await P.geolocate();
      $("pt-city").innerHTML = `<option value="-1" selected>📍 ${loc.name}</option>` + P.citySelectHTML(-1);
      $("pt-method").value = loc.method;
      refresh(); M.toast(t("تم تحديد موقعك ✓"));
    } catch (e) { M.toast(e.message); }
    b.disabled = false; b.textContent = t("📍 استخدم موقعي");
  });

  render(); tick();
  setInterval(tick, 1000);
})();
