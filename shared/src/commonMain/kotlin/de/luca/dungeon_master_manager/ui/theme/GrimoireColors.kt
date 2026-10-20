package de.luca.dungeon_master_manager.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

object GrimoireColors {
    // Backgrounds
    val Background = Color(0xFF141312)
    val Surface = Color(0xFF1A1918)
    val SurfaceLow = Color(0xFF171615)
    val SurfaceCard = Color(0xFF24211C)
    val SurfaceHover = Color(0xFF2E2A23)
    val SurfaceActive = Color(0xFF353028)

    // Primary & Accent (Antique Gold & Bronze)
    val GoldPrimary = Color(0xFFC69B4C)
    val GoldBright = Color(0xFFD4AF37)
    val GoldMuted = Color(0xFFA27B38)
    val GoldContainer = Color(0xFF382C17)
    val OnGoldContainer = Color(0xFFF3E5C8)

    // Secondary
    val BronzeSecondary = Color(0xFFA27B38)
    val BronzeContainer = Color(0xFF2D2316)
    val OnBronzeContainer = Color(0xFFE8DAC2)

    // Text & Parchment
    val ParchmentLight = Color(0xFFEDE4D0)
    val ParchmentMuted = Color(0xFFA89E8C)
    val ParchmentDim = Color(0xFF7D7466)

    // Borders & Dividers
    val BorderBronze = Color(0xFF483F33)
    val BorderBronzeSubtle = Color(0xFF352E25)
    val BorderGold = Color(0xFFC69B4C)

    // Status Colors
    val CrimsonError = Color(0xFFCF4D4D)
    val ErrorContainer = Color(0xFF3A1818)
    val OnErrorContainer = Color(0xFFFFB4B4)
    val EmeraldSuccess = Color(0xFF4E9E68)
}

val GrimoireColorScheme = darkColorScheme(
    primary = GrimoireColors.GoldPrimary,
    onPrimary = GrimoireColors.Background,
    primaryContainer = GrimoireColors.GoldContainer,
    onPrimaryContainer = GrimoireColors.OnGoldContainer,

    secondary = GrimoireColors.BronzeSecondary,
    onSecondary = GrimoireColors.Background,
    secondaryContainer = GrimoireColors.BronzeContainer,
    onSecondaryContainer = GrimoireColors.OnBronzeContainer,

    background = GrimoireColors.Background,
    onBackground = GrimoireColors.ParchmentLight,

    surface = GrimoireColors.Surface,
    onSurface = GrimoireColors.ParchmentLight,
    surfaceVariant = GrimoireColors.SurfaceCard,
    onSurfaceVariant = GrimoireColors.ParchmentMuted,

    surfaceContainerLowest = GrimoireColors.Background,
    surfaceContainerLow = GrimoireColors.SurfaceLow,
    surfaceContainer = GrimoireColors.Surface,
    surfaceContainerHigh = GrimoireColors.SurfaceCard,
    surfaceContainerHighest = GrimoireColors.SurfaceHover,

    outline = GrimoireColors.BorderBronze,
    outlineVariant = GrimoireColors.BorderBronzeSubtle,

    error = GrimoireColors.CrimsonError,
    onError = Color.White,
    errorContainer = GrimoireColors.ErrorContainer,
    onErrorContainer = GrimoireColors.OnErrorContainer,
)
