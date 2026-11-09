package com.osatks.mizan.ui.screens

import com.osatks.mizan.core.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.osatks.mizan.adhkar.AdhkarSection
import com.osatks.mizan.adhkar.AdhkarTr
import com.osatks.mizan.adhkar.Dhikr
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.quran.QuranMeta
import kotlinx.datetime.LocalDate

/** القسم المناسب للوقت: الصباح حتى الظهر، والمساء من العصر، والنوم بعد العشاء، وفي رمضان أذكاره */
internal fun suggested(hour: Int = kotlinx.datetime.LocalDateTime.now().hour): String = when (hour) {
    in 4..11 -> "morning"
    in 15..20 -> "evening"
    in 21..23, in 0..1 -> "sleep"
    else -> if (Hijri.toHijri(LocalDate.now())?.m == 9) "ramadan" else "afterPrayer"
}

internal fun doneKey(id: String) = "adhkar_done_$id"

internal fun doneToday(id: String) = AppSettings.getInt(doneKey(id), -1) == LocalDate.now().toEpochDay().toInt()

internal fun AdhkarTr.title(sec: AdhkarSection, lang: Lang) = if (lang == Lang.AR) sec.titleAr else sections[sec.id]?.first ?: sec.titleAr

internal fun AdhkarTr.desc(sec: AdhkarSection, lang: Lang) = if (lang == Lang.AR) sec.descAr else sections[sec.id]?.second ?: sec.descAr

/** الترجمة المعروضة تحت الذكر: ترجمته، أو ترجمة معاني آياته إن كان قرآناً */
internal fun translation(d: Dhikr, tr: AdhkarTr, lang: Lang, quran: Array<String>?): String? {
    if (lang == Lang.AR) return null
    tr.items[d.id]?.t?.let { return it }
    if (d.ayah.isEmpty() || quran == null) return null
    return d.ayah.joinToString("\n") { (su, a, b) -> (a..b).joinToString(" ") { quran[QuranMeta.index(su, it)] } }
}

/** "صحيح البخاري 6306 · صحيح مسلم 2702"، وللقرآن اسم السورة ورقم الآية */
internal fun sourceLine(d: Dhikr, tr: AdhkarTr, s: Strings): List<String> = d.src.map { (b, n) ->
    if (b == "quran") {
        val su = n.substringBefore(':').toIntOrNull()
        val name = su?.let { s.surahNames.getOrNull(it - 1) }
        if (name != null) s.surahAyah(name, n.substringAfter(':').substringBefore('-').toIntOrNull() ?: 1) + (if ('-' in n) "–" + n.substringAfter('-') else "") else n
    } else "${tr.books[b] ?: b} ${n}".trim()
}

internal fun gradeLine(d: Dhikr, tr: AdhkarTr): String =
    listOfNotNull(tr.grades[d.grade] ?: d.grade, d.by.takeIf { it.isNotEmpty() }?.let { tr.by[it] ?: it }).joinToString(" · ")

internal fun shareBody(d: Dhikr, tr: AdhkarTr, lang: Lang, s: Strings, t: String?) = buildString {
    append(d.ar)
    if (t != null) append("\n\n").append(t)
    append("\n\n").append(sourceLine(d, tr, s).joinToString(" · "))
    append(" (").append(gradeLine(d, tr)).append(")")
}

// ---------- الدليل ----------
