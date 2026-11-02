import SwiftUI
import WidgetKit

// تصميم أدوات ميزان: سطح بلون التطبيق بتدرج مائل (وأخضر قاتم قريب من الأسود في الوضع الداكن)،
// ولمعة ذهبية في الزاوية، وزخرفة نجمة ثمانية بخط ذهبي رفيع، ونص أبيض بدرجات وذهبي للمهم.

extension Color {
    init(hex: String) {
        var s = hex.trimmingCharacters(in: .whitespaces)
        if s.hasPrefix("#") { s.removeFirst() }
        var v: UInt64 = 0
        _ = Scanner(string: s).scanHexInt64(&v)
        self.init(.sRGB,
                  red: Double((v >> 16) & 0xFF) / 255,
                  green: Double((v >> 8) & 0xFF) / 255,
                  blue: Double(v & 0xFF) / 255,
                  opacity: 1)
    }
}

enum Mizan {
    static let gold = Color(hex: "#E8C15A")
    static let goldDeep = Color(hex: "#B88A1E")
    static let text = Color.white
    static let text2 = Color.white.opacity(0.78)
    static let text3 = Color.white.opacity(0.56)
    static let fallback = Palette(hero1: "#0F6A55", hero2: "#08463A", night1: "#1B2D28", night2: "#0B1311", accent: "#3FB595", gold: "#E8C15A")

    /// خط المصحف (Amiri Quran) مضمّن في الأداة؛ إن تعذّر يُستعمل خط النظام
    static func quran(_ size: CGFloat) -> Font { .custom("AmiriQuran-Regular", size: size) }

    static func rounded(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
        .system(size: size, weight: weight, design: .rounded)
    }
}

/// نجمة ثمانية (ربع الحزب): مربعان متقاطعان ودائرتان في الوسط
struct StarOrnament: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        let c = CGPoint(x: rect.midX, y: rect.midY)
        for scale in [0.5, 0.36] {
            let r = min(rect.width, rect.height) * CGFloat(scale)
            let s = r / sqrt(2)
            p.addRect(CGRect(x: c.x - s, y: c.y - s, width: 2 * s, height: 2 * s))
            p.move(to: CGPoint(x: c.x, y: c.y - r))
            p.addLine(to: CGPoint(x: c.x + r, y: c.y))
            p.addLine(to: CGPoint(x: c.x, y: c.y + r))
            p.addLine(to: CGPoint(x: c.x - r, y: c.y))
            p.closeSubpath()
        }
        for scale in [0.22, 0.13] {
            let r = min(rect.width, rect.height) * CGFloat(scale)
            p.addEllipse(in: CGRect(x: c.x - r, y: c.y - r, width: 2 * r, height: 2 * r))
        }
        return p
    }
}

/// نجمة ثمانية ممتلئة صغيرة (رمز ميزان في العناوين)
struct StarGlyph: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        let c = CGPoint(x: rect.midX, y: rect.midY)
        let r = min(rect.width, rect.height) / 2
        let s = r / sqrt(2)
        p.addRect(CGRect(x: c.x - s, y: c.y - s, width: 2 * s, height: 2 * s))
        p.move(to: CGPoint(x: c.x, y: c.y - r))
        p.addLine(to: CGPoint(x: c.x + r, y: c.y))
        p.addLine(to: CGPoint(x: c.x, y: c.y + r))
        p.addLine(to: CGPoint(x: c.x - r, y: c.y))
        p.closeSubpath()
        return p
    }
}

/// خلفية الأداة
struct MizanBackground: View {
    let palette: Palette
    let dark: Bool

    var body: some View {
        let top = Color(hex: dark ? palette.night1 : palette.hero1)
        let bottom = Color(hex: dark ? palette.night2 : palette.hero2)
        ZStack {
            LinearGradient(colors: [top, bottom], startPoint: .topLeading, endPoint: .bottomTrailing)
            RadialGradient(colors: [Mizan.gold.opacity(dark ? 0.16 : 0.22), Mizan.gold.opacity(0)],
                           center: UnitPoint(x: 0.96, y: 0.0), startRadius: 0, endRadius: 210)
            LinearGradient(colors: [Color.white.opacity(0.07), Color.white.opacity(0)], startPoint: .top, endPoint: .center)
            GeometryReader { g in
                let size = max(g.size.height * 1.15, 120)
                StarOrnament()
                    .stroke(Mizan.gold.opacity(dark ? 0.14 : 0.17), lineWidth: 1)
                    .frame(width: size, height: size)
                    .position(x: g.size.width - size * 0.18, y: size * 0.16)
            }
        }
    }
}

extension Snapshot {
    func isDark(_ scheme: ColorScheme) -> Bool {
        switch theme {
        case "dark": return true
        case "light": return false
        default: return scheme == .dark
        }
    }
}

/// غلاف كل أداة على الشاشة الرئيسية: containerBackground في iOS 17، وخلفية وهوامش يدوية قبلها
struct Shell<Content: View>: View {
    let snap: Snapshot?
    let url: URL?
    @ViewBuilder let content: () -> Content
    @Environment(\.colorScheme) var scheme

    var body: some View {
        let palette = snap?.colors ?? Mizan.fallback
        let dark = snap?.isDark(scheme) ?? (scheme == .dark)
        let rtl = snap?.rtl ?? Fallback.arabic
        let inner = content()
            .environment(\.layoutDirection, rtl ? .rightToLeft : .leftToRight)
            .environment(\.locale, Locale(identifier: snap?.lang ?? Fallback.lang))
            .foregroundColor(Mizan.text)
            .widgetURL(url)
        if #available(iOS 17.0, *) {
            inner.containerBackground(for: .widget) { MizanBackground(palette: palette, dark: dark) }
        } else {
            inner
                .padding(16)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(MizanBackground(palette: palette, dark: dark))
        }
    }
}

/// غلاف أدوات شاشة القفل: بلا لون (يلوّنها النظام)، وخلفية النظام الضبابية للدائرية
struct AccessoryShell<Content: View>: View {
    let snap: Snapshot?
    let url: URL?
    var circular: Bool = false
    @ViewBuilder let content: () -> Content

    var body: some View {
        let rtl = snap?.rtl ?? Fallback.arabic
        let inner = content()
            .environment(\.layoutDirection, rtl ? .rightToLeft : .leftToRight)
            .environment(\.locale, Locale(identifier: snap?.lang ?? Fallback.lang))
            .widgetURL(url)
        if #available(iOS 17.0, *) {
            inner.containerBackground(for: .widget) {
                if circular { AccessoryBackdrop() } else { Color.clear }
            }
        } else {
            ZStack {
                if circular { AccessoryBackdrop() }
                inner
            }
        }
    }
}

/// خلفية الدائرة في شاشة القفل (iOS 16 فما بعد)
struct AccessoryBackdrop: View {
    var body: some View {
        if #available(iOS 16.0, *) {
            AccessoryWidgetBackground()
        } else {
            Color.clear
        }
    }
}

/// عنوان الأداة: رمز ذهبي واسم بالذهبي، وما يلحق به في الطرف الآخر
struct Header<Trailing: View>: View {
    let symbol: String?
    let title: String
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        HStack(spacing: 6) {
            if let symbol = symbol {
                Image(systemName: symbol)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(Mizan.gold)
            } else {
                StarGlyph().fill(Mizan.gold).frame(width: 11, height: 11)
            }
            Text(title)
                .font(Mizan.rounded(12, .semibold))
                .foregroundColor(Mizan.gold)
                .lineLimit(1)
            Spacer(minLength: 4)
            trailing()
        }
    }
}

extension Header where Trailing == EmptyView {
    init(symbol: String?, title: String) {
        self.symbol = symbol
        self.title = title
        self.trailing = { EmptyView() }
    }
}

/// شارة صغيرة شفافة
struct Chip: View {
    let text: String
    var gold: Bool = false

    var body: some View {
        Text(text)
            .font(Mizan.rounded(10, .semibold))
            .foregroundColor(gold ? Mizan.gold : Mizan.text)
            .lineLimit(1)
            .padding(.horizontal, 8)
            .padding(.vertical, 2)
            .background(Capsule().fill(gold ? Mizan.gold.opacity(0.18) : Color.white.opacity(0.13)))
    }
}

/// شريط ما مضى من وقت الصلاة: يتحرك وحده في iOS 16 فما بعد
struct TimeBar: View {
    let from: Date
    let to: Date
    let now: Date

    var fraction: Double {
        let total = max(to.timeIntervalSince(from), 1)
        return min(max(now.timeIntervalSince(from) / total, 0), 1)
    }

    var body: some View {
        Group {
            if #available(iOS 16.0, *) {
                if from < to {
                    ProgressView(timerInterval: from...to, countsDown: false, label: { EmptyView() }, currentValueLabel: { EmptyView() })
                } else {
                    ProgressView(value: fraction)
                }
            } else {
                ProgressView(value: fraction)
            }
        }
        .progressViewStyle(.linear)
        .tint(Mizan.gold)
    }
}

/// ما يظهر حين لا لقطة: لا موقع للأداة ولا لقطة من التطبيق (رسالة إذن الموقع)، أو لقطة انتهت أيامها
struct NeedsApp: View {
    let snap: Snapshot?

    var body: some View {
        VStack(spacing: 6) {
            if snap == nil {
                Image(systemName: "location.circle.fill")
                    .font(.system(size: 22, weight: .regular))
                    .foregroundColor(Mizan.gold)
                Text(Fallback.text("allowLocation"))
                    .font(Mizan.rounded(13, .semibold))
                    .multilineTextAlignment(.center)
                    .foregroundColor(Mizan.text)
                    .minimumScaleFactor(0.7)
                Text(Fallback.text("locationHint"))
                    .font(Mizan.rounded(10.5))
                    .multilineTextAlignment(.center)
                    .foregroundColor(Mizan.text2)
                    .minimumScaleFactor(0.7)
                // رمز صغير للتشخيص: حال إذن الموقع، ثم حال لقطة التطبيق (لا مجموعة، أو لا ملف، أو ملف لا يُقرأ)
                Text(Feed.diagnosis())
                    .font(.system(size: 8, weight: .regular, design: .monospaced))
                    .foregroundColor(Mizan.text2.opacity(0.55))
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                    .environment(\.layoutDirection, .leftToRight)
            } else {
                StarGlyph().fill(Mizan.gold).frame(width: 22, height: 22)
                Text(snap?.label("openApp") ?? Fallback.text("openApp"))
                    .font(Mizan.rounded(13, .medium))
                    .multilineTextAlignment(.center)
                    .foregroundColor(Mizan.text2)
                    .minimumScaleFactor(0.7)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// رابط يفتح شاشة في التطبيق: mizan://open?route=prayer&section=morning
enum Deep {
    static func url(_ route: String, section: String? = nil) -> URL? {
        var c = URLComponents()
        c.scheme = "mizan"
        c.host = "open"
        // مسار السورة فيه ? و & و = ؛ يُرمَّز كله حتى يعود كما هو في التطبيق
        func enc(_ s: String) -> String { s.addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? s }
        var q = [URLQueryItem(name: "route", value: enc(route))]
        if let section = section { q.append(URLQueryItem(name: "section", value: enc(section))) }
        c.percentEncodedQueryItems = q
        return c.url
    }
}

/// مقاس الأداة كما يضبطه WidgetKit، أو ما يضبطه معرض الأدوات (iosApp/Gallery) حين يرسم الأداة في تطبيق للمعاينة:
/// widgetFamily لا يُضبط خارج WidgetKit. في الأداة نفسها galleryFamily فارغ دائماً فيبقى مقاس WidgetKit
@propertyWrapper
struct WidgetFamilyValue: DynamicProperty {
    @Environment(\.widgetFamily) private var system
    @Environment(\.galleryFamily) private var gallery

    var wrappedValue: WidgetFamily { gallery ?? system }
}

private struct GalleryFamilyKey: EnvironmentKey {
    static let defaultValue: WidgetFamily? = nil
}

private struct GalleryBareKey: EnvironmentKey {
    static let defaultValue: Bool? = nil
}

extension EnvironmentValues {
    /// للمعرض وحده: مقاس الأداة المرسومة
    var galleryFamily: WidgetFamily? {
        get { self[GalleryFamilyKey.self] }
        set { self[GalleryFamilyKey.self] = newValue }
    }

    /// للمعرض وحده: الأداة بلا خلفية كما في StandBy
    var galleryBare: Bool? {
        get { self[GalleryBareKey.self] }
        set { self[GalleryBareKey.self] = newValue }
    }
}

extension WidgetFamily {
    /// أدوات شاشة القفل (iOS 16 فما بعد)
    var isAccessory: Bool {
        if #available(iOS 16.0, *) {
            switch self {
            case .accessoryCircular, .accessoryRectangular, .accessoryInline: return true
            default: return false
            }
        }
        return false
    }
}

/// الأحجام المدعومة: أدوات شاشة القفل تُضاف في iOS 16 فما بعد فقط
enum Families {
    static func make(_ system: [WidgetFamily], circular: Bool = false, rectangular: Bool = false, inline: Bool = false) -> [WidgetFamily] {
        var f = system
        if #available(iOS 16.0, *) {
            if circular { f.append(.accessoryCircular) }
            if rectangular { f.append(.accessoryRectangular) }
            if inline { f.append(.accessoryInline) }
        }
        return f
    }
}
