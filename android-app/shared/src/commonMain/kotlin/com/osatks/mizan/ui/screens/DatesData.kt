package com.osatks.mizan.ui.screens

import com.osatks.mizan.core.*
import com.osatks.mizan.core.HDate
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.i18n.greg
import com.osatks.mizan.i18n.hijri
import com.osatks.mizan.i18n.weekday
import kotlinx.datetime.LocalDate
import com.osatks.mizan.i18n.Strings

internal data class IslamicEvent(val m: Int, val d: Int, val key: String, val emoji: String)

internal val EVENTS = listOf(
    IslamicEvent(9, 1, "ramadan", "🌙"),
    IslamicEvent(9, 21, "lastTen", "✨"),
    IslamicEvent(10, 1, "eidFitr", "🎉"),
    IslamicEvent(10, 2, "shawwal", "📿"),
    IslamicEvent(12, 1, "dhulHijjah", "🕋"),
    IslamicEvent(12, 9, "arafah", "🤲"),
    IslamicEvent(12, 10, "eidAdha", "🐑"),
    IslamicEvent(1, 1, "newYear", "📅"),
    IslamicEvent(1, 10, "ashura", "🌊"),
)

/** المناسبات القادمة (من اليوم) بتواريخها الميلادية وسنتها الهجرية، الأقرب أولاً */
internal fun upcomingEvents(today: LocalDate): List<Triple<IslamicEvent, LocalDate, Int>> {
    val th = Hijri.toHijri(today) ?: return emptyList()
    return EVENTS.mapNotNull { e ->
        listOf(th.y, th.y + 1, th.y + 2).firstNotNullOfOrNull { y -> Hijri.toGregorian(y, e.m, e.d)?.takeIf { !it.isBefore(today) }?.let { Triple(e, it, y) } }
    }.sortedBy { it.second }
}

/** نتيجة التحويل بين التقويمين: التاريخ المقابل وسطر تحته (اليوم والتاريخ الأصلي)، أو «—» وطلب السنة */
internal fun dateConversion(s: Strings, dir: String, gy: String, gm: Int, gd: Int, hy: String, hm: Int, hd: Int): Pair<String, String> =
    if (dir == "g2h") {
        val y = gy.toIntOrNull()
        val g = if (y != null && y in 1882..2174) runCatching { LocalDate.of(y, gm, gd.coerceAtMost(monthLength(y, gm))) }.getOrNull() else null
        val h = g?.let { Hijri.toHijri(it) }
        if (g != null && h != null) s.hijri(h) to "${s.weekday(g)} — ${s.greg(g)}" else "—" to s.enterGregYear
    } else {
        val y = hy.toIntOrNull()
        val g = if (y != null) Hijri.toGregorian(y, hm, hd) else null
        if (g != null && y != null) s.greg(g) to "${s.weekday(g)} — ${s.hijri(HDate(y, hm, hd))}" else "—" to s.enterHijriYear
    }

/** تاريخ الميلاد من الحقول (هجرياً أو ميلادياً)، أو null إن لم يكتمل */
internal fun birthDate(hijri: Boolean, y: String, m: Int, d: Int): LocalDate? = y.toIntOrNull()?.let { yi ->
    if (hijri) Hijri.toGregorian(yi, m, d)
    else if (yi in 1800..2200) runCatching { LocalDate.of(yi, m, d.coerceAtMost(monthLength(yi, m))) }.getOrNull() else null
}

/** العمر بالتقويم الهجري (سنوات وأشهر وأيام) */
internal fun hijriAge(s: Strings, birth: LocalDate, today: LocalDate): String {
    val hb = Hijri.toHijri(birth); val ht = Hijri.toHijri(today)
    return if (hb != null && ht != null) {
        var yy = ht.y - hb.y; var mm = ht.m - hb.m; var dd = ht.d - hb.d
        if (dd < 0) { mm--; val pm = if (ht.m == 1) 12 else ht.m - 1; val py = if (ht.m == 1) ht.y - 1 else ht.y; dd += Hijri.monthLength(py, pm) }
        if (mm < 0) { yy--; mm += 12 }
        s.ymd(yy, mm, dd)
    } else "—"
}
