import WidgetKit
import SwiftUI

/// مدخل واحد في خط زمن الأداة: لحظته واللقطة وموضع المستخدم في أذكار النافذة
struct MizanEntry: TimelineEntry {
    let date: Date
    let snap: Snapshot?
    var offset: Int = 0
}

/// أي أداة يُبنى لها خط الزمن (يحدد اللحظات التي يتغير عندها ما تعرضه)
enum WidgetKind {
    case prayer, day, adhkar
}

/**
 * خط الزمن من اللقطة وحدها: مدخل عند دخول كل صلاة وعند منتصف كل ليلة، ولأداة الأذكار
 * عند حدود نوافذها وكل [rotate] دقيقة (تدوير الذكر). فتبقى الأدوات صحيحة أياماً دون فتح التطبيق،
 * ولا تُطلب إعادة البناء إلا بعد آخر مدخل أو حين يكتب التطبيق لقطة جديدة.
 */
struct MizanProvider: TimelineProvider {
    let kind: WidgetKind

    func placeholder(in context: Context) -> MizanEntry {
        MizanEntry(date: Date(), snap: Sample.snapshot)
    }

    func getSnapshot(in context: Context, completion: @escaping (MizanEntry) -> Void) {
        let now = Date()
        let snap = Store.load().flatMap { $0.day(at: now) == nil ? nil : $0 } ?? Sample.snapshot
        completion(MizanEntry(date: now, snap: snap, offset: AdhkarCursor.offset(for: snap.window(at: now)?.key)))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<MizanEntry>) -> Void) {
        let now = Date()
        guard let snap = Store.load() else {
            // لم يُفتح التطبيق بعد: نعيد المحاولة بعد نصف ساعة
            completion(Timeline(entries: [MizanEntry(date: now, snap: nil)], policy: .after(now.addingTimeInterval(1800))))
            return
        }
        let t0 = now.timeIntervalSince1970
        var times: [Double] = [t0]
        let horizon: Double
        switch kind {
        case .prayer:
            horizon = t0 + 36 * 3600
            times += snap.prayers.map { $0.at }.filter { $0 > t0 && $0 < horizon }
            times += snap.days.map { $0.start }.filter { $0 > t0 && $0 < horizon }
        case .day:
            horizon = t0 + 72 * 3600
            times += snap.days.map { $0.start }.filter { $0 > t0 && $0 < horizon }
        case .adhkar:
            horizon = t0 + 10 * 3600
            let step = Double(max(snap.rotate, 1) * 60)
            for w in snap.windows where w.e > t0 && w.s < horizon {
                if w.s > t0 { times.append(w.s) }
                var t = w.s + step
                while t < w.e && t < horizon {
                    if t > t0 { times.append(t) }
                    t += step
                }
            }
        }
        // بعد آخر أيام اللقطة: مدخل يعرض «افتح التطبيق»
        if let last = snap.days.last, last.start + 86_400 > t0, last.start + 86_400 < horizon {
            times.append(last.start + 86_400)
        }
        let sorted = Array(Set(times)).sorted().prefix(72)
        let entries = sorted.map { t -> MizanEntry in
            let d = Date(timeIntervalSince1970: t)
            return MizanEntry(date: d, snap: snap, offset: AdhkarCursor.offset(for: snap.window(at: d)?.key))
        }
        completion(Timeline(entries: entries, policy: .atEnd))
    }
}

/// بيانات المعاينة في معرض الأدوات قبل أول فتح للتطبيق
enum Sample {
    static var snapshot: Snapshot {
        let now = Date()
        let cal = Calendar.current
        let start = cal.startOfDay(for: now).timeIntervalSince1970
        let ar = Fallback.arabic
        func pt(_ k: String, _ name: String, _ h: Double, _ s: String) -> PrayerTime {
            PrayerTime(k: k, name: name, at: start + h * 3600, t: s, s: s)
        }
        let names = ar
            ? ["الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"]
            : ["Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"]
        func day(_ i: Int) -> Day {
            let off = Double(i) * 24
            return Day(
                date: "", start: start + off * 3600, weekday: ar ? "الجمعة" : "Friday", friday: true,
                greg: ar ? "٣٠ أكتوبر ٢٠٢٦" : "30 October 2026", hijri: ar ? "١٨ جمادى الأولى ١٤٤٨" : "18 Jumada al-Ula 1448",
                hijriDay: ar ? "١٨" : "18", hijriMonth: ar ? "جمادى الأولى ١٤٤٨" : "Jumada al-Ula 1448",
                times: [
                    pt("fajr", names[0], off + 4.9, "4:54"), pt("sunrise", names[1], off + 6.2, "6:12"),
                    pt("dhuhr", names[2], off + 11.9, "11:54"), pt("asr", names[3], off + 15.1, "3:06"),
                    pt("maghrib", names[4], off + 17.5, "5:30"), pt("isha", names[5], off + 19.0, "7:00"),
                ],
                ayah: Ayah(text: "فَإِنَّ مَعَ ٱلْعُسْرِ يُسْرًا ۝ إِنَّ مَعَ ٱلْعُسْرِ يُسْرًا", ref: ar ? "الشرح · الآية ٥" : "Ash-Sharh · Ayah 5",
                           meaning: ar ? nil : "So, surely with hardship comes ease. Surely with hardship comes ease.", route: "quran")
            )
        }
        let w = AdhkarWindow(k: "friday", key: "sample", s: start, e: start + 86_400 * 2,
                             title: ar ? "سنن الجمعة" : "Friday sunnahs", chip: nil, until: "", section: "tasbih", items: ["s1"])
        let item = Dhikr(ar: "«مَنْ قَرَأَ سُورَةَ الْكَهْفِ فِي يَوْمِ الْجُمُعَةِ أَضَاءَ لَهُ مِنَ النُّورِ مَا بَيْنَ الْجُمُعَتَيْنِ»", n: 1, c: nil,
                         src: ar ? "المستدرك للحاكم 2/368" : "al-Mustadrak (al-Hakim) 2/368",
                         tr: ar ? nil : "Whoever recites Surat al-Kahf on Friday, a light will shine for him between the two Fridays.",
                         fadl: ar ? "اقرأ سورة الكهف اليوم" : "Read Surat al-Kahf today")
        return Snapshot(
            v: 1, at: now.timeIntervalSince1970, lang: ar ? "ar" : "en", rtl: ar, theme: "system", colors: Mizan.fallback,
            place: ar ? "مكة المكرمة" : "Makkah", rotate: 20, labels: [:], days: [day(0), day(1)], windows: [w], items: ["s1": item]
        )
    }
}
