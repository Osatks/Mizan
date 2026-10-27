package com.osatks.mizan.core

import androidx.compose.runtime.mutableStateMapOf
import com.osatks.mizan.platform.AdhanAudio
import com.osatks.mizan.platform.Fs
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.platform.httpGet
import com.osatks.mizan.platform.path
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** الصلوات الخمس التي يُنبَّه لها */
val ALERT_PRAYERS = listOf("fajr", "dhuhr", "asr", "maghrib", "isha")

/** طريقة التنبيه لكل صلاة */
enum class AlertMode(val key: String) {
    /** الأذان كاملاً (في iOS: أول 30 ثانية في الإشعار، وكاملاً إن كان التطبيق مفتوحاً) */
    ADHAN("adhan"),
    /** اهتزاز بلا صوت */
    VIBRATE("vibrate"),
    /** إشعار صامت */
    SILENT("silent"),
    /** لا تنبيه */
    OFF("off");

    companion object {
        fun of(k: String?) = entries.firstOrNull { it.key == k } ?: ADHAN
    }
}

/**
 * نمط اهتزاز: [timings] أزمنة متعاقبة بالملّي ثانية تبدأ بانتظار ثم اهتزاز ثم انتظار…،
 * و[amps] شدة كل مقطع 0..255 (0 للانتظار). الشدة تعمل في الأجهزة التي تدعمها، والأزمنة في كل جهاز.
 */
enum class VibePattern(val key: String, val timings: List<Long>, val amps: List<Int>) {
    /** اهتزازة واحدة قصيرة */
    SHORT("short", listOf(0, 320), listOf(0, 255)),
    /** نقرتان */
    DOUBLE("double", listOf(0, 110, 130, 110), listOf(0, 255, 0, 255)),
    /** ثلاث نقرات */
    TRIPLE("triple", listOf(0, 90, 110, 90, 110, 90), listOf(0, 255, 0, 255, 0, 255)),
    LONG("long", listOf(0, 1400), listOf(0, 230)),
    HEARTBEAT("heartbeat", listOf(0, 90, 120, 180, 650, 90, 120, 180, 650, 90, 120, 180), listOf(0, 160, 0, 255, 0, 160, 0, 255, 0, 160, 0, 255)),
    PULSE("pulse", listOf(0, 450, 300, 450, 300, 450), listOf(0, 255, 0, 255, 0, 255)),
    /** يعلو ثم يهدأ (الانتظارات صفرية فتتصل المقاطع) */
    WAVE("wave", listOf(0, 220, 0, 220, 0, 220, 0, 220, 0, 220, 0, 220), listOf(0, 60, 0, 130, 0, 200, 0, 255, 0, 170, 0, 90)),
    /** يتصاعد من الخفيف إلى القوي */
    RISE("rise", listOf(0, 260, 0, 260, 0, 260, 0, 260, 0, 260), listOf(0, 40, 0, 90, 0, 145, 0, 200, 0, 255)),
    /** هادئ: نبضتان لطيفتان */
    GENTLE("gentle", listOf(0, 350, 300, 350), listOf(0, 70, 0, 70)),
    /** قوي: اهتزازتان طويلتان بأقصى شدة */
    STRONG("strong", listOf(0, 700, 180, 700), listOf(0, 255, 0, 255)),
    /** طَرق الباب: طرقتان ثم طرقتان */
    KNOCK("knock", listOf(0, 55, 100, 55, 420, 55, 100, 55), listOf(0, 255, 0, 255, 0, 255, 0, 255)),
    /** رفرفة سريعة متتابعة */
    RAPID("rapid", listOf(0, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45), List(20) { if (it % 2 == 0) 0 else 220 });

    /** مجموع مدته بالملّي ثانية */
    val millis get() = timings.sum()

    /** المقاطع المهتزة: (البداية، المدة، الشدة) بالملّي ثانية — لرسم النمط وتشغيله في iOS */
    val pulses: List<Triple<Long, Long, Int>>
        get() {
            var t = 0L
            return timings.mapIndexedNotNull { i, ms ->
                val start = t
                t += ms
                amps[i].takeIf { it > 0 && ms > 0 }?.let { Triple(start, ms, it) }
            }
        }

    companion object {
        val DEFAULT = HEARTBEAT
        fun of(k: String?) = entries.firstOrNull { it.key == k } ?: DEFAULT
    }
}

/**
 * تسجيل أذان. الملف الكامل adhan/<id>.m4a (مضمّن في assets أو منزَّل من موقع ميزان)،
 * وصوت إشعار iOS (أقل من 30 ثانية) adhan_<id>.caf في جذر حزمة التطبيق أو Library/Sounds.
 * [credit] المؤذن أو المسجّل والرخصة والمصدر كما في صفحة التسجيل (أسماء علم لا تُترجم).
 */
data class Muezzin(
    val id: String,
    val fajr: Boolean,
    val bundled: Boolean,
    val seconds: Int,
    val sizeKb: Int,
    val credit: String,
) {
    val file get() = "$id.m4a"
    val iosSound get() = "adhan_$id.caf"
}

/** التسجيلات المتاحة: اثنان مضمّنان (أذان وأذان فجر)، والبقية تُنزَّل عند اختيارها */
object Muezzins {
    const val BASE = "https://mizan-al-muslem.com/adhan/"
    /** اختيار أذان الفجر: مثل بقية الصلوات */
    const val SAME = "same"

    val ALL: List<Muezzin> = AdhanCatalog.ALL
    val DEFAULT: Muezzin get() = ALL.first { it.bundled && !it.fajr }
    val FAJR_DEFAULT: Muezzin get() = ALL.first { it.bundled && it.fajr }

    fun of(id: String?): Muezzin? = ALL.firstOrNull { it.id == id }

    /** الأذان المختار لصلاة، أو المضمّن إن لم يكن المختار على الجهاز (حُذف أو لم يكتمل تنزيله) */
    fun forPrayer(prayer: String): Muezzin {
        val fajrPick = if (prayer == "fajr") of(AppSettings.fajrMuezzin) else null
        val pick = fajrPick ?: of(AppSettings.muezzin)?.takeIf { !it.fajr } ?: DEFAULT
        return if (AdhanSounds.available(pick)) pick else if (pick.fajr) FAJR_DEFAULT else DEFAULT
    }
}

/** تنزيل التسجيلات غير المضمّنة من موقع ميزان (files/adhan)، كحزم اللغات */
object AdhanSounds {
    /** التنزيلات الجارية: النسبة 0..100، أو -1 إن فشل التنزيل */
    val progress = mutableStateMapOf<String, Int>()
    /** يتغير بعد كل تنزيل أو حذف لتُعاد قراءة [available] في الواجهة */
    private val version = mutableStateMapOf<String, Int>()

    private val dir get() = path(Platform.filesDir, "adhan")

    /** مسار الملف المنزَّل (null للمضمّن في التطبيق) */
    fun localFile(m: Muezzin): String? = if (m.bundled) null else path(dir, m.file)

    fun available(m: Muezzin): Boolean {
        version[m.id]
        return m.bundled || Fs.exists(path(dir, m.file))
    }

    suspend fun download(m: Muezzin, base: String = Muezzins.BASE): Boolean {
        if (m.bundled || progress[m.id]?.let { it >= 0 } == true) return false
        progress[m.id] = 0
        return try {
            withContext(Dispatchers.IO) {
                val sound = if (AdhanAudio.needsNotificationSound) httpGet(base + m.iosSound) { progress[m.id] = it / 10 } else null
                val audio = httpGet(base + m.file) { progress[m.id] = 10 + it * 9 / 10 }
                require(audio.size > 10_000) { "short file" }
                sound?.let { AdhanAudio.saveNotificationSound(m.iosSound, it) }
                Fs.write(path(dir, m.file), audio)
            }
            progress.remove(m.id)
            version[m.id] = (version[m.id] ?: 0) + 1
            true
        } catch (e: CancellationException) {
            progress.remove(m.id); throw e
        } catch (e: Throwable) {
            progress[m.id] = -1
            false
        }
    }

    private val scope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    /** يبدأ التنزيل في الخلفية؛ onDone بالنتيجة */
    fun start(m: Muezzin, onDone: (Boolean) -> Unit = {}) {
        scope.launch { onDone(download(m)) }
    }

    fun delete(m: Muezzin) {
        if (m.bundled) return
        Fs.delete(path(dir, m.file))
        AdhanAudio.deleteNotificationSound(m.iosSound)
        version[m.id] = (version[m.id] ?: 0) + 1
        // كان المحذوف هو المختار: يعود الاختيار إلى المضمّن
        if (AppSettings.muezzin == m.id) AppSettings.updateMuezzin(Muezzins.DEFAULT.id)
        if (AppSettings.fajrMuezzin == m.id) AppSettings.updateFajrMuezzin(Muezzins.FAJR_DEFAULT.id)
        PrayerAlerts.reschedule()
    }
}
