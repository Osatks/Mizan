// يقارن منطق القرآن في Swift بنتائج Kotlin (Fixtures/quran، يولّدها QuranFixturesExport.kt) حالةً حالة.
// الاستعمال: swift run CheckQuran [مجلد الحزمة]
import Foundation
import MizanBase
import MizanQuran

let pkg: String = CommandLine.arguments.count > 1 ? CommandLine.arguments[1]
    : URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent().path
let fixtures = pkg + "/Fixtures/quran/"
Assets.root = pkg + "/../../android-app/shared/src/commonMain/assets"
QuranData.open = { Assets.data($0) }
QuranData.filesDir = NSTemporaryDirectory() + "/mizan-checkquran-\(getpid())"

var checked = 0
var failed = 0
var section = "" {
    didSet {
        let now = Date()
        if !oldValue.isEmpty { print("  \(oldValue): \(String(format: "%.1f", now.timeIntervalSince(sectionStart)))s") }
        sectionStart = now
    }
}
var sectionStart = Date()

func expect(_ ok: Bool, _ msg: @autoclosure () -> String) {
    checked += 1
    if !ok {
        failed += 1
        if failed <= 60 { print("✗ [\(section)] \(msg())") }
    }
}

func load(_ name: String) -> Any {
    guard let d = FileManager.default.contents(atPath: fixtures + name), let o = try? JSONSerialization.jsonObject(with: d, options: [.fragmentsAllowed]) else {
        print("missing fixture \(name)"); exit(2)
    }
    return o
}

func I(_ v: Any?) -> Int? {
    if v is NSNull || v == nil { return nil }
    if let i = v as? Int { return i }
    if let d = v as? Double { return Int(d) }
    if let n = v as? NSNumber { return n.intValue }
    return nil
}
func D(_ v: Any?) -> Double { (v as? Double) ?? (v as? NSNumber)?.doubleValue ?? Double(I(v) ?? 0) }
func B(_ v: Any?) -> Bool { (v as? Bool) ?? ((v as? NSNumber)?.boolValue ?? false) }
func S(_ v: Any?) -> String? { v as? String }
func A(_ v: Any?) -> [Any] { (v as? [Any]) ?? [] }
func M(_ v: Any?) -> [String: Any] { (v as? [String: Any]) ?? [:] }
func IA(_ v: Any?) -> [Int] { A(v).map { I($0)! } }
func SA(_ v: Any?) -> [String] { A(v).map { $0 as! String } }

/// String.hashCode في Java
func jh(_ s: String) -> Int32 {
    var h: Int32 = 0
    for u in s.utf16 { h = h &* 31 &+ Int32(u) }
    return h
}
func chunks(_ a: [String], _ f: (String) -> String = { $0 }) -> [Int] {
    stride(from: 0, to: a.count, by: 50).map { s in
        var h: Int32 = 17
        for i in s..<min(s + 50, a.count) { h = h &* 31 &+ jh(f(a[i])) }
        return Int(h)
    }
}
func same(_ a: String, _ b: String?) -> Bool { b != nil && a.utf8.elementsEqual(b!.utf8) }
func show(_ s: String) -> String { s.unicodeScalars.map { $0.value < 0x80 && $0.value >= 0x20 ? String($0) : String(format: "\\u{%X}", $0.value) }.joined() }

// ---------- البيانات الوصفية ----------
section = "meta"
do {
    let m = M(load("meta.json"))
    expect(I(m["ayahs"]) == QuranMeta.AYAHS, "AYAHS")
    for s in A(m["surahs"]) {
        let o = M(s); let n = I(o["n"])!
        let first = QuranMeta.STARTS[n - 1], last = first + QuranMeta.COUNTS[n - 1] - 1
        expect(I(o["count"]) == QuranMeta.COUNTS[n - 1], "count \(n)")
        expect(I(o["start"]) == first, "start \(n)")
        expect(B(o["medinan"]) == QuranMeta.isMedinan(n), "medinan \(n)")
        expect(same(QuranMeta.NAMES_AR[n - 1], S(o["ar"])), "ar \(n)")
        expect(same(QuranMeta.TNAMES[n - 1], S(o["t"])), "t \(n)")
        expect(same(QuranMeta.ENAMES[n - 1], S(o["e"])), "e \(n)")
        expect(same(QuranMeta.NAMES_EN[n - 1], S(o["en"])), "en \(n)")
        expect(I(o["firstPage"]) == QuranMeta.pageOf(first) && I(o["lastPage"]) == QuranMeta.pageOf(last), "pages \(n)")
        expect(I(o["firstJuz"]) == QuranMeta.juzOf(first) && I(o["lastJuz"]) == QuranMeta.juzOf(last), "juz \(n)")
        expect(I(o["hizb"]) == QuranMeta.hizbOf(first), "hizb \(n)")
        expect(I(o["index1"]) == QuranMeta.index(n, 1) && I(o["indexLast"]) == QuranMeta.index(n, QuranMeta.COUNTS[n - 1]), "index \(n)")
    }
    expect(IA(m["juz"]) == QuranMeta.JUZ, "JUZ")
    expect(IA(m["pages"]) == QuranMeta.PAGES, "PAGES")
    expect(IA(m["quarters"]) == QuranMeta.QUARTERS, "QUARTERS")
    expect(IA(m["sajdas"]) == QuranMeta.SAJDAS, "SAJDAS")
    let ls = IA(m["locSurah"]), la = IA(m["locAyah"]), jz = IA(m["juzOf"]), pg = IA(m["pageOf"]), hz = IA(m["hizbOf"])
    for i in 0..<QuranMeta.AYAHS {
        let (s, a) = QuranMeta.locate(i)
        expect(s == ls[i] && a == la[i], "locate \(i)")
        expect(QuranMeta.juzOf(i) == jz[i] && QuranMeta.pageOf(i) == pg[i] && QuranMeta.hizbOf(i) == hz[i], "juz/page/hizb \(i)")
        expect(QuranMeta.index(s, a) == i, "index roundtrip \(i)")
    }
    for (k, r) in A(m["pageRanges"]).enumerated() {
        let v = IA(r); let p = k + 1
        let range = QuranMeta.pageRange(p)
        let loc = QuranMeta.locate(range.lowerBound)
        expect([range.lowerBound, range.upperBound, QuranMeta.juzOf(range.lowerBound), QuranMeta.hizbOf(range.lowerBound), loc.surah, loc.ayah] == v, "page \(p)")
    }
    expect(SA(m["juzOrdinals"]) == QuranMeta.JUZ_ORDINALS, "juzOrdinals")
    for x in A(m["arabicDigits"]) { let v = A(x); expect(same(arabicDigits(I(v[0])!), S(v[1])), "arabicDigits \(v[0])") }
    for (k, x) in A(m["isRight"]).enumerated() { expect(QuranSpreads.isRight(k + 1) == B(x), "isRight \(k + 1)") }
    for (key, spv) in M(m["spreads"]) {
        let sp = key == "true"; let o = M(spv)
        expect(I(o["count"]) == QuranSpreads.count(sp), "spread count \(sp)")
        for (k, x) in IA(o["index"]).enumerated() { expect(QuranSpreads.index(k - 2, sp) == x, "spread index \(k - 2) \(sp)") }
        for (k, x) in A(o["pages"]).enumerated() { let r = QuranSpreads.pages(k, sp); expect([r.lowerBound, r.upperBound] == IA(x), "spread pages \(k) \(sp)") }
        for (k, x) in A(o["ayahs"]).enumerated() { let r = QuranSpreads.ayahs(k, sp); expect([r.lowerBound, r.upperBound] == IA(x), "spread ayahs \(k) \(sp)") }
        for x in A(o["follow"]) { let v = IA(x); expect(QuranSpreads.follow(v[0], v[1], sp) == v[2], "follow \(v) \(sp)") }
    }
}

// ---------- حزم اللغات والمصادر ----------
section = "sources"
let searchFx = M(load("search.json"))
Sources.loadInstalledPacks()
do {
    let packs = A(searchFx["packs"]).map(M)
    expect(Sources.packs.map { $0.code } == packs.map { S($0["code"])! }, "packs \(Sources.packs.map { $0.code })")
    for (p, f) in zip(Sources.packs, packs) {
        expect(p.quranId == S(f["id"]) && p.quranAuthor == (S(f["author"]) ?? "") && p.translit == S(f["translit"]) && p.tafsirFallback == SA(f["fallback"]), "pack \(p.code)")
    }
    let all = A(searchFx["all"]).map(SA)
    expect(all.count == Sources.ALL.count, "ALL count")
    for (s, f) in zip(Sources.ALL, all) { expect([s.id, s.lang, s.kind.rawValue, s.author] == f, "source \(s.id)") }
    for l in A(searchFx["langs"]).map(M) {
        let code = S(l["code"])!
        expect(Sources.translationFor(code)?.id == S(l["translation"]), "translationFor \(code)")
        expect(Sources.defaultFor(code).id == S(l["default"]), "defaultFor \(code)")
        expect(Sources.ordered(code).map { $0.id } == SA(l["ordered"]), "ordered \(code)")
    }
}

// ---------- النصوص ----------
section = "text"
do {
    let t = M(load("text.json"))
    for (path, v) in M(t["files"]) {
        let o = M(v)
        guard let a = try? QuranData.load(path) else { expect(false, "load \(path)"); continue }
        expect(a.count == QuranMeta.AYAHS, "size \(path)")
        let c = chunks(a), fc = IA(o["chunks"])
        for k in 0..<fc.count { expect(k < c.count && c[k] == fc[k], "chunk \(path) #\(k) (ayahs \(k * 50)…)") }
        let n = chunks(a) { Search.normalize($0) }, fn = IA(o["norm"])
        for k in 0..<fn.count { expect(k < n.count && n[k] == fn[k], "norm chunk \(path) #\(k) (ayahs \(k * 50)…)") }
        for x in A(o["samples"]) { let v = A(x); let i = I(v[0])!; expect(same(a[i], S(v[1])), "sample \(path) \(i)") }
        expect(a.filter { $0.isEmpty }.count == I(o["empty"]), "empty \(path)")
    }
    let u = try! QuranData.uthmani(), c = try! QuranData.clean()
    for x in A(t["bismillah"]) {
        let v = A(x); let i = I(v[0])!
        expect(same(QuranData.withoutBismillah(u, i), S(v[1])), "bismillah uthmani \(i)")
        expect(same(QuranData.withoutBismillah(c, i), S(v[2])), "bismillah clean \(i)")
    }
    for (k, x) in A(t["header"]).enumerated() { expect(QuranData.hasBismillahHeader(k + 1) == B(x), "header \(k + 1)") }
    for x in A(t["parse"]) {
        let v = A(x); let a = QuranData.parse(Data(S(v[0])!.utf8)); let want = SA(v[1])
        let got = (0...8).map { a[$0] } + [a[QuranMeta.index(2, 286)]]
        for k in 0..<want.count { expect(same(got[k], want[k]), "parse #\(k): \(show(got[k])) != \(show(want[k]))") }
    }
    for x in A(t["translit"]) { let v = SA(x); let g = Translit.uzLatn(v[0]); expect(same(g, v[1]), "uzLatn \(v[0]) → \(g) != \(v[1])") }
}

// ---------- التطبيع ----------
section = "normalize"
do {
    let n = M(load("normalize.json"))
    for x in A(n["normalize"]) { let v = SA(x); let g = Search.normalize(v[0]); expect(same(g, v[1]), "normalize \(show(v[0])) → \(show(g)) != \(show(v[1]))") }
    for x in A(n["terms"]) { let v = A(x); let g = Search.terms(S(v[0])!); expect(g.count == SA(v[1]).count && zip(g, SA(v[1])).allSatisfy { same($0, $1) }, "terms \(show(S(v[0])!))") }
    for x in A(n["score"]) {
        let v = A(x)
        let g = Search.score(Search.terms(S(v[0])!), S(v[1])!, S(v[2])!)
        expect(g == I(v[3]), "score \(v[0]) / \(v[1]) / \(v[2]) = \(g) != \(v[3])")
    }
}

if CommandLine.arguments.contains("--bench") {
    var t = Date(); _ = QuranSearch.ayahs("الرحمن", lang: "en"); print("first ayahs: \(Date().timeIntervalSince(t))")
    t = Date(); for _ in 0..<200 { _ = QuranSearch.ayahs("الحي القيوم", lang: "en") }; print("200 ayahs: \(Date().timeIntervalSince(t))")
    t = Date(); for _ in 0..<200 { _ = QuranSearch.surahs("kahf", names: nil) }; print("200 surahs: \(Date().timeIntervalSince(t))")
    t = Date(); for _ in 0..<200 { _ = QuranSearch.ayahs("mercy", lang: "en") }; print("200 ayahs en: \(Date().timeIntervalSince(t))")
}

// ---------- البحث ----------
section = "search"
do {
    var names: [String: [String]] = [:]
    for l in A(searchFx["langs"]).map(M) { names[S(l["code"])!] = SA(l["surahNames"]) }
    for q in A(searchFx["queries"]).map(M) {
        let s = S(q["q"])!
        expect(QuranSearch.parseRef(s) == I(q["ref"]), "parseRef \(show(s)) = \(String(describing: QuranSearch.parseRef(s)))")
        let tg = Search.terms(s), tw = SA(q["terms"])
        expect(tg.count == tw.count && zip(tg, tw).allSatisfy { same($0, $1) }, "terms \(show(s))")
        for (lang, v) in M(q["surahs"]) { let g = QuranSearch.surahs(s, names: names[lang]); expect(g == IA(v), "surahs \(show(s)) [\(lang)] \(g) != \(IA(v))") }
        for (lang, v) in M(q["ayahs"]) { let g = QuranSearch.ayahs(s, lang: lang); expect(g == IA(v), "ayahs \(show(s)) [\(lang)] \(g.prefix(8)) != \(IA(v).prefix(8))") }
        for (lang, v) in M(q["ayahs6"]) { let g = QuranSearch.ayahs(s, lang: lang, limit: 6); expect(g == IA(v), "ayahs6 \(show(s)) [\(lang)] \(g) != \(IA(v))") }
    }
    for q in A(searchFx["names"]).map(M) {
        let s = S(q["q"])!
        for (lang, v) in M(q["surahs"]) { let g = QuranSearch.surahs(s, names: names[lang]); expect(g == IA(v), "surah names \(show(s)) [\(lang)] \(g) != \(IA(v))") }
    }
}

// ---------- آية اليوم ----------
section = "ayahOfDay"
do {
    let u = try! QuranData.uthmani()
    let short = AyahOfDay.shortAyahs(u)
    for (k, x) in A(load("ayahofday.json")).enumerated() {
        let v = IA(x)
        expect(AyahOfDay.pick(short, day: v[0]) == v[1], "day \(v[0])")
        if k % 1000 == 0 { expect(ayahOfDay(u, day: v[0]) == v[1], "ayahOfDay \(v[0])") }
    }
    expect(ayahOfDay(u, LocalDate(2026, 10, 15)) == AyahOfDay.pick(short, day: LocalDate(2026, 10, 15).epochDay), "LocalDate overload")
}

// ---------- مصحف المدينة ----------
section = "madinah"
do {
    let m = M(load("madinah.json"))
    for (k, pv) in A(m["pages"]).enumerated() {
        let p = k + 1
        let lines = Madinah.lines(p), want = A(pv)
        expect(lines.count == want.count, "lines count \(p)")
        for (j, w) in want.enumerated() where j < lines.count {
            let l = lines[j]
            if w is NSNull { expect(l == nil, "line \(p):\(j) nil"); continue }
            let v = A(w)
            switch (S(v[0])!, l) {
            case ("s", .header(let s)?): expect(s == I(v[1]), "header \(p):\(j)")
            case ("b", .basmala?): expect(true, "")
            case ("a", .ayah(let centered, let glyphs, let ayahs, let ends)?):
                expect(centered == B(v[1]), "centered \(p):\(j)")
                if let g = v[2] as? [Any] { expect(glyphs == g.map { $0 as! String }, "glyphs \(p):\(j)") }
                else { expect(Int(jh(glyphs.joined())) == I(v[2]), "glyphs hash \(p):\(j)") }
                expect(ayahs == IA(v[3]), "ayahs \(p):\(j)")
                expect(ends.sorted() == IA(v[4]), "ends \(p):\(j)")
            default: expect(false, "kind \(p):\(j) \(v[0]) vs \(String(describing: l))")
            }
        }
    }
    for (k, x) in A(m["headers"]).enumerated() { expect(same(Madinah.headerCode(k), S(x)), "headerCode \(k)") }
    expect(Madinah.lines(0).count == I(m["missing"]), "missing page")
}

// ---------- الختمة ----------
func storeValues(_ o: [String: Any]) -> [String: Any] {
    var out: [String: Any] = [:]
    for (k, v) in o { if let s = v as? String { out[k] = s } else if let i = I(v) { out[k] = i } }
    return out
}
func storeMatches(_ mem: MemoryQuranStore, _ want: [String: Any], prefix: String) -> Bool {
    let got = mem.values.filter { $0.key.hasPrefix(prefix) }
    let w = storeValues(want)
    guard Set(got.keys) == Set(w.keys) else { return false }
    for (k, v) in w {
        if let s = v as? String { guard let g = got[k] as? String, same(g, s) else { return false } }
        else if let i = v as? Int { guard let g = got[k] as? Int, g == i else { return false } }
    }
    return true
}
func ranges(_ pages: [Int]) -> String {
    var parts: [String] = []
    var i = 0
    while i < pages.count {
        var j = i
        while j + 1 < pages.count && pages[j + 1] == pages[j] + 1 { j += 1 }
        parts.append(j > i ? "\(pages[i])-\(pages[j])" : "\(pages[i])")
        i = j + 1
    }
    return parts.joined(separator: ",")
}

section = "progress"
do {
    let p = M(load("progress.json"))
    let today = QuranDay.parse(S(p["today"])!)!
    expect(QuranDay.format(today) == S(p["today"]), "format today")
    QuranProgress.today = { today }
    expect(I(p["pages"]) == QuranProgress.PAGES && IA(p["plans"]) == QuranProgress.PLANS, "constants")
    for scv in A(p["scenarios"]) {
        let sc = M(scv); let name = S(sc["name"])!
        let mem = MemoryQuranStore(storeValues(M(sc["store"])))
        QuranSettings.store = mem
        QuranProgress.reset()
        for stv in A(sc["steps"]) {
            let st = A(stv); let step = S(st[0])!; let w = M(st[1])
            let parts = step.split(separator: " ").map(String.init)
            switch parts[0] {
            case "ensure": QuranProgress.ensure()
            case "read": QuranProgress.markRead(Int(parts[1])!)
            case "restart": QuranProgress.restart(parts.count > 1 ? Int(parts[1])! : nil)
            case "plan": QuranProgress.changePlan(Int(parts[1])!)
            case "rollover": mem.values["k_today_date"] = QuranDay.format(today - 1); QuranProgress.ensure()
            case "rollover2":
                mem.values["k_today_date"] = QuranDay.format(today - 1); mem.values["k_last"] = QuranDay.format(today - 1)
                QuranProgress.reset(); QuranProgress.ensure()
            case "reload": QuranProgress.reset(); QuranProgress.ensure()
            default: expect(false, "unknown step \(step)")
            }
            let q = QuranProgress.self
            let tag = "\(name) / \(step)"
            expect(q.readCount == I(w["readCount"]), "\(tag) readCount \(q.readCount) != \(w["readCount"]!)")
            expect(q.todayCount == I(w["todayCount"]), "\(tag) todayCount \(q.todayCount) != \(w["todayCount"]!)")
            expect(q.streak == I(w["streak"]), "\(tag) streak \(q.streak) != \(w["streak"]!)")
            expect(q.completed == I(w["completed"]), "\(tag) completed")
            expect(q.planDays == I(w["planDays"]), "\(tag) planDays")
            expect(QuranDay.format(q.start) == S(w["start"]), "\(tag) start \(QuranDay.format(q.start)) != \(w["start"]!)")
            expect(q.daysLeft() == I(w["daysLeft"]), "\(tag) daysLeft \(q.daysLeft()) != \(w["daysLeft"]!)")
            expect(q.dailyGoal() == I(w["dailyGoal"]), "\(tag) dailyGoal \(q.dailyGoal()) != \(w["dailyGoal"]!)")
            expect(q.nextUnread() == I(w["nextUnread"]), "\(tag) nextUnread")
            expect(q.finished == B(w["finished"]), "\(tag) finished")
            expect(ranges((0...610).filter { q.isRead($0) }) == S(w["read"]), "\(tag) read pages")
            expect(storeMatches(mem, M(w["store"]), prefix: "k_"), "\(tag) store \(mem.values.filter { $0.key.hasPrefix("k_") }) != \(M(w["store"]))")
        }
    }
}

// ---------- إعدادات القراءة ----------
section = "prefs"
do {
    func compare(_ w: [String: Any], _ mem: MemoryQuranStore, _ tag: String) {
        let P = QuranPrefs.self
        expect(P.last == I(w["last"]) && P.textSize == I(w["textSize"]) && P.mode == S(w["mode"]) && P.source == S(w["source"]), "\(tag) last/size/mode/source")
        expect(P.hidden == SA(w["hidden"]), "\(tag) hidden \(P.hidden)")
        expect(P.tone == S(w["tone"]) && P.brightness == Float(D(w["brightness"])), "\(tag) tone/brightness \(P.brightness) != \(w["brightness"]!)")
        expect(P.reciter == S(w["reciter"]) && P.repeat == I(w["repeat"]) && P.focusLast == I(w["focusLast"]), "\(tag) reciter/repeat/focus")
        expect(P.rangeMode == S(w["rangeMode"]) && P.customFrom == I(w["customFrom"]) && P.customTo == I(w["customTo"]) && P.rangeRepeat == I(w["rangeRepeat"]), "\(tag) range")
        expect(P.gap == I(w["gap"]) && P.speed == Float(D(w["speed"])) && P.sleep == I(w["sleep"]) && P.script == S(w["script"]), "\(tag) gap/speed/sleep/script \(P.speed)")
        expect(P.bookmarks == IA(w["bookmarks"]), "\(tag) bookmarks \(P.bookmarks)")
        expect(P.modeFor("ar") == S(w["modeAr"]) && P.modeFor("en") == S(w["modeEn"]), "\(tag) modeFor")
        expect(P.sourceFor("ar").id == S(w["sourceAr"]) && P.sourceFor("en").id == S(w["sourceEn"]) && P.sourceFor("kk").id == S(w["sourceKk"]), "\(tag) sourceFor \(P.sourceFor("ar").id) \(P.sourceFor("en").id) \(P.sourceFor("kk").id)")
        expect(Sources.visible("ar").map { $0.id } == SA(w["visibleAr"]) && Sources.visible("uz").map { $0.id } == SA(w["visibleUz"]), "\(tag) visible")
        expect(storeMatches(mem, M(w["store"]), prefix: "q_"), "\(tag) store \(mem.values.filter { $0.key.hasPrefix("q_") }) != \(M(w["store"]))")
    }
    for cv in A(load("prefs.json")) {
        let c = M(cv)
        let mem = MemoryQuranStore(storeValues(M(c["store"])))
        QuranSettings.store = mem
        QuranPrefs.reset(); QuranPrefs.ensure()
        compare(M(c["loaded"]), mem, "load \(M(c["store"]).keys.sorted().prefix(3))")
        for stv in A(c["steps"]) {
            let st = A(stv); let a = S(st[0])!
            let p = a.split(separator: " ").map(String.init)
            let P = QuranPrefs.self
            switch p[0] {
            case "last": P.updateLast(Int(p[1])!)
            case "size": P.updateSize(Int(p[1])!)
            case "mode": P.updateMode(p[1])
            case "source": P.updateSource(p[1])
            case "tone": P.updateTone(p[1])
            case "bright": P.updateBrightness(Float(p[1])!)
            case "reciter": P.updateReciter(p[1])
            case "repeat": P.updateRepeat(Int(p[1])!)
            case "rmode": P.updateRangeMode(p[1])
            case "custom": P.updateCustom(Int(p[1])!, Int(p[2])!)
            case "rrep": P.updateRangeRepeat(Int(p[1])!)
            case "gap": P.updateGap(Int(p[1])!)
            case "speed": P.updateSpeed(Float(p[1])!)
            case "sleep": P.updateSleep(Int(p[1])!)
            case "script": P.updateScript(p[1])
            case "focus": P.updateFocusLast(Int(p[1])!)
            case "mark": P.toggleBookmark(Int(p[1])!)
            case "hide": P.setVisible(p[1], false)
            case "show": P.setVisible(p[1], true)
            default: expect(false, "unknown action \(a)")
            }
            compare(M(st[1]), mem, a)
        }
    }
}

section = "done"
print("CheckQuran: \(checked) checks, \(failed) mismatches")
exit(failed == 0 ? 0 : 1)
