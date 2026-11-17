package com.nyl.mytasks.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val Light = lightColorScheme(
    primary = Color(0xFF00695C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF455A64),
    tertiary = Color(0xFF5E35B1),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF7FAF9),
    surface = Color(0xFFF7FAF9),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF4DB6AC),
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF004D40),
    onPrimaryContainer = Color(0xFFB2DFDB),
    secondary = Color(0xFFB0BEC5),
    tertiary = Color(0xFFB39DDB),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF101413),
    surface = Color(0xFF101413),
)

/** [fontScale] : multiplicateur utilisateur appliqué en plus de la taille de police système. */
@Composable
fun MyTasksTheme(fontScale: Float = 1f, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    val density = Density(base.density, base.fontScale * fontScale)
    CompositionLocalProvider(LocalDensity provides density) {
        MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
    }
}
