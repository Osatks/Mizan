import SwiftUI
import UIKit
import Shared

/// زكاة الفطر والفدية والكفارات بواجهة النظام (SwiftUI) بطراز الرئيسية (NativeHome.swift): ترويسة بتدرج لون التطبيق ونجمته
/// تحت شريط مثبّت فيه زر الرجوع (والعنوان يظهر في زجاجه عند التمرير)، وبطاقة النتيجة الخضراء، وبطاقة المدخلات
/// (عدّاد النظام، واختيار مقسّم، وقائمة، وحقول أرقام بلوحة الكسور)، والملاحظات، والأدلة — كما في FitrFidyaScreens.kt.
/// الحالة كلها من Kotlin (NativeFitrFidya.ios.kt) مع الإشعار MizanFitrFidyaState: الحساب والنصوص والأرقام هناك، وهنا الرسم فقط.
/// kind: ‏"fitr" لزكاة الفطر، و"fidya" للفدية والكفارات.
enum NativeFitrFidyaScreen {
    static func install() {
        NativeFitrFidya.shared.attach { session in FitrFidyaHostController(session: session) }
    }
}

final class FitrFidyaHostController: UIHostingController<FitrFidyaRoot> {
    init(session: NativeFitrFidyaSession) {
        let model = FitrFidyaModel(session: session)
        super.init(rootView: FitrFidyaRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        // الهوامش من Kotlin، ولوحة المفاتيح وحدها من النظام: يبقى الحقل ظاهراً فوقها
        if #available(iOS 16.4, *) { safeAreaRegions = .keyboard }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct FitrFidyaState: Decodable {
    struct Option: Decodable, Identifiable, Hashable { let key, label: String; var id: String { key } }
    /// صف في بطاقة المدخلات: stepper أو segmented أو menu أو field أو stat أو note
    struct Row: Decodable, Identifiable {
        let id, type: String
        let key, label, valueText, text, suffix, hint, selected: String?
        let value, min, max: Int?
        let options: [Option]?
        let strong: Bool?
    }
    struct Result: Decodable { let label, value: String; let unit, badge: String? }
    struct Proof: Decodable, Identifiable {
        let id, kind, concept, title, body, source: String
        let ayah, tr, hadith: String?
    }
    struct Evidence: Decodable { let key, title, sub: String; let kinds: [String: String]; let points: [Proof] }

    let kind: String
    let rtl, dark, wide: Bool
    let top, bottom, start, end, width: Double
    let back, hideKeyboard, title, subtitle: String
    let result: Result
    let rows: [Row]
    let notes: [String]
    let evidence: Evidence
    let c: HomeState.Colors

    static func decode(_ json: String) -> FitrFidyaState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(FitrFidyaState.self, from: data)
    }
}

final class FitrFidyaModel: ObservableObject {
    let session: NativeFitrFidyaSession
    @Published private(set) var state: FitrFidyaState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativeFitrFidyaSession) {
        self.session = session
        state = FitrFidyaState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanFitrFidyaState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = FitrFidyaState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    func step(_ key: String, _ value: Int) { session.step(key: key, value: Int32(value)) }
    func change(_ key: String, _ value: String) { session.change(key: key, value: value) }
    func back() { session.back() }
}

// MARK: - الشاشة

struct FitrFidyaRoot: View {
    @ObservedObject var model: FitrFidyaModel
    var body: some View {
        Group {
            if let s = model.state { FitrFidyaView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea(.container, edges: .all)
    }
}

private struct FitrFidyaHeaderBottomKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

private extension View {
    /// سحب الصفحة يُنزل لوحة المفاتيح (iOS 16)، وقبله زر في الشريط العلوي
    @ViewBuilder func fitrFidyaDismissKeyboardOnScroll() -> some View {
        if #available(iOS 16.0, *) { self.scrollDismissesKeyboard(.interactively) } else { self }
    }
}

struct FitrFidyaView: View {
    let s: FitrFidyaState
    let p: HomePalette
    let model: FitrFidyaModel
    @State private var headerBottom: CGFloat = .greatestFiniteMagnitude
    @FocusState private var focus: String?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// زجاج الشريط العلوي وعنوانه يظهران حين تمر الترويسة تحته
    private var glass: Double {
        let bar = CGFloat(s.top) + 52, ramp: CGFloat = 36
        return Double(min(max((bar + ramp - headerBottom) / ramp, 0), 1))
    }

    private var symbol: String { s.kind == "fitr" ? "leaf.fill" : "hands.sparkles.fill" }

    var body: some View {
        ZStack(alignment: .top) {
            p.background.ignoresSafeArea()
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    FitrFidyaHeader(title: s.title, subtitle: s.subtitle, symbol: symbol, s: s, p: p)
                        .background(GeometryReader { g in
                            Color.clear.preference(key: FitrFidyaHeaderBottomKey.self, value: g.frame(in: .named("fitrFidya")).maxY)
                        })
                    content
                        .padding(.leading, CGFloat(s.start))
                        .padding(.trailing, CGFloat(s.end))
                }
                .padding(.bottom, CGFloat(s.bottom))
            }
            .coordinateSpace(name: "fitrFidya")
            .onPreferenceChange(FitrFidyaHeaderBottomKey.self) { headerBottom = $0 }
            .fitrFidyaDismissKeyboardOnScroll()
            FitrFidyaBar(s: s, p: p, amount: glass, keyboard: focus != nil,
                         onBack: { focus = nil; model.back() }, onHide: { focus = nil })
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }

    /// عمود واحد على الهاتف (النتيجة، المدخلات، الملاحظات، الأدلة)، وعمودان على الأوسع كما في ToolPage
    @ViewBuilder private var content: some View {
        if s.wide {
            HStack(alignment: .top, spacing: 16) {
                VStack(spacing: 14) {
                    form
                    evidence
                }
                .frame(maxWidth: .infinity)
                VStack(spacing: 14) {
                    FitrFidyaHero(r: s.result, p: p)
                    notes
                }
                .frame(width: CGFloat(s.width) * 0.42)
            }
        } else {
            VStack(spacing: 14) {
                FitrFidyaHero(r: s.result, p: p)
                form
                notes
                evidence
            }
        }
    }

    /// المدخلات ونتائجها الجزئية؛ الصفوف تظهر وتختفي بحسب الاختيار (من Kotlin) بتلاشٍ لطيف
    private var form: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(s.rows) { row in
                FitrFidyaRow(row: row, p: p, model: model, focus: $focus)
                    .transition(.opacity)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.6), lineWidth: 1))
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.25), value: s.rows.map { $0.id })
    }

    private var notes: some View {
        VStack(spacing: 14) {
            ForEach(Array(s.notes.enumerated()), id: \.offset) { _, n in FitrFidyaNote(text: n, p: p) }
        }
    }

    private var evidence: some View {
        // أدلة كل تبويب تبدأ مطوية (كما في EvidenceCard)
        FitrFidyaEvidence(e: s.evidence, p: p).id(s.evidence.key)
    }
}

// MARK: الشريط والترويسة

/// الشريط العلوي المثبّت: زر الرجوع دائماً، وزر إنزال لوحة المفاتيح حين تكون ظاهرة، وزجاج بالعنوان عند التمرير
private struct FitrFidyaBar: View {
    let s: FitrFidyaState
    let p: HomePalette
    let amount: Double
    let keyboard: Bool
    let onBack: () -> Void
    let onHide: () -> Void
    var body: some View {
        ZStack {
            Text(s.title)
                .font(sys(16, .medium)).foregroundColor(p.onSurface).lineLimit(1)
                .padding(.horizontal, 64)
                .opacity(amount)
                .allowsHitTesting(false)
                .accessibilityHidden(true)
            HStack {
                FitrFidyaRoundButton(symbol: "chevron.backward", label: s.back, p: p, action: onBack)
                Spacer()
                if keyboard {
                    FitrFidyaRoundButton(symbol: "keyboard.chevron.compact.down", label: s.hideKeyboard, p: p, action: onHide)
                        .transition(.opacity)
                }
            }
            .padding(.leading, CGFloat(max(s.start - 4, 12)))
            .padding(.trailing, CGFloat(max(s.end - 4, 12)))
        }
        .frame(height: 52)
        .padding(.top, CGFloat(s.top))
        .background(
            Rectangle().fill(.ultraThinMaterial)
                .overlay(Rectangle().fill(p.outlineVariant.opacity(0.8)).frame(height: 1), alignment: .bottom)
                .opacity(amount)
                .allowsHitTesting(false)
        )
    }
}

private struct FitrFidyaRoundButton: View {
    let symbol: String
    let label: String
    let p: HomePalette
    let action: () -> Void
    var body: some View {
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

/// ترويسة بطراز ترويسة الرئيسية: تدرج من لون التطبيق الفاتح، ونجمة باهتة، وأيقونة الأداة والعنوان والوصف (تحت الشريط المثبّت)
private struct FitrFidyaHeader: View {
    let title: String
    let subtitle: String
    let symbol: String
    let s: FitrFidyaState
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
        .padding(.top, CGFloat(s.top) + 52 + 4)
        .padding(.bottom, 18)
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

// MARK: النتيجة

/// بطاقة النتيجة الكبيرة بتدرج لون التطبيق (ResultCard): القيمة تتدحرج عند التغيّر، ووحدتها بجانبها، وشارة العدد تحتها
private struct FitrFidyaHero: View {
    let r: FitrFidyaState.Result
    let p: HomePalette
    var body: some View {
        VStack(spacing: 6) {
            Text(r.label).font(sys(14, .medium)).foregroundColor(.white.opacity(0.85)).multilineTextAlignment(.center)
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(r.value).font(sys(38, .heavy)).foregroundColor(.white).lineLimit(1).minimumScaleFactor(0.5)
                    .modifier(NumericRoll(value: r.value))
                if let u = r.unit {
                    Text(u).font(sys(20, .semibold)).foregroundColor(.white.opacity(0.9))
                }
            }
            if let b = r.badge {
                Text(b).font(sys(12, .bold)).foregroundColor(.white).multilineTextAlignment(.center)
                    .padding(.horizontal, 14).padding(.vertical, 5)
                    .background(Capsule().fill(Color(red: 0.247, green: 0.71, blue: 0.584).opacity(0.25)))
                    .modifier(NumericRoll(value: b))
            }
        }
        .padding(22)
        .frame(maxWidth: .infinity)
        .background(
            ZStack(alignment: .bottomLeading) {
                LinearGradient(colors: [p.hero1, p.hero2], startPoint: .topLeading, endPoint: .bottomTrailing)
                StarEmblem(color: .white.opacity(0.07), radius: 72, stroke: 1).offset(x: -30, y: 40)
            }
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .accessibilityElement(children: .combine)
    }
}

// MARK: الصفوف

private struct FitrFidyaRow: View {
    let row: FitrFidyaState.Row
    let p: HomePalette
    let model: FitrFidyaModel
    var focus: FocusState<String?>.Binding

    var body: some View {
        switch row.type {
        case "stepper": FitrFidyaStepper(row: row, p: p, model: model)
        case "segmented": FitrFidyaSegmented(row: row, model: model)
        case "menu": FitrFidyaMenu(row: row, p: p, model: model)
        case "field": FitrFidyaField(row: row, p: p, model: model, focus: focus)
        case "stat": FitrFidyaStat(row: row, p: p)
        case "note": FitrFidyaNote(text: row.label ?? "", p: p)
        default: EmptyView()
        }
    }
}

/// عدّاد النظام، والعدد منسّق من Kotlin بأرقام التطبيق
private struct FitrFidyaStepper: View {
    let row: FitrFidyaState.Row
    let p: HomePalette
    let model: FitrFidyaModel
    var body: some View {
        let lo = row.min ?? 0
        let hi = Swift.max(row.max ?? 99, lo)
        let key = row.key ?? row.id
        Stepper(value: Binding(get: { row.value ?? lo }, set: { v in
            UISelectionFeedbackGenerator().selectionChanged()
            model.step(key, v)
        }), in: lo...hi) {
            HStack(spacing: 8) {
                Text(row.label ?? "").font(sys(16, .semibold)).foregroundColor(p.onSurface)
                Spacer(minLength: 4)
                Text(row.valueText ?? "").font(sys(18, .bold)).monospacedDigit().foregroundColor(p.primary)
                    .modifier(NumericRoll(value: row.valueText))
            }
        }
        .accessibilityValue(row.valueText ?? "")
    }
}

private struct FitrFidyaSegmented: View {
    let row: FitrFidyaState.Row
    let model: FitrFidyaModel
    var body: some View {
        let key = row.key ?? row.id
        Picker(row.label ?? "", selection: Binding(get: { row.selected ?? "" }, set: { v in
            if v != row.selected { UISelectionFeedbackGenerator().selectionChanged() }
            model.change(key, v)
        })) {
            ForEach(row.options ?? []) { o in Text(o.label).tag(o.key) }
        }
        .pickerStyle(.segmented)
        .labelsHidden()
    }
}

/// قائمة النظام بحقل كحقول المواقيت، والمختار بعلامة صح
private struct FitrFidyaMenu: View {
    let row: FitrFidyaState.Row
    let p: HomePalette
    let model: FitrFidyaModel
    private var current: String { row.options?.first(where: { $0.key == row.selected })?.label ?? "" }
    var body: some View {
        let key = row.key ?? row.id
        VStack(alignment: .leading, spacing: 6) {
            Text(row.label ?? "").font(sys(13, .medium)).foregroundColor(p.onSurfaceVariant)
                .accessibilityHidden(true)
            Menu {
                ForEach(row.options ?? []) { o in
                    Button { model.change(key, o.key) } label: {
                        if o.key == row.selected { Label(o.label, systemImage: "checkmark") } else { Text(o.label) }
                    }
                }
            } label: {
                HStack {
                    Text(current).font(sys(15)).foregroundColor(p.onSurface).lineLimit(2).multilineTextAlignment(.leading)
                    Spacer(minLength: 6)
                    Image(systemName: "chevron.up.chevron.down").font(.system(size: 13, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
                }
                .padding(.horizontal, 14).padding(.vertical, 12)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.background.opacity(0.7)))
                .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(p.outlineVariant, lineWidth: 1))
            }
            .accessibilityLabel(row.label ?? "")
            .accessibilityValue(current)
        }
    }
}

/// حقل رقم بلوحة الكسور: النص يبقى هنا أثناء الكتابة، وكل تغيير يذهب إلى Kotlin فيحسب ويعيد النتائج
private struct FitrFidyaField: View {
    let row: FitrFidyaState.Row
    let p: HomePalette
    let model: FitrFidyaModel
    var focus: FocusState<String?>.Binding
    @State private var text: String

    init(row: FitrFidyaState.Row, p: HomePalette, model: FitrFidyaModel, focus: FocusState<String?>.Binding) {
        self.row = row
        self.p = p
        self.model = model
        self.focus = focus
        _text = State(initialValue: row.text ?? "")
    }

    private var focused: Bool { focus.wrappedValue == row.id }

    var body: some View {
        let key = row.key ?? row.id
        VStack(alignment: .leading, spacing: 6) {
            Text(row.label ?? "").font(sys(13, .medium)).foregroundColor(p.onSurfaceVariant)
                .accessibilityHidden(true)
            HStack(spacing: 8) {
                TextField("", text: Binding(get: { text }, set: { v in
                    text = v
                    model.change(key, v)
                }))
                .keyboardType(.decimalPad)
                .font(sys(17, .semibold))
                .foregroundColor(p.onSurface)
                .focused(focus, equals: row.id)
                .frame(maxWidth: .infinity)
                .accessibilityLabel(row.label ?? "")
                .accessibilityHint(row.hint ?? "")
                if let u = row.suffix {
                    Text(u).font(sys(15)).foregroundColor(p.onSurfaceVariant).accessibilityHidden(true)
                }
            }
            .padding(.horizontal, 14).padding(.vertical, 12)
            .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.background.opacity(0.7)))
            .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(focused ? p.primary : p.outlineVariant, lineWidth: focused ? 1.5 : 1))
            if let h = row.hint {
                Text(h).font(sys(12)).foregroundColor(p.onSurfaceVariant).accessibilityHidden(true)
            }
        }
    }
}

/// سطر نتيجة جزئية (StatRow): العنوان في البداية والقيمة في النهاية
private struct FitrFidyaStat: View {
    let row: FitrFidyaState.Row
    let p: HomePalette
    var body: some View {
        let strong = row.strong ?? false
        HStack(spacing: 8) {
            Text(row.label ?? "").font(sys(14)).foregroundColor(p.onSurfaceVariant)
            Spacer(minLength: 8)
            Text(row.valueText ?? "").font(sys(strong ? 17 : 16, strong ? .heavy : .bold))
                .foregroundColor(strong ? p.primary : p.onSurface)
                .multilineTextAlignment(.trailing)
                .modifier(NumericRoll(value: row.valueText))
        }
        .padding(.horizontal, 14).padding(.vertical, 11)
        .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(p.background))
        .accessibilityElement(children: .combine)
    }
}

/// ملاحظة بخلفية لون التطبيق الفاتح (Note)
private struct FitrFidyaNote: View {
    let text: String
    let p: HomePalette
    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "info.circle.fill").font(.system(size: 17)).foregroundColor(p.primary)
                .accessibilityHidden(true)
            Text(text).font(sys(14)).foregroundColor(p.onPrimaryContainer)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.primaryContainer))
    }
}

// MARK: الأدلة

/// أيقونة فكرة كل نقطة من الدليل (conceptIcon في EvidenceCard.kt)
private func fitrFidyaConceptSymbol(_ c: String) -> String {
    switch c {
    case "people": return "person.3.fill"
    case "time": return "clock.fill"
    case "money", "value": return "banknote.fill"
    case "fast", "moon": return "moon.stars.fill"
    case "food": return "fork.knife"
    case "feed": return "hands.sparkles.fill"
    case "family": return "figure.2.and.child.holdinghands"
    case "order": return "hammer.fill"
    case "scholars": return "bubble.left.and.bubble.right.fill"
    case "duty": return "checkmark.seal.fill"
    case "quran": return "book.fill"
    default: return "lightbulb.fill"
    }
}

/// «الدليل وأقوال الفقهاء»: تنفتح بلمسة، ولكل نقطة أيقونة فكرتها ونوعها ونص الآية أو الحديث وشرحها ومصدرها
private struct FitrFidyaEvidence: View {
    let e: FitrFidyaState.Evidence
    let p: HomePalette
    @State private var open = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private func color(_ kind: String) -> Color {
        switch kind {
        case "quran": return p.goldDeep
        case "hadith": return p.primary
        default: return p.goldDeep.opacity(0.85)
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

    private var kinds: [String] {
        var seen: [String] = []
        for pt in e.points where !seen.contains(pt.kind) { seen.append(pt.kind) }
        return seen
    }

    private func chip(_ kind: String) -> some View {
        Label(e.kinds[kind] ?? kind, systemImage: symbol(kind))
            .font(sys(11, .bold)).foregroundColor(color(kind)).lineLimit(1)
            .padding(.horizontal, 8).padding(.vertical, 3)
            .background(Capsule().fill(color(kind).opacity(0.12)))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button { withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.3)) { open.toggle() } } label: {
                HStack(spacing: 12) {
                    Image(systemName: "book.closed.fill").foregroundColor(p.goldDeep)
                        .frame(width: 42, height: 42)
                        .background(RoundedRectangle(cornerRadius: 13, style: .continuous).fill(p.gold.opacity(0.16)))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(e.title).font(sys(16, .bold)).foregroundColor(p.onSurface)
                        Text(e.sub).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                        HStack(spacing: 6) {
                            ForEach(kinds, id: \.self) { k in chip(k) }
                        }
                        .padding(.top, 4)
                    }
                    Spacer(minLength: 0)
                    Image(systemName: "chevron.down").font(.system(size: 14, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
                        .rotationEffect(.degrees(open ? 180 : 0))
                        .accessibilityHidden(true)
                }
                .padding(16)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            if open {
                VStack(alignment: .leading, spacing: 12) {
                    ForEach(Array(e.points.enumerated()), id: \.element.id) { i, pt in
                        if i > 0 { Rectangle().fill(p.outlineVariant.opacity(0.6)).frame(height: 1) }
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

    private func point(_ pt: FitrFidyaState.Proof) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: fitrFidyaConceptSymbol(pt.concept))
                .font(.system(size: 16, weight: .semibold)).foregroundColor(color(pt.kind))
                .frame(width: 38, height: 38)
                .background(Circle().fill(color(pt.kind).opacity(0.13)))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 6) {
                chip(pt.kind)
                Text(pt.title).font(sys(14, .bold)).foregroundColor(p.onSurface)
                if let a = pt.ayah {
                    FitrFidyaQuote(p: p) {
                        Text(a).font(quranFont(19)).foregroundColor(p.onSurface).lineSpacing(8)
                            .multilineTextAlignment(.leading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .environment(\.layoutDirection, .rightToLeft)
                    }
                }
                if let t = pt.tr { Text(t).font(sys(12)).foregroundColor(p.onSurfaceVariant) }
                if let h = pt.hadith {
                    FitrFidyaQuote(p: p) {
                        Text(h).font(sys(14)).foregroundColor(p.onSurface).lineSpacing(6)
                            .multilineTextAlignment(.leading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .environment(\.layoutDirection, .rightToLeft)
                    }
                }
                Text(pt.body).font(sys(14)).foregroundColor(p.onSurface)
                Label(pt.source, systemImage: symbol(pt.kind))
                    .font(sys(12, .bold)).foregroundColor(color(pt.kind))
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

/// الاقتباس (آية أو حديث) بخط ذهبي في جهة البداية (Quote في EvidenceCard.kt)
private struct FitrFidyaQuote<Content: View>: View {
    let p: HomePalette
    @ViewBuilder let content: () -> Content
    var body: some View {
        content()
            .padding(.vertical, 8)
            .padding(.leading, 15)
            .padding(.trailing, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(p.gold.opacity(0.08))
            .overlay(Rectangle().fill(p.gold).frame(width: 3), alignment: .leading)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}
