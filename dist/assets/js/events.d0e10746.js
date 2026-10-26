(function () {
  "use strict";
  const M = window.Mizan;
  const $ = (id) => document.getElementById(id);
  if (!$("events-app")) return;

  const EVENTS = [
    { m: 9, d: 1, name: t("شهر رمضان المبارك"), icon: "🌙", desc: t("شهر الصيام والقيام وتلاوة القرآن، أوله يُثبت برؤية الهلال.") },
    { m: 9, d: 21, name: t("العشر الأواخر من رمضان"), icon: "✨", desc: t("فيها ليلة القدر خير من ألف شهر، وكان النبي ﷺ يجتهد فيها ما لا يجتهد في غيرها.") },
    { m: 10, d: 1, name: t("عيد الفطر"), icon: "🎉", desc: t("يوم الجائزة بعد صيام رمضان، وتُخرج زكاة الفطر قبل صلاته.") },
    { m: 10, d: 2, name: t("صيام ست من شوال"), icon: "📿", desc: t("من صام رمضان ثم أتبعه ستاً من شوال كان كصيام الدهر (رواه مسلم).") },
    { m: 12, d: 1, name: t("العشر الأوائل من ذي الحجة"), icon: "🕋", desc: t("أفضل أيام الدنيا، يُستحب فيها الإكثار من العمل الصالح والتكبير.") },
    { m: 12, d: 8, name: t("يوم التروية"), icon: "⛺", desc: t("اليوم الذي يتوجه فيه الحجاج إلى منى استعداداً للوقوف بعرفة.") },
    { m: 12, d: 9, name: t("يوم عرفة"), icon: "🤲", desc: t("صيامه يكفّر السنة الماضية والباقية لغير الحاج (رواه مسلم).") },
    { m: 12, d: 10, name: t("عيد الأضحى"), icon: "🐑", desc: t("يوم النحر، تُذبح فيه الأضاحي، وتليه أيام التشريق الثلاثة.") },
    { m: 1, d: 1, name: t("رأس السنة الهجرية"), icon: "📅", desc: t("بداية عام هجري جديد وأول شهر محرم أحد الأشهر الحرم.") },
    { m: 1, d: 10, name: t("يوم عاشوراء"), icon: "🌊", desc: t("صيامه يكفّر السنة الماضية (رواه مسلم)، ويُستحب صيام التاسع معه.") },
    { m: 7, d: 1, name: t("شهر رجب"), icon: "🗓️", desc: t("أحد الأشهر الحرم الأربعة التي عظّم الله حرمتها.") },
    { m: 8, d: 1, name: t("شهر شعبان"), icon: "🌱", desc: t("كان النبي ﷺ يكثر فيه من الصيام استعداداً لرمضان.") },
  ];

  const now0 = () => { const d = new Date(); d.setHours(0, 0, 0, 0); return d; };
  function nextDate(ev) {
    const today = now0(), h = M.toHijri(today);
    for (const y of [h.y, h.y + 1, h.y + 2]) {
      const g = M.fromHijri(y, ev.m, ev.d);
      if (g && g >= today) return { g, y };
    }
    return null;
  }
  // الأيام البيض القادمة
  function whiteDays() {
    const today = now0(), h = M.toHijri(today);
    let y = h.y, m = h.m;
    if (h.d > 15) { m++; if (m > 12) { m = 1; y++; } }
    return { name: t("الأيام البيض"), icon: "🌕", desc: t("صيام 13 و14 و15 من كل شهر هجري، وصيام ثلاثة أيام من كل شهر كصيام الدهر (متفق عليه)."), g: M.fromHijri(y, m, 13), y, m, d: 13 };
  }

  const list = EVENTS.map((e) => { const r = nextDate(e); return r ? { ...e, g: r.g, y: r.y } : null; }).filter(Boolean);
  list.push(whiteDays());
  list.sort((a, b) => a.g - b.g);
  let sel = 0;
  const paramName = new URLSearchParams(location.search).get("e");
  if (paramName) { const i = list.findIndex((x) => x.name.includes(paramName)); if (i >= 0) sel = i; }

  function renderList() {
    const today = now0();
    $("ev-list").innerHTML = list.map((e, i) => {
      const days = Math.round((e.g - today) / 86400000);
      return `<button type="button" class="event-card${i === sel ? " active" : ""}" data-i="${i}">
        <h3>${e.icon} ${e.name}</h3>
        <span class="d">${t("{d} {month} {y} هـ", { d: e.d, month: M.HIJRI_MONTHS[e.m - 1], y: e.y })} · ${M.WEEKDAYS[e.g.getDay()]} ${M.gregStr(e.g)}</span>
        <span class="left">${days === 0 ? t("اليوم") : days === 1 ? t("غداً") : t("بعد {n} يوماً", { n: days })}</span></button>`;
    }).join("");
  }

  function tick() {
    const e = list[sel];
    $("ev-name").textContent = `${e.icon} ${e.name}`;
    $("ev-date").textContent = t("{hijri} — الموافق {weekday} {date} (تقريباً)", { hijri: t("{d} {month} {y} هـ", { d: e.d, month: M.HIJRI_MONTHS[e.m - 1], y: e.y }), weekday: M.WEEKDAYS[e.g.getDay()], date: M.gregStr(e.g) });
    $("ev-desc").textContent = e.desc;
    // يبدأ اليوم الهجري من مغرب الليلة السابقة، نعتمد هنا منتصف الليل للتبسيط
    let s = Math.max(0, Math.floor((e.g - new Date()) / 1000));
    $("ev-d").textContent = Math.floor(s / 86400); s %= 86400;
    $("ev-h").textContent = Math.floor(s / 3600); s %= 3600;
    $("ev-m").textContent = Math.floor(s / 60);
    $("ev-s").textContent = s % 60;
  }

  $("ev-list").addEventListener("click", (e) => {
    const b = e.target.closest("[data-i]"); if (!b) return;
    sel = +b.dataset.i; renderList(); tick();
    $("events-app").scrollIntoView({ block: "start" });
  });
  $("ev-share").addEventListener("click", () => {
    const e = list[sel], days = Math.round((e.g - now0()) / 86400000);
    M.share(e.name, t("باقي {n} يوماً على {name} ({date} تقريباً)", { n: days, name: e.name, date: M.gregStr(e.g) }));
  });
  renderList(); tick();
  setInterval(tick, 1000);
})();
