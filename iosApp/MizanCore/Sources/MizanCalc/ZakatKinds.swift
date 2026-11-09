// زكاة بهيمة الأنعام والزروع والمعادن والركاز (نقلاً عن core/ZakatKinds.kt)، وحساب صفحاتها كما في NativeZakat.ios.kt وZakatKindsScreens.kt.
// الأنعام: كتاب أبي بكر في الصدقات (البخاري 1454) للإبل والغنم، وحديث معاذ في البقر (أبو داود 1576، الترمذي 623).

public enum ZakatKinds {
    /// ما يجب في الأنعام: نوع السن (shah شاة، bintMakhad بنت مخاض، bintLabun بنت لبون، hiqqah حقة،
    /// jadhaah جذعة، tabi تبيع، musinnah مسنة) وعدده
    public struct Due: Equatable, Sendable {
        public let kind: String
        public let count: Int
        public init(_ kind: String, _ count: Int) {
            self.kind = kind
            self.count = count
        }
    }

    public static let sheepNisab = 40
    public static let cattleNisab = 30
    public static let camelNisab = 5

    public static func sheep(_ n: Int) -> [Due] {
        if n < 40 { return [] }
        if n <= 120 { return [Due("shah", 1)] }
        if n <= 200 { return [Due("shah", 2)] }
        if n <= 399 { return [Due("shah", 3)] }
        return [Due("shah", n / 100)]
    }

    /// في كل ثلاثين تبيع، وفي كل أربعين مسنة؛ يُختار ما يستوعب أكثر العدد، وعند التساوي المسنّات (أنفع للفقراء)
    public static func cattle(_ n: Int) -> [Due] {
        if n < 30 { return [] }
        if n < 40 { return [Due("tabi", 1)] }
        if n < 60 { return [Due("musinnah", 1)] }
        let (a, b) = bestSplit(n, 30, 40)
        var out: [Due] = []
        if a > 0 { out.append(Due("tabi", a)) }
        if b > 0 { out.append(Due("musinnah", b)) }
        return out
    }

    /// الإبل: شياه حتى أربع وعشرين، ثم أسنان الإبل، وما زاد على عشرين ومائة ففي كل أربعين بنت لبون وفي كل خمسين حقة
    public static func camels(_ n: Int) -> [Due] {
        if n < 5 { return [] }
        if n < 25 { return [Due("shah", n / 5)] }
        if n <= 35 { return [Due("bintMakhad", 1)] }
        if n <= 45 { return [Due("bintLabun", 1)] }
        if n <= 60 { return [Due("hiqqah", 1)] }
        if n <= 75 { return [Due("jadhaah", 1)] }
        if n <= 90 { return [Due("bintLabun", 2)] }
        if n <= 120 { return [Due("hiqqah", 2)] }
        let (a, b) = bestSplit(n, 40, 50)
        var out: [Due] = []
        if a > 0 { out.append(Due("bintLabun", a)) }
        if b > 0 { out.append(Due("hiqqah", b)) }
        return out
    }

    /// أكبر ما يُستوعب من n بمضاعفات x و y (عدد x وعدد y)، وعند التساوي يُقدَّم الأكبر y
    private static func bestSplit(_ n: Int, _ x: Int, _ y: Int) -> (Int, Int) {
        var best = (0, 0)
        var cover = -1
        for b in 0...(n / y) {
            let a = (n - b * y) / x
            let c = a * x + b * y
            if c > cover || (c == cover && b > best.1) { cover = c; best = (a, b) }
        }
        return best
    }

    /// نصاب الزروع والثمار: خمسة أوسق، نحو 612 كغ من القمح (والتقديرات بين 612 و653 كغ)
    public static let cropsNisabKg = 612.0

    /// نسبة الواجب: rain العشر (بلا كلفة)، irrigated نصف العشر (بكلفة)، mixed ثلاثة أرباع العشر
    public static func cropsRate(_ irrigation: String) -> Double {
        switch irrigation {
        case "irrigated": return 0.05
        case "mixed": return 0.075
        default: return 0.10
        }
    }

    public static func crops(_ kg: Double, _ irrigation: String) -> Double {
        kg >= cropsNisabKg ? kg * cropsRate(irrigation) : 0.0
    }

    /// الركاز (دفين الجاهلية): الخمس في الحال بلا نصاب ولا حول عند الجمهور
    public static func rikaz(_ value: Double) -> Double { katLeast(value, 0.0) * 0.2 }

    /// المعادن المستخرجة من الأرض: ربع العشر إذا بلغت نصاب الذهب (الحنابلة والشافعية)
    public static func mineral(_ value: Double, _ goldNisabValue: Double) -> Double {
        goldNisabValue > 0 && value >= goldNisabValue ? value * 0.025 : 0.0
    }
}

// MARK: - بهيمة الأنعام (LivestockZakat)

/// زكاة الأنعام: الأعداد كما يقرؤها التطبيق من النص (ZakatLivestock.count) ومعها شرطا السوم والحول
public struct ZakatLivestock {
    public enum Kind: String, CaseIterable { case camels, cattle, sheep }

    /// صف صنف: العدد، وما يجب فيه، ونصابه
    public struct Row {
        public let kind: Kind
        public let n: Int
        public let dues: [ZakatKinds.Due]
        public let nisab: Int
    }

    /// حالة الصنف في بطاقته: لا شيء لعدم الشروط، أو دون النصاب، أو ما يجب
    public enum RowStatus: Equatable { case nothingDue, belowNisab, dues([ZakatKinds.Due]) }

    /// شارة النتيجة
    public enum Badge: String { case conditionsNotMet, nisabReached, nothingDue }

    public let rows: [Row]
    public let grazing: Bool
    public let year: Bool
    /// الشرطان متحققان
    public let ok: Bool
    /// مجموع ما يجب من الرؤوس (صفر إن لم تتحقق الشروط)
    public let total: Int

    /// العدد من القيمة المحلَّلة (Fmt.parse): يُبتر ولا يقل عن صفر
    public static func count(_ parsed: Double) -> Int { Swift.max(kotlinToInt(parsed), 0) }

    public init(camels: Int, cattle: Int, sheep: Int, grazing: Bool = true, year: Bool = true) {
        rows = [
            Row(kind: .camels, n: camels, dues: ZakatKinds.camels(camels), nisab: ZakatKinds.camelNisab),
            Row(kind: .cattle, n: cattle, dues: ZakatKinds.cattle(cattle), nisab: ZakatKinds.cattleNisab),
            Row(kind: .sheep, n: sheep, dues: ZakatKinds.sheep(sheep), nisab: ZakatKinds.sheepNisab),
        ]
        self.grazing = grazing
        self.year = year
        ok = grazing && year
        total = ok ? rows.flatMap { $0.dues }.reduce(0) { $0 + $1.count } : 0
    }

    public var badge: Badge { !ok ? .conditionsNotMet : (total > 0 ? .nisabReached : .nothingDue) }
    /// حالة بطاقة النتيجة: nil إن لم يُدخل شيء
    public var resultOk: Bool? { rows.allSatisfy { $0.n == 0 } ? nil : total > 0 }
    /// الأصناف المُدخلة (عددها أكبر من صفر) لبطاقاتها
    public var shownRows: [Row] { rows.filter { $0.n > 0 } }

    public func status(_ r: Row) -> RowStatus {
        if !ok { return .nothingDue }
        if r.dues.isEmpty { return .belowNisab }
        return .dues(r.dues)
    }
}

// MARK: - الزروع والثمار (CropsZakat)

public struct ZakatCrops {
    /// شارة النتيجة: enterWeight (نص النصاب)، أو بلغ النصاب، أو دونه
    public enum Badge: String { case enterWeight, nisabReached, belowNisab }

    public let kg: Double
    public let irrigation: String
    public let price: Double
    /// الواجب بالكيلوغرام
    public let due: Double

    public init(kg: Double, irrigation: String = "rain", price: Double = 0) {
        self.kg = kg
        self.irrigation = irrigation
        self.price = price
        due = ZakatKinds.crops(kg, irrigation)
    }

    public var nisab: Double { ZakatKinds.cropsNisabKg }
    /// النسبة المئوية المعروضة (10 أو 5 أو 7.5)
    public var ratePercent: Double { ZakatKinds.cropsRate(irrigation) * 100 }
    public var badge: Badge { kg <= 0 ? .enterWeight : (due > 0 ? .nisabReached : .belowNisab) }
    public var resultOk: Bool? { kg <= 0 ? nil : due > 0 }
    /// القيمة التقريبية إن وجب شيء وأُدخل سعر الكيلو
    public var value: Double? { due > 0 && price > 0 ? due * price : nil }
}

// MARK: - المعادن والركاز (MineralsZakat)

public struct ZakatMinerals {
    public let goldPrice: Double
    public let rikaz: Double
    public let mineral: Double

    /// rikazValue قيمة الركاز، mineralValue قيمة المعدن، goldPrice سعر غرام الذهب عيار 24 (حقل zk_gold_price المشترك)
    public init(rikazValue: Double, mineralValue: Double, goldPrice: Double) {
        self.goldPrice = goldPrice
        rikaz = ZakatKinds.rikaz(rikazValue)
        mineral = ZakatKinds.mineral(mineralValue, 85 * goldPrice)
    }

    public var total: Double { rikaz + mineral }
    /// نصاب المعدن (85 غراماً من الذهب)، nil إن لم يُدخل سعر الذهب
    public var nisab: Double? { goldPrice > 0 ? 85 * goldPrice : nil }
}
