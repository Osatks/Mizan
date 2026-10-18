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

actual class Prefs actual constructor(name: String) {
    private val sp = AndroidContext.app.getSharedPreferences(name, Context.MODE_PRIVATE)
    actual fun getString(k: String): String? = sp.getString(k, null)
    actual fun putString(k: String, v: String) = sp.edit().putString(k, v).apply()
    actual fun getInt(k: String, d: Int) = sp.getInt(k, d)
    actual fun putInt(k: String, v: Int) = sp.edit().putInt(k, v).apply()
    actual fun getLong(k: String, d: Long) = sp.getLong(k, d)
    actual fun putLong(k: String, v: Long) = sp.edit().putLong(k, v).apply()
    actual fun getBool(k: String, d: Boolean) = sp.getBoolean(k, d)
    actual fun putBool(k: String, v: Boolean) = sp.edit().putBoolean(k, v).apply()
    actual fun contains(k: String) = sp.contains(k)
    actual fun remove(k: String) = sp.edit().remove(k).apply()
    actual fun isEmpty() = sp.all.isEmpty()
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
