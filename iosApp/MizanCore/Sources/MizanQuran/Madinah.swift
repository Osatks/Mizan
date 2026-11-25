import Foundation
import MizanBase

/// سطر من صفحة مصحف المدينة كما في الطبعة (MadinahLine في Madinah.kt)
public enum MadinahLine: Equatable {
    case header(surah: Int)
    case basmala
    /// glyphs رموز الكلمات بخط الصفحة، و ayahs فهرس آية كل كلمة، و ends مواضع علامات نهاية الآيات
    case ayah(centered: Bool, glyphs: [String], ayahs: [Int], ends: Set<Int>)
}

/// مصحف المدينة النبوية (طبعة مجمع الملك فهد ١٤٢١هـ، خمسة عشر سطراً): أسطر كل صفحة وكلماتها كما في المطبوع
/// (assets/madinah، يبنيها tools/make_madinah.py)، وتُرسم بخط الصفحة نفسها من خطوط المجمع (QPC V2).
/// تنزيل الخطوط خدمة في التطبيق؛ هنا المسارات والبيانات.
public enum Madinah {
    public static let PAGES = 604
    public static let FONT_URL = "https://static-cdn.tarteel.ai/qul/fonts/quran_fonts/v2/ttf/p%d.ttf"

    /// إطار ترويسة السورة واسمها (خط المجمع)، والبسملة بخط حفص العثماني من المجمع
    public static let HEADER_FONT = "fonts/qcf_surah_header.ttf"
    public static let BASMALA_FONT = "fonts/uthmanic_hafs.ttf"
    public static let BASMALA = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"

    /// قارئ ملفات assets؛ قابل للتغيير في الاختبارات
    public static var open: (String) -> Data? = { Assets.data($0) }

    private static let lock = NSLock()
    private static var pages: [Int: [MadinahLine?]] = [:]
    private static var allCodes: [Int]?

    public static func fontURL(_ page: Int) -> String { FONT_URL.replacingOccurrences(of: "%d", with: String(page)) }
    public static func dir() -> String { QuranData.filesDir + "/madinah" }
    public static func fontPath(_ page: Int) -> String { dir() + "/p\(page).ttf" }
    public static func hasFont(_ page: Int) -> Bool { page >= 1 && page <= PAGES && FileManager.default.fileExists(atPath: fontPath(page)) }
    public static func downloadedCount() -> Int { (1...PAGES).filter { hasFont($0) }.count }

    /// رمز إطار ترويسة السورة واسمها في خط الترويسات (فارغ إن تعذّر)
    public static func headerCode(_ surah: Int) -> String {
        let codes = headerCodes()
        guard surah >= 1, surah <= codes.count, let u = Unicode.Scalar(UInt32(truncatingIfNeeded: codes[surah - 1] & 0xFFFF)) else { return "" }
        return String(Character(u))
    }

    private static func headerCodes() -> [Int] {
        lock.lock(); defer { lock.unlock() }
        if let c = allCodes { return c }
        // الرموز من بيانات الخط (QUL) بترتيب السور: assets/madinah/headers.json
        var list: [Int] = []
        if let d = open("madinah/headers.json"), let a = (try? JSONSerialization.jsonObject(with: d)) as? [Any] {
            list = a.compactMap { jsonInt($0) }
        }
        let c = list.count == 114 ? list : []
        allCodes = c
        return c
    }

    /// أسطر الصفحة الخمسة عشر (nil للسطر الفارغ)
    public static func lines(_ page: Int) -> [MadinahLine?] {
        lock.lock(); defer { lock.unlock() }
        if let p = pages[page] { return p }
        var out: [MadinahLine?] = []
        if let d = open("madinah/p\(page).json"), let o = (try? JSONSerialization.jsonObject(with: d)) as? [String: Any] {
            out = ((o["l"] as? [Any]) ?? []).map { l -> MadinahLine? in
                guard let a = l as? [Any] else { return nil }
                switch a.first as? String {
                case "s": return .header(surah: jsonInt(a[1]) ?? 0)
                case "b": return .basmala
                case "a":
                    return .ayah(
                        centered: jsonInt(a[1]) == 1,
                        glyphs: ((a[2] as? [Any]) ?? []).map { ($0 as? String) ?? "" },
                        ayahs: ((a[3] as? [Any]) ?? []).map { jsonInt($0) ?? 0 },
                        ends: Set(((a[4] as? [Any]) ?? []).map { jsonInt($0) ?? 0 })
                    )
                default: return nil
                }
            }
        }
        pages[page] = out
        return out
    }
}
