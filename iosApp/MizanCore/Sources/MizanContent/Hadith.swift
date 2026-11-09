import Foundation
import MizanBase

/// حديث بلغة واحدة كما في موسوعة الأحاديث النبوية (HadeethEnc.com) بلا تعديل — hadith/Hadith.kt
public struct HadithText: Hashable {
    public let title: String
    public let text: String
    public let expl: String
    public let benefits: [String]
    public let grade: String
    public let takhrij: String
    public init(title: String, text: String, expl: String, benefits: [String], grade: String, takhrij: String) {
        self.title = title; self.text = text; self.expl = expl; self.benefits = benefits; self.grade = grade; self.takhrij = takhrij
    }
}

/// حديث اليوم: الأصل العربي، وترجمته بلغة الواجهة (أو أقرب لغة لها ترجمته)، ورابطه في الموسوعة
public struct Hadith: Hashable, Identifiable {
    public let id: String
    public let ar: HadithText
    public let tr: HadithText?
    public let trLang: String?
    public init(id: String, ar: HadithText, tr: HadithText?, trLang: String?) { self.id = id; self.ar = ar; self.tr = tr; self.trLang = trLang }

    /// رابط الحديث في الموسوعة بلغة الترجمة المعروضة
    public var link: String { "https://hadeethenc.com/\(trLang ?? "ar")/browse/hadith/\(id)" }
}

/// أحاديث اليوم: حديث لكل يوم بترتيب ثابت (assets/hadith)، فيتفق التطبيق والأدوات على حديث اليوم.
/// اللغات التي لا ترجمة فيها لحديث تأخذ أقرب لغة: الملايوية الإندونيسية، والكازاخية والأوزبكية الروسية، وغيرها الإنجليزية.
public enum Hadiths {
    /// قارئ الملفات (مسار داخل assets)؛ يُستبدل في الاختبارات
    public static var open: (String) -> Data? = { contentAsset($0) }

    public static let source = "HadeethEnc.com"
    public static let sourceURL = "https://hadeethenc.com"

    private static let lock = NSLock()
    private static var order: [String]?
    private static var files: [String: [String: HadithText]] = [:]

    static let fallback = ["ms": "id", "kk": "ru", "uz": "ru"]

    private static func file(_ code: String) -> [String: HadithText] {
        lock.lock()
        if let f = files[code] { lock.unlock(); return f }
        lock.unlock()
        guard let o = jsonObject(open("hadith/\(code).json")) else {
            lock.lock(); files[code] = [:]; lock.unlock()
            return [:]
        }
        var map: [String: HadithText] = [:]
        for (id, v) in (o["items"] as? [String: Any]) ?? [:] {
            guard let m = v as? [String: Any] else { continue }
            func s(_ k: String) -> String { m[k] as? String ?? "" }
            map[id] = HadithText(title: s("title"), text: s("text"), expl: s("expl"),
                                 benefits: ((m["benefits"] as? [Any]) ?? []).compactMap { $0 as? String }, grade: s("grade"), takhrij: s("takhrij"))
        }
        lock.lock(); defer { lock.unlock() }
        if code == "ar" { order = ((o["order"] as? [Any]) ?? []).compactMap { $0 as? String } }
        if let f = files[code] { return f }
        files[code] = map
        return map
    }

    /// ترتيب الأحاديث (معرّفاتها)
    public static func ids() -> [String] {
        lock.lock()
        if let o = order { lock.unlock(); return o }
        lock.unlock()
        _ = file("ar")
        lock.lock(); defer { lock.unlock() }
        return order ?? []
    }

    /// موضع حديث اليوم في الترتيب: يتتابع يوماً بعد يوم فلا يتكرر حديث قبل أن تمر الأحاديث كلها
    public static func indexFor(_ day: LocalDate) -> Int {
        let n = ids().count
        if n == 0 { return 0 }
        let r = day.epochDay % n
        return r < 0 ? r + n : r
    }

    /// حديث اليوم بلغة [code]؛ nil إن لم تُقرأ البيانات
    public static func forDay(_ day: LocalDate, code: String) -> Hadith? {
        let all = ids()
        let i = indexFor(day)
        return i < all.count ? get(all[i], code: code) : nil
    }

    /// الحديث في الموضع [index] من الترتيب (يدور)
    public static func at(_ index: Int, code: String) -> Hadith? {
        let all = ids()
        if all.isEmpty { return nil }
        let r = index % all.count
        return get(all[r < 0 ? r + all.count : r], code: code)
    }

    public static func get(_ id: String, code: String) -> Hadith? {
        guard let ar = file("ar")[id] else { return nil }
        if code == "ar" { return Hadith(id: id, ar: ar, tr: nil, trLang: nil) }
        var chain: [String] = []
        for c in [code, fallback[code], "en"] { if let c = c, !chain.contains(c) { chain.append(c) } }
        for c in chain { if let t = file(c)[id] { return Hadith(id: id, ar: ar, tr: t, trLang: c) } }
        return Hadith(id: id, ar: ar, tr: nil, trLang: nil)
    }

    /// للاختبارات: يُنسى ما قُرئ
    public static func clearCache() { lock.lock(); order = nil; files = [:]; lock.unlock() }
}
