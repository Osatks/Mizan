package com.osatks.mizan

import com.osatks.mizan.core.PrayTimes
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * الجداول الرسمية كاملة (ملفات csv في مجلد official، منسوخة من مصادرها المذكورة في رأس كل ملف): ما يعرضه التطبيق لكل مدينة
 * بطريقتها الافتراضية يطابق جدول جهتها الرسمية بالدقيقة، أو لا يبعد عنه أكثر من الحد المذكور لكل صلاة.
 * التقرير الكامل في build/official-report.txt.
 */
class OfficialTablesTest {
    private val keys = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")
    private val dir = File("src/androidUnitTest/official")

    private class Row(val day: LocalDate, val times: List<Int?>)

    private fun minutes(s: String): Int? = s.trim().takeIf { it.matches(Regex("\\d{1,2}:\\d{2}")) }
        ?.split(":")?.let { (h, m) -> h.toInt() * 60 + m.toInt() }

    private fun load(f: File): List<Row> = f.readLines().map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .mapNotNull { line ->
            val c = line.split(",")
            runCatching { LocalDate.parse(c[0].trim()) }.getOrNull()?.let { d -> Row(d, (1..6).map { i -> c.getOrNull(i)?.let(::minutes) }) }
        }

    /** «CC_city[_variant].csv» → مفتاح المدينة في التطبيق */
    private fun cityOf(f: File) = f.name.removeSuffix(".csv").split("_")[1]

    class Stat { var exact = 0; var one = 0; var more = 0; var worst = 0 }

    /** حدود الملف: أقصى فرق مقبول لكل صلاة (الافتراضي دقيقة)، وصلوات لا تُقارن، وأيام شاذة في الجدول الرسمي نفسه */
    private class Limits(val tol: Map<String, Int> = emptyMap(), val skip: Set<String> = emptySet(), val odd: Set<String> = emptySet())

    private val europe = Regex("(AT_|BE_|DE_|NL_|SE_).*")

    /** null: جدول مسجد بعينه لا جهة رسمية، يُعرض في التقرير ولا يُشترط */
    private fun limits(name: String): Limits? = when {
        // مساجد تتبع غير الطريقة الافتراضية لبلدها (العصر الحنفي، أو عشاء بعد المغرب بمدة، أو جدول يدوي)
        name.matches(Regex("(GB_birmingham|FR_lyon|US_.*|CA_.*|AU_.*)\\.csv")) -> null
        // جدول لندن الموحد: الفجر والعشاء من الرصد، وأقرب نموذج له حد لجنة رؤية الهلال الموسمي
        name.startsWith("GB_london") -> Limits(skip = setOf("fajr", "isha"))
        // عمود الظهر في جداول باكستان هو الزوال لا دخول الوقت
        name.startsWith("PK_") -> Limits(skip = setOf("dhuhr"))
        // تقويم الأقصى الدائم جدول ثابت لا معادلة: فرق موسمي حتى ثلاث دقائق
        name.startsWith("PS_") -> Limits(tol = mapOf("fajr" to 3, "sunrise" to 2, "maghrib" to 2, "isha" to 2))
        // مؤتمر إسطنبول في العروض العليا صيفاً: قاعدة التقدير تقريبية
        name.matches(europe) -> Limits(tol = mapOf("fajr" to 13, "isha" to 10))
        // جدول المشيخة فيه تذبذب موسمي ±1.5، وعصر JAKIM لنقطة مرجعية في المنطقة لا لوسط المدينة
        name.startsWith("BA_") -> Limits(tol = keys.associateWith { 2 })
        name.startsWith("MY_") -> Limits(tol = mapOf("asr" to 2))
        // صحيفتا 21 و22 شباط مزاحتان يوماً في الجدول الرسمي، ورمضان الوزارة في قطر قبل جدول التطبيق بيوم
        name.startsWith("SY_") || name.startsWith("LB_") || name.startsWith("QA_") || name.startsWith("BD_") ->
            Limits(tol = keys.associateWith { 2 })
        // يوما التحويل إلى قاعدة الصيف وعنها مطبوعان شاذين في جدول قازان
        name.startsWith("RU_kazan") -> Limits(odd = setOf("2026-05-05 fajr", "2026-08-08 isha"))
        else -> Limits()
    }

    /**
     * المغرب على توقيت غرينتش دائماً منذ 2026-09-20 (المرسوم 2.26.530)، وجداول الوزارة به؛ بيانات المناطق الزمنية في
     * Java على جهاز الاختبار قد تسبق ذلك، فتُقارن بتوقيت ثابت (الأجهزة تأخذ التوقيت من نظامها المحدَّث)
     */
    private fun tzOf(f: File, tz: String) = if (f.name.startsWith("MA_")) "UTC" else tz

    private fun compare(f: File, failures: MutableList<String> = ArrayList()): Map<String, Stat> {
        val c = PrayTimes.city(cityOf(f)) ?: error("unknown city in ${f.name}")
        val hanafi = PrayTimes.method(c.method).hanafi
        val lim = limits(f.name)
        val stats = keys.associateWith { Stat() }
        for (r in load(f)) {
            val t = PrayTimes.compute(r.day, c.lat, c.lng, tzOf(f, c.tz), c.method, hanafi, c.elev)
            keys.forEachIndexed { i, k ->
                val official = r.times[i] ?: return@forEachIndexed
                val shown = minutes(PrayTimes.format(t[k], h24 = true)) ?: return@forEachIndexed
                var d = shown - official
                if (d > 720) d -= 1440 else if (d < -720) d += 1440
                val s = stats.getValue(k)
                when (abs(d)) { 0 -> s.exact++; 1 -> s.one++; else -> s.more++ }
                if (abs(d) > abs(s.worst)) s.worst = d
                if (lim != null && k !in lim.skip && "${r.day} $k" !in lim.odd && abs(d) > (lim.tol[k] ?: 1)) {
                    failures += "${f.name} ${r.day} $k: app ${PrayTimes.format(t[k], h24 = true)}, official ${"%02d:%02d".format(official / 60, official % 60)}"
                }
            }
        }
        return stats
    }

    @Test fun report() {
        val files = dir.listFiles { f -> f.name.endsWith(".csv") }!!.sortedBy { it.name }
        assertTrue("no official tables in $dir", files.isNotEmpty())
        val out = StringBuilder("file method | per prayer: exact/±1/±2+ (worst app−official)\n")
        for (f in files) {
            val c = PrayTimes.city(cityOf(f))!!
            val st = compare(f)
            out.append(f.name.padEnd(30)).append(c.method.padEnd(11)).append(" | ")
            for (k in keys) {
                val s = st.getValue(k)
                if (s.exact + s.one + s.more == 0) continue
                out.append("$k ${s.exact}/${s.one}/${s.more} (${if (s.worst > 0) "+" else ""}${s.worst})  ")
            }
            out.append('\n')
        }
        File("build").mkdirs()
        File("build/official-report.txt").writeText(out.toString())
    }

    /** كل جدول رسمي ضمن حدوده */
    @Test fun matchesOfficialTables() {
        val failures = ArrayList<String>()
        for (f in dir.listFiles { f -> f.name.endsWith(".csv") }!!.sortedBy { it.name }) compare(f, failures)
        assertTrue("${failures.size} times outside the official tables:\n" + failures.take(40).joinToString("\n"), failures.isEmpty())
    }
}
