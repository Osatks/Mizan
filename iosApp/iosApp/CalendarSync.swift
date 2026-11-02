import Foundation
import EventKit
import UIKit
import CryptoKit
import Shared

/// مواقيت الصلاة في تقويم النظام: تقويم خاص بها («مواقيت الصلاة · ميزان») فيه مواقيت ثلاثين يوماً يحسبها Kotlin
/// (IosCalendar) بلغة التطبيق. يُكتب عند تفعيل المفتاح في إعدادات الأذان، ويتجدد عند كل عودة إلى التطبيق إن تغيّرت
/// المواقيت، ويُزال التقويم كله عند إطفائه. لا يُقرأ من التقويم غير هذا التقويم، ولا يُرسل شيء إلى أي جهة.
enum CalendarSync {
    private static let store = EKEventStore()
    private static let calendarKey = "mizan.calendar.id"
    private static let hashKey = "mizan.calendar.hash"
    private static var running = false

    struct Payload: Decodable {
        var on: Bool
        var prompt: Bool
        var name: String
        var events: [Item]
    }

    struct Item: Decodable {
        var title: String
        var start: Date
        var end: Date
        var notes: String?
    }

    /// من إشعار MizanCalendarSync (LiveActivities.start)
    static func handle(_ json: String?) {
        guard let data = json?.data(using: .utf8), let p = try? LiveJSON.decoder.decode(Payload.self, from: data) else { return }
        // تحديث تلقائي والتقويم يُكتب الآن: يُترك (العودة التالية تحدّثه)
        if running && !p.prompt { return }
        running = true
        Task { @MainActor in
            let (ok, denied) = await apply(p, hash: fingerprint(p))
            running = false
            IosCalendar.shared.done(on: p.on, ok: ok, denied: denied, quiet: !p.prompt)
        }
    }

    /// بصمة المواقيت واسم التقويم (ثابتة بين تشغيل وآخر): تُكتب من جديد إذا تغيّرت فقط
    private static func fingerprint(_ p: Payload) -> String {
        let text = p.name + "\n" + p.events.map {
            "\($0.title)|\(Int($0.start.timeIntervalSince1970))|\(Int($0.end.timeIntervalSince1970))|\($0.notes ?? "")"
        }.joined(separator: "\n")
        return SHA256.hash(data: Data(text.utf8)).map { String(format: "%02x", $0) }.joined()
    }

    @MainActor
    private static func apply(_ p: Payload, hash: String) async -> (Bool, Bool) {
        guard p.on else { return (remove(), false) }
        guard await access(prompt: p.prompt) else { return (false, true) }
        guard let calendar = calendar(named: p.name) else { return (false, false) }
        // المواقيت نفسها مكتوبة من قبل: لا شيء يتغير
        let defaults = UserDefaults.standard
        if !p.prompt, defaults.string(forKey: hashKey) == hash { return (true, false) }
        do {
            let now = Date()
            let old = store.events(matching: store.predicateForEvents(
                withStart: now.addingTimeInterval(-3 * 86_400), end: now.addingTimeInterval(400 * 86_400), calendars: [calendar]
            ))
            for e in old { try store.remove(e, span: .thisEvent, commit: false) }
            for item in p.events where item.end > now {
                let e = EKEvent(eventStore: store)
                e.calendar = calendar
                e.title = item.title
                e.startDate = item.start
                e.endDate = item.end
                e.notes = item.notes
                // لا يحجز الوقت (الوقت «متاح» في الدعوات والمواعيد)، ولا تنبيه: تنبيه الأذان من التطبيق
                e.availability = .free
                try store.save(e, span: .thisEvent, commit: false)
            }
            try store.commit()
            defaults.set(hash, forKey: hashKey)
            return (true, false)
        } catch {
            store.reset()
            return (false, false)
        }
    }

    /// إذن الوصول الكامل (لكتابة تقويم خاص وتجديده وإزالته): يُسأل المستخدم حين يفعّل المفتاح بنفسه فقط
    @MainActor
    private static func access(prompt: Bool) async -> Bool {
        let status = EKEventStore.authorizationStatus(for: .event)
        if #available(iOS 17.0, *) {
            switch status {
            case .fullAccess: return true
            case .notDetermined:
                guard prompt else { return false }
                return (try? await store.requestFullAccessToEvents()) ?? false
            default: return false
            }
        }
        switch status {
        case .authorized: return true
        case .notDetermined:
            guard prompt else { return false }
            return await withCheckedContinuation { c in
                store.requestAccess(to: .event) { granted, _ in c.resume(returning: granted) }
            }
        default: return false
        }
    }

    /// التقويم الخاص بالمواقيت: المحفوظ، وإلا يُنشأ في حساب التقويم الافتراضي (iCloud عادةً، فيظهر في الساعة والـ Mac)
    /// أو على الجهاز
    private static func calendar(named name: String) -> EKCalendar? {
        let defaults = UserDefaults.standard
        if let id = defaults.string(forKey: calendarKey), let c = store.calendar(withIdentifier: id) {
            if c.title != name {
                c.title = name
                try? store.saveCalendar(c, commit: true)
            }
            return c
        }
        let sources = [store.defaultCalendarForNewEvents?.source, store.sources.first { $0.sourceType == .local }].compactMap { $0 }
        for source in sources {
            let c = EKCalendar(for: .event, eventStore: store)
            c.title = name
            c.source = source
            c.cgColor = UIColor(red: 0.06, green: 0.42, blue: 0.33, alpha: 1).cgColor
            do {
                try store.saveCalendar(c, commit: true)
                defaults.set(c.calendarIdentifier, forKey: calendarKey)
                defaults.removeObject(forKey: hashKey)
                return c
            } catch {
                continue
            }
        }
        return nil
    }

    /// يزيل التقويم كله (ومعه مواقيته)
    @MainActor
    private static func remove() -> Bool {
        let defaults = UserDefaults.standard
        defer {
            defaults.removeObject(forKey: calendarKey)
            defaults.removeObject(forKey: hashKey)
        }
        guard let id = defaults.string(forKey: calendarKey), let c = store.calendar(withIdentifier: id) else { return true }
        do {
            try store.removeCalendar(c, commit: true)
            return true
        } catch {
            return false
        }
    }
}
