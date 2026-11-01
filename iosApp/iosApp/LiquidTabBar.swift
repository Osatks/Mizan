import UIKit
import Shared

/// شريط التبويبات في iOS 26 فما بعده: شريط النظام نفسه (UITabBarController) بزجاج Liquid Glass فوق واجهة Compose،
/// بدل الشريط الزجاجي المرسوم بـ Compose (TabBar.kt). الكبسولة العائمة، وعدسة الاختيار التي تنساب بين التبويبات
/// وتتبع الإصبع، وحركة الظهور والاختفاء، وعارض المحتوى الكبير لأحجام الخط الكبيرة، و VoiceOver: كلها من النظام.
/// التنقل يبقى كله في Compose (NavHost): صفحات المتحكم فارغة شفافة، والشريط يعرض حالة يرسلها Kotlin (NativeTabBar.kt)
/// ويخبره باللمس. قبل iOS 26، أو إن بُني بـ SDK أقدم، أو مع UIDesignRequiresCompatibility، لا يُسجَّل فيبقى شريط Compose.
enum LiquidTabBar {
    /// يُستدعى مرة عند التشغيل، قبل إنشاء MainViewController
    static func install() {
        #if compiler(>=6.2)
        guard #available(iOS 26, *) else { return }
        if Bundle.main.object(forInfoDictionaryKey: "UIDesignRequiresCompatibility") as? Bool == true { return }
        NativeTabBar.shared.attach { LiquidTabBarController() }
        #endif
    }
}

/// متحكم الشريط: يضعه المتحكم الجذر (MizanRootController في MainViewController.kt) فوق Compose وتحت نوافذها المنبثقة.
/// لا يلتقط إلا اللمس الواقع على الشريط نفسه، وما سواه يمر إلى Compose تحته.
@available(iOS 26, *)
final class LiquidTabBarController: UIViewController, UITabBarControllerDelegate {
    /// أقصى عرض للشريط: في الوسط على iPad بالطول كشريط Compose، لا بعرض الشاشة
    private static let maxWidth: CGFloat = 560

    private let tabs = BarController()
    private var routes: [String] = []
    private var state: LiquidTabBarState?
    /// التبويب المختار الآن في الشريط
    private var selected: String?
    private var reportedHeight: CGFloat = 0
    /// عاد شريط Compose (لم تُفهم الحالة، أو لم يكن الشريط أسفل الشاشة): يبقى هذا مخفياً لا يلتقط شيئاً
    private var gaveUp = false
    private let haptic = UISelectionFeedbackGenerator()

    override func loadView() {
        let v = PassThroughView()
        v.barFrame = { [weak self] in self?.visibleBarFrame }
        v.backgroundColor = .clear
        v.isOpaque = false
        view = v
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        tabs.delegate = self
        tabs.didLayout = { [weak self] in self?.measure() }
        // شريط الهاتف السفلي في كل مكان: iPad بعرضه العادي يضع شريط التبويبات أعلى الشاشة، وصفحات ميزان تنتظره
        // في الأسفل (وفي iPadOS 26 يصير كبسولة بقدر محتواها في الوسط)
        tabs.traitOverrides.horizontalSizeClass = .compact
        tabs.view.backgroundColor = .clear
        addChild(tabs)
        view.addSubview(tabs.view)
        tabs.didMove(toParent: self)
        tabs.setTabBarHidden(true, animated: false)
        // يرسلها Kotlin على الخيط الرئيسي (NativeTabBar.CHANGED)، فتُطبَّق في الإطار نفسه الذي تتحرك فيه صفحات Compose
        NotificationCenter.default.addObserver(self, selector: #selector(stateChanged(_:)), name: Notification.Name("MizanTabBarChanged"), object: nil)
        apply(NativeTabBar.shared.state)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        let w = min(view.bounds.width, Self.maxWidth)
        let frame = CGRect(x: ((view.bounds.width - w) / 2).rounded(), y: 0, width: w, height: view.bounds.height)
        if tabs.view.frame != frame { tabs.view.frame = frame }
    }

    // MARK: الحالة من Kotlin

    @objc private func stateChanged(_ note: Notification) {
        guard let json = note.object as? String else { return }
        apply(json)
    }

    private func apply(_ json: String) {
        guard !gaveUp, !json.isEmpty else { return }
        guard let s = LiquidTabBarState.decode(json), !s.tabs.isEmpty else {
            // حالة لا يفهمها هذا الإصدار: شريط Compose أسلم من تطبيق بلا شريط
            giveUp()
            return
        }
        let first = state == nil
        if s.tabs != state?.tabs { setTabs(s.tabs) }
        if s.rtl != state?.rtl {
            // اتجاه لغة التطبيق لا لغة النظام: العربية من اليمين وإن كان النظام بالإنجليزية
            tabs.traitOverrides.layoutDirection = s.rtl ? .rightToLeft : .leftToRight
            tabs.tabBar.semanticContentAttribute = s.rtl ? .forceRightToLeft : .forceLeftToRight
            tabs.tabBar.setNeedsLayout()
        }
        // مظهر التطبيق، وقد يخالف مظهر النظام (الإعدادات: فاتح أو داكن دائماً)
        if s.dark != state?.dark { tabs.overrideUserInterfaceStyle = s.dark ? .dark : .light }
        if s.accent != state?.accent, let c = s.accentRGB {
            tint(UIColor(red: c.red, green: c.green, blue: c.blue, alpha: 1))
        }
        if let route = s.selected, route != selected, let i = routes.firstIndex(of: route) {
            selected = route
            tabs.selectedIndex = i
        }
        state = s
        if s.visible == tabs.isTabBarHidden {
            // بحركة النظام نفسها (تنزلق الكبسولة)، إلا عند أول ظهور ومع «تقليل الحركة»
            let animated = !first && view.window != nil && !UIAccessibility.isReduceMotionEnabled
            tabs.setTabBarHidden(!s.visible, animated: animated)
            if s.visible { haptic.prepare() }
        }
    }

    private func setTabs(_ list: [LiquidTabBarState.Tab]) {
        if let pages = tabs.viewControllers, pages.count == list.count, routes == list.map(\.route),
           state?.tabs.map(\.symbol) == list.map(\.symbol) {
            // اللغة وحدها تغيرت: تتبدل الأسماء في مكانها ويبقى الاختيار
            for (page, tab) in zip(pages, list) { page.tabBarItem.title = tab.title }
            return
        }
        routes = list.map(\.route)
        selected = nil
        tabs.setViewControllers(list.map { Self.page($0) }, animated: false)
    }

    /// صفحة فارغة شفافة لكل تبويب (الصفحات نفسها في Compose تحت الشريط)، ورمزه ممتلئاً حين يُختار
    private static func page(_ tab: LiquidTabBarState.Tab) -> UIViewController {
        let page = UIViewController()
        page.view.backgroundColor = .clear
        page.view.isUserInteractionEnabled = false
        page.tabBarItem = UITabBarItem(
            title: tab.title,
            image: UIImage(systemName: tab.symbol),
            selectedImage: UIImage(systemName: tab.symbol + ".fill")
        )
        return page
    }

    /// لون التطبيق للتبويب المختار. tintColor وحده لا يلوّنه في كل إصدارات iOS 26، فاللون في مظهر العنصر المختار أيضاً،
    /// دون لمس خلفية الشريط (تبقى زجاج النظام)
    private func tint(_ color: UIColor) {
        let bar = tabs.tabBar
        bar.tintColor = color
        let a = bar.standardAppearance
        func colored(_ item: UITabBarItemAppearance) -> UITabBarItemAppearance {
            item.selected.iconColor = color
            item.selected.titleTextAttributes[.foregroundColor] = color
            return item
        }
        a.stackedLayoutAppearance = colored(a.stackedLayoutAppearance)
        a.inlineLayoutAppearance = colored(a.inlineLayoutAppearance)
        a.compactInlineLayoutAppearance = colored(a.compactInlineLayoutAppearance)
        bar.standardAppearance = a
        bar.scrollEdgeAppearance = a
    }

    // MARK: اللمس والقياس

    func tabBarController(_ tabBarController: UITabBarController, didSelect viewController: UIViewController) {
        guard let i = tabBarController.viewControllers?.firstIndex(where: { $0 === viewController }), i < routes.count else { return }
        let route = routes[i]
        if route != selected {
            selected = route
            haptic.selectionChanged()
            haptic.prepare()
        }
        // لمس التبويب المختار نفسه يصل أيضاً (كما في شريط Compose)
        NativeTabBar.shared.select(route: route)
    }

    /// إطار الشريط الظاهر في إحداثيات هذا المتحكم؛ nil وهو مخفي
    private var visibleBarFrame: CGRect? {
        guard !gaveUp, !tabs.isTabBarHidden, tabs.tabBar.window != nil else { return nil }
        return tabs.tabBar.convert(tabs.tabBar.bounds, to: view)
    }

    /// ارتفاع الشريط فوق المنطقة الآمنة السفلية، للمساحة أسفل صفحات Compose (LocalBottomInset)
    private func measure() {
        guard let r = visibleBarFrame, r.height > 0, view.bounds.height > 0 else { return }
        // الشريط في أعلى الشاشة (شريط iPad العلوي إن لم يعمل تجاوز فئة العرض): يعود شريط Compose
        if r.maxY < view.bounds.height / 2 {
            DispatchQueue.main.async { [weak self] in self?.giveUp() }
            return
        }
        let h = (view.bounds.height - view.safeAreaInsets.bottom - r.minY).rounded()
        guard h > 20, h < 160, abs(h - reportedHeight) >= 1 else { return }
        reportedHeight = h
        NativeTabBar.shared.reportHeight(points: Double(h))
    }

    /// يعيد شريط Compose ويخفي هذا نهائياً، فلا يبقى التطبيق بلا شريط
    private func giveUp() {
        guard !gaveUp else { return }
        gaveUp = true
        tabs.setTabBarHidden(true, animated: false)
        tabs.view.isHidden = true
        NativeTabBar.shared.fallBack()
    }
}

/// UITabBarController يخبر بكل تخطيط له (يُقاس الشريط بعده: الظهور، والتدوير، و Split View)
@available(iOS 26, *)
private final class BarController: UITabBarController {
    var didLayout: (() -> Void)?

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        didLayout?()
    }
}

/// يمر اللمس إلى Compose تحته إلا على إطار الشريط الظاهر
@available(iOS 26, *)
private final class PassThroughView: UIView {
    var barFrame: () -> CGRect? = { nil }

    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        guard let r = barFrame(), r.contains(point) else { return nil }
        return super.hitTest(point, with: event)
    }
}
