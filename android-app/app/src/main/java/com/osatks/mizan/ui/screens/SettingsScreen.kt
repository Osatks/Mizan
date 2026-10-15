package com.osatks.mizan.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.osatks.mizan.BuildConfigInfo
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.notify.PrayerAlarms
import com.osatks.mizan.ui.ClickRow
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.ToolPage

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        AppSettings.updateNotify(ok); PrayerAlarms.scheduleNext(ctx)
    }
    ToolPage("الإعدادات") {
        item {
            SectionCard("المظهر") {
                Segmented(listOf("system" to "تلقائي", "light" to "فاتح", "dark" to "داكن"), AppSettings.theme) { AppSettings.updateTheme(it) }
            }
        }
        item {
            SectionCard("مواقيت الصلاة") {
                ClickRow("تنبيه الأذان", "إشعار عند دخول وقت كل صلاة", onClick = {}) {
                    Switch(checked = AppSettings.notify, onCheckedChange = { on ->
                        if (on && Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else { AppSettings.updateNotify(on); PrayerAlarms.scheduleNext(ctx) }
                    })
                }
                ClickRow("نظام 24 ساعة", "عرض الأوقات بصيغة 24 ساعة", onClick = { AppSettings.updateUse24h(!AppSettings.use24h) }) {
                    Switch(checked = AppSettings.use24h, onCheckedChange = { AppSettings.updateUse24h(it) })
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ClickRow("التنبيه في الوقت الدقيق", "اسمح للتطبيق بالمنبهات الدقيقة لضبط وقت الأذان", onClick = {
                        runCatching {
                            ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
                        }
                    })
                }
            }
        }
        item {
            SectionCard("عن التطبيق") {
                Text("ميزان — أدوات المسلم", style = MaterialTheme.typography.titleMedium)
                Text("الإصدار ${BuildConfigInfo.versionName(ctx)}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "تطبيق مجاني يجمع أدوات يحتاجها المسلم يومياً: الزكاة والمواريث ومواقيت الصلاة والقبلة والتاريخ الهجري والمسبحة. " +
                        "تعتمد الحسابات على المعتمد عند جمهور الفقهاء، ومواقيت الصلاة على معادلات فلكية، والتاريخ على تقويم أم القرى.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            SectionCard("الخصوصية") {
                Text(
                    "لا يجمع التطبيق أي بيانات شخصية ولا يحتاج إلى اتصال بالإنترنت. يُستخدم الموقع على جهازك فقط لحساب المواقيت والقبلة، " +
                        "وتُحفظ إعداداتك وأرقامك على الجهاز ولا تُرسل إلى أي جهة.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item { Note("النتائج إرشادية، ويُرجع في المسائل الشرعية الخاصة إلى أهل العلم والجهات الرسمية المختصة.", warn = true) }
    }
}
