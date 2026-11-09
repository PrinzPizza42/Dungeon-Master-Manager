package de.luca.dungeon_master_manager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import java.awt.Cursor

/**
 * An interactive divider handle that allows hovering and dragging horizontally
 * to resize the file explorer sidebar.
 */
@Composable
fun ExplorerResizeHandle(
    isDragging: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onHorizontalDrag: (dragAmountPx: Float) -> Unit,
    modifier: Modifier = Modifier,
    handleWidth: Dp = 8.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isActive = isHovered || isDragging

    val lineColor by animateColorAsState(
        targetValue = when {
            isDragging -> GrimoireColors.GoldBright
            isHovered -> GrimoireColors.GoldPrimary
            else -> GrimoireColors.BorderBronze
        },
        animationSpec = tween(140),
        label = "ExplorerResizeLineColor"
    )

    val backgroundHighlight by animateColorAsState(
        targetValue = when {
            isDragging -> GrimoireColors.GoldContainer.copy(alpha = 0.35f)
            isHovered -> GrimoireColors.GoldContainer.copy(alpha = 0.18f)
            else -> Color.Transparent
        },
        animationSpec = tween(140),
        label = "ExplorerResizeBgHighlight"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(handleWidth)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        onHorizontalDrag(dragAmount)
                    }
                )
            }
            .background(backgroundHighlight),
        contentAlignment = Alignment.Center
    ) {
        // Center divider line (1dp default, 2dp when hovered or dragging)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(if (isActive) 2.dp else 1.dp)
                .background(lineColor)
        )
    }
}
