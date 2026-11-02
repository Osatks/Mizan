package com.osatks.mizan.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.osatks.mizan.i18n.Strings
import kotlinx.datetime.LocalDate

/**
 * حال المسبحة: الذكر المختار، والعدد، والهدف، والدورات، وعدد اليوم والمجموع.
 * قيمة ثابتة تُحسب منها الحال التالية ([TasbihState.count]) فتُختبر بلا تخزين ولا واجهة.
 */
data class TasbihState(
    /** free: ذكر يختاره المستخدم بهدف يختاره، و salah: تسبيح ما بعد الصلاة (33 + 33 + 33 ثم تمام المائة) */
    val mode: String = "free",
    /** الذكر المختار في free: موضعه في [Tasbih.DHIKR] */
    val idx: Int = 0,
    /** الخطوة في salah: موضعها في [Tasbih.SALAH] */
    val step: Int = 0,
    val count: Int = 0,
    /** هدف free: 33 أو 34 أو 100 أو 1000، و 0 بلا حد */
    val target: Int = 33,
    val rounds: Int = 0,
    val total: Int = 0,
    val today: Int = 0,
    /** يوم [today] (yyyy-mm-dd): يعود عدد اليوم صفراً حين يتغير */
    val day: String = "",
) {
    /** موضع الذكر في [Tasbih.DHIKR]، أو -1 لتمام المائة */
    val dhikrIndex: Int get() = if (mode == "salah") Tasbih.SALAH[step.coerceIn(Tasbih.SALAH.indices)].first else idx.coerceIn(Tasbih.DHIKR.indices)

    /** الذكر بالعربية كما يُعرض في كل اللغات */
    val text: String get() = dhikrIndex.let { if (it < 0) Tasbih.TAMAM else Tasbih.DHIKR[it] }

    /** العدد المطلوب للذكر الحالي، و 0 بلا حد */
    val goal: Int get() = if (mode == "salah") Tasbih.SALAH[step.coerceIn(Tasbih.SALAH.indices)].second else target

    fun meaning(s: Strings): String = dhikrIndex.let { if (it < 0) s.tamamMeaning else s.dhikrMeanings.getOrElse(it) { "" } }

    /** يُبدأ يوم جديد: عدد اليوم صفر */
    fun on(day: String): TasbihState = if (this.day == day) this else copy(day = day, today = 0)

    /** تسبيحة واحدة: العدد والمجموع وعدد اليوم، ثم إتمام الهدف إن بلغه */
    fun count(): Pair<TasbihState, Tasbih.Event> {
        val g = goal
        val n = copy(count = count + 1, total = total + 1, today = today + 1)
        if (g <= 0 || n.count < g) return n to Tasbih.Event.COUNT
        return if (mode == "salah") {
            if (step < Tasbih.SALAH.size - 1) n.copy(step = step + 1, count = 0) to Tasbih.Event.NEXT_DHIKR
            else n.copy(step = 0, count = 0, rounds = rounds + 1) to Tasbih.Event.HUNDRED
        } else n.copy(count = 0, rounds = rounds + 1) to Tasbih.Event.ROUND
    }

    /** يعود العدد صفراً (وفي salah إلى أول خطوة)؛ الدورات والمجموع باقية */
    fun reset(): TasbihState = copy(count = 0, step = 0)

    /** يتراجع تسبيحة واحدة في الذكر الحالي */
    fun undo(): TasbihState = if (count <= 0) this
        else copy(count = count - 1, total = (total - 1).coerceAtLeast(0), today = (today - 1).coerceAtLeast(0))
}

/**
 * المسبحة الواحدة في التطبيق: شاشة المسبحة، وفي iOS زر الإجراء ومركز التحكم والجزيرة الديناميكية وأداة الشاشة الرئيسية،
 * كلها تعدّ في الحال نفسها (المفاتيح ts_* في الإعدادات، كما كانت قبل)، وكل تغيير يصل إلى [onChange].
 */
object Tasbih {
    /** نصوص الأذكار بالعربية (تُعرض كما هي في كل اللغات، ومعناها تحتها) */
    val DHIKR = listOf(
        "سُبْحَانَ اللَّهِ",
        "الْحَمْدُ لِلَّهِ",
        "اللَّهُ أَكْبَرُ",
        "لَا إِلَهَ إِلَّا اللَّهُ",
        "أَسْتَغْفِرُ اللَّهَ",
        "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
        "سُبْحَانَ اللَّهِ الْعَظِيمِ",
        "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
        "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ",
    )

    /** تسبيح ما بعد الصلاة: 33 + 33 + 33 ثم تمام المائة */
    val SALAH = listOf(0 to 33, 1 to 33, 2 to 33, -1 to 1)
    const val TAMAM = "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ"
    val TARGETS = listOf(33, 34, 100, 1000, 0)

    /** ما حدث مع التسبيحة: عدّ فقط، أو انتقال إلى الذكر التالي (بعد الصلاة)، أو إتمام دورة، أو إتمام المائة */
    enum class Event { COUNT, NEXT_DHIKR, ROUND, HUNDRED }

    private var loaded = false

    var state by mutableStateOf(TasbihState())
        private set

    /** iOS: الجزيرة الديناميكية والأداة ومركز التحكم تتبع كل تغيير (IosTasbih) */
    var onChange: (() -> Unit)? = null

    fun ensure() {
        if (!loaded) {
            loaded = true
            AppSettings.init()
            state = TasbihState(
                mode = AppSettings.getString("ts_mode", "free").takeIf { it == "salah" } ?: "free",
                idx = AppSettings.getInt("ts_idx", 0).coerceIn(DHIKR.indices),
                step = AppSettings.getInt("ts_step", 0).coerceIn(SALAH.indices),
                count = AppSettings.getInt("ts_count", 0).coerceAtLeast(0),
                target = AppSettings.getInt("ts_target", 33).let { if (it in TARGETS) it else 33 },
                rounds = AppSettings.getInt("ts_rounds", 0).coerceAtLeast(0),
                total = AppSettings.getInt("ts_total", 0).coerceAtLeast(0),
                today = AppSettings.getInt("ts_today", 0).coerceAtLeast(0),
                day = AppSettings.getString("ts_day", ""),
            )
        }
        // يوم جديد منذ آخر تسبيحة
        val today = LocalDate.now().toString()
        if (state.day != today) set(state.on(today), notify = false)
    }

    private fun set(s: TasbihState, notify: Boolean = true) {
        state = s
        AppSettings.putString("ts_mode", s.mode); AppSettings.putInt("ts_idx", s.idx); AppSettings.putInt("ts_step", s.step)
        AppSettings.putInt("ts_count", s.count); AppSettings.putInt("ts_target", s.target); AppSettings.putInt("ts_rounds", s.rounds)
        AppSettings.putInt("ts_total", s.total); AppSettings.putInt("ts_today", s.today); AppSettings.putString("ts_day", s.day)
        if (notify) onChange?.invoke()
    }

    /** تسبيحة واحدة؛ يعيد ما حدث (لاهتزاز إتمام الدورة ورسالته) */
    fun count(): Event {
        ensure()
        val (next, event) = state.count()
        set(next)
        return event
    }

    /** عدة تسبيحات دفعة واحدة (ما عُدّ في الأداة حين تعذّر تشغيل التطبيق)؛ يعيد آخر حدث غير العدّ المجرد */
    fun count(times: Int): Event {
        ensure()
        var s = state
        var last = Event.COUNT
        repeat(times.coerceIn(0, 100_000)) { val (n, e) = s.count(); s = n; if (e != Event.COUNT) last = e }
        set(s)
        return last
    }

    fun reset() { ensure(); set(state.reset()) }

    fun undo() { ensure(); set(state.undo()) }

    fun setMode(mode: String) { ensure(); set(state.copy(mode = if (mode == "salah") "salah" else "free", count = 0, step = 0)) }

    fun setDhikr(i: Int) { ensure(); set(state.copy(idx = i.coerceIn(DHIKR.indices), count = 0)) }

    fun setTarget(t: Int) { ensure(); set(state.copy(target = if (t in TARGETS) t else 33, count = 0)) }
}
