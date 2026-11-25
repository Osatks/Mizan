package com.osatks.mizan.core

import com.osatks.mizan.i18n.LocaleSync
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.platform.LiveCountdown
import com.osatks.mizan.platform.LiveTasbih
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.QuranProgress
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/** مخزن مفاتيح وقيم يُنسخ ويُستعاد: إعدادات التطبيق ([AppSettings.backupStore])، أو خريطة في الاختبارات */
interface BackupStore {
    /** القيم بالأنواع الموحّدة: String أو Long أو Double أو Boolean أو Set<String> */
    fun snapshot(): Map<String, Any>
    fun put(k: String, v: Any)
    fun remove(k: String)
}

/** ملف لا يصلح للاستعادة؛ [reason] يحدد الرسالة التي تُعرض */
class BackupException(val reason: Reason) : Exception(reason.name) {
    enum class Reason {
        /** ليس JSON، أو ليس نسخة احتياطية من ميزان */
        INVALID,
        /** صيغته أحدث من هذا الإصدار من التطبيق */
        TOO_NEW,
        /** لا بيانات فيه */
        EMPTY,
    }
}

/** نسخة احتياطية مقروءة: [createdAt] بالميلي ثانية منذ 1970 (UTC)، و [data] المفاتيح بقيمها الموحّدة */
data class BackupDoc(
    val format: Int,
    val appVersion: String,
    val platform: String,
    val createdAt: Long,
    val data: Map<String, Any>,
)

/**
 * النسخ الاحتياطي: مستند JSON واحد بإصدار صيغته، فيه كل مفاتيح المستخدم من مخزن الإعدادات («mizan»):
 * الإعدادات، والعلامات وموضع القراءة والختمة، والمسبحة، ومدخلات الزكاة، وما تضيفه الميزات الجديدة.
 * التصميم عام: كل مفتاح يُنسخ تلقائياً إلا ما في قائمة الاستثناء (ما يخص هذا الجهاز أو يتغير وحده).
 * فميزة جديدة تحفظ مفاتيحها في الإعدادات (fast_…، kids_…) تدخل في النسخة دون تعديل هنا.
 *
 * الأنواع موحّدة بين أندرويد و iOS (نص، عدد، صح/خطأ، مجموعة نصوص)، فنسخة أندرويد تُستعاد في iPhone والعكس.
 * المستند نفسه يُحفظ ملفاً، ويُزامَن عبر iCloud في iOS. لا كلمات سر فيه، ولا يُرسل إلى أي خادم لميزان.
 *
 * {
 *   "kind": "mizan-backup", "format": 1, "app": "Mizan", "appVersion": "2.2.1", "platform": "android",
 *   "createdAt": 1764064800000, "createdAtUtc": "2026-11-25T10:00:00Z",
 *   "data": { "accent": "emerald", "notify": true, "q_size": 26, "q_marks": "12,255", … }
 * }
 */
object Backup {
    const val KIND = "mizan-backup"
    /** إصدار الصيغة: يزيد إن تغيّر معنى المستند (لا حين تُضاف مفاتيح)؛ النسخة الأحدث صيغةً تُرفض برسالة واضحة */
    const val FORMAT = 1
    /** أكبر ملف يُقرأ (النسخة الفعلية بضعة كيلوبايتات) */
    const val MAX_BYTES = 2 * 1024 * 1024

    /**
     * مفاتيح لا تُنسخ ولا تُمسّ عند الاستعادة: حالة هذا الجهاز وأذوناته، وسجلات الإشعارات، وتلميحات الواجهة.
     * ميزة جديدة لها مفتاح من هذا النوع تضيفه هنا، أو تسمّيه ببادئة من [EXCLUDED_PREFIXES].
     */
    val EXCLUDED_KEYS: Set<String> = setOf(
        "lang_synced",      // أندرويد: مزامنة لغة التطبيق مع «لغات التطبيقات» في النظام
        "dnd_prev",         // أندرويد: «عدم الإزعاج» قبل وضع التركيز في هذا الجهاز
        "dnd_asked",        // أندرويد: سُئل عن إذن «عدم الإزعاج» في هذا الجهاز
        "adhan_done",       // آخر أذان أُكمل في التطبيق (سجل الإشعارات)
        "calendar_sync",    // iOS: تقويم المواقيت في هذا الجهاز (يحتاج إذنه)
        "spotlight_quran",  // iOS: فهرسة السور في بحث هذا الجهاز
        "onboarded",        // الترحيب وأسئلة الأذونات تخص الجهاز
        "guideSeen",
        "ayah_hint",        // تلميح لمس الآية
        "open_route",       // iOS: وسيطا تشغيل للقطات المتجر وتسجيل الحركة (fastlane/screenshots.sh)
        "motion_script",
    )

    /**
     * بادئات لا تُنسخ: rv_ عدّاد طلب التقييم، icloud_ حال المزامنة في هذا الجهاز، calendar. معرّف تقويم الجهاز
     * (يكتبه Swift)، وأعراف للميزات الجديدة: cache_ و device_ و notif_ و widget_ لما يخص الجهاز أو يتغير وحده.
     */
    val EXCLUDED_PREFIXES: List<String> = listOf("rv_", "icloud_", "calendar.", "cache_", "device_", "notif_", "widget_")

    /** هل يدخل المفتاح في النسخة؟ */
    fun included(key: String): Boolean =
        key.isNotEmpty() && key !in EXCLUDED_KEYS && EXCLUDED_PREFIXES.none { key.startsWith(it) }

    /** القيمة بنوع موحّد، أو null لما لا يُنقل (أعداد غير منتهية، قوائم فيها غير النصوص…) */
    internal fun normalize(v: Any?): Any? = when (v) {
        is String, is Boolean, is Long -> v
        is Int -> v.toLong()
        is Short -> v.toLong()
        is Byte -> v.toLong()
        is Double -> v.takeIf { it.isFinite() }
        is Float -> v.toDouble().takeIf { it.isFinite() }
        is Collection<*> -> if (v.all { it is String }) v.filterIsInstance<String>().sorted().toSet() else null
        else -> null
    }

    /** يكتب المستند من قيم المخزن (المستثنى منها يُحذف) */
    fun encode(values: Map<String, Any>, createdAt: Long, platform: String, appVersion: String): String {
        val data = LinkedHashMap<String, Any>()
        for (k in values.keys.sorted()) {
            if (!included(k)) continue
            val v = normalize(values[k]) ?: continue
            // مجموعة النصوص مصفوفة في JSON
            data[k] = if (v is Set<*>) v.toList() else v
        }
        val doc = linkedMapOf<String, Any>(
            "kind" to KIND,
            "format" to FORMAT.toLong(),
            "app" to "Mizan",
            "appVersion" to appVersion,
            "platform" to platform,
            "createdAt" to createdAt,
            "createdAtUtc" to Instant.fromEpochMilliseconds(createdAt).toString(),
            "data" to data,
        )
        return Json.write(doc) + "\n"
    }

    /** يقرأ المستند ويتحقق منه؛ يرمي [BackupException] إن لم يصلح */
    fun decode(text: String): BackupDoc {
        val root = try {
            Json.obj(text.removePrefix("﻿").trim())
        } catch (e: Exception) {
            throw BackupException(BackupException.Reason.INVALID)
        }
        if (root["kind"] != KIND) throw BackupException(BackupException.Reason.INVALID)
        val format = (root["format"] as? Long)?.toInt() ?: throw BackupException(BackupException.Reason.INVALID)
        if (format < 1) throw BackupException(BackupException.Reason.INVALID)
        if (format > FORMAT) throw BackupException(BackupException.Reason.TOO_NEW)
        val raw = root["data"] as? Map<*, *> ?: throw BackupException(BackupException.Reason.INVALID)
        val data = LinkedHashMap<String, Any>()
        for ((k, v) in raw) {
            val key = k as? String ?: continue
            if (!included(key)) continue
            normalize(v)?.let { data[key] = it }
        }
        if (data.isEmpty()) throw BackupException(BackupException.Reason.EMPTY)
        return BackupDoc(
            format = format,
            appVersion = root["appVersion"] as? String ?: "",
            platform = root["platform"] as? String ?: "",
            createdAt = (root["createdAt"] as? Number)?.toLong() ?: 0L,
            data = data,
        )
    }

    /**
     * يجعل المخزن مطابقاً للنسخة: يحذف كل مفتاح منسوخ ليس فيها ويكتب ما فيها،
     * ولا يمس المفاتيح المستثناة (حال هذا الجهاز تبقى كما هي)
     */
    fun apply(store: BackupStore, data: Map<String, Any>) {
        for (k in store.snapshot().keys) if (included(k) && k !in data) store.remove(k)
        for ((k, v) in data) if (included(k)) store.put(k, v)
    }

    // ---------- في التطبيق ----------

    fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    /** اسم الملف: Mizan-backup-2026-11-25.json */
    fun fileName(): String = "Mizan-backup-${LocalDate.now()}.json"

    /** نسخة من إعدادات التطبيق الآن ([createdAt]: وقتها، وفي iCloud وقت آخر تغيير) */
    fun export(createdAt: Long = nowMillis()): String {
        AppSettings.init()
        val values = AppSettings.backupStore.snapshot().toMutableMap()
        // لغة لم يخترها المستخدم ليس لها مفتاح: تُكتب كما هي («system») حتى لا يظنها الترحيل لغة الإصدارات العربية
        if ("lang" !in values) values["lang"] = AppSettings.lang
        return encode(values, createdAt, Platform.name, Platform.versionName())
    }

    /** أثناء الاستعادة: الكتابات ليست تغييرات محلية (لا تُرسل إلى iCloud) */
    var restoring: Boolean = false
        private set

    /** بعد كل استعادة (يضبطه iOS: مزامنة iCloud وتحديث الأدوات). [fromCloud]: البيانات جاءت من iCloud نفسه */
    var afterRestore: ((fromCloud: Boolean) -> Unit)? = null

    /**
     * يستبدل بيانات التطبيق ببيانات النسخة، ثم يعيد قراءة ما في الذاكرة (حالة Compose في الإعدادات والمصحف
     * والختمة والمسبحة) ويعيد جدولة التنبيهات: تتبدل الواجهة فوراً دون إعادة تشغيل التطبيق.
     */
    fun restore(doc: BackupDoc, fromCloud: Boolean = false) {
        AppSettings.init()
        val data = if ("lang" in doc.data) doc.data else doc.data + ("lang" to AppSettings.lang)
        restoring = true
        try {
            apply(AppSettings.backupStore, data)
            refresh()
        } finally {
            restoring = false
        }
        afterRestore?.invoke(fromCloud)
    }

    private fun refresh() {
        AppSettings.reload()
        QuranPrefs.reload()
        QuranProgress.reload()
        Tasbih.reload()
        LiveCountdown.forget()
        LiveTasbih.forget()
        // أندرويد 13+: اللغة إلى «لغات التطبيقات» في النظام، وإلا أعادها النظام عند العودة إلى التطبيق
        LocaleSync.choose(AppSettings.lang)
        PrayerAlerts.reschedule()
        // أذان مختار غير مضمّن ولم يُنزَّل في هذا الجهاز: يُنزَّل في الخلفية، وحتى ذلك يُسمع المضمّن
        listOfNotNull(Muezzins.of(AppSettings.muezzin), Muezzins.of(AppSettings.fajrMuezzin))
            .distinct()
            .filter { !AdhanSounds.available(it) }
            .forEach { m -> AdhanSounds.start(m) { ok -> if (ok) PrayerAlerts.reschedule() } }
    }
}

/**
 * تقسيم النسخة في iCloud (NSUbiquitousKeyValueStore: 1 ميغابايت و 1024 مفتاحاً على الأكثر):
 * قطع نصية متتالية تُجمع بترتيبها، ويُتحقق من طولها قبل قراءتها (قد تصل القطع في أوقات مختلفة)
 */
object CloudChunks {
    fun split(text: String, size: Int): List<String> {
        require(size > 0)
        if (text.isEmpty()) return listOf("")
        return text.chunked(size)
    }

    /** النص كاملاً، أو null إن نقصت قطعة أو لم يطابق الطول المتوقع */
    fun join(parts: List<String?>, length: Int): String? {
        if (parts.any { it == null }) return null
        val text = parts.joinToString("") { it!! }
        return text.takeIf { it.length == length }
    }
}
