// يقارن نتائج MizanBase و MizanPrayer بنتائج Kotlin في Fixtures/prayer (يصدّرها PrayerFixturesExport.kt)،
// ويعيد اختبارات Kotlin: OfficialTimesTest و OfficialTablesTest و PrayerEndsTest و CoreTest (التنسيق).
// الاستعمال: swift run CheckPrayer [مجلد Fixtures/prayer] — يخرج بغير الصفر عند أي اختلاف.
import Foundation
import MizanBase
import MizanPrayer

let here = URL(fileURLWithPath: #filePath).deletingLastPathComponent()
let packageRoot = here.deletingLastPathComponent().deletingLastPathComponent()
let repoRoot = packageRoot.deletingLastPathComponent().deletingLastPathComponent()
let fixtures = CommandLine.arguments.count > 1 ? URL(fileURLWithPath: CommandLine.arguments[1]) : packageRoot.appendingPathComponent("Fixtures/prayer")
let officialDir = repoRoot.appendingPathComponent("android-app/shared/src/androidUnitTest/official")

var failures = 0
var checked = 0
var tzdataDiffs = 0
var sectionFails = 0
func fail(_ s: String) {
    failures += 1; sectionFails += 1
    if sectionFails <= 15 { print("  ✗ " + s) }
}

func load(_ name: String) -> [[Any]] {
    let url = fixtures.appendingPathComponent(name)
    guard let data = try? Data(contentsOf: url), let arr = try? JSONSerialization.jsonObject(with: data) as? [Any] else {
        print("✗ لا يُقرأ \(url.path)"); exit(2)
    }
    return arr.map { $0 as! [Any] }
}

func dbl(_ v: Any?) -> Double {
    if v == nil || v is NSNull { return .nan }
    if let n = v as? NSNumber { return n.doubleValue }
    if let s = v as? String { return Double(s) ?? .nan }
    return .nan
}
func int(_ v: Any?) -> Int { (v as? NSNumber)?.intValue ?? Int((v as? String) ?? "") ?? 0 }
func str(_ v: Any?) -> String? { v as? String }
func bool(_ v: Any?) -> Bool { int(v) != 0 }
func same(_ a: Double, _ b: Double, _ tol: Double = 0) -> Bool {
    if a.isNaN || b.isNaN { return a.isNaN && b.isNaN }
    if a.isInfinite || b.isInfinite { return a == b }
    return a == b || abs(a - b) <= tol
}
func section(_ title: String, _ body: () -> Int) {
    sectionFails = 0
    let start = failures
    let n = body()
    checked += n
    print((failures == start ? "✓ " : "✗ ") + "\(title): \(n) حالة" + (failures == start ? "" : "، \(failures - start) اختلاف"))
}

// ---------- المواقيت ----------
var exactValues = 0, nearValues = 0
func checkTimes(_ file: String) -> Int {
    var n = 0
    for c in load(file) {
        let name = str(c[0])!, lat = dbl(c[1]), lng = dbl(c[2]), tz = str(c[3])!, method = str(c[4])!
        let hanafi = bool(c[5]), elev = int(c[6]), date = LocalDate.parse(str(c[7])!)!
        let expect = (c[8] as! [Any]).map(dbl), shown = str(c[9])!, offset = dbl(c[10])
        var zone = tz
        let off = PrayTimes.tzOffset(tz, date)
        if !same(off, offset) {
            // بيانات المناطق الزمنية في Foundation على Linux (ICU 2023b) أقدم من Java (كازاخستان على +5 منذ 2024):
            // اختلاف بيانات لا حساب، فيُعاد الحساب بمنطقة ثابتة بإزاحة Kotlin
            tzdataDiffs += 1
            if tzdataDiffs <= 3 { print("  ~ tzdata \(name) \(tz) \(date): kotlin \(offset) swift \(off) (\(TimeZone.timeZoneDataVersion))") }
            let h = Int(offset)
            zone = h == 0 ? "UTC" : "Etc/GMT\(h > 0 ? "-" : "+")\(abs(h))"
            if Double(h) != offset || !same(PrayTimes.tzOffset(zone, date), offset) { fail("tz \(tz) \(date) \(offset)"); continue }
        }
        let t = PrayTimes.compute(date, lat, lng, zone, method, hanafi, elev: elev)
        let got = [t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha, t.sunset, t.maghribEnd, t.midnight]
        let gotShown = (PrayTimes.PRAYERS.map { PrayTimes.format(t[$0], true) } + PrayTimes.FIVE.map { PrayTimes.format(t.end($0), true) }).joined(separator: " ")
        n += 1
        var bad = false
        for i in 0..<9 {
            if expect[i] == got[i] || (expect[i].isNaN && got[i].isNaN) { exactValues += 1 }
            else if same(expect[i], got[i], 1e-9) { nearValues += 1 }
            else { bad = true }
        }
        if bad || gotShown != shown {
            fail("\(name) \(method) hanafi=\(hanafi) \(date): kotlin [\(shown)] \(expect) swift [\(gotShown)] \(got)")
        }
    }
    return n
}
section("المواقيت بطريقة كل مدينة (كل مدينة، أيام السنة، العصران)") { checkTimes("times_default.json") }
section("كل طريقة في كل مدينة (الانقلابان، العصران)") { checkTimes("times_methods.json") }
section("العروض العليا ونقاط عشوائية (كل طريقة)") { checkTimes("times_extra.json") }
print("  القيم: \(exactValues) مطابقة بالبت، \(nearValues) ضمن 1e-9 ساعة؛ حُسب \(tzdataDiffs) بمنطقة ثابتة لاختلاف بيانات المناطق الزمنية")

section("الطرق والمدن") {
    var n = 0
    let methods = load("methods.json")
    if methods.count != PrayTimes.METHODS.count { fail("عدد الطرق \(methods.count) ≠ \(PrayTimes.METHODS.count)") }
    for (i, m) in methods.enumerated() where i < PrayTimes.METHODS.count {
        let s = PrayTimes.METHODS[i]
        n += 1
        func dict(_ v: Any) -> [String: Double] { (v as! [String: Any]).mapValues { dbl($0) } }
        let dep = (m[12] as? [Any]).map { $0.map(dbl) }
        let ok = s.key == str(m[0]) && s.fajr == dbl(m[1]) && same(s.ishaAngle ?? .nan, dbl(m[2])) && s.ishaMinutes == (m[3] is NSNull ? nil : int(m[3]))
            && same(s.maghribAngle ?? .nan, dbl(m[4])) && s.minutes == dict(m[5]) && s.hanafi == bool(m[6]) && s.elevation == dbl(m[7])
            && s.dailySun == bool(m[8]) && same(s.fajrFallback ?? .nan, dbl(m[9])) && same(s.ishaFallback ?? .nan, dbl(m[10]))
            && s.seasonal == bool(m[11]) && (dep == nil ? s.depression == nil : (s.depression?.0 == dep![0] && s.depression?.1 == dep![1]))
            && s.ramadan == dict(m[13])
        if !ok { fail("الطريقة \(str(m[0])!)") }
    }
    let cities = load("cities.json")
    if cities.count != PrayTimes.CITIES.count { fail("عدد المدن \(cities.count) ≠ \(PrayTimes.CITIES.count)") }
    for (i, c) in cities.enumerated() where i < PrayTimes.CITIES.count {
        let s = PrayTimes.CITIES[i]
        n += 1
        if !(s.key == str(c[0]) && s.cc == str(c[1]) && s.lat == dbl(c[2]) && s.lng == dbl(c[3]) && s.tz == str(c[4]) && s.method == str(c[5]) && s.elev == int(c[6])) {
            fail("المدينة \(str(c[0])!)")
        }
    }
    return n
}

section("تنسيق الأوقات (PrayTimes.format)") {
    var n = 0
    for c in load("format.json") {
        let v = dbl(c[0])
        let got = [PrayTimes.format(v, true), PrayTimes.format(v, false), PrayTimes.format(v, false, am: "ص", pm: "م")]
        n += 1
        if got != [str(c[1])!, str(c[2])!, str(c[3])!] { fail("format(\(v)): kotlin \(c[1...3]) swift \(got)") }
    }
    return n
}

section("القبلة والمسافة وأقرب مدينة والموقع من الإحداثيات") {
    var n = 0
    for c in load("geo.json") {
        let lat = dbl(c[0]), lng = dbl(c[1])
        let q = PrayTimes.qibla(lat, lng), km = PrayTimes.distanceKm(lat, lng)
        let (city, d2) = PrayTimes.nearestCity(lat, lng)
        let p = LocationHelper.placeFrom(lat, lng)
        n += 1
        let ok = same(q, dbl(c[2]), 1e-9) && same(km, dbl(c[3]), 1e-7) && city.key == str(c[4]) && same(d2, dbl(c[5]), 1e-12)
            && PrayTimes.elevationNear(lat, lng) == int(c[6]) && PrayTimes.directionIndex(q) == int(c[7])
            && p.key == str(c[8]) && p.lat == dbl(c[9]) && p.lng == dbl(c[10]) && p.method == str(c[11]) && p.elev == int(c[12])
            && PrayTimes.defaultMethod(cityKey: p.key, lat: lat, lng: lng) == str(c[13])
        if !ok { fail("geo \(lat),\(lng): kotlin \(c) swift \(q) \(km) \(city.key) \(d2) \(PrayTimes.directionIndex(q)) \(p)") }
    }
    return n
}

section("حد الشفق الموسمي") {
    var n = 0
    for c in load("twilight.json") {
        let lat = dbl(c[0]), d = LocalDate.parse(str(c[1])!)!
        n += 1
        let a = PrayTimes.seasonalTwilight(lat, d, morning: true), b = PrayTimes.seasonalTwilight(lat, d, morning: false)
        if a != dbl(c[2]) || b != dbl(c[3]) { fail("twilight \(lat) \(d): kotlin \(c[2]) \(c[3]) swift \(a) \(b)") }
    }
    return n
}

// ---------- الصلاة القادمة والحاضرة ----------
func cfg(_ key: String) -> (PrayerConfig, (LocalDate) -> PrayerTimes) {
    let conf = PrayerConfig(city: PrayTimes.city(key)!)
    return (conf, { PrayerUtil.times($0, conf) })
}
func parseLDT(_ s: String) -> LocalDateTime {
    // 2026-03-14T05:07[:09[.123]]
    let parts = s.split(separator: "T")
    let d = LocalDate.parse(String(parts[0]))!
    let tp = parts[1].split(separator: ":")
    let h = Int(tp[0])!, m = Int(tp[1])!
    var sec = 0, nano = 0
    if tp.count > 2 {
        let sp = tp[2].split(separator: ".")
        sec = Int(sp[0])!
        if sp.count > 1 { let f = String(sp[1]); nano = Int(f + String(repeating: "0", count: 9 - f.count))! }
    }
    return LocalDateTime(d, h, m, sec, nano)
}

section("الصلاة القادمة والحاضرة (PrayerUtil)") {
    var n = 0
    for c in load("next.json") {
        let (conf, times) = cfg(str(c[0])!)
        let nowS = str(c[1])!
        let now = parseLDT(nowS)
        n += 1
        if now.description != nowS { fail("LocalDateTime.description \(nowS) → \(now)") }
        let nx = PrayerUtil.next(conf, now: now)
        let e = c[2] as! [Any]
        if nx.key != str(e[0]) || nx.at.description != str(e[1]) || nx.remainingSeconds != Int64(int(e[2])) || PrayerUtil.hms(nx.remaining) != str(e[3]) {
            fail("next \(c[0]) \(nowS): kotlin \(e) swift \(nx.key) \(nx.at) \(nx.remainingSeconds) \(PrayerUtil.hms(nx.remaining))")
        }
        let t = times(now.date)
        let sec = PrayerUtil.secondOfDay(now)
        if sec != dbl(c[4]) { fail("secondOfDay \(nowS)") }
        if PrayerUtil.current(t, secondOfDay: sec) != str(c[3]) { fail("current \(c[0]) \(nowS): kotlin \(c[3]) swift \(String(describing: PrayerUtil.current(t, secondOfDay: sec)))") }
    }
    for c in load("hms.json") {
        n += 1
        if PrayerUtil.hms(Int64(int(c[0]))) != str(c[1]) { fail("hms \(c[0])") }
    }
    return n
}

// ---------- التاريخ الهجري والتواريخ ----------
section("التاريخ الهجري (toHijri)") {
    var n = 0
    for c in load("hijri.json") {
        let d = LocalDate(epochDay: int(c[0]))
        let h = Hijri.toHijri(d)
        n += 1
        if let e = c[1] as? [Any] {
            if h != HDate(int(e[0]), int(e[1]), int(e[2])) { fail("toHijri \(d): kotlin \(e) swift \(String(describing: h))") }
        } else if h != nil { fail("toHijri \(d): kotlin null swift \(h!)") }
    }
    return n
}

section("الهجري إلى الميلادي وأطوال الأشهر") {
    var n = 0
    for c in load("hijri_greg.json") {
        let y = int(c[0]), m = int(c[1])
        n += 1
        if str(c[2]) == "len" {
            if Hijri.monthLength(y, m) != int(c[3]) { fail("monthLength \(y)/\(m)") }
        } else {
            let g = Hijri.toGregorian(y, m, int(c[2]))
            if g?.description != str(c[3]) { fail("toGregorian \(y)/\(m)/\(c[2]): kotlin \(c[3]) swift \(String(describing: g))") }
        }
    }
    return n
}

section("التواريخ (epochDay، الأسبوع، الأشهر، periodBetween)") {
    var n = 0
    for c in load("dates.json") {
        let a = LocalDate.parse(str(c[0])!)!, nn = int(c[5]), b = LocalDate.parse(str(c[9])!)!
        n += 1
        let p = periodBetween(min(a, b), max(a, b)), p2 = periodBetween(max(a, b), min(a, b))
        let got: [String] = [a.description, "\(a.epochDay)", "\(a.dayOfWeek)", "\(a.dayOfYear)", "\(a.lengthOfMonth())",
                             a.plusMonths(nn).description, a.plusYears(nn).description, a.withYear(2027).description, "\(daysBetween(a, b))",
                             "\(p.years),\(p.months),\(p.days)", "\(p2.years),\(p2.months),\(p2.days)", "\(LocalDate(epochDay: a.epochDay) == a)"]
        let pe = (c[11] as! [Any]).map(int), pe2 = (c[12] as! [Any]).map(int)
        let exp: [String] = [str(c[0])!, "\(int(c[1]))", "\(int(c[2]))", "\(int(c[3]))", "\(int(c[4]))", str(c[6])!, str(c[7])!, str(c[8])!, "\(int(c[10]))",
                             "\(pe[0]),\(pe[1]),\(pe[2])", "\(pe2[0]),\(pe2[1]),\(pe2[2])", "true"]
        if got != exp { fail("date \(a): kotlin \(exp) swift \(got)") }
    }
    return n
}

section("فرق المنطقة الزمنية لوقت محلي (tzOffsetHours)") {
    var n = 0, diffs = 0
    for c in load("tz.json") {
        let z = str(c[0])!, t = parseLDT(str(c[1])!)
        n += 1
        let got = tzOffsetHours(z, t)
        if got != dbl(c[2]) {
            // Casablanca: بيانات Java أقدم من بيانات النظام (المغرب على غرينتش دائماً منذ 2026-09-20)
            if z == "Africa/Casablanca" || z == "Not/AZone" { diffs += 1; continue }
            fail("tz \(z) \(t): kotlin \(c[2]) swift \(got)")
        }
    }
    if diffs > 0 { print("  ~ \(diffs) اختلاف في Africa/Casablanca أو المنطقة المجهولة (بيانات المناطق/منطقة الجهاز، لا الحساب)") }
    return n
}

section("حسابات شاشات التاريخ (العمر الهجري، عيد الميلاد، المناسبات)") {
    var n = 0
    for c in load("datescreens.json") {
        let today = LocalDate.parse(str(c[0])!)!, birth = LocalDate.parse(str(c[1])!)!
        n += 1
        let age = DateCalc.hijriAge(birth, today)
        let ea = (c[2] as? [Any]).map { $0.map(int) }
        if (age.map { [$0.years, $0.months, $0.days] }) != ea { fail("hijriAge \(birth) \(today)") }
        if DateCalc.daysToNextBirthday(birth, today) != int(c[3]) { fail("nextBirthday \(birth) \(today)") }
        let up = DateCalc.upcomingEvents(today).map { "\($0.event.key) \($0.date) \($0.hijriYear)" }
        let eu = (c[4] as! [[Any]]).map { "\(str($0[0])!) \(str($0[1])!) \(int($0[2]))" }
        if up != eu { fail("events \(today): kotlin \(eu) swift \(up)") }
    }
    return n
}

// ---------- التنسيق ----------
section("Fmt.num و formatNumber لكل لغة") {
    var n = 0
    for c in load("fmt_num.json") {
        let v = dbl(c[0])
        let lang = NumberLang.of(str(c[1])!)!
        let exp = (c[2] as! [Any]).map { str($0)! }
        let got = (-1...3).map { Fmt.num(v, digits: $0, lang: lang) }
        let got3 = formatNumber(v, digits: 3, decimal: lang.decimal, group: lang.group)
        n += 1
        if got != exp || got3 != str(c[3])! { fail("num \(c[0]) \(lang.code): kotlin \(exp) \(c[3]) swift \(got) \(got3)") }
    }
    return n
}
section("Fmt.int و Fmt.two") {
    var n = 0
    for c in load("fmt_int.json") {
        let v = Int64(str(c[0])!)!
        n += 1
        let got = Fmt.int(v, lang: NumberLang.of(str(c[1])!)!)
        if got != str(c[2])! { fail("int \(v) \(c[1]): kotlin \(c[2]) swift \(got)") }
    }
    for c in load("fmt_two.json") {
        n += 1
        if Fmt.two(Int64(int(c[0]))) != str(c[1])! { fail("two \(c[0])") }
    }
    return n
}
section("Fmt.parse لكل لغة (أرقام عربية وفارسية وبنغالية)") {
    var n = 0
    for c in load("fmt_parse.json") {
        let s = str(c[0])!, lang = NumberLang.of(str(c[1])!)!
        n += 1
        let got = Fmt.parse(s, lang: lang)
        if got != dbl(c[2]) { fail("parse \"\(s)\" \(lang.code): kotlin \(c[2]) swift \(got)") }
    }
    return n
}

// ---------- اختبارات Kotlin ----------
// PrayerEndsTest
section("PrayerEndsTest") {
    let day = LocalDate(2026, 10, 27)
    let t = PrayTimes.compute(day, 21.4225, 39.8262, "Asia/Riyadh", "makkah", false)
    func check(_ ok: Bool, _ what: String) { if !ok { fail("PrayerEnds: " + what) } }
    check(abs(t.sunrise - t.end("fajr")) < 1e-9, "fajr ends at sunrise")
    check(abs(t.asr - t.end("dhuhr")) < 1e-9, "dhuhr ends at asr")
    check(t.end("asr") >= t.asr && t.end("asr") <= t.isha, "asr end")
    check(t.end("maghrib") > t.maghrib && t.end("maghrib") < t.isha, "maghrib end before isha")
    let m = (t.end("maghrib") - t.maghrib) * 60
    check(m >= 60 && m <= 85, "maghrib lasts \(m) min")
    check(t.end("isha") > t.isha && t.end("isha") < t.fajr + 24, "isha end")
    check(t.end("sunrise").isNaN, "sunrise has no end")
    let l = PrayTimes.compute(day, 51.5074, -0.1278, "Europe/London", "mwl", false)
    check(abs(l.isha - l.end("maghrib")) < 1e-9, "angle method maghrib ends at isha")
    return 8
}

// CoreTest: التنسيق، وطهران، والمدن
section("CoreTest (التنسيق والمدن)") {
    var n = 0
    func eq(_ a: Double, _ b: Double, _ what: String) { n += 1; if abs(a - b) > 1e-9 { fail("CoreTest \(what): \(a) ≠ \(b)") } }
    func eqs(_ a: String, _ b: String) { n += 1; if a != b { fail("CoreTest \(a) ≠ \(b)") } }
    eq(Fmt.parse("1,234.5", lang: .en), 1234.5, "1,234.5 en")
    eq(Fmt.parse("1.234,5", lang: .de), 1234.5, "1.234,5 de")
    eq(Fmt.parse("1.500", lang: .de), 1500, "1.500 de")
    eq(Fmt.parse("1.500", lang: .en), 1.5, "1.500 en")
    eq(Fmt.parse("2,5", lang: .fr), 2.5, "2,5 fr")
    eq(Fmt.parse("1 500", lang: .fr), 1500, "1 500 fr")
    eq(Fmt.parse("2,5", lang: .en), 2.5, "2,5 en")
    eq(Fmt.parse("١٢٣٤", lang: .ar), 1234, "arabic digits")
    eq(Fmt.parse("١٢٫٥", lang: .ar), 12.5, "arabic decimal")
    eq(Fmt.parse("۱۲۳۴", lang: .fa), 1234, "persian")
    eq(Fmt.parse("১২৩৪", lang: .bn), 1234, "bengali")
    eq(Fmt.parse("abc", lang: .en), 0, "abc")
    eq(Fmt.parse("-5", lang: .en), 0, "-5")
    eqs(Fmt.num(1234.5, digits: 2, lang: .en), "1,234.5")
    eqs(Fmt.num(1234.5, digits: 2, lang: .de), "1.234,5")
    eqs(Fmt.num(1234.5, digits: 2, lang: .tr), "1.234,5")
    eqs(Fmt.num(1234.5, digits: 2, lang: .ar), "1,234.5")
    for l in NumberLang.builtIn { eq(Fmt.parse(Fmt.num(1234.5, digits: 2, lang: l), lang: l), 1234.5, "roundtrip \(l.code)") }
    n += 1
    if Set(PrayTimes.CITIES.map { $0.key }).count != PrayTimes.CITIES.count { fail("مفاتيح المدن مكررة") }
    for c in PrayTimes.CITIES {
        n += 1
        if !PrayTimes.METHODS.contains(where: { $0.key == c.method }) { fail("طريقة المدينة \(c.key) غير معروفة") }
    }
    return n
}

// OfficialTimesTest و OfficialTablesTest: الجداول الرسمية بحدودها كما في Kotlin
func minutes(_ s: String) -> Int? {
    let t = s.trimmingCharacters(in: .whitespaces)
    let p = t.split(separator: ":", omittingEmptySubsequences: false)
    guard p.count == 2, (1...2).contains(p[0].count), p[1].count == 2, let h = Int(p[0]), let m = Int(p[1]),
          p[0].allSatisfy({ $0.isASCII && $0.isNumber }), p[1].allSatisfy({ $0.isASCII && $0.isNumber }) else { return nil }
    return h * 60 + m
}

section("OfficialTimesTest (عمّان، تشرين الثاني 2026)") {
    let rows = """
    2026-11-01,05:30,06:46,12:20,15:24,17:53,19:10
    2026-11-02,05:30,06:47,12:20,15:24,17:52,19:09
    2026-11-03,05:31,06:48,12:20,15:23,17:51,19:08
    2026-11-04,05:32,06:49,12:20,15:22,17:51,19:07
    2026-11-05,05:33,06:50,12:20,15:22,17:50,19:07
    2026-11-06,05:33,06:51,12:20,15:21,17:49,19:06
    2026-11-07,05:34,06:51,12:20,15:21,17:48,19:06
    2026-11-08,05:35,06:52,12:20,15:20,17:48,19:05
    2026-11-09,05:36,06:53,12:20,15:19,17:47,19:04
    2026-11-10,05:36,06:54,12:21,15:19,17:46,19:04
    """
    let c = PrayTimes.city("amman")!
    let hanafi = PrayTimes.method(c.method).hanafi
    var n = 0
    for line in rows.split(separator: "\n") {
        let cells = line.trimmingCharacters(in: .whitespaces).split(separator: ",").map(String.init)
        let day = LocalDate.parse(cells[0])!
        let t = PrayTimes.compute(day, c.lat, c.lng, c.tz, c.method, hanafi)
        for (i, k) in PrayTimes.PRAYERS.enumerated() {
            n += 1
            let shown = PrayTimes.format(t[k], true)
            if minutes(shown) != minutes(cells[i + 1]) { fail("amman \(day) \(k): app \(shown), official \(cells[i + 1])") }
        }
    }
    return n
}

section("OfficialTablesTest (كل الجداول الرسمية بحدودها)") {
    struct Limits { var tol: [String: Int] = [:]; var skip: Set<String> = []; var odd: Set<String> = [] }
    func matches(_ s: String, _ pattern: String) -> Bool { s.range(of: "^(" + pattern + ")$", options: .regularExpression) != nil }
    let keys = PrayTimes.PRAYERS
    func limits(_ name: String) -> Limits? {
        if matches(name, "(GB_birmingham|FR_lyon|US_.*|CA_.*|AU_.*)\\.csv") { return nil }
        if name.hasPrefix("GB_london") { return Limits(skip: ["fajr", "isha"]) }
        if name.hasPrefix("PK_") { return Limits(skip: ["dhuhr"]) }
        if name.hasPrefix("PS_") { return Limits(tol: ["fajr": 3, "sunrise": 2, "maghrib": 2, "isha": 2]) }
        if matches(name, "(AT_|BE_|DE_|NL_|SE_).*") { return Limits(tol: ["fajr": 13, "isha": 10]) }
        if name.hasPrefix("BA_") { return Limits(tol: Dictionary(uniqueKeysWithValues: keys.map { ($0, 2) })) }
        if name.hasPrefix("MY_") { return Limits(tol: ["asr": 2]) }
        if name.hasPrefix("SY_") || name.hasPrefix("LB_") || name.hasPrefix("QA_") || name.hasPrefix("BD_") {
            return Limits(tol: Dictionary(uniqueKeysWithValues: keys.map { ($0, 2) }))
        }
        if name.hasPrefix("RU_kazan") { return Limits(odd: ["2026-05-05 fajr", "2026-08-08 isha"]) }
        return Limits()
    }
    let files = ((try? FileManager.default.contentsOfDirectory(atPath: officialDir.path)) ?? []).filter { $0.hasSuffix(".csv") }.sorted()
    if files.isEmpty { fail("لا جداول رسمية في \(officialDir.path)"); return 0 }
    var n = 0
    for f in files {
        let cityKey = String(f.dropLast(4).split(separator: "_")[1])
        guard let c = PrayTimes.city(cityKey) else { fail("مدينة مجهولة في \(f)"); continue }
        let hanafi = PrayTimes.method(c.method).hanafi
        let lim = limits(f)
        // المغرب على غرينتش (كما في Kotlin)؛ وكازاخستان على +5 منذ 2024 إن كانت بيانات المناطق هنا أقدم (ICU في Linux)
        var tz = f.hasPrefix("MA_") ? "UTC" : c.tz
        if f.hasPrefix("KZ_") && tzOffsetHours(c.tz, LocalDateTime(2026, 6, 1, 12, 0)) != 5 { tz = "Etc/GMT-5" }
        let text = (try? String(contentsOf: officialDir.appendingPathComponent(f), encoding: .utf8)) ?? ""
        for raw in text.split(omittingEmptySubsequences: false, whereSeparator: { $0 == "\n" || $0 == "\r\n" }) {
            let line = raw.trimmingCharacters(in: .whitespaces)
            if line.isEmpty || line.hasPrefix("#") { continue }
            let cells = line.split(separator: ",", omittingEmptySubsequences: false).map(String.init)
            guard let day = LocalDate.parse(cells[0].trimmingCharacters(in: .whitespaces)) else { continue }
            let t = PrayTimes.compute(day, c.lat, c.lng, tz, c.method, hanafi, elev: c.elev)
            for (i, k) in keys.enumerated() {
                guard i + 1 < cells.count, let official = minutes(cells[i + 1]) else { continue }
                guard let shown = minutes(PrayTimes.format(t[k], true)) else { continue }
                var d = shown - official
                if d > 720 { d -= 1440 } else if d < -720 { d += 1440 }
                n += 1
                if let lim = lim, !lim.skip.contains(k), !lim.odd.contains("\(day) \(k)"), abs(d) > (lim.tol[k] ?? 1) {
                    fail("\(f) \(day) \(k): app \(PrayTimes.format(t[k], true)), official \(cells[i + 1])")
                }
            }
        }
    }
    return n
}

print("")
print(failures == 0 ? "✓ كل الحالات مطابقة: \(checked) حالة" : "✗ \(failures) اختلاف من \(checked) حالة")
exit(failures == 0 ? 0 : 1)
