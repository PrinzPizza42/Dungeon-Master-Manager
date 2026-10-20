package de.luca.dungeon_master_manager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

val LocalFantasyFontFamily = staticCompositionLocalOf<FontFamily> {
    FontFamily.Serif
}

val GrimoireShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

@Composable
fun GrimoireTheme(
    content: @Composable () -> Unit
) {
    val fantasyFamily = getFantasyFontFamily()
    val typography = grimoireTypography()

    CompositionLocalProvider(
        LocalFantasyFontFamily provides fantasyFamily
    ) {
        MaterialTheme(
            colorScheme = GrimoireColorScheme,
            typography = typography,
            shapes = GrimoireShapes,
            content = content
        )
    }
}
