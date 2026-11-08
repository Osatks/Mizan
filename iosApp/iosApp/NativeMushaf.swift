import UIKit
import CoreText
import Shared

/// قارئ المصحف من النظام: صفحات مصحف المدينة (طبعة مجمع الملك فهد) يقلّبها UICollectionView ويرسمها Core Text،
/// بدل HorizontalPager المرسوم بـ Compose (Skia). التقليب والانسياب والارتداد من UIKit نفسه بمعدل الشاشة (120 هرتز)،
/// والصفحة طبقة مرسومة مرة واحدة لا تُعاد مع كل إطار من السحب.
///
/// الصفحة نفسها التي يرسمها Compose (MadinahPage.kt و MushafStyle.kt): الأسطر الخمسة عشر كما في المطبوع بخط الصفحة،
/// والورقة والإطار المذهّب ورأس الصفحة ورقمها وشريط العلامة، والتظليل وأشرطة الحفظ. البيانات من الملفات نفسها
/// (assets/madinah، وخطوط الصفحات التي ينزّلها Kotlin)، والحالة من Kotlin (NativeMushaf.ios.kt) مع الإشعار MizanMushafState،
/// واللمس يعود إليه كما من صفحة Compose. ما فوق الصفحات (الرأس، شريط خيارات الآية، الأوراق) يبقى من Compose فوق القارئ.
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

/// يرسم سطراً Core Text مكبَّراً بـ [scale] وخط أساسه عند (x, y) في سياق UIKit (المحور الرأسي إلى الأسفل)
private func paint(_ line: CTLine, _ ctx: CGContext, x: CGFloat, y: CGFloat, scale: CGFloat = 1, color: UIColor) {
    ctx.saveGState()
    ctx.setFillColor(color.cgColor)
    ctx.textMatrix = CGAffineTransform(scaleX: scale, y: -scale)
    ctx.textPosition = CGPoint(x: x, y: y)
    CTLineDraw(line, ctx)
    ctx.restoreGState()
}

// MARK: - حساب الصفحة (كما في prepareMadinah، MadinahPage.kt)

struct PlacedGlyph {
    let line: CTLine   // بمقاس 100، ويُرسم مكبَّراً بمقاس الصفحة
    let left: CGFloat
    let right: CGFloat
    let ayah: Int
    let end: Bool
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
        var measured: [Int: [(CTLine, CGFloat)]] = [:]
        for (i, l) in lines.enumerated() {
            if case .ayah(_, let glyphs, _, _) = l {
                measured[i] = glyphs.map { (g: String) -> (CTLine, CGFloat) in
                    let line = ctLine(g, f100)
                    return (line, lineWidth(line))
                }
            }
        }
        func natural(_ i: Int) -> CGFloat { measured[i]?.reduce(0) { $0 + $1.1 } ?? 0 }
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
                let total = ws.reduce(0) { $0 + $1.1 } * k
                let centered = c || special || glyphs.count == 1
                let gap = centered ? 0 : (lineW - total) / CGFloat(glyphs.count - 1)
                var right = centered ? x0 + (lineW + total) / 2 : x0 + lineW
                var placed: [PlacedGlyph] = []
                placed.reserveCapacity(glyphs.count)
                for j in glyphs.indices {
                    let ww = ws[j].1 * k
                    placed.append(PlacedGlyph(line: ws[j].0, left: right - ww, right: right, ayah: ayahs[j], end: ends.contains(j)))
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
            path.addRoundedRect(in: CGRect(x: x0 - 4, y: top + height * 0.06, width: x1 - x0 + 8, height: height * 0.88),
                                cornerWidth: 10, cornerHeight: 10)
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
}

// MARK: - الرسم

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
    /// عرض طرف الصفحة الذي يقلبها لمسُه في وضع التركيز
    static let edge: CGFloat = 0.14
}

/// ورقة الصفحة وإطارها ورأسها ورقمها وشريط العلامة: تُرسم مرة لكل صفحة (لا مع التقليب)
final class PageChromeView: UIView {
    var page = 1
    var colors: MushafColors?
    var title = "", subtitle = "", digits = ""
    var read = false
    var marked = false

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
        if l.capped, let first = l.rows.first, let last = l.rows.last {
            drawCappedFrame(ctx, l, first: first, last: last, p)
        }
        let k = l.textSize / MadinahLayout.ref
        for row in l.rows {
            switch row {
            case .text(let top, let height, let words):
                let y = top + height / 2 + l.textSize * 0.3
                for g in words {
                    if hifz && !g.end && !revealed.contains(g.ayah) {
                        // الكلمة المخفية للحفظ: شريط لين بطولها
                        let inset = l.textSize * 0.04, bh = l.textSize * 0.62
                        roundRect(ctx, CGRect(x: g.left + inset, y: y - l.textSize * 0.52, width: max(g.right - g.left - 2 * inset, 1), height: bh),
                                  radius: bh / 2, fill: p.ink.withAlphaComponent(0.16))
                    } else {
                        paint(g.line, ctx, x: g.left, y: y, scale: k, color: g.end ? p.accent : p.ink)
                    }
                }
            case .banner(let top, let height, let surah):
                guard let hf = MadinahStore.shared.headerFont else { continue }
                let code = MadinahStore.shared.headerCode(surah)
                if code.isEmpty { continue }
                let line = ctLine(code, CTFontCreateWithGraphicsFont(hf, MadinahLayout.ref, nil, nil))
                let gw = max(lineWidth(line), 1)
                // الإطار ممتد إلى طرفي الصفحة (وفي الفاتحة وأول البقرة بعرض أسطرهما)، ولا يتجاوز ارتفاع السطر إلا قليلاً
                let ts = min(MadinahLayout.ref * (l.capped ? l.lineW : w) / gw, height * 1.12 / 0.39)
                let s = ts / MadinahLayout.ref
                paint(line, ctx, x: (w - gw * s) / 2, y: top + height / 2, scale: s, color: p.accent)
            case .basmala(let top, let height):
                guard let bf = MadinahStore.shared.basmalaFont else { continue }
                let line = ctLine(MadinahStore.basmala, CTFontCreateWithGraphicsFont(bf, MadinahLayout.ref, nil, nil))
                let bw = lineWidth(line)
                var size = l.textSize * 1.02
                if bw * size / MadinahLayout.ref > l.lineW * 0.75 { size = l.lineW * 0.75 * MadinahLayout.ref / bw }
                let s = size / MadinahLayout.ref
                paint(line, ctx, x: (w - bw * s) / 2, y: top + height / 2 + size * 0.32, scale: s, color: p.ink)
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

    private let card = UIView()
    private let chrome = PageChromeView()
    private let textArea = UIView()
    private let selectLayer = CAShapeLayer()
    private let pressLayer = CAShapeLayer()
    private let glyphs = PageGlyphView()
    private let spinner = UIActivityIndicatorView(style: .medium)
    private let note = UILabel()
    private let retry = UIButton(type: .system)
    let veil = UIView()

    private var layoutSize: CGSize = .zero
    private var shownSelection = -2
    private var pressTimer: Timer?
    private var pressStart: CGPoint?

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
        card.addSubview(textArea)
        textArea.isUserInteractionEnabled = false
        selectLayer.opacity = 1
        textArea.layer.addSublayer(selectLayer)
        textArea.layer.addSublayer(pressLayer)
        pressLayer.opacity = 0
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

    private var bottomInset: CGFloat { CGFloat(host?.state?.bottom ?? 0) }

    /// مقاس منطقة الأسطر (لحساب الصفحات المجاورة مسبقاً بالمقاس نفسه)
    var textAreaSize: CGSize { layoutSize }

    override func layoutSubviews() {
        super.layoutSubviews()
        let cardFrame = CGRect(x: Geo.padSide, y: Geo.padTop, width: bounds.width - 2 * Geo.padSide,
                               height: bounds.height - Geo.padTop - Geo.padBottom - bottomInset)
        if card.frame != cardFrame {
            card.frame = cardFrame
            layer.shadowPath = UIBezierPath(roundedRect: cardFrame, cornerRadius: Geo.corner).cgPath
        }
        chrome.frame = card.bounds
        veil.frame = card.bounds
        let top = Geo.colTop + Geo.headH + Geo.ruleH + Geo.canvasTop
        let area = CGRect(x: Geo.colSide, y: top, width: card.bounds.width - 2 * Geo.colSide,
                          height: card.bounds.height - top - Geo.colBottom - Geo.canvasBottom)
        textArea.frame = area
        glyphs.frame = textArea.bounds
        spinner.center = CGPoint(x: area.midX, y: area.midY - 12)
        note.frame = CGRect(x: area.minX, y: area.midY + 4, width: area.width, height: 40)
        retry.sizeToFit()
        retry.center = CGPoint(x: area.midX, y: area.midY + 60)
        if area.size != layoutSize { layoutSize = area.size; refreshText(animated: false) }
    }

    /// يعرض الصفحة [p] (عند إعادة استعمال الخلية) أو يحدّثها بالحالة الجديدة
    func show(page p: Int) {
        let changed = p != page
        page = p
        if changed {
            shownSelection = -2
            glyphs.layout = nil
            cancelPress()
        }
        refreshChrome()
        refreshText(animated: false)
        updateSelection(animated: false)
    }

    func refreshChrome() {
        guard let host = host, let s = host.state, page > 0 else { return }
        chrome.page = page
        chrome.colors = host.colors
        chrome.title = host.session.title(page: Int32(page))
        chrome.subtitle = host.session.subtitle(page: Int32(page))
        chrome.digits = host.session.digits(page: Int32(page))
        chrome.read = s.isRead(page)
        chrome.marked = s.marked.contains(page)
        chrome.setNeedsDisplay()
        veil.backgroundColor = host.colors?.backdrop
        note.textColor = host.colors?.muted
        retry.tintColor = host.colors?.accent
        spinner.color = host.colors?.muted
    }

    /// الأسطر: من الذاكرة إن حُسبت، وإلا تُحسب الآن (والخط إن لم يكن في الجهاز يُطلب، ويظهر مؤشر التنزيل)
    func refreshText(animated: Bool) {
        guard let host = host, let s = host.state, page > 0, layoutSize.width > 0, layoutSize.height > 0 else { return }
        let path = host.session.fontPath(page: Int32(page))
        let l = LayoutCache.shared.obtain(page, layoutSize, fontPath: path)
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
    }

    /// تظليل الآية المختارة: الشريط الجديد يظهر والسابق يتلاشى
    func updateSelection(animated: Bool) {
        guard let host = host, let s = host.state else { return }
        let sel = s.selected
        if sel == shownSelection { return }
        shownSelection = sel
        selectLayer.fillColor = host.colors?.select.cgColor
        let path = glyphs.layout?.band(sel)
        if animated && !UIAccessibility.isReduceMotionEnabled {
            let fade = CATransition()
            fade.type = .fade
            fade.duration = 0.28
            selectLayer.add(fade, forKey: "fade")
        }
        CATransaction.begin()
        if !animated { CATransaction.setDisableActions(true) }
        selectLayer.path = path
        CATransaction.commit()
    }

    // MARK: اللمس

    private func ayah(at point: CGPoint) -> Int {
        guard let l = glyphs.layout else { return -1 }
        let p = convert(point, to: textArea)
        guard p.y >= -l.lineH, p.y <= textArea.bounds.height + l.lineH else { return -1 }
        return l.hit(p)
    }

    @objc private func tapped(_ g: UITapGestureRecognizer) {
        guard let host = host, let s = host.state else { return }
        let pt = g.location(in: card)
        // في وضع التركيز: لمس طرف الصفحة يقلبها، الأيسر إلى التالية والأيمن إلى السابقة كالكتاب العربي
        if s.focus && card.bounds.width > 0 {
            if pt.x < card.bounds.width * Geo.edge { host.session.edge(dir: 1); return }
            if pt.x > card.bounds.width * (1 - Geo.edge) { host.session.edge(dir: -1); return }
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
        guard touches.count == 1, let t = touches.first else { return }
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

final class MushafPageCell: UICollectionViewCell {
    static let id = "page"
    let pageView = MushafPageView()

    override init(frame: CGRect) {
        super.init(frame: frame)
        contentView.addSubview(pageView)
    }
    required init?(coder: NSCoder) { fatalError() }

    /// عرض الصفحة: الشاشة كلها في الهاتف، وفي الشاشات العريضة صفحة بنسبة المصحف المطبوع في الوسط
    var frameWidth: CGFloat = 0 { didSet { if frameWidth != oldValue { setNeedsLayout() } } }

    override func layoutSubviews() {
        super.layoutSubviews()
        let w = frameWidth > 0 ? min(frameWidth, contentView.bounds.width) : contentView.bounds.width
        let size = CGSize(width: w, height: contentView.bounds.height)
        // bounds و center لا frame: الصفحة قد تكون مصغَّرة أثناء التقليب (transform)
        if pageView.bounds.size != size { pageView.bounds = CGRect(origin: .zero, size: size) }
        let c = CGPoint(x: contentView.bounds.midX, y: contentView.bounds.midY)
        if pageView.center != c { pageView.center = c }
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
    private let count = 604
    /// آخر انتقال طلبه التطبيق وطُبِّق
    private var appliedSeq = -1
    /// موضع الصفحة الظاهرة (رقمها ناقص واحد)
    private var current = 0
    private var laidOutSize: CGSize = .zero
    /// الصفحات أُنشئت: عُرف المقاس ووصلت الحالة
    private var loaded = false
    private var edgeLinked = false

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
            // تغيّر المقاس (تدوير iPad، Split View): الصفحة نفسها تبقى ظاهرة
            flow.invalidateLayout()
            pages.layoutIfNeeded()
            scroll(to: current, animated: false)
        } else {
            loadIfReady()
        }
    }

    /// الصفحات تُنشأ بعد أن يُعرف مقاسها وتصل الحالة (لا خلايا صغيرة تُحسب ثم تُرمى، ولا الصفحة الأولى قبل صفحة القارئ)
    private func loadIfReady() {
        guard !loaded, laidOutSize != .zero, state != nil else { return }
        loaded = true
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
        if old == nil {
            current = s.jump
            appliedSeq = s.seq
            loadIfReady()
            return
        }
        let geometry = old.map { $0.bottom != s.bottom || $0.frame != s.frame } ?? false
        for cell in pages.visibleCells.compactMap({ $0 as? MushafPageCell }) {
            cell.frameWidth = CGFloat(s.frame)
            let v = cell.pageView
            if geometry { v.setNeedsLayout(); v.layoutIfNeeded() }
            if old == nil || colorsChanged || old?.marked != s.marked || old?.read != s.read { v.refreshChrome() }
            if old == nil || colorsChanged || old?.hifz != s.hifz || old?.revealed != s.revealed || old?.fonts != s.fonts || old?.failed != s.failed {
                v.refreshText(animated: old != nil)
            }
            if old?.selected != s.selected || colorsChanged { v.updateSelection(animated: !colorsChanged) }
        }
        // انتقال طلبه التطبيق (التلاوة، القفز إلى صفحة، طرف الصفحة في وضع التركيز)
        if s.seq != appliedSeq, loaded {
            appliedSeq = s.seq
            if s.jump != current || pages.isDragging || pages.isDecelerating {
                let near = abs(s.jump - current) <= 2
                scroll(to: s.jump, animated: s.animate && near && view.window != nil && !UIAccessibility.isReduceMotionEnabled)
            }
        }
    }

    private func scroll(to index: Int, animated: Bool) {
        let i = max(0, min(count - 1, index))
        current = i
        guard pages.numberOfItems(inSection: 0) > i else { return }
        pages.scrollToItem(at: IndexPath(item: i, section: 0), at: .centeredHorizontally, animated: animated)
        if !animated {
            pages.layoutIfNeeded()
            updateDepth()
            session.prefetch(page: Int32(i + 1))
        }
    }

    // MARK: الصفحات

    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int { loaded ? count : 0 }

    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: MushafPageCell.id, for: indexPath) as! MushafPageCell
        cell.frameWidth = CGFloat(state?.frame ?? 0)
        cell.pageView.host = self
        cell.pageView.show(page: indexPath.item + 1)
        cell.setNeedsLayout()
        cell.layoutIfNeeded()
        return cell
    }

    func collectionView(_ collectionView: UICollectionView, prefetchItemsAt indexPaths: [IndexPath]) {
        // أسطر الصفحات القادمة تُحسب في الخلفية بمقاس الصفحة الظاهرة، فلا يُحسب شيء لحظة وصولها
        guard let visible = (pages.visibleCells.first as? MushafPageCell)?.pageView else { return }
        let size = visible.textAreaSize
        guard size.width > 0 else { return }
        for ip in indexPaths {
            let p = ip.item + 1
            if session.hasFont(page: Int32(p)) { LayoutCache.shared.prefetch(p, size, fontPath: session.fontPath(page: Int32(p))) }
            else { session.requestFont(page: Int32(p)) }
        }
    }

    func collectionView(_ collectionView: UICollectionView, willDisplay cell: UICollectionViewCell, forItemAt indexPath: IndexPath) {
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
        session.prefetch(page: Int32(ip.item + 1))
    }

    /// الصفحة المغادرة تنحسر قليلاً وتخبو نحو لون ما حولها، كما في المصحف بـ Compose
    private func updateDepth() {
        let w = pages.bounds.width
        guard w > 0 else { return }
        let mid = pages.bounds.midX
        for cell in pages.visibleCells.compactMap({ $0 as? MushafPageCell }) {
            let away = min(abs(cell.center.x - mid) / w, 1)
            let sc = 1 - 0.06 * away
            cell.pageView.transform = CGAffineTransform(scaleX: sc, y: sc)
            cell.pageView.veil.alpha = 0.4 * away
        }
    }
}
