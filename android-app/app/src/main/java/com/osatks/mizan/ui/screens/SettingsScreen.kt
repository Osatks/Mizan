package com.osatks.mizan.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.osatks.mizan.BuildConfigInfo
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.notify.PrayerAlarms
import com.osatks.mizan.ui.ClickRow
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.ToolPage

@Composable
fun SettingsScreen() {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val ctx = LocalContext.current
    var showLang by remember { mutableStateOf(false) }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        AppSettings.updateNotify(ok); PrayerAlarms.scheduleNext(ctx)
    }
    val system = Lang.fromLocale(LocalConfiguration.current.locales[0])

    ToolPage(s.settingsTitle) {
        item {
            SectionCard(s.language) {
                ClickRow(
                    lang.native,
                    if (AppSettings.lang == Lang.SYSTEM) "${s.deviceLanguage} · ${system.native}" else null,
                    onClick = { showLang = true },
                    leading = Icons.Filled.Translate
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        item {
            SectionCard(s.appearance) {
                Segmented(listOf("system" to s.themeAuto, "light" to s.themeLight, "dark" to s.themeDark), AppSettings.theme) { AppSettings.updateTheme(it) }
            }
        }
        item {
            SectionCard(s.prayerTitle) {
                ClickRow(s.adhanAlert, s.adhanAlertDesc, onClick = {}, leading = Icons.Filled.NotificationsActive) {
                    Switch(checked = AppSettings.notify, onCheckedChange = { on ->
                        if (on && Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else { AppSettings.updateNotify(on); PrayerAlarms.scheduleNext(ctx) }
                    })
                }
                ClickRow(s.use24h, s.use24hDesc, onClick = { AppSettings.updateUse24h(!AppSettings.use24h) }, leading = Icons.Filled.AccessTime) {
                    Switch(checked = AppSettings.use24h, onCheckedChange = { AppSettings.updateUse24h(it) })
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ClickRow(s.exactAlarm, s.exactAlarmDesc, leading = Icons.Filled.Alarm, onClick = {
                        runCatching {
                            ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
                        }
                    })
                }
            }
        }
        item {
            SectionCard(s.about) {
                Text(s.aboutName, style = MaterialTheme.typography.titleMedium)
                Text(s.version(BuildConfigInfo.versionName(ctx)), style = MaterialTheme.typography.bodySmall)
                Text(s.aboutText, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            SectionCard(s.privacy) {
                Text(s.privacyText, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item { Note(s.disclaimer, warn = true) }
    }

    if (showLang) LanguageSheet { showLang = false }
}
