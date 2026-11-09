import SwiftUI
import UIKit
import CoreText
import Shared

/// الرئيسية بواجهة النظام (SwiftUI) بدل رئيسية Compose: الترويسة وبطاقة اليوم وأذكار الوقت والصلاة القادمة،
/// ومواقيت اليوم، وأدوات كل يوم، والقرآن الكريم، وآية اليوم وحديثه — كما في HomeScreen.kt.
/// الحالة من Kotlin (NativeHome.ios.kt) كل دقيقة وعند كل تغيّر، مع الإشعار MizanHomeState؛ والعدّادات تتقدم هنا كل ثانية
/// من أوقات الصلاة (epoch) دون أن يُعاد شيء في Compose. اللمس يعود إلى Kotlin فيفتح الشاشة نفسها كما من رئيسية Compose.
enum NativeHomeScreen {
    /// يُستدعى مرة عند التشغيل، قبل إنشاء MainViewController
    static func install() {
        registerQuranFont()
        NativeHome.shared.attach { session in HomeHostController(session: session) }
    }

    /// خط المصحف (Amiri Quran) لاسم اليوم والآية واسم السورة، من الملف نفسه الذي يستعمله Compose
    private static func registerQuranFont() {
        guard let root = Bundle.main.resourcePath else { return }
        let url = URL(fileURLWithPath: root + "/assets/fonts/amiri_quran.ttf")
        _ = CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
    }
}

final class HomeHostController: UIHostingController<HomeRoot> {
    init(session: NativeHomeSession) {
        let model = HomeModel(session: session)
        super.init(rootView: HomeRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        // الهوامش من Kotlin (شريط الحالة والتبويبات) لا من المنطقة الآمنة لهذا المتحكم
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}


extension UIViewController {
    /// مظهر التطبيق واتجاه لغته على ما يرسمه UIKit داخل الواجهة (القوائم، والأوراق، والمفاتيح، والعدّادات):
    /// قد يخالفان مظهر النظام ولغته (الإعدادات: فاتح أو داكن دائماً، ولغة التطبيق)
    func applyAppStyle(dark: Bool, rtl: Bool) {
        overrideUserInterfaceStyle = dark ? .dark : .light
        view.semanticContentAttribute = rtl ? .forceRightToLeft : .forceLeftToRight
    }
}

// MARK: - الحالة

struct HomeState: Decodable {
    struct Day: Decodable { let weekday, hMonth, hDay, hYear, gMonth, gDay, gYear: String }
    struct Adhkar: Decodable { let title, section: String }
    struct Next: Decodable { let key, name: String; let at: Double; let today: Bool }
    struct Prayer: Decodable, Identifiable { let key, name, time: String; let start: Double; var id: String { key } }
    struct Tool: Decodable, Identifiable { let key, name: String; var id: String { key } }
    struct Quran: Decodable { let kind, label, title, sub, arabic, route: String; let focusLabel, focusRoute: String? }
    struct Ayah: Decodable { let text, ref, route: String }
    struct Hadith: Decodable { let ar: String; let tr: String?; let meta: String }
    struct Colors: Decodable {
        let primary, primaryContainer, onPrimaryContainer, background, surface, onSurface, onSurfaceVariant,
            outlineVariant, gold, goldDeep, hero1, hero2: String
    }

    let rtl: Bool
    let lang: String
    let dark: Bool
    let top, bottom, start, end: Double
    /// عرض المحتوى بين الهامشين (نقاط)
    let width: Double
    let appName, tagline, search, language, prayersTitle, place, daily, sinceAdhan, ayahTitle, tafsir, hadithTitle, explanation: String
    let day: Day
    let adhkar: Adhkar?
    let next: Next
    let prayers: [Prayer]
    let tools: [Tool]
    let quran: Quran
    let ayah: Ayah?
    let hadith: Hadith?
    let c: Colors

    static func decode(_ json: String) -> HomeState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(HomeState.self, from: data)
    }
}

final class HomeModel: ObservableObject {
    let session: NativeHomeSession
    @Published private(set) var state: HomeState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativeHomeSession) {
        self.session = session
        state = HomeState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanHomeState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = HomeState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    func open(_ route: String) { session.open(route: route) }
    func language() { session.language() }
    func adhkar(_ section: String) { session.adhkar(section: section) }
}

/// ألوان التطبيق كما في MaterialTheme (بلون التطبيق المختار ومظهره)
struct HomePalette {
    let primary, primaryContainer, onPrimaryContainer, background, surface, onSurface, onSurfaceVariant,
        outlineVariant, gold, goldDeep, hero1, hero2: Color

    init(_ c: HomeState.Colors) {
        primary = Self.color(c.primary); primaryContainer = Self.color(c.primaryContainer)
        onPrimaryContainer = Self.color(c.onPrimaryContainer); background = Self.color(c.background)
        surface = Self.color(c.surface); onSurface = Self.color(c.onSurface); onSurfaceVariant = Self.color(c.onSurfaceVariant)
        outlineVariant = Self.color(c.outlineVariant); gold = Self.color(c.gold); goldDeep = Self.color(c.goldDeep)
        hero1 = Self.color(c.hero1); hero2 = Self.color(c.hero2)
    }

    /// "#AARRGGBB"
    static func color(_ hex: String) -> Color {
        let v = UInt32(hex.dropFirst(), radix: 16) ?? 0xFF000000
        return Color(.sRGB, red: Double((v >> 16) & 0xFF) / 255, green: Double((v >> 8) & 0xFF) / 255,
                     blue: Double(v & 0xFF) / 255, opacity: Double((v >> 24) & 0xFF) / 255)
    }
}

// MARK: - أدوات صغيرة

/// خط النظام بمقاس يتبع حجم النص في الإعدادات (كما تتبعه sp في Compose)، وأقصاه 1.8 ضعفاً كما في Compose
func sys(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
    .system(size: min(UIFontMetrics.default.scaledValue(for: size), size * 1.8), weight: weight)
}

func quranFont(_ size: CGFloat) -> Font { .custom("AmiriQuran-Regular", size: size, relativeTo: .body) }

/// بداية الثانية الحالية: العدّادات كلها تتقدم معاً على حدود الثواني (كأوقات الصلاة بالثواني)
func wholeSecond() -> Date { Date(timeIntervalSince1970: Date().timeIntervalSince1970.rounded(.down)) }

/// ساعات:دقائق:ثوانٍ بأرقام لاتينية (PrayerUtil.hms)
func hms(_ seconds: Double) -> String {
    let s = max(0, Int(seconds))
    return String(format: "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
}

/// رموز الأوقات: الفجر نجوم، والشروق والمغرب أفق، والظهر شمس، والعصر شمس مائلة، والعشاء قمر
func prayerSymbol(_ key: String) -> String {
    switch key {
    case "fajr": return "sparkles"
    case "sunrise": return "sunrise"
    case "dhuhr": return "sun.max"
    case "asr": return "sun.min"
    case "maghrib": return "sunset"
    case "isha": return "moon.stars"
    default: return "clock"
    }
}

private func toolSymbol(_ key: String) -> String {
    switch key {
    case "adhkar": return "sparkles"
    case "tasbih": return "hand.tap"
    case "qibla": return "safari"
    case "prayer": return "clock"
    default: return "square.grid.2x2"
    }
}

/// يصغر قليلاً تحت الإصبع ثم يرتد
struct PressScale: ButtonStyle {
    var scale: CGFloat = 0.97
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? scale : 1)
            .animation(.spring(response: 0.25, dampingFraction: 0.7), value: configuration.isPressed)
    }
}

/// نجمة ثمانية من مربعين متراكبين (drawStar8 في Emblem.kt)
struct Star8: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        let c = CGPoint(x: rect.midX, y: rect.midY), r = min(rect.width, rect.height) / 2
        for sq in 0..<2 {
            for i in 0..<4 {
                let a = (Double(sq) * 45 + Double(i) * 90 - 90) * .pi / 180
                let pt = CGPoint(x: c.x + r * CGFloat(cos(a)), y: c.y + r * CGFloat(sin(a)))
                if i == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
            }
            p.closeSubpath()
        }
        return p
    }
}

/// النجمة الزخرفية الباهتة خلف الترويسة والبطاقات البارزة (بحلقتيها)
struct StarEmblem: View {
    let color: Color
    let radius: CGFloat
    let stroke: CGFloat
    var body: some View {
        ZStack {
            Star8().stroke(color, lineWidth: stroke)
            Circle().stroke(color, lineWidth: stroke * 0.8).frame(width: radius * 1.04, height: radius * 1.04)
            Circle().stroke(color, lineWidth: stroke * 0.8).frame(width: radius * 0.36, height: radius * 0.36)
        }
        .frame(width: radius * 2, height: radius * 2)
        .accessibilityHidden(true)
    }
}

/// الأرقام تتدحرج عند تغيّرها (iOS 17)، وتثبت قبله؛ value هو النص المعروض (بدونه لا حركة)
struct NumericRoll: ViewModifier {
    var value: String? = nil
    func body(content: Content) -> some View {
        if #available(iOS 17.0, *) {
            content.contentTransition(.numericText(countsDown: true))
                .animation(value == nil || UIAccessibility.isReduceMotionEnabled ? nil : .easeOut(duration: 0.3), value: value)
        } else { content }
    }
}

private struct HeaderBottomKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

// MARK: - الشاشة

struct HomeRoot: View {
    @ObservedObject var model: HomeModel

    var body: some View {
        Group {
            if let s = model.state { HomeView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

struct HomeView: View {
    let s: HomeState
    let p: HomePalette
    let model: HomeModel
    /// شفافية الشريط الزجاجي: 0 والصفحة في أعلاها، و1 حين يمر طرف الترويسة السفلي تحته (GlassTopBar)
    @State private var glass: Double = 0
    @State private var appeared = false

    /// تُكتب الشفافية حين تتغير فقط، فلا يُعاد بناء الصفحة مع كل إطار تمرير
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
                    HomeHeader(s: s, p: p, model: model)
                        .background(GeometryReader { g in
                            Color.clear.preference(key: HeaderBottomKey.self, value: g.frame(in: .named("home")).maxY)
                        })
                        .reveal(appeared, 0)
                    VStack(spacing: 0) {
                        SectionTitle(title: s.prayersTitle, p: p, trailing: s.place, symbol: "mappin.and.ellipse") { model.open("prayer") }
                            .padding(.top, 22).reveal(appeared, 2)
                        PrayerGrid(s: s, p: p) { model.open("prayer") }
                            .padding(.top, 4).reveal(appeared, 3)
                        SectionTitle(title: s.daily, p: p).padding(.top, 22).reveal(appeared, 4)
                        DailyRow(tools: s.tools, p: p, dark: s.dark, open: model.open)
                            .padding(.top, 6).reveal(appeared, 5)
                        QuranCard(q: s.quran, p: p, open: model.open).padding(.top, 18).reveal(appeared, 6)
                        if let a = s.ayah {
                            AyahCard(a: a, s: s, p: p) { model.open(a.route) }.padding(.top, 14).reveal(appeared, 7)
                        }
                        if let h = s.hadith {
                            HadithCard(h: h, s: s, p: p) { model.open("hadith") }.padding(.top, 14).reveal(appeared, 7)
                        }
                    }
                    .padding(.leading, CGFloat(s.start))
                    .padding(.trailing, CGFloat(s.end))
                }
                .padding(.bottom, CGFloat(s.bottom))
            }
            .coordinateSpace(name: "home")
            .onPreferenceChange(HeaderBottomKey.self) { track($0) }
            GlassBar(title: s.appName, top: CGFloat(s.top), p: p, amount: glass)
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
        .onAppear { appeared = true }
    }
}

private extension View {
    /// ظهور متتابع عند أول عرض (كما في reveal بـ Compose)، ولا حركة مع «تقليل الحركة»
    func reveal(_ on: Bool, _ step: Int) -> some View {
        let still = UIAccessibility.isReduceMotionEnabled
        return self
            .opacity(on || still ? 1 : 0)
            .offset(y: on || still ? 0 : 14)
            .animation(still ? nil : .easeOut(duration: 0.45).delay(Double(step) * 0.05), value: on)
    }
}

/// الشريط الزجاجي العلوي باسم التطبيق حين تمر الترويسة تحته
private struct GlassBar: View {
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

// MARK: الترويسة

private struct HomeHeader: View {
    let s: HomeState
    let p: HomePalette
    let model: HomeModel

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .center, spacing: 10) {
                VStack(alignment: .leading, spacing: 0) {
                    Text(s.appName).font(sys(30, .heavy)).foregroundColor(p.primary).lineLimit(1)
                    Text(s.tagline).font(sys(14)).foregroundColor(p.onSurfaceVariant).lineLimit(1)
                }
                Spacer(minLength: 8)
                RoundButton(symbol: "magnifyingglass", label: s.search, p: p) { model.open("search") }
                RoundButton(symbol: "globe", label: s.language, p: p) { model.language() }
            }
            HStack(alignment: .center, spacing: 14) {
                // بطاقة اليوم وعمود أذكار الوقت بنسبة 1 : 1.15 كما في رئيسية Compose (عنوان الأذكار الطويل لا يُقص)
                DayCard(d: s.day, arabic: s.lang == "ar", p: p)
                    .frame(width: max(120, (CGFloat(s.width) - 8 - 14) / 2.15))
                VStack(spacing: 10) {
                    if let a = s.adhkar {
                        Button { model.adhkar(a.section) } label: {
                            HStack(spacing: 8) {
                                Image(systemName: "sparkles").font(.system(size: 16, weight: .semibold)).foregroundColor(p.gold).accessibilityHidden(true)
                                Text(a.title).font(sys(14, .bold)).foregroundColor(p.onSurface).lineLimit(1)
                                Spacer(minLength: 0)
                            }
                            .padding(.horizontal, 14).padding(.vertical, 13)
                            .background(Capsule().fill(p.surface).shadow(color: .black.opacity(0.07), radius: 4, y: 1))
                        }
                        .buttonStyle(PressScale())
                    }
                    NextChip(next: s.next, p: p)
                }
                .frame(maxWidth: .infinity)
            }
            .padding(.top, 16)
        }
        .padding(.leading, CGFloat(s.start) + 4)
        .padding(.trailing, CGFloat(s.end) + 4)
        .padding(.top, CGFloat(s.top) + 14)
        .padding(.bottom, 8)
        .frame(maxWidth: .infinity)
        .background(
            ZStack(alignment: s.rtl ? .topTrailing : .topLeading) {
                // التدرج من لون التطبيق الفاتح إلى لون الصفحة، ويمتد فوق الترويسة عند سحب الصفحة إلى الأسفل
                GeometryReader { g in
                    VStack(spacing: 0) {
                        p.primaryContainer.frame(height: 800)
                        LinearGradient(colors: [p.primaryContainer, p.primaryContainer.opacity(0.55), p.background], startPoint: .top, endPoint: .bottom)
                            .frame(height: g.size.height)
                    }
                    .offset(y: -800)
                }
                // نجمة باهتة كبيرة مركزها خارج زاوية البداية العليا (starBackdrop: ‎-70، ‎-40)
                StarEmblem(color: p.primary.opacity(0.07), radius: 150, stroke: 1.7)
                    .offset(x: s.rtl ? 220 : -220, y: -190)
            }
            // المواضع هنا باليسار واليمين صراحةً (لا تنقلب مع اتجاه اللغة)
            .environment(\.layoutDirection, .leftToRight)
            .allowsHitTesting(false)
        )
    }
}

private struct RoundButton: View {
    let symbol: String
    let label: String
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 19, weight: .semibold))
                .foregroundColor(p.primary)
                .frame(width: 46, height: 46)
                .background(Circle().fill(p.surface.opacity(0.85)).shadow(color: .black.opacity(0.08), radius: 3, y: 1))
        }
        .buttonStyle(PressScale(scale: 0.9))
        .accessibilityLabel(label)
    }
}

/// بطاقة اليوم: اسم اليوم (بخط المصحف بالعربية)، وتحته الهجري والميلادي: الشهر، فاليوم كبيراً، فالسنة
private struct DayCard: View {
    let d: HomeState.Day
    let arabic: Bool
    let p: HomePalette
    var body: some View {
        VStack(spacing: 0) {
            Text(d.weekday)
                .font(arabic ? quranFont(30) : sys(24, .heavy))
                .foregroundColor(p.primary)
                .lineLimit(1).minimumScaleFactor(0.7)
            Rectangle().fill(p.outlineVariant).frame(height: 1).padding(.vertical, 8)
            HStack(spacing: 0) {
                DateColumn(month: d.hMonth, day: d.hDay, year: d.hYear, p: p)
                Rectangle().fill(p.outlineVariant).frame(width: 1)
                DateColumn(month: d.gMonth, day: d.gDay, year: d.gYear, p: p)
            }
            .fixedSize(horizontal: false, vertical: true)
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(p.surface).shadow(color: .black.opacity(0.08), radius: 6, y: 2))
        .accessibilityElement(children: .combine)
    }
}

private struct DateColumn: View {
    let month, day, year: String
    let p: HomePalette
    var body: some View {
        VStack(spacing: 0) {
            Text(month).font(sys(11, .medium)).foregroundColor(p.onSurfaceVariant).lineLimit(1).minimumScaleFactor(0.8)
            Text(day).font(sys(24, .heavy)).foregroundColor(p.onSurface).lineLimit(1).minimumScaleFactor(0.5)
            Text(year).font(sys(12, .medium)).foregroundColor(p.onSurfaceVariant).lineLimit(1).minimumScaleFactor(0.5)
        }
        .padding(.horizontal, 4)
        .frame(maxWidth: .infinity)
    }
}

/// الصلاة القادمة والعدّ إليها كل ثانية
private struct NextChip: View {
    let next: HomeState.Next
    let p: HomePalette
    var body: some View {
        TimelineView(.periodic(from: wholeSecond(), by: 1)) { ctx in
            HStack(spacing: 8) {
                let left = hms(next.at - ctx.date.timeIntervalSince1970)
                Image(systemName: prayerSymbol(next.key))
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(p.primary)
                    .frame(width: 30, height: 30)
                    .background(Circle().fill(p.surface))
                    .accessibilityHidden(true)
                Text(next.name).font(sys(14)).foregroundColor(p.onSurfaceVariant).lineLimit(1)
                Text(left)
                    .font(sys(14, .heavy)).monospacedDigit()
                    .foregroundColor(p.onSurface)
                    .lineLimit(1).minimumScaleFactor(0.5)
                    .modifier(NumericRoll(value: left))
                    .padding(.horizontal, 9).padding(.vertical, 4)
                    .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(p.gold.opacity(0.22)))
                    .environment(\.layoutDirection, .leftToRight)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 4)
        }
    }
}

// MARK: المواقيت

private struct SectionTitle: View {
    let title: String
    let p: HomePalette
    var trailing: String? = nil
    var symbol: String? = nil
    var action: (() -> Void)? = nil
    var body: some View {
        HStack(alignment: .center) {
            Text(title).font(sys(22, .heavy)).foregroundColor(p.primary).accessibilityAddTraits(.isHeader)
            Spacer(minLength: 8)
            if let t = trailing {
                Button { action?() } label: {
                    HStack(spacing: 4) {
                        Text(t).font(sys(14)).foregroundColor(p.onSurfaceVariant).lineLimit(1)
                        if let sym = symbol { Image(systemName: sym).font(.system(size: 14)).foregroundColor(p.onSurfaceVariant).accessibilityHidden(true) }
                    }
                    .padding(6)
                }
                .buttonStyle(PressScale(scale: 0.95))
                .disabled(action == nil)
            }
        }
        .padding(.horizontal, 4).padding(.vertical, 2)
    }
}

/// مواقيت اليوم: ستة أوقات في شبكة، والحاضرة مظللة بلون التطبيق ومعها ما مضى على أذانها، والقادمة بحدّ ذهبي ومعها العدّ إليها
private struct PrayerGrid: View {
    let s: HomeState
    let p: HomePalette
    let action: () -> Void

    var body: some View {
        TimelineView(.periodic(from: wholeSecond(), by: 1)) { ctx in
            let now = ctx.date.timeIntervalSince1970
            let list = s.prayers
            let current: String? = {
                guard let i = list.lastIndex(where: { $0.key != "sunrise" && $0.start <= now }) else { return nil }
                // الحاضرة حتى الوقت الذي بعدها (الشروق ينهي الفجر)
                return (i + 1 < list.count && now >= list[i + 1].start) ? nil : list[i].key
            }()
            VStack(spacing: 10) {
                ForEach(Array(stride(from: 0, to: list.count, by: 3)), id: \.self) { from in
                    HStack(spacing: 10) {
                        ForEach(list[from..<min(from + 3, list.count)]) { pr in
                            let isCur = pr.key == current
                            let isNext = pr.key == s.next.key && s.next.today
                            PrayerCell(
                                pr: pr, p: p, current: isCur, next: isNext,
                                label: isCur ? s.sinceAdhan : nil,
                                value: isCur ? hms(now - pr.start) : (isNext ? hms(s.next.at - now) : nil),
                                action: action
                            )
                        }
                        ForEach(0..<(3 - min(3, list.count - from)), id: \.self) { _ in Color.clear.frame(maxWidth: .infinity) }
                    }
                    .fixedSize(horizontal: false, vertical: true)
                }
            }
        }
    }
}

private struct PrayerCell: View {
    let pr: HomeState.Prayer
    let p: HomePalette
    let current: Bool
    let next: Bool
    let label: String?
    let value: String?
    let action: () -> Void

    var body: some View {
        let fg = current ? p.onPrimaryContainer : p.onSurface
        Button(action: action) {
            VStack(spacing: 0) {
                Image(systemName: prayerSymbol(pr.key))
                    .font(.system(size: 17, weight: .regular))
                    .foregroundColor(current || next ? p.gold : p.onSurfaceVariant)
                    .frame(height: 20)
                    .accessibilityHidden(true)
                Text(pr.name).font(sys(14)).foregroundColor(fg.opacity(0.8)).lineLimit(1).padding(.top, 6)
                Text(pr.time).font(sys(16, .heavy)).foregroundColor(fg).lineLimit(1).minimumScaleFactor(0.8)
                if let v = value {
                    if let l = label { Text(l).font(sys(11, .medium)).foregroundColor(fg.opacity(0.7)).lineLimit(1).padding(.top, 4) }
                    Text(v).font(sys(14, .bold)).monospacedDigit()
                        .foregroundColor(current ? p.primary : p.goldDeep)
                        .lineLimit(1).minimumScaleFactor(0.5)
                        .modifier(NumericRoll(value: v))
                        .padding(.top, label == nil ? 4 : 0)
                        .environment(\.layoutDirection, .leftToRight)
                }
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 10).padding(.vertical, 12)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(current ? p.primaryContainer : p.surface)
                    .shadow(color: .black.opacity(current ? 0.08 : 0), radius: 4, y: 1)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(next ? p.gold : p.outlineVariant.opacity(0.6), lineWidth: next ? 1.5 : 1)
            )
        }
        .buttonStyle(PressScale(scale: 0.96))
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .accessibilityElement(children: .combine)
    }
}

// MARK: الأدوات والبطاقات

/// أدوات كل يوم في دوائر ملوّنة هادئة: الأذكار، والمسبحة، والقبلة، والمواقيت
private struct DailyRow: View {
    let tools: [HomeState.Tool]
    let p: HomePalette
    let dark: Bool
    let open: (String) -> Void

    private func tint(_ key: String) -> Color {
        let light: [String: UInt32] = ["adhkar": 0xDDEFE3, "tasbih": 0xF1EBDD, "qibla": 0xE9E3F3, "prayer": 0xFBE3E3]
        let deep: [String: UInt32] = ["adhkar": 0x26443A, "tasbih": 0x4A4232, "qibla": 0x3E3550, "prayer": 0x523234]
        let v = (dark ? deep[key] : light[key]) ?? 0xEEEEEE
        return Color(.sRGB, red: Double((v >> 16) & 0xFF) / 255, green: Double((v >> 8) & 0xFF) / 255, blue: Double(v & 0xFF) / 255, opacity: 1)
    }

    var body: some View {
        HStack(spacing: 0) {
            ForEach(tools) { t in
                Button { open(t.key) } label: {
                    VStack(spacing: 6) {
                        Image(systemName: toolSymbol(t.key))
                            .font(.system(size: 24, weight: .regular))
                            .foregroundColor(p.primary)
                            .frame(width: 66, height: 66)
                            .background(Circle().fill(tint(t.key)))
                            .accessibilityHidden(true)
                        Text(t.name).font(sys(14, .medium)).foregroundColor(p.onSurface).lineLimit(1).minimumScaleFactor(0.8)
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(PressScale(scale: 0.92))
            }
        }
    }
}

/// القرآن الكريم: متابعة القراءة من آخر موضع (ووضع التركيز)، أو البدء
private struct QuranCard: View {
    let q: HomeState.Quran
    let p: HomePalette
    let open: (String) -> Void

    var body: some View {
        Button { open(q.route) } label: {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .center, spacing: 14) {
                    if q.kind == "start" {
                        Image(systemName: "book.fill")
                            .font(.system(size: 19)).foregroundColor(p.gold)
                            .frame(width: 46, height: 46)
                            .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(Color.white.opacity(0.14)))
                            .accessibilityHidden(true)
                    }
                    VStack(alignment: .leading, spacing: 1) {
                        if q.kind == "start" {
                            Text(q.title).font(sys(16, .medium)).foregroundColor(.white)
                            Text(q.label).font(sys(14, .bold)).foregroundColor(p.gold)
                        } else {
                            Text(q.label).font(sys(14, .bold)).foregroundColor(p.gold)
                            Text(q.title).font(sys(16, .medium)).foregroundColor(.white)
                            Text(q.sub).font(sys(12)).foregroundColor(.white.opacity(0.8))
                        }
                    }
                    Spacer(minLength: 4)
                    Text(q.arabic).font(quranFont(26)).foregroundColor(.white).lineLimit(1)
                    if q.kind != "start" {
                        Image(systemName: "chevron.forward").font(.system(size: 15, weight: .semibold)).foregroundColor(.white.opacity(0.8)).accessibilityHidden(true)
                    }
                }
                if let label = q.focusLabel, let route = q.focusRoute {
                    Button { open(route) } label: {
                        HStack(spacing: 8) {
                            Image(systemName: "arrow.up.left.and.arrow.down.right").font(.system(size: 14, weight: .semibold)).foregroundColor(p.gold).accessibilityHidden(true)
                            Text(label).font(sys(14, .medium)).foregroundColor(.white).lineLimit(1)
                        }
                        .padding(.leading, 12).padding(.trailing, 14).padding(.vertical, 7)
                        .background(Capsule().fill(Color.white.opacity(0.14)))
                        .overlay(Capsule().strokeBorder(p.gold.opacity(0.55), lineWidth: 1))
                    }
                    .buttonStyle(PressScale(scale: 0.95))
                }
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                ZStack(alignment: .trailing) {
                    LinearGradient(colors: [p.hero1, p.hero2], startPoint: .topLeading, endPoint: .bottomTrailing)
                    StarEmblem(color: .white.opacity(0.07), radius: 60, stroke: 1).offset(x: 30)
                }
            )
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        }
        .buttonStyle(PressScale())
    }
}

/// آية اليوم بخط المصحف ومعناها؛ لمسها يفتح تفسيرها
private struct AyahCard: View {
    let a: HomeState.Ayah
    let s: HomeState
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            VStack(spacing: 10) {
                CardHead(symbol: "book", title: s.ayahTitle, more: s.tafsir, p: p)
                Text(a.text)
                    .font(quranFont(22)).foregroundColor(p.onSurface)
                    .multilineTextAlignment(.center).lineLimit(6).lineSpacing(6)
                    .frame(maxWidth: .infinity)
                    .environment(\.layoutDirection, .rightToLeft)
                Text(a.ref).font(sys(12, .medium)).foregroundColor(p.onSurfaceVariant)
            }
            .padding(18)
            .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(p.surface))
            .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.6), lineWidth: 1))
        }
        .buttonStyle(PressScale(scale: 0.98))
    }
}

/// حديث اليوم: طرفه بالعربية وترجمته بلغة الواجهة؛ لمسه يفتح الحديث وشرحه
private struct HadithCard: View {
    let h: HomeState.Hadith
    let s: HomeState
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            VStack(spacing: 6) {
                CardHead(symbol: "quote.opening", title: s.hadithTitle, more: s.explanation, p: p).padding(.bottom, 4)
                Text("«\(h.ar)»")
                    .font(sys(16, .medium)).foregroundColor(p.onSurface)
                    .multilineTextAlignment(.center).lineLimit(4).lineSpacing(8)
                    .frame(maxWidth: .infinity)
                    .environment(\.layoutDirection, .rightToLeft)
                if let tr = h.tr {
                    Text(tr).font(sys(14)).foregroundColor(p.onSurfaceVariant)
                        .multilineTextAlignment(.center).lineLimit(3).frame(maxWidth: .infinity)
                }
                if !h.meta.isEmpty {
                    Text(h.meta).font(sys(12, .medium)).foregroundColor(p.onSurfaceVariant).lineLimit(1)
                }
            }
            .padding(18)
            .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(p.surface))
            .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.6), lineWidth: 1))
        }
        .buttonStyle(PressScale(scale: 0.98))
    }
}

private struct CardHead: View {
    let symbol: String
    let title: String
    let more: String
    let p: HomePalette
    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: symbol).font(.system(size: 15, weight: .semibold)).foregroundColor(p.gold).accessibilityHidden(true)
            Text(title).font(sys(14, .bold)).foregroundColor(p.primary)
            Spacer(minLength: 8)
            Text(more).font(sys(14, .bold)).foregroundColor(p.goldDeep)
        }
    }
}
