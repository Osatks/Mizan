import Foundation
import WatchConnectivity
import Shared

/// يرسل إلى ميزان على Apple Watch لقطة المواقيت نفسها التي تقرؤها الأدوات (بلا الأذكار، فهي أصغر بكثير)
/// ونصوص المسبحة بلغة التطبيق، كلما كتب Kotlin لقطة جديدة (WidgetSync)، وحين تطلبها الساعة في أول تشغيل.
/// updateApplicationContext: تصل الساعة آخر لقطة وإن كانت بعيدة أو التطبيق فيها مغلق.
final class WatchSync: NSObject, WCSessionDelegate {
    static let shared = WatchSync()

    /// آخر لقطة لم تُرسل بعد (الجلسة لم تُفعَّل أو لا ساعة)
    private var pending: Data?
    private var last: Data?

    func start() {
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    /// من MizanWidgetsReload: لقطة Kotlin الجديدة
    func send(snapshot json: String) {
        guard WCSession.isSupported(), let data = payload(json) else { return }
        pending = data
        flush()
    }

    private func flush() {
        let s = WCSession.default
        guard s.activationState == .activated, s.isPaired, s.isWatchAppInstalled, let data = pending else { return }
        pending = nil
        guard data != last else { return }
        do {
            try s.updateApplicationContext(["payload": data])
            last = data
        } catch {
            pending = data
        }
    }

    /// اللقطة بلا نوافذ الأذكار ونصوصها، ونصوص المسبحة من حالها في Kotlin
    private func payload(_ json: String) -> Data? {
        guard let raw = json.data(using: .utf8), var snap = try? JSONDecoder().decode(Snapshot.self, from: raw) else { return nil }
        snap.windows = []
        snap.items = [:]
        var labels: TasbihWatchLabels?
        if let t = LiveJSON.tasbih(IosTasbih.shared.current())?.state {
            // «من 33» ← «من %d» (بلا حد: «21/33» في الساعة)
            let of = t.goal > 0 && t.goalText.contains(String(t.goal)) ? t.goalText.replacingOccurrences(of: String(t.goal), with: "%d") : nil
            labels = TasbihWatchLabels(title: t.title, reset: t.reset, of: of, today: t.todayLabel, rounds: t.roundsLabel)
        }
        return try? JSONEncoder().encode(WatchPayloadOut(snapshot: snap, tasbih: labels))
    }

    // ---------- WCSessionDelegate ----------

    func session(_ session: WCSession, activationDidCompleteWith state: WCSessionActivationState, error: Error?) {
        // لا ساعة مقترنة بها ميزان: لا شيء يُحسب
        guard state == .activated, session.isPaired, session.isWatchAppInstalled else { return }
        DispatchQueue.main.async {
            if self.pending == nil { IosWidgets.shared.refresh() } else { self.flush() }
        }
    }

    /// أول تشغيل في الساعة: تطلب لقطة الآن
    func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        guard message["want"] as? String == "snapshot" else { return }
        DispatchQueue.main.async {
            self.last = nil
            IosWidgets.shared.refresh()
        }
    }

    func sessionWatchStateDidChange(_ session: WCSession) {
        DispatchQueue.main.async { self.flush() }
    }

    func sessionDidBecomeInactive(_ session: WCSession) {}

    func sessionDidDeactivate(_ session: WCSession) {
        // ساعة أخرى: تُفعَّل الجلسة لها
        WCSession.default.activate()
    }
}

/// ما تقرؤه الساعة (WatchPayload و TasbihLabels في MizanWatch/WatchData.swift): المفاتيح نفسها
private struct WatchPayloadOut: Encodable {
    var snapshot: Snapshot
    var tasbih: TasbihWatchLabels?
}

private struct TasbihWatchLabels: Encodable {
    var title: String
    var reset: String
    var of: String?
    var today: String
    var rounds: String
}
