import SwiftUI
import UIKit
import Shared

/// اختيار صفحة من مصحف المدينة بطريقة مميزة (PagePicker.android.kt): بطاقة على ورق المصحف (بلون الورق الذي اختاره القارئ،
/// والليلي وثلاثي الأبعاد كذلك) بإطاره المذهّب المزدوج ونجومه. في أعلاها الصفحة نفسها مصغّرة: إطارها ورأسها الحقيقي وأول
/// سطرين منها بخط المصحف، وعلى خطها السفلي رقم الصفحة كبيراً في ميدالية ذهبية يدور كعدّاد (تمتلئ بالذهب إن قُرئت في الختمة)،
/// لمسه يكتب الرقم بلوحة الأرقام، وعن جانبيه − و + نجمتان مذهّبتان (الضغط المستمر يكرّرهما ويسرع). تحتها سور الصفحة وجزؤها
/// وحزبها، ثم «حافة المصحف» (حواف الأوراق، وخط ذهبي عند كل جزء، وعلامات الأحزاب، وأشرطة الأجزاء، والمؤشر شريط علامة بشرّابة)
/// بنقرة خفيفة مع كل صفحة وأقوى عند حدود الأجزاء، ثم شرائح آخر قراءة والورد وبدايات الأجزاء، وزر الفتح.
/// في قسم «الصفحات» من تبويب القرآن (NativeQuran.swift) وفي ورقة «انتقل إلى صفحة» في القارئ.
/// البيانات والنصوص من Kotlin (pagePickerData في NativeQuranSheets.kt)، ومعاينة الصفحة منه صفحةً صفحة (MushafPagePreview).

// MARK: - البيانات

struct PagePickerData: Decodable {
    /// ما في الصفحة: سورها بلغة الواجهة، وجزؤها وحزبها (كما في رأسها)، واسمها لقارئ الشاشة («الصفحة 173»)
    struct Page: Decodable {
        let s: String
        let j, h: Int
        let n: String
    }
    struct Juz: Decodable, Identifiable {
        let n: Int
        let name: String
        let from, to: Int
        let span: String
        var id: Int { n }
    }
    struct Quick: Decodable, Identifiable {
        let label: String
        let page: Int
        var id: String { label }
    }

    let page: Int
    let pages: [Page]
    let read: String
    let juz: [Juz]
    let hizbNames: [String]
    let quick: [Quick]
    let sep, title, action, prev, next, typeHint, juzTitle: String
    let paper, paperEdge, ink, muted, gold, goldDeep, fill, fillDeep: String
    /// نص المصحف جاهز لمعاينة الصفحة (MushafPagePreview في NativeQuranSheets.kt)
    let ready: Bool?
    /// صفحات بداية الأحزاب الستين: علاماتها على حافة المصحف
    let hizbStarts: [Int]?

    /// عدد الصفحات (604)
    var count: Int { max(pages.count, 1) }

    private func juzName(_ j: Int) -> String { juz.indices.contains(j - 1) ? juz[j - 1].name : "\(j)" }
    private func hizbName(_ h: Int) -> String { hizbNames.indices.contains(h - 1) ? hizbNames[h - 1] : "\(h)" }

    /// ما في الصفحة في سطر تحت رقمها: «الأعراف · الجزء 9 · الحزب 18» (pageSummary)
    func summary(_ page: Int) -> String {
        guard pages.indices.contains(page - 1) else { return "" }
        let pg = pages[page - 1]
        return "\(pg.s) · \(juzName(pg.j)) · \(hizbName(pg.h))"
    }

    /// ما يقرؤه VoiceOver: «الصفحة 173، الأعراف، الجزء 9» (pageSpeech)
    func speech(_ page: Int) -> String {
        guard pages.indices.contains(page - 1) else { return "\(page)" }
        let pg = pages[page - 1]
        return [pg.n, pg.s, juzName(pg.j)].joined(separator: sep)
    }

    /// الصفحة محسوبة مقروءة في الختمة
    func isRead(_ page: Int) -> Bool {
        guard page >= 1, page <= read.utf8.count else { return false }
        return read.utf8[read.utf8.index(read.utf8.startIndex, offsetBy: page - 1)] == UInt8(ascii: "1")
    }

    /// جزء الصفحة بحسب صفحات بداية الأجزاء: شريحة الجزء الذي اختاره القارئ (juzByStart)
    func juzByStart(_ page: Int) -> Int { juz.last(where: { $0.from <= page })?.n ?? 1 }

    /// هل يعبر الانتقال من [a] إلى [b] بداية جزء: اهتزاز أقوى (crossesJuz)
    func crossesJuz(_ a: Int, _ b: Int) -> Bool {
        let lo = min(a, b), hi = max(a, b)
        return juz.contains { $0.from > lo && $0.from <= hi }
    }
}

/// ألوان ورق المصحف (MushafPalette)
struct PagePickerColors {
    let paper, paperEdge, ink, muted, gold, goldDeep, fill, fillDeep: Color
    init(_ d: PagePickerData) {
        paper = HomePalette.color(d.paper); paperEdge = HomePalette.color(d.paperEdge)
        ink = HomePalette.color(d.ink); muted = HomePalette.color(d.muted)
        gold = HomePalette.color(d.gold); goldDeep = HomePalette.color(d.goldDeep)
        fill = HomePalette.color(d.fill); fillDeep = HomePalette.color(d.fillDeep)
    }
}

/// نقرة خفيفة مع كل صفحة، وأقوى حين يعبر الاختيار بداية جزء
enum PagePickerHaptics {
    private static let light = UISelectionFeedbackGenerator()
    private static let strong = UIImpactFeedbackGenerator(style: .rigid)

    static func step(crossingJuz: Bool) {
        if crossingJuz { strong.impactOccurred(); strong.prepare() } else { light.selectionChanged(); light.prepare() }
    }
}

// MARK: - الاختيار

struct MushafPagePicker: View {
    let d: PagePickerData
    /// ألوان التطبيق: الزر والشرائح
    let p: HomePalette
    let onPrimary: Color
    /// في ورقة القارئ: أصغر وبلا ظل
    let compact: Bool
    @Binding var page: Int
    let action: (Int) -> Void

    @State private var editing = false
    @State private var typed = ""
    @FocusState private var focused: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    init(d: PagePickerData, p: HomePalette, onPrimary: Color, compact: Bool, page: Binding<Int>, action: @escaping (Int) -> Void) {
        self.d = d
        self.p = p
        self.onPrimary = onPrimary
        self.compact = compact
        self._page = page
        self.action = action
    }

    /// ينتقل إلى الصفحة [to] بنقرتها (يقرأ الصفحة الحالية من الربط، فيصح مع تكرار − و + بالضغط المستمر والسحب على الحافة)
    private func move(_ to: Int) {
        let v = min(max(to, 1), d.count)
        guard v != page else { return }
        PagePickerHaptics.step(crossingJuz: d.crossesJuz(page, v))
        page = v
    }

    /// قطر الميدالية: يكبر مع مقاس الخط حتى مرة ونصف
    private var dialSize: CGFloat {
        let base: CGFloat = compact ? 84 : 104
        return min(UIFontMetrics.default.scaledValue(for: base), base * 1.5)
    }

    var body: some View {
        let c = PagePickerColors(d)
        let dd = dialSize
        VStack(spacing: compact ? 8 : 12) {
            // الصفحة مصغّرة، والرقم في ميدالية على خطَّي إطارها السفليين، و − و + عن جانبيه (يتبعان اتجاه اللغة كالحافة)
            ZStack(alignment: .bottom) {
                PagePickerMiniPage(d: d, page: page, c: c, compact: compact, dial: dd)
                    .padding(.bottom, dd / 2 - PagePickerGeo.medallionCenter)
                HStack(alignment: .bottom, spacing: 6) {
                    PagePickerStepButton(symbol: "minus", label: d.prev, enabled: page > 1, c: c) { focused = false; move(page - 1) }
                    Spacer(minLength: 0)
                    self.dial(c, size: dd)
                    Spacer(minLength: 0)
                    PagePickerStepButton(symbol: "plus", label: d.next, enabled: page < d.count, c: c) { focused = false; move(page + 1) }
                }
            }
            Text(d.summary(page))
                .font(sys(15, .semibold)).foregroundColor(c.ink)
                .multilineTextAlignment(.center).lineLimit(3)
                .frame(maxWidth: .infinity)
                .accessibilityHidden(true)
            // حافة المصحف: VoiceOver يزيد صفحة وينقص صفحة ويقرأ «الصفحة 173، الأعراف، الجزء 9»
            PagePickerEdge(d: d, page: page, c: c) { v in focused = false; move(v) }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Text(d.title))
                .accessibilityValue(Text(d.speech(page)))
                .accessibilityAdjustableAction { dir in
                    switch dir {
                    case .increment: move(page + 1)
                    case .decrement: move(page - 1)
                    @unknown default: break
                    }
                }
            chips(c)
            Button {
                focused = false
                action(page)
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "book.fill").font(.system(size: 17)).accessibilityHidden(true)
                    Text(d.action).font(sys(15, .bold)).lineLimit(1).minimumScaleFactor(0.8)
                }
                .foregroundColor(onPrimary)
                .frame(maxWidth: .infinity, minHeight: 50)
                .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(p.primary))
            }
            .buttonStyle(PressScale(scale: 0.97))
        }
        .padding(.horizontal, 20)
        .padding(.vertical, compact ? 18 : 22)
        .background(PagePickerPaper(c: c, radius: compact ? 22 : 26, shadow: !compact))
        .onChange(of: focused) { on in if !on { editing = false } }
    }

    // MARK: الرقم

    /// الرقم كبيراً في ميدالية كميدالية رقم الصفحة في المصحف، يدور خانةً خانة كالعدّاد؛ لمسه يكتب الرقم بلوحة الأرقام
    @ViewBuilder private func dial(_ c: PagePickerColors, size: CGFloat) -> some View {
        let fontSize = size * 0.4
        if editing {
            ZStack {
                PagePickerMedallion(c: c, read: d.isRead(page))
                TextField("", text: $typed)
                    .keyboardType(.numberPad)
                    .focused($focused)
                    .multilineTextAlignment(.center)
                    .font(.system(size: fontSize, weight: .bold))
                    .foregroundColor(c.goldDeep)
                    .padding(.horizontal, 14)
                    .onChange(of: typed) { v in accept(v) }
                    .accessibilityLabel(Text(d.typeHint))
                    .accessibilityValue(Text(d.speech(page)))
            }
            .frame(width: size, height: size)
            .environment(\.layoutDirection, .leftToRight)
        } else {
            ZStack {
                PagePickerMedallion(c: c, read: d.isRead(page))
                PagePickerOdometer(value: page, size: fontSize, color: c.goldDeep)
            }
            .frame(width: size, height: size)
            .environment(\.layoutDirection, .leftToRight)
            .contentShape(Circle())
            .onTapGesture { beginTyping() }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text(d.speech(page)))
            .accessibilityHint(Text(d.typeHint))
            .accessibilityAddTraits(.isButton)
            .accessibilityAction { beginTyping() }
        }
    }

    private func beginTyping() {
        typed = "\(page)"
        editing = true
        DispatchQueue.main.async { focused = true }
    }

    /// يُقبل الرقم بالأرقام اللاتينية والعربية المشرقية والفارسية، وكل رقم صحيح (1–604) يُختار وهو يُكتب
    private func accept(_ text: String) {
        var digits = ""
        for ch in text.unicodeScalars {
            let v = ch.value
            if (0x30...0x39).contains(v) { digits.unicodeScalars.append(ch) }
            else if (0x660...0x669).contains(v), let u = Unicode.Scalar(0x30 + v - 0x660) { digits.unicodeScalars.append(u) }
            else if (0x6F0...0x6F9).contains(v), let u = Unicode.Scalar(0x30 + v - 0x6F0) { digits.unicodeScalars.append(u) }
        }
        digits = String(digits.prefix(3))
        if digits != text { typed = digits; return }
        if let n = Int(digits), n >= 1, n <= d.count { move(n) }
    }

    // MARK: الشرائح

    /// آخر قراءة والورد، ثم بدايات الأجزاء؛ تمرّ إلى جزء الصفحة المختارة
    private func chips(_ c: PagePickerColors) -> some View {
        let current = d.juzByStart(page)
        return ScrollViewReader { proxy in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(d.quick) { q in
                        chip("\(q.label) · \(q.page)", on: false) { focused = false; move(q.page) }
                    }
                    ForEach(d.juz) { j in
                        chip(j.name, on: j.n == current) { focused = false; move(j.from) }
                            .id("juz\(j.n)")
                    }
                }
                .padding(.horizontal, 2).padding(.vertical, 2)
            }
            .onAppear { proxy.scrollTo("juz\(current)", anchor: .center) }
            .onChange(of: current) { v in
                if reduceMotion { proxy.scrollTo("juz\(v)", anchor: .center) }
                else { withAnimation(.easeInOut(duration: 0.25)) { proxy.scrollTo("juz\(v)", anchor: .center) } }
            }
        }
    }

    private func chip(_ label: String, on: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label).font(sys(14, .bold)).lineLimit(1)
                .foregroundColor(on ? onPrimary : p.onSurfaceVariant)
                .padding(.horizontal, 14).padding(.vertical, 8)
                .background(Capsule().fill(on ? p.primary : p.surface))
                .overlay(Capsule().strokeBorder(on ? p.primary : p.outlineVariant, lineWidth: 1))
        }
        .buttonStyle(PressScale(scale: 0.92))
        .accessibilityAddTraits(on ? .isSelected : [])
    }
}

// MARK: - أجزاء الاختيار

/// أبعاد الصفحة كما في MushafStyle.kt: المسافة بين أسفل الورقة وخطَّي الإطار السفليين، ومركز رقم الصفحة عليهما
enum PagePickerGeo {
    static let frameFoot: CGFloat = 13
    static let medallionCenter: CGFloat = 9 + frameFoot
}

/// الرقم يدور خانةً خانة كعدّاد المسافات: كل خانة شريط أرقام ينساب إلى رقمها الجديد مارّاً بما بينهما
struct PagePickerOdometer: View {
    let value: Int
    let size: CGFloat
    let color: Color
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        let digits = String(value).compactMap { $0.wholeNumberValue }
        let n = digits.count
        let h = ceil(size * 1.25)
        HStack(spacing: 0) {
            // المفتاح من اليمين: خانة الآحاد تبقى خانة الآحاد مهما طال الرقم
            ForEach(Array((0..<n).reversed()), id: \.self) { place in
                let digit = digits[n - 1 - place]
                VStack(spacing: 0) {
                    ForEach(0..<10, id: \.self) { k in
                        Text(verbatim: "\(k)")
                            .font(.system(size: size, weight: .bold).monospacedDigit())
                            .foregroundColor(color)
                            .frame(height: h)
                    }
                }
                .fixedSize()
                .offset(y: -CGFloat(digit) * h)
                .frame(height: h, alignment: .top)
                .clipped()
                .animation(reduceMotion ? nil : .spring(response: 0.38, dampingFraction: 0.78), value: digit)
            }
        }
        .accessibilityHidden(true)
    }
}

/// الصفحة المختارة مصغّرة كما في المصحف: الورقة والإطار المزدوج ونجومه، والرأس الحقيقي (السورة في اليمين، والجزء والحزب
/// في اليسار) وتحته الخط ذو المعيّن، ثم أول سطرين منها بخط المصحف (نص المصحف المضمَّن من Kotlin: MushafPagePreview)؛
/// تتبدل بتلاشٍ قصير. أسفلها مساحة نصف الميدالية [dial] التي تجلس على خطَّي إطارها السفليين.
struct PagePickerMiniPage: View {
    let d: PagePickerData
    let page: Int
    let c: PagePickerColors
    let compact: Bool
    let dial: CGFloat
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        let preview = MushafPagePreview.shared
        let opening = (d.ready ?? true) ? preview.opening(page: Int32(page)) : ""
        let size: CGFloat = compact ? 18 : 20
        VStack(spacing: 0) {
            HStack(spacing: 8) {
                Text(verbatim: preview.title(page: Int32(page)))
                    .font(quranFont(compact ? 13 : 14)).foregroundColor(c.goldDeep).lineLimit(1)
                Spacer(minLength: 0)
                Text(verbatim: preview.subtitle(page: Int32(page)))
                    .font(quranFont(compact ? 11 : 12)).foregroundColor(c.muted).lineLimit(1)
            }
            .frame(height: 22)
            PagePickerRule(c: c, inset: 0).frame(height: 10)
            Text(verbatim: opening)
                .font(quranFont(size))
                .foregroundColor(c.ink)
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .lineSpacing(size * 0.45)
                .frame(maxWidth: .infinity, minHeight: size * 3.6)
                .padding(.top, 4)
                .id(page)
                .transition(.opacity)
        }
        .padding(.horizontal, 20)
        .padding(.top, 17)
        .padding(.bottom, PagePickerGeo.medallionCenter + dial / 2 + 4)
        .frame(maxWidth: .infinity)
        .background(PagePickerPageFrame(c: c))
        // الرأس من اليمين إلى اليسار كما في الصفحة نفسها، مهما كانت لغة الواجهة
        .environment(\.layoutDirection, .rightToLeft)
        .animation(reduceMotion ? nil : .easeOut(duration: 0.16), value: page)
        .accessibilityHidden(true)
    }
}

/// ورقة الصفحة المصغّرة بإطار صفحة المصحف (drawPageFrame): الخطان الذهبيان فوق مسافة الرقم في أسفلها، والنجوم في زوايا الداخلي
struct PagePickerPageFrame: View {
    let c: PagePickerColors

    private static let corners: [Alignment] = [.topLeading, .topTrailing, .bottomLeading, .bottomTrailing]

    var body: some View {
        let foot = PagePickerGeo.frameFoot
        ZStack {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(RadialGradient(gradient: Gradient(colors: [c.paper, c.paper, c.paperEdge]), center: .center, startRadius: 0, endRadius: 320))
                .shadow(color: Color(red: 0x5C / 255, green: 0x43 / 255, blue: 0).opacity(0.18), radius: 4, y: 2)
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(c.goldDeep.opacity(0.8), lineWidth: 1.6)
                .padding(EdgeInsets(top: 7, leading: 7, bottom: 7 + foot, trailing: 7))
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .strokeBorder(c.gold.opacity(0.7), lineWidth: 0.8)
                .padding(EdgeInsets(top: 11, leading: 11, bottom: 11 + foot, trailing: 11))
            ForEach(0..<4, id: \.self) { k in
                ZStack {
                    Circle().fill(c.paper).frame(width: 9, height: 9)
                    Star8().stroke(c.goldDeep, lineWidth: 1).frame(width: 10, height: 10)
                    Circle().fill(c.goldDeep).frame(width: 2.2, height: 2.2)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: Self.corners[k])
                .padding(EdgeInsets(top: 7, leading: 7, bottom: 7 + foot, trailing: 7))
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}

/// «حافة المصحف»: حواف أوراق المصحف مرصوصة بلون الورق وظلالها (الصفحة الأولى في اليمين بالعربية)، وخط ذهبي أثخن عند
/// بداية كل جزء وعلامة صغيرة عند كل حزب، وفوقها أشرطة الأجزاء الثلاثين (شريط الجزء المختار أعرض وفيه رقمه؛ لمس الأشرطة
/// يقفز إلى بداية أقرب جزء)، والمؤشر شريط علامة ذهبي تتدلى منه شرّابة. السحب يختار صفحة صفحة (PagePicker.android.kt).
struct PagePickerEdge: View {
    let d: PagePickerData
    let page: Int
    let c: PagePickerColors
    let pick: (Int) -> Void
    @Environment(\.layoutDirection) private var direction

    static let ribbons: CGFloat = 18, edge: CGFloat = 30, tassel: CGFloat = 22, inset: CGFloat = 12

    private func x(_ pg: Double, _ w: CGFloat) -> CGFloat {
        let x0 = Self.inset, x1 = w - Self.inset
        let f = CGFloat((pg - 1) / Double(max(d.count - 1, 1)))
        return direction == .rightToLeft ? x1 - f * (x1 - x0) : x0 + f * (x1 - x0)
    }

    private func pageAt(_ px: CGFloat, _ w: CGFloat) -> Int {
        let x0 = Self.inset, x1 = w - Self.inset
        let f = min(max((px - x0) / max(x1 - x0, 1), 0), 1)
        let t = direction == .rightToLeft ? 1 - f : f
        return Int((1 + t * CGFloat(d.count - 1)).rounded())
    }

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let current = d.juzByStart(page)
            Canvas { ctx, size in
                let top = Self.ribbons, bottom = Self.ribbons + Self.edge
                let x0 = Self.inset, x1 = size.width - Self.inset
                let block = CGRect(x: x0, y: top, width: x1 - x0, height: bottom - top)
                // كتلة الأوراق وظلها
                ctx.fill(Path(roundedRect: block.offsetBy(dx: 0, dy: 2), cornerRadius: 5), with: .color(.black.opacity(0.1)))
                ctx.fill(Path(roundedRect: block, cornerRadius: 5),
                         with: .linearGradient(Gradient(colors: [c.paperEdge, c.paper, c.paper, c.paperEdge]),
                                               startPoint: CGPoint(x: 0, y: top), endPoint: CGPoint(x: 0, y: bottom)))
                // حواف الأوراق: خطوط رفيعة متقاربة بدرجتين من الظل
                var light = Path(), dark = Path()
                var lx = x0 + 1, n = 0
                while lx < x1 - 1 {
                    if (n * 7 + n / 3) % 5 < 2 {
                        dark.move(to: CGPoint(x: lx, y: top + 2)); dark.addLine(to: CGPoint(x: lx, y: bottom - 2))
                    } else {
                        light.move(to: CGPoint(x: lx, y: top + 2)); light.addLine(to: CGPoint(x: lx, y: bottom - 2))
                    }
                    lx += 1.6; n += 1
                }
                ctx.stroke(light, with: .color(c.ink.opacity(0.07)), lineWidth: 0.6)
                ctx.stroke(dark, with: .color(c.ink.opacity(0.14)), lineWidth: 0.6)
                ctx.stroke(Path(roundedRect: block, cornerRadius: 5), with: .color(c.gold.opacity(0.55)), lineWidth: 0.8)
                // علامات الأحزاب في أسفل الحافة (الزوجية؛ الفردية بدايات أجزاء)
                for (i, hp) in (d.hizbStarts ?? []).enumerated() where i % 2 == 1 {
                    let hx = x(Double(hp), size.width)
                    var tick = Path()
                    tick.move(to: CGPoint(x: hx, y: bottom - 7)); tick.addLine(to: CGPoint(x: hx, y: bottom - 1))
                    ctx.stroke(tick, with: .color(c.goldDeep.opacity(0.6)), lineWidth: 1)
                }
                // بدايات الأجزاء: خط ذهبي أثخن، وفوقه شريط الجزء
                for j in d.juz {
                    let jx = x(Double(j.from), size.width)
                    let on = j.n == current
                    if j.n > 1 {
                        var line = Path()
                        line.move(to: CGPoint(x: jx, y: top)); line.addLine(to: CGPoint(x: jx, y: bottom))
                        ctx.stroke(line, with: .color(c.gold), lineWidth: 1.6)
                    }
                    let rw: CGFloat = on ? 15 : 5, rh: CGFloat = on ? Self.ribbons : Self.ribbons * 0.55, notch: CGFloat = on ? 4 : 2
                    let left = min(max(jx - rw / 2, 0), size.width - rw)
                    var ribbon = Path()
                    ribbon.move(to: CGPoint(x: left, y: top)); ribbon.addLine(to: CGPoint(x: left + rw, y: top))
                    ribbon.addLine(to: CGPoint(x: left + rw, y: top - rh)); ribbon.addLine(to: CGPoint(x: left + rw / 2, y: top - rh + notch))
                    ribbon.addLine(to: CGPoint(x: left, y: top - rh)); ribbon.closeSubpath()
                    let colors = on ? [c.gold, c.goldDeep] : [c.gold.opacity(0.35), c.gold.opacity(0.7)]
                    ctx.fill(ribbon, with: .linearGradient(Gradient(colors: colors), startPoint: CGPoint(x: 0, y: top - rh), endPoint: CGPoint(x: 0, y: top)))
                    if on {
                        ctx.draw(Text(verbatim: "\(j.n)").font(.system(size: 9, weight: .bold)).foregroundColor(c.paper),
                                 at: CGPoint(x: left + rw / 2, y: top - (rh - notch) / 2), anchor: .center)
                    }
                }
                // المؤشر: شريط علامة ذهبي يمرّ فوق الحافة وتحتها، وشرّابة تتدلى منه
                let tx = x(Double(page), size.width)
                let rw: CGFloat = 10, rTop = top - 6, rBottom = bottom + 8, cut: CGFloat = 4
                var mark = Path()
                mark.move(to: CGPoint(x: tx - rw / 2, y: rTop)); mark.addLine(to: CGPoint(x: tx + rw / 2, y: rTop))
                mark.addLine(to: CGPoint(x: tx + rw / 2, y: rBottom)); mark.addLine(to: CGPoint(x: tx, y: rBottom - cut))
                mark.addLine(to: CGPoint(x: tx - rw / 2, y: rBottom)); mark.closeSubpath()
                ctx.fill(mark.offsetBy(dx: 0.9, dy: 1.5), with: .color(.black.opacity(0.18)))
                ctx.fill(mark, with: .linearGradient(Gradient(colors: [c.gold, c.goldDeep, c.gold]),
                                                    startPoint: CGPoint(x: 0, y: rTop), endPoint: CGPoint(x: 0, y: rBottom)))
                var shine = Path()
                shine.move(to: CGPoint(x: tx - rw * 0.22, y: rTop + 1)); shine.addLine(to: CGPoint(x: tx - rw * 0.22, y: rBottom - cut * 1.3))
                ctx.stroke(shine, with: .color(c.paper.opacity(0.5)), lineWidth: 0.8)
                ctx.stroke(mark, with: .color(c.goldDeep), lineWidth: 0.6)
                // الشرّابة: خيط وعقدة وأهداب
                let knotY = rBottom + 5
                var cord = Path()
                cord.move(to: CGPoint(x: tx, y: rBottom - cut)); cord.addLine(to: CGPoint(x: tx, y: knotY))
                ctx.stroke(cord, with: .color(c.goldDeep), lineWidth: 1)
                ctx.fill(Path(ellipseIn: CGRect(x: tx - 2.6, y: knotY - 2.6, width: 5.2, height: 5.2)), with: .color(c.goldDeep))
                var fringe = Path()
                for f in -2...2 {
                    fringe.move(to: CGPoint(x: tx + CGFloat(f) * 0.8, y: knotY + 2))
                    fringe.addLine(to: CGPoint(x: tx + CGFloat(f) * 1.8, y: knotY + 9))
                }
                ctx.stroke(fringe, with: .color(c.goldDeep.opacity(0.9)), lineWidth: 0.9)
            }
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { v in
                        // لمس الأشرطة ينتظر رفع الإصبع (يقفز إلى بداية أقرب جزء)؛ والسحب يختار صفحة صفحة
                        if v.startLocation.y < Self.ribbons && abs(v.translation.width) < 6 { return }
                        pick(pageAt(v.location.x, w))
                    }
                    .onEnded { v in
                        guard v.startLocation.y < Self.ribbons, abs(v.translation.width) < 6 else { return }
                        let pg = pageAt(v.location.x, w)
                        if let near = d.juz.min(by: { abs($0.from - pg) < abs($1.from - pg) }) { pick(near.from) }
                    }
            )
        }
        .frame(height: Self.ribbons + Self.edge + Self.tassel)
    }
}

/// زر − أو + على نجمة ثمانية مذهّبة (مربعان متراكبان كعلامة الحزب في المصحف): لمسة صفحة واحدة، والضغط المستمر
/// يكرّره ويسرع شيئاً فشيئاً. الخطوة الأولى عند رفع الإصبع (لا عند وضعه) فلا يقلب الصفحة من يبدأ تمرير القائمة من فوقه.
struct PagePickerStepButton: View {
    let symbol: String
    let label: String
    let enabled: Bool
    let c: PagePickerColors
    let step: () -> Void

    @State private var pressed = false
    @State private var repeated = false
    @State private var timer: Timer?

    var body: some View {
        ZStack {
            ZStack {
                RoundedRectangle(cornerRadius: 3, style: .continuous).fill(c.fill.opacity(enabled ? 1 : 0.45))
                RoundedRectangle(cornerRadius: 3, style: .continuous).fill(c.fill.opacity(enabled ? 1 : 0.45)).rotationEffect(.degrees(45))
                Star8().stroke(c.goldDeep.opacity(enabled ? 0.85 : 0.3), lineWidth: 1.1).scaleEffect(1.414)
            }
            .frame(width: 32, height: 32)
            .rotationEffect(.degrees(pressed ? 22.5 : 0))
            Circle().stroke(c.gold.opacity(enabled ? 0.55 : 0.25), lineWidth: 0.8).frame(width: 23, height: 23)
            Image(systemName: symbol)
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(enabled ? c.goldDeep : c.muted.opacity(0.5))
        }
        .frame(width: 48, height: 48)
        .scaleEffect(pressed ? 0.84 : 1)
        .animation(.spring(response: 0.25, dampingFraction: 0.6), value: pressed)
        .contentShape(Circle())
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in if !pressed { begin() } }
                .onEnded { v in end(tap: hypot(v.translation.width, v.translation.height) < 12) }
        )
        .onDisappear { stop() }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(label))
        .accessibilityAddTraits(.isButton)
        .accessibilityAction { if enabled { step() } }
    }

    private func begin() {
        pressed = true
        repeated = false
        guard enabled else { return }
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 0.42, repeats: false) { _ in
            repeated = true
            step()
            schedule(0.12)
        }
    }

    private func schedule(_ gap: TimeInterval) {
        timer = Timer.scheduledTimer(withTimeInterval: gap, repeats: false) { _ in
            guard pressed else { return }
            step()
            schedule(max(0.035, gap * 0.86))
        }
    }

    private func end(tap: Bool) {
        let wasRepeated = repeated
        stop()
        if enabled && tap && !wasRepeated { step() }
    }

    private func stop() {
        timer?.invalidate()
        timer = nil
        pressed = false
        repeated = false
    }
}

/// ميدالية الرقم: حلقتان ذهبيتان ونجمة ثمانية وثماني نقاط على الحلقة، وتمتلئ بالذهب إن قُرئت الصفحة (كميدالية رقم الصفحة في المصحف)
struct PagePickerMedallion: View {
    let c: PagePickerColors
    let read: Bool
    var body: some View {
        GeometryReader { geo in
            let r = min(geo.size.width, geo.size.height) / 2
            ZStack {
                Circle().fill(RadialGradient(gradient: Gradient(colors: [c.paper, c.paper, c.fill]), center: .center, startRadius: 0, endRadius: r))
                Circle().fill(c.fillDeep.opacity(read ? 0.65 : 0))
                Circle().strokeBorder(c.goldDeep, lineWidth: 1.8)
                Circle().strokeBorder(c.gold.opacity(0.75), lineWidth: 0.8).padding(4.6)
                Star8().stroke(c.gold.opacity(read ? 0.68 : 0.28), lineWidth: 1).padding(7)
                ForEach(0..<8, id: \.self) { k in
                    Circle().fill(c.goldDeep).frame(width: 2.6, height: 2.6)
                        .offset(y: -(r - 3))
                        .rotationEffect(.degrees(Double(k) * 45))
                }
            }
            .frame(width: 2 * r, height: 2 * r)
            .position(x: geo.size.width / 2, y: geo.size.height / 2)
        }
        .shadow(color: Color(red: 0x5C / 255, green: 0x43 / 255, blue: 0).opacity(0.22), radius: 5, y: 2)
        .animation(UIAccessibility.isReduceMotionEnabled ? nil : .easeInOut(duration: 0.3), value: read)
        .accessibilityHidden(true)
    }
}

/// ورق المصحف بإطاره: خط ذهبي عريض وآخر رفيع بداخله، وفي زوايا الداخلي نجوم ثمانية (drawPageFrame)
struct PagePickerPaper: View {
    let c: PagePickerColors
    let radius: CGFloat
    var shadow = true

    private static let corners: [Alignment] = [.topLeading, .topTrailing, .bottomLeading, .bottomTrailing]

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: radius, style: .continuous)
                .fill(RadialGradient(gradient: Gradient(colors: [c.paper, c.paper, c.paperEdge]), center: .center, startRadius: 0, endRadius: 420))
                .shadow(color: Color(red: 0x5C / 255, green: 0x43 / 255, blue: 0).opacity(shadow ? 0.22 : 0), radius: 8, y: 3)
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(c.goldDeep.opacity(0.8), lineWidth: 1.6).padding(7)
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(c.gold.opacity(0.7), lineWidth: 0.8).padding(11)
            ForEach(0..<4, id: \.self) { k in
                ZStack {
                    Circle().fill(c.paper).frame(width: 9, height: 9)
                    Star8().stroke(c.goldDeep, lineWidth: 1).frame(width: 10, height: 10)
                    Circle().fill(c.goldDeep).frame(width: 2.2, height: 2.2)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: Self.corners[k])
                .padding(7)
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}

/// خط فاصل رفيع في وسطه معيّن صغير (drawRule)
struct PagePickerRule: View {
    let c: PagePickerColors
    var inset: CGFloat = 36
    var body: some View {
        HStack(spacing: 4) {
            Rectangle().fill(c.gold.opacity(0.7)).frame(height: 0.8)
            Rectangle().fill(c.goldDeep).frame(width: 5, height: 5).rotationEffect(.degrees(45))
            Rectangle().fill(c.gold.opacity(0.7)).frame(height: 0.8)
        }
        .padding(.horizontal, inset)
        .accessibilityHidden(true)
    }
}

/// الأجزاء الثلاثون في شبكة صغيرة بصفحاتها (JuzPagesGrid)؛ جزء الصفحة المختارة مظلَّل، ولمس جزء يختار صفحة بدايته
struct PagePickerJuzGrid: View {
    let d: PagePickerData
    let p: HomePalette
    let page: Int
    let width: Double
    let pick: (Int) -> Void

    var body: some View {
        let cols = max(2, min(6, Int(width / 112)))
        let current = d.juzByStart(page)
        VStack(alignment: .leading, spacing: 8) {
            Text(d.juzTitle).font(sys(14, .bold)).foregroundColor(p.primary)
                .padding(.leading, 6).padding(.top, 4)
                .accessibilityAddTraits(.isHeader)
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: cols), spacing: 8) {
                ForEach(d.juz) { j in
                    let on = j.n == current
                    Button {
                        if !on { UISelectionFeedbackGenerator().selectionChanged() }
                        pick(j.from)
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(j.name).font(sys(14, .bold)).foregroundColor(on ? p.onPrimaryContainer : p.onSurface).lineLimit(2)
                            Text(j.span).font(sys(11)).foregroundColor(p.onSurfaceVariant).lineLimit(2)
                        }
                        .padding(.horizontal, 10).padding(.vertical, 8)
                        .frame(maxWidth: .infinity, minHeight: 52, alignment: .leading)
                        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(on ? p.primaryContainer : p.surface))
                        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(on ? p.gold : p.outlineVariant, lineWidth: 1))
                    }
                    .buttonStyle(PressScale(scale: 0.95))
                    .accessibilityElement(children: .combine)
                    .accessibilityAddTraits(on ? AccessibilityTraits([.isButton, .isSelected]) : AccessibilityTraits.isButton)
                }
            }
        }
    }
}

// MARK: - ورقة «انتقل إلى صفحة» في القارئ

struct PagePickerSheetState: SheetStyled {
    let rtl, dark: Bool
    let c: HomeState.Colors
    let onPrimary: String
    let close: String
    let picker: PagePickerData
}

struct PagePickerSheetView: View {
    @ObservedObject var model: SheetModel<PagePickerSheetState>
    var body: some View {
        Group {
            if let s = model.state {
                PagePickerSheetContent(s: s, model: model)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                    .background(HomePalette(s.c).background.ignoresSafeArea())
            } else {
                Color.clear
            }
        }
    }
}

/// عنوان الورقة وزر الإغلاق، ثم الاختيار المصغّر؛ «اذهب» يعيد رقم الصفحة إلى Kotlin فيقلب القارئ إليها.
/// [scroll]: داخل التمرير (على الشاشة)، وبلا تمرير لقياس ارتفاع الورقة
struct PagePickerSheetContent: View {
    let s: PagePickerSheetState
    let model: SheetModel<PagePickerSheetState>
    var scroll = true
    @State private var page: Int

    init(s: PagePickerSheetState, model: SheetModel<PagePickerSheetState>, scroll: Bool = true) {
        self.s = s
        self.model = model
        self.scroll = scroll
        _page = State(initialValue: min(max(s.picker.page, 1), max(s.picker.pages.count, 1)))
    }

    var body: some View {
        Group {
            if scroll {
                ScrollView(.vertical, showsIndicators: false) { content }
            } else {
                content
            }
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }

    private var content: some View {
        let p = HomePalette(s.c)
        return VStack(spacing: 14) {
            HStack(spacing: 12) {
                Image(systemName: "doc.text.magnifyingglass")
                    .font(sys(18, .semibold)).foregroundColor(p.onPrimaryContainer)
                    .frame(width: 40, height: 40)
                    .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(p.primaryContainer))
                    .accessibilityHidden(true)
                Text(s.picker.title).font(sys(20, .bold)).foregroundColor(p.onSurface).lineLimit(2)
                    .accessibilityAddTraits(.isHeader)
                Spacer(minLength: 0)
                QuranSheetCloseButton(label: s.close, p: p) { model.send("close") }
            }
            MushafPagePicker(d: s.picker, p: p, onPrimary: HomePalette.color(s.onPrimary), compact: true, page: $page) { pg in
                model.send("go", "\(pg)")
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 20)
        .padding(.bottom, 12)
    }
}
