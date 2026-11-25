import Foundation
import MizanBase

public enum QuranDataError: Error, Equatable {
    case missing(String)
}

/// نصوص المصحف والتفاسير المضمّنة (ملفات تنزيل كما هي، بصيغة "سورة|آية|نص")، نقلاً عن QuranData.kt.
/// تُقرأ عند أول طلب وتبقى في الذاكرة؛ القراءة متزامنة، فتُستدعى خارج الخيط الرئيسي.
public enum QuranData {
    public static let UTHMANI = "quran/uthmani.txt"
    public static let CLEAN = "quran/clean.txt"

    /// مجلد ملفات التطبيق (Application Support كما في Platform.filesDir)
    public static var filesDir: String = {
        let base = NSSearchPathForDirectoriesInDomains(.applicationSupportDirectory, .userDomainMask, true).first ?? NSTemporaryDirectory()
        try? FileManager.default.createDirectory(atPath: base, withIntermediateDirectories: true)
        return base
    }()

    /// ترجمات معاني الحزم المنزَّلة في files/lang، وما عداها في assets
    public static var open: (String) -> Data? = { path in
        FileManager.default.contents(atPath: filesDir + "/lang/" + path) ?? Assets.data(path)
    }

    private static let lock = NSLock()
    private static var cache: [String: [String]] = [:]
    private static var locks: [String: NSLock] = [:]
    private static var translitMap: [String: String] = [:]

    /// معرّف ترجمة المعاني ← تحويل الخط عند قراءتها (QuranTranslit)
    public static func setTranslit(_ id: String, _ scheme: String?) {
        lock.lock(); defer { lock.unlock() }
        translitMap[id] = scheme
    }

    public static func translit(_ id: String) -> String? {
        lock.lock(); defer { lock.unlock() }
        return translitMap[id]
    }

    public static func cached(_ path: String) -> [String]? {
        lock.lock(); defer { lock.unlock() }
        return cache[path]
    }

    /// يمسح النصوص المقروءة (للاختبارات أو بعد تحديث حزمة)
    public static func clearCache() {
        lock.lock(); defer { lock.unlock() }
        cache.removeAll()
    }

    public static func load(_ path: String) throws -> [String] {
        if let c = cached(path) { return c }
        lock.lock()
        let l = locks[path] ?? NSLock()
        locks[path] = l
        lock.unlock()
        l.lock(); defer { l.unlock() }
        if let c = cached(path) { return c }
        guard let bytes = open(path) else { throw QuranDataError.missing(path) }
        var a = parse(bytes)
        // نص بخط آخر (ترجمة الأوزبكية الكيريلية تُعرض باللاتينية)
        var name = path.split(separator: "/", omittingEmptySubsequences: false).last.map(String.init) ?? path
        if name.hasSuffix(".txt") { name = String(name.dropLast(4)) }
        if let t = translit(name) { for i in a.indices { a[i] = Translit.apply(t, a[i]) } }
        lock.lock(); cache[path] = a; lock.unlock()
        return a
    }

    public static func uthmani() throws -> [String] { try load(UTHMANI) }
    public static func clean() throws -> [String] { try load(CLEAN) }
    public static func source(_ id: String) throws -> [String] { try load("tafsir/\(id).txt") }

    public static func parse(_ data: Data) -> [String] {
        var out = [String?](repeating: nil, count: QuranMeta.AYAHS)
        let bytes = [UInt8](data)
        var start = 0
        let n = bytes.count
        func handle(_ s: Int, _ e: Int) {
            if s >= e || bytes[s] == UInt8(ascii: "#") { return }
            var a = -1, b = -1
            var i = s
            while i < e { if bytes[i] == UInt8(ascii: "|") { a = i; break }; i += 1 }
            if a >= 0 {
                i = a + 1
                while i < e { if bytes[i] == UInt8(ascii: "|") { b = i; break }; i += 1 }
            }
            if a - s <= 0 || b <= a { return }
            guard let surah = ktInt(String(decoding: bytes[s..<a], as: UTF8.self)),
                  let ayah = ktInt(String(decoding: bytes[(a + 1)..<b], as: UTF8.self)) else { return }
            if surah < 1 || surah > QuranMeta.SURAHS || ayah < 1 || ayah > QuranMeta.COUNTS[surah - 1] { return }
            out[QuranMeta.index(surah, ayah)] = ktTrim(String(decoding: bytes[(b + 1)..<e], as: UTF8.self))
        }
        var i = 0
        while i < n {
            let c = bytes[i]
            if c == 0x0A || c == 0x0D { handle(start, i); start = i + 1 }
            i += 1
        }
        handle(start, n)
        return out.map { $0 ?? "" }
    }

    /// في نص تنزيل تبدأ الآية الأولى من كل سورة بالبسملة (عدا الفاتحة، فهي آيتها الأولى، والتوبة بلا بسملة).
    /// تُعرض البسملة زخرفةً مستقلة فوق السورة، وتُعرض الآية بدونها.
    public static func withoutBismillah(_ text: [String], _ index: Int) -> String {
        let (surah, ayah) = QuranMeta.locate(index)
        let t = text[index]
        if ayah != 1 || surah == 1 || surah == 9 { return t }
        // البسملة أربع كلمات؛ قد تختلف علامات تشكيلها (في التين والقدر: بِّسْمِ)، فتُقارن بلا تشكيل
        let words = t.unicodeScalars.split(separator: " ", omittingEmptySubsequences: false)
        if words.count <= 4 { return t }
        let head = words.prefix(4).map { String($0) }.joined(separator: " ")
        if Search.normalize(head).utf8.elementsEqual(Search.normalize(text[0]).utf8) {
            return words.dropFirst(4).map { String($0) }.joined(separator: " ")
        }
        return t
    }

    public static func hasBismillahHeader(_ surah: Int) -> Bool { surah != 1 && surah != 9 }
}

// MARK: - المصادر

public enum SourceKind: String {
    case tafsir = "TAFSIR", translation = "TRANSLATION"
}

/// مصدر تفسير أو ترجمة معانٍ: مدمج (ملف من tanzil.net/trans في assets/tafsir) أو منزَّل من مكتبة التفاسير (remoteSlug)
public struct TafsirSource: Equatable {
    public let id: String
    /// رمز اللغة (ar, en, uz...)
    public let lang: String
    public let kind: SourceKind
    public let author: String
    public let remoteSlug: String?

    public init(id: String, lang: String, kind: SourceKind, author: String, remoteSlug: String? = nil) {
        self.id = id; self.lang = lang; self.kind = kind; self.author = author; self.remoteSlug = remoteSlug
    }
}

/// ما يحتاجه القرآن من حزمة لغة مثبتة (LangPack): ترجمة المعاني وتحويل خطها، واللغات التي تُعرض تفاسيرها بعدها
public struct QuranLangPack: Equatable {
    public let code: String
    public let version: Int
    public let quranId: String?
    public let quranAuthor: String
    public let translit: String?
    public let tafsirFallback: [String]

    public init(code: String, version: Int = 0, quranId: String?, quranAuthor: String = "", translit: String? = nil, tafsirFallback: [String] = []) {
        self.code = code; self.version = version; self.quranId = quranId; self.quranAuthor = quranAuthor
        self.translit = translit; self.tafsirFallback = tafsirFallback
    }

    /// من ملف الحزمة (assets/lang/<code>.json أو files/lang/<code>.json)
    public static func parse(_ data: Data) -> QuranLangPack? {
        guard let o = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any], let code = o["code"] as? String else { return nil }
        let q = o["quran"] as? [String: Any]
        return QuranLangPack(
            code: code, version: jsonInt(o["version"]) ?? 0, quranId: q?["id"] as? String,
            quranAuthor: q?["author"] as? String ?? "", translit: q?["translit"] as? String,
            tafsirFallback: (o["tafsirFallback"] as? [Any])?.compactMap { $0 as? String } ?? []
        )
    }
}

public enum Sources {
    /// لغات الواجهة المكتوبة في التطبيق (Lang.BUILT_IN)
    public static let builtInLangs = ["ar", "en", "fr", "tr", "ur", "id", "ms", "fa", "bn", "ru", "de", "es", "uk"]

    private static let BUILT_IN: [TafsirSource] = [
        TafsirSource(id: "ar.muyassar", lang: "ar", kind: .tafsir, author: "مجمع الملك فهد لطباعة المصحف الشريف"),
        TafsirSource(id: "ar.jalalayn", lang: "ar", kind: .tafsir, author: "جلال الدين المحلي وجلال الدين السيوطي"),
        TafsirSource(id: "id.jalalayn", lang: "id", kind: .tafsir, author: "Tafsir Jalalayn (Bahasa Indonesia)"),
        TafsirSource(id: "en.sahih", lang: "en", kind: .translation, author: "Saheeh International"),
        TafsirSource(id: "fr.hamidullah", lang: "fr", kind: .translation, author: "Muhammad Hamidullah"),
        TafsirSource(id: "tr.diyanet", lang: "tr", kind: .translation, author: "Diyanet İşleri"),
        TafsirSource(id: "ur.jalandhry", lang: "ur", kind: .translation, author: "فتح محمد جالندھری"),
        TafsirSource(id: "id.indonesian", lang: "id", kind: .translation, author: "Kementerian Agama RI"),
        TafsirSource(id: "ms.basmeih", lang: "ms", kind: .translation, author: "Abdullah Muhammad Basmeih"),
        TafsirSource(id: "fa.fooladvand", lang: "fa", kind: .translation, author: "محمدمهدی فولادوند"),
        TafsirSource(id: "bn.bengali", lang: "bn", kind: .translation, author: "মুহিউদ্দীন খান"),
        TafsirSource(id: "ru.kuliev", lang: "ru", kind: .translation, author: "Эльмир Кулиев"),
        TafsirSource(id: "de.bubenheim", lang: "de", kind: .translation, author: "Bubenheim & Elyas"),
        TafsirSource(id: "es.garcia", lang: "es", kind: .translation, author: "Muhammad Isa García"),
        TafsirSource(id: "uk.yakubovych", lang: "uk", kind: .translation, author: "Михайло Якубович"),
    ]

    /// حزم اللغات المثبتة مرتبة برمزها (Lang.packs)
    public private(set) static var packs: [QuranLangPack] = []

    /// التفاسير المنزَّلة من مكتبة التفاسير (TafsirLibrary.downloaded)؛ يضبطها التطبيق
    public static var downloaded: () -> [TafsirSource] = { [] }

    /// يضيف حزم اللغات أو يستبدلها (ولا تحل حزمة محل لغة مدمجة)، ويسجّل تحويل خط ترجماتها (Lang.register و publish)
    public static func register(_ list: [QuranLangPack]) {
        for p in list { if let id = p.quranId, let t = p.translit { QuranData.setTranslit(id, t) } }
        let add = list.filter { p in !builtInLangs.contains(p.code) }
        packs = (packs.filter { l in !add.contains { $0.code == l.code } } + add).sorted { $0.code < $1.code }
    }

    public static func unregister(_ code: String) { packs = packs.filter { $0.code != code } }

    /// الحزم المثبتة: الأحدث إصداراً بين المضمّنة (assets/lang) والمنزَّلة (files/lang)، كما في LanguagePacks.installed
    public static func installedPacks() -> [QuranLangPack] {
        var best: [String: QuranLangPack] = [:]
        var order: [String] = []
        func take(_ p: QuranLangPack) {
            if let b = best[p.code] { if p.version > b.version { best[p.code] = p } } else { best[p.code] = p; order.append(p.code) }
        }
        for n in Assets.list("lang").sorted() where n.hasSuffix(".json") {
            if let d = Assets.data("lang/" + n), let p = QuranLangPack.parse(d) { take(p) }
        }
        let dir = QuranData.filesDir + "/lang"
        for n in ((try? FileManager.default.contentsOfDirectory(atPath: dir)) ?? []).sorted() where n.hasSuffix(".json") {
            if let d = FileManager.default.contents(atPath: dir + "/" + n), let p = QuranLangPack.parse(d) { take(p) }
        }
        return order.compactMap { best[$0] }
    }

    /// يسجّل الحزم المثبتة (عند بدء التطبيق، وبعد تنزيل حزمة)
    public static func loadInstalledPacks() { register(installedPacks()) }

    private static func pack(_ lang: String) -> QuranLangPack? { packs.first { $0.code == lang } }

    /// المصادر المدمجة وترجمات معاني لغات الحزم المثبتة
    public static var ALL: [TafsirSource] {
        BUILT_IN + packs.compactMap { p in p.quranId.map { TafsirSource(id: $0, lang: p.code, kind: .translation, author: p.quranAuthor) } }
    }

    /// المصادر الموجودة على الجهاز: المدمجة في التطبيق، ثم ما نزّله المستخدم من مكتبة التفاسير
    public static func installed() -> [TafsirSource] { ALL + downloaded() }

    public static func byId(_ id: String?) -> TafsirSource? { installed().first { $0.id == id } }

    /// ترجمة المعاني بلغة الواجهة (لا توجد للعربية)
    public static func translationFor(_ lang: String) -> TafsirSource? { ALL.first { $0.lang == lang && $0.kind == .translation } }

    /// المصدر الافتراضي عند فتح التفسير: تفسير بلغة الواجهة إن وُجد (الميسر للعربية)، وإلا ترجمتها، من المصادر الظاهرة
    public static func defaultFor(_ lang: String) -> TafsirSource {
        let v = visible(lang)
        return v.first { $0.lang == lang && $0.kind == .tafsir } ?? v.first { $0.lang == lang } ?? v[0]
    }

    /// مصادر لغة الواجهة أولاً، ثم العربية، ثم بقية اللغات
    public static func ordered(_ lang: String, _ list: [TafsirSource]? = nil) -> [TafsirSource] {
        let list = list ?? installed()
        // لغة الحزمة قد تقترح لغات تُعرض تفاسيرها بعدها (كالروسية للكازاخية)
        let near = (pack(lang)?.tafsirFallback ?? []).filter { $0 != lang && $0 != "ar" }
        return list.filter { $0.lang == lang } + near.flatMap { c in list.filter { $0.lang == c } } +
            list.filter { $0.lang == "ar" && $0.lang != lang } +
            list.filter { $0.lang != lang && $0.lang != "ar" && !near.contains($0.lang) }
    }

    /// ما يظهر في ورقة التفسير: المصادر التي لم يُخفها المستخدم (ويبقى واحد على الأقل)
    public static func visible(_ lang: String) -> [TafsirSource] {
        let v = ordered(lang).filter { !QuranPrefs.isHidden($0.id) }
        return v.isEmpty ? [ALL[0]] : v
    }

    /// تفسير آية من مصدر مدمج إن كان جاهزاً في الذاكرة
    public static func cachedText(_ src: TafsirSource, _ surah: Int, _ ayah: Int) -> String? {
        guard src.remoteSlug == nil else { return nil }
        return QuranData.cached("tafsir/\(src.id).txt")?[QuranMeta.index(surah, ayah)]
    }

    /// تفسير آية من مصدر مدمج (المنزَّلة من المكتبة تقرؤها خدمة المكتبة)
    public static func text(_ src: TafsirSource, _ surah: Int, _ ayah: Int) throws -> String? {
        guard src.remoteSlug == nil else { return nil }
        return try QuranData.source(src.id)[QuranMeta.index(surah, ayah)]
    }
}

func jsonInt(_ v: Any?) -> Int? {
    if let i = v as? Int { return i }
    if let d = v as? Double { return Int(d) }
    if let n = v as? NSNumber { return n.intValue }
    return nil
}

// MARK: - تحويل الخط

/// تحويل الخط لنصوص الحزم (Translit.kt). uz-latn: الأوزبكية من الكيريلية إلى اللاتينية الرسمية (أبجدية 1995).
public enum Translit {
    public static func apply(_ scheme: String?, _ text: String) -> String {
        scheme == "uz-latn" ? uzLatn(text) : text
    }

    private static let VOWELS: Set<UInt16> = Set("аеёиоуэюяўАЕЁИОУЭЮЯЎ".utf16)
    private static let UZ: [UInt16: String] = {
        let m: [Character: String] = [
            "а": "a", "б": "b", "в": "v", "г": "g", "д": "d", "ё": "yo", "ж": "j", "з": "z",
            "и": "i", "й": "y", "к": "k", "л": "l", "м": "m", "н": "n", "о": "o", "п": "p",
            "р": "r", "с": "s", "т": "t", "у": "u", "ф": "f", "х": "x", "ч": "ch", "ш": "sh",
            "щ": "sh", "ъ": "ʼ", "ы": "i", "ь": "", "э": "e", "ю": "yu", "я": "ya",
            "ў": "oʻ", "қ": "q", "ғ": "gʻ", "ҳ": "h",
        ]
        var out: [UInt16: String] = [:]
        for (k, v) in m { out[String(k).utf16.first!] = v }
        return out
    }()

    private static func scalar(_ c: UInt16) -> Unicode.Scalar? { Unicode.Scalar(c) }
    private static func isLetter(_ c: UInt16) -> Bool {
        guard let s = scalar(c) else { return false }
        switch s.properties.generalCategory {
        case .uppercaseLetter, .lowercaseLetter, .titlecaseLetter, .modifierLetter, .otherLetter: return true
        default: return false
        }
    }
    private static func isUpper(_ c: UInt16) -> Bool { scalar(c)?.properties.isUppercase ?? false }
    private static func lower(_ c: UInt16) -> UInt16 {
        guard let s = scalar(c) else { return c }
        let m = s.properties.lowercaseMapping.utf16
        return m.count == 1 ? m.first! : c
    }
    private static func upperFirst(_ s: String) -> String {
        guard let f = s.unicodeScalars.first else { return s }
        let up = f.properties.uppercaseMapping
        let u = up.unicodeScalars.count == 1 ? up : String(f)
        return u + String(s.unicodeScalars.dropFirst())
    }

    public static func uzLatn(_ text: String) -> String {
        let t = Array(text.utf16)
        var b: [UInt16] = []
        b.reserveCapacity(t.count + t.count / 10)
        let ye = Array("ye".utf16), e = Array("e".utf16), ts = Array("ts".utf16), s = Array("s".utf16)
        for i in 0..<t.count {
            let c = t[i]
            let low = lower(c)
            let prev: UInt16? = i > 0 ? t[i - 1] : nil
            let wordStart = prev == nil || !isLetter(prev!)
            var out: [UInt16]?
            switch low {
            // е بعد صائت أو في أول الكلمة: ye؛ وإلا e
            case 0x0435: out = (wordStart || VOWELS.contains(prev!) || prev == 0x044A || prev == 0x042A || prev == 0x044C) ? ye : e
            // ц بين صائتين: ts؛ وإلا s
            case 0x0446: out = (!wordStart && VOWELS.contains(prev!)) ? ts : s
            default: out = UZ[low].map { Array($0.utf16) }
            }
            guard let o = out else { b.append(c); continue }
            if isUpper(c) && !o.isEmpty {
                // كلمة كلها بالأحرف الكبيرة: SH؛ وإلا Sh
                let next: UInt16? = i + 1 < t.count ? t[i + 1] : nil
                let allCaps = (next != nil && isLetter(next!) && isUpper(next!)) || (!wordStart && isUpper(prev!))
                let str = String(decoding: o, as: UTF16.self)
                b.append(contentsOf: (allCaps ? str.uppercased() : upperFirst(str)).utf16)
            } else {
                b.append(contentsOf: o)
            }
        }
        return String(decoding: b, as: UTF16.self)
    }
}
