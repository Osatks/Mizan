@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.osatks.mizan.platform

import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Backup
import com.osatks.mizan.core.BackupDoc
import com.osatks.mizan.core.CloudChunks
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.widget.IosWidgets
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSUbiquitousKeyValueStore
import platform.Foundation.NSUbiquitousKeyValueStoreDidChangeExternallyNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.darwin.NSObjectProtocol
import kotlin.random.Random

/**
 * مزامنة iCloud في iOS: النسخة الاحتياطية نفسها ([Backup.export]) في مخزن iCloud للمفاتيح والقيم
 * (NSUbiquitousKeyValueStore، يحتاج الاستحقاق com.apple.developer.ubiquity-kvstore-identifier في Mizan.entitlements).
 *
 * - تُرسل بعد هدوء قصير من كل تغيير في الإعدادات ([PrefsChanges])، وعند الخروج من التطبيق.
 * - تُقرأ عند الإقلاع وحين يُبلغ النظام بتغيير من جهاز آخر (didChangeExternallyNotification).
 * - الأحدث يغلب: لكل نسخة وقت آخر تغيير فيها، فتُستعاد نسخة iCloud إن كانت أحدث من بيانات هذا الجهاز وتُرسل إن كانت أقدم.
 * - عند التفعيل وفي iCloud بيانات من جهاز آخر تختلف عن بيانات هذا الجهاز: يُسأل المستخدم ([CloudSync.offer]).
 *
 * المخزن محدود (1 ميغابايت و 1024 مفتاحاً): النسخة قطع نصية (mizan.backup.0، 1…) ووصفها في mizan.backup.meta
 * (الوقت وعدد القطع والطول ومعرّف الجهاز). حال المزامنة في هذا الجهاز مفاتيح icloud_* لا تُنسخ احتياطياً.
 */
object IosCloudSync {
    private const val ON = "icloud_sync"
    /** وقت بيانات هذا الجهاز: آخر تغيير محلي، أو وقت نسخة iCloud التي استُعيدت */
    private const val STAMP = "icloud_stamp"
    private const val LAST = "icloud_last"
    private const val DEVICE = "icloud_device"

    private const val META = "mizan.backup.meta"
    private const val CHUNK = "mizan.backup."
    /** حروف كل قطعة (أقل من 300 كيلوبايت بأي ترميز) */
    private const val CHUNK_CHARS = 100_000
    /** أكبر نسخة تُرسل، دون حصة التطبيق (1 ميغابايت) */
    private const val MAX_BYTES = 900_000
    /** هدوء بعد آخر تغيير قبل الإرسال */
    private const val DEBOUNCE_MS = 5_000L
    /** عند التفعيل وiCloud فارغ: مهلة لوصول بيانات الأجهزة الأخرى (المزامنة الأولى بطيئة) قبل الإرسال */
    private const val FIRST_WAIT_MS = 8_000L

    private class Meta(val stamp: Long, val chunks: Int, val length: Int, val device: String)

    private val kv: NSUbiquitousKeyValueStore get() = NSUbiquitousKeyValueStore.defaultStore
    private val scope = MainScope()
    private var pushJob: Job? = null
    private var started = false
    private var dirty = false
    private val observers = ArrayList<NSObjectProtocol>()
    /** وقت نسخة iCloud المعروضة في [CloudSync.offer] */
    private var offerStamp = 0L

    /** عند الإقلاع (MainViewController) */
    fun start() {
        if (started) return
        started = true
        AppSettings.init()
        CloudSync.supported = true
        CloudSync.enabled = AppSettings.getBool(ON, false)
        CloudSync.lastSync = stamp(LAST)
        CloudSync.request = { on -> if (on) enable() else disable() }
        CloudSync.answer = { restore -> answer(restore) }
        PrefsChanges.listener = { key -> changed(key) }
        Backup.afterRestore = { fromCloud ->
            // الاستعادة من ملف تغيير محلي يُرسل إلى iCloud؛ وفي الحالين تُحدَّث أدوات الشاشة الرئيسية
            if (!fromCloud) changed(null)
            IosWidgets.refresh()
        }
        val nc = NSNotificationCenter.defaultCenter
        observers += nc.addObserverForName(NSUbiquitousKeyValueStoreDidChangeExternallyNotification, kv, NSOperationQueue.mainQueue) { _ ->
            if (CloudSync.enabled) pull()
        }
        // عند الخروج من التطبيق: يُرسل ما لم يُرسل بعد (المراقب بلا طابور: يُنفَّذ فوراً قبل أن يُعلَّق التطبيق)
        observers += nc.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, null) { _ -> flush() }
        if (CloudSync.enabled) scope.launch {
            // بعد أن تستقر الواجهة
            delay(1_500)
            kv.synchronize()
            pull()
        }
    }

    // ---------- التفعيل والإيقاف ----------

    private fun enable() {
        // بلا استحقاق iCloud (تثبيت بحساب مجاني مثلاً) لا يعمل المخزن
        if (!kv.synchronize()) {
            CloudSync.enabled = false
            Platform.toast(Lang.current.strings.icloudUnavailable)
            return
        }
        AppSettings.putBool(ON, true)
        CloudSync.enabled = true
        val m = meta()
        if (m == null) {
            // iCloud فارغ، أو لم تصل بياناته بعد: مهلة قصيرة، فإن وصلت بيانات جهاز آخر سُئل المستخدم
            firstWait = true
            pushJob?.cancel()
            pushJob = scope.launch {
                delay(FIRST_WAIT_MS)
                firstWait = false
                if (CloudSync.offer == null) { touch(); push() }
            }
            return
        }
        firstCheck(m)
    }

    /** مهلة المزامنة الأولى بعد التفعيل جارية */
    private var firstWait = false

    /** أول فحص بعد التفعيل: بيانات جهاز آخر تختلف عن بيانات هذا الجهاز يُسأل عنها، وإلا تُرسل بيانات هذا الجهاز */
    private fun firstCheck(m: Meta) {
        if (m.device != deviceId()) {
            val doc = remote(m)
            if (doc != null) {
                val mine = runCatching { Backup.decode(Backup.export()).data }.getOrNull()
                if (mine == doc.data) {
                    // البيانات نفسها: يُعتمد وقت iCloud
                    setStamp(STAMP, m.stamp)
                    synced()
                    return
                }
                offerStamp = m.stamp
                CloudSync.offer = doc
                return
            }
        }
        touch()
        push()
    }

    private fun answer(restore: Boolean?) {
        val doc = pendingOffer()
        when (restore) {
            true -> if (doc != null) {
                Backup.restore(doc, fromCloud = true)
                setStamp(STAMP, offerStamp)
                synced()
                Platform.toast(Lang.current.strings.backupRestored)
            }
            false -> { touch(); push() }
            null -> disable()
        }
    }

    /** نسخة iCloud التي عُرضت للسؤال، مقروءة من جديد (قد تكون تغيّرت أثناء السؤال) */
    private fun pendingOffer(): BackupDoc? = meta()?.let { m -> remote(m)?.also { offerStamp = m.stamp } }

    private fun disable() {
        pushJob?.cancel()
        pushJob = null
        firstWait = false
        AppSettings.putBool(ON, false)
        CloudSync.enabled = false
        CloudSync.offer = null
    }

    // ---------- التغييرات المحلية ----------

    /** مفتاح تغيّر في الإعدادات ([key] null: استُعيدت نسخة من ملف) */
    private fun changed(key: String?) {
        if (!CloudSync.enabled || Backup.restoring || CloudSync.offer != null || firstWait) return
        if (key != null && !Backup.included(key)) return
        touch()
        pushJob?.cancel()
        pushJob = scope.launch {
            delay(DEBOUNCE_MS)
            push()
        }
    }

    private fun touch() {
        setStamp(STAMP, Backup.nowMillis())
        dirty = true
    }

    private fun flush() {
        if (CloudSync.enabled && dirty && CloudSync.offer == null && !firstWait) push()
    }

    // ---------- الإرسال والاستقبال ----------

    private fun push() {
        pushJob?.cancel()
        pushJob = null
        if (!CloudSync.enabled) return
        val stamp = stamp(STAMP).takeIf { it > 0 } ?: Backup.nowMillis().also { setStamp(STAMP, it) }
        val json = runCatching { Backup.export(stamp) }.getOrNull() ?: return
        // أكبر من حصة iCloud (لا يحدث بالبيانات الحالية): يبقى الملف وحده للنسخ
        if (json.encodeToByteArray().size > MAX_BYTES) return
        val parts = CloudChunks.split(json, CHUNK_CHARS)
        val old = meta()?.chunks ?: 0
        // القطع أولاً ثم وصفها: قارئ يرى الوصف الجديد قبل قطعه يرفضها بالطول ويقرأ في الإشعار التالي
        parts.forEachIndexed { i, s -> kv.setString(s, "$CHUNK$i") }
        for (i in parts.size until old) kv.removeObjectForKey("$CHUNK$i")
        kv.setString(
            Json.write(
                linkedMapOf<String, Any>(
                    "format" to Backup.FORMAT.toLong(), "stamp" to stamp, "chunks" to parts.size.toLong(),
                    "length" to json.length.toLong(), "device" to deviceId(),
                )
            ),
            META,
        )
        kv.synchronize()
        dirty = false
        synced()
    }

    /** نسخة iCloud: تُستعاد إن كانت أحدث من بيانات هذا الجهاز، وتُرسل بيانات الجهاز إن كانت أحدث */
    private fun pull() {
        if (CloudSync.offer != null) return
        val m = meta()
        if (firstWait) {
            // وصلت بيانات في مهلة التفعيل الأولى: السؤال كما في أول فحص
            if (m != null) {
                firstWait = false
                pushJob?.cancel()
                firstCheck(m)
            }
            return
        }
        val local = stamp(STAMP)
        when {
            m == null -> { if (local == 0L) touch(); push() }
            m.stamp > local -> {
                val doc = remote(m) ?: return
                Backup.restore(doc, fromCloud = true)
                setStamp(STAMP, m.stamp)
                dirty = false
                synced()
            }
            m.stamp < local -> push()
            else -> synced()
        }
    }

    private fun meta(): Meta? {
        val text = kv.stringForKey(META) ?: return null
        val o = runCatching { Json.obj(text) }.getOrNull() ?: return null
        val stamp = (o["stamp"] as? Long) ?: return null
        val chunks = (o["chunks"] as? Long)?.toInt()?.takeIf { it in 1..1000 } ?: return null
        val length = (o["length"] as? Long)?.toInt() ?: return null
        return Meta(stamp, chunks, length, o["device"] as? String ?: "")
    }

    /** النسخة من قطعها، أو null إن لم تكتمل بعد أو لم تصلح (صيغة أحدث مثلاً) */
    private fun remote(m: Meta): BackupDoc? {
        val text = CloudChunks.join((0 until m.chunks).map { kv.stringForKey("$CHUNK$it") }, m.length) ?: return null
        return runCatching { Backup.decode(text) }.getOrNull()
    }

    private fun synced() {
        val t = Backup.nowMillis()
        setStamp(LAST, t)
        CloudSync.lastSync = t
    }

    // ---------- حال هذا الجهاز ----------

    private fun stamp(key: String): Long = AppSettings.getString(key).toLongOrNull() ?: 0L

    private fun setStamp(key: String, v: Long) = AppSettings.putString(key, v.toString())

    /** معرّف عشوائي لهذا التثبيت: يميّز بيانات هذا الجهاز في iCloud من بيانات غيره */
    private fun deviceId(): String = AppSettings.getString(DEVICE).ifEmpty {
        (1..16).joinToString("") { Random.nextInt(16).toString(16) }.also { AppSettings.putString(DEVICE, it) }
    }
}
