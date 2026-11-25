import Foundation

// زكاة النقدين وعروض التجارة في نصاب واحد (نقلاً عن ZakatMoneyScreen في ZakatScreen.kt وmoneyPage في NativeZakat.ios.kt)، والعملات.

/// مدخلات الحساب بعد تحليل النصوص (Fmt.parse)؛ الأسماء كحقول ZAKAT_FIELDS المحفوظة بالبادئة zk_
public struct ZakatMoneyInput: Equatable {
    public var goldPrice = 0.0, silverPrice = 0.0
    public var cash = 0.0, bank = 0.0, receivable = 0.0, other = 0.0
    public var g24 = 0.0, g22 = 0.0, g21 = 0.0, g18 = 0.0, silver = 0.0, jewel = 0.0
    public var stocksTrade = 0.0, stocksLong = 0.0, goods = 0.0, debts = 0.0
    /// أساس النصاب: "gold" أو "silver" (المحفوظ zk_basis)
    public var basis = "gold"
    /// ضمّ الحلي المستعمل (zk_jewelry)
    public var includeJewelry = true

    public init() {}

    /// من قيم الحقول بمفاتيحها في ZakatMoney.fields (بلا البادئة zk_)
    public init(values: [String: Double], basis: String = "gold", includeJewelry: Bool = true) {
        func v(_ k: String) -> Double { values[k] ?? 0 }
        goldPrice = v("gold_price"); silverPrice = v("silver_price")
        cash = v("cash"); bank = v("bank"); receivable = v("receivable"); other = v("other")
        g24 = v("g24"); g22 = v("g22"); g21 = v("g21"); g18 = v("g18"); silver = v("silver"); jewel = v("jewel")
        stocksTrade = v("stocks_trade"); stocksLong = v("stocks_long"); goods = v("goods"); debts = v("debts")
        self.basis = basis
        self.includeJewelry = includeJewelry
    }
}

public struct ZakatMoney {
    /// حقول النقدين المحفوظة (بالبادئة zk_ في الإعدادات)
    public static let fields = ["gold_price", "silver_price", "cash", "bank", "receivable", "other", "g24", "g22", "g21", "g18", "silver", "jewel", "stocks_trade", "stocks_long", "goods", "debts"]

    /// أجزاء الوعاء بترتيبها في التفصيل
    public enum Part: String, CaseIterable { case cash, receivables, gold, silver, stocks, trade }

    /// شارة النتيجة
    public enum Badge: String { case enterGoldPrice, enterSilverPrice, nisabReached, belowNisab, enterAssets }

    public let input: ZakatMoneyInput
    /// الذهب الخالص بعيار 24 بالغرام، وما استُثني منه من الحلي
    public let pure24: Double
    public let excluded: Double
    /// الأجزاء وقيمها بالعملة
    public let parts: [(part: Part, value: Double)]
    public let total: Double
    /// الديون المحسومة (لا تزيد على المجموع)
    public let debts: Double
    public let net: Double
    public let nisab: Double
    public let due: Bool
    public let zakat: Double

    public init(_ i: ZakatMoneyInput) {
        input = i
        let gp = i.goldPrice, sp = i.silverPrice
        let pure24 = i.g24 + i.g22 * 22 / 24 + i.g21 * 21 / 24 + i.g18 * 18 / 24
        self.pure24 = pure24
        let excluded = i.includeJewelry ? 0.0 : kmin(pure24, i.jewel * 21 / 24)
        self.excluded = excluded
        let gold = (pure24 - excluded) * gp
        parts = [
            (.cash, i.cash + i.bank + i.other),
            (.receivables, i.receivable),
            (.gold, gold),
            (.silver, i.silver * sp),
            (.stocks, i.stocksTrade + i.stocksLong),
            (.trade, i.goods),
        ]
        let total = parts.reduce(0.0) { $0 + $1.value }
        self.total = total
        debts = kmin(i.debts, total)
        net = katLeast(total - debts, 0.0)
        nisab = i.basis == "silver" ? 595 * sp : 85 * gp
        due = nisab > 0 && net >= nisab && net > 0
        zakat = due ? net * 0.025 : 0.0
    }

    public var goldValue: Double { parts[2].value }

    public var badge: Badge {
        if nisab <= 0 { return input.basis == "silver" ? .enterSilverPrice : .enterGoldPrice }
        if due { return .nisabReached }
        if net > 0 { return .belowNisab }
        return .enterAssets
    }

    /// حالة بطاقة النتيجة: nil إن لم يُعرف النصاب أو لم يُدخل شيء
    public var resultOk: Bool? { nisab <= 0 || net <= 0 ? nil : due }

    /// نسبة الوعاء إلى النصاب (0…1)، nil إن لم يُعرف النصاب
    public var progress: Double? {
        guard nisab > 0 else { return nil }
        let r = net / nisab
        return r.isFinite ? kcoerce(r, 0.0, 1.0) : nil
    }

    /// ما يعادل الزكاة بغرامات الذهب عيار 24
    public var goldGrams: Double? { zakat > 0 && input.goldPrice > 0 ? zakat / input.goldPrice : nil }

    /// زر المشاركة يظهر عند وجوب الزكاة
    public var canShare: Bool { zakat > 0 }

    /// تفصيل الزكاة على الأجزاء: يظهر إن وجبت وفي الوعاء أكثر من جزء
    public var breakdown: [(part: Part, value: Double)] {
        guard zakat > 0 && parts.filter({ $0.value > 0 }).count > 1 else { return [] }
        let ratio = total > 0 ? net / total : 0.0
        return parts.filter { $0.value > 0 }.map { ($0.part, $0.value * ratio * 0.025) }
    }
}

// MARK: - العملات

/// عملة: رمز ISO، ورمزها المختصر واسمها بالعربية (بقية اللغات تأخذ الاسم من النظام)
public struct Currency: Equatable, Sendable {
    public let iso: String
    public let arSymbol: String
    public let arName: String

    public init(_ iso: String, _ arSymbol: String, _ arName: String) {
        self.iso = iso
        self.arSymbol = arSymbol
        self.arName = arName
    }

    /// الرمز المعروض: العربي في العربية، و$ € £ في كل اللغات، وإلا رمز ISO
    public func symbol(arabic: Bool) -> String {
        if arabic { return arSymbol }
        if iso == "USD" || iso == "EUR" || iso == "GBP" { return arSymbol }
        return iso
    }

    /// الاسم المعروض: العربي في العربية، وإلا اسم النظام بحرف أول كبير (أو رمز ISO)
    public func name(langCode: String) -> String {
        if langCode == "ar" { return arName }
        guard let n = Currencies.systemName(iso, langCode) else { return iso }
        guard let first = n.first else { return n }
        return String(first).uppercased() + n.dropFirst()
    }
}

public enum Currencies {
    public static let list: [Currency] = [
        Currency("SAR", "ر.س", "ريال سعودي"), Currency("AED", "د.إ", "درهم إماراتي"), Currency("KWD", "د.ك", "دينار كويتي"),
        Currency("QAR", "ر.ق", "ريال قطري"), Currency("BHD", "د.ب", "دينار بحريني"), Currency("OMR", "ر.ع", "ريال عماني"),
        Currency("EGP", "ج.م", "جنيه مصري"), Currency("JOD", "د.أ", "دينار أردني"), Currency("IQD", "د.ع", "دينار عراقي"),
        Currency("MAD", "د.م", "درهم مغربي"), Currency("DZD", "د.ج", "دينار جزائري"), Currency("TND", "د.ت", "دينار تونسي"),
        Currency("LBP", "ل.ل", "ليرة لبنانية"), Currency("SYP", "ل.س", "ليرة سورية"), Currency("YER", "ر.ي", "ريال يمني"),
        Currency("SDG", "ج.س", "جنيه سوداني"), Currency("LYD", "د.ل", "دينار ليبي"), Currency("TRY", "TRY", "ليرة تركية"),
        Currency("PKR", "PKR", "روبية باكستانية"), Currency("INR", "INR", "روبية هندية"), Currency("BDT", "BDT", "تاكا بنغلاديشي"),
        Currency("IDR", "IDR", "روبية إندونيسية"), Currency("MYR", "MYR", "رينغيت ماليزي"), Currency("IRR", "IRR", "ريال إيراني"),
        Currency("RUB", "₽", "روبل روسي"), Currency("GBP", "£", "جنيه إسترليني"), Currency("USD", "$", "دولار أمريكي"), Currency("EUR", "€", "يورو"),
        Currency("CAD", "C$", "دولار كندي"), Currency("AUD", "A$", "دولار أسترالي"),
        Currency("UAH", "₴", "هريفنيا أوكرانية"), Currency("MXN", "MX$", "بيزو مكسيكي"), Currency("ARS", "ARS", "بيزو أرجنتيني"),
        Currency("UZS", "UZS", "سوم أوزبكي"), Currency("KZT", "₸", "تنغي كازاخستاني"), Currency("KGS", "KGS", "سوم قيرغيزي"),
        Currency("TJS", "TJS", "سوموني طاجيكي"), Currency("AZN", "₼", "مانات أذربيجاني"),
    ]

    /// اسم العملة من النظام (Platform.currencyName في Kotlin: NSLocale displayNameForKey)
    public static func systemName(_ iso: String, _ lang: String) -> String? {
        Locale(identifier: lang).localizedString(forCurrencyCode: iso)
    }

    /// عملات التطبيق، ومعها عملة حزمة اللغة الحالية (packCurrency) إن لم تكن في القائمة
    public static func all(packCurrency: String?) -> [Currency] {
        guard let extra = packCurrency, !list.contains(where: { $0.iso == extra }), systemName(extra, "en") != nil else { return list }
        return list + [Currency(extra, extra, systemName(extra, "ar") ?? extra)]
    }

    /// العملة الافتراضية حسب لغة الواجهة عند أول استخدام
    public static func defaultIso(langCode: String, packCurrency: String?) -> String {
        if let p = packCurrency { return p }
        switch langCode {
        case "ar": return "SAR"
        case "tr": return "TRY"
        case "ur": return "PKR"
        case "id": return "IDR"
        case "ms": return "MYR"
        case "fa": return "IRR"
        case "bn": return "BDT"
        case "ru": return "RUB"
        case "fr", "de", "es": return "EUR"
        case "uk": return "UAH"
        default: return "USD"
        }
    }

    /// العملة المحفوظة (zk_cur): الإصدارات السابقة حفظت الرمز العربي ("ر.س") فيُحوَّل إلى رمز ISO
    public static func saved(raw: String, langCode: String, packCurrency: String?) -> String {
        all(packCurrency: packCurrency).first(where: { $0.iso == raw || $0.arSymbol == raw })?.iso
            ?? defaultIso(langCode: langCode, packCurrency: packCurrency)
    }

    /// العملة المختارة من القائمة، وإلا الأولى
    public static func find(_ iso: String, packCurrency: String?) -> Currency {
        let l = all(packCurrency: packCurrency)
        return l.first(where: { $0.iso == iso }) ?? l[0]
    }
}

// MARK: - مفاتيح الحفظ

/// مفاتيح حقول الزكاة في AppSettings كما تحفظها Kotlin (ZakatScreen وNativeZakat.ios)؛ النصوص تُحفظ كما كتبها المستخدم
public enum ZakatKeys {
    /// حقول النقدين: "zk_" + ZakatMoney.fields
    public static let moneyFields = ZakatMoney.fields.map { "zk_" + $0 }
    /// حقول الأنعام والزروع والمعادن (نصوص)
    public static let kindFields = ["zk_camels", "zk_cattle", "zk_sheep", "zk_crop_kg", "zk_crop_price", "zk_rikaz", "zk_mineral"]
    /// العملة (رمز ISO، وقد تكون رمزاً عربياً من إصدار قديم: Currencies.saved)
    public static let currency = "zk_cur"
    /// أساس النصاب، والافتراضي "gold"
    public static let basis = "zk_basis"
    public static let basisDefault = "gold"
    /// طريقة السقي، والافتراضي "rain"
    public static let irrigation = "zk_irr"
    public static let irrigationDefault = "rain"
    /// المفاتيح (نعم/لا)، والافتراضي نعم لكلها: السوم، والحول، وضم الحلي
    public static let grazing = "zk_grazing"
    public static let lunarYear = "zk_lyear"
    public static let jewelry = "zk_jewelry"
}
