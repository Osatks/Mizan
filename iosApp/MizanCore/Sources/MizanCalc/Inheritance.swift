// محرك علم الفرائض على مذهب الجمهور (نقلاً عن core/Inheritance.kt)، والتركة الصافية بعد التجهيز والدين والوصية (InheritanceScreen.kt)

/// وارث: مفتاحه، وأكثر عدد له، والجنس الذي يرثه وحده ('f' الزوج إذا كان الميت امرأة، 'm' الزوجة إذا كان رجلاً)، ومجموعته
public struct Heir: Equatable, Sendable {
    public let key: String
    public let max: Int
    public let only: Character?
    public let group: String

    public init(_ key: String, _ max: Int, _ only: Character? = nil, _ group: String) {
        self.key = key
        self.max = max
        self.only = only
        self.group = group
    }
}

/// نوع استحقاق الوارث؛ يُترجم إلى نص في طبقة اللغات (rawValue اسمه في Kotlin)
public enum ShareType: String, CaseIterable, Sendable {
    case fard = "FARD"
    case fardTasib = "FARD_TASIB"
    case fardTakmila = "FARD_TAKMILA"
    case tasib = "TASIB"
    case tasibBilGhayr = "TASIB_BIL_GHAYR"
    case tasibMaaGhayr = "TASIB_MAA_GHAYR"
    case tasibMaaJadd = "TASIB_MAA_JADD"
    case fardThenMuqasama = "FARD_THEN_MUQASAMA"
    case jaddMuqasama = "JADD_MUQASAMA"
    case jaddThirdEstate = "JADD_THIRD_ESTATE"
    case jaddThirdRest = "JADD_THIRD_REST"
    case jaddSixth = "JADD_SIXTH"
}

/// نصيب الوارث؛ blocked سبب الحجب: مفتاح وارث (مثل "son") أو أحد المفاتيح في Inheritance.blockers
public final class Share {
    public var share: Fr?
    public var type: ShareType
    public var blocked: String?
    public var radd: Bool

    public init(_ share: Fr? = nil, _ type: ShareType = .fard, blocked: String? = nil, radd: Bool = false) {
        self.share = share
        self.type = type
        self.blocked = blocked
        self.radd = radd
    }
}

/// ملاحظات المسألة؛ تُترجم في طبقة اللغات. description كنص Kotlin (toString)
public enum InhNote: Equatable, CustomStringConvertible, Sendable {
    case matSiblingsShare
    case umariyya
    case grandmothersShare
    case fullSistersGhayr
    case akdariyya
    case jaddRule(otherFard: Bool)
    case muadda
    case nothingLeft(winner: String)
    case mushtaraka
    case awl(asl: Int64, awl: Int64)
    case radd
    case spouseOnlyRadd
    case noHeirs

    public var description: String {
        switch self {
        case .matSiblingsShare: return "MatSiblingsShare"
        case .umariyya: return "Umariyya"
        case .grandmothersShare: return "GrandmothersShare"
        case .fullSistersGhayr: return "FullSistersGhayr"
        case .akdariyya: return "Akdariyya"
        case .jaddRule(let o): return "JaddRule(otherFard=\(o))"
        case .muadda: return "Muadda"
        case .nothingLeft(let w): return "NothingLeft(winner=\(w))"
        case .mushtaraka: return "Mushtaraka"
        case .awl(let a, let w): return "Awl(asl=\(a), awl=\(w))"
        case .radd: return "Radd"
        case .spouseOnlyRadd: return "SpouseOnlyRadd"
        case .noHeirs: return "NoHeirs"
        }
    }
}

/// نتيجة القسمة: res الأنصبة بمفاتيح الورثة (keys بترتيب إدخالها كما في Kotlin)، وأصل المسألة وعولها وتصحيحها
public struct InheritanceResult {
    public let res: [String: Share]
    public let keys: [String]
    public let notes: [InhNote]
    public let asl: Int64
    public let awl: Int64?
    public let tashih: Int64
    public let radd: Bool
}

// خريطة مرتبة بترتيب الإدخال (LinkedHashMap)
struct OrderedMap<V> {
    private(set) var keys: [String] = []
    private(set) var dict: [String: V] = [:]

    subscript(_ k: String) -> V? {
        get { dict[k] }
        set {
            if let v = newValue {
                if dict[k] == nil { keys.append(k) }
                dict[k] = v
            } else if dict[k] != nil {
                dict[k] = nil
                keys.removeAll { $0 == k }
            }
        }
    }

    var values: [V] { keys.map { dict[$0]! } }
    var isEmpty: Bool { keys.isEmpty }
}

/// محرك علم الفرائض على مذهب الجمهور
public enum Inheritance {
    public static let heirs: [Heir] = [
        Heir("husband", 1, "f", "spouses"),
        Heir("wife", 4, "m", "spouses"),
        Heir("father", 1, nil, "ascendants"),
        Heir("mother", 1, nil, "ascendants"),
        Heir("gf", 1, nil, "ascendants"),
        Heir("gmm", 1, nil, "ascendants"),
        Heir("gmf", 1, nil, "ascendants"),
        Heir("son", 30, nil, "descendants"),
        Heir("daughter", 30, nil, "descendants"),
        Heir("gson", 30, nil, "descendants"),
        Heir("gdau", 30, nil, "descendants"),
        Heir("fb", 30, nil, "siblings"),
        Heir("fs", 30, nil, "siblings"),
        Heir("pb", 30, nil, "siblings"),
        Heir("ps", 30, nil, "siblings"),
        Heir("mb", 30, nil, "siblings"),
        Heir("ms", 30, nil, "siblings"),
        Heir("fnep", 30, nil, "distant"),
        Heir("pnep", 30, nil, "distant"),
        Heir("fun", 30, nil, "distant"),
        Heir("pun", 30, nil, "distant"),
        Heir("fcous", 30, nil, "distant"),
        Heir("pcous", 30, nil, "distant"),
    ]
    public static let groups = ["spouses", "ascendants", "descendants", "siblings", "distant"]

    /// أسباب حجب ليست اسم وارث بعينه ("gf" هنا: الجد باسمه المختصر)
    public static let blockers = ["gf", "twoDaughters", "descendant", "fbAfterMuadda", "fsTookRest", "gfAndSiblings", "twoFullSisters", "fsGhayr"]

    /// الورثة المعروضون لجنس الميت ('m' أو 'f')
    public static func heirs(for gender: Character) -> [Heir] {
        heirs.filter { $0.only == nil || $0.only == gender }
    }

    /// مدخلات الحساب من عدّادات الشاشة: الورثة المعروضون لهذا الجنس وعددهم أكبر من صفر (كما في InheritanceScreen)
    public static func active(_ counts: [String: Int], gender: Character) -> [String: Int] {
        var out: [String: Int] = [:]
        for h in heirs(for: gender) where (counts[h.key] ?? 0) > 0 { out[h.key] = counts[h.key] ?? 0 }
        return out
    }

    private static func f(_ n: Int64, _ d: Int64 = 1) -> Fr { Fr.of(n, d) }

    public static func solve(_ input: [String: Int], gender: Character) -> InheritanceResult? {
        var c = input
        if gender == "m" { c["husband"] = 0 } else { c["wife"] = 0 }
        func cnt(_ k: String) -> Int { c[k] ?? 0 }
        var res = OrderedMap<Share>()
        var notes: [InhNote] = []
        var fard = OrderedMap<Fr>()
        var label: [String: ShareType] = [:]
        func block(_ k: String, _ by: String) { if cnt(k) > 0 { res[k] = Share(blocked: by) } }

        let present = c.filter { $0.value > 0 }.map { $0.key }
        if present.isEmpty { return nil }

        // الفروع
        let son = cnt("son"), dau = cnt("daughter")
        var gson = cnt("gson"), gdau = cnt("gdau")
        if son > 0 { block("gson", "son"); block("gdau", "son"); gson = 0; gdau = 0 }
        if son == 0 && dau >= 2 && gdau > 0 && gson == 0 { block("gdau", "twoDaughters"); gdau = 0 }
        let maleDesc = son > 0 || gson > 0
        let femDesc = dau > 0 || gdau > 0
        let desc = maleDesc || femDesc

        // الأصول
        let father = cnt("father"), mother = cnt("mother")
        var gf = cnt("gf")
        if father > 0 && gf > 0 { block("gf", "father"); gf = 0 }
        var gmm = cnt("gmm"), gmf = cnt("gmf")
        if mother > 0 { block("gmm", "mother"); block("gmf", "mother"); gmm = 0; gmf = 0 }
        if father > 0 && gmf > 0 { block("gmf", "father"); gmf = 0 }

        let sibAll = cnt("fb") + cnt("fs") + cnt("pb") + cnt("ps") + cnt("mb") + cnt("ms")
        let spouseK = gender == "m" ? "wife" : "husband"
        let spouse = cnt(spouseK)

        if spouse > 0 {
            fard[spouseK] = gender == "f" ? (desc ? f(1, 4) : f(1, 2)) : (desc ? f(1, 8) : f(1, 4))
            label[spouseK] = .fard
        }

        // الإخوة لأم
        let matN = cnt("mb") + cnt("ms")
        if matN > 0 {
            let by: String? = desc ? "descendant" : father > 0 ? "father" : gf > 0 ? "gf" : nil
            if let by = by { block("mb", by); block("ms", by) } else {
                let total = matN == 1 ? f(1, 6) : f(1, 3)
                if cnt("mb") > 0 { fard["mb"] = total * cnt("mb") / matN; label["mb"] = .fard }
                if cnt("ms") > 0 { fard["ms"] = total * cnt("ms") / matN; label["ms"] = .fard }
                if matN > 1 { notes.append(.matSiblingsShare) }
            }
        }

        // الإخوة الأشقاء ولأب
        var fb = cnt("fb"), fs = cnt("fs"), pb = cnt("pb"), ps = cnt("ps")
        let sibBlocker: String? = son > 0 ? "son" : gson > 0 ? "gson" : father > 0 ? "father" : nil
        if let sb = sibBlocker { for k in ["fb", "fs", "pb", "ps"] { block(k, sb) }; fb = 0; fs = 0; pb = 0; ps = 0 }
        let jaddMode = gf > 0 && (fb + fs + pb + ps) > 0

        // البنات وبنات الابن
        if dau > 0 && son == 0 { fard["daughter"] = dau == 1 ? f(1, 2) : f(2, 3); label["daughter"] = .fard }
        if gdau > 0 && gson == 0 {
            if dau == 0 { fard["gdau"] = gdau == 1 ? f(1, 2) : f(2, 3); label["gdau"] = .fard }
            else if dau == 1 { fard["gdau"] = f(1, 6); label["gdau"] = .fardTakmila }
        }

        // الأم والجدات
        if mother > 0 {
            if desc || sibAll >= 2 { fard["mother"] = f(1, 6) }
            else if spouse > 0 && father > 0 && present.allSatisfy({ ["husband", "wife", "father", "mother"].contains($0) || res[$0]?.blocked != nil }) {
                fard["mother"] = (Fr.one - fard[spouseK]!) / 3
                notes.append(.umariyya)
            } else { fard["mother"] = f(1, 3) }
            label["mother"] = .fard
        }
        if gmm + gmf > 0 {
            let n = gmm + gmf
            if gmm > 0 { fard["gmm"] = f(1, 6) / n; label["gmm"] = .fard }
            if gmf > 0 { fard["gmf"] = f(1, 6) / n; label["gmf"] = .fard }
            if n > 1 { notes.append(.grandmothersShare) }
        }

        // الأب والجد
        if father > 0 {
            if maleDesc { fard["father"] = f(1, 6); label["father"] = .fard }
            else if femDesc { fard["father"] = f(1, 6); label["father"] = .fardTasib }
        }
        if gf > 0 && !jaddMode {
            if maleDesc { fard["gf"] = f(1, 6); label["gf"] = .fard }
            else if femDesc { fard["gf"] = f(1, 6); label["gf"] = .fardTasib }
        }

        // الأخوات (خارج باب الجد)
        var fsGhayr = false, psGhayr = false
        if !jaddMode {
            if fs > 0 && fb == 0 {
                if femDesc { fsGhayr = true; notes.append(.fullSistersGhayr) }
                else { fard["fs"] = fs == 1 ? f(1, 2) : f(2, 3); label["fs"] = .fard }
            }
            if fb > 0 { block("pb", "fb"); block("ps", "fb"); pb = 0; ps = 0 }
            else if fsGhayr { block("pb", "fsGhayr"); block("ps", "fsGhayr"); pb = 0; ps = 0 }
            if ps > 0 && pb == 0 {
                if femDesc && fs == 0 { psGhayr = true }
                else if fs == 0 { fard["ps"] = ps == 1 ? f(1, 2) : f(2, 3); label["ps"] = .fard }
                else if fs == 1 { fard["ps"] = f(1, 6); label["ps"] = .fardTakmila }
                else { block("ps", "twoFullSisters"); ps = 0 }
            }
        }

        var fardSum = fard.values.reduce(Fr.zero) { $0 + $1 }

        // الأكدرية
        func onlyKeys(_ keys: [String]) -> Bool { present.allSatisfy { keys.contains($0) || res[$0]?.blocked != nil } }
        if jaddMode && gender == "f" && spouse > 0 && mother > 0 && (fs + ps) == 1 && fb == 0 && pb == 0 &&
            onlyKeys(["husband", "mother", "gf", "fs", "ps"]) {
            let sk = fs > 0 ? "fs" : "ps"
            res["husband"] = Share(f(9, 27), .fard); res["mother"] = Share(f(6, 27), .fard)
            res["gf"] = Share(f(8, 27), .fardThenMuqasama); res[sk] = Share(f(4, 27), .fardThenMuqasama)
            notes.append(.akdariyya)
            return InheritanceResult(res: res.dict, keys: res.keys, notes: notes, asl: 6, awl: 9, tashih: 27, radd: false)
        }

        // الجد مع الإخوة (المقاسمة عند الجمهور)
        if jaddMode {
            let rem = Fr.one - fardSum
            let units = 2 + fb * 2 + fs + pb * 2 + ps
            let muq = rem > Fr.zero ? rem * 2 / units : Fr.zero
            let otherFard = fardSum > Fr.zero
            let gfShare: Fr
            let how: ShareType
            if !otherFard {
                let third = f(1, 3)
                if muq >= third { gfShare = muq; how = .jaddMuqasama } else { gfShare = third; how = .jaddThirdEstate }
            } else {
                let sixth = f(1, 6)
                let thirdRem = rem > Fr.zero ? rem / 3 : Fr.zero
                var best = sixth
                var bestHow = ShareType.jaddSixth
                if thirdRem > best { best = thirdRem; bestHow = .jaddThirdRest }
                if muq > best { best = muq; bestHow = .jaddMuqasama }
                gfShare = best; how = bestHow
            }
            res["gf"] = Share(gfShare, how)
            fardSum = fardSum + gfShare
            var sibRem = Fr.one - fardSum
            if sibRem < Fr.zero { sibRem = Fr.zero }
            notes.append(.jaddRule(otherFard: otherFard))
            if fb + fs > 0 && pb + ps > 0 { notes.append(.muadda) }

            func give(_ bk: String, _ sk: String, _ b: Int, _ s: Int, _ amount: Fr, _ type: ShareType) {
                let u = b * 2 + s
                if u == 0 { return }
                if b > 0 { res[bk] = Share(amount * (b * 2) / u, type) }
                if s > 0 { res[sk] = Share(amount * s / u, type) }
            }
            if fb > 0 {
                give("fb", "fs", fb, fs, sibRem, .tasib)
                if pb > 0 { res["pb"] = Share(blocked: "fbAfterMuadda") }
                if ps > 0 { res["ps"] = Share(blocked: "fbAfterMuadda") }
            } else if fs > 0 {
                if pb + ps > 0 {
                    let cap = fs == 1 ? f(1, 2) : f(2, 3)
                    let fsGet = sibRem > cap ? cap : sibRem
                    res["fs"] = Share(fsGet, .tasibMaaJadd)
                    let left = sibRem - fsGet
                    if left > Fr.zero { give("pb", "ps", pb, ps, left, .tasib) }
                    else {
                        if pb > 0 { res["pb"] = Share(blocked: "fsTookRest") }
                        if ps > 0 { res["ps"] = Share(blocked: "fsTookRest") }
                    }
                } else { give("fb", "fs", 0, fs, sibRem, .tasibMaaJadd) }
            } else {
                give("pb", "ps", pb, ps, sibRem, .tasibMaaJadd)
            }
            for k in ["fnep", "pnep", "fun", "pun", "fcous", "pcous"] { block(k, "gfAndSiblings") }
        }

        let fardDen = fard.values.reduce(Int64(1)) { Fr.lcm($0, $1.d) }
        let hasFard = !fard.isEmpty

        // العصبات
        var asabaTaken = jaddMode
        if !jaddMode {
            // nm: مفتاح العاصب كما يظهر في سبب الحجب
            struct Rank { let m: String?; let f: String?; let nm: String; var ghayr = false }
            let order = [
                Rank(m: "son", f: "daughter", nm: "son"), Rank(m: "gson", f: "gdau", nm: "gson"),
                Rank(m: "father", f: nil, nm: "father"), Rank(m: "gf", f: nil, nm: "gf"),
                Rank(m: "fb", f: "fs", nm: "fb"), Rank(m: nil, f: "fs", nm: "fs", ghayr: true),
                Rank(m: "pb", f: "ps", nm: "pb"), Rank(m: nil, f: "ps", nm: "ps", ghayr: true),
                Rank(m: "fnep", f: nil, nm: "fnep"), Rank(m: "pnep", f: nil, nm: "pnep"),
                Rank(m: "fun", f: nil, nm: "fun"), Rank(m: "pun", f: nil, nm: "pun"),
                Rank(m: "fcous", f: nil, nm: "fcous"), Rank(m: "pcous", f: nil, nm: "pcous"),
            ]
            let counts: [String: Int] = ["son": son, "daughter": dau, "gson": gson, "gdau": gdau, "father": father,
                                         "gf": gf, "fb": fb, "fs": fs, "pb": pb, "ps": ps]
            func eff(_ k: String) -> Int { counts[k] ?? cnt(k) }
            func live(_ k: String) -> Bool { eff(k) > 0 && res[k]?.blocked == nil }
            var winner: Rank? = nil
            for r in order {
                if r.ghayr {
                    let ok = (r.f == "fs" && fsGhayr) || (r.f == "ps" && psGhayr)
                    if ok && winner == nil { winner = r }
                    continue
                }
                guard let rm = r.m, live(rm) else { continue }
                if let w = winner {
                    block(rm, w.nm)
                    if let rf = r.f, live(rf), fard[rf] == nil { block(rf, w.nm) }
                } else { winner = r }
            }
            let rem = Fr.one - fardSum
            if let winner = winner {
                asabaTaken = true
                let residue = rem > Fr.zero ? rem : Fr.zero
                let mC = winner.m.map { eff($0) } ?? 0
                let fC: Int = { if let wf = winner.f, fard[wf] == nil { return eff(wf) }; return 0 }()
                let u = mC * 2 + fC
                let tLabel: ShareType = winner.m != nil ? .tasib : .tasibMaaGhayr
                if let wm = winner.m {
                    let s = residue * (mC * 2) / u
                    let own = fard[wm]
                    let type: ShareType = own != nil ? (label[wm] ?? .fardTasib) : (fC > 0 ? .tasibBilGhayr : tLabel)
                    res[wm] = Share((own ?? Fr.zero) + s, type)
                    fard[wm] = nil
                }
                if fC > 0, let wf = winner.f { res[wf] = Share(residue * fC / u, winner.m != nil ? .tasibBilGhayr : tLabel) }
                if residue.isZero && (mC > 0 || fC > 0) { notes.append(.nothingLeft(winner: winner.nm)) }
                if residue.isZero && winner.m == "fb" && matN >= 2 && spouse > 0 && (mother > 0 || gmm > 0 || gmf > 0) && !desc && father == 0 && gf == 0 {
                    notes.append(.mushtaraka)
                }
            }
        }

        for k in fard.keys where res[k] == nil { res[k] = Share(fard[k]!, label[k] ?? .fard) }

        var asl: Int64 = 1
        if jaddMode || !hasFard {
            for s in res.values { if let sh = s.share, !sh.isZero { asl = Fr.lcm(asl, sh.d) } }
        } else { asl = fardDen }
        let total = res.values.reduce(Fr.zero) { a, s in s.share.map { a + $0 } ?? a }
        var awl: Int64? = nil
        var radd = false

        if total > Fr.one {
            let a = javaRound(total.value * Double(asl))
            awl = a
            for s in res.values { s.share = s.share.map { $0 / total } }
            notes.append(.awl(asl: asl, awl: a))
        } else if total < Fr.one && !asabaTaken {
            let nonSpouse = res.keys.filter { res[$0]!.share != nil && $0 != "husband" && $0 != "wife" }
            let remain = Fr.one - total
            if !nonSpouse.isEmpty {
                let base = nonSpouse.reduce(Fr.zero) { $0 + res[$1]!.share! }
                for k in nonSpouse {
                    let s = res[k]!
                    s.share = s.share! + remain * s.share! / base
                    s.radd = true
                }
                radd = true
                notes.append(.radd)
            } else if let sp = res[spouseK], sp.share != nil {
                sp.share = Fr.one
                sp.radd = true
                radd = true
                notes.append(.spouseOnlyRadd)
            }
        }
        if !res.values.contains(where: { $0.share != nil && !$0.share!.isZero }) {
            notes.append(.noHeirs)
        }

        var tashih: Int64 = 1
        for k in res.keys {
            guard let sh = res[k]!.share else { continue }
            if !sh.isZero { tashih = Fr.lcm(tashih, (sh / Swift.max(c[k] ?? 1, 1)).d) }
        }
        if !jaddMode && !hasFard { asl = tashih }
        return InheritanceResult(res: res.dict, keys: res.keys, notes: notes, asl: asl, awl: awl, tashih: tashih, radd: radd)
    }
}

/// التقريب كما في Math.round / roundToLong في Kotlin (نصف الواحد إلى الأعلى)
func javaRound(_ x: Double) -> Int64 {
    if x.isNaN { return 0 }
    if x >= 9.223372036854775807e18 { return Int64.max }
    if x <= -9.223372036854775808e18 { return Int64.min }
    let fl = x.rounded(.down)
    return Int64(fl) + (x - fl >= 0.5 ? 1 : 0)
}

/// التركة الصافية: بعد التجهيز والديون، ثم الوصية في حدود الثلث (InheritanceScreen.kt)
public struct InheritanceEstate: Equatable {
    /// ما بقي بعد التجهيز والديون (لا يقل عن صفر)
    public let afterDebt: Double
    /// الوصية أكثر من الثلث فرُدَّت إليه
    public let willCapped: Bool
    /// ما يُقسم على الورثة
    public let net: Double

    public init(estate: Double, funeral: Double, debts: Double, will: Double) {
        let afterDebt = katLeast(estate - funeral - debts, 0.0)
        self.afterDebt = afterDebt
        willCapped = will > afterDebt / 3 + 1e-9
        net = katLeast(afterDebt - kmin(will, afterDebt / 3), 0.0)
    }

    /// مبلغ الوارث من التركة الصافية، ونصيب الفرد منه إذا كانوا أكثر من واحد
    public func amount(_ share: Fr) -> Double { net * share.value }
    public func perPerson(_ share: Fr, count: Int) -> Double { net * share.value / Double(count) }
}
