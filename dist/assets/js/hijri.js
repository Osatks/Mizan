(function () {
  "use strict";
  const M = window.Mizan;
  const $ = (id) => document.getElementById(id);
  if (!$("hijri-app")) return;
  let dir = "g2h";

  const opts = (sel, n, labels) => { sel.innerHTML = Array.from({ length: n }, (_, i) => `<option value="${i + 1}">${labels ? labels[i] : i + 1}</option>`).join(""); };
  opts($("g-month"), 12, M.GREG_MONTHS.map((m, i) => `${i + 1} - ${m}`));
  opts($("h-month"), 12, M.HIJRI_MONTHS.map((m, i) => `${i + 1} - ${m}`));

  function fillDays(sel, n) {
    const cur = +sel.value || 1;
    opts(sel, n);
    sel.value = Math.min(cur, n);
  }

  function setToday() {
    const t = new Date(), h = M.toHijri(t);
    $("g-year").value = t.getFullYear(); $("g-month").value = t.getMonth() + 1;
    fillDays($("g-day"), 31); $("g-day").value = t.getDate();
    $("h-year").value = h.y; $("h-month").value = h.m;
    fillDays($("h-day"), 30); $("h-day").value = h.d;
    calYM = { y: h.y, m: h.m };
    convert();
  }

  let resultText = "";
  function convert() {
    const res = $("hj-res"), sub = $("hj-res-sub");
    if (dir === "g2h") {
      const y = +$("g-year").value, m = +$("g-month").value;
      fillDays($("g-day"), new Date(y, m, 0).getDate());
      const d = +$("g-day").value;
      if (!y || y < 623 || y > 2100) { res.textContent = "—"; sub.textContent = "أدخل سنة بين 623 و2100"; return; }
      const g = new Date(y, m - 1, d); g.setFullYear(y);
      const h = M.toHijri(g);
      res.textContent = M.hijriStr(h);
      sub.textContent = `${M.WEEKDAYS[g.getDay()]} — ${M.gregStr(g)}`;
      $("hj-res-label").textContent = "التاريخ الهجري الموافق";
      resultText = `${M.gregStr(g)} يوافق ${M.hijriStr(h)}`;
      calYM = { y: h.y, m: h.m };
    } else {
      const y = +$("h-year").value, m = +$("h-month").value;
      if (!y || y < 1 || y > 1500) { res.textContent = "—"; sub.textContent = "أدخل سنة هجرية بين 1 و1500"; return; }
      fillDays($("h-day"), M.hijriMonthLength(y, m));
      const d = +$("h-day").value;
      const g = M.fromHijri(y, m, d);
      if (!g) { res.textContent = "—"; sub.textContent = "تعذر التحويل لهذا التاريخ"; return; }
      res.textContent = M.gregStr(g);
      sub.textContent = `${M.WEEKDAYS[g.getDay()]} — ${M.hijriStr({ y, m, d })}`;
      $("hj-res-label").textContent = "التاريخ الميلادي الموافق";
      resultText = `${M.hijriStr({ y, m, d })} يوافق ${M.gregStr(g)}`;
      calYM = { y, m };
    }
    renderCal();
  }

  // التقويم الشهري المزدوج
  let calYM = null;
  function renderCal() {
    if (!calYM) return;
    const { y, m } = calYM;
    const first = M.fromHijri(y, m, 1);
    if (!first) return;
    const len = M.hijriMonthLength(y, m);
    const last = new Date(first.getFullYear(), first.getMonth(), first.getDate() + len - 1);
    const gSpan = first.getMonth() === last.getMonth() ? `${M.GREG_MONTHS[first.getMonth()]} ${first.getFullYear()}` : `${M.GREG_MONTHS[first.getMonth()]} – ${M.GREG_MONTHS[last.getMonth()]} ${last.getFullYear()}`;
    $("cal-title").innerHTML = `${M.HIJRI_MONTHS[m - 1]} ${y} هـ<br><small class="muted" style="font-family:var(--font);font-size:.8rem">${gSpan}</small>`;
    const today = new Date(); today.setHours(0, 0, 0, 0);
    let html = ["أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت"].map((d) => `<div class="dow">${d}</div>`).join("");
    for (let i = 0; i < first.getDay(); i++) html += "<div></div>";
    for (let d = 1; d <= len; d++) {
      const g = new Date(first.getFullYear(), first.getMonth(), first.getDate() + d - 1);
      const isToday = g.getTime() === today.getTime();
      html += `<button type="button" class="day${isToday ? " today" : ""}" data-h="${d}" style="border:0;cursor:pointer" aria-label="${d} ${M.HIJRI_MONTHS[m - 1]} يوافق ${M.gregStr(g)}"><b>${d}</b><small>${g.getDate()}/${g.getMonth() + 1}</small></button>`;
    }
    $("cal-grid").innerHTML = html;
  }
  $("cal-grid").addEventListener("click", (e) => {
    const b = e.target.closest("[data-h]"); if (!b) return;
    setDir("h2g");
    $("h-year").value = calYM.y; $("h-month").value = calYM.m;
    fillDays($("h-day"), M.hijriMonthLength(calYM.y, calYM.m)); $("h-day").value = b.dataset.h;
    convert();
  });
  const shift = (k) => { let { y, m } = calYM; m += k; if (m < 1) { m = 12; y--; } if (m > 12) { m = 1; y++; } calYM = { y, m }; renderCal(); };
  $("cal-prev").addEventListener("click", () => shift(-1));
  $("cal-next").addEventListener("click", () => shift(1));

  function setDir(d) {
    dir = d;
    $("hj-dir").querySelectorAll("button").forEach((b) => b.setAttribute("aria-selected", b.dataset.dir === d));
    $("hj-g2h").hidden = d !== "g2h"; $("hj-h2g").hidden = d !== "h2g";
  }
  $("hj-dir").addEventListener("click", (e) => { const b = e.target.closest("button"); if (b) { setDir(b.dataset.dir); convert(); } });
  $("hijri-app").addEventListener("change", convert);
  $("hijri-app").addEventListener("input", (e) => { if (e.target.type === "number") convert(); });
  $("hj-today").addEventListener("click", setToday);
  $("hj-copy").addEventListener("click", async () => { try { await navigator.clipboard.writeText(resultText); M.toast("تم النسخ ✓"); } catch (e) {} });

  setToday();
})();
