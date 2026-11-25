package com.osatks.mizan.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.osatks.mizan.quran.Reciters

/**
 * ركن الأطفال: السور القصيرة، وأسماء الله الحسنى من آيات وردت فيها، وأدعية اليوم من الأذكار، ولوحة النجوم.
 * المحتوى كله مما في التطبيق (نص المصحف وترجمات معانيه، والأذكار وترجماتها، والتلاوة آية آية)؛ وهنا الاختيار منه،
 * وما يُحفظ على الجهاز: السور المحفوظة (نجومها)، وتكرار كل آية، والقارئ. المفاتيح كلها تبدأ بـ kids_ فتشملها
 * النسخة الاحتياطية.
 */
object Kids {
    /** الفاتحة، ثم قصار جزء عمّ من الناس إلى الضحى (بترتيب حفظ الأطفال) */
    val SURAHS: List<Int> = listOf(1) + (114 downTo 93)

    /**
     * اسم من أسماء الله الحسنى: الاسم بالعربية، وكتابته باللاتينية (تُعرض مع لغات الواجهة غير العربية)،
     * وآية ورد فيها: يُعرض نصها وترجمة معانيها بلغة الواجهة (أو التفسير الميسر بالعربية)، فلا نصوص جديدة تُترجم
     */
    class Name(val ar: String, val latin: String, val surah: Int, val ayah: Int)

    /** الأسماء مع آيات قصيرة وردت فيها (مراجعها مطابقة لنص المصحف في assets/quran) */
    val NAMES: List<Name> = listOf(
        Name("اللَّه", "Allah", 112, 1),
        Name("الرَّحْمَٰن", "Ar-Rahman", 1, 3),
        Name("الرَّحِيم", "Ar-Rahim", 1, 3),
        Name("الأَحَد", "Al-Ahad", 112, 1),
        Name("الصَّمَد", "As-Samad", 112, 2),
        Name("المَلِك", "Al-Malik", 62, 1),
        Name("القُدُّوس", "Al-Quddus", 62, 1),
        Name("السَّلَام", "As-Salam", 59, 23),
        Name("المُؤْمِن", "Al-Mu’min", 59, 23),
        Name("المُهَيْمِن", "Al-Muhaymin", 59, 23),
        Name("العَزِيز", "Al-‘Aziz", 62, 1),
        Name("الجَبَّار", "Al-Jabbar", 59, 23),
        Name("المُتَكَبِّر", "Al-Mutakabbir", 59, 23),
        Name("الخَالِق", "Al-Khaliq", 59, 24),
        Name("البَارِئ", "Al-Bari’", 59, 24),
        Name("المُصَوِّر", "Al-Musawwir", 59, 24),
        Name("الحَكِيم", "Al-Hakim", 62, 1),
        Name("الغَفُور", "Al-Ghafur", 85, 14),
        Name("الوَدُود", "Al-Wadud", 85, 14),
        Name("الوَهَّاب", "Al-Wahhab", 38, 9),
        Name("الرَّزَّاق", "Ar-Razzaq", 51, 58),
        Name("المَتِين", "Al-Matin", 51, 58),
        Name("السَّمِيع", "As-Sami‘", 44, 6),
        Name("العَلِيم", "Al-‘Alim", 44, 6),
        Name("البَصِير", "Al-Basir", 40, 20),
        Name("اللَّطِيف", "Al-Latif", 67, 14),
        Name("الخَبِير", "Al-Khabir", 67, 14),
        Name("العَظِيم", "Al-‘Azim", 56, 74),
        Name("الأَعْلَى", "Al-A‘la", 87, 1),
        Name("الكَرِيم", "Al-Karim", 82, 6),
        Name("الحَيّ", "Al-Hayy", 3, 2),
        Name("القَيُّوم", "Al-Qayyum", 3, 2),
        Name("الأَوَّل", "Al-Awwal", 57, 3),
        Name("الآخِر", "Al-Akhir", 57, 3),
        Name("الظَّاهِر", "Az-Zahir", 57, 3),
        Name("البَاطِن", "Al-Batin", 57, 3),
        Name("الغَنِيّ", "Al-Ghani", 35, 15),
        Name("الحَمِيد", "Al-Hamid", 35, 15),
        Name("القَوِيّ", "Al-Qawiyy", 42, 19),
        Name("الوَاحِد", "Al-Wahid", 38, 65),
        Name("القَهَّار", "Al-Qahhar", 38, 65),
        Name("الفَتَّاح", "Al-Fattah", 34, 26),
        Name("التَّوَّاب", "At-Tawwab", 2, 37),
        Name("الوَلِيّ", "Al-Waliyy", 42, 28),
        Name("الخَلَّاق", "Al-Khallaq", 15, 86),
        Name("البَرّ", "Al-Barr", 52, 28),
        Name("العَلِيّ", "Al-‘Aliyy", 42, 4),
        Name("الكَبِير", "Al-Kabir", 13, 9),
        Name("المُتَعَال", "Al-Muta‘al", 13, 9),
    )

    /**
     * أدعية اليوم القصيرة بمعرّفاتها في الأذكار (assets/adhkar/adhkar.json): قبل الطعام وبعده، والنوم والاستيقاظ،
     * والخروج من المنزل ودخوله، والخلاء، واللباس، والعطاس، ودخول المسجد، وشكر من أحسن إليك
     */
    val DUAS: List<String> = listOf(
        "ftm01", "ftm05", "slp07", "wak01", "hom01", "hom03", "hom04", "hom05", "lbs01", "dly01", "dly02", "msj03", "dly13",
    )

    /** قارئ يناسب الأطفال: الحصري في المصحف المعلّم (تلاوة متأنية للتعليم) */
    const val DEFAULT_RECITER = "Husary_Muallim_128kbps"

    /** تكرار كل آية في «ردّد معي» */
    val REPEATS = listOf(1, 2, 3)

    private const val K_STARS = "kids_stars"
    private const val K_REPEAT = "kids_repeat"
    private const val K_RECITER = "kids_reciter"

    private var loaded = false

    /** السور التي حفظها الطفل (نجمة لكل سورة)، بترتيب [SURAHS] */
    var stars by mutableStateOf<List<Int>>(emptyList())
        private set
    var repeat by mutableIntStateOf(2)
        private set
    var reciter by mutableStateOf(DEFAULT_RECITER)
        private set

    /** يزيد مع كل احتفال: سورة حُفظت، أو خُتمت قراءتها مع القارئ في «ردّد معي» */
    var celebrations by mutableIntStateOf(0)
        private set

    fun ensure() {
        if (loaded) return
        loaded = true
        val saved = AppSettings.getString(K_STARS).split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
        stars = SURAHS.filter { it in saved }
        repeat = AppSettings.getInt(K_REPEAT, 2).coerceIn(REPEATS.first(), REPEATS.last())
        val id = AppSettings.getString(K_RECITER)
        reciter = Reciters.ALL.firstOrNull { it.id == id }?.id ?: DEFAULT_RECITER
    }

    /** عدد النجوم كلها */
    val total: Int get() = stars.size

    fun isMemorized(surah: Int) = surah in stars

    /** يضع نجمة السورة أو يرفعها؛ true إن صارت محفوظة الآن (فيُحتفل بها) */
    fun toggleStar(surah: Int): Boolean {
        if (surah !in SURAHS) return false
        val added = surah !in stars
        val set = stars.toSet().let { if (added) it + surah else it - surah }
        stars = SURAHS.filter { it in set }
        AppSettings.putString(K_STARS, stars.joinToString(","))
        if (added) celebrations++
        return added
    }

    fun updateRepeat(n: Int) {
        repeat = n.coerceIn(REPEATS.first(), REPEATS.last())
        AppSettings.putInt(K_REPEAT, repeat)
    }

    fun updateReciter(id: String) {
        reciter = Reciters.ALL.firstOrNull { it.id == id }?.id ?: DEFAULT_RECITER
        AppSettings.putString(K_RECITER, reciter)
    }

    /** احتفال بلا نجمة جديدة (ختم السورة مع القارئ) */
    fun celebrate() {
        celebrations++
    }
}
