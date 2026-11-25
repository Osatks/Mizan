import SwiftUI
import UIKit
import Shared

/// حاسبة المواريث بواجهة النظام (SwiftUI) بطراز الرئيسية (NativeHome.swift): ترويسة بتدرج لون التطبيق ونجمته، وشريط علوي ثابت
/// بزر الرجوع، وأصل المسألة في بطاقة بارزة، وأنصبة الورثة بأشرطة ملونة وكسورها ومبالغها وأدلتها والمحجوبين، والملاحظات،
/// ثم الميت والتركة (حقول أرقام)، والورثة بعدّاد أو مفتاح لكل وارث، و«مسألة جديدة»، والأدلة والتنبيه — كما في InheritanceScreen.kt.
/// الحالة من Kotlin (NativeInheritance.ios.kt) مع الإشعار MizanInheritanceState، وكل لمسة تعود إليه فيحسب القسمة وينسّقها.
enum NativeInheritanceScreen {
    static func install() {
        NativeInheritance.shared.attach { session in InheritanceHostController(session: session) }
    }
}

final class InheritanceHostController: UIHostingController<InheritanceRoot> {
    init(session: NativeInheritanceSession) {
        let model = InheritanceModel(session: session)
        super.init(rootView: InheritanceRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        // الحواف من Kotlin، ويبقى حيّز لوحة المفاتيح فتنزاح الحقول فوقها
        if #available(iOS 16.4, *) { safeAreaRegions = .keyboard }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct InheritanceState: Decodable {
    struct Hero: Decodable { let label, value, badge, badgeColor: String; let tashih: String? }
    struct ShareRow: Decodable, Identifiable {
        let key, name: String
        let blocked, share: String?
        let value: Double?
        let detail, source, color: String?
        var id: String { key }
    }
    struct EstateField: Decodable, Identifiable { let key, label, value: String; let hint: String?; var id: String { key } }
    struct Estate: Decodable {
        let title, male, female, gender: String
        let fields: [EstateField]
        let netLabel: String
        let net: String?
    }
    struct HeirItem: Decodable, Identifiable { let key, name: String; let count: Int; let countText: String; let max: Int; var id: String { key } }
    struct HeirGroup: Decodable, Identifiable { let key, title: String; let heirs: [HeirItem]; var id: String { key } }
    struct Point: Decodable { let kind, concept, title, body, source: String; let ayah, tr, hadith: String? }
    struct Proofs: Decodable {
        let title, sub: String
        let open: Bool
        let kinds: [String: String]
        let kindOrder: [String]
        let points: [Point]
    }
    struct Extra: Decodable { let secondary, secondaryContainer, onSecondaryContainer, error, surfaceVariant, outline, tertiary: String }

    let rtl, dark, wide: Bool
    let top, bottom, start, end, width: Double
    let title, subtitle, back, close: String
    let hero: Hero
    let sharesTitle, shareLabel: String
    let shares: [ShareRow]?
    let notes: [String]
    let estate: Estate
    let groups: [HeirGroup]
    let newCase: String
    let resetTick: Int
    let evidence: Proofs
    let disclaimer: String
    let c: HomeState.Colors
    let x: Extra

    static func decode(_ json: String) -> InheritanceState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(InheritanceState.self, from: data)
    }
}

final class InheritanceModel: ObservableObject {
    let session: NativeInheritanceSession
    @Published private(set) var state: InheritanceState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativeInheritanceSession) {
        self.session = session
        state = InheritanceState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanInheritanceState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = InheritanceState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    func back() { session.back() }
    func gender(_ key: String) { session.gender(key: key) }
    func heir(_ key: String, _ count: Int) { session.heir(key: key, count: Int32(count)) }
    func field(_ key: String, _ text: String) { session.field(key: key, text: text) }
    func reset() { session.reset() }
    func share() { session.share() }
}

/// ألوان إضافية من MaterialTheme: الملاحظة التحذيرية، والحجب، وخلفية الأشرطة، وأنواع الأدلة
struct InheritancePalette {
    let secondary, secondaryContainer, onSecondaryContainer, error, surfaceVariant, outline, tertiary: Color

    init(_ x: InheritanceState.Extra) {
        secondary = HomePalette.color(x.secondary); secondaryContainer = HomePalette.color(x.secondaryContainer)
        onSecondaryContainer = HomePalette.color(x.onSecondaryContainer); error = HomePalette.color(x.error)
        surfaceVariant = HomePalette.color(x.surfaceVariant); outline = HomePalette.color(x.outline)
        tertiary = HomePalette.color(x.tertiary)
    }
}

// MARK: - الشاشة

struct InheritanceRoot: View {
    @ObservedObject var model: InheritanceModel
    var body: some View {
        Group {
            if let s = model.state {
                InheritanceView(s: s, p: HomePalette(s.c), x: InheritancePalette(s.x), model: model)
            } else { Color.clear }
        }
        // حيّز لوحة المفاتيح وحده يبقى (والحواف الأخرى من Kotlin)
        .ignoresSafeArea(.container, edges: .all)
    }
}

private struct InheritanceHeaderBottomKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

/// سحب الصفحة يخفي لوحة المفاتيح (iOS 16)
private struct InheritanceKeyboardDismiss: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) { content.scrollDismissesKeyboard(.interactively) } else { content }
    }
}

struct InheritanceView: View {
    let s: InheritanceState
    let p: HomePalette
    let x: InheritancePalette
    let model: InheritanceModel
    @State private var headerBottom: CGFloat = .greatestFiniteMagnitude
    @FocusState private var focused: String?

    /// شفافية زجاج الشريط العلوي: 0 والصفحة في أعلاها، و1 حين يمر طرف الترويسة تحته
    private var glass: Double {
        let bar = CGFloat(s.top) + 52, ramp: CGFloat = 36
        return Double(min(max((bar + ramp - headerBottom) / ramp, 0), 1))
    }

    var body: some View {
        ZStack(alignment: .top) {
            p.background.ignoresSafeArea()
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    InheritanceHeader(s: s, p: p)
                        .background(GeometryReader { g in
                            Color.clear.preference(key: InheritanceHeaderBottomKey.self, value: g.frame(in: .named("inheritance")).maxY)
                        })
                    content
                        .padding(.top, 6)
                        .padding(.leading, CGFloat(s.start))
                        .padding(.trailing, CGFloat(s.end))
                }
                .padding(.bottom, CGFloat(s.bottom))
            }
            .coordinateSpace(name: "inheritance")
            .onPreferenceChange(InheritanceHeaderBottomKey.self) { headerBottom = $0 }
            .modifier(InheritanceKeyboardDismiss())
            InheritanceTopBar(s: s, p: p, amount: glass, typing: focused != nil, onBack: { model.back() }, onDone: { focused = nil })
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }

    /// عمود واحد على الهاتف (القسمة أولاً)، وعلى الأوسع المدخلات والقسمة متجاورتان كما في ToolPage
    @ViewBuilder private var content: some View {
        if s.wide {
            HStack(alignment: .top, spacing: 20) {
                VStack(spacing: 14) { inputs }.frame(maxWidth: .infinity)
                VStack(spacing: 14) { results }.frame(maxWidth: .infinity)
            }
        } else {
            VStack(spacing: 14) {
                results
                inputs
            }
        }
    }

    @ViewBuilder private var results: some View {
        InheritanceHero(h: s.hero, p: p)
        if let rows = s.shares {
            InheritanceSharesCard(s: s, rows: rows, p: p, x: x, model: model)
        }
        ForEach(Array(s.notes.enumerated()), id: \.offset) { _, n in
            InheritanceNote(text: n, warn: false, p: p, x: x)
        }
    }

    @ViewBuilder private var inputs: some View {
        InheritanceEstateCard(e: s.estate, resetTick: s.resetTick, p: p, x: x, model: model, focused: $focused)
        ForEach(s.groups) { g in
            InheritanceGroupCard(g: g, p: p, model: model)
        }
        Button { focused = nil; model.reset() } label: {
            Label(s.newCase, systemImage: "arrow.counterclockwise")
                .font(sys(15, .semibold)).foregroundColor(p.primary)
                .frame(maxWidth: .infinity).padding(.vertical, 13)
                .background(Capsule().strokeBorder(p.outlineVariant, lineWidth: 1))
                .contentShape(Capsule())
        }
        .buttonStyle(PressScale())
        InheritanceEvidence(e: s.evidence, p: p, x: x)
        InheritanceNote(text: s.disclaimer, warn: true, p: p, x: x)
    }
}

// MARK: الترويسة والشريط

/// ترويسة بطراز الرئيسية: تدرج من لون التطبيق الفاتح ونجمة باهتة، وأيقونة الأداة والعنوان؛ وفوقها مكان زر الرجوع الثابت
private struct InheritanceHeader: View {
    let s: InheritanceState
    let p: HomePalette
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "person.3.fill")
                .font(.system(size: 18, weight: .semibold)).foregroundColor(p.primary)
                .frame(width: 46, height: 46)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.gold.opacity(0.18)))
            VStack(alignment: .leading, spacing: 1) {
                Text(s.title).font(sys(26, .heavy)).foregroundColor(p.primary).lineLimit(2).minimumScaleFactor(0.8)
                Text(s.subtitle).font(sys(14)).foregroundColor(p.onSurfaceVariant).lineLimit(2)
            }
            Spacer(minLength: 0)
        }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
        .padding(.leading, CGFloat(s.start) + 4)
        .padding(.trailing, CGFloat(s.end) + 4)
        .padding(.top, CGFloat(s.top) + 60)
        .padding(.bottom, 16)
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
    }
}

/// الشريط العلوي الثابت: زر الرجوع دائماً، ويظهر الزجاج والعنوان حين تمر الترويسة تحته؛ وزر إخفاء لوحة المفاتيح أثناء الكتابة
private struct InheritanceTopBar: View {
    let s: InheritanceState
    let p: HomePalette
    let amount: Double
    let typing: Bool
    let onBack: () -> Void
    let onDone: () -> Void
    var body: some View {
        ZStack {
            Text(s.title)
                .font(sys(16, .medium)).foregroundColor(p.onSurface).lineLimit(1)
                .padding(.horizontal, 64)
                .opacity(amount)
                .allowsHitTesting(false)
                .accessibilityHidden(true)
            HStack {
                InheritanceRoundButton(symbol: "chevron.backward", label: s.back, p: p, action: onBack)
                Spacer(minLength: 0)
                if typing {
                    InheritanceRoundButton(symbol: "keyboard.chevron.compact.down", label: s.close, p: p, action: onDone)
                        .transition(.opacity)
                }
            }
            .padding(.leading, CGFloat(s.start) + 4)
            .padding(.trailing, CGFloat(s.end) + 4)
        }
        .frame(height: 52)
        .padding(.top, CGFloat(s.top))
        .frame(maxWidth: .infinity)
        .background(
            VStack(spacing: 0) {
                Rectangle().fill(.ultraThinMaterial)
                Rectangle().fill(p.outlineVariant.opacity(0.8)).frame(height: 1)
            }
            .opacity(amount)
            .allowsHitTesting(amount > 0.5)
        )
        .animation(UIAccessibility.isReduceMotionEnabled ? nil : .easeOut(duration: 0.2), value: typing)
    }
}

private struct InheritanceRoundButton: View {
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

// MARK: الأجزاء

/// بطاقة بيضاء بحافة رفيعة كبطاقات الرئيسية (SectionCard)
private struct InheritanceCard<Content: View>: View {
    let p: HomePalette
    var title: String? = nil
    @ViewBuilder let content: () -> Content
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            if let t = title { Text(t).font(sys(18, .heavy)).foregroundColor(p.primary).accessibilityAddTraits(.isHeader) }
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.6), lineWidth: 1))
    }
}

/// أصل المسألة (والعول إن كان) في بطاقة بتدرج لون التطبيق، وحالتها (عادلة أو عائلة أو ردّ)، والتصحيح
private struct InheritanceHero: View {
    let h: InheritanceState.Hero
    let p: HomePalette
    var body: some View {
        VStack(spacing: 6) {
            Text(h.label).font(sys(14)).foregroundColor(.white.opacity(0.85)).multilineTextAlignment(.center)
            Text(h.value).font(sys(34, .heavy)).foregroundColor(.white).multilineTextAlignment(.center)
                .modifier(NumericRoll(value: h.value))
            Text(h.badge).font(sys(12, .bold)).foregroundColor(.white).multilineTextAlignment(.center)
                .padding(.horizontal, 14).padding(.vertical, 5)
                .background(Capsule().fill(HomePalette.color(h.badgeColor).opacity(0.25)))
                .animation(UIAccessibility.isReduceMotionEnabled ? nil : .easeOut(duration: 0.3), value: h.badgeColor)
            if let t = h.tashih {
                Text(t).font(sys(14)).foregroundColor(.white.opacity(0.85)).multilineTextAlignment(.center)
            }
        }
        .padding(22)
        .frame(maxWidth: .infinity)
        .background(
            ZStack(alignment: .bottomLeading) {
                LinearGradient(colors: [p.hero1, p.hero2], startPoint: .topLeading, endPoint: .bottomTrailing)
                StarEmblem(color: .white.opacity(0.07), radius: 72, stroke: 1).offset(x: -30, y: 30)
            }
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .accessibilityElement(children: .combine)
    }
}

/// الأنصبة: لكل وارث كسره ونسبته وشريطه ونوع استحقاقه ومبلغه ودليله، والمحجوب ومن حجبه، ثم مشاركة القسمة
private struct InheritanceSharesCard: View {
    let s: InheritanceState
    let rows: [InheritanceState.ShareRow]
    let p: HomePalette
    let x: InheritancePalette
    let model: InheritanceModel
    var body: some View {
        InheritanceCard(p: p, title: s.sharesTitle) {
            ForEach(rows) { r in
                if let b = r.blocked {
                    HStack(alignment: .firstTextBaseline, spacing: 8) {
                        Text(r.name).font(sys(15)).foregroundColor(p.onSurfaceVariant)
                        Spacer(minLength: 8)
                        Text(b).font(sys(12)).foregroundColor(x.error).multilineTextAlignment(.trailing)
                    }
                    .padding(12)
                    .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(x.surfaceVariant.opacity(0.5)))
                    .accessibilityElement(children: .combine)
                } else {
                    InheritanceShareRow(r: r, p: p, track: x.surfaceVariant)
                        .padding(.vertical, 2)
                }
            }
            Button { model.share() } label: {
                Label(s.shareLabel, systemImage: "square.and.arrow.up")
                    .font(sys(15, .semibold)).foregroundColor(.white)
                    .frame(maxWidth: .infinity).padding(.vertical, 13)
                    .background(Capsule().fill(LinearGradient(colors: [p.hero1, p.hero2], startPoint: .leading, endPoint: .trailing)))
            }
            .buttonStyle(PressScale())
            .padding(.top, 4)
        }
    }
}

private struct InheritanceShareRow: View {
    let r: InheritanceState.ShareRow
    let p: HomePalette
    let track: Color
    /// الشريط يمتد من الصفر إلى النصيب عند الظهور ويتحرك عند كل تغيير
    @State private var shown: Double = 0

    var body: some View {
        let color = HomePalette.color(r.color ?? "#FF0D5C4B")
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(r.name).font(sys(15, .bold)).foregroundColor(p.onSurface)
                Spacer(minLength: 8)
                Text(r.share ?? "").font(sys(15, .bold)).foregroundColor(p.onSurface)
                    .modifier(NumericRoll(value: r.share))
            }
            GeometryReader { g in
                ZStack(alignment: .leading) {
                    Capsule().fill(track)
                    if shown > 0 {
                        Capsule().fill(color).frame(width: max(g.size.width * CGFloat(shown), 10))
                    }
                }
            }
            .frame(height: 10)
            .accessibilityHidden(true)
            if let d = r.detail {
                Text(d).font(sys(12)).foregroundColor(p.onSurfaceVariant)
            }
            if let src = r.source {
                HStack(spacing: 4) {
                    Image(systemName: "book.fill").font(.system(size: 11, weight: .semibold)).foregroundColor(p.goldDeep)
                    Text(src).font(sys(12, .bold)).foregroundColor(p.goldDeep)
                }
            }
        }
        .accessibilityElement(children: .combine)
        .onAppear { grow(r.value ?? 0) }
        .onChange(of: r.value ?? 0) { v in grow(v) }
    }

    private func grow(_ v: Double) {
        let target = min(max(v, 0), 1)
        if UIAccessibility.isReduceMotionEnabled { shown = target } else {
            withAnimation(.spring(response: 0.7, dampingFraction: 0.8)) { shown = target }
        }
    }
}

/// ملاحظة (معلومة) أو تنبيه، كـ Note في Compose
private struct InheritanceNote: View {
    let text: String
    let warn: Bool
    let p: HomePalette
    let x: InheritancePalette
    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: warn ? "exclamationmark.triangle.fill" : "info.circle.fill")
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(warn ? x.secondary : p.primary)
                .accessibilityHidden(true)
            Text(text).font(sys(14)).foregroundColor(warn ? x.onSecondaryContainer : p.onPrimaryContainer)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(warn ? x.secondaryContainer : p.primaryContainer))
        .accessibilityElement(children: .combine)
    }
}

/// الميت والتركة: جنس الميت، والتركة ومؤن التجهيز والديون والوصية (حقول أرقام)، وصافي التركة
private struct InheritanceEstateCard: View {
    let e: InheritanceState.Estate
    let resetTick: Int
    let p: HomePalette
    let x: InheritancePalette
    let model: InheritanceModel
    let focused: FocusState<String?>.Binding
    var body: some View {
        InheritanceCard(p: p, title: e.title) {
            Picker(e.title, selection: Binding(get: { e.gender }, set: { model.gender($0) })) {
                Text(e.male).tag("m")
                Text(e.female).tag("f")
            }
            .pickerStyle(.segmented)
            ForEach(e.fields) { f in
                // «مسألة جديدة» تعيد إنشاء الحقل فيبدأ من نص Kotlin (فارغاً)
                InheritanceAmountField(f: f, p: p, model: model, focused: focused)
                    .id("\(f.key)-\(resetTick)")
            }
            if let n = e.net {
                HStack(spacing: 8) {
                    Text(e.netLabel).font(sys(14)).foregroundColor(p.onSurfaceVariant)
                    Spacer(minLength: 8)
                    Text(n).font(sys(16, .heavy)).foregroundColor(p.onSurface).multilineTextAlignment(.trailing)
                        .modifier(NumericRoll(value: n))
                }
                .padding(.horizontal, 14).padding(.vertical, 11)
                .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(x.surfaceVariant.opacity(0.6)))
                .accessibilityElement(children: .combine)
            }
        }
    }
}

/// حقل مبلغ بلوحة أرقام النظام؛ يحفظ ما يُكتب فيه (فلا يقفز المؤشر) ويرسل كل تغيير إلى Kotlin
private struct InheritanceAmountField: View {
    let f: InheritanceState.EstateField
    let p: HomePalette
    let model: InheritanceModel
    let focused: FocusState<String?>.Binding
    @State private var text: String

    init(f: InheritanceState.EstateField, p: HomePalette, model: InheritanceModel, focused: FocusState<String?>.Binding) {
        self.f = f
        self.p = p
        self.model = model
        self.focused = focused
        _text = State(initialValue: f.value)
    }

    var body: some View {
        let active = focused.wrappedValue == f.key
        VStack(alignment: .leading, spacing: 4) {
            Text(f.label).font(sys(13, .medium)).foregroundColor(active ? p.primary : p.onSurfaceVariant)
                .accessibilityHidden(true)
            TextField("", text: $text)
                .keyboardType(.decimalPad)
                .font(sys(17, .semibold))
                .foregroundColor(p.onSurface)
                .focused(focused, equals: f.key)
                .padding(.horizontal, 14).padding(.vertical, 12)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.background.opacity(0.7)))
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .strokeBorder(active ? p.primary : p.outlineVariant, lineWidth: active ? 2 : 1)
                )
                .accessibilityLabel(f.label)
                .onChange(of: text) { v in model.field(f.key, v) }
            if let h = f.hint {
                Text(h).font(sys(12)).foregroundColor(p.onSurfaceVariant).padding(.horizontal, 4)
            }
        }
    }
}

/// مجموعة من الورثة: مفتاح لمن لا يكون إلا واحداً، وعدّاد لمن يتعدد
private struct InheritanceGroupCard: View {
    let g: InheritanceState.HeirGroup
    let p: HomePalette
    let model: InheritanceModel
    var body: some View {
        InheritanceCard(p: p, title: g.title) {
            ForEach(g.heirs) { h in
                InheritanceHeirRow(h: h, p: p, model: model)
            }
        }
    }
}

private struct InheritanceHeirRow: View {
    let h: InheritanceState.HeirItem
    let p: HomePalette
    let model: InheritanceModel
    private let tick = UISelectionFeedbackGenerator()

    var body: some View {
        let on = h.count > 0
        HStack(spacing: 10) {
            Text(h.name).font(sys(15, .semibold)).foregroundColor(on ? p.onPrimaryContainer : p.onSurface)
                .frame(maxWidth: .infinity, alignment: .leading)
            if h.max <= 1 {
                Toggle(h.name, isOn: Binding(get: { on }, set: { v in tick.selectionChanged(); model.heir(h.key, v ? 1 : 0) }))
                    .labelsHidden()
                    .tint(p.primary)
            } else {
                Text(h.countText).font(sys(17, .heavy)).monospacedDigit()
                    .foregroundColor(on ? p.primary : p.onSurfaceVariant)
                    .frame(minWidth: 28)
                    .modifier(NumericRoll(value: h.countText))
                    .accessibilityHidden(true)
                Stepper(h.name, value: Binding(get: { h.count }, set: { v in tick.selectionChanged(); model.heir(h.key, v) }), in: 0...max(h.max, 1))
                    .labelsHidden()
                    .accessibilityValue(h.countText)
            }
        }
        .padding(.leading, 14).padding(.trailing, 8).padding(.vertical, 6)
        .frame(minHeight: 52)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(on ? p.primaryContainer : p.surface))
        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(on ? p.primary : p.outlineVariant, lineWidth: 1))
        .animation(UIAccessibility.isReduceMotionEnabled ? nil : .easeOut(duration: 0.25), value: on)
    }
}

/// الدليل وأقوال الفقهاء: آيات وأحاديث وأقوال، تنفتح بلمسة (مفتوحة أولاً إن كانت في المسألة ورثة، كما في EvidenceCard)
private struct InheritanceEvidence: View {
    let e: InheritanceState.Proofs
    let p: HomePalette
    let x: InheritancePalette
    @State private var open: Bool

    init(e: InheritanceState.Proofs, p: HomePalette, x: InheritancePalette) {
        self.e = e
        self.p = p
        self.x = x
        _open = State(initialValue: e.open)
    }

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

    /// أيقونة فكرة الدليل (conceptIcon في EvidenceCard.kt)
    private func concept(_ c: String) -> String {
        switch c {
        case "order": return "list.number"
        case "family": return "person.3.fill"
        case "share": return "chart.pie.fill"
        case "money": return "banknote.fill"
        case "scholars": return "bubble.left.and.bubble.right.fill"
        default: return "lightbulb.fill"
        }
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
                    Image(systemName: "book.closed.fill").foregroundColor(p.goldDeep)
                        .frame(width: 42, height: 42)
                        .background(RoundedRectangle(cornerRadius: 13, style: .continuous).fill(p.gold.opacity(0.16)))
                    VStack(alignment: .leading, spacing: 4) {
                        Text(e.title).font(sys(16, .bold)).foregroundColor(p.onSurface)
                        Text(e.sub).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                        HStack(spacing: 6) {
                            ForEach(e.kindOrder, id: \.self) { k in chip(k) }
                        }
                        .padding(.top, 2)
                    }
                    Spacer(minLength: 0)
                    Image(systemName: "chevron.down").font(.system(size: 14, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
                        .rotationEffect(.degrees(open ? 180 : 0))
                }
                .padding(16)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(.isHeader)
            if open {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(e.points.enumerated()), id: \.offset) { i, pt in
                        if i > 0 {
                            Rectangle().fill(p.outlineVariant.opacity(0.6)).frame(height: 1).padding(.vertical, 12)
                        }
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: concept(pt.concept))
                                .font(.system(size: 16, weight: .semibold)).foregroundColor(color(pt.kind))
                                .frame(width: 38, height: 38)
                                .background(Circle().fill(color(pt.kind).opacity(0.13)))
                                .accessibilityHidden(true)
                            VStack(alignment: .leading, spacing: 6) {
                                chip(pt.kind)
                                Text(pt.title).font(sys(14, .bold)).foregroundColor(p.onSurface)
                                if let a = pt.ayah {
                                    Text(a).font(quranFont(19)).foregroundColor(p.onSurface).lineSpacing(8)
                                        .frame(maxWidth: .infinity, alignment: .trailing)
                                        .multilineTextAlignment(.trailing)
                                        .padding(10)
                                        .background(RoundedRectangle(cornerRadius: 12).fill(p.gold.opacity(0.08)))
                                        .environment(\.layoutDirection, .rightToLeft)
                                }
                                if let t = pt.tr { Text(t).font(sys(12)).foregroundColor(p.onSurfaceVariant) }
                                if let h = pt.hadith {
                                    Text(h).font(sys(14)).foregroundColor(p.onSurface).lineSpacing(6)
                                        .padding(10)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .background(RoundedRectangle(cornerRadius: 12).fill(p.gold.opacity(0.08)))
                                        .environment(\.layoutDirection, .rightToLeft)
                                }
                                Text(pt.body).font(sys(14)).foregroundColor(p.onSurface)
                                HStack(spacing: 5) {
                                    Image(systemName: symbol(pt.kind)).font(.system(size: 11, weight: .semibold)).foregroundColor(color(pt.kind))
                                    Text(pt.source).font(sys(12, .bold)).foregroundColor(color(pt.kind))
                                }
                            }
                        }
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
