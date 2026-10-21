package de.luca.dungeon_master_manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Window
import kotlin.math.abs

/**
 * Standard fantasy-themed custom titlebar for standalone windows (Launcher, Entity Popouts, Detached Editors).
 *
 * Provides:
 * - App/Fantasy heraldic emblem on the left.
 * - Draggable area across the middle with centered title text.
 * - Minimize, Maximize/Restore, and Close action buttons on the right.
 * - Double-click on draggable title to toggle maximize/restore.
 * - Dragging a maximized window down restores floating dimensions (Aero Snap).
 */
@Composable
fun GrimoireTitleBar(
    window: Window,
    windowState: WindowState,
    title: String,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
    showEmblem: Boolean = true,
    subtitle: String? = null,
) {
    LaunchedEffect(window) {
        WindowsDwmHelper.install(window)
    }

    val isMaximized = windowState.placement == WindowPlacement.Maximized
    val toggleMaximize = {
        if (!WindowsDwmHelper.maximizeOrRestore(window, isMaximized)) {
            windowState.placement = if (isMaximized) {
                WindowPlacement.Floating
            } else {
                WindowPlacement.Maximized
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(GrimoireColors.SurfaceLow)
    ) {
        // Draggable background spanning the entire titlebar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowDraggable(
                    window = window,
                    windowState = windowState,
                    onDoubleTap = toggleMaximize
                ),
            contentAlignment = Alignment.Center
        ) {
            // Centered title text
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = GrimoireColors.ParchmentLight,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 140.dp)
            )
        }

        // Left Branding & Emblem
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showEmblem) {
                Text(
                    text = "⚜",
                    color = GrimoireColors.GoldPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = GrimoireColors.ParchmentDim,
                    maxLines = 1
                )
            }
        }

        // Right Window Controls (Min, Max/Restore, Close)
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WindowControlButtons(
                window = window,
                windowState = windowState,
                onCloseRequest = onCloseRequest
            )
        }

        // Bottom 1px hairline border
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(GrimoireColors.BorderBronze)
        )
    }
}

/**
 * Minimize, Maximize/Restore, and Close buttons styled with Grimoire colors.
 */
@Composable
fun WindowControlButtons(
    window: Window,
    windowState: WindowState,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMaximized = windowState.placement == WindowPlacement.Maximized

    Row(modifier = modifier.height(34.dp)) {
        // Minimize Button
        WindowControlButton(
            text = "—",
            hoverBackground = GrimoireColors.SurfaceHover,
            hoverContentColor = GrimoireColors.GoldBright,
            normalContentColor = GrimoireColors.ParchmentMuted,
            fontSize = 11.sp,
            onClick = {
                if (!WindowsDwmHelper.minimize(window)) {
                    windowState.isMinimized = true
                }
            }
        )

        // Maximize / Restore Button
        WindowControlButton(
            text = if (isMaximized) "❐" else "□",
            hoverBackground = GrimoireColors.SurfaceHover,
            hoverContentColor = GrimoireColors.GoldBright,
            normalContentColor = GrimoireColors.ParchmentMuted,
            fontSize = if (isMaximized) 11.sp else 12.sp,
            onClick = {
                if (!WindowsDwmHelper.maximizeOrRestore(window, isMaximized)) {
                    windowState.placement = if (isMaximized) {
                        WindowPlacement.Floating
                    } else {
                        WindowPlacement.Maximized
                    }
                }
            }
        )

        // Close Button
        WindowControlButton(
            text = "✕",
            hoverBackground = GrimoireColors.CrimsonError,
            hoverContentColor = Color.White,
            normalContentColor = GrimoireColors.ParchmentMuted,
            fontSize = 11.sp,
            width = 44.dp,
            onClick = {
                if (!WindowsDwmHelper.close(window)) {
                    onCloseRequest()
                }
            }
        )
    }
}

@Composable
private fun WindowControlButton(
    text: String,
    hoverBackground: Color,
    hoverContentColor: Color,
    normalContentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    width: androidx.compose.ui.unit.Dp = 38.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val background = if (isHovered) hoverBackground else Color.Transparent
    val contentColor = if (isHovered) hoverContentColor else normalContentColor

    Box(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Modifier that enables smooth window dragging across screens, Aero Snap un-maximize on drag down,
 * and double-click to toggle maximize.
 */
fun Modifier.windowDraggable(
    window: Window,
    windowState: WindowState? = null,
    onDoubleTap: (() -> Unit)? = null
): Modifier = this.pointerInput(window) {
    var lastClickTime = 0L
    awaitEachGesture {
        val down = awaitFirstDown()
        val now = System.currentTimeMillis()
        if (now - lastClickTime < 350) {
            onDoubleTap?.invoke()
            lastClickTime = 0L
            return@awaitEachGesture
        }
        lastClickTime = now

        val startMouse = MouseInfo.getPointerInfo()?.location ?: return@awaitEachGesture
        var startLoc = Point(window.location)
        var hasUnmaximized = false

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: break
            if (!change.pressed) break
            val currentMouse = MouseInfo.getPointerInfo()?.location ?: break
            val dx = currentMouse.x - startMouse.x
            val dy = currentMouse.y - startMouse.y

            // Trigger drag after a small movement
            if (!hasUnmaximized && (abs(dx) > 3 || abs(dy) > 3)) {
                val isMaximized = (window is java.awt.Frame &&
                        (window.extendedState and java.awt.Frame.MAXIMIZED_BOTH) == java.awt.Frame.MAXIMIZED_BOTH) ||
                        (windowState?.placement == WindowPlacement.Maximized)

                if (isMaximized) {
                    // Aero Snap: Dragging down un-maximizes and restores floating dimensions
                    val screenWidth = window.width.coerceAtLeast(1)
                    val clickRatio = (startMouse.x - window.x).toFloat() / screenWidth

                    if (!WindowsDwmHelper.restore(window)) {
                        if (windowState != null) {
                            windowState.placement = WindowPlacement.Floating
                        } else if (window is java.awt.Frame) {
                            window.extendedState = java.awt.Frame.NORMAL
                        }
                    }

                    val floatingWidth = window.width
                    val newX = (currentMouse.x - (floatingWidth * clickRatio)).toInt()
                    val newY = currentMouse.y - 16
                    window.setLocation(newX, newY)
                    startLoc = Point(newX, newY)
                    hasUnmaximized = true
                    continue
                }
                hasUnmaximized = true
            }

            if (hasUnmaximized) {
                window.setLocation(startLoc.x + dx, startLoc.y + dy)
            }
        }
    }
}
