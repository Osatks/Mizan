(function () {
  "use strict";
  const M = window.Mizan;
  const $ = (id) => document.getElementById(id);
  const app = $("fitr-app");
  if (!app) return;
  let mode = M.store.get("fitr-mode", "food");

  const saved = M.store.get("fitr", {});
  ["fr-people", "fr-kgprice", "fr-value", "fr-custom"].forEach((id) => { if (saved[id]) $(id).value = saved[id]; });
  if (saved.typeIndex != null) $("fr-type").selectedIndex = saved.typeIndex;

  function setMode(m) {
    mode = m;
    $("fr-mode").querySelectorAll("button").forEach((b) => b.setAttribute("aria-selected", b.dataset.mode === m));
    $("fr-food").hidden = m !== "food";
    $("fr-money").hidden = m !== "money";
    calc();
  }

  function calc() {
    const people = Math.max(1, Math.min(99, parseInt($("fr-people").value, 10) || 1));
    const custom = $("fr-type").value === "custom";
    $("fr-custom-wrap").hidden = !custom;
    const sa = custom ? M.num($("fr-custom")) : parseFloat($("fr-type").value);
    const stats = [];
    let main;

    if (mode === "food") {
      const kg = sa * people;
      main = `${M.fmt(kg)} <small>كغ</small>`;
      stats.push(["مقدار الصاع للفرد", M.fmt(sa) + " كغ"], ["عدد الأفراد", people], ["الإجمالي", M.fmt(kg) + " كغ"]);
      const price = M.num($("fr-kgprice"));
      if (price) stats.push(["التكلفة التقريبية", M.fmt(kg * price)]);
      $("fr-label").textContent = "مقدار الطعام الواجب إخراجه";
    } else {
      const val = M.num($("fr-value"));
      main = val ? `${M.fmt(val * people)}` : "—";
      stats.push(["قيمة الفرد", val ? M.fmt(val) : "أدخل القيمة"], ["عدد الأفراد", people], ["الإجمالي", val ? M.fmt(val * people) : "—"]);
      $("fr-label").textContent = "القيمة النقدية الإجمالية";
    }
    $("fr-total").innerHTML = main;
    $("fr-sub").textContent = people === 1 ? "عن فرد واحد" : people === 2 ? "عن فردين" : `عن ${people} أفراد`;
    $("fr-stats").innerHTML = stats.map(([a, b]) => `<li><span>${a}</span><span>${b}</span></li>`).join("");

    M.store.set("fitr", { "fr-people": people, "fr-kgprice": $("fr-kgprice").value, "fr-value": $("fr-value").value, "fr-custom": $("fr-custom").value, typeIndex: $("fr-type").selectedIndex });
    M.store.set("fitr-mode", mode);
    app._summary = $("fr-label").textContent + ": " + $("fr-total").textContent + " " + $("fr-sub").textContent;
  }

  $("fr-mode").addEventListener("click", (e) => { const b = e.target.closest("button"); if (b) setMode(b.dataset.mode); });
  app.addEventListener("input", calc);
  app.addEventListener("change", calc);
  $("fr-share").addEventListener("click", () => M.share("زكاة الفطر", app._summary + " — عبر موقع ميزان"));
  setMode(mode);
})();
