(function () {
  "use strict";
  const M = window.Mizan;
  const $ = (id) => document.getElementById(id);
  const app = $("age-app");
  if (!app) return;
  let cal = "g";
  const saved = M.store.get("age", null);

  const pad = (n) => String(n).padStart(2, "0");
  const iso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  $("age-at").value = iso(new Date());

  function fillMonths() {
    const names = cal === "g" ? M.GREG_MONTHS : M.HIJRI_MONTHS;
    const cur = $("age-m").value;
    $("age-m").innerHTML = names.map((n, i) => `<option value="${i + 1}">${i + 1} - ${n}</option>`).join("");
    if (cur) $("age-m").value = cur;
  }
  function fillDays() {
    const y = +$("age-y").value || 2000, m = +$("age-m").value || 1;
    const n = cal === "g" ? new Date(y, m, 0).getDate() : (y > 0 && y < 1600 ? M.hijriMonthLength(y, m) : 30);
    const cur = +$("age-d").value || 1;
    $("age-d").innerHTML = Array.from({ length: n }, (_, i) => `<option>${i + 1}</option>`).join("");
    $("age-d").value = Math.min(cur, n);
  }

  // فرق بين تاريخين بصيغة سنوات/أشهر/أيام لتقويم معيّن
  function diffYMD(a, b, monthLen) {
    let y = b.y - a.y, m = b.m - a.m, d = b.d - a.d;
    if (d < 0) { m--; const pm = b.m === 1 ? 12 : b.m - 1, py = b.m === 1 ? b.y - 1 : b.y; d += monthLen(py, pm); }
    if (m < 0) { y--; m += 12; }
    return { y, m, d };
  }
  const ymdStr = (r) => `${r.y} <small>سنة</small> ${r.m} <small>شهر</small> ${r.d} <small>يوم</small>`;

  function birthDate() {
    const y = +$("age-y").value, m = +$("age-m").value, d = +$("age-d").value;
    if (!y) return null;
    if (cal === "g") { if (y < 1800 || y > 2200) return null; const g = new Date(y, m - 1, d); return g; }
    if (y < 1200 || y > 1600) return null;
    return M.fromHijri(y, m, d);
  }

  function calc() {
    const birth = birthDate();
    const atParts = $("age-at").value.split("-").map(Number);
    const at = atParts.length === 3 ? new Date(atParts[0], atParts[1] - 1, atParts[2]) : new Date();
    at.setHours(0, 0, 0, 0);
    if (!birth || birth > at) {
      $("age-g").textContent = "—"; $("age-h").textContent = "—"; $("age-list").innerHTML = birth > at ? "<li><span>تاريخ الميلاد بعد تاريخ الحساب</span></li>" : ""; $("age-extra").hidden = true;
      return;
    }
    const gA = { y: birth.getFullYear(), m: birth.getMonth() + 1, d: birth.getDate() };
    const gB = { y: at.getFullYear(), m: at.getMonth() + 1, d: at.getDate() };
    const gAge = diffYMD(gA, gB, (y, m) => new Date(y, m, 0).getDate());
    const hA = M.toHijri(birth), hB = M.toHijri(at);
    const hAge = diffYMD(hA, hB, (y, m) => M.hijriMonthLength(y, m));
    $("age-g").innerHTML = ymdStr(gAge);
    $("age-h").innerHTML = ymdStr(hAge);

    const days = Math.round((at - birth) / 86400000);
    // عيد الميلاد القادم
    let next = new Date(at.getFullYear(), birth.getMonth(), birth.getDate());
    if (next < at) next = new Date(at.getFullYear() + 1, birth.getMonth(), birth.getDate());
    const toNext = Math.round((next - at) / 86400000);
    let nh = M.fromHijri(hB.y, hA.m, Math.min(hA.d, M.hijriMonthLength(hB.y, hA.m)));
    if (!nh || nh < at) nh = M.fromHijri(hB.y + 1, hA.m, Math.min(hA.d, M.hijriMonthLength(hB.y + 1, hA.m)));
    const toNextH = nh ? Math.round((nh - at) / 86400000) : null;

    $("age-list").innerHTML = [
      ["تاريخ الميلاد ميلادياً", M.gregStr(birth)],
      ["تاريخ الميلاد هجرياً", M.hijriStr(hA)],
      ["يوم الميلاد", M.WEEKDAYS[birth.getDay()]],
      ["عيد ميلادك القادم (ميلادي)", toNext === 0 ? "اليوم 🎉" : `بعد ${toNext} يوماً`],
      ["ذكرى ميلادك الهجرية القادمة", toNextH === 0 ? "اليوم" : toNextH != null ? `بعد ${toNextH} يوماً` : "—"],
    ].map(([a, b]) => `<li><span>${a}</span><span>${b}</span></li>`).join("");

    const months = gAge.y * 12 + gAge.m;
    $("age-tiles").innerHTML = [
      [M.fmt(days, 0), "يوماً"], [M.fmt(days * 1440, 0), "دقيقة"],[M.fmt(months, 0), "شهراً"],
      [M.fmt(days * 24, 0), "ساعة"], [M.fmt(hAge.y, 0), "رمضان مرّ عليك"],
      [M.fmt(Math.floor(days / 7), 0), "يوم جمعة"],
    ].map(([n, l]) => `<div class="tile"><b>${n}</b><span>${l}</span></div>`).join("");
    $("age-extra").hidden = false;
    app._summary = `عمري ${gAge.y} سنة ميلادية و${hAge.y} سنة هجرية`;
    M.store.set("age", { cal, y: $("age-y").value, m: $("age-m").value, d: $("age-d").value });
  }

  function setCal(c, keep) {
    cal = c;
    $("age-cal").querySelectorAll("button").forEach((b) => b.setAttribute("aria-selected", b.dataset.cal === c));
    if (!keep) {
      // تحويل التاريخ المدخل إلى التقويم الآخر
      const prev = c === "h" ? "g" : "h";
      cal = prev; const b = birthDate(); cal = c;
      if (b) {
        if (c === "h") { const h = M.toHijri(b); $("age-y").value = h.y; fillMonths(); $("age-m").value = h.m; fillDays(); $("age-d").value = h.d; }
        else { $("age-y").value = b.getFullYear(); fillMonths(); $("age-m").value = b.getMonth() + 1; fillDays(); $("age-d").value = b.getDate(); }
      } else { fillMonths(); fillDays(); }
      $("age-y").placeholder = c === "h" ? "مثال: 1415" : "مثال: 1995";
    }
    calc();
  }

  $("age-cal").addEventListener("click", (e) => { const b = e.target.closest("button"); if (b && b.dataset.cal !== cal) setCal(b.dataset.cal); });
  app.addEventListener("change", (e) => { if (e.target.id === "age-m" || e.target.id === "age-y") fillDays(); calc(); });
  $("age-y").addEventListener("input", () => { fillDays(); calc(); });
  $("age-share").addEventListener("click", () => M.share("حاسبة العمر", (app._summary || "احسب عمرك") + " — عبر موقع ميزان"));

  if (saved) { cal = saved.cal || "g"; }
  fillMonths();
  if (saved) { $("age-y").value = saved.y || ""; $("age-m").value = saved.m || 1; }
  fillDays();
  if (saved) $("age-d").value = saved.d || 1;
  setCal(cal, true);
})();
