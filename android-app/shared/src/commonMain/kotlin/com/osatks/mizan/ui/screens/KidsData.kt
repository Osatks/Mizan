package com.osatks.mizan.ui.screens

import com.osatks.mizan.adhkar.Adhkar
import com.osatks.mizan.core.Kids
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.ui.quran.surahName

/** دعاء في ركن الأطفال: مناسبته (عنوان بابه في الأذكار)، ونصه، وترجمته بلغة الواجهة (لا ترجمة للعربية) */
internal class KidsDua(val id: String, val title: String, val ar: String, val tr: String?)

/** أدعية اليوم من الأذكار بترتيب [Kids.DUAS]، وعناوينها وترجماتها كما في صفحة الأذكار (يقرأ الملف: في الخلفية) */
internal fun kidsDuas(lang: Lang): List<KidsDua> {
    val sections = Adhkar.sections()
    val tr = Adhkar.tr(lang)
    return Kids.DUAS.mapNotNull { id ->
        for (sec in sections) for (g in sec.groups) {
            val d = g.items.firstOrNull { it.id == id } ?: continue
            // الباب بلا عنوان (أذكار الاستيقاظ): عنوان القسم
            val title = if (lang == Lang.AR) g.titleAr ?: sec.titleAr
            else tr.groups[g.key] ?: tr.sections[sec.id]?.first ?: g.titleAr ?: sec.titleAr
            return@mapNotNull KidsDua(id, title, d.ar, translation(d, tr, lang, null))
        }
        null
    }
}

/** مصدر معنى آيات الأسماء: الميسر بالعربية، وترجمة المعاني بلغة الواجهة لغيرها */
internal fun kidsMeaningSource(lang: Lang): String? = if (lang == Lang.AR) "ar.muyassar" else Sources.translationFor(lang)?.id

/** عنوان المعنى تحت آية الاسم */
internal fun kidsMeaningLabel(lang: Lang, s: Strings): String = if (lang == Lang.AR) s.tafsirMuyassar else s.translationOfMeanings

/** نص آية الاسم (بلا البسملة في أول السورة) */
internal fun kidsNameAyah(n: Kids.Name, quran: Array<String>?): String =
    quran?.let { QuranData.withoutBismillah(it, QuranMeta.index(n.surah, n.ayah)) } ?: ""

/** مرجع آية الاسم: «الإخلاص: 1» */
internal fun kidsNameRef(n: Kids.Name, s: Strings): String = s.surahAyah(s.surahName(n.surah), n.ayah)
