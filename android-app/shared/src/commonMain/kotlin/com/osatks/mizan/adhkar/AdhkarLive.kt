package com.osatks.mizan.adhkar

import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.now
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.ui.screens.PrayerUtil
import com.osatks.mizan.widget.LiveText
import kotlinx.datetime.LocalDate

/**
 * عدّاد الأذكار على شاشة القفل وفي الجزيرة الديناميكية (iOS: زر الإجراء و Siri وأزرار النشاط الحي):
 * أذكار الصباح من الفجر إلى العصر، والمساء من العصر إلى الفجر. كل ضغطة تعدّ مرة من الذكر الحالي، فإذا بلغ عدده
 * انتقل إلى الذي بعده، حتى يتم القسم. الموضع محفوظ لليوم والقسم (al_*)، فيُتابَع من حيث تُرك.
 */
object AdhkarLive {
    /** ما حدث مع الضغطة: عدّ، أو انتقال إلى الذكر التالي، أو إتمام القسم */
    enum class Event { COUNT, NEXT, DONE }

    data class Position(val section: String, val index: Int, val count: Int, val day: String, val done: Boolean = false)

    /** أذكار الوقت الآن: الصباح من الفجر حتى العصر، وإلا المساء */
    fun sectionNow(): String {
        val now = PrayerUtil.now()
        val t = PrayerUtil.times(now.date)
        val h = now.hour + now.minute / 60.0
        val fajr = t["fajr"]
        val asr = t["asr"]
        return if (!fajr.isNaN() && !asr.isNaN() && h >= fajr && h < asr) "morning" else "evening"
    }

    /** موضع القسم: المحفوظ إن كان القسم نفسه اليوم، وإلا من أوله */
    fun position(section: String = sectionNow()): Position {
        AppSettings.init()
        val today = LocalDate.now().toString()
        val saved = Position(
            AppSettings.getString("al_section", ""), AppSettings.getInt("al_index", 0),
            AppSettings.getInt("al_count", 0), AppSettings.getString("al_day", ""), AppSettings.getBool("al_done", false),
        )
        return if (saved.section == section && saved.day == today) saved else Position(section, 0, 0, today)
    }

    private fun save(p: Position) {
        AppSettings.putString("al_section", p.section)
        AppSettings.putInt("al_index", p.index)
        AppSettings.putInt("al_count", p.count)
        AppSettings.putString("al_day", p.day)
        AppSettings.putBool("al_done", p.done)
    }

    private fun items(section: String): List<Dhikr> = Adhkar.section(section)?.items.orEmpty()

    /** ضغطة: مرة من الذكر الحالي، والانتقال إلى التالي عند تمام عدده */
    fun count(section: String = sectionNow()): Pair<Position, Event> {
        val p = position(section)
        val list = items(section)
        if (list.isEmpty() || p.done) return p to Event.DONE
        val d = list[p.index.coerceIn(list.indices)]
        val c = p.count + 1
        val (next, event) = when {
            c < d.count.coerceAtLeast(1) -> p.copy(count = c) to Event.COUNT
            p.index + 1 < list.size -> p.copy(index = p.index + 1, count = 0) to Event.NEXT
            else -> p.copy(count = 0, done = true) to Event.DONE
        }
        save(next)
        return next to event
    }

    /** الذكر التالي دون إتمام عدد الحالي */
    fun skip(section: String = sectionNow()): Pair<Position, Event> {
        val p = position(section)
        val list = items(section)
        if (list.isEmpty() || p.done) return p to Event.DONE
        val next = if (p.index + 1 < list.size) p.copy(index = p.index + 1, count = 0) else p.copy(count = 0, done = true)
        save(next)
        return next to if (next.done) Event.DONE else Event.NEXT
    }

    /** يبدأ القسم من أوله (بعد إتمامه أو من زر «من البداية») */
    fun restart(section: String = sectionNow()): Position {
        val p = Position(section, 0, 0, LocalDate.now().toString())
        save(p)
        return p
    }

    /** نص قصير بقدر ما يسعه النشاط الحي: يُقطع عند آخر كلمة قبل [max] حرفاً */
    internal fun clip(text: String, max: Int): String {
        val t = text.replace('\n', ' ').trim()
        if (t.length <= max) return t
        val cut = t.lastIndexOf(' ', max).takeIf { it > max / 2 } ?: max
        return t.substring(0, cut).trimEnd('،', ',', ' ') + " …"
    }

    /** الحال للنشاط الحي (AdhkarLiveState في Swift) */
    fun state(p: Position, lang: Lang = Lang.current): Map<String, Any?> {
        val s = lang.strings
        val sec = Adhkar.section(p.section)
        val tr = Adhkar.tr(lang)
        val list = sec?.items.orEmpty()
        val d = list.getOrNull(p.index.coerceIn(0, (list.size - 1).coerceAtLeast(0)))
        val title = if (lang == Lang.AR) sec?.titleAr.orEmpty() else tr.sections[p.section]?.first ?: sec?.titleAr.orEmpty()
        val n = d?.count?.coerceAtLeast(1) ?: 1
        val done = p.done
        val progress = if (list.isEmpty()) 0.0 else if (done) 1.0 else (p.index + p.count.toDouble() / n) / list.size
        val meaning = if (lang == Lang.AR || d == null) "" else tr.items[d.id]?.t?.let { clip(it, 90) }.orEmpty()
        return linkedMapOf(
            "id" to "adhkar-${p.section}",
            "section" to p.section,
            "title" to title,
            "text" to if (done) "" else d?.ar?.let { clip(it, 130) }.orEmpty(),
            "meaning" to if (done) "" else meaning,
            "count" to p.count,
            "goal" to n,
            "countText" to "${p.count}/$n",
            "position" to "${(p.index + 1).coerceAtMost(list.size)}/${list.size}",
            "progress" to progress.coerceIn(0.0, 1.0),
            "done" to done,
            "doneText" to LiveText.of(lang.code).adhkarDone(title),
            "next" to com.osatks.mizan.widget.WidgetText.of(lang.code).next,
            "end" to LiveText.of(lang.code).end,
            "tap" to s.tapToCount,
            "rtl" to lang.rtl,
        )
    }
}
