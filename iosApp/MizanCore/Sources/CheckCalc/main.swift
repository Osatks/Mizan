// يقارن نتائج MizanCalc بنتائج Kotlin المصدَّرة في Fixtures/calc (CalcFixturesExport.kt)، ويخرج بخطأ عند أي اختلاف.
// الاستعمال: swift run CheckCalc [مجلد Fixtures/calc]
import Foundation
import MizanBase
import MizanCalc

let dir: String = CommandLine.arguments.count > 1 ? CommandLine.arguments[1]
    : URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
        .appendingPathComponent("Fixtures/calc").path

var failures = 0
var checked = 0
var current = ""

func fail(_ msg: String) {
    failures += 1
    if failures <= 60 { print("✗ [\(current)] \(msg)") }
}

func load(_ name: String) -> [[String: Any]] {
    let url = URL(fileURLWithPath: dir).appendingPathComponent(name)
    guard let data = try? Data(contentsOf: url),
          let arr = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
        print("✗ cannot read \(url.path)"); exit(2)
    }
    return arr
}

// الأعداد العشرية مكتوبة نصوصاً (toString في Kotlin)
func dbl(_ v: Any?) -> Double? {
    guard let s = v as? String else { return nil }
    switch s {
    case "NaN": return .nan
    case "Infinity": return .infinity
    case "-Infinity": return -.infinity
    default: return Double(s)
    }
}
func int(_ v: Any?) -> Int? { (v as? NSNumber)?.intValue }
func i64(_ v: Any?) -> Int64? { (v as? NSNumber)?.int64Value }
func bool(_ v: Any?) -> Bool? { (v as? NSNumber)?.boolValue }
func str(_ v: Any?) -> String? { v as? String }

func same(_ a: Double?, _ b: Double?) -> Bool {
    switch (a, b) {
    case (nil, nil): return true
    case let (x?, y?): return x.bitPattern == y.bitPattern || (x.isNaN && y.isNaN)
    default: return false
    }
}
func eqD(_ label: String, _ swift: Double?, _ kotlin: Any?) {
    let k = dbl(kotlin)
    if !same(swift, k) { fail("\(label): swift=\(swift.map { "\($0)" } ?? "nil") kotlin=\(k.map { "\($0)" } ?? "nil")") }
}
func eq<T: Equatable>(_ label: String, _ swift: T?, _ kotlin: T?) {
    if swift != kotlin { fail("\(label): swift=\(swift.map { "\($0)" } ?? "nil") kotlin=\(kotlin.map { "\($0)" } ?? "nil")") }
}
func counts(_ v: Any?) -> [String: Int] {
    var m: [String: Int] = [:]
    for (k, x) in (v as? [String: Any]) ?? [:] { m[k] = int(x) }
    return m
}
func gender(_ v: Any?) -> Character { Character(str(v) ?? "m") }

// MARK: المواريث
for (i, c) in load("inheritance.json").enumerated() {
    checked += 1
    let input = counts(c["in"])
    current = "inheritance #\(i) \(str(c["g"])!) \(input.sorted { $0.key < $1.key })"
    let r = Inheritance.solve(input, gender: gender(c["g"]))
    guard let k = c["r"] as? [String: Any] else {
        if r != nil { fail("expected nil") }
        continue
    }
    guard let r = r else { fail("got nil"); continue }
    let kres = (k["res"] as? [[Any]]) ?? []
    eq("keys", r.keys, kres.map { $0[0] as! String })
    for row in kres {
        let key = row[0] as! String
        guard let s = r.res[key] else { fail("missing \(key)"); continue }
        eq("\(key).share", s.share?.description, row[1] as? String)
        eq("\(key).type", s.type.rawValue, row[2] as? String)
        eq("\(key).blocked", s.blocked, row[3] as? String)
        eq("\(key).radd", s.radd, bool(row[4]))
    }
    eq("notes", r.notes.map { $0.description }, (k["notes"] as? [String]) ?? [])
    eq("asl", r.asl, i64(k["asl"]))
    eq("awl", r.awl, i64(k["awl"]))
    eq("tashih", r.tashih, i64(k["tashih"]))
    eq("radd", r.radd, bool(k["radd"]))
}

// MARK: التركة ومبالغ الورثة
for (i, c) in load("estate.json").enumerated() {
    checked += 1
    current = "estate #\(i)"
    let e = InheritanceEstate(estate: dbl(c["estate"])!, funeral: dbl(c["funeral"])!, debts: dbl(c["debts"])!, will: dbl(c["will"])!)
    eqD("afterDebt", e.afterDebt, c["afterDebt"])
    eqD("net", e.net, c["net"])
    eq("capped", e.willCapped, bool(c["capped"]))
    let input = counts(c["in"])
    let r = Inheritance.solve(input, gender: gender(c["g"]))
    guard let amounts = c["amounts"] as? [[Any]] else { if r != nil { fail("expected nil result") }; continue }
    guard let r = r else { fail("nil result"); continue }
    let mine = r.keys.filter { r.res[$0]!.share != nil && r.res[$0]!.blocked == nil }
    eq("amount keys", mine, amounts.map { $0[0] as! String })
    for row in amounts {
        let key = row[0] as! String
        guard let sh = r.res[key]?.share else { continue }
        let c = input[key] ?? 1
        eqD("\(key).amount", e.amount(sh), row[1])
        eqD("\(key).perPerson", e.perPerson(sh, count: c), row[2])
        eqD("\(key).pct", sh.value * 100, row[3])
    }
}

// MARK: زكاة النقدين
for (i, c) in load("zakat_money.json").enumerated() {
    checked += 1
    current = "zakat_money #\(i)"
    var vals: [String: Double] = [:]
    for (k, v) in (c["in"] as? [String: Any]) ?? [:] { vals[k] = dbl(v) }
    let z = ZakatMoney(ZakatMoneyInput(values: vals, basis: str(c["basis"])!, includeJewelry: bool(c["jewelry"])!))
    eqD("pure24", z.pure24, c["pure24"])
    eqD("excluded", z.excluded, c["excluded"])
    let parts = (c["parts"] as? [[Any]]) ?? []
    eq("parts", z.parts.map { $0.part.rawValue }, parts.map { $0[0] as! String })
    for (p, row) in zip(z.parts, parts) { eqD("part \(p.part)", p.value, row[1]) }
    eqD("total", z.total, c["total"])
    eqD("debts", z.debts, c["debts"])
    eqD("net", z.net, c["net"])
    eqD("nisab", z.nisab, c["nisab"])
    eq("due", z.due, bool(c["due"]))
    eqD("zakat", z.zakat, c["zakat"])
    eq("badge", z.badge.rawValue, str(c["badge"]))
    eq("ok", z.resultOk, bool(c["ok"]))
    eqD("progress", z.progress, c["progress"])
    eqD("grams", z.goldGrams, c["grams"])
    let bd = (c["breakdown"] as? [[Any]]) ?? []
    eq("breakdown", z.breakdown.map { $0.part.rawValue }, bd.map { $0[0] as! String })
    for (p, row) in zip(z.breakdown, bd) { eqD("breakdown \(p.part)", p.value, row[1]) }
}

// MARK: العملات
for c in load("currencies.json") {
    let lang = str(c["lang"])!
    current = "currencies \(lang)"
    checked += 1
    eq("default", Currencies.defaultIso(langCode: lang, packCurrency: nil), str(c["default"]))
    let list = (c["list"] as? [[String]]) ?? []
    eq("list", Currencies.all(packCurrency: nil).map { [$0.iso, $0.arSymbol, $0.arName, $0.symbol(arabic: lang == "ar")] }, list)
    for row in (c["saved"] as? [[String]]) ?? [] {
        checked += 1
        eq("saved \(row[0])", Currencies.saved(raw: row[0], langCode: lang, packCurrency: nil), row[1])
    }
}

// MARK: الأنعام
func duesList(_ v: Any?) -> [ZakatKinds.Due] { ((v as? [[Any]]) ?? []).map { ZakatKinds.Due($0[0] as! String, int($0[1])!) } }
for c in load("livestock_dues.json") {
    checked += 1
    let n = int(c["n"])!
    current = "livestock_dues n=\(n)"
    eq("camels", ZakatKinds.camels(n), duesList(c["camels"]))
    eq("cattle", ZakatKinds.cattle(n), duesList(c["cattle"]))
    eq("sheep", ZakatKinds.sheep(n), duesList(c["sheep"]))
}
for (i, c) in load("livestock.json").enumerated() {
    checked += 1
    current = "livestock #\(i)"
    let ns = ((c["in"] as? [Any]) ?? []).map { ZakatLivestock.count(dbl($0)!) }
    eq("n", ns, (c["n"] as? [NSNumber])?.map { $0.intValue })
    let z = ZakatLivestock(camels: ns[0], cattle: ns[1], sheep: ns[2], grazing: bool(c["grazing"])!, year: bool(c["year"])!)
    eq("total", z.total, int(c["total"]))
    eq("badge", z.badge.rawValue, str(c["badge"]))
    eq("ok", z.resultOk, bool(c["ok"]))
    let status: [[String]] = z.shownRows.map { r in
        let idx = ZakatLivestock.Kind.allCases.firstIndex(of: r.kind)!
        let s: String
        switch z.status(r) { case .nothingDue: s = "nothingDue"; case .belowNisab: s = "belowNisab"; case .dues: s = "dues" }
        return ["\(idx)", s]
    }
    eq("status", status, ((c["status"] as? [[Any]]) ?? []).map { ["\(int($0[0])!)", $0[1] as! String] })
}
if let data = try? Data(contentsOf: URL(fileURLWithPath: dir).appendingPathComponent("to_int.json")),
   let arr = try? JSONSerialization.jsonObject(with: data) as? [[Any]] {
    for row in arr {
        checked += 1
        current = "to_int \(row[0])"
        eq("count", ZakatLivestock.count(dbl(row[0])!), int(row[1]))
    }
}

// MARK: الزروع
for (i, c) in load("crops.json").enumerated() {
    checked += 1
    current = "crops #\(i)"
    let z = ZakatCrops(kg: dbl(c["kg"])!, irrigation: str(c["irr"])!, price: dbl(c["price"])!)
    eqD("due", z.due, c["due"])
    eqD("rate", z.ratePercent, c["rate"])
    eq("badge", z.badge.rawValue, str(c["badge"]))
    eq("ok", z.resultOk, bool(c["ok"]))
    eqD("value", z.value, c["value"])
}

// MARK: المعادن والركاز
for (i, c) in load("minerals.json").enumerated() {
    checked += 1
    current = "minerals #\(i)"
    let z = ZakatMinerals(rikazValue: dbl(c["rikazValue"])!, mineralValue: dbl(c["mineralValue"])!, goldPrice: dbl(c["gold"])!)
    eqD("rikaz", z.rikaz, c["rikaz"])
    eqD("mineral", z.mineral, c["mineral"])
    eqD("total", z.total, c["total"])
    eqD("nisab", z.nisab, c["nisab"])
}

// MARK: الفطر والفدية
for (i, c) in load("fitr.json").enumerated() {
    checked += 1
    current = "fitr #\(i)"
    let f = Fitr(people: int(c["people"])!, mode: str(c["mode"])!, food: Fitr.food(str(c["food"])!)!, custom: dbl(c["custom"])!,
                 kgPrice: dbl(c["kgPrice"])!, valuePerPerson: dbl(c["value"])!)
    eqD("sa", f.sa, c["sa"])
    eqD("kg", f.kg, c["kg"])
    eqD("cost", f.cost, c["cost"])
    eqD("cash", f.cashTotal, c["cash"])
}
for (i, c) in load("fidya.json").enumerated() {
    checked += 1
    current = "fidya #\(i)"
    let measure = dbl(c["measure"])!
    let f = Fidya(tab: str(c["tab"])!, days: int(c["days"])!, oaths: int(c["oaths"])!, measure: measure, price: dbl(c["price"])!)
    eq("poor", f.poor, int(c["poor"]))
    eqD("kg", f.kg, c["kg"])
    eq("fast", f.fastDays, int(c["fast"]))
    eqD("value", f.value, c["valueOut"])
    eq("measureKey", Fidya.measureKey(measure), str(c["measureKey"]))
    eq("measure", Fidya.measure(Fidya.measureKey(measure)), measure)
}
for (i, c) in load("jima_months.json").enumerated() {
    checked += 1
    current = "jima #\(i)"
    let ml = int(c["ml"])!
    let t = Fidya.twoMonthsTarget(y: int(c["y"])!, m: int(c["m"])!, d: int(c["d"])!) { _, _ in ml }
    eq("target", [t.y, t.m, t.d], (c["out"] as? [NSNumber])?.map { $0.intValue })
}

for c in load("jima_period.json") {
    checked += 1
    let start = LocalDate.parse(str(c["start"])!)!
    current = "jima_period \(start)"
    let p = Fidya.twoMonths(from: start)
    eq("end", p.map { "\($0.end)" }, str(c["end"]))
    eq("days", p.map { $0.days }, int(c["days"]))
}

if failures > 0 {
    print("CheckCalc: \(failures) mismatches in \(checked) cases")
    exit(1)
}
print("CheckCalc: all \(checked) cases match")
