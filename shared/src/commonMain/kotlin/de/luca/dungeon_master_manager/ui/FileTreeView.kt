package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
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
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.DragAndDropTransferable
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
import de.luca.dungeon_master_manager.ui.components.ExplorerDropPayload
import de.luca.dungeon_master_manager.ui.components.InternalFileDragData
import de.luca.dungeon_master_manager.ui.components.InternalFileTransferable
import de.luca.dungeon_master_manager.ui.components.extractExplorerPayload
import de.luca.dungeon_master_manager.ui.components.isExplorerFileDrag
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors

/**
 * Renders a recursive file tree in ancient grimoire style with Drag and Drop support.
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
    hoveredDropFolderPath: String? = null,
    onDropTargetHoverChange: (folderPath: String?, isEntered: Boolean) -> Unit = { _, _ -> },
    onPayloadDropped: (payload: ExplorerDropPayload, targetFolderPath: String) -> Boolean = { _, _ -> false },
    onDragTransferCompleted: (filePath: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        for (child in rootNode.children) {
            FileTreeNodeRow(
                node = child,
                depth = 0,
                expandedPaths = expandedPaths,
                activeFilePath = activeFilePath,
                hoveredDropFolderPath = hoveredDropFolderPath,
                onDropTargetHoverChange = onDropTargetHoverChange,
                onPayloadDropped = onPayloadDropped,
                onDragTransferCompleted = onDragTransferCompleted,
                onNodeClick = onNodeClick,
                onRenameClick = onRenameClick,
                onMoveClick = onMoveClick,
                onDeleteClick = onDeleteClick,
                onPopOutClick = onPopOutClick,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun FileTreeNodeRow(
    node: FileNode,
    depth: Int,
    expandedPaths: Set<String>,
    activeFilePath: String?,
    hoveredDropFolderPath: String?,
    onDropTargetHoverChange: (folderPath: String?, isEntered: Boolean) -> Unit,
    onPayloadDropped: (payload: ExplorerDropPayload, targetFolderPath: String) -> Boolean,
    onDragTransferCompleted: (filePath: String) -> Unit,
    onNodeClick: (FileNode) -> Unit,
    onRenameClick: (FileNode) -> Unit,
    onMoveClick: (FileNode) -> Unit,
    onDeleteClick: (FileNode) -> Unit,
    onPopOutClick: (FileNode) -> Unit,
) {
    val isExpanded = node.path in expandedPaths
    val isActive = node.path == activeFilePath
    val isDropTargetHovered = node.isDirectory && hoveredDropFolderPath == node.path
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var showMenu by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isDropTargetHovered -> GrimoireColors.GoldContainer.copy(alpha = 0.55f)
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
        FileExtensions.isPdf(node.extension) -> "📕"
        FileExtensions.isAudio(node.extension) -> "🎵"
        FileExtensions.isArchive(node.extension) -> "📦"
        node.extension == "log" -> "📋"
        FileExtensions.isText(node.extension) -> "📄"
        else -> "📄"
    }

    val dragSourceModifier = if (!node.isDirectory && !node.isVirtualHeading) {
        Modifier.dragAndDropSource {
            DragAndDropTransferData(
                transferable = DragAndDropTransferable(
                    InternalFileTransferable(InternalFileDragData(node.path, node.name))
                ),
                supportedActions = listOf(DragAndDropTransferAction.Copy, DragAndDropTransferAction.Move),
                onTransferCompleted = {
                    onDragTransferCompleted(node.path)
                }
            )
        }
    } else {
        Modifier
    }

    val folderDropTarget = remember(node.path) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                onDropTargetHoverChange(node.path, false)
                val payload = event.extractExplorerPayload() ?: return false
                return onPayloadDropped(payload, node.path)
            }

            override fun onEntered(event: DragAndDropEvent) {
                onDropTargetHoverChange(node.path, true)
            }

            override fun onExited(event: DragAndDropEvent) {
                onDropTargetHoverChange(node.path, false)
            }

            override fun onEnded(event: DragAndDropEvent) {
                onDropTargetHoverChange(node.path, false)
            }
        }
    }

    val columnModifier = if (node.isDirectory) {
        Modifier.fillMaxWidth().dragAndDropTarget(
            shouldStartDragAndDrop = { event -> event.isExplorerFileDrag() },
            target = folderDropTarget
        )
    } else {
        Modifier.fillMaxWidth()
    }

    Column(modifier = columnModifier) {
        val rowBorderModifier = if (isDropTargetHovered) {
            Modifier.border(1.dp, GrimoireColors.GoldPrimary, RoundedCornerShape(4.dp))
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(dragSourceModifier)
                .then(rowBorderModifier)
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
                    fontWeight = if (isActive || isDropTargetHovered) FontWeight.SemiBold else FontWeight.Normal,
                    color = when {
                        isDropTargetHovered -> GrimoireColors.GoldBright
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
                    hoveredDropFolderPath = hoveredDropFolderPath,
                    onDropTargetHoverChange = onDropTargetHoverChange,
                    onPayloadDropped = onPayloadDropped,
                    onDragTransferCompleted = onDragTransferCompleted,
                    onNodeClick = onNodeClick,
                    onRenameClick = onRenameClick,
                    onMoveClick = onMoveClick,
                    onDeleteClick = onDeleteClick,
                    onPopOutClick = onPopOutClick,
                )
            }
        }
    }
}
