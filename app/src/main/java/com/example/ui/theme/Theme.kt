package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class AppThemeMode(val label: String) {
    TACTICAL_DARK("Dark"),
    CRISP_LIGHT("Light"),
    SYSTEM_DYNAMIC("Auto")
}

private val CyberDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF00232B),
    primaryContainer = CyanContainerDark,
    onPrimaryContainer = OnCyanContainerDark,
    secondary = CyberIndigo,
    onSecondary = Color(0xFF0F1436),
    secondaryContainer = CyberIndigoContainer,
    onSecondaryContainer = Color(0xFFDCE1FF),
    tertiary = ThreatSafe,
    onTertiary = Color(0xFF002914),
    tertiaryContainer = ThreatSafeBgDark,
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = CyberObsidian,
    onBackground = TextPrimaryDark,
    surface = CyberNavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CyberNavyCard,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = CyberObsidian,
    surfaceContainerLow = CyberNavySurface,
    surfaceContainer = CyberNavyCard,
    surfaceContainerHigh = CyberNavyElevated,
    outline = CyberOutlineDark,
    outlineVariant = Color(0xFF1F2D45),
    error = ThreatCritical,
    onError = Color.White,
    errorContainer = ThreatCriticalBgDark,
    onErrorContainer = Color(0xFFFFDADE)
)

private val CyberLightColorScheme = lightColorScheme(
    primary = CyberCyanLight,
    onPrimary = Color.White,
    primaryContainer = CyanContainerLight,
    onPrimaryContainer = OnCyanContainerLight,
    secondary = Color(0xFF3F51B5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF1E1B4B),
    tertiary = Color(0xFF00875A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = CyberLightBg,
    onBackground = TextPrimaryLight,
    surface = CyberLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CyberLightCard,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = CyberLightBg,
    surfaceContainer = CyberLightCard,
    surfaceContainerHigh = Color(0xFFDCE6F2),
    outline = CyberLightOutline,
    outlineVariant = Color(0xFFDCE5F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

val CyberShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CyberAiTheme(
    themeMode: AppThemeMode = AppThemeMode.TACTICAL_DARK,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val context = LocalContext.current

    val colorScheme = when (themeMode) {
        AppThemeMode.TACTICAL_DARK -> CyberDarkColorScheme
        AppThemeMode.CRISP_LIGHT -> CyberLightColorScheme
        AppThemeMode.SYSTEM_DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (systemDark) CyberDarkColorScheme else CyberLightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = CyberShapes,
        content = content
    )
}
