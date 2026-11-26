import SwiftUI
import UIKit
import Shared

/// اختيار صفحة من مصحف المدينة بطريقة لطيفة (PagePicker.android.kt): بطاقة على ورق المصحف (بلون الورق الذي اختاره القارئ،
/// والوضعان ثلاثيا الأبعاد كذلك) بإطاره المذهّب المزدوج ونجومه، وفي وسطها رقم الصفحة كبيراً في ميدالية ذهبية (تمتلئ بالذهب
/// إن قُرئت في الختمة)، لمسه يكتب الرقم بلوحة الأرقام؛ وعن جانبيه − و + (الضغط المستمر يكرّرهما ويسرع)، وتحته سور الصفحة
/// وجزؤها وحزبها، وشريط من 1 إلى 604 بنقرة خفيفة مع كل صفحة وأقوى حين يعبر بداية جزء، وشرائح آخر قراءة والورد وبدايات
/// الأجزاء، وزر الفتح. في قسم «الصفحات» من تبويب القرآن (NativeQuran.swift) وفي ورقة «انتقل إلى صفحة» في القارئ.
/// البيانات والنصوص من Kotlin (pagePickerData في NativeQuranSheets.kt).

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
    /// في ورقة القارئ: ميدالية أصغر وبلا ظل
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

    /// ينتقل إلى الصفحة [to] بنقرتها (يقرأ الصفحة الحالية من الربط، فيصح مع تكرار − و + بالضغط المستمر)
    private func move(_ to: Int) {
        let v = min(max(to, 1), d.count)
        guard v != page else { return }
        PagePickerHaptics.step(crossingJuz: d.crossesJuz(page, v))
        page = v
    }

    var body: some View {
        let c = PagePickerColors(d)
        VStack(spacing: compact ? 10 : 14) {
            // − والرقم و +: يتبعان اتجاه اللغة كالشريط (الصفحة الأولى في اليمين بالعربية)
            HStack(spacing: 6) {
                PagePickerStepButton(symbol: "minus", label: d.prev, enabled: page > 1, c: c) { focused = false; move(page - 1) }
                Spacer(minLength: 0)
                dial(c)
                Spacer(minLength: 0)
                PagePickerStepButton(symbol: "plus", label: d.next, enabled: page < d.count, c: c) { focused = false; move(page + 1) }
            }
            Text(d.summary(page))
                .font(sys(15, .semibold)).foregroundColor(c.ink)
                .multilineTextAlignment(.center).lineLimit(3)
                .frame(maxWidth: .infinity)
                .accessibilityHidden(true)
            PagePickerRule(c: c).frame(height: 10)
            slider(c)
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
        .padding(.horizontal, 22)
        .padding(.vertical, compact ? 18 : 24)
        .background(PagePickerPaper(c: c, radius: compact ? 22 : 26, shadow: !compact))
        .onChange(of: focused) { on in if !on { editing = false } }
    }

    // MARK: الرقم

    /// الرقم كبيراً في ميدالية كميدالية رقم الصفحة في المصحف؛ يكبر مع مقاس الخط حتى مرة ونصف
    @ViewBuilder private func dial(_ c: PagePickerColors) -> some View {
        let base: CGFloat = compact ? 100 : 128
        let size = min(UIFontMetrics.default.scaledValue(for: base), base * 1.5)
        let numberFont = Font.system(size: size * 0.42, weight: .bold)
        if editing {
            ZStack {
                PagePickerMedallion(c: c, read: d.isRead(page))
                TextField("", text: $typed)
                    .keyboardType(.numberPad)
                    .focused($focused)
                    .multilineTextAlignment(.center)
                    .font(numberFont)
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
                Text("\(page)")
                    .font(numberFont.monospacedDigit())
                    .foregroundColor(c.goldDeep)
                    .lineLimit(1).minimumScaleFactor(0.5)
                    .padding(.horizontal, 14)
                    .modifier(NumericRoll(value: "\(page)"))
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

    // MARK: الشريط

    /// الشريط: كل خطوة صفحة؛ وVoiceOver يزيد صفحة وينقص صفحة ويقرأ «الصفحة 173، الأعراف، الجزء 9»
    private func slider(_ c: PagePickerColors) -> some View {
        ZStack {
            Slider(
                value: Binding(get: { Double(page) }, set: { v in focused = false; move(Int(v.rounded())) }),
                in: 1...Double(max(d.count, 2)), step: 1
            )
            .tint(c.gold)
        }
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

/// زر − أو + دائري بحلقة ذهبية: لمسة صفحة واحدة، والضغط المستمر يكرّره ويسرع شيئاً فشيئاً.
/// الخطوة الأولى عند رفع الإصبع (لا عند وضعه) فلا يقلب الصفحة من يبدأ تمرير القائمة من فوقه.
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
        Image(systemName: symbol)
            .font(.system(size: 18, weight: .bold))
            .foregroundColor(enabled ? c.goldDeep : c.muted.opacity(0.5))
            .frame(width: 48, height: 48)
            .background(Circle().fill(c.fill.opacity(enabled ? 1 : 0.45)))
            .overlay(Circle().strokeBorder(c.goldDeep.opacity(enabled ? 0.8 : 0.3), lineWidth: 1))
            .scaleEffect(pressed ? 0.86 : 1)
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

/// ميدالية الرقم: حلقتان ذهبيتان ونجمة ثمانية، وتمتلئ بالذهب إن قُرئت الصفحة (كميدالية رقم الصفحة في المصحف)
struct PagePickerMedallion: View {
    let c: PagePickerColors
    let read: Bool
    var body: some View {
        ZStack {
            Circle().fill(c.paper)
            Circle().fill(c.fillDeep.opacity(read ? 0.6 : 0))
            Circle().strokeBorder(c.goldDeep, lineWidth: 1.6)
            Circle().strokeBorder(c.gold.opacity(0.7), lineWidth: 0.8).padding(4.6)
            Star8().stroke(c.gold.opacity(read ? 0.7 : 0.3), lineWidth: 1).padding(6)
        }
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
    var body: some View {
        HStack(spacing: 4) {
            Rectangle().fill(c.gold.opacity(0.7)).frame(height: 0.8)
            Rectangle().fill(c.goldDeep).frame(width: 5, height: 5).rotationEffect(.degrees(45))
            Rectangle().fill(c.gold.opacity(0.7)).frame(height: 0.8)
        }
        .padding(.horizontal, 36)
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
