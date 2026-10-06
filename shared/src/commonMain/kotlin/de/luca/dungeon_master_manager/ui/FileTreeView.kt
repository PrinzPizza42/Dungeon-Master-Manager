package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.luca.dungeon_master_manager.data.FileNode

import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background

/**
 * Renders a recursive file tree. Directories are expandable, files are clickable.
 */
@Composable
fun FileTreeView(
    rootNode: FileNode,
    expandedPaths: Set<String>,
    activeFilePath: String?,
    onNodeClick: (FileNode) -> Unit,
    onRenameClick: (FileNode) -> Unit,
    onMoveClick: (FileNode) -> Unit,
    onDeleteClick: (FileNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        for (child in rootNode.children) {
            FileTreeNodeRow(
                node = child,
                depth = 0,
                expandedPaths = expandedPaths,
                activeFilePath = activeFilePath,
                onNodeClick = onNodeClick,
                onRenameClick = onRenameClick,
                onMoveClick = onMoveClick,
                onDeleteClick = onDeleteClick,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FileTreeNodeRow(
    node: FileNode,
    depth: Int,
    expandedPaths: Set<String>,
    activeFilePath: String?,
    onNodeClick: (FileNode) -> Unit,
    onRenameClick: (FileNode) -> Unit,
    onMoveClick: (FileNode) -> Unit,
    onDeleteClick: (FileNode) -> Unit,
) {
    val isExpanded = node.path in expandedPaths
    val isActive = node.path == activeFilePath
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var showMenu by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        isHovered -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    }

    val icon = when {
        node.isDirectory && isExpanded -> "📂"
        node.isDirectory -> "📁"
        node.isVirtualHeading -> "#️⃣"
        node.extension == "md" -> "📝"
        node.extension == "map" -> "🗺"
        node.extension in setOf("png", "jpg", "jpeg", "gif", "bmp") -> "🖼"
        else -> "📄"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.button == PointerButton.Secondary) {
                            showMenu = true
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(interactionSource)
                .clickable { onNodeClick(node) }
                .background(backgroundColor)
                .padding(start = (depth * 16 + 8).dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                icon,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text(
                node.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            if (!node.isVirtualHeading) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        showMenu = false
                        onRenameClick(node)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Move") },
                    onClick = {
                        showMenu = false
                        onMoveClick(node)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showMenu = false
                        onDeleteClick(node)
                    }
                )
            }
        }
    }

    // Render children if expanded
    if (node.isDirectory && isExpanded) {
        for (child in node.children) {
            FileTreeNodeRow(
                node = child,
                depth = depth + 1,
                expandedPaths = expandedPaths,
                activeFilePath = activeFilePath,
                onNodeClick = onNodeClick,
                onRenameClick = onRenameClick,
                onMoveClick = onMoveClick,
                onDeleteClick = onDeleteClick,
            )
        }
    }
}
