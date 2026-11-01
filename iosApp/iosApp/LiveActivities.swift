import Foundation
import UIKit
import BackgroundTasks
import ActivityKit
import Shared

/// الأنشطة الحية في التطبيق: Kotlin (IosLive) يرسل حالها بإشعارات فيها JSON، وهنا تُبدأ وتُحدَّث وتُنهى:
///  - MizanLivePrayer: العدّ التنازلي للصلاة القادمة (iOS 16.2)، يبدأ عند مغادرة التطبيق قبل الصلاة بأقل من ثلاث ساعات
///    إن فعّله المستخدم، ويصير «حان الآن» عند دخول الوقت، وينتهي بعده بعشر دقائق.
///  - MizanLiveRecitation: «يُتلى الآن» ما دامت التلاوة.
/// وتصل إليه النوايا المشتركة مع الأداة (فتح شاشة، وزر التلاوة) فيمررها إلى Kotlin.
final class LiveActivities {
    static let shared = LiveActivities()
    /// مهمة الخلفية التي تنهي العدّ التنازلي بعد دخول الوقت (BGTaskSchedulerPermittedIdentifiers في Info.plist)
    static let refreshTask = "com.mizan.almuslim.live"

    private var started = false

    /// من application(_:didFinishLaunchingWithOptions:): مهام الخلفية تُسجَّل قبل انتهاء الإقلاع
    func start() {
        guard !started else { return }
        started = true
        let nc = NotificationCenter.default
        // المراقبون بلا طابور: يُنفَّذون فوراً على خيط Kotlin الرئيسي، فيُبدأ النشاط والتطبيق ما زال في المقدمة
        nc.addObserver(forName: Notification.Name("MizanLivePrayer"), object: nil, queue: nil) { note in
            guard let json = note.object as? String, let message = LiveJSON.prayer(json) else { return }
            if #available(iOS 16.2, *) { PrayerActivity.apply(message) }
        }
        nc.addObserver(forName: Notification.Name("MizanLiveRecitation"), object: nil, queue: nil) { note in
            guard let json = note.object as? String, let state = LiveJSON.recitation(json) else { return }
            if #available(iOS 16.2, *) { RecitationActivity.apply(state) }
        }
        nc.addObserver(forName: MizanBridge.toggleRecitation, object: nil, queue: .main) { _ in
            IosLive.shared.toggleRecitation()
        }
        nc.addObserver(forName: MizanBridge.openRoute, object: nil, queue: .main) { note in
            guard let route = note.userInfo?["route"] as? String else { return }
            IosLive.shared.open(route: route, section: note.userInfo?["section"] as? String)
        }
        // قد يطفئ المستخدم الأنشطة الحية لميزان في الإعدادات ثم يعود
        nc.addObserver(forName: UIApplication.didBecomeActiveNotification, object: nil, queue: .main) { _ in
            IosLive.shared.availability(supported: Self.supported, systemOff: Self.systemOff)
        }
        _ = BGTaskScheduler.shared.register(forTaskWithIdentifier: Self.refreshTask, using: nil) { task in
            guard #available(iOS 16.2, *) else { task.setTaskCompleted(success: true); return }
            let work = Task { @MainActor in
                await PrayerActivity.endExpired()
                task.setTaskCompleted(success: true)
            }
            task.expirationHandler = { work.cancel() }
        }
        IosLive.shared.start(supported: Self.supported, systemOff: Self.systemOff)
    }

    static var supported: Bool {
        guard #available(iOS 16.2, *) else { return false }
        // iPad: لا يُعرض الإعداد إلا إن كانت الأنشطة الحية متاحة فيه
        if UIDevice.current.userInterfaceIdiom != .phone { return ActivityAuthorizationInfo().areActivitiesEnabled }
        return true
    }

    static var systemOff: Bool {
        if #available(iOS 16.2, *) { return !ActivityAuthorizationInfo().areActivitiesEnabled }
        return false
    }

    /// تطلب من النظام إيقاظ التطبيق بعد [date] لإنهاء ما انقضى (يقرر النظام الموعد الفعلي)
    static func scheduleRefresh(after date: Date) {
        let request = BGAppRefreshTaskRequest(identifier: refreshTask)
        request.earliestBeginDate = date
        try? BGTaskScheduler.shared.submit(request)
    }
}

/// العدّ التنازلي للصلاة القادمة
@available(iOS 16.2, *)
enum PrayerActivity {
    /// بعد دخول الوقت بعشر دقائق ينتهي النشاط
    static let grace: TimeInterval = 600
    /// صلاة بدأنا نشاطها، وصلاة أغلق المستخدم نشاطها (لا يُعاد لها)
    private static let startedKey = "live.prayer.started"
    private static let dismissedKey = "live.prayer.dismissed"

    private static var alive: [Activity<PrayerActivityAttributes>] {
        Activity<PrayerActivityAttributes>.activities.filter { $0.activityState == .active || $0.activityState == .stale }
    }

    static func apply(_ message: PrayerLiveMessage) {
        let defaults = UserDefaults.standard
        guard message.on else {
            defaults.removeObject(forKey: startedKey)
            defaults.removeObject(forKey: dismissedKey)
            end(alive)
            return
        }
        let now = Date()
        let expired = alive.filter { $0.content.state.at.addingTimeInterval(grace) <= now }
        end(expired)
        let current = alive.filter { a in !expired.contains { $0.id == a.id } }
        guard let state = message.prayer else { return }
        let content = ActivityContent(state: state, staleDate: state.at, relevanceScore: 50)
        if let same = current.first(where: { $0.attributes.id == state.id }) {
            if same.content.state != state { Task { await same.update(content) } }
            end(current.filter { $0.id != same.id })
            scheduleEnd(state.at)
            return
        }
        // بدأناه لهذه الصلاة ولم يعد ظاهراً: أغلقه المستخدم من شاشة القفل، فلا يُعاد
        if defaults.string(forKey: startedKey) == state.id {
            defaults.set(state.id, forKey: dismissedKey)
            defaults.removeObject(forKey: startedKey)
        }
        // نشاط جديد عند مغادرة التطبيق فقط (والتطبيق ما زال في المقدمة)
        guard message.start == true, defaults.string(forKey: dismissedKey) != state.id,
              ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        end(current)
        do {
            _ = try Activity.request(attributes: PrayerActivityAttributes(id: state.id), content: content, pushType: nil)
            defaults.set(state.id, forKey: startedKey)
            scheduleEnd(state.at)
        } catch {
            // لا إذن، أو التطبيق في الخلفية: يُعاد في المغادرة التالية
        }
    }

    /// ينهي النشاط بعد دخول الوقت بعشر دقائق: بمؤقت إن بقي التطبيق يعمل، وبمهمة خلفية إن عُلّق
    private static func scheduleEnd(_ at: Date) {
        let end = at.addingTimeInterval(grace)
        DispatchQueue.main.asyncAfter(deadline: .now() + max(1, end.timeIntervalSinceNow) + 1) {
            Task { await endExpired() }
        }
        LiveActivities.scheduleRefresh(after: end)
    }

    @MainActor
    static func endExpired() async {
        let now = Date()
        for a in alive where a.content.state.at.addingTimeInterval(grace) <= now {
            if UserDefaults.standard.string(forKey: startedKey) == a.attributes.id {
                UserDefaults.standard.removeObject(forKey: startedKey)
            }
            await a.end(nil, dismissalPolicy: .immediate)
        }
    }

    private static func end(_ list: [Activity<PrayerActivityAttributes>]) {
        for a in list {
            if UserDefaults.standard.string(forKey: startedKey) == a.attributes.id {
                UserDefaults.standard.removeObject(forKey: startedKey)
            }
            Task { await a.end(nil, dismissalPolicy: .immediate) }
        }
    }
}

/// «يُتلى الآن»: يتبع التلاوة، ولا يُعاد إن أغلقه المستخدم حتى تبدأ تلاوة جديدة
@available(iOS 16.2, *)
enum RecitationActivity {
    private static var ours: String?
    private static var suppressed = false

    static func apply(_ state: RecitationLiveState?) {
        let all = Activity<RecitationActivityAttributes>.activities
        let alive = all.filter { $0.activityState == .active || $0.activityState == .stale }
        guard let state = state else {
            ours = nil
            suppressed = false
            for a in alive { Task { await a.end(nil, dismissalPolicy: .immediate) } }
            return
        }
        let content = ActivityContent(state: state, staleDate: nil, relevanceScore: 100)
        if let a = alive.first {
            if a.content.state != state { Task { await a.update(content) } }
            for b in alive.dropFirst() { Task { await b.end(nil, dismissalPolicy: .immediate) } }
            return
        }
        // كان لهذه التلاوة نشاط واختفى: أغلقه المستخدم
        if ours != nil {
            ours = nil
            suppressed = true
        }
        guard !suppressed, ActivityAuthorizationInfo().areActivitiesEnabled,
              UIApplication.shared.applicationState != .background else { return }
        if let a = try? Activity.request(attributes: RecitationActivityAttributes(), content: content, pushType: nil) {
            ours = a.id
        }
    }
}
