package com.osatks.mizan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Place
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.platform.LocationResult
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.platform.rememberLocationRequest
import com.osatks.mizan.platform.rememberNotifyToggle
import com.osatks.mizan.platform.AppStore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.osatks.mizan.ui.theme.Gold

/**
 * زرّا اختيار الموقع: الموقع الحالي (GPS) أو مدينة من القائمة.
 * [onHero] للخلفية الخضراء (جولة أول تشغيل)، وإلا بألوان السطح العادية.
 */
@Composable
fun LocationButtons(onHero: Boolean, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    var locating by remember { mutableStateOf(false) }
    var pickCity by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    val locate = rememberLocationRequest { r ->
        locating = false
        when (r) {
            is LocationResult.Found -> { AppSettings.updatePlace(r.place); PrayerAlerts.reschedule(); Platform.toast(s.locationFound) }
            LocationResult.Failed -> Platform.toast(s.locationFailed)
            LocationResult.Denied -> denied = true
        }
    }
    if (denied) LocationDeniedDialog(onPickCity = { denied = false; pickCity = true }, onDismiss = { denied = false })
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = { locating = true; locate() }, enabled = !locating, modifier = Modifier.fillMaxWidth(),
            colors = if (onHero) ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF1E1A14), disabledContainerColor = Gold.copy(alpha = .6f), disabledContentColor = Color(0xFF1E1A14))
            else ButtonDefaults.buttonColors(),
        ) {
            if (locating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = if (onHero) Color(0xFF1E1A14) else MaterialTheme.colorScheme.onPrimary)
            else Icon(Icons.Filled.MyLocation, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (locating) s.locating else s.useMyLocation, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = { pickCity = true }, modifier = Modifier.fillMaxWidth(),
            border = if (onHero) BorderStroke(1.dp, Color.White.copy(alpha = .45f)) else ButtonDefaults.outlinedButtonBorder(true),
            colors = if (onHero) ButtonDefaults.outlinedButtonColors(contentColor = Color.White) else ButtonDefaults.outlinedButtonColors(),
        ) {
            Icon(Icons.Filled.LocationCity, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.pickCity)
        }
    }
    if (pickCity) CityPickerSheet(AppSettings.place.key.takeIf { AppSettings.placeSet && !AppSettings.place.gps }, onDismiss = { pickCity = false }) { c ->
        AppSettings.updatePlace(Place.of(c))
        PrayerAlerts.reschedule()
    }
}

/** بطاقة تظهر ما دام المستخدم لم يختر موقعه، فلا تُعرض مواقيت مكة على أنها مواقيته */
@Composable
fun LocationNeededCard(modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    AnimatedVisibility(!AppSettings.placeSet, modifier) {
        Surface(
            shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Place, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(s.locationNeeded, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.size(12.dp))
                LocationButtons(onHero = false)
            }
        }
    }
}

/**
 * رفض المستخدم إذن الموقع (الآن أو من قبل، فلا يسأل النظام ثانية): التطبيق يبقى صالحاً كله باختيار المدينة،
 * ومن يريد الموقع التلقائي يُفتح له تطبيق الإعدادات
 */
@Composable
fun LocationDeniedDialog(onPickCity: () -> Unit, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    com.osatks.mizan.ui.PlatformDialog(
        onDismissRequest = onDismiss,
        title = s.locationOffTitle,
        message = s.locationOffBody,
        actions = listOf(
            com.osatks.mizan.ui.DialogAction(s.openSettings) { onDismiss(); AppStore.openSettings() },
            com.osatks.mizan.ui.DialogAction(s.pickCity, com.osatks.mizan.ui.ActionRole.Preferred, onPickCity),
        ),
    )
}

/**
 * مفتاح تنبيهات الأذان: يطلب إذن الإشعارات عند التفعيل فقط، وإن رفضه النظام (رُفض من قبل فلا يسأل ثانية)
 * يشرح ذلك ويفتح الإعدادات بطلب المستخدم، بدل مفتاح يعود إلى الإيقاف بلا سبب
 */
@Composable
fun rememberAlertsToggle(): (Boolean) -> Unit {
    val s = LocalStrings.current
    var wanted by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    val toggle = rememberNotifyToggle { ok ->
        AppSettings.updateNotify(ok)
        PrayerAlerts.reschedule()
        if (wanted && !ok) denied = true
    }
    if (denied) com.osatks.mizan.ui.PlatformDialog(
        onDismissRequest = { denied = false },
        title = s.notifyOffTitle,
        message = s.notifyOffBody,
        actions = listOf(
            com.osatks.mizan.ui.DialogAction(s.cancel, com.osatks.mizan.ui.ActionRole.Cancel) { denied = false },
            com.osatks.mizan.ui.DialogAction(s.openSettings, com.osatks.mizan.ui.ActionRole.Preferred) { denied = false; AppStore.openSettings() },
        ),
    )
    return remember(toggle) { { on -> wanted = on; toggle(on) } }
}
