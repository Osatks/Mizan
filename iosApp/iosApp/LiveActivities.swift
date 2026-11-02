import Foundation
import UIKit
import BackgroundTasks
import ActivityKit
import os
import Shared

/// الأنشطة الحية في التطبيق: Kotlin يرسل حالها JSON، وهنا تُبدأ وتُحدَّث وتُنهى:
///  - الصلاة القادمة (iOS 16.2): يبدأ عند مغادرة التطبيق قبل الصلاة بأقل من ثلاث ساعات إن فعّله المستخدم، أو متى شاء
///    بزر الإجراء و Siri (ActionPrayerCountdownIntent، NextPrayerIntent)؛ يصير «حان الآن» عند دخول الوقت ثم يعدّ إلى الإقامة، وينتهي بعدها.
///  - المسبحة وعدّاد الأذكار (iOS 17 لأزرارها): تبدؤها النوايا (Shared/LiveIntents.swift) من زر الإجراء ومركز التحكم
///    والأداة و Siri، وتعدّ في الحال نفسها التي تعدّ فيها الشاشة (AppLive).
/// والتلاوة لا نشاط حياً لها: «يُشغَّل الآن» (مشغّل النظام) يظهر في شاشة القفل والجزيرة الديناميكية كما في تطبيقات Apple.
/// وتصل إليه النية المشتركة مع الأداة (فتح شاشة) فيمررها إلى Kotlin.
/// ما حدث في آخر ضغطة لزر الإجراء: يُعرض سطراً مع الاختصار في نسخة الاختبار (تشخيص مؤقت)
enum LiveDebug {
    private static var notes: [String] = []
    static func note(_ s: String) { notes.append(s) }
    static func take() -> String { defer { notes = [] }; return notes.joined(separator: " · ") }
    @available(iOS 16.2, *)
    static func prayerStates() -> String {
        let a = Activity<PrayerActivityAttributes>.activities.map { "\($0.activityState)" }
        return a.isEmpty ? "none" : a.joined(separator: ",")
    }
}

final class LiveActivities {
    static let log = Logger(subsystem: "com.mizan.almuslim", category: "live")
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
        // العدّ من شاشة المسبحة: يُحدَّث النشاط الظاهر (بلا تنبيه، فالتطبيق أمام المستخدم) والأداة وزر مركز التحكم
        nc.addObserver(forName: Notification.Name("MizanTasbihState"), object: nil, queue: .main) { note in
            guard let m = LiveJSON.tasbih(note.object as? String) else { return }
            TasbihSync.saved(m.state, immediate: false)
            if #available(iOS 16.2, *) { Task { @MainActor in _ = await TasbihActivity.apply(m, start: false, alert: false) } }
        }
        // زر «على شاشة القفل» في شاشة المسبحة
        nc.addObserver(forName: Notification.Name("MizanTasbihShow"), object: nil, queue: .main) { note in
            guard let m = LiveJSON.tasbih(note.object as? String) else { return }
            if #available(iOS 16.2, *) { Task { @MainActor in _ = await TasbihActivity.apply(m, start: true, alert: false) } }
        }
        // مواقيت الصلاة في التقويم (IosCalendar في Kotlin): إضافة أو إزالة أو تجديد
        nc.addObserver(forName: Notification.Name("MizanCalendarSync"), object: nil, queue: .main) { note in
            CalendarSync.handle(note.object as? String)
        }
        // يخرج المستخدم من التطبيق: الأداة وزر مركز التحكم بآخر عدد
        nc.addObserver(forName: UIApplication.didEnterBackgroundNotification, object: nil, queue: .main) { _ in
            TasbihSync.flush()
            LiveRouter.reloadTasbih()
        }
        nc.addObserver(forName: MizanBridge.openRoute, object: nil, queue: .main) { note in
            guard let route = note.userInfo?["route"] as? String else { return }
            IosLive.shared.open(route: route, section: note.userInfo?["section"] as? String)
        }
        // قد يطفئ المستخدم الأنشطة الحية لميزان في الإعدادات ثم يعود
        nc.addObserver(forName: UIApplication.didBecomeActiveNotification, object: nil, queue: .main) { _ in
            IosLive.shared.availability(supported: Self.supported, systemOff: Self.systemOff)
            // صلاة دخل وقتها والتطبيق معلّق: تُرسم الآن «حان الآن» والعدّ إلى الإقامة
            if #available(iOS 16.2, *) { Task { @MainActor in await PrayerActivity.refreshArrived() } }
        }
        _ = BGTaskScheduler.shared.register(forTaskWithIdentifier: Self.refreshTask, using: nil) { task in
            guard #available(iOS 16.2, *) else { task.setTaskCompleted(success: true); return }
            let work = Task { @MainActor in
                await PrayerActivity.endExpired()
                await PrayerActivity.refreshArrived()
                PrayerActivity.scheduleNextRefresh()
                task.setTaskCompleted(success: true)
            }
            task.expirationHandler = { work.cancel() }
        }
        IosLive.shared.start(supported: Self.supported, systemOff: Self.systemOff)
        // القبلة بالكاميرا (ArQibla.swift)
        ArQibla.install()
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
    /// صلاة بدأنا نشاطها، وصلاة أغلق المستخدم نشاطها (لا يُعاد لها)، وصلاة أظهرها المستخدم بنفسه (زر الإجراء)
    private static let startedKey = "live.prayer.started"
    private static let dismissedKey = "live.prayer.dismissed"
    private static let forcedKey = "live.prayer.forced"

    private static var alive: [Activity<PrayerActivityAttributes>] {
        Activity<PrayerActivityAttributes>.activities.filter { $0.activityState == .active || $0.activityState == .stale }
    }

    @discardableResult
    static func apply(_ message: PrayerLiveMessage) -> LiveOutcome {
        let defaults = UserDefaults.standard
        let forced = defaults.string(forKey: forcedKey)
        guard message.on || message.force == true else {
            // الإعداد مطفأ: يبقى ما أظهره المستخدم بنفسه حتى ينتهي
            defaults.removeObject(forKey: startedKey)
            defaults.removeObject(forKey: dismissedKey)
            end(alive.filter { $0.attributes.id != forced })
            LiveDebug.note("apply: setting off, not forced")
            return .ok
        }
        let now = Date()
        let expired = alive.filter { $0.content.state.endsAt <= now }
        end(expired)
        let current = alive.filter { a in !expired.contains { $0.id == a.id } }
        guard let state = message.prayer else { return .needsPlace }
        let content = ActivityContent(state: state, staleDate: state.at, relevanceScore: 50)
        if let same = current.first(where: { $0.attributes.id == state.id }) {
            LiveDebug.note("apply: same id exists (\(same.activityState)) → updated")
            if same.content.state != state { Task { await same.update(content) } }
            end(current.filter { $0.id != same.id })
            scheduleEnd(state.endsAt)
            scheduleArrival(state.at)
            return .ok
        }
        // بدأناه لهذه الصلاة ولم يعد ظاهراً: أغلقه المستخدم من شاشة القفل، فلا يُعاد تلقائياً
        if defaults.string(forKey: startedKey) == state.id {
            defaults.set(state.id, forKey: dismissedKey)
            defaults.removeObject(forKey: startedKey)
        }
        // نشاط جديد عند مغادرة التطبيق (والتطبيق ما زال في المقدمة)، أو حين يطلبه المستخدم (زر الإجراء، Siri)
        let force = message.force == true
        guard force || (message.start == true && defaults.string(forKey: dismissedKey) != state.id),
              ActivityAuthorizationInfo().areActivitiesEnabled else {
            LiveDebug.note("apply: not started (force=\(force) start=\(message.start == true) enabled=\(ActivityAuthorizationInfo().areActivitiesEnabled))")
            return .ok
        }
        end(current)
        do {
            let started = try Activity.request(attributes: PrayerActivityAttributes(id: state.id), content: content, pushType: nil)
            LiveDebug.note("request ok \(started.activityState), ends in \(Int(state.endsAt.timeIntervalSinceNow))s, now=\(LiveDebug.prayerStates())")
            defaults.set(state.id, forKey: startedKey)
            if force {
                defaults.set(state.id, forKey: forcedKey)
                defaults.removeObject(forKey: dismissedKey)
            }
            scheduleEnd(state.endsAt)
            scheduleArrival(state.at)
            return .ok
        } catch {
            // لا إذن، أو التطبيق في الخلفية بلا نية: يُعاد في المغادرة التالية (وزر الإجراء يعرض السبب)
            LiveActivities.log.error("prayer Activity.request failed: \(String(describing: error), privacy: .public)")
            return .failed(String(describing: error))
        }
    }

    /// زر الإجراء و Siri: يُظهر العدّ التنازلي للصلاة القادمة أياً كان بُعدها، والضغطة التالية تخفيه
    @MainActor
    static func toggle() async -> LiveOutcome {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return .activitiesOff }
        guard let json = IosLive.shared.prayerNowJson(), let message = LiveJSON.prayer(json), let state = message.prayer else {
            return .needsPlace
        }
        LiveDebug.note("toggle \(state.key) before=\(LiveDebug.prayerStates())")
        if let same = alive.first(where: { $0.attributes.id == state.id }) {
            LiveDebug.note("same id visible → hidden")
            // ظاهر الآن: يُخفى، ولا يعود تلقائياً لهذه الصلاة
            UserDefaults.standard.set(state.id, forKey: dismissedKey)
            UserDefaults.standard.removeObject(forKey: startedKey)
            UserDefaults.standard.removeObject(forKey: forcedKey)
            await same.end(nil, dismissalPolicy: .immediate)
            return .ok
        }
        return apply(message)
    }

    /// «الصلاة القادمة» من زر الإجراء: يُظهر العدّ التنازلي (ولا يخفيه إن كان ظاهراً)
    @MainActor
    static func show() async -> LiveOutcome {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return .activitiesOff }
        guard let json = IosLive.shared.prayerNowJson(), let message = LiveJSON.prayer(json), let state = message.prayer else {
            return .needsPlace
        }
        LiveDebug.note("show \(state.key) before=\(LiveDebug.prayerStates())")
        if alive.contains(where: { $0.attributes.id == state.id }) { LiveDebug.note("already visible"); return .ok }
        return apply(message)
    }

    /// عند دخول الوقت والتطبيق يعمل: يُرسم النشاط من جديد («حان الآن» والعدّ إلى الإقامة) دون انتظار النظام،
    /// وكذلك كلما عاد المستخدم إلى التطبيق بعد الوقت (refreshArrived)
    private static func scheduleArrival(_ at: Date) {
        let delay = at.timeIntervalSinceNow
        guard delay > 0 else { return }
        DispatchQueue.main.asyncAfter(deadline: .now() + delay + 1) {
            Task { await refreshArrived() }
        }
        // والتطبيق في الخلفية: يوقظه النظام قرب وقت الصلاة (لا بالدقيقة) فيُرسم «حان الآن»، ثم موعد الإنهاء.
        // النظام لا يعلّم النشاط «قديماً» وحده عند staleDate (اختبار staleprobe في iosApp/Gallery)
        LiveActivities.scheduleRefresh(after: at)
    }

    /// مهمة الخلفية التالية: أقرب وقت صلاة أو إنهاء لنشاط ظاهر
    static func scheduleNextRefresh() {
        let now = Date()
        let next = alive.flatMap { [$0.content.state.at, $0.content.state.endsAt] }.filter { $0 > now }.min()
        if let next = next { LiveActivities.scheduleRefresh(after: next) }
    }

    /// النشاط الذي دخل وقته يُحدَّث بالمحتوى نفسه: يرسمه النظام من جديد فيرى isStale
    @MainActor
    static func refreshArrived() async {
        let now = Date()
        for a in alive where a.content.state.at <= now && a.content.state.endsAt > now {
            await a.update(ActivityContent(state: a.content.state, staleDate: a.content.state.at, relevanceScore: 50))
        }
    }

    /// ينهي النشاط بعد دخول الوقت بعشر دقائق أو بعد الإقامة: بمؤقت إن بقي التطبيق يعمل، وبمهمة خلفية إن عُلّق
    private static func scheduleEnd(_ end: Date) {
        DispatchQueue.main.asyncAfter(deadline: .now() + max(1, end.timeIntervalSinceNow) + 1) {
            Task { await endExpired() }
        }
        LiveActivities.scheduleRefresh(after: end)
    }

    @MainActor
    static func endExpired() async {
        let now = Date()
        for a in alive where a.content.state.endsAt <= now {
            forget(a.attributes.id)
            await a.end(nil, dismissalPolicy: .immediate)
        }
    }

    private static func forget(_ id: String) {
        let d = UserDefaults.standard
        if d.string(forKey: startedKey) == id { d.removeObject(forKey: startedKey) }
        if d.string(forKey: forcedKey) == id { d.removeObject(forKey: forcedKey) }
    }

    private static func end(_ list: [Activity<PrayerActivityAttributes>]) {
        for a in list {
            forget(a.attributes.id)
            Task { await a.end(nil, dismissalPolicy: .immediate) }
        }
    }
}

/// المسبحة على شاشة القفل وفي الجزيرة الديناميكية
@available(iOS 16.2, *)
enum TasbihActivity {
    static var current: Activity<TasbihActivityAttributes>? {
        Activity<TasbihActivityAttributes>.activities.first { $0.activityState == .active || $0.activityState == .stale }
    }

    /// يحدّث النشاط الظاهر، أو يبدؤه إن طُلب [start]. عند إتمام دورة: تنبيه قصير (صوت واهتزاز وتتسع الجزيرة)
    @MainActor
    static func apply(_ m: LiveMessage<TasbihLiveState>, start: Bool, alert: Bool) async -> LiveOutcome {
        // يبقى حتى ثماني ساعات بلا تسبيح، ثم يزيله النظام (العدد محفوظ في التطبيق)
        let content = ActivityContent(state: m.state, staleDate: Date().addingTimeInterval(8 * 3600), relevanceScore: 80)
        if let a = current {
            if alert, !m.alertTitle.isEmpty, m.event != "count", m.event != "none" {
                let config = AlertConfiguration(
                    title: LocalizedStringResource(stringLiteral: m.alertTitle),
                    body: LocalizedStringResource(stringLiteral: m.alertBody),
                    sound: .named("tasbih_round.wav")
                )
                await a.update(content, alertConfiguration: config)
            } else {
                await a.update(content)
            }
            return .ok
        }
        // أزرار النشاط نوايا iOS 17: قبله لا يُبدأ (الأداة لا ترسمه)
        guard start, #available(iOS 17.0, *) else { return .ok }
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return .activitiesOff }
        do {
            _ = try Activity.request(attributes: TasbihActivityAttributes(), content: content, pushType: nil)
            return .ok
        } catch {
            LiveActivities.log.error("Activity.request failed: \(String(describing: error), privacy: .public)")
            return .failed(String(describing: error))
        }
    }

    static func end() async {
        for a in Activity<TasbihActivityAttributes>.activities {
            await a.end(nil, dismissalPolicy: .immediate)
        }
    }
}

/// عدّاد الأذكار على شاشة القفل وفي الجزيرة الديناميكية
@available(iOS 16.2, *)
enum AdhkarActivity {
    static var current: Activity<AdhkarActivityAttributes>? {
        Activity<AdhkarActivityAttributes>.activities.first { $0.activityState == .active || $0.activityState == .stale }
    }

    /// قسم النشاط الظاهر (الصباح أو المساء): تتابعه الضغطات وإن دخل وقت القسم الآخر
    static var section: String? { current?.attributes.section }

    @MainActor
    static func apply(_ m: LiveMessage<AdhkarLiveState>, start: Bool) async -> LiveOutcome {
        let content = ActivityContent(state: m.state, staleDate: Date().addingTimeInterval(4 * 3600), relevanceScore: 70)
        if let a = current, a.attributes.section == m.state.section {
            LiveDebug.note("adhkar updated \(m.state.position) count=\(m.state.count)")
            if m.event == "done" {
                // تمّ القسم: «أتممت أذكار الصباح» مع تنبيه، ثم يبقى على شاشة القفل دقائق ويختفي
                let config = AlertConfiguration(
                    title: LocalizedStringResource(stringLiteral: m.alertTitle),
                    body: LocalizedStringResource(stringLiteral: m.state.title),
                    sound: .named("adhkar_done.wav")
                )
                await a.update(content, alertConfiguration: config)
            } else {
                await a.update(content)
            }
            return .ok
        }
        LiveDebug.note("adhkar \(m.state.section) \(m.state.position) start=\(start) existing=\(current.map { "\($0.activityState)" } ?? "none")")
        if let old = current { await old.end(nil, dismissalPolicy: .immediate) }
        guard start, #available(iOS 17.0, *) else { return .ok }
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return .activitiesOff }
        do {
            let a = try Activity.request(attributes: AdhkarActivityAttributes(section: m.state.section), content: content, pushType: nil)
            LiveDebug.note("request ok \(a.activityState)")
            return .ok
        } catch {
            LiveActivities.log.error("Activity.request failed: \(String(describing: error), privacy: .public)")
            return .failed(String(describing: error))
        }
    }

    static func end() async {
        for a in Activity<AdhkarActivityAttributes>.activities {
            await a.end(nil, dismissalPolicy: .immediate)
        }
    }
}

/// ما تنفذه النوايا (Shared/LiveIntents.swift) في عملية التطبيق
@available(iOS 17.0, *)
enum AppLive {
    @MainActor
    static func run(_ action: LiveAction) async -> LiveOutcome {
        switch action {
        // التسبيحة من الأداة وزر شاشة القفل ومركز التحكم وزر النشاط: تُعدّ، ويُحدَّث النشاط إن كان ظاهراً ولا يُفتح
        case .tasbihCount: return await tasbih(IosTasbih.shared.count(), start: false)
        case .tasbihReset: return await tasbih(IosTasbih.shared.reset(), start: false)
        case .tasbihShow: return await tasbih(IosTasbih.shared.current(), start: true)
        case .tasbihEnd:
            await TasbihActivity.end()
            return .ok
        case .adhkarCount: return await adhkar(IosAdhkar.shared.count(section: AdhkarActivity.section))
        case .adhkarSkip: return await adhkar(IosAdhkar.shared.skip(section: AdhkarActivity.section))
        case .adhkarRestart: return await adhkar(IosAdhkar.shared.restart(section: AdhkarActivity.section))
        case .adhkarEnd:
            await AdhkarActivity.end()
            return .ok
        case .prayerToggle: return await PrayerActivity.toggle()
        case .prayerShow: return await PrayerActivity.show()
        }
    }

    /// زر الإجراء والاختصارات (نوايا في التطبيق وحده، MizanShortcuts.swift)
    enum ActionKind { case tasbih, adhkar, prayerToggle, nextPrayer }

    @MainActor
    static func button(_ b: ActionKind) async -> LiveOutcome {
        switch b {
        case .tasbih:
            // تسبيحة بكل ضغطة، بلا بطاقة على شاشة القفل: يُحدَّث النشاط إن كان ظاهراً (بدأه المستخدم من التطبيق) ولا يُبدأ
            return await tasbih(IosTasbih.shared.count(), start: false)
        case .adhkar:
            // الضغطة الأولى تُظهر الذكر الحالي في الجزيرة وعلى شاشة القفل، والتالية تعدّ
            if AdhkarActivity.current == nil { return await adhkar(IosAdhkar.shared.current(section: nil)) }
            return await run(.adhkarCount)
        case .prayerToggle: return await PrayerActivity.toggle()
        case .nextPrayer: return await PrayerActivity.show()
        }
    }

    @MainActor
    private static func tasbih(_ json: String, start: Bool) async -> LiveOutcome {
        guard let m = LiveJSON.tasbih(json) else { return .ok }
        TasbihSync.saved(m.state, immediate: true)
        return await TasbihActivity.apply(m, start: start, alert: true)
    }

    @MainActor
    private static func adhkar(_ json: String) async -> LiveOutcome {
        guard let m = LiveJSON.adhkar(json) else { return .ok }
        return await AdhkarActivity.apply(m, start: true)
    }
}

/// حال المسبحة للأداة وزر مركز التحكم: Kotlin يكتبها في حاوية App Group، وهنا تُكتب في سلسلة المفاتيح المشتركة أيضاً
/// (تثبيت بلا App Group) ويُطلب إعادة رسمهما؛ من شاشة المسبحة مرة كل ثانيتين على الأكثر (التطبيق ظاهر فلا تُرى الأداة)
enum TasbihSync {
    private static var pending: DispatchWorkItem?

    static func saved(_ state: TasbihLiveState, immediate: Bool) {
        pending?.cancel()
        if immediate {
            pending = nil
            store(state)
            LiveRouter.reloadTasbih()
            return
        }
        // التسبيح السريع في الشاشة: آخر حال وحدها بعد ثانيتين
        let work = DispatchWorkItem {
            TasbihSync.pending = nil
            TasbihSync.store(state)
            LiveRouter.reloadTasbih()
        }
        pending = work
        DispatchQueue.main.asyncAfter(deadline: .now() + 2, execute: work)
    }

    /// عند الخروج من التطبيق: ما بقي ينتظر يُكتب الآن
    static func flush() {
        guard let work = pending else { return }
        // يُنفَّذ الآن ثم يُلغى موعده (العنصر الملغى لا يُنفَّذ)
        work.perform()
        work.cancel()
    }

    private static func store(_ state: TasbihLiveState) {
        let e = JSONEncoder()
        e.dateEncodingStrategy = .secondsSince1970
        if let data = try? e.encode(state) { SharedKeychain.write(data, account: SharedKeychain.tasbihAccount) }
    }
}
