import Foundation

/// حالة شريط التبويبات كما يرسلها Kotlin (tabBarStateJson في TabBar.kt، من الصفحة الظاهرة في IosNav.kt) مع الإشعار
/// MizanTabBarChanged، ويطبّقها TabsController (MizanNavigation.swift). Foundation وحدها، فتُختبر خارج Xcode أيضاً.
struct LiquidTabBarState: Decodable, Equatable {
    struct Tab: Decodable, Equatable {
        let route: String
        let title: String
        /// رمز SF Symbols؛ يُعرض بنسخته الممتلئة (symbol.fill) في كل التبويبات
        let symbol: String
    }

    let tabs: [Tab]
    /// التبويب المختار؛ nil دائماً الآن (الاختيار في UITabBarController نفسه)
    let selected: String?
    /// false أثناء ستار تبديل اللغة: يختفي الشريط معه
    let visible: Bool
    /// اتجاه لغة التطبيق لا لغة النظام: العربية من اليمين وإن كان النظام بالإنجليزية
    let rtl: Bool
    /// مظهر التطبيق (قد يخالف مظهر النظام)
    let dark: Bool
    /// لون التطبيق #RRGGBB
    let accent: String

    static func decode(_ json: String) -> LiquidTabBarState? {
        guard let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(LiquidTabBarState.self, from: data)
    }

    /// مكوّنات لون التطبيق من 0 إلى 1 (sRGB)
    var accentRGB: (red: Double, green: Double, blue: Double)? {
        let hex = accent.hasPrefix("#") ? String(accent.dropFirst()) : accent
        guard hex.count == 6, hex.allSatisfy(\.isHexDigit), let v = UInt32(hex, radix: 16) else { return nil }
        return (Double((v >> 16) & 0xFF) / 255, Double((v >> 8) & 0xFF) / 255, Double(v & 0xFF) / 255)
    }
}
