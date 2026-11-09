import SwiftUI
import UIKit
import CoreLocation
import Shared

/// شاشة القبلة بواجهة النظام (SwiftUI) بطراز الرئيسية (NativeHome.swift): ترويسة بتدرج لون التطبيق ونجمته وشريط علوي ثابت
/// فيه زر الرجوع، وبوصلة حية من اتجاه الهاتف (الشمال الحقيقي) تتذهّب حلقتها وتنبض حين يتجه الهاتف إلى القبلة (مع اهتزاز)،
/// وكم يستدير المستخدم، والقبلة بالكاميرا (ArQibla.swift)، وزاوية القبلة، والموقع والمسافة إلى الكعبة، ونصيحة الدقة،
/// ومعايرة البوصلة، والأدلة — كما في QiblaScreen.kt. الحالة من Kotlin (NativeQibla.ios.kt) مع الإشعار MizanQiblaState.
/// وفي هذا الملف أيضاً ما تشترك فيه صفحتا القبلة والمسبحة (NativeTasbih.swift): الشريط العلوي، والترويسة، والبطاقة، والأدلة.
enum NativeQiblaScreen {
    static func install() {
        NativeQibla.shared.attach { session in QiblaHostController(session: session) }
    }
}

final class QiblaHostController: UIHostingController<QiblaRoot> {
    init(session: NativeQiblaSession) {
        let model = QiblaModel(session: session)
        super.init(rootView: QiblaRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

/// أدلة الأداة (nativeProofs في NativeQibla.ios.kt): للقبلة والمسبحة
struct QiblaTasbihProofs: Decodable {
    struct Point: Decodable, Identifiable {
        let kind, concept, title, body, source: String
        let ayah, tr, hadith: String?
        var id: String { title + source }
    }
    let title, sub: String
    let kinds: [String: String]
    let points: [Point]
}

struct QiblaState: Decodable {
    struct Ar: Decodable { let title, hint: String }
    struct Calib: Decodable { let title, first, done: String; let steps: [String] }
    struct Extra: Decodable { let north, warnBg, warnFg, warn: String }

    let rtl, dark, canBack: Bool
    let top, bottom, start, end, lead, width: Double
    let back, title, subtitle: String
    let bearing: Double
    let noCompass, facing, turnRight, turnLeft: String
    let angleLabel, angle, towards, locationLabel, location, distanceLabel, distance, headingLabel, tip, lowAccuracy: String
    let ar: Ar?
    let calib: Calib
    let evidence: QiblaTasbihProofs
    let c: HomeState.Colors
    let x: Extra

    static func decode(_ json: String) -> QiblaState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(QiblaState.self, from: data)
    }
}

final class QiblaModel: ObservableObject {
    let session: NativeQiblaSession
    /// اتجاه الهاتف: خارج الحالة، فلا تُعاد الصفحة كلها مع كل درجة (تتبعه البوصلة وسطر الاتجاه وحدهما)
    let heading = QiblaHeading()
    @Published private(set) var state: QiblaState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativeQiblaSession) {
        self.session = session
        state = QiblaState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanQiblaState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = QiblaState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit {
        if let t = token { NotificationCenter.default.removeObserver(t) }
        heading.stop()
    }

    func back() { session.back() }
    func ar() { session.ar() }
}

/// اتجاه أعلى الشاشة من البوصلة: الشمال الحقيقي إن عُرف (ويُطلب الموقع لذلك إن كان مأذوناً)، وإلا المغناطيسي.
/// dial زاوية متصلة لا تقفز من 359 إلى 0، فتدور البوصلة في أقصر طريق عبر الشمال.
final class QiblaHeading: NSObject, ObservableObject, CLLocationManagerDelegate {
    /// 0..<360
    @Published private(set) var heading: Double = 0
    @Published private(set) var dial: Double = 0
    /// دقة منخفضة (أكثر من 25° أو مجهولة): تُعرض نصيحة المعايرة
    @Published private(set) var low = false
    let available = CLLocationManager.headingAvailable()
    private let manager = CLLocationManager()
    private var running = false
    private var locating = false
    private var hasReading = false

    override init() {
        super.init()
        manager.delegate = self
        manager.headingFilter = 1
        manager.desiredAccuracy = kCLLocationAccuracyKilometer
    }

    func start() {
        guard available, !running else { return }
        running = true
        startLocationIfAllowed()
        manager.startUpdatingHeading()
    }

    func stop() {
        guard running else { return }
        running = false
        manager.stopUpdatingHeading()
        if locating { manager.stopUpdatingLocation(); locating = false }
    }

    /// الموقع يجعل trueHeading معروفاً (الشمال الحقيقي)؛ لا يُسأل الإذن هنا
    private func startLocationIfAllowed() {
        guard running, !locating else { return }
        switch manager.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            locating = true
            manager.startUpdatingLocation()
        default: break
        }
    }

    /// البوصلة تقيس من أعلى الجهاز قائماً؛ على iPad بالعرض يُزاح الاتجاه إلى أعلى الشاشة
    private func screenOffset() -> Double {
        guard let scene = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).first else { return 0 }
        let orientation: UIInterfaceOrientation
        if #available(iOS 16.0, *) { orientation = scene.effectiveGeometry.interfaceOrientation } else { orientation = scene.interfaceOrientation }
        switch orientation {
        case .landscapeLeft: return -90
        case .landscapeRight: return 90
        case .portraitUpsideDown: return 180
        default: return 0
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        let raw = newHeading.trueHeading >= 0 ? newHeading.trueHeading : newHeading.magneticHeading
        var v = (raw + screenOffset()).truncatingRemainder(dividingBy: 360)
        if v < 0 { v += 360 }
        if hasReading {
            let d = ((v - heading).truncatingRemainder(dividingBy: 360) + 540).truncatingRemainder(dividingBy: 360) - 180
            dial += d
        } else {
            dial = v
            hasReading = true
        }
        heading = v
        let l = newHeading.headingAccuracy < 0 || newHeading.headingAccuracy > 25
        if l != low { low = l }
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) { startLocationIfAllowed() }
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {}
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {}
    /// دقة البوصلة منخفضة: يعرض iOS شاشة معايرته (كما في Services.ios.kt)
    func locationManagerShouldDisplayHeadingCalibration(_ manager: CLLocationManager) -> Bool { true }
}

// MARK: - الشاشة

struct QiblaRoot: View {
    @ObservedObject var model: QiblaModel
    var body: some View {
        Group {
            if let s = model.state { QiblaView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

private struct QiblaHeaderBottomKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

struct QiblaView: View {
    let s: QiblaState
    let p: HomePalette
    let model: QiblaModel
    @State private var glass: Double = 0
    @State private var calib = false

    private func track(_ headerBottom: CGFloat) {
        let g = qiblaTasbihGlass(top: CGFloat(s.top), headerBottom: headerBottom)
        if g != glass { glass = g }
    }

    var body: some View {
        ZStack(alignment: .top) {
            p.background
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    QiblaTasbihHeader(title: s.title, subtitle: s.subtitle, symbol: "safari.fill",
                                      top: CGFloat(s.top), start: CGFloat(s.start), end: CGFloat(s.end), rtl: s.rtl, p: p)
                        .background(GeometryReader { g in
                            Color.clear.preference(key: QiblaHeaderBottomKey.self, value: g.frame(in: .named("qibla")).maxY)
                        })
                    VStack(spacing: 14) {
                        QiblaCompassCard(s: s, p: p, heading: model.heading)
                        if let ar = s.ar {
                            QiblaTasbihCard(p: p) {
                                QiblaLinkRow(title: ar.title, sub: ar.hint, symbol: "arkit", p: p) { model.ar() }
                            }
                        }
                        angleHero
                        QiblaStatsCard(s: s, p: p, heading: model.heading)
                        QiblaNote(s: s, p: p, heading: model.heading)
                        if model.heading.available {
                            QiblaTasbihCard(p: p) {
                                QiblaLinkRow(title: s.calib.title, sub: s.calib.first, symbol: "safari", p: p) { calib = true }
                            }
                        }
                        QiblaTasbihProofsCard(e: s.evidence, p: p)
                    }
                    .padding(.leading, CGFloat(s.start))
                    .padding(.trailing, CGFloat(s.end))
                }
                .padding(.bottom, CGFloat(s.bottom))
            }
            .coordinateSpace(name: "qibla")
            .onPreferenceChange(QiblaHeaderBottomKey.self) { track($0) }
            QiblaTasbihTopBar(title: s.title, back: s.back, canBack: s.canBack, top: CGFloat(s.top), lead: CGFloat(s.lead),
                              p: p, amount: glass) { model.back() }
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
        .onAppear { model.heading.start() }
        .onDisappear { model.heading.stop() }
        .sheet(isPresented: $calib) {
            QiblaCalibSheet(c: s.calib, p: p) { calib = false }
                .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
                .environment(\.colorScheme, s.dark ? .dark : .light)
        }
    }

    /// زاوية القبلة من الشمال وجهتها في بطاقة بتدرج لون التطبيق (ResultCard)
    private var angleHero: some View {
        VStack(spacing: 6) {
            Text(s.angleLabel).font(sys(15)).foregroundColor(.white.opacity(0.85)).multilineTextAlignment(.center)
            Text(s.angle).font(sys(32, .heavy)).foregroundColor(.white)
                .modifier(NumericRoll(value: s.angle))
                .environment(\.layoutDirection, .leftToRight)
            Text(s.towards).font(sys(12, .bold)).foregroundColor(.white)
                .padding(.horizontal, 12).padding(.vertical, 5)
                .background(Capsule().fill(p.gold.opacity(0.25)))
        }
        .padding(22)
        .frame(maxWidth: .infinity)
        .background(
            ZStack(alignment: .bottomLeading) {
                LinearGradient(colors: [p.hero1, p.hero2], startPoint: .topLeading, endPoint: .bottomTrailing)
                StarEmblem(color: .white.opacity(0.06), radius: 72, stroke: 1).offset(x: -40, y: 50)
            }
            .environment(\.layoutDirection, .leftToRight)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .accessibilityElement(children: .combine)
    }
}

/// البوصلة وسطر الاستدارة؛ وعند الاتجاه الصحيح اهتزاز نجاح (كما في Compose)
private struct QiblaCompassCard: View {
    let s: QiblaState
    let p: HomePalette
    @ObservedObject var heading: QiblaHeading

    var body: some View {
        let has = heading.available
        let h = has ? heading.heading : 0
        // موجب = لليمين
        let diff = ((s.bearing - h).truncatingRemainder(dividingBy: 360) + 540).truncatingRemainder(dividingBy: 360) - 180
        let aligned = has && abs(diff) < 4
        let hint: String = {
            if !has { return s.noCompass }
            if aligned { return s.facing }
            return (diff > 0 ? s.turnRight : s.turnLeft).replacingOccurrences(of: "{d}", with: String(Int(abs(diff))))
        }()
        QiblaTasbihCard(p: p) {
            QiblaDial(bearing: s.bearing, dial: has ? heading.dial : 0, aligned: aligned, p: p, north: HomePalette.color(s.x.north))
                .frame(maxWidth: min(440, CGFloat(s.width)))
                .padding(8)
                .frame(maxWidth: .infinity)
                .accessibilityHidden(true)
            Text(hint)
                .font(sys(16, .bold))
                .foregroundColor(aligned ? p.primary : p.onSurface)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .animation(.easeOut(duration: 0.25), value: aligned)
                .accessibilityAddTraits(.updatesFrequently)
        }
        .onChange(of: aligned) { on in
            if on { UINotificationFeedbackGenerator().notificationOccurred(.success) }
        }
    }
}

/// قرص البوصلة (Compass في QiblaScreen.kt): يدور بعكس اتجاه الهاتف، وعليه سهم القبلة والكعبة؛ لا ينعكس في العربية
private struct QiblaDial: View {
    let bearing: Double
    let dial: Double
    let aligned: Bool
    let p: HomePalette
    let north: Color

    var body: some View {
        GeometryReader { g in
            let side = min(g.size.width, g.size.height)
            let r = side / 2
            let k = r * 0.13
            ZStack {
                Circle().fill(aligned ? p.gold : p.primaryContainer)
                    .animation(.easeOut(duration: 0.3), value: aligned)
                Circle().fill(p.surface).frame(width: r * 1.8, height: r * 1.8)
                // موجة ذهبية تنطلق من المركز ما دام الهاتف متجهاً للقبلة
                if aligned {
                    QiblaWave(color: p.gold, size: r * 1.8).transition(.opacity)
                }
                ZStack {
                    QiblaTicks(kind: .short).stroke(p.onSurfaceVariant.opacity(0.35), style: StrokeStyle(lineWidth: 1.2, lineCap: .round))
                    QiblaTicks(kind: .long).stroke(p.onSurfaceVariant.opacity(0.7), style: StrokeStyle(lineWidth: 2, lineCap: .round))
                    QiblaTicks(kind: .north).stroke(north, style: StrokeStyle(lineWidth: 2.4, lineCap: .round))
                    // سهم القبلة والكعبة في طرفه
                    ZStack {
                        Capsule().fill(p.gold).frame(width: 4, height: r * 0.62).offset(y: -r * 0.31)
                        Rectangle().fill(Color(white: 0.11)).frame(width: k, height: k)
                            .overlay(Rectangle().fill(p.gold).frame(height: k * 0.22), alignment: .top)
                            .overlay(Rectangle().stroke(p.gold, lineWidth: 1))
                            .offset(y: -(r * 0.62 + k))
                    }
                    .rotationEffect(.degrees(bearing))
                }
                .frame(width: side, height: side)
                .rotationEffect(.degrees(-dial))
                .animation(UIAccessibility.isReduceMotionEnabled ? nil : .easeOut(duration: 0.25), value: dial)
                // علامة أعلى الهاتف
                QiblaMarker().fill(p.primary).frame(width: 24, height: 18).offset(y: -r + 7)
                Circle().fill(p.primary).frame(width: r * 0.12, height: r * 0.12)
                Circle().fill(p.surface).frame(width: r * 0.06, height: r * 0.06)
            }
            .frame(width: side, height: side)
            .position(x: g.size.width / 2, y: g.size.height / 2)
        }
        .aspectRatio(1, contentMode: .fit)
        .environment(\.layoutDirection, .leftToRight)
    }
}

/// شرطات القرص كل 10°، والطويلة كل 30°، وشرطة الشمال وحدها
private struct QiblaTicks: Shape {
    enum Kind { case short, long, north }
    let kind: Kind
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let c = CGPoint(x: rect.midX, y: rect.midY), r = min(rect.width, rect.height) / 2
        for a in stride(from: 0, to: 360, by: 10) {
            let isLong = a % 30 == 0
            switch kind {
            case .north: if a != 0 { continue }
            case .long: if !isLong || a == 0 { continue }
            case .short: if isLong { continue }
            }
            let rad = Double(a) * .pi / 180
            let r1 = r * 0.86, r2 = isLong ? r * 0.76 : r * 0.81
            path.move(to: CGPoint(x: c.x + r1 * CGFloat(sin(rad)), y: c.y - r1 * CGFloat(cos(rad))))
            path.addLine(to: CGPoint(x: c.x + r2 * CGFloat(sin(rad)), y: c.y - r2 * CGFloat(cos(rad))))
        }
        return path
    }
}

private struct QiblaMarker: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.midX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        path.closeSubpath()
        return path
    }
}

/// النبض الذهبي حول المركز (ثابت باهت مع «تقليل الحركة»)
private struct QiblaWave: View {
    let color: Color
    let size: CGFloat
    @State private var on = false
    var body: some View {
        let still = UIAccessibility.isReduceMotionEnabled
        Circle().fill(color.opacity(0.28))
            .frame(width: size, height: size)
            .scaleEffect(still ? 1 : (on ? 1 : 0.02))
            .opacity(still ? 0.45 : (on ? 0 : 1))
            .onAppear {
                guard !still else { return }
                withAnimation(.easeOut(duration: 1.6).repeatForever(autoreverses: false)) { on = true }
            }
    }
}

private struct QiblaStatsCard: View {
    let s: QiblaState
    let p: HomePalette
    @ObservedObject var heading: QiblaHeading
    var body: some View {
        QiblaTasbihCard(p: p) {
            QiblaStatRow(label: s.locationLabel, value: s.location, p: p)
            QiblaStatRow(label: s.distanceLabel, value: s.distance, p: p)
            if heading.available {
                QiblaStatRow(label: s.headingLabel, value: "\(Int(heading.heading))°", p: p)
            }
        }
    }
}

private struct QiblaStatRow: View {
    let label: String
    let value: String
    let p: HomePalette
    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 10) {
            Text(label).font(sys(14)).foregroundColor(p.onSurfaceVariant)
            Spacer(minLength: 8)
            Text(value).font(sys(15, .bold)).foregroundColor(p.onSurface).multilineTextAlignment(.trailing)
        }
        .accessibilityElement(children: .combine)
    }
}

/// نصيحة الاستعمال، أو تنبيه الدقة المنخفضة
private struct QiblaNote: View {
    let s: QiblaState
    let p: HomePalette
    @ObservedObject var heading: QiblaHeading
    var body: some View {
        let warn = heading.available && heading.low
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: warn ? "exclamationmark.triangle.fill" : "info.circle.fill")
                .font(.system(size: 17)).foregroundColor(warn ? HomePalette.color(s.x.warn) : p.primary)
            Text(warn ? s.lowAccuracy : s.tip).font(sys(14))
                .foregroundColor(warn ? HomePalette.color(s.x.warnFg) : p.onPrimaryContainer)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(warn ? HomePalette.color(s.x.warnBg) : p.primaryContainer))
        .animation(.easeOut(duration: 0.25), value: warn)
    }
}

/// صف يُلمس: أيقونة وعنوان ووصف وسهم (ClickRow)
private struct QiblaLinkRow: View {
    let title: String
    let sub: String
    let symbol: String
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: symbol).font(.system(size: 18, weight: .semibold)).foregroundColor(p.primary)
                    .frame(width: 42, height: 42)
                    .background(RoundedRectangle(cornerRadius: 13, style: .continuous).fill(p.gold.opacity(0.16)))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(sys(16, .bold)).foregroundColor(p.onSurface)
                    Text(sub).font(sys(12)).foregroundColor(p.onSurfaceVariant).lineLimit(2)
                }
                Spacer(minLength: 6)
                Image(systemName: "chevron.forward").font(.system(size: 14, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(PressScale(scale: 0.98))
    }
}

/// خطوات معايرة البوصلة مرقّمة (حوار Compose)
private struct QiblaCalibSheet: View {
    let c: QiblaState.Calib
    let p: HomePalette
    let onDone: () -> Void
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack(spacing: 12) {
                        Image(systemName: "safari").font(.system(size: 20, weight: .semibold)).foregroundColor(p.primary)
                            .frame(width: 44, height: 44)
                            .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.gold.opacity(0.16)))
                        Text(c.title).font(sys(22, .bold)).foregroundColor(p.onSurface)
                            .accessibilityAddTraits(.isHeader)
                    }
                    ForEach(Array(c.steps.enumerated()), id: \.offset) { i, step in
                        HStack(alignment: .top, spacing: 12) {
                            Text(verbatim: String(i + 1)).font(sys(14, .heavy)).foregroundColor(p.primary)
                                .frame(width: 28, height: 28).background(Circle().fill(p.primaryContainer))
                            Text(step).font(sys(15)).foregroundColor(p.onSurface)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .accessibilityElement(children: .combine)
                    }
                }
                .padding(22)
            }
            Button(action: onDone) {
                Text(c.done).font(sys(16, .semibold)).foregroundColor(.white)
                    .frame(maxWidth: .infinity).padding(.vertical, 14)
                    .background(Capsule().fill(LinearGradient(colors: [p.hero1, p.hero2], startPoint: .leading, endPoint: .trailing)))
            }
            .buttonStyle(PressScale())
            .padding(.horizontal, 22).padding(.bottom, 18)
        }
        .background(p.surface.ignoresSafeArea())
        .modifier(QiblaHalfSheet())
    }
}

/// نصف الشاشة في iOS 16 فما بعد (وقبله ورقة كاملة)
private struct QiblaHalfSheet: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content.presentationDetents([.medium, .large])
        } else {
            content
        }
    }
}

// MARK: - مشترك بين القبلة والمسبحة

/// شفافية الشريط العلوي: 0 والصفحة في أعلاها، و1 حين يمر طرف الترويسة السفلي تحته (GlassTopBar)
func qiblaTasbihGlass(top: CGFloat, headerBottom: CGFloat) -> Double {
    let bar = top + 52, ramp: CGFloat = 36
    return Double(min(max((bar + ramp - headerBottom) / ramp, 0), 1))
}

/// الشريط العلوي الثابت للصفحة المفتوحة: زر الرجوع دائماً، وزجاج وعنوان صغير حين تمر الترويسة تحته
struct QiblaTasbihTopBar: View {
    let title: String
    let back: String
    let canBack: Bool
    let top: CGFloat
    let lead: CGFloat
    let p: HomePalette
    let amount: Double
    let onBack: () -> Void

    var body: some View {
        let t = min(max((amount - 0.4) / 0.6, 0), 1)
        ZStack {
            Text(title)
                .font(sys(16, .medium)).foregroundColor(p.onSurface).lineLimit(1)
                .padding(.horizontal, 60)
                .opacity(t)
                .offset(y: CGFloat(1 - t) * 6)
                .accessibilityHidden(true)
            if canBack {
                HStack(spacing: 0) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.backward")
                            .font(.system(size: 18, weight: .semibold)).foregroundColor(p.primary)
                            .frame(width: 44, height: 44)
                            .background(
                                ZStack {
                                    Circle().fill(p.surface.opacity(0.85 * (1 - amount)))
                                        .shadow(color: .black.opacity(0.08 * (1 - amount)), radius: 3, y: 1)
                                    Circle().fill(p.onSurface.opacity(0.06 * amount))
                                }
                            )
                    }
                    .buttonStyle(PressScale(scale: 0.88))
                    .accessibilityLabel(back)
                    Spacer(minLength: 0)
                }
                .padding(.leading, 10)
            }
        }
        .frame(height: 52)
        .padding(.leading, lead)
        .padding(.top, top)
        .frame(maxWidth: .infinity)
        .background(
            Rectangle().fill(.ultraThinMaterial)
                .overlay(Rectangle().fill(p.outlineVariant.opacity(0.8)).frame(height: 1), alignment: .bottom)
                .opacity(amount)
                .allowsHitTesting(false)
        )
    }
}

/// ترويسة الصفحة بطراز ترويسة الرئيسية تحت الشريط العلوي: تدرج من لون التطبيق الفاتح، ونجمة باهتة، وأيقونة الأداة والعنوان
struct QiblaTasbihHeader: View {
    let title: String
    let subtitle: String
    let symbol: String
    let top: CGFloat
    let start: CGFloat
    let end: CGFloat
    let rtl: Bool
    let p: HomePalette

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 20, weight: .semibold)).foregroundColor(p.primary)
                .frame(width: 46, height: 46)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.surface.opacity(0.85)).shadow(color: .black.opacity(0.08), radius: 3, y: 1))
            VStack(alignment: .leading, spacing: 1) {
                Text(title).font(sys(28, .heavy)).foregroundColor(p.primary).lineLimit(2).minimumScaleFactor(0.8)
                Text(subtitle).font(sys(14)).foregroundColor(p.onSurfaceVariant).lineLimit(2)
            }
            Spacer(minLength: 0)
        }
        .padding(.leading, start + 4)
        .padding(.trailing, end + 4)
        .padding(.top, top + 56)
        .padding(.bottom, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            ZStack(alignment: rtl ? .topTrailing : .topLeading) {
                GeometryReader { g in
                    VStack(spacing: 0) {
                        p.primaryContainer.frame(height: 800)
                        LinearGradient(colors: [p.primaryContainer, p.primaryContainer.opacity(0.55), p.background], startPoint: .top, endPoint: .bottom)
                            .frame(height: g.size.height)
                    }
                    .offset(y: -800)
                }
                StarEmblem(color: p.primary.opacity(0.07), radius: 150, stroke: 1.7)
                    .offset(x: rtl ? 220 : -220, y: -190)
            }
            .environment(\.layoutDirection, .leftToRight)
            .allowsHitTesting(false)
        )
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }
}

/// بطاقة بيضاء بحافة رفيعة كبطاقات الرئيسية
struct QiblaTasbihCard<Content: View>: View {
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

/// «الدليل وأقوال الفقهاء» (EvidenceCard): يُفتح باللمس، ولكل نقطة نوعها وفكرتها والآية بخط المصحف أو الحديث والشرح والمصدر
struct QiblaTasbihProofsCard: View {
    let e: QiblaTasbihProofs
    let p: HomePalette
    @State private var open = false

    /// صريح: الحالة الخاصة تجعل المُنشئ التلقائي خاصاً بالملف، والمسبحة تستعمله
    init(e: QiblaTasbihProofs, p: HomePalette) {
        self.e = e
        self.p = p
    }

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

    private func conceptSymbol(_ c: String) -> String {
        switch c {
        case "qibla", "compass": return "safari"
        case "dhikr", "fingers": return "hand.tap"
        case "time": return "clock"
        case "sun": return "sun.max"
        case "moon": return "moon"
        case "star": return "star"
        case "quran": return "book"
        case "people": return "person.3"
        case "scholars": return "graduationcap"
        default: return "lightbulb"
        }
    }

    private var kindsShown: [String] {
        var seen: [String] = []
        for pt in e.points where !seen.contains(pt.kind) { seen.append(pt.kind) }
        return seen
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
                            ForEach(kindsShown, id: \.self) { k in chip(k) }
                        }
                    }
                    Spacer(minLength: 6)
                    Image(systemName: "chevron.down").font(.system(size: 14, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
                        .rotationEffect(.degrees(open ? 180 : 0))
                }
                .padding(16)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(.isHeader)
            if open {
                VStack(alignment: .leading, spacing: 14) {
                    ForEach(e.points) { pt in
                        if pt.id != e.points.first?.id { Rectangle().fill(p.outlineVariant.opacity(0.6)).frame(height: 1) }
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: conceptSymbol(pt.concept)).font(.system(size: 16)).foregroundColor(color(pt.kind))
                                .frame(width: 38, height: 38).background(Circle().fill(color(pt.kind).opacity(0.13)))
                                .accessibilityHidden(true)
                            VStack(alignment: .leading, spacing: 6) {
                                chip(pt.kind)
                                Text(pt.title).font(sys(14, .bold)).foregroundColor(p.onSurface)
                                if let a = pt.ayah {
                                    Text(a).font(quranFont(19)).foregroundColor(p.onSurface).lineSpacing(8)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .padding(10)
                                        .background(quote)
                                        .environment(\.layoutDirection, .rightToLeft)
                                }
                                if let t = pt.tr { Text(t).font(sys(12)).foregroundColor(p.onSurfaceVariant) }
                                if let h = pt.hadith {
                                    Text(h).font(sys(14)).foregroundColor(p.onSurface).lineSpacing(6)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .padding(10)
                                        .background(quote)
                                        .environment(\.layoutDirection, .rightToLeft)
                                }
                                Text(pt.body).font(sys(14)).foregroundColor(p.onSurface)
                                Label(pt.source, systemImage: symbol(pt.kind)).font(sys(12, .bold)).foregroundColor(color(pt.kind))
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

    /// الاقتباس بخط جانبي ذهبي (Quote)
    private var quote: some View {
        ZStack(alignment: .leading) {
            RoundedRectangle(cornerRadius: 12, style: .continuous).fill(p.gold.opacity(0.08))
            Rectangle().fill(p.gold).frame(width: 3)
        }
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func chip(_ kind: String) -> some View {
        Label(e.kinds[kind] ?? kind, systemImage: symbol(kind))
            .font(sys(11, .bold)).foregroundColor(color(kind))
            .padding(.horizontal, 8).padding(.vertical, 3)
            .background(Capsule().fill(color(kind).opacity(0.12)))
    }
}
