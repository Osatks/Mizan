import Foundation
import MizanBase
import MizanContent

// يقارن نتائج MizanContent بنتائج Kotlin في Fixtures/content (يصدّرها ContentFixturesExport.kt)، ويخرج بخطأ عند أي اختلاف.
// الاستعمال: swift run CheckContent [مجلد الحزمة]

let pkg = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : FileManager.default.currentDirectoryPath
let fixtures = pkg + "/Fixtures/content"
Assets.root = pkg + "/../../android-app/shared/src/commonMain/assets"

var checked = 0
var failures = 0
var section = ""

func load(_ name: String) -> Any {
    guard let d = FileManager.default.contents(atPath: fixtures + "/" + name),
          let o = try? JSONSerialization.jsonObject(with: d, options: [.fragmentsAllowed]) else {
        print("لا يمكن قراءة \(name)"); exit(2)
    }
    return o
}

// ---------- مقارنة قيم JSON ----------

indirect enum J: Equatable, CustomStringConvertible {
    case null, bool(Bool), num(Double), str(String), arr([J]), obj([String: J])
    var description: String {
        switch self {
        case .null: return "null"
        case .bool(let b): return "\(b)"
        case .num(let d): return d == d.rounded() && abs(d) < 1e15 ? String(Int64(d)) : "\(d)"
        case .str(let s): return "\"\(s)\""
        case .arr(let a): return "[" + a.map { $0.description }.joined(separator: ",") + "]"
        case .obj(let o): return "{" + o.keys.sorted().map { "\"\($0)\":\(o[$0]!)" }.joined(separator: ",") + "}"
        }
    }
}

func isBoolValue(_ v: Any) -> Bool {
    if type(of: v) == Bool.self { return true }
    #if canImport(Darwin)
    if let n = v as? NSNumber { return CFGetTypeID(n) == CFBooleanGetTypeID() }
    return false
    #else
    if let n = v as? NSNumber, type(of: v) != Int.self, type(of: v) != Double.self { return String(cString: n.objCType) == "c" }
    return false
    #endif
}

func norm(_ v: Any?) -> J {
    guard let v = v else { return .null }
    if v is NSNull { return .null }
    let mirror = Mirror(reflecting: v)
    if mirror.displayStyle == .optional {
        guard let first = mirror.children.first else { return .null }
        return norm(first.value)
    }
    if let s = v as? String {
        // الأعداد العشرية في الملفات نصوص "#<Double.toString>" لتُقرأ بدقة (JSONSerialization في Linux يقرّبها)
        if s.hasPrefix("#"), let d = Double(s.dropFirst()) { return .num(d) }
        return .str(s)
    }
    if isBoolValue(v) { return .bool((v as? Bool) ?? ((v as? NSNumber)?.boolValue ?? false)) }
    if let i = v as? Int { return .num(Double(i)) }
    if let i = v as? Int64 { return .num(Double(i)) }
    if let d = v as? Double { return .num(d) }
    if let n = v as? NSNumber { return .num(n.doubleValue) }
    if let a = v as? [Any] { return .arr(a.map(norm)) }
    if let a = v as? [Any?] { return .arr(a.map(norm)) }
    if let o = v as? [String: Any] { return .obj(o.mapValues(norm)) }
    if let o = v as? [String: Any?] { return .obj(o.mapValues(norm)) }
    return .str("\(v)")
}

/// أول موضع يختلف فيه الجانبان (للتشخيص)
func firstDiff(_ a: J, _ b: J, _ path: String) -> String {
    switch (a, b) {
    case (.arr(let x), .arr(let y)):
        if x.count != y.count { return "\(path): طول \(x.count) ≠ \(y.count)" }
        for i in x.indices where x[i] != y[i] { return firstDiff(x[i], y[i], "\(path)[\(i)]") }
    case (.obj(let x), .obj(let y)):
        for k in Set(x.keys).union(y.keys).sorted() where x[k] != y[k] { return firstDiff(x[k] ?? .null, y[k] ?? .null, "\(path).\(k)") }
    case (.str(let x), .str(let y)):
        let xu = Array(x.utf16), yu = Array(y.utf16)
        var i = 0
        while i < min(xu.count, yu.count) && xu[i] == yu[i] { i += 1 }
        return "\(path): نص يختلف عند \(i) (طول \(xu.count) و \(yu.count))"
    default: break
    }
    return "\(path): \(String(a.description.prefix(200))) ≠ \(String(b.description.prefix(200)))"
}

func eq(_ swift: Any?, _ kotlin: Any?, _ ctx: @autoclosure () -> String) {
    checked += 1
    let a = norm(swift), b = norm(kotlin)
    if a != b {
        failures += 1
        if failures <= 80 { print("✗ [\(section)] \(ctx()) — \(firstDiff(a, b, ""))") }
    }
}

/// FNV-1a 64 على وحدات UTF-16 (كما في ContentFixturesExport.h)
func h(_ s: String?) -> Any? {
    guard let s = s else { return nil }
    var x: UInt64 = 0xcbf29ce484222325
    for u in s.utf16 { x = (x ^ UInt64(u)) &* 0x100000001b3 }
    let hex = String(x, radix: 16)
    return String(repeating: "0", count: 16 - hex.count) + hex
}

func dict(_ v: Any?) -> [String: Any] { v as? [String: Any] ?? [:] }
func arr(_ v: Any?) -> [Any] { v as? [Any] ?? [] }
func str(_ v: Any?) -> String? { v as? String }
func int(_ v: Any?) -> Int { (v as? NSNumber)?.intValue ?? (v as? Int) ?? 0 }
func dbl(_ v: Any?) -> Double {
    if v == nil || v is NSNull { return .nan }
    if let s = v as? String {
        if s.hasPrefix("#") { return Double(s.dropFirst()) ?? .nan }
        return s == "nan" ? .nan : s == "inf" ? .infinity : s == "-inf" ? -.infinity : Double(s) ?? .nan
    }
    return (v as? NSNumber)?.doubleValue ?? (v as? Double) ?? .nan
}

// ---------- الأذكار ----------

func dhikrJ(_ d: Dhikr) -> [String: Any?] {
    ["id": d.id, "ar": d.ar, "count": d.count, "ayah": d.ayah.map { [$0.surah, $0.from, $0.to] },
     "src": d.src.map { [$0.book, $0.number] }, "grade": d.grade, "by": d.by, "narr": d.narr, "fadl": d.fadl, "note": d.note]
}

func checkAdhkar() {
    section = "adhkar.sections"
    let f = dict(load("adhkar_sections.json"))
    let secs = Adhkar.sections()
    let fs = arr(f["sections"])
    eq(secs.count, fs.count, "عدد الأقسام")
    for (i, s) in secs.enumerated() where i < fs.count {
        let k = dict(fs[i])
        let sj: [String: Any?] = ["id": s.id, "icon": s.icon, "titleAr": s.titleAr, "descAr": s.descAr,
                                  "groups": s.groups.map { g -> [String: Any?] in ["key": g.key, "titleAr": g.titleAr, "items": g.items.map { $0.id }] }]
        eq(sj, k, "القسم \(s.id)")
    }
    let items = dict(f["items"])
    let all = secs.flatMap { $0.items }
    eq(all.count, items.count, "عدد الأذكار")
    for d in all { eq(dhikrJ(d), items[d.id], "الذكر \(d.id)") }
    eq(Adhkar.loaded()?.count, secs.count, "loaded")

    section = "adhkar.tr"
    let trs = dict(load("adhkar_tr.json"))
    for code in trs.keys.sorted() {
        let k = dict(trs[code])
        let tr = Adhkar.tr(code)
        eq(tr.sections.mapValues { [$0.title, $0.desc] }, k["sections"], "\(code) sections")
        eq(tr.groups, k["groups"], "\(code) groups")
        eq(tr.books, k["books"], "\(code) books")
        eq(tr.grades, k["grades"], "\(code) grades")
        eq(tr.by, k["by"], "\(code) by")
        eq(tr.items.mapValues { [h($0.t), h($0.narr), h($0.fadl), h($0.note)] }, k["items"], "\(code) items")
        eq(Dictionary(uniqueKeysWithValues: secs.map { ($0.id, [tr.title($0, lang: code), tr.desc($0, lang: code)]) }), k["sectionTitles"], "\(code) sectionTitles")
        eq(Dictionary(uniqueKeysWithValues: secs.flatMap { $0.groups }.map { ($0.key, tr.groupTitle($0, lang: code) as Any?) }), k["groupTitles"], "\(code) groupTitles")
        eq(Dictionary(uniqueKeysWithValues: all.map { ($0.id, h(Adhkar.translation($0, tr: tr, lang: code, quran: nil))) }), k["translation"], "\(code) translation")
        eq(Dictionary(uniqueKeysWithValues: all.map { ($0.id, Adhkar.gradeLine($0, tr: tr)) }), k["gradeLine"], "\(code) gradeLine")
        // sourceLine: مراجع القرآن من نصوص الواجهة (تُمرَّر)، والبقية تُقارن
        let ks = dict(k["sourceLine"])
        for d in all {
            let kl = arr(ks[d.id])
            var i = -1
            let sl = Adhkar.sourceLine(d, tr: tr) { _ in i += 1; return "" }
            let mixed: [Any?] = sl.enumerated().map { (j, s) in d.src[j].book == "quran" ? (j < kl.count ? kl[j] : nil) : s }
            eq(mixed, kl, "\(code) sourceLine \(d.id)")
        }
        let wt = dict(k["windowTitles"])
        for kind in wt.keys.sorted() { eq(AdhkarClock.title(kind: kind, lang: code, tr: tr), wt[kind], "\(code) title \(kind)") }
    }

    section = "adhkar.misc"
    let m = dict(load("adhkar_misc.json"))
    let hm = m["todayHijriMonth"] as? Int
    for (hour, s) in arr(m["suggested"]).enumerated() { eq(Adhkar.suggested(hour: hour, hijriMonth: hm), s, "suggested \(hour)") }
    // رمضان (لا يحدث اليوم في Kotlin، فيُختبر هنا بالقاعدة نفسها)
    eq(Adhkar.suggested(hour: 13, hijriMonth: 9), "ramadan", "suggested رمضان")
    eq(Adhkar.suggested(hour: 2, hijriMonth: 9), "ramadan", "suggested رمضان")
    for (id, key) in dict(m["doneKeys"]) { eq(Adhkar.doneKey(id), key, "doneKey \(id)") }
    for (t, ids) in dict(m["expand"]) { eq(AdhkarClock.expand([t]).map { $0.id }, ids, "expand \(t)") }
    let listsK = dict(m["expandLists"])
    let lists: [String: [String]] = [
        "adhan": AdhkarClock.adhan, "iftar": AdhkarClock.iftar, "after": AdhkarClock.after, "morning": AdhkarClock.morning,
        "evening": AdhkarClock.evening, "dhikr": AdhkarClock.dhikr, "friday": AdhkarClock.friday, "arafah": AdhkarClock.arafah,
        "night": AdhkarClock.night, "lastThird": AdhkarClock.lastThird,
        "fridayEveNight": [WidgetText.salawatId, WidgetText.kahfId] + AdhkarClock.night,
        "fridayEvening": [WidgetText.hourId] + AdhkarClock.evening,
        "mixed": ["@sleep", "@sleep/2", "@sleep/99", "@nope", "@morning/0", "mor01", "zzz", "fri_hour", "@daily/25"],
    ]
    eq(lists.count, listsK.count, "expandLists count")
    for (name, l) in lists { eq(AdhkarClock.expand(l).map { $0.id }, listsK[name], "expandList \(name)") }
    for c in arr(m["clip"]) {
        let a = arr(c)
        eq(AdhkarLive.clip(str(a[0])!, int(a[1])), a[2], "clip \(int(a[1])) «\(String(str(a[0])!.prefix(30)))»")
    }
    for (id, v) in dict(m["byId"]) { eq(Adhkar.byId(id)?.id, v, "byId \(id)") }

    // ما عُدّ واكتمل القسم (منطق الشاشة)
    section = "adhkar.screen"
    let sec = Adhkar.section("morning")!
    var done: [String: Int] = [:]
    for d in sec.items { for _ in 0..<d.count { _ = Adhkar.tap(d, done: &done) } }
    eq(Adhkar.allDone(sec, done: done), true, "allDone")
    eq(Adhkar.progress(sec, done: done).done, sec.items.reduce(0) { $0 + $1.count }, "progress")
    eq(Adhkar.tap(sec.items[0], done: &done), 0, "tap بعد التمام يعود صفراً")
    eq(Adhkar.allDone(sec, done: done), false, "allDone بعد التصفير")
}

// ---------- الأحاديث ----------

func checkHadith() {
    section = "hadith"
    let f = dict(load("hadith.json"))
    let ids = Hadiths.ids()
    eq(ids, f["ids"], "الترتيب")
    let ar = dict(f["ar"])
    for id in ids {
        let a = Hadiths.get(id, code: "ar")!.ar
        eq([a.title, h(a.text), h(a.expl), a.benefits.count, h(a.benefits.joined(separator: "\u{1}")), a.grade, h(a.takhrij)] as [Any?], ar[id], "ar \(id)")
    }
    let langs = dict(f["langs"])
    for code in langs.keys.sorted() {
        let k = dict(langs[code])
        for id in ids {
            let x = Hadiths.get(id, code: code)!
            let t = x.tr.map { h([$0.title, $0.text, $0.expl, $0.benefits.joined(separator: "\u{1}"), $0.grade, $0.takhrij].joined(separator: "\u{2}")) } ?? nil
            eq([x.trLang, x.link, t] as [Any?], k[id], "\(code) \(id)")
        }
    }
    let start = LocalDate.parse(str(f["indexStart"])!)!
    for (i, v) in arr(f["index"]).enumerated() { eq(Hadiths.indexFor(start.plusDays(i)), v, "indexFor \(start.plusDays(i))") }
    for (day, m) in dict(f["forDay"]) {
        for (code, v) in dict(m) {
            let x = Hadiths.forDay(LocalDate.parse(day)!, code: code)
            eq(x.map { [$0.id, $0.trLang] as [Any?] }, v, "forDay \(day) \(code)")
        }
    }
    for (i, v) in dict(f["at"]) { eq(Hadiths.at(Int(i)!, code: "en")?.id, v, "at \(i)") }
    eq(Hadiths.get("nope", code: "en")?.id, f["missing"], "missing")
    eq(Hadiths.get(ids[0], code: "xx")?.trLang, f["unknownLang"], "unknownLang")
}

// ---------- المسبحة ----------

func stJ(_ s: TasbihState, text: Bool) -> [String: Any] {
    var m: [String: Any] = ["mode": s.mode, "idx": s.idx, "step": s.step, "count": s.count, "target": s.target, "rounds": s.rounds,
                            "total": s.total, "today": s.today, "day": s.day, "dhikrIndex": s.dhikrIndex, "goal": s.goal]
    if text { m["text"] = s.text }
    return m
}

func stFrom(_ k: [String: Any]) -> TasbihState {
    TasbihState(mode: str(k["mode"])!, idx: int(k["idx"]), step: int(k["step"]), count: int(k["count"]), target: int(k["target"]),
                rounds: int(k["rounds"]), total: int(k["total"]), today: int(k["today"]), day: str(k["day"])!)
}

func checkTasbih() {
    section = "tasbih"
    let f = dict(load("tasbih.json"))
    eq(Tasbih.dhikr, f["dhikr"], "DHIKR")
    eq(Tasbih.salah.map { [$0.0, $0.1] }, f["salah"], "SALAH")
    eq(Tasbih.tamam, f["tamam"], "TAMAM")
    eq(Tasbih.targets, f["targets"], "TARGETS")
    for (si, sc) in arr(f["scripts"]).enumerated() {
        let m = dict(sc)
        let initK = dict(m["init"])
        var s = stFrom(initK)
        eq(stJ(s, text: true), initK, "script \(si) init")
        for (i, step) in arr(m["steps"]).enumerated() {
            let a = arr(step)
            let op = str(a[0])!
            var ev: String? = nil
            let arg = op.contains(":") ? String(op[op.range(of: ":")!.upperBound...]) : ""
            if op == "count" { let (n, e) = s.counted(); s = n; ev = e.rawValue }
            else if op.hasPrefix("countN:") { let (n, e) = s.counted(times: Int(arg)!); s = n; ev = e.rawValue }
            else if op == "reset" { s = s.reset() }
            else if op == "undo" { s = s.undo() }
            else if op.hasPrefix("mode:") { s = s.withMode(arg) }
            else if op.hasPrefix("dhikr:") { s = s.withDhikr(Int(arg)!) }
            else if op.hasPrefix("target:") { s = s.withTarget(Int(arg)!) }
            else if op.hasPrefix("day:") { s = s.on(arg) }
            eq([op, ev as Any?, stJ(s, text: false)] as [Any?], a, "script \(si) step \(i) \(op)")
        }
    }
    eq(TasbihState(mode: "salah", step: 3).meaning(meanings: ["a"], tamamMeaning: "T"), "T", "meaning تمام")
    eq(TasbihState(idx: 5).meaning(meanings: ["a"], tamamMeaning: "T"), "", "meaning ناقص")

    section = "tasbih.store"
    let st = dict(load("tasbih_store.json"))
    let today = str(st["today"])!
    for (ci, c) in arr(st["cases"]).enumerated() {
        let m = dict(c)
        let (defaults, settings) = freshSettings("ts\(ci)")
        writePreset(dict(m["preset"]), defaults)
        let t = Tasbih(settings: settings, today: { today })
        var changes = 0
        t.onChange = { _ in changes += 1 }
        for (i, step) in arr(m["steps"]).enumerated() {
            let a = arr(step)
            let op = str(a[0])!
            let arg = op.contains(":") ? String(op[op.range(of: ":")!.upperBound...]) : ""
            var ev: String? = nil
            switch op {
            case "ensure": t.ensure()
            case "count": ev = t.count().rawValue
            case "undo": t.undo()
            case "reset": t.reset()
            default:
                if op.hasPrefix("countN:") { ev = t.count(Int(arg)!).rawValue }
                else if op.hasPrefix("setMode:") { t.setMode(arg) }
                else if op.hasPrefix("setDhikr:") { t.setDhikr(Int(arg)!) }
                else if op.hasPrefix("setTarget:") { t.setTarget(Int(arg)!) }
            }
            eq(ev, a[1], "case \(ci) step \(i) \(op) event")
            eq(stJ(t.state, text: true), a[2], "case \(ci) step \(i) \(op) state")
            eq(dump(defaults).filter { $0.key.hasPrefix("ts_") }, dict(a[3]).filter { $0.key.hasPrefix("ts_") }, "case \(ci) step \(i) \(op) store")
            // ما كتبه Kotlin تقرؤه Swift كما هو
            let (d2, s2) = freshSettings("ts\(ci)r")
            writePreset(dict(a[3]), d2)
            if op != "ensure" || dict(a[2])["day"] as? String == today { eq(stJ(Tasbih.read(s2), text: true), a[2], "case \(ci) step \(i) read") }
        }
        eq(changes, arr(m["steps"]).count - 1, "case \(ci) onChange")
    }
}

// ---------- الإعدادات ----------

func freshSettings(_ name: String) -> (UserDefaults, AppSettings) {
    let suite = "com.mizan.check.\(name)"
    let d = UserDefaults(suiteName: suite)!
    d.removePersistentDomain(forName: suite)
    for k in d.dictionaryRepresentation().keys where k.hasPrefix("mizan.") { d.removeObject(forKey: k) }
    return (d, AppSettings(defaults: d))
}

/// يكتب القيم كما يكتبها Prefs في iOS: setObject للنص، و setInteger للعدد، و setBool للمنطقي
func writePreset(_ p: [String: Any], _ d: UserDefaults) {
    for (k, v) in p {
        let m = dict(v)
        switch str(m["t"]) {
        case "string": d.set(str(m["v"])!, forKey: "mizan." + k)
        case "integer": d.set(int(m["v"]), forKey: "mizan." + k)
        case "bool": d.set(m["v"] as! Bool, forKey: "mizan." + k)
        default: print("نوع غير معروف \(k)"); failures += 1
        }
    }
}

/// ما في المخزن تحت «mizan.» بأنواعه
func dump(_ d: UserDefaults) -> [String: Any] {
    var out: [String: Any] = [:]
    for (k, v) in d.dictionaryRepresentation() where k.hasPrefix("mizan.") {
        let key = String(k.dropFirst("mizan.".count))
        if let s = v as? String { out[key] = ["t": "string", "v": s] }
        else if isBoolValue(v) { out[key] = ["t": "bool", "v": v as! Bool] }
        else if let n = v as? NSNumber { out[key] = ["t": "integer", "v": n.intValue] }
        else if let i = v as? Int { out[key] = ["t": "integer", "v": i] }
        else { out[key] = ["t": "other", "v": "\(v)"] }
    }
    return out
}

func placeJ(_ p: Place) -> [String: Any?] {
    ["key": p.key, "lat": p.lat, "lng": p.lng, "tz": p.tz, "method": p.method, "gps": p.gps, "elev": p.elev]
}

func placeFrom(_ m: [String: Any]) -> Place {
    Place(key: str(m["key"]), lat: dbl(m["lat"]), lng: dbl(m["lng"]), tz: str(m["tz"])!, method: str(m["method"])!, gps: m["gps"] as! Bool, elev: int(m["elev"]))
}

func snapshot(_ s: AppSettings) -> [String: Any?] {
    ["placeSet": s.placeSet, "place": placeJ(s.place), "method": s.method, "hanafiChoice": s.hanafiChoice, "use24h": s.use24h,
     "iqama": s.iqama, "theme": s.theme, "accent": s.accent, "notify": s.notify, "alertModes": s.alertModes.mapValues { $0.key },
     "muezzin": s.muezzin, "fajrMuezzin": s.fajrMuezzin, "vibePattern": s.vibePattern.key, "lang": s.lang]
}

func checkSettings() {
    section = "settings"
    let f = dict(load("settings.json"))
    let kotlinTz = str(f["systemTimeZone"])!
    let cities: [(String, String)] = arr(f["arabicCities"]).map { let a = arr($0); return (str(a[0])!, str(a[1])!) }
    let arabicKey: (String) -> String? = { name in cities.first { $0.1 == name }?.0 }
    let defs = dict(f["defaults"])
    eq(placeJ(Place.makkah), defs["place"], "المكان الافتراضي")
    eq(IQAMA_DEFAULT, defs["iqama"], "IQAMA_DEFAULT")
    eq(Muezzins.default.id, defs["muezzin"], "muezzin الافتراضي")
    eq(Muezzins.fajrDefault.id, defs["fajrMuezzin"], "fajrMuezzin الافتراضي")
    eq(VibePattern.default.key, defs["vibePattern"], "vibePattern الافتراضي")

    /// منطقة الجهاز: ما كتبه Kotlin بمنطقة JVM يُقارن بمنطقة الجهاز هنا
    func fixTz(_ snap: Any?) -> Any? {
        guard var m = snap as? [String: Any], var p = m["place"] as? [String: Any] else { return snap }
        if str(p["tz"]) == kotlinTz { p["tz"] = systemTimeZoneId() }
        m["place"] = p
        return m
    }

    for (si, sc) in arr(f["scenarios"]).enumerated() {
        let m = dict(sc)
        let name = str(m["name"])!
        let (d, s) = freshSettings("set\(si)")
        writePreset(dict(m["preset"]), d)
        let elev = m["elevationNear"].map { int($0) } ?? 0
        let def = str(m["defaultMethod"]) ?? "mwl"
        AppSettings.elevationNear = { _, _ in elev }
        for (i, step) in arr(m["steps"]).enumerated() {
            let a = arr(step)
            let op = arr(a[0])
            let o = str(op[0])!
            switch o {
            case "init": s.initialize(arabicCityKey: arabicKey, defaultMethod: { _ in def })
            case "updatePlace": s.updatePlace(placeFrom(dict(op[1])))
            case "updateMethod": s.updateMethod(str(op[1])!)
            case "updateHanafi": s.updateHanafi(op[1] as! Bool)
            case "updateUse24h": s.updateUse24h(op[1] as! Bool)
            case "updateIqama": s.updateIqama(str(op[1])!, int(op[2]))
            case "updateTheme": s.updateTheme(str(op[1])!)
            case "updateAccent": s.updateAccent(str(op[1])!)
            case "updateNotify": s.updateNotify(op[1] as! Bool)
            case "updateAlertMode": s.updateAlertMode(str(op[1])!, AlertMode.of(str(op[2])))
            case "updateMuezzin": s.updateMuezzin(str(op[1])!)
            case "updateFajrMuezzin": s.updateFajrMuezzin(str(op[1])!)
            case "updateVibePattern": s.updateVibePattern(VibePattern.of(str(op[1])))
            case "updateLang": s.updateLang(str(op[1])!)
            case "putInt": s.putInt(str(op[1])!, int(op[2]))
            case "putBool": s.putBool(str(op[1])!, op[2] as! Bool)
            case "putString": s.putString(str(op[1])!, str(op[2])!)
            default: print("عملية غير معروفة \(o)"); failures += 1
            }
            let ctx = "\(name) step \(i) \(o)"
            eq(dump(d), a[1], "\(ctx) المخزن")
            eq(snapshot(s), fixTz(a[2]), "\(ctx) القراءة")
            // ما كتبه Kotlin تقرؤه Swift كما هو
            let (d2, s2) = freshSettings("set\(si)r")
            writePreset(dict(a[1]), d2)
            eq(snapshot(s2), fixTz(a[2]), "\(ctx) قراءة ما كتبه Kotlin")
        }
    }
    AppSettings.elevationNear = { _, _ in 0 }

    for p in arr(f["doubles"]) {
        let a = arr(p)
        let v = dbl(a[0])
        eq(kotlinDoubleString(v), a[1], "Double.toString \(v)")
        eq(Double(str(a[1])!), v, "toDouble \(str(a[1])!)")
    }

    // المخزن العام والقيم الافتراضية
    let (_, s) = freshSettings("misc")
    eq(s.isEmpty(), true, "isEmpty")
    eq(s.getInt("x", 7), 7, "getInt افتراضي")
    eq(s.getBool("x", true), true, "getBool افتراضي")
    eq(s.getLong("x", 9), 9, "getLong افتراضي")
    s.putLong("x", 1 << 40)
    eq(s.getLong("x", 0), 1 << 40, "putLong")
    eq(s.liveCounters, true, "live_counters افتراضي")
    s.updateLiveCounters(false)
    eq(s.liveCounters, false, "live_counters")
    eq(s.isEmpty(), false, "isEmpty بعد الكتابة")
    s.remove("x")
    eq(s.contains("x"), false, "remove")
}

// ---------- عدّاد الأذكار ----------

func posJ(_ p: AdhkarLive.Position) -> [String: Any] { ["section": p.section, "index": p.index, "count": p.count, "day": p.day, "done": p.done] }

func checkAdhkarLive() {
    section = "adhkarLive"
    let f = dict(load("adhkar_live.json"))
    let today = str(f["today"])!
    AdhkarLive.today = { today }
    for (ri, r) in arr(f["runs"]).enumerated() {
        let m = dict(r)
        let name = str(m["name"])!
        let (d, s) = freshSettings("al\(ri)")
        AdhkarLive.settings = s
        writePreset(dict(m["preset"]), d)
        for (i, step) in arr(m["steps"]).enumerated() {
            let a = arr(step)
            let parts = str(a[0])!.components(separatedBy: ":")
            let (sec, act) = (parts[0], parts[1])
            var p: AdhkarLive.Position
            var e: AdhkarLive.Event? = nil
            switch act {
            case "count": (p, e) = { let x = AdhkarLive.count(sec); return (x.0, Optional(x.1)) }()
            case "skip": (p, e) = { let x = AdhkarLive.skip(sec); return (x.0, Optional(x.1)) }()
            case "restart": p = AdhkarLive.restart(sec)
            default: p = AdhkarLive.position(sec)
            }
            eq(e?.rawValue, a[1], "\(name) step \(i) \(str(a[0])!) event")
            eq(posJ(p), a[2], "\(name) step \(i) position")
            eq(dump(d).filter { $0.key.hasPrefix("al_") }, dict(a[3]).filter { $0.key.hasPrefix("al_") }, "\(name) step \(i) store")
        }
    }
    AdhkarLive.settings = .shared

    section = "adhkarLive.state"
    for (code, list) in dict(f["states"]) {
        for item in arr(list) {
            let a = arr(item)
            let pm = dict(a[0])
            let p = AdhkarLive.Position(section: str(pm["section"])!, index: int(pm["index"]), count: int(pm["count"]), day: str(pm["day"])!, done: pm["done"] as! Bool)
            let st = AdhkarLive.state(p, lang: code)
            let sj: [String: Any] = ["id": st.id, "section": st.section, "title": st.title, "text": st.text, "meaning": st.meaning,
                                     "count": st.count, "goal": st.goal, "countText": st.countText, "position": st.position,
                                     "progress": st.progress, "done": st.done, "doneText": st.doneText, "next": st.next,
                                     "again": st.again, "end": st.end]
            eq(sj, a[1], "\(code) \(p.section) \(p.index)/\(p.count) done=\(p.done)")
        }
    }
    eq(AdhkarLive.section(hour: 5.0, fajr: 4.9, asr: 15.5), "morning", "section صباح")
    eq(AdhkarLive.section(hour: 15.5, fajr: 4.9, asr: 15.5), "evening", "section مساء")
    eq(AdhkarLive.section(hour: 10, fajr: .nan, asr: 15.5), "evening", "section NaN")
}

// ---------- أذكار الوقت ----------

func checkClock() {
    section = "clock"
    let f = dict(load("adhkar_clock.json"))
    eq([AdhkarClock.adhanMin, AdhkarClock.afterMin, AdhkarClock.rotateSec], [f["adhanMin"], f["afterMin"], f["rotateSec"]], "الثوابت")
    for (k, v) in dict(f["sections"]) { eq(AdhkarClock.sectionOf(k), v, "sectionOf \(k)") }
    let lists = arr(f["lists"]).map { str($0)! }

    func times(_ v: Any?) -> AdhkarDayTimes {
        let a = arr(v).map(dbl)
        return AdhkarDayTimes(fajr: a[0], sunrise: a[1], dhuhr: a[2], asr: a[3], maghrib: a[4], isha: a[5])
    }
    func hd(_ v: Any?) -> HDate? { (v as? [Any]).map { HDate(int($0[0]), int($0[1]), int($0[2])) } }
    func wJ(_ w: AdhkarWindow) -> [Any?] {
        [w.kind, w.start.epochSecond, w.end.epochSecond, w.section, lists.firstIndex(of: w.items.joined(separator: ",")) ?? -1, w.prayer, w.tag, w.key]
    }

    let cases = arr(f["cases"]).map(dict)
    var inputs: [String: [String: (AdhkarDayTimes, HDate?)]] = [:]
    for c in cases where c["inputs"] != nil {
        var m: [String: (AdhkarDayTimes, HDate?)] = [:]
        for (d, v) in dict(c["inputs"]) { let a = arr(v); m[d] = (times(a[0]), hd(a[1])) }
        inputs[str(c["loc"])!] = m
    }
    for c in cases where c["windows"] != nil {
        let loc = str(c["loc"])!
        let inp = inputs[loc]!
        let tf: (LocalDate) -> AdhkarDayTimes = { d in
            guard let x = inp[d.description] else { print("ينقص يوم \(d) في \(loc)"); failures += 1; return times([0, 0, 0, 0, 0, 0]) }
            return x.0
        }
        let hf: (LocalDate) -> HDate? = { d in inp[d.description]?.1 ?? nil }
        let day = LocalDate.parse(str(c["day"])!)!
        eq(AdhkarClock.day(day, times: tf, hijri: hf).map(wJ), c["windows"], "\(loc) \(day)")
        for at in arr(c["at"]) {
            let a = arr(at)
            let now = LocalDateTime(epochSecond: Int64(int(a[1])))
            let w = AdhkarClock.at(now, times: tf, hijri: hf)
            eq([now.description, now.epochSecond, w.key, AdhkarClock.index(now, w, offset: 3, count: 7),
                AdhkarClock.index(now, w, offset: -10, count: 4), AdhkarClock.index(now, w, offset: 0, count: 0)] as [Any], a, "\(loc) at \(now)")
        }
    }
    var synth: [AdhkarDayTimes] = []
    for s in arr(f["sane"]) {
        let a = arr(s)
        let t = times(a[0])
        synth.append(t)
        eq(AdhkarClock.sane(t), a[1], "sane \(a[0])")
    }
    for s in arr(f["synthDays"]) {
        let a = arr(s)
        let t = synth[int(a[0])]
        let h = hd(a[1])
        let day = LocalDate.parse(str(a[2])!)!
        eq(AdhkarClock.day(day, times: { _ in t }, hijri: { _ in h }).map(wJ), a[3], "synth \(int(a[0])) \(String(describing: h)) \(day)")
    }
    // range = أيام متتالية
    let inp = inputs["makkah"]!
    let tf: (LocalDate) -> AdhkarDayTimes = { inp[$0.description]?.0 ?? AdhkarDayTimes(fajr: .nan, sunrise: .nan, dhuhr: .nan, asr: .nan, maghrib: .nan, isha: .nan) }
    let r = AdhkarClock.range(LocalDate(2026, 3, 1), days: 3, times: tf, hijri: { inp[$0.description]?.1 ?? nil })
    let joined = (0..<3).flatMap { AdhkarClock.day(LocalDate(2026, 3, 1).plusDays($0), times: tf, hijri: { inp[$0.description]?.1 ?? nil }) }
    eq(r.map { $0.key }, joined.map { $0.key }, "range")
}

// ---------- النصوص والجداول ----------

func checkTexts() {
    section = "texts"
    let f = dict(load("texts.json"))
    for (code, v) in dict(f["liveText"]) {
        let t = LiveText.of(code)
        eq(["end": t.end, "iqamaIn": t.iqamaIn, "iftar": t.iftar, "suhoor": t.suhoor, "iftarNow": t.iftarNow, "adhkarDone": t.adhkarDone,
            "tasbih": t.tasbih, "descTasbih": t.descTasbih, "wird": t.wird, "descWird": t.descWird, "showLive": t.showLive, "again": t.again], v, "LiveText \(code)")
    }
    for (code, v) in dict(f["widgetText"]) {
        let t = WidgetText.of(code)
        let m: [String: Any?] = [
            "adhkar": t.adhkar, "nextPrayer": t.nextPrayer, "prayerTimes": t.prayerTimes, "hijri": t.hijri, "dhikr": t.dhikr,
            "friday": t.friday, "arafah": t.arafah, "iftar": t.iftar, "lastThird": t.lastThird, "fridayEve": t.fridayEve,
            "lastTen": t.lastTen, "until": t.until, "next": t.next, "inTime": t.inTime, "openApp": t.openApp, "kahfCta": t.kahfCta,
            "descNext": t.descNext, "descTimes": t.descTimes, "descHijri": t.descHijri, "descAyah": t.descAyah, "descAdhkar": t.descAdhkar,
            "kahf": t.kahf, "salawat": t.salawat, "hour": t.hour, "after": t.after, "allowLocation": t.allowLocation, "locationHint": t.locationHint,
        ]
        eq(m, v, "WidgetText \(code)")
    }
    for (code, v) in dict(f["liveTasbihText"]) { let t = LiveTasbihText.of(code); eq([t.title, t.desc], v, "LiveTasbihText \(code)") }
    eq(LiveText.iftarDua, f["iftarDua"], "IFTAR_DUA")
    let extra = arr(f["widgetExtra"]).map(dict)
    eq(WidgetText.extra.count, extra.count, "EXTRA count")
    for e in extra { eq(WidgetText.extra[str(e["id"])!].map(dhikrJ), e, "EXTRA \(str(e["id"])!)") }
    eq([LiveText.of("xx") == LiveText.en, WidgetText.of("xx") == WidgetText.en], f["fallback"], "fallback")
    eq(LiveText.en.adhkarDone("Morning"), "Morning completed ✓", "adhkarDone")
    eq(WidgetText.en.untilTime("5:00"), "until 5:00", "untilTime")

    let mz = arr(f["muezzins"])
    eq(AdhanCatalog.all.count, mz.count, "عدد المؤذنين")
    for (i, m) in AdhanCatalog.all.enumerated() where i < mz.count {
        eq(["id": m.id, "fajr": m.fajr, "bundled": m.bundled, "seconds": m.seconds, "sizeKb": m.sizeKb, "takbir": m.takbir,
            "credit": m.credit, "file": m.file, "iosSound": m.iosSound] as [String: Any], mz[i], "Muezzin \(m.id)")
    }
    eq(Muezzins.default.id, f["muezzinDefault"], "Muezzins.DEFAULT")
    eq(Muezzins.fajrDefault.id, f["muezzinFajrDefault"], "Muezzins.FAJR_DEFAULT")
    eq(AlertMode.allCases.map { $0.key }, f["alertModes"], "AlertMode")
    let vp = arr(f["vibePatterns"])
    eq(VibePattern.allCases.count, vp.count, "عدد أنماط الاهتزاز")
    for (i, v) in VibePattern.allCases.enumerated() where i < vp.count {
        eq(["key": v.key, "timings": v.timings, "amps": v.amps, "millis": v.millis, "pulses": v.pulses.map { [$0.start, $0.duration, Int64($0.amp)] }] as [String: Any],
           vp[i], "VibePattern \(v.key)")
    }
    eq(AlertMode.of("bogus"), AlertMode.adhan, "AlertMode.of")
    eq(VibePattern.of(nil).key, "heartbeat", "VibePattern.of")
    // الأذان المختار لصلاة
    eq(Muezzins.forPrayer("fajr", muezzin: "asim", fajrMuezzin: "same", available: { _ in true }).id, "asim", "forPrayer same")
    eq(Muezzins.forPrayer("fajr", muezzin: "asim", fajrMuezzin: "kas_fajr", available: { $0.bundled }).id, "fouad_fajr", "forPrayer غير منزَّل")
    eq(Muezzins.forPrayer("dhuhr", muezzin: "kalkan_fajr", fajrMuezzin: "same", available: { _ in true }).id, "fouad", "forPrayer أذان فجر للظهر")
    eq(Muezzins.forPrayer("asr", muezzin: "klcc", fajrMuezzin: "same", available: { $0.bundled }).id, "fouad", "forPrayer غير منزَّل")
}

checkTexts()
checkAdhkar()
checkHadith()
checkTasbih()
checkSettings()
checkAdhkarLive()
checkClock()

print("CheckContent: \(checked) مقارنة، \(failures) اختلاف")
exit(failures == 0 ? 0 : 1)
