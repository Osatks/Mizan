import Foundation
import ActivityKit

// يبدأ أنشطة حية حقيقية بالحالات من Kotlin، فيرسمها امتداد الأدوات الحقيقي (GalleryWidgets: مصادر MizanWidgets نفسها)
// في الجزيرة الديناميكية وعلى شاشة القفل، ويلتقطها اختبار الواجهة (GalleryUITests).

enum LiveStarter {
    /// [scenario]: أسماء مفصولة بـ + (tasbih, tasbihSalah, tasbihFree, tasbihTamam, adhkar, adhkarDone, adhkarLong, prayer, iftar)
    static func start(_ scenario: String, _ f: LangFixture) async -> String {
        guard #available(iOS 17.0, *) else { return "LIVE NEEDS iOS 17" }
        var probe = ""
        await endAll()
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return "LIVE DISABLED" }
        do {
            for part in scenario.split(separator: "+").map(String.init) {
                switch part {
                case "tasbih": try tasbih(f.tasbih)
                case "tasbihSalah": try tasbih(f.tasbihSalah)
                case "tasbihFree": try tasbih(f.tasbihFree)
                case "tasbihTamam": try tasbih(f.tasbihTamam)
                case "adhkar": try adhkar(f.adhkar)
                case "adhkarDone": try adhkar(f.adhkarDone)
                case "adhkarLong": try adhkar(f.adhkarLong)
                // قبل الصلاة بخمس وعشرين دقيقة
                case "prayer": try prayer(f.prayer.moved(to: Date().addingTimeInterval(25 * 60)))
                // المغرب في رمضان كما في التطبيق: يبدأ النشاط قبل الأذان بثوانٍ ثم يصير «قديماً» عند دخول الوقت،
                // فيعرض «حان وقت الإفطار» ودعاءه والعدّ إلى الإقامة
                case "iftar":
                    try prayer(f.prayerIftar.moved(to: Date().addingTimeInterval(4)))
                    try? await Task.sleep(nanoseconds: 7_000_000_000)
                // متى يعلّم النظام النشاط «قديماً» وحده بعد staleDate، وهل يكفي تحديثه بالمحتوى نفسه (refreshArrived في التطبيق)
                case "staleprobe":
                    try prayer(f.prayerIftar.moved(to: Date().addingTimeInterval(5)))
                    var waited = 0
                    var stale = false
                    while waited < 90 && !stale {
                        try? await Task.sleep(nanoseconds: 5_000_000_000)
                        waited += 5
                        stale = Activity<PrayerActivityAttributes>.activities.contains { $0.activityState == .stale }
                    }
                    probe = stale ? "stale by itself after \(waited)s" : "not stale after \(waited)s"
                    if !stale {
                        for a in Activity<PrayerActivityAttributes>.activities {
                            await a.update(ActivityContent(state: a.content.state, staleDate: a.content.state.at, relevanceScore: 50))
                        }
                        try? await Task.sleep(nanoseconds: 3_000_000_000)
                        probe += "; after update: " + states()
                    }
                default: return "LIVE UNKNOWN \(part)"
                }
            }
        } catch {
            return "LIVE ERROR \(error)"
        }
        // يُرسم النشاط في الامتداد بعد قليل
        try? await Task.sleep(nanoseconds: 1_500_000_000)
        return "LIVE STARTED · " + (probe.isEmpty ? "" : probe + " · ") + states()
    }

    /// حال كل نشاط ظاهر (active أو stale أو ended…): تُطبع في سجل الاختبار
    @available(iOS 17.0, *)
    private static func states() -> String {
        var out: [String] = []
        for a in Activity<TasbihActivityAttributes>.activities { out.append("tasbih:\(a.activityState)") }
        for a in Activity<AdhkarActivityAttributes>.activities { out.append("adhkar:\(a.activityState)") }
        for a in Activity<PrayerActivityAttributes>.activities { out.append("prayer:\(a.activityState)") }
        return out.joined(separator: " ")
    }

    @available(iOS 17.0, *)
    private static func endAll() async {
        for a in Activity<TasbihActivityAttributes>.activities { await a.end(nil, dismissalPolicy: .immediate) }
        for a in Activity<AdhkarActivityAttributes>.activities { await a.end(nil, dismissalPolicy: .immediate) }
        for a in Activity<PrayerActivityAttributes>.activities { await a.end(nil, dismissalPolicy: .immediate) }
    }

    @available(iOS 17.0, *)
    private static func tasbih(_ s: TasbihLiveState) throws {
        _ = try Activity.request(attributes: TasbihActivityAttributes(), content: ActivityContent(state: s, staleDate: Date().addingTimeInterval(8 * 3600), relevanceScore: 80))
    }

    @available(iOS 17.0, *)
    private static func adhkar(_ s: AdhkarLiveState) throws {
        _ = try Activity.request(attributes: AdhkarActivityAttributes(section: s.section), content: ActivityContent(state: s, staleDate: Date().addingTimeInterval(4 * 3600), relevanceScore: 70))
    }

    /// كما يبدؤها التطبيق (LiveActivities.swift، بالأولوية نفسها في الجزيرة): الصلاة «قديمة» عند دخول وقتها فتعرض «حان الآن» والإقامة
    @available(iOS 17.0, *)
    private static func prayer(_ s: PrayerLiveState) throws {
        _ = try Activity.request(attributes: PrayerActivityAttributes(id: s.id), content: ActivityContent(state: s, staleDate: s.at, relevanceScore: 50))
    }
}
