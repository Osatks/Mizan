import SwiftUI
import UIKit
import Shared

/// تبويب الإعدادات بواجهة النظام (SwiftUI) بطراز الرئيسية: ترويسة بتدرج لون التطبيق ونجمته، ثم مجموعات بطاقات من الصفوف
/// (أيقونة على مربع ملوّن، والعنوان، والوصف، وسهم أو مفتاح) — كما في SettingsScreen.kt. كل اختيار يعود إلى Kotlin
/// (NativeSettings.ios.kt) فيحفظه ويعيد نشر الحالة، ومعها المظهر واتجاه اللغة فتتلون الشاشة فوراً.
enum NativeSettingsScreen {
    static func install() {
        NativeSettings.shared.attach { session in SettingsHostController(session: session) }
    }
}

final class SettingsHostController: UIHostingController<SettingsRoot> {
    init(session: NativeSettingsSession) {
        let model = SettingsModel(session: session)
        super.init(rootView: SettingsRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        // الهوامش من Kotlin (شريط الحالة والتبويبات) لا من المنطقة الآمنة لهذا المتحكم
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct SettingsState: Decodable {
    struct Row: Decodable { let title: String; let sub: String? }
    struct Option: Decodable, Identifiable { let key, name: String; var id: String { key } }
    struct AccentItem: Decodable, Identifiable { let key, name, c1, c2: String; var id: String { key } }
    struct Warn: Decodable { let bg, fg, icon: String }

    let rtl, dark: Bool
    let top, bottom, start, end, width: Double
    let title, languageTitle: String
    let language, guide: Row
    let appearance, theme: String
    let themes: [Option]
    let accentLabel, accent: String
    let accents: [AccentItem]
    let quranTitle: String
    let tafsir: Row
    let prayerTitle: String
    let notify: Bool
    let alert, adhan: Row
    let use24h: Bool
    let clock: Row
    let exactAlarm: Row?
    let rate, shareApp, feedback: Row
    let aboutTitle, aboutName, version, aboutText, sources: String
    let licenses: Row
    let privacyTitle, privacyText, disclaimer: String
    let w: Warn
    let c: HomeState.Colors

    static func decode(_ json: String) -> SettingsState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(SettingsState.self, from: data)
    }
}

final class SettingsModel: ObservableObject {
    let session: NativeSettingsSession
    @Published private(set) var state: SettingsState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativeSettingsSession) {
        self.session = session
        state = SettingsState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanSettingsState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = SettingsState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    func open(_ route: String) { session.open(route: route) }
    func language() { session.language() }
    func theme(_ key: String) { session.theme(key: key) }
    func accent(_ key: String) { session.accent(key: key) }
    func notify(_ on: Bool) { session.notify(on: on) }
    func use24h(_ on: Bool) { session.use24h(on: on) }
    func exactAlarm() { session.exactAlarm() }
    func rate() { session.rate() }
    func share() { session.share() }
    func feedback() { session.feedback() }
}

/// نص Markdown بروابطه (المصادر) بلون التطبيق
private func settingsLinked(_ md: String, _ color: Color) -> AttributedString {
    var a = (try? AttributedString(markdown: md, options: AttributedString.MarkdownParsingOptions(interpretedSyntax: .inlineOnlyPreservingWhitespace)))
        ?? AttributedString(md)
    for run in a.runs where run.link != nil {
        a[run.range].foregroundColor = color
    }
    return a
}

private struct SettingsHeaderKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

// MARK: - الشاشة

struct SettingsRoot: View {
    @ObservedObject var model: SettingsModel
    var body: some View {
        Group {
            if let s = model.state { SettingsView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

struct SettingsView: View {
    let s: SettingsState
    let p: HomePalette
    let model: SettingsModel
    /// شفافية الشريط الزجاجي: 0 والصفحة في أعلاها، و1 حين يمر طرف الترويسة السفلي تحته (GlassTopBar)
    @State private var glass: Double = 0

    private func track(_ headerBottom: CGFloat) {
        let bar = CGFloat(s.top) + 52, ramp: CGFloat = 36
        let g = Double(min(max((bar + ramp - headerBottom) / ramp, 0), 1))
        if g != glass { glass = g }
    }

    var body: some View {
        ZStack(alignment: .top) {
            p.background
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    header
                        .background(GeometryReader { g in
                            Color.clear.preference(key: SettingsHeaderKey.self, value: g.frame(in: .named("settings")).maxY)
                        })
                    VStack(alignment: .leading, spacing: 20) {
                        SettingsGroup(title: s.languageTitle, p: p) {
                            SettingsNavRow(symbol: "character.bubble.fill", row: s.language, p: p) { model.language() }
                        }
                        SettingsGroup(title: nil, p: p) {
                            SettingsNavRow(symbol: "safari.fill", row: s.guide, p: p) { model.open("guide") }
                        }
                        SettingsGroup(title: s.appearance, p: p) {
                            SettingsAppearance(s: s, p: p, model: model)
                        }
                        // مكتبة التفاسير: تنزيل التفاسير واختيار ما يظهر منها
                        SettingsGroup(title: s.quranTitle, p: p) {
                            SettingsNavRow(symbol: "books.vertical.fill", row: s.tafsir, p: p) { model.open("tafsirs") }
                        }
                        SettingsGroup(title: s.prayerTitle, p: p) {
                            SettingsToggleRow(symbol: "bell.badge.fill", row: s.alert, isOn: s.notify, p: p) { model.notify($0) }
                            SettingsDivider(p: p)
                            SettingsNavRow(symbol: "speaker.wave.2.fill", row: s.adhan, p: p) { model.open("adhan") }
                            SettingsDivider(p: p)
                            SettingsToggleRow(symbol: "clock.fill", row: s.clock, isOn: s.use24h, p: p) { model.use24h($0) }
                            if let ex = s.exactAlarm {
                                SettingsDivider(p: p)
                                SettingsNavRow(symbol: "alarm.fill", row: ex, chevron: false, p: p) { model.exactAlarm() }
                            }
                        }
                        // التقييم والمشاركة والملاحظات
                        SettingsGroup(title: nil, p: p) {
                            SettingsNavRow(symbol: "star.fill", row: s.rate, p: p) { model.rate() }
                            SettingsDivider(p: p)
                            SettingsNavRow(symbol: "square.and.arrow.up", row: s.shareApp, p: p) { model.share() }
                            SettingsDivider(p: p)
                            SettingsNavRow(symbol: "envelope.fill", row: s.feedback, p: p) { model.feedback() }
                        }
                        SettingsGroup(title: s.aboutTitle, p: p) {
                            SettingsAbout(s: s, p: p)
                            SettingsDivider(p: p, inset: 16)
                            // المصادر والتراخيص كاملة
                            SettingsNavRow(symbol: "info.circle.fill", row: s.licenses, p: p) { model.open("licenses") }
                        }
                        SettingsGroup(title: s.privacyTitle, p: p) {
                            HStack(alignment: .top, spacing: 12) {
                                SettingsTile(symbol: "hand.raised.fill", p: p)
                                Text(s.privacyText).font(sys(15)).foregroundColor(p.onSurface)
                                    .fixedSize(horizontal: false, vertical: true)
                                Spacer(minLength: 0)
                            }
                            .padding(16)
                        }
                        SettingsWarnNote(text: s.disclaimer, w: s.w)
                    }
                    .padding(.leading, CGFloat(s.start))
                    .padding(.trailing, CGFloat(s.end))
                }
                .padding(.bottom, CGFloat(s.bottom))
            }
            .coordinateSpace(name: "settings")
            .onPreferenceChange(SettingsHeaderKey.self) { track($0) }
            SettingsGlassBar(title: s.title, top: CGFloat(s.top), p: p, amount: glass)
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }

    private var header: some View {
        HStack(spacing: 12) {
            Image(systemName: "gearshape.fill")
                .font(.system(size: 20, weight: .semibold)).foregroundColor(p.primary)
                .frame(width: 46, height: 46)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.surface.opacity(0.85)).shadow(color: .black.opacity(0.08), radius: 3, y: 1))
                .accessibilityHidden(true)
            Text(s.title).font(sys(28, .heavy)).foregroundColor(p.primary).lineLimit(1).minimumScaleFactor(0.8)
            Spacer(minLength: 0)
        }
        .padding(.leading, CGFloat(s.start) + 4)
        .padding(.trailing, CGFloat(s.end) + 4)
        .padding(.top, CGFloat(s.top) + 18)
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

/// الشريط الزجاجي العلوي بعنوان الصفحة حين تمر الترويسة تحته
private struct SettingsGlassBar: View {
    let title: String
    let top: CGFloat
    let p: HomePalette
    let amount: Double
    var body: some View {
        Text(title)
            .font(sys(16, .medium))
            .foregroundColor(p.onSurface)
            .lineLimit(1)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .padding(.top, top)
            .background(.ultraThinMaterial)
            .overlay(Rectangle().fill(p.outlineVariant.opacity(0.8)).frame(height: 1), alignment: .bottom)
            .opacity(amount)
            .allowsHitTesting(amount > 0.5)
            .accessibilityHidden(true)
    }
}

// MARK: - المجموعات والصفوف

/// مجموعة بأسلوب iOS: عنوان صغير فوق بطاقة بيضاء مستديرة صفوفها مفصولة بخطوط رفيعة
private struct SettingsGroup<Content: View>: View {
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
            // القص قبل الخلفية: يُقص تظليل الصف المضغوط على الزوايا ولا يُقص ظل البطاقة
            VStack(spacing: 0) { content }
                .frame(maxWidth: .infinity)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(p.surface).shadow(color: .black.opacity(0.04), radius: 3, y: 1))
                .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.8), lineWidth: 1))
        }
    }
}

/// أيقونة الصف على مربع ملوّن
private struct SettingsTile: View {
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

private struct SettingsRowText: View {
    let row: SettingsState.Row
    let p: HomePalette
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(row.title).font(sys(16, .semibold)).foregroundColor(p.onSurface)
                .fixedSize(horizontal: false, vertical: true)
            if let sub = row.sub {
                Text(sub).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

/// يظلل الصف تحت الإصبع كصفوف إعدادات النظام
private struct SettingsRowPress: ButtonStyle {
    let p: HomePalette
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(p.onSurface.opacity(configuration.isPressed ? 0.07 : 0))
    }
}

/// صف يفتح صفحة أو ينفذ أمراً: الأيقونة والعنوان والوصف والسهم
private struct SettingsNavRow: View {
    let symbol: String
    let row: SettingsState.Row
    var chevron: Bool = true
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                SettingsTile(symbol: symbol, p: p)
                SettingsRowText(row: row, p: p)
                Spacer(minLength: 8)
                if chevron {
                    Image(systemName: "chevron.forward")
                        .font(.system(size: 14, weight: .semibold)).foregroundColor(p.onSurfaceVariant.opacity(0.7))
                        .accessibilityHidden(true)
                }
            }
            .padding(.horizontal, 16).padding(.vertical, 12)
            .frame(minHeight: 56)
            .contentShape(Rectangle())
        }
        .buttonStyle(SettingsRowPress(p: p))
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }
}

/// صف بمفتاح تشغيل (يرسل التغيير إلى Kotlin، والقيمة المعروضة منه)
private struct SettingsToggleRow: View {
    let symbol: String
    let row: SettingsState.Row
    let isOn: Bool
    let p: HomePalette
    let set: (Bool) -> Void
    var body: some View {
        Toggle(isOn: Binding(get: { isOn }, set: { set($0) })) {
            HStack(spacing: 12) {
                SettingsTile(symbol: symbol, p: p)
                SettingsRowText(row: row, p: p)
            }
        }
        .tint(p.primary)
        .padding(.horizontal, 16).padding(.vertical, 12)
        .frame(minHeight: 56)
    }
}

private struct SettingsDivider: View {
    let p: HomePalette
    /// يبدأ الخط بعد الأيقونة (16 + 34 + 12)
    var inset: CGFloat = 62
    var body: some View {
        Rectangle().fill(p.outlineVariant.opacity(0.8)).frame(height: 0.5)
            .padding(.leading, inset)
            .accessibilityHidden(true)
    }
}

// MARK: - المظهر

/// المظهر (تلقائي / فاتح / داكن) ولون التطبيق: عشرة ألوان في صفين (وفي صف واحد على الأوسع)
private struct SettingsAppearance: View {
    let s: SettingsState
    let p: HomePalette
    let model: SettingsModel

    private var perRow: Int { s.width >= 560 ? 10 : 5 }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Picker(s.appearance, selection: Binding(get: { s.theme }, set: { model.theme($0) })) {
                ForEach(s.themes) { t in Text(t.name).tag(t.key) }
            }
            .pickerStyle(.segmented)
            Text(s.accentLabel).font(sys(13, .semibold)).foregroundColor(p.onSurfaceVariant)
                .padding(.top, 16).padding(.bottom, 8).padding(.horizontal, 4)
            VStack(spacing: 6) {
                ForEach(Array(stride(from: 0, to: s.accents.count, by: perRow)), id: \.self) { i in
                    HStack(spacing: 0) {
                        ForEach(s.accents[i..<min(i + perRow, s.accents.count)]) { a in
                            SettingsSwatch(a: a, on: a.key == s.accent, p: p) { model.accent(a.key) }
                                .frame(maxWidth: .infinity)
                        }
                    }
                }
            }
        }
        .padding(16)
    }
}

/// دائرة اللون بتدرجه؛ المختار عليه حلقة وعلامة صح
private struct SettingsSwatch: View {
    let a: SettingsState.AccentItem
    let on: Bool
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            ZStack {
                Circle()
                    .fill(LinearGradient(colors: [HomePalette.color(a.c1), HomePalette.color(a.c2)], startPoint: .topLeading, endPoint: .bottomTrailing))
                    .padding(5)
                Circle().strokeBorder(on ? p.onSurface : Color.clear, lineWidth: 2)
                if on {
                    Image(systemName: "checkmark").font(.system(size: 16, weight: .bold)).foregroundColor(.white)
                }
            }
            .frame(width: 48, height: 48)
            .animation(UIAccessibility.isReduceMotionEnabled ? nil : .easeOut(duration: 0.2), value: on)
        }
        .buttonStyle(PressScale(scale: 0.9))
        .accessibilityLabel(a.name)
        .accessibilityAddTraits(on ? .isSelected : [])
    }
}

// MARK: - عن التطبيق

private struct SettingsAbout: View {
    let s: SettingsState
    let p: HomePalette
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Star8().fill(p.gold).frame(width: 18, height: 18).accessibilityHidden(true)
                Text(s.aboutName).font(sys(17, .bold)).foregroundColor(p.onSurface)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Text(s.version).font(sys(12)).foregroundColor(p.onSurfaceVariant)
            Text(s.aboutText).font(sys(15)).foregroundColor(p.onSurface)
                .fixedSize(horizontal: false, vertical: true)
            Text(settingsLinked(s.sources, p.primary)).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// تنبيه: الحساب تقريبي والفتوى لأهلها (Note بتحذير)
private struct SettingsWarnNote: View {
    let text: String
    let w: SettingsState.Warn
    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 17)).foregroundColor(HomePalette.color(w.icon))
                .accessibilityHidden(true)
            Text(text).font(sys(14)).foregroundColor(HomePalette.color(w.fg))
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(HomePalette.color(w.bg)))
    }
}
