package com.example.xiangqi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF2B2118)
val Paper = Color(0xFFF6EBD5)
val WoodLight = Color(0xFFE8C98E)
val WoodDark = Color(0xFFC99B5B)
val Crimson = Color(0xFFB23A2E)
val Jade = Color(0xFF2F6B5B)
val Gold = Color(0xFFC9A227)

private val LightColors = lightColorScheme(
    primary = Crimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF4D9C8),
    onPrimaryContainer = Color(0xFF4B1B12),
    secondary = Jade,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4E6DE),
    onSecondaryContainer = Color(0xFF12372C),
    tertiary = Gold,
    background = Paper,
    onBackground = Ink,
    surface = Color(0xFFFFF9EE),
    onSurface = Ink,
    surfaceVariant = Color(0xFFECDFC8),
    onSurfaceVariant = Color(0xFF6A5A44),
    outline = Color(0xFF9C8769)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE08A78),
    onPrimary = Color(0xFF3A120B),
    primaryContainer = Color(0xFF5A2419),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFF8FC4B2),
    onSecondary = Color(0xFF10352A),
    secondaryContainer = Color(0xFF254C40),
    onSecondaryContainer = Color(0xFFCFE9DF),
    tertiary = Color(0xFFE0C66A),
    background = Color(0xFF1B1611),
    onBackground = Color(0xFFEDE2D1),
    surface = Color(0xFF241D16),
    onSurface = Color(0xFFEDE2D1),
    surfaceVariant = Color(0xFF3A3025),
    onSurfaceVariant = Color(0xFFCDBBA2),
    outline = Color(0xFF8B775C)
)

@Composable
fun MiniXiangqiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
