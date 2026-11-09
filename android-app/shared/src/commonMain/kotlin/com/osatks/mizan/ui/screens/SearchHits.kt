package com.osatks.mizan.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Place
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.Search
import com.osatks.mizan.i18n.Ar
import com.osatks.mizan.i18n.En
import com.osatks.mizan.i18n.city
import com.osatks.mizan.i18n.country
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.i18n.time
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranSearch
import com.osatks.mizan.ui.quran.ayahRef
import com.osatks.mizan.ui.quran.surahName
import com.osatks.mizan.ui.quran.surahRoute
import androidx.compose.material.icons.automirrored.filled.MenuBook
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.i18n.Lang

/** طلب مؤجل لصفحة تُفتح من البحث (مناسبة بعينها) */
object Pending {
    var event: String? = null
}

internal class SearchHit(
    val group: String, val id: String, val title: String, val subtitle: String,
    val icon: ImageVector, val score: Int, val action: () -> Unit
)

/**
 * نتائج البحث الشامل عن [q] مرتبةً: أفضل مجموعة أولاً (بحسب أعلى نتيجة فيها)، وداخلها الأعلى درجة. بلا بحث: الأدوات.
 * [ayahHits] آيات مطابقة حُسبت في الخلفية، و[quran] نص المصحف (لسطر الآية) إن كان جاهزاً
 */
internal fun searchHits(
    s: Strings, lang: Lang, q: String, ayahHits: List<Int>, quran: Array<String>?,
    open: (String) -> Unit, showLanguage: () -> Unit,
): List<SearchHit> {
    val terms = Search.terms(q)
    val out = mutableListOf<SearchHit>()
    fun add(group: String, id: String, title: String, sub: String, icon: ImageVector, extra: String, action: () -> Unit) {
        val sc = if (terms.isEmpty()) (if (group == s.searchToolsGroup) 1 else 0) else Search.score(terms, title, "$sub $extra")
        if (sc > 0) out += SearchHit(group, id, title, sub, icon, sc, action)
    }
    TOOL_ORDER.forEach { k ->
        val t = s.tools.getValue(k)
        add(s.searchToolsGroup, "t-$k", t.name, t.desc, TOOL_ICONS.getValue(k),
            "${s.toolKeywords[k]} ${En.tools[k]?.name} ${En.toolKeywords[k]} ${Ar.tools[k]?.name} ${Ar.toolKeywords[k]}") { open(k) }
    }
    if (terms.isNotEmpty()) {
        for (n in 1..QuranMeta.SURAHS) {
            add(s.searchQuranGroup, "q-$n", s.surahName(n),
                "${QuranMeta.NAMES_AR[n - 1]} · ${if (QuranMeta.isMedinan(n)) s.medinan else s.meccan} · ${s.ayahCount(QuranMeta.COUNTS[n - 1])}",
                Icons.AutoMirrored.Filled.MenuBook,
                "$n ${QuranMeta.NAMES_AR[n - 1]} ${QuranMeta.TNAMES[n - 1]} ${QuranMeta.ENAMES[n - 1]} ${En.surahName(n)} ${Ar.surahName(n)} ${s.quranTitle}") {
                open(surahRoute(n, 0, "hit"))
            }
        }
        val refs = (listOfNotNull(QuranSearch.parseRef(q)) + ayahHits).distinct()
        refs.forEachIndexed { k, i ->
            val (sn, an) = QuranMeta.locate(i)
            val sub = quran?.let { QuranData.withoutBismillah(it, i) } ?: ""
            out += SearchHit(s.searchAyahsGroup, "v-$i", s.ayahRef(i), sub, Icons.AutoMirrored.Filled.MenuBook, 40 - k) { open(surahRoute(sn, an, "hit")) }
        }
        val today = PrayerUtil.times(PrayerUtil.now().date)
        PrayTimes.PRAYERS.forEach { k ->
            add(s.searchPrayersGroup, "p-$k", s.prayer(k), s.time(today[k]), Icons.Filled.AccessTime,
                "${En.prayer(k)} ${Ar.prayer(k)} ${s.prayerTitle}") { open("prayer") }
        }
        PrayTimes.CITIES.forEach { c ->
            val country = s.country(c.cc, lang)
            add(s.searchCitiesGroup, "c-${c.key}", s.city(c.key), "$country · ${s.setAsMyCity}", Icons.Filled.LocationCity,
                "${En.city(c.key)} ${Ar.city(c.key)} ${c.cc}") {
                AppSettings.updatePlace(Place.of(c)); PrayerAlerts.reschedule(); open("prayer")
            }
        }
        s.events.forEach { (k, e) ->
            add(s.searchEventsGroup, "e-$k", e.name, e.desc, Icons.Filled.Event,
                "${En.events[k]?.name} ${Ar.events[k]?.name}") { Pending.event = k; open("events") }
        }
        s.dhikrLabels.forEachIndexed { i, label ->
            val meaning = s.dhikrMeanings[i].ifEmpty { DHIKR[i] }
            add(s.searchDhikrGroup, "d-$i", label, meaning, Icons.Filled.TouchApp,
                "${En.dhikrLabels[i]} ${En.dhikrMeanings[i]} ${Ar.dhikrLabels[i]} ${DHIKR[i]}") {
                AppSettings.putString("ts_mode", "free"); AppSettings.putInt("ts_idx", i); AppSettings.putInt("ts_count", 0)
                open("tasbih")
            }
        }
        add(s.searchSettingsGroup, "s-lang", s.language, lang.native, Icons.Filled.Translate,
            "${En.language} ${Ar.language} language lang") { showLanguage() }
        add(s.searchSettingsGroup, "s-theme", s.appearance, "${s.themeLight} · ${s.themeDark}", Icons.Filled.DarkMode,
            "${En.appearance} ${En.themeDark} ${Ar.appearance} ${Ar.themeDark}") { open("settings") }
        add(s.searchSettingsGroup, "s-adhan", s.adhanAlert, s.adhanAlertDesc, Icons.Filled.NotificationsActive,
            "${En.adhanAlert} ${Ar.adhanAlert}") { open("settings") }
        add(s.searchSettingsGroup, "s-24h", s.use24h, s.use24hDesc, Icons.Filled.AccessTime,
            "${En.use24h} ${Ar.use24h}") { open("settings") }
        add(s.searchSettingsGroup, "s-method", s.calcMethod, s.locationAndMethod, Icons.Filled.Tune,
            "${En.calcMethod} ${Ar.calcMethod} ${s.methods.values.joinToString(" ")}") { open("prayer") }
    }
    val order = listOf(s.searchToolsGroup, s.searchQuranGroup, s.searchAyahsGroup, s.searchPrayersGroup, s.searchCitiesGroup, s.searchEventsGroup, s.searchDhikrGroup, s.searchSettingsGroup)
    // أفضل مجموعة أولاً (بحسب أعلى نتيجة فيها)، وداخلها الأعلى درجة
    val groups = out.groupBy { it.group }
    return groups.keys.sortedWith(compareByDescending<String> { g -> groups.getValue(g).maxOf { it.score } }.thenBy { order.indexOf(it) })
        .flatMap { g -> groups.getValue(g).sortedByDescending { it.score }.take(if (terms.isEmpty()) 20 else if (g == s.searchCitiesGroup) 8 else 6) }
}
