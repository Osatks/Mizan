/* حاسبة المواريث — محرك الفرائض على مذهب الجمهور */
(function () {
  "use strict";

  /* ---------- الكسور ---------- */
  const gcd = (a, b) => { a = Math.abs(a); b = Math.abs(b); while (b) [a, b] = [b, a % b]; return a || 1; };
  const lcm = (a, b) => (a / gcd(a, b)) * b;
  class F {
    constructor(n, d = 1) { if (d < 0) { n = -n; d = -d; } const g = gcd(n, d); this.n = n / g; this.d = d / g; }
    add(o) { return new F(this.n * o.d + o.n * this.d, this.d * o.d); }
    sub(o) { return new F(this.n * o.d - o.n * this.d, this.d * o.d); }
    mul(o) { return typeof o === "number" ? new F(this.n * o, this.d) : new F(this.n * o.n, this.d * o.d); }
    div(o) { return typeof o === "number" ? new F(this.n, this.d * o) : new F(this.n * o.d, this.d * o.n); }
    cmp(o) { return this.n * o.d - o.n * this.d; }
    get v() { return this.n / this.d; }
    isZero() { return this.n === 0; }
    toString() { return this.d === 1 ? String(this.n) : `${this.n}/${this.d}`; }
  }
  const Z = new F(0), ONE = new F(1);
  const max = (...xs) => xs.reduce((a, b) => (b.cmp(a) > 0 ? b : a));

  /* ---------- تعريف الورثة ---------- */
  const HEIRS = [
    { g: "الزوجان" },
    { k: "husband", n: "الزوج", max: 1, only: "f" },
    { k: "wife", n: "الزوجة", max: 4, only: "m" },
    { g: "الأصول" },
    { k: "father", n: "الأب", max: 1 },
    { k: "mother", n: "الأم", max: 1 },
    { k: "gf", n: "الجد (أبو الأب)", max: 1 },
    { k: "gmm", n: "الجدة (أم الأم)", max: 1 },
    { k: "gmf", n: "الجدة (أم الأب)", max: 1 },
    { g: "الفروع" },
    { k: "son", n: "الابن", max: 30 },
    { k: "daughter", n: "البنت", max: 30 },
    { k: "gson", n: "ابن الابن", max: 30 },
    { k: "gdau", n: "بنت الابن", max: 30 },
    { g: "الإخوة والأخوات" },
    { k: "fb", n: "الأخ الشقيق", max: 30 },
    { k: "fs", n: "الأخت الشقيقة", max: 30 },
    { k: "pb", n: "الأخ لأب", max: 30 },
    { k: "ps", n: "الأخت لأب", max: 30 },
    { k: "mb", n: "الأخ لأم", max: 30 },
    { k: "ms", n: "الأخت لأم", max: 30 },
    { g: "الحواشي (العصبات البعيدة)" },
    { k: "fnep", n: "ابن الأخ الشقيق", max: 30 },
    { k: "pnep", n: "ابن الأخ لأب", max: 30 },
    { k: "fun", n: "العم الشقيق", max: 30 },
    { k: "pun", n: "العم لأب", max: 30 },
    { k: "fcous", n: "ابن العم الشقيق", max: 30 },
    { k: "pcous", n: "ابن العم لأب", max: 30 },
  ];
  const NAME = Object.fromEntries(HEIRS.filter((h) => h.k).map((h) => [h.k, h.n]));
  const COLORS = ["var(--primary)", "var(--gold)", "#2f7fb5", "#b4412f", "#7a52b3", "#2e9e74", "#d07a2b", "#4b6b8a", "#a0527a", "#5c8f2e"];

  /* ---------- المحرك ---------- */
  function solve(c, gender) {
    const cnt = (k) => c[k] || 0;
    const res = {}; // k -> {share:F, type, note, blocked}
    const notes = [];
    const fard = {}; // k -> F
    const label = {};
    const block = (k, by) => { if (cnt(k)) res[k] = { blocked: by }; };

    if (gender === "m") c.husband = 0; else c.wife = 0;
    const present = Object.keys(c).filter((k) => c[k] > 0);
    if (!present.length) return null;

    // الفروع
    const son = cnt("son"), dau = cnt("daughter");
    let gson = cnt("gson"), gdau = cnt("gdau");
    if (son) { block("gson", "الابن"); block("gdau", "الابن"); gson = 0; gdau = 0; }
    if (!son && dau >= 2 && gdau && !gson) { block("gdau", "البنتين (استكمال الثلثين)"); gdau = 0; }
    const maleDesc = son > 0 || gson > 0;
    const femDesc = dau > 0 || gdau > 0;
    const desc = maleDesc || femDesc;

    // الأصول
    const father = cnt("father"), mother = cnt("mother");
    let gf = cnt("gf");
    if (father && gf) { block("gf", "الأب"); gf = 0; }
    let gmm = cnt("gmm"), gmf = cnt("gmf");
    if (mother) { block("gmm", "الأم"); block("gmf", "الأم"); gmm = gmf = 0; }
    if (father && gmf) { block("gmf", "الأب"); gmf = 0; }

    const sibAll = cnt("fb") + cnt("fs") + cnt("pb") + cnt("ps") + cnt("mb") + cnt("ms");
    const spouseK = gender === "m" ? "wife" : "husband";
    const spouse = cnt(spouseK);

    // الزوجان
    if (spouse) {
      if (gender === "f") { fard.husband = desc ? new F(1, 4) : new F(1, 2); }
      else { fard.wife = desc ? new F(1, 8) : new F(1, 4); }
      label[spouseK] = "فرض";
    }

    // الإخوة لأم
    const matN = cnt("mb") + cnt("ms");
    if (matN) {
      const by = desc ? "الفرع الوارث" : father ? "الأب" : gf ? "الجد" : null;
      if (by) { block("mb", by); block("ms", by); }
      else {
        const total = matN === 1 ? new F(1, 6) : new F(1, 3);
        if (cnt("mb")) { fard.mb = total.mul(cnt("mb")).div(matN); label.mb = "فرض"; }
        if (cnt("ms")) { fard.ms = total.mul(cnt("ms")).div(matN); label.ms = "فرض"; }
        if (matN > 1) notes.push("الإخوة لأم يشتركون في الثلث بالتساوي بين الذكر والأنثى.");
      }
    }

    // الإخوة الأشقاء ولأب
    let fb = cnt("fb"), fs = cnt("fs"), pb = cnt("pb"), ps = cnt("ps");
    const sibBlocker = son ? "الابن" : gson ? "ابن الابن" : father ? "الأب" : null;
    if (sibBlocker) { ["fb", "fs", "pb", "ps"].forEach((k) => block(k, sibBlocker)); fb = fs = pb = ps = 0; }
    const jaddMode = gf && (fb + fs + pb + ps) > 0;

    // البنات وبنات الابن
    if (dau && !son) { fard.daughter = dau === 1 ? new F(1, 2) : new F(2, 3); label.daughter = "فرض"; }
    if (gdau && !gson) {
      if (!dau) { fard.gdau = gdau === 1 ? new F(1, 2) : new F(2, 3); label.gdau = "فرض"; }
      else if (dau === 1) { fard.gdau = new F(1, 6); label.gdau = "فرض (تكملة الثلثين)"; }
    }

    // الأم والجدات
    let umariyya = false;
    if (mother) {
      if (desc || sibAll >= 2) fard.mother = new F(1, 6);
      else if (spouse && father && present.every((k) => ["husband", "wife", "father", "mother"].includes(k) || res[k]?.blocked)) {
        umariyya = true;
        fard.mother = ONE.sub(fard[spouseK]).div(3);
        notes.push("هذه إحدى المسألتين العُمَريّتين (الغرّاوين): للأم ثلث الباقي بعد نصيب الزوج أو الزوجة.");
      } else fard.mother = new F(1, 3);
      label.mother = "فرض";
    }
    if (gmm || gmf) {
      const n = gmm + gmf, sixth = new F(1, 6);
      if (gmm) { fard.gmm = sixth.div(n); label.gmm = "فرض"; }
      if (gmf) { fard.gmf = sixth.div(n); label.gmf = "فرض"; }
      if (n > 1) notes.push("الجدتان تشتركان في السدس.");
    }

    // الأب والجد
    if (father) {
      if (maleDesc) { fard.father = new F(1, 6); label.father = "فرض"; }
      else if (femDesc) { fard.father = new F(1, 6); label.father = "فرض وتعصيب"; }
    }
    if (gf && !jaddMode) {
      if (maleDesc) { fard.gf = new F(1, 6); label.gf = "فرض"; }
      else if (femDesc) { fard.gf = new F(1, 6); label.gf = "فرض وتعصيب"; }
    }

    // الأخوات (خارج باب الجد)
    let fsAsabaGhayr = false, psAsabaGhayr = false;
    if (!jaddMode) {
      if (fs && !fb) {
        if (femDesc) { fsAsabaGhayr = true; notes.push("الأخوات الشقيقات مع البنات عصبة مع الغير."); }
        else { fard.fs = fs === 1 ? new F(1, 2) : new F(2, 3); label.fs = "فرض"; }
      }
      if (fb) { block("pb", "الأخ الشقيق"); block("ps", "الأخ الشقيق"); pb = ps = 0; }
      else if (fsAsabaGhayr) { block("pb", "الأخت الشقيقة (عصبة مع الغير)"); block("ps", "الأخت الشقيقة (عصبة مع الغير)"); pb = ps = 0; }
      if (ps && !pb) {
        if (femDesc && !fs) { psAsabaGhayr = true; }
        else if (!fs) { fard.ps = ps === 1 ? new F(1, 2) : new F(2, 3); label.ps = "فرض"; }
        else if (fs === 1) { fard.ps = new F(1, 6); label.ps = "فرض (تكملة الثلثين)"; }
        else { block("ps", "الأختين الشقيقتين"); ps = 0; }
      }
    }

    let fardSum = Object.values(fard).reduce((a, b) => a.add(b), Z);

    /* --- الأكدرية --- */
    const onlyKeys = (keys) => present.every((k) => keys.includes(k) || res[k]?.blocked);
    if (jaddMode && gender === "f" && spouse && mother && gf && (fs + ps) === 1 && !fb && !pb &&
        onlyKeys(["husband", "mother", "gf", "fs", "ps"])) {
      const sk = fs ? "fs" : "ps";
      const out = {
        husband: { share: new F(9, 27), type: "فرض" }, mother: { share: new F(6, 27), type: "فرض" },
        gf: { share: new F(8, 27), type: "فرض ثم مقاسمة" }, [sk]: { share: new F(4, 27), type: "فرض ثم مقاسمة" },
      };
      Object.assign(res, out);
      notes.push("هذه المسألة الأكدرية: يُفرض للأخت النصف وللجد السدس فتعول إلى 9، ثم يُجمع نصيبهما ويُقسم للذكر مثل حظ الأنثيين فتصح من 27.");
      return { res, notes, asl: 6, awl: 9, tashih: 27, radd: false };
    }

    /* --- الجد مع الإخوة (المقاسمة عند الجمهور) --- */
    if (jaddMode) {
      const rem = ONE.sub(fardSum);
      const hasFull = fb + fs > 0;
      const units = 2 + fb * 2 + fs + pb * 2 + ps;
      const muq = rem.cmp(Z) > 0 ? rem.mul(2).div(units) : Z;
      const otherFard = fardSum.cmp(Z) > 0;
      let gfShare, how;
      if (!otherFard) {
        const third = new F(1, 3);
        gfShare = max(muq, third); how = gfShare === muq ? "المقاسمة" : "ثلث المال";
      } else {
        const sixth = new F(1, 6), thirdRem = rem.cmp(Z) > 0 ? rem.div(3) : Z;
        gfShare = max(sixth, thirdRem, muq);
        how = gfShare === muq ? "المقاسمة" : gfShare === sixth ? "سدس المال" : "ثلث الباقي";
      }
      res.gf = { share: gfShare, type: "مع الإخوة: " + how };
      fardSum = fardSum.add(gfShare);
      let sibRem = ONE.sub(fardSum);
      if (sibRem.cmp(Z) < 0) sibRem = Z;
      notes.push(`الجد مع الإخوة يأخذ الأحظّ له من: المقاسمة، أو ${otherFard ? "ثلث الباقي، أو سدس المال" : "ثلث المال"} — وهو قول الجمهور (المالكية والشافعية والحنابلة)، أما أبو حنيفة فيرى أن الجد يحجب الإخوة كالأب.`);
      if (hasFull && (pb + ps)) notes.push("الإخوة لأب عُدّوا على الجد في المقاسمة (المعادّة) ثم يُرد نصيبهم للأشقاء عند الحاجة.");

      const give = (bk, sk, b, s, amount, type) => {
        const u = b * 2 + s;
        if (!u) return;
        if (b) res[bk] = { share: amount.mul(b * 2).div(u), type };
        if (s) res[sk] = { share: amount.mul(s).div(u), type };
      };
      if (fb) {
        give("fb", "fs", fb, fs, sibRem, "تعصيب");
        if (pb) res.pb = { blocked: "الأخ الشقيق (بعد المعادّة)" };
        if (ps) res.ps = { blocked: "الأخ الشقيق (بعد المعادّة)" };
      } else if (fs) {
        if (pb + ps) {
          const cap = fs === 1 ? new F(1, 2) : new F(2, 3);
          const fsGet = sibRem.cmp(cap) > 0 ? cap : sibRem;
          res.fs = { share: fsGet, type: "تعصيب مع الجد" };
          const left = sibRem.sub(fsGet);
          if (left.cmp(Z) > 0) give("pb", "ps", pb, ps, left, "تعصيب");
          else { if (pb) res.pb = { blocked: "استغراق الأخت الشقيقة للباقي" }; if (ps) res.ps = { blocked: "استغراق الأخت الشقيقة للباقي" }; }
        } else give("fb", "fs", 0, fs, sibRem, "تعصيب مع الجد");
      } else give("pb", "ps", pb, ps, sibRem, "تعصيب مع الجد");
      ["fnep", "pnep", "fun", "pun", "fcous", "pcous"].forEach((k) => block(k, "الجد والإخوة"));
      fardSum = Object.values(fard).reduce((a, b) => a.add(b), Z).add(gfShare).add(sibRem.cmp(Z) > 0 ? sibRem : Z);
    }

    const fardDen = Object.values(fard).reduce((a, f) => lcm(a, f.d), 1);
    const hasFard = Object.keys(fard).length > 0;

    /* --- العصبات --- */
    let asabaTaken = jaddMode;
    if (!jaddMode) {
      const order = [
        ["son", "daughter", "الابن"],
        ["gson", "gdau", "ابن الابن"],
        ["father", null, "الأب"],
        ["gf", null, "الجد"],
        ["fb", "fs", "الأخ الشقيق"],
        [null, "fs", "الأخت الشقيقة", "ghayr"],
        ["pb", "ps", "الأخ لأب"],
        [null, "ps", "الأخت لأب", "ghayr"],
        ["fnep", null, "ابن الأخ الشقيق"],
        ["pnep", null, "ابن الأخ لأب"],
        ["fun", null, "العم الشقيق"],
        ["pun", null, "العم لأب"],
        ["fcous", null, "ابن العم الشقيق"],
        ["pcous", null, "ابن العم لأب"],
      ];
      const counts = { son, daughter: dau, gson, gdau, father, gf, fb, fs, pb, ps };
      const live = (k) => (k in counts ? counts[k] : cnt(k)) > 0 && !res[k]?.blocked;
      let winner = null;
      for (const [m, f, nm, kind] of order) {
        if (kind === "ghayr") {
          const ok = (f === "fs" && fsAsabaGhayr) || (f === "ps" && psAsabaGhayr);
          if (!ok) continue;
          if (!winner) winner = { m: null, f, nm };
          continue;
        }
        if (!m || !live(m)) continue;
        if (!winner) winner = { m, f, nm };
        else {
          block(m, winner.nm);
          // الإناث المرافقات للعاصب المحجوب
          if (f && live(f) && !fard[f]) block(f, winner.nm);
        }
      }
      let rem = ONE.sub(fardSum);
      if (winner) {
        asabaTaken = true;
        const r = rem.cmp(Z) > 0 ? rem : Z;
        const mC = winner.m ? (winner.m in counts ? counts[winner.m] : cnt(winner.m)) : 0;
        const fC = winner.f && !fard[winner.f] ? (winner.f in counts ? counts[winner.f] : cnt(winner.f)) : 0;
        const u = mC * 2 + fC;
        const tLabel = winner.m ? "تعصيب" : "تعصيب مع الغير";
        if (winner.m) {
          const s = r.mul(mC * 2).div(u);
          res[winner.m] = { share: (fard[winner.m] || Z).add(s), type: fard[winner.m] ? label[winner.m] : (fC ? "تعصيب بالغير" : tLabel) };
          if (fard[winner.m]) delete fard[winner.m];
        }
        if (fC) res[winner.f] = { share: r.mul(fC).div(u), type: winner.m ? "تعصيب بالغير" : tLabel };
        if (r.isZero() && (mC || fC)) notes.push(`لم يبقَ شيء بعد أصحاب الفروض، فلا شيء للعاصب (${winner.nm}).`);
        if (r.isZero() && winner.m === "fb" && matN >= 2 && spouse && (mother || gmm || gmf) && !desc && !father && !gf) {
          notes.push("هذه المسألة المُشتَرَكة (الحِماريّة): ذهب المالكية والشافعية إلى تشريك الأشقاء مع الإخوة لأم في الثلث، والنتيجة هنا على قول الحنفية والحنابلة.");
        }
      }
    }

    /* --- تجميع الفروض --- */
    for (const k in fard) if (!res[k]) res[k] = { share: fard[k], type: label[k] || "فرض" };

    // الأصل قبل العول/الرد
    let asl = 1;
    if (jaddMode || !hasFard) { for (const k in res) if (res[k].share && !res[k].share.isZero()) asl = lcm(asl, res[k].share.d); }
    else asl = fardDen;
    let total = Object.values(res).reduce((a, r) => (r.share ? a.add(r.share) : a), Z);
    let awl = null, radd = false;

    if (total.cmp(ONE) > 0) {
      awl = total.mul(asl).v;
      for (const k in res) if (res[k].share) res[k].share = res[k].share.div(total);
      notes.push(`زادت الفروض على أصل المسألة فعالت من ${asl} إلى ${Math.round(awl)}، ونقص نصيب كل وارث بنسبة العول.`);
    } else if (total.cmp(ONE) < 0 && !asabaTaken) {
      const nonSpouse = Object.keys(res).filter((k) => res[k].share && k !== "husband" && k !== "wife");
      const remain = ONE.sub(total);
      if (nonSpouse.length) {
        const base = nonSpouse.reduce((a, k) => a.add(res[k].share), Z);
        nonSpouse.forEach((k) => { res[k].share = res[k].share.add(remain.mul(res[k].share).div(base)); res[k].type += " وردّ"; });
        radd = true;
        notes.push("لا يوجد عاصب، فرُدّ الباقي على أصحاب الفروض بنسبة فروضهم عدا الزوجين.");
      } else if (res[spouseK]?.share) {
        res[spouseK].share = ONE; res[spouseK].type += " وردّ";
        radd = true;
        notes.push("لم يوجد وارث غير الزوج/الزوجة: الباقي يُرد عليه عند كثير من المعاصرين وفي عدد من قوانين الأحوال الشخصية، والأصل عند المتقدمين أنه لذوي الأرحام ثم لبيت المال.");
      }
    }
    if (!Object.values(res).some((r) => r.share && !r.share.isZero())) {
      notes.push("لا يوجد وارث من أصحاب الفروض أو العصبات؛ تنتقل التركة إلى ذوي الأرحام ثم بيت المال، ويُرجع في ذلك للمحكمة المختصة.");
    }

    // التصحيح: مقام مشترك لأنصبة الأفراد
    let tashih = 1;
    for (const k in res) if (res[k].share && !res[k].share.isZero()) tashih = lcm(tashih, res[k].share.div(c[k]).d);
    if (!jaddMode && !hasFard) asl = tashih;
    return { res, notes, asl, awl, tashih, radd };
  }

  /* ---------- الواجهة ---------- */
  const M = window.Mizan;
  const $ = (id) => document.getElementById(id);
  const app = $("inh-app");
  if (!app) return;
  const saved = M.store.get("inheritance", {});
  let gender = saved.gender || "m";
  const counts = saved.counts || {};
  ["inh-estate", "inh-funeral", "inh-debts", "inh-will"].forEach((id) => { if (saved[id]) $(id).value = saved[id]; });

  function renderHeirs() {
    let html = "", open = false;
    HEIRS.forEach((h) => {
      if (h.g) { if (open) html += "</div>"; html += `<h3 class="group-title" style="margin:18px 0 10px">${h.g}</h3><div class="heirs-grid">`; open = true; return; }
      if (h.only && h.only !== gender) return;
      const v = counts[h.k] || 0;
      html += `<div class="heir${v ? " on" : ""}" data-k="${h.k}"><span class="hname">${h.n}</span>
        <span class="mini-stepper"><button type="button" data-d="-1" aria-label="إنقاص ${h.n}" ${v ? "" : "disabled"}>−</button>
        <output aria-live="polite">${v}</output>
        <button type="button" data-d="1" aria-label="إضافة ${h.n}" ${v >= h.max ? "disabled" : ""}>+</button></span></div>`;
    });
    $("inh-heirs").innerHTML = html + "</div>";
  }

  function net() {
    const e = M.num($("inh-estate")), f = M.num($("inh-funeral")), d = M.num($("inh-debts"));
    const afterDebt = Math.max(0, e - f - d);
    const will = Math.min(M.num($("inh-will")), afterDebt / 3);
    return { value: Math.max(0, afterDebt - will), will, capped: M.num($("inh-will")) > afterDebt / 3 + 1e-9 };
  }

  function render() {
    $("inh-gender").querySelectorAll("button").forEach((b) => b.setAttribute("aria-selected", b.dataset.g === gender));
    const n = net();
    $("inh-net").textContent = M.num($("inh-estate")) ? M.fmt(n.value) + (n.capped ? " (قُصرت الوصية على الثلث)" : "") : "—";

    const c = {};
    HEIRS.forEach((h) => { if (h.k && (!h.only || h.only === gender) && counts[h.k]) c[h.k] = counts[h.k]; });
    const out = solve({ ...c }, gender);
    M.store.set("inheritance", { gender, counts, "inh-estate": $("inh-estate").value, "inh-funeral": $("inh-funeral").value, "inh-debts": $("inh-debts").value, "inh-will": $("inh-will").value });

    if (!out) {
      $("inh-result").hidden = true;
      $("inh-asl").textContent = "—";
      $("inh-kind").className = "badge gold"; $("inh-kind").textContent = "أضف الورثة";
      $("inh-bars").innerHTML = '<div class="empty-state"><p>اختر الورثة من القائمة لعرض التقسيم.</p></div>';
      return;
    }
    const { res, notes, asl, awl, tashih, radd } = out;
    $("inh-result").hidden = false;
    $("inh-asl").innerHTML = awl ? `${asl} <small>عالت إلى ${Math.round(awl)}</small>` : `${asl} <small>أصل المسألة</small>`;
    $("inh-kind").className = "badge " + (awl ? "no" : radd ? "gold" : "ok");
    $("inh-kind").textContent = awl ? "مسألة عائلة" : radd ? "مسألة فيها ردّ" : "مسألة عادلة";

    const rows = [], bars = [];
    let ci = 0;
    HEIRS.forEach((h) => {
      const k = h.k; if (!k || !c[k]) return;
      const r = res[k] || { blocked: "—" };
      if (r.blocked) {
        rows.push(`<tr class="blocked"><td>${NAME[k]}</td><td>${c[k]}</td><td>محجوب بـ${r.blocked}</td><td>—</td><td>0</td><td>0%</td><td>0</td><td>0</td></tr>`);
        return;
      }
      const s = r.share, per = s.div(c[k]);
      const pct = s.v * 100, amt = n.value * s.v;
      const sahm = Math.round(s.v * tashih);
      rows.push(`<tr><td><b>${NAME[k]}</b></td><td>${c[k]}</td><td>${r.type}</td><td><span class="frac">${s}</span></td><td>${sahm}</td><td>${M.fmt(pct, 2)}%</td><td>${n.value ? M.fmt(amt) : "—"}</td><td>${n.value && c[k] > 1 ? M.fmt(amt / c[k]) : c[k] > 1 ? `<span class="frac">${per}</span>` : "—"}</td></tr>`);
      const col = COLORS[ci++ % COLORS.length];
      bars.push(`<div class="share-row"><div class="top"><span>${NAME[k]}${c[k] > 1 ? " ×" + c[k] : ""}</span><b>${M.fmt(pct, 1)}%${n.value ? " · " + M.fmt(amt) : ""}</b></div><div class="bar"><i style="width:${pct}%;background:${col}"></i></div></div>`);
    });
    $("inh-tbody").innerHTML = rows.join("");
    $("inh-bars").innerHTML = bars.join("") || '<div class="empty-state"><p>لا يوجد وارث مستحق.</p></div>';
    $("inh-notes").innerHTML = (tashih !== asl && !awl ? `<div class="note"><p>تصح المسألة من <b>${tashih}</b> سهماً (عمود السهام).</p></div>` : awl && tashih !== Math.round(awl) ? `<div class="note"><p>تصح المسألة من <b>${tashih}</b> سهماً.</p></div>` : "") +
      notes.map((t) => `<div class="note"><p>${t}</p></div>`).join("");
    app._summary = HEIRS.filter((h) => h.k && c[h.k] && res[h.k]?.share).map((h) => `${NAME[h.k]}: ${res[h.k].share} (${M.fmt(res[h.k].share.v * 100, 1)}%)`).join("، ");
  }

  $("inh-heirs").addEventListener("click", (e) => {
    const b = e.target.closest("button[data-d]"); if (!b) return;
    const k = b.closest(".heir").dataset.k;
    const def = HEIRS.find((h) => h.k === k);
    counts[k] = Math.max(0, Math.min(def.max, (counts[k] || 0) + +b.dataset.d));
    renderHeirs(); render();
    const nb = $("inh-heirs").querySelector(`[data-k="${k}"] button[data-d="${b.dataset.d}"]`);
    (nb && !nb.disabled ? nb : $("inh-heirs").querySelector(`[data-k="${k}"] button:not([disabled])`))?.focus();
  });
  $("inh-gender").addEventListener("click", (e) => {
    const b = e.target.closest("button"); if (!b) return;
    gender = b.dataset.g; renderHeirs(); render();
  });
  app.addEventListener("input", (e) => { if (e.target.matches("input")) render(); });
  $("inh-reset").addEventListener("click", () => {
    Object.keys(counts).forEach((k) => delete counts[k]);
    ["inh-estate", "inh-funeral", "inh-debts", "inh-will"].forEach((id) => ($(id).value = ""));
    renderHeirs(); render(); M.toast("تم بدء مسألة جديدة");
  });
  $("inh-share").addEventListener("click", () => M.share("تقسيم التركة", "تقسيم التركة: " + (app._summary || "") + " — عبر حاسبة المواريث في موقع ميزان"));

  window.__mizanSolve = solve; // للاختبار
  renderHeirs(); render();
})();
