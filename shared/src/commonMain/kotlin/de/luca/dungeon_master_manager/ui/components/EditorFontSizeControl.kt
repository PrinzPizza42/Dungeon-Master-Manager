package de.luca.dungeon_master_manager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.luca.dungeon_master_manager.data.AppSettingsRepository.DEFAULT_EDITOR_FONT_SIZE
import de.luca.dungeon_master_manager.data.AppSettingsRepository.MAX_EDITOR_FONT_SIZE
import de.luca.dungeon_master_manager.data.AppSettingsRepository.MIN_EDITOR_FONT_SIZE
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors

/**
 * Stepper control for adjusting editor font size in toolbar or settings.
 * Displays [-] [size sp] [+] and resets to default when clicking the size text.
 */
@Composable
fun EditorFontSizeControl(
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decrease Button
        OutlinedButton(
            onClick = { onFontSizeChange((fontSize - 1).coerceAtLeast(MIN_EDITOR_FONT_SIZE)) },
            enabled = fontSize > MIN_EDITOR_FONT_SIZE,
            border = BorderStroke(
                1.dp,
                if (fontSize > MIN_EDITOR_FONT_SIZE) GrimoireColors.BorderBronze else GrimoireColors.BorderBronzeSubtle
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = GrimoireColors.SurfaceCard,
                contentColor = GrimoireColors.ParchmentLight,
                disabledContentColor = GrimoireColors.ParchmentDim,
                disabledContainerColor = GrimoireColors.SurfaceCard
            ),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.defaultMinSize(minWidth = 28.dp, minHeight = 30.dp)
        ) {
            Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.width(4.dp))

        // Size Display Button (Click to reset to default)
        TextButton(
            onClick = { onFontSizeChange(DEFAULT_EDITOR_FONT_SIZE) },
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (fontSize != DEFAULT_EDITOR_FONT_SIZE) GrimoireColors.GoldPrimary else GrimoireColors.ParchmentLight
            ),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 30.dp)
        ) {
            Text(
                "${fontSize}sp",
                fontSize = 12.sp,
                fontWeight = if (fontSize != DEFAULT_EDITOR_FONT_SIZE) FontWeight.Bold else FontWeight.Medium
            )
        }

        Spacer(Modifier.width(4.dp))

        // Increase Button
        OutlinedButton(
            onClick = { onFontSizeChange((fontSize + 1).coerceAtMost(MAX_EDITOR_FONT_SIZE)) },
            enabled = fontSize < MAX_EDITOR_FONT_SIZE,
            border = BorderStroke(
                1.dp,
                if (fontSize < MAX_EDITOR_FONT_SIZE) GrimoireColors.BorderBronze else GrimoireColors.BorderBronzeSubtle
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = GrimoireColors.SurfaceCard,
                contentColor = GrimoireColors.ParchmentLight,
                disabledContentColor = GrimoireColors.ParchmentDim,
                disabledContainerColor = GrimoireColors.SurfaceCard
            ),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.defaultMinSize(minWidth = 28.dp, minHeight = 30.dp)
        ) {
            Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
