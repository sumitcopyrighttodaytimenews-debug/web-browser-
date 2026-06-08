package com.example.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

enum class BrowserThemeId(val displayName: String, val isDark: Boolean) {
    BOLD_TYPOGRAPHY("Bold Typography", false),
    ARCTIC("Minimalist Arctic", false),
    CYBERPUNK("Cyberpunk Neon", true),
    SAKURA("Sakura Garden", false),
    FOREST("Forest Sage", true),
    SUNSET("Sunset Glow", true),
    AMOLED("AMOLED Eclipse", true)
}

private val BoldTypographyColorScheme = lightColorScheme(
    primary = Color(0xFF21005D), // Deep rich elegant purple
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF), // Soft lavender container
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF381E72), // Medium deep purple accent
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0BCFF),
    onSecondaryContainer = Color(0xFF381E72),
    background = Color(0xFFFEF7FF), // Off-white/slate purple-tinted
    surface = Color(0xFFF3EDF7), // Neutral light lilac highlight surface
    onBackground = Color(0xFF1D1B20),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFF3EDF7),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFFCAC4D0),
    outlineVariant = Color(0xFFE7E0EC)
)

private val ArcticColorScheme = lightColorScheme(
    primary = Color(0xFF006874),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF97F0FF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF4A6267),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF4FAFB),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF191C1D),
    onSurface = Color(0xFF191C1D),
    surfaceVariant = Color(0xFFDFE4E6),
    onSurfaceVariant = Color(0xFF40484A)
)

private val CyberpunkColorScheme = darkColorScheme(
    primary = Color(0xFF00FFCC), // Neon Cyan
    onPrimary = Color(0xFF0C0714),
    primaryContainer = Color(0xFF1C003D),
    onPrimaryContainer = Color(0xFF00FFCC),
    secondary = Color(0xFFFF007F), // Neon Magenta
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF0C0714), // Pitch deep dark space
    surface = Color(0xFF1F1235), // Dark synth surface
    onBackground = Color(0xFFE2E0EC),
    onSurface = Color(0xFFE2E0EC),
    surfaceVariant = Color(0xFF332051),
    onSurfaceVariant = Color(0xFF00FFCC)
)

private val SakuraColorScheme = lightColorScheme(
    primary = Color(0xFF9C2A6A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD8EB),
    onPrimaryContainer = Color(0xFF3B0024),
    secondary = Color(0xFF745667),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFEFF6), // Mild sakura pink
    surface = Color(0xFFFFF7FA),
    onBackground = Color(0xFF201A1D),
    onSurface = Color(0xFF201A1D),
    surfaceVariant = Color(0xFFF2DDE6),
    onSurfaceVariant = Color(0xFF51434A)
)

private val ForestColorScheme = darkColorScheme(
    primary = Color(0xFF81C784), // Sage mint
    onPrimary = Color(0xFF003913),
    primaryContainer = Color(0xFF2E4E30),
    onPrimaryContainer = Color(0xFFC7F3C7),
    secondary = Color(0xFF7CB342),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF142017), // Deep pine mist
    surface = Color(0xFF202E24),
    onBackground = Color(0xFFE1ECE4),
    onSurface = Color(0xFFE1ECE4),
    surfaceVariant = Color(0xFF37493D),
    onSurfaceVariant = Color(0xFFB0C9B7)
)

private val SunsetColorScheme = darkColorScheme(
    primary = Color(0xFFFF8F00), // Tangerine
    onPrimary = Color(0xFF4D2300),
    primaryContainer = Color(0xFF6B3100),
    onPrimaryContainer = Color(0xFFFFDCC4),
    secondary = Color(0xFFE040FB), // Sunset magenta
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF1D0E25), // Twilight night
    surface = Color(0xFF2F1B3E),
    onBackground = Color(0xFFFCF7FF),
    onSurface = Color(0xFFFCF7FF),
    surfaceVariant = Color(0xFF493657),
    onSurfaceVariant = Color(0xFFECD8FF)
)

private val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF), // High contrast white
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF333333),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF8E8E93),
    onSecondary = Color(0xFF000000),
    background = Color(0xFF000000), // Zero background
    surface = Color(0xFF121212),
    onBackground = Color(0xFFF2F2F7),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFE5E5EA)
)

@Composable
fun ApexBrowserTheme(
    themeId: BrowserThemeId,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val selectedScheme = when (themeId) {
        BrowserThemeId.BOLD_TYPOGRAPHY -> BoldTypographyColorScheme
        BrowserThemeId.ARCTIC -> ArcticColorScheme
        BrowserThemeId.CYBERPUNK -> CyberpunkColorScheme
        BrowserThemeId.SAKURA -> SakuraColorScheme
        BrowserThemeId.FOREST -> ForestColorScheme
        BrowserThemeId.SUNSET -> SunsetColorScheme
        BrowserThemeId.AMOLED -> AmoledColorScheme
    }

    // Apply scale factor to modern material typography values
    val scaledTypography = Typography.copy(
        displayLarge = Typography.displayLarge.scale(fontScale),
        displayMedium = Typography.displayMedium.scale(fontScale),
        displaySmall = Typography.displaySmall.scale(fontScale),
        headlineLarge = Typography.headlineLarge.scale(fontScale),
        headlineMedium = Typography.headlineMedium.scale(fontScale),
        headlineSmall = Typography.headlineSmall.scale(fontScale),
        titleLarge = Typography.titleLarge.scale(fontScale),
        titleMedium = Typography.titleMedium.scale(fontScale),
        titleSmall = Typography.titleSmall.scale(fontScale),
        bodyLarge = Typography.bodyLarge.scale(fontScale),
        bodyMedium = Typography.bodyMedium.scale(fontScale),
        bodySmall = Typography.bodySmall.scale(fontScale),
        labelLarge = Typography.labelLarge.scale(fontScale),
        labelMedium = Typography.labelMedium.scale(fontScale),
        labelSmall = Typography.labelSmall.scale(fontScale)
    )

    MaterialTheme(
        colorScheme = selectedScheme,
        typography = scaledTypography,
        content = content
    )
}

// Extension function to scale dynamic text sizes in Jetpack Compose
private fun TextStyle.scale(factor: Float): TextStyle {
    return this.copy(
        fontSize = (this.fontSize.value * factor).sp,
        lineHeight = if (this.lineHeight.isSp) (this.lineHeight.value * factor).sp else this.lineHeight
    )
}
