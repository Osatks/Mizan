@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.osatks.mizan.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.osatks.mizan.core.Backup
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.darwin.NSObject

/**
 * الملف في مجلد مؤقت ثم قائمة المشاركة في النظام برابطه: «حفظ في الملفات» و AirDrop والبريد…
 * [onDone] بـ true إن أتمّ المستخدم أمراً من القائمة، ولا يُستدعى إن أغلقها
 */
@Composable
actual fun rememberBackupSaver(onDone: (Boolean?) -> Unit): (fileName: String, text: String) -> Unit {
    val done by rememberUpdatedState(onDone)
    return remember {
        { fileName: String, text: String ->
            val path = NSTemporaryDirectory().trimEnd('/') + "/" + fileName
            if (runCatching { Fs.write(path, text.encodeToByteArray()) }.isFailure) {
                done(false)
            } else {
                val vc = UIActivityViewController(listOf(NSURL.fileURLWithPath(path)), null)
                vc.completionWithItemsHandler = { _, completed, _, _ -> if (completed) done(true) }
                presentOnTop(vc)
            }
        }
    }
}

/** منتقي المستندات من النظام لملفات JSON (نسخة من الملف: لا حاجة إلى إذن الوصول إليه بعد ذلك) */
@Composable
actual fun rememberBackupPicker(onPicked: (ByteArray?) -> Unit): () -> Unit {
    val picked by rememberUpdatedState(onPicked)
    return remember {
        {
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeJSON, UTTypePlainText), asCopy = true)
            picker.allowsMultipleSelection = false
            val delegate = BackupPickerDelegate(
                done = { bytes -> backupPicker = null; picked(bytes) },
                cancelled = { backupPicker = null },
            )
            // مفوَّض المنتقي مرجع ضعيف: يبقى حياً هنا حتى يُختار ملف أو يُلغى
            backupPicker = delegate
            picker.delegate = delegate
            presentOnTop(picker)
        }
    }
}

private var backupPicker: BackupPickerDelegate? = null

// صنف لا object: Kotlin/Native لا يقبل object يرث صنفاً من Objective-C
private class BackupPickerDelegate(
    private val done: (ByteArray?) -> Unit,
    private val cancelled: () -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        if (url == null) return done(null)
        val scoped = url.startAccessingSecurityScopedResource()
        val data = NSData.dataWithContentsOfURL(url)
        if (scoped) url.stopAccessingSecurityScopedResource()
        done(data?.takeIf { it.length.toLong() <= Backup.MAX_BYTES }?.toByteArray())
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) = cancelled()
}

/** يعرض متحكماً فوق أعلى ما يظهر؛ في iPad نافذة منبثقة وسط الشاشة بلا سهم (وإلا انهارت قائمة المشاركة) */
private fun presentOnTop(vc: UIViewController) {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    while (top.presentedViewController != null) top = top.presentedViewController!!
    vc.popoverPresentationController?.let { pop ->
        pop.sourceView = top.view
        top.view.bounds.useContents { pop.sourceRect = CGRectMake(origin.x + size.width / 2, origin.y + size.height / 2, 0.0, 0.0) }
        pop.permittedArrowDirections = 0u
    }
    top.presentViewController(vc, true, null)
}
