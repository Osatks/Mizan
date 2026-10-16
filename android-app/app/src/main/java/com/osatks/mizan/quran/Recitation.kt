package com.osatks.mizan.quran

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.osatks.mizan.i18n.Lang
import java.io.File

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
 * ما يُتلى عند البدء من الآية start: الآيات حتى آخر سورتها، كل آية times مرة (0 = بلا نهاية: مرة واحدة يكررها المشغّل)،
 * وتسبق أولَ آية في السورة البسملةُ مرة واحدة، إلا في الفاتحة (البسملة آيتها الأولى) والتوبة.
 */
fun recitationQueue(start: Int, times: Int): List<Clip> {
    val (surah, _) = QuranMeta.locate(start)
    val first = QuranMeta.STARTS[surah - 1]
    val end = first + QuranMeta.COUNTS[surah - 1] - 1
    val n = if (times <= 0) 1 else times
    return buildList {
        for (i in start..end) {
            if (i == first && surah != 1 && surah != 9) add(Clip(i, basmala = true))
            repeat(n) { add(Clip(i)) }
        }
    }
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
@OptIn(UnstableApi::class)
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

    private var player: ExoPlayer? = null
    private var cache: SimpleCache? = null
    private var reciter = ""
    private var times = 1

    private fun cache(ctx: Context) = cache ?: SimpleCache(
        File(ctx.cacheDir, "recitation"), LeastRecentlyUsedCacheEvictor(300L * 1024 * 1024), StandaloneDatabaseProvider(ctx)
    ).also { cache = it }

    private fun player(ctx: Context): ExoPlayer = player ?: run {
        val app = ctx.applicationContext
        val http = DefaultHttpDataSource.Factory().setUserAgent("Mizan").setConnectTimeoutMs(15_000).setReadTimeoutMs(20_000)
        val source = CacheDataSource.Factory()
            .setCache(cache(app))
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(app, http))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val attrs = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build()
        ExoPlayer.Builder(app)
            .setMediaSourceFactory(DefaultMediaSourceFactory(source))
            .setAudioAttributes(attrs, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { it.addListener(listener); player = it }
    }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            item?.mediaId?.let { onClip(Clip.parse(it)) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { wantsPlay = playWhenReady }

        override fun onPlaybackStateChanged(state: Int) {
            buffering = state == Player.STATE_BUFFERING
            if (state == Player.STATE_ENDED) stop()
        }

        override fun onPlayerError(error: PlaybackException) { errors++; stop() }
    }

    private fun onClip(c: Clip) {
        current = c.index
        basmala = c.basmala
        val p = player ?: return
        // «بلا نهاية»: تتكرر الآية الحالية حتى ينتقل المستخدم، أما البسملة فلا تتكرر
        p.repeatMode = if (times <= 0 && !c.basmala) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        // عند آخر مقطع تُضاف السورة التالية، فتستمر التلاوة دون توقف
        if (p.currentMediaItemIndex >= p.mediaItemCount - 1) {
            val surah = QuranMeta.locate(c.index).first
            if (surah < QuranMeta.SURAHS) p.addMediaItems(items(recitationQueue(QuranMeta.STARTS[surah], times)))
        }
    }

    private fun items(clips: List<Clip>) = clips.map { MediaItem.Builder().setUri(recitationUrl(reciter, it)).setMediaId(it.id).build() }

    /** يبدأ التلاوة من الآية index بالقارئ والتكرار المختارين */
    fun play(ctx: Context, index: Int, start: Boolean = true) {
        reciter = Reciters.byId(QuranPrefs.reciter).id
        times = QuranPrefs.repeat
        val p = player(ctx)
        p.setMediaItems(items(recitationQueue(index.coerceIn(0, QuranMeta.AYAHS - 1), times)))
        p.prepare()
        p.playWhenReady = start
        wantsPlay = start
        active = true
        current = index
    }

    fun toggle() {
        val p = player ?: return
        if (p.playWhenReady) p.pause() else p.play()
    }

    fun pause() { player?.pause() }

    /** الآية التالية أو السابقة (يتجاوز التكرار) */
    fun skip(ctx: Context, delta: Int) {
        if (active && current >= 0) play(ctx, (current + delta).coerceIn(0, QuranMeta.AYAHS - 1), player?.playWhenReady ?: true)
    }

    /** تغيّر القارئ أو التكرار: تُستأنف التلاوة من الآية الحالية بالإعداد الجديد */
    fun restart(ctx: Context) {
        if (active && current >= 0) play(ctx, current, player?.playWhenReady ?: true)
    }

    fun stop() {
        player?.let { it.stop(); it.clearMediaItems() }
        active = false; playing = false; buffering = false; wantsPlay = false; current = -1; basmala = false
    }

    /** لصور الاختبار: تلاوة ظاهرة دون مشغّل */
    internal fun preview(index: Int, play: Boolean = true) {
        active = true; wantsPlay = play; playing = play; current = index; basmala = false
    }

    /** عند الخروج من صفحة القراءة */
    fun release() {
        stop()
        player?.let { it.removeListener(listener); it.release() }
        player = null
    }
}
