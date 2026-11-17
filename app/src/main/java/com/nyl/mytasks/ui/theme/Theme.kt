package com.nyl.mytasks.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Palette « indigo » : fond lavande très clair, cartes blanches, barre de navigation encre. */
object Palette {
    val Ink = Color(0xFF16143A)
    val InkMuted = Color(0xFF62607F)
    val Ground = Color(0xFFF4F3FA)
    val Indigo = Color(0xFF4F46E5)
    val IndigoSoft = Color(0xFFE8E7FD)
    val IndigoOnDark = Color(0xFF9C97FF)
    val OnIndigoMuted = Color(0xFFDCDAFA)
}

private val Light = lightColorScheme(
    primary = Palette.Indigo,
    onPrimary = Color.White,
    primaryContainer = Palette.IndigoSoft,
    onPrimaryContainer = Color(0xFF2B2599),
    secondary = Palette.Ink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E4F4),
    onSecondaryContainer = Palette.Ink,
    tertiary = Color(0xFFA12474),
    tertiaryContainer = Color(0xFFFCE4F2),
    onTertiaryContainer = Color(0xFF5E0F43),
    error = Color(0xFFB3202C),
    errorContainer = Color(0xFFFDE8EA),
    onErrorContainer = Color(0xFFB3202C),
    background = Palette.Ground,
    onBackground = Palette.Ink,
    surface = Palette.Ground,
    onSurface = Palette.Ink,
    onSurfaceVariant = Palette.InkMuted,
    outline = Color(0xFF8A88A8),
    outlineVariant = Color(0xFFE1DFF0),
    surfaceContainerLowest = Color(0xFFFAFAFE),
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEFEEF8),
    surfaceContainerHigh = Color(0xFFE8E7F3),
    surfaceContainerHighest = Color(0xFFE1E0EE),
)

private val Dark = darkColorScheme(
    primary = Palette.IndigoOnDark,
    onPrimary = Color(0xFF15123F),
    primaryContainer = Color(0xFF2E2A7A),
    onPrimaryContainer = Color(0xFFE0DEFF),
    secondary = Color(0xFFF1F0FF),
    onSecondary = Color(0xFF16143A),
    secondaryContainer = Color(0xFF2A2850),
    onSecondaryContainer = Color(0xFFF1F0FF),
    tertiary = Color(0xFFFF7AC6),
    tertiaryContainer = Color(0xFF5E0F43),
    onTertiaryContainer = Color(0xFFFFD9EE),
    error = Color(0xFFFF6B76),
    errorContainer = Color(0xFF5A1A21),
    onErrorContainer = Color(0xFFFFD6D9),
    background = Color(0xFF100F24),
    onBackground = Color(0xFFF1F0FF),
    surface = Color(0xFF100F24),
    onSurface = Color(0xFFF1F0FF),
    onSurfaceVariant = Color(0xFFA9A7CC),
    outline = Color(0xFF7C7A9E),
    outlineVariant = Color(0xFF34325A),
    surfaceContainerLowest = Color(0xFF16152E),
    surfaceContainerLow = Color(0xFF1B1A38),
    surfaceContainer = Color(0xFF211F42),
    surfaceContainerHigh = Color(0xFF29274D),
    surfaceContainerHighest = Color(0xFF312F58),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.Bold),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.Bold),
        labelMedium = t.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = t.labelSmall.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Titre d'écran en grand (remplace la barre d'application classique). */
val ScreenTitleStyle = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)

/** [fontScale] : multiplicateur utilisateur appliqué en plus de la taille de police système. */
@Composable
fun MyTasksTheme(fontScale: Float = 1f, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    val density = Density(base.density, base.fontScale * fontScale)
    CompositionLocalProvider(LocalDensity provides density) {
        MaterialTheme(
            colorScheme = if (isSystemInDarkTheme()) Dark else Light,
            shapes = AppShapes,
            typography = AppTypography,
            content = content,
        )
    }
}
