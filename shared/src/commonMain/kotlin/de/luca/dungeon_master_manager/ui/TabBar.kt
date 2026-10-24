package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.luca.dungeon_master_manager.data.EditorTab
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors

/**
 * Horizontal tab bar for open tabs.
 */
@Composable
fun TabBar(
    tabs: List<EditorTab>,
    activeTabPath: String?,
    onTabSelect: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onTabPopOut: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(GrimoireColors.SurfaceLow)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(tabs, key = { it.filePath }) { tab ->
                TabItem(
                    tab = tab,
                    isActive = tab.filePath == activeTabPath,
                    onSelect = { onTabSelect(tab.filePath) },
                    onClose = { onTabClose(tab.filePath) },
                    onPopOut = { onTabPopOut(tab.filePath) },
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GrimoireColors.BorderBronze)
        )
    }
}

@Composable
internal fun TabItem(
    tab: EditorTab,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onPopOut: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor = when {
        isActive -> GrimoireColors.SurfaceCard
        isHovered -> GrimoireColors.SurfaceHover
        else -> GrimoireColors.SurfaceLow
    }

    Box(
        modifier = Modifier
            .hoverable(interactionSource)
            .clickable(onClick = onSelect)
            .background(backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Open as window button
            IconButton(
                onClick = onPopOut,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = "Open as window",
                    tint = if (isActive) GrimoireColors.GoldPrimary else GrimoireColors.ParchmentDim,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = tab.fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isActive -> GrimoireColors.ParchmentLight
                    isHovered -> GrimoireColors.GoldBright
                    else -> GrimoireColors.ParchmentMuted
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp),
            )
            Spacer(Modifier.width(6.dp))
            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Tab",
                    tint = if (isHovered || isActive) GrimoireColors.ParchmentMuted else GrimoireColors.ParchmentDim,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        // Active bottom gold accent indicator
        if (isActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .background(GrimoireColors.GoldPrimary)
            )
        }
    }
}
