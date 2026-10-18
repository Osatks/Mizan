package com.osatks.mizan.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.AppSettings
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

val Gold = Color(0xFFE0B54A)
val GoldDeep = Color(0xFFB88A1E)
val BrandGreen = Color(0xFF0F6A55)
val BrandGreenDeep = Color(0xFF08463A)

/**
 * لون التطبيق: عشرة ألوان يختار منها المستخدم في الإعدادات.
 * كل لون يغيّر درجة (hue) الألوان الخضراء فقط ويحفظ إضاءتها في OKLCH، فيبقى التباين كما هو في كل لون.
 * الذهبي والكريمي وورق المصحف وألوان التنبيه ثابتة. [chroma] يضاعف التشبّع لألوان تبدو باهتة بتشبّع الأخضر.
 */
enum class Accent(val key: String, val hue: Float, val chroma: Float) {
    EMERALD("emerald", 172f, 1f),
    TEAL("teal", 212f, 1.25f),
    BLUE("blue", 255f, 1.45f),
    PURPLE("purple", 300f, 1.5f),
    PINK("pink", 350f, 1.6f),
    MAROON("maroon", 20f, 1.45f),
    ORANGE("orange", 50f, 1.5f),
    HONEY("honey", 80f, 1.45f),
    BROWN("brown", 60f, 0.75f),
    GRAPHITE("graphite", 255f, 0.15f);

    companion object {
        fun of(key: String): Accent = entries.firstOrNull { it.key == key } ?: EMERALD
    }
}

val LocalAccent = androidx.compose.runtime.staticCompositionLocalOf { Accent.EMERALD }

/** يحوّل لوناً أخضر من ألوان التطبيق إلى درجة [a]؛ الألوان الأخرى (الذهبي والكريمي والأحمر) تبقى كما هي */
fun Color.recolor(a: Accent): Color {
    if (a == Accent.EMERALD) return this
    val (l, c, h) = toOklch()
    if (c < 0.004f || h !in 140f..210f) return this
    return oklch(l, c * a.chroma, a.hue, alpha)
}

/** اللون الأخضر نفسه بلون التطبيق الحالي */
val Color.themed: Color
    @Composable @ReadOnlyComposable get() = recolor(LocalAccent.current)

private val heroBrushes = Accent.entries.associateWith { Brush.linearGradient(listOf(BrandGreen.recolor(it), BrandGreenDeep.recolor(it))) }

fun heroBrush(a: Accent): Brush = heroBrushes.getValue(a)

/** تدرّج الترويسات والبطاقات البارزة بلون التطبيق */
val HeroBrush: Brush
    @Composable @ReadOnlyComposable get() = heroBrush(LocalAccent.current)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0D5C4B), onPrimary = Color.White,
    primaryContainer = Color(0xFFE3EFE9), onPrimaryContainer = Color(0xFF08463A),
    secondary = GoldDeep, onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6ECD2), onSecondaryContainer = Color(0xFF5C4300),
    background = Color(0xFFF8F5EE), onBackground = Color(0xFF1D2B27),
    surface = Color.White, onSurface = Color(0xFF1D2B27),
    surfaceVariant = Color(0xFFF1ECE0), onSurfaceVariant = Color(0xFF4F5E59),
    outline = Color(0xFFD9D2C0), outlineVariant = Color(0xFFE5DFD0),
    error = Color(0xFFB4412F), errorContainer = Color(0xFFFBE9E5), onErrorContainer = Color(0xFF6E2418),
    tertiary = GoldDeep, onTertiary = Color.White,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFBF9F4), surfaceContainer = Color(0xFFF6F2EA),
    surfaceContainerHigh = Color(0xFFF1ECE0), surfaceContainerHighest = Color(0xFFECE6D8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3FB595), onPrimary = Color(0xFF07231C),
    primaryContainer = Color(0xFF183A31), onPrimaryContainer = Color(0xFFBFEBDD),
    secondary = Gold, onSecondary = Color(0xFF2A1F00),
    secondaryContainer = Color(0xFF3A3020), onSecondaryContainer = Color(0xFFF1CF72),
    background = Color(0xFF0E1715), onBackground = Color(0xFFE8EFEC),
    surface = Color(0xFF16231F), onSurface = Color(0xFFE8EFEC),
    surfaceVariant = Color(0xFF1F302B), onSurfaceVariant = Color(0xFFB5C3BE),
    outline = Color(0xFF35504A), outlineVariant = Color(0xFF263A34),
    error = Color(0xFFEF8A76), errorContainer = Color(0xFF3D221D), onErrorContainer = Color(0xFFFFDAD3),
    tertiary = Gold, onTertiary = Color(0xFF2A1F00),
    surfaceContainerLowest = Color(0xFF0B1311), surfaceContainerLow = Color(0xFF131F1C), surfaceContainer = Color(0xFF182622),
    surfaceContainerHigh = Color(0xFF1F302B), surfaceContainerHighest = Color(0xFF263A34),
)

private val base = Typography()
private val AppTypography = Typography(
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
)

/** خط للأذكار والآيات بطابع تقليدي */
val DhikrStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 48.sp)

@Composable
fun isDark(): Boolean = when (AppSettings.theme) {
    "dark" -> true
    "light" -> false
    else -> isSystemInDarkTheme()
}

/** الوضع الداكن الحالي */
val LocalDark = androidx.compose.runtime.staticCompositionLocalOf { false }

/** مفتاح يتغيّر مع الوضع الداكن أو لون التطبيق؛ القوائم تُبنى من جديد عنده حتى تأخذ كل البطاقات ألوانها الجديدة فوراً */
val LocalThemeKey = androidx.compose.runtime.staticCompositionLocalOf<Any> { false }

private val schemes = HashMap<Pair<Accent, Boolean>, ColorScheme>()

internal fun schemeFor(a: Accent, dark: Boolean): ColorScheme =
    schemes.getOrPut(a to dark) { (if (dark) DarkColors else LightColors).recolor(a) }

@Composable
fun MizanTheme(content: @Composable () -> Unit) {
    val dark = isDark()
    val accent = Accent.of(AppSettings.accent)
    MaterialTheme(colorScheme = schemeFor(accent, dark), typography = AppTypography) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalDark provides dark, LocalAccent provides accent, LocalThemeKey provides (accent to dark), content = content
        )
    }
}

private fun ColorScheme.recolor(a: Accent): ColorScheme {
    if (a == Accent.EMERALD) return this
    fun Color.r() = recolor(a)
    return copy(
        primary = primary.r(), onPrimary = onPrimary.r(), primaryContainer = primaryContainer.r(), onPrimaryContainer = onPrimaryContainer.r(),
        inversePrimary = inversePrimary.r(),
        secondary = secondary.r(), onSecondary = onSecondary.r(), secondaryContainer = secondaryContainer.r(), onSecondaryContainer = onSecondaryContainer.r(),
        tertiary = tertiary.r(), onTertiary = onTertiary.r(), tertiaryContainer = tertiaryContainer.r(), onTertiaryContainer = onTertiaryContainer.r(),
        background = background.r(), onBackground = onBackground.r(),
        surface = surface.r(), onSurface = onSurface.r(), surfaceVariant = surfaceVariant.r(), onSurfaceVariant = onSurfaceVariant.r(),
        surfaceTint = surfaceTint.r(), inverseSurface = inverseSurface.r(), inverseOnSurface = inverseOnSurface.r(),
        outline = outline.r(), outlineVariant = outlineVariant.r(),
        surfaceBright = surfaceBright.r(), surfaceDim = surfaceDim.r(),
        surfaceContainerLowest = surfaceContainerLowest.r(), surfaceContainerLow = surfaceContainerLow.r(), surfaceContainer = surfaceContainer.r(),
        surfaceContainerHigh = surfaceContainerHigh.r(), surfaceContainerHighest = surfaceContainerHighest.r(),
    )
}

// OKLab (Björn Ottosson): درجة اللون تتغيّر وتبقى الإضاءة كما تراها العين

private fun lin(c: Float): Float = if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
private fun gam(c: Float): Float = if (c <= 0.0031308f) 12.92f * c else 1.055f * c.pow(1f / 2.4f) - 0.055f

private fun Color.toOklch(): Triple<Float, Float, Float> {
    val r = lin(red); val g = lin(green); val b = lin(blue)
    val l = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
    val m = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
    val s = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)
    val L = 0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s
    val A = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s
    val B = 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s
    val h = (atan2(B, A) * 180f / PI.toFloat()).let { if (it < 0) it + 360f else it }
    return Triple(L, sqrt(A * A + B * B), h)
}

/** sRGB خطي من OKLCH */
private fun oklchLinear(L: Float, c: Float, h: Float): FloatArray {
    val rad = h * PI.toFloat() / 180f
    val a = c * cos(rad); val b = c * sin(rad)
    val l = (L + 0.3963377774f * a + 0.2158037573f * b).let { it * it * it }
    val m = (L - 0.1055613458f * a - 0.0638541728f * b).let { it * it * it }
    val s = (L - 0.0894841775f * a - 1.2914855480f * b).let { it * it * it }
    return floatArrayOf(
        4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
        -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
        -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s,
    )
}

/** لون من OKLCH؛ إن خرج عن نطاق sRGB يُخفَّض التشبّع حتى يدخل، فتبقى الإضاءة والدرجة */
private fun oklch(L: Float, c: Float, h: Float, alpha: Float): Color {
    fun inGamut(v: FloatArray) = v.all { it in -1e-4f..1.0001f }
    var rgb = oklchLinear(L, c, h)
    if (!inGamut(rgb)) {
        var lo = 0f; var hi = c
        repeat(20) { val mid = (lo + hi) / 2; if (inGamut(oklchLinear(L, mid, h))) lo = mid else hi = mid }
        rgb = oklchLinear(L, lo, h)
    }
    return Color(gam(rgb[0].coerceIn(0f, 1f)), gam(rgb[1].coerceIn(0f, 1f)), gam(rgb[2].coerceIn(0f, 1f)), alpha)
}
