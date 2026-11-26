import UIKit
import CoreText
import CoreMotion
import CoreImage
import Shared

/// قارئ المصحف من النظام: صفحات مصحف المدينة (طبعة مجمع الملك فهد) يقلّبها UICollectionView ويرسمها Core Text،
/// بدل HorizontalPager المرسوم بـ Compose (Skia). التقليب والانسياب والارتداد من UIKit نفسه بمعدل الشاشة (120 هرتز)،
/// والصفحة طبقة مرسومة مرة واحدة لا تُعاد مع كل إطار من السحب.
///
/// الصفحة نفسها التي يرسمها Compose (MadinahPage.kt و MushafStyle.kt): الأسطر الخمسة عشر كما في المطبوع بخط الصفحة،
/// والورقة والإطار المذهّب ورأس الصفحة ورقمها وشريط العلامة، والتظليل وأشرطة الحفظ. البيانات من الملفات نفسها
/// (assets/madinah، وخطوط الصفحات التي ينزّلها Kotlin)، والحالة من Kotlin (NativeMushaf.ios.kt) مع الإشعار MizanMushafState،
/// واللمس يعود إليه كما من صفحة Compose. ما فوق الصفحات (الرأس، شريط خيارات الآية، الأوراق) يبقى من Compose فوق القارئ.
/// وفي الكتاب المفتوح (iPad بالعرض، iPhone Duo مفتوحاً: «spread» في الحالة) كل موضع تقليب صفحتان متقابلتان،
/// الفردية على اليمين، ومواضع التقليب بمعنى Spreads.index و Spreads.count.
enum NativeMushafReader {
    /// يُستدعى مرة عند التشغيل، قبل إنشاء MainViewController
    static func install() {
        NativeMushaf.shared.attach { session in MushafReaderController(session: session) }
    }
}

// MARK: - الحالة من Kotlin

struct MushafState: Decodable {
    var jump: Int
    var animate: Bool
    var seq: Int
    var selected: Int
    var bottom: Double
    var frame: Double
    var focus: Bool
    var dark: Bool
    var hifz: Bool
    var revealed: [Int]
    var fonts: Int
    var failed: [Int]
    var marked: [Int]
    var read: String
    var paper: String
    var paperEdge: String
    var ink: String
    var muted: String
    var gold: String
    var goldDeep: String
    var fill: String
    var fillDeep: String
    var select: String
    var press: String
    var accent: String
    var backdrop: String
    var downloading: String
    var retry: String
    /// الكتاب المفتوح: كل موضع صفحتان متقابلتان (اليمنى 2i+1 واليسرى 2i+2)، و count عدد المواضع (Spreads.count)
    var spread: Bool?
    var count: Int?
    var pages: Int?
    /// ثلاثي الأبعاد: الكلمات بارزة فوق الورقة بظلها وتتحرك مع ميل الجهاز (MushafDepthMotion)
    var depth: Bool?
    /// لون غباش الكلمات "#AARRGGBB": ظل في الفاتح، ووهج ذهبي في الليلي
    var haze: String?
    /// «انتقل إلى صفحة»: فعل VoiceOver على الصفحة (ولمس رقمها في أسفلها يفتح اختيار الصفحة)
    var goToPage: String?
    /// رحلة الانتقال إلى صفحة اختارها القارئ، مع الانتقال المطلوب نفسه (seq): MushafJourneyPlayer
    var journey: MushafJourney?

    /// عدد صفحات المصحف
    var totalPages: Int { max(pages ?? 604, 1) }
    var isSpread: Bool { spread ?? false }
    /// عدد مواضع التقليب
    var items: Int { max(count ?? (isSpread ? (totalPages + 1) / 2 : totalPages), 1) }

    /// صفحتا الموضع [item]: اليمنى (أو الصفحة الوحيدة) واليسرى إن كانت (كما في Spreads.pages)
    func pagesOf(_ item: Int) -> (right: Int, left: Int?) {
        guard isSpread else { return (item + 1, nil) }
        let r = 2 * item + 1
        return (r, r + 1 <= totalPages ? r + 1 : nil)
    }

    static func decode(_ json: String) -> MushafState? {
        guard let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(MushafState.self, from: data)
    }

    func isRead(_ page: Int) -> Bool {
        guard page >= 1, page <= read.utf8.count else { return false }
        return read.utf8[read.utf8.index(read.utf8.startIndex, offsetBy: page - 1)] == UInt8(ascii: "1")
    }
}

/// ألوان المصحف كما في MushafPalette (MushafStyle.kt)، ولون التطبيق لعلامات الآيات والترويسات
struct MushafColors: Equatable {
    var paper, paperEdge, ink, muted, gold, goldDeep, fill, fillDeep, select, press, accent, backdrop: UIColor

    init(_ s: MushafState) {
        paper = Self.color(s.paper); paperEdge = Self.color(s.paperEdge); ink = Self.color(s.ink); muted = Self.color(s.muted)
        gold = Self.color(s.gold); goldDeep = Self.color(s.goldDeep); fill = Self.color(s.fill); fillDeep = Self.color(s.fillDeep)
        select = Self.color(s.select); press = Self.color(s.press); accent = Self.color(s.accent); backdrop = Self.color(s.backdrop)
    }

    /// "#AARRGGBB"
    static func color(_ hex: String) -> UIColor {
        let v = UInt32(hex.dropFirst(), radix: 16) ?? 0xFF000000
        return UIColor(red: CGFloat((v >> 16) & 0xFF) / 255, green: CGFloat((v >> 8) & 0xFF) / 255,
                       blue: CGFloat(v & 0xFF) / 255, alpha: CGFloat((v >> 24) & 0xFF) / 255)
    }
}

// MARK: - بيانات الصفحات والخطوط

/// سطر من صفحة المصحف كما في الطبعة (assets/madinah/p{N}.json، يبنيه tools/make_madinah.py)
enum MadinahRow {
    case header(Int)
    case basmala
    /// رموز الكلمات بخط الصفحة، وآية كل كلمة (فهرسها في المصحف كله)، ومواضع علامات نهاية الآيات
    case ayah(centered: Bool, glyphs: [String], ayahs: [Int], ends: Set<Int>)
    case empty
}

final class MadinahStore {
    static let shared = MadinahStore()
    static let basmala = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"

    private let root = (Bundle.main.resourcePath ?? "") + "/assets/"
    private let lock = NSLock()
    private var pages: [Int: [MadinahRow]] = [:]
    private var pageFonts: [Int: CGFont] = [:]
    private var fontOrder: [Int] = []

    lazy var headerFont: CGFont? = Self.load(root + "fonts/qcf_surah_header.ttf")
    lazy var basmalaFont: CGFont? = Self.load(root + "fonts/uthmanic_hafs.ttf")
    lazy var amiriFont: CGFont? = Self.load(root + "fonts/amiri_quran.ttf")

    /// رمز إطار ترويسة السورة واسمها في خط الترويسات
    lazy var headerCodes: [Int] = {
        guard let data = FileManager.default.contents(atPath: root + "madinah/headers.json"),
              let list = try? JSONSerialization.jsonObject(with: data) as? [NSNumber], list.count == 114 else { return [] }
        return list.map { $0.intValue }
    }()

    static func load(_ path: String) -> CGFont? {
        guard let provider = CGDataProvider(filename: path) else { return nil }
        return CGFont(provider)
    }

    func headerCode(_ surah: Int) -> String {
        guard surah >= 1, surah <= headerCodes.count, let u = UnicodeScalar(headerCodes[surah - 1]) else { return "" }
        return String(Character(u))
    }

    /// أسطر الصفحة الخمسة عشر
    func rows(_ page: Int) -> [MadinahRow] {
        lock.lock(); defer { lock.unlock() }
        if let r = pages[page] { return r }
        var out: [MadinahRow] = []
        if let data = FileManager.default.contents(atPath: root + "madinah/p\(page).json"),
           let o = try? JSONSerialization.jsonObject(with: data) as? [String: Any], let l = o["l"] as? [Any] {
            out = l.map { (item: Any) -> MadinahRow in
                guard let a = item as? [Any], let kind = a.first as? String else { return .empty }
                switch kind {
                case "s": return .header((a.count > 1 ? a[1] as? NSNumber : nil)?.intValue ?? 1)
                case "b": return .basmala
                case "a":
                    guard a.count >= 5 else { return .empty }
                    let glyphs = (a[2] as? [Any])?.compactMap { $0 as? String } ?? []
                    let ayahs = (a[3] as? [NSNumber])?.map { $0.intValue } ?? []
                    let ends = Set((a[4] as? [NSNumber])?.map { $0.intValue } ?? [])
                    guard !glyphs.isEmpty, ayahs.count == glyphs.count else { return .empty }
                    return .ayah(centered: (a[1] as? NSNumber)?.intValue == 1, glyphs: glyphs, ayahs: ayahs, ends: ends)
                default: return .empty
                }
            }
        }
        pages[page] = out
        return out
    }

    /// خط الصفحة (ينزّله Kotlin إلى Application Support/madinah)؛ تبقى آخر عشرين صفحة في الذاكرة
    func pageFont(_ page: Int, path: String) -> CGFont? {
        lock.lock(); defer { lock.unlock() }
        if let f = pageFonts[page] { return f }
        guard FileManager.default.fileExists(atPath: path), let f = Self.load(path) else { return nil }
        pageFonts[page] = f
        fontOrder.append(page)
        if fontOrder.count > 20 { pageFonts[fontOrder.removeFirst()] = nil }
        return f
    }
}

/// سطر Core Text بخط ومقاس، يأخذ لونه من السياق عند الرسم (fill color)
private func ctLine(_ text: String, _ font: CTFont) -> CTLine {
    let attrs: [NSAttributedString.Key: Any] = [
        NSAttributedString.Key(kCTFontAttributeName as String): font,
        NSAttributedString.Key(kCTForegroundColorFromContextAttributeName as String): true,
    ]
    return CTLineCreateWithAttributedString(NSAttributedString(string: text, attributes: attrs))
}

private func lineWidth(_ line: CTLine) -> CGFloat { CGFloat(CTLineGetTypographicBounds(line, nil, nil, nil)) }

/// يرسم سطراً Core Text خط أساسه عند (x, y) في سياق UIKit (المحور الرأسي إلى الأسفل): يُقلب السياق كله حول خط الأساس
/// (لا مصفوفة النص وحدها)، فتبقى مواضع الحركات ووصل الحروف إلى الأعلى كما يضعها الخط
private func paint(_ line: CTLine, _ ctx: CGContext, x: CGFloat, y: CGFloat, color: UIColor) {
    ctx.saveGState()
    ctx.setFillColor(color.cgColor)
    ctx.translateBy(x: x, y: y)
    ctx.scaleBy(x: 1, y: -1)
    ctx.textMatrix = .identity
    ctx.textPosition = .zero
    CTLineDraw(line, ctx)
    ctx.restoreGState()
}

// MARK: - حساب الصفحة (كما في prepareMadinah، MadinahPage.kt)

struct PlacedGlyph {
    /// بمقاس الصفحة نفسه: Core Text لا يكبّر مواضع الحروف داخل السطر مع مصفوفة النص، فالكلمة ذات الرموز
    /// المتعددة والبسملة (وصل الحروف ووضع الحركات) تُبنى بمقاسها لا بمقاس 100 مكبَّراً
    let line: CTLine
    let left: CGFloat
    let right: CGFloat
    let ayah: Int
    let end: Bool
    /// إزاحة رسم الكلمة عن طرفها الأيسر (الكلمة التي يقلّص التشكيل عرضها)
    var dx: CGFloat = 0
}

enum LaidRow {
    case text(top: CGFloat, height: CGFloat, words: [PlacedGlyph])
    case banner(top: CGFloat, height: CGFloat, surah: Int)
    case basmala(top: CGFloat, height: CGFloat)
}

final class MadinahLayout {
    static let ref: CGFloat = 100
    let page: Int
    let size: CGSize
    let textSize: CGFloat
    let lineH: CGFloat
    let rows: [LaidRow]
    /// صفحتا الفاتحة وأول البقرة: أسطر قليلة في وسط إطار مزخرف
    let capped: Bool
    let font: CGFont
    /// عرض أسطر الصفحة (للترويسة والبسملة)
    let lineW: CGFloat

    private init(page: Int, size: CGSize, textSize: CGFloat, lineH: CGFloat, rows: [LaidRow], capped: Bool, font: CGFont) {
        self.page = page; self.size = size; self.textSize = textSize; self.lineH = lineH; self.rows = rows
        self.capped = capped; self.font = font
        var left = CGFloat.greatestFiniteMagnitude, right: CGFloat = 0
        for r in rows { if case .text(_, _, let ws) = r { for w in ws { left = min(left, w.left); right = max(right, w.right) } } }
        lineW = max(right > left ? right - left : size.width, size.width * 0.6)
    }

    static func make(page: Int, size: CGSize, font: CGFont) -> MadinahLayout? {
        let lines = MadinahStore.shared.rows(page)
        let w = size.width, h = size.height
        guard !lines.isEmpty, w > 0, h > 0 else { return nil }
        let f100 = CTFontCreateWithGraphicsFont(font, ref, nil, nil)
        // عرض الكلمة من رموزها كلاً على حدة إن كان التشكيل يقلّصه: في بعض خطوط المجمع يُعطى رمز الحركة (بلا عرض)
        // عرضاً سالباً، فتنسحب الكلمة كلها نحو جارتها وتلتصق بها («طسٓ» بـ«تِلۡكَ» في أول النمل)؛ والفرق إزاحة رسمها
        var shifts: [String: CGFloat] = [:]
        var measured: [Int: [(CTLine, CGFloat)]] = [:]
        for (i, l) in lines.enumerated() {
            if case .ayah(_, let glyphs, _, _) = l {
                measured[i] = glyphs.map { (g: String) -> (CTLine, CGFloat) in
                    let line = ctLine(g, f100)
                    let whole = lineWidth(line)
                    guard g.unicodeScalars.count > 1 else { return (line, whole) }
                    let parts = g.unicodeScalars.reduce(CGFloat(0)) { $0 + lineWidth(ctLine(String($1), f100)) }
                    if parts > whole + 0.5 { shifts[g] = parts - whole; return (line, parts) }
                    return (line, whole)
                }
            }
        }
        // فراغ صغير ثابت بين الكلمات (٠٫٠٦ من المقاس): كلمات خطوط المجمع لا فراغ بينها إلا هوامش الحروف، وأسطر الصفحة
        // متقاربة العرض، فلو مُلئ أطول سطر بلا فراغ لتقاربت الكلمات في الأسطر كلها حتى تكاد تلتصق
        let space = ref * 0.06
        func natural(_ i: Int) -> CGFloat {
            guard let m = measured[i] else { return 0 }
            return m.reduce(0) { $0 + $1.1 } + space * CGFloat(max(m.count - 1, 0))
        }
        let special = page <= 2
        let full = lines.indices.filter { i in
            if case .ayah(let c, _, _, _) = lines[i] { return !c || special }
            return false
        }
        guard let widest = full.map(natural).max() ?? measured.keys.map(natural).max(), widest > 0 else { return nil }
        let slots: CGFloat = special ? 11 : 15
        let lineH = h / slots
        let target = special ? w * 0.80 : w
        // المقاس من أطول سطر بعرض الصفحة، ولا يتجاوز ما يسعه ارتفاع السطر (الحروف بين -0.6 و 1.2 من المقاس)
        let textSize = min(ref * target / widest, lineH / 1.62)
        let lineW = widest * textSize / ref
        let x0 = (w - lineW) / 2
        let k = textSize / ref
        let fs = CTFontCreateWithGraphicsFont(font, textSize, nil, nil)
        let used: [Int] = special ? lines.indices.filter { if case .empty = lines[$0] { return false }; return true } : Array(lines.indices)
        let top0 = special ? (h - CGFloat(used.count) * lineH) / 2 : 0
        var rows: [LaidRow] = []
        for (slot, li) in used.enumerated() {
            let top = top0 + CGFloat(slot) * lineH
            switch lines[li] {
            case .header(let s): rows.append(.banner(top: top, height: lineH, surah: s))
            case .basmala: rows.append(.basmala(top: top, height: lineH))
            case .ayah(let c, let glyphs, let ayahs, let ends):
                guard let ws = measured[li] else { continue }
                let inked = ws.reduce(0) { $0 + $1.1 } * k
                let centered = c || special || glyphs.count == 1
                // السطر الموسَّط بالفراغ الثابت، والكامل يملأ العرض (ولا يقل فراغه عنه)
                let gap = centered ? space * k : (lineW - inked) / CGFloat(glyphs.count - 1)
                let total = inked + (centered ? gap * CGFloat(max(glyphs.count - 1, 0)) : 0)
                var right = centered ? x0 + (lineW + total) / 2 : x0 + lineW
                var placed: [PlacedGlyph] = []
                placed.reserveCapacity(glyphs.count)
                for j in glyphs.indices {
                    let ww = ws[j].1 * k
                    placed.append(PlacedGlyph(line: ctLine(glyphs[j], fs), left: right - ww, right: right, ayah: ayahs[j],
                                              end: ends.contains(j), dx: (shifts[glyphs[j]] ?? 0) * k))
                    right -= ww + gap
                }
                rows.append(.text(top: top, height: lineH, words: placed))
            case .empty: break
            }
        }
        return MadinahLayout(page: page, size: size, textSize: textSize, lineH: lineH, rows: rows, capped: special, font: font)
    }

    /// الآية تحت نقطة اللمس، أو -1 (كما في PageLayout.hit)
    func hit(_ p: CGPoint) -> Int {
        var best: (top: CGFloat, height: CGFloat, words: [PlacedGlyph])?
        var bestD = CGFloat.greatestFiniteMagnitude
        for r in rows {
            guard case .text(let top, let height, let words) = r else { continue }
            let d = p.y < top ? top - p.y : (p.y > top + height ? p.y - top - height : 0)
            if d < bestD { bestD = d; best = (top, height, words) }
        }
        guard let row = best, p.y >= row.top - row.height / 2, p.y <= row.top + row.height * 1.5 else { return -1 }
        var ayah = -1, bestX = CGFloat.greatestFiniteMagnitude
        for w in row.words {
            let d = (p.x >= w.left && p.x <= w.right) ? 0 : min(abs(p.x - w.left), abs(p.x - w.right))
            if d < bestX { bestX = d; ayah = w.ayah }
        }
        return ayah
    }

    /// أشرطة تظليل الآية: شريط في كل سطر فيه شيء منها
    func band(_ ayah: Int) -> CGPath? {
        guard ayah >= 0 else { return nil }
        let path = CGMutablePath()
        for r in rows {
            guard case .text(let top, let height, let words) = r else { continue }
            var x0 = CGFloat.greatestFiniteMagnitude, x1: CGFloat = -1
            for w in words where w.ayah == ayah { x0 = min(x0, w.left); x1 = max(x1, w.right) }
            if x1 < 0 { continue }
            let rect = CGRect(x: x0 - 4, y: top + height * 0.06, width: x1 - x0 + 8, height: height * 0.88)
            guard rect.width > 0, rect.height > 0, rect.width.isFinite, rect.height.isFinite else { continue }
            // نصف القطر لا يتجاوز نصف ضلع الشريط: Core Graphics يتوقف (assert) إن زاد ضعف الزاوية على الضلع
            // (أسطر قصيرة في iPhone SE مع مشغّل التلاوة، أو شريط علامة آية وحدها)
            let r = max(0, min(10, rect.width / 2, rect.height / 2) - 0.01)
            path.addRoundedRect(in: rect, cornerWidth: r, cornerHeight: r)
        }
        return path.isEmpty ? nil : path
    }
}

/// الصفحات المحسوبة لمقاس معين (آخر ستة عشر)، وحساب الصفحات المجاورة في الخلفية قبل أن تظهر
final class LayoutCache {
    static let shared = LayoutCache()
    private let lock = NSLock()
    private var items: [String: MadinahLayout] = [:]
    private var order: [String] = []
    private let queue = DispatchQueue(label: "mizan.mushaf.layout", qos: .userInitiated)

    private func key(_ page: Int, _ size: CGSize) -> String { "\(page)@\(Int(size.width * 2))x\(Int(size.height * 2))" }

    func peek(_ page: Int, _ size: CGSize) -> MadinahLayout? {
        lock.lock(); defer { lock.unlock() }
        return items[key(page, size)]
    }

    func obtain(_ page: Int, _ size: CGSize, fontPath: String) -> MadinahLayout? {
        if let l = peek(page, size) { return l }
        guard let font = MadinahStore.shared.pageFont(page, path: fontPath),
              let l = MadinahLayout.make(page: page, size: size, font: font) else { return nil }
        lock.lock()
        let k = key(page, size)
        if items[k] == nil { order.append(k) }
        items[k] = l
        if order.count > 16 { items[order.removeFirst()] = nil }
        lock.unlock()
        return l
    }

    func prefetch(_ page: Int, _ size: CGSize, fontPath: String) {
        guard peek(page, size) == nil else { return }
        queue.async { _ = self.obtain(page, size, fontPath: fontPath) }
    }

    /// يحسب الصفحة في الخلفية ثم يستدعي [done] في الخيط الرئيس (بعد أن يستقر تغيّر المقاس)
    func build(_ page: Int, _ size: CGSize, fontPath: String, done: @escaping () -> Void) {
        queue.async {
            _ = self.obtain(page, size, fontPath: fontPath)
            DispatchQueue.main.async(execute: done)
        }
    }
}

// MARK: - الرسم

/// موضع الصفحة في الكتاب المفتوح (PageSide في Spreads.kt)
enum MushafSide { case single, right, left }

/// شكل الورقة: في الكتاب المفتوح الزاويتان عند الكعب أصغر (كما في MushafPage)؛ أنصاف الأقطار لا تتجاوز نصف الضلع
private func mushafPageShape(_ r: CGRect, side: MushafSide) -> CGPath {
    let big = Geo.corner, small = Geo.spineCorner
    let m = max(0, min(r.width, r.height) / 2)
    let tl = min(side == .right ? small : big, m), bl = tl
    let tr = min(side == .left ? small : big, m), br = tr
    let p = CGMutablePath()
    p.move(to: CGPoint(x: r.minX + tl, y: r.minY))
    p.addArc(tangent1End: CGPoint(x: r.maxX, y: r.minY), tangent2End: CGPoint(x: r.maxX, y: r.maxY), radius: tr)
    p.addArc(tangent1End: CGPoint(x: r.maxX, y: r.maxY), tangent2End: CGPoint(x: r.minX, y: r.maxY), radius: br)
    p.addArc(tangent1End: CGPoint(x: r.minX, y: r.maxY), tangent2End: CGPoint(x: r.minX, y: r.minY), radius: bl)
    p.addArc(tangent1End: CGPoint(x: r.minX, y: r.minY), tangent2End: CGPoint(x: r.maxX, y: r.minY), radius: tl)
    p.closeSubpath()
    return p
}

/// نجمة ثمانية من مربعين متراكبين (drawStar8 في Emblem.kt)
private func star8(_ ctx: CGContext, center c: CGPoint, radius r: CGFloat, color: UIColor, stroke: CGFloat, rings: Bool) {
    let path = CGMutablePath()
    for sq in 0..<2 {
        for i in 0..<4 {
            let a = (Double(sq) * 45 + Double(i) * 90 - 90) * .pi / 180
            let p = CGPoint(x: c.x + r * CGFloat(cos(a)), y: c.y + r * CGFloat(sin(a)))
            if i == 0 { path.move(to: p) } else { path.addLine(to: p) }
        }
        path.closeSubpath()
    }
    ctx.saveGState()
    ctx.setStrokeColor(color.cgColor)
    ctx.setLineWidth(stroke)
    ctx.setLineJoin(.miter)
    ctx.addPath(path)
    ctx.strokePath()
    if rings {
        ctx.setLineWidth(stroke * 0.8)
        for f: CGFloat in [0.52, 0.18] {
            ctx.strokeEllipse(in: CGRect(x: c.x - r * f, y: c.y - r * f, width: 2 * r * f, height: 2 * r * f))
        }
    }
    ctx.restoreGState()
}

private func roundRect(_ ctx: CGContext, _ rect: CGRect, radius: CGFloat, fill: UIColor? = nil, stroke: UIColor? = nil, width: CGFloat = 1) {
    let path = UIBezierPath(roundedRect: rect, cornerRadius: radius).cgPath
    if let f = fill { ctx.setFillColor(f.cgColor); ctx.addPath(path); ctx.fillPath() }
    if let s = stroke { ctx.setStrokeColor(s.cgColor); ctx.setLineWidth(width); ctx.addPath(path); ctx.strokePath() }
}

private func circle(_ ctx: CGContext, _ c: CGPoint, _ r: CGFloat, fill: UIColor? = nil, stroke: UIColor? = nil, width: CGFloat = 1) {
    let rect = CGRect(x: c.x - r, y: c.y - r, width: 2 * r, height: 2 * r)
    if let f = fill { ctx.setFillColor(f.cgColor); ctx.fillEllipse(in: rect) }
    if let s = stroke { ctx.setStrokeColor(s.cgColor); ctx.setLineWidth(width); ctx.strokeEllipse(in: rect) }
}

/// أبعاد الصفحة كما في MushafPage (SurahScreen.kt) و MushafStyle.kt
private enum Geo {
    static let padSide: CGFloat = 12, padTop: CGFloat = 10, padBottom: CGFloat = 8
    static let colSide: CGFloat = 20, colTop: CGFloat = 17
    static let frameFoot: CGFloat = 13
    static let medallionCenter: CGFloat = 9 + frameFoot
    static let medallionSize: CGFloat = 32
    static var colBottom: CGFloat { medallionCenter + medallionSize / 2 + 2 }
    static let headH: CGFloat = 22, ruleH: CGFloat = 10
    static let canvasTop: CGFloat = 8, canvasBottom: CGFloat = 2
    static let corner: CGFloat = 18
    /// في الكتاب المفتوح: زاويتا الكعب (بين الصفحتين) والهامش عنده
    static let spineCorner: CGFloat = 6, spineGap: CGFloat = 3
    /// عرض طرف الصفحة الذي يقلبها لمسُه في وضع التركيز
    static let edge: CGFloat = 0.14
    /// نصف قطر منطقة لمس رقم الصفحة: أوسع قليلاً من الميدالية
    static let medallionTouch: CGFloat = 22
}

/// حركة الجهاز للوضع ثلاثي الأبعاد: اتجاه الجاذبية بمحوري الشاشة كما يراها القارئ، والفرق بينه وبين وضع مرجعي
/// يلحق بطريقة الإمساك ببطء (نحو ثانيتين)، فتعود الكلمات إلى مكانها حين يستقر الجهاز على أي زاوية.
/// تعمل ما دامت صفحة ثلاثية الأبعاد ظاهرة فقط، وتتوقف مع «تقليل الحركة»
final class MushafDepthMotion {
    static let shared = MushafDepthMotion()
    private let manager = CMMotionManager()
    private let views = NSHashTable<MushafPageView>.weakObjects()
    private var ref: (x: Double, y: Double)?
    private var smooth: (x: Double, y: Double) = (x: 0, y: 0)
    /// الميل الحالي، لكل محور بين -1 و 1 (المحور الرأسي إلى الأسفل كما في UIKit)
    private(set) var tilt: CGPoint = .zero

    func attach(_ v: MushafPageView) {
        views.add(v)
        start()
    }

    func detach(_ v: MushafPageView) {
        views.remove(v)
        if views.allObjects.isEmpty { stop() }
    }

    private func start() {
        guard !manager.isDeviceMotionActive, manager.isDeviceMotionAvailable, !UIAccessibility.isReduceMotionEnabled else { return }
        ref = nil
        smooth = (x: 0, y: 0)
        manager.deviceMotionUpdateInterval = 1.0 / 60
        manager.startDeviceMotionUpdates(to: .main) { [weak self] motion, _ in
            guard let self = self, let g = motion?.gravity else { return }
            self.update(g)
        }
    }

    private func stop() {
        if manager.isDeviceMotionActive { manager.stopDeviceMotionUpdates() }
        tilt = .zero
    }

    private func update(_ g: CMAcceleration) {
        // محورا الشاشة (x إلى اليمين، y إلى الأعلى) حسب اتجاه الواجهة
        var orientation = UIInterfaceOrientation.portrait
        if #available(iOS 13.0, *), let o = views.allObjects.first?.window?.windowScene?.interfaceOrientation { orientation = o }
        let gx: Double, gy: Double
        switch orientation {
        case .landscapeRight: gx = -g.y; gy = g.x
        case .landscapeLeft: gx = g.y; gy = -g.x
        case .portraitUpsideDown: gx = -g.x; gy = -g.y
        default: gx = g.x; gy = g.y
        }
        var r: (x: Double, y: Double) = ref ?? (x: gx, y: gy)
        r.x += (gx - r.x) * 0.012
        r.y += (gy - r.y) * 0.012
        ref = r
        smooth.x += ((gx - r.x) - smooth.x) * 0.18
        smooth.y += ((gy - r.y) - smooth.y) * 0.18
        // ميل نحو 15° يبلغ أقصى الحركة؛ الكلمات تنزاح نحو الطرف المائل إلى الأسفل
        let x = CGFloat(max(-1, min(1, smooth.x / 0.26)))
        let y = CGFloat(max(-1, min(1, -smooth.y / 0.26)))
        guard abs(x - tilt.x) > 0.004 || abs(y - tilt.y) > 0.004 else { return }
        tilt = CGPoint(x: x, y: y)
        for v in views.allObjects { v.applyTilt(tilt) }
    }
}

/// ورقة الصفحة وإطارها ورأسها ورقمها وشريط العلامة: تُرسم مرة لكل صفحة (لا مع التقليب)
final class PageChromeView: UIView {
    var page = 1
    var colors: MushafColors?
    var title = "", subtitle = "", digits = ""
    var read = false
    var marked = false
    var side = MushafSide.single

    override init(frame: CGRect) {
        super.init(frame: frame)
        isOpaque = true
        contentMode = .redraw
        isUserInteractionEnabled = false
    }
    required init?(coder: NSCoder) { fatalError() }

    override func draw(_ rect: CGRect) {
        guard let ctx = UIGraphicsGetCurrentContext(), let p = colors else { return }
        let w = bounds.width, h = bounds.height
        // الورقة: تدرّج خفيف من الوسط إلى الحواف
        let space = CGColorSpaceCreateDeviceRGB()
        if let g = CGGradient(colorsSpace: space, colors: [p.paper.cgColor, p.paper.cgColor, p.paperEdge.cgColor] as CFArray, locations: [0, 0.5, 1]) {
            ctx.drawRadialGradient(g, startCenter: CGPoint(x: w / 2, y: h / 2), startRadius: 0,
                                   endCenter: CGPoint(x: w / 2, y: h / 2), endRadius: max(w, h) * 0.75, options: [.drawsAfterEndLocation])
        }
        // في الكتاب المفتوح: ظل خفيف عند الكعب (drawSpine في MushafStyle.kt)
        if side != .single {
            let sw = min(26, w / 4), innerLeft = side == .right
            let shade = UIColor(red: 0, green: 0, blue: 0, alpha: 0.09).cgColor, clear = UIColor(red: 0, green: 0, blue: 0, alpha: 0).cgColor
            let x0 = innerLeft ? 0 : w - sw
            if let g = CGGradient(colorsSpace: space, colors: (innerLeft ? [shade, clear] : [clear, shade]) as CFArray, locations: [0, 1]) {
                ctx.saveGState()
                ctx.clip(to: CGRect(x: x0, y: 0, width: sw, height: h))
                ctx.drawLinearGradient(g, start: CGPoint(x: x0, y: 0), end: CGPoint(x: x0 + sw, y: 0), options: [])
                ctx.restoreGState()
            }
        }
        // الإطار: خط ذهبي عريض وآخر رفيع بداخله، وفي زوايا الداخلي نجوم ثمانية
        let o: CGFloat = 7, i: CGFloat = 11, foot = Geo.frameFoot
        roundRect(ctx, CGRect(x: o, y: o, width: w - 2 * o, height: h - 2 * o - foot), radius: 12, stroke: p.goldDeep.withAlphaComponent(0.8), width: 1.6)
        roundRect(ctx, CGRect(x: i, y: i, width: w - 2 * i, height: h - 2 * i - foot), radius: 8, stroke: p.gold.withAlphaComponent(0.7), width: 0.8)
        let r: CGFloat = 5, c = i + 1, bottom = h - foot - c
        for pt in [CGPoint(x: c, y: c), CGPoint(x: w - c, y: c), CGPoint(x: c, y: bottom), CGPoint(x: w - c, y: bottom)] {
            circle(ctx, pt, r * 0.9, fill: p.paper)
            star8(ctx, center: pt, radius: r, color: p.goldDeep, stroke: 1, rings: false)
            circle(ctx, pt, r * 0.22, fill: p.goldDeep)
        }
        // شريط العلامة إن كان في الصفحة آية محفوظة
        if marked {
            let rw: CGFloat = 14, rh: CGFloat = 40, notch: CGFloat = 6, x = w / 2 - rw / 2
            let ribbon = CGMutablePath()
            ribbon.move(to: CGPoint(x: x, y: 0)); ribbon.addLine(to: CGPoint(x: x + rw, y: 0)); ribbon.addLine(to: CGPoint(x: x + rw, y: rh))
            ribbon.addLine(to: CGPoint(x: x + rw / 2, y: rh - notch)); ribbon.addLine(to: CGPoint(x: x, y: rh)); ribbon.closeSubpath()
            ctx.saveGState()
            ctx.addPath(ribbon); ctx.clip()
            if let g = CGGradient(colorsSpace: space, colors: [p.goldDeep.cgColor, p.gold.cgColor] as CFArray, locations: [0, 1]) {
                ctx.drawLinearGradient(g, start: CGPoint(x: 0, y: 0), end: CGPoint(x: 0, y: rh), options: [])
            }
            ctx.restoreGState()
            ctx.setStrokeColor(p.paper.withAlphaComponent(0.45).cgColor); ctx.setLineWidth(0.8)
            ctx.move(to: CGPoint(x: x + rw * 0.28, y: 0)); ctx.addLine(to: CGPoint(x: x + rw * 0.28, y: rh - notch * 1.4)); ctx.strokePath()
        }
        // رأس الصفحة: السورة في اليمين، والجزء والحزب في اليسار، وتحتهما خط فاصل في وسطه معيّن
        let x0 = Geo.colSide, x1 = w - Geo.colSide, top = Geo.colTop
        if let amiri = MadinahStore.shared.amiriFont {
            let sub = ctLine(subtitle, CTFontCreateWithGraphicsFont(amiri, 13, nil, nil))
            let subW = lineWidth(sub)
            paint(sub, ctx, x: x0, y: baseline(top, CTFontCreateWithGraphicsFont(amiri, 13, nil, nil)), color: p.muted)
            let titleFont = CTFontCreateWithGraphicsFont(amiri, 15, nil, nil)
            var head = ctLine(title, titleFont)
            let room = x1 - x0 - subW - 10
            if lineWidth(head) > room, room > 20, let t = CTLineCreateTruncatedLine(head, Double(room), .end, ctLine("…", titleFont)) { head = t }
            paint(head, ctx, x: x1 - lineWidth(head), y: baseline(top, titleFont), color: p.goldDeep)
        }
        let ry = top + Geo.headH + Geo.ruleH / 2, cx = w / 2, d: CGFloat = 3.5, gap: CGFloat = 7
        ctx.setStrokeColor(p.gold.withAlphaComponent(0.7).cgColor); ctx.setLineWidth(0.8)
        ctx.move(to: CGPoint(x: x0, y: ry)); ctx.addLine(to: CGPoint(x: cx - gap, y: ry))
        ctx.move(to: CGPoint(x: cx + gap, y: ry)); ctx.addLine(to: CGPoint(x: x1, y: ry)); ctx.strokePath()
        ctx.setFillColor(p.goldDeep.cgColor)
        ctx.move(to: CGPoint(x: cx, y: ry - d)); ctx.addLine(to: CGPoint(x: cx + d, y: ry)); ctx.addLine(to: CGPoint(x: cx, y: ry + d))
        ctx.addLine(to: CGPoint(x: cx - d, y: ry)); ctx.closePath(); ctx.fillPath()
        // رقم الصفحة في ميدالية على خطَّي الإطار السفليين، تمتلئ بالذهب حين تُحسب الصفحة مقروءة
        let mc = CGPoint(x: w / 2, y: h - Geo.medallionCenter), mr = Geo.medallionSize / 2 - 1
        circle(ctx, mc, mr, fill: p.paper)
        if read { circle(ctx, mc, mr, fill: p.fillDeep) }
        circle(ctx, mc, mr, stroke: p.goldDeep, width: 1.3)
        star8(ctx, center: mc, radius: mr * 0.86, color: p.gold.withAlphaComponent(read ? 0.85 : 0.4), stroke: 0.9, rings: false)
        if let amiri = MadinahStore.shared.amiriFont {
            let num = ctLine(digits, CTFontCreateWithGraphicsFont(amiri, page >= 100 ? 12.5 : 14, nil, nil))
            let b = CTLineGetBoundsWithOptions(num, .useGlyphPathBounds)
            // حدود الحروف الفعلية في وسط الدائرة (المحور الرأسي في Core Text إلى الأعلى)
            paint(num, ctx, x: mc.x - b.midX, y: mc.y + b.midY, color: p.ink)
        }
    }

    /// خط أساس نص الرأس في سطر ارتفاعه 22 (الحروف في وسطه)
    private func baseline(_ top: CGFloat, _ font: CTFont) -> CGFloat {
        let a = CTFontGetAscent(font), d = CTFontGetDescent(font)
        return top + (Geo.headH - (a + d)) / 2 + a
    }
}

/// أسطر الصفحة بخطها: طبقة تُرسم مرة (وتُعاد عند تغيّر الحفظ أو الألوان فقط)
final class PageGlyphView: UIView {
    var layout: MadinahLayout?
    var colors: MushafColors?
    var hifz = false
    var revealed: Set<Int> = []
    /// لون واحد لكل ما يُرسم: نسخة ظل الكلمات على الورقة في الوضع ثلاثي الأبعاد (بلا إطار الفاتحة ولا أشرطة الحفظ)
    var tint: UIColor?

    override init(frame: CGRect) {
        super.init(frame: frame)
        isOpaque = false
        backgroundColor = .clear
        contentMode = .redraw
        isUserInteractionEnabled = false
        layer.drawsAsynchronously = true
    }
    required init?(coder: NSCoder) { fatalError() }

    override func draw(_ rect: CGRect) {
        guard let ctx = UIGraphicsGetCurrentContext(), let l = layout, let p = colors else { return }
        let w = bounds.width
        let ink = tint ?? p.ink, accent = tint ?? p.accent
        if tint == nil, l.capped, let first = l.rows.first, let last = l.rows.last {
            drawCappedFrame(ctx, l, first: first, last: last, p)
        }
        for row in l.rows {
            switch row {
            case .text(let top, let height, let words):
                let y = top + height / 2 + l.textSize * 0.3
                for g in words {
                    if hifz && !g.end && !revealed.contains(g.ayah) {
                        if tint != nil { continue }
                        // الكلمة المخفية للحفظ: شريط لين بطولها
                        let inset = l.textSize * 0.04, bh = l.textSize * 0.62
                        roundRect(ctx, CGRect(x: g.left + inset, y: y - l.textSize * 0.52, width: max(g.right - g.left - 2 * inset, 1), height: bh),
                                  radius: bh / 2, fill: p.ink.withAlphaComponent(0.16))
                    } else {
                        paint(g.line, ctx, x: g.left + g.dx, y: y, color: g.end ? accent : ink)
                    }
                }
            case .banner(let top, let height, let surah):
                guard let hf = MadinahStore.shared.headerFont else { continue }
                let code = MadinahStore.shared.headerCode(surah)
                if code.isEmpty { continue }
                let gw = max(lineWidth(ctLine(code, CTFontCreateWithGraphicsFont(hf, MadinahLayout.ref, nil, nil))), 1)
                // الإطار ممتد إلى طرفي الصفحة (وفي الفاتحة وأول البقرة بعرض أسطرهما)، ولا يتجاوز ارتفاع السطر إلا قليلاً
                let ts = min(MadinahLayout.ref * (l.capped ? l.lineW : w) / gw, height * 1.12 / 0.39)
                let line = ctLine(code, CTFontCreateWithGraphicsFont(hf, ts, nil, nil))
                paint(line, ctx, x: (w - lineWidth(line)) / 2, y: top + height / 2, color: accent)
            case .basmala(let top, let height):
                guard let bf = MadinahStore.shared.basmalaFont else { continue }
                let bw = lineWidth(ctLine(MadinahStore.basmala, CTFontCreateWithGraphicsFont(bf, MadinahLayout.ref, nil, nil)))
                var size = l.textSize * 1.02
                if bw * size / MadinahLayout.ref > l.lineW * 0.75 { size = l.lineW * 0.75 * MadinahLayout.ref / bw }
                let line = ctLine(MadinahStore.basmala, CTFontCreateWithGraphicsFont(bf, size, nil, nil))
                paint(line, ctx, x: (w - lineWidth(line)) / 2, y: top + height / 2 + size * 0.32, color: ink)
            }
        }
    }

    /// صفحتا الفاتحة وأول البقرة: إطار مزخرف حول النص كما في المصاحف المطبوعة
    private func drawCappedFrame(_ ctx: CGContext, _ l: MadinahLayout, first: LaidRow, last: LaidRow, _ p: MushafColors) {
        func span(_ r: LaidRow) -> (CGFloat, CGFloat) {
            switch r {
            case .text(let t, let h, _), .banner(let t, let h, _), .basmala(let t, let h): return (t, h)
            }
        }
        let w = bounds.width, h = bounds.height
        let pad = l.lineH * 0.45
        let top = span(first).0 - pad
        let bottom = span(last).0 + span(last).1 + pad
        let inset: CGFloat = 4
        let outer = CGRect(x: -inset, y: top, width: w + 2 * inset, height: bottom - top)
        roundRect(ctx, outer, radius: 22, fill: p.fill.withAlphaComponent(0.35), stroke: p.goldDeep.withAlphaComponent(0.8), width: 1.4)
        roundRect(ctx, outer.insetBy(dx: 4, dy: 4), radius: 18, stroke: p.gold.withAlphaComponent(0.7), width: 0.7)
        if top > l.lineH * 1.5 {
            let rr = min(top * 0.32, w * 0.16)
            star8(ctx, center: CGPoint(x: w / 2, y: top / 2), radius: rr, color: p.gold.withAlphaComponent(0.5), stroke: 1.2, rings: true)
            star8(ctx, center: CGPoint(x: w / 2, y: (bottom + h) / 2), radius: rr, color: p.gold.withAlphaComponent(0.5), stroke: 1.2, rings: true)
        }
    }
}

// MARK: - الصفحة

protocol MushafPageHost: AnyObject {
    var session: NativeMushafSession { get }
    var state: MushafState? { get }
    var colors: MushafColors? { get }
}

/// صفحة واحدة: الورقة بظلها، والأسطر فوق أشرطة التظليل، وغشاء يخبو به ما يغادر أثناء التقليب
final class MushafPageView: UIView {
    weak var host: MushafPageHost?
    private(set) var page = 0
    /// موضعها في الكتاب المفتوح: الهامش والزوايا عند الكعب وظله وطرف التقليب
    var side = MushafSide.single { didSet { if side != oldValue { setNeedsLayout() } } }

    private let card = UIView()
    private let cardMask = CAShapeLayer()
    private let chrome = PageChromeView()
    private let textArea = UIView()
    private let selectLayer = CAShapeLayer()
    private let pressLayer = CAShapeLayer()
    private let glyphs = PageGlyphView()
    /// ظل الكلمات على الورقة في الوضع ثلاثي الأبعاد: غباش لين تحت الأسطر (لا نسخة من الحروف)، صورة تُحسب مرة لكل صفحة
    private let shade = UIImageView()
    /// يرسم الأسطر بلون الظل لصورة الغباش (خارج الشاشة)
    private let shadeDrawer = PageGlyphView()
    /// الوضع ثلاثي الأبعاد مفعّل في هذه الصفحة
    private var depthOn = false
    private let spinner = UIActivityIndicatorView(style: .medium)
    private let note = UILabel()
    private let retry = UIButton(type: .system)
    let veil = UIView()

    private var layoutSize: CGSize = .zero
    private var cardSide: MushafSide?
    private var shownSelection = -2
    private var pressTimer: Timer?
    private var pressStart: CGPoint?
    /// تغيّر المقاس يُطبَّق فوراً (إعداد الخلية، أو تغيّر الحالة كظهور مشغّل التلاوة) لا بعد أن يستقر
    var resizeNow = false
    /// المقاس يتغير مع كل إطار (طيّ الرأس في وضع التركيز): تبقى الأسطر المحسوبة كما هي حتى يستقر، ثم تُحسب في الخلفية
    private var resizing = false
    private var resizeWork: DispatchWorkItem?

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .clear
        layer.shadowColor = UIColor(red: 0x5C / 255, green: 0x43 / 255, blue: 0, alpha: 1).cgColor
        layer.shadowOpacity = 0.22
        layer.shadowRadius = 8
        layer.shadowOffset = CGSize(width: 0, height: 3)
        card.clipsToBounds = true
        card.layer.cornerRadius = Geo.corner
        if #available(iOS 13.0, *) { card.layer.cornerCurve = .continuous }
        addSubview(card)
        card.addSubview(chrome)
        shade.isHidden = true
        shade.isUserInteractionEnabled = false
        shade.contentMode = .scaleToFill
        shadeDrawer.tint = MushafPageView.depthShadow
        card.addSubview(shade)
        card.addSubview(textArea)
        textArea.isUserInteractionEnabled = false
        selectLayer.opacity = 1
        textArea.layer.addSublayer(selectLayer)
        textArea.layer.addSublayer(pressLayer)
        pressLayer.opacity = 0
        // الأسطر لا تتجاوز منطقتها أثناء تغيّر المقاس (الرسم نفسه داخلها دائماً)
        glyphs.clipsToBounds = true
        textArea.addSubview(glyphs)
        spinner.hidesWhenStopped = true
        card.addSubview(spinner)
        note.font = .preferredFont(forTextStyle: .footnote)
        note.textAlignment = .center
        note.numberOfLines = 2
        card.addSubview(note)
        retry.titleLabel?.font = .preferredFont(forTextStyle: .subheadline)
        retry.addTarget(self, action: #selector(retryTapped), for: .touchUpInside)
        card.addSubview(retry)
        veil.isUserInteractionEnabled = false
        veil.alpha = 0
        card.addSubview(veil)

        let tap = UITapGestureRecognizer(target: self, action: #selector(tapped(_:)))
        let long = UILongPressGestureRecognizer(target: self, action: #selector(longPressed(_:)))
        long.minimumPressDuration = 0.4
        tap.require(toFail: long)
        addGestureRecognizer(tap)
        addGestureRecognizer(long)

        isAccessibilityElement = true
        accessibilityTraits = .staticText
        accessibilityLanguage = "ar"
    }
    required init?(coder: NSCoder) { fatalError() }

    override var accessibilityLabel: String? {
        get { page > 0 ? host?.session.pageText(page: Int32(page)) : nil }
        set {}
    }

    /// VoiceOver: «انتقل إلى صفحة» من الصفحة نفسها (كلمس رقمها في أسفلها)
    override var accessibilityCustomActions: [UIAccessibilityCustomAction]? {
        get {
            guard let name = host?.state?.goToPage else { return nil }
            return [UIAccessibilityCustomAction(name: name) { [weak self] _ in
                self?.host?.session.pageTap()
                return true
            }]
        }
        set {}
    }

    private var bottomInset: CGFloat { CGFloat(host?.state?.bottom ?? 0) }

    /// مقاس منطقة الأسطر (لحساب الصفحات المجاورة مسبقاً بالمقاس نفسه)
    var textAreaSize: CGSize { layoutSize }

    override func layoutSubviews() {
        super.layoutSubviews()
        // في الكتاب المفتوح الهامش عند الكعب ضيق (كما في MushafPage)
        let padLeft = side == .right ? Geo.spineGap : Geo.padSide
        let padRight = side == .left ? Geo.spineGap : Geo.padSide
        let cardFrame = CGRect(x: padLeft, y: Geo.padTop, width: max(bounds.width - padLeft - padRight, 0),
                               height: max(bounds.height - Geo.padTop - Geo.padBottom - bottomInset, 0))
        if card.frame != cardFrame || cardSide != side {
            card.frame = cardFrame
            cardSide = side
            if side == .single {
                card.layer.mask = nil
                card.layer.cornerRadius = Geo.corner
                layer.shadowPath = UIBezierPath(roundedRect: cardFrame, cornerRadius: Geo.corner).cgPath
            } else {
                // زاويتان كبيرتان في الطرف الخارجي وصغيرتان عند الكعب: قناع بالشكل لا cornerRadius واحد
                card.layer.cornerRadius = 0
                CATransaction.begin()
                CATransaction.setDisableActions(true)
                cardMask.frame = card.bounds
                cardMask.path = mushafPageShape(card.bounds, side: side)
                CATransaction.commit()
                card.layer.mask = cardMask
                layer.shadowPath = mushafPageShape(cardFrame, side: side)
            }
        }
        if chrome.side != side { chrome.side = side; chrome.setNeedsDisplay() }
        chrome.frame = card.bounds
        veil.frame = card.bounds
        let top = Geo.colTop + Geo.headH + Geo.ruleH + Geo.canvasTop
        let area = CGRect(x: Geo.colSide, y: top, width: max(card.bounds.width - 2 * Geo.colSide, 0),
                          height: max(card.bounds.height - top - Geo.colBottom - Geo.canvasBottom, 0))
        // الإطار بالمركز والحدود (لا frame): منطقة الأسطر وظلها قد تُزاحان بـ transform في الوضع ثلاثي الأبعاد
        textArea.bounds = CGRect(origin: .zero, size: area.size)
        textArea.center = CGPoint(x: area.midX, y: area.midY)
        shade.bounds = textArea.bounds.insetBy(dx: -Self.hazePad, dy: -Self.hazePad)
        shade.center = textArea.center
        glyphs.frame = textArea.bounds
        spinner.center = CGPoint(x: area.midX, y: area.midY - 12)
        note.frame = CGRect(x: area.minX, y: area.midY + 4, width: area.width, height: 40)
        retry.sizeToFit()
        retry.center = CGPoint(x: area.midX, y: area.midY + 60)
        if area.size != layoutSize {
            layoutSize = area.size
            if canDeferResize() { deferRelayout() } else { endResize(); refreshText(animated: false) }
        }
    }

    /// تغيّر المقاس يُرجأ إن كانت أسطر الصفحة معروضة ولم تُحسب للمقاس الجديد بعد (لا حساب مع كل إطار من التحريك)
    private func canDeferResize() -> Bool {
        guard !resizeNow, window != nil, let l = glyphs.layout, l.page == page, layoutSize.width > 0, layoutSize.height > 0 else { return false }
        return LayoutCache.shared.peek(page, layoutSize) == nil
    }

    private func deferRelayout() {
        if !resizing {
            resizing = true
            // الأسطر المرسومة تبقى في مكانها بلا إعادة رسم مع كل إطار
            glyphs.contentMode = .topLeft
        }
        resizeWork?.cancel()
        let p = page
        let work = DispatchWorkItem { [weak self] in
            guard let self = self, self.page == p, self.resizing, let host = self.host else { return }
            let size = self.layoutSize
            LayoutCache.shared.build(p, size, fontPath: host.session.fontPath(page: Int32(p))) { [weak self] in
                guard let self = self, self.page == p, self.resizing, self.layoutSize == size else { return }
                self.endResize()
                self.refreshText(animated: true)
            }
        }
        resizeWork = work
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15, execute: work)
    }

    private func endResize() {
        resizeWork?.cancel()
        resizeWork = nil
        guard resizing else { return }
        resizing = false
        glyphs.contentMode = .redraw
        glyphs.setNeedsDisplay()
    }

    /// يعرض الصفحة [p] (عند إعادة استعمال الخلية) أو يحدّثها بالحالة الجديدة
    func show(page p: Int) {
        let changed = p != page
        page = p
        updateLift()
        if changed {
            endResize()
            shownSelection = -2
            glyphs.layout = nil
            // تُمحى أسطر الصفحة السابقة في الخلية المعاد استعمالها (وإن لم يكن خط هذه الصفحة في الجهاز بعد)
            glyphs.setNeedsDisplay()
            cancelPress()
        }
        refreshChrome()
        refreshText(animated: false)
        updateSelection(animated: false)
    }

    func refreshChrome() {
        guard let host = host, let s = host.state, page > 0 else { return }
        let title = host.session.title(page: Int32(page))
        let subtitle = host.session.subtitle(page: Int32(page))
        let digits = host.session.digits(page: Int32(page))
        let read = s.isRead(page), marked = s.marked.contains(page)
        // يُعاد الرسم حين يتغير شيء فقط (تُستدعى مع كل ظهور للخلية)
        if chrome.page != page || chrome.colors != host.colors || chrome.title != title || chrome.subtitle != subtitle
            || chrome.digits != digits || chrome.read != read || chrome.marked != marked || chrome.side != side {
            chrome.page = page
            chrome.colors = host.colors
            chrome.title = title
            chrome.subtitle = subtitle
            chrome.digits = digits
            chrome.read = read
            chrome.marked = marked
            chrome.side = side
            chrome.setNeedsDisplay()
        }
        veil.backgroundColor = host.colors?.backdrop
        note.textColor = host.colors?.muted
        retry.tintColor = host.colors?.accent
        spinner.color = host.colors?.muted
    }

    /// الأسطر: من الذاكرة إن حُسبت، وإلا تُحسب الآن (والخط إن لم يكن في الجهاز يُطلب، ويظهر مؤشر التنزيل)
    func refreshText(animated: Bool) {
        guard let host = host, let s = host.state, page > 0 else { return }
        // أثناء تغيّر المقاس تبقى الأسطر بمقاسها السابق حتى يستقر
        let size = resizing ? (glyphs.layout?.size ?? layoutSize) : layoutSize
        guard size.width > 0, size.height > 0 else { return }
        let path = host.session.fontPath(page: Int32(page))
        let l = LayoutCache.shared.obtain(page, size, fontPath: path)
        // الخط ليس في الجهاز: يُطلب (وبعد تعذّره بلا اتصال لا يُعاد إلا بزر إعادة المحاولة)
        if l == nil && !s.failed.contains(page) { host.session.requestFont(page: Int32(page)) }
        let hifzChanged = glyphs.hifz != s.hifz || glyphs.revealed != Set(s.revealed)
        let colorChanged = glyphs.colors != host.colors
        let layoutChanged = l !== glyphs.layout
        if !layoutChanged && !hifzChanged && !colorChanged { updateWaiting(l == nil, s) ; return }
        let apply = {
            self.glyphs.layout = l
            self.glyphs.colors = host.colors
            self.glyphs.hifz = s.hifz
            self.glyphs.revealed = Set(s.revealed)
            self.glyphs.setNeedsDisplay()
            self.syncShade()
        }
        if animated && window != nil && !UIAccessibility.isReduceMotionEnabled {
            UIView.transition(with: glyphs, duration: layoutChanged ? 0.24 : 0.32, options: [.transitionCrossDissolve, .allowUserInteraction], animations: apply)
        } else {
            apply()
        }
        if layoutChanged { shownSelection = -2; updateSelection(animated: false) }
        updateWaiting(l == nil, s)
    }

    private func updateWaiting(_ waiting: Bool, _ s: MushafState) {
        let failed = waiting && s.failed.contains(page)
        if waiting && !failed { spinner.startAnimating() } else { spinner.stopAnimating() }
        note.isHidden = !waiting
        note.text = s.downloading
        retry.isHidden = !failed
        retry.setTitle(s.retry, for: .normal)
    }

    @objc private func retryTapped() {
        host?.session.requestFont(page: Int32(page))
        retry.isHidden = true
        spinner.startAnimating()
        // إن تعذّر التنزيل قبل أن تُرسل الحالة لم تتغير فلا يصل إشعار: تُراجع بعد لحظة فيعود زر إعادة المحاولة
        let p = page
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) { [weak self] in
            guard let self = self, self.page == p, let s = self.host?.state else { return }
            self.updateWaiting(self.glyphs.layout == nil, s)
        }
    }

    /// تظليل الآية المختارة: الشريط الجديد يظهر والسابق يتلاشى
    func updateSelection(animated: Bool) {
        guard let host = host, let s = host.state else { return }
        let sel = s.selected
        CATransaction.begin()
        // لا تحريك ضمني للون ولا للمسار: شريط آية لا يتحوّل إلى شريط أخرى، بل يظهر الجديد ويتلاشى السابق فقط
        CATransaction.setDisableActions(true)
        // اللون يتبع الورقة ولو بقيت الآية نفسها (تغيّر لون الورقة أو الوضع الليلي)
        selectLayer.fillColor = host.colors?.select.cgColor
        if sel != shownSelection {
            shownSelection = sel
            if animated && !UIAccessibility.isReduceMotionEnabled {
                let fade = CATransition()
                fade.type = .fade
                fade.duration = 0.28
                selectLayer.add(fade, forKey: "fade")
            }
            selectLayer.path = glyphs.layout?.band(sel)
        }
        CATransaction.commit()
    }

    // MARK: ثلاثي الأبعاد

    /// لون الأسطر قبل تمويهها إلى غباش (يخفّ كثيراً بعد التمويه؛ DepthShadow في MushafStyle.kt)
    static let depthShadow = UIColor(red: 0x3B / 255, green: 0x2A / 255, blue: 0x10 / 255, alpha: 0.7)
    /// ارتفاع الكلمات عن الورقة: أقصى إزاحة لها مع الميل
    private static let lift: CGFloat = 6

    /// يفعّل الوضع ثلاثي الأبعاد أو يوقفه حسب الحالة: نسخة الظل، وقراءة حركة الجهاز ما دامت الصفحة ظاهرة
    func updateLift() {
        let on = host?.state?.depth ?? false
        if on != depthOn {
            depthOn = on
            shade.isHidden = !on
            syncShade()
            if !on { textArea.transform = .identity; shade.transform = .identity; shade.image = nil }
        }
        if on && window != nil { MushafDepthMotion.shared.attach(self) } else { MushafDepthMotion.shared.detach(self) }
        applyTilt(MushafDepthMotion.shared.tilt)
    }

    /// هامش صورة الغباش حول منطقة الأسطر (يتسع له التمويه)
    private static let hazePad: CGFloat = 24
    private static let ciContext = CIContext(options: [.useSoftwareRenderer: false])

    /// الغباش يتبع أسطر الصفحة: تُرسم الأسطر بلون الظل بدقة منخفضة ثم تُموَّه بقوة (Core Image)، فيبقى ظل لين
    /// تحت الأسطر لا يُقرأ فيه شكل الحروف. يُحسب مرة لكل صفحة (لا مع الحركة)
    private func syncShade() {
        guard depthOn, let l = glyphs.layout, let colors = glyphs.colors else {
            shade.image = nil
            return
        }
        let size = textArea.bounds.size
        guard size.width > 0, size.height > 0 else { return }
        let pad = Self.hazePad
        shadeDrawer.frame = CGRect(origin: .zero, size: size)
        shadeDrawer.tint = host?.state?.haze.map { MushafColors.color($0) } ?? MushafPageView.depthShadow
        shadeDrawer.layout = l
        shadeDrawer.colors = colors
        shadeDrawer.hifz = glyphs.hifz
        shadeDrawer.revealed = glyphs.revealed
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        format.opaque = false
        let full = CGSize(width: size.width + 2 * pad, height: size.height + 2 * pad)
        let flat = UIGraphicsImageRenderer(size: full, format: format).image { r in
            r.cgContext.translateBy(x: pad, y: pad)
            self.shadeDrawer.draw(CGRect(origin: .zero, size: size))
        }
        guard let input = CIImage(image: flat) else { shade.image = nil; return }
        let blurred = input.clampedToExtent().applyingGaussianBlur(sigma: 7).cropped(to: input.extent)
        guard let cg = Self.ciContext.createCGImage(blurred, from: input.extent) else { shade.image = nil; return }
        shade.image = UIImage(cgImage: cg)
    }

    /// ميل الجهاز (كل محور بين -1 و 1): الكلمات تنزاح معه، وظلها على الورقة عكسه، فتبدو مرفوعة عنها.
    /// الظل يظهر مع الحركة فقط ويقوى بقدر الميل: والجهاز ساكن (أثناء القراءة) الميل صفر بعد لحظة، فلا ظل
    /// يشوّش على الحروف والصفحة نظيفة كالمطبوعة. مع «تقليل الحركة» يبقى الميل صفراً فلا حركة ولا ظل
    func applyTilt(_ t: CGPoint) {
        guard depthOn else { return }
        let h = Self.lift
        let amount = min(1, (t.x * t.x + t.y * t.y).squareRoot())
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        textArea.transform = CGAffineTransform(translationX: t.x * h, y: t.y * h)
        shade.transform = CGAffineTransform(translationX: -t.x * h * 0.6, y: -t.y * h * 0.6)
        shade.alpha = amount
        CATransaction.commit()
    }

    override func didMoveToWindow() {
        super.didMoveToWindow()
        if depthOn && window != nil { MushafDepthMotion.shared.attach(self) } else { MushafDepthMotion.shared.detach(self) }
    }

    // MARK: اللمس

    private func ayah(at point: CGPoint) -> Int {
        guard let l = glyphs.layout else { return -1 }
        let p = convert(point, to: textArea)
        guard p.y >= -l.lineH, p.y <= textArea.bounds.height + l.lineH else { return -1 }
        return l.hit(p)
    }

    /// اللمس على رقم الصفحة في أسفلها (الميدالية على خطَّي الإطار السفليين)
    private func onMedallion(_ point: CGPoint) -> Bool {
        let pt = convert(point, to: card)
        let b = card.bounds
        guard b.width > 0, b.height > 0 else { return false }
        return hypot(pt.x - b.width / 2, pt.y - (b.height - Geo.medallionCenter)) <= Geo.medallionTouch
    }

    @objc private func tapped(_ g: UITapGestureRecognizer) {
        guard let host = host, let s = host.state else { return }
        // لمس رقم الصفحة يفتح اختيار الصفحة (قبل الآيات: آخر سطر قريب منه)
        if onMedallion(g.location(in: self)) { host.session.pageTap(); return }
        let pt = g.location(in: card)
        // في وضع التركيز: لمس طرف الصفحة يقلبها، الأيسر إلى التالية والأيمن إلى السابقة كالكتاب العربي؛
        // وفي الكتاب المفتوح الطرفان الخارجيان فقط (طرف الكعب بين الصفحتين لا يقلب)
        if s.focus && card.bounds.width > 0 {
            if side != .right && pt.x < card.bounds.width * Geo.edge { host.session.edge(dir: 1); return }
            if side != .left && pt.x > card.bounds.width * (1 - Geo.edge) { host.session.edge(dir: -1); return }
        }
        let i = ayah(at: g.location(in: self))
        if i >= 0 { host.session.tap(ayah: Int32(i)) } else { host.session.frameTap() }
    }

    @objc private func longPressed(_ g: UILongPressGestureRecognizer) {
        guard g.state == .began else { return }
        let i = ayah(at: g.location(in: self))
        // الضغط المطوّل: شريط خيارات الآية (التفسير والاستماع والحفظ والنسخ والمشاركة)
        if i >= 0 { host?.session.longPress(ayah: Int32(i)) }
        cancelPress()
    }

    // ظل خفيف تحت الآية الملموسة بعد لحظة (لا مع بدء السحب لتقليب الصفحة)
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesBegan(touches, with: event)
        guard touches.count == 1, let t = touches.first, !onMedallion(t.location(in: self)) else { return }
        let i = ayah(at: t.location(in: self))
        guard i >= 0, let path = glyphs.layout?.band(i) else { return }
        pressStart = t.location(in: self)
        pressTimer?.invalidate()
        pressTimer = Timer.scheduledTimer(withTimeInterval: 0.07, repeats: false) { [weak self] _ in
            guard let self = self else { return }
            CATransaction.begin()
            CATransaction.setDisableActions(true)
            self.pressLayer.fillColor = self.host?.colors?.press.cgColor
            self.pressLayer.path = path
            CATransaction.commit()
            let a = CABasicAnimation(keyPath: "opacity")
            a.fromValue = 0; a.toValue = 1; a.duration = 0.14
            self.pressLayer.opacity = 1
            self.pressLayer.add(a, forKey: "press")
        }
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesMoved(touches, with: event)
        if let s = pressStart, let t = touches.first {
            let p = t.location(in: self)
            if hypot(p.x - s.x, p.y - s.y) > 10 { cancelPress() }
        }
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesEnded(touches, with: event)
        cancelPress()
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesCancelled(touches, with: event)
        cancelPress()
    }

    private func cancelPress() {
        pressTimer?.invalidate()
        pressTimer = nil
        pressStart = nil
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        pressLayer.removeAllAnimations()
        pressLayer.opacity = 0
        CATransaction.commit()
    }
}

/// موضع تقليب واحد: صفحة واحدة، أو كتاب مفتوح بصفحتين متقابلتين (الفردية على اليمين كمصحف المدينة المطبوع)
final class MushafPageCell: UICollectionViewCell {
    static let id = "page"
    /// ما يُرسم من الموضع (الصفحة أو الصفحتان معاً): ينحسر كله أثناء التقليب كما في Compose
    let book = UIView()
    /// الصفحة الوحيدة، أو اليمنى في الكتاب المفتوح
    let right = MushafPageView()
    /// اليسرى في الكتاب المفتوح
    let left = MushafPageView()
    private(set) var spread = false
    private(set) var hasLeft = false

    override init(frame: CGRect) {
        super.init(frame: frame)
        contentView.addSubview(book)
        book.addSubview(right)
        book.addSubview(left)
        left.side = .left
        left.isHidden = true
        book.accessibilityElements = [right]
    }
    required init?(coder: NSCoder) { fatalError() }

    /// عرض الموضع: الشاشة كلها في الهاتف، وفي الشاشات العريضة صفحة (أو صفحتان) بنسبة المصحف المطبوع في الوسط
    var frameWidth: CGFloat = 0 { didSet { if frameWidth != oldValue { setNeedsLayout() } } }

    /// الصفحات الظاهرة في الموضع (اليمنى أولاً، وهو ترتيب قارئ الشاشة)
    var pageViews: [MushafPageView] { hasLeft ? [right, left] : [right] }

    func setSpread(_ on: Bool, left withLeft: Bool) {
        let l = on && withLeft
        let changed = on != spread || l != hasLeft
        spread = on
        hasLeft = l
        right.side = on ? .right : .single
        left.isHidden = !l
        if changed {
            book.accessibilityElements = l ? [right, left] : [right]
            setNeedsLayout()
        }
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let full = contentView.bounds
        let w = frameWidth > 0 ? min(frameWidth, full.width) : full.width
        let size = CGSize(width: w, height: full.height)
        // bounds و center لا frame: الموضع قد يكون مصغَّراً أثناء التقليب (transform)
        if book.bounds.size != size { book.bounds = CGRect(origin: .zero, size: size) }
        let c = CGPoint(x: full.midX, y: full.midY)
        if book.center != c { book.center = c }
        // اليمنى في النصف الأيمن واليسرى في الأيسر؛ والصفحة الوحيدة بالعرض كله
        let half = spread ? w / 2 : w
        let r = CGRect(x: w - half, y: 0, width: half, height: size.height)
        if right.frame != r { right.frame = r }
        // اليسرى المخفية (صفحة واحدة) لا تُخطَّط فلا تُحسب أسطر صفحة لا تظهر
        let l = CGRect(x: 0, y: 0, width: half, height: size.height)
        if hasLeft && left.frame != l { left.frame = l }
    }

    /// تخطيط فوري: تغيّر المقاس هنا (إعادة استعمال الخلية، أو تغيّر الحالة) لا ينتظر أن يستقر
    func layoutNow() {
        right.resizeNow = true
        left.resizeNow = true
        setNeedsLayout()
        layoutIfNeeded()
        right.setNeedsLayout()
        right.layoutIfNeeded()
        if hasLeft { left.setNeedsLayout(); left.layoutIfNeeded() }
        right.resizeNow = false
        left.resizeNow = false
    }
}

/// التخطيط من اليمين إلى اليسار: الصفحة الأولى في أقصى اليمين، والتالية على يسارها كالمصحف المطبوع
final class RightToLeftFlowLayout: UICollectionViewFlowLayout {
    override var flipsHorizontallyInOppositeLayoutDirection: Bool { true }
}

// MARK: - المتحكم

final class MushafReaderController: UIViewController, UICollectionViewDataSource, UICollectionViewDelegateFlowLayout,
    UICollectionViewDataSourcePrefetching, MushafPageHost {
    let session: NativeMushafSession
    private(set) var state: MushafState?
    private(set) var colors: MushafColors?
    private let flow = RightToLeftFlowLayout()
    private lazy var pages = UICollectionView(frame: .zero, collectionViewLayout: flow)
    /// عدد مواضع التقليب المعروضة الآن (604 صفحة، أو 302 كتاباً مفتوحاً)؛ يتغير مع reloadData فقط
    private var itemCount = 0
    /// آخر انتقال طلبه التطبيق وطُبِّق
    private var appliedSeq = -1
    /// موضع التقليب الظاهر (Spreads.index: رقم الصفحة ناقص واحد، أو رقم الكتاب المفتوح)
    private var current = 0
    private var laidOutSize: CGSize = .zero
    /// الصفحات أُنشئت: عُرف المقاس ووصلت الحالة
    private var loaded = false
    private var edgeLinked = false
    /// رحلة الانتقال إلى صفحة الجارية (أو توهج الهبوط وحده)
    private var journey: MushafJourneyPlayer?
    /// توهج رقم الصفحة حين تستقر التقليبة القصيرة إلى الصفحة المجاورة
    private var pendingGlow: MushafJourney?

    init(session: NativeMushafSession) {
        self.session = session
        super.init(nibName: nil, bundle: nil)
    }
    required init?(coder: NSCoder) { fatalError() }

    deinit { NotificationCenter.default.removeObserver(self) }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .clear
        flow.scrollDirection = .horizontal
        flow.minimumLineSpacing = 0
        flow.minimumInteritemSpacing = 0
        pages.semanticContentAttribute = .forceRightToLeft
        pages.isPagingEnabled = true
        pages.decelerationRate = .fast
        pages.showsHorizontalScrollIndicator = false
        pages.showsVerticalScrollIndicator = false
        pages.alwaysBounceVertical = false
        pages.contentInsetAdjustmentBehavior = .never
        // ظل الآية الملموسة يبدأ فوراً، ويُلغى إن بدأ السحب
        pages.delaysContentTouches = false
        pages.scrollsToTop = false
        pages.dataSource = self
        pages.delegate = self
        pages.prefetchDataSource = self
        pages.register(MushafPageCell.self, forCellWithReuseIdentifier: MushafPageCell.id)
        pages.frame = view.bounds
        pages.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(pages)
        NotificationCenter.default.addObserver(self, selector: #selector(stateChanged(_:)), name: Notification.Name("MizanMushafState"), object: session)
        apply(session.state)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        linkBackGesture()
    }

    /// سحب الحافة للرجوع (من Compose، UIScreenEdgePanGestureRecognizer في جذر الواجهة) يسبق تقليب الصفحات من الطرف
    private func linkBackGesture() {
        guard !edgeLinked else { return }
        var v: UIView? = view.superview
        while let s = v {
            for g in s.gestureRecognizers ?? [] where g is UIScreenEdgePanGestureRecognizer {
                pages.panGestureRecognizer.require(toFail: g)
                edgeLinked = true
            }
            v = s.superview
        }
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        let size = pages.bounds.size
        guard size.width > 0, size.height > 0, size != laidOutSize else { return }
        laidOutSize = size
        flow.itemSize = size
        linkBackGesture()
        if loaded {
            // تغيّر المقاس (تدوير iPad، Split View، طيّ الرأس في وضع التركيز): الموضع نفسه يبقى ظاهراً
            flow.invalidateLayout()
            pages.layoutIfNeeded()
            scroll(to: current, animated: false, quiet: true)
        } else {
            loadIfReady()
        }
    }

    /// الصفحات تُنشأ بعد أن يُعرف مقاسها وتصل الحالة (لا خلايا صغيرة تُحسب ثم تُرمى، ولا الصفحة الأولى قبل صفحة القارئ)
    private func loadIfReady() {
        guard !loaded, laidOutSize != .zero, let s = state else { return }
        loaded = true
        itemCount = s.items
        pages.reloadData()
        pages.layoutIfNeeded()
        scroll(to: current, animated: false)
    }

    @objc private func stateChanged(_ note: Notification) {
        apply(session.state)
    }

    private func apply(_ json: String) {
        guard let s = MushafState.decode(json) else { return }
        let old = state
        state = s
        let c = MushafColors(s)
        let colorsChanged = c != colors
        colors = c
        if colorsChanged { pages.backgroundColor = c.backdrop }
        guard let o = old else {
            current = s.jump
            appliedSeq = s.seq
            loadIfReady()
            return
        }
        // صفحة واحدة ↔ صفحتان متقابلتان (تدوير iPad، فتح iPhone Duo وطيّه): المواضع كلها جديدة، والقارئ على الموضع
        // الذي فيه صفحته (Kotlin يبني NativeTurns جديداً موضعه Spreads.index(anchor, spread))
        if o.isSpread != s.isSpread || o.items != s.items {
            current = s.jump
            appliedSeq = s.seq
            if loaded {
                itemCount = s.items
                pages.reloadData()
                pages.layoutIfNeeded()
                scroll(to: current, animated: false)
            }
            return
        }
        let geometry = o.bottom != s.bottom || o.frame != s.frame
        for cell in pages.visibleCells.compactMap({ $0 as? MushafPageCell }) {
            cell.frameWidth = CGFloat(s.frame)
            if geometry { cell.layoutNow() }
            for v in cell.pageViews {
                if o.depth != s.depth { v.updateLift() }
                else if o.haze != s.haze { v.refreshText(animated: false) }
                if colorsChanged || o.marked != s.marked || o.read != s.read { v.refreshChrome() }
                if colorsChanged || o.hifz != s.hifz || o.revealed != s.revealed || o.fonts != s.fonts || o.failed != s.failed {
                    v.refreshText(animated: true)
                }
                if o.selected != s.selected || colorsChanged { v.updateSelection(animated: !colorsChanged) }
            }
        }
        // انتقال طلبه التطبيق (التلاوة، القفز إلى صفحة، طرف الصفحة في وضع التركيز، رحلة الانتقال إلى صفحة)
        if s.seq != appliedSeq, loaded {
            appliedSeq = s.seq
            // انتقال جديد أثناء رحلة: تنتهي الرحلة فوراً (والانتقال الجديد يحدد الموضع)
            journey?.finish(notify: false)
            journey = nil
            pendingGlow = nil
            if let j = s.journey, view.window != nil, !pages.isDragging, !pages.isDecelerating {
                travel(j, to: s.jump)
            } else if s.jump != current || pages.isDragging || pages.isDecelerating {
                let near = abs(s.jump - current) <= 2
                let motion = s.animate && view.window != nil && !UIAccessibility.isReduceMotionEnabled
                if motion && !near && !pages.isDragging && !pages.isDecelerating {
                    // انتقال بعيد (اختيار صفحة): يُقفز بلا حركة إلى الموضع المجاور لها، ثم تُقلب إليها بحركة التقليب المعتادة
                    scroll(to: s.jump + (s.jump > current ? -1 : 1), animated: false)
                    scroll(to: s.jump, animated: true)
                } else {
                    scroll(to: s.jump, animated: motion && near)
                }
            }
        }
    }

    /// رحلة الانتقال إلى الموضع [target]: بعيدة بالرفع والتقليب والهبوط (وتلاشٍ مع تقليل الحركة)، وقريبة بتقليبة معتادة
    /// ثم التوهج، والصفحة نفسها بالتوهج وحده
    private func travel(_ j: MushafJourney, to target: Int) {
        let reduce = UIAccessibility.isReduceMotionEnabled
        if j.kind == "still" || (j.kind == "short" && !reduce) {
            if target != current {
                pendingGlow = j
                UISelectionFeedbackGenerator().selectionChanged()
                scroll(to: target, animated: true)
            } else {
                landingGlow(j)
            }
            return
        }
        guard let player = MushafJourneyPlayer(host: self, pages: pages, journey: j, target: target, reduce: reduce),
              player.start(in: view, jump: { self.scroll(to: target, animated: false) }) else {
            pages.isHidden = false
            scroll(to: target, animated: false)
            landingGlow(j)
            return
        }
        journey = player
    }

    /// الضوء الذهبي وتوهج رقم الصفحة الظاهرة ونقرة النجاح
    private func landingGlow(_ j: MushafJourney) {
        journey?.finish(notify: false)
        journey = nil
        guard let player = MushafJourneyPlayer(host: self, pages: pages, journey: j, target: current, reduce: UIAccessibility.isReduceMotionEnabled),
              player.startGlowOnly(in: view) else { return }
        journey = player
    }

    /// انتهت الرحلة (أو التوهج): القارئ على موضعه الجديد
    func journeyEnded(_ p: MushafJourneyPlayer) {
        guard journey === p else { return }
        journey = nil
        settle()
    }

    /// [quiet]: إعادة المحاذاة بعد تغيّر المقاس (مع كل إطار أحياناً)، بلا طلب خطوط
    private func scroll(to index: Int, animated: Bool, quiet: Bool = false) {
        let i = max(0, min(itemCount - 1, index))
        current = i
        guard pages.numberOfItems(inSection: 0) > i else { return }
        pages.scrollToItem(at: IndexPath(item: i, section: 0), at: .centeredHorizontally, animated: animated)
        if !animated {
            pages.layoutIfNeeded()
            updateDepth()
            if !quiet { prefetchFonts(around: i) }
        }
    }

    /// خطوط الصفحات حول الموضع (في الكتاب المفتوح حول صفحته اليمنى: يشمل اليسرى والكتابين قبله وبعده)
    private func prefetchFonts(around item: Int) {
        guard let s = state else { return }
        session.prefetch(page: Int32(s.pagesOf(item).right))
    }

    // MARK: الصفحات

    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int { loaded ? itemCount : 0 }

    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: MushafPageCell.id, for: indexPath) as! MushafPageCell
        configure(cell, item: indexPath.item)
        return cell
    }

    /// يعدّ الخلية للموضع [item] بالحالة الحالية؛ رخيص إن لم يتغير شيء
    private func configure(_ cell: MushafPageCell, item: Int) {
        guard let s = state else { return }
        let pg = s.pagesOf(item)
        cell.right.host = self
        cell.left.host = self
        cell.frameWidth = CGFloat(s.frame)
        cell.setSpread(s.isSpread, left: pg.left != nil)
        cell.right.show(page: pg.right)
        if let l = pg.left { cell.left.show(page: l) }
        cell.layoutNow()
    }

    func collectionView(_ collectionView: UICollectionView, prefetchItemsAt indexPaths: [IndexPath]) {
        // أسطر الصفحات القادمة تُحسب في الخلفية بمقاس الصفحة الظاهرة، فلا يُحسب شيء لحظة وصولها
        guard let s = state, let visible = (pages.visibleCells.first as? MushafPageCell)?.right else { return }
        let size = visible.textAreaSize
        guard size.width > 0, size.height > 0 else { return }
        for ip in indexPaths {
            let pg = s.pagesOf(ip.item)
            for p in [pg.right] + (pg.left.map { [$0] } ?? []) where p >= 1 && p <= s.totalPages {
                if session.hasFont(page: Int32(p)) { LayoutCache.shared.prefetch(p, size, fontPath: session.fontPath(page: Int32(p))) }
                else { session.requestFont(page: Int32(p)) }
            }
        }
    }

    func collectionView(_ collectionView: UICollectionView, willDisplay cell: UICollectionViewCell, forItemAt indexPath: IndexPath) {
        // الخلية المحضَّرة مسبقاً أو العائدة بعد أن غادرت لم تصلها الحالة الأخيرة (الورقة، المقاس، التظليل، الخط): تُحدَّث الآن
        if let c = cell as? MushafPageCell { configure(c, item: indexPath.item) }
        updateDepth()
    }

    // MARK: التقليب

    func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
        session.scrolling()
    }

    func scrollViewDidScroll(_ scrollView: UIScrollView) { updateDepth() }

    func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) { settle() }

    func scrollViewDidEndDragging(_ scrollView: UIScrollView, willDecelerate decelerate: Bool) {
        if !decelerate { settle() }
    }

    func scrollViewDidEndScrollingAnimation(_ scrollView: UIScrollView) { settle() }

    private func settle() {
        let mid = CGPoint(x: pages.bounds.midX, y: pages.bounds.midY)
        guard let ip = pages.indexPathForItem(at: mid) else { return }
        current = ip.item
        session.settled(index: Int32(ip.item))
        prefetchFonts(around: ip.item)
        // التقليبة القصيرة من رحلة الانتقال استقرت: التوهج
        if let j = pendingGlow {
            pendingGlow = nil
            landingGlow(j)
        }
    }

    /// الموضع المغادر ينحسر قليلاً وتخبو صفحاته نحو لون ما حولها، كما في المصحف بـ Compose
    private func updateDepth() {
        let w = pages.bounds.width
        guard w > 0 else { return }
        let mid = pages.bounds.midX
        for cell in pages.visibleCells.compactMap({ $0 as? MushafPageCell }) {
            let away = min(abs(cell.center.x - mid) / w, 1)
            let sc = 1 - 0.06 * away
            cell.book.transform = CGAffineTransform(scaleX: sc, y: sc)
            for v in cell.pageViews { v.veil.alpha = 0.4 * away }
        }
    }
}

// MARK: - رحلة الانتقال إلى صفحة

/// رحلة الانتقال إلى صفحة كما يخططها Kotlin (PageJourney في quran/PageJourney.kt): [sheets] مواضع الأوراق بالترتيب
/// (الأولى الموضع الحالي والأخيرة المقصود)، و[pages] رقم الصفحة على كل ورقة للرقم الدوّار، و[turns] مدة كل تقليبة بالملّي ثانية
struct MushafJourney: Decodable {
    /// still: الصفحة نفسها (توهج فقط)، short: قريبة (تقليبة واحدة)، flip: بعيدة
    let kind: String
    let forward: Bool
    let sheets: [Int]
    let pages: [Int]
    let turns: [Int]
    let lift: Int
    let land: Int
    let juzStarts: [Int]
    let juzNames: [String]
}

/// الميدالية الذهبية فوق الكتاب أثناء الرحلة: الرقم يدور فيها كعدّاد بالأرقام المشرقية (كرقم صفحة المصحف)، وتحتها اسم
/// الجزء يعبر حين تتجاوز الأوراق بداية جزء (JourneyBadge في PageJourneyOverlay.kt)
final class MushafJourneyBadge: UIView {
    private let glow = CAGradientLayer()
    private let disc = CAShapeLayer()
    private let ring = CAShapeLayer()
    private let inner = CAShapeLayer()
    private let star = CAShapeLayer()
    private let numbers = UIView()
    private var columns: [(box: UIView, cur: UILabel, next: UILabel)] = []
    private let juz = UILabel()
    private let juzBox = UIView()
    private let font: UIFont
    private let digitW: CGFloat
    private let digitH: CGFloat
    private var shownJuz = ""
    private let ink: UIColor
    static let size: CGFloat = 104

    init(colors p: MushafColors) {
        let f = UIFont.systemFont(ofSize: 30, weight: .bold)
        let glyphs = "٠١٢٣٤٥٦٧٨٩".map { String($0) }
        font = f
        digitW = ceil(glyphs.map { ($0 as NSString).size(withAttributes: [.font: f]).width }.max() ?? 18)
        digitH = ceil(f.lineHeight)
        ink = p.goldDeep
        super.init(frame: CGRect(x: 0, y: 0, width: MushafJourneyBadge.size, height: MushafJourneyBadge.size + 40))
        isUserInteractionEnabled = false
        let s = MushafJourneyBadge.size, c = CGPoint(x: s / 2, y: s / 2), m = s / 2 * 0.78
        glow.type = .radial
        glow.colors = [p.gold.withAlphaComponent(0.45).cgColor, p.gold.withAlphaComponent(0).cgColor]
        glow.startPoint = CGPoint(x: 0.5, y: 0.5)
        glow.endPoint = CGPoint(x: 1, y: 1)
        glow.frame = CGRect(x: 0, y: 0, width: s, height: s)
        layer.addSublayer(glow)
        disc.path = UIBezierPath(arcCenter: c, radius: m, startAngle: 0, endAngle: .pi * 2, clockwise: true).cgPath
        disc.fillColor = p.paper.cgColor
        disc.shadowColor = UIColor.black.cgColor
        disc.shadowOpacity = 0.2
        disc.shadowRadius = 4
        disc.shadowOffset = CGSize(width: 0, height: 3)
        layer.addSublayer(disc)
        ring.path = UIBezierPath(arcCenter: c, radius: m - 1, startAngle: 0, endAngle: .pi * 2, clockwise: true).cgPath
        ring.fillColor = p.fill.withAlphaComponent(0.6).cgColor
        ring.strokeColor = p.goldDeep.cgColor
        ring.lineWidth = 2
        layer.addSublayer(ring)
        inner.path = UIBezierPath(arcCenter: c, radius: m - 5, startAngle: 0, endAngle: .pi * 2, clockwise: true).cgPath
        inner.fillColor = nil
        inner.strokeColor = p.gold.withAlphaComponent(0.8).cgColor
        inner.lineWidth = 0.9
        layer.addSublayer(inner)
        let sp = CGMutablePath(), r = m - 6
        for sq in 0..<2 {
            for i in 0..<4 {
                let a = (Double(sq) * 45 + Double(i) * 90 - 90) * .pi / 180
                let pt = CGPoint(x: c.x + r * CGFloat(cos(a)), y: c.y + r * CGFloat(sin(a)))
                if i == 0 { sp.move(to: pt) } else { sp.addLine(to: pt) }
            }
            sp.closeSubpath()
        }
        star.path = sp
        star.fillColor = nil
        star.strokeColor = p.gold.withAlphaComponent(0.35).cgColor
        star.lineWidth = 1
        layer.addSublayer(star)
        numbers.frame = CGRect(x: 0, y: (s - digitH) / 2, width: s, height: digitH)
        addSubview(numbers)
        juzBox.backgroundColor = p.paper.withAlphaComponent(0.94)
        juzBox.layer.borderColor = p.gold.withAlphaComponent(0.7).cgColor
        juzBox.layer.borderWidth = 1
        juzBox.clipsToBounds = true
        juz.font = UIFont.systemFont(ofSize: 14, weight: .bold)
        juz.textColor = p.goldDeep
        juz.textAlignment = .center
        juzBox.addSubview(juz)
        addSubview(juzBox)
    }
    required init?(coder: NSCoder) { fatalError() }

    /// اسم الجزء تحت الرقم؛ يعبر من أسفل حين يتقدم ومن أعلى حين يرجع
    func setJuz(_ name: String, up: Bool) {
        guard name != shownJuz else { return }
        let first = shownJuz.isEmpty
        shownJuz = name
        if !first {
            let t = CATransition()
            t.type = .push
            t.subtype = up ? .fromTop : .fromBottom
            t.duration = 0.22
            juz.layer.add(t, forKey: "cross")
        }
        juz.text = name
        let w = ceil((name as NSString).size(withAttributes: [.font: juz.font as Any]).width) + 28
        juzBox.frame = CGRect(x: (MushafJourneyBadge.size - w) / 2, y: MushafJourneyBadge.size + 6, width: w, height: 28)
        juzBox.layer.cornerRadius = 14
        juz.frame = juzBox.bounds
    }

    /// عدّاد كعدّاد المسافات: الآحاد تدور مع القيمة، وما فوقها يدور حين يبلغ ما تحته ٩؛ والأصفار في أول الرقم لا تُرسم
    func setValue(_ value: Double) {
        let v = max(0, value)
        let whole = Int(v.rounded(.down))
        let frac = CGFloat(v - Double(whole))
        let count = max(1, String(whole).count)
        while columns.count < count {
            let box = UIView()
            box.clipsToBounds = true
            let cur = UILabel(), next = UILabel()
            for l in [cur, next] { l.font = font; l.textColor = ink; l.textAlignment = .center; box.addSubview(l) }
            numbers.addSubview(box)
            columns.append((box, cur, next))
        }
        let left = (MushafJourneyBadge.size - digitW * CGFloat(count)) / 2
        var pow = 1
        for _ in 1..<count { pow *= 10 }
        for (i, col) in columns.enumerated() {
            guard i < count else { col.box.isHidden = true; continue }
            col.box.isHidden = false
            col.box.frame = CGRect(x: left + CGFloat(i) * digitW, y: 0, width: digitW, height: digitH)
            let c = whole / pow
            let d = c % 10
            let f: CGFloat = (pow == 1 || whole % pow == pow - 1) ? frac : 0
            col.cur.text = (pow == 1 || c > 0) ? digit(d) : ""
            col.cur.frame = CGRect(x: 0, y: -f * digitH, width: digitW, height: digitH)
            col.next.isHidden = f <= 0
            col.next.text = digit((d + 1) % 10)
            col.next.frame = CGRect(x: 0, y: (1 - f) * digitH, width: digitW, height: digitH)
            pow = max(1, pow / 10)
        }
    }

    private func digit(_ d: Int) -> String {
        guard let u = Unicode.Scalar(0x660 + d) else { return "\(d)" }
        return String(Character(u))
    }
}

/// يرسم رحلة الانتقال إلى صفحة فوق القارئ (كما يرسمها Compose في PageJourneyOverlay.kt): صورة الصفحة الحالية تُرفع عن الكتاب
/// قليلاً بظلها، ثم تنقلب أوراق المصحف بسرعة في اتجاه القراءة بطيّ ثلاثي الأبعاد (CATransform3D) وفوقها الميدالية والرقم الدوّار،
/// ثم تهبط الصفحة المقصودة بارتداد خفيف ويمرّ عليها ضوء ذهبي ويتوهج رقمها، مع نقرات خفيفة مع كل ورقة ونقرة نجاح عند الهبوط.
/// الأوراق بين الطرفين ورق بإطاره ورأسه ورقمه (PageChromeView) وأسطر باهتة، والصفحة المقصودة صورتها بعد أن يُقفز إليها
/// تحت الأوراق بلا حركة؛ فلا تُخطَّط صفحات المصحف التي تمرّ بها. [reduce]: تقليل الحركة: تلاشٍ ثم التوهج.
final class MushafJourneyPlayer: NSObject {
    private weak var host: MushafReaderController?
    private let j: MushafJourney
    private let target: Int
    private let reduce: Bool
    private let state: MushafState
    private let colors: MushafColors
    private let session: NativeMushafSession
    private weak var pages: UIView?
    private let stage = UIView()
    private let book = UIView()
    private let liftShade = CAGradientLayer()
    private let landing = UIView()
    private let sweep = CAGradientLayer()
    private let sweepBox = UIView()
    private let glow = CAGradientLayer()
    private let glowRing = CAShapeLayer()
    private var badge: MushafJourneyBadge?
    private var starts: [UIView] = []
    private var ends: [UIView] = []
    private var link: CADisplayLink?
    private var t0: CFTimeInterval = 0
    private var turnShown = -1
    private var landed = false
    private var finished = false
    /// ورقة تدور، وما يُرسم ثابتاً تحتها، وظلالها
    private var leaves: [(view: UIView, dim: UIView, angle: (CGFloat) -> CGFloat, shade: (CGFloat) -> CGFloat, sign: CGFloat)] = []
    /// توهج الهبوط وحده (الصفحة نفسها، أو بعد تقليبة قصيرة إلى المجاورة): بلا صور ولا أوراق
    private var glowOnly = false
    private let tick = UISelectionFeedbackGenerator()
    private let thud = UIImpactFeedbackGenerator(style: .rigid)
    private let success = UINotificationFeedbackGenerator()

    private var liftMs: Double { reduce || glowOnly ? 0 : Double(j.lift) }
    private var flipMs: Double { glowOnly ? 0 : (reduce ? 320 : Double(j.turns.reduce(0, +))) }
    private let landMs: Double = 900

    init?(host: MushafReaderController, pages: UIView, journey: MushafJourney, target: Int, reduce: Bool) {
        guard let s = host.state, let c = host.colors, !journey.sheets.isEmpty else { return nil }
        self.host = host
        self.pages = pages
        self.j = journey
        self.target = target
        self.reduce = reduce
        self.state = s
        self.colors = c
        self.session = host.session
        super.init()
    }

    // MARK: الهندسة (كما يضع MushafPageCell و MushafPageView الصفحة)

    private var size: CGSize { stage.bounds.size }

    /// حدود ورقة الصفحة: [half] nil للصفحة الوحيدة، true لليمنى، false لليسرى في الكتاب المفتوح
    private func cardRect(_ half: Bool?) -> CGRect {
        let w = size.width, h = size.height
        let bw = state.frame > 0 ? min(CGFloat(state.frame), w) : w
        let left = (w - bw) / 2, mid = w / 2, right = left + bw
        let top = Geo.padTop, bottom = h - Geo.padBottom - CGFloat(state.bottom)
        switch half {
        case nil: return CGRect(x: left + Geo.padSide, y: top, width: max(bw - 2 * Geo.padSide, 0), height: max(bottom - top, 0))
        case true?: return CGRect(x: mid + Geo.spineGap, y: top, width: max(right - Geo.padSide - mid - Geo.spineGap, 0), height: max(bottom - top, 0))
        case false?: return CGRect(x: left + Geo.padSide, y: top, width: max(mid - Geo.spineGap - left - Geo.padSide, 0), height: max(bottom - top, 0))
        }
    }

    private var bookRect: CGRect {
        guard state.isSpread else { return cardRect(nil) }
        let l = cardRect(false), r = cardRect(true)
        return CGRect(x: l.minX, y: l.minY, width: r.maxX - l.minX, height: l.height)
    }

    // MARK: البدء

    /// يصوّر الصفحة الحالية، ويقفز إلى المقصودة تحت الصور، ويصوّرها، ثم يبدأ الحركة؛ false إن تعذّر التصوير
    func start(in container: UIView, jump: () -> Void) -> Bool {
        guard let pages = pages else { return false }
        stage.frame = pages.frame
        stage.accessibilityElementsHidden = true
        guard let s1 = pages.snapshotView(afterScreenUpdates: false), let s2 = pages.snapshotView(afterScreenUpdates: false) else { return false }
        starts = [s1, s2]
        container.addSubview(stage)
        book.frame = stage.bounds
        stage.addSubview(book)
        // ظل الكتاب المرفوع
        liftShade.type = .radial
        liftShade.colors = [UIColor.black.withAlphaComponent(0.26).cgColor, UIColor.black.withAlphaComponent(0).cgColor]
        liftShade.startPoint = CGPoint(x: 0.5, y: 0.5)
        liftShade.endPoint = CGPoint(x: 1, y: 1)
        let br = bookRect
        liftShade.frame = br.insetBy(dx: -br.width * 0.08, dy: -br.height * 0.04).offsetBy(dx: 0, dy: 14)
        liftShade.opacity = 0
        book.layer.addSublayer(liftShade)
        // الصورة الحالية تغطي القارئ، ويُقفز تحتها إلى الصفحة المقصودة
        s1.frame = stage.bounds
        book.addSubview(s1)
        jump()
        pages.layoutIfNeeded()
        guard let e1 = pages.snapshotView(afterScreenUpdates: true), let e2 = pages.snapshotView(afterScreenUpdates: true) else {
            stage.removeFromSuperview()
            return false
        }
        ends = [e1, e2]
        pages.isHidden = true
        if !reduce && j.kind == "flip" {
            let b = MushafJourneyBadge(colors: colors)
            b.center = CGPoint(x: stage.bounds.midX, y: stage.bounds.midY + 20)
            b.alpha = 0
            stage.addSubview(b)
            badge = b
            b.setValue(Double(j.pages.first ?? target + 1))
            b.setJuz(juzName(Double(j.pages.first ?? 1)), up: true)
        }
        tick.prepare(); thud.prepare(); success.prepare()
        t0 = CACurrentMediaTime()
        let l = CADisplayLink(target: self, selector: #selector(step(_:)))
        l.add(to: .main, forMode: .common)
        link = l
        return true
    }

    /// التوهج وحده فوق الصفحة الظاهرة: الضوء الذهبي ورقمها، ونقرة النجاح
    func startGlowOnly(in container: UIView) -> Bool {
        guard let pages = pages else { return false }
        glowOnly = true
        stage.frame = pages.frame
        stage.accessibilityElementsHidden = true
        stage.isUserInteractionEnabled = false
        container.addSubview(stage)
        success.prepare()
        t0 = CACurrentMediaTime()
        let l = CADisplayLink(target: self, selector: #selector(step(_:)))
        l.add(to: .main, forMode: .common)
        link = l
        return true
    }

    private func juzName(_ page: Double) -> String {
        let p = Int(page)
        let i = j.juzStarts.lastIndex(where: { $0 <= max(p, 1) }) ?? 0
        return j.juzNames.indices.contains(i) ? j.juzNames[i] : ""
    }

    // MARK: الإطار

    @objc private func step(_ l: CADisplayLink) {
        let ms = (CACurrentMediaTime() - t0) * 1000
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        if ms < liftMs {
            let t = CGFloat(ms / liftMs)
            setLift(1 - 0.08 * easeOut(t))
            badge?.alpha = easeOut(min(1, CGFloat(ms / 240)))
            let sc = 0.86 + 0.14 * (badge?.alpha ?? 1)
            badge?.transform = CGAffineTransform(scaleX: sc, y: sc)
        } else if ms < liftMs + flipMs {
            if reduce {
                // تقليل الحركة: الصورة الحالية تتلاشى عن المقصودة
                if turnShown < 0 { turnShown = 0; showFlat(ends[0]); ends[0].addSubview(starts[0]); starts[0].frame = stage.bounds }
                starts[0].alpha = 1 - CGFloat((ms - liftMs) / flipMs)
            } else {
                setLift(0.92)
                badge?.alpha = 1
                badge?.transform = .identity
                flip(ms - liftMs)
            }
        } else if !landed {
            beginLanding()
        }
        if landed {
            land(ms - liftMs - flipMs)
        }
        CATransaction.commit()
    }

    private func setLift(_ s: CGFloat) {
        book.transform = CGAffineTransform(scaleX: s, y: s)
        liftShade.opacity = Float(min(max((1 - s) / 0.08, 0), 1))
    }

    private func easeOut(_ t: CGFloat) -> CGFloat { 1 - pow(1 - min(max(t, 0), 1), 3) }
    private func easeInOut(_ t: CGFloat) -> CGFloat {
        let x = min(max(t, 0), 1)
        return x < 0.5 ? 4 * x * x * x : 1 - pow(-2 * x + 2, 3) / 2
    }

    /// التقليبة الجارية بعد [ms] من بدء التقليب، وما يدور فيها
    private func flip(_ ms: Double) {
        var k = 0, acc = 0.0
        while k < j.turns.count - 1 && ms >= acc + Double(j.turns[k]) { acc += Double(j.turns[k]); k += 1 }
        let f = easeInOut(CGFloat((ms - acc) / Double(max(j.turns[k], 1))))
        if k != turnShown { showTurn(k) }
        for leaf in leaves {
            let a = leaf.angle(f)
            var t = CATransform3DIdentity
            t.m34 = -1 / 900
            leaf.view.layer.transform = CATransform3DRotate(t, leaf.sign * a * .pi / 180, 0, 1, 0)
            leaf.view.isHidden = a >= 89.5
            leaf.dim.alpha = min(0.5, 0.26 * sin(a * .pi / 180) + leaf.shade(f))
        }
        if j.pages.count > k + 1 {
            let v = Double(j.pages[k]) + Double(j.pages[k + 1] - j.pages[k]) * Double(f)
            badge?.setValue(v)
            badge?.setJuz(juzName(v.rounded()), up: j.forward)
        }
    }

    /// الورقة [i]: الأولى صورة الصفحة الحالية، والأخيرة صورة المقصودة، وما بينهما ورق خفيف بإطاره ورأسه ورقمه
    private func sheet(_ i: Int, copy: Int) -> UIView {
        let n = j.sheets.count - 1
        if i <= 0 { return starts[copy] }
        if i >= n { return ends[copy] }
        return placeholder(j.sheets[i])
    }

    private func showFlat(_ v: UIView) {
        clearLeaves()
        v.removeFromSuperview()
        v.transform = .identity
        v.alpha = 1
        v.frame = stage.bounds
        book.addSubview(v)
    }

    private func clearLeaves() {
        for v in book.subviews { v.removeFromSuperview() }
        leaves.removeAll()
    }

    /// يبني التقليبة [k] (نقرة خفيفة، وأقوى حين تعبر الورقة بداية جزء). الصفحة الواحدة: الورقة تدور حول طرفها الأيمن (الكعب)؛
    /// الكتاب المفتوح: اليسرى تنقلب فوق الكعب إلى اليمين وظهرها اليمنى من الكتاب التالي (وفي الرجوع بالعكس)
    private func showTurn(_ k: Int) {
        turnShown = k
        clearLeaves()
        if j.pages.count > k + 1 {
            let a = j.pages[k], b = j.pages[k + 1]
            let lo = min(a, b), hi = max(a, b)
            if j.juzStarts.contains(where: { $0 > lo && $0 <= hi }) { thud.impactOccurred(); thud.prepare() }
            else { tick.selectionChanged(); tick.prepare() }
        }
        let fwd = j.forward
        if !state.isSpread {
            let under = fwd ? k + 1 : k, top = fwd ? k : k + 1
            addLeaf(sheet(under, copy: 0), half: nil, angle: { _ in 0 }, shade: { f in (fwd ? 1 - f : f) * 0.16 })
            addLeaf(sheet(top, copy: 1), half: nil, angle: { f in 90 * (fwd ? f : 1 - f) }, shade: { _ in 0 })
        } else {
            // النصف الذي ينكشف من تحته الكتاب الجديد: الأيسر في الأمام، والأيمن في الرجوع
            let revealed = !fwd, covered = fwd
            addLeaf(sheet(k + 1, copy: 0), half: revealed, angle: { _ in 0 }, shade: { f in (1 - min(1, f * 2)) * 0.16 })
            addLeaf(sheet(k, copy: 0), half: covered, angle: { _ in 0 }, shade: { f in min(1, max(0, (f - 0.5) * 2)) * 0.16 })
            // وجه الورقة من الكتاب الحالي يرتفع حتى يقف على الكعب، ثم ظهرها من الكتاب الجديد يهبط على الجهة الأخرى
            addLeaf(sheet(k, copy: 1), half: revealed, angle: { f in f < 0.5 ? 90 * min(1, f * 2) : 90 }, shade: { _ in 0 })
            addLeaf(sheet(k + 1, copy: 1), half: covered, angle: { f in f < 0.5 ? 90 : 90 * (1 - min(1, (f - 0.5) * 2)) }, shade: { _ in 0 })
        }
    }

    /// ورقة أو نصفها في حاوية تُقصّ عليه، ومحور دورانها عند الكعب: طرف الصفحة الأيمن، أو وسط الكتاب المفتوح
    private func addLeaf(_ content: UIView, half: Bool?, angle: @escaping (CGFloat) -> CGFloat, shade: @escaping (CGFloat) -> CGFloat) {
        let w = size.width, h = size.height
        let frame: CGRect
        let anchor: CGPoint
        switch half {
        case nil:
            frame = CGRect(x: 0, y: 0, width: w, height: h)
            anchor = CGPoint(x: w > 0 ? cardRect(nil).maxX / w : 1, y: 0.5)
        case true?:
            frame = CGRect(x: w / 2, y: 0, width: w / 2, height: h)
            anchor = CGPoint(x: 0, y: 0.5)
        case false?:
            frame = CGRect(x: 0, y: 0, width: w / 2, height: h)
            anchor = CGPoint(x: 1, y: 0.5)
        }
        let holder = UIView()
        holder.clipsToBounds = half != nil
        holder.layer.anchorPoint = anchor
        holder.bounds = CGRect(origin: .zero, size: frame.size)
        holder.layer.position = CGPoint(x: frame.minX + anchor.x * frame.width, y: frame.minY + anchor.y * frame.height)
        content.removeFromSuperview()
        content.transform = .identity
        content.alpha = 1
        content.frame = CGRect(x: -frame.minX, y: 0, width: w, height: h)
        holder.addSubview(content)
        let dim = UIView()
        dim.backgroundColor = .black
        dim.alpha = 0
        dim.isUserInteractionEnabled = false
        dim.layer.cornerRadius = Geo.corner
        let card = half.map { cardRect($0) } ?? cardRect(nil)
        dim.frame = card.offsetBy(dx: -frame.minX, dy: 0)
        if half == nil && state.isSpread { dim.frame = bookRect }
        holder.addSubview(dim)
        book.addSubview(holder)
        // الصفحة الواحدة: الطرف الحر على يسار المحور فيقترب من العين بزاوية موجبة، والنصف الأيمن بالعكس
        leaves.append((holder, dim, angle, shade, half == true ? -1 : 1))
    }

    /// ورقة خفيفة لموضع التقليب [item]: صفحة واحدة أو صفحتا الكتاب المفتوح بإطارهما ورأسيهما ورقميهما وأسطر باهتة
    private func placeholder(_ item: Int) -> UIView {
        let v = UIView(frame: stage.bounds)
        v.isUserInteractionEnabled = false
        let pg = state.pagesOf(item)
        if state.isSpread {
            v.addSubview(card(pg.right, side: .right, rect: cardRect(true)))
            if let l = pg.left { v.addSubview(card(l, side: .left, rect: cardRect(false))) }
        } else {
            v.addSubview(card(pg.right, side: .single, rect: cardRect(nil)))
        }
        return v
    }

    private func card(_ page: Int, side: MushafSide, rect: CGRect) -> UIView {
        let holder = UIView(frame: rect)
        holder.layer.shadowColor = UIColor(red: 0x5C / 255, green: 0x43 / 255, blue: 0, alpha: 1).cgColor
        holder.layer.shadowOpacity = 0.22
        holder.layer.shadowRadius = 8
        holder.layer.shadowOffset = CGSize(width: 0, height: 3)
        let paper = UIView(frame: holder.bounds)
        paper.clipsToBounds = true
        if side == .single {
            paper.layer.cornerRadius = Geo.corner
            if #available(iOS 13.0, *) { paper.layer.cornerCurve = .continuous }
            holder.layer.shadowPath = UIBezierPath(roundedRect: holder.bounds, cornerRadius: Geo.corner).cgPath
        } else {
            let mask = CAShapeLayer()
            mask.path = mushafPageShape(paper.bounds, side: side)
            paper.layer.mask = mask
            holder.layer.shadowPath = mushafPageShape(holder.bounds, side: side)
        }
        let chrome = PageChromeView(frame: paper.bounds)
        chrome.page = page
        chrome.colors = colors
        chrome.title = session.title(page: Int32(page))
        chrome.subtitle = session.subtitle(page: Int32(page))
        chrome.digits = session.digits(page: Int32(page))
        chrome.side = side
        paper.addSubview(chrome)
        let lines = CAShapeLayer()
        lines.frame = paper.bounds
        lines.path = ghostLines(page, size: paper.bounds.size)
        lines.fillColor = colors.ink.withAlphaComponent(0.09).cgColor
        paper.layer.addSublayer(lines)
        holder.addSubview(paper)
        return holder
    }

    /// خمسة عشر سطراً باهتاً من «كلمات» بأطوال مختلفة (ثابتة لكل صفحة)، مضبوطة من الطرفين كأسطر المصحف (drawGhostLines)
    private func ghostLines(_ page: Int, size s: CGSize) -> CGPath {
        let path = CGMutablePath()
        let left = Geo.colSide, right = s.width - Geo.colSide
        let top = Geo.colTop + Geo.headH + Geo.ruleH + Geo.canvasTop
        let bottom = s.height - Geo.colBottom - 2
        guard right - left > 40, bottom - top > 60 else { return path }
        let rows = 15
        let step = (bottom - top) / CGFloat(rows)
        let h = min(step * 0.26, 5), gap: CGFloat = 4, width = right - left
        var seed = page &* 7919 &+ 17
        func next() -> CGFloat { seed = (seed &* 1103515245 &+ 12345) & 0x7fffffff; return CGFloat(seed % 1000) / 1000 }
        for r in 0..<rows {
            let y = top + step * (CGFloat(r) + 0.5)
            var words: [CGFloat] = []
            var used: CGFloat = 0
            while used < width { let w = width * (0.05 + 0.11 * next()); words.append(w); used += w + gap }
            let scale = (width - gap * CGFloat(words.count - 1)) / words.reduce(0, +)
            var x = right
            for w0 in words {
                let w = w0 * scale
                path.addRoundedRect(in: CGRect(x: x - w, y: y - h / 2, width: w, height: h), cornerWidth: h / 2, cornerHeight: h / 2)
                x -= w + gap
            }
        }
        return path
    }

    // MARK: الهبوط

    /// الصفحة المقصودة (القارئ نفسه) تحل محل الأوراق وتهبط إلى مكانها، ويمرّ الضوء الذهبي ويتوهج رقمها، ونقرة النجاح
    private func beginLanding() {
        landed = true
        // اللمس يعود إلى القارئ مع بدء الهبوط
        stage.isUserInteractionEnabled = false
        clearLeaves()
        starts.forEach { $0.removeFromSuperview() }
        ends.forEach { $0.removeFromSuperview() }
        book.isHidden = true
        if let p = pages {
            p.isHidden = false
            p.transform = reduce || glowOnly ? .identity : CGAffineTransform(scaleX: 0.92, y: 0.92)
        }
        success.notificationOccurred(.success)
        // الضوء والتوهج فوق الصفحة المقصودة، ويصغران ويكبران معها
        landing.frame = stage.bounds
        landing.isUserInteractionEnabled = false
        stage.insertSubview(landing, at: 0)
        // في الكتاب المفتوح يتوهج رقم الصفحة المقصودة نفسها: اليمنى إن كانت فردية
        let toPage = j.pages.last ?? (target + 1)
        let side: Bool? = state.isSpread ? (toPage % 2 == 1) : nil
        let card = cardRect(side)
        sweepBox.frame = card
        sweepBox.clipsToBounds = true
        sweepBox.layer.cornerRadius = Geo.corner
        sweep.colors = [UIColor.clear.cgColor, colors.gold.withAlphaComponent(0.28).cgColor, UIColor.white.withAlphaComponent(0.22).cgColor,
                        colors.gold.withAlphaComponent(0.28).cgColor, UIColor.clear.cgColor]
        sweep.startPoint = CGPoint(x: 0, y: 0.3)
        sweep.endPoint = CGPoint(x: 1, y: 0.7)
        sweep.frame = CGRect(x: -card.width, y: 0, width: card.width * 0.55, height: card.height)
        sweepBox.layer.addSublayer(sweep)
        sweepBox.isHidden = reduce
        landing.addSubview(sweepBox)
        let mc = CGPoint(x: card.midX, y: card.maxY - Geo.medallionCenter)
        glow.type = .radial
        glow.colors = [colors.gold.withAlphaComponent(0.55).cgColor, colors.gold.withAlphaComponent(0).cgColor]
        glow.startPoint = CGPoint(x: 0.5, y: 0.5)
        glow.endPoint = CGPoint(x: 1, y: 1)
        glow.frame = CGRect(x: mc.x - 40, y: mc.y - 40, width: 80, height: 80)
        glow.opacity = 0
        landing.layer.addSublayer(glow)
        glowRing.path = UIBezierPath(arcCenter: mc, radius: Geo.medallionSize / 2 + 3, startAngle: 0, endAngle: .pi * 2, clockwise: true).cgPath
        glowRing.fillColor = nil
        glowRing.strokeColor = colors.goldDeep.cgColor
        glowRing.lineWidth = 1.4
        glowRing.opacity = 0
        landing.layer.addSublayer(glowRing)
    }

    private func land(_ ms: Double) {
        let t = ms / 1000
        // ارتداد خفيف: من 0.92 إلى 1 بتذبذب يخمد سريعاً
        let s: CGFloat = reduce || glowOnly ? 1 : (ms >= Double(j.land) ? 1 : 1 - 0.08 * CGFloat(exp(-7 * t) * cos(2 * .pi * 1.8 * t)))
        pages?.transform = CGAffineTransform(scaleX: s, y: s)
        landing.transform = CGAffineTransform(scaleX: s, y: s)
        // الضوء يعبر الورقة مرة
        let sp = CGFloat(min(1, ms / 720))
        let e = sp < 0.5 ? 2 * sp * sp : 1 - pow(-2 * sp + 2, 2) / 2
        let bw = sweepBox.bounds.width
        sweep.frame.origin.x = -bw * 0.55 + (bw * 1.55) * e
        sweep.opacity = Float(min(1, sp / 0.15) * min(1, (1 - sp) / 0.2))
        // رقم الصفحة يتوهج ثم يخبو
        let g: Double = ms < 200 ? ms / 200 : max(0, 1 - (ms - 200) / 680)
        glow.opacity = Float(g)
        glowRing.opacity = Float(g * 0.7)
        let rs = CGFloat(1 + 0.25 * (1 - g))
        glowRing.setAffineTransform(CGAffineTransform(translationX: glowRing.path?.boundingBox.midX ?? 0, y: glowRing.path?.boundingBox.midY ?? 0)
            .scaledBy(x: rs, y: rs).translatedBy(x: -(glowRing.path?.boundingBox.midX ?? 0), y: -(glowRing.path?.boundingBox.midY ?? 0)))
        // الميدالية تنبض مرة ثم تختفي
        if let b = badge {
            let pulse: CGFloat = ms < 150 ? 1 + 0.12 * CGFloat(ms / 150) : (ms < 300 ? 1.12 - 0.12 * CGFloat((ms - 150) / 150) : 1)
            b.transform = CGAffineTransform(scaleX: pulse, y: pulse)
            b.alpha = ms < 300 ? 1 : CGFloat(max(0, 1 - (ms - 300) / 260))
            if let last = j.pages.last { b.setValue(Double(last)); b.setJuz(juzName(Double(last)), up: j.forward) }
        }
        if ms >= landMs { finish() }
    }

    /// ينهي الرحلة (عند اكتمالها، أو حين يطلب التطبيق انتقالاً آخر أثناءها): القارئ بمقاسه، وتُزال الطبقات.
    /// [notify]: الرحلة اكتملت فيُبلَّغ القارئ باستقرار موضعه
    func finish(notify: Bool = true) {
        guard !finished else { return }
        finished = true
        link?.invalidate()
        link = nil
        pages?.isHidden = false
        pages?.transform = .identity
        stage.removeFromSuperview()
        if notify { host?.journeyEnded(self) }
    }
}
