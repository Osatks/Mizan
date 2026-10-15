(function () {
  "use strict";
  const M = window.Mizan;
  const input = document.getElementById("tool-search");
  const cards = [...document.querySelectorAll("#tools .tool-card")];
  const empty = document.querySelector("#tools .no-results");
  const norm = (s) => s.replace(/[أإآ]/g, "ا").replace(/ة/g, "ه").replace(/ى/g, "ي").replace(/[ً-ْ]/g, "").toLowerCase();

  input?.addEventListener("input", () => {
    const q = norm(input.value.trim());
    let shown = 0;
    cards.forEach((c) => {
      const hit = !q || norm(c.textContent).includes(q);
      c.classList.toggle("hidden", !hit);
      if (hit) shown++;
    });
    empty.style.display = shown ? "none" : "block";
  });

  // أقرب مناسبة قادمة
  const pill = document.getElementById("hero-next-event");
  if (pill) {
    const today = new Date(); today.setHours(0, 0, 0, 0);
    const h = M.toHijri(today);
    const events = [[9, 1, "رمضان"], [10, 1, "عيد الفطر"], [12, 9, "يوم عرفة"], [12, 10, "عيد الأضحى"], [1, 1, "رأس السنة الهجرية"]];
    let best = null;
    for (const [m, d, name] of events) {
      for (const y of [h.y, h.y + 1]) {
        const g = M.fromHijri(y, m, d);
        if (g && g >= today) {
          const days = Math.round((g - today) / 86400000);
          if (!best || days < best.days) best = { name, days };
          break;
        }
      }
    }
    if (best) {
      pill.innerHTML = best.days === 0 ? `اليوم <b>${best.name}</b>` : `<b>${best.name}</b> بعد ${best.days} يوماً`;
      pill.hidden = false;
      pill.style.cursor = "pointer";
      pill.addEventListener("click", () => (location.href = "/islamic-events/"));
    }
  }
})();
