(function () {
  "use strict";
  const M = window.Mizan, P = window.PrayTimes;
  const $ = (id) => document.getElementById(id);
  if (!$("qibla-app")) return;
  let loc = P.getLocation();
  let qibla = 0, heading = null;

  // علامات البوصلة
  let ticks = "";
  for (let a = 0; a < 360; a += 10) {
    const long = a % 30 === 0, r1 = 96, r2 = long ? 86 : 90;
    const rad = (a * Math.PI) / 180;
    ticks += `<line x1="${100 + r1 * Math.sin(rad)}" y1="${100 - r1 * Math.cos(rad)}" x2="${100 + r2 * Math.sin(rad)}" y2="${100 - r2 * Math.cos(rad)}" stroke="currentColor" stroke-width="${long ? 1.6 : 0.8}" opacity="${long ? 0.6 : 0.35}"/>`;
  }
  $("qb-ticks").innerHTML = ticks;

  const DIRS = ["الشمال", "الشمال الشرقي", "الشرق", "الجنوب الشرقي", "الجنوب", "الجنوب الغربي", "الغرب", "الشمال الغربي"];
  const dirName = (a) => DIRS[Math.round(a / 45) % 8];

  function renderCity() {
    $("qb-city").innerHTML = (loc.cityIndex === -1 ? `<option value="-1" selected>📍 ${loc.name}</option>` : "") + P.citySelectHTML(loc.cityIndex);
  }

  function update() {
    qibla = P.qibla(loc.lat, loc.lng);
    $("qb-angle").innerHTML = `${qibla.toFixed(1)}°`;
    $("qb-dir").textContent = "باتجاه " + dirName(qibla);
    $("qb-loc").textContent = loc.name;
    $("qb-dist").textContent = M.fmt(P.distanceKm(loc.lat, loc.lng), 0) + " كم";
    $("qb-needle").style.transform = `rotate(${qibla}deg)`;
    rotate();
  }

  let smooth = null;
  function rotate() {
    if (heading == null) { $("qb-dial").style.transform = "none"; return; }
    // تنعيم القراءة مع مراعاة الالتفاف حول 360
    if (smooth == null) smooth = heading;
    let d = ((heading - smooth + 540) % 360) - 180;
    smooth = (smooth + d * 0.25 + 360) % 360;
    $("qb-dial").style.transform = `rotate(${-smooth}deg)`;
    $("qb-heading").textContent = `${Math.round(smooth)}°`;
    const off = Math.abs(((qibla - smooth + 540) % 360) - 180);
    const aligned = off < 5;
    $("qb-compass").classList.toggle("aligned", aligned);
    const st = $("qb-status");
    st.className = "badge " + (aligned ? "ok" : "gold");
    st.textContent = aligned ? "✓ أنت متجه نحو القبلة" : `أدِر هاتفك ${(((qibla - smooth + 360) % 360) < 180 ? "يميناً" : "يساراً")} بمقدار ${Math.round(off)}°`;
    if (aligned && navigator.vibrate && !rotate._v) { navigator.vibrate(60); rotate._v = true; }
    if (!aligned) rotate._v = false;
  }

  function onOrient(e) {
    let h = null;
    if (typeof e.webkitCompassHeading === "number") h = e.webkitCompassHeading;
    else if (e.absolute && e.alpha != null) h = 360 - e.alpha;
    else if (e.alpha != null && e.type === "deviceorientationabsolute") h = 360 - e.alpha;
    if (h == null) return;
    heading = (h + (screen.orientation?.angle || 0)) % 360;
    rotate();
  }

  $("qb-start").addEventListener("click", async () => {
    try {
      if (typeof DeviceOrientationEvent !== "undefined" && typeof DeviceOrientationEvent.requestPermission === "function") {
        const r = await DeviceOrientationEvent.requestPermission();
        if (r !== "granted") throw new Error("تم رفض إذن البوصلة");
      }
      if ("ondeviceorientationabsolute" in window) addEventListener("deviceorientationabsolute", onOrient);
      else addEventListener("deviceorientation", onOrient);
      $("qb-status").textContent = "حرّك هاتفك ببطء…";
      setTimeout(() => { if (heading == null) { $("qb-status").textContent = "لا تتوفر بوصلة في هذا الجهاز. استخدم الزاوية الموضحة مع بوصلة عادية."; } }, 2500);
    } catch (err) { M.toast(err.message || "تعذر تشغيل البوصلة"); }
  });

  $("qb-gps").addEventListener("click", async () => {
    const b = $("qb-gps"); b.disabled = true;
    try { loc = await P.geolocate(); P.setLocation(loc); renderCity(); update(); M.toast("تم تحديد موقعك ✓"); }
    catch (e) { M.toast(e.message); }
    b.disabled = false;
  });
  $("qb-city").addEventListener("change", () => {
    const i = +$("qb-city").value; if (i < 0) return;
    loc = { ...P.CITIES[i], cityIndex: i }; P.setLocation(loc); update();
  });

  renderCity(); update();
})();
