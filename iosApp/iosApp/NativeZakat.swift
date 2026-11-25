import SwiftUI
import UIKit
import Combine
import Shared

/// الزكاة بواجهة النظام (SwiftUI) بطراز الرئيسية (NativeHome.swift): ترويسة بتدرج لون التطبيق ونجمته وشريط علوي ثابت فيه زر الرجوع،
/// وأنواع الزكاة الخمسة في بطاقات مرقمة بالنجمة، ثم حاسبة النوع: بطاقة النتيجة الخضراء، وبطاقات الحقول والصفوف والاختيارات،
/// والملاحظات، والأدلة (الآيات بخط المصحف والأحاديث وأقوال الفقهاء) — كما في ZakatScreen.kt وZakatKindsScreens.kt.
/// الحالة من Kotlin (NativeZakat.ios.kt) مع الإشعار MizanZakatState؛ كل حرف يُكتب يُرسل إليه فيحسب ويعيد النتائج منسقة.
enum NativeZakatScreen {
    static func install() {
        NativeZakat.shared.attach { session in ZakatHostController(session: session) }
    }
}

final class ZakatHostController: UIHostingController<ZakatRoot> {
    init(session: NativeZakatSession) {
        let model = ZakatModel(session: session)
        super.init(rootView: ZakatRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        // حواف الشاشة من Kotlin؛ ولوحة المفاتيح وحدها تُترك للنظام فيرتفع المحتوى فوقها
        if #available(iOS 16.4, *) { safeAreaRegions = .keyboard }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct ZakatState: Decodable {
    struct Kind: Decodable, Identifiable { let key, n, name, desc: String; var id: String { key } }
    struct Option: Decodable, Identifiable, Hashable { let key, label: String; var id: String { key } }
    /// صف في بطاقة: field حقل عددي، stat قيمة، check مفتاح، seg أزرار متجاورة، picker قائمة، button مشاركة
    struct Row: Decodable, Identifiable {
        let t, id: String
        let key, label, value, suffix, hint, selected: String?
        let strong, on: Bool?
        let options: [Option]?
    }
    struct Proof: Decodable {
        let kind, concept, title, body, source: String
        let ayah, tr, hadith: String?
    }
    /// كتلة في الصفحة: result بطاقة النتيجة، card بطاقة صفوف، note ملاحظة، evidence الأدلة؛ side تجعلها في عمود النتائج على الشاشة الواسعة
    struct Block: Decodable, Identifiable {
        let t, id: String
        let side: Bool?
        let label, value, unit, badge: String?
        let ok: Bool?
        let progress: Double?
        let title, sub, text: String?
        let rows: [Row]?
        let open: Bool?
        let kinds: [String]?
        let points: [Proof]?
    }
    struct Page: Decodable { let kind, title, subtitle: String; let blocks: [Block] }
    struct Extra: Decodable { let surfaceVariant, outline, quran, hadith, consensus, scholars: String }

    let rtl, dark, wide: Bool
    let top, bottom, start, end: Double
    let back, close, title, subtitle, heading, note: String
    let kinds: [Kind]
    let kindLabels: [String: String]
    let page: Page?
    let x: Extra
    let c: HomeState.Colors

    static func decode(_ json: String) -> ZakatState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(ZakatState.self, from: data)
    }
}

final class ZakatModel: ObservableObject {
    let session: NativeZakatSession
    @Published private(set) var state: ZakatState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativeZakatSession) {
        self.session = session
        state = ZakatState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanZakatState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = ZakatState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    func open(_ kind: String) { session.open(kind: kind) }
    func back() { session.back() }
    func share() { session.share() }
    func edit(_ key: String, _ value: String) { session.edit(key: key, value: value) }
    func toggle(_ key: String, _ on: Bool) { session.toggle(key: key, on: on) }
    func choose(_ key: String, _ value: String) { session.choose(key: key, value: value) }
}

/// أيقونات أنواع الزكاة بأيقونات النظام (كما في ZakatTypes)
private func zakatKindSymbol(_ key: String) -> String {
    switch key {
    case "money": return "diamond.fill"
    case "trade": return "cart.fill"
    case "livestock": return "pawprint.fill"
    case "crops": return "leaf.fill"
    case "minerals":
        if #available(iOS 16.0, *) { return "mountain.2.fill" }
        return "cube.fill"
    default: return "wallet.pass.fill"
    }
}

/// أيقونة فكرة النقطة في الدليل (conceptIcon في EvidenceCard.kt)
private func zakatConceptSymbol(_ c: String) -> String {
    switch c {
    case "duty": return "checkmark.seal.fill"
    case "gold", "jewelry": return "diamond.fill"
    case "nisab": return "scalemass.fill"
    case "year": return "calendar"
    case "rate": return "percent"
    case "debt": return "minus.circle.fill"
    case "trade": return "cart.fill"
    case "value", "money": return "banknote.fill"
    case "shares": return "chart.bar.fill"
    case "camel", "cattle": return "pawprint.fill"
    case "grazing", "harvest", "crops": return "leaf.fill"
    case "age": return "hourglass"
    case "water": return "drop.fill"
    case "treasure": return "shippingbox.fill"
    case "mine": return "cube.fill"
    case "people": return "person.3.fill"
    case "time": return "clock.fill"
    case "scholars": return "text.bubble.fill"
    case "quran": return "book.fill"
    default: return "lightbulb.fill"
    }
}

/// أيقونة نوع الدليل: قرآن، سنة، إجماع، أقوال الفقهاء
private func zakatProofKindSymbol(_ k: String) -> String {
    switch k {
    case "quran": return "book.fill"
    case "hadith": return "quote.opening"
    case "consensus": return "person.3.fill"
    default: return "text.bubble.fill"
    }
}

private func zakatHideKeyboard() {
    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
}

// MARK: - الشاشة

struct ZakatRoot: View {
    @ObservedObject var model: ZakatModel
    var body: some View {
        Group {
            if let s = model.state { ZakatView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        // حواف الشاشة من Kotlin، ولوحة المفاتيح تُحترم
        .ignoresSafeArea(.container)
    }
}

struct ZakatView: View {
    let s: ZakatState
    let p: HomePalette
    let model: ZakatModel

    var body: some View {
        // فتح النوع والرجوع منه بلا حركة هنا: يرسم الانتقال المتحكم الجذر (NavMotion.kt) كما بين الصفحات
        ZStack {
            p.background.ignoresSafeArea()
            if let pg = s.page {
                ZakatCalcPage(s: s, pg: pg, p: p, model: model)
                    .id(pg.kind)
            } else {
                ZakatTypesPage(s: s, p: p, model: model)
            }
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }
}

private struct ZakatHeaderBottomKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

/// هيكل الصفحة: ترويسة كبيرة تمر تحت شريط علوي ثابت (زر الرجوع دائماً، والعنوان يظهر حين تختفي الترويسة)
private struct ZakatScaffold<Content: View>: View {
    let s: ZakatState
    let p: HomePalette
    let title: String
    let subtitle: String
    let symbol: String
    let onBack: () -> Void
    @ViewBuilder let content: () -> Content
    @State private var headerBottom: CGFloat = .greatestFiniteMagnitude
    @State private var keyboard = false

    private var glass: Double {
        let bar = CGFloat(s.top) + 52, ramp: CGFloat = 36
        return Double(min(max((bar + ramp - headerBottom) / ramp, 0), 1))
    }

    var body: some View {
        ZStack(alignment: .top) {
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    ZakatHeader(title: title, subtitle: subtitle, symbol: symbol, s: s, p: p)
                        .background(GeometryReader { g in
                            Color.clear.preference(key: ZakatHeaderBottomKey.self, value: g.frame(in: .named("zakat")).maxY)
                        })
                    content()
                        .padding(.leading, CGFloat(s.start))
                        .padding(.trailing, CGFloat(s.end))
                }
                .padding(.bottom, CGFloat(s.bottom))
            }
            .coordinateSpace(name: "zakat")
            .onPreferenceChange(ZakatHeaderBottomKey.self) { headerBottom = $0 }
            .modifier(ZakatScrollDismiss())
            ZakatTopBar(title: title, s: s, p: p, amount: glass, keyboard: keyboard, onBack: onBack)
        }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillShowNotification)) { _ in keyboard = true }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillHideNotification)) { _ in keyboard = false }
    }
}

/// سحب الصفحة يُنزل لوحة المفاتيح (iOS 16 فأحدث)
private struct ZakatScrollDismiss: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content.scrollDismissesKeyboard(.interactively)
        } else {
            content
        }
    }
}

/// الشريط العلوي الثابت: زر الرجوع، والعنوان على زجاج حين تمر الترويسة تحته، وزر إخفاء لوحة المفاتيح ما دامت ظاهرة
private struct ZakatTopBar: View {
    let title: String
    let s: ZakatState
    let p: HomePalette
    let amount: Double
    let keyboard: Bool
    let onBack: () -> Void

    var body: some View {
        ZStack {
            Text(title)
                .font(sys(16, .medium)).foregroundColor(p.onSurface).lineLimit(1)
                .padding(.horizontal, 64)
                .opacity(amount)
                .allowsHitTesting(false)
                .accessibilityHidden(true)
            HStack {
                barButton("chevron.backward", s.back, action: onBack)
                Spacer(minLength: 0)
                if keyboard {
                    barButton("keyboard.chevron.compact.down", s.close, action: zakatHideKeyboard)
                        .transition(.opacity)
                }
            }
            .padding(.leading, CGFloat(s.start))
            .padding(.trailing, CGFloat(s.end))
            .animation(UIAccessibility.isReduceMotionEnabled ? nil : .easeOut(duration: 0.2), value: keyboard)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 52)
        .padding(.top, CGFloat(s.top))
        .background(
            Rectangle().fill(.ultraThinMaterial)
                .overlay(Rectangle().fill(p.outlineVariant.opacity(0.8)).frame(height: 1), alignment: .bottom)
                .opacity(amount)
                .allowsHitTesting(false)
        )
    }

    private func barButton(_ symbol: String, _ label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 17, weight: .semibold)).foregroundColor(p.primary)
                .frame(width: 42, height: 42)
                .background(Circle().fill(p.surface.opacity(0.85)).shadow(color: .black.opacity(0.08), radius: 3, y: 1))
        }
        .buttonStyle(PressScale(scale: 0.9))
        .accessibilityLabel(label)
    }
}

/// ترويسة بطراز الرئيسية: تدرج من لون التطبيق الفاتح ونجمة باهتة، وأيقونة النوع وعنوانه ووصفه
private struct ZakatHeader: View {
    let title: String
    let subtitle: String
    let symbol: String
    let s: ZakatState
    let p: HomePalette

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 20, weight: .semibold)).foregroundColor(p.primary)
                .frame(width: 46, height: 46)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.gold.opacity(0.18)))
            VStack(alignment: .leading, spacing: 1) {
                Text(title).font(sys(26, .heavy)).foregroundColor(p.primary).lineLimit(2).minimumScaleFactor(0.8)
                Text(subtitle).font(sys(14)).foregroundColor(p.onSurfaceVariant).lineLimit(3)
            }
            Spacer(minLength: 0)
        }
        .padding(.leading, CGFloat(s.start) + 4)
        .padding(.trailing, CGFloat(s.end) + 4)
        .padding(.top, CGFloat(s.top) + 58)
        .padding(.bottom, 18)
        .frame(maxWidth: .infinity, alignment: .leading)
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

// MARK: الأنواع

private struct ZakatTypesPage: View {
    let s: ZakatState
    let p: HomePalette
    let model: ZakatModel

    var body: some View {
        ZakatScaffold(s: s, p: p, title: s.title, subtitle: s.subtitle, symbol: "wallet.pass.fill", onBack: { model.back() }) {
            VStack(alignment: .leading, spacing: 12) {
                Text(s.heading).font(sys(17, .bold)).foregroundColor(p.primary)
                    .padding(.leading, 6)
                    .accessibilityAddTraits(.isHeader)
                ForEach(s.kinds) { k in
                    ZakatTypeCard(k: k, p: p) { model.open(k.key) }
                }
                ZakatNote(text: s.note, p: p)
            }
        }
    }
}

/// بطاقة النوع: رقمه في نجمة، وأيقونته، واسمه ووصفه
private struct ZakatTypeCard: View {
    let k: ZakatState.Kind
    let p: HomePalette
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                ZStack {
                    Star8().stroke(p.primary.opacity(0.55), lineWidth: 1.6)
                    Text(k.n).font(sys(14, .bold)).foregroundColor(p.primary)
                }
                .frame(width: 40, height: 40)
                .accessibilityHidden(true)
                Image(systemName: zakatKindSymbol(k.key))
                    .font(.system(size: 19, weight: .semibold)).foregroundColor(p.primary)
                    .frame(width: 44, height: 44)
                    .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.gold.opacity(0.15)))
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    Text(k.name).font(sys(17, .bold)).foregroundColor(p.onSurface)
                    Text(k.desc).font(sys(13)).foregroundColor(p.onSurfaceVariant).fixedSize(horizontal: false, vertical: true)
                }
                .multilineTextAlignment(.leading)
                Spacer(minLength: 0)
                Image(systemName: "chevron.forward")
                    .font(.system(size: 14, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
                    .accessibilityHidden(true)
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(p.surface).shadow(color: .black.opacity(0.04), radius: 3, y: 1))
            .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.8), lineWidth: 1))
            .contentShape(Rectangle())
        }
        .buttonStyle(PressScale(scale: 0.98))
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }
}

// MARK: الحاسبة

private struct ZakatCalcPage: View {
    let s: ZakatState
    let pg: ZakatState.Page
    let p: HomePalette
    let model: ZakatModel

    var body: some View {
        ZakatScaffold(s: s, p: p, title: pg.title, subtitle: pg.subtitle, symbol: zakatKindSymbol(pg.kind), onBack: { model.open("") }) {
            // على الشاشة الواسعة: المدخلات في عمود، والنتيجة وتفصيلها بجانبها
            if s.wide {
                HStack(alignment: .top, spacing: 20) {
                    column(pg.blocks.filter { !($0.side ?? false) })
                    column(pg.blocks.filter { $0.side ?? false })
                }
            } else {
                column(pg.blocks)
            }
        }
    }

    private func column(_ blocks: [ZakatState.Block]) -> some View {
        VStack(spacing: 14) {
            ForEach(blocks) { b in
                ZakatBlockView(b: b, s: s, p: p, model: model)
                    .transition(.opacity)
            }
        }
        .frame(maxWidth: .infinity, alignment: .top)
        .animation(UIAccessibility.isReduceMotionEnabled ? nil : .easeOut(duration: 0.25), value: blocks.map { $0.id })
    }
}

private struct ZakatBlockView: View {
    let b: ZakatState.Block
    let s: ZakatState
    let p: HomePalette
    let model: ZakatModel

    var body: some View {
        switch b.t {
        case "result": ZakatResultCard(b: b, p: p)
        case "card": ZakatCard(b: b, s: s, p: p, model: model)
        case "note": ZakatNote(text: b.text ?? "", p: p)
        case "evidence": ZakatEvidence(b: b, s: s, p: p)
        default: EmptyView()
        }
    }
}

/// بطاقة النتيجة بتدرج لون التطبيق: المبلغ يتدحرج عند التغير، وحالة النصاب، وشريط البلوغ إليه
private struct ZakatResultCard: View {
    let b: ZakatState.Block
    let p: HomePalette

    private var badgeColor: Color {
        switch b.ok {
        case .some(true): return Color(red: 0x3F / 255.0, green: 0xB5 / 255.0, blue: 0x95 / 255.0)
        case .some(false): return Color(red: 0xEF / 255.0, green: 0x8A / 255.0, blue: 0x76 / 255.0)
        case .none: return p.gold
        }
    }

    var body: some View {
        let still = UIAccessibility.isReduceMotionEnabled
        VStack(spacing: 8) {
            Text(b.label ?? "").font(sys(14)).foregroundColor(.white.opacity(0.85)).multilineTextAlignment(.center)
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(b.value ?? "").font(sys(36, .heavy)).foregroundColor(.white)
                    .lineLimit(1).minimumScaleFactor(0.5)
                    .modifier(NumericRoll(value: b.value))
                if let u = b.unit {
                    Text(u).font(sys(20, .semibold)).foregroundColor(.white.opacity(0.9)).lineLimit(1)
                }
            }
            if let badge = b.badge {
                Text(badge).font(sys(12, .bold)).foregroundColor(.white).multilineTextAlignment(.center)
                    .padding(.horizontal, 14).padding(.vertical, 5)
                    .background(Capsule().fill(badgeColor.opacity(0.25)))
                    .animation(still ? nil : .easeOut(duration: 0.3), value: b.ok)
            }
            if let pr = b.progress {
                GeometryReader { g in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Color.white.opacity(0.2))
                        Capsule().fill(p.gold).frame(width: g.size.width * CGFloat(min(max(pr, 0), 1)))
                    }
                }
                .frame(height: 8)
                .padding(.top, 4)
                .animation(still ? nil : .spring(response: 0.6, dampingFraction: 0.85), value: pr)
                .accessibilityHidden(true)
            }
        }
        .padding(22)
        .frame(maxWidth: .infinity)
        .background(
            ZStack(alignment: .bottomLeading) {
                LinearGradient(colors: [p.hero1, p.hero2], startPoint: .topLeading, endPoint: .bottomTrailing)
                StarEmblem(color: .white.opacity(0.06), radius: 72, stroke: 1).offset(x: -30, y: 40)
            }
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .accessibilityElement(children: .combine)
    }
}

/// بطاقة بيضاء بحافة رفيعة فيها صفوف (حقول وقيم واختيارات)
private struct ZakatCard: View {
    let b: ZakatState.Block
    let s: ZakatState
    let p: HomePalette
    let model: ZakatModel

    var body: some View {
        let rows = b.rows ?? []
        VStack(alignment: .leading, spacing: 12) {
            if let t = b.title {
                Text(t).font(sys(17, .bold)).foregroundColor(p.primary).accessibilityAddTraits(.isHeader)
            }
            ForEach(rows) { r in
                ZakatRowView(r: r, s: s, p: p, model: model)
                    .transition(.opacity)
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(p.surface).shadow(color: .black.opacity(0.04), radius: 3, y: 1))
        .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).strokeBorder(p.outlineVariant, lineWidth: 1))
        .animation(UIAccessibility.isReduceMotionEnabled ? nil : .easeOut(duration: 0.25), value: rows.map { $0.id })
    }
}

private struct ZakatRowView: View {
    let r: ZakatState.Row
    let s: ZakatState
    let p: HomePalette
    let model: ZakatModel

    var body: some View {
        switch r.t {
        case "field":
            ZakatField(r: r, p: p) { model.edit(r.key ?? "", $0) }
        case "stat":
            ZakatStat(r: r, s: s, p: p)
        case "check":
            Toggle(isOn: Binding(get: { r.on ?? false }, set: { model.toggle(r.key ?? "", $0) })) {
                Text(r.label ?? "").font(sys(15)).foregroundColor(p.onSurface)
            }
            .tint(p.primary)
        case "seg":
            VStack(alignment: .leading, spacing: 6) {
                if let l = r.label { Text(l).font(sys(13, .medium)).foregroundColor(p.onSurfaceVariant) }
                Picker(r.label ?? "", selection: Binding(get: { r.selected ?? "" }, set: { model.choose(r.key ?? "", $0) })) {
                    ForEach(r.options ?? []) { o in Text(o.label).tag(o.key) }
                }
                .pickerStyle(.segmented)
            }
        case "picker":
            ZakatMenuField(r: r, p: p) { model.choose(r.key ?? "", $0) }
        case "button":
            Button { model.share() } label: {
                HStack(spacing: 8) {
                    Image(systemName: "square.and.arrow.up").font(.system(size: 15, weight: .semibold))
                    Text(r.label ?? "").font(sys(15, .semibold))
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 13)
                .background(Capsule().fill(LinearGradient(colors: [p.hero1, p.hero2], startPoint: .leading, endPoint: .trailing)))
            }
            .buttonStyle(PressScale())
        default:
            EmptyView()
        }
    }
}

/// حقل عددي: النص محلي فلا يقفز المؤشر، وكل تعديل يُرسل إلى Kotlin الذي يحلله (والأرقام العربية والفارسية) ويحسب
private struct ZakatField: View {
    let r: ZakatState.Row
    let p: HomePalette
    let onEdit: (String) -> Void
    @State private var text: String
    @FocusState private var focused: Bool

    init(r: ZakatState.Row, p: HomePalette, onEdit: @escaping (String) -> Void) {
        self.r = r
        self.p = p
        self.onEdit = onEdit
        _text = State(initialValue: r.value ?? "")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(r.label ?? "").font(sys(13, .medium)).foregroundColor(p.onSurfaceVariant)
                .accessibilityHidden(true)
            HStack(spacing: 8) {
                TextField("", text: Binding(get: { text }, set: { v in text = v; onEdit(v) }))
                    .keyboardType(.decimalPad)
                    .focused($focused)
                    .font(sys(17, .semibold))
                    .foregroundColor(p.onSurface)
                    .padding(.vertical, 13)
                    .frame(maxWidth: .infinity)
                    .accessibilityLabel(r.label ?? "")
                    .accessibilityHint(r.suffix ?? "")
                if let u = r.suffix {
                    Text(u).font(sys(14, .medium)).foregroundColor(p.onSurfaceVariant).lineLimit(1).fixedSize()
                        .accessibilityHidden(true)
                }
            }
            .padding(.horizontal, 14)
            .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.background.opacity(0.7)))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(focused ? p.primary : p.outlineVariant, lineWidth: focused ? 1.6 : 1)
            )
            .animation(.easeOut(duration: 0.15), value: focused)
            if let h = r.hint {
                Text(h).font(sys(12)).foregroundColor(p.onSurfaceVariant).padding(.leading, 4)
            }
        }
    }
}

private struct ZakatStat: View {
    let r: ZakatState.Row
    let s: ZakatState
    let p: HomePalette

    var body: some View {
        HStack(spacing: 8) {
            Text(r.label ?? "").font(sys(14)).foregroundColor(p.onSurfaceVariant)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(r.value ?? "").font(sys(16, (r.strong ?? false) ? .heavy : .bold)).foregroundColor(p.onSurface)
                .multilineTextAlignment(.trailing)
                .modifier(NumericRoll(value: r.value))
        }
        .padding(.horizontal, 14).padding(.vertical, 11)
        .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(HomePalette.color(s.x.surfaceVariant).opacity(0.6)))
        .accessibilityElement(children: .combine)
    }
}

/// اختيار من قائمة (العملة): حقل يفتح قائمة النظام، والمختار فيها بعلامة صح
private struct ZakatMenuField: View {
    let r: ZakatState.Row
    let p: HomePalette
    let onChoose: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(r.label ?? "").font(sys(13, .medium)).foregroundColor(p.onSurfaceVariant)
                .accessibilityHidden(true)
            Menu {
                ForEach(r.options ?? []) { o in
                    Button { onChoose(o.key) } label: {
                        if o.key == r.selected { Label(o.label, systemImage: "checkmark") } else { Text(o.label) }
                    }
                }
            } label: {
                HStack {
                    Text(r.value ?? "").font(sys(15)).foregroundColor(p.onSurface).lineLimit(1)
                    Spacer(minLength: 6)
                    Image(systemName: "chevron.up.chevron.down").font(.system(size: 13, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
                }
                .padding(.horizontal, 14).padding(.vertical, 12)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.background.opacity(0.7)))
                .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(p.outlineVariant, lineWidth: 1))
            }
            .accessibilityLabel(r.label ?? "")
            .accessibilityValue(r.value ?? "")
        }
    }
}

private struct ZakatNote: View {
    let text: String
    let p: HomePalette

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "info.circle.fill").font(.system(size: 17)).foregroundColor(p.primary)
                .accessibilityHidden(true)
            Text(text).font(sys(14)).foregroundColor(p.onPrimaryContainer)
                .frame(maxWidth: .infinity, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.primaryContainer))
    }
}

// MARK: الأدلة

/// «الدليل وأقوال الفقهاء»: أنواع الأدلة في رأسه، وتنفتح بلمسة عن نقاطه (فكرتها ونوعها ونص الآية أو الحديث وشرحه ومصدره)
private struct ZakatEvidence: View {
    let b: ZakatState.Block
    let s: ZakatState
    let p: HomePalette
    @State private var open: Bool

    init(b: ZakatState.Block, s: ZakatState, p: HomePalette) {
        self.b = b
        self.s = s
        self.p = p
        _open = State(initialValue: b.open ?? false)
    }

    private func color(_ kind: String) -> Color {
        switch kind {
        case "quran": return HomePalette.color(s.x.quran)
        case "hadith": return HomePalette.color(s.x.hadith)
        case "consensus": return HomePalette.color(s.x.consensus)
        default: return HomePalette.color(s.x.scholars)
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button {
                if UIAccessibility.isReduceMotionEnabled { open.toggle() } else { withAnimation(.easeInOut(duration: 0.3)) { open.toggle() } }
            } label: {
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: "book.closed.fill").foregroundColor(p.goldDeep)
                        .frame(width: 42, height: 42)
                        .background(RoundedRectangle(cornerRadius: 13, style: .continuous).fill(p.gold.opacity(0.16)))
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(b.title ?? "").font(sys(16, .bold)).foregroundColor(p.onSurface)
                        Text(b.sub ?? "").font(sys(12)).foregroundColor(p.onSurfaceVariant)
                        HStack(spacing: 6) {
                            ForEach(b.kinds ?? [], id: \.self) { k in
                                ZakatKindChip(kind: k, label: s.kindLabels[k] ?? k, color: color(k))
                            }
                        }
                        .padding(.top, 3)
                    }
                    .multilineTextAlignment(.leading)
                    Spacer(minLength: 0)
                    Image(systemName: "chevron.down").font(.system(size: 14, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
                        .rotationEffect(.degrees(open ? 180 : 0))
                        .padding(.top, 12)
                        .accessibilityHidden(true)
                }
                .padding(16)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            if open {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array((b.points ?? []).enumerated()), id: \.offset) { i, pt in
                        if i > 0 {
                            Rectangle().fill(p.outlineVariant.opacity(0.6)).frame(height: 1).padding(.vertical, 12)
                        }
                        ZakatProofView(pt: pt, label: s.kindLabels[pt.kind] ?? pt.kind, color: color(pt.kind), p: p)
                    }
                }
                .padding(.horizontal, 16).padding(.bottom, 16)
                .transition(.opacity)
            }
        }
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(p.gold.opacity(0.45), lineWidth: 1))
    }
}

private struct ZakatKindChip: View {
    let kind: String
    let label: String
    let color: Color

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: zakatProofKindSymbol(kind)).font(.system(size: 10, weight: .bold))
            Text(label).font(sys(11, .bold)).lineLimit(1).minimumScaleFactor(0.7)
        }
        .foregroundColor(color)
        .padding(.horizontal, 8).padding(.vertical, 3)
        .background(Capsule().fill(color.opacity(0.12)))
    }
}

private struct ZakatProofView: View {
    let pt: ZakatState.Proof
    let label: String
    let color: Color
    let p: HomePalette

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: zakatConceptSymbol(pt.concept)).font(.system(size: 17)).foregroundColor(color)
                .frame(width: 38, height: 38)
                .background(Circle().fill(color.opacity(0.13)))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 6) {
                ZakatKindChip(kind: pt.kind, label: label, color: color)
                Text(pt.title).font(sys(15, .bold)).foregroundColor(p.onSurface)
                if let a = pt.ayah {
                    ZakatQuote(p: p) { Text(a).font(quranFont(19)).foregroundColor(p.onSurface).lineSpacing(10) }
                }
                if let t = pt.tr { Text(t).font(sys(12)).foregroundColor(p.onSurfaceVariant) }
                if let h = pt.hadith {
                    ZakatQuote(p: p) { Text(h).font(sys(15)).foregroundColor(p.onSurface).lineSpacing(6) }
                }
                Text(pt.body).font(sys(14)).foregroundColor(p.onSurface)
                HStack(spacing: 5) {
                    Image(systemName: zakatProofKindSymbol(pt.kind)).font(.system(size: 12))
                    Text(pt.source).font(sys(12, .bold))
                }
                .foregroundColor(color)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

/// الاقتباس (آية أو حديث) من اليمين، بخط جانبي ذهبي في بداية السطر
private struct ZakatQuote<Content: View>: View {
    let p: HomePalette
    @ViewBuilder let content: () -> Content

    var body: some View {
        content()
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
            .environment(\.layoutDirection, .rightToLeft)
            .padding(.vertical, 8).padding(.leading, 15).padding(.trailing, 12)
            .background(p.gold.opacity(0.08))
            .overlay(Rectangle().fill(p.gold).frame(width: 3), alignment: .leading)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}
