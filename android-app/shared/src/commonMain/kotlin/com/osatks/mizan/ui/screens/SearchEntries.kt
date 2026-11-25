package com.osatks.mizan.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.osatks.mizan.adhkar.Adhkar
import com.osatks.mizan.hadith.Hadiths
import com.osatks.mizan.i18n.Ar
import com.osatks.mizan.i18n.En
import com.osatks.mizan.i18n.HomeText
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.ui.quran.surahName
import kotlinx.datetime.LocalDate

/**
 * عنصر ثابت في البحث (أداة، مناسبة، قسم إعدادات، قسم أذكار، حديث): يشترك فيه البحث داخل التطبيق وبحث النظام
 * (Spotlight في iPhone) فيبقيان متفقين. [extra] كلمات بحث إضافية مفصولة بمسافات، و[link] الرابط الذي يفتح الصفحة
 * (مسار، وقد يحمل معاملاً لصفحته: انظر [SearchLinks]).
 */
internal class SearchEntry(
    val group: String, val id: String, val title: String, val subtitle: String,
    val icon: ImageVector, val extra: String, val link: String,
)

/** الأدوات بترتيب الرئيسية، بكلماتها في لغة الواجهة والإنجليزية والعربية */
internal fun toolEntries(s: Strings): List<SearchEntry> = TOOL_ORDER.mapNotNull { k ->
    val t = s.tools[k] ?: return@mapNotNull null
    SearchEntry(
        s.searchToolsGroup, "t-$k", t.name, t.desc, TOOL_ICONS.getValue(k),
        words(s.toolKeywords[k], En.tools[k]?.name, En.toolKeywords[k], Ar.tools[k]?.name, Ar.toolKeywords[k]), k,
    )
}

/** المناسبات: كل واحدة تفتح صفحة المناسبات عليها */
internal fun eventEntries(s: Strings): List<SearchEntry> = s.events.map { (k, e) ->
    SearchEntry(s.searchEventsGroup, "e-$k", e.name, e.desc, Icons.Filled.Event, words(En.events[k]?.name, Ar.events[k]?.name), SearchLinks.event(k))
}

/** أقسام الإعدادات التي يصل إليها البحث (s-lang تفتح ورقة اللغة في البحث داخل التطبيق، والإعدادات من خارجه) */
internal fun settingsEntries(s: Strings, lang: Lang): List<SearchEntry> {
    val g = s.searchSettingsGroup
    return listOf(
        SearchEntry(g, "s-lang", s.language, lang.native, Icons.Filled.Translate,
            words(En.language, Ar.language, "language lang"), "settings#lang"),
        SearchEntry(g, "s-theme", s.appearance, "${s.themeLight} · ${s.themeDark}", Icons.Filled.DarkMode,
            words(En.appearance, En.themeDark, Ar.appearance, Ar.themeDark), "settings#theme"),
        SearchEntry(g, "s-adhan", s.adhanAlert, s.adhanAlertDesc, Icons.Filled.NotificationsActive,
            words(En.adhanAlert, Ar.adhanAlert), "settings#adhan"),
        SearchEntry(g, "s-24h", s.use24h, s.use24hDesc, Icons.Filled.AccessTime,
            words(En.use24h, Ar.use24h), "settings#24h"),
        SearchEntry(g, "s-method", s.calcMethod, s.locationAndMethod, Icons.Filled.Tune,
            words(En.calcMethod, Ar.calcMethod, s.methods.values.joinToString(" ")), "prayer#method"),
    )
}

/** أقسام الأذكار (الصباح، المساء، النوم…) بعناوينها في لغة الواجهة والعربية والإنجليزية؛ كل قسم يفتح الأذكار عليه */
internal fun adhkarEntries(s: Strings, lang: Lang): List<SearchEntry> {
    val sections = runCatching { Adhkar.sections() }.getOrNull().orEmpty()
    if (sections.isEmpty()) return emptyList()
    val tr = Adhkar.tr(lang)
    val en = if (lang == Lang.EN) tr else Adhkar.tr(Lang.EN)
    val adhkar = s.tools["adhkar"]?.name.orEmpty()
    return sections.map { sec ->
        SearchEntry(
            adhkar, "a-${sec.id}", tr.title(sec, lang), "${tr.desc(sec, lang)} · ${s.dhikrItems(sec.items.size)}",
            Icons.Filled.AutoAwesome, words(sec.titleAr, en.title(sec, Lang.EN), adhkar, Ar.tools["adhkar"]?.name), SearchLinks.adhkar(sec.id),
        )
    }
}

/** الأحاديث بعناوينها وحدها (طرف الحديث بلغة الواجهة، والعربي كلمات بحث)؛ كل حديث يفتح صفحة الحديث عليه */
internal fun hadithEntries(lang: Lang): List<SearchEntry> {
    val ids = runCatching { Hadiths.ids() }.getOrNull().orEmpty()
    val label = HomeText.of(lang.code).hadithOfDay
    return ids.mapNotNull { id ->
        val h = runCatching { Hadiths.get(id, lang.code) }.getOrNull() ?: return@mapNotNull null
        val title = (h.tr?.title ?: h.ar.title).ifBlank { return@mapNotNull null }
        val grade = (h.tr?.grade ?: h.ar.grade).ifBlank { label }
        SearchEntry(label, "h-$id", title, grade, Icons.Filled.FormatQuote, words(h.ar.title), SearchLinks.hadith(id))
    }
}

/** كلمات البحث عن السورة [n] غير اسمها بلغة الواجهة: رقمها واسمها بالعربية وبالحروف اللاتينية ومعناه (البحث داخل التطبيق وSiri) */
internal fun surahSearchExtra(s: Strings, n: Int): String =
    "$n ${QuranMeta.NAMES_AR[n - 1]} ${QuranMeta.TNAMES[n - 1]} ${QuranMeta.ENAMES[n - 1]} ${En.surahName(n)} ${Ar.surahName(n)} ${s.quranTitle}"

/** إزاحة اليوم (صفر أو أقل) الذي يكون فيه الحديث [id] حديثَ اليوم: تفتح عليه صفحة الحديث، فهي لا تعرض أياماً قادمة */
internal fun hadithOffset(id: String, today: LocalDate): Int {
    val ids = Hadiths.ids()
    val target = ids.indexOf(id)
    if (ids.isEmpty() || target < 0) return 0
    return -((Hadiths.indexFor(today) - target).mod(ids.size))
}

/** كلمات البحث الإضافية في سطر واحد، بلا ما نقص منها */
private fun words(vararg parts: String?): String = parts.filterNot { it.isNullOrBlank() }.joinToString(" ")

/**
 * روابط تحمل معاملاً لصفحتها (من بحث النظام والاختصارات): «events?event=…» مناسبة بعينها، و«hadith?id=…» حديث بعينه،
 * و«adhkar?section=…» قسم من الأذكار. وما بعد # يميّز عناصر تفتح الصفحة نفسها (settings#theme) ولا يصل إليها.
 */
internal object SearchLinks {
    fun event(key: String) = "events?event=$key"
    fun hadith(id: String) = "hadith?id=$id"
    fun adhkar(section: String) = "adhkar?section=$section"

    /** يضع معامل الرابط حيث تقرؤه صفحته، ويعيد مسار الصفحة؛ وما سوى ذلك (ومنه صفحة السورة بمعاملاتها) يعود كما هو */
    fun apply(link: String): String {
        val route = link.substringBefore('#')
        val base = route.substringBefore('?')
        val arg = route.substringAfter('?', "").substringAfter('=', "")
        return when (base) {
            "events" -> { if (arg.isNotEmpty()) Pending.event = arg; base }
            "hadith" -> { if (arg.isNotEmpty()) Pending.hadith = arg; base }
            "adhkar" -> { if (arg.isNotEmpty()) com.osatks.mizan.widget.WidgetLaunch.adhkarSection = arg; base }
            else -> route
        }
    }
}
