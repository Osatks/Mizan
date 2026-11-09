import SwiftUI
import UIKit
import Shared

/// الأذان والتنبيهات بواجهة النظام (SwiftUI) بطراز الرئيسية (NativeHome.swift): ترويسة بتدرج لون التطبيق ونجمته تحت شريط
/// مثبّت فيه زر الرجوع، ثم مجموعات بطاقات: مفاتيح التنبيه والعدّ الحي والتقويم، وطريقة التنبيه لكل صلاة (قائمة النظام)،
/// والمؤذن وأذان الفجر مع الاستماع والتنزيل بتقدمه والحذف، وأنماط الاهتزاز برسمها وتجربتها، ومصادر التسجيلات — كما في AdhanScreen.kt.
/// الحالة كلها من Kotlin (NativeAdhan.ios.kt) مع الإشعار MizanAdhanState، وتتجدد مع تقدم التنزيل والمعاينة؛
/// الحفظ والتنزيل والتشغيل والاهتزاز هناك، وهنا الرسم فقط واللمس يعود إليه.
enum NativeAdhanScreen {
    static func install() {
        NativeAdhan.shared.attach { session in AdhanSetHostController(session: session) }
    }
}

final class AdhanSetHostController: UIHostingController<AdhanSetRoot> {
    init(session: NativeAdhanSession) {
        let model = AdhanSetModel(session: session)
        super.init(rootView: AdhanSetRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        // الهوامش من Kotlin لا من المنطقة الآمنة لهذا المتحكم
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct AdhanSetState: Decodable {
    struct Row: Decodable { let title, sub: String }
    struct Switch: Decodable { let title, sub: String; let on: Bool; let busy: Bool? }
    struct Mode: Decodable, Identifiable { let key, name: String; var id: String { key } }
    struct Prayer: Decodable, Identifiable { let key, name, mode, detail: String; var id: String { key } }
    struct Muezzin: Decodable, Identifiable {
        let id, name, sub: String
        let selected, available, downloading, failed, canDelete, playing: Bool
        let progress: Double
    }
    struct Same: Decodable { let title, sub: String; let on: Bool }
    /// نمط اهتزاز؛ pulses: لكل مقطع [البداية، العرض، الارتفاع] نسباً من 0 إلى 1
    struct Vibe: Decodable, Identifiable {
        let key, name: String
        let selected: Bool
        let pulses: [[Double]]
        var id: String { key }
    }

    let rtl, dark: Bool
    let top, bottom, start, end, width: Double
    let back, title, subtitle: String
    let alert: Switch
    let exactAlarm: Row?
    let live, calendar: Switch?
    let perPrayerTitle, modesHint: String
    let modes: [Mode]
    let prayers: [Prayer]
    let muezzinTitle: String
    let muezzins: [Muezzin]
    let fajrTitle, fajrHint: String
    let fajrMuezzins: [Muezzin]
    let fajrSame: Same
    let vibrationTitle: String
    let vibes: [Vibe]
    let note, sourcesTitle: String
    let sources: [String]
    let download, delete, play, stop, tryVibration: String
    let error: String
    let c: HomeState.Colors

    static func decode(_ json: String) -> AdhanSetState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(AdhanSetState.self, from: data)
    }
}

final class AdhanSetModel: ObservableObject {
    let session: NativeAdhanSession
    @Published private(set) var state: AdhanSetState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativeAdhanSession) {
        self.session = session
        state = AdhanSetState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanAdhanState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = AdhanSetState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    func notify(_ on: Bool) { session.notify(on: on) }
    func live(_ on: Bool) { session.live(on: on) }
    func calendar(_ on: Bool) { session.calendar(on: on) }
    func exactAlarm() { session.exactAlarm() }
    func alertMode(_ prayer: String, _ mode: String) { session.alertMode(prayer: prayer, mode: mode) }
    func pick(_ id: String, fajr: Bool) { if fajr { session.pickFajr(id: id) } else { session.pick(id: id) } }
    func download(_ id: String) { session.download(id: id) }
    func remove(_ id: String) { session.remove(id: id) }
    func play(_ id: String) { session.play(id: id) }
    func vibe(_ key: String) { session.vibe(key: key) }
    func tryVibe(_ key: String) { session.tryVibe(key: key) }
    func back() { session.back() }
}

/// رمز كل طريقة تنبيه (AlertMode.icon في AdhanScreen.kt)
private func adhanModeSymbol(_ key: String) -> String {
    switch key {
    case "adhan": return "speaker.wave.2.fill"
    case "vibrate": return "iphone.radiowaves.left.and.right"
    case "silent": return "bell"
    default: return "bell.slash"
    }
}

// MARK: - الشاشة

struct AdhanSetRoot: View {
    @ObservedObject var model: AdhanSetModel
    var body: some View {
        Group {
            if let s = model.state { AdhanSetView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

private struct AdhanSetHeaderBottomKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

struct AdhanSetView: View {
    let s: AdhanSetState
    let p: HomePalette
    let model: AdhanSetModel
    @State private var headerBottom: CGFloat = .greatestFiniteMagnitude

    /// زجاج الشريط العلوي وعنوانه يظهران حين تمر الترويسة تحته
    private var glass: Double {
        let bar = CGFloat(s.top) + 52, ramp: CGFloat = 36
        return Double(min(max((bar + ramp - headerBottom) / ramp, 0), 1))
    }

    var body: some View {
        ZStack(alignment: .top) {
            p.background
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    AdhanSetHeader(s: s, p: p)
                        .background(GeometryReader { g in
                            Color.clear.preference(key: AdhanSetHeaderBottomKey.self, value: g.frame(in: .named("adhanSet")).maxY)
                        })
                    content
                        .padding(.leading, CGFloat(s.start))
                        .padding(.trailing, CGFloat(s.end))
                }
                .padding(.bottom, CGFloat(s.bottom))
            }
            .coordinateSpace(name: "adhanSet")
            .onPreferenceChange(AdhanSetHeaderBottomKey.self) { headerBottom = $0 }
            AdhanSetBar(s: s, p: p, amount: glass, onBack: model.back)
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 20) {
            // التنبيه، وإعداد المنبّه الدقيق (أندرويد)، والعدّ الحي، والتقويم
            AdhanSetGroup(title: nil, p: p) {
                AdhanSetToggleRow(symbol: "bell.badge.fill", row: s.alert, p: p) { model.notify($0) }
                if let ex = s.exactAlarm {
                    AdhanSetDivider(p: p)
                    AdhanSetNavRow(symbol: "alarm.fill", row: ex, p: p) { model.exactAlarm() }
                }
                if let l = s.live {
                    AdhanSetDivider(p: p)
                    AdhanSetToggleRow(symbol: "timer", row: l, p: p) { model.live($0) }
                }
                if let cal = s.calendar {
                    AdhanSetDivider(p: p)
                    AdhanSetToggleRow(symbol: "calendar", row: cal, p: p) { model.calendar($0) }
                }
            }
            AdhanSetGroup(title: s.perPrayerTitle, p: p) {
                ForEach(Array(s.prayers.enumerated()), id: \.element.id) { i, pr in
                    if i > 0 { AdhanSetDivider(p: p) }
                    AdhanSetPrayerRow(pr: pr, modes: s.modes, p: p, model: model)
                }
                AdhanSetFootnote(text: s.modesHint, p: p)
            }
            AdhanSetGroup(title: s.muezzinTitle, p: p) {
                ForEach(Array(s.muezzins.enumerated()), id: \.element.id) { i, m in
                    if i > 0 { AdhanSetDivider(p: p, inset: 52) }
                    AdhanSetMuezzinRow(m: m, fajr: false, s: s, p: p, model: model)
                }
            }
            AdhanSetGroup(title: s.fajrTitle, p: p) {
                AdhanSetFootnote(text: s.fajrHint, p: p, top: true)
                ForEach(s.fajrMuezzins) { m in
                    AdhanSetMuezzinRow(m: m, fajr: true, s: s, p: p, model: model)
                    AdhanSetDivider(p: p, inset: 52)
                }
                AdhanSetSameRow(same: s.fajrSame, p: p) { model.pick("same", fajr: true) }
            }
            AdhanSetGroup(title: s.vibrationTitle, p: p) {
                AdhanSetVibeGrid(s: s, p: p, model: model)
            }
            AdhanSetNote(text: s.note, p: p)
            AdhanSetGroup(title: s.sourcesTitle, p: p) {
                VStack(alignment: .leading, spacing: 6) {
                    ForEach(Array(s.sources.enumerated()), id: \.offset) { _, line in
                        Text(line).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

// MARK: الشريط والترويسة

/// الشريط العلوي المثبّت: زر الرجوع دائماً، وزجاج بالعنوان عند التمرير
private struct AdhanSetBar: View {
    let s: AdhanSetState
    let p: HomePalette
    let amount: Double
    let onBack: () -> Void
    var body: some View {
        ZStack {
            Text(s.title)
                .font(sys(16, .medium)).foregroundColor(p.onSurface).lineLimit(1)
                .padding(.horizontal, 64)
                .opacity(amount)
                .allowsHitTesting(false)
                .accessibilityHidden(true)
            HStack {
                Button(action: onBack) {
                    Image(systemName: "chevron.backward")
                        .font(.system(size: 17, weight: .semibold)).foregroundColor(p.primary)
                        .frame(width: 42, height: 42)
                        .background(Circle().fill(p.surface.opacity(0.85)).shadow(color: .black.opacity(0.08), radius: 3, y: 1))
                }
                .buttonStyle(PressScale(scale: 0.9))
                .accessibilityLabel(s.back)
                Spacer()
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

/// ترويسة بطراز ترويسة الرئيسية: تدرج من لون التطبيق الفاتح، ونجمة باهتة، وأيقونة الأذان والعنوان والوصف
private struct AdhanSetHeader: View {
    let s: AdhanSetState
    let p: HomePalette
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "speaker.wave.2.fill")
                .font(.system(size: 20, weight: .semibold)).foregroundColor(p.primary)
                .frame(width: 46, height: 46)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.gold.opacity(0.18)))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 1) {
                Text(s.title).font(sys(26, .heavy)).foregroundColor(p.primary).lineLimit(2).minimumScaleFactor(0.8)
                Text(s.subtitle).font(sys(14)).foregroundColor(p.onSurfaceVariant).lineLimit(3)
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

// MARK: المجموعات والصفوف

/// مجموعة بأسلوب iOS: عنوان صغير فوق بطاقة بيضاء مستديرة صفوفها مفصولة بخطوط رفيعة (كمجموعات الإعدادات)
private struct AdhanSetGroup<Content: View>: View {
    let title: String?
    let p: HomePalette
    let content: Content

    init(title: String?, p: HomePalette, @ViewBuilder content: () -> Content) {
        self.title = title
        self.p = p
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let t = title {
                Text(t).font(sys(13, .semibold)).foregroundColor(p.onSurfaceVariant)
                    .padding(.horizontal, 12)
                    .accessibilityAddTraits(.isHeader)
            }
            VStack(spacing: 0) { content }
                .frame(maxWidth: .infinity)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(p.surface).shadow(color: .black.opacity(0.04), radius: 3, y: 1))
                .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.8), lineWidth: 1))
        }
    }
}

/// أيقونة الصف على مربع ذهبي فاتح
private struct AdhanSetTile: View {
    let symbol: String
    let p: HomePalette
    var body: some View {
        Image(systemName: symbol)
            .font(.system(size: 15, weight: .semibold)).foregroundColor(p.primary)
            .frame(width: 34, height: 34)
            .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(p.gold.opacity(0.16)))
            .accessibilityHidden(true)
    }
}

private struct AdhanSetRowText: View {
    let title: String
    let sub: String?
    let p: HomePalette
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title).font(sys(16, .semibold)).foregroundColor(p.onSurface)
                .fixedSize(horizontal: false, vertical: true)
            if let sub = sub, !sub.isEmpty {
                Text(sub).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

/// يظلل الصف تحت الإصبع كصفوف إعدادات النظام
private struct AdhanSetRowPress: ButtonStyle {
    let p: HomePalette
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(p.onSurface.opacity(configuration.isPressed ? 0.07 : 0))
    }
}

/// صف بمفتاح تشغيل (يرسل التغيير إلى Kotlin، والقيمة المعروضة منه)؛ وأثناء العمل (التقويم) مؤشر انتظار مكانه
private struct AdhanSetToggleRow: View {
    let symbol: String
    let row: AdhanSetState.Switch
    let p: HomePalette
    let set: (Bool) -> Void
    var body: some View {
        Group {
            if row.busy == true {
                HStack(spacing: 12) {
                    AdhanSetTile(symbol: symbol, p: p)
                    AdhanSetRowText(title: row.title, sub: row.sub, p: p)
                    Spacer(minLength: 8)
                    ProgressView().tint(p.primary)
                }
                .accessibilityElement(children: .combine)
            } else {
                Toggle(isOn: Binding(get: { row.on }, set: { set($0) })) {
                    HStack(spacing: 12) {
                        AdhanSetTile(symbol: symbol, p: p)
                        AdhanSetRowText(title: row.title, sub: row.sub, p: p)
                    }
                }
                .tint(p.primary)
            }
        }
        .padding(.horizontal, 16).padding(.vertical, 12)
        .frame(minHeight: 56)
    }
}

/// صف يفتح إعدادات النظام (المنبّه الدقيق)
private struct AdhanSetNavRow: View {
    let symbol: String
    let row: AdhanSetState.Row
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                AdhanSetTile(symbol: symbol, p: p)
                AdhanSetRowText(title: row.title, sub: row.sub, p: p)
                Spacer(minLength: 8)
                Image(systemName: "arrow.up.forward.app")
                    .font(.system(size: 14, weight: .semibold)).foregroundColor(p.onSurfaceVariant.opacity(0.7))
                    .accessibilityHidden(true)
            }
            .padding(.horizontal, 16).padding(.vertical, 12)
            .frame(minHeight: 56)
            .contentShape(Rectangle())
        }
        .buttonStyle(AdhanSetRowPress(p: p))
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }
}

private struct AdhanSetDivider: View {
    let p: HomePalette
    /// يبدأ الخط بعد الأيقونة (16 + 34 + 12)
    var inset: CGFloat = 62
    var body: some View {
        Rectangle().fill(p.outlineVariant.opacity(0.8)).frame(height: 0.5)
            .padding(.leading, inset)
            .accessibilityHidden(true)
    }
}

/// شرح صغير داخل البطاقة
private struct AdhanSetFootnote: View {
    let text: String
    let p: HomePalette
    var top: Bool = false
    var body: some View {
        Text(text).font(sys(12)).foregroundColor(p.onSurfaceVariant)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.top, top ? 14 : 4)
            .padding(.bottom, top ? 4 : 14)
    }
}

// MARK: طريقة التنبيه

/// صلاة: رمز وقتها واسمها والطريقة المختارة تحته، وقائمة النظام بالطرق الأربع (برموزها والمختارة بعلامة صح)
private struct AdhanSetPrayerRow: View {
    let pr: AdhanSetState.Prayer
    let modes: [AdhanSetState.Mode]
    let p: HomePalette
    let model: AdhanSetModel
    var body: some View {
        HStack(spacing: 12) {
            AdhanSetTile(symbol: prayerSymbol(pr.key), p: p)
            AdhanSetRowText(title: pr.name, sub: pr.detail, p: p)
                .accessibilityHidden(true)
            Spacer(minLength: 8)
            Menu {
                Picker(pr.name, selection: Binding(get: { pr.mode }, set: { v in
                    if v != pr.mode { UISelectionFeedbackGenerator().selectionChanged() }
                    model.alertMode(pr.key, v)
                })) {
                    ForEach(modes) { m in Label(m.name, systemImage: adhanModeSymbol(m.key)).tag(m.key) }
                }
            } label: {
                let off = pr.mode == "off"
                HStack(spacing: 6) {
                    Image(systemName: adhanModeSymbol(pr.mode))
                        .font(.system(size: 15, weight: .semibold))
                        .frame(width: 20)
                    Image(systemName: "chevron.up.chevron.down")
                        .font(.system(size: 11, weight: .bold))
                        .opacity(0.8)
                }
                .foregroundColor(off ? p.onSurfaceVariant : p.onPrimaryContainer)
                .padding(.horizontal, 12).frame(height: 36)
                .background(Capsule().fill(off ? p.onSurfaceVariant.opacity(0.1) : p.primaryContainer))
            }
            .accessibilityLabel(pr.name)
            .accessibilityValue(pr.detail)
        }
        .padding(.horizontal, 16).padding(.vertical, 10)
        .frame(minHeight: 56)
    }
}

// MARK: المؤذنون

/// علامة الاختيار (RadioDot)
private struct AdhanSetRadio: View {
    let on: Bool
    let p: HomePalette
    var body: some View {
        ZStack {
            Circle().fill(on ? p.primary : p.onSurfaceVariant.opacity(0.12))
            if on {
                Image(systemName: "checkmark").font(.system(size: 12, weight: .bold)).foregroundColor(.white)
            }
        }
        .frame(width: 24, height: 24)
        .accessibilityHidden(true)
    }
}

/// حلقة تقدم التنزيل
private struct AdhanSetRing: View {
    let value: Double
    let p: HomePalette
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    var body: some View {
        ZStack {
            Circle().stroke(p.primaryContainer, lineWidth: 2.5)
            Circle().trim(from: 0, to: CGFloat(min(max(value, 0.02), 1)))
                .stroke(p.primary, style: StrokeStyle(lineWidth: 2.5, lineCap: .round))
                .rotationEffect(.degrees(-90))
        }
        .frame(width: 24, height: 24)
        .padding(10)
        .animation(reduceMotion ? nil : .easeOut(duration: 0.3), value: value)
    }
}

/// مؤذن: الاختيار (يُنزَّل أولاً إن لم يكن على الجهاز ثم يُختار)، والمدة أو الحجم أو نسبة التنزيل،
/// وفي النهاية حلقة التقدم أو زر التنزيل، أو الحذف (للمنزَّل غير المختار) وزر الاستماع
private struct AdhanSetMuezzinRow: View {
    let m: AdhanSetState.Muezzin
    let fajr: Bool
    let s: AdhanSetState
    let p: HomePalette
    let model: AdhanSetModel
    var body: some View {
        HStack(spacing: 4) {
            Button {
                if !m.selected { UISelectionFeedbackGenerator().selectionChanged() }
                model.pick(m.id, fajr: fajr)
            } label: {
                HStack(spacing: 12) {
                    AdhanSetRadio(on: m.selected, p: p)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(m.name).font(sys(16, .semibold)).foregroundColor(p.onSurface)
                            .fixedSize(horizontal: false, vertical: true)
                        Text(m.sub).font(sys(12)).foregroundColor(m.failed ? HomePalette.color(s.error) : p.onSurfaceVariant)
                            .modifier(NumericRoll(value: m.downloading ? m.sub : nil))
                    }
                    Spacer(minLength: 4)
                }
                .padding(.leading, 16).padding(.vertical, 10)
                .frame(minHeight: 56)
                .contentShape(Rectangle())
            }
            .buttonStyle(AdhanSetRowPress(p: p))
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(m.selected ? [.isButton, .isSelected] : .isButton)
            trailing.padding(.trailing, 8)
        }
    }

    @ViewBuilder private var trailing: some View {
        if m.downloading {
            AdhanSetRing(value: m.progress, p: p)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(m.sub)
        } else if !m.available {
            Button {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                model.download(m.id)
            } label: {
                Image(systemName: "arrow.down.circle")
                    .font(.system(size: 22, weight: .medium)).foregroundColor(p.primary)
                    .frame(width: 44, height: 44)
            }
            .buttonStyle(PressScale(scale: 0.9))
            .accessibilityLabel(s.download)
            .accessibilityHint(m.name)
        } else {
            HStack(spacing: 2) {
                if m.canDelete {
                    Button { model.remove(m.id) } label: {
                        Image(systemName: "trash")
                            .font(.system(size: 16, weight: .medium)).foregroundColor(p.onSurfaceVariant)
                            .frame(width: 40, height: 44)
                    }
                    .buttonStyle(PressScale(scale: 0.9))
                    .accessibilityLabel(s.delete)
                    .accessibilityHint(m.name)
                }
                Button { model.play(m.id) } label: {
                    Image(systemName: m.playing ? "stop.fill" : "play.fill")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(m.playing ? .white : p.primary)
                        .frame(width: 36, height: 36)
                        .background(Circle().fill(m.playing ? p.primary : p.primaryContainer))
                        .frame(width: 44, height: 44)
                }
                .buttonStyle(PressScale(scale: 0.9))
                .accessibilityLabel(m.playing ? s.stop : s.play)
                .accessibilityHint(m.name)
            }
        }
    }
}

/// «مثل بقية الصلوات» لأذان الفجر
private struct AdhanSetSameRow: View {
    let same: AdhanSetState.Same
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button {
            if !same.on { UISelectionFeedbackGenerator().selectionChanged() }
            action()
        } label: {
            HStack(spacing: 12) {
                AdhanSetRadio(on: same.on, p: p)
                AdhanSetRowText(title: same.title, sub: same.sub, p: p)
                Spacer(minLength: 4)
            }
            .padding(.horizontal, 16).padding(.vertical, 10)
            .frame(minHeight: 56)
            .contentShape(Rectangle())
        }
        .buttonStyle(AdhanSetRowPress(p: p))
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(same.on ? [.isButton, .isSelected] : .isButton)
    }
}

// MARK: الاهتزاز

/// أنماط الاهتزاز شبكةً من عمودين (وثلاثة على الأوسع)؛ اختيار النمط يجرّبه فوراً
private struct AdhanSetVibeGrid: View {
    let s: AdhanSetState
    let p: HomePalette
    let model: AdhanSetModel
    private var perRow: Int { s.width >= 560 ? 3 : 2 }
    var body: some View {
        VStack(spacing: 8) {
            ForEach(Array(stride(from: 0, to: s.vibes.count, by: perRow)), id: \.self) { i in
                HStack(spacing: 8) {
                    ForEach(s.vibes[i..<min(i + perRow, s.vibes.count)]) { v in
                        AdhanSetVibeTile(v: v, s: s, p: p, model: model).frame(maxWidth: .infinity)
                    }
                    ForEach(0..<(perRow - min(perRow, s.vibes.count - i)), id: \.self) { _ in
                        Color.clear.frame(maxWidth: .infinity, maxHeight: 1)
                    }
                }
            }
        }
        .padding(12)
    }
}

private struct AdhanSetVibeTile: View {
    let v: AdhanSetState.Vibe
    let s: AdhanSetState
    let p: HomePalette
    let model: AdhanSetModel
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    var body: some View {
        HStack(spacing: 0) {
            Button { model.vibe(v.key) } label: {
                VStack(alignment: .leading, spacing: 6) {
                    Text(v.name).font(sys(14, .semibold)).foregroundColor(p.onSurface).lineLimit(1)
                    AdhanSetVibeShape(pulses: v.pulses, rtl: s.rtl)
                        .fill(v.selected ? p.primary : p.onSurfaceVariant.opacity(0.6))
                        .frame(height: 14)
                        .overlay(Rectangle().fill((v.selected ? p.primary : p.onSurfaceVariant).opacity(0.25)).frame(height: 1), alignment: .bottom)
                }
                .padding(.leading, 12).padding(.vertical, 8)
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(PressScale(scale: 0.97))
            .accessibilityLabel(v.name)
            .accessibilityAddTraits(v.selected ? [.isButton, .isSelected] : .isButton)
            Button { model.tryVibe(v.key) } label: {
                Image(systemName: "play.fill").font(.system(size: 15, weight: .bold)).foregroundColor(p.primary)
                    .frame(width: 40, height: 44)
            }
            .buttonStyle(PressScale(scale: 0.9))
            .accessibilityLabel(s.tryVibration)
            .accessibilityHint(v.name)
        }
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous)
            .fill(v.selected ? p.primaryContainer : p.onSurfaceVariant.opacity(0.08)))
        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous)
            .strokeBorder(v.selected ? p.primary : Color.clear, lineWidth: 1.5))
        .animation(reduceMotion ? nil : .easeOut(duration: 0.18), value: v.selected)
    }
}

/// رسم النمط: كل مقطع مهتز شريط عرضه مدته وارتفاعه شدته، على خط زمني واحد يتبع اتجاه اللغة (VibeShape)
private struct AdhanSetVibeShape: Shape {
    let pulses: [[Double]]
    let rtl: Bool
    func path(in rect: CGRect) -> Path {
        var path = Path()
        for pulse in pulses where pulse.count >= 3 {
            let w = max(CGFloat(pulse[1]) * rect.width - 1, 2)
            let x = CGFloat(pulse[0]) * rect.width
            let h = rect.height * CGFloat(pulse[2])
            let r = CGRect(x: rtl ? rect.width - x - w : x, y: rect.height - h, width: w, height: h)
            path.addRoundedRect(in: r, cornerSize: CGSize(width: 1.5, height: 1.5))
        }
        return path
    }
}

/// ملاحظة بخلفية لون التطبيق الفاتح (Note)
private struct AdhanSetNote: View {
    let text: String
    let p: HomePalette
    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "info.circle.fill").font(.system(size: 17)).foregroundColor(p.primary)
                .accessibilityHidden(true)
            Text(text).font(sys(14)).foregroundColor(p.onPrimaryContainer)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.primaryContainer))
    }
}
