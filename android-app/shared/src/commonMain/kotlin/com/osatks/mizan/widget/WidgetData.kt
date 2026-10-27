package com.osatks.mizan.widget

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.osatks.mizan.adhkar.Adhkar
import com.osatks.mizan.adhkar.AdhkarTr
import com.osatks.mizan.adhkar.Dhikr
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.core.HDate
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.PrayerTimes
import com.osatks.mizan.core.atStartOfDay
import com.osatks.mizan.core.plusDays
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.i18n.greg
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.i18n.time
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.i18n.weekday
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.quran.ayahOfDay
import com.osatks.mizan.ui.quran.surahName
import com.osatks.mizan.ui.quran.surahRoute
import com.osatks.mizan.ui.screens.PrayerUtil
import com.osatks.mizan.ui.screens.sourceLine
import com.osatks.mizan.ui.theme.Accent
import com.osatks.mizan.ui.theme.BrandGreen
import com.osatks.mizan.ui.theme.BrandGreenDeep
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.recolor
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** ذكر جاهز للعرض في الأداة: النص، وعدد التكرار، والمصدر، والترجمة بلغة الواجهة، والفضل */
class WidgetDhikr(
    val id: String, val ar: String, val count: Int, val countText: String?,
    val source: String, val tr: String?, val fadl: String?,
)

/** ألوان الأدوات بلون التطبيق الذي اختاره المستخدم (ARGB) */
class WidgetColors(val hero1: Int, val hero2: Int, val night1: Int, val night2: Int, val accentLight: Int, val gold: Int) {
    companion object {
        fun of(key: String): WidgetColors {
            val a = Accent.of(key)
            return WidgetColors(
                BrandGreen.recolor(a).toArgb(), BrandGreenDeep.recolor(a).toArgb(),
                Color(0xFF1B2D28).recolor(a).toArgb(), Color(0xFF0B1311).recolor(a).toArgb(),
                Color(0xFF3FB595).recolor(a).toArgb(), Gold.toArgb(),
            )
        }
    }
}

/**
 * بيانات أدوات الشاشة الرئيسية المشتركة بين أندرويد و iOS: نوافذ الأذكار وأذكارها،
 * وفي iOS لقطة JSON مسبقة الحساب لأيام قادمة (المواقيت والتاريخ وآية اليوم والأذكار) تقرؤها أدوات WidgetKit.
 */
object WidgetData {
    fun times(d: LocalDate): PrayerTimes = PrayerUtil.times(d)
    fun hijri(d: LocalDate): HDate? = Hijri.toHijri(d)

    /** نافذة الأذكار الآن في موقع المستخدم */
    fun window(now: LocalDateTime = PrayerUtil.now()): AdhkarWindow = AdhkarClock.at(now, ::times, ::hijri)

    fun zone(): TimeZone = runCatching { TimeZone.of(AppSettings.place.tz) }.getOrElse { TimeZone.currentSystemDefault() }

    /** عنوان النافذة بلغة الواجهة */
    fun title(w: AdhkarWindow, lang: Lang, wt: WidgetText, tr: AdhkarTr): String = when (w.kind) {
        "dhikr" -> wt.dhikr
        "friday" -> wt.friday
        "arafah" -> wt.arafah
        "iftar" -> wt.iftar
        "lastThird" -> wt.lastThird
        "afterPrayer" -> wt.after
        else -> {
            val id = if (w.kind == "night") "sleep" else w.kind
            if (lang == Lang.AR) Adhkar.section(id)?.titleAr ?: id else tr.sections[id]?.first ?: Adhkar.section(id)?.titleAr ?: id
        }
    }

    /** شارة صغيرة بجانب العنوان: اسم الصلاة، أو الجمعة، أو ليلة الجمعة، أو العشر الأواخر */
    fun chip(w: AdhkarWindow, s: Strings, wt: WidgetText): String? = when {
        w.prayer != null -> s.prayer(w.prayer)
        w.tag == "friday" && w.kind != "friday" -> s.weekdays[4]
        w.tag == "fridayEve" -> wt.fridayEve
        w.tag == "lastTen" -> wt.lastTen
        else -> null
    }

    /** يفك "@قسم" و"@قسم/ن" إلى أذكارها، ويحذف ما ليس في المكتبة */
    fun expand(tokens: List<String>): List<Dhikr> = tokens.flatMap { t ->
        when {
            t.startsWith("@") -> {
                val id = t.drop(1).substringBefore('/')
                val sec = Adhkar.section(id) ?: return@flatMap emptyList()
                val g = t.substringAfter('/', "").toIntOrNull()
                if (g == null) sec.items else sec.groups.getOrNull(g)?.items.orEmpty()
            }
            else -> listOfNotNull(WidgetText.EXTRA[t] ?: Adhkar.byId(t))
        }
    }.distinctBy { it.id }

    fun dhikr(d: Dhikr, lang: Lang, s: Strings, wt: WidgetText, tr: AdhkarTr): WidgetDhikr {
        val extra = WidgetText.EXTRA.containsKey(d.id)
        val t = if (lang == Lang.AR) null else when (d.id) {
            WidgetText.KAHF -> wt.kahf
            WidgetText.SALAWAT -> wt.salawat
            WidgetText.HOUR -> wt.hour
            else -> tr.items[d.id]?.t
        }
        val fadl = when {
            d.id == WidgetText.KAHF -> wt.kahfCta
            extra -> null
            lang == Lang.AR -> d.fadl
            else -> tr.items[d.id]?.fadl
        }
        val src = runCatching { sourceLine(d, tr, s) }.getOrDefault(emptyList()).take(2).joinToString(" · ")
        return WidgetDhikr(d.id, d.ar, d.count, if (d.count > 1) s.dhikrTimes(d.count) else null, src, t, fadl)
    }

    fun dhikrs(w: AdhkarWindow, lang: Lang): List<WidgetDhikr> {
        val s = lang.strings; val wt = WidgetText.of(lang.code); val tr = Adhkar.tr(lang)
        return expand(w.items).map { dhikr(it, lang, s, wt, tr) }
    }

    /** وقت الساعة لـ LocalDateTime بصيغة التطبيق (١٢/٢٤ ساعة وص/م بلغة الواجهة) */
    fun clock(s: Strings, t: LocalDateTime) = s.time(t.time.toSecondOfDay() / 3600.0)

    private fun epoch(t: LocalDateTime, z: TimeZone) = t.toInstant(z).epochSeconds

    private fun hex(c: Int) = "#" + (c and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()

    /**
     * لقطة الأدوات في iOS: [days] أيام من اليوم، بأوقات مطلقة (ثوانٍ منذ ١٩٧٠) حتى تبقى صحيحة بلا التطبيق.
     * النصوص منسقة مسبقاً بلغة التطبيق وأرقامها، فلا تحتاج الأداة إلى أي حساب.
     */
    suspend fun snapshot(days: Int = 7, now: LocalDateTime = PrayerUtil.now()): Map<String, Any?> {
        val lang = Lang.current
        val s = lang.strings
        val wt = WidgetText.of(lang.code)
        val tr = Adhkar.tr(lang)
        val z = zone()
        val today = now.date
        val c = WidgetColors.of(AppSettings.accent)

        val quran = runCatching { QuranData.uthmani() }.getOrNull()
        val meaning = Sources.translationFor(lang)?.let { src -> runCatching { QuranData.source(src.id) }.getOrNull() }

        val dayList = (0 until days).map { i ->
            val d = today.plusDays(i)
            val t = times(d)
            val h = hijri(d)
            val ayah = quran?.let { q ->
                val idx = ayahOfDay(q, d)
                val (sn, an) = QuranMeta.locate(idx)
                mapOf(
                    "text" to QuranData.withoutBismillah(q, idx),
                    "ref" to s.surahAyah(s.surahName(sn), an),
                    "meaning" to meaning?.getOrNull(idx),
                    "route" to surahRoute(sn, an, "widget"),
                )
            }
            mapOf(
                "date" to d.toString(),
                "start" to epoch(d.atStartOfDay(), z),
                "weekday" to s.weekday(d),
                "friday" to (d.dayOfWeek == kotlinx.datetime.DayOfWeek.FRIDAY),
                "greg" to s.greg(d),
                "hijri" to (h?.let { s.hijriDate(it.d, it.m, it.y) } ?: ""),
                "hijriDay" to (h?.d?.toString() ?: ""),
                "hijriMonth" to (h?.let { s.hijriMonthTitle(it.m, it.y) } ?: ""),
                "times" to PrayTimes.PRAYERS.filter { !t[it].isNaN() }.map { k ->
                    mapOf(
                        "k" to k, "name" to s.prayer(k),
                        "at" to epoch(PrayerUtil.at(d, t[k]), z),
                        "t" to s.time(t[k]),
                        "s" to PrayTimes.format(t[k], AppSettings.use24h, "", "").trim(),
                    )
                },
                "ayah" to ayah,
            )
        }

        // نوافذ الأذكار من ليلة أمس (ما قبل فجر اليوم) إلى آخر الأيام
        val windows = AdhkarClock.range(today.plusDays(-1), days + 1, ::times, ::hijri).filter { epoch(it.end, z) > epoch(now, z) - 3600 }
        val used = LinkedHashMap<String, Dhikr>()
        val winList = windows.map { w ->
            val items = expand(w.items)
            items.forEach { used[it.id] = it }
            mapOf(
                "k" to w.kind, "key" to w.key,
                "s" to epoch(w.start, z), "e" to epoch(w.end, z),
                "title" to title(w, lang, wt, tr), "chip" to chip(w, s, wt),
                "until" to wt.untilTime(clock(s, w.end)),
                "section" to w.section,
                "items" to items.map { it.id },
            )
        }
        val itemMap = used.values.associate { d ->
            val x = dhikr(d, lang, s, wt, tr)
            d.id to mapOf("ar" to x.ar, "n" to x.count, "c" to x.countText, "src" to x.source, "tr" to x.tr, "fadl" to x.fadl)
        }

        return mapOf(
            "v" to 1,
            "at" to epoch(now, z),
            "lang" to lang.code, "rtl" to lang.rtl,
            "theme" to AppSettings.theme,
            "colors" to mapOf(
                "hero1" to hex(c.hero1), "hero2" to hex(c.hero2), "night1" to hex(c.night1), "night2" to hex(c.night2),
                "accent" to hex(c.accentLight), "gold" to hex(c.gold),
            ),
            "place" to s.placeName(AppSettings.place),
            "rotate" to AdhkarClock.ROTATE_MIN,
            "labels" to mapOf(
                "adhkar" to wt.adhkar, "nextPrayer" to wt.nextPrayer, "prayerTimes" to wt.prayerTimes, "hijri" to wt.hijri,
                "ayah" to s.ayahOfDay, "next" to wt.next, "in" to wt.inTime, "openApp" to wt.openApp,
                "descNext" to wt.descNext, "descTimes" to wt.descTimes, "descHijri" to wt.descHijri,
                "descAyah" to wt.descAyah, "descAdhkar" to wt.descAdhkar,
            ),
            "days" to dayList,
            "windows" to winList,
            "items" to itemMap,
        )
    }

    suspend fun snapshotJson(): String = Json.write(snapshot())
}

/** الشاشة التي تفتحها الأداة: قسم الأذكار (يقرؤه AdhkarScreen مرة واحدة)، وفي iOS الشاشة نفسها من رابط الأداة */
object WidgetLaunch {
    var adhkarSection: String? = null
    var route by mutableStateOf<String?>(null)

    fun takeAdhkarSection(): String = (adhkarSection ?: "").also { adhkarSection = null }
}
