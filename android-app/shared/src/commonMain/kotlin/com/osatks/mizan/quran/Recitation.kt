package com.osatks.mizan.quran

import com.osatks.mizan.i18n.Lang
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/** قارئ: id اسم مجلده في everyayah.com، واسمه بالعربية وبالحروف اللاتينية */
class Reciter(val id: String, val ar: String, val en: String) {
    fun name(lang: Lang) = if (lang == Lang.AR || lang == Lang.UR || lang == Lang.FA) ar else en
}

object Reciters {
    val ALL = listOf(
        Reciter("Alafasy_128kbps", "مشاري راشد العفاسي", "Mishary Rashid Alafasy"),
        Reciter("Husary_128kbps", "محمود خليل الحصري", "Mahmoud Khalil Al-Husary"),
        Reciter("Husary_Muallim_128kbps", "الحصري (المصحف المعلّم)", "Al-Husary (Mu’allim, for learning)"),
        Reciter("Minshawy_Murattal_128kbps", "محمد صديق المنشاوي", "Muhammad Siddiq Al-Minshawi"),
        Reciter("Abdul_Basit_Murattal_64kbps", "عبد الباسط عبد الصمد", "Abdul Basit Abdul Samad"),
        Reciter("MaherAlMuaiqly128kbps", "ماهر المعيقلي", "Maher Al-Muaiqly"),
        Reciter("Abdurrahmaan_As-Sudais_64kbps", "عبد الرحمن السديس", "Abdul Rahman Al-Sudais"),
        Reciter("Saood_ash-Shuraym_128kbps", "سعود الشريم", "Saud Al-Shuraim"),
        Reciter("Ghamadi_40kbps", "سعد الغامدي", "Saad Al-Ghamdi"),
        Reciter("Yasser_Ad-Dussary_128kbps", "ياسر الدوسري", "Yasser Al-Dosari"),
        Reciter("Abu_Bakr_Ash-Shaatree_128kbps", "أبو بكر الشاطري", "Abu Bakr Al-Shatri"),
    )

    fun byId(id: String) = ALL.firstOrNull { it.id == id } ?: ALL[0]
}

/** مقطع في قائمة التلاوة: آية، أو البسملة قبل أول آية في السورة */
data class Clip(val index: Int, val basmala: Boolean = false) {
    val id get() = if (basmala) "b$index" else "$index"

    companion object {
        fun parse(id: String) = if (id.startsWith("b")) Clip(id.drop(1).toInt(), true) else Clip(id.toInt())
    }
}

/**
 * ما يُتلى من الآية start إلى end (أو إلى آخر سورتها إن كانت end سالبة)، كل آية times مرة
 * (0 = بلا نهاية: مرة واحدة يكررها المشغّل)، وتسبق أولَ آية في كل سورة البسملةُ مرة واحدة،
 * إلا في الفاتحة (البسملة آيتها الأولى) والتوبة.
 */
fun recitationQueue(start: Int, times: Int, end: Int = -1): List<Clip> {
    val surah = QuranMeta.locate(start).first
    val last = if (end >= start) end.coerceAtMost(QuranMeta.AYAHS - 1) else QuranMeta.STARTS[surah - 1] + QuranMeta.COUNTS[surah - 1] - 1
    val n = if (times <= 0) 1 else times
    return buildList {
        for (i in start..last) {
            val s = QuranMeta.locate(i).first
            if (i == QuranMeta.STARTS[s - 1] && s != 1 && s != 9) add(Clip(i, basmala = true))
            repeat(n) { add(Clip(i)) }
        }
    }
}

/** المقطع كاملاً مكرراً rangeTimes مرة (للتكرار بلا نهاية يكرره المشغّل) */
fun rangeQueue(start: Int, end: Int, times: Int, rangeTimes: Int): List<Clip> {
    val one = recitationQueue(start, times, end)
    return if (end < 0 || rangeTimes <= 1) one else List(rangeTimes) { one }.flatten()
}

/** رابط ملف الآية عند القارئ: السورة والآية بثلاثة أرقام (البسملة = أول الفاتحة) */
fun recitationUrl(reciter: String, clip: Clip): String {
    val (s, a) = if (clip.basmala) 1 to 1 else QuranMeta.locate(clip.index)
    return "https://everyayah.com/data/$reciter/${s.toString().padStart(3, '0')}${a.toString().padStart(3, '0')}.mp3"
}

/**
 * تلاوة القرآن آية آية مع القراءة: الآية المتلوة تُظلَّل في الصفحة وتُقلَّب الصفحات معها.
 * الملفات تُحفظ بعد سماعها أول مرة، فما سُمع يعمل دون اتصال. تتوقف التلاوة مؤقتاً عند مكالمة أو فصل السماعة،
 * وعند مغادرة التطبيق، وتنتهي بالخروج من صفحة القراءة.
 */
object Recitation {
    /** هناك تلاوة (تعمل أو متوقفة مؤقتاً) */
    var active by mutableStateOf(false)
        private set
    var playing by mutableStateOf(false)
        private set
    var buffering by mutableStateOf(false)
        private set
    /** المستخدم يريد التشغيل (قد ينتظر التحميل)؛ لزر التشغيل والإيقاف */
    var wantsPlay by mutableStateOf(false)
        private set
    /** الآية التي تُتلى (أو التي تسبقها البسملة الآن) */
    var current by mutableIntStateOf(-1)
        private set
    var basmala by mutableStateOf(false)
        private set
    /** يزيد عند كل خطأ في التشغيل (انقطاع الإنترنت مثلاً) لتُظهر الواجهة رسالة */
    var errors by mutableIntStateOf(0)
        private set

    /** متى يتوقف مؤقت الإيقاف (0 بلا مؤقت) */
    var sleepAt by mutableLongStateOf(0L)
        private set

    /** يُستدعى بعد كل تغيّر في حال التلاوة (iOS: «يُشغَّل الآن» في شاشة القفل ومركز التحكم والجزيرة الديناميكية) */
    var onChange: (() -> Unit)? = null
    private fun changed() { onChange?.invoke() }

    private var player: ClipPlayer? = null
    private val scope = MainScope()
    /** في وقفة بعد آية ليرددها القارئ: المشغّل متوقف لكن التلاوة مستمرة */
    private var gapping = false
    private var gapJob: Job? = null
    private var sleepJob: Job? = null
    private var clipMs = 0L
    private var lastClip: Clip? = null
    private var rangeEnd = -1
    private var rangeTimes = 1
    private var reciter = ""
    private var times = 1

    private fun cancelGap() { gapJob?.cancel(); gapJob = null; gapping = false }

    private fun player(): ClipPlayer = player ?: createClipPlayer().also { it.events = events; player = it }

    private val events = object : ClipEvents {
        override fun onClip(id: String, auto: Boolean) {
            val p = player
            // انتهت آية: وقفة ليرددها القارئ بعد الشيخ (ثابتة أو بطول الآية نفسها)
            if (p != null && auto && lastClip?.basmala == false) {
                val ms = when (val g = QuranPrefs.gap) {
                    0 -> 0L
                    -1 -> (clipMs / QuranPrefs.speed).toLong().coerceIn(1500L, 20_000L)
                    else -> g * 1000L
                }
                if (ms > 0) {
                    gapping = true; p.pause()
                    gapJob = scope.launch { delay(ms); gapping = false; player?.play() }
                }
            }
            onClip(Clip.parse(id))
            changed()
        }

        override fun onPlaying(playing: Boolean) { this@Recitation.playing = playing; changed() }
        override fun onWantsPlay(wants: Boolean) { wantsPlay = wants || gapping; changed() }
        override fun onBuffering(buffering: Boolean) { this@Recitation.buffering = buffering }
        override fun onDuration(ms: Long) { if (ms > 0) clipMs = ms }
        override fun onEnded() = stop()
        override fun onError() { errors++; stop() }
    }

    private fun onClip(c: Clip) {
        current = c.index
        basmala = c.basmala
        lastClip = c
        val p = player ?: return
        // «بلا نهاية»: تتكرر الآية الحالية حتى ينتقل المستخدم (والبسملة لا تتكرر)، أو يتكرر المقطع كله
        p.repeat = when {
            times <= 0 && !c.basmala -> Repeat.ONE
            rangeEnd >= 0 && rangeTimes <= 0 -> Repeat.ALL
            else -> Repeat.OFF
        }
        // التلاوة المتواصلة: عند آخر مقطع تُضاف السورة التالية فلا تتوقف
        if (rangeEnd < 0 && p.index >= p.count - 1) {
            val surah = QuranMeta.locate(c.index).first
            if (surah < QuranMeta.SURAHS) p.addItems(items(recitationQueue(QuranMeta.STARTS[surah], times)))
        }
    }

    private fun items(clips: List<Clip>) = clips.map { it.id to recitationUrl(reciter, it) }

    /** يبدأ التلاوة من الآية index إلى end (سالبة = متواصلة) بالقارئ والإعدادات المختارة */
    fun play(index: Int, end: Int = -1, start: Boolean = true) {
        reciter = Reciters.byId(QuranPrefs.reciter).id
        times = QuranPrefs.repeat
        rangeEnd = end
        rangeTimes = QuranPrefs.rangeRepeat
        cancelGap()
        val from = index.coerceIn(0, QuranMeta.AYAHS - 1)
        val p = player()
        p.setSpeed(QuranPrefs.speed)
        p.setItems(items(rangeQueue(from, if (end >= 0) end.coerceAtLeast(from) else -1, times, rangeTimes)))
        if (start) p.play() else p.pause()
        if (!active) applySleep()
        wantsPlay = start
        active = true
        current = index
        changed()
    }

    fun toggle() {
        val p = player ?: return
        if (gapping) { cancelGap(); wantsPlay = false; changed(); return }
        if (p.wantsPlay) p.pause() else p.play()
    }

    fun applySpeed() { player?.setSpeed(QuranPrefs.speed) }

    /** يبدأ مؤقت الإيقاف من الآن (أو يلغيه) */
    fun applySleep() {
        sleepJob?.cancel(); sleepJob = null
        val min = QuranPrefs.sleep
        sleepAt = if (min > 0) Clock.System.now().toEpochMilliseconds() + min * 60_000L else 0L
        if (min > 0) sleepJob = scope.launch { delay(min * 60_000L); stop() }
    }

    fun pause() {
        if (gapping) { cancelGap(); wantsPlay = false; changed() }
        player?.pause()
    }

    /** الآية التالية أو السابقة (يتجاوز التكرار) */
    fun skip(delta: Int) {
        if (!active || current < 0) return
        val to = (current + delta).coerceIn(0, QuranMeta.AYAHS - 1).let { if (rangeEnd >= 0) it.coerceAtMost(rangeEnd) else it }
        play(to, rangeEnd, wantsPlay)
    }

    /** تغيّر القارئ أو التكرار: تُستأنف التلاوة من الآية الحالية بالإعداد الجديد */
    fun restart() {
        if (active && current >= 0) play(current, rangeEnd, wantsPlay)
    }

    fun stop() {
        cancelGap(); sleepJob?.cancel(); sleepJob = null
        sleepAt = 0L; lastClip = null
        player?.stop()
        active = false; playing = false; buffering = false; wantsPlay = false; current = -1; basmala = false
        changed()
    }

    /** لصور الاختبار: تلاوة ظاهرة دون مشغّل */
    internal fun preview(index: Int, play: Boolean = true) {
        active = true; wantsPlay = play; playing = play; current = index; basmala = false
    }

    /** عند الخروج من صفحة القراءة */
    fun release() {
        stop()
        player?.release()
        player = null
    }
}
