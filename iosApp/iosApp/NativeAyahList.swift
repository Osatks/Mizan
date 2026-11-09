import SwiftUI
import UIKit
import CoreText
import Shared

/// عرض آية آية في صفحة السورة بواجهة النظام (SwiftUI) بدل LazyColumn من Compose: ترويسة السورة المذهّبة والبسملة،
/// ثم الآيات بطاقةً بطاقة على ورق المصحف: نص الآية بخط المصحف بالمقاس الذي اختاره القارئ تنتهي بعلامة ۝ ورقمها،
/// وتحتها ترجمة المعاني، والعلامة الذهبية للآية المحفوظة، والآية المختارة أو المتلوة مظلَّلة، وبين الآيات زخرفة رفيعة،
/// وفي آخرها الانتقال إلى السورة السابقة والتالية — كما في SurahScreen.kt.
/// القائمة كسولة (LazyVStack): تُنشأ الآية حين تقترب من الشاشة ونصها يُقرأ من Kotlin حينها، فتنساب البقرة (286 آية) بلا ثقل.
/// الحالة من Kotlin (NativeAyahList.ios.kt) مع الإشعار MizanAyahListState؛ وأول آية ظاهرة واللمس والضغط المطوّل تعود إليه.
/// ما فوق القائمة (الرأس، شريط خيارات الآية، المشغّل، اللوحات) يبقى من Compose فوقها.
enum NativeAyahListScreen {
    /// يُستدعى مرة عند التشغيل، قبل إنشاء MainViewController
    static func install() {
        // خط المصحف (Amiri Quran) من الملف نفسه الذي يستعمله Compose؛ تسجيله مرة ثانية لا يضر
        if let root = Bundle.main.resourcePath {
            let url = URL(fileURLWithPath: root + "/assets/fonts/amiri_quran.ttf")
            _ = CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
        }
        NativeAyahList.shared.attach { session in AyahListHostController(session: session) }
    }
}

final class AyahListHostController: UIHostingController<AyahListRoot> {
    init(session: NativeAyahListSession) {
        let model = AyahListModel(session: session)
        super.init(rootView: AyahListRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        // الهوامش من Kotlin لا من المنطقة الآمنة لهذا المتحكم
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct AyahListState: Decodable {
    struct Nav: Decodable { let surah: Int; let label, name: String }

    let surah, count, base: Int
    let title: String
    let basmala: String?
    let subtitle: String
    /// رقم الآية المختارة في هذه السورة، أو 0
    let selected: Int
    let marks: [Int]
    let size: Double
    let tr, trRtl: Bool
    let trKey: String
    let jump: Int
    let animate, follow: Bool
    let seq: Int
    /// الهامش السفلي للمحتوى، وما يغطي أسفل القائمة (شريط النظام والمشغّل)، وهامشا الجانبين
    let bottom, cover, start, end: Double
    let rtl, dark: Bool
    let prev, next: Nav?
    let hint, tafsir, save, unsave: String
    let paper, paperEdge, ink, muted, gold, goldDeep, fill, fillDeep, marker, select, accent, outline: String

    static func decode(_ json: String) -> AyahListState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(AyahListState.self, from: data)
    }
}

/// ألوان ورق المصحف كما في MushafPalette (بلون الورق الذي اختاره القارئ)
struct AyahListColors: Equatable {
    let paper, paperEdge, ink, muted, gold, goldDeep, fill, fillDeep, select, accent, outline: Color
    let markerUI, inkUI: UIColor
    /// مفتاح ألوان علامة الآية في ذاكرة صورها
    let markKey: String

    init(_ s: AyahListState) {
        paper = HomePalette.color(s.paper); paperEdge = HomePalette.color(s.paperEdge)
        ink = HomePalette.color(s.ink); muted = HomePalette.color(s.muted)
        gold = HomePalette.color(s.gold); goldDeep = HomePalette.color(s.goldDeep)
        fill = HomePalette.color(s.fill); fillDeep = HomePalette.color(s.fillDeep)
        select = HomePalette.color(s.select); accent = HomePalette.color(s.accent); outline = HomePalette.color(s.outline)
        markerUI = ayahListUIColor(s.marker); inkUI = ayahListUIColor(s.ink)
        markKey = s.marker + s.ink
    }
}

/// "#AARRGGBB"
private func ayahListUIColor(_ hex: String) -> UIColor {
    let v = UInt32(hex.dropFirst(), radix: 16) ?? 0xFF000000
    return UIColor(red: CGFloat((v >> 16) & 0xFF) / 255, green: CGFloat((v >> 8) & 0xFF) / 255,
                   blue: CGFloat(v & 0xFF) / 255, alpha: CGFloat((v >> 24) & 0xFF) / 255)
}

/// نص الآية وترجمتها ورقمها (من Kotlin مرة لكل آية تظهر)
struct AyahListText {
    let words: String
    let tr: String?
    let digits: String
}

/// حدود عنصر من القائمة في إطار التمرير (الأعلى والأسفل)
struct AyahListSpan: Equatable {
    let minY, maxY: CGFloat
}

struct AyahListSpansKey: PreferenceKey {
    static var defaultValue: [Int: AyahListSpan] = [:]
    static func reduce(value: inout [Int: AyahListSpan], nextValue: () -> [Int: AyahListSpan]) {
        value.merge(nextValue()) { $1 }
    }
}

final class AyahListModel: ObservableObject {
    let session: NativeAyahListSession
    @Published private(set) var state: AyahListState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    // التمرير: لا يُبلَّغ Kotlin بموضع القائمة قبل أن تعود إلى موضعها المحفوظ (وإلا حُفظ أعلاها بدل موضع القارئ)
    private var lastSeq = -1
    private var ready = false
    private var spans: [Int: AyahListSpan] = [:]
    private var height: CGFloat = 0
    private var reported = -1
    private var upId = -1
    private var upY: CGFloat = 0
    private var upRun: CGFloat = 0

    // نصوص الآيات الظاهرة (تُفرَّغ حين تتغير السورة أو الترجمة)
    private var texts: [Int: AyahListText] = [:]
    private var textsKey = ""

    init(session: NativeAyahListSession) {
        self.session = session
        state = AyahListState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanAyahListState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = AyahListState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    func text(_ s: AyahListState, _ ayah: Int) -> AyahListText {
        let key = "\(s.surah)|\(s.trKey)"
        if key != textsKey { texts = [:]; textsKey = key }
        if let t = texts[ayah] { return t }
        let i = Int32(s.base + ayah - 1)
        let t = AyahListText(
            words: session.ayahText(index: i),
            tr: s.tr ? session.translation(index: i) : nil,
            digits: session.digits(n: Int32(ayah))
        )
        texts[ayah] = t
        return t
    }

    func tap(_ index: Int) { session.tap(index: Int32(index)) }
    func longPress(_ index: Int) { session.longPress(index: Int32(index)) }
    func tafsir(_ index: Int) { session.tafsir(index: Int32(index)) }
    func bookmark(_ index: Int) { session.bookmark(index: Int32(index)) }
    func openSurah(_ n: Int) { session.openSurah(surah: Int32(n)) }

    /// حين تُنشأ القائمة (أو سورة جديدة فيها): تعود إلى الموضع الذي يحفظه Kotlin بلا حركة
    func restore(_ proxy: ScrollViewProxy) {
        ready = false
        reported = -1
        upId = -1
        upRun = 0
        lastSeq = state?.seq ?? -1
        let top = Int(session.top())
        DispatchQueue.main.async { [weak self] in
            if top > 0 { proxy.scrollTo(top, anchor: .top) }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) { self?.ready = true }
        }
    }

    /// انتقال طلبه التطبيق (seq جديد): إلى أعلى العنصر، أو اتباع الآية المتلوة إن لم تكن ظاهرة كلها
    func apply(_ proxy: ScrollViewProxy) {
        guard let s = state, s.seq != lastSeq else { return }
        lastSeq = s.seq
        let target = s.jump
        if s.follow, let sp = spans[target], sp.minY >= 0, sp.maxY <= height - CGFloat(s.cover) { return }
        let animate = s.animate && !UIAccessibility.isReduceMotionEnabled
        DispatchQueue.main.async {
            if animate {
                withAnimation(.easeInOut(duration: 0.4)) { proxy.scrollTo(target, anchor: .top) }
            } else {
                proxy.scrollTo(target, anchor: .top)
            }
        }
    }

    /// مع كل إطار تمرير: أول عنصر ظاهر (يُبلَّغ حين يتغير فقط)، والتمرير إلى الأعلى (أدوات وضع التركيز).
    /// لا يغيّر شيئاً منشوراً، فلا يُعاد بناء القائمة مع كل إطار
    func track(_ v: [Int: AyahListSpan], height h: CGFloat) {
        spans = v
        height = h
        guard ready, let s = state, !v.isEmpty else { return }
        var first = Int.max
        for (k, sp) in v where sp.maxY > 1 && k < first { first = k }
        if first != Int.max {
            let item = min(first, s.count)
            if item != reported {
                reported = item
                session.scrolled(first: Int32(item))
            }
        }
        if let sp = v[upId] {
            let d = sp.minY - upY
            upY = sp.minY
            if d > 0 { upRun += d } else if d < 0 { upRun = 0 }
            if upRun > 40 {
                upRun = 0
                session.scrolledUp()
            }
        } else if first != Int.max, let sp = v[first] {
            upId = first
            upY = sp.minY
            upRun = 0
        }
    }
}

// MARK: - المقاسات وعلامة الآية

/// مقاس خط المصحف يتبع حجم النص في الإعدادات (كما تتبعه sp في Compose)، وأقصاه 1.8 ضعفاً
private func ayahListScaled(_ size: CGFloat) -> CGFloat {
    min(UIFontMetrics(forTextStyle: .body).scaledValue(for: size), size * 1.8)
}

/// ما يُضاف بين الأسطر ليكون ارتفاع السطر ضعفي المقاس تقريباً (lineHeight = 2.05 في quranStyle)
private func ayahListGap(_ size: CGFloat) -> CGFloat {
    let natural = UIFont(name: "AmiriQuran-Regular", size: size)?.lineHeight ?? size * 1.6
    return max(0, size * 2.05 - natural)
}

private func ayahListQuran(_ size: CGFloat) -> Font { .custom("AmiriQuran-Regular", fixedSize: size) }

/// علامة نهاية الآية: ۝ بخط المصحف بلون العلامة وفي وسطها رقم الآية بلون الحبر (drawAyahEnd في MushafText.kt)،
/// صورةً داخل نص الآية تجلس على سطرها كما يضعها الخط. تُرسم مرة لكل رقم ومقاس ولون.
enum AyahListMarks {
    struct Mark {
        let image: Image
        /// إزاحة الصورة تحت خط الأساس (سالبة): الحلقة تتدلى أسفله
        let drop: CGFloat
    }

    private static var cache: [String: Mark] = [:]
    // مركز الحلقة في الخط نسبةً إلى المقاس، كما في MushafText.kt (RING_CX و RING_CY)
    private static let ringX: CGFloat = 0.64
    private static let ringY: CGFloat = 0.166

    static func mark(ayah: Int, digits: String, size: CGFloat, c: AyahListColors) -> Mark {
        let key = "\(ayah)|\(Int(size * 10))|\(c.markKey)"
        if let m = cache[key] { return m }
        if cache.count > 1200 { cache.removeAll() }
        let font = CTFontCreateWithName("AmiriQuran-Regular" as CFString, size, nil)
        let ring = line("\u{06DD}", font)
        let rb = CTLineGetBoundsWithOptions(ring, .useGlyphPathBounds)
        let numFont = CTFontCreateWithName("AmiriQuran-Regular" as CFString, size * (ayah >= 100 ? 0.38 : 0.5), nil)
        let num = line(digits, numFont)
        let nb = CTLineGetBoundsWithOptions(num, .useGlyphPathBounds)
        let x0 = size * 0.02
        // حدود الصورة فوق خط الأساس وتحته (محور Core Text إلى الأعلى)
        let top = ceil(max(rb.maxY, size * 0.72)) + 1
        let bottom = floor(min(rb.minY, 0)) - 1
        let width = ceil(max(size * 1.32, x0 + rb.maxX + 1))
        let format = UIGraphicsImageRendererFormat.preferred()
        format.opaque = false
        let ui = UIGraphicsImageRenderer(size: CGSize(width: width, height: top - bottom), format: format).image { r in
            let ctx = r.cgContext
            // خط الأساس على بعد top من أعلى الصورة، والمحور الرأسي إلى الأعلى كما في Core Text
            ctx.translateBy(x: 0, y: top)
            ctx.scaleBy(x: 1, y: -1)
            ctx.textMatrix = .identity
            ctx.setFillColor(c.markerUI.cgColor)
            ctx.textPosition = CGPoint(x: x0, y: 0)
            CTLineDraw(ring, ctx)
            ctx.setFillColor(c.inkUI.cgColor)
            ctx.textPosition = CGPoint(x: x0 + size * ringX - nb.midX, y: size * ringY - nb.midY)
            CTLineDraw(num, ctx)
        }
        let image: Image
        if let cg = ui.cgImage { image = Image(decorative: cg, scale: ui.scale) } else { image = Image(uiImage: ui) }
        let m = Mark(image: image, drop: bottom)
        cache[key] = m
        return m
    }

    /// سطر Core Text يأخذ لونه من السياق عند الرسم
    private static func line(_ text: String, _ font: CTFont) -> CTLine {
        let attrs: [NSAttributedString.Key: Any] = [
            NSAttributedString.Key(kCTFontAttributeName as String): font,
            NSAttributedString.Key(kCTForegroundColorFromContextAttributeName as String): true,
        ]
        return CTLineCreateWithAttributedString(NSAttributedString(string: text, attributes: attrs))
    }
}

// MARK: - الشاشة

struct AyahListRoot: View {
    @ObservedObject var model: AyahListModel
    var body: some View {
        Group {
            if let s = model.state { AyahListView(s: s, c: AyahListColors(s), model: model) } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

struct AyahListView: View {
    let s: AyahListState
    let c: AyahListColors
    let model: AyahListModel
    /// حجم النص في الإعدادات: تغيّره يعيد حساب المقاسات
    @Environment(\.sizeCategory) private var sizeCategory

    var body: some View {
        let size = ayahListScaled(CGFloat(s.size))
        let gap = ayahListGap(size)
        let marks = Set(s.marks)
        GeometryReader { outer in
            ScrollViewReader { proxy in
                ScrollView(.vertical) {
                    LazyVStack(spacing: 0) {
                        AyahListBanner(s: s, c: c)
                            .id(0)
                            .background(AyahListSpanReader(id: 0))
                        ForEach(1..<(s.count + 1), id: \.self) { a in
                            let t = model.text(s, a)
                            AyahListRow(
                                ayah: a, index: s.base + a - 1, text: t, size: size, gap: gap,
                                selected: s.selected == a, marked: marks.contains(a), trRtl: s.trRtl, last: a == s.count,
                                c: c, hint: s.hint, tafsir: s.tafsir, mark: marks.contains(a) ? s.unsave : s.save, model: model
                            )
                            .equatable()
                            .id(a)
                            .background(AyahListSpanReader(id: a))
                        }
                        AyahListNav(s: s, c: c, model: model)
                            .id(s.count + 1)
                    }
                    .padding(.leading, CGFloat(s.start))
                    .padding(.trailing, CGFloat(s.end))
                    .padding(.top, 16)
                    .padding(.bottom, CGFloat(s.bottom))
                }
                // سورة أخرى: قائمة جديدة من أعلاها (أو من الآية المطلوبة)
                .id(s.surah)
                .coordinateSpace(name: "ayahList")
                .onPreferenceChange(AyahListSpansKey.self) { v in model.track(v, height: outer.size.height) }
                .onAppear { model.restore(proxy) }
                .onChange(of: s.seq) { _ in model.apply(proxy) }
            }
        }
        .background(AyahListPaper(c: c))
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }
}

/// يبلغ حدود العنصر في إطار التمرير
private struct AyahListSpanReader: View {
    let id: Int
    var body: some View {
        GeometryReader { g in
            let f = g.frame(in: .named("ayahList"))
            Color.clear.preference(key: AyahListSpansKey.self, value: [id: AyahListSpan(minY: f.minY, maxY: f.maxY)])
        }
    }
}

/// ورقة المصحف: تدرّج خفيف من الوسط إلى الحواف (drawPaper)
private struct AyahListPaper: View {
    let c: AyahListColors
    var body: some View {
        GeometryReader { g in
            RadialGradient(
                gradient: Gradient(stops: [
                    .init(color: c.paper, location: 0), .init(color: c.paper, location: 0.5), .init(color: c.paperEdge, location: 1),
                ]),
                center: .center, startRadius: 0, endRadius: max(g.size.width, g.size.height) * 0.75
            )
        }
        .ignoresSafeArea()
    }
}

// MARK: - الترويسة

/// ترويسة السورة: الإطار المذهّب باسمها، والبسملة، وتحتهما سطر الإرشاد
private struct AyahListBanner: View {
    let s: AyahListState
    let c: AyahListColors
    var body: some View {
        let h: CGFloat = 58
        let bas = ayahListScaled(26)
        VStack(spacing: 0) {
            ZStack {
                AyahListCartouche(c: c)
                Text(verbatim: s.title)
                    .font(ayahListQuran(h * 0.4))
                    .foregroundColor(c.goldDeep)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                    .padding(.horizontal, h * 1.36)
                    .offset(y: -h * 0.06)
                    .environment(\.layoutDirection, .rightToLeft)
            }
            .frame(maxWidth: .infinity)
            .frame(height: h)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text(verbatim: s.title))
            .accessibilityAddTraits(.isHeader)
            if let b = s.basmala {
                Text(verbatim: b)
                    .font(ayahListQuran(bas))
                    .lineSpacing(ayahListGap(bas))
                    .foregroundColor(c.ink)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 10)
                    .environment(\.layoutDirection, .rightToLeft)
            }
            Text(verbatim: s.subtitle)
                .font(sys(12))
                .foregroundColor(c.muted)
                .multilineTextAlignment(.center)
                .padding(.top, 4)
        }
        .frame(maxWidth: .infinity)
        .padding(.bottom, 10)
    }
}

/// إطار ترويسة السورة كما في المصاحف (drawCartouche في MushafStyle.kt): إطار مزدوج، وميداليتان بنجمة ثمانية،
/// ولوح مدبّب الطرفين في الوسط للاسم
private struct AyahListCartouche: View {
    let c: AyahListColors
    var body: some View {
        Canvas { ctx, size in
            let w = size.width, h = size.height
            let rect = CGRect(origin: .zero, size: size).insetBy(dx: 0.7, dy: 0.7)
            let r = h * 0.2
            let outer = Path(roundedRect: rect, cornerRadius: r)
            ctx.fill(outer, with: .linearGradient(Gradient(colors: [c.fill, c.fillDeep]), startPoint: .zero, endPoint: CGPoint(x: 0, y: h)))
            ctx.stroke(outer, with: .color(c.goldDeep), lineWidth: 1.4)
            ctx.stroke(Path(roundedRect: rect.insetBy(dx: 3, dy: 3), cornerRadius: max(r - 3, 1)), with: .color(c.gold), lineWidth: 0.7)
            let cy = h / 2, mr = h * 0.33
            let ends = [h * 0.56, w - h * 0.56]
            for cx in ends {
                let disc = Path(ellipseIn: CGRect(x: cx - mr, y: cy - mr, width: mr * 2, height: mr * 2))
                ctx.fill(disc, with: .color(c.paper))
                ctx.stroke(disc, with: .color(c.goldDeep), lineWidth: 1.2)
                let sr = mr * 0.74
                ctx.stroke(Star8().path(in: CGRect(x: cx - sr, y: cy - sr, width: sr * 2, height: sr * 2)), with: .color(c.gold), lineWidth: 1)
                let d = mr * 0.16
                ctx.fill(Path(ellipseIn: CGRect(x: cx - d, y: cy - d, width: d * 2, height: d * 2)), with: .color(c.goldDeep))
            }
            let x0 = h * 1.08, x1 = w - h * 1.08, ph = h * 0.62, tip = ph * 0.45
            var plate = Path()
            plate.move(to: CGPoint(x: x0, y: cy))
            plate.addLine(to: CGPoint(x: x0 + tip, y: cy - ph / 2))
            plate.addLine(to: CGPoint(x: x1 - tip, y: cy - ph / 2))
            plate.addLine(to: CGPoint(x: x1, y: cy))
            plate.addLine(to: CGPoint(x: x1 - tip, y: cy + ph / 2))
            plate.addLine(to: CGPoint(x: x0 + tip, y: cy + ph / 2))
            plate.closeSubpath()
            ctx.fill(plate, with: .color(c.paper))
            ctx.stroke(plate, with: .color(c.goldDeep), lineWidth: 1)
            let dot: CGFloat = 1.4
            for x in [(ends[0] + mr + x0) / 2, (ends[1] - mr + x1) / 2] {
                ctx.fill(Path(ellipseIn: CGRect(x: x - dot, y: cy - dot, width: dot * 2, height: dot * 2)), with: .color(c.goldDeep))
            }
        }
        .accessibilityHidden(true)
    }
}

// MARK: - الآية

/// آية: النص بخط المصحف تنتهي بعلامة ۝ ورقمها، وتحته ترجمة المعاني، على الورق بلا صناديق؛ المختارة تُظلَّل،
/// واللمس والضغط المطوّل إلى Kotlin (tapAyah و longPress في SurahScreen)
private struct AyahListRow: View, Equatable {
    let ayah: Int
    let index: Int
    let text: AyahListText
    let size: CGFloat
    let gap: CGFloat
    let selected: Bool
    let marked: Bool
    let trRtl: Bool
    let last: Bool
    let c: AyahListColors
    let hint: String
    let tafsir: String
    let mark: String
    let model: AyahListModel

    static func == (a: AyahListRow, b: AyahListRow) -> Bool {
        a.ayah == b.ayah && a.index == b.index && a.text.words == b.text.words && a.text.tr == b.text.tr
            && a.size == b.size && a.selected == b.selected && a.marked == b.marked && a.trRtl == b.trRtl
            && a.last == b.last && a.c == b.c && a.hint == b.hint && a.tafsir == b.tafsir && a.mark == b.mark
    }

    var body: some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 8) {
                if marked {
                    Image(systemName: "bookmark.fill")
                        .font(.system(size: 15))
                        .foregroundColor(c.goldDeep)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                        .accessibilityHidden(true)
                }
                quran
                if let tr = text.tr {
                    Text(verbatim: tr)
                        .font(sys(14))
                        .lineSpacing(5)
                        .foregroundColor(c.muted)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .fixedSize(horizontal: false, vertical: true)
                        .environment(\.layoutDirection, trRtl ? .rightToLeft : .leftToRight)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(selected ? c.select : Color.clear)
                    .animation(UIAccessibility.isReduceMotionEnabled ? nil : .easeInOut(duration: 0.32), value: selected)
            )
            .contentShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            // اللمس قبل الضغط المطوّل حتى يبقى التمرير فورياً، ولا يُعدّ الضغط المطوّل لمساً بعده
            .onTapGesture { model.tap(index) }
            .onLongPressGesture(minimumDuration: 0.45) { model.longPress(index) }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text(verbatim: label))
            .accessibilityHint(Text(verbatim: hint))
            .accessibilityAddTraits(traits)
            .accessibilityAction { model.tap(index) }
            .accessibilityAction(named: Text(verbatim: tafsir)) { model.tafsir(index) }
            .accessibilityAction(named: Text(verbatim: mark)) { model.bookmark(index) }
            if !last { AyahListOrnament(c: c).padding(.vertical, 2) }
        }
    }

    /// نص الآية وعلامة نهايتها: من اليمين دائماً مهما كانت لغة الواجهة
    private var quran: some View {
        let m = AyahListMarks.mark(ayah: ayah, digits: text.digits, size: size, c: c)
        return (Text(verbatim: text.words).foregroundColor(c.ink) + Text(verbatim: "\u{00A0}") + Text(m.image).baselineOffset(m.drop))
            .font(ayahListQuran(size))
            .lineSpacing(gap)
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
            .fixedSize(horizontal: false, vertical: true)
            .environment(\.layoutDirection, .rightToLeft)
    }

    private var traits: AccessibilityTraits { selected ? [.isButton, .isSelected] : [.isButton] }

    private var label: String {
        var l = text.words + " (" + text.digits + ")"
        if let tr = text.tr { l += "\n" + tr }
        return l
    }
}

/// فاصل مزخرف بين الآيات: خطان يخفتان نحو الطرفين وفي وسطهما نجمة ثمانية (OrnamentDivider)
private struct AyahListOrnament: View {
    let c: AyahListColors
    var body: some View {
        Canvas { ctx, size in
            let y = size.height / 2, cx = size.width / 2
            var l = Path()
            l.move(to: CGPoint(x: size.width * 0.12, y: y))
            l.addLine(to: CGPoint(x: cx - 10, y: y))
            ctx.stroke(l, with: .linearGradient(Gradient(colors: [c.gold.opacity(0), c.gold.opacity(0.6)]),
                                                startPoint: CGPoint(x: 0, y: y), endPoint: CGPoint(x: cx - 10, y: y)), lineWidth: 0.8)
            var r = Path()
            r.move(to: CGPoint(x: cx + 10, y: y))
            r.addLine(to: CGPoint(x: size.width * 0.88, y: y))
            ctx.stroke(r, with: .linearGradient(Gradient(colors: [c.gold.opacity(0.6), c.gold.opacity(0)]),
                                                startPoint: CGPoint(x: cx + 10, y: y), endPoint: CGPoint(x: size.width, y: y)), lineWidth: 0.8)
            ctx.stroke(Star8().path(in: CGRect(x: cx - 5, y: y - 5, width: 10, height: 10)), with: .color(c.goldDeep), lineWidth: 0.9)
        }
        .frame(height: 14)
        .accessibilityHidden(true)
    }
}

// MARK: - السورة السابقة والتالية

private struct AyahListNav: View {
    let s: AyahListState
    let c: AyahListColors
    let model: AyahListModel
    var body: some View {
        HStack(spacing: 10) {
            if let p = s.prev { button(p, forward: false) } else { Color.clear.frame(maxWidth: .infinity, maxHeight: 1) }
            if let n = s.next { button(n, forward: true) } else { Color.clear.frame(maxWidth: .infinity, maxHeight: 1) }
        }
        .padding(.top, 16)
    }

    private func button(_ n: AyahListState.Nav, forward: Bool) -> some View {
        Button { model.openSurah(n.surah) } label: {
            HStack(spacing: 4) {
                if !forward { Image(systemName: "chevron.backward").font(.system(size: 14, weight: .semibold)) }
                VStack(spacing: 1) {
                    Text(verbatim: n.label).font(sys(11, .medium))
                    Text(verbatim: n.name).font(sys(14, .medium)).lineLimit(1)
                }
                .frame(maxWidth: .infinity)
                if forward { Image(systemName: "chevron.forward").font(.system(size: 14, weight: .semibold)) }
            }
            .foregroundColor(c.accent)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .frame(maxWidth: .infinity, minHeight: 52)
            .overlay(Capsule().stroke(c.outline, lineWidth: 1))
            .contentShape(Capsule())
        }
        .buttonStyle(PressScale())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: n.label + ", " + n.name))
        .accessibilityAddTraits(.isButton)
    }
}
