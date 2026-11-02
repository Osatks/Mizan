import SwiftUI
import WatchKit
import CoreLocation

// اتجاه القبلة في الساعة: قرص بوصلة يدور مع اتجاه رأس الساعة (الثانية عشرة) فيبقى شماله نحو الشمال، والكعبة على حلقته
// في زاوية القبلة من الشمال الجغرافي من موقع المستخدم في التطبيق (لقطة iPhone). المؤشر الثابت أعلى القرص هو وجهتك:
// حين تصير الكعبة تحته (±5°) يتوهج ذهبياً وتهتز الساعة مرة. الموقع يُطلب لتصحيح الشمال المغناطيسي إلى الجغرافي فقط،
// ولا يُرسل إلى أي جهة. بلا بوصلة (المحاكي، وساعات بلا بوصلة) يثبت القرص والشمال أعلاه، والزاوية مكتوبة.

/// بوصلة الساعة: اتجاه رأسها من الشمال الجغرافي إن عُرف الموقع، وإلا من المغناطيسي
final class Compass: NSObject, ObservableObject, CLLocationManagerDelegate {
    /// بالدرجات مع عقارب الساعة من الشمال، و nil قبل أول قراءة
    @Published private(set) var heading: Double?
    /// دقة القراءة بالدرجات (سالبة: غير صالحة)
    @Published private(set) var accuracy: Double = -1
    let available = CLLocationManager.headingAvailable()
    private let manager = CLLocationManager()
    private var running = false

    override init() {
        super.init()
        manager.delegate = self
        manager.headingFilter = 1
        manager.desiredAccuracy = kCLLocationAccuracyKilometer
    }

    func start() {
        guard available, !running else { return }
        running = true
        switch manager.authorizationStatus {
        case .notDetermined: manager.requestWhenInUseAuthorization()
        case .authorizedWhenInUse, .authorizedAlways: manager.startUpdatingLocation()
        default: break
        }
        manager.startUpdatingHeading()
    }

    func stop() {
        guard running else { return }
        running = false
        manager.stopUpdatingHeading()
        manager.stopUpdatingLocation()
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let s = manager.authorizationStatus
        if running && (s == .authorizedWhenInUse || s == .authorizedAlways) { manager.startUpdatingLocation() }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateHeading h: CLHeading) {
        // الجغرافي يصح مع تحديث الموقع فقط؛ قبله (أو بلا إذن) المغناطيسي
        let v = h.trueHeading >= 0 ? h.trueHeading : h.magneticHeading
        guard v >= 0 else { return }
        heading = v
        accuracy = h.headingAccuracy
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {}
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {}
}

struct QiblaPage: View {
    @ObservedObject var model: WatchModel
    /// الصفحة ظاهرة: البوصلة تعمل الآن فقط
    let active: Bool

    @StateObject private var compass = Compass()
    @Environment(\.scenePhase) private var scenePhase
    /// دوران القرص متصلاً (بلا قفزة من 359° إلى 0°)
    @State private var rotation: Double = 0
    @State private var aligned = false

    var body: some View {
        Group {
            if let snap = model.payload?.snapshot, let w = snap.watch {
                content(snap, w)
            } else {
                NoData()
            }
        }
        .containerBackground(WatchColors.background(model.payload?.snapshot), for: .tabView)
        .navigationTitle(model.payload?.snapshot.watch?.qiblaTitle ?? (model.rtl ? "القبلة" : "Qibla"))
        .onAppear { update() }
        .onDisappear { compass.stop() }
        .onChange(of: active) { _, _ in update() }
        .onChange(of: scenePhase) { _, _ in update() }
        .onChange(of: compass.heading) { _, h in turn(to: h) }
    }

    private func update() {
        if active && scenePhase == .active { compass.start() } else { compass.stop() }
    }

    /// يدير القرص بأقصر طريق إلى القراءة الجديدة، ويهتز مرة عند بلوغ القبلة
    private func turn(to heading: Double?) {
        guard let heading = heading else { return }
        var d = (-heading - rotation).truncatingRemainder(dividingBy: 360)
        if d > 180 { d -= 360 }
        if d < -180 { d += 360 }
        withAnimation(.easeOut(duration: 0.25)) { rotation += d }
        guard let q = model.payload?.snapshot.watch?.qibla else { return }
        let off = abs(Self.relative(q, heading))
        if !aligned && off <= 5 {
            aligned = true
            WKInterfaceDevice.current().play(.success)
        } else if aligned && off > 10 {
            aligned = false
        }
    }

    /// زاوية القبلة من وجهتك: موجبة يميناً وسالبة يساراً (-180 إلى 180)
    static func relative(_ qibla: Double, _ heading: Double) -> Double {
        var r = (qibla - heading).truncatingRemainder(dividingBy: 360)
        if r > 180 { r -= 360 }
        if r < -180 { r += 360 }
        return r
    }

    @ViewBuilder
    private func content(_ snap: Snapshot, _ w: WatchExtras) -> some View {
        let gold = WatchColors.gold
        let live = compass.available ? compass.heading : nil
        let rel = live.map { Self.relative(w.qibla, $0) }
        let onTarget = rel.map { abs($0) <= 5 } ?? false
        GeometryReader { g in
            // تحت القرص سطران مع البوصلة، وسطر بلاها
            let size = max(60, min(g.size.width, g.size.height - (live == nil ? 24 : 42)))
            VStack(spacing: 4) {
                ZStack {
                    // القرص: يدور مع الساعة (ثابت بلا بوصلة)
                    Dial(qibla: w.qibla, turn: live == nil ? 0 : rotation, glow: onTarget)
                    // وجهتك: المؤشر الثابت أعلى القرص
                    NorthMark()
                        .fill(onTarget ? gold : Color.white)
                        .frame(width: 12, height: 9)
                        .rotationEffect(.degrees(180))
                        .offset(y: -size / 2 - 3)
                    center(w, rel: rel, onTarget: onTarget)
                }
                .frame(width: size, height: size)
                // زوايا الدوران مع عقارب الساعة في كل اللغات (rotationEffect تُعكس من اليمين إلى اليسار)
                .environment(\.layoutDirection, .leftToRight)
                // مع البوصلة: الزاوية والجهة تحت القرص (وسطه لما تدور به)، أو «أنت متجه نحو القبلة»
                if live != nil {
                    Text(verbatim: onTarget ? w.facing : "\(w.qiblaText) · \(w.qiblaDir)")
                        .font(.system(size: 13, weight: onTarget ? .semibold : .medium, design: .rounded))
                        .foregroundStyle(onTarget ? gold : .white.opacity(0.85))
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                }
                HStack(spacing: 4) {
                    Image(systemName: "location.fill").font(.system(size: 8, weight: .semibold))
                    Text(verbatim: "\(snap.place) · \(w.qiblaKm)")
                }
                .font(.system(size: 11, weight: .medium, design: .rounded))
                .foregroundStyle(.white.opacity(0.6))
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            }
            .frame(width: g.size.width, height: g.size.height)
        }
        .animation(.easeOut(duration: 0.2), value: onTarget)
    }

    /// وسط القرص: كم تدور ونحو أي جهة، أو علامة الوصول، أو الزاوية بلا بوصلة
    @ViewBuilder
    private func center(_ w: WatchExtras, rel: Double?, onTarget: Bool) -> some View {
        if let rel = rel {
            if onTarget {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 30, weight: .semibold))
                    .foregroundStyle(WatchColors.gold)
            } else {
                VStack(spacing: 0) {
                    Image(systemName: rel > 0 ? "arrow.turn.up.right" : "arrow.turn.up.left")
                        .font(.system(size: 18, weight: .semibold))
                    Text(verbatim: "\(Int(abs(rel).rounded()))°")
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                        .monospacedDigit()
                }
                .foregroundStyle(.white)
            }
        } else {
            // بلا بوصلة: الزاوية من الشمال وجهتها
            VStack(spacing: 0) {
                Text(verbatim: w.qiblaText)
                    .font(.system(size: 26, weight: .bold, design: .rounded))
                    .monospacedDigit()
                Text(verbatim: w.qiblaDir)
                    .font(.system(size: 12, weight: .medium, design: .rounded))
                    .foregroundStyle(.white.opacity(0.7))
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                    .frame(maxWidth: 80)
            }
            .foregroundStyle(.white)
        }
    }
}

/// قرص البوصلة مُداراً بـ [turn]: علامات كل ٥°، والشمال مثلث أحمر، والكعبة على الحلقة في زاوية القبلة
/// (قائمة على الشاشة لا تميل مع القرص)
private struct Dial: View {
    let qibla: Double
    let turn: Double
    let glow: Bool

    var body: some View {
        GeometryReader { g in
            let r = min(g.size.width, g.size.height) / 2
            let gold = WatchColors.gold
            ZStack {
                // وجه القرص: عمق خفيف وحافة ذهبية
                Circle().fill(RadialGradient(colors: [Color.white.opacity(0.07), Color.black.opacity(0.4)],
                                             center: .center, startRadius: 0, endRadius: r))
                Circle()
                    .strokeBorder(LinearGradient(colors: [gold.opacity(glow ? 0.95 : 0.55), gold.opacity(0.08)],
                                                 startPoint: .top, endPoint: .bottom), lineWidth: 1.5)
                ForEach(0..<72, id: \.self) { i in
                    let major = i % 6 == 0
                    Capsule()
                        .fill(Color.white.opacity(major ? 0.75 : 0.3))
                        .frame(width: major ? 2 : 1, height: major ? r * 0.12 : r * 0.07)
                        .offset(y: -r + (major ? r * 0.06 : r * 0.035) + 3)
                        .rotationEffect(.degrees(Double(i) * 5))
                }
                NorthMark()
                    .fill(Color(red: 0.93, green: 0.33, blue: 0.3))
                    .frame(width: r * 0.16, height: r * 0.14)
                    .offset(y: -r * 0.74)
                // خط نحو الكعبة (بعيداً عن نص الوسط)
                Capsule()
                    .fill(gold.opacity(glow ? 0.95 : 0.5))
                    .frame(width: 2, height: r * 0.16)
                    .offset(y: -r * 0.38)
                    .rotationEffect(.degrees(qibla))
                ZStack {
                    Circle()
                        .fill(Color(red: 0.06, green: 0.16, blue: 0.13))
                    Circle()
                        .fill(gold.opacity(glow ? 0.38 : 0.16))
                    Circle()
                        .strokeBorder(gold.opacity(glow ? 1 : 0.7), lineWidth: 1.5)
                    Image("mizan.kaaba")
                        .font(.system(size: r * 0.25, weight: .semibold))
                        .foregroundStyle(gold)
                }
                .frame(width: r * 0.44, height: r * 0.44)
                .shadow(color: gold.opacity(glow ? 0.8 : 0), radius: glow ? 8 : 0)
                // قائمة: تدور إلى موضعها مع القرص، وتُعاد قبل ذلك بقدر الدورانين
                .rotationEffect(.degrees(-qibla - turn))
                .offset(y: -r * 0.66)
                .rotationEffect(.degrees(qibla))
            }
            .frame(width: g.size.width, height: g.size.height)
            .rotationEffect(.degrees(turn))
        }
    }
}
