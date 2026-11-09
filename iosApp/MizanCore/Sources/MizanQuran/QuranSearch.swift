import Foundation

/// البحث في الآيات: بالنص العربي (بلا تشكيل) وبترجمة لغة الواجهة، أو بالإحالة مثل 2:255 (QuranSearch في QuranData.kt)،
/// والبحث في السور كما في فهرس المصحف (QuranScreen.kt).
public enum QuranSearch {
    private static let lock = NSLock()
    private static var normalized: [String: [[UInt8]]] = [:]

    private static func norm(_ key: String, _ load: () throws -> [String]) rethrows -> [[UInt8]] {
        lock.lock()
        if let n = normalized[key] { lock.unlock(); return n }
        lock.unlock()
        let src = try load()
        let n = src.map { Array(Search.normalize($0).utf8) }
        lock.lock(); normalized[key] = n; lock.unlock()
        return n
    }

    /// "2:255" أو "2 255" أو "٢:٢٥٥" ← فهرس الآية
    public static func parseRef(_ q: String) -> Int? {
        let digits = Search.normalize(q).replacingOccurrences(of: " : ", with: ":")
        let c = Array(digits.utf16)
        func isDigit(_ x: UInt16) -> Bool { x >= 48 && x <= 57 }
        func isWs(_ x: UInt16) -> Bool { x == 0x20 || (x >= 0x09 && x <= 0x0D) }
        // ^(\d{1,3})\s*[:.\s]\s*(\d{1,3})$
        var i = 0
        while i < c.count && isDigit(c[i]) { i += 1 }
        let n1 = i
        guard n1 >= 1 && n1 <= 3 else { return nil }
        var marks = 0, sep = 0
        while i < c.count && (isWs(c[i]) || c[i] == 58 || c[i] == 46) {
            if !isWs(c[i]) { marks += 1 }
            sep += 1; i += 1
        }
        guard sep >= 1 && marks <= 1 else { return nil }
        let s2 = i
        while i < c.count && isDigit(c[i]) { i += 1 }
        let n2 = i - s2
        guard i == c.count, n2 >= 1, n2 <= 3 else { return nil }
        let surah = Int(String(decoding: c[0..<n1], as: UTF16.self))!
        let ayah = Int(String(decoding: c[s2..<i], as: UTF16.self))!
        if surah < 1 || surah > QuranMeta.SURAHS || ayah < 1 || ayah > QuranMeta.COUNTS[surah - 1] { return nil }
        return QuranMeta.index(surah, ayah)
    }

    /// الآيات المطابقة لكل كلمات البحث، بالنص العربي إن كُتب بالعربية ثم بترجمة لغة الواجهة [lang].
    /// تمر على ٦٢٣٦ آية، فتُستدعى خارج الخيط الرئيسي.
    public static func ayahs(_ query: String, lang: String, limit: Int = 60) -> [Int] {
        let terms = Search.terms(query).map { Array($0.utf8) }
        if terms.isEmpty || Search.terms(query).reduce(0, { $0 + $1.utf16.count }) < 2 { return [] }
        var out: [Int] = []
        var seen = Set<Int>()
        func add(_ i: Int) { if seen.insert(i).inserted { out.append(i) } }
        let arabicScript = query.utf16.contains { $0 >= 0x0600 && $0 <= 0x06FF }
        if arabicScript, let clean = try? norm(QuranData.CLEAN, { try QuranData.clean() }) {
            for i in clean.indices {
                if terms.allSatisfy({ bytesContains(clean[i], $0) }) { add(i) }
                if out.count >= limit { break }
            }
        }
        if let tr = Sources.translationFor(lang), out.count < limit,
           let text = try? norm("tafsir/\(tr.id)", { try QuranData.source(tr.id) }) {
            for i in text.indices {
                if terms.allSatisfy({ bytesContains(text[i], $0) }) { add(i) }
                if out.count >= limit { break }
            }
        }
        return out
    }

    /// يُضاف إلى اسم السورة في البحث: رقمها وأسماؤها بالعربية وبنطق تنزيل ومعناه وبالإنجليزية
    public static func surahExtra(_ n: Int) -> String {
        "\(n) \(QuranMeta.NAMES_AR[n - 1]) \(QuranMeta.TNAMES[n - 1]) \(QuranMeta.ENAMES[n - 1]) \(QuranMeta.NAMES_EN[n - 1]) \(QuranMeta.NAMES_AR[n - 1])"
    }

    /// السور المطابقة في فهرس المصحف، الأعلى درجة أولاً (12 على الأكثر)؛ [names] أسماء السور بلغة الواجهة
    public static func surahs(_ query: String, names: [String]?, limit: Int = 12) -> [Int] {
        let terms = Search.terms(query)
        if terms.isEmpty { return [] }
        let scored = (1...QuranMeta.SURAHS).map { n in (n, Search.score(terms, QuranMeta.surahName(n, names: names), surahExtra(n))) }
            .filter { $0.1 > 0 }
        // ترتيب ثابت: الأعلى درجة، ثم بترتيب المصحف (sortedByDescending في Kotlin ثابت)
        return Array(scored.sorted { $0.1 != $1.1 ? $0.1 > $1.1 : $0.0 < $1.0 }.map { $0.0 }.prefix(limit))
    }
}
