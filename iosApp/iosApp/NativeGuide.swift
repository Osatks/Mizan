import SwiftUI
import UIKit
import Shared

/// جولة التطبيق بواجهة النظام (SwiftUI): خلفية خضراء بتدرج لون التطبيق ونجمة كبيرة تدور ببطء، وصفحات تُقلَّب بالسحب
/// (لكل صفحة شارة النجمة الذهبية وأيقونتها، والعنوان والنص)، ونقاط الصفحات وزر «التالي»، و«تخطي» في الأعلى،
/// وصفحة الموقع الأخيرة بزرّيها — كما في Onboarding.kt. الصفحة الحالية والتنقل وطلب الموقع في Kotlin (NativeGuide.ios.kt).
enum NativeGuideScreen {
    static func install() {
        NativeGuide.shared.attach { session in GuideHostController(session: session) }
    }
}

final class GuideHostController: UIHostingController<GuideRoot> {
    init(session: NativeGuideSession) {
        let model = GuideModel(session: session)
        super.init(rootView: GuideRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        // الهوامش من Kotlin (شريط الحالة والمؤشر السفلي) لا من المنطقة الآمنة لهذا المتحكم
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct GuideState: Decodable {
    struct Page: Decodable { let title, body, icon: String }

    let rtl, dark: Bool
    let top, bottom, width: Double
    let pages: [Page]
    let page: Int
    let last, location, showSkip: Bool
    let place: String?
    let locating: Bool
    let skip, next, start, useMyLocation, locatingText, pickCity: String
    let c: HomeState.Colors

    static func decode(_ json: String) -> GuideState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(GuideState.self, from: data)
    }
}

final class GuideModel: ObservableObject {
    let session: NativeGuideSession
    @Published private(set) var state: GuideState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    /// الصفحة الظاهرة: تتبع Kotlin، ويغيّرها السحب فوراً ثم يُخبَر بها Kotlin
    @Published private(set) var page: Int = 0
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativeGuideSession) {
        self.session = session
        state = GuideState.decode(session.state)
        page = state?.page ?? 0
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanGuideState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = GuideState.decode(self.session.state) else { return }
            self.state = s
            // «التالي» و«تخطي» من Kotlin: انتقال متحرك إلى الصفحة الجديدة
            if s.page != self.page {
                withAnimation(UIAccessibility.isReduceMotionEnabled ? nil : .easeInOut(duration: 0.35)) { self.page = s.page }
            }
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    /// سحب المستخدم إلى صفحة
    func select(_ i: Int) {
        guard i != page else { return }
        page = i
        session.go(page: Int32(i))
    }

    func next() { session.goNext() }
    func skip() { session.skip() }
    func locate() { session.locate() }
    func pickCity() { session.pickCity() }
}

/// أيقونات الصفحات (PAGE_ICONS و guideIcons في Onboarding.kt) برموز النظام
private func guideSymbol(_ key: String) -> String {
    switch key {
    case "star": return "star.fill"
    case "explore": return "safari.fill"
    case "quran": return "book.fill"
    case "calc": return "plus.slash.minus"
    case "search": return "magnifyingglass"
    case "widgets": return "square.grid.2x2.fill"
    case "quote": return "quote.bubble.fill"
    case "share": return "square.and.arrow.up"
    case "lock": return "lock.fill"
    case "tap": return "hand.tap.fill"
    case "watch": return "applewatch"
    case "adhan": return "bell.badge.fill"
    case "place": return "mappin.and.ellipse"
    default: return "star.fill"
    }
}

/// لون النص على الذهبي (0xFF1E1A14 في Compose)
private let guideInk = Color(.sRGB, red: 0x1E / 255.0, green: 0x1A / 255.0, blue: 0x14 / 255.0, opacity: 1)

// MARK: - الشاشة

struct GuideRoot: View {
    @ObservedObject var model: GuideModel
    var body: some View {
        Group {
            if let s = model.state { GuideView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

struct GuideView: View {
    let s: GuideState
    let p: HomePalette
    @ObservedObject var model: GuideModel

    private var dir: LayoutDirection { s.rtl ? .rightToLeft : .leftToRight }

    /// ترتيب الصفحات في المقلّب: في العربية تُعكس فتأتي التالية من اليسار (المقلّب نفسه يبقى من اليسار إلى اليمين)
    private var order: [Int] {
        let all = Array(0..<s.pages.count)
        return s.rtl ? all.reversed() : all
    }

    var body: some View {
        ZStack {
            LinearGradient(colors: [p.hero1, p.hero2], startPoint: .topLeading, endPoint: .bottomTrailing)
            // نجمة كبيرة تدور ببطء خلف الصفحات (في زاوية النهاية)
            ZStack(alignment: s.rtl ? .topLeading : .topTrailing) {
                Color.clear
                GuideSpinStar()
                    .offset(x: s.rtl ? -110 : 110, y: -70)
            }
            .environment(\.layoutDirection, .leftToRight)
            .allowsHitTesting(false)
            VStack(spacing: 0) {
                topRow
                TabView(selection: Binding(get: { model.page }, set: { model.select($0) })) {
                    ForEach(order, id: \.self) { i in
                        GuidePageView(page: s.pages[i], isLocation: s.location && i == s.pages.count - 1, s: s, p: p, model: model)
                            .environment(\.layoutDirection, dir)
                            .tag(i)
                    }
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
                .environment(\.layoutDirection, .leftToRight)
                .frame(maxHeight: .infinity)
                bottomRow
            }
        }
        .environment(\.layoutDirection, dir)
        .environment(\.colorScheme, .dark)
    }

    /// «تخطي» في جهة النهاية؛ يختفي في الصفحة الأخيرة متى اختير الموقع
    private var topRow: some View {
        HStack(spacing: 0) {
            Spacer(minLength: 0)
            if s.showSkip {
                Button(action: model.skip) {
                    Text(s.skip).font(sys(15, .medium)).foregroundColor(.white.opacity(0.85))
                        .padding(.horizontal, 12)
                        .frame(minHeight: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(PressScale(scale: 0.92))
            }
        }
        .frame(minHeight: 48)
        .padding(.horizontal, 8)
        .padding(.top, CGFloat(s.top) + 4)
    }

    /// النقاط وزر المتابعة (بعرض أقصاه 640 في الوسط على الشاشات العريضة)
    private var bottomRow: some View {
        HStack(spacing: 12) {
            HStack(spacing: 4) {
                ForEach(0..<s.pages.count, id: \.self) { i in
                    Capsule()
                        .fill(i == model.page ? p.gold : Color.white.opacity(0.35))
                        .frame(width: i == model.page ? 18 : 6, height: 6)
                }
            }
            .animation(UIAccessibility.isReduceMotionEnabled ? nil : .easeOut(duration: 0.25), value: model.page)
            .accessibilityHidden(true)
            Spacer(minLength: 0)
            Button(action: model.next) {
                Text(s.last ? s.start : s.next)
                    .font(sys(15, .bold)).foregroundColor(guideInk)
                    .lineLimit(1)
                    .padding(.horizontal, 24)
                    .frame(minHeight: 44)
                    .background(Capsule().fill(p.gold))
            }
            .buttonStyle(PressScale())
        }
        .padding(24)
        .frame(maxWidth: 640)
        .frame(maxWidth: .infinity)
        .padding(.bottom, CGFloat(s.bottom))
    }
}

/// النجمة الكبيرة الباهتة تدور دورة كل تسعين ثانية (ولا تدور إذا أُزيلت الحركات من إعدادات النظام)
private struct GuideSpinStar: View {
    @State private var spin = false
    var body: some View {
        StarEmblem(color: Color.white.opacity(0.06), radius: 210, stroke: 1.5)
            .rotationEffect(.degrees(spin ? 360 : 0))
            .onAppear {
                guard !UIAccessibility.isReduceMotionEnabled else { return }
                withAnimation(.linear(duration: 90).repeatForever(autoreverses: false)) { spin = true }
            }
    }
}

/// صفحة من الجولة: الشارة والعنوان والنص، وفي صفحة الموقع الموقع المختار وزرّاه. تُمرَّر في النوافذ القصيرة
private struct GuidePageView: View {
    let page: GuideState.Page
    let isLocation: Bool
    let s: GuideState
    let p: HomePalette
    @ObservedObject var model: GuideModel

    var body: some View {
        GeometryReader { g in
            ScrollView(.vertical, showsIndicators: false) {
                content(short: g.size.height < 560)
                    .frame(maxWidth: 560)
                    .padding(.horizontal, 32)
                    .frame(maxWidth: .infinity, minHeight: g.size.height)
            }
        }
    }

    private func content(short: Bool) -> some View {
        VStack(spacing: 0) {
            GuideBadge(icon: page.icon, short: short, p: p)
            Text(page.title)
                .font(sys(24, .bold)).foregroundColor(.white)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, short ? 20 : 36)
                .accessibilityAddTraits(.isHeader)
            Text(page.body)
                .font(sys(16)).foregroundColor(.white.opacity(0.85))
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 14)
            if isLocation {
                GuideLocation(s: s, p: p, model: model)
                    .padding(.top, 28)
            }
        }
    }
}

/// الشارة: دائرة باهتة ونجمة ثمانية ذهبية وفي وسطها أيقونة الصفحة
private struct GuideBadge: View {
    let icon: String
    let short: Bool
    let p: HomePalette
    var body: some View {
        let size: CGFloat = short ? 120 : 180
        ZStack {
            Circle().fill(Color.white.opacity(0.08))
            Star8().stroke(p.gold.opacity(0.9), lineWidth: 2.5)
                .frame(width: size * 0.84, height: size * 0.84)
            Circle().fill(Color.white.opacity(0.10))
                .frame(width: size * 0.54, height: size * 0.54)
            Image(systemName: guideSymbol(icon))
                .font(.system(size: short ? 34 : 48, weight: .regular))
                .foregroundColor(p.gold)
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

/// صفحة الموقع: الموقع المختار فوق الزرين بعد تحديده، و«استعمل موقعي» (الإذن في Kotlin) و«اختر مدينة» (ورقة Compose)
private struct GuideLocation: View {
    let s: GuideState
    let p: HomePalette
    @ObservedObject var model: GuideModel
    var body: some View {
        VStack(spacing: 10) {
            if let place = s.place {
                HStack(spacing: 8) {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 20)).foregroundColor(p.gold)
                        .accessibilityHidden(true)
                    Text(place).font(sys(16, .bold)).foregroundColor(.white)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.bottom, 6)
                .accessibilityElement(children: .combine)
            }
            Button(action: model.locate) {
                HStack(spacing: 8) {
                    if s.locating {
                        ProgressView().progressViewStyle(CircularProgressViewStyle(tint: guideInk))
                    } else {
                        Image(systemName: "location.fill").font(.system(size: 16, weight: .semibold))
                    }
                    Text(s.locating ? s.locatingText : s.useMyLocation).font(sys(15, .bold))
                }
                .foregroundColor(guideInk)
                .frame(maxWidth: .infinity, minHeight: 48)
                .background(Capsule().fill(p.gold.opacity(s.locating ? 0.6 : 1)))
            }
            .buttonStyle(PressScale())
            .disabled(s.locating)
            Button(action: model.pickCity) {
                HStack(spacing: 8) {
                    Image(systemName: "building.2.fill").font(.system(size: 16, weight: .semibold))
                    Text(s.pickCity).font(sys(15, .medium))
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity, minHeight: 48)
                .overlay(Capsule().strokeBorder(Color.white.opacity(0.45), lineWidth: 1))
                .contentShape(Capsule())
            }
            .buttonStyle(PressScale())
        }
    }
}
