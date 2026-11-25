package com.osatks.mizan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Muezzin
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.LocaleSync
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.platform.AdhanPreview
import com.osatks.mizan.platform.systemLanguageTag
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.ui.BackHandler
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.RollingText
import com.osatks.mizan.ui.SlowStar
import com.osatks.mizan.ui.StatusBarStyle
import com.osatks.mizan.ui.drawStar8
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.quran.ToneCard
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep
import com.osatks.mizan.ui.theme.HeroBrush
import com.osatks.mizan.ui.theme.LocalDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/** أيقونة كل خطوة بترتيب WelcomeFlow */
private val STEP_ICONS = listOf(
    Icons.Filled.Star, Icons.Filled.Place, Icons.Filled.NotificationsActive,
    Icons.Filled.Palette, Icons.Filled.TouchApp, Icons.Filled.CheckCircle,
)

/** لون النص على الذهبي */
private val Ink = Color(0xFF1E1A14)

/**
 * إعداد أول تشغيل في أندرويد: خطوات يفعل فيها المستخدم شيئاً لا صفحات يقرؤها فقط (WelcomeFlow) — يختار لغته فتتبدل الواجهة
 * فوراً، ويحدد موقعه فيرى مواقيت اليوم والعدّ إلى الصلاة القادمة، ويفعّل الأذان ويسمع المؤذن، ويختار المظهر فتتبدل الشاشة
 * نفسها، ويجرّب حبات المسبحة، ثم يرى ملخص ما ضبطه. على خلفية التطبيق الخضراء ونجمتها الدوّارة كما كانت الجولة.
 * التقدم خطوات في الأعلى، و«رجوع» و«التالي» في الأسفل مع السحب، و«تخطي» متاح دائماً: كل اختيار محفوظ فوراً،
 * وما لم يُختر يبقى على الافتراضي. الجولة القديمة (Onboarding) تبقى لـ«دليل التطبيق» ولصفحات الدليل الجديدة بعد التحديث.
 */
@Composable
fun Welcome(onDone: () -> Unit) {
    val s = LocalStrings.current
    val dark = LocalDark.current
    QuranPrefs.ensure()
    val count = WelcomeFlow.COUNT
    val pager = rememberPagerState { count }
    val scope = rememberCoroutineScope()
    val page = pager.currentPage
    val last = page == count - 1
    fun go(i: Int) {
        scope.launch { pager.animateScrollToPage(i.coerceIn(0, count - 1)) }
    }
    // معاينة الأذان لا تستمر خارج صفحته ولا بعد الإعداد
    LaunchedEffect(page) { if (page != WelcomeFlow.ADHAN) AdhanPreview.stop() }
    DisposableEffect(Unit) { onDispose { AdhanPreview.stop() } }
    // رجوع النظام يعود خطوة، ومن الأولى يتصرف كما كان
    BackHandler(enabled = page > 0) { go(page - 1) }
    // خلفية خضراء كاملة: نص شريط الحالة فاتح
    StatusBarStyle(lightContent = true)
    // المظهر الداكن يُعتم الخلفية قليلاً، فيرى المستخدم الشاشة نفسها تتبدل مع اختياره
    val scrim by animateFloatAsState(if (dark) .32f else 0f, tween(Motion.LONG), label = "scrim")
    Box(Modifier.fillMaxSize().background(HeroBrush).background(Color.Black.copy(alpha = scrim))) {
        // نجمة كبيرة تدور ببطء خلف الخطوات، في زاوية النهاية
        val starStroke = with(LocalDensity.current) { 1.5.dp.toPx() }
        SlowStar(Modifier.size(420.dp).align(Alignment.TopEnd).offset(x = 110.dp, y = (-70).dp), Color.White.copy(alpha = .06f), 90_000, starStroke)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // شريط الخطوات و«تخطي»
            Row(
                Modifier.fillMaxWidth().wrapContentWidth().widthIn(max = 640.dp).heightIn(min = 52.dp)
                    .padding(start = 24.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepBar(page, count, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                if (!last) TextButton(onClick = onDone, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(s.obSkip, color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium))
                } else Spacer(Modifier.height(48.dp))
            }
            HorizontalPager(pager, Modifier.weight(1f)) { i ->
                // في النوافذ القصيرة (هاتف بالعرض) تصغر الشارة وتُمرَّر الخطوة، وعلى الشاشات العريضة يبقى المحتوى بعرض مريح في الوسط
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val short = maxHeight < 560.dp
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).heightIn(min = maxHeight)
                            .padding(horizontal = 24.dp, vertical = 8.dp).wrapContentWidth().widthIn(max = 560.dp)
                            .graphicsLayer {
                                val o = ((pager.currentPage - i) + pager.currentPageOffsetFraction).absoluteValue.coerceAtMost(1f)
                                alpha = 1f - 0.6f * o; scaleX = 1f - 0.06f * o; scaleY = 1f - 0.06f * o
                            },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        val (title, body) = when (i) {
                            WelcomeFlow.HELLO -> s.obHelloTitle to s.obHelloBody
                            WelcomeFlow.PLACE -> s.obLocationTitle to s.obLocationBody
                            WelcomeFlow.ADHAN -> s.obAdhanTitle to s.obAdhanBody
                            WelcomeFlow.STYLE -> s.obStyleTitle to s.obStyleBody
                            WelcomeFlow.TASTE -> s.obTryTitle to s.obTryBody
                            else -> s.obDoneTitle to s.obDoneBody
                        }
                        StepBadge(STEP_ICONS[i], short)
                        Spacer(Modifier.height(if (short) 14.dp else 24.dp))
                        Text(
                            title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                            fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(body, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), color = Color.White.copy(alpha = .85f), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(if (short) 18.dp else 26.dp))
                        when (i) {
                            WelcomeFlow.HELLO -> HelloStep()
                            WelcomeFlow.PLACE -> PlaceStep()
                            WelcomeFlow.ADHAN -> AdhanStep()
                            WelcomeFlow.STYLE -> StyleStep()
                            WelcomeFlow.TASTE -> TasteStep()
                            else -> DoneStep()
                        }
                    }
                }
            }
            // «رجوع» و«التالي» (بعرض أقصاه 640 في الوسط على الشاشات العريضة)
            Row(
                Modifier.fillMaxWidth().wrapContentWidth().widthIn(max = 640.dp).padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (page > 0) TextButton(onClick = { go(page - 1) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, Modifier.size(20.dp), tint = Color.White.copy(alpha = .85f))
                    Spacer(Modifier.width(2.dp))
                    Text(s.back, color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium))
                }
                Spacer(Modifier.weight(1f))
                // زر كبسولي ذهبي يرتد عند اللمس؛ في الخطوة الأخيرة «ابدأ» بحجم أكبر
                val nextSrc = remember { MutableInteractionSource() }
                Button(
                    onClick = { if (last) onDone() else go(page + 1) },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
                    contentPadding = PaddingValues(horizontal = if (last) 36.dp else 26.dp),
                    interactionSource = nextSrc,
                    modifier = Modifier.heightIn(min = if (last) 52.dp else 46.dp).pressScale(nextSrc),
                ) {
                    Text(
                        if (last) s.obStart else s.obNext, maxLines = 1,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = if (last) 17.sp else 15.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

/** التقدم: شرطة لكل خطوة، ذهبية لما مضى منها والحالية؛ ويقرؤها قارئ الشاشة «الخطوة ٢ من ٦» */
@Composable
private fun StepBar(page: Int, count: Int, modifier: Modifier) {
    val s = LocalStrings.current
    Row(
        modifier.widthIn(max = 280.dp).clearAndSetSemantics { contentDescription = s.obStep(page + 1, count) },
        horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val c by animateColorAsState(if (i <= page) Gold else Color.White.copy(alpha = .28f), tween(Motion.MEDIUM), label = "step")
            val h by animateFloatAsState(if (i == page) 6f else 4f, tween(Motion.MEDIUM), label = "stepH")
            Box(Modifier.weight(1f).height(h.dp).clip(CircleShape).background(c))
        }
    }
}

/** الشارة: نجمة ثمانية ذهبية وفي وسطها أيقونة الخطوة (أصغر من شارة الجولة لتتسع الخطوة لما فيها) */
@Composable
private fun StepBadge(icon: ImageVector, short: Boolean) {
    Box(Modifier.size(if (short) 76.dp else 112.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = .08f), size.minDimension / 2)
            drawStar8(Offset(size.width / 2, size.height / 2), size.minDimension * 0.42f, Gold.copy(alpha = .9f), 2.dp.toPx(), rings = false)
            drawCircle(Color.White.copy(alpha = .10f), size.minDimension * 0.27f)
        }
        Icon(icon, null, Modifier.size(if (short) 26.dp else 36.dp), tint = Gold)
    }
}

/** عنوان صغير فوق مجموعة على الخلفية الخضراء */
@Composable
private fun HeroLabel(icon: ImageVector, text: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = Gold)
        Spacer(Modifier.width(8.dp))
        Text(text, Modifier.semantics { heading() }, color = Color.White, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold))
    }
}

/** بطاقة بألوان سطح التطبيق (تتبدل مع المظهر فوراً) فوق الخلفية الخضراء */
@Composable
private fun HeroCard(content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(cs.surface, tween(Motion.MEDIUM), label = "card")
    Surface(shape = RoundedCornerShape(24.dp), color = bg, contentColor = cs.onSurface, modifier = Modifier.fillMaxWidth()) {
        Column { content() }
    }
}

/** اختيار على الخلفية الخضراء: ذهبي حين يُختار، وشفاف بحافة رفيعة قبله */
@Composable
private fun HeroChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Gold else Color.White.copy(alpha = .10f), tween(Motion.SHORT), label = "chip")
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier.heightIn(min = 44.dp).clip(shape).background(bg)
            .border(1.dp, if (selected) Gold else Color.White.copy(alpha = .30f), shape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, color = if (selected) Ink else Color.White, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
        )
    }
}

// ---------- الخطوات ----------

/** الترحيب: لغات التطبيق أزراراً؛ اختيار لغة يبدّل الإعداد نفسه فوراً (LocaleSync كما في ورقة اللغات) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HelloStep() {
    val s = LocalStrings.current
    val system = Lang.fromTag(systemLanguageTag())
    HeroLabel(Icons.Filled.Translate, s.language)
    FlowRow(
        Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeroChip("${s.deviceLanguage} · ${system.native}", AppSettings.lang == Lang.SYSTEM) { chooseLanguage(Lang.SYSTEM) }
        Lang.entries.forEach { l -> HeroChip(l.native, AppSettings.lang == l.code) { chooseLanguage(l.code) } }
    }
}

private fun chooseLanguage(code: String) {
    if (code != AppSettings.lang) LocaleSync.choose(code)
}

/** الموقع: بعد تحديده بطاقة مواقيت اليوم والعدّ إلى القادمة، وتحتها زرّا الموقع لتغييره */
@Composable
private fun PlaceStep() {
    AnimatedVisibility(
        AppSettings.placeSet,
        enter = fadeIn(tween(Motion.MEDIUM)) + expandVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)),
        exit = fadeOut(tween(Motion.SHORT)) + shrinkVertically(tween(Motion.SHORT)),
    ) {
        Column {
            PrayerPreview()
            Spacer(Modifier.height(16.dp))
        }
    }
    LocationButtons(onHero = true, modifier = Modifier.fillMaxWidth())
}

/** معاينة حية: مدينة المستخدم ومواقيت اليوم الخمسة (القادمة مميزة) والعدّ إليها كل ثانية */
@Composable
private fun PrayerPreview() {
    val s = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    var next by remember { mutableStateOf(PrayerUtil.next()) }
    var now by remember { mutableStateOf(PrayerUtil.now()) }
    LaunchedEffect(AppSettings.place, AppSettings.method, AppSettings.hanafi) {
        while (true) { next = PrayerUtil.next(); now = PrayerUtil.now(); delay(1000) }
    }
    val today = now.date
    val nextToday = next.key.takeIf { next.at.date == today }
    val prayers = remember(s, today, nextToday, AppSettings.place, AppSettings.method, AppSettings.hanafi, AppSettings.use24h) {
        welcomePrayers(s, today, nextToday)
    }
    HeroCard {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, null, Modifier.size(22.dp), tint = cs.primary)
            Spacer(Modifier.width(10.dp))
            Text(s.placeName(AppSettings.place), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), fontWeight = FontWeight.Bold)
        }
        Text(
            s.obTodayTimes, Modifier.padding(start = 48.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = cs.onSurfaceVariant,
        )
        Column(Modifier.padding(horizontal = 8.dp)) {
            prayers.forEach { p ->
                val bg by animateColorAsState(if (p.next) cs.primaryContainer else Color.Transparent, tween(Motion.MEDIUM), label = "next")
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg)
                        .padding(horizontal = 10.dp, vertical = 7.dp).semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(p.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), fontWeight = if (p.next) FontWeight.Bold else FontWeight.Normal,
                        color = if (p.next) cs.onPrimaryContainer else cs.onSurface)
                    Text(p.time, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), fontWeight = if (p.next) FontWeight.Bold else FontWeight.Medium,
                        color = if (p.next) cs.onPrimaryContainer else cs.onSurface)
                }
            }
        }
        HorizontalDivider(Modifier.padding(top = 8.dp), color = cs.outlineVariant)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(s.nextPrayerLabel(s.prayer(next.key)), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = cs.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Gold.copy(alpha = .22f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                RollingText(PrayerUtil.hms(next.remaining), MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold), cs.onSurface)
            }
        }
    }
}

/** الأذان: مفتاح التنبيهات (يطلب إذن الإشعارات بالطريقة نفسها)، والمؤذن مع الاستماع إليه */
@Composable
private fun AdhanStep() {
    val s = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val toggle = rememberAlertsToggle()
    val voices = WelcomeFlow.muezzins()
    HeroCard {
        Row(
            Modifier.fillMaxWidth().toggleable(AppSettings.notify, role = Role.Switch) { toggle(it) }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.NotificationsActive, null, Modifier.size(24.dp), tint = cs.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(s.adhanAlert, style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp), fontWeight = FontWeight.Bold)
                Text(s.adhanAlertDesc, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = cs.onSurfaceVariant)
            }
            Spacer(Modifier.width(10.dp))
            Switch(checked = AppSettings.notify, onCheckedChange = null)
        }
        HorizontalDivider(color = cs.outlineVariant)
        Text(
            s.muezzinTitle, Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 2.dp).semantics { heading() },
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), color = cs.primary, fontWeight = FontWeight.Bold,
        )
        Column(Modifier.selectableGroup().padding(bottom = 6.dp)) {
            voices.forEach { m -> VoiceRow(m, AppSettings.muezzin == m.id) }
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(s.obMoreVoices, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = Color.White.copy(alpha = .75f), textAlign = TextAlign.Center)
}

/** مؤذن: اختياره لكل الصلوات، وزر الاستماع (المعاينة نفسها في صفحة الأذان) */
@Composable
private fun VoiceRow(m: Muezzin, selected: Boolean) {
    val s = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val playing = AdhanPreview.playing == m.id
    val name = s.muezzinName(m)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).heightIn(min = 56.dp)
                .selectable(selected, role = Role.RadioButton) { WelcomeFlow.pickMuezzin(m) }
                .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                Text(duration(m.seconds), style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = cs.onSurfaceVariant)
            }
        }
        IconButton(onClick = { AdhanPreview.toggle(m) }, modifier = Modifier.padding(end = 8.dp)) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(if (playing) cs.primary else cs.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    "${if (playing) s.stopAdhan else s.playAdhan}: $name",
                    Modifier.size(20.dp), tint = if (playing) cs.onPrimary else cs.primary,
                )
            }
        }
    }
}

/** المظهر (تلقائي، فاتح، داكن) والشاشة تتبدل معه، ولون صفحة المصحف بطاقاتٍ مصغّرة كما في ورقة العرض */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleStep() {
    val s = LocalStrings.current
    HeroLabel(Icons.Filled.DarkMode, s.appearance)
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WelcomeFlow.THEMES.forEach { key ->
            val icon = when (key) { "light" -> Icons.Filled.LightMode; "dark" -> Icons.Filled.DarkMode; else -> Icons.Filled.BrightnessAuto }
            ThemeTile(icon, s.welcomeThemeName(key), AppSettings.theme == key, Modifier.weight(1f)) { AppSettings.updateTheme(key) }
        }
    }
    Spacer(Modifier.height(22.dp))
    HeroLabel(Icons.AutoMirrored.Filled.MenuBook, s.pageColor)
    HeroCard {
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WelcomeFlow.TONES.forEach { t -> ToneCard(t, s.welcomeToneName(t)) }
        }
    }
}

/** خيار المظهر: أيقونته واسمه، ذهبي حين يُختار */
@Composable
private fun ThemeTile(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Gold else Color.White.copy(alpha = .10f), tween(Motion.SHORT), label = "theme")
    val fg = if (selected) Ink else Color.White
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.heightIn(min = 76.dp).clip(shape).background(bg)
            .border(1.dp, if (selected) Gold else Color.White.copy(alpha = .30f), shape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = fg)
        Spacer(Modifier.height(6.dp))
        Text(
            label, color = fg, textAlign = TextAlign.Center, maxLines = 2,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
        )
    }
}

/** تجربة صغيرة: حبة مسبحة تُلمس ثلاث مرات باهتزاز خفيف، ثم علامة إتمام مبهجة (لا تُحسب في المسبحة نفسها) */
@Composable
private fun TasteStep() {
    val s = LocalStrings.current
    val haptic = LocalHapticFeedback.current
    var count by rememberSaveable { mutableIntStateOf(0) }
    val done = count >= WelcomeFlow.BEADS
    val bump = remember { Animatable(1f) }
    LaunchedEffect(count) {
        if (count == 0 || Motion.reduced) return@LaunchedEffect
        bump.snapTo(.9f)
        bump.animateTo(1f, spring(dampingRatio = .45f, stiffness = Spring.StiffnessMedium))
    }
    val src = remember { MutableInteractionSource() }
    Text(WelcomeFlow.DHIKR, color = Gold, style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    Box(
        Modifier.size(148.dp).graphicsLayer { scaleX = bump.value; scaleY = bump.value }
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Gold, GoldDeep)))
            .border(3.dp, Color.White.copy(alpha = .35f), CircleShape)
            .clickable(interactionSource = src, indication = ripple(color = Color.White), role = Role.Button) {
                if (done) count = 1 else count++
                haptic.performHapticFeedback(if (count >= WelcomeFlow.BEADS) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
            }
            .semantics { contentDescription = s.obBead; stateDescription = "$count / ${WelcomeFlow.BEADS}"; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Filled.Check, null, Modifier.size(64.dp), tint = Ink)
        else Text("$count", color = Ink, style = MaterialTheme.typography.displaySmall.copy(fontSize = 48.sp, fontWeight = FontWeight.Black))
    }
    Spacer(Modifier.height(16.dp))
    // حبات صغيرة تمتلئ مع كل لمسة
    Row(Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(WelcomeFlow.BEADS) { i ->
            val c by animateColorAsState(if (i < count) Gold else Color.White.copy(alpha = .25f), tween(Motion.SHORT), label = "bead")
            Box(Modifier.size(14.dp).clip(CircleShape).background(c))
        }
    }
    AnimatedVisibility(done, enter = fadeIn(tween(Motion.MEDIUM)) + scaleIn(tween(Motion.MEDIUM, easing = Motion.Decelerate), initialScale = .85f), exit = fadeOut(tween(Motion.SHORT))) {
        Text(
            s.obTryDone, Modifier.padding(top = 16.dp), color = Color.White, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

/** الملخص: ما ضُبط (اللغة، المدينة، الأذان، المظهر)، وما لم يُضبط بدائرة فارغة */
@Composable
private fun DoneStep() {
    val s = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val rows = welcomeSummary(s, LocalLang.current)
    HeroCard {
        rows.forEachIndexed { i, r ->
            if (i > 0) HorizontalDivider(Modifier.padding(start = 56.dp), color = cs.outlineVariant)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (r.ok) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, Modifier.size(24.dp),
                    tint = if (r.ok) cs.primary else cs.onSurfaceVariant,
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = cs.onSurfaceVariant)
                    Text(r.value, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
