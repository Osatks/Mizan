import SwiftUI
import Shared

/// ميزان في iOS: الواجهة كلها من Compose Multiplatform (الوحدة المشتركة مع أندرويد)
@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
