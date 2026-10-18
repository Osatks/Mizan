package com.osatks.mizan.core

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.periodUntil
import kotlinx.datetime.plus
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** أسماء مألوفة (كما في java.time) فوق kotlinx-datetime، ليعمل الكود نفسه في أندرويد و iOS */

fun LocalDate.Companion.now(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
fun LocalDate.Companion.ofEpochDay(day: Long): LocalDate = fromEpochDays(day.toInt())
fun LocalDate.Companion.of(y: Int, m: Int, d: Int): LocalDate = LocalDate(y, m, d)
fun LocalDateTime.Companion.now(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

fun LocalDate.toEpochDay(): Long = toEpochDays().toLong()
fun LocalDate.plusDays(n: Long): LocalDate = plus(n, DateTimeUnit.DAY)
fun LocalDate.plusDays(n: Int): LocalDate = plus(n, DateTimeUnit.DAY)
fun LocalDate.minusDays(n: Long): LocalDate = minus(n, DateTimeUnit.DAY)
fun LocalDate.minusDays(n: Int): LocalDate = minus(n, DateTimeUnit.DAY)
fun LocalDate.plusMonths(n: Int): LocalDate = plus(n, DateTimeUnit.MONTH)
fun LocalDate.plusYears(n: Int): LocalDate = plus(n, DateTimeUnit.YEAR)
fun LocalDate.withDayOfMonth(d: Int): LocalDate = LocalDate(year, monthNumber, d)
val LocalDate.monthValue: Int get() = monthNumber
fun LocalDate.isBefore(o: LocalDate) = this < o
fun LocalDate.isAfter(o: LocalDate) = this > o
fun LocalDate.lengthOfMonth(): Int = LocalDate(year, monthNumber, 1).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
fun LocalDate.atTime(h: Int, m: Int) = LocalDateTime(year, monthNumber, dayOfMonth, h, m)

/** الأيام بين تاريخين (موجب إن كان b بعد a) */
fun daysBetween(a: LocalDate, b: LocalDate): Long = (b.toEpochDays() - a.toEpochDays()).toLong()

/** رقم اليوم في الأسبوع: الاثنين 1 … الأحد 7 */
val DayOfWeek.iso: Int get() = isoDayNumber

/** فرق المنطقة الزمنية عن UTC بالساعات في وقت محلي معيّن (يتبع التوقيت الصيفي) */
fun tzOffsetHours(tz: String, at: LocalDateTime): Double {
    val zone = runCatching { TimeZone.of(tz) }.getOrElse { TimeZone.currentSystemDefault() }
    return zone.offsetAt(at.toInstant(zone)).totalSeconds / 3600.0
}

fun systemTimeZoneId(): String = TimeZone.currentSystemDefault().id

// ---------- LocalDateTime (وقت محلي بلا منطقة؛ الحساب كأنه UTC فلا يتأثر بالتوقيت الصيفي داخل اليوم) ----------

private val UTC = TimeZone.UTC

/** الوقت الآن في منطقة زمنية (معرّف IANA مثل Asia/Riyadh) */
fun nowIn(tz: String): LocalDateTime {
    val zone = runCatching { TimeZone.of(tz) }.getOrElse { TimeZone.currentSystemDefault() }
    return Clock.System.now().toLocalDateTime(zone)
}

fun LocalDateTime.toLocalDate(): LocalDate = date
fun LocalDate.atStartOfDay(): LocalDateTime = LocalDateTime(year, monthNumber, dayOfMonth, 0, 0)
fun LocalDateTime.plusSeconds(n: Long): LocalDateTime = (toInstant(UTC) + n.seconds).toLocalDateTime(UTC)
fun LocalDateTime.plusHours(n: Long): LocalDateTime = plusSeconds(n * 3600)
fun LocalDateTime.isAfter(o: LocalDateTime) = this > o
fun LocalDateTime.isBefore(o: LocalDateTime) = this < o

/** المدة بين وقتين محليين */
fun durationBetween(a: LocalDateTime, b: LocalDateTime): Duration = b.toInstant(UTC) - a.toInstant(UTC)

/** عدد أيام شهر ميلادي */
fun monthLength(year: Int, month: Int): Int = LocalDate(year, month, 1).lengthOfMonth()

/** الفرق بالسنوات والأشهر والأيام (كما في java.time.Period.between) */
fun periodBetween(a: LocalDate, b: LocalDate): kotlinx.datetime.DatePeriod = a.periodUntil(b)

/** التاريخ نفسه في سنة أخرى (29 فبراير يصير 28 في السنة غير الكبيسة) */
fun LocalDate.withYear(y: Int): LocalDate = LocalDate(y, monthNumber, dayOfMonth.coerceAtMost(monthLength(y, monthNumber)))
