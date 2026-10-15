(function () {
  "use strict";
  const M = window.Mizan;
  const $ = (id) => document.getElementById(id);
  const app = $("zakat-app");
  if (!app) return;

  const FIELDS = ["zk-gold-price", "zk-silver-price", "zk-cash", "zk-bank", "zk-receivable", "zk-other", "zk-g24", "zk-g22", "zk-g21", "zk-g18", "zk-silver", "zk-jewel-g", "zk-stocks-trade", "zk-stocks-long", "zk-goods", "zk-debts"];
  const GOLD_NISAB = 85, SILVER_NISAB = 595, RATE = 0.025;

  // استرجاع البيانات المحفوظة
  const saved = M.store.get("zakat", {});
  FIELDS.forEach((id) => { if (saved[id]) $(id).value = saved[id]; });
  if (saved.currency) $("zk-currency").value = saved.currency;
  if (saved.basis) $("zk-basis").value = saved.basis;
  if (saved.jewelry === false) $("zk-jewelry").checked = false;

  function cur() { return $("zk-currency").value; }

  function calc() {
    document.querySelectorAll("[data-cur]").forEach((el) => (el.textContent = cur()));
    $("zk-jewel-wrap").hidden = $("zk-jewelry").checked;

    const v = {}; FIELDS.forEach((id) => (v[id] = M.num($(id))));
    const gp = v["zk-gold-price"], sp = v["zk-silver-price"];
    const pure24 = v["zk-g24"] + v["zk-g22"] * 22 / 24 + v["zk-g21"] * 21 / 24 + v["zk-g18"] * 18 / 24;
    const excluded = $("zk-jewelry").checked ? 0 : Math.min(pure24, v["zk-jewel-g"] * 21 / 24);
    const gold = (pure24 - excluded) * gp;
    const silver = v["zk-silver"] * sp;

    const parts = [
      ["النقود والأرصدة", v["zk-cash"] + v["zk-bank"] + v["zk-other"]],
      ["الديون المرجوّة", v["zk-receivable"]],
      ["الذهب", gold],
      ["الفضة", silver],
      ["الأسهم", v["zk-stocks-trade"] + v["zk-stocks-long"]],
      ["عروض التجارة", v["zk-goods"]],
    ];
    const total = parts.reduce((s, p) => s + p[1], 0);
    const debts = Math.min(v["zk-debts"], total);
    const net = Math.max(0, total - debts);

    const basis = $("zk-basis").value;
    const nisab = basis === "silver" ? SILVER_NISAB * sp : GOLD_NISAB * gp;

    $("zk-total").textContent = M.money(total, cur());
    $("zk-debts-out").textContent = M.money(debts, cur());
    $("zk-net").textContent = M.money(net, cur());
    $("zk-nisab").textContent = nisab ? M.money(nisab, cur()) : "أدخل سعر " + (basis === "silver" ? "الفضة" : "الذهب");

    const status = $("zk-status");
    let zakat = 0;
    if (!nisab) {
      status.className = "badge gold"; status.textContent = "أدخل سعر " + (basis === "silver" ? "الفضة" : "الذهب") + " لحساب النصاب";
    } else if (net >= nisab && net > 0) {
      zakat = net * RATE;
      status.className = "badge ok"; status.textContent = "✓ بلغ مالك النصاب — تجب الزكاة";
    } else if (net > 0) {
      status.className = "badge no"; status.textContent = "لم يبلغ النصاب — لا زكاة عليك";
    } else {
      status.className = "badge gold"; status.textContent = "أدخل أموالك";
    }
    const amountEl = $("zk-amount");
    const newText = M.fmt(zakat) + " ";
    if (amountEl.dataset.v !== newText) {
      amountEl.innerHTML = `${M.fmt(zakat)} <small>${cur()}</small>`;
      amountEl.dataset.v = newText;
      amountEl.classList.remove("pop"); void amountEl.offsetWidth; amountEl.classList.add("pop");
    }
    $("zk-progress").style.width = nisab ? Math.min(100, (net / nisab) * 100) + "%" : "0";

    // تفصيل الزكاة لكل نوع
    const bd = $("zk-breakdown");
    const rows = parts.filter((p) => p[1] > 0);
    if (zakat > 0 && rows.length > 1) {
      const ratio = net / total;
      bd.innerHTML = '<div class="label" style="font-size:.88rem;color:var(--muted);margin-bottom:6px">توزيع الزكاة حسب نوع المال</div><ul class="stat-list">' +
        rows.map((p) => `<li><span>${p[0]}</span><span>${M.money(p[1] * ratio * RATE, cur())}</span></li>`).join("") + "</ul>";
    } else bd.innerHTML = "";

    if (zakat > 0 && gp) {
      bd.innerHTML += `<p class="muted" style="font-size:.88rem;margin:12px 0 0">تعادل زكاتك تقريباً <b>${M.fmt(zakat / gp, 2)}</b> غرام ذهب عيار 24.</p>`;
    }

    const data = { currency: cur(), basis, jewelry: $("zk-jewelry").checked };
    FIELDS.forEach((id) => { if ($(id).value) data[id] = $(id).value; });
    M.store.set("zakat", data);
    app._result = { zakat, net, nisab };
  }

  app.addEventListener("input", calc);
  app.addEventListener("change", calc);
  $("zk-reset").addEventListener("click", () => {
    FIELDS.forEach((id) => ($(id).value = ""));
    M.store.del("zakat"); calc(); M.toast("تم مسح البيانات");
  });
  $("zk-share").addEventListener("click", () => {
    const r = app._result || {};
    M.share("حاسبة الزكاة", r.zakat ? `زكاتي المستحقة: ${M.money(r.zakat, cur())} (الوعاء الزكوي ${M.money(r.net, cur())}) — حسبتها عبر حاسبة الزكاة في موقع ميزان` : "احسب زكاتك بسهولة عبر حاسبة الزكاة في موقع ميزان");
  });
  calc();
})();
