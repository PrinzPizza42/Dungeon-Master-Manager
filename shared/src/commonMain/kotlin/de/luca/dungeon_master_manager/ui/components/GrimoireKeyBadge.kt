package de.luca.dungeon_master_manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.luca.dungeon_master_manager.data.KeybindingRegistry
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors

@Composable
fun GrimoireKeyBadge(
    keyLabel: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    fontSize: Int = 11
) {
    Box(
        modifier = modifier
            .background(
                color = if (isHighlighted) GrimoireColors.GoldContainer else GrimoireColors.SurfaceLow,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = if (isHighlighted) GrimoireColors.GoldPrimary else GrimoireColors.BorderBronze,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = keyLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp,
            color = if (isHighlighted) GrimoireColors.GoldBright else GrimoireColors.ParchmentLight,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun GrimoireShortcutDisplay(
    shortcutString: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    val tokens = KeybindingRegistry.parseShortcutTokens(shortcutString)
    if (tokens.isEmpty()) {
        Text(
            text = "Unbound",
            style = MaterialTheme.typography.labelSmall,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            fontSize = 11.sp,
            color = GrimoireColors.ParchmentDim,
            modifier = modifier
        )
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tokens.forEachIndexed { index, token ->
                GrimoireKeyBadge(keyLabel = token, isHighlighted = isHighlighted)
                if (index < tokens.size - 1) {
                    Text(
                        text = "+",
                        fontSize = 11.sp,
                        color = GrimoireColors.ParchmentDim,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
