package com.osatks.mizan.quran

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.osatks.mizan.core.AppSettings
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.BitSet
import kotlin.math.ceil

/**
 * متابعة القراءة والختمة: الصفحات المقروءة من الختمة الحالية، ومدة الختمة المختارة،
 * وورد اليوم (عدد الصفحات المطلوبة اليوم لإتمام الختمة في موعدها)، وأيام القراءة المتتالية.
 * تُحسب الصفحة مقروءة إذا بقي عليها القارئ ثلاث ثوانٍ.
 */
object QuranProgress {
    const val PAGES = 604
    val PLANS = listOf(0, 7, 10, 15, 30, 60)

    private var loaded = false
    private var read = BitSet(PAGES + 1)

    var readCount by mutableIntStateOf(0)
        private set
    var planDays by mutableIntStateOf(30)
        private set
    var start by mutableStateOf(LocalDate.now())
        private set
    var todayCount by mutableIntStateOf(0)
        private set
    /** الصفحات المقروءة قبل اليوم (لحساب ورد اليوم بثبات طوال اليوم) */
    private var beforeToday = 0
    var streak by mutableIntStateOf(0)
        private set
    var completed by mutableIntStateOf(0)
        private set
    private var lastDay: LocalDate? = null

    fun ensure() {
        // قد تبقى العملية حية إلى اليوم التالي (الأدوات): يبدأ ورد يوم جديد
        if (loaded) { if (AppSettings.getString("k_today_date") != LocalDate.now().toString()) rollDay(); return }
        loaded = true
        read = BitSet.valueOf(java.util.Base64.getDecoder().decode(AppSettings.getString("k_read")))
        readCount = read.cardinality()
        planDays = AppSettings.getInt("k_plan", 30)
        start = runCatching { LocalDate.parse(AppSettings.getString("k_start")) }.getOrDefault(LocalDate.now())
        completed = AppSettings.getInt("k_done", 0)
        streak = AppSettings.getInt("k_streak", 0)
        lastDay = runCatching { LocalDate.parse(AppSettings.getString("k_last")) }.getOrNull()
        rollDay()
    }

    /** يوم جديد: يبدأ عدّ ورد اليوم من الصفر، وتنقطع السلسلة إن فات يوم دون قراءة */
    private fun rollDay() {
        val today = LocalDate.now()
        if (AppSettings.getString("k_today_date") == today.toString()) {
            todayCount = AppSettings.getInt("k_today", 0)
            beforeToday = AppSettings.getInt("k_before", readCount)
        } else {
            todayCount = 0
            beforeToday = readCount
            AppSettings.putString("k_today_date", today.toString())
            AppSettings.putInt("k_today", 0)
            AppSettings.putInt("k_before", beforeToday)
        }
        val last = lastDay
        if (last != null && ChronoUnit.DAYS.between(last, today) > 1) { streak = 0; AppSettings.putInt("k_streak", 0) }
    }

    fun isRead(page: Int) = read[page]

    fun markRead(page: Int) {
        ensure()
        if (page !in 1..PAGES || read[page]) return
        if (AppSettings.getString("k_today_date") != LocalDate.now().toString()) rollDay()
        read.set(page)
        readCount = read.cardinality()
        todayCount++
        val today = LocalDate.now()
        if (lastDay != today) {
            streak = if (lastDay != null && ChronoUnit.DAYS.between(lastDay, today) == 1L) streak + 1 else 1
            lastDay = today
            AppSettings.putInt("k_streak", streak)
            AppSettings.putString("k_last", today.toString())
        }
        if (readCount >= PAGES) completed++.also { AppSettings.putInt("k_done", completed) }
        save()
    }

    private fun save() {
        AppSettings.putString("k_read", java.util.Base64.getEncoder().encodeToString(read.toByteArray()))
        AppSettings.putInt("k_today", todayCount)
    }

    val finished get() = readCount >= PAGES

    /** ختمة جديدة بالمدة المختارة (0 = بلا مدة محددة) */
    fun restart(days: Int = planDays) {
        ensure()
        read = BitSet(PAGES + 1); readCount = 0
        planDays = days; start = LocalDate.now()
        todayCount = 0; beforeToday = 0
        AppSettings.putInt("k_plan", days)
        AppSettings.putString("k_start", start.toString())
        AppSettings.putInt("k_before", 0)
        save()
    }

    fun changePlan(days: Int) { ensure(); planDays = days; AppSettings.putInt("k_plan", days) }

    /** الأيام الباقية في الخطة (اليوم منها) */
    fun daysLeft(): Int = if (planDays == 0) 0 else (planDays - ChronoUnit.DAYS.between(start, LocalDate.now()).toInt()).coerceAtLeast(1)

    /** صفحات ورد اليوم: ما بقي من الختمة عند بداية اليوم مقسوماً على الأيام الباقية */
    fun dailyGoal(): Int = if (planDays == 0) 0 else ceil((PAGES - beforeToday).coerceAtLeast(0) / daysLeft().toDouble()).toInt()

    /** أول صفحة لم تُقرأ (لمتابعة الورد) */
    fun nextUnread(): Int = (1..PAGES).firstOrNull { !read[it] } ?: 1
}
