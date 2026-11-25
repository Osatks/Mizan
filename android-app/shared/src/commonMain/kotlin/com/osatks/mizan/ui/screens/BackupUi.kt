package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.osatks.mizan.core.Backup
import com.osatks.mizan.core.BackupDoc
import com.osatks.mizan.core.BackupException
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.i18n.greg
import com.osatks.mizan.i18n.time
import com.osatks.mizan.platform.CloudSync
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.rememberBackupPicker
import com.osatks.mizan.platform.rememberBackupSaver
import com.osatks.mizan.ui.ActionRole
import com.osatks.mizan.ui.DialogAction
import com.osatks.mizan.ui.PlatformDialog
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * قسم «النسخ الاحتياطي والمزامنة» في الإعدادات: الحفظ في ملف، والاستعادة منه بعد التأكيد، وسؤال iCloud.
 * المنطق هنا مشترك، والصفوف في SettingsScreen (أندرويد) و NativeSettings.swift (iOS، عبر NativeSettings.ios.kt).
 * يُنشأ في الشاشة نفسها لا في عنصر قائمة، فلا تضيع النافذة ولا منتقي الملفات إن مُرِّرت القائمة.
 */
class BackupUi internal constructor(
    pendingState: MutableState<BackupDoc?>,
    private val saver: (fileName: String, text: String) -> Unit,
    private val picker: () -> Unit,
) {
    /** نسخة اختيرت وتنتظر تأكيد الاستعادة */
    var pending: BackupDoc? by pendingState

    /** «حفظ نسخة احتياطية»: الملف ثم أداة الحفظ أو المشاركة في النظام */
    fun save() {
        val text = runCatching { Backup.export() }.getOrNull()
        if (text == null) Platform.toast(Lang.current.strings.backupSaveFailed) else saver(Backup.fileName(), text)
    }

    /** «الاستعادة من نسخة احتياطية»: منتقي الملفات، ثم التحقق والتأكيد */
    fun restore() = picker()
}

@Composable
fun rememberBackupUi(): BackupUi {
    val pending = remember { mutableStateOf<BackupDoc?>(null) }
    val saver = rememberBackupSaver { ok ->
        val s = Lang.current.strings
        when (ok) {
            true -> Platform.toast(s.backupSaved)
            false -> Platform.toast(s.backupSaveFailed)
            null -> {}
        }
    }
    val picker = rememberBackupPicker { bytes -> pending.value = readBackup(bytes) }
    return remember(saver, picker) { BackupUi(pending, saver, picker) }
}

/** الملف المختار نسخةً صالحة، أو رسالة تبيّن السبب و null */
private fun readBackup(bytes: ByteArray?): BackupDoc? {
    val s = Lang.current.strings
    if (bytes == null) {
        Platform.toast(s.backupReadFailed)
        return null
    }
    return try {
        Backup.decode(bytes.decodeToString())
    } catch (e: BackupException) {
        Platform.toast(if (e.reason == BackupException.Reason.TOO_NEW) s.backupTooNew else s.backupInvalid)
        null
    } catch (e: Exception) {
        Platform.toast(s.backupInvalid)
        null
    }
}

/** تاريخ النسخة ووقتها بتوقيت الجهاز ولغة الواجهة: «25 نوفمبر 2026 · 10:30 ص» */
fun backupDate(ms: Long, s: Strings): String {
    if (ms <= 0L) return "—"
    val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${s.greg(t.date)} · ${s.time(t.hour + t.minute / 60.0)}"
}

/** وصف صف iCloud: آخر مزامنة إن تمت، وإلا الوصف العام */
fun icloudSubtitle(s: Strings): String =
    if (CloudSync.enabled && CloudSync.lastSync > 0L) s.icloudLastSync(backupDate(CloudSync.lastSync, s)) else s.icloudSyncSub

/** ملاحظة القسم: النسخ التلقائي في أندرويد، والخصوصية في النظامين */
fun backupNote(s: Strings): String =
    if (Platform.name == "android") "${s.backupAutoNote}\n${s.backupPrivacyNote}" else s.backupPrivacyNote

/** تأكيد الاستعادة (تحلّ محل البيانات الحالية) وسؤال iCloud عند التفعيل؛ نوافذ النظام فوق الإعدادات */
@Composable
fun BackupDialogs(ui: BackupUi) {
    val s = LocalStrings.current
    ui.pending?.let { doc ->
        PlatformDialog(
            onDismissRequest = { ui.pending = null },
            title = s.backupConfirmTitle,
            message = s.backupConfirmBody(backupDate(doc.createdAt, s)),
            actions = listOf(
                DialogAction(s.cancel, ActionRole.Cancel) { ui.pending = null },
                DialogAction(s.backupRestoreAction, ActionRole.Destructive) {
                    ui.pending = null
                    val ok = runCatching { Backup.restore(doc) }.isSuccess
                    val now = Lang.current.strings
                    Platform.toast(if (ok) now.backupRestored else now.backupRestoreFailed)
                },
            ),
        )
    }
    CloudSync.offer?.let { doc ->
        PlatformDialog(
            onDismissRequest = { CloudSync.resolve(null) },
            title = s.icloudOfferTitle,
            message = s.icloudOfferBody(backupDate(doc.createdAt, s)),
            actions = listOf(
                DialogAction(s.backupRestoreAction, ActionRole.Preferred) { CloudSync.resolve(true) },
                DialogAction(s.icloudKeepLocal) { CloudSync.resolve(false) },
                DialogAction(s.cancel, ActionRole.Cancel) { CloudSync.resolve(null) },
            ),
        )
    }
}
