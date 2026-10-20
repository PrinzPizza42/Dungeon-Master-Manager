package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.luca.dungeon_master_manager.data.FileExtensions
import de.luca.dungeon_master_manager.data.FileNode
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors

/**
 * Renders a recursive file tree in ancient grimoire style.
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
    onPopOutClick: (FileNode) -> Unit,
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
                onPopOutClick = onPopOutClick,
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
    onPopOutClick: (FileNode) -> Unit,
) {
    val isExpanded = node.path in expandedPaths
    val isActive = node.path == activeFilePath
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var showMenu by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isActive -> GrimoireColors.GoldContainer.copy(alpha = 0.45f)
        isHovered -> GrimoireColors.SurfaceHover
        else -> Color.Transparent
    }

    val icon = when {
        node.isDirectory && isExpanded -> "📂"
        node.isDirectory -> "📁"
        node.isVirtualHeading -> "#️⃣"
        FileExtensions.isMarkdown(node.extension) -> "📝"
        FileExtensions.isMap(node.extension) -> "🗺"
        FileExtensions.isImage(node.extension) -> "🖼"
        node.extension == "log" -> "📋"
        FileExtensions.isText(node.extension) -> "📄"
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
                .padding(start = (depth * 14 + 10).dp, top = 5.dp, bottom = 5.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isActive) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(GrimoireColors.GoldPrimary, RoundedCornerShape(1.dp))
                )
                Spacer(Modifier.width(4.dp))
            }

            Text(
                text = icon,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isActive -> GrimoireColors.GoldBright
                    isHovered -> GrimoireColors.ParchmentLight
                    else -> GrimoireColors.ParchmentMuted
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .background(GrimoireColors.SurfaceCard)
                .border(1.dp, GrimoireColors.BorderBronze, RoundedCornerShape(4.dp))
        ) {
            if (!node.isVirtualHeading) {
                if (!node.isDirectory) {
                    DropdownMenuItem(
                        text = { Text("Open as window", color = GrimoireColors.ParchmentLight) },
                        onClick = {
                            showMenu = false
                            onPopOutClick(node)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Rename", color = GrimoireColors.ParchmentLight) },
                    onClick = {
                        showMenu = false
                        onRenameClick(node)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Move", color = GrimoireColors.ParchmentLight) },
                    onClick = {
                        showMenu = false
                        onMoveClick(node)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = GrimoireColors.CrimsonError) },
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
                onPopOutClick = onPopOutClick,
            )
        }
    }
}
