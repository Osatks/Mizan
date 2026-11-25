import Foundation
import MizanBase

/// آية قصيرة (تسع الأداة) تُختار بثبات لكل يوم (AyahOfDay.kt)
public enum AyahOfDay {
    /// الآيات التي يتراوح طول نصها بلا بسملة بين 40 و160 محرفاً (UTF-16 كما في Kotlin)
    public static func shortAyahs(_ text: [String]) -> [Int] {
        text.indices.filter { (40...160).contains(QuranData.withoutBismillah(text, $0).utf16.count) }
    }

    /// من قائمة الآيات القصيرة المحسوبة مسبقاً؛ [day] رقم اليوم من 1970-01-01
    public static func pick(_ short: [Int], day: Int) -> Int {
        let n = short.count
        let m = (day &* 7919) % n
        return short[m < 0 ? m + n : m]
    }
}

/// آية اليوم: فهرسها في المصحف؛ [day] رقم اليوم من 1970-01-01 (QuranDay.today())
public func ayahOfDay(_ text: [String], day: Int) -> Int {
    AyahOfDay.pick(AyahOfDay.shortAyahs(text), day: day)
}

/// آية اليوم ليوم ميلادي
public func ayahOfDay(_ text: [String], _ date: LocalDate) -> Int { ayahOfDay(text, day: date.epochDay) }
