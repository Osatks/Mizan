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

  // مثل PrayTimes.METHODS في التطبيق: min دقائق الجهة الرسمية لكل وقت بعد حسابه (مطابقة لجدولها المنشور؛ الغائب صفر،
  // وما لا min له دقيقة احتياط للظهر)، و maghrib زاوية المغرب، و elev معامل ارتفاع المكان، و daily موضع الشمس مرة لليوم،
  // و dep نسبتا أعمق انخفاض للشمس صيفاً، و seasonal حد لجنة رؤية الهلال الموسمي، و ramadan دقائق رمضان
  const METHODS = {
    makkah: { name: t("أم القرى (السعودية)"), fajr: 18.5, isha: { min: 90 }, min: { fajr: 0.5, sunrise: -0.5, dhuhr: 0.5, asr: 0.5, maghrib: 0.5 } },
    mwl: { name: t("رابطة العالم الإسلامي"), fajr: 18, isha: 17 },
    egypt: { name: t("الهيئة المصرية العامة للمساحة"), fajr: 19.5, isha: 17.5, min: {} },
    dubai: { name: t("الإمارات (دبي)"), fajr: 18, isha: 18, maghrib: 1.38, min: { fajr: -1.1, sunrise: -3.6, dhuhr: 2.3, asr: 0.45, maghrib: 0.3, isha: 0.3 } },
    kuwait: { name: t("الكويت|بلد"), fajr: 18, isha: 17.5 },
    qatar: { name: t("قطر"), fajr: 18, isha: { min: 90 }, min: { fajr: -1.1, maghrib: 1.95 }, ramadan: { maghrib: 1.05 } },
    bahrain: { name: t("البحرين"), fajr: 18, isha: 18, min: { fajr: -0.9, dhuhr: 1, maghrib: 0.5, isha: -1.1 } },
    jordan: { name: t("الأردن"), fajr: 18, isha: 18, min: { fajr: -0.25, sunrise: -6.27, dhuhr: 0.36, asr: 0.4, maghrib: 6.45, isha: 0.34 } },
    palestine: { name: t("فلسطين"), fajr: 18, isha: 18, min: { fajr: -0.25, sunrise: -3, dhuhr: -0.5, asr: -0.4, maghrib: 6, isha: -0.55 } },
    lebanon: { name: t("لبنان"), fajr: 19.2, isha: 17.3, min: { sunrise: 1.35, dhuhr: 0.2, asr: -0.6, maghrib: 3.4 } },
    syria: { name: t("سوريا"), fajr: 18, isha: 17, min: { fajr: -0.4, sunrise: -0.6, dhuhr: 0.4, asr: 0.6, maghrib: 2.4, isha: 0.6 } },
    libya: { name: t("ليبيا"), fajr: 18, isha: 18, min: { fajr: -2.6, sunrise: -0.8, dhuhr: 3.1, asr: -0.8, maghrib: 3.4, isha: 1.2 } },
    morocco: { name: t("المغرب|بلد"), fajr: 19, isha: 17, min: { fajr: -0.3, sunrise: -2.2, dhuhr: 5.2, asr: 0.25, maghrib: 3.4, isha: 0.2 }, elev: 0.7 },
    algeria: { name: t("الجزائر|بلد"), fajr: 18, isha: 17, min: { fajr: 0.5, dhuhr: 1, maghrib: 3.5 } },
    tunisia: { name: t("تونس|بلد"), fajr: 18, isha: 18, min: { fajr: -0.5, sunrise: -0.5, dhuhr: 7, asr: 0, maghrib: 2.5, isha: 0.5 } },
    karachi: { name: t("جامعة العلوم الإسلامية بكراتشي"), fajr: 18, isha: 18 },
    turkey: { name: t("رئاسة الشؤون الدينية التركية"), fajr: 18, isha: 17, min: { sunrise: -7, dhuhr: 5, asr: 4, maghrib: 7 }, daily: true },
    europe: { name: t("الطريقة الأوروبية الموحدة (إسطنبول 2021)"), fajr: 18, isha: 16, min: { sunrise: -7, dhuhr: 5, asr: 4, maghrib: 7 }, daily: true, dep: [0.66, 0.62] },
    malaysia: { name: t("ماليزيا"), fajr: 18, isha: 18, min: { fajr: 1.8, sunrise: -0.9, dhuhr: 2.2, asr: 1.7, maghrib: 1.7, isha: 1.8 } },
    indonesia: { name: t("إندونيسيا"), fajr: 20, isha: 18, min: { fajr: 2.5, sunrise: -3.15, dhuhr: 3.55, asr: 2.55, maghrib: 3.2, isha: 2.5 }, elev: 1 },
    uk: { name: t("بريطانيا"), fajr: 18, isha: 18, min: { sunrise: -3, dhuhr: 5, maghrib: 3 }, seasonal: true },
    paris: { name: t("باريس"), fajr: 18, isha: { min: 87 }, min: { dhuhr: 5, asr: -0.8, maghrib: 3 }, seasonal: true },
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
    [t("المنامة"), t("البحرين"), 26.2285, 50.586, "Asia/Bahrain", "bahrain"],
    [t("مسقط"), t("عُمان"), 23.588, 58.3829, "Asia/Muscat", "mwl"],
    [t("القاهرة"), t("مصر"), 30.0444, 31.2357, "Africa/Cairo", "egypt"],
    [t("الإسكندرية"), t("مصر"), 31.2001, 29.9187, "Africa/Cairo", "egypt"],
    [t("أسوان"), t("مصر"), 24.0889, 32.8998, "Africa/Cairo", "egypt"],
    [t("عمّان"), t("الأردن"), 31.9454, 35.9284, "Asia/Amman", "jordan"],
    [t("القدس"), t("فلسطين"), 31.7683, 35.2137, "Asia/Jerusalem", "palestine"],
    [t("غزة"), t("فلسطين"), 31.5017, 34.4668, "Asia/Gaza", "palestine"],
    [t("بيروت"), t("لبنان"), 33.8938, 35.5018, "Asia/Beirut", "lebanon"],
    [t("دمشق"), t("سوريا"), 33.5138, 36.2765, "Asia/Damascus", "syria"],
    [t("حلب"), t("سوريا"), 36.2021, 37.1343, "Asia/Damascus", "syria"],
    [t("بغداد"), t("العراق"), 33.3152, 44.3661, "Asia/Baghdad", "mwl"],
    [t("البصرة"), t("العراق"), 30.5085, 47.7804, "Asia/Baghdad", "mwl"],
    [t("أربيل"), t("العراق"), 36.1912, 44.0092, "Asia/Baghdad", "mwl"],
    [t("صنعاء"), t("اليمن"), 15.3694, 44.191, "Asia/Aden", "mwl"],
    [t("عدن"), t("اليمن"), 12.7855, 45.0187, "Asia/Aden", "mwl"],
    [t("الخرطوم"), t("السودان"), 15.5007, 32.5599, "Africa/Khartoum", "egypt"],
    [t("طرابلس"), t("ليبيا"), 32.8872, 13.1913, "Africa/Tripoli", "libya"],
    [t("بنغازي"), t("ليبيا"), 32.1167, 20.0667, "Africa/Tripoli", "libya"],
    [t("تونس"), t("تونس|بلد"), 36.8065, 10.1815, "Africa/Tunis", "tunisia"],
    [t("الجزائر"), t("الجزائر|بلد"), 36.7538, 3.0588, "Africa/Algiers", "algeria"],
    [t("وهران"), t("الجزائر|بلد"), 35.6971, -0.6308, "Africa/Algiers", "algeria"],
    [t("الرباط"), t("المغرب|بلد"), 34.0209, -6.8416, "Africa/Casablanca", "morocco", 50],
    [t("الدار البيضاء"), t("المغرب|بلد"), 33.5731, -7.5898, "Africa/Casablanca", "morocco", 50],
    [t("مراكش"), t("المغرب|بلد"), 31.6295, -7.9811, "Africa/Casablanca", "morocco", 466],
    [t("نواكشوط"), t("موريتانيا"), 18.0735, -15.9582, "Africa/Nouakchott", "mwl"],
    [t("مقديشو"), t("الصومال"), 2.0469, 45.3182, "Africa/Mogadishu", "mwl"],
    [t("جيبوتي"), t("جيبوتي"), 11.5721, 43.1456, "Africa/Djibouti", "mwl"],
    [t("إسطنبول"), t("تركيا"), 41.0082, 28.9784, "Europe/Istanbul", "turkey"],
    [t("كراتشي"), t("باكستان"), 24.8607, 67.0011, "Asia/Karachi", "karachi"],
    [t("كوالالمبور"), t("ماليزيا"), 3.139, 101.6869, "Asia/Kuala_Lumpur", "malaysia"],
    [t("جاكرتا"), t("إندونيسيا"), -6.2088, 106.8456, "Asia/Jakarta", "indonesia"],
    [t("لندن"), t("بريطانيا"), 51.5074, -0.1278, "Europe/London", "uk"],
    [t("باريس"), t("فرنسا"), 48.8566, 2.3522, "Europe/Paris", "paris"],
    [t("برلين"), t("ألمانيا"), 52.52, 13.405, "Europe/Berlin", "europe"],
    [t("ستوكهولم"), t("السويد"), 59.3293, 18.0686, "Europe/Stockholm", "europe"],
    [t("نيويورك"), t("الولايات المتحدة"), 40.7128, -74.006, "America/New_York", "isna"],
    [t("تورونتو"), t("كندا"), 43.6532, -79.3832, "America/Toronto", "isna"],
    [t("سيدني"), t("أستراليا"), -33.8688, 151.2093, "Australia/Sydney", "mwl"],
  ].map(([name, country, lat, lng, tz, method, elev = 0]) => ({ name, country, lat, lng, tz, method, elev }));

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
    const jd0 = julian(date.getFullYear(), date.getMonth() + 1, date.getDate());
    const jDate = jd0 - lng / (15 * 24);
    // الأفق، ويزيد بانخفاض الأفق للمرتفع في الطرق التي تعتمد ارتفاع المكان
    const elev = opts.elev || 0;
    const horizon = 0.833 + (elev > 0 && m.elev ? m.elev * 0.0347 * Math.sqrt(elev) : 0);
    // موضع الشمس عند كل وقت، أو مرة لليوم عند منتصف الليل بتوقيت غرينتش
    const daily = m.daily ? sunPosition(jd0) : null;
    const sun = (t) => daily || sunPosition(jDate + t);

    const midDay = (t) => fixHour(12 - sun(t).eqt);
    const sunAngleTime = (angle, t, ccw) => {
      const decl = sun(t).decl;
      const noon = midDay(t);
      const x = (-sin(angle) - sin(decl) * sin(lat)) / (cos(decl) * cos(lat));
      if (x < -1 || x > 1) return NaN;
      const tt = acos(x) / 15;
      return noon + (ccw ? -tt : tt);
    };
    const asrTime = (factor, t) => {
      const decl = sun(t).decl;
      return sunAngleTime(-acot(factor + tan(Math.abs(lat - decl))), t);
    };

    // في العروض العليا صيفاً: الزاويتان نسبة من أعمق انخفاض للشمس تلك الليلة إن قلّتا عنه
    let fajrAngle = m.fajr, ishaAngle = typeof m.isha === "number" ? m.isha : null;
    if (m.dep) {
      const decl = sunPosition(jDate + 0.5).decl;
      const deepest = 90 - Math.abs(lat) - (lat >= 0 ? decl : -decl);
      fajrAngle = Math.min(fajrAngle, m.dep[0] * deepest);
      if (ishaAngle != null) ishaAngle = Math.min(ishaAngle, m.dep[1] * deepest);
    }

    // تقديرات أولية ثم تكرار لتحسين الدقة
    let t = { fajr: 5, sunrise: 6, dhuhr: 12, asr: 13, sunset: 18, maghrib: 18, isha: 18 };
    for (let i = 0; i < 2; i++) {
      const p = Object.fromEntries(Object.entries(t).map(([k, v]) => [k, (isNaN(v) ? 12 : v) / 24]));
      const sunset = sunAngleTime(horizon, p.sunset);
      t = {
        fajr: sunAngleTime(fajrAngle, p.fajr, true),
        sunrise: sunAngleTime(horizon, p.sunrise, true),
        dhuhr: midDay(p.dhuhr),
        asr: asrTime(opts.asr === "hanafi" ? 2 : 1, p.asr),
        sunset,
        maghrib: typeof m.maghrib === "number" ? sunAngleTime(m.maghrib, p.maghrib) : sunset,
        isha: ishaAngle != null ? sunAngleTime(ishaAngle, p.isha) : NaN,
      };
    }
    const adj = tz - lng / 15;
    for (const k in t) t[k] += adj;
    if (isNaN(t.maghrib)) t.maghrib = t.sunset;

    // خطوط العرض العليا
    const night = fixHour(t.sunrise - t.sunset + 24);
    if (m.seasonal) {
      // لجنة رؤية الهلال العالمية: فوق 55° سُبع الليل، ثم لا يسبق الفجرُ الحدَّ الموسمي ولا يتأخر العشاء عنه
      if (Math.abs(lat) >= 55) {
        t.fajr = t.sunrise - night / 7;
        if (ishaAngle != null) t.isha = t.sunset + night / 7;
      }
      const safeFajr = t.sunrise - seasonalTwilight(lat, date, true) / 60;
      if (isNaN(t.fajr) || safeFajr > t.fajr) t.fajr = safeFajr;
      if (ishaAngle != null) {
        const safeIsha = t.sunset + seasonalTwilight(lat, date, false) / 60;
        if (isNaN(t.isha) || safeIsha < t.isha) t.isha = safeIsha;
      }
    } else if (!m.dep) {
      // طريقة الزاوية (نسبة من الليل)
      const fajrMax = (fajrAngle / 60) * night;
      if (isNaN(t.fajr) || fixHour(t.sunrise - t.fajr) > fajrMax) t.fajr = t.sunrise - fajrMax;
      if (ishaAngle != null) {
        const ishaMax = (ishaAngle / 60) * night;
        if (isNaN(t.isha) || fixHour(t.isha - t.sunset) > ishaMax) t.isha = t.sunset + ishaMax;
      }
    }

    // دقائق الجهة الرسمية (وزيادتها في رمضان)، ثم العشاء بالمدة الثابتة بعد المغرب (أم القرى وقطر)
    const ramadan = opts.hijriMonth === 9 && (!!m.ramadan || opts.method === "makkah");
    const mins = (k) => ((m.min || { dhuhr: 1 })[k] || 0) + (ramadan && m.ramadan ? m.ramadan[k] || 0 : 0);
    for (const k of ["fajr", "sunrise", "dhuhr", "asr", "maghrib"]) t[k] += mins(k) / 60;
    if (typeof m.isha === "object") t.isha = t.maghrib + (ramadan && opts.method === "makkah" ? 120 : m.isha.min) / 60;
    t.isha += mins("isha") / 60;
    delete t.sunset;
    return t;
  }

  /** حد الشفق الموسمي للجنة رؤية الهلال العالمية بالدقائق (قبل الشروق للفجر، وبعد الغروب للعشاء) */
  function seasonalTwilight(lat, date, morning) {
    const l = Math.abs(lat);
    const [a, b, c, d] = morning
      ? [75 + (28.65 / 55) * l, 75 + (19.44 / 55) * l, 75 + (32.74 / 55) * l, 75 + (48.1 / 55) * l]
      : [75 + (25.6 / 55) * l, 75 + (2.05 / 55) * l, 75 - (9.21 / 55) * l, 75 + (6.14 / 55) * l];
    const y = date.getFullYear();
    const leap = (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0;
    const days = leap ? 366 : 365;
    const doy = Math.round((Date.UTC(y, date.getMonth(), date.getDate()) - Date.UTC(y, 0, 1)) / 86400000) + 1;
    let dyy = lat >= 0 ? doy + 10 : doy - (leap ? 173 : 172);
    if (lat >= 0 && dyy >= days) dyy -= days;
    if (lat < 0 && dyy < 0) dyy += days;
    const adj = dyy < 91 ? a + ((b - a) / 91) * dyy
      : dyy < 137 ? b + ((c - b) / 46) * (dyy - 91)
      : dyy < 183 ? c + ((d - c) / 46) * (dyy - 137)
      : dyy < 229 ? d + ((c - d) / 46) * (dyy - 183)
      : dyy < 275 ? c + ((b - c) / 46) * (dyy - 229)
      : b + ((a - b) / 91) * (dyy - 275);
    return Math.round(adj * 60) / 60;
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
  /** أقرب مدينة معروفة ومربع بعدها بالدرجات */
  function nearest(lat, lng) {
    let best = CITIES[0], bd = Infinity;
    CITIES.forEach((c) => { const d = (c.lat - lat) ** 2 + (c.lng - lng) ** 2; if (d < bd) { bd = d; best = c; } });
    return [best, bd];
  }

  function getLocation() {
    const loc = window.Mizan.store.get("location", { ...CITIES[0], cityIndex: 0, rv: 2 });
    // الاسم المحفوظ قد يكون بلغة أخرى، فيُؤخذ اسم المدينة ودولتها من القائمة بلغة الصفحة
    const c = CITIES[loc.cityIndex];
    if (c) { loc.name = c.name; loc.country = c.country; }
    // قواعد المواقيت 2: الموقع المحفوظ من قبل ينتقل إلى طريقة الجهة الرسمية لمدينته وارتفاعها
    if ((loc.rv || 1) < 2) {
      const [near, d] = c ? [c, 0] : nearest(loc.lat, loc.lng);
      loc.method = near.method;
      loc.elev = d < 0.09 ? near.elev : 0;
      loc.rv = 2;
      setLocation(loc);
    }
    return loc;
  }
  function setLocation(loc) { window.Mizan.store.set("location", loc); }

  function geolocate() {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) return reject(new Error(t("المتصفح لا يدعم تحديد الموقع")));
      navigator.geolocation.getCurrentPosition(
        (p) => {
          const lat = +p.coords.latitude.toFixed(4), lng = +p.coords.longitude.toFixed(4);
          // أقرب مدينة معروفة لاقتراح طريقة الحساب، وارتفاعها إن كان الموقع فيها أو قريباً منها
          const [best, bd] = nearest(lat, lng);
          const tz = Intl.DateTimeFormat().resolvedOptions().timeZone;
          resolve({ name: bd < 1.5 ? best.name : t("موقعي الحالي"), country: bd < 1.5 ? best.country : "", lat, lng, tz, method: best.method, elev: bd < 0.09 ? best.elev : 0, cityIndex: -1, gps: true, rv: 2 });
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
