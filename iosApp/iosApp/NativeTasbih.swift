import SwiftUI
import UIKit
import Shared

/// المسبحة بواجهة النظام (SwiftUI) بطراز الرئيسية (NativeHome.swift): ترويسة بتدرج لون التطبيق ونجمته وشريط علوي ثابت فيه
/// زر الرجوع، والذكر بخط المصحف ومعناه، والحبة الكبيرة بحلقة تقدّم ذهبية وموجة لكل لمسة وانفجار ذهبي عند الإتمام،
/// والتصفير والتراجع، والعدّاد على شاشة القفل، واختيار الذكر والهدف، والإحصاء، والأدلة — كما في TasbihScreen.kt.
/// العدّ يظهر هنا فور اللمس (مع الاهتزاز)، ثم تصل اللمسة إلى Kotlin (Tasbih) فيبقى مصدر الحال للأداة والنشاط الحي؛
/// والحال من NativeTasbih.ios.kt مع الإشعار MizanTasbihScreenState، و"taps" فيها تخبر متى لحقت بما عُدّ هنا.
/// الشريط العلوي والترويسة والبطاقة والأدلة مشتركة مع القبلة (NativeQibla.swift).
enum NativeTasbihScreen {
    static func install() {
        NativeTasbih.shared.attach { session in TasbihScreenHost(session: session) }
    }
}

final class TasbihScreenHost: UIHostingController<TasbihScreenRoot> {
    init(session: NativeTasbihSession) {
        let model = TasbihScreenModel(session: session)
        super.init(rootView: TasbihScreenRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct TasbihScreenState: Decodable {
    struct Mode: Decodable, Identifiable { let key, label: String; var id: String { key } }
    struct Live: Decodable { let on: Bool; let show, title, desc: String }
    struct Target: Decodable, Identifiable { let value: Int; let label: String; var id: Int { value } }
    struct Chooser: Decodable {
        let title, targetLabel: String
        let labels: [String]
        let idx, target: Int
        let targets: [Target]
    }
    struct Tile: Decodable { let value, label: String }
    struct Stats: Decodable { let title: String; let tiles: [Tile] }

    let rtl, dark, wide, canBack: Bool
    let top, bottom, start, end, lead: Double
    let back, title, subtitle: String
    let taps: Int
    let mode: String
    let modes: [Mode]
    let text, meaning: String
    let count, goal: Int
    let goalLabel, doneMessage, tapToCount, reset, undo: String
    let live: Live?
    let chooser: Chooser
    let stats: Stats
    let evidence: QiblaTasbihProofs
    let c: HomeState.Colors
    let bead1, bead2, secondary: String

    static func decode(_ json: String) -> TasbihScreenState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(TasbihScreenState.self, from: data)
    }
}

/// ما يتغير مع كل لمسة (العدد المعروض والرسالة والموجات): وحده، فلا تُعاد الصفحة كلها مع كل تسبيحة
final class TasbihScreenCounter: ObservableObject {
    @Published var shown = 0
    @Published var message: String?
    /// لمسات هذه الشاشة: موجة لكل لمسة
    @Published var taps = 0
    /// إتمامات: انفجار ذهبي لكل إتمام
    @Published var bursts = 0
}

final class TasbihScreenModel: ObservableObject {
    let session: NativeTasbihSession
    let counter = TasbihScreenCounter()
    @Published private(set) var state: TasbihScreenState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    // اختيارات تظهر فور اللمس، ثم تؤكدها الحال من Kotlin
    @Published private(set) var mode = "free"
    @Published private(set) var idx = 0
    @Published private(set) var target = 33
    @Published private(set) var liveOn = false
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?
    /// اللمسات المرسلة إلى Kotlin
    private var sent = 0
    private let tick = UIImpactFeedbackGenerator(style: .light)
    private let finish = UINotificationFeedbackGenerator()

    init(session: NativeTasbihSession) {
        self.session = session
        if let s = TasbihScreenState.decode(session.state) {
            sent = s.taps
            apply(s)
        }
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanTasbihScreenState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = TasbihScreenState.decode(self.session.state) else { return }
            self.apply(s)
        }
        tick.prepare()
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    private func apply(_ s: TasbihScreenState) {
        state = s
        if mode != s.mode { mode = s.mode }
        if idx != s.chooser.idx { idx = s.chooser.idx }
        if target != s.chooser.target { target = s.chooser.target }
        let on = s.live?.on ?? false
        if liveOn != on { liveOn = on }
        // لحقت الحال بكل ما عُدّ هنا: عددها هو الصحيح (ويشمل ما عدّته الأداة وزر الإجراء)
        if s.taps >= sent, counter.shown != s.count { counter.shown = s.count }
    }

    /// يرسل إلى Kotlin بعد رسم اللمسة، وبترتيب اللمسات نفسه
    private func later(_ work: @escaping (NativeTasbihSession) -> Void) {
        let session = self.session
        DispatchQueue.main.async { work(session) }
    }

    /// تسبيحة: تظهر الآن (العدد، والإتمام عند بلوغ الهدف)، ثم تصل إلى Tasbih
    func tap() {
        guard let s = state else { return }
        var n = counter.shown + 1
        let complete = s.goal > 0 && n >= s.goal
        if complete { n = 0 }
        counter.shown = n
        counter.taps += 1
        if complete {
            counter.bursts += 1
            counter.message = s.doneMessage.isEmpty ? nil : s.doneMessage
            finish.notificationOccurred(.success)
            if UIAccessibility.isVoiceOverRunning, !s.doneMessage.isEmpty {
                UIAccessibility.post(notification: .announcement, argument: s.doneMessage)
            }
        } else {
            counter.message = nil
            tick.impactOccurred()
        }
        tick.prepare()
        sent += 1
        later { $0.count() }
    }

    func reset() {
        counter.shown = 0
        later { $0.reset() }
    }

    func undo() {
        if counter.shown > 0 { counter.shown -= 1 }
        later { $0.undo() }
    }

    func setMode(_ key: String) {
        guard key != mode else { return }
        mode = key
        counter.shown = 0
        later { $0.mode(key: key) }
    }

    func setDhikr(_ i: Int) {
        idx = i
        counter.shown = 0
        later { $0.dhikr(index: Int32(i)) }
    }

    func setTarget(_ t: Int) {
        target = t
        counter.shown = 0
        later { $0.target(value: Int32(t)) }
    }

    func showLive() { later { $0.showLive() } }

    func setLive(_ on: Bool) {
        liveOn = on
        later { $0.live(on: on) }
    }

    func back() { session.back() }
}

// MARK: - الشاشة

struct TasbihScreenRoot: View {
    @ObservedObject var model: TasbihScreenModel
    var body: some View {
        Group {
            if let s = model.state { TasbihScreenView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

private struct TasbihHeaderBottomKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

struct TasbihScreenView: View {
    let s: TasbihScreenState
    let p: HomePalette
    @ObservedObject var model: TasbihScreenModel
    @State private var glass: Double = 0

    private var still: Bool { UIAccessibility.isReduceMotionEnabled }

    private func track(_ headerBottom: CGFloat) {
        let g = qiblaTasbihGlass(top: CGFloat(s.top), headerBottom: headerBottom)
        if g != glass { glass = g }
    }

    var body: some View {
        ZStack(alignment: .top) {
            p.background
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    QiblaTasbihHeader(title: s.title, subtitle: s.subtitle, symbol: "hand.tap.fill",
                                      top: CGFloat(s.top), start: CGFloat(s.start), end: CGFloat(s.end), rtl: s.rtl, p: p)
                        .background(GeometryReader { g in
                            Color.clear.preference(key: TasbihHeaderBottomKey.self, value: g.frame(in: .named("tasbih")).maxY)
                        })
                    content
                        .padding(.leading, CGFloat(s.start))
                        .padding(.trailing, CGFloat(s.end))
                        .animation(still ? nil : .easeOut(duration: 0.3), value: model.mode)
                }
                .padding(.bottom, CGFloat(s.bottom))
            }
            .coordinateSpace(name: "tasbih")
            .onPreferenceChange(TasbihHeaderBottomKey.self) { track($0) }
            QiblaTasbihTopBar(title: s.title, back: s.back, canBack: s.canBack, top: CGFloat(s.top), lead: CGFloat(s.lead),
                              p: p, amount: glass) { model.back() }
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }

    /// عمود واحد على الهاتف، وعمودان على الشاشة العريضة (الحبة والأدلة، ثم الاختيار والإحصاء) كما في ToolPage
    @ViewBuilder private var content: some View {
        if s.wide {
            HStack(alignment: .top, spacing: 20) {
                VStack(spacing: 14) {
                    mainCard
                    QiblaTasbihProofsCard(e: s.evidence, p: p)
                }
                VStack(spacing: 14) {
                    chooser
                    stats
                }
            }
        } else {
            VStack(spacing: 14) {
                mainCard
                chooser
                stats
                QiblaTasbihProofsCard(e: s.evidence, p: p)
            }
        }
    }

    private var mainCard: some View {
        QiblaTasbihCard(p: p) {
            Picker(s.title, selection: Binding(get: { model.mode }, set: { model.setMode($0) })) {
                ForEach(s.modes) { m in Text(m.label).tag(m.key) }
            }
            .pickerStyle(.segmented)
            .labelsHidden()
            // الذكر ومعناه: يدخل الجديد من أسفل قليلاً
            ZStack {
                VStack(spacing: 4) {
                    Text(s.text)
                        .font(quranFont(s.text.count > 40 ? 21 : 30)).foregroundColor(p.primary)
                        .lineSpacing(s.text.count > 40 ? 8 : 4)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .environment(\.layoutDirection, .rightToLeft)
                    if !s.meaning.isEmpty {
                        Text(s.meaning).font(sys(14)).foregroundColor(p.onSurfaceVariant).multilineTextAlignment(.center)
                    }
                }
                .padding(.vertical, 6)
                .frame(maxWidth: .infinity, minHeight: 72)
                .id(s.text + "|" + s.meaning)
                .transition(.asymmetric(insertion: .opacity.combined(with: .offset(y: 14)), removal: .opacity))
            }
            .animation(still ? nil : .easeOut(duration: 0.3), value: s.text + "|" + s.meaning)
            .accessibilityElement(children: .combine)
            TasbihScreenBead(s: s, p: p, counter: model.counter) { model.tap() }
            HStack(spacing: 10) {
                TasbihScreenOutline(title: s.reset, symbol: "arrow.counterclockwise", p: p) { model.reset() }
                TasbihScreenOutline(title: s.undo, symbol: "arrow.uturn.backward", p: p) { model.undo() }
            }
            .frame(maxWidth: .infinity)
            if let live = s.live { liveSection(live) }
        }
    }

    /// iOS: العدّاد على شاشة القفل وفي الجزيرة الديناميكية، ومفتاح إطفائه
    @ViewBuilder private func liveSection(_ live: TasbihScreenState.Live) -> some View {
        if model.liveOn {
            Button { model.showLive() } label: {
                Label(live.show, systemImage: "iphone")
                    .font(sys(15, .semibold)).foregroundColor(p.primary)
                    .frame(maxWidth: .infinity).padding(.vertical, 8)
            }
            .buttonStyle(PressScale(scale: 0.96))
        }
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(live.title).font(sys(15, .semibold)).foregroundColor(p.onSurface)
                Text(live.desc).font(sys(12)).foregroundColor(p.onSurfaceVariant)
            }
            Spacer(minLength: 8)
            Toggle(live.title, isOn: Binding(get: { model.liveOn }, set: { model.setLive($0) }))
                .labelsHidden()
                .tint(p.primary)
        }
        .padding(.horizontal, 4)
    }

    /// اختيار الذكر والهدف (في الوضع الحر وحده)
    @ViewBuilder private var chooser: some View {
        if model.mode == "free" {
            QiblaTasbihCard(p: p, title: s.chooser.title) {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 120), spacing: 8)], spacing: 8) {
                    ForEach(Array(s.chooser.labels.enumerated()), id: \.offset) { i, label in
                        TasbihScreenChip(text: label, selected: i == model.idx, p: p) { model.setDhikr(i) }
                    }
                }
                HStack(spacing: 10) {
                    Text(s.chooser.targetLabel).font(sys(15, .medium)).foregroundColor(p.onSurface)
                    Spacer(minLength: 8)
                    Menu {
                        ForEach(s.chooser.targets) { t in
                            Button { model.setTarget(t.value) } label: {
                                if t.value == model.target { Label(t.label, systemImage: "checkmark") } else { Text(t.label) }
                            }
                        }
                    } label: {
                        HStack(spacing: 6) {
                            Text(s.chooser.targets.first(where: { $0.value == model.target })?.label ?? "\(model.target)")
                                .font(sys(15, .bold)).foregroundColor(p.primary)
                            Image(systemName: "chevron.up.chevron.down").font(.system(size: 12, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
                        }
                        .padding(.horizontal, 14).padding(.vertical, 9)
                        .background(Capsule().fill(p.primaryContainer))
                    }
                    .accessibilityLabel(s.chooser.targetLabel)
                }
            }
            .transition(.opacity.combined(with: .move(edge: .top)))
        }
    }

    private var stats: some View {
        QiblaTasbihCard(p: p, title: s.stats.title) {
            HStack(spacing: 10) {
                ForEach(Array(s.stats.tiles.enumerated()), id: \.offset) { _, t in
                    VStack(spacing: 2) {
                        Text(t.value).font(sys(22, .heavy)).foregroundColor(p.primary).lineLimit(1).minimumScaleFactor(0.6)
                            .modifier(NumericRoll(value: t.value))
                        Text(t.label).font(sys(12)).foregroundColor(p.onSurfaceVariant).multilineTextAlignment(.center).lineLimit(2)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12).padding(.horizontal, 6)
                    .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(p.primaryContainer.opacity(0.6)))
                    .accessibilityElement(children: .combine)
                }
            }
        }
    }
}

/// الحبة: حلقة التقدّم، والعدد بأرقام تتدحرج، وموجة لكل لمسة وانفجار ذهبي عند الإتمام، ورسالة الإتمام تحتها
private struct TasbihScreenBead: View {
    let s: TasbihScreenState
    let p: HomePalette
    @ObservedObject var counter: TasbihScreenCounter
    let onTap: () -> Void
    @State private var ripples: [Int] = []
    @State private var squeeze = false

    var body: some View {
        let still = UIAccessibility.isReduceMotionEnabled
        let progress: Double = s.goal > 0
            ? min(max(Double(counter.shown) / Double(s.goal), 0), 1)
            : Double(counter.shown % 100) / 100
        VStack(spacing: 8) {
            ZStack {
                ForEach(ripples, id: \.self) { _ in TasbihScreenRipple(color: p.gold) }
                if counter.bursts > 0 && !still {
                    TasbihScreenBurst(color: p.gold).id(counter.bursts)
                }
                Circle().stroke(p.outlineVariant, lineWidth: 5).frame(width: 250, height: 250)
                Circle().trim(from: 0, to: CGFloat(progress))
                    .stroke(p.gold, style: StrokeStyle(lineWidth: 5, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .frame(width: 250, height: 250)
                    .animation(still ? nil : .spring(response: 0.45, dampingFraction: 0.8), value: progress)
                Button(action: onTap) {
                    ZStack {
                        Circle().fill(RadialGradient(colors: [HomePalette.color(s.bead1), HomePalette.color(s.bead2)],
                                                     center: .center, startRadius: 0, endRadius: 105))
                        VStack(spacing: 2) {
                            Text("\(counter.shown)")
                                .font(sys(64, .heavy)).monospacedDigit().foregroundColor(.white)
                                .lineLimit(1).minimumScaleFactor(0.4)
                                .modifier(NumericRoll(value: "\(counter.shown)"))
                            Text(s.goalLabel).font(sys(15)).foregroundColor(.white.opacity(0.8)).lineLimit(1).minimumScaleFactor(0.7)
                        }
                        .padding(24)
                    }
                    .frame(width: 210, height: 210)
                    .contentShape(Circle())
                }
                .buttonStyle(TasbihScreenBeadPress())
                .scaleEffect(squeeze ? 0.93 : 1)
                .accessibilityLabel(s.tapToCount)
                .accessibilityValue("\(counter.shown) \(s.goalLabel)")
            }
            .frame(width: 270, height: 270)
            .frame(maxWidth: .infinity)
            .environment(\.layoutDirection, .leftToRight)
            // سطر الإتمام يظهر عند الإتمام فقط، ويختفي مع التسبيحة التالية
            ZStack {
                if let m = counter.message {
                    Text(m).font(sys(16, .bold)).foregroundColor(HomePalette.color(s.secondary))
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .id(m)
                        .transition(.scale(scale: 0.8).combined(with: .opacity))
                }
            }
            .animation(still ? nil : .spring(response: 0.35, dampingFraction: 0.55), value: counter.message)
        }
        .onChange(of: counter.taps) { n in
            guard !UIAccessibility.isReduceMotionEnabled else { return }
            // نبضة الحبة: تصغر فوراً ثم ترتد
            var t = Transaction()
            t.disablesAnimations = true
            withTransaction(t) { squeeze = true }
            DispatchQueue.main.async {
                withAnimation(.spring(response: 0.3, dampingFraction: 0.45)) { squeeze = false }
            }
            ripples.append(n)
            if ripples.count > 6 { ripples.removeFirst() }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.75) { ripples.removeAll { $0 == n } }
        }
    }
}

private struct TasbihScreenBeadPress: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(UIAccessibility.isReduceMotionEnabled ? nil : .spring(response: 0.25, dampingFraction: 0.6), value: configuration.isPressed)
    }
}

/// موجة ذهبية تخرج من حافة الحبة
private struct TasbihScreenRipple: View {
    let color: Color
    @State private var on = false
    var body: some View {
        Circle().stroke(color, lineWidth: on ? 0.5 : 2.5)
            .frame(width: 222, height: 222)
            .scaleEffect(on ? 1.29 : 1)
            .opacity(on ? 0 : 0.45)
            .allowsHitTesting(false)
            .onAppear { withAnimation(.easeOut(duration: 0.7)) { on = true } }
    }
}

/// ست عشرة حبة ذهبية تتناثر عند الإتمام
private struct TasbihScreenBurst: View {
    let color: Color
    @State private var on = false
    var body: some View {
        ZStack {
            ForEach(0..<16, id: \.self) { k in
                let a = Double(k) * .pi * 2 / 16
                let d: CGFloat = on ? 138 : 111
                Circle().fill(color)
                    .frame(width: on ? 2 : 6, height: on ? 2 : 6)
                    .offset(x: d * CGFloat(cos(a)), y: d * CGFloat(sin(a)))
            }
        }
        .opacity(on ? 0 : 1)
        .allowsHitTesting(false)
        .onAppear { withAnimation(.easeOut(duration: 0.9)) { on = true } }
    }
}

/// زر بحدّ رفيع (التصفير والتراجع)
private struct TasbihScreenOutline: View {
    let title: String
    let symbol: String
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Label(title, systemImage: symbol)
                .font(sys(15, .semibold)).foregroundColor(p.primary)
                .padding(.horizontal, 16).padding(.vertical, 10)
                .background(Capsule().strokeBorder(p.outlineVariant, lineWidth: 1))
                .contentShape(Capsule())
        }
        .buttonStyle(PressScale(scale: 0.95))
    }
}

/// ذكر يُختار (ChipButton)
private struct TasbihScreenChip: View {
    let text: String
    let selected: Bool
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(text)
                .font(sys(14, .semibold)).foregroundColor(selected ? .white : p.onSurfaceVariant)
                .multilineTextAlignment(.center).lineLimit(2)
                .padding(.horizontal, 10).padding(.vertical, 9)
                .frame(maxWidth: .infinity, minHeight: 42)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(selected ? p.primary : p.surface))
                .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(selected ? p.primary : p.outlineVariant, lineWidth: 1))
        }
        .buttonStyle(PressScale(scale: 0.95))
        .animation(.easeOut(duration: 0.2), value: selected)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}
