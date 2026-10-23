package com.ssk.chhupanchhupai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Night-time street game palette: dusk indigo + kite-festival saffron.
val Night = Color(0xFF0E1026)
val NightSurface = Color(0xFF181A36)
val NightSurfaceHigh = Color(0xFF23264A)
val Saffron = Color(0xFFFF9933)
val Marigold = Color(0xFFFFC145)
val HiderTeal = Color(0xFF2EC4B6)
val SeekerRed = Color(0xFFFF4D6D)
val SpectatorGrey = Color(0xFF9AA0C3)
val TextPrimary = Color(0xFFF4F4FA)
val TextMuted = Color(0xFFB4B7D6)

private val colors = darkColorScheme(
    primary = Saffron,
    onPrimary = Night,
    primaryContainer = Color(0xFF5A3410),
    onPrimaryContainer = Marigold,
    secondary = HiderTeal,
    onSecondary = Night,
    tertiary = SeekerRed,
    onTertiary = Night,
    background = Night,
    onBackground = TextPrimary,
    surface = NightSurface,
    onSurface = TextPrimary,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = TextMuted,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = Color(0xFF2C3058),
    outline = Color(0xFF4A4E80),
    error = SeekerRed,
    onError = Night,
)

private val base = Typography()
private val typography = base.copy(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Black),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
)

val CountdownStyle = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)

@Composable
fun ChhupanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}

fun roleColor(role: String?): Color = when (role) {
    "seeker" -> SeekerRed
    "hider" -> HiderTeal
    else -> SpectatorGrey
}
