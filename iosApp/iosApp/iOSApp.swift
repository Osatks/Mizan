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
        if !AppDelegate.usesScenes {
            let w = UIWindow(frame: UIScreen.main.bounds)
            w.rootViewController = MainViewControllerKt.MainViewController()
            w.makeKeyAndVisible()
            window = w
        }
        return true
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
        let w = UIWindow(windowScene: windowScene)
        w.rootViewController = MainViewControllerKt.MainViewController()
        w.makeKeyAndVisible()
        window = w
        // التطبيق فُتح بلمس أداة وهو مغلق
        if let url = connectionOptions.urlContexts.first?.url { WidgetSync.open(url) }
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
