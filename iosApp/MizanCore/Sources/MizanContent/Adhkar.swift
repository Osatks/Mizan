import Foundation
import MizanBase

/// موضع آيات في ذكر قرآني: السورة ومن آية إلى آية
public struct AyahRange: Hashable {
    public let surah: Int
    public let from: Int
    public let to: Int
    public init(_ surah: Int, _ from: Int, _ to: Int) { self.surah = surah; self.from = from; self.to = to }
}

/// مصدر الذكر: رمز الكتاب ورقم الحديث، مثل ("bukhari", "6306") أو ("quran", "2:255")
public struct DhikrSource: Hashable {
    public let book: String
    public let number: String
    public init(_ book: String, _ number: String) { self.book = book; self.number = number }
}

/// ذكر واحد: نصه العربي، وعدد تكراره، ودليله (مصادره ودرجته وراويه وفضله) — adhkar/Adhkar.kt
public struct Dhikr: Hashable, Identifiable {
    public let id: String
    public let ar: String
    public let count: Int
    public let ayah: [AyahRange]
    public let src: [DhikrSource]
    public let grade: String
    public let by: String
    public let narr: String?
    public let fadl: String?
    public let note: String?

    public init(id: String, ar: String, count: Int, ayah: [AyahRange], src: [DhikrSource], grade: String, by: String,
                narr: String?, fadl: String?, note: String?) {
        self.id = id; self.ar = ar; self.count = count; self.ayah = ayah; self.src = src
        self.grade = grade; self.by = by; self.narr = narr; self.fadl = fadl; self.note = note
    }
}

/// مجموعة أذكار تحت عنوان فرعي؛ key: "<القسم>/<الترتيب>" لترجمة العنوان
public struct DhikrGroup: Hashable {
    public let key: String
    public let titleAr: String?
    public let items: [Dhikr]
    public init(key: String, titleAr: String?, items: [Dhikr]) { self.key = key; self.titleAr = titleAr; self.items = items }
}

/// قسم من الأذكار (الصباح، المساء، النوم…)
public struct AdhkarSection: Hashable, Identifiable {
    public let id: String
    public let icon: String
    public let titleAr: String
    public let descAr: String
    public let groups: [DhikrGroup]
    public init(id: String, icon: String, titleAr: String, descAr: String, groups: [DhikrGroup]) {
        self.id = id; self.icon = icon; self.titleAr = titleAr; self.descAr = descAr; self.groups = groups
    }
    public var items: [Dhikr] { groups.flatMap { $0.items } }
}

/// ترجمة ذكر: النص والراوي والفضل والملاحظة
public struct DhikrTr: Hashable {
    public let t: String?
    public let narr: String?
    public let fadl: String?
    public let note: String?
    public init(t: String?, narr: String?, fadl: String?, note: String?) { self.t = t; self.narr = narr; self.fadl = fadl; self.note = note }
}

/// عنوان القسم ووصفه مترجمين
public struct SectionTr: Hashable {
    public let title: String
    public let desc: String
    public init(title: String, desc: String) { self.title = title; self.desc = desc }
}

/// ترجمة الأذكار بلغة: عناوين الأقسام والمجموعات، وأسماء الكتب ودرجات الأحاديث، وترجمة كل ذكر
public struct AdhkarTr {
    public let sections: [String: SectionTr]
    public let groups: [String: String]
    public let books: [String: String]
    public let grades: [String: String]
    public let by: [String: String]
    public let items: [String: DhikrTr]

    public init(sections: [String: SectionTr], groups: [String: String], books: [String: String], grades: [String: String],
                by: [String: String], items: [String: DhikrTr]) {
        self.sections = sections; self.groups = groups; self.books = books; self.grades = grades; self.by = by; self.items = items
    }

    public static let empty = AdhkarTr(sections: [:], groups: [:], books: [:], grades: [:], by: [:], items: [:])

    /// يكمل الناقص من ترجمة أخرى (الإنجليزية)
    public func orElse(_ o: AdhkarTr) -> AdhkarTr {
        var it: [String: DhikrTr] = [:]
        for k in Set(o.items.keys).union(items.keys) {
            let a = items[k], b = o.items[k]
            it[k] = DhikrTr(t: a?.t ?? b?.t, narr: a?.narr ?? b?.narr, fadl: a?.fadl ?? b?.fadl, note: a?.note ?? b?.note)
        }
        return AdhkarTr(
            sections: o.sections.merging(sections) { _, n in n }, groups: o.groups.merging(groups) { _, n in n },
            books: o.books.merging(books) { _, n in n }, grades: o.grades.merging(grades) { _, n in n },
            by: o.by.merging(by) { _, n in n }, items: it
        )
    }

    /// من JSON بصيغة assets/adhkar/<code>.json
    public static func from(_ o: [String: Any]) -> AdhkarTr {
        func strMap(_ k: String) -> [String: String] {
            var r: [String: String] = [:]
            for (a, b) in (o[k] as? [String: Any]) ?? [:] { if let s = nonBlank(b) { r[a] = s } }
            return r
        }
        var secs: [String: SectionTr] = [:]
        for (k, v) in (o["sections"] as? [String: Any]) ?? [:] {
            let m = v as? [String: Any]
            if let t = nonBlank(m?["title"]) { secs[k] = SectionTr(title: t, desc: nonBlank(m?["desc"]) ?? "") }
        }
        var items: [String: DhikrTr] = [:]
        for (k, v) in (o["items"] as? [String: Any]) ?? [:] {
            let m = v as? [String: Any]
            items[k] = DhikrTr(t: nonBlank(m?["t"]), narr: nonBlank(m?["narr"]), fadl: nonBlank(m?["fadl"]), note: nonBlank(m?["note"]))
        }
        return AdhkarTr(sections: secs, groups: strMap("groups"), books: strMap("books"), grades: strMap("grades"), by: strMap("by"), items: items)
    }

    // ---------- عرض (ui/screens/AdhkarScreen.kt) ----------

    /** عنوان القسم بلغة الواجهة */
    public func title(_ sec: AdhkarSection, lang: String) -> String { lang == "ar" ? sec.titleAr : sections[sec.id]?.title ?? sec.titleAr }

    /** وصف القسم بلغة الواجهة */
    public func desc(_ sec: AdhkarSection, lang: String) -> String { lang == "ar" ? sec.descAr : sections[sec.id]?.desc ?? sec.descAr }

    /** عنوان المجموعة بلغة الواجهة */
    public func groupTitle(_ g: DhikrGroup, lang: String) -> String? { lang == "ar" ? g.titleAr : groups[g.key] ?? g.titleAr }
}

/// الأذكار: تُقرأ من assets/adhkar عند أول طلب وتبقى في الذاكرة
public enum Adhkar {
    /// قارئ الملفات (مسار داخل assets)؛ يُستبدل في الاختبارات
    public static var open: (String) -> Data? = { contentAsset($0) }

    private static let lock = NSLock()
    private static var cached: [AdhkarSection]?
    private static var trs: [String: AdhkarTr] = [:]

    private static func read(_ path: String) -> [String: Any]? { jsonObject(open(path)) }

    public static func sections() -> [AdhkarSection] {
        lock.lock(); defer { lock.unlock() }
        if let c = cached { return c }
        let s = parse(read("adhkar/adhkar.json") ?? [:])
        cached = s
        return s
    }

    /// الأقسام إن قُرئت من قبل (فلا يظهر مؤشر التحميل عند العودة)
    public static func loaded() -> [AdhkarSection]? { lock.lock(); defer { lock.unlock() }; return cached }

    public static func section(_ id: String) -> AdhkarSection? { sections().first { $0.id == id } }

    public static func byId(_ id: String) -> Dhikr? {
        for s in sections() { if let d = s.items.first(where: { $0.id == id }) { return d } }
        return nil
    }

    /// ترجمة لغة الواجهة مكمَّلة بالإنجليزية (والعربية تأخذ عناوينها وأسماء الكتب من ar.json).
    /// [pack]: حقل "adhkar" في حزمة لغة منزَّلة إن وُجد (يُقدَّم على assets/adhkar/<code>.json)
    public static func tr(_ code: String, pack: [String: Any]? = nil) -> AdhkarTr {
        lock.lock()
        if let t = trs[code] { lock.unlock(); return t }
        lock.unlock()
        let en = read("adhkar/en.json").map(AdhkarTr.from) ?? .empty
        let t: AdhkarTr
        if code == "en" { t = en } else {
            let own = (pack ?? read("adhkar/\(code).json")).map(AdhkarTr.from) ?? .empty
            t = own.orElse(en)
        }
        lock.lock(); defer { lock.unlock() }
        if let old = trs[code] { return old }
        trs[code] = t
        return t
    }

    /// بعد تثبيت حزمة أو تحديثها
    public static func clearTranslations() { lock.lock(); trs = [:]; lock.unlock() }

    /// للاختبارات: يُنسى ما قُرئ
    public static func clearCache() { lock.lock(); cached = nil; trs = [:]; lock.unlock() }

    public static func parse(_ o: [String: Any]) -> [AdhkarSection] {
        let items = (o["items"] as? [String: Any]) ?? [:]
        func dhikr(_ id: String) -> Dhikr? {
            guard let m = items[id] as? [String: Any] else { return nil }
            func s(_ k: String) -> String? { nonBlank(m[k]) }
            guard let ar = s("ar") else { return nil }
            let ayah: [AyahRange] = ((m["ayah"] as? [Any]) ?? []).compactMap { a in
                guard let l = a as? [Any] else { return nil }
                let n = l.compactMap { jsonInt($0) }
                return n.count == 3 ? AyahRange(n[0], n[1], n[2]) : nil
            }
            let src: [DhikrSource] = ((m["src"] as? [Any]) ?? []).compactMap { r in
                guard let rm = r as? [String: Any], let b = rm["b"] as? String else { return nil }
                return DhikrSource(b, jsonString(rm["n"]) ?? "")
            }
            return Dhikr(id: id, ar: ar, count: max(jsonInt(m["count"]) ?? 1, 1), ayah: ayah, src: src,
                         grade: s("grade") ?? "", by: s("by") ?? "", narr: s("narr"), fadl: s("fadl"), note: s("note"))
        }
        return ((o["sections"] as? [Any]) ?? []).compactMap { sv in
            guard let sm = sv as? [String: Any], let id = sm["id"] as? String else { return nil }
            var groups: [DhikrGroup] = []
            for (gi, gv) in ((sm["groups"] as? [Any]) ?? []).enumerated() {
                guard let gm = gv as? [String: Any] else { continue }
                let list = ((gm["items"] as? [Any]) ?? []).compactMap { ($0 as? String).flatMap(dhikr) }
                if list.isEmpty { continue }
                groups.append(DhikrGroup(key: "\(id)/\(gi)", titleAr: nonBlank(gm["title"]), items: list))
            }
            return AdhkarSection(id: id, icon: sm["icon"] as? String ?? "star", titleAr: sm["title"] as? String ?? id,
                                 descAr: sm["desc"] as? String ?? "", groups: groups)
        }
    }
}

// ---------- منطق شاشة الأذكار (ui/screens/AdhkarScreen.kt) ----------

extension Adhkar {
    /// القسم المناسب للوقت: الصباح حتى الظهر، والمساء من العصر، والنوم بعد العشاء، وفي رمضان أذكاره.
    /// [hijriMonth]: الشهر الهجري اليوم (Hijri.toHijri(LocalDate.now())?.m)
    public static func suggested(hour: Int, hijriMonth: Int?) -> String {
        switch hour {
        case 4...11: return "morning"
        case 15...20: return "evening"
        case 21...23, 0...1: return "sleep"
        default: return hijriMonth == 9 ? "ramadan" : "afterPrayer"
        }
    }

    /// القسم المناسب الآن (بتوقيت الجهاز)
    public static func suggestedNow() -> String {
        let now = nowIn(nil)
        return suggested(hour: now.hour, hijriMonth: Hijri.toHijri(now.date)?.m)
    }

    /// مفتاح «أتممت القسم اليوم» في الإعدادات: قيمته رقم اليوم (epochDay)
    public static func doneKey(_ id: String) -> String { "adhkar_done_\(id)" }

    /// هل أُتم القسم اليوم؟
    public static func doneToday(_ id: String, settings: AppSettings = .shared, today: LocalDate = LocalDate.now()) -> Bool {
        settings.getInt(doneKey(id), -1) == today.epochDay
    }

    /// يحفظ إتمام القسم اليوم (حين تبلغ كل أذكاره عددها)
    public static func markDone(_ id: String, settings: AppSettings = .shared, today: LocalDate = LocalDate.now()) {
        settings.putInt(doneKey(id), today.epochDay)
    }

    /// تمّت أذكار القسم كلها بالعدد المطلوب؟ [done]: ما عُدّ لكل ذكر
    public static func allDone(_ sec: AdhkarSection, done: [String: Int]) -> Bool {
        sec.items.allSatisfy { (done[$0.id] ?? 0) >= $0.count }
    }

    /// تقدّم القسم: ما عُدّ (بلا زيادة على عدد كل ذكر) من مجموع الأعداد
    public static func progress(_ sec: AdhkarSection, done: [String: Int]) -> (done: Int, total: Int) {
        (sec.items.reduce(0) { $0 + min(done[$1.id] ?? 0, $1.count) }, sec.items.reduce(0) { $0 + $1.count })
    }

    /// لمسة على ذكر: يزيد عدّه حتى يبلغ عدده، ثم يعود صفراً. يعيد العدد الجديد
    public static func tap(_ d: Dhikr, done: inout [String: Int]) -> Int {
        let n = done[d.id] ?? 0
        let v = n < d.count ? n + 1 : 0
        done[d.id] = v
        return v
    }

    /// الترجمة المعروضة تحت الذكر: ترجمته، أو ترجمة معاني آياته إن كان قرآناً.
    /// [quran]: ترجمة المعاني بفهرس الآية العام، و[ayahIndex] من (سورة، آية) إلى ذلك الفهرس (QuranMeta.index)
    public static func translation(_ d: Dhikr, tr: AdhkarTr, lang: String, quran: [String]?, ayahIndex: ((Int, Int) -> Int)? = nil) -> String? {
        if lang == "ar" { return nil }
        if let t = tr.items[d.id]?.t { return t }
        guard !d.ayah.isEmpty, let quran = quran, let idx = ayahIndex else { return nil }
        return d.ayah.map { r in
            (r.from...max(r.from, r.to)).filter { $0 <= r.to }.map { quran[idx(r.surah, $0)] }.joined(separator: " ")
        }.joined(separator: "\n")
    }

    /// "صحيح البخاري 6306 · صحيح مسلم 2702"؛ وللقرآن [quranRef] (اسم السورة ورقم الآية من نصوص الواجهة)
    public static func sourceLine(_ d: Dhikr, tr: AdhkarTr, quranRef: (String) -> String) -> [String] {
        d.src.map { s in
            s.book == "quran" ? quranRef(s.number) : "\(tr.books[s.book] ?? s.book) \(s.number)".trimmingCharacters(in: kotlinTrim)
        }
    }

    /// مرجع آية بصيغة "2:255" أو "2:255-257" كما يعرضه sourceLine في Kotlin:
    /// [surahName] اسم السورة (nil إن لم يُعرف فيُعرض النص كما هو)، و[surahAyah] النص "سورة X، الآية N"
    public static func quranRef(_ n: String, surahName: (Int) -> String?, surahAyah: (String, Int) -> String) -> String {
        let su = Int(n.components(separatedBy: ":").first ?? "")
        guard let s = su, let name = surahName(s) else { return n }
        let after = n.contains(":") ? String(n[n.range(of: ":")!.upperBound...]) : n
        let first = Int(after.components(separatedBy: "-").first ?? "") ?? 1
        let tail = n.contains("-") ? "–" + String(n[n.range(of: "-")!.upperBound...]) : ""
        return surahAyah(name, first) + tail
    }

    /// الدرجة ومن حكم بها: "صحيح · الألباني"
    public static func gradeLine(_ d: Dhikr, tr: AdhkarTr) -> String {
        var parts = [tr.grades[d.grade] ?? d.grade]
        if !d.by.isEmpty { parts.append(tr.by[d.by] ?? d.by) }
        return parts.joined(separator: " · ")
    }

    /// نص المشاركة: الذكر، وترجمته، ومصادره ودرجته
    public static func shareBody(_ d: Dhikr, tr: AdhkarTr, translation t: String?, quranRef: (String) -> String) -> String {
        var s = d.ar
        if let t = t { s += "\n\n" + t }
        s += "\n\n" + sourceLine(d, tr: tr, quranRef: quranRef).joined(separator: " · ")
        s += " (" + gradeLine(d, tr: tr) + ")"
        return s
    }
}

/// ما يقطعه trim() في Kotlin (Char.isWhitespace): مسافات يونيكود ومحارف الفصل 0x1C…0x1F
let kotlinTrim: CharacterSet = {
    var c = CharacterSet.whitespacesAndNewlines
    c.insert(charactersIn: Unicode.Scalar(0x1C)!...Unicode.Scalar(0x1F)!)
    return c
}()
