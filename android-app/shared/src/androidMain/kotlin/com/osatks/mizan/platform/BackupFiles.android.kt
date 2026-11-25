package com.osatks.mizan.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.osatks.mizan.core.Backup
import java.io.ByteArrayOutputStream
import java.io.InputStream

/** «إنشاء مستند» من النظام (ACTION_CREATE_DOCUMENT): يختار المستخدم المكان (الهاتف، Google Drive…) ثم يُكتب الملف فيه */
@Composable
actual fun rememberBackupSaver(onDone: (Boolean?) -> Unit): (fileName: String, text: String) -> Unit {
    val ctx = LocalContext.current
    val done by rememberUpdatedState(onDone)
    // النص ينتظر هنا حتى يعود المستخدم من نافذة الحفظ
    val pending = remember { arrayOfNulls<String>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val text = pending[0]
        pending[0] = null
        if (uri == null || text == null) {
            done(null)
        } else {
            val ok = runCatching {
                val out = ctx.contentResolver.openOutputStream(uri) ?: error("no output stream")
                out.use { it.write(text.encodeToByteArray()) }
            }.isSuccess
            done(ok)
        }
    }
    return remember(launcher) {
        { fileName: String, text: String ->
            pending[0] = text
            // لا تطبيق ملفات في الجهاز (نادر): رسالة تعذّر الحفظ بدل الانهيار
            if (runCatching { launcher.launch(fileName) }.isFailure) {
                pending[0] = null
                done(false)
            }
        }
    }
}

/** «فتح مستند» من النظام (ACTION_OPEN_DOCUMENT): ملفات JSON والنصوص (بعض التطبيقات لا تعرف نوع .json) */
@Composable
actual fun rememberBackupPicker(onPicked: (ByteArray?) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val picked by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            picked(runCatching { ctx.contentResolver.openInputStream(uri)?.use { readLimited(it) } }.getOrNull())
        }
    }
    return remember(launcher) {
        {
            if (runCatching { launcher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }.isFailure) picked(null)
        }
    }
}

/** يقرأ الملف إن لم يتجاوز [Backup.MAX_BYTES]، وإلا null (ليس نسخة احتياطية) */
private fun readLimited(input: InputStream): ByteArray? {
    val out = ByteArrayOutputStream()
    val buf = ByteArray(16 * 1024)
    var total = 0
    while (true) {
        val n = input.read(buf)
        if (n < 0) break
        total += n
        if (total > Backup.MAX_BYTES) return null
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}
