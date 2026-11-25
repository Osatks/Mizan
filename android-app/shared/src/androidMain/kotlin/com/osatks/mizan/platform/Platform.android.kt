package com.osatks.mizan.platform

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.Currency
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** سياق التطبيق؛ يُضبط مرة عند بدء أي جزء من التطبيق (النشاط، الودجت، المنبّه) */
@SuppressLint("StaticFieldLeak")
object AndroidContext {
    lateinit var app: Context
        private set
    val ready get() = ::app.isInitialized
    fun set(ctx: Context) { if (!ready) app = ctx.applicationContext ?: ctx }
}

/**
 * القراءة تتسامح مع اختلاف النوع: نسخة احتياطية من iOS لا تميّز Int من Long، فقد يُكتب المفتاح بنوع غير الذي
 * يُقرأ به. SharedPreferences ترمي ClassCastException حينها، فتُحوَّل القيمة المخزنة بدل أن ينهار التطبيق.
 */
actual class Prefs actual constructor(name: String) {
    private val sp = AndroidContext.app.getSharedPreferences(name, Context.MODE_PRIVATE)
    actual fun getString(k: String): String? =
        try { sp.getString(k, null) } catch (e: ClassCastException) { sp.all[k]?.toString() }
    actual fun putString(k: String, v: String) = sp.edit().putString(k, v).apply()
    actual fun getInt(k: String, d: Int): Int =
        try { sp.getInt(k, d) } catch (e: ClassCastException) { stored(k)?.toInt() ?: d }
    actual fun putInt(k: String, v: Int) = sp.edit().putInt(k, v).apply()
    actual fun getLong(k: String, d: Long): Long =
        try { sp.getLong(k, d) } catch (e: ClassCastException) { stored(k)?.toLong() ?: d }
    actual fun putLong(k: String, v: Long) = sp.edit().putLong(k, v).apply()
    actual fun getBool(k: String, d: Boolean): Boolean =
        try { sp.getBoolean(k, d) } catch (e: ClassCastException) {
            when (val v = sp.all[k]) {
                is Number -> v.toLong() != 0L
                is String -> v.toBooleanStrictOrNull() ?: d
                else -> d
            }
        }
    actual fun putBool(k: String, v: Boolean) = sp.edit().putBoolean(k, v).apply()
    actual fun contains(k: String) = sp.contains(k)
    actual fun remove(k: String) = sp.edit().remove(k).apply()
    actual fun isEmpty() = sp.all.isEmpty()

    /** القيمة المخزنة عدداً (أو نصاً فيه عدد، أو صح/خطأ) حين لا يطابق نوعها نوع القراءة */
    private fun stored(k: String): Number? = when (val v = sp.all[k]) {
        is Number -> v
        is String -> v.toLongOrNull() ?: v.toDoubleOrNull()
        is Boolean -> if (v) 1 else 0
        else -> null
    }

    actual fun snapshot(): Map<String, Any> {
        val out = LinkedHashMap<String, Any>()
        for ((k, v) in sp.all) {
            val n: Any = when (v) {
                is String, is Boolean, is Long -> v
                is Int -> v.toLong()
                is Float -> v.toDouble()
                is Set<*> -> v.filterIsInstance<String>().toSet()
                else -> null
            } ?: continue
            out[k] = n
        }
        return out
    }

    actual fun putValue(k: String, v: Any) {
        val e = sp.edit()
        when (v) {
            is String -> e.putString(k, v)
            is Boolean -> e.putBoolean(k, v)
            // الأعداد الصحيحة Int ما وسعها (كما يكتبها التطبيق)، و Long لما جاوزها
            is Long -> if (v >= Int.MIN_VALUE && v <= Int.MAX_VALUE) e.putInt(k, v.toInt()) else e.putLong(k, v)
            is Int -> e.putInt(k, v)
            is Double -> e.putFloat(k, v.toFloat())
            is Float -> e.putFloat(k, v)
            is Set<*> -> e.putStringSet(k, v.filterIsInstance<String>().toSet())
            else -> return
        }
        e.apply()
    }
}

actual object Platform {
    actual val name = "android"
    private val ctx get() = AndroidContext.app

    actual fun asset(path: String): ByteArray? = runCatching { ctx.assets.open(path).use { it.readBytes() } }.getOrNull()
    actual fun assetList(dir: String): List<String> = ctx.assets.list(dir).orEmpty().toList()
    actual val filesDir: String get() = ctx.filesDir.path
    actual val cacheDir: String get() = ctx.cacheDir.path
    actual fun versionName(): String =
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "—"
    actual fun systemLanguage(): String = Locale.getDefault().toLanguageTag()
    actual fun displayCountry(cc: String, lang: String): String = Locale("", cc).getDisplayCountry(Locale.forLanguageTag(lang))
    actual fun displayLanguage(code: String, inLang: String): String =
        Locale.forLanguageTag(code).getDisplayLanguage(Locale.forLanguageTag(inLang)).takeIf { it != code } ?: ""
    actual fun currencyName(iso: String, lang: String): String? =
        runCatching { Currency.getInstance(iso).getDisplayName(Locale.forLanguageTag(lang)) }.getOrNull()
    actual fun decompose(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFD)
    actual fun toast(msg: String) {
        Handler(Looper.getMainLooper()).post { Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() }
    }
    actual fun share(text: String, title: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        ctx.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** الصورة في ملف مؤقت يُشارَك برابط content:// (FileProvider في AndroidManifest للتطبيق: <الحزمة>.share) */
    actual fun shareImage(png: ByteArray, text: String, title: String) {
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "mizan-${System.currentTimeMillis()}.png").apply { writeBytes(png) }
        val uri = runCatching { androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.share", file) }.getOrNull()
            ?: return share(text, title)
        val send = Intent(Intent.ACTION_SEND).setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, text)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // معاينة الصورة في قائمة المشاركة، وإذن القراءة لمن يستقبلها
        send.clipData = android.content.ClipData.newRawUri(null, uri)
        ctx.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }
}

actual object Fs {
    actual fun exists(path: String) = File(path).exists()
    actual fun read(path: String): ByteArray? = File(path).takeIf { it.isFile }?.readBytes()
    actual fun write(path: String, bytes: ByteArray) {
        val f = File(path); f.parentFile?.mkdirs()
        val tmp = File("$path.tmp"); tmp.writeBytes(bytes)
        if (!tmp.renameTo(f)) { f.delete(); if (!tmp.renameTo(f)) error("rename $path") }
    }
    actual fun delete(path: String) { File(path).delete() }
    actual fun deleteDir(path: String) { File(path).deleteRecursively() }
    actual fun list(dir: String): List<String> = File(dir).list()?.toList().orEmpty()
    actual fun mkdirs(dir: String) { File(dir).mkdirs() }
    actual fun gzip(bytes: ByteArray): ByteArray = ByteArrayOutputStream().also { o -> GZIPOutputStream(o).use { it.write(bytes) } }.toByteArray()
    actual fun gunzip(bytes: ByteArray): ByteArray = GZIPInputStream(bytes.inputStream()).use { it.readBytes() }
}

actual suspend fun httpGet(url: String, onProgress: (Int) -> Unit): ByteArray = withContext(Dispatchers.IO) {
    val conn = URL(url).openConnection().apply { connectTimeout = 15_000; readTimeout = 60_000 }
    try {
        if (conn is HttpURLConnection && conn.responseCode != 200) error("HTTP ${conn.responseCode}")
        val total = conn.contentLengthLong
        val out = ByteArrayOutputStream(if (total > 0) total.toInt() else 64 * 1024)
        conn.getInputStream().use { input ->
            val buf = ByteArray(32 * 1024)
            var done = 0L
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                out.write(buf, 0, n); done += n
                if (total > 0) onProgress((done * 100 / total).toInt().coerceIn(0, 100))
            }
        }
        out.toByteArray()
    } finally {
        (conn as? HttpURLConnection)?.disconnect()
    }
}
