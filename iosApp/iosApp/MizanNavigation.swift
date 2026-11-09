import UIKit
import Shared

/// التنقل في iOS من النظام نفسه: UITabBarController (شريط النظام، بزجاج Liquid Glass تلقائياً في iOS 26) فيه
/// UINavigationController لكل تبويب، وكل صفحة متحكم خاص بها تصنعه Kotlin (IosNav.page في IosNav.kt): واجهة Compose
/// صغيرة لا تعرض إلا شاشة مسارها. Kotlin يطلب التنقل عبر IosNavigator، وهذا الملف ينفّذه بحركة النظام:
/// تنمو الصفحة من موضع اللمس وتصغر إليه عند الرجوع (انتقال التكبير في iOS 18، وحركة مثله قبل ذلك)، والرجوع بسحب الحافة يتبع الإصبع.
enum MizanNavigation {
    /// يُستدعى مرة عند التشغيل، قبل إنشاء MainViewController
    static func install() {
        IosNav.shared.install { AppRootController() }
    }
}

/// نافذة التطبيق: تخبر Kotlin بموضع كل لمس يبدأ، فتنمو الصفحة التي يفتحها من مكانه
final class TouchWindow: UIWindow {
    override func sendEvent(_ event: UIEvent) {
        if event.type == .touches, let touches = event.allTouches {
            for touch in touches where touch.phase == .began {
                let p = touch.location(in: self)
                IosNav.shared.touched(x: Double(p.x), y: Double(p.y))
            }
        }
        super.sendEvent(event)
    }
}

/// جذر النافذة: التبويبات، وفوقها جولة أول تشغيل حين يحين وقتها. منه تُعرض الأوراق والتنبيهات (IosStatusBar.controller)
final class AppRootController: UIViewController {
    private let tabs = TabsController()
    private var guide: UIViewController?

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        tabs.root = self
        embed(tabs)
        IosNav.shared.attach(navigator: tabs, root: self)
        // جولة أول تشغيل فوق كل شيء (وفوق شريط التبويبات)، مرة واحدة
        if let g = IosNav.shared.guide() {
            guide = g
            embed(g)
        }
    }

    private func embed(_ child: UIViewController) {
        addChild(child)
        child.view.frame = view.bounds
        child.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(child.view)
        child.didMove(toParent: self)
    }

    func closeGuide() {
        guard let g = guide else { return }
        guide = nil
        setNeedsStatusBarAppearanceUpdate()
        g.view.isUserInteractionEnabled = false
        UIView.animate(withDuration: 0.3, animations: { g.view.alpha = 0 }, completion: { _ in
            g.willMove(toParent: nil)
            g.view.removeFromSuperview()
            g.removeFromParent()
        })
    }

    // شريط الحالة كما تطلبه الصفحة الظاهرة
    override var childForStatusBarStyle: UIViewController? { guide ?? tabs }
    override var childForStatusBarHidden: UIViewController? { guide ?? tabs }
}

/// التبويبات: شريط النظام بأسماء التبويبات بلغة التطبيق واتجاهها ومظهره ولونه (حالة يرسلها Kotlin، LiquidTabBarState)،
/// وينفّذ طلبات التنقل من Kotlin (IosNavigator) على التبويب الظاهر، ويؤجّلها إن جاءت أثناء انتقال جارٍ
final class TabsController: UITabBarController, UITabBarControllerDelegate, IosNavigator {
    weak var root: AppRootController?
    private var routes: [String] = []
    private var state: LiquidTabBarState?
    private let haptic = UISelectionFeedbackGenerator()
    /// طلبات تنتظر انتهاء الانتقال الجاري
    private var queue: [() -> Void] = []
    private var restoreBar: DispatchWorkItem?

    override func viewDidLoad() {
        super.viewDidLoad()
        delegate = self
        // iPad: الشريط السفلي في كل مكان كما في الهاتف (iPadOS 18 يضعه أعلى الشاشة في العرض العادي، وصفحات ميزان
        // تنتظره في الأسفل). قبل iOS 18 هو في الأسفل على iPad أصلاً
        if #available(iOS 17.0, *) {
            traitOverrides.horizontalSizeClass = .compact
        }
        // خلفية الشريط ثابتة: محتوى Compose لا يخبر UIKit بأنه عند حافة التمرير (وإلا ظهر الشريط شفافاً بلا خلفية)
        tabBar.scrollEdgeAppearance = tabBar.standardAppearance
        routes = IosNav.shared.tabRoutes()
        // صفحات التبويبات تُنشأ الآن خفيفة، وواجهة كل منها تُبنى أول مرة يُختار
        setViewControllers(routes.map { PageNavController(rootViewController: IosNav.shared.page(route: $0, tab: true)) }, animated: false)
        apply(IosNav.shared.tabState())
        // يرسلها Kotlin على الخيط الرئيسي من الصفحة الظاهرة (IosNav.TAB_STATE)
        NotificationCenter.default.addObserver(self, selector: #selector(stateChanged(_:)), name: Notification.Name("MizanTabBarChanged"), object: nil)
        // التبويبات الأخرى تُجهَّز بعد أن تستقر الواجهة، فيظهر كل منها أسرع أول مرة
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) { [weak self] in self?.warmUp(1) }
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        haptic.prepare()
    }

    override var childForStatusBarStyle: UIViewController? { selectedViewController }
    override var childForStatusBarHidden: UIViewController? { selectedViewController }

    private var nav: PageNavController? { selectedViewController as? PageNavController }

    private func warmUp(_ i: Int) {
        guard let list = viewControllers, i < list.count else { return }
        (list[i] as? UINavigationController)?.viewControllers.first?.loadViewIfNeeded()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in self?.warmUp(i + 1) }
    }

    // MARK: الحالة من Kotlin

    @objc private func stateChanged(_ note: Notification) {
        guard let json = note.object as? String else { return }
        apply(json)
    }

    private func apply(_ json: String) {
        guard let s = LiquidTabBarState.decode(json), let list = viewControllers, s.tabs.count == list.count else { return }
        if s.tabs != state?.tabs {
            let sameSymbols = state?.tabs.map(\.symbol) == s.tabs.map(\.symbol)
            for (page, tab) in zip(list, s.tabs) {
                if sameSymbols {
                    // اللغة وحدها تغيرت: تتبدل الأسماء في مكانها
                    page.tabBarItem.title = tab.title
                } else {
                    // الرموز ممتلئة في كل التبويبات كما في تطبيقات Apple؛ يميّز المختارَ لونُه
                    let symbol = UIImage(systemName: tab.symbol + ".fill") ?? UIImage(systemName: tab.symbol)
                    page.tabBarItem = UITabBarItem(title: tab.title, image: symbol, selectedImage: symbol)
                }
            }
        }
        if s.rtl != state?.rtl {
            // اتجاه لغة التطبيق لا لغة النظام: العربية من اليمين وإن كان النظام بالإنجليزية (والرجوع بالسحب من حافتها)
            let attribute: UISemanticContentAttribute = s.rtl ? .forceRightToLeft : .forceLeftToRight
            if #available(iOS 17.0, *) {
                traitOverrides.layoutDirection = s.rtl ? .rightToLeft : .leftToRight
            }
            view.semanticContentAttribute = attribute
            tabBar.semanticContentAttribute = attribute
            for case let page as PageNavController in list { page.rtl = s.rtl }
            tabBar.setNeedsLayout()
        }
        // مظهر التطبيق للشريط وحده (قد يخالف مظهر النظام): الصفحات تأخذ مظهرها من Compose، ولو تجاوزه UIKit
        // لما وصل تغيّر مظهر النظام إليها
        if s.dark != state?.dark { tabBar.overrideUserInterfaceStyle = s.dark ? .dark : .light }
        if s.accent != state?.accent, let c = s.accentRGB {
            tint(UIColor(red: c.red, green: c.green, blue: c.blue, alpha: 1))
        }
        // ستار تبديل اللغة يغطي الصفحة لا الشريط: يختفي الشريط معه ويعود بالأسماء الجديدة
        let hide = !s.visible
        if hide != (tabBar.alpha < 0.5) {
            UIView.animate(withDuration: 0.25) { self.tabBar.alpha = hide ? 0 : 1 }
        }
        restoreBar?.cancel()
        restoreBar = nil
        if hide {
            // احتياط: لا يبقى الشريط مخفياً إن خرجت الصفحة قبل أن ينكشف الستار
            let work = DispatchWorkItem { [weak self] in
                UIView.animate(withDuration: 0.25) { self?.tabBar.alpha = 1 }
            }
            restoreBar = work
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.5, execute: work)
        }
        state = s
    }

    /// لون التطبيق للتبويب المختار. tintColor وحده لا يلوّنه في كل إصدارات iOS 26، فاللون في مظهر العنصر المختار أيضاً،
    /// دون لمس خلفية الشريط (تبقى مادة النظام أو زجاجه)
    private func tint(_ color: UIColor) {
        let bar = tabBar
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

    // MARK: IosNavigator (من Kotlin)

    func push(route: String) {
        perform { [weak self] in self?.nav?.open(route) }
    }

    func pop() {
        perform { [weak self] in self?.nav?.back() }
    }

    func selectTab(route: String, closeCurrent: Bool) {
        perform { [weak self] in self?.switchTo(route, closeCurrent: closeCurrent) }
    }

    func popToRoot() {
        perform { [weak self] in _ = self?.nav?.popToRootViewController(animated: false) }
    }

    func replaceTop(route: String) {
        perform { [weak self] in self?.nav?.replaceTop(route) }
    }

    func topRoute() -> String? {
        IosNav.shared.routeOf(page: nav?.topViewController)
    }

    func closeGuide() {
        root?.closeGuide()
    }

    /// ينتقل إلى التبويب ويرجع إلى أوله (بحركة إن كان هو الظاهر)
    private func switchTo(_ route: String, closeCurrent: Bool) {
        guard let i = routes.firstIndex(of: route), let list = viewControllers, i < list.count else { return }
        let previous = nav
        let target = list[i] as? PageNavController
        let same = selectedIndex == i
        if !same { selectedIndex = i }
        if closeCurrent, let p = previous, p !== target { _ = p.popToRootViewController(animated: false) }
        _ = target?.popToRootViewController(animated: same && view.window != nil)
        setNeedsStatusBarAppearanceUpdate()
    }

    /// انتقال جارٍ (فتح أو رجوع أو سحب الحافة): الطلب ينتظر انتهاءه، فلا يتداخل انتقالان
    private func perform(_ action: @escaping () -> Void) {
        queue.append(action)
        drain()
    }

    private func drain() {
        while !queue.isEmpty {
            if let running = nav?.transitionCoordinator ?? transitionCoordinator {
                _ = running.animate(alongsideTransition: nil, completion: { [weak self] _ in
                    DispatchQueue.main.async { self?.drain() }
                })
                return
            }
            let action = queue.removeFirst()
            action()
        }
    }

    // MARK: UITabBarControllerDelegate

    func tabBarController(_ tabBarController: UITabBarController, shouldSelect viewController: UIViewController) -> Bool {
        // لا تبديل أثناء انتقال في التبويب الظاهر
        if nav?.transitionCoordinator != nil { return false }
        if viewController !== selectedViewController {
            haptic.selectionChanged()
            haptic.prepare()
        }
        return true
    }

    func tabBarController(_ tabBarController: UITabBarController, didSelect viewController: UIViewController) {
        setNeedsStatusBarAppearanceUpdate()
    }

    /// تبديل التبويب: تلاشٍ سريع
    func tabBarController(_ tabBarController: UITabBarController, animationControllerForTransitionFrom fromVC: UIViewController,
                          to toVC: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        FadeAnimator(duration: 0.18, reverse: false)
    }
}

/// تنقل تبويب واحد: شريط التنقل مخفي (الشاشات ترسم رؤوسها وأزرار الرجوع بنفسها)، والرجوع بسحب الحافة يتبع الإصبع
final class PageNavController: UINavigationController, UINavigationControllerDelegate {
    /// اتجاه لغة التطبيق: منه حافة الرجوع
    var rtl = false {
        didSet { if isViewLoaded { applyDirection() } }
    }

    /// سحب الحافتين: الرجوع قبل iOS 18 (ومع «تقليل الحركة»)، والرجوع الداخلي في الصفحة دائماً
    private var pans: [UIScreenEdgePanGestureRecognizer] = []
    /// مندوب السحب (كائن مستقل: لا يمس ما قد ينفّذه UINavigationController نفسه من دوال المندوب)
    private let gate = GestureGate()
    private var interactive: UIPercentDrivenInteractiveTransition?
    private var innerSwipe = false
    /// موضع اللمس الذي فُتحت منه كل صفحة (نقاط النافذة)، ومنظر التكبير الصغير عنده (iOS 18)
    private var origins: [ObjectIdentifier: CGPoint] = [:]
    private var markers: [ObjectIdentifier: UIView] = [:]

    override func viewDidLoad() {
        super.viewDidLoad()
        setNavigationBarHidden(true, animated: false)
        delegate = self
        gate.shouldBegin = { [weak self] g in self?.shouldBegin(g) ?? false }
        gate.waits = { [weak self] g, other in self?.waits(g, for: other) ?? false }
        // شريط التنقل مخفي: سحب النظام يحتاج مندوباً يسمح به
        interactivePopGestureRecognizer?.delegate = gate
        for edge in [UIRectEdge.left, UIRectEdge.right] {
            let pan = UIScreenEdgePanGestureRecognizer(target: self, action: #selector(edgePan(_:)))
            pan.edges = edge
            pan.delegate = gate
            view.addGestureRecognizer(pan)
            pans.append(pan)
        }
        applyDirection()
    }

    private func applyDirection() {
        let attribute: UISemanticContentAttribute = rtl ? .forceRightToLeft : .forceLeftToRight
        view.semanticContentAttribute = attribute
        navigationBar.semanticContentAttribute = attribute
    }

    override var childForStatusBarStyle: UIViewController? { topViewController }
    override var childForStatusBarHidden: UIViewController? { topViewController }

    // MARK: التنقل

    /// يفتح صفحة فوق الظاهرة (لا مرتين: نقرتان سريعتان على بطاقة)
    func open(_ route: String) {
        if IosNav.shared.routeOf(page: topViewController) == route { return }
        pushViewController(page(route), animated: true)
    }

    func back() {
        guard viewControllers.count > 1 else { return }
        _ = popViewController(animated: true)
    }

    /// الصفحة الجديدة مكان العليا (نتيجة من البحث): تنمو فوقها ثم تُزال التي تحتها
    func replaceTop(_ route: String) {
        guard viewControllers.count > 1 else { return open(route) }
        var list = viewControllers
        list[list.count - 1] = page(route)
        setViewControllers(list, animated: true)
    }

    private func page(_ route: String) -> UIViewController {
        let page = IosNav.shared.page(route: route, tab: false)
        page.hidesBottomBarWhenPushed = true
        let id = ObjectIdentifier(page)
        origins[id] = touchPoint()
        if #available(iOS 18.0, *), !UIAccessibility.isReduceMotionEnabled {
            // انتقال التكبير من النظام: تنمو الصفحة من منظر شفاف صغير عند موضع اللمس، وتصغر إليه عند الرجوع،
            // والرجوع بسحب الحافة يتبع الإصبع. السحب لأسفل والقرص لا يبدآنه (المحتوى نفسه يُمرَّر ويُقرَص)
            let options = UIViewController.Transition.ZoomOptions()
            options.interactiveDismissShouldBegin = { [weak page] context in
                guard let page = page else { return false }
                if IosNav.shared.hasInnerBack(page: page) { return false }
                let width = page.view.bounds.width
                return context.location.x < 44 || context.location.x > width - 44
            }
            page.preferredTransition = .zoom(options: options) { [weak self, weak page] context in
                guard let self = self, let page = page else { return nil }
                return self.marker(for: page, in: context.sourceViewController)
            }
        }
        return page
    }

    /// موضع آخر لمس، وإلا منتصف الشاشة (رابط أو اختصار)
    private func touchPoint() -> CGPoint {
        if let t = IosNav.shared.recentTouch() { return CGPoint(x: t.x, y: t.y) }
        let b = view.window?.bounds ?? UIScreen.main.bounds
        return CGPoint(x: b.midX, y: b.midY)
    }

    private func origin(of page: UIViewController) -> CGPoint {
        origins[ObjectIdentifier(page)] ?? touchPoint()
    }

    /// منظر شفاف صغير في الصفحة التي تحت [page] عند موضع اللمس الذي فُتحت منه: منه تنمو وإليه تصغر
    private func marker(for page: UIViewController, in source: UIViewController) -> UIView? {
        guard let host = source.view else { return nil }
        let id = ObjectIdentifier(page)
        let m: UIView
        if let existing = markers[id] {
            m = existing
        } else {
            m = UIView()
            m.backgroundColor = .clear
            m.isUserInteractionEnabled = false
            markers[id] = m
        }
        if m.superview !== host {
            m.removeFromSuperview()
            host.addSubview(m)
        }
        let p = host.convert(origin(of: page), from: nil)
        m.frame = CGRect(x: p.x - 28, y: p.y - 28, width: 56, height: 56)
        return m
    }

    private func usesSystemZoom(_ page: UIViewController) -> Bool {
        if #available(iOS 18.0, *) {
            return page.preferredTransition != nil && !UIAccessibility.isReduceMotionEnabled
        }
        return false
    }

    // MARK: UINavigationControllerDelegate

    func navigationController(_ navigationController: UINavigationController, animationControllerFor operation: UINavigationController.Operation,
                              from fromVC: UIViewController, to toVC: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        guard operation != .none else { return nil }
        let page = operation == .push ? toVC : fromVC
        // «تقليل الحركة»: تلاشٍ بلا تكبير ولا انزلاق
        if UIAccessibility.isReduceMotionEnabled { return FadeAnimator(duration: 0.3, reverse: operation == .pop) }
        // iOS 18: انتقال التكبير من النظام (preferredTransition)
        if usesSystemZoom(page) { return nil }
        return ZoomAnimator(push: operation == .push, origin: origin(of: page))
    }

    func navigationController(_ navigationController: UINavigationController,
                              interactionControllerFor animationController: UIViewControllerAnimatedTransitioning) -> UIViewControllerInteractiveTransitioning? {
        interactive
    }

    func navigationController(_ navigationController: UINavigationController, didShow viewController: UIViewController, animated: Bool) {
        // مناظر التكبير ومواضع اللمس للصفحات التي خرجت
        let alive = Set(viewControllers.map { ObjectIdentifier($0) })
        for (id, m) in markers where !alive.contains(id) { m.removeFromSuperview() }
        markers = markers.filter { alive.contains($0.key) }
        origins = origins.filter { alive.contains($0.key) }
    }

    // MARK: سحب الحافة

    private func shouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        guard transitionCoordinator == nil, let top = topViewController else { return false }
        let inner = IosNav.shared.hasInnerBack(page: top)
        if gestureRecognizer === interactivePopGestureRecognizer {
            return !inner && viewControllers.count > 1 && usesSystemZoom(top)
        }
        if pans.contains(where: { $0 === gestureRecognizer }) {
            return inner || (viewControllers.count > 1 && !usesSystemZoom(top))
        }
        return true
    }

    /// قوائم التمرير (والمصحف الأفقي) تنتظر أن يفشل سحب الحافة: لمسة تبدأ من الحافة رجوعٌ لا تمرير
    private func waits(_ gestureRecognizer: UIGestureRecognizer, for otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        let mine = gestureRecognizer === interactivePopGestureRecognizer || pans.contains(where: { $0 === gestureRecognizer })
        return mine && otherGestureRecognizer.view is UIScrollView
    }

    @objc private func edgePan(_ g: UIScreenEdgePanGestureRecognizer) {
        let width = max(view.bounds.width, 1)
        let sign: CGFloat = g.edges == .left ? 1 : -1
        let progress = min(max(g.translation(in: view).x * sign / width, 0), 1)
        let velocity = g.velocity(in: view).x * sign
        switch g.state {
        case .began:
            // رجوع داخلي (صفحة فرعية، وضع التركيز): تتولاه الصفحة نفسها حين يكتمل السحب
            if let top = topViewController, IosNav.shared.hasInnerBack(page: top) {
                innerSwipe = true
                return
            }
            innerSwipe = false
            let t = UIPercentDrivenInteractiveTransition()
            t.completionCurve = .easeOut
            interactive = t
            if popViewController(animated: true) == nil { interactive = nil }
        case .changed:
            interactive?.update(progress)
        case .ended, .cancelled, .failed:
            let complete = g.state == .ended && velocity > -200 && (progress > 0.3 || velocity > 500)
            if innerSwipe {
                innerSwipe = false
                if complete, let top = topViewController { IosNav.shared.innerBack(page: top) }
                return
            }
            guard let t = interactive else { return }
            interactive = nil
            if complete { t.finish() } else { t.cancel() }
        default:
            break
        }
    }
}

/// مندوب سحب الحافة لـ PageNavController
private final class GestureGate: NSObject, UIGestureRecognizerDelegate {
    var shouldBegin: (UIGestureRecognizer) -> Bool = { _ in true }
    var waits: (UIGestureRecognizer, UIGestureRecognizer) -> Bool = { _, _ in false }

    func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        shouldBegin(gestureRecognizer)
    }

    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldBeRequiredToFailBy otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        waits(gestureRecognizer, otherGestureRecognizer)
    }
}

// MARK: الحركات

/// فتح صفحة قبل iOS 18: تتراجع السابقة (تصغر وتعتم بزوايا مستديرة فوق خلفية سوداء)، وتنمو الجديدة من موضع اللمس
/// حين يُرسم أول إطار منها. الرجوع عكسه: تصغر الصفحة نحو الموضع الذي فُتحت منه وتختفي، وتعود التي تحتها إلى مكانها
final class ZoomAnimator: NSObject, UIViewControllerAnimatedTransitioning {
    private static let radius: CGFloat = 38
    /// الصفحة السابقة وهي متراجعة
    private static let backScale: CGFloat = 0.94
    private static let backAlpha: CGFloat = 0.72
    /// حجم الصفحة الجديدة حين تبدأ النمو، وحجم الخارجة حين تختفي
    private static let startScale: CGFloat = 0.88
    private static let exitScale: CGFloat = 0.86

    private let push: Bool
    /// بنقاط النافذة
    private let origin: CGPoint

    init(push: Bool, origin: CGPoint) {
        self.push = push
        self.origin = origin
        super.init()
    }

    func transitionDuration(using transitionContext: UIViewControllerContextTransitioning?) -> TimeInterval {
        push ? 0.5 : 0.44
    }

    /// تكبير بمقدار [s] حول [o] بدل منتصف الصفحة
    private static func zoom(_ s: CGFloat, around o: CGPoint, in bounds: CGRect) -> CGAffineTransform {
        CGAffineTransform(a: s, b: 0, c: 0, d: s, tx: (1 - s) * (o.x - bounds.midX), ty: (1 - s) * (o.y - bounds.midY))
    }

    func animateTransition(using ctx: UIViewControllerContextTransitioning) {
        guard let to = ctx.view(forKey: .to), let from = ctx.view(forKey: .from), let toVC = ctx.viewController(forKey: .to) else {
            ctx.completeTransition(!ctx.transitionWasCancelled)
            return
        }
        let box = ctx.containerView
        let background = box.backgroundColor
        box.backgroundColor = .black
        to.frame = ctx.finalFrame(for: toVC)
        let o = box.convert(origin, from: nil)
        let bounds = box.bounds
        let duration = transitionDuration(using: ctx)
        let interactive = ctx.isInteractive
        // [moving] تنمو أو تصغر، و [still] تتراجع أو تعود
        let moving = push ? to : from
        let still = push ? from : to
        if push { box.addSubview(to) } else { box.insertSubview(to, belowSubview: from) }
        for v in [to, from] {
            v.layer.cornerCurve = .continuous
            v.clipsToBounds = true
        }
        let back = CGAffineTransform(scaleX: Self.backScale, y: Self.backScale)
        if push {
            moving.transform = Self.zoom(Self.startScale, around: o, in: bounds)
            moving.layer.cornerRadius = Self.radius
            moving.alpha = 0
        } else {
            still.transform = back
            still.alpha = Self.backAlpha
            still.layer.cornerRadius = Self.radius
        }
        let finish: (Bool) -> Void = { _ in
            for v in [to, from] {
                v.transform = .identity
                v.alpha = 1
                v.layer.cornerRadius = 0
                v.clipsToBounds = false
            }
            box.backgroundColor = background
            let done = !ctx.transitionWasCancelled
            if !done { to.removeFromSuperview() }
            ctx.completeTransition(done)
        }
        if interactive {
            // سحب الحافة: كل شيء خطي يتبع الإصبع
            UIView.animate(withDuration: duration, delay: 0, options: [.curveLinear], animations: {
                still.transform = .identity
                still.alpha = 1
                still.layer.cornerRadius = 0
                moving.transform = Self.zoom(Self.exitScale, around: o, in: bounds)
                moving.layer.cornerRadius = Self.radius
                moving.alpha = 0
            }, completion: finish)
            return
        }
        if push {
            // تتراجع السابقة فوراً، وتنمو الجديدة حين يُرسم أول إطار منها
            UIView.animate(withDuration: duration, delay: 0, usingSpringWithDamping: 1, initialSpringVelocity: 0, options: [.curveEaseOut], animations: {
                still.transform = back
                still.alpha = Self.backAlpha
                still.layer.cornerRadius = Self.radius
            }, completion: nil)
            FrameWaiter(maxWait: 0.5) {
                UIView.animate(withDuration: 0.22, delay: 0, options: [.curveEaseOut], animations: { moving.alpha = 1 }, completion: nil)
                UIView.animate(withDuration: duration, delay: 0, usingSpringWithDamping: 1, initialSpringVelocity: 0, options: [.beginFromCurrentState], animations: {
                    moving.transform = .identity
                    moving.layer.cornerRadius = 0
                }, completion: finish)
            }.start()
        } else {
            UIView.animate(withDuration: duration, delay: 0, usingSpringWithDamping: 1, initialSpringVelocity: 0, options: [.beginFromCurrentState], animations: {
                still.transform = .identity
                still.alpha = 1
                still.layer.cornerRadius = 0
                moving.transform = Self.zoom(Self.exitScale, around: o, in: bounds)
                moving.layer.cornerRadius = Self.radius
            }, completion: finish)
            UIView.animate(withDuration: 0.26, delay: 0.05, options: [.curveEaseIn], animations: { moving.alpha = 0 }, completion: nil)
        }
    }
}

/// تلاشٍ: تبديل التبويب، والتنقل كله مع «تقليل الحركة». [reverse]: الصفحة الخارجة تخبو فوق التي تحتها (الرجوع)
final class FadeAnimator: NSObject, UIViewControllerAnimatedTransitioning {
    private let duration: TimeInterval
    private let reverse: Bool

    init(duration: TimeInterval, reverse: Bool) {
        self.duration = duration
        self.reverse = reverse
        super.init()
    }

    func transitionDuration(using transitionContext: UIViewControllerContextTransitioning?) -> TimeInterval {
        duration
    }

    func animateTransition(using ctx: UIViewControllerContextTransitioning) {
        guard let to = ctx.view(forKey: .to), let toVC = ctx.viewController(forKey: .to) else {
            ctx.completeTransition(!ctx.transitionWasCancelled)
            return
        }
        let from = ctx.view(forKey: .from)
        let box = ctx.containerView
        to.frame = ctx.finalFrame(for: toVC)
        if reverse, let from = from {
            box.insertSubview(to, belowSubview: from)
        } else {
            to.alpha = 0
            box.addSubview(to)
        }
        let reverse = self.reverse
        UIView.animate(withDuration: duration, delay: 0, options: ctx.isInteractive ? [.curveLinear] : [.curveEaseOut], animations: {
            if reverse { from?.alpha = 0 } else { to.alpha = 1 }
        }, completion: { _ in
            from?.alpha = 1
            to.alpha = 1
            let done = !ctx.transitionWasCancelled
            if !done { to.removeFromSuperview() }
            ctx.completeTransition(done)
        })
    }
}

/// ينتظر أن تُرسم الصفحة الجديدة: إطارات الشاشة تتوالى بلا تأخر (ثلاثة على الأقل، والأخير في وقته)،
/// أو [maxWait] ثوانٍ على الأكثر، ثم ينفذ ما بعده مرة واحدة. يبقى حياً ما دام ينتظر (CADisplayLink يمسكه)
private final class FrameWaiter: NSObject {
    private let maxWait: CFTimeInterval
    private var then: (() -> Void)?
    private var link: CADisplayLink?
    private var frames = 0
    private var last: CFTimeInterval = 0
    private let began = CACurrentMediaTime()

    init(maxWait: CFTimeInterval, then: @escaping () -> Void) {
        self.maxWait = maxWait
        self.then = then
        super.init()
    }

    func start() {
        let l = CADisplayLink(target: self, selector: #selector(tick(_:)))
        l.add(to: .main, forMode: .common)
        link = l
    }

    @objc private func tick(_ sender: CADisplayLink) {
        let dt = frames == 0 ? 1 : sender.timestamp - last
        last = sender.timestamp
        frames += 1
        if (frames >= 3 && dt < 0.034) || CACurrentMediaTime() - began > maxWait {
            link?.invalidate()
            link = nil
            let next = then
            then = nil
            next?()
        }
    }
}
