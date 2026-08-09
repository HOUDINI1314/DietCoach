package com.dietcoach.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

private val Forest = Color(0xFF1B4332)
private val Moss = Color(0xFF2D6A4F)
private val Leaf = Color(0xFF40916C)
private val Mist = Color(0xFFD8F3DC)
private val Sand = Color(0xFFF7F5F0)
private val Ink = Color(0xFF1A1C19)
private val Ember = Color(0xFFBC4749)

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Mist,
    onPrimaryContainer = Forest,
    secondary = Moss,
    onSecondary = Color.White,
    tertiary = Ember,
    background = Sand,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8EDE8),
    onSurfaceVariant = Color(0xFF3F4943)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF95D5B2),
    onPrimary = Color(0xFF003822),
    primaryContainer = Moss,
    onPrimaryContainer = Mist,
    secondary = Leaf,
    onSecondary = Color.White,
    tertiary = Color(0xFFFFB4B0),
    background = Color(0xFF101412),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF1A1C19),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF3F4943),
    onSurfaceVariant = Color(0xFFBFC9C2)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)

@Composable
fun DietCoachTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
