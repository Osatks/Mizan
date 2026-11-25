package com.osatks.mizan.quran

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.osatks.mizan.core.Kids
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * «ردّد معي» في ركن الأطفال: تُتلى الآية بصوت القارئ (ملفات التلاوة آية آية نفسها التي في صفحة السورة، وتُحفظ بعد
 * سماعها أول مرة)، ثم وقفة بطول الآية تقريباً يرددها فيها الطفل، وتتكرر الآية [Kids.repeat] مرة ثم تأتي التالية.
 * البسملة قبل السورة (عدا الفاتحة) تُتلى مرة ويرددها الطفل مرة. مشغّل خاص بها (لا يمس تلاوة صفحة السورة)،
 * ويتوقف بالخروج من الصفحة ([release]) وبمغادرة التطبيق ([pause]).
 */
object KidsRecite {
    enum class Phase { IDLE, LISTEN, REPEAT, DONE }

    /** السورة المفتوحة (0 لا سورة) */
    var surah by mutableIntStateOf(0)
        private set

    /** خطوات السورة: البسملة إن كانت، ثم آياتها */
    var steps by mutableStateOf<List<Clip>>(emptyList())
        private set

    /** الخطوة الحالية في [steps] (-1 لا شيء) */
    var step by mutableIntStateOf(-1)
        private set

    /** المرة الحالية من تكرار الآية (من 1 إلى [times]) */
    var rep by mutableIntStateOf(1)
        private set

    /** عدد مرات كل آية في هذا التشغيل */
    var times by mutableIntStateOf(1)
        private set

    var phase by mutableStateOf(Phase.IDLE)
        private set

    /** متوقف مؤقتاً (يُستأنف من حيث وقف) */
    var paused by mutableStateOf(false)
        private set

    var buffering by mutableStateOf(false)
        private set

    /** يزيد عند كل خطأ في التشغيل (انقطاع الإنترنت مثلاً) لتُظهر الواجهة رسالة */
    var errors by mutableIntStateOf(0)
        private set

    /** يتلو الآن أو ينتظر ترديد الطفل (ولو متوقفاً مؤقتاً) */
    val active: Boolean get() = phase == Phase.LISTEN || phase == Phase.REPEAT

    /** يسمع الطفلُ الصوتَ الآن أو ينتظره (لزر التشغيل والإيقاف المؤقت) */
    val running: Boolean get() = active && !paused

    private var player: ClipPlayer? = null
    private val scope = MainScope()
    private var gapJob: Job? = null
    private var clipMs = 0L
    private var reciter = Kids.DEFAULT_RECITER

    private fun player(): ClipPlayer = player ?: createClipPlayer().also { it.events = events; player = it }

    private val events = object : ClipEvents {
        override fun onClip(id: String, auto: Boolean) {}
        override fun onPlaying(playing: Boolean) {}
        override fun onWantsPlay(wants: Boolean) {}
        override fun onBuffering(buffering: Boolean) { this@KidsRecite.buffering = buffering }
        override fun onDuration(ms: Long) { if (ms > 0) clipMs = ms }
        override fun onEnded() = afterClip()
        override fun onError() {
            errors++
            stop()
        }
    }

    /** خطوات سورة: البسملة قبل كل سورة إلا الفاتحة (البسملة آيتها الأولى) والتوبة، ثم الآيات */
    fun stepsFor(surah: Int): List<Clip> = buildList {
        if (QuranData.hasBismillahHeader(surah)) add(Clip(QuranMeta.STARTS[surah - 1], basmala = true))
        for (a in 1..QuranMeta.COUNTS[surah - 1]) add(Clip(QuranMeta.index(surah, a)))
    }

    /** يفتح سورة (يوقف ما قبلها إن كانت غيرها) */
    fun open(surah: Int) {
        if (surah == this.surah) return
        stop()
        this.surah = surah
        steps = if (surah in 1..QuranMeta.SURAHS) stepsFor(surah) else emptyList()
    }

    /** يبدأ من الخطوة [from] بالقارئ والتكرار المختارين */
    fun start(from: Int = 0) {
        if (steps.isEmpty()) return
        // تلاوة صفحة السورة لا تُسمع مع هذه
        if (Recitation.active) Recitation.stop()
        Kids.ensure()
        reciter = Kids.reciter
        times = Kids.repeat
        cancelGap()
        step = from.coerceIn(0, steps.size - 1)
        rep = 1
        playStep()
    }

    /** تشغيل أو إيقاف مؤقت (أو البدء من أول السورة إن لم يبدأ) */
    fun toggle() {
        when {
            !active -> start(0)
            paused -> resume()
            else -> pause()
        }
    }

    fun pause() {
        if (!active || paused) return
        paused = true
        cancelGap()
        player?.pause()
    }

    private fun resume() {
        paused = false
        // وقف أثناء التلاوة: تكمل الآية؛ وأثناء ترديد الطفل: تُعاد الآية نفسها
        if (phase == Phase.LISTEN) player?.play() else playStep()
    }

    fun stop() {
        cancelGap()
        player?.stop()
        phase = Phase.IDLE
        step = -1
        rep = 1
        paused = false
        buffering = false
    }

    /** عند الخروج من الصفحة */
    fun release() {
        stop()
        player?.release()
        player = null
        surah = 0
        steps = emptyList()
    }

    private fun cancelGap() {
        gapJob?.cancel()
        gapJob = null
    }

    private fun playStep() {
        val c = steps.getOrNull(step) ?: return finish()
        phase = Phase.LISTEN
        paused = false
        clipMs = 0L
        val p = player()
        p.repeat = Repeat.OFF
        p.setSpeed(1f)
        p.setItems(listOf(c.id to recitationUrl(reciter, c)))
        p.play()
    }

    /** انتهت تلاوة الآية: دور الطفل، بطول الآية تقريباً */
    private fun afterClip() {
        if (phase != Phase.LISTEN || paused) return
        val c = steps.getOrNull(step) ?: return finish()
        phase = Phase.REPEAT
        val ms = gapFor(c)
        cancelGap()
        gapJob = scope.launch {
            delay(ms)
            gapJob = null
            advance()
        }
    }

    private fun gapFor(c: Clip): Long {
        // مدة الآية إن عُرفت، وإلا تقدير من طول نصها
        val ms = if (clipMs > 0) clipMs else {
            val text = QuranData.cached(QuranData.UTHMANI)?.getOrNull(if (c.basmala) 0 else c.index)
            if (text != null) text.length * 55L else 4000L
        }
        return (ms + 600L).coerceIn(2000L, 20_000L)
    }

    private fun advance() {
        val c = steps.getOrNull(step) ?: return finish()
        if (!c.basmala && rep < times) rep++ else {
            step++
            rep = 1
        }
        if (step >= steps.size) finish() else playStep()
    }

    /** خُتمت السورة: احتفال */
    private fun finish() {
        cancelGap()
        player?.stop()
        phase = Phase.DONE
        step = -1
        rep = 1
        paused = false
        buffering = false
        Kids.celebrate()
    }
}
