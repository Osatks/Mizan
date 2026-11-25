import SwiftUI
import UIKit
import Shared

/// متابعة الصيام بواجهة النظام (SwiftUI) بطراز المسبحة والزكاة (NativeTasbih.swift و NativeZakat.swift): ترويسة بتدرج لون
/// التطبيق ونجمته وشريط علوي ثابت فيه زر الرجوع، وبطاقة الصائم الخضراء بالعدّ إلى نهاية السحور (الفجر) وإلى الإفطار (المغرب)،
/// واليوم ومفتاح صيام التطوع، وشبكة رمضان الحالي أو الأخير، وأيام القضاء وسجلها، وأيام التطوع القادمة وتذكيرها — كما في
/// FastingScreen.kt. الحالة كلها من Kotlin (NativeFasting.ios.kt) مع الإشعار MizanFastingState، وكل لمس يعود إليه؛
/// والعدّ بالثواني هنا من لحظتي الفجر والمغرب (epoch). الشريط والترويسة والبطاقة مشتركة مع القبلة (NativeQibla.swift).
enum NativeFastingScreen {
    static func install() {
        NativeFasting.shared.attach { session in FastingScreenHost(session: session) }
    }
}

final class FastingScreenHost: UIHostingController<FastingScreenRoot> {
    init(session: NativeFastingSession) {
        let model = FastingScreenModel(session: session)
        super.init(rootView: FastingScreenRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct FastingScreenState: Decodable {
    /// بطاقة الصائم: لحظتا الفجر والمغرب اليوم (ثوانٍ منذ 1970، أو null إن تعذّر حسابهما) ووقتاهما منسقين
    struct Hero: Decodable {
        let label, suhoorLabel, iftarLabel, iftarNow, fajrTime, maghribTime, place: String
        let fajrAt, maghribAt: Double?
    }
    struct Voluntary: Decodable { let label, hint: String; let on: Bool }
    struct Today: Decodable {
        let title, hijri, greg: String
        let kinds: [String]
        let forbidden: String?
        let voluntary: Voluntary?
    }
    /// يوم من رمضان: mark ‏"0" بلا تحديد، و"f" صيام، و"m" إفطار
    struct Day: Decodable, Identifiable {
        let d: Int
        let mark: String
        let enabled, today: Bool
        let label: String
        var id: Int { d }
    }
    struct Tile: Decodable { let value, label: String }
    struct Ramadan: Decodable {
        let title, hint: String
        let year: Int
        let days: [Day]
        let fasted, missed, none: String
        let tiles: [Tile]
    }
    struct Entry: Decodable { let label, date, delta: String }
    struct Qada: Decodable {
        let title, owed, countText, madeUp, history, empty, decrease, increase: String
        let count, max: Int
        let log: [Entry]
    }
    struct Upcoming: Decodable, Identifiable { let id, kinds, date, rel: String }
    struct Sunnah: Decodable {
        let title, logged, loggedValue, upcoming, remind, remindDesc: String
        let shawwal: String?
        let days: [Upcoming]
        let remindOn: Bool
    }

    let rtl, dark, wide, canBack: Bool
    let top, bottom, start, end, lead: Double
    let back, title, subtitle, note: String
    let hero: Hero?
    let today: Today
    let ramadan: Ramadan?
    let qada: Qada
    let sunnah: Sunnah
    let c: HomeState.Colors
    let missedColor, secondary: String

    static func decode(_ json: String) -> FastingScreenState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(FastingScreenState.self, from: data)
    }
}

final class FastingScreenModel: ObservableObject {
    let session: NativeFastingSession
    @Published private(set) var state: FastingScreenState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?
    private let tick = UISelectionFeedbackGenerator()

    init(session: NativeFastingSession) {
        self.session = session
        state = FastingScreenState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanFastingState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = FastingScreenState.decode(self.session.state) else { return }
            self.state = s
        }
        tick.prepare()
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    /// يرسل إلى Kotlin بعد رسم اللمسة (كما في المسبحة)
    private func later(_ work: @escaping (NativeFastingSession) -> Void) {
        let session = self.session
        DispatchQueue.main.async { work(session) }
    }

    func back() { session.back() }

    func mark(_ year: Int, _ day: Int) {
        tick.selectionChanged()
        later { $0.mark(year: Int32(year), day: Int32(day)) }
    }

    func setQada(_ value: Int, current: Int) {
        tick.selectionChanged()
        if value > current { later { $0.qadaPlus() } } else if value < current { later { $0.qadaMinus() } }
    }

    func madeUp() {
        UINotificationFeedbackGenerator().notificationOccurred(.success)
        later { $0.madeUp() }
    }

    func voluntary(_ on: Bool) { later { $0.voluntary(on: on) } }

    func remind(_ on: Bool) { later { $0.remind(on: on) } }
}

// MARK: - الشاشة

struct FastingScreenRoot: View {
    @ObservedObject var model: FastingScreenModel
    var body: some View {
        Group {
            if let s = model.state { FastingScreenView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

private struct FastingHeaderBottomKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

struct FastingScreenView: View {
    let s: FastingScreenState
    let p: HomePalette
    @ObservedObject var model: FastingScreenModel
    @State private var glass: Double = 0

    private func track(_ headerBottom: CGFloat) {
        let g = qiblaTasbihGlass(top: CGFloat(s.top), headerBottom: headerBottom)
        if g != glass { glass = g }
    }

    var body: some View {
        ZStack(alignment: .top) {
            p.background
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    QiblaTasbihHeader(title: s.title, subtitle: s.subtitle, symbol: "moon.stars.fill",
                                      top: CGFloat(s.top), start: CGFloat(s.start), end: CGFloat(s.end), rtl: s.rtl, p: p)
                        .background(GeometryReader { g in
                            Color.clear.preference(key: FastingHeaderBottomKey.self, value: g.frame(in: .named("fasting")).maxY)
                        })
                    content
                        .padding(.leading, CGFloat(s.start))
                        .padding(.trailing, CGFloat(s.end))
                }
                .padding(.bottom, CGFloat(s.bottom))
            }
            .coordinateSpace(name: "fasting")
            .onPreferenceChange(FastingHeaderBottomKey.self) { track($0) }
            QiblaTasbihTopBar(title: s.title, back: s.back, canBack: s.canBack, top: CGFloat(s.top), lead: CGFloat(s.lead),
                              p: p, amount: glass) { model.back() }
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }

    /// عمود واحد على الهاتف، وعمودان على الشاشة العريضة (اليوم ورمضان، ثم القضاء والتطوع) كما في ToolPage
    @ViewBuilder private var content: some View {
        if s.wide {
            HStack(alignment: .top, spacing: 20) {
                VStack(spacing: 14) {
                    if let h = s.hero { FastingHero(h: h, p: p) }
                    FastingTodayCard(t: s.today, p: p, model: model)
                    if let r = s.ramadan { FastingRamadanCard(r: r, p: p, missed: HomePalette.color(s.missedColor), model: model) }
                    FastingNote(text: s.note, p: p)
                }
                VStack(spacing: 14) {
                    FastingQadaCard(q: s.qada, p: p, model: model)
                    FastingSunnahCard(u: s.sunnah, p: p, secondary: HomePalette.color(s.secondary), model: model)
                }
            }
        } else {
            VStack(spacing: 14) {
                if let h = s.hero { FastingHero(h: h, p: p) }
                FastingTodayCard(t: s.today, p: p, model: model)
                if let r = s.ramadan { FastingRamadanCard(r: r, p: p, missed: HomePalette.color(s.missedColor), model: model) }
                FastingQadaCard(q: s.qada, p: p, model: model)
                FastingSunnahCard(u: s.sunnah, p: p, secondary: HomePalette.color(s.secondary), model: model)
                FastingNote(text: s.note, p: p)
            }
        }
    }
}

// MARK: بطاقة الصائم

/// بطاقة بتدرج لون التطبيق (ResultCard): العدّ إلى نهاية السحور ثم إلى الإفطار كل ثانية، ووقتاهما تحته
private struct FastingHero: View {
    let h: FastingScreenState.Hero
    let p: HomePalette
    var body: some View {
        TimelineView(.periodic(from: wholeSecond(), by: 1)) { ctx in
            FastingHeroContent(h: h, p: p, now: ctx.date.timeIntervalSince1970)
        }
    }
}

private struct FastingHeroContent: View {
    let h: FastingScreenState.Hero
    let p: HomePalette
    let now: Double

    private var beforeFajr: Bool { if let f = h.fajrAt { return now < f } else { return false } }
    private var beforeMaghrib: Bool { if let m = h.maghribAt { return now < m } else { return false } }
    private var counting: Bool { beforeFajr || beforeMaghrib }
    private var label: String { beforeFajr ? h.suhoorLabel : h.iftarLabel }
    private var value: String {
        if beforeFajr, let f = h.fajrAt { return hms(f - now) }
        if beforeMaghrib, let m = h.maghribAt { return hms(m - now) }
        return h.iftarNow
    }

    var body: some View {
        VStack(spacing: 6) {
            Text(label).font(sys(14, .medium)).foregroundColor(.white.opacity(0.85)).multilineTextAlignment(.center)
            Text(value)
                .font(sys(counting ? 38 : 26, .heavy)).monospacedDigit().foregroundColor(.white)
                .lineLimit(1).minimumScaleFactor(0.5)
                .modifier(NumericRoll(value: value))
                .environment(\.layoutDirection, .leftToRight)
            Text(h.label).font(sys(12, .bold)).foregroundColor(.white)
                .padding(.horizontal, 14).padding(.vertical, 5)
                .background(Capsule().fill(Color(red: 0.247, green: 0.71, blue: 0.584).opacity(0.25)))
            HStack(spacing: 8) {
                FastingHeroTile(label: h.suhoorLabel, time: h.fajrTime, done: h.fajrAt != nil && !beforeFajr, p: p)
                FastingHeroTile(label: h.iftarLabel, time: h.maghribTime, done: h.maghribAt != nil && !beforeMaghrib, p: p)
            }
            .padding(.top, 6)
            Text(h.place).font(sys(12)).foregroundColor(.white.opacity(0.8)).lineLimit(1)
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

/// خانة وقت بيضاء على خلفية البطاقة، بعلامة صح ذهبية بعد مضيّه
private struct FastingHeroTile: View {
    let label, time: String
    let done: Bool
    let p: HomePalette
    var body: some View {
        VStack(spacing: 2) {
            Text(label).font(sys(12, .medium)).foregroundColor(.white.opacity(0.8)).lineLimit(1).minimumScaleFactor(0.7)
            HStack(spacing: 4) {
                Text(time).font(sys(17, .heavy)).foregroundColor(.white).lineLimit(1).minimumScaleFactor(0.7)
                if done { Image(systemName: "checkmark.circle.fill").font(.system(size: 14, weight: .semibold)).foregroundColor(p.gold) }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10).padding(.horizontal, 6)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(Color.white.opacity(0.12)))
        .accessibilityElement(children: .combine)
    }
}

// MARK: اليوم

/// التاريخ الهجري (بفرق المستخدم) وأسباب التطوع فيه، ومفتاح صيام تطوع اليوم، أو تنبيه يومَي العيد وأيام التشريق
private struct FastingTodayCard: View {
    let t: FastingScreenState.Today
    let p: HomePalette
    @ObservedObject var model: FastingScreenModel
    var body: some View {
        QiblaTasbihCard(p: p, title: t.title) {
            VStack(alignment: .leading, spacing: 2) {
                Text(t.hijri).font(sys(17, .bold)).foregroundColor(p.onSurface)
                Text(t.greg).font(sys(13)).foregroundColor(p.onSurfaceVariant)
            }
            .accessibilityElement(children: .combine)
            if !t.kinds.isEmpty { FastingKindChips(kinds: t.kinds, p: p) }
            if let f = t.forbidden { FastingNote(text: f, p: p, warn: true) }
            if let v = t.voluntary {
                HStack(spacing: 12) {
                    Text(v.label).font(sys(15, .semibold)).foregroundColor(p.onSurface)
                    Spacer(minLength: 8)
                    Toggle(v.label, isOn: Binding(get: { v.on }, set: { model.voluntary($0) }))
                        .labelsHidden()
                        .tint(p.primary)
                }
                Text(v.hint).font(sys(12)).foregroundColor(p.onSurfaceVariant)
            }
        }
    }
}

/// أسباب صيام التطوع: كبسولات ذهبية فاتحة تلتف على أكثر من سطر
private struct FastingKindChips: View {
    let kinds: [String]
    let p: HomePalette
    var body: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 120), spacing: 6, alignment: .leading)], alignment: .leading, spacing: 6) {
            ForEach(Array(kinds.enumerated()), id: \.offset) { _, k in
                Text(k).font(sys(13, .bold)).foregroundColor(p.primary).lineLimit(1).minimumScaleFactor(0.8)
                    .padding(.horizontal, 12).padding(.vertical, 5)
                    .background(Capsule().fill(p.gold.opacity(0.18)))
            }
        }
    }
}

// MARK: رمضان

/// شبكة أيام رمضان (اللمس يتنقل بين الصيام والإفطار وبلا تحديد؛ الأيام القادمة لا تُلمس)، وعدد كل حال
private struct FastingRamadanCard: View {
    let r: FastingScreenState.Ramadan
    let p: HomePalette
    let missed: Color
    @ObservedObject var model: FastingScreenModel
    var body: some View {
        QiblaTasbihCard(p: p, title: r.title) {
            Text(r.hint).font(sys(13)).foregroundColor(p.onSurfaceVariant)
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 7), spacing: 6) {
                ForEach(r.days) { d in
                    FastingDayCell(d: d, state: d.mark == "f" ? r.fasted : (d.mark == "m" ? r.missed : r.none), p: p, missed: missed) {
                        model.mark(r.year, d.d)
                    }
                }
            }
            HStack(spacing: 10) {
                ForEach(Array(r.tiles.enumerated()), id: \.offset) { _, t in
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

private struct FastingDayCell: View {
    let d: FastingScreenState.Day
    let state: String
    let p: HomePalette
    let missed: Color
    let action: () -> Void

    private var fill: Color { d.mark == "f" ? p.primary : (d.mark == "m" ? missed : p.primaryContainer.opacity(0.6)) }
    private var ink: Color { d.mark == "f" || d.mark == "m" ? .white : p.onSurfaceVariant }

    var body: some View {
        Button(action: action) {
            ZStack {
                RoundedRectangle(cornerRadius: 12, style: .continuous).fill(fill)
                RoundedRectangle(cornerRadius: 12, style: .continuous).strokeBorder(p.gold, lineWidth: d.today ? 2 : 0)
                Text(verbatim: String(d.d)).font(sys(15, .bold)).monospacedDigit().foregroundColor(ink)
            }
            .aspectRatio(1, contentMode: .fit)
            .animation(UIAccessibility.isReduceMotionEnabled ? nil : .easeOut(duration: 0.2), value: d.mark)
        }
        .buttonStyle(PressScale(scale: 0.9))
        .disabled(!d.enabled)
        .opacity(d.enabled ? 1 : 0.45)
        .accessibilityLabel(d.label)
        .accessibilityValue(state)
    }
}

// MARK: القضاء

/// أيام القضاء: العدد وعدّاد النظام، وزر «قضيت يوماً»، وآخر ما سُجّل
private struct FastingQadaCard: View {
    let q: FastingScreenState.Qada
    let p: HomePalette
    @ObservedObject var model: FastingScreenModel
    var body: some View {
        QiblaTasbihCard(p: p, title: q.title) {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(q.countText).font(sys(36, .heavy)).foregroundColor(p.primary).lineLimit(1)
                        .modifier(NumericRoll(value: q.countText))
                    Text(q.owed).font(sys(14)).foregroundColor(p.onSurfaceVariant)
                }
                .accessibilityElement(children: .combine)
                Spacer(minLength: 8)
                Stepper(q.owed, value: Binding(get: { q.count }, set: { model.setQada($0, current: q.count) }), in: 0...Swift.max(q.max, 1))
                    .labelsHidden()
                    .accessibilityLabel(q.owed)
                    .accessibilityValue(q.countText)
            }
            Button { model.madeUp() } label: {
                Label(q.madeUp, systemImage: "checkmark.circle.fill")
                    .font(sys(16, .semibold)).foregroundColor(.white)
                    .frame(maxWidth: .infinity).padding(.vertical, 13)
                    .background(Capsule().fill(LinearGradient(colors: [p.hero1, p.hero2], startPoint: .topLeading, endPoint: .bottomTrailing)))
            }
            .buttonStyle(PressScale(scale: 0.97))
            .disabled(q.count == 0)
            .opacity(q.count == 0 ? 0.45 : 1)
            Text(q.history).font(sys(15, .bold)).foregroundColor(p.onSurface).accessibilityAddTraits(.isHeader)
            if q.log.isEmpty {
                Text(q.empty).font(sys(13)).foregroundColor(p.onSurfaceVariant)
            }
            ForEach(Array(q.log.enumerated()), id: \.offset) { _, e in
                FastingRow(title: e.label, sub: e.date.isEmpty ? nil : e.date, trailing: e.delta, trailingColor: p.onSurface, p: p)
            }
        }
    }
}

// MARK: التطوع

/// صيام التطوع: ما سُجّل، والست من شوال في شوال، والأيام القادمة بأسبابها، ومفتاح التذكير
private struct FastingSunnahCard: View {
    let u: FastingScreenState.Sunnah
    let p: HomePalette
    let secondary: Color
    @ObservedObject var model: FastingScreenModel
    var body: some View {
        QiblaTasbihCard(p: p, title: u.title) {
            FastingRow(title: u.logged, sub: nil, trailing: u.loggedValue, trailingColor: p.onSurface, p: p)
            if let sh = u.shawwal {
                Text(sh).font(sys(15, .bold)).foregroundColor(p.primary)
            }
            Text(u.upcoming).font(sys(15, .bold)).foregroundColor(p.onSurface).accessibilityAddTraits(.isHeader)
            ForEach(u.days) { d in
                FastingRow(title: d.kinds, sub: d.date, trailing: d.rel, trailingColor: secondary, p: p)
            }
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(u.remind).font(sys(15, .semibold)).foregroundColor(p.onSurface)
                    Text(u.remindDesc).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                }
                Spacer(minLength: 8)
                Toggle(u.remind, isOn: Binding(get: { u.remindOn }, set: { model.remind($0) }))
                    .labelsHidden()
                    .tint(p.primary)
            }
            .padding(.top, 4)
        }
    }
}

/// سطر على خلفية فاتحة: عنوان وسطر تحته، وقيمة في نهايته (StatRow وقائمة المناسبات)
private struct FastingRow: View {
    let title: String
    let sub: String?
    let trailing: String
    let trailingColor: Color
    let p: HomePalette
    var body: some View {
        HStack(spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(sys(15, .bold)).foregroundColor(p.onSurface).fixedSize(horizontal: false, vertical: true)
                if let sub = sub { Text(sub).font(sys(12)).foregroundColor(p.onSurfaceVariant).fixedSize(horizontal: false, vertical: true) }
            }
            Spacer(minLength: 4)
            Text(trailing).font(sys(14, .heavy)).foregroundColor(trailingColor).multilineTextAlignment(.trailing)
        }
        .padding(.horizontal, 14).padding(.vertical, 11)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.primaryContainer.opacity(0.45)))
        .accessibilityElement(children: .combine)
    }
}

/// ملاحظة على خلفية فاتحة بلون التطبيق (Note)، وبالذهبي للتنبيه
private struct FastingNote: View {
    let text: String
    let p: HomePalette
    var warn: Bool = false
    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: warn ? "exclamationmark.triangle.fill" : "info.circle.fill")
                .font(.system(size: 16, weight: .semibold)).foregroundColor(warn ? p.goldDeep : p.primary)
                .accessibilityHidden(true)
            Text(text).font(sys(14)).foregroundColor(warn ? p.onSurface : p.onPrimaryContainer)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(warn ? p.gold.opacity(0.18) : p.primaryContainer))
    }
}
