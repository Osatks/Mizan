package com.osatks.mizan.widget

import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.core.Place
import com.osatks.mizan.core.Tasbih
import com.osatks.mizan.core.TasbihState
import com.osatks.mizan.core.plusSeconds
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.hijri
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.prayer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * حال الأنشطة الحية وأداة المسبحة كما يفكّها Swift (iosApp/Shared/LiveStates.swift): مفاتيح كل خريطة هنا هي أسماء
 * الحقول هناك بالضبط. أي اختلاف (مفتاح ناقص أو باسم آخر أو بنوع آخر) يُفشل فكّ JSON كله في Swift بلا خطأ ظاهر،
 * فيختفي النشاط أو يبقى بلا تحديث؛ لذا يتحقق LiveStatesTest من المفاتيح والأنواع بكل اللغات.
 * دوال خالصة: كل ما تحتاجه يُمرَّر إليها، فتُختبر على JVM. يستدعيها IosLive و IosTasbih و IosAdhkar بإعدادات المستخدم.
 */
object LiveStates {
    /** يبقى العدّ التنازلي بعد دخول الوقت عشر دقائق */
    const val GRACE_SEC = 600L

    /** ألوان النشاط بلون التطبيق الذي اختاره المستخدم */
    fun palette(accent: String): Map<String, Any?> {
        val c = WidgetColors.of(accent)
        return mapOf("hero1" to WidgetData.hex(c.hero1), "hero2" to WidgetData.hex(c.hero2), "gold" to WidgetData.hex(c.gold))
    }

    /** المسبحة: النشاط الحي وأداة الشاشة الرئيسية وزر مركز التحكم (TasbihLiveState) */
    fun tasbih(st: TasbihState, lang: Lang, accent: String, at: Long): Map<String, Any?> {
        val s = lang.strings
        val lt = LiveText.of(lang.code)
        val goal = st.goal
        val progress = if (goal > 0) st.count.toDouble() / goal else (st.count % 100) / 100.0
        val mode = if (st.mode == "salah") "${s.modeAfterPrayer} · ${st.step + 1}/${Tasbih.SALAH.size}" else ""
        return linkedMapOf<String, Any?>(
            "id" to "tasbih",
            "text" to st.text,
            // المعنى تحت الذكر لغير العربية
            "meaning" to if (lang == Lang.AR) "" else st.meaning(s),
            "count" to st.count,
            "goal" to goal,
            "progress" to progress.coerceIn(0.0, 1.0),
            "countText" to st.count.toString(),
            "goalText" to if (goal > 0) s.ofGoal(goal) else s.noLimit,
            "rounds" to Fmt.int(st.rounds.toLong()),
            "roundsLabel" to s.roundsDone,
            "today" to Fmt.int(st.today.toLong()),
            "todayLabel" to s.todayCount,
            "mode" to mode,
            "title" to lt.tasbih,
            "tap" to s.tapToCount,
            "reset" to s.reset,
            "end" to lt.end,
            "rtl" to lang.rtl,
            "at" to at,
        ) + palette(accent)
    }

    /**
     * العدّ التنازلي للصلاة القادمة (PrayerLiveState). [atSec] وقتها و [from] دخول الصلاة قبلها (ثوانٍ منذ ١٩٧٠)،
     * و [today] يوم المستخدم الآن (للتاريخ الهجري)، و [iqamaMin] دقائق الإقامة بعد الأذان (0 بلا إقامة).
     */
    fun prayer(
        key: String, at: LocalDateTime, atSec: Long, from: Long, today: LocalDate,
        lang: Lang, place: Place, iqamaMin: Int, h24: Boolean, accent: String,
    ): Map<String, Any?> {
        val s = lang.strings
        val lt = LiveText.of(lang.code)
        val name = s.prayer(key)
        // الإقامة بعد الأذان بما ضبطه المستخدم لهذه الصلاة: بعد دخول الوقت يعدّ النشاط إليها
        val iq = if (key == "sunrise") 0 else iqamaMin.coerceAtLeast(0)
        val iqama = if (iq > 0) atSec + iq * 60L else null
        // في رمضان: المغرب موعد الإفطار (ودعاؤه عند دخوله)، والفجر نهاية السحور
        val ramadan = Hijri.toHijri(at.date)?.m == 9
        val iftar = ramadan && key == "maghrib"
        val label = when {
            iftar -> lt.iftar
            ramadan && key == "fajr" -> lt.suhoor
            else -> WidgetText.of(lang.code).nextPrayer
        }
        return linkedMapOf<String, Any?>(
            "id" to "${at.date}-$key",
            "key" to key,
            "name" to name,
            "time" to WidgetData.clock(s, at, h24),
            "at" to atSec,
            "from" to from,
            "hijri" to s.hijri(Hijri.toHijri(today)),
            "place" to s.placeName(place),
            "label" to label,
            "now" to if (iftar) lt.iftarNow else s.prayerNowTitle(name),
            "rtl" to lang.rtl,
            "iqama" to iqama,
            "iqamaLabel" to lt.iqamaIn,
            "iqamaTime" to iqama?.let { WidgetData.clock(s, at.plusSeconds(iq * 60L), h24) },
            "dua" to if (iftar) LiveText.IFTAR_DUA else null,
            // ينتهي النشاط بعد دخول الوقت بعشر دقائق، أو بعد الإقامة بدقيقتين إن تأخرت عنها
            "until" to maxOf(atSec + GRACE_SEC, (iqama ?: 0L) + 120L),
        ) + palette(accent)
    }
}
