package de.luca.dungeon_master_manager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import java.awt.Cursor
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Rectangle
import java.awt.Window

/**
 * A custom window frame wrapper for undecorated Compose Desktop windows.
 *
 * When floating (not maximized), provides:
 * - A hairline Grimoire bronze accent border around the window.
 * - 8 interactive edge/corner drag handles for resizing the window via mouse.
 *
 * When maximized, edge resizing and borders are disabled to touch screen edges cleanly.
 */
@Composable
fun GrimoireWindowFrame(
    window: Window,
    windowState: WindowState,
    minWidth: Dp = 480.dp,
    minHeight: Dp = 360.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isMaximized = windowState.placement == WindowPlacement.Maximized
    val borderThickness = if (isMaximized) 0.dp else 1.dp
    val handleThickness = 6.dp
    val cornerSize = 12.dp

    LaunchedEffect(window) {
        WindowsDwmHelper.install(window)
    }

    val minW = minWidth.value.toInt()
    val minH = minHeight.value.toInt()

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (!isMaximized) {
                    Modifier.border(BorderStroke(borderThickness, GrimoireColors.BorderBronze))
                } else Modifier
            )
    ) {
        // Window Content
        content()

        // 8 Resize Handles (Only active when not maximized)
        if (!isMaximized) {
            // --- Corners ---
            // Top-Left (NW)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(cornerSize)
                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR)))
                    .resizeDragModifier(
                        window = window,
                        onDragCalc = { startMouse, startBounds, currMouse ->
                            val dx = currMouse.x - startMouse.x
                            val dy = currMouse.y - startMouse.y
                            val newW = maxOf(minW, startBounds.width - dx)
                            val actualDx = startBounds.width - newW
                            val newH = maxOf(minH, startBounds.height - dy)
                            val actualDy = startBounds.height - newH
                            window.setBounds(startBounds.x + actualDx, startBounds.y + actualDy, newW, newH)
                        }
                    )
            )

            // Top-Right (NE)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(cornerSize)
                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR)))
                    .resizeDragModifier(
                        window = window,
                        onDragCalc = { startMouse, startBounds, currMouse ->
                            val dx = currMouse.x - startMouse.x
                            val dy = currMouse.y - startMouse.y
                            val newW = maxOf(minW, startBounds.width + dx)
                            val newH = maxOf(minH, startBounds.height - dy)
                            val actualDy = startBounds.height - newH
                            window.setBounds(startBounds.x, startBounds.y + actualDy, newW, newH)
                        }
                    )
            )

            // Bottom-Left (SW)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(cornerSize)
                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR)))
                    .resizeDragModifier(
                        window = window,
                        onDragCalc = { startMouse, startBounds, currMouse ->
                            val dx = currMouse.x - startMouse.x
                            val dy = currMouse.y - startMouse.y
                            val newW = maxOf(minW, startBounds.width - dx)
                            val actualDx = startBounds.width - newW
                            val newH = maxOf(minH, startBounds.height + dy)
                            window.setBounds(startBounds.x + actualDx, startBounds.y, newW, newH)
                        }
                    )
            )

            // Bottom-Right (SE)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(cornerSize)
                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)))
                    .resizeDragModifier(
                        window = window,
                        onDragCalc = { startMouse, startBounds, currMouse ->
                            val dx = currMouse.x - startMouse.x
                            val dy = currMouse.y - startMouse.y
                            val newW = maxOf(minW, startBounds.width + dx)
                            val newH = maxOf(minH, startBounds.height + dy)
                            window.setBounds(startBounds.x, startBounds.y, newW, newH)
                        }
                    )
            )

            // --- Edges ---
            // Top (N)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = cornerSize)
                    .height(handleThickness)
                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)))
                    .resizeDragModifier(
                        window = window,
                        onDragCalc = { startMouse, startBounds, currMouse ->
                            val dy = currMouse.y - startMouse.y
                            val newH = maxOf(minH, startBounds.height - dy)
                            val actualDy = startBounds.height - newH
                            window.setBounds(startBounds.x, startBounds.y + actualDy, startBounds.width, newH)
                        }
                    )
            )

            // Bottom (S)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = cornerSize)
                    .height(handleThickness)
                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR)))
                    .resizeDragModifier(
                        window = window,
                        onDragCalc = { startMouse, startBounds, currMouse ->
                            val dy = currMouse.y - startMouse.y
                            val newH = maxOf(minH, startBounds.height + dy)
                            window.setBounds(startBounds.x, startBounds.y, startBounds.width, newH)
                        }
                    )
            )

            // Left (W)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .padding(vertical = cornerSize)
                    .width(handleThickness)
                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)))
                    .resizeDragModifier(
                        window = window,
                        onDragCalc = { startMouse, startBounds, currMouse ->
                            val dx = currMouse.x - startMouse.x
                            val newW = maxOf(minW, startBounds.width - dx)
                            val actualDx = startBounds.width - newW
                            window.setBounds(startBounds.x + actualDx, startBounds.y, newW, startBounds.height)
                        }
                    )
            )

            // Right (E)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = cornerSize)
                    .width(handleThickness)
                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)))
                    .resizeDragModifier(
                        window = window,
                        onDragCalc = { startMouse, startBounds, currMouse ->
                            val dx = currMouse.x - startMouse.x
                            val newW = maxOf(minW, startBounds.width + dx)
                            window.setBounds(startBounds.x, startBounds.y, newW, startBounds.height)
                        }
                    )
            )
        }
    }
}

/**
 * Helper to attach pointerInput drag gesture listener that tracks screen mouse coordinates.
 */
private fun Modifier.resizeDragModifier(
    window: Window,
    onDragCalc: (startMouse: Point, startBounds: Rectangle, currMouse: Point) -> Unit
): Modifier = this.pointerInput(window) {
    var startMouse = Point()
    var startBounds = Rectangle()

    detectDragGestures(
        onDragStart = {
            val pt = MouseInfo.getPointerInfo()?.location
            if (pt != null) {
                startMouse = Point(pt)
            }
            startBounds = Rectangle(window.bounds)
        },
        onDrag = { change, _ ->
            change.consume()
            val curr = MouseInfo.getPointerInfo()?.location ?: return@detectDragGestures
            onDragCalc(startMouse, startBounds, curr)
        }
    )
}
