import UIKit
import WidgetKit
import Shared

/// ميزان في iOS: الواجهة كلها من Compose Multiplatform (الوحدة المشتركة مع أندرويد).
/// متحكم Kotlin (MainViewController) هو جذر النافذة مباشرة، لا داخل SwiftUI: فيسأله النظام عن شريط الحالة
/// (نص فاتح فوق الرؤوس الخضراء، وإخفاؤه في وضع التركيز في المصحف)، وهو ما يتجاهله UIHostingController.
///
/// يعمل بدورة المشاهد (UIScene) إن وُجد UIApplicationSceneManifest في Info.plist، وإلا بنافذة التطبيق القديمة.
@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        WidgetSync.shared.start()
        // ميزان على Apple Watch: اللقطة نفسها إلى الساعة (WatchSync.swift)
        WatchSync.shared.start()
        // الأنشطة الحية و«يُشغَّل الآن» والاختصارات و Spotlight (LiveActivities.swift)
        LiveActivities.shared.start()
        // iOS 26: شريط التبويبات من النظام بزجاج Liquid Glass بدل شريط Compose (LiquidTabBar.swift)
        LiquidTabBar.install()
        // صفحات مصحف المدينة يقلّبها ويرسمها النظام (UICollectionView و Core Text) بدل Compose (NativeMushaf.swift)
        NativeMushafReader.install()
        // والرئيسية بواجهة SwiftUI (NativeHome.swift)
        NativeHomeScreen.install()
        // وشاشة المواقيت بطراز الرئيسية (NativePrayer.swift)
        NativePrayerScreen.install()
        // والأذكار (NativeAdhkar.swift)
        NativeAdhkarScreen.install()
        // وتبويب الأدوات (NativeTools.swift)
        NativeToolsScreen.install()
        // وتبويب القرآن والإعدادات، وكل أداة: القبلة والمسبحة والزكاة والمواريث والتاريخ والعمر والمناسبات والفطر والفدية
        NativeQuranScreen.install()
        NativeSettingsScreen.install()
        NativeQiblaScreen.install()
        NativeTasbihScreen.install()
        NativeZakatScreen.install()
        NativeInheritanceScreen.install()
        NativeDatesScreen.install()
        NativeFitrFidyaScreen.install()
        // والحديث والبحث ومكتبة التفاسير والأذان والدليل والتراخيص، وأوراق اللغة والمدينة
        NativeHadithScreen.install()
        NativeSearchScreen.install()
        NativeTafsirsScreen.install()
        NativeAdhanScreen.install()
        NativeGuideScreen.install()
        NativeLicensesScreen.install()
        NativeSheetsUI.install()
        if !AppDelegate.usesScenes {
            let w = UIWindow(frame: UIScreen.main.bounds)
            w.rootViewController = MainViewControllerKt.MainViewController()
            w.makeKeyAndVisible()
            LaunchCurtain.cover(w)
            window = w
        }
        return true
    }

    // نتيجة من بحث iPhone (سورة في Spotlight)
    func application(_ application: UIApplication, continue userActivity: NSUserActivity,
                     restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
        SpotlightLink.open(userActivity)
    }

    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        let config = UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
        config.delegateClass = SceneDelegate.self
        return config
    }

    // بلا مشاهد: روابط الأدوات والخروج إلى الخلفية تصل إلى التطبيق نفسه
    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool {
        WidgetSync.open(url)
        return true
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        if !AppDelegate.usesScenes { WidgetSync.shared.refreshInBackground() }
    }

    static var usesScenes: Bool {
        Bundle.main.object(forInfoDictionaryKey: "UIApplicationSceneManifest") != nil
    }
}

final class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?

    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        guard let windowScene = scene as? UIWindowScene else { return }
        // فحص أزرار الأنشطة الحية على المحاكي (LiveCheck.swift): بلا واجهة Compose
        if #available(iOS 17.0, *), let check = LiveCheck.action {
            let w = UIWindow(windowScene: windowScene)
            w.rootViewController = LiveCheck.viewController(check)
            w.makeKeyAndVisible()
            window = w
            Task { @MainActor in await LiveCheck.run(check) }
            return
        }
        let w = UIWindow(windowScene: windowScene)
        w.rootViewController = MainViewControllerKt.MainViewController()
        w.makeKeyAndVisible()
        LaunchCurtain.cover(w)
        window = w
        // التطبيق فُتح بلمس أداة وهو مغلق
        if let url = connectionOptions.urlContexts.first?.url { WidgetSync.open(url) }
        if let activity = connectionOptions.userActivities.first { SpotlightLink.open(activity) }
    }

    // نتيجة من بحث iPhone (سورة في Spotlight) والتطبيق يعمل
    func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
        SpotlightLink.open(userActivity)
    }

    // لمس أداة: mizan://open?route=adhkar&section=morning
    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        if let url = URLContexts.first?.url { WidgetSync.open(url) }
    }

    // ما تغير في التطبيق (الموقع واللغة واللون) يصل إلى الأدوات عند الخروج منه
    func sceneDidEnterBackground(_ scene: UIScene) {
        WidgetSync.shared.refreshInBackground()
    }
}

/// يربط Kotlin بـ WidgetKit: Kotlin يكتب اللقطة ثم يرسل MizanWidgetsReload، فنطلب إعادة رسم الأدوات
final class WidgetSync {
    static let shared = WidgetSync()
    private var task: UIBackgroundTaskIdentifier = .invalid

    func start() {
        NotificationCenter.default.addObserver(forName: Notification.Name("MizanWidgetsReload"), object: nil, queue: .main) { [weak self] note in
            // اللقطة نفسها تُكتب أيضاً في سلسلة المفاتيح المشتركة، لتثبيتات بلا App Group (Sideloadly بحساب مجاني)
            if let json = note.object as? String, let data = json.data(using: .utf8) { SharedKeychain.write(data) }
            if let json = note.object as? String { WatchSync.shared.send(snapshot: json) }
            WidgetCenter.shared.reloadAllTimelines()
            self?.endTask()
        }
    }

    /// كتابة اللقطة تحتاج لحظة بعد الخروج من التطبيق: نطلب من النظام مهلة قصيرة
    func refreshInBackground() {
        endTask()
        task = UIApplication.shared.beginBackgroundTask(withName: "MizanWidgets") { [weak self] in self?.endTask() }
        IosWidgets.shared.refresh()
        DispatchQueue.main.asyncAfter(deadline: .now() + 10) { [weak self] in self?.endTask() }
    }

    private func endTask() {
        if task != .invalid {
            UIApplication.shared.endBackgroundTask(task)
            task = .invalid
        }
    }

    static func open(_ url: URL) {
        guard url.scheme == "mizan",
              let items = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems,
              let route = items.first(where: { $0.name == "route" })?.value else { return }
        let section = items.first(where: { $0.name == "section" })?.value
        IosWidgets.shared.open(route: route, section: section)
    }
}
