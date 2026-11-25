package com.osatks.mizan.ui.screens

import com.osatks.mizan.core.AdhanSounds
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Muezzin
import com.osatks.mizan.core.Muezzins
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.i18n.time
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.quran.QuranPrefs
import kotlinx.datetime.LocalDate

/**
 * خطوات الإعداد في أول تشغيل بترتيبها: الترحيب واللغة، والموقع مع معاينة المواقيت، والأذان، والمظهر، وتجربة صغيرة، ثم الملخص.
 * يرسمها Welcome في أندرويد و NativeWelcome.swift في iOS (عبر IosWelcome)؛ وكل اختيار يُحفظ فوراً في إعدادات التطبيق نفسها،
 * فلا يضيع شيء إن تخطى المستخدم الباقي.
 */
internal object WelcomeFlow {
    const val HELLO = 0
    const val PLACE = 1
    const val ADHAN = 2
    const val STYLE = 3
    const val TASTE = 4
    const val DONE = 5
    const val COUNT = 6

    /** مفاتيح الخطوات للواجهة الأصلية (بالترتيب نفسه) */
    val KEYS = listOf("hello", "place", "adhan", "style", "taste", "done")

    /** المظهر: مفاتيح AppSettings.theme */
    val THEMES = listOf("system", "light", "dark")

    /** ألوان صفحة المصحف بترتيب ورقة العرض (QuranPrefs.tone) */
    val TONES = listOf("", "sepia", "night", "depth", "depthNight")

    /** حبات تجربة المسبحة، والتسبيحة فوقها (نص عربي في كل اللغات) */
    const val BEADS = 3
    const val DHIKR = "سُبْحَانَ اللَّهِ"

    /** الصلوات الخمس في بطاقة المعاينة (بلا الشروق) */
    val FIVE = listOf("fajr", "dhuhr", "asr", "maghrib", "isha")

    /** أصوات الأذان الموجودة على الجهاز الآن (المضمّنة وما نُزِّل من قبل): لا تنزيل أثناء الترحيب، والبقية في صفحة الأذان */
    fun muezzins(): List<Muezzin> = Muezzins.ALL.filter { !it.fajr && AdhanSounds.available(it) }

    /** المؤذن المختار لكل الصلوات، كما في صفحة الأذان */
    fun pickMuezzin(m: Muezzin) {
        AppSettings.updateMuezzin(m.id)
        PrayerAlerts.reschedule()
    }
}

/** اسم المظهر بلغة الواجهة */
internal fun Strings.welcomeThemeName(key: String) = when (key) {
    "light" -> themeLight
    "dark" -> themeDark
    else -> themeAuto
}

/** اسم لون صفحة المصحف بلغة الواجهة */
internal fun Strings.welcomeToneName(tone: String) = when (tone) {
    "sepia" -> toneSepia
    "night" -> toneNight
    "depth" -> toneDepth
    "depthNight" -> toneDepthNight
    else -> toneAuto
}

/** صلاة في بطاقة المعاينة: [next] الصلاة القادمة اليوم */
internal class WelcomePrayer(val key: String, val name: String, val time: String, val next: Boolean)

/** مواقيت اليوم الخمسة في الموقع المختار، و[nextKey] القادمة إن كانت اليوم */
internal fun welcomePrayers(s: Strings, today: LocalDate, nextKey: String?): List<WelcomePrayer> {
    val t = PrayerUtil.times(today)
    return WelcomeFlow.FIVE.filter { !t[it].isNaN() }.map { k -> WelcomePrayer(k, s.prayer(k), s.time(t[k]), k == nextKey) }
}

/** سطر في ملخص الخطوة الأخيرة: مفتاح الأيقونة (language، place، adhan، style)، والعنوان، والقيمة، وهل ضُبط */
internal class WelcomeRow(val icon: String, val label: String, val value: String, val ok: Boolean)

/** ما ضُبط أثناء الترحيب: اللغة والمدينة والأذان والمظهر */
internal fun welcomeSummary(s: Strings, lang: Lang): List<WelcomeRow> {
    QuranPrefs.ensure()
    val place = AppSettings.placeSet
    val notify = AppSettings.notify
    return listOf(
        WelcomeRow("language", s.language, lang.native, true),
        WelcomeRow("place", s.obCity, if (place) s.placeName(AppSettings.place) else s.obNotSet, place),
        WelcomeRow("adhan", s.adhanSettings, if (notify) "${s.obOn} · ${s.muezzinName(Muezzins.forPrayer("dhuhr"))}" else s.obOff, notify),
        WelcomeRow("style", s.obStyleTitle, "${s.welcomeThemeName(AppSettings.theme)} · ${s.welcomeToneName(QuranPrefs.tone)}", true),
    )
}
