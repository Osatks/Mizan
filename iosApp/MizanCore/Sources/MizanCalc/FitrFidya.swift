import MizanBase

// زكاة الفطر والفدية والكفارات (نقلاً عن FitrFidyaScreens.kt)

/// زكاة الفطر: صاع عن كل شخص طعاماً، أو قيمته
public struct Fitr {
    /// نوع الطعام: الوزن بالكيلوغرام (‎-1 = يُدخله المستخدم)، و approx إن كان الوزن تقريبياً
    public struct Food: Equatable, Sendable {
        public let key: String
        public let kg: Double
        public let approx: Bool
        public init(_ key: String, _ kg: Double, approx: Bool = true) {
            self.key = key
            self.kg = kg
            self.approx = approx
        }
        /// وزنه يُدخله المستخدم
        public var isCustom: Bool { kg < 0 }
    }

    public static let foods: [Food] = [
        Food("rice", 2.5), Food("riceSafe", 3.0, approx: false), Food("wheat", 2.04),
        Food("dates", 2.5), Food("barley", 2.2), Food("other", -1.0),
    ]
    /// وزن الصاع الابتدائي لحقل «أخرى»
    public static let defaultCustomKg = 2.5
    public static let peopleRange = 1...99

    public static func food(_ key: String) -> Food? { foods.first { $0.key == key } }

    public let people: Int
    /// "food" أو "money"
    public let mode: String
    public let food: Food
    /// الصاع للشخص بالكيلوغرام
    public let sa: Double
    /// مجموع الطعام بالكيلوغرام
    public let kg: Double
    public let kgPrice: Double
    public let valuePerPerson: Double

    /// custom وزن الصاع المُدخل لـ «أخرى»، kgPrice سعر الكيلو، valuePerPerson القيمة عن الشخص (كلها بعد Fmt.parse)
    public init(people: Int, mode: String = "food", food: Food = Fitr.foods[0], custom: Double = Fitr.defaultCustomKg, kgPrice: Double = 0, valuePerPerson: Double = 0) {
        self.people = people
        self.mode = mode
        self.food = food
        sa = food.kg < 0 ? custom : food.kg
        kg = sa * Double(people)
        self.kgPrice = kgPrice
        self.valuePerPerson = valuePerPerson
    }

    /// التكلفة التقريبية للطعام إن أُدخل سعر الكيلو
    public var cost: Double? { kgPrice > 0 ? kg * kgPrice : nil }
    /// مجموع القيمة نقداً إن أُدخلت القيمة عن الشخص
    public var cashTotal: Double? { valuePerPerson > 0 ? valuePerPerson * Double(people) : nil }
}

/// الفدية (عن أيام الصيام) وكفارة اليمين وكفارة الجماع في نهار رمضان: إطعام المساكين
public struct Fidya {
    public static let daysRange = 1...360
    public static let oathsRange = 1...100
    public static let tabs = ["fidya", "yamin", "jima"]
    /// مقدار الإطعام: مدّ (0.75 كغ) أو نصف صاع (1.5 كغ)
    public static func measure(_ key: String) -> Double { key == "mudd" ? 0.75 : 1.5 }
    public static func measureKey(_ measure: Double) -> String { measure == 0.75 ? "mudd" : "half" }

    /// "fidya" أو "yamin" أو غيرهما (الجماع)
    public let tab: String
    public let days: Int
    public let oaths: Int
    public let measure: Double
    public let price: Double
    /// عدد المساكين
    public let poor: Int
    /// مجموع الطعام بالكيلوغرام
    public let kg: Double

    public init(tab: String = "fidya", days: Int = 30, oaths: Int = 1, measure: Double = 0.75, price: Double = 0) {
        self.tab = tab
        self.days = days
        self.oaths = oaths
        self.measure = measure
        self.price = price
        switch tab {
        case "fidya": poor = days
        case "yamin": poor = 10 * oaths
        default: poor = 60
        }
        kg = measure * Double(poor)
    }

    /// كفارة اليمين: صيام ثلاثة أيام عن كل يمين لمن لم يجد
    public var fastDays: Int? { tab == "yamin" ? 3 * oaths : nil }
    /// القيمة التقريبية إن أُدخل سعر الكيلو
    public var value: Double? { price > 0 ? price * kg : nil }

    /// كفارة الجماع: صيام شهرين هجريين متتابعين من اليوم الهجري (y, m, d)؛ يعيد اليوم الهجري الذي ينتهي الصيام قبله بيوم.
    /// التطبيق يحوّله إلى ميلادي (Hijri.toGregorian) ويطرح يوماً، والمدة = الأيام بين البداية والنهاية + 1.
    public static func twoMonthsTarget(y: Int, m: Int, d: Int, monthLength: (Int, Int) -> Int) -> (y: Int, m: Int, d: Int) {
        var y2 = y
        var m2 = m + 2
        if m2 > 12 { m2 -= 12; y2 += 1 }
        return (y2, m2, Swift.min(d, monthLength(y2, m2)))
    }

    /// كفارة الجماع إذا بدأ الصيام يوم start: آخر يوم من الشهرين الهجريين، وعدد الأيام (nil خارج مدى التقويم)
    public static func twoMonths(from start: LocalDate) -> (end: LocalDate, days: Int)? {
        guard let h = Hijri.toHijri(start) else { return nil }
        let t = twoMonthsTarget(y: h.y, m: h.m, d: h.d, monthLength: Hijri.monthLength)
        guard let end = Hijri.toGregorian(t.y, t.m, t.d)?.minusDays(1) else { return nil }
        return (end, end.epochDay - start.epochDay + 1)
    }
}
