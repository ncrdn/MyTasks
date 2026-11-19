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

/**
 * Palette : fonds neutres (gris clair / graphite) pour que l'indigo, seule couleur d'accent de l'interface,
 * ressorte au lieu de se fondre dans un fond bleuté. Les types de tâches gardent leur propre couleur.
 */
object Palette {
    val Ink = Color(0xFF1B1B21)
    val InkMuted = Color(0xFF5F5F6B)
    val Ground = Color(0xFFF5F5F7)
    val Indigo = Color(0xFF4F46E5)
    val IndigoSoft = Color(0xFFE8E7FD)
    val IndigoOnDark = Color(0xFF9C97FF)
    val OnIndigoMuted = Color(0xFFDCDAFA)

    /** Barre de navigation flottante : encre en clair, graphite surélevé en sombre. */
    val NavLight = Color(0xFF1B1B21)
    val NavDark = Color(0xFF2E2E35)
    val NavIcon = Color(0xFFB4B4BF)
}

private val Light = lightColorScheme(
    primary = Palette.Indigo,
    onPrimary = Color.White,
    primaryContainer = Palette.IndigoSoft,
    onPrimaryContainer = Color(0xFF2B2599),
    secondary = Palette.Ink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E9EE),
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
    outline = Color(0xFF8A8A96),
    outlineVariant = Color(0xFFDEDEE4),
    // Cartes blanches sur fond gris ; les champs de saisie (« Lowest ») sont légèrement grisés dans les cartes.
    surfaceContainerLowest = Color(0xFFF1F1F4),
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEDEDF1),
    surfaceContainerHigh = Color(0xFFE6E6EB),
    surfaceContainerHighest = Color(0xFFDFDFE5),
)

private val Dark = darkColorScheme(
    primary = Palette.IndigoOnDark,
    onPrimary = Color(0xFF15123F),
    primaryContainer = Color(0xFF3A3680),
    onPrimaryContainer = Color(0xFFE3E1FF),
    secondary = Color(0xFFECECF1),
    onSecondary = Color(0xFF1B1B21),
    secondaryContainer = Color(0xFF33333B),
    onSecondaryContainer = Color(0xFFECECF1),
    tertiary = Color(0xFFFF7AC6),
    tertiaryContainer = Color(0xFF5E0F43),
    onTertiaryContainer = Color(0xFFFFD9EE),
    error = Color(0xFFFF6B76),
    errorContainer = Color(0xFF5A1A21),
    onErrorContainer = Color(0xFFFFD6D9),
    background = Color(0xFF111114),
    onBackground = Color(0xFFECECF1),
    surface = Color(0xFF111114),
    onSurface = Color(0xFFECECF1),
    onSurfaceVariant = Color(0xFFA6A6B2),
    outline = Color(0xFF7E7E8A),
    outlineVariant = Color(0xFF3B3B44),
    // Graphite neutre : cartes plus claires que le fond, champs plus sombres que les cartes.
    surfaceContainerLowest = Color(0xFF17171B),
    surfaceContainerLow = Color(0xFF1F1F24),
    surfaceContainer = Color(0xFF26262C),
    surfaceContainerHigh = Color(0xFF2E2E35),
    surfaceContainerHighest = Color(0xFF37373F),
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

/** Couleur de la barre de navigation flottante selon le thème. */
@Composable
fun navBarColor(): Color = if (isSystemInDarkTheme()) Palette.NavDark else Palette.NavLight

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
