import SwiftUI
import UIKit
import CoreLocation
import Shared

/// شاشة المواقيت بواجهة النظام (SwiftUI) بطراز الرئيسية (NativeHome.swift): ترويسة بتدرج لون التطبيق ونجمته،
/// والصلاة القادمة في بطاقة بارزة يعدّ عدّادها كل ثانية، ومواقيت اليوم المختار (الحاضرة مظللة ومعها ما مضى على أذانها،
/// والقادمة بحدّ ذهبي ومعها العدّ إليها، ونهاية كل وقت وإقامته)، وجدول الشهر، والقبلة ببوصلة الهاتف نفسها،
/// والموقع وطريقة الحساب والعصر، والإقامة، والتنبيهات، والأدلة — كما في PrayerScreen.kt.
/// الحالة من Kotlin (NativePrayer.ios.kt) مع الإشعار MizanPrayerState، واللمس يعود إليه.
enum NativePrayerScreen {
    static func install() {
        NativePrayer.shared.attach { session in PrayerHostController(session: session) }
    }
}

final class PrayerHostController: UIHostingController<PrayerRoot> {
    init(session: NativePrayerSession) {
        let model = PrayerModel(session: session)
        super.init(rootView: PrayerRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct PrayerState: Decodable {
    struct Next: Decodable { let key, label, atText: String; let at: Double; let today: Bool }
    struct Row: Decodable, Identifiable { let key, name, time: String; let end, iqama: String?; let start: Double; var id: String { key } }
    struct Day: Decodable { let offset: Int; let isToday: Bool; let title, hijri: String; let rows: [Row] }
    struct MonthRow: Decodable, Identifiable { let day: String; let times: [String]; let today: Bool; var id: String { day } }
    struct Month: Decodable { let title: String; let headers: [String]; let rows: [MonthRow] }
    struct Qibla: Decodable { let bearing: Double; let title, facing, info: String }
    struct Method: Decodable, Identifiable, Hashable { let key, name: String; var id: String { key } }
    struct Location: Decodable {
        let title, useMyLocation, locatingLabel, orPickCity, place, methodLabel, method, asrLabel, asrStd, asrHanafi, latLngLabel, latLng: String
        let locating, hanafi: Bool
        let methods: [Method]
    }
    struct IqamaItem: Decodable, Identifiable { let key, name: String; let min: Int; var id: String { key } }
    struct Iqama: Decodable { let title, desc: String; let items: [IqamaItem] }
    struct Alerts: Decodable { let title, desc, settings, settingsSub, note: String; let on: Bool }
    struct Proof: Decodable, Identifiable {
        let kind, title, body, source: String
        let ayah, tr, hadith: String?
        var id: String { title + source }
    }
    struct Evidence: Decodable { let title, sub: String; let kinds: [String: String]; let points: [Proof] }

    let rtl: Bool
    let lang: String
    let dark: Bool
    let top, bottom, start, end: Double
    let title, subtitle: String
    let next: Next
    let day: Day
    let sinceAdhan, endsNote, todayLabel, monthLabel, hideMonthLabel, prevDay, nextDay: String
    let month: Month
    let qibla: Qibla
    let location: Location
    let iqama: Iqama
    let alerts: Alerts
    let evidence: Evidence
    let c: HomeState.Colors

    static func decode(_ json: String) -> PrayerState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(PrayerState.self, from: data)
    }
}

final class PrayerModel: ObservableObject {
    let session: NativePrayerSession
    @Published private(set) var state: PrayerState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativePrayerSession) {
        self.session = session
        state = PrayerState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanPrayerState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = PrayerState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    func open(_ route: String) { session.open(route: route) }
    func day(_ delta: Int) { session.day(delta: Int32(delta)) }
    func locate() { session.locate() }
    func pickCity() { session.pickCity() }
    func notify(_ on: Bool) { session.notify(on: on) }
    func method(_ key: String) { session.method(key: key) }
    func hanafi(_ on: Bool) { session.hanafi(on: on) }
    func iqama(_ key: String, _ minutes: Int) { session.iqama(key: key, minutes: Int32(minutes)) }
}

/// اتجاه الهاتف من البوصلة (الشمال الحقيقي إن توفر)، ما دامت البطاقة ظاهرة
final class Compass: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published var heading: Double = 0
    @Published var available = CLLocationManager.headingAvailable()
    private let manager = CLLocationManager()

    override init() {
        super.init()
        manager.delegate = self
        manager.headingFilter = 1
    }

    func start() { if available { manager.startUpdatingHeading() } }
    func stop() { manager.stopUpdatingHeading() }

    func locationManager(_ manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        heading = newHeading.trueHeading >= 0 ? newHeading.trueHeading : newHeading.magneticHeading
    }
}

// MARK: - الشاشة

struct PrayerRoot: View {
    @ObservedObject var model: PrayerModel
    var body: some View {
        Group {
            if let s = model.state { PrayerView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

private struct PrayerHeaderBottomKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

struct PrayerView: View {
    let s: PrayerState
    let p: HomePalette
    let model: PrayerModel
    @State private var headerBottom: CGFloat = .greatestFiniteMagnitude
    @State private var showMonth = false
    @State private var forward = true

    private var glass: Double {
        let bar = CGFloat(s.top) + 52, ramp: CGFloat = 36
        return Double(min(max((bar + ramp - headerBottom) / ramp, 0), 1))
    }

    var body: some View {
        ZStack(alignment: .top) {
            p.background
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    PageHeader(title: s.title, subtitle: s.subtitle, symbol: "clock", s: s, p: p)
                        .background(GeometryReader { g in
                            Color.clear.preference(key: PrayerHeaderBottomKey.self, value: g.frame(in: .named("prayer")).maxY)
                        })
                    VStack(spacing: 14) {
                        NextHero(next: s.next, p: p)
                        dayCard
                        QiblaCard(q: s.qibla, p: p) { model.open("qibla") }
                        if showMonth { MonthCard(m: s.month, p: p).transition(.opacity.combined(with: .move(edge: .top))) }
                        LocationCard(l: s.location, p: p, model: model)
                        IqamaCard(q: s.iqama, p: p, model: model)
                        AlertsCard(a: s.alerts, p: p, model: model)
                        EvidenceView(e: s.evidence, p: p)
                    }
                    .padding(.top, 6)
                    .padding(.leading, CGFloat(s.start))
                    .padding(.trailing, CGFloat(s.end))
                }
                .padding(.bottom, CGFloat(s.bottom))
            }
            .coordinateSpace(name: "prayer")
            .onPreferenceChange(PrayerHeaderBottomKey.self) { headerBottom = $0 }
            PrayerGlassBar(title: s.title, top: CGFloat(s.top), p: p, amount: glass)
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }

    /// اليوم المختار: سهمان لليوم السابق والتالي، ومواقيته بطراز شبكة الرئيسية، و«اليوم» وجدول الشهر
    private var dayCard: some View {
        Card(p: p) {
            HStack {
                ArrowButton(symbol: "chevron.backward", label: s.prevDay, p: p) { forward = false; model.day(-1) }
                VStack(spacing: 2) {
                    Text(s.day.title).font(sys(16, .bold)).foregroundColor(p.onSurface).multilineTextAlignment(.center)
                    Text(s.day.hijri).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                }
                .frame(maxWidth: .infinity)
                ArrowButton(symbol: "chevron.forward", label: s.nextDay, p: p) { forward = true; model.day(1) }
            }
            // يوم جديد ينزلق من جهة السهم المضغوط (الجهتان تتبعان اتجاه اللغة)
            ZStack {
                DayRows(s: s, p: p)
                    .id(s.day.offset)
                    .transition(.asymmetric(
                        insertion: .move(edge: forward ? .trailing : .leading).combined(with: .opacity),
                        removal: .opacity
                    ))
            }
            .animation(.easeOut(duration: 0.28), value: s.day.offset)
            .clipped()
            Text(s.endsNote).font(sys(12)).foregroundColor(p.onSurfaceVariant)
            HStack(spacing: 8) {
                if !s.day.isToday {
                    Pill(text: s.todayLabel, p: p) { forward = s.day.offset < 0; model.day(0) }
                }
                Pill(text: showMonth ? s.hideMonthLabel : s.monthLabel, p: p) { withAnimation(.easeOut(duration: 0.3)) { showMonth.toggle() } }
                Spacer(minLength: 0)
            }
        }
    }
}

// MARK: الأجزاء

/// ترويسة الصفحة بطراز ترويسة الرئيسية: تدرج من لون التطبيق الفاتح، ونجمة باهتة، وأيقونة الأداة والعنوان والموقع
struct PageHeader: View {
    let title: String
    let subtitle: String
    let symbol: String
    let s: PrayerState
    let p: HomePalette
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 20, weight: .semibold)).foregroundColor(p.primary)
                .frame(width: 46, height: 46)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.surface.opacity(0.85)).shadow(color: .black.opacity(0.08), radius: 3, y: 1))
            VStack(alignment: .leading, spacing: 0) {
                Text(title).font(sys(28, .heavy)).foregroundColor(p.primary).lineLimit(1).minimumScaleFactor(0.8)
                Text(subtitle).font(sys(14)).foregroundColor(p.onSurfaceVariant).lineLimit(1)
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

private struct PrayerGlassBar: View {
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

/// بطاقة بيضاء بحافة رفيعة كبطاقات الرئيسية
private struct Card<Content: View>: View {
    let p: HomePalette
    var title: String? = nil
    @ViewBuilder let content: () -> Content
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if let t = title { Text(t).font(sys(18, .heavy)).foregroundColor(p.primary).accessibilityAddTraits(.isHeader) }
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.6), lineWidth: 1))
    }
}

private struct ArrowButton: View {
    let symbol: String
    let label: String
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Image(systemName: symbol).font(.system(size: 15, weight: .bold)).foregroundColor(p.primary)
                .frame(width: 38, height: 38).background(Circle().fill(p.primaryContainer))
        }
        .buttonStyle(PressScale(scale: 0.9))
        .accessibilityLabel(label)
    }
}

private struct Pill: View {
    let text: String
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(text).font(sys(14, .semibold)).foregroundColor(p.primary)
                .padding(.horizontal, 14).padding(.vertical, 8)
                .background(Capsule().fill(p.primaryContainer))
        }
        .buttonStyle(PressScale(scale: 0.95))
    }
}

/// الصلاة القادمة في بطاقة بتدرج لون التطبيق (كبطاقة القرآن في الرئيسية)، والعدّ إليها كل ثانية
private struct NextHero: View {
    let next: PrayerState.Next
    let p: HomePalette
    var body: some View {
        TimelineView(.periodic(from: Date(), by: 1)) { ctx in
            HStack(spacing: 14) {
                Image(systemName: prayerSymbol(next.key))
                    .font(.system(size: 22, weight: .semibold)).foregroundColor(p.gold)
                    .frame(width: 52, height: 52)
                    .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(Color.white.opacity(0.14)))
                VStack(alignment: .leading, spacing: 2) {
                    Text(next.label).font(sys(14, .bold)).foregroundColor(p.gold).lineLimit(1)
                    Text(hms(next.at - ctx.date.timeIntervalSince1970))
                        .font(sys(34, .heavy)).monospacedDigit().foregroundColor(.white)
                        .modifier(NumericRoll())
                        .environment(\.layoutDirection, .leftToRight)
                    Text(next.atText).font(sys(13)).foregroundColor(.white.opacity(0.8))
                }
                Spacer(minLength: 0)
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                ZStack(alignment: .trailing) {
                    LinearGradient(colors: [p.hero1, p.hero2], startPoint: .topLeading, endPoint: .bottomTrailing)
                    StarEmblem(color: .white.opacity(0.07), radius: 70, stroke: 1).offset(x: 30)
                }
            )
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .accessibilityElement(children: .combine)
        }
    }
}

/// مواقيت اليوم: الحاضرة مظللة بلون التطبيق، والقادمة بحدّ ذهبي، كما في شبكة الرئيسية
private struct DayRows: View {
    let s: PrayerState
    let p: HomePalette
    var body: some View {
        TimelineView(.periodic(from: Date(), by: 1)) { ctx in
            let now = ctx.date.timeIntervalSince1970
            let rows = s.day.rows
            let current: String? = {
                guard s.day.isToday, let i = rows.lastIndex(where: { $0.key != "sunrise" && $0.start <= now }) else { return nil }
                return (i + 1 < rows.count && now >= rows[i + 1].start) ? nil : rows[i].key
            }()
            VStack(spacing: 8) {
                ForEach(rows) { r in
                    let isCur = r.key == current
                    let isNext = s.day.isToday && r.key == s.next.key && s.next.today
                    DayRow(r: r, p: p, current: isCur, next: isNext,
                           extraLabel: isCur ? s.sinceAdhan : nil,
                           extra: isCur ? hms(now - r.start) : (isNext ? hms(s.next.at - now) : nil))
                }
            }
        }
    }
}

private struct DayRow: View {
    let r: PrayerState.Row
    let p: HomePalette
    let current: Bool
    let next: Bool
    let extraLabel: String?
    let extra: String?
    var body: some View {
        let fg = current ? p.onPrimaryContainer : p.onSurface
        let sunrise = r.key == "sunrise"
        HStack(spacing: 12) {
            Image(systemName: prayerSymbol(r.key))
                .font(.system(size: 16, weight: .regular))
                .foregroundColor(current || next ? p.gold : p.onSurfaceVariant)
                .frame(width: 34, height: 34)
                .background(Circle().fill(current ? p.surface.opacity(0.7) : p.primaryContainer.opacity(0.6)))
            VStack(alignment: .leading, spacing: 1) {
                Text(r.name).font(sys(sunrise ? 15 : 17, .bold)).foregroundColor(sunrise ? p.onSurfaceVariant : fg)
                if let e = r.end { Text(e).font(sys(12)).foregroundColor(fg.opacity(0.7)) }
            }
            Spacer(minLength: 6)
            VStack(alignment: .trailing, spacing: 1) {
                Text(r.time).font(sys(18, .heavy)).foregroundColor(fg)
                if let x = extra {
                    HStack(spacing: 4) {
                        if let l = extraLabel { Text(l).font(sys(11, .medium)).foregroundColor(fg.opacity(0.7)) }
                        Text(x).font(sys(13, .bold)).monospacedDigit().foregroundColor(current ? p.primary : p.goldDeep)
                            .modifier(NumericRoll()).environment(\.layoutDirection, .leftToRight)
                    }
                } else if let iq = r.iqama {
                    Text(iq).font(sys(12, .semibold)).foregroundColor(p.primary)
                }
            }
        }
        .padding(.horizontal, 12).padding(.vertical, 10)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(current ? p.primaryContainer : p.background.opacity(0.6))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(next ? p.gold : Color.clear, lineWidth: 1.5)
        )
        .accessibilityElement(children: .combine)
    }
}

/// جدول الشهر: اليوم وأوقات الصلوات الخمس، واليوم الحالي مظلل
private struct MonthCard: View {
    let m: PrayerState.Month
    let p: HomePalette
    var body: some View {
        Card(p: p, title: m.title) {
            VStack(spacing: 2) {
                HStack(spacing: 0) {
                    ForEach(Array(m.headers.enumerated()), id: \.offset) { _, h in
                        Text(h).font(sys(11, .bold)).foregroundColor(p.onSurfaceVariant).lineLimit(1).minimumScaleFactor(0.7).frame(maxWidth: .infinity)
                    }
                }
                .padding(.bottom, 4)
                ForEach(m.rows) { r in
                    HStack(spacing: 0) {
                        Text(r.day).font(sys(12, .bold)).foregroundColor(p.onSurface).frame(maxWidth: .infinity)
                        ForEach(Array(r.times.enumerated()), id: \.offset) { _, t in
                            Text(t).font(sys(12)).monospacedDigit().foregroundColor(p.onSurface).frame(maxWidth: .infinity)
                        }
                    }
                    .padding(.vertical, 5)
                    .background(RoundedRectangle(cornerRadius: 8, style: .continuous).fill(r.today ? p.primaryContainer : Color.clear))
                }
            }
        }
    }
}

/// القبلة: بوصلة صغيرة تدور مع الهاتف، والإبرة الذهبية إلى الكعبة، وتتذهّب الحلقة عند الاتجاه الصحيح
private struct QiblaCard: View {
    let q: PrayerState.Qibla
    let p: HomePalette
    let action: () -> Void
    @StateObject private var compass = Compass()

    var body: some View {
        let heading = compass.available ? compass.heading : 0
        let diff = ((q.bearing - heading).truncatingRemainder(dividingBy: 360) + 540).truncatingRemainder(dividingBy: 360) - 180
        let aligned = compass.available && abs(diff) < 4
        Button(action: action) {
            HStack(spacing: 12) {
                ZStack {
                    Circle().stroke(aligned ? p.gold : p.outlineVariant, lineWidth: 2).padding(2)
                    ForEach(0..<4, id: \.self) { i in
                        Circle().fill(i == 0 ? Color(red: 0.9, green: 0.33, blue: 0.29) : (aligned ? p.gold : p.outlineVariant))
                            .frame(width: i == 0 ? 5 : 3.2, height: i == 0 ? 5 : 3.2)
                            .offset(y: -19)
                            .rotationEffect(.degrees(Double(i) * 90 - heading))
                    }
                    Capsule().fill(p.gold).frame(width: 3, height: 13).offset(y: -6.5).rotationEffect(.degrees(diff))
                    Rectangle().fill(Color(white: 0.07)).frame(width: 9, height: 9)
                        .overlay(Rectangle().fill(p.gold).frame(height: 2).offset(y: -1.5))
                        .offset(y: -14).rotationEffect(.degrees(diff))
                    Circle().fill(p.gold).frame(width: 6, height: 6)
                }
                .frame(width: 52, height: 52)
                .animation(.easeOut(duration: 0.22), value: diff)
                VStack(alignment: .leading, spacing: 2) {
                    Text(aligned ? q.facing : q.title).font(sys(16, .bold)).foregroundColor(aligned ? p.primary : p.onSurface)
                    Text(q.info).font(sys(12)).foregroundColor(p.onSurfaceVariant).lineLimit(2)
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.forward").font(.system(size: 14, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
            }
            .padding(14)
            .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(p.surface))
            .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(aligned ? p.gold : p.outlineVariant.opacity(0.6), lineWidth: aligned ? 1.5 : 1))
        }
        .buttonStyle(PressScale(scale: 0.98))
        .onAppear { compass.start() }
        .onDisappear { compass.stop() }
    }
}

/// الموقع وطريقة الحساب: تحديد الموقع، أو اختيار مدينة، وطريقة الحساب، ومذهب العصر
private struct LocationCard: View {
    let l: PrayerState.Location
    let p: HomePalette
    let model: PrayerModel
    var body: some View {
        Card(p: p, title: l.title) {
            Button { model.locate() } label: {
                HStack(spacing: 8) {
                    if l.locating { ProgressView().tint(.white) } else { Image(systemName: "location.fill") }
                    Text(l.locating ? l.locatingLabel : l.useMyLocation).font(sys(15, .semibold))
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity).padding(.vertical, 13)
                .background(Capsule().fill(LinearGradient(colors: [p.hero1, p.hero2], startPoint: .leading, endPoint: .trailing)))
            }
            .buttonStyle(PressScale())
            .disabled(l.locating)
            Text(l.orPickCity).font(sys(13, .medium)).foregroundColor(p.onSurfaceVariant)
            Button { model.pickCity() } label: {
                Field(text: l.place, symbol: "mappin.and.ellipse", p: p)
            }
            .buttonStyle(PressScale(scale: 0.98))
            Text(l.methodLabel).font(sys(13, .medium)).foregroundColor(p.onSurfaceVariant)
            Menu {
                ForEach(l.methods) { m in
                    Button { model.method(m.key) } label: {
                        if m.key == l.method { Label(m.name, systemImage: "checkmark") } else { Text(m.name) }
                    }
                }
            } label: {
                Field(text: l.methods.first(where: { $0.key == l.method })?.name ?? l.method, symbol: "chevron.up.chevron.down", p: p)
            }
            Text(l.asrLabel).font(sys(13, .medium)).foregroundColor(p.onSurfaceVariant)
            Picker(l.asrLabel, selection: Binding(get: { l.hanafi }, set: { model.hanafi($0) })) {
                Text(l.asrStd).tag(false)
                Text(l.asrHanafi).tag(true)
            }
            .pickerStyle(.segmented)
            HStack {
                Text(l.latLngLabel).font(sys(13)).foregroundColor(p.onSurfaceVariant)
                Spacer()
                Text(l.latLng).font(sys(13, .semibold)).foregroundColor(p.onSurface).environment(\.layoutDirection, .leftToRight)
            }
        }
    }
}

private struct Field: View {
    let text: String
    let symbol: String
    let p: HomePalette
    var body: some View {
        HStack {
            Text(text).font(sys(15)).foregroundColor(p.onSurface).lineLimit(1)
            Spacer(minLength: 6)
            Image(systemName: symbol).font(.system(size: 13, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
        }
        .padding(.horizontal, 14).padding(.vertical, 12)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.background.opacity(0.7)))
        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(p.outlineVariant, lineWidth: 1))
    }
}

/// الإقامة: دقائق بعد الأذان لكل صلاة
private struct IqamaCard: View {
    let q: PrayerState.Iqama
    let p: HomePalette
    let model: PrayerModel
    var body: some View {
        Card(p: p, title: q.title) {
            Text(q.desc).font(sys(12)).foregroundColor(p.onSurfaceVariant)
            ForEach(q.items) { it in
                HStack {
                    Image(systemName: prayerSymbol(it.key)).font(.system(size: 14)).foregroundColor(p.gold).frame(width: 22)
                    Text(it.name).font(sys(15, .semibold)).foregroundColor(p.onSurface)
                    Spacer()
                    Text("+\(it.min)").font(sys(15, .bold)).monospacedDigit().foregroundColor(p.primary)
                        .environment(\.layoutDirection, .leftToRight)
                    Stepper("", value: Binding(get: { it.min }, set: { model.iqama(it.key, $0) }), in: 0...90)
                        .labelsHidden()
                }
            }
        }
    }
}

/// تنبيه الأذان وإعداداته
private struct AlertsCard: View {
    let a: PrayerState.Alerts
    let p: HomePalette
    let model: PrayerModel
    var body: some View {
        Card(p: p) {
            Toggle(isOn: Binding(get: { a.on }, set: { model.notify($0) })) {
                HStack(spacing: 10) {
                    Image(systemName: "bell.fill").foregroundColor(p.primary)
                    VStack(alignment: .leading, spacing: 1) {
                        Text(a.title).font(sys(15, .bold)).foregroundColor(p.onSurface)
                        Text(a.desc).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                    }
                }
            }
            .tint(p.primary)
            Button { model.open("adhan") } label: {
                HStack(spacing: 10) {
                    Image(systemName: "speaker.wave.2.fill").foregroundColor(p.primary)
                    VStack(alignment: .leading, spacing: 1) {
                        Text(a.settings).font(sys(15, .bold)).foregroundColor(p.onSurface)
                        Text(a.settingsSub).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                    }
                    Spacer()
                    Image(systemName: "chevron.forward").font(.system(size: 14, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(PressScale(scale: 0.98))
            Text(a.note).font(sys(12)).foregroundColor(p.onSurfaceVariant)
        }
    }
}

/// الأدلة: آيات وأحاديث وأقوال، تنفتح بلمسة
private struct EvidenceView: View {
    let e: PrayerState.Evidence
    let p: HomePalette
    @State private var open = false

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
        default: return "graduationcap.fill"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button { withAnimation(.easeInOut(duration: 0.3)) { open.toggle() } } label: {
                HStack(spacing: 12) {
                    Image(systemName: "book.closed.fill").foregroundColor(p.goldDeep)
                        .frame(width: 42, height: 42)
                        .background(RoundedRectangle(cornerRadius: 13, style: .continuous).fill(p.gold.opacity(0.16)))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(e.title).font(sys(16, .bold)).foregroundColor(p.onSurface)
                        Text(e.sub).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                    }
                    Spacer()
                    Image(systemName: "chevron.down").font(.system(size: 14, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
                        .rotationEffect(.degrees(open ? 180 : 0))
                }
                .padding(16)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            if open {
                VStack(alignment: .leading, spacing: 14) {
                    ForEach(e.points) { pt in
                        VStack(alignment: .leading, spacing: 6) {
                            Label(e.kinds[pt.kind] ?? pt.kind, systemImage: symbol(pt.kind))
                                .font(sys(11, .bold)).foregroundColor(color(pt.kind))
                                .padding(.horizontal, 8).padding(.vertical, 3)
                                .background(Capsule().fill(color(pt.kind).opacity(0.12)))
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
                            Text(pt.source).font(sys(12, .bold)).foregroundColor(color(pt.kind))
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
