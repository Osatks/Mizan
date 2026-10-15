(function () {
  "use strict";
  const M = window.Mizan;
  const $ = (id) => document.getElementById(id);
  const app = $("fidya-app");
  if (!app) return;
  let tab = M.store.get("fidya-tab", "fidya");
  const pad = (n) => String(n).padStart(2, "0");
  const t0 = new Date();
  $("fd-start").value = `${t0.getFullYear()}-${pad(t0.getMonth() + 1)}-${pad(t0.getDate())}`;

  function setTab(t) {
    tab = t; M.store.set("fidya-tab", t);
    $("fd-tabs").querySelectorAll("button").forEach((b) => b.setAttribute("aria-selected", b.dataset.t === t));
    app.querySelectorAll("[data-panel]").forEach((p) => (p.hidden = p.dataset.panel !== t));
    calc();
  }

  function calc() {
    const custom = $("fd-measure").value === "custom";
    $("fd-custom-wrap").hidden = !custom;
    const per = custom ? M.num($("fd-custom")) : parseFloat($("fd-measure").value);
    const price = M.num($("fd-price")), meal = $("fd-price-type").value === "meal";
    let poor, label, sub, note, stats = [];

    if (tab === "fidya") {
      const days = Math.max(1, parseInt($("fd-days").value, 10) || 1);
      poor = days; label = "فدية الصيام"; sub = `عن ${days} يوماً — مسكين عن كل يوم`;
      note = "يجوز إطعام مسكين واحد عن كل الأيام أو مساكين متعددين، ويجوز إخراجها يوماً بيوم أو جملة واحدة في آخر الشهر.";
    } else if (tab === "yamin") {
      const n = Math.max(1, parseInt($("fd-oaths").value, 10) || 1);
      poor = 10 * n; label = "كفارة اليمين"; sub = n > 1 ? `${n} كفارات × 10 مساكين` : "إطعام 10 مساكين";
      stats.push(["البديل عند العجز", `صيام ${3 * n} أيام`]);
      note = "يُخيّر بين الإطعام والكسوة وتحرير الرقبة، ولا ينتقل إلى الصيام إلا عند العجز عنها.";
    } else {
      poor = 60; label = "كفارة الجماع في نهار رمضان"; sub = "إطعام 60 مسكيناً عند العجز عن الصيام";
      const p = $("fd-start").value.split("-").map(Number);
      if (p.length === 3) {
        const s = new Date(p[0], p[1] - 1, p[2]);
        const h = M.toHijri(s);
        // شهران هجريان متتابعان: من اليوم نفسه في الشهر بعد التالي
        let y = h.y, m = h.m + 2; if (m > 12) { m -= 12; y++; }
        let end = M.fromHijri(y, m, Math.min(h.d, M.hijriMonthLength(y, m)));
        if (end) { end = new Date(end.getFullYear(), end.getMonth(), end.getDate() - 1); stats.push(["الصيام: من", M.gregStr(s)], ["إلى (شهرين هجريين)", M.gregStr(end)], ["أي نحو", Math.round((end - s) / 86400000) + 1 + " يوماً"]); }
      }
      note = "الواجب على الترتيب، ويُراعى ألا يتخلل الشهرين يوما العيد وأيام التشريق إلا لعذر شرعي. وعليه قضاء اليوم مع التوبة.";
    }
    const kg = per * poor;
    $("fd-label").textContent = label;
    $("fd-main").innerHTML = `${M.fmt(kg)} <small>كغ</small>`;
    $("fd-sub").textContent = sub;
    stats.unshift(["عدد المساكين / الوجبات", poor], ["المقدار لكل مسكين", M.fmt(per) + " كغ"]);
    if (price) stats.push(["القيمة التقريبية", M.fmt(meal ? price * poor : price * kg)]);
    $("fd-stats").innerHTML = stats.map(([a, b]) => `<li><span>${a}</span><span>${b}</span></li>`).join("");
    $("fd-note").innerHTML = `<p>${note}</p>`;
  }

  $("fd-tabs").addEventListener("click", (e) => { const b = e.target.closest("button"); if (b) setTab(b.dataset.t); });
  app.addEventListener("click", (e) => { const c = e.target.closest("[data-days]"); if (c) { $("fd-days").value = c.dataset.days; calc(); } });
  app.addEventListener("input", calc);
  app.addEventListener("change", calc);
  setTab(tab);
})();
