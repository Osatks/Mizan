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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.AppSettings

val Gold = Color(0xFFE0B54A)
val GoldDeep = Color(0xFFB88A1E)
val BrandGreen = Color(0xFF0F6A55)
val BrandGreenDeep = Color(0xFF08463A)
val HeroBrush = Brush.linearGradient(listOf(BrandGreen, BrandGreenDeep))

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

/** الوضع الداكن الحالي؛ القوائم تُبنى من جديد عند تغيّره حتى تأخذ كل البطاقات ألوانها الجديدة فوراً */
val LocalDark = androidx.compose.runtime.staticCompositionLocalOf { false }

@Composable
fun MizanTheme(content: @Composable () -> Unit) {
    val dark = isDark()
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = AppTypography) {
        androidx.compose.runtime.CompositionLocalProvider(LocalDark provides dark, content = content)
    }
}
