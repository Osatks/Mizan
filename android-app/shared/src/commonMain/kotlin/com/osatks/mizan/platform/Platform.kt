package com.osatks.mizan.platform

/**
 * ما يختلف بين أندرويد و iOS، في واجهة واحدة يستعملها الكود المشترك.
 * التنفيذ في androidMain (Context، SharedPreferences…) و iosMain (Foundation، UIKit…).
 */

/** تخزين مفاتيح وقيم دائم (SharedPreferences / NSUserDefaults) */
expect class Prefs(name: String) {
    fun getString(k: String): String?
    fun putString(k: String, v: String)
    fun getInt(k: String, d: Int): Int
    fun putInt(k: String, v: Int)
    fun getLong(k: String, d: Long): Long
    fun putLong(k: String, v: Long)
    fun getBool(k: String, d: Boolean): Boolean
    fun putBool(k: String, v: Boolean)
    fun contains(k: String): Boolean
    fun remove(k: String)
    fun isEmpty(): Boolean

    /**
     * كل المفاتيح بقيمها، بأنواع موحّدة بين النظامين (للنسخ الاحتياطي): String أو Long أو Double أو Boolean
     * أو Set<String>. ما سواها (بيانات ثنائية مثلاً) لا يُعاد.
     */
    fun snapshot(): Map<String, Any>

    /** يكتب قيمة بنوع من أنواع [snapshot] (الاستعادة من نسخة احتياطية) */
    fun putValue(k: String, v: Any)
}

expect object Platform {
    /** "android" أو "ios" */
    val name: String

    /** ملف من مجلد assets المشترك (نصوص المصحف والتفاسير والأذكار والخطوط) */
    fun asset(path: String): ByteArray?

    /** أسماء الملفات في مجلد من assets */
    fun assetList(dir: String): List<String>

    /** مجلد ملفات التطبيق الخاصة (التفاسير المنزَّلة، حزم اللغات) */
    val filesDir: String

    /** مجلد الذاكرة المؤقتة (التلاوات) */
    val cacheDir: String

    fun versionName(): String

    /** لغة الجهاز (رمز BCP-47 مثل ar أو en-US) */
    fun systemLanguage(): String

    /** اسم دولة من رمزها بلغة ما (فارغ إن لم يُعرف) */
    fun displayCountry(cc: String, lang: String): String

    /** اسم لغة بلغة أخرى (فارغ إن لم يُعرف) */
    fun displayLanguage(code: String, inLang: String): String

    /** اسم عملة بلغة ما */
    fun currencyName(iso: String, lang: String): String?

    /** التفكيك القانوني NFD (لإزالة علامات الحروف اللاتينية في البحث) */
    fun decompose(s: String): String

    fun toast(msg: String)

    fun share(text: String, title: String)

    /** يشارك صورة PNG ومعها نص بقائمة المشاركة في النظام (حفظ في الصور، والتطبيقات) */
    fun shareImage(png: ByteArray, text: String, title: String)
}

/** ملفات التطبيق؛ المسارات كاملة (تبدأ من [Platform.filesDir]) */
expect object Fs {
    fun exists(path: String): Boolean
    fun read(path: String): ByteArray?
    /** يكتب في ملف مؤقت ثم يعيد تسميته، فلا يبقى ملف ناقص */
    fun write(path: String, bytes: ByteArray)
    fun delete(path: String)
    fun deleteDir(path: String)
    /** أسماء الملفات في مجلد (لا المسارات) */
    fun list(dir: String): List<String>
    fun mkdirs(dir: String)
    fun gzip(bytes: ByteArray): ByteArray
    fun gunzip(bytes: ByteArray): ByteArray
}

/** طلب GET؛ يرمي استثناءً إن لم يكن الرد 200. onProgress بالنسبة المئوية إن عُرف الحجم */
expect suspend fun httpGet(url: String, onProgress: (Int) -> Unit = {}): ByteArray

fun path(vararg parts: String) = parts.joinToString("/").replace("//", "/")

/** تعديلات بأسلوب SharedPreferences (edit().putX().apply())؛ كل تعديل يُحفظ فوراً */
class PrefsEditor internal constructor(private val p: Prefs) {
    fun putString(k: String, v: String?) = also { if (v == null) p.remove(k) else p.putString(k, v); PrefsChanges.changed(k) }
    fun putBoolean(k: String, v: Boolean) = also { p.putBool(k, v); PrefsChanges.changed(k) }
    fun putInt(k: String, v: Int) = also { p.putInt(k, v); PrefsChanges.changed(k) }
    fun putLong(k: String, v: Long) = also { p.putLong(k, v); PrefsChanges.changed(k) }
    fun remove(k: String) = also { p.remove(k); PrefsChanges.changed(k) }
    fun apply() {}
}

/**
 * يُبلَّغ بكل مفتاح يتغير عبر [PrefsEditor] (كل كتابات الإعدادات تمر به): مزامنة iCloud في iOS تتبع التغييرات
 * فترسل البيانات بعد هدوء قصير. لا مستمع في أندرويد.
 */
object PrefsChanges {
    var listener: ((String) -> Unit)? = null

    internal fun changed(k: String) { listener?.invoke(k) }
}

fun Prefs.edit() = PrefsEditor(this)
