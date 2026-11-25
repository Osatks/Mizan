package com.osatks.mizan.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.LocationFetcher
import com.osatks.mizan.core.Mosque
import com.osatks.mizan.core.MosqueFinder
import com.osatks.mizan.core.Mosques
import com.osatks.mizan.core.NearMosque
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.platform.AppStore
import com.osatks.mizan.platform.LocationResult
import com.osatks.mizan.platform.rememberLocationRequest
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.HeroBrush
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

/** حالة صفحة المساجد القريبة */
private sealed interface MosquesUi {
    data object Locating : MosquesUi
    data object Searching : MosquesUi
    data class Found(val list: List<NearMosque>) : MosquesUi
    data object Empty : MosquesUi
    /** تعذّر الاتصال أو ردّ الخادم بخطأ */
    data object Failed : MosquesUi
    /** تعذّر تحديد الموقع ولا موقع محفوظ في التطبيق */
    data object NoLocation : MosquesUi
    /** لا إذن بالموقع ولا موقع محفوظ؛ denied: رُفض الطلب (فالحل في الإعدادات) */
    data class NeedPermission(val denied: Boolean) : MosquesUi
}

/** لماذا يُبحث حول موقع التطبيق المحفوظ لا الموقع الحالي */
private enum class SavedWhy { NotAsked, Denied, Failed }

/**
 * المساجد القريبة في أندرويد بطراز صفحة القبلة: الموقع الحالي إن كان مأذوناً (LocationFetcher، بلا طلب إذن جديد)،
 * وإلا موقع التطبيق المحفوظ مع زر لاستعمال الموقع الحالي. البيانات من OpenStreetMap (MosqueFinder)، مرتبة بالمسافة،
 * ولكل مسجد اسمه بلغة التطبيق إن وُجد وعنوانه والمسافة والجهة؛ لمسه يفتح التنقل في خرائط Google إن وُجدت وإلا أي
 * تطبيق خرائط (geo:). مع حالات التحميل وعدم وجود مساجد والخطأ ورفض الإذن، وإسناد بيانات OpenStreetMap.
 */
@Composable
fun MosquesScreen(onBack: (() -> Unit)? = null) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var ui by remember { mutableStateOf<MosquesUi>(MosquesUi.Locating) }
    // null: حول الموقع الحالي
    var savedWhy by remember { mutableStateOf<SavedWhy?>(null) }
    var center by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }

    fun search(lat: Double, lng: Double, why: SavedWhy?) {
        savedWhy = why
        center = lat to lng
        job?.cancel()
        MosqueFinder.cached(lat, lng, lang.code)?.let { list ->
            ui = if (list.isEmpty()) MosquesUi.Empty else MosquesUi.Found(list)
            return
        }
        ui = MosquesUi.Searching
        job = scope.launch {
            ui = try {
                val list = MosqueFinder.find(lat, lng, lang.code)
                if (list.isEmpty()) MosquesUi.Empty else MosquesUi.Found(list)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MosquesUi.Failed
            }
        }
    }

    // موقع التطبيق المحفوظ (من صفحة المواقيت) حين يتعذّر الحالي
    fun useSaved(why: SavedWhy) {
        if (AppSettings.placeSet) search(AppSettings.place.lat, AppSettings.place.lng, why)
        else {
            savedWhy = null
            ui = when (why) {
                SavedWhy.Failed -> MosquesUi.NoLocation
                SavedWhy.Denied -> MosquesUi.NeedPermission(denied = true)
                SavedWhy.NotAsked -> MosquesUi.NeedPermission(denied = false)
            }
        }
    }

    fun locate() {
        ui = MosquesUi.Locating
        LocationFetcher.fetch(ctx) { p -> if (p != null) search(p.lat, p.lng, null) else useSaved(SavedWhy.Failed) }
    }

    // يطلب الإذن (إن لم يُمنح) ثم يحدد الموقع؛ لا يُستدعى إلا بلمس المستخدم
    val ask = rememberLocationRequest { r ->
        when (r) {
            is LocationResult.Found -> search(r.place.lat, r.place.lng, null)
            LocationResult.Denied -> useSaved(SavedWhy.Denied)
            LocationResult.Failed -> useSaved(SavedWhy.Failed)
        }
    }

    fun retry() {
        val c = center
        if (c != null) search(c.first, c.second, savedWhy) else locate()
    }

    LaunchedEffect(Unit) {
        val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }
        if (granted) locate() else useSaved(SavedWhy.NotAsked)
    }

    val state = ui
    val why = savedWhy
    val showSaved = why != null && state !is MosquesUi.Locating
    ToolPage(s.mosquesTitle, s.mosquesSubtitle, onBack, sharedKey = "mosques", icon = TOOL_ICONS["mosques"], plainIcon = true) {
        // يُعرض ما حول الموقع المحفوظ: لماذا، وكيف يُستعمل الموقع الحالي
        if (showSaved) item(key = "saved") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Note(s.mosquesNearSaved(s.placeName(AppSettings.place)), warn = why != SavedWhy.NotAsked)
                when (why) {
                    SavedWhy.Denied -> PillButton(s.openSettings, Icons.Filled.Settings, primary = false) { AppStore.openSettings() }
                    else -> PillButton(s.useMyLocation, Icons.Filled.MyLocation, primary = false) { ask() }
                }
            }
        }
        when (state) {
            MosquesUi.Locating -> item(key = "status") { StatusCard(s.locating) }
            MosquesUi.Searching -> item(key = "status") { StatusCard(s.mosquesLoading) }
            is MosquesUi.Found -> state.list.forEach { m ->
                item(key = "m-${m.mosque.id}") {
                    val name = m.mosque.name ?: s.mosque
                    MosqueRow(m, name, s) { navigate(ctx, m.mosque, name) }
                }
            }
            MosquesUi.Empty -> item(key = "message") {
                MessageCard(Icons.Filled.SearchOff, s.mosquesEmpty, s.mosquesEmptyHint) {
                    PillButton(s.openInMaps, Icons.Filled.Map, primary = true) { mapsSearch(ctx, center, s.mosque) }
                }
            }
            MosquesUi.Failed -> item(key = "message") {
                MessageCard(Icons.Filled.CloudOff, s.mosquesError, s.mosquesErrorHint) {
                    PillButton(s.retry, Icons.Filled.Refresh, primary = true) { retry() }
                    PillButton(s.openInMaps, Icons.Filled.Map, primary = false) { mapsSearch(ctx, center, s.mosque) }
                }
            }
            MosquesUi.NoLocation -> item(key = "message") {
                MessageCard(Icons.Filled.LocationOff, s.mosquesLocationFailed, null) {
                    PillButton(s.retry, Icons.Filled.Refresh, primary = true) { locate() }
                    PillButton(s.openInMaps, Icons.Filled.Map, primary = false) { mapsSearch(ctx, null, s.mosque) }
                }
            }
            is MosquesUi.NeedPermission -> item(key = "message") {
                MessageCard(Icons.Filled.LocationOff, if (state.denied) s.locationOffTitle else s.mosquesTitle, s.mosquesNeedLocation) {
                    PillButton(s.useMyLocation, Icons.Filled.MyLocation, primary = !state.denied) { ask() }
                    if (state.denied) PillButton(s.openSettings, Icons.Filled.Settings, primary = true) { AppStore.openSettings() }
                }
            }
        }
        // ما يُرسل للبحث، وإسناد بيانات OpenStreetMap (رخصة ODbL تشترطه حيث تُعرض بياناتها)
        item(key = "footer") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    s.mosquesPrivacyOsm, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state is MosquesUi.Found || state is MosquesUi.Empty) {
                    Text(
                        s.osmAttribution,
                        Modifier.heightIn(min = 32.dp)
                            .clickable(role = Role.Button) { runCatching { uri.openUri("https://www.openstreetmap.org/copyright") } }
                            .padding(vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** بطاقة الصفحة كما في القبلة: بيضاء بزوايا 22 وحد رفيع، وحشوة 16 */
@Composable
private fun MosqueCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(cs.surface).border(1.dp, cs.outlineVariant.copy(alpha = .6f), shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** تحديد الموقع أو البحث جارٍ؛ يعلنه قارئ الشاشة */
@Composable
private fun StatusCard(text: String) {
    MosqueCard(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.5.dp)
            Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp), color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** رسالة في وسط بطاقة: أيقونة في دائرة، وعنوان وشرح، وأزرار */
@Composable
private fun MessageCard(icon: ImageVector, title: String, body: String?, actions: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    MosqueCard {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(cs.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(28.dp), tint = cs.primary)
            }
            Text(
                title, Modifier.fillMaxWidth().semantics { heading() }, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = cs.onSurface,
            )
            if (body != null) Text(
                body, Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = cs.onSurfaceVariant,
            )
            Column(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = actions,
            )
        }
    }
}

/** زر بشكل كبسولة: بتدرج لون التطبيق، أو بحد رفيع للثانوي */
@Composable
private fun PillButton(text: String, icon: ImageVector, primary: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val src = remember { MutableInteractionSource() }
    val fill = if (primary) Modifier.background(HeroBrush) else Modifier.background(cs.surface).border(1.dp, cs.outlineVariant, CircleShape)
    Row(
        Modifier.heightIn(min = 48.dp).pressScale(src, 0.97f).clip(CircleShape).then(fill)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = if (primary) Color.White else cs.primary)
        Text(
            text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            color = if (primary) Color.White else cs.primary,
        )
    }
}

/** مسجد في القائمة: اسمه وعنوانه، والمسافة والجهة؛ لمسه يفتح التنقل إليه */
@Composable
private fun MosqueRow(m: NearMosque, name: String, s: Strings, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val src = remember { MutableInteractionSource() }
    val distance = Mosques.distance(m.meters, s)
    val direction = Mosques.direction(m.bearing, s)
    MosqueCard(
        Modifier.pressScale(src, 0.98f)
            .clickable(interactionSource = src, indication = null, onClickLabel = s.mosqueDirections, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Gold.copy(alpha = .16f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Mosque, null, Modifier.size(22.dp), tint = cs.primary) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    name, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = cs.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                m.mosque.address?.let {
                    Text(
                        it, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = cs.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "$distance · $direction",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = cs.primary,
                )
            }
            Icon(Icons.Filled.Directions, null, Modifier.size(24.dp), tint = cs.primary)
        }
    }
}

/** إحداثية لروابط الخرائط بنقطة لاتينية دائماً */
private fun c(v: Double) = String.format(Locale.US, "%.6f", v)

/** التنقل إلى المسجد: خرائط Google إن وُجدت، وإلا أي تطبيق خرائط (geo:)، وإلا خريطة OpenStreetMap في المتصفح */
private fun navigate(ctx: Context, m: Mosque, label: String) {
    val google = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${c(m.lat)},${c(m.lng)}"))
        .setPackage("com.google.android.apps.maps")
    if (runCatching { ctx.startActivity(google) }.isSuccess) return
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${c(m.lat)},${c(m.lng)}?q=${c(m.lat)},${c(m.lng)}(${Uri.encode(label)})"))
    if (runCatching { ctx.startActivity(geo) }.isSuccess) return
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/?mlat=${c(m.lat)}&mlon=${c(m.lng)}#map=17/${c(m.lat)}/${c(m.lng)}")))
    }
}

/** البحث عن «مسجد» في تطبيق الخرائط حول الموقع (أو حيث يرى التطبيق إن لم يُعرف) */
private fun mapsSearch(ctx: Context, center: Pair<Double, Double>?, word: String) {
    val at = center?.let { "${c(it.first)},${c(it.second)}" } ?: "0,0"
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$at?q=${Uri.encode(word)}"))
    if (runCatching { ctx.startActivity(geo) }.isSuccess) return
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/search?query=${Uri.encode(word)}")))
    }
}
