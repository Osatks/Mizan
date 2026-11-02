package com.osatks.mizan.widget

import com.osatks.mizan.adhkar.AdhkarLive
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.quran.ayahOfDay
import com.osatks.mizan.ui.quran.surahName
import com.osatks.mizan.ui.quran.surahRoute
import com.osatks.mizan.ui.screens.PrayerUtil
import kotlinx.coroutines.runBlocking

/**
 * جسر عدّاد الأذكار (نشاط حي يبدؤه زر الإجراء أو Siri، وأزراره «عُدّ» و«التالي» و«إنهاء») وآية اليوم لـ Siri.
 * تستدعيه النوايا في Swift (Shared/LiveIntents.swift) وتأخذ رسالة JSON: الحال، وما حدث، ونص التنبيه عند الإتمام.
 * [section]: قسم النشاط الظاهر (morning أو evening)، أو null لأذكار الوقت الآن.
 */
object IosAdhkar {
    fun current(section: String?): String = run(section) { s -> AdhkarLive.position(s) to null }

    fun count(section: String?): String = run(section) { s -> AdhkarLive.count(s) }

    fun skip(section: String?): String = run(section) { s -> AdhkarLive.skip(s) }

    /** بعد إتمام القسم: من أوله */
    fun restart(section: String?): String = run(section) { s -> AdhkarLive.restart(s) to null }

    private inline fun run(section: String?, action: (String) -> Pair<AdhkarLive.Position, AdhkarLive.Event?>): String {
        AppSettings.init()
        val s = section?.takeIf { it == "morning" || it == "evening" } ?: AdhkarLive.sectionNow()
        val (p, e) = action(s)
        val state = AdhkarLive.state(p) + IosLive.palette(AppSettings.accent)
        val event = when (e) {
            null -> "none"
            AdhkarLive.Event.COUNT -> "count"
            AdhkarLive.Event.NEXT -> "next"
            AdhkarLive.Event.DONE -> "done"
        }
        return Json.write(mapOf("state" to state, "event" to event, "alertTitle" to (state["doneText"] ?: ""), "alertBody" to ""))
    }

    /** آية اليوم لـ Siri: نصها ومرجعها ومعناها بلغة التطبيق، وعبارة تقرؤها Siri (المعنى، أو النص بالعربية) */
    fun ayahOfDayJson(): String? = runCatching {
        AppSettings.init()
        val lang = Lang.current
        val s = lang.strings
        val q = QuranData.cached(QuranData.UTHMANI) ?: runBlocking { QuranData.uthmani() }
        val idx = ayahOfDay(q, PrayerUtil.now().date)
        val (surah, ayah) = QuranMeta.locate(idx)
        val text = QuranData.withoutBismillah(q, idx)
        val ref = s.surahAyah(s.surahName(surah), ayah)
        val meaning = if (lang == Lang.AR) null else Sources.translationFor(lang)?.let { src ->
            (QuranData.cached("tafsir/${src.id}.txt") ?: runBlocking { QuranData.source(src.id) }).getOrNull(idx)
        }
        Json.write(
            linkedMapOf<String, Any?>(
                "title" to s.ayahOfDay, "text" to text, "ref" to ref, "meaning" to meaning,
                "dialog" to "${meaning ?: text} — $ref",
                "route" to surahRoute(surah, ayah, "widget"),
                "rtl" to lang.rtl,
            ) + IosLive.palette(AppSettings.accent)
        )
    }.getOrNull()
}
