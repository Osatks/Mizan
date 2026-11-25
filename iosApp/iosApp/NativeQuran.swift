import SwiftUI
import UIKit
import Shared

/// تبويب القرآن بواجهة النظام (SwiftUI) بطراز الرئيسية (NativeHome.swift): ترويسة بتدرج لون التطبيق ونجمته،
/// ومتابعة القراءة في بطاقة خضراء (ووضع التركيز)، والختمة بحلقة إنجازها وورد اليوم ومدتها، والدليل،
/// والبحث في السور والآيات (يكتب القارئ هنا ويبحث Kotlin)، والسور والأجزاء والمحفوظات في قوائم كسولة — كما في QuranScreen.kt.
/// الحالة من Kotlin (NativeQuran.ios.kt) مع الإشعار MizanQuranState، واللمس يعود إليه.
enum NativeQuranScreen {
    static func install() {
        NativeQuran.shared.attach { session in QuranTabHostController(session: session) }
    }
}

final class QuranTabHostController: UIHostingController<QuranTabRoot> {
    init(session: NativeQuranSession) {
        let model = QuranTabModel(session: session)
        super.init(rootView: QuranTabRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct QuranTabState: Decodable {
    struct Surah: Decodable, Identifiable {
        let n: Int
        let num, name, arabic, meta, page, route: String
        let progress: Double
        var id: Int { n }
    }
    struct Juz: Decodable, Identifiable {
        let n: Int
        let num, title, sub, page, route: String
        var id: Int { n }
    }
    struct Hit: Decodable, Identifiable {
        let key, ref, num, route: String
        let text: String?
        let mark: Bool
        var id: String { key }
    }
    struct Focus: Decodable { let label, page, route: String }
    struct Continue: Decodable { let label, title, sub, arabic, route: String; let focus: Focus? }
    struct Plan: Decodable, Identifiable { let days: Int; let label: String; let on: Bool; var id: Int { days } }
    struct Goal: Decodable { let label, value: String; let done: Bool; let frac: Double }
    struct Khatma: Decodable {
        let title, percent, pages, streak, plan, planLabel, newKhatma, done, cta, ctaRoute: String
        let frac: Double
        let streakOn, canRestart, finished: Bool
        let daysLeft, completed: String?
        let plans: [Plan]
        let goal: Goal?
    }
    struct Proof: Decodable, Identifiable {
        let kind, concept, title, body, source: String
        let ayah, tr, hadith: String?
        var id: String { title + source }
    }
    struct Evidence: Decodable { let title, sub: String; let kinds: [String: String]; let points: [Proof] }
    struct Tab: Decodable, Identifiable { let key, label: String; var id: String { key } }
    struct Search: Decodable {
        let ref: Hit?
        let surahTitle: String
        let surahs: [Int]
        let ayahTitle: String?
        let busy: Bool
        let ayahs: [Hit]
        let none: String?
    }
    struct Extra: Decodable { let secondary, tertiary, surfaceVariant, secondaryContainer, onSecondaryContainer, onPrimary: String }

    let rtl, dark, wide, arabicUi: Bool
    let top, bottom, start, end, width: Double
    let title, subtitle, hint, clear, noMarks, query, tab: String
    let tabs: [Tab]
    let cont: Continue?
    let khatma: Khatma
    let evidence: Evidence
    let surahs: [Surah]
    let juz: [Juz]
    let marks: [Hit]
    let search: Search?
    let c: HomeState.Colors
    let x: Extra

    static func decode(_ json: String) -> QuranTabState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(QuranTabState.self, from: data)
    }
}

final class QuranTabModel: ObservableObject {
    let session: NativeQuranSession
    @Published private(set) var state: QuranTabState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativeQuranSession) {
        self.session = session
        state = QuranTabState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanQuranState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = QuranTabState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    func open(_ route: String) { session.open(route: route) }
    func query(_ text: String) { session.query(text: text) }
    func tab(_ key: String) { session.tab(key: key) }
    func plan(_ days: Int) { session.plan(days: Int32(days)) }
    func restart() { session.restart() }
}

/// ارتفاع لوحة المفاتيح فوق أسفل الشاشة: الصفحة لا تتبع المنطقة الآمنة (safeAreaRegions = [])، فتُزاد مسافتها السفلية به
final class QuranTabKeyboard: ObservableObject {
    @Published private(set) var height: CGFloat = 0
    private var tokens: [NSObjectProtocol] = []

    init() {
        let nc = NotificationCenter.default
        tokens.append(nc.addObserver(forName: UIResponder.keyboardWillChangeFrameNotification, object: nil, queue: .main) { [weak self] n in
            guard let end = (n.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue)?.cgRectValue else { return }
            self?.height = max(0, UIScreen.main.bounds.height - end.minY)
        })
        tokens.append(nc.addObserver(forName: UIResponder.keyboardWillHideNotification, object: nil, queue: .main) { [weak self] _ in
            self?.height = 0
        })
    }

    deinit { tokens.forEach { NotificationCenter.default.removeObserver($0) } }
}

/// ألوان إضافية لا تحملها لوحة الرئيسية
struct QuranTabColors {
    let secondary, tertiary, surfaceVariant, secondaryContainer, onSecondaryContainer, onPrimary: Color
    init(_ x: QuranTabState.Extra) {
        secondary = HomePalette.color(x.secondary); tertiary = HomePalette.color(x.tertiary)
        surfaceVariant = HomePalette.color(x.surfaceVariant); secondaryContainer = HomePalette.color(x.secondaryContainer)
        onSecondaryContainer = HomePalette.color(x.onSecondaryContainer); onPrimary = HomePalette.color(x.onPrimary)
    }
}

// MARK: - الشاشة

struct QuranTabRoot: View {
    @ObservedObject var model: QuranTabModel
    var body: some View {
        Group {
            if let s = model.state {
                QuranTabView(s: s, p: HomePalette(s.c), x: QuranTabColors(s.x), model: model)
            } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

private struct QuranTabHeaderBottomKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

private extension View {
    /// التمرير يخفي لوحة المفاتيح (iOS 16)
    @ViewBuilder func quranTabDismissKeyboardOnScroll() -> some View {
        if #available(iOS 16.0, *) { self.scrollDismissesKeyboard(.interactively) } else { self }
    }
}

struct QuranTabView: View {
    let s: QuranTabState
    let p: HomePalette
    let x: QuranTabColors
    let model: QuranTabModel
    @State private var glass: Double = 0
    @State private var text: String
    @StateObject private var keyboard = QuranTabKeyboard()

    init(s: QuranTabState, p: HomePalette, x: QuranTabColors, model: QuranTabModel) {
        self.s = s; self.p = p; self.x = x; self.model = model
        _text = State(initialValue: s.query)
    }

    /// شفافية الشريط الزجاجي تُكتب حين تتغير فقط
    private func track(_ headerBottom: CGFloat) {
        let bar = CGFloat(s.top) + 52, ramp: CGFloat = 36
        let g = Double(min(max((bar + ramp - headerBottom) / ramp, 0), 1))
        if g != glass { glass = g }
    }

    private var searching: Bool { s.search != nil }

    var body: some View {
        ZStack(alignment: .top) {
            p.background
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    QuranTabHeader(s: s, p: p)
                        .background(GeometryReader { g in
                            Color.clear.preference(key: QuranTabHeaderBottomKey.self, value: g.frame(in: .named("quranTab")).maxY)
                        })
                    // ما فوق القائمة خارج المكدّس الكسول، فيبقى حقل البحث ونصه حين تمرّ القائمة الطويلة
                    VStack(spacing: 10) {
                        if !searching { cards }
                        QuranTabSearchField(text: $text, hint: s.hint, clear: s.clear, p: p)
                        if !searching {
                            QuranTabSegmented(tabs: s.tabs, selected: s.tab, p: p, x: x) { model.tab($0) }
                        }
                    }
                    .padding(.top, 6)
                    .padding(.leading, CGFloat(s.start))
                    .padding(.trailing, CGFloat(s.end))
                    LazyVStack(spacing: 10) {
                        if let r = s.search { results(r) } else { list }
                    }
                    .padding(.top, 10)
                    .padding(.leading, CGFloat(s.start))
                    .padding(.trailing, CGFloat(s.end))
                }
                .padding(.bottom, max(CGFloat(s.bottom), keyboard.height + 16))
            }
            .quranTabDismissKeyboardOnScroll()
            .coordinateSpace(name: "quranTab")
            .onPreferenceChange(QuranTabHeaderBottomKey.self) { track($0) }
            QuranTabGlassBar(title: s.title, top: CGFloat(s.top), p: p, amount: glass)
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
        .onChange(of: text) { model.query($0) }
    }

    /// متابعة القراءة والختمة والدليل؛ متجاورة على الشاشة الواسعة (الختمة في العمود الثاني)
    @ViewBuilder private var cards: some View {
        if s.wide {
            HStack(alignment: .top, spacing: 20) {
                VStack(spacing: 10) {
                    if let c = s.cont { QuranTabContinueCard(c: c, p: p, open: model.open) }
                    QuranTabEvidenceCard(e: s.evidence, p: p, x: x)
                }
                .frame(maxWidth: .infinity)
                QuranTabKhatmaCard(k: s.khatma, p: p, x: x, model: model)
                    .frame(maxWidth: .infinity)
            }
        } else {
            if let c = s.cont { QuranTabContinueCard(c: c, p: p, open: model.open) }
            QuranTabKhatmaCard(k: s.khatma, p: p, x: x, model: model)
            QuranTabEvidenceCard(e: s.evidence, p: p, x: x)
        }
    }

    /// القسم المختار: السور، أو الأجزاء، أو المحفوظات
    @ViewBuilder private var list: some View {
        switch s.tab {
        case "juz":
            QuranTabCells(items: s.juz, two: s.wide) { j in
                QuranTabJuzRow(j: j, p: p, x: x) { model.open(j.route) }
            }
        case "saved":
            if s.marks.isEmpty {
                QuranTabNote(text: s.noMarks, p: p)
            } else {
                QuranTabCells(items: s.marks, two: s.wide) { h in
                    QuranTabAyahHit(h: h, p: p) { model.open(h.route) }
                }
            }
        default:
            QuranTabCells(items: s.surahs, two: s.wide) { sr in
                QuranTabSurahRow(sr: sr, arabicUi: s.arabicUi, p: p, x: x) { model.open(sr.route) }
            }
        }
    }

    /// نتائج البحث: الإحالة المكتوبة (2:255)، والسور، والآيات (أو انتظارها)، أو لا نتائج
    @ViewBuilder private func results(_ r: QuranTabState.Search) -> some View {
        if let h = r.ref {
            QuranTabAyahHit(h: h, p: p) { model.open(h.route) }
        }
        let found = r.surahs.compactMap { n in s.surahs.indices.contains(n - 1) ? s.surahs[n - 1] : nil }
        if !found.isEmpty {
            QuranTabGroupTitle(text: r.surahTitle, p: p)
            QuranTabCells(items: found, two: s.wide) { sr in
                QuranTabSurahRow(sr: sr, arabicUi: s.arabicUi, p: p, x: x) { model.open(sr.route) }
            }
        }
        if r.busy {
            ProgressView().progressViewStyle(.circular).tint(p.primary)
                .frame(maxWidth: .infinity).padding(16)
        } else if !r.ayahs.isEmpty {
            if let t = r.ayahTitle { QuranTabGroupTitle(text: t, p: p) }
            QuranTabCells(items: r.ayahs, two: s.wide) { h in
                QuranTabAyahHit(h: h, p: p) { model.open(h.route) }
            }
        } else if let none = r.none {
            QuranTabNote(text: none, p: p)
        }
    }
}

// MARK: - الشبكة

private struct QuranTabPair<Item: Identifiable>: Identifiable {
    let a: Item
    let b: Item?
    var id: Item.ID { a.id }
}

/// عناصر متجانسة: واحد في كل صف على الهاتف، واثنان متساويا الارتفاع على الشاشة الواسعة (PageScope.cells)
private struct QuranTabCells<Item: Identifiable, Cell: View>: View {
    let items: [Item]
    let two: Bool
    @ViewBuilder let cell: (Item) -> Cell

    private var pairs: [QuranTabPair<Item>] {
        stride(from: 0, to: items.count, by: 2).map { i in
            QuranTabPair(a: items[i], b: i + 1 < items.count ? items[i + 1] : nil)
        }
    }

    var body: some View {
        if two {
            ForEach(pairs) { pr in
                HStack(alignment: .top, spacing: 10) {
                    cell(pr.a).frame(maxWidth: .infinity, maxHeight: .infinity)
                    if let b = pr.b {
                        cell(b).frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else {
                        Color.clear.frame(maxWidth: .infinity)
                    }
                }
                .fixedSize(horizontal: false, vertical: true)
            }
        } else {
            ForEach(items) { item in cell(item) }
        }
    }
}

// MARK: - الترويسة والشريط

/// ترويسة الصفحة بطراز ترويسة الرئيسية: تدرج من لون التطبيق الفاتح، ونجمة باهتة، وأيقونة المصحف والعنوان
private struct QuranTabHeader: View {
    let s: QuranTabState
    let p: HomePalette
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "book.fill")
                .font(.system(size: 20, weight: .semibold)).foregroundColor(p.primary)
                .frame(width: 46, height: 46)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.surface.opacity(0.85)).shadow(color: .black.opacity(0.08), radius: 3, y: 1))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 0) {
                Text(s.title).font(sys(28, .heavy)).foregroundColor(p.primary).lineLimit(1).minimumScaleFactor(0.8)
                Text(s.subtitle).font(sys(14)).foregroundColor(p.onSurfaceVariant).lineLimit(2)
            }
            Spacer(minLength: 0)
        }
        .padding(.leading, CGFloat(s.start) + 4)
        .padding(.trailing, CGFloat(s.end) + 4)
        .padding(.top, CGFloat(s.top) + 18)
        .padding(.bottom, 16)
        .frame(maxWidth: .infinity)
        .background(
            ZStack(alignment: s.rtl ? .topTrailing : .topLeading) {
                GeometryReader { g in
                    VStack(spacing: 0) {
                        p.primaryContainer.frame(height: 800)
                        LinearGradient(colors: [p.primaryContainer, p.primaryContainer.opacity(0.55), p.background], startPoint: .top, endPoint: .bottom)
                            .frame(height: g.size.height)
                    }
                    .offset(y: -800)
                }
                StarEmblem(color: p.primary.opacity(0.07), radius: 150, stroke: 1.7)
                    .offset(x: s.rtl ? 220 : -220, y: -190)
            }
            .environment(\.layoutDirection, .leftToRight)
            .allowsHitTesting(false)
        )
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }
}

/// الشريط الزجاجي العلوي بعنوان الصفحة حين تمرّ الترويسة تحته
private struct QuranTabGlassBar: View {
    let title: String
    let top: CGFloat
    let p: HomePalette
    let amount: Double
    var body: some View {
        Text(title)
            .font(sys(16, .medium)).foregroundColor(p.onSurface).lineLimit(1)
            .frame(maxWidth: .infinity).frame(height: 52)
            .padding(.top, top)
            .background(.ultraThinMaterial)
            .overlay(Rectangle().fill(p.outlineVariant.opacity(0.8)).frame(height: 1), alignment: .bottom)
            .opacity(amount)
            .allowsHitTesting(false)
            .accessibilityHidden(true)
    }
}

// MARK: - البحث والأقسام

/// حقل البحث: النص يذهب إلى Kotlin فيبحث ويعيد نشر النتائج
private struct QuranTabSearchField: View {
    @Binding var text: String
    let hint: String
    let clear: String
    let p: HomePalette
    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 17, weight: .semibold)).foregroundColor(p.primary)
                .accessibilityHidden(true)
            TextField(hint, text: $text)
                .font(sys(16))
                .foregroundColor(p.onSurface)
                .disableAutocorrection(true)
                .submitLabel(.search)
            if !text.isEmpty {
                Button { text = "" } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 18)).foregroundColor(p.onSurfaceVariant)
                        .frame(width: 32, height: 32)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(clear)
            }
        }
        .padding(.leading, 14).padding(.trailing, 8)
        .frame(minHeight: 54)
        .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).strokeBorder(p.outlineVariant, lineWidth: 1))
    }
}

/// أقسام الفهرس (Segmented في Components.kt): حبة بيضاء تنزلق إلى القسم المختار
private struct QuranTabSegmented: View {
    let tabs: [QuranTabState.Tab]
    let selected: String
    let p: HomePalette
    let x: QuranTabColors
    let select: (String) -> Void
    @Namespace private var ns

    var body: some View {
        HStack(spacing: 0) {
            ForEach(tabs) { t in
                let on = t.key == selected
                Button {
                    if !on { UISelectionFeedbackGenerator().selectionChanged() }
                    select(t.key)
                } label: {
                    Text(t.label)
                        .font(sys(14, .bold))
                        .foregroundColor(on ? p.primary : p.onSurfaceVariant)
                        .lineLimit(1).minimumScaleFactor(0.8)
                        .padding(.vertical, 10).padding(.horizontal, 6)
                        .frame(maxWidth: .infinity)
                        .background {
                            if on {
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .fill(p.surface)
                                    .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).strokeBorder(p.outlineVariant, lineWidth: 1))
                                    .matchedGeometryEffect(id: "pill", in: ns)
                            }
                        }
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(on ? AccessibilityTraits([.isButton, .isSelected]) : AccessibilityTraits.isButton)
            }
        }
        .padding(4)
        .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(x.surfaceVariant))
        .animation(UIAccessibility.isReduceMotionEnabled ? nil : .spring(response: 0.32, dampingFraction: 0.78), value: selected)
    }
}

private struct QuranTabGroupTitle: View {
    let text: String
    let p: HomePalette
    var body: some View {
        Text(text).font(sys(14, .bold)).foregroundColor(p.primary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.leading, 6).padding(.top, 4)
            .accessibilityAddTraits(.isHeader)
    }
}

/// ملاحظة بخلفية لون التطبيق الفاتح (Note في Components.kt)
private struct QuranTabNote: View {
    let text: String
    let p: HomePalette
    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "info.circle.fill").font(.system(size: 18)).foregroundColor(p.primary).accessibilityHidden(true)
            Text(text).font(sys(14)).foregroundColor(p.onPrimaryContainer).frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.primaryContainer))
    }
}

// MARK: - الصفوف

/// رقم السورة أو الجزء في نجمة، تحيط بها حلقة ذهبية بقدر ما قُرئ، وتمتلئ بالذهب حين تكتمل (NumberStar)
private struct QuranTabNumberStar: View {
    let num: String
    var progress: Double = 0
    var size: CGFloat = 40
    let p: HomePalette

    var body: some View {
        let done = progress >= 1
        let r = size / 2
        let starR = progress > 0 ? r * 0.72 : r - 1
        ZStack {
            if progress > 0 {
                Circle().stroke(p.outlineVariant, lineWidth: 2.5).padding(1.25)
                Circle().trim(from: 0, to: CGFloat(min(progress, 1)))
                    .stroke(p.gold, style: StrokeStyle(lineWidth: 2.5, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .padding(1.25)
            }
            if done {
                Circle().fill(p.gold.opacity(0.18)).frame(width: starR * 1.6, height: starR * 1.6)
            }
            Star8()
                .stroke(done ? p.goldDeep : p.primary.opacity(0.55), lineWidth: progress > 0 ? 0.9 : 1.1)
                .frame(width: starR * 2, height: starR * 2)
            Text(num)
                .font(.system(size: num.count >= 3 && progress > 0 ? 11 : 14, weight: .bold))
                .foregroundColor(done ? p.goldDeep : p.primary)
                .lineLimit(1).minimumScaleFactor(0.7)
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

/// علامة الآية: نجمة ذهبية بداخلها رقمها بالأرقام المشرقية (AyahMarker)
private struct QuranTabAyahMarker: View {
    let num: String
    let p: HomePalette
    var body: some View {
        ZStack {
            Star8().stroke(p.goldDeep, lineWidth: 1.5).padding(1.5)
            Circle().fill(p.gold.opacity(0.14)).frame(width: 18, height: 18)
            Text(num).font(.system(size: num.count >= 3 ? 8 : 10, weight: .semibold))
                .foregroundColor(p.onSurface).lineLimit(1).minimumScaleFactor(0.6)
        }
        .frame(width: 28, height: 28)
        .environment(\.layoutDirection, .leftToRight)
        .accessibilityHidden(true)
    }
}

/// بطاقة صف بيضاء بحافة رفيعة تصغر قليلاً تحت الإصبع
private struct QuranTabRowCard<Content: View>: View {
    let p: HomePalette
    var radius: CGFloat = 16
    var edge: Double = 1
    let action: () -> Void
    @ViewBuilder let content: () -> Content
    var body: some View {
        Button(action: action) {
            content()
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
                .background(RoundedRectangle(cornerRadius: radius, style: .continuous).fill(p.surface))
                .overlay(RoundedRectangle(cornerRadius: radius, style: .continuous).strokeBorder(p.outlineVariant.opacity(edge), lineWidth: 1))
                .contentShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
        }
        .buttonStyle(PressScale(scale: 0.98))
    }
}

/// صف السورة: رقمها في نجمة بحلقة ما قُرئ، واسمها (بخط المصحف في الواجهة العربية)، ونوعها وعدد آياتها،
/// واسمها بخط المصحف (أو صفحة بدايتها في الواجهة العربية)
private struct QuranTabSurahRow: View {
    let sr: QuranTabState.Surah
    let arabicUi: Bool
    let p: HomePalette
    let x: QuranTabColors
    let action: () -> Void
    var body: some View {
        QuranTabRowCard(p: p, radius: 18, edge: 0.7, action: action) {
            HStack(spacing: 12) {
                QuranTabNumberStar(num: sr.num, progress: sr.progress, size: 42, p: p)
                VStack(alignment: .leading, spacing: 1) {
                    if arabicUi {
                        Text(sr.arabic).font(quranFont(21)).foregroundColor(p.onSurface).lineLimit(1)
                    } else {
                        Text(sr.name).font(sys(16, .medium)).foregroundColor(p.onSurface).lineLimit(1)
                    }
                    Text(sr.meta).font(sys(12)).foregroundColor(p.onSurfaceVariant).lineLimit(2)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                if arabicUi {
                    Text(sr.page).font(sys(12, .bold)).foregroundColor(x.secondary).lineLimit(1)
                } else {
                    Text(sr.arabic).font(quranFont(22)).foregroundColor(p.primary).lineLimit(1)
                        .environment(\.layoutDirection, .rightToLeft)
                }
            }
            .padding(.horizontal, 12).padding(.vertical, 8)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(Text(arabicUi ? "\(sr.arabic), \(sr.meta), \(sr.page)" : "\(sr.name), \(sr.meta)"))
        .accessibilityAddTraits(.isButton)
    }
}

private struct QuranTabJuzRow: View {
    let j: QuranTabState.Juz
    let p: HomePalette
    let x: QuranTabColors
    let action: () -> Void
    var body: some View {
        QuranTabRowCard(p: p, action: action) {
            HStack(spacing: 12) {
                QuranTabNumberStar(num: j.num, p: p)
                VStack(alignment: .leading, spacing: 1) {
                    Text(j.title).font(sys(16, .medium)).foregroundColor(p.onSurface).lineLimit(1)
                    Text(j.sub).font(sys(12)).foregroundColor(p.onSurfaceVariant).lineLimit(2)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Text(j.page).font(sys(12, .medium)).foregroundColor(x.secondary).lineLimit(1)
            }
            .padding(12)
        }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }
}

/// آية في نتائج البحث أو المحفوظات: علامتها والإحالة، ونصها بخط المصحف في ثلاثة أسطر
private struct QuranTabAyahHit: View {
    let h: QuranTabState.Hit
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        QuranTabRowCard(p: p, action: action) {
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 8) {
                    QuranTabAyahMarker(num: h.num, p: p)
                    Text(h.ref).font(sys(14, .bold)).foregroundColor(p.primary).lineLimit(2)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    if h.mark {
                        Image(systemName: "bookmark.fill").font(.system(size: 15)).foregroundColor(p.gold).accessibilityHidden(true)
                    }
                }
                if let t = h.text {
                    Text(t).font(quranFont(20)).foregroundColor(p.onSurface)
                        .lineLimit(3).lineSpacing(6)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .environment(\.layoutDirection, .rightToLeft)
                }
            }
            .padding(14)
        }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }
}

// MARK: - البطاقات

/// متابعة القراءة في بطاقة بتدرج لون التطبيق، ومعها «تابع في وضع التركيز» إن كان للقارئ موضع فيه
private struct QuranTabContinueCard: View {
    let c: QuranTabState.Continue
    let p: HomePalette
    let open: (String) -> Void

    var body: some View {
        Button { open(c.route) } label: {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .center, spacing: 10) {
                    VStack(alignment: .leading, spacing: 1) {
                        Text(c.label).font(sys(14, .bold)).foregroundColor(p.gold)
                        Text(c.title).font(sys(16, .medium)).foregroundColor(.white)
                        Text(c.sub).font(sys(12)).foregroundColor(.white.opacity(0.8))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityElement(children: .combine)
                    Text(c.arabic).font(quranFont(26)).foregroundColor(.white).lineLimit(1).accessibilityHidden(true)
                    Image(systemName: "chevron.forward").font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.white.opacity(0.8)).accessibilityHidden(true)
                }
                if let f = c.focus {
                    Button { open(f.route) } label: {
                        HStack(spacing: 8) {
                            Image(systemName: "arrow.up.left.and.arrow.down.right")
                                .font(.system(size: 14, weight: .semibold)).foregroundColor(p.gold).accessibilityHidden(true)
                            Text(f.label).font(sys(14, .bold)).foregroundColor(.white).lineLimit(1)
                            Text(f.page).font(sys(14, .bold)).foregroundColor(.white.opacity(0.75)).lineLimit(1).fixedSize()
                        }
                        .padding(.leading, 12).padding(.trailing, 14).padding(.vertical, 7)
                        .background(Capsule().fill(Color.white.opacity(0.14)))
                        .overlay(Capsule().strokeBorder(p.gold.opacity(0.55), lineWidth: 1))
                        .accessibilityElement(children: .combine)
                    }
                    .buttonStyle(PressScale(scale: 0.95))
                }
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                ZStack(alignment: .trailing) {
                    LinearGradient(colors: [p.hero1, p.hero2], startPoint: .topLeading, endPoint: .bottomTrailing)
                    StarEmblem(color: .white.opacity(0.07), radius: 60, stroke: 1).offset(x: 30)
                }
            )
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        }
        .buttonStyle(PressScale())
    }
}

/// الختمة: حلقة الإنجاز حول نجمة تمتلئ بالذهب، والصفحات المقروءة وأيام القراءة المتتالية، ومدتها في قائمة،
/// وورد اليوم بشريط ذهبي، وزر يفتح أول صفحة لم تُقرأ (KhatmaCard)
private struct QuranTabKhatmaCard: View {
    let k: QuranTabState.Khatma
    let p: HomePalette
    let x: QuranTabColors
    let model: QuranTabModel

    private var motion: Animation? {
        UIAccessibility.isReduceMotionEnabled ? nil : .spring(response: 0.9, dampingFraction: 0.9)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .center, spacing: 14) {
                ring
                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 6) {
                        Text(k.title).font(sys(16, .medium)).foregroundColor(p.onSurface).lineLimit(1)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        planMenu
                    }
                    Text(k.pages).font(sys(14)).foregroundColor(p.onSurfaceVariant)
                    HStack(spacing: 4) {
                        Image(systemName: "flame.fill").font(.system(size: 14))
                            .foregroundColor(k.streakOn ? p.gold : p.outlineVariant).accessibilityHidden(true)
                        Text(k.streak).font(sys(14, .bold)).foregroundColor(p.onSurface).lineLimit(1)
                        if let d = k.daysLeft {
                            Text("·").font(sys(12)).foregroundColor(x.secondary)
                            Text(d).font(sys(12, .medium)).foregroundColor(x.secondary).lineLimit(1)
                        }
                    }
                }
            }
            if k.finished {
                Text(k.done).font(sys(15, .bold)).foregroundColor(p.primary)
                if let c = k.completed { Text(c).font(sys(12)).foregroundColor(p.onSurfaceVariant) }
                primaryButton(k.newKhatma, symbol: nil) { model.restart() }
            } else {
                if let g = k.goal {
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text(g.label).font(sys(14, .bold)).foregroundColor(p.onSurface)
                            Spacer(minLength: 6)
                            Text(g.value).font(sys(14, .medium)).foregroundColor(g.done ? p.primary : p.onSurfaceVariant)
                        }
                        .accessibilityElement(children: .combine)
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule().fill(p.outlineVariant)
                                Capsule()
                                    .fill(LinearGradient(colors: [p.goldDeep, p.gold], startPoint: .leading, endPoint: .trailing))
                                    .frame(width: geo.size.width * CGFloat(g.frac))
                                    .opacity(g.frac > 0 ? 1 : 0)
                            }
                        }
                        .frame(height: 8)
                        .animation(motion, value: g.frac)
                        .accessibilityHidden(true)
                    }
                }
                primaryButton(k.cta, symbol: "book.fill") { model.open(k.ctaRoute) }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 24, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous).strokeBorder(p.gold.opacity(0.45), lineWidth: 1))
    }

    /// حلقة الإنجاز بتدرج ذهبي، ونسبته في وسطها
    private var ring: some View {
        ZStack {
            Circle().stroke(p.outlineVariant, lineWidth: 6).padding(3)
            Circle().trim(from: 0, to: CGFloat(min(max(k.frac, 0), 1)))
                .stroke(AngularGradient(gradient: Gradient(colors: [p.gold, p.goldDeep, p.gold]), center: .center),
                        style: StrokeStyle(lineWidth: 6, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .padding(3)
            Star8().stroke(p.gold.opacity(0.25 + 0.5 * k.frac), lineWidth: 1).frame(width: 38, height: 38)
            Text(k.percent).font(.system(size: 14, weight: .bold)).foregroundColor(p.primary)
                .lineLimit(1).minimumScaleFactor(0.7).padding(.horizontal, 8)
        }
        .frame(width: 64, height: 64)
        .environment(\.layoutDirection, .leftToRight)
        .animation(motion, value: k.frac)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(k.percent))
    }

    /// مدة الختمة في شارة صغيرة؛ تفتح قائمة بالمدد، وبدء ختمة جديدة
    private var planMenu: some View {
        Menu {
            Section {
                ForEach(k.plans) { pl in
                    Button { model.plan(pl.days) } label: {
                        if pl.on { Label(pl.label, systemImage: "checkmark") } else { Text(pl.label) }
                    }
                }
            }
            if k.canRestart {
                Section {
                    Button { model.restart() } label: { Label(k.newKhatma, systemImage: "arrow.clockwise") }
                }
            }
        } label: {
            HStack(spacing: 2) {
                Text(k.plan).font(sys(12, .bold)).lineLimit(1)
                Image(systemName: "chevron.down").font(.system(size: 10, weight: .bold)).accessibilityHidden(true)
            }
            .foregroundColor(x.onSecondaryContainer)
            .padding(.leading, 10).padding(.trailing, 8).padding(.vertical, 4)
            .background(Capsule().fill(x.secondaryContainer))
        }
        .accessibilityLabel(Text("\(k.planLabel), \(k.plan)"))
    }

    private func primaryButton(_ title: String, symbol: String?, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let sym = symbol { Image(systemName: sym).font(.system(size: 17)).accessibilityHidden(true) }
                Text(title).font(sys(15, .bold)).lineLimit(1)
            }
            .foregroundColor(x.onPrimary)
            .frame(maxWidth: .infinity, minHeight: 48)
            .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(p.primary))
        }
        .buttonStyle(PressScale(scale: 0.97))
    }
}

/// «الدليل وأقوال الفقهاء»: أنواع الأدلة في لمحة، وتنفتح بلمسة على الآيات والأحاديث وشرحها ومصادرها (EvidenceCard)
private struct QuranTabEvidenceCard: View {
    let e: QuranTabState.Evidence
    let p: HomePalette
    let x: QuranTabColors
    @State private var open = false

    private func color(_ kind: String) -> Color {
        switch kind {
        case "quran": return p.goldDeep
        case "hadith": return p.primary
        case "consensus": return x.tertiary
        default: return x.secondary
        }
    }

    private func symbol(_ kind: String) -> String {
        switch kind {
        case "quran": return "book.fill"
        case "hadith": return "quote.opening"
        case "consensus": return "person.3.fill"
        default: return "bubble.left.and.bubble.right.fill"
        }
    }

    /// أيقونة الفكرة في كل نقطة (conceptIcon في EvidenceCard.kt)
    private func concept(_ key: String) -> String {
        switch key {
        case "quran": return "book.fill"
        case "time": return "clock.fill"
        case "people": return "person.3.fill"
        case "moon", "fast": return "moon.stars.fill"
        case "sun": return "sun.max.fill"
        case "star": return "star.fill"
        default: return "lightbulb.fill"
        }
    }

    private var kinds: [String] {
        var seen: [String] = []
        for pt in e.points where !seen.contains(pt.kind) { seen.append(pt.kind) }
        return seen
    }

    private func chip(_ kind: String) -> some View {
        Label(e.kinds[kind] ?? kind, systemImage: symbol(kind))
            .font(sys(11, .bold)).foregroundColor(color(kind))
            .padding(.horizontal, 8).padding(.vertical, 3)
            .background(Capsule().fill(color(kind).opacity(0.12)))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button {
                if UIAccessibility.isReduceMotionEnabled { open.toggle() } else { withAnimation(.easeInOut(duration: 0.3)) { open.toggle() } }
            } label: {
                HStack(spacing: 12) {
                    Image(systemName: "book.fill").foregroundColor(p.goldDeep)
                        .frame(width: 42, height: 42)
                        .background(RoundedRectangle(cornerRadius: 13, style: .continuous).fill(p.gold.opacity(0.16)))
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(e.title).font(sys(16, .bold)).foregroundColor(p.onSurface)
                        Text(e.sub).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                        HStack(spacing: 6) { ForEach(kinds, id: \.self) { chip($0) } }.padding(.top, 4)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    Image(systemName: "chevron.down").font(.system(size: 14, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
                        .rotationEffect(.degrees(open ? 180 : 0))
                        .accessibilityHidden(true)
                }
                .padding(16)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(open ? AccessibilityTraits([.isButton, .isSelected]) : AccessibilityTraits.isButton)
            if open {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(e.points.enumerated()), id: \.element.id) { i, pt in
                        if i > 0 { Rectangle().fill(p.outlineVariant.opacity(0.6)).frame(height: 1).padding(.vertical, 12) }
                        point(pt)
                    }
                }
                .padding(.horizontal, 16).padding(.bottom, 16)
                .transition(.opacity)
            }
        }
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(p.gold.opacity(0.45), lineWidth: 1))
    }

    private func point(_ pt: QuranTabState.Proof) -> some View {
        let c = color(pt.kind)
        return HStack(alignment: .top, spacing: 12) {
            Image(systemName: concept(pt.concept)).font(.system(size: 17)).foregroundColor(c)
                .frame(width: 38, height: 38).background(Circle().fill(c.opacity(0.13)))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 6) {
                chip(pt.kind)
                Text(pt.title).font(sys(14, .bold)).foregroundColor(p.onSurface)
                if let a = pt.ayah {
                    quote {
                        Text(a).font(quranFont(19)).foregroundColor(p.onSurface).lineSpacing(8)
                            .multilineTextAlignment(.leading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                if let t = pt.tr { Text(t).font(sys(12)).foregroundColor(p.onSurfaceVariant) }
                if let h = pt.hadith {
                    quote {
                        Text(h).font(sys(14)).foregroundColor(p.onSurface).lineSpacing(6)
                            .multilineTextAlignment(.leading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                Text(pt.body).font(sys(14)).foregroundColor(p.onSurface)
                Label(pt.source, systemImage: symbol(pt.kind)).font(sys(12, .bold)).foregroundColor(c)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    /// الاقتباس (آية أو حديث) بخط جانبي ذهبي، من اليمين دائماً
    private func quote<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        HStack(spacing: 0) {
            Rectangle().fill(p.gold).frame(width: 3)
            content().padding(.horizontal, 12).padding(.vertical, 8)
        }
        .background(p.gold.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .environment(\.layoutDirection, .rightToLeft)
    }
}
