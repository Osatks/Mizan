import SwiftUI
import UIKit
import WidgetKit
import Shared

/// ميزان في iOS: الواجهة كلها من Compose Multiplatform (الوحدة المشتركة مع أندرويد)
@main
struct iOSApp: App {
    @Environment(\.scenePhase) var phase

    init() {
        WidgetSync.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
                // لمس أداة: mizan://open?route=adhkar&section=morning
                .onOpenURL { url in WidgetSync.open(url) }
        }
        .onChange(of: phase) { p in
            // ما تغير في التطبيق (الموقع واللغة واللون) يصل إلى الأدوات عند الخروج منه
            if p == .background { WidgetSync.shared.refreshInBackground() }
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
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
