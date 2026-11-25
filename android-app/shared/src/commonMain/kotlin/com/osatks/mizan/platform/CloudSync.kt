package com.osatks.mizan.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.osatks.mizan.core.BackupDoc

/**
 * المزامنة مع iCloud (iPhone و iPad): النسخة الاحتياطية نفسها ([com.osatks.mizan.core.Backup]) في مخزن iCloud
 * للمفاتيح والقيم، فتتطابق بيانات الأجهزة التي تستعمل حساب Apple نفسه (الأحدث تغييراً يغلب).
 * الإعداد والواجهة مشتركان والتنفيذ في iOS (IosCloudSync)؛ أندرويد لا يضبط [supported] فيبقى المفتاح مخفياً فيه
 * (وفيه النسخ الاحتياطي التلقائي من النظام إلى حساب Google).
 */
object CloudSync {
    /** يضبطه iOS عند الإقلاع */
    var supported by mutableStateOf(false)

    /** المزامنة مفعّلة في هذا الجهاز (مطفأة افتراضياً) */
    var enabled by mutableStateOf(false)

    /** آخر مزامنة ناجحة بالميلي ثانية منذ 1970، و 0 إن لم تتم بعد */
    var lastSync by mutableStateOf(0L)

    /**
     * عند التفعيل وفي iCloud بيانات من جهاز آخر: يُسأل المستخدم أيستعيدها أم يُبقي بيانات هذا الجهاز.
     * يعرض السؤالَ قسمُ النسخ الاحتياطي في الإعدادات، ويجيب بـ [resolve]
     */
    var offer by mutableStateOf<BackupDoc?>(null)

    /** iOS: تفعيل المزامنة أو إيقافها */
    var request: ((Boolean) -> Unit)? = null

    /** iOS: جواب [offer]: true تُستعاد بيانات iCloud، false تُحفظ بيانات هذا الجهاز فيها، null تُلغى المزامنة */
    var answer: ((Boolean?) -> Unit)? = null

    fun update(on: Boolean) { request?.invoke(on) }

    fun resolve(restore: Boolean?) {
        offer = null
        answer?.invoke(restore)
    }
}

/**
 * يحفظ ملف النسخة الاحتياطية بأداة النظام: «إنشاء مستند» في أندرويد (الملفات، Google Drive…)،
 * وقائمة المشاركة في iOS (حفظ في «الملفات»، AirDrop…). [onDone]: true حُفظ، false تعذّر الحفظ، null أُلغي.
 * يعيد دالة تأخذ اسم الملف ونصه.
 */
@Composable
expect fun rememberBackupSaver(onDone: (Boolean?) -> Unit): (fileName: String, text: String) -> Unit

/**
 * يختار ملف نسخة احتياطية بأداة النظام (ACTION_OPEN_DOCUMENT في أندرويد، و UIDocumentPickerViewController في iOS).
 * [onPicked] بمحتوى الملف، أو null إن تعذّرت قراءته؛ ولا يُستدعى إن ألغى المستخدم الاختيار.
 */
@Composable
expect fun rememberBackupPicker(onPicked: (ByteArray?) -> Unit): () -> Unit
