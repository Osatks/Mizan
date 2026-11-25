import SwiftUI
import UIKit
import Shared

/// إعداد أول تشغيل بواجهة النظام (SwiftUI): خطوات يفعل فيها المستخدم شيئاً — يختار لغته، ويحدد موقعه فيرى مواقيت اليوم
/// والعدّ إلى الصلاة القادمة، ويفعّل الأذان ويسمع المؤذن، ويختار المظهر فتتبدل الشاشة نفسها، ويجرّب حبات المسبحة، ثم يرى
/// ملخص ما ضبطه — كما في Welcome.kt. على خلفية التطبيق الخضراء ونجمتها الكبيرة الدوّارة كما كانت الجولة (NativeGuide.swift).
/// الخطوة الحالية وكل اختيار في Kotlin (NativeWelcome.ios.kt و IosWelcome.kt)؛ وحبات التجربة تُعدّ هنا فوراً باهتزازها.
enum NativeWelcomeScreen {
    static func install() {
        NativeWelcome.shared.attach { session in WelcomeHostController(session: session) }
    }
}

final class WelcomeHostController: UIHostingController<WelcomeRoot> {
    init(session: NativeWelcomeSession) {
        let model = WelcomeModel(session: session)
        super.init(rootView: WelcomeRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        // الهوامش من Kotlin (شريط الحالة والمؤشر السفلي) لا من المنطقة الآمنة لهذا المتحكم
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct WelcomeState: Decodable {
    struct Language: Decodable, Identifiable { let code, name: String; let selected: Bool; var id: String { code } }
    struct Prayer: Decodable, Identifiable { let key, name, time: String; let next: Bool; var id: String { key } }
    struct NextPrayer: Decodable { let label: String; let at: Double }
    struct AlertRow: Decodable { let title, sub: String; let on: Bool }
    struct Voice: Decodable, Identifiable { let id, name, sub: String; let selected, playing: Bool }
    struct Choice: Decodable, Identifiable { let key, label: String; let selected: Bool; var id: String { key } }
    struct Tone: Decodable, Identifiable {
        let key, label: String
        let selected, depth: Bool
        let paper, paperEdge, ink, gold, goldDeep: String
        var id: String { key }
    }
    struct Row: Decodable, Identifiable { let icon, label, value: String; let ok: Bool; var id: String { icon } }

    let rtl, dark: Bool
    let top, bottom, width: Double
    let step: Int
    let steps: [String]
    let stepLabel: String
    let skip, next, back, start: String
    let titles, bodies: [String]
    let languageLabel: String
    let langs: [Language]
    let place: String?
    let locating: Bool
    let useMyLocation, locatingText, pickCity, todayTitle: String
    let prayers: [Prayer]
    let nextPrayer: NextPrayer?
    let alert: AlertRow
    let muezzinTitle, moreVoices, play, stop: String
    let muezzins: [Voice]
    let appearance: String
    let themes: [Choice]
    let pageColor: String
    let tones: [Tone]
    let dhikr, bead, tryDone: String
    let beads: Int
    let summary: [Row]
    let c: HomeState.Colors

    static func decode(_ json: String) -> WelcomeState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(WelcomeState.self, from: data)
    }
}

final class WelcomeModel: ObservableObject {
    let session: NativeWelcomeSession
    @Published private(set) var state: WelcomeState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    /// الخطوة الظاهرة: تتبع Kotlin، ويغيّرها السحب فوراً ثم يُخبَر بها Kotlin
    @Published private(set) var step: Int = 0
    /// حبات تجربة المسبحة (لا تُحسب في المسبحة نفسها)
    @Published private(set) var beads: Int = 0
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?
    private let tick = UIImpactFeedbackGenerator(style: .light)
    private let finish = UINotificationFeedbackGenerator()

    init(session: NativeWelcomeSession) {
        self.session = session
        state = WelcomeState.decode(session.state)
        step = state?.step ?? 0
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanWelcomeState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = WelcomeState.decode(self.session.state) else { return }
            self.state = s
            // «التالي» و«رجوع» من Kotlin: انتقال متحرك إلى الخطوة الجديدة
            if s.step != self.step {
                withAnimation(UIAccessibility.isReduceMotionEnabled ? nil : .easeInOut(duration: 0.35)) { self.step = s.step }
            }
        }
        tick.prepare()
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    /// سحب المستخدم إلى خطوة
    func select(_ i: Int) {
        guard i != step else { return }
        step = i
        session.go(page: Int32(i))
    }

    func next() { session.goNext() }
    func back() { session.goBack() }
    func skip() { session.skip() }
    func language(_ code: String) {
        UISelectionFeedbackGenerator().selectionChanged()
        session.language(code: code)
    }
    func locate() { session.locate() }
    func pickCity() { session.pickCity() }
    func alerts(_ on: Bool) { session.alerts(on: on) }
    func muezzin(_ id: String) {
        UISelectionFeedbackGenerator().selectionChanged()
        session.muezzin(id: id)
    }
    func play(_ id: String) { session.play(id: id) }
    func theme(_ key: String) {
        UISelectionFeedbackGenerator().selectionChanged()
        session.theme(key: key)
    }
    func tone(_ key: String) {
        UISelectionFeedbackGenerator().selectionChanged()
        session.tone(key: key)
    }

    /// لمسة على الحبة: تُعدّ فوراً باهتزاز خفيف، وبالأخيرة اهتزاز النجاح؛ وبعد الإتمام تبدأ من جديد
    func tapBead(goal: Int) {
        beads = beads >= goal ? 1 : beads + 1
        if beads >= goal { finish.notificationOccurred(.success) } else { tick.impactOccurred() }
    }
}

/// رموز الخطوات (STEP_ICONS في Welcome.kt) برموز النظام
private func welcomeSymbol(_ key: String) -> String {
    switch key {
    case "hello": return "star.fill"
    case "place": return "mappin.and.ellipse"
    case "adhan": return "bell.badge.fill"
    case "style": return "paintpalette.fill"
    case "taste": return "hand.tap.fill"
    case "done": return "checkmark.seal.fill"
    default: return "star.fill"
    }
}

/// رموز سطور الملخص
private func summarySymbol(_ key: String) -> String {
    switch key {
    case "language": return "globe"
    case "place": return "mappin.and.ellipse"
    case "adhan": return "bell.fill"
    case "style": return "paintpalette.fill"
    default: return "checkmark"
    }
}

/// لون النص على الذهبي (0xFF1E1A14 في Compose)
private let welcomeInk = Color(.sRGB, red: 0x1E / 255.0, green: 0x1A / 255.0, blue: 0x14 / 255.0, opacity: 1)

private var welcomeReduceMotion: Bool { UIAccessibility.isReduceMotionEnabled }

// MARK: - الشاشة

struct WelcomeRoot: View {
    @ObservedObject var model: WelcomeModel
    var body: some View {
        Group {
            if let s = model.state { WelcomeView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

struct WelcomeView: View {
    let s: WelcomeState
    let p: HomePalette
    @ObservedObject var model: WelcomeModel

    private var dir: LayoutDirection { s.rtl ? .rightToLeft : .leftToRight }
    private var count: Int { s.steps.count }
    private var last: Bool { model.step == count - 1 }

    /// ترتيب الخطوات في المقلّب: في العربية تُعكس فتأتي التالية من اليسار (المقلّب نفسه يبقى من اليسار إلى اليمين)
    private var order: [Int] {
        let all = Array(0..<count)
        return s.rtl ? all.reversed() : all
    }

    var body: some View {
        ZStack {
            LinearGradient(colors: [p.hero1, p.hero2], startPoint: .topLeading, endPoint: .bottomTrailing)
            // المظهر الداكن يُعتم الخلفية قليلاً، فيرى المستخدم الشاشة نفسها تتبدل مع اختياره
            Color.black.opacity(s.dark ? 0.32 : 0)
                .animation(welcomeReduceMotion ? nil : .easeInOut(duration: 0.45), value: s.dark)
                .allowsHitTesting(false)
            // نجمة كبيرة تدور ببطء خلف الخطوات (في زاوية النهاية)
            ZStack(alignment: s.rtl ? .topLeading : .topTrailing) {
                Color.clear
                WelcomeSpinStar()
                    .offset(x: s.rtl ? -110 : 110, y: -70)
            }
            .environment(\.layoutDirection, .leftToRight)
            .allowsHitTesting(false)
            VStack(spacing: 0) {
                topRow
                TabView(selection: Binding(get: { model.step }, set: { model.select($0) })) {
                    ForEach(order, id: \.self) { i in
                        WelcomePage(index: i, s: s, p: p, model: model)
                            .environment(\.layoutDirection, dir)
                            .tag(i)
                    }
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
                .environment(\.layoutDirection, .leftToRight)
                .frame(maxHeight: .infinity)
                bottomRow
            }
        }
        .environment(\.layoutDirection, dir)
        .environment(\.colorScheme, .dark)
    }

    /// شريط الخطوات، و«تخطي» في جهة النهاية (ما عدا الخطوة الأخيرة)
    private var topRow: some View {
        HStack(spacing: 8) {
            WelcomeStepBar(step: model.step, count: count, p: p, label: s.stepLabel)
            Spacer(minLength: 0)
            if !last {
                Button(action: model.skip) {
                    Text(s.skip).font(sys(15, .medium)).foregroundColor(.white.opacity(0.85))
                        .padding(.horizontal, 12)
                        .frame(minHeight: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(PressScale(scale: 0.92))
            }
        }
        .frame(minHeight: 52)
        .padding(.leading, 24).padding(.trailing, 8)
        .frame(maxWidth: 640)
        .frame(maxWidth: .infinity)
        .padding(.top, CGFloat(s.top) + 4)
    }

    /// «رجوع» و«التالي» (بعرض أقصاه 640 في الوسط على الشاشات العريضة)؛ في الخطوة الأخيرة «ابدأ» بحجم أكبر
    private var bottomRow: some View {
        HStack(spacing: 12) {
            if model.step > 0 {
                Button(action: model.back) {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.backward").font(.system(size: 15, weight: .semibold))
                        Text(s.back).font(sys(15, .medium))
                    }
                    .foregroundColor(.white.opacity(0.85))
                    .padding(.horizontal, 12)
                    .frame(minHeight: 44)
                    .contentShape(Rectangle())
                }
                .buttonStyle(PressScale(scale: 0.94))
            }
            Spacer(minLength: 0)
            Button(action: model.next) {
                Text(last ? s.start : s.next)
                    .font(sys(last ? 17 : 15, .bold)).foregroundColor(welcomeInk)
                    .lineLimit(1)
                    .padding(.horizontal, last ? 36 : 26)
                    .frame(minHeight: last ? 52 : 46)
                    .background(Capsule().fill(p.gold))
            }
            .buttonStyle(PressScale())
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 16)
        .frame(maxWidth: 640)
        .frame(maxWidth: .infinity)
        .padding(.bottom, CGFloat(s.bottom))
    }
}

/// التقدم: شرطة لكل خطوة، ذهبية لما مضى منها والحالية؛ ويقرؤها VoiceOver «الخطوة ٢ من ٦»
private struct WelcomeStepBar: View {
    let step: Int
    let count: Int
    let p: HomePalette
    let label: String
    var body: some View {
        HStack(spacing: 5) {
            ForEach(0..<count, id: \.self) { i in
                Capsule()
                    .fill(i <= step ? p.gold : Color.white.opacity(0.28))
                    .frame(height: i == step ? 6 : 4)
                    .frame(maxWidth: .infinity)
            }
        }
        .frame(maxWidth: 280)
        .animation(welcomeReduceMotion ? nil : .easeOut(duration: 0.3), value: step)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
    }
}

/// النجمة الكبيرة الباهتة تدور دورة كل تسعين ثانية (ولا تدور إذا أُزيلت الحركات من إعدادات النظام)
private struct WelcomeSpinStar: View {
    @State private var spin = false
    var body: some View {
        StarEmblem(color: Color.white.opacity(0.06), radius: 210, stroke: 1.5)
            .rotationEffect(.degrees(spin ? 360 : 0))
            .onAppear {
                guard !welcomeReduceMotion else { return }
                withAnimation(.linear(duration: 90).repeatForever(autoreverses: false)) { spin = true }
            }
    }
}

/// خطوة: الشارة والعنوان والنص، ثم ما يُفعل فيها. تُمرَّر في النوافذ القصيرة وبالخط الكبير
private struct WelcomePage: View {
    let index: Int
    let s: WelcomeState
    let p: HomePalette
    @ObservedObject var model: WelcomeModel

    var body: some View {
        GeometryReader { g in
            ScrollView(.vertical, showsIndicators: false) {
                content(short: g.size.height < 560)
                    .frame(maxWidth: 560)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 8)
                    .frame(maxWidth: .infinity, minHeight: g.size.height)
            }
        }
    }

    private func content(short: Bool) -> some View {
        VStack(spacing: 0) {
            WelcomeBadge(symbol: welcomeSymbol(s.steps[index]), short: short, p: p)
            Text(s.titles[index])
                .font(sys(24, .bold)).foregroundColor(.white)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, short ? 14 : 24)
                .accessibilityAddTraits(.isHeader)
            Text(s.bodies[index])
                .font(sys(16)).foregroundColor(.white.opacity(0.85))
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 10)
            stepContent
                .padding(.top, short ? 18 : 26)
        }
    }

    @ViewBuilder private var stepContent: some View {
        switch s.steps[index] {
        case "hello": WelcomeLanguages(s: s, p: p, model: model)
        case "place": WelcomePlace(s: s, p: p, model: model)
        case "adhan": WelcomeAdhan(s: s, p: p, model: model)
        case "style": WelcomeStyle(s: s, p: p, model: model)
        case "taste": WelcomeTaste(s: s, p: p, model: model)
        default: WelcomeSummary(s: s, p: p)
        }
    }
}

/// الشارة: دائرة باهتة ونجمة ثمانية ذهبية وفي وسطها رمز الخطوة (أصغر من شارة الجولة لتتسع الخطوة لما فيها)
private struct WelcomeBadge: View {
    let symbol: String
    let short: Bool
    let p: HomePalette
    var body: some View {
        let size: CGFloat = short ? 76 : 112
        ZStack {
            Circle().fill(Color.white.opacity(0.08))
            Star8().stroke(p.gold.opacity(0.9), lineWidth: 2)
                .frame(width: size * 0.84, height: size * 0.84)
            Circle().fill(Color.white.opacity(0.10))
                .frame(width: size * 0.54, height: size * 0.54)
            Image(systemName: symbol)
                .font(.system(size: short ? 24 : 34, weight: .regular))
                .foregroundColor(p.gold)
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

/// عنوان صغير فوق مجموعة على الخلفية الخضراء
private struct WelcomeLabel: View {
    let symbol: String
    let text: String
    let p: HomePalette
    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: symbol).font(.system(size: 15, weight: .semibold)).foregroundColor(p.gold)
                .accessibilityHidden(true)
            Text(text).font(sys(15, .bold)).foregroundColor(.white)
            Spacer(minLength: 0)
        }
        .padding(.bottom, 10)
        .accessibilityAddTraits(.isHeader)
    }
}

/// بطاقة بألوان سطح التطبيق (تتبدل مع المظهر فوراً) فوق الخلفية الخضراء
private struct WelcomeCard<Content: View>: View {
    let p: HomePalette
    let dark: Bool
    @ViewBuilder let content: () -> Content
    var body: some View {
        content()
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: 24, style: .continuous).fill(p.surface))
            .environment(\.colorScheme, dark ? .dark : .light)
            .animation(welcomeReduceMotion ? nil : .easeInOut(duration: 0.3), value: dark)
    }
}

/// اختيار على الخلفية الخضراء: ذهبي حين يُختار، وشفاف بحافة رفيعة قبله
private struct WelcomeChip: View {
    let label: String
    let selected: Bool
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(label)
                .font(sys(15, selected ? .bold : .medium))
                .foregroundColor(selected ? welcomeInk : .white)
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .minimumScaleFactor(0.85)
                .padding(.horizontal, 14).padding(.vertical, 10)
                .frame(maxWidth: .infinity, minHeight: 44)
                .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(selected ? p.gold : Color.white.opacity(0.10)))
                .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(selected ? p.gold : Color.white.opacity(0.30), lineWidth: 1))
                .contentShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        }
        .buttonStyle(PressScale(scale: 0.95))
        .accessibilityAddTraits(selected ? [.isButton, .isSelected] : .isButton)
    }
}

// MARK: - الخطوات

/// الترحيب: لغة الجهاز في صف كامل، ثم لغات التطبيق؛ اختيار لغة يبدّل الواجهة كلها فوراً
private struct WelcomeLanguages: View {
    let s: WelcomeState
    let p: HomePalette
    @ObservedObject var model: WelcomeModel
    var body: some View {
        VStack(spacing: 8) {
            WelcomeLabel(symbol: "globe", text: s.languageLabel, p: p)
            if let device = s.langs.first(where: { $0.code == "system" }) {
                WelcomeChip(label: device.name, selected: device.selected, p: p) { model.language(device.code) }
            }
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 140), spacing: 8)], spacing: 8) {
                ForEach(s.langs.filter { $0.code != "system" }) { l in
                    WelcomeChip(label: l.name, selected: l.selected, p: p) { model.language(l.code) }
                }
            }
        }
    }
}

/// الموقع: بعد تحديده بطاقة مواقيت اليوم والعدّ إلى القادمة، وتحتها زرّا الموقع لتغييره
private struct WelcomePlace: View {
    let s: WelcomeState
    let p: HomePalette
    @ObservedObject var model: WelcomeModel
    var body: some View {
        VStack(spacing: 16) {
            if let place = s.place {
                WelcomePreview(s: s, p: p, place: place)
                    .transition(.opacity.combined(with: .scale(scale: 0.96)))
            }
            VStack(spacing: 10) {
                Button(action: model.locate) {
                    HStack(spacing: 8) {
                        if s.locating {
                            ProgressView().progressViewStyle(CircularProgressViewStyle(tint: welcomeInk))
                        } else {
                            Image(systemName: "location.fill").font(.system(size: 16, weight: .semibold))
                        }
                        Text(s.locating ? s.locatingText : s.useMyLocation).font(sys(15, .bold))
                    }
                    .foregroundColor(welcomeInk)
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .background(Capsule().fill(p.gold.opacity(s.locating ? 0.6 : 1)))
                }
                .buttonStyle(PressScale())
                .disabled(s.locating)
                Button(action: model.pickCity) {
                    HStack(spacing: 8) {
                        Image(systemName: "building.2.fill").font(.system(size: 16, weight: .semibold))
                        Text(s.pickCity).font(sys(15, .medium))
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .overlay(Capsule().strokeBorder(Color.white.opacity(0.45), lineWidth: 1))
                    .contentShape(Capsule())
                }
                .buttonStyle(PressScale())
            }
        }
        .animation(welcomeReduceMotion ? nil : .easeInOut(duration: 0.3), value: s.place)
    }
}

/// معاينة حية: المدينة ومواقيت اليوم الخمسة (القادمة مميزة) والعدّ إليها كل ثانية
private struct WelcomePreview: View {
    let s: WelcomeState
    let p: HomePalette
    let place: String
    var body: some View {
        WelcomeCard(p: p, dark: s.dark) {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 10) {
                    Image(systemName: "checkmark.circle.fill").font(.system(size: 20)).foregroundColor(p.primary)
                        .accessibilityHidden(true)
                    Text(place).font(sys(17, .bold)).foregroundColor(p.onSurface)
                        .fixedSize(horizontal: false, vertical: true)
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, 16).padding(.top, 16)
                Text(s.todayTitle).font(sys(13)).foregroundColor(p.onSurfaceVariant)
                    .padding(.leading, 46).padding(.trailing, 16).padding(.top, 2).padding(.bottom, 8)
                VStack(spacing: 0) {
                    ForEach(s.prayers) { pr in
                        HStack(spacing: 8) {
                            Text(pr.name).font(sys(16, pr.next ? .bold : .regular))
                                .foregroundColor(pr.next ? p.onPrimaryContainer : p.onSurface)
                            Spacer(minLength: 8)
                            Text(pr.time).font(sys(16, pr.next ? .bold : .medium)).monospacedDigit()
                                .foregroundColor(pr.next ? p.onPrimaryContainer : p.onSurface)
                        }
                        .padding(.horizontal, 10).padding(.vertical, 7)
                        .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(pr.next ? p.primaryContainer : Color.clear))
                        .accessibilityElement(children: .combine)
                    }
                }
                .padding(.horizontal, 8)
                Rectangle().fill(p.outlineVariant).frame(height: 1).padding(.top, 8)
                if let n = s.nextPrayer {
                    TimelineView(.periodic(from: wholeSecond(), by: 1)) { ctx in
                        let left = hms(n.at - ctx.date.timeIntervalSince1970)
                        HStack(spacing: 8) {
                            Text(n.label).font(sys(14)).foregroundColor(p.onSurfaceVariant)
                                .fixedSize(horizontal: false, vertical: true)
                            Spacer(minLength: 8)
                            Text(left)
                                .font(sys(17, .heavy)).monospacedDigit()
                                .foregroundColor(p.onSurface)
                                .lineLimit(1)
                                .modifier(NumericRoll(value: left))
                                .padding(.horizontal, 10).padding(.vertical, 4)
                                .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(p.gold.opacity(0.22)))
                                .environment(\.layoutDirection, .leftToRight)
                        }
                        .padding(.horizontal, 16).padding(.vertical, 12)
                        .accessibilityElement(children: .combine)
                    }
                }
            }
        }
    }
}

/// الأذان: مفتاح التنبيهات (يطلب إذن الإشعارات في Kotlin)، والمؤذن مع الاستماع إليه
private struct WelcomeAdhan: View {
    let s: WelcomeState
    let p: HomePalette
    @ObservedObject var model: WelcomeModel
    var body: some View {
        VStack(spacing: 10) {
            WelcomeCard(p: p, dark: s.dark) {
                VStack(spacing: 0) {
                    Toggle(isOn: Binding(get: { s.alert.on }, set: { model.alerts($0) })) {
                        HStack(spacing: 14) {
                            Image(systemName: "bell.badge.fill").font(.system(size: 20)).foregroundColor(p.primary)
                                .accessibilityHidden(true)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(s.alert.title).font(sys(16, .bold)).foregroundColor(p.onSurface)
                                    .fixedSize(horizontal: false, vertical: true)
                                Text(s.alert.sub).font(sys(13)).foregroundColor(p.onSurfaceVariant)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                        }
                    }
                    .tint(p.primary)
                    .padding(16)
                    Rectangle().fill(p.outlineVariant).frame(height: 1)
                    Text(s.muezzinTitle).font(sys(14, .bold)).foregroundColor(p.primary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16).padding(.top, 12).padding(.bottom, 2)
                        .accessibilityAddTraits(.isHeader)
                    ForEach(s.muezzins) { m in
                        WelcomeVoiceRow(m: m, s: s, p: p, model: model)
                    }
                }
                .padding(.bottom, 6)
            }
            Text(s.moreVoices).font(sys(13)).foregroundColor(.white.opacity(0.75))
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

/// مؤذن: اختياره لكل الصلوات، وزر الاستماع (المعاينة نفسها في صفحة الأذان)
private struct WelcomeVoiceRow: View {
    let m: WelcomeState.Voice
    let s: WelcomeState
    let p: HomePalette
    let model: WelcomeModel
    var body: some View {
        HStack(spacing: 4) {
            Button { model.muezzin(m.id) } label: {
                HStack(spacing: 12) {
                    ZStack {
                        Circle().strokeBorder(m.selected ? p.primary : p.onSurfaceVariant, lineWidth: 2)
                        if m.selected { Circle().fill(p.primary).padding(5) }
                    }
                    .frame(width: 22, height: 22)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(m.name).font(sys(16, m.selected ? .semibold : .regular)).foregroundColor(p.onSurface)
                            .fixedSize(horizontal: false, vertical: true)
                        Text(m.sub).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                    }
                    Spacer(minLength: 4)
                }
                .padding(.leading, 16).padding(.vertical, 8)
                .frame(minHeight: 56)
                .contentShape(Rectangle())
            }
            .buttonStyle(PressScale(scale: 0.98))
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(m.selected ? [.isButton, .isSelected] : .isButton)
            Button { model.play(m.id) } label: {
                Image(systemName: m.playing ? "stop.fill" : "play.fill")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(m.playing ? .white : p.primary)
                    .frame(width: 38, height: 38)
                    .background(Circle().fill(m.playing ? p.primary : p.primaryContainer))
                    .frame(width: 44, height: 44)
            }
            .buttonStyle(PressScale(scale: 0.9))
            .accessibilityLabel(m.playing ? s.stop : s.play)
            .accessibilityHint(m.name)
            .padding(.trailing, 8)
        }
    }
}

/// المظهر (تلقائي، فاتح، داكن) والشاشة تتبدل معه، ولون صفحة المصحف بطاقاتٍ مصغّرة كما في ورقة العرض
private struct WelcomeStyle: View {
    let s: WelcomeState
    let p: HomePalette
    @ObservedObject var model: WelcomeModel
    var body: some View {
        VStack(spacing: 0) {
            WelcomeLabel(symbol: "circle.lefthalf.fill", text: s.appearance, p: p)
            HStack(spacing: 8) {
                ForEach(s.themes) { t in
                    WelcomeThemeTile(choice: t, p: p) { model.theme(t.key) }
                }
            }
            WelcomeLabel(symbol: "book.closed.fill", text: s.pageColor, p: p)
                .padding(.top, 22)
            WelcomeCard(p: p, dark: s.dark) {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 84, maximum: 120), spacing: 12)], spacing: 14) {
                    ForEach(s.tones) { t in
                        WelcomeToneCard(t: t, p: p) { model.tone(t.key) }
                    }
                }
                .padding(.horizontal, 12).padding(.vertical, 16)
            }
        }
    }
}

/// خيار المظهر: رمزه واسمه، ذهبي حين يُختار
private struct WelcomeThemeTile: View {
    let choice: WelcomeState.Choice
    let p: HomePalette
    let action: () -> Void
    private var symbol: String {
        switch choice.key {
        case "light": return "sun.max.fill"
        case "dark": return "moon.fill"
        default: return "circle.lefthalf.fill"
        }
    }
    var body: some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: symbol).font(.system(size: 22, weight: .semibold))
                    .accessibilityHidden(true)
                Text(choice.label).font(sys(14, choice.selected ? .bold : .medium))
                    .multilineTextAlignment(.center).lineLimit(2).minimumScaleFactor(0.85)
            }
            .foregroundColor(choice.selected ? welcomeInk : .white)
            .padding(.horizontal, 6).padding(.vertical, 12)
            .frame(maxWidth: .infinity, minHeight: 76)
            .background(RoundedRectangle(cornerRadius: 18, style: .continuous).fill(choice.selected ? p.gold : Color.white.opacity(0.10)))
            .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).strokeBorder(choice.selected ? p.gold : Color.white.opacity(0.30), lineWidth: 1))
            .contentShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(PressScale(scale: 0.95))
        .animation(welcomeReduceMotion ? nil : .easeOut(duration: 0.2), value: choice.selected)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(choice.selected ? [.isButton, .isSelected] : .isButton)
    }
}

/// لون الصفحة صفحةً مصغّرة كما في القراءة: ورقها بتدرّجه وإطارها المذهّب والبسملة بحبرها (ToneCard في ReadingControls.kt)
private struct WelcomeToneCard: View {
    let t: WelcomeState.Tone
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        let paper = HomePalette.color(t.paper)
        let edge = HomePalette.color(t.paperEdge)
        let ink = HomePalette.color(t.ink)
        let gold = HomePalette.color(t.gold)
        let goldDeep = HomePalette.color(t.goldDeep)
        Button(action: action) {
            VStack(spacing: 6) {
                ZStack {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(RadialGradient(colors: [paper, paper, edge], center: .center, startRadius: 0, endRadius: 70))
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .stroke(goldDeep.opacity(0.75), lineWidth: 1.2).padding(6)
                    RoundedRectangle(cornerRadius: 7, style: .continuous)
                        .stroke(gold.opacity(0.6), lineWidth: 0.6).padding(9)
                    VStack(spacing: 1) {
                        Text("بِسۡمِ ٱللَّهِ").font(quranFont(13)).foregroundColor(ink)
                            .lineLimit(1).minimumScaleFactor(0.5)
                        Text("ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ").font(quranFont(10.5)).foregroundColor(ink.opacity(0.85))
                            .lineLimit(1).minimumScaleFactor(0.5)
                    }
                    .padding(.horizontal, 12)
                    .shadow(color: t.depth ? goldDeep.opacity(0.5) : .clear, radius: 4, x: -1, y: 2)
                    .environment(\.layoutDirection, .rightToLeft)
                    if t.depth {
                        VStack {
                            HStack {
                                Spacer(minLength: 0)
                                Text("3D").font(.system(size: 10, weight: .bold)).foregroundColor(paper)
                                    .padding(.horizontal, 5).padding(.vertical, 1)
                                    .background(RoundedRectangle(cornerRadius: 5, style: .continuous).fill(goldDeep))
                            }
                            Spacer(minLength: 0)
                        }
                        .padding(12)
                        .environment(\.layoutDirection, .leftToRight)
                    }
                    if t.selected {
                        VStack {
                            Spacer(minLength: 0)
                            Image(systemName: "checkmark.circle.fill").font(.system(size: 18))
                                .foregroundColor(p.primary)
                                .background(Circle().fill(paper))
                        }
                        .padding(.bottom, 11)
                        .transition(.scale.combined(with: .opacity))
                    }
                }
                .frame(height: 108)
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .strokeBorder(t.selected ? p.primary : gold.opacity(0.45), lineWidth: t.selected ? 2.5 : 1)
                )
                .scaleEffect(t.selected ? 1 : 0.95)
                Text(t.label).font(sys(13, t.selected ? .bold : .regular))
                    .foregroundColor(t.selected ? p.primary : p.onSurfaceVariant)
                    .multilineTextAlignment(.center).lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .buttonStyle(PressScale(scale: 0.95))
        .animation(welcomeReduceMotion ? nil : .spring(response: 0.3, dampingFraction: 0.7), value: t.selected)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(t.label)
        .accessibilityAddTraits(t.selected ? [.isButton, .isSelected] : .isButton)
    }
}

/// تجربة صغيرة: حبة مسبحة تُلمس ثلاث مرات باهتزاز خفيف، ثم علامة إتمام مبهجة
private struct WelcomeTaste: View {
    let s: WelcomeState
    let p: HomePalette
    @ObservedObject var model: WelcomeModel
    var body: some View {
        let done = model.beads >= s.beads
        VStack(spacing: 16) {
            Text(s.dhikr).font(sys(24, .bold)).foregroundColor(p.gold)
                .multilineTextAlignment(.center)
            Button { model.tapBead(goal: s.beads) } label: {
                ZStack {
                    Circle().fill(RadialGradient(colors: [p.gold, p.goldDeep], center: .center, startRadius: 0, endRadius: 80))
                    Circle().strokeBorder(Color.white.opacity(0.35), lineWidth: 3)
                    if done {
                        Image(systemName: "checkmark").font(.system(size: 56, weight: .bold)).foregroundColor(welcomeInk)
                            .transition(.scale.combined(with: .opacity))
                    } else {
                        Text("\(model.beads)").font(.system(size: 48, weight: .black)).foregroundColor(welcomeInk)
                            .modifier(NumericRoll(value: "\(model.beads)"))
                    }
                }
                .frame(width: 148, height: 148)
                .contentShape(Circle())
            }
            .buttonStyle(PressScale(scale: 0.9))
            .accessibilityLabel(s.bead)
            .accessibilityValue("\(model.beads) / \(s.beads)")
            HStack(spacing: 10) {
                ForEach(0..<s.beads, id: \.self) { i in
                    Circle().fill(i < model.beads ? p.gold : Color.white.opacity(0.25)).frame(width: 14, height: 14)
                }
            }
            .accessibilityHidden(true)
            if done {
                Text(s.tryDone).font(sys(16, .semibold)).foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                    .transition(.opacity.combined(with: .scale(scale: 0.85)))
            }
        }
        .animation(welcomeReduceMotion ? nil : .spring(response: 0.35, dampingFraction: 0.6), value: model.beads)
    }
}

/// الملخص: ما ضُبط (اللغة، المدينة، الأذان، المظهر)، وما لم يُضبط بدائرة فارغة
private struct WelcomeSummary: View {
    let s: WelcomeState
    let p: HomePalette
    var body: some View {
        WelcomeCard(p: p, dark: s.dark) {
            VStack(spacing: 0) {
                ForEach(Array(s.summary.enumerated()), id: \.offset) { i, r in
                    if i > 0 { Rectangle().fill(p.outlineVariant).frame(height: 1).padding(.leading, 56) }
                    HStack(spacing: 16) {
                        Image(systemName: r.ok ? "checkmark.circle.fill" : "circle")
                            .font(.system(size: 22)).foregroundColor(r.ok ? p.primary : p.onSurfaceVariant)
                            .frame(width: 24)
                            .accessibilityHidden(true)
                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: 6) {
                                Image(systemName: summarySymbol(r.icon)).font(.system(size: 11, weight: .semibold))
                                    .accessibilityHidden(true)
                                Text(r.label).font(sys(13))
                            }
                            .foregroundColor(p.onSurfaceVariant)
                            Text(r.value).font(sys(16, .semibold)).foregroundColor(p.onSurface)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        Spacer(minLength: 0)
                    }
                    .padding(.horizontal, 16).padding(.vertical, 12)
                    .accessibilityElement(children: .combine)
                }
            }
        }
    }
}
