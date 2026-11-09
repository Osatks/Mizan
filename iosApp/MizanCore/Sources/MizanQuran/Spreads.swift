import Foundation

/// تقليب المصحف صفحةً صفحة أو كتاباً مفتوحاً (صفحتين متقابلتين)، كـ Spreads.kt.
/// الكتاب العربي يُفتح من اليمين، فالصفحة الفردية على اليمين والزوجية على يسارها كما في مصحف المدينة المطبوع.
public enum QuranSpreads {
    public static var PAGES: Int { QuranMeta.PAGES.count }

    /// عدد مواضع التقليب
    public static func count(_ spread: Bool) -> Int { spread ? (PAGES + 1) / 2 : PAGES }

    /// موضع التقليب الذي فيه الصفحة [page]
    public static func index(_ page: Int, _ spread: Bool) -> Int {
        let i = clamp(page, 1, PAGES) - 1
        return spread ? i / 2 : i
    }

    /// الصفحات الظاهرة في موضع التقليب [index] (اليمنى أولاً)
    public static func pages(_ index: Int, _ spread: Bool) -> ClosedRange<Int> {
        spread ? (2 * index + 1)...min(2 * index + 2, PAGES) : (index + 1)...(index + 1)
    }

    /// الصفحة اليمنى من الكتاب المفتوح
    public static func isRight(_ page: Int) -> Bool { page % 2 == 1 }

    /// آيات الصفحات الظاهرة كلها
    public static func ayahs(_ index: Int, _ spread: Bool) -> ClosedRange<Int> {
        let p = pages(index, spread)
        return QuranMeta.pageRange(p.lowerBound).lowerBound...QuranMeta.pageRange(p.upperBound).upperBound
    }

    /// الصفحة التي يقف عندها القارئ بعد الانتقال إلى الموضع [index]: تبقى [anchor] إن كانت ظاهرة فيه، وإلا فأول ما يظهر
    public static func follow(_ anchor: Int, _ index: Int, _ spread: Bool) -> Int {
        let p = pages(index, spread)
        return p.contains(anchor) ? anchor : p.lowerBound
    }
}
