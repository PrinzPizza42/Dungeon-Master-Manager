package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.luca.dungeon_master_manager.data.EditorTab

/**
 * Horizontal tab bar for open files.
 */
@Composable
fun TabBar(
    tabs: List<EditorTab>,
    activeTabPath: String?,
    onTabSelect: (String) -> Unit,
    onTabClose: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(tabs, key = { it.filePath }) { tab ->
            TabItem(
                tab = tab,
                isActive = tab.filePath == activeTabPath,
                onSelect = { onTabSelect(tab.filePath) },
                onClose = { onTabClose(tab.filePath) },
            )
        }
    }
}

@Composable
private fun TabItem(
    tab: EditorTab,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor = when {
        isActive -> MaterialTheme.colorScheme.surface
        isHovered -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    Row(
        modifier = Modifier
            .hoverable(interactionSource)
            .clickable(onClick = onSelect)
            .background(backgroundColor)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            tab.fileName,
            style = MaterialTheme.typography.bodySmall,
            color = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 150.dp),
        )
        Spacer(Modifier.width(4.dp))
        // Close button
        Box(
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "✕",
                style = MaterialTheme.typography.labelSmall,
                color = if (isHovered || isActive) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                },
            )
        }
    }
}
