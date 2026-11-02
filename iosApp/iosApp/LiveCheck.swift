import UIKit
import ActivityKit
import Shared

/// فحص أزرار الأنشطة الحية على المحاكي (ios-live-check.yml). يُشغَّل التطبيق بوسيط `-mizan.live_check <action>`
/// فلا تُبنى واجهة Compose (كما يعمل التطبيق في الخلفية لنية زر الإجراء أو زر مركز التحكم)، ويُنفَّذ الزر نفسه الذي
/// تنفذه النوايا، ويُكتب ما حدث في Documents/live-check.json: النتيجة، وخطوات التنفيذ، وحال كل نشاط بعده.
/// بعد إنهاء التطبيق تبقى الأنشطة ظاهرة فتُلتقط الجزيرة الديناميكية. لا أثر له في الاستعمال العادي.
@available(iOS 17.0, *)
enum LiveCheck {
    static var action: String? {
        let a = ProcessInfo.processInfo.arguments
        guard let i = a.firstIndex(of: "-mizan.live_check"), i + 1 < a.count else { return nil }
        return a[i + 1]
    }

    static func viewController(_ action: String) -> UIViewController {
        let vc = UIViewController()
        vc.view.backgroundColor = .black
        let label = UILabel()
        label.text = "live check: \(action)"
        label.textColor = .white
        label.translatesAutoresizingMaskIntoConstraints = false
        vc.view.addSubview(label)
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: vc.view.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: vc.view.centerYAnchor),
        ])
        return vc
    }

    @MainActor
    static func run(_ action: String) async {
        var result: [String: Any] = [
            "action": action,
            "enabled": ActivityAuthorizationInfo().areActivitiesEnabled,
            "before": states(),
        ]
        let outcome: LiveOutcome
        switch action {
        // زر الإجراء والاختصارات (نوايا التطبيق وحده)
        case "tasbih": outcome = await AppLive.button(.tasbih)
        case "adhkar": outcome = await AppLive.button(.adhkar)
        case "prayer": outcome = await AppLive.button(.prayerToggle)
        case "nextPrayer": outcome = await AppLive.button(.nextPrayer)
        // أزرار مركز التحكم وشاشة القفل والأداة (النوايا المشتركة عبر LiveRouter)
        case "tasbihControl": outcome = await LiveRouter.run(.tasbihCount)
        case "adhkarControl": outcome = await LiveRouter.run(.adhkarCount)
        case "prayerControl": outcome = await LiveRouter.run(.prayerShow)
        default: outcome = .failed("unknown action \(action)")
        }
        result["outcome"] = "\(outcome)"
        result["notes"] = LiveDebug.take()
        try? await Task.sleep(nanoseconds: 1_500_000_000)
        result["after"] = states()
        write(result)
    }

    /// حال كل نشاط ظاهر: النوع وحاله (active أو stale أو ended…) وموجز محتواه
    static func states() -> [String] {
        var out: [String] = []
        for a in Activity<TasbihActivityAttributes>.activities {
            out.append("tasbih \(a.activityState) \(a.content.state.text) \(a.content.state.countText)")
        }
        for a in Activity<AdhkarActivityAttributes>.activities {
            out.append("adhkar \(a.activityState) \(a.content.state.section) \(a.content.state.position) \(a.content.state.countText)")
        }
        for a in Activity<PrayerActivityAttributes>.activities {
            out.append("prayer \(a.activityState) \(a.content.state.key) at=\(a.content.state.at) ends=\(a.content.state.endsAt)")
        }
        return out
    }

    private static func write(_ result: [String: Any]) {
        guard let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first,
              let data = try? JSONSerialization.data(withJSONObject: result, options: [.prettyPrinted, .sortedKeys]) else { return }
        try? data.write(to: dir.appendingPathComponent("live-check.json"))
        LiveActivities.log.notice("live check \(result["action"] as? String ?? "", privacy: .public): \(result["outcome"] as? String ?? "", privacy: .public)")
    }
}
