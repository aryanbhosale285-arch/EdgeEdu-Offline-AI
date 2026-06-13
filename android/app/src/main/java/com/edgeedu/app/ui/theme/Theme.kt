package com.edgeedu.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * EdgeEdu visual design (ported from the "Core screens" design kit): indigo →
 * purple gradients, a teal accent, a light-lavender canvas, white rounded
 * cards, and heavy display headings. Tokens map 1:1 to the design's CSS vars.
 */
object EdgeEduColors {
    val Primary = Color(0xFF4A55E8)
    val PrimaryPurple = Color(0xFF8B5CF6)
    val Accent = Color(0xFF00C9A7)
    val AccentDark = Color(0xFF00A884)
    val Background = Color(0xFFF4F6FF)
    val Foreground = Color(0xFF11142D)
    val Card = Color(0xFFFFFFFF)
    val Secondary = Color(0xFFEEF0FE)
    val Muted = Color(0xFFE9EBF8)
    val MutedForeground = Color(0xFF6B7094)
    val InputBackground = Color(0xFFF0F2FF)
    val Border = Color(0xFFDADFF6)
    val Destructive = Color(0xFFE53E3E)
}

/** Reusable gradients matching the design's headers, pills and progress bars. */
object EdgeEduGradients {
    val Header = Brush.linearGradient(listOf(EdgeEduColors.Primary, EdgeEduColors.PrimaryPurple))
    val Accent = Brush.linearGradient(listOf(EdgeEduColors.Accent, EdgeEduColors.AccentDark))
    val Progress = Brush.linearGradient(listOf(EdgeEduColors.Primary, EdgeEduColors.Accent))
}

private val EdgeEduColorScheme = lightColorScheme(
    primary = EdgeEduColors.Primary,
    onPrimary = Color.White,
    primaryContainer = EdgeEduColors.Secondary,
    onPrimaryContainer = EdgeEduColors.Primary,
    secondary = EdgeEduColors.Accent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6FAF7),
    onSecondaryContainer = EdgeEduColors.AccentDark,
    tertiary = EdgeEduColors.PrimaryPurple,
    onTertiary = Color.White,
    background = EdgeEduColors.Background,
    onBackground = EdgeEduColors.Foreground,
    surface = EdgeEduColors.Card,
    onSurface = EdgeEduColors.Foreground,
    surfaceVariant = EdgeEduColors.Secondary,
    onSurfaceVariant = EdgeEduColors.MutedForeground,
    outline = EdgeEduColors.Border,
    outlineVariant = EdgeEduColors.Muted,
    error = EdgeEduColors.Destructive,
    onError = Color.White,
    errorContainer = Color(0xFFFCE4E4),
    onErrorContainer = EdgeEduColors.Destructive,
)

// Heavy, rounded display style mimicking Nunito/Nunito Sans from the design kit.
private val EdgeEduTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.Black),
        headlineMedium = headlineMedium.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold),
        headlineSmall = headlineSmall.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
        titleLarge = titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.Bold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold),
        labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
    )
}

private val EdgeEduShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun EdgeEduTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EdgeEduColorScheme,
        typography = EdgeEduTypography,
        shapes = EdgeEduShapes,
        content = content,
    )
}
