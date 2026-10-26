/* حساب مواقيت الصلاة فلكياً + اتجاه القبلة + المدن
   الخوارزمية مبنية على المعادلات الفلكية القياسية لموضع الشمس */
(function () {
  "use strict";
  const dtr = (d) => (d * Math.PI) / 180, rtd = (r) => (r * 180) / Math.PI;
  const sin = (d) => Math.sin(dtr(d)), cos = (d) => Math.cos(dtr(d)), tan = (d) => Math.tan(dtr(d));
  const asin = (x) => rtd(Math.asin(x)), acos = (x) => rtd(Math.acos(x)), atan2 = (y, x) => rtd(Math.atan2(y, x));
  const acot = (x) => rtd(Math.atan(1 / x));
  const fix = (a, b) => { a = a - b * Math.floor(a / b); return a < 0 ? a + b : a; };
  const fixAngle = (a) => fix(a, 360), fixHour = (a) => fix(a, 24);

  const METHODS = {
    makkah: { name: t("أم القرى (السعودية)"), fajr: 18.5, isha: { min: 90 } },
    mwl: { name: t("رابطة العالم الإسلامي"), fajr: 18, isha: 17 },
    egypt: { name: t("الهيئة المصرية العامة للمساحة"), fajr: 19.5, isha: 17.5 },
    dubai: { name: t("الإمارات (دبي)"), fajr: 18.2, isha: 18.2 },
    kuwait: { name: t("الكويت|بلد"), fajr: 18, isha: 17.5 },
    qatar: { name: t("قطر"), fajr: 18, isha: { min: 90 } },
    jordan: { name: t("الأردن وفلسطين"), fajr: 18, isha: 18 },
    morocco: { name: t("المغرب|بلد"), fajr: 19, isha: 17 },
    algeria: { name: t("الجزائر|بلد"), fajr: 18, isha: 17 },
    tunisia: { name: t("تونس|بلد"), fajr: 18, isha: 18 },
    karachi: { name: t("جامعة العلوم الإسلامية بكراتشي"), fajr: 18, isha: 18 },
    turkey: { name: t("رئاسة الشؤون الدينية التركية"), fajr: 18, isha: 17 },
    isna: { name: t("أمريكا الشمالية (ISNA)"), fajr: 15, isha: 15 },
    france: { name: t("اتحاد المنظمات الإسلامية بفرنسا"), fajr: 12, isha: 12 },
  };

  // [الاسم، الدولة، خط العرض، خط الطول، المنطقة الزمنية، طريقة الحساب]
  const CITIES = [
    [t("مكة المكرمة"), t("السعودية"), 21.4225, 39.8262, "Asia/Riyadh", "makkah"],
    [t("المدينة المنورة"), t("السعودية"), 24.4672, 39.6111, "Asia/Riyadh", "makkah"],
    [t("الرياض"), t("السعودية"), 24.7136, 46.6753, "Asia/Riyadh", "makkah"],
    [t("جدة"), t("السعودية"), 21.5433, 39.1728, "Asia/Riyadh", "makkah"],
    [t("الدمام"), t("السعودية"), 26.4207, 50.0888, "Asia/Riyadh", "makkah"],
    [t("أبها"), t("السعودية"), 18.2164, 42.5053, "Asia/Riyadh", "makkah"],
    [t("دبي"), t("الإمارات"), 25.2048, 55.2708, "Asia/Dubai", "dubai"],
    [t("أبوظبي"), t("الإمارات"), 24.4539, 54.3773, "Asia/Dubai", "dubai"],
    [t("الشارقة"), t("الإمارات"), 25.3463, 55.4209, "Asia/Dubai", "dubai"],
    [t("الكويت"), t("الكويت|بلد"), 29.3759, 47.9774, "Asia/Kuwait", "kuwait"],
    [t("الدوحة"), t("قطر"), 25.2854, 51.531, "Asia/Qatar", "qatar"],
    [t("المنامة"), t("البحرين"), 26.2285, 50.586, "Asia/Bahrain", "makkah"],
    [t("مسقط"), t("عُمان"), 23.588, 58.3829, "Asia/Muscat", "mwl"],
    [t("القاهرة"), t("مصر"), 30.0444, 31.2357, "Africa/Cairo", "egypt"],
    [t("الإسكندرية"), t("مصر"), 31.2001, 29.9187, "Africa/Cairo", "egypt"],
    [t("أسوان"), t("مصر"), 24.0889, 32.8998, "Africa/Cairo", "egypt"],
    [t("عمّان"), t("الأردن"), 31.9454, 35.9284, "Asia/Amman", "jordan"],
    [t("القدس"), t("فلسطين"), 31.7683, 35.2137, "Asia/Jerusalem", "jordan"],
    [t("غزة"), t("فلسطين"), 31.5017, 34.4668, "Asia/Gaza", "jordan"],
    [t("بيروت"), t("لبنان"), 33.8938, 35.5018, "Asia/Beirut", "mwl"],
    [t("دمشق"), t("سوريا"), 33.5138, 36.2765, "Asia/Damascus", "mwl"],
    [t("حلب"), t("سوريا"), 36.2021, 37.1343, "Asia/Damascus", "mwl"],
    [t("بغداد"), t("العراق"), 33.3152, 44.3661, "Asia/Baghdad", "mwl"],
    [t("البصرة"), t("العراق"), 30.5085, 47.7804, "Asia/Baghdad", "mwl"],
    [t("أربيل"), t("العراق"), 36.1912, 44.0092, "Asia/Baghdad", "mwl"],
    [t("صنعاء"), t("اليمن"), 15.3694, 44.191, "Asia/Aden", "mwl"],
    [t("عدن"), t("اليمن"), 12.7855, 45.0187, "Asia/Aden", "mwl"],
    [t("الخرطوم"), t("السودان"), 15.5007, 32.5599, "Africa/Khartoum", "egypt"],
    [t("طرابلس"), t("ليبيا"), 32.8872, 13.1913, "Africa/Tripoli", "egypt"],
    [t("بنغازي"), t("ليبيا"), 32.1167, 20.0667, "Africa/Tripoli", "egypt"],
    [t("تونس"), t("تونس|بلد"), 36.8065, 10.1815, "Africa/Tunis", "tunisia"],
    [t("الجزائر"), t("الجزائر|بلد"), 36.7538, 3.0588, "Africa/Algiers", "algeria"],
    [t("وهران"), t("الجزائر|بلد"), 35.6971, -0.6308, "Africa/Algiers", "algeria"],
    [t("الرباط"), t("المغرب|بلد"), 34.0209, -6.8416, "Africa/Casablanca", "morocco"],
    [t("الدار البيضاء"), t("المغرب|بلد"), 33.5731, -7.5898, "Africa/Casablanca", "morocco"],
    [t("مراكش"), t("المغرب|بلد"), 31.6295, -7.9811, "Africa/Casablanca", "morocco"],
    [t("نواكشوط"), t("موريتانيا"), 18.0735, -15.9582, "Africa/Nouakchott", "mwl"],
    [t("مقديشو"), t("الصومال"), 2.0469, 45.3182, "Africa/Mogadishu", "mwl"],
    [t("جيبوتي"), t("جيبوتي"), 11.5721, 43.1456, "Africa/Djibouti", "mwl"],
    [t("إسطنبول"), t("تركيا"), 41.0082, 28.9784, "Europe/Istanbul", "turkey"],
    [t("كراتشي"), t("باكستان"), 24.8607, 67.0011, "Asia/Karachi", "karachi"],
    [t("كوالالمبور"), t("ماليزيا"), 3.139, 101.6869, "Asia/Kuala_Lumpur", "mwl"],
    [t("جاكرتا"), t("إندونيسيا"), -6.2088, 106.8456, "Asia/Jakarta", "mwl"],
    [t("لندن"), t("بريطانيا"), 51.5074, -0.1278, "Europe/London", "mwl"],
    [t("باريس"), t("فرنسا"), 48.8566, 2.3522, "Europe/Paris", "france"],
    [t("برلين"), t("ألمانيا"), 52.52, 13.405, "Europe/Berlin", "mwl"],
    [t("ستوكهولم"), t("السويد"), 59.3293, 18.0686, "Europe/Stockholm", "mwl"],
    [t("نيويورك"), t("الولايات المتحدة"), 40.7128, -74.006, "America/New_York", "isna"],
    [t("تورونتو"), t("كندا"), 43.6532, -79.3832, "America/Toronto", "isna"],
    [t("سيدني"), t("أستراليا"), -33.8688, 151.2093, "Australia/Sydney", "mwl"],
  ].map(([name, country, lat, lng, tz, method]) => ({ name, country, lat, lng, tz, method }));

  /** فرق التوقيت بالساعات لمنطقة زمنية في تاريخ معين */
  function tzOffset(tz, date) {
    if (!tz) return -date.getTimezoneOffset() / 60;
    try {
      const f = new Intl.DateTimeFormat("en-US", { timeZone: tz, hourCycle: "h23", year: "numeric", month: "numeric", day: "numeric", hour: "numeric", minute: "numeric" });
      const noon = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate(), 12));
      const p = Object.fromEntries(f.formatToParts(noon).map((x) => [x.type, x.value]));
      const asUTC = Date.UTC(+p.year, +p.month - 1, +p.day, +p.hour % 24, +p.minute);
      return (asUTC - noon.getTime()) / 3600000;
    } catch (e) { return -date.getTimezoneOffset() / 60; }
  }

  function julian(y, m, d) {
    if (m <= 2) { y -= 1; m += 12; }
    const A = Math.floor(y / 100), B = 2 - A + Math.floor(A / 4);
    return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + B - 1524.5;
  }

  function sunPosition(jd) {
    const D = jd - 2451545.0;
    const g = fixAngle(357.529 + 0.98560028 * D);
    const q = fixAngle(280.459 + 0.98564736 * D);
    const L = fixAngle(q + 1.915 * sin(g) + 0.020 * sin(2 * g));
    const e = 23.439 - 0.00000036 * D;
    const RA = atan2(cos(e) * sin(L), cos(L)) / 15;
    return { decl: asin(sin(e) * sin(L)), eqt: q / 15 - fixHour(RA) };
  }

  /**
   * يحسب مواقيت يوم واحد
   * opts: {lat,lng,tz,method,asr:'std'|'hanafi',hijriMonth}
   * يعيد الأوقات بالساعات العشرية حسب التوقيت المحلي للمدينة
   */
  function compute(date, opts) {
    const { lat, lng } = opts;
    const m = METHODS[opts.method] || METHODS.mwl;
    const tz = tzOffset(opts.tz, date);
    const jDate = julian(date.getFullYear(), date.getMonth() + 1, date.getDate()) - lng / (15 * 24);

    const midDay = (t) => fixHour(12 - sunPosition(jDate + t).eqt);
    const sunAngleTime = (angle, t, ccw) => {
      const decl = sunPosition(jDate + t).decl;
      const noon = midDay(t);
      const x = (-sin(angle) - sin(decl) * sin(lat)) / (cos(decl) * cos(lat));
      if (x < -1 || x > 1) return NaN;
      const tt = acos(x) / 15;
      return noon + (ccw ? -tt : tt);
    };
    const asrTime = (factor, t) => {
      const decl = sunPosition(jDate + t).decl;
      return sunAngleTime(-acot(factor + tan(Math.abs(lat - decl))), t);
    };

    // تقديرات أولية ثم تكرار لتحسين الدقة
    let t = { fajr: 5, sunrise: 6, dhuhr: 12, asr: 13, sunset: 18, isha: 18 };
    for (let i = 0; i < 2; i++) {
      const p = Object.fromEntries(Object.entries(t).map(([k, v]) => [k, (isNaN(v) ? 12 : v) / 24]));
      t = {
        fajr: sunAngleTime(m.fajr, p.fajr, true),
        sunrise: sunAngleTime(0.833, p.sunrise, true),
        dhuhr: midDay(p.dhuhr),
        asr: asrTime(opts.asr === "hanafi" ? 2 : 1, p.asr),
        sunset: sunAngleTime(0.833, p.sunset),
        isha: typeof m.isha === "number" ? sunAngleTime(m.isha, p.isha) : NaN,
      };
    }
    const adj = tz - lng / 15;
    for (const k in t) t[k] += adj;
    t.maghrib = t.sunset;
    if (typeof m.isha === "object") t.isha = t.maghrib + (opts.hijriMonth === 9 && opts.method === "makkah" ? 120 : m.isha.min) / 60;

    // تعديل خطوط العرض العليا (طريقة الزاوية)
    const night = fixHour(t.sunrise - t.sunset + 24);
    const fajrMax = (m.fajr / 60) * night;
    if (isNaN(t.fajr) || fixHour(t.sunrise - t.fajr) > fajrMax) t.fajr = t.sunrise - fajrMax;
    if (typeof m.isha === "number") {
      const ishaMax = (m.isha / 60) * night;
      if (isNaN(t.isha) || fixHour(t.isha - t.sunset) > ishaMax) t.isha = t.sunset + ishaMax;
    }
    // احتياط دقيقة واحدة للظهر كما تفعل أغلب التقاويم
    t.dhuhr += 1 / 60;
    delete t.sunset;
    return t;
  }

  /** يحوّل الساعات العشرية إلى كائن Date في يوم معين */
  function toDate(date, hours) {
    const mins = Math.round(hours * 60);
    return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 0, mins);
  }
  function fmtTime(hours, h12) {
    if (isNaN(hours)) return "—";
    let mins = Math.round(fixHour(hours) * 60);
    let h = Math.floor(mins / 60) % 24, m = mins % 60;
    const mm = String(m).padStart(2, "0");
    if (!h12) return `${String(h).padStart(2, "0")}:${mm}`;
    const am = h < 12;
    h = h % 12 || 12;
    return am ? t("{time} ص", { time: `${h}:${mm}` }) : t("{time} م", { time: `${h}:${mm}` });
  }

  const KAABA = { lat: 21.4225, lng: 39.8262 };
  function qibla(lat, lng) {
    const dl = dtr(KAABA.lng - lng), p1 = dtr(lat), p2 = dtr(KAABA.lat);
    const b = Math.atan2(Math.sin(dl), Math.cos(p1) * Math.tan(p2) - Math.sin(p1) * Math.cos(dl));
    return fixAngle(rtd(b));
  }
  function distanceKm(lat, lng) {
    const R = 6371, dLat = dtr(KAABA.lat - lat), dLng = dtr(KAABA.lng - lng);
    const a = Math.sin(dLat / 2) ** 2 + Math.cos(dtr(lat)) * Math.cos(dtr(KAABA.lat)) * Math.sin(dLng / 2) ** 2;
    return 2 * R * Math.asin(Math.sqrt(a));
  }

  /** موقع المستخدم: المحفوظ أو مكة افتراضياً */
  function getLocation() {
    const loc = window.Mizan.store.get("location", { ...CITIES[0], cityIndex: 0 });
    // الاسم المحفوظ قد يكون بلغة أخرى، فيُؤخذ اسم المدينة ودولتها من القائمة بلغة الصفحة
    const c = CITIES[loc.cityIndex];
    if (c) { loc.name = c.name; loc.country = c.country; }
    return loc;
  }
  function setLocation(loc) { window.Mizan.store.set("location", loc); }

  function geolocate() {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) return reject(new Error(t("المتصفح لا يدعم تحديد الموقع")));
      navigator.geolocation.getCurrentPosition(
        (p) => {
          const lat = +p.coords.latitude.toFixed(4), lng = +p.coords.longitude.toFixed(4);
          // أقرب مدينة معروفة لاقتراح طريقة الحساب
          let best = CITIES[0], bd = Infinity;
          CITIES.forEach((c) => { const d = (c.lat - lat) ** 2 + (c.lng - lng) ** 2; if (d < bd) { bd = d; best = c; } });
          const tz = Intl.DateTimeFormat().resolvedOptions().timeZone;
          resolve({ name: bd < 1.5 ? best.name : t("موقعي الحالي"), country: bd < 1.5 ? best.country : "", lat, lng, tz, method: best.method, cityIndex: -1, gps: true });
        },
        (err) => reject(new Error(err.code === 1 ? t("تم رفض إذن الوصول إلى الموقع") : t("تعذر تحديد الموقع"))),
        { enableHighAccuracy: false, timeout: 12000, maximumAge: 600000 }
      );
    });
  }

  function citySelectHTML(selected) {
    const byCountry = {};
    CITIES.forEach((c, i) => (byCountry[c.country] = byCountry[c.country] || []).push([c, i]));
    return Object.entries(byCountry).map(([country, list]) =>
      `<optgroup label="${country}">` + list.map(([c, i]) => `<option value="${i}"${i === selected ? " selected" : ""}>${c.name}</option>`).join("") + "</optgroup>").join("");
  }

  window.PrayTimes = { METHODS, CITIES, compute, toDate, fmtTime, qibla, distanceKm, getLocation, setLocation, geolocate, citySelectHTML, tzOffset };
})();
