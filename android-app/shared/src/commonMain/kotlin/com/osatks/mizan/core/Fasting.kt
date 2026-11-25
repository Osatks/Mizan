package com.osatks.mizan.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** سبب صيام التطوع في يوم؛ [key] مفتاح اسمه في Strings.fastingKinds */
enum class SunnahKind(val key: String) {
    ARAFAH("arafah"), TASUA("tasua"), ASHURA("ashura"), SHAWWAL("shawwal"),
    WHITE("white"), MONDAY("monday"), THURSDAY("thursday"),
}

/** يوم يُصام تطوعاً: تاريخه الميلادي والهجري (بفرق المستخدم) وأسبابه */
data class SunnahDay(val date: LocalDate, val hijri: HDate, val kinds: List<SunnahKind>)

/** حال يوم من رمضان في المتابعة: لم يُحدَّد، أو صامه، أو أفطره */
enum class DayMark(val code: Char) {
    NONE('0'), FASTED('f'), MISSED('m');

    /** اللمس يتنقل: بلا تحديد ← صيام ← إفطار ← بلا تحديد */
    fun next(): DayMark = when (this) { NONE -> FASTED; FASTED -> MISSED; MISSED -> NONE }

    companion object {
        fun of(c: Char): DayMark = entries.firstOrNull { it.code == c } ?: NONE
    }
}

/** سطر في سجل القضاء: اليوم (yyyy-mm-dd)، والتغيّر (+1 أو -1)، ونوعه: add أو remove أو done أو missed */
data class QadaEntry(val date: String, val delta: Int, val kind: String)

/**
 * بيانات متابعة الصيام كلها: علامات رمضان لكل سنة هجرية، وأيام القضاء وسجلها، وأيام التطوع المسجلة، والتذكير.
 * قيمة ثابتة تُحسب منها الحال التالية، فتُختبر بلا تخزين ولا واجهة (كما في [TasbihState]).
 */
data class FastingState(
    /** لكل سنة هجرية نص بطول 30 من رموز [DayMark] (اليوم الأول أولاً) */
    val ramadan: Map<Int, String> = emptyMap(),
    /** أيام القضاء الباقية */
    val qada: Int = 0,
    /** سجل القضاء، الأحدث أولاً */
    val qadaLog: List<QadaEntry> = emptyList(),
    /** أيام التطوع المسجلة (yyyy-mm-dd)، تصاعدياً بلا تكرار */
    val sunnah: List<String> = emptyList(),
    /** تذكير مساء اليوم السابق لصيام التطوع (مطفأ افتراضياً) */
    val remind: Boolean = false,
) {
    fun marks(year: Int): String = (ramadan[year] ?: "").padEnd(30, '0').take(30)

    fun mark(year: Int, day: Int): DayMark = DayMark.of(marks(year)[(day - 1).coerceIn(0, 29)])

    /** عدد أيام رمضان [year] بالعلامة [mark] من أول [length] يوماً */
    fun count(year: Int, mark: DayMark, length: Int): Int = marks(year).take(length.coerceIn(0, 30)).count { it == mark.code }

    /** يغيّر علامة يوم من رمضان ([day] من 1): الإفطار يضيف يوماً إلى القضاء، ورفع الإفطار يحذفه */
    fun withMark(year: Int, day: Int, target: DayMark, today: String): FastingState {
        if (day !in 1..30) return this
        val old = this.mark(year, day)
        if (old == target) return this
        val chars = marks(year).toCharArray()
        chars[day - 1] = target.code
        val m = chars.concatToString()
        var s = copy(ramadan = if (m.all { it == DayMark.NONE.code }) ramadan - year else ramadan + (year to m))
        if (target == DayMark.MISSED) s = s.qadaChange(+1, "missed", today)
        else if (old == DayMark.MISSED) s = s.qadaChange(-1, "remove", today)
        return s
    }

    /** يزيد أيام القضاء أو ينقصها ([kind] نوع السطر في السجل)؛ لا تنزل عن صفر */
    fun qadaChange(delta: Int, kind: String, today: String): FastingState {
        val n = (qada + delta).coerceIn(0, Fasting.MAX_QADA)
        if (n == qada) return this
        return copy(qada = n, qadaLog = (listOf(QadaEntry(today, n - qada, kind)) + qadaLog).take(Fasting.LOG_SIZE))
    }

    /** يسجّل صيام تطوع في [date] أو يرفعه */
    fun withSunnah(date: String, on: Boolean): FastingState =
        copy(sunnah = (if (on) sunnah + date else sunnah - date).distinct().sorted().takeLast(Fasting.SUNNAH_SIZE))
}

/** حساب أيام الصيام من التقويم الهجري (Hijri.kt، أم القرى)؛ [offset] فرق المستخدم بالأيام */
object FastCalendar {
    /** التاريخ الهجري ليوم ميلادي بعد فرق المستخدم */
    fun hijri(date: LocalDate, offset: Int): HDate? = Hijri.toHijri(date.plusDays(offset))

    /** اليوم الميلادي لتاريخ هجري بعد فرق المستخدم */
    fun gregorian(y: Int, m: Int, d: Int, offset: Int): LocalDate? = Hijri.toGregorian(y, m, d)?.minusDays(offset)

    /** الأيام التي لا تُصام: عيد الفطر (1 شوال)، وعيد الأضحى وأيام التشريق (10 إلى 13 ذي الحجة) */
    fun forbidden(h: HDate): Boolean = (h.m == 10 && h.d == 1) || (h.m == 12 && h.d in 10..13)

    /**
     * أسباب صيام التطوع في يوم، الأخص أولاً. لا شيء في رمضان (صيامه فريضة) ولا في الأيام التي لا تُصام،
     * فيسقط الأبيض الثالث عشر من ذي الحجة والاثنين والخميس إن وافقا عيداً. [shawwalDone]: ما سُجّل من الست في شوال هذه السنة.
     */
    fun kinds(date: LocalDate, h: HDate, shawwalDone: Int): List<SunnahKind> {
        if (h.m == 9 || forbidden(h)) return emptyList()
        val out = ArrayList<SunnahKind>(3)
        if (h.m == 12 && h.d == 9) out += SunnahKind.ARAFAH
        if (h.m == 1 && h.d == 9) out += SunnahKind.TASUA
        if (h.m == 1 && h.d == 10) out += SunnahKind.ASHURA
        if (h.m == 10 && h.d >= 2 && shawwalDone < 6) out += SunnahKind.SHAWWAL
        if (h.d in 13..15) out += SunnahKind.WHITE
        when (date.dayOfWeek.iso) {
            1 -> out += SunnahKind.MONDAY
            4 -> out += SunnahKind.THURSDAY
        }
        return out
    }

    /** أيام التطوع من [from] (ضمناً)، أقصاها [count] يوماً في [horizon] يوماً */
    fun upcoming(from: LocalDate, offset: Int, count: Int, horizon: Int = 120, shawwalDone: (Int) -> Int = { 0 }): List<SunnahDay> {
        val out = ArrayList<SunnahDay>(count)
        for (i in 0 until horizon) {
            if (out.size >= count) break
            val d = from.plusDays(i)
            val h = hijri(d, offset) ?: continue
            val k = kinds(d, h, if (h.m == 10) shawwalDone(h.y) else 0)
            if (k.isNotEmpty()) out += SunnahDay(d, h, k)
        }
        return out
    }

    /** سنة رمضان المعروض: الحالي إن كنا فيه أو بعده هذه السنة، وإلا رمضان السنة الماضية */
    fun ramadanYear(today: LocalDate, offset: Int): Int? = hijri(today, offset)?.let { if (it.m >= 9) it.y else it.y - 1 }

    /** تذكيرات المساء السابق ([Fasting.REMIND_HOUR]) لأيام التطوع بعد [now]، الأقرب أولاً */
    fun reminders(now: LocalDateTime, offset: Int, count: Int, shawwalDone: (Int) -> Int = { 0 }): List<Pair<LocalDateTime, SunnahDay>> =
        upcoming(now.date, offset, count + 2, 60, shawwalDone)
            .map { day ->
                val eve = day.date.minusDays(1)
                LocalDateTime(eve.year, eve.monthNumber, eve.dayOfMonth, Fasting.REMIND_HOUR, 0) to day
            }
            .filter { it.first > now }
            .take(count)
}

/**
 * متابعة الصيام الواحدة في التطبيق (شاشة أندرويد وجسر iOS والتذكير)، محفوظة في الإعدادات بالمفاتيح fast_*
 * (قائمتها [STORE_KEYS] للنسخ الاحتياطي). التواريخ بيوم الموقع المختار، كمواقيت الصلاة.
 */
object Fasting {
    const val K_RAMADAN = "fast_ramadan"
    const val K_QADA = "fast_qada"
    const val K_QADA_LOG = "fast_qada_log"
    const val K_SUNNAH = "fast_sunnah"
    const val K_REMIND = "fast_remind"

    /** كل مفاتيح الصيام في الإعدادات */
    val STORE_KEYS = listOf(K_RAMADAN, K_QADA, K_QADA_LOG, K_SUNNAH, K_REMIND)

    /**
     * فرق التاريخ الهجري الذي يختاره المستخدم بالأيام (ليوافق رؤية الهلال في بلده). لا يوجد إعداد له بعد في التطبيق؛
     * يُقرأ هنا وحده، فيكفي ربطه به حين يضاف
     */
    const val K_HIJRI_OFFSET = "hijri_offset"

    /** ساعة التذكير مساء اليوم السابق (بوقت الموقع) */
    const val REMIND_HOUR = 21
    const val MAX_QADA = 999
    const val LOG_SIZE = 40
    const val SUNNAH_SIZE = 500

    private var loaded = false

    var state by mutableStateOf(FastingState())
        private set

    fun ensure() {
        if (loaded) return
        loaded = true
        AppSettings.init()
        state = read()
    }

    /** يُعاد قراءة المفاتيح fast_* من الإعدادات (بعد استعادة نسخة احتياطية مثلاً) */
    fun reload() {
        loaded = false
        ensure()
    }

    private fun read(): FastingState = FastingState(
        ramadan = AppSettings.getString(K_RAMADAN, "").split(';').mapNotNull { e ->
            val y = e.substringBefore(':').toIntOrNull() ?: return@mapNotNull null
            val m = e.substringAfter(':', "").filter { it == 'f' || it == 'm' || it == '0' }.take(30)
            if (m.isEmpty()) null else y to m
        }.toMap(),
        qada = AppSettings.getInt(K_QADA, 0).coerceIn(0, MAX_QADA),
        qadaLog = AppSettings.getString(K_QADA_LOG, "").split(';').mapNotNull { e ->
            val p = e.split(',')
            if (p.size != 3) return@mapNotNull null
            val d = p[1].removePrefix("+").toIntOrNull() ?: return@mapNotNull null
            QadaEntry(p[0], d, p[2])
        }.take(LOG_SIZE),
        sunnah = AppSettings.getString(K_SUNNAH, "").split(',').filter { it.length == 10 }.distinct().sorted(),
        remind = AppSettings.getBool(K_REMIND, false),
    )

    private fun set(s: FastingState) {
        val old = state
        state = s
        if (s.ramadan != old.ramadan) AppSettings.putString(K_RAMADAN, s.ramadan.entries.sortedByDescending { it.key }.joinToString(";") { "${it.key}:${it.value}" })
        if (s.qada != old.qada) AppSettings.putInt(K_QADA, s.qada)
        if (s.qadaLog != old.qadaLog) AppSettings.putString(K_QADA_LOG, s.qadaLog.joinToString(";") { "${it.date},${if (it.delta > 0) "+" else ""}${it.delta},${it.kind}" })
        if (s.sunnah != old.sunnah) AppSettings.putString(K_SUNNAH, s.sunnah.joinToString(","))
        if (s.remind != old.remind) AppSettings.putBool(K_REMIND, s.remind)
    }

    fun offset(): Int = if (AppSettings.ready) AppSettings.getInt(K_HIJRI_OFFSET, 0).coerceIn(-3, 3) else 0

    /** الآن بوقت الموقع المختار */
    fun now(): LocalDateTime = nowIn(AppSettings.place.tz)

    fun today(): LocalDate = now().date

    fun hijriToday(): HDate? = FastCalendar.hijri(today(), offset())

    /** ما سُجّل من الست في شوال السنة الهجرية [year] */
    fun shawwalDone(year: Int): Int {
        ensure()
        val off = offset()
        return state.sunnah.count { d ->
            val g = runCatching { LocalDate.parse(d) }.getOrNull() ?: return@count false
            FastCalendar.hijri(g, off)?.let { it.y == year && it.m == 10 && it.d >= 2 } == true
        }
    }

    /** أيام التطوع من اليوم، أقصاها [count] */
    fun upcoming(count: Int): List<SunnahDay> {
        ensure()
        return FastCalendar.upcoming(today(), offset(), count) { shawwalDone(it) }
    }

    /** تذكيرات المساء السابق القادمة، أقصاها [count] */
    fun reminders(count: Int): List<Pair<LocalDateTime, SunnahDay>> {
        ensure()
        return FastCalendar.reminders(now(), offset(), count) { shawwalDone(it) }
    }

    /** لحظة وقت محلي بالموقع المختار (ميلي ثانية منذ 1970) */
    fun epochMillis(at: LocalDateTime): Long {
        val zone = runCatching { TimeZone.of(AppSettings.place.tz) }.getOrElse { TimeZone.currentSystemDefault() }
        return at.toInstant(zone).toEpochMilliseconds()
    }

    /** هل يصوم المستخدم اليوم: أيام رمضان ما لم يُعلَّم إفطاراً، أو تطوع سجّله اليوم، أو يوم قضاه اليوم */
    fun fastingToday(): Boolean {
        ensure()
        val today = today()
        val h = FastCalendar.hijri(today, offset()) ?: return false
        if (FastCalendar.forbidden(h)) return false
        if (h.m == 9) return state.mark(h.y, h.d) != DayMark.MISSED
        val d = today.toString()
        return d in state.sunnah || state.qadaLog.any { it.date == d && it.kind == "done" && it.delta < 0 }
    }

    /** يوم من رمضان [year] في المستقبل لا يُعلَّم */
    fun canMark(year: Int, day: Int): Boolean {
        val g = FastCalendar.gregorian(year, 9, day, offset()) ?: return false
        return g <= today()
    }

    /** لمس يوم من رمضان: ينتقل إلى العلامة التالية */
    fun cycle(year: Int, day: Int) {
        ensure()
        if (!canMark(year, day)) return
        set(state.withMark(year, day, state.mark(year, day).next(), today().toString()))
    }

    fun qadaPlus() { ensure(); set(state.qadaChange(+1, "add", today().toString())) }

    fun qadaMinus() { ensure(); set(state.qadaChange(-1, "remove", today().toString())) }

    /** «قضيت يوماً» */
    fun madeUp() { ensure(); set(state.qadaChange(-1, "done", today().toString())) }

    fun voluntaryToday(): Boolean = today().toString() in state.sunnah

    /** صيام تطوع اليوم: يُسجَّل أو يُرفع */
    fun setVoluntaryToday(on: Boolean) { ensure(); set(state.withSunnah(today().toString(), on)) }

    /** التذكير؛ يعيد صاحبه جدولة الإشعارات بعده (PrayerAlerts.reschedule) */
    fun setRemind(on: Boolean) { ensure(); set(state.copy(remind = on)) }
}
