@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.osatks.mizan.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.NSLocaleCurrencyCode
import platform.Foundation.NSLocaleLanguageCode
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLRequestUseProtocolCachePolicy
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.decomposedStringWithCanonicalMapping
import platform.Foundation.preferredLanguages
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal fun NSData.toByteArray(): ByteArray {
    val n = length.toInt()
    val out = ByteArray(n)
    if (n > 0) out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
}

internal fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }

/** مفاتيح كل مخزن تبدأ باسمه في NSUserDefaults */
actual class Prefs actual constructor(name: String) {
    private val d = NSUserDefaults.standardUserDefaults
    private val p = "$name."
    actual fun getString(k: String): String? = d.stringForKey(p + k)
    actual fun putString(k: String, v: String) = d.setObject(v, p + k)
    actual fun getInt(k: String, d: Int): Int = if (contains(k)) this.d.integerForKey(p + k).toInt() else d
    actual fun putInt(k: String, v: Int) = d.setInteger(v.toLong(), p + k)
    actual fun getLong(k: String, d: Long): Long = if (contains(k)) this.d.integerForKey(p + k) else d
    actual fun putLong(k: String, v: Long) = d.setInteger(v, p + k)
    actual fun getBool(k: String, d: Boolean): Boolean = if (contains(k)) this.d.boolForKey(p + k) else d
    actual fun putBool(k: String, v: Boolean) = d.setBool(v, p + k)
    actual fun contains(k: String): Boolean = d.objectForKey(p + k) != null
    actual fun remove(k: String) = d.removeObjectForKey(p + k)
    actual fun isEmpty(): Boolean = d.dictionaryRepresentation().keys.none { (it as? String)?.startsWith(p) == true }
}

/** رسالة قصيرة تُعرض فوق الواجهة (لا يوجد Toast في iOS)؛ يرسمها MainViewController */
object IosToast {
    var message by mutableStateOf<String?>(null)
}

actual object Platform {
    actual val name = "ios"

    private fun dir(kind: ULong): String {
        @Suppress("UNCHECKED_CAST")
        val base = (NSSearchPathForDirectoriesInDomains(kind, NSUserDomainMask, true) as List<String>).first()
        NSFileManager.defaultManager.createDirectoryAtPath(base, true, null, null)
        return base
    }

    actual fun asset(path: String): ByteArray? {
        val root = NSBundle.mainBundle.resourcePath ?: return null
        return NSData.dataWithContentsOfFile("$root/assets/$path")?.toByteArray()
    }
    @Suppress("UNCHECKED_CAST")
    actual fun assetList(dir: String): List<String> {
        val root = NSBundle.mainBundle.resourcePath ?: return emptyList()
        return (NSFileManager.defaultManager.contentsOfDirectoryAtPath("$root/assets/$dir", null) as? List<String>).orEmpty()
    }
    actual val filesDir: String by lazy { dir(NSApplicationSupportDirectory) }
    actual val cacheDir: String by lazy { dir(NSCachesDirectory) }
    actual fun versionName(): String = NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: "—"
    actual fun systemLanguage(): String = (NSLocale.preferredLanguages.firstOrNull() as? String) ?: "en"
    actual fun displayCountry(cc: String, lang: String): String =
        NSLocale(localeIdentifier = lang).displayNameForKey(NSLocaleCountryCode, cc) ?: ""
    actual fun displayLanguage(code: String, inLang: String): String =
        NSLocale(localeIdentifier = inLang).displayNameForKey(NSLocaleLanguageCode, code) ?: ""
    actual fun currencyName(iso: String, lang: String): String? =
        NSLocale(localeIdentifier = lang).displayNameForKey(NSLocaleCurrencyCode, iso)
    actual fun decompose(s: String): String = (s as NSString).decomposedStringWithCanonicalMapping
    actual fun toast(msg: String) { IosToast.message = msg }
    actual fun share(text: String, title: String) {
        val vc = UIActivityViewController(listOf(text), null)
        var top = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        while (top.presentedViewController != null) top = top.presentedViewController!!
        // في iPad تُعرض المشاركة نافذة منبثقة، ولا بد لها من موضع وإلا انهار التطبيق: وسط الشاشة بلا سهم
        vc.popoverPresentationController?.let { pop ->
            pop.sourceView = top.view
            top.view.bounds.useContents { pop.sourceRect = CGRectMake(origin.x + size.width / 2, origin.y + size.height / 2, 0.0, 0.0) }
            pop.permittedArrowDirections = 0u
        }
        top.presentViewController(vc, true, null)
    }
}

actual object Fs {
    private val fm get() = NSFileManager.defaultManager
    actual fun exists(path: String) = fm.fileExistsAtPath(path)
    actual fun read(path: String): ByteArray? = NSData.dataWithContentsOfFile(path)?.toByteArray()
    actual fun write(path: String, bytes: ByteArray) {
        mkdirs(path.substringBeforeLast('/'))
        if (!bytes.toNSData().writeToFile(path, true)) error("write $path")
    }
    actual fun delete(path: String) { fm.removeItemAtPath(path, null) }
    actual fun deleteDir(path: String) { fm.removeItemAtPath(path, null) }
    @Suppress("UNCHECKED_CAST")
    actual fun list(dir: String): List<String> = (fm.contentsOfDirectoryAtPath(dir, null) as? List<String>).orEmpty()
    actual fun mkdirs(dir: String) { fm.createDirectoryAtPath(dir, true, null, null) }
    // التفاسير المنزَّلة تُحفظ في iOS بلا ضغط (المساحة صغيرة، والملفات لا تنتقل بين الأنظمة)
    actual fun gzip(bytes: ByteArray): ByteArray = bytes
    actual fun gunzip(bytes: ByteArray): ByteArray = bytes
}

actual suspend fun httpGet(url: String, onProgress: (Int) -> Unit): ByteArray = suspendCancellableCoroutine { cont ->
    val u = NSURL.URLWithString(url) ?: throw IllegalArgumentException("bad url $url")
    // مهلة 30 ثانية بلا بيانات: انقطاع الشبكة يُظهر رسالة الفشل وزر الإعادة بدل انتظار بلا نهاية
    val request = NSURLRequest.requestWithURL(u, NSURLRequestUseProtocolCachePolicy, 30.0)
    val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, error ->
        val code = (response as? NSHTTPURLResponse)?.statusCode?.toInt() ?: 0
        when {
            error != null -> cont.resumeWithException(IllegalStateException(error.localizedDescription))
            code != 200 -> cont.resumeWithException(IllegalStateException("HTTP $code"))
            else -> { onProgress(100); cont.resume(data?.toByteArray() ?: ByteArray(0)) }
        }
    }
    cont.invokeOnCancellation { task.cancel() }
    task.resume()
}
