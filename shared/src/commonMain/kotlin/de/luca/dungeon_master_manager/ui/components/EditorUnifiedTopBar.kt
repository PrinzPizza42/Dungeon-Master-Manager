package de.luca.dungeon_master_manager.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import de.luca.dungeon_master_manager.data.EditorTab
import de.luca.dungeon_master_manager.ui.TabItem
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import java.awt.Window

/**
 * Unified top titlebar for the Campaign Editor screen.
 *
 * Merges:
 * - App heraldic emblem ("⚜")
 * - Navigation: [← Campaigns] button
 * - Campaign Name (aligned directly above the Explorer sidebar)
 * - Open file tabs (with close and pop-out controls)
 * - Draggable window region
 * - Window controls (Minimize, Maximize/Restore, Close) permanently anchored to top-right
 *
 * Spans the entire width above both the file explorer sidebar and the content area.
 */
@Composable
fun EditorUnifiedTopBar(
    window: Window,
    windowState: WindowState,
    projectName: String,
    tabs: List<EditorTab>,
    activeTabPath: String?,
    onTabSelect: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onTabPopOut: (String) -> Unit,
    onBack: () -> Unit,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onManageEntities: (() -> Unit)? = null,
    isExplorerVisible: Boolean = true,
    onToggleExplorer: (() -> Unit)? = null,
) {
    LaunchedEffect(window) {
        WindowsDwmHelper.install(window)
    }

    val isMaximized = windowState.placement == WindowPlacement.Maximized
    val toggleMaximize = {
        if (!WindowsDwmHelper.maximizeOrRestore(window, isMaximized)) {
            windowState.placement = if (isMaximized) WindowPlacement.Floating else WindowPlacement.Maximized
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(GrimoireColors.SurfaceLow)
    ) {
        // --- Left Section: Navigation & Campaign Name (Width: 260dp matching Explorer sidebar when visible) ---
        val leftSectionWidth by animateDpAsState(
            targetValue = if (isExplorerVisible) 260.dp else 220.dp,
            animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
            label = "LeftSectionWidth"
        )
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(leftSectionWidth)
                .padding(start = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fantasy Emblem
            Text(
                text = "⚜",
                color = GrimoireColors.GoldPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 4.dp)
            )

            // Back to Launcher Button
            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(contentColor = GrimoireColors.ParchmentMuted),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    "← Campaigns",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrimoireColors.ParchmentMuted
                )
            }

            // Divider between Back and Campaign Name
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .width(1.dp)
                    .height(14.dp)
                    .background(GrimoireColors.BorderBronzeSubtle)
            )

            // Campaign Name
            Text(
                text = projectName,
                style = MaterialTheme.typography.bodyMedium,
                color = GrimoireColors.GoldPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (onToggleExplorer != null) {
                IconButton(
                    onClick = onToggleExplorer,
                    modifier = Modifier.size(24.dp).padding(start = 2.dp)
                ) {
                    Icon(
                        imageVector = if (isExplorerVisible) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                        contentDescription = if (isExplorerVisible) "Hide Explorer" else "Show Explorer",
                        tint = if (isExplorerVisible) GrimoireColors.GoldPrimary else GrimoireColors.ParchmentMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Vertical divider lining up exactly with the sidebar divider if visible
        val dividerAlpha by animateFloatAsState(
            targetValue = if (isExplorerVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 180),
            label = "TopBarDividerAlpha"
        )
        if (dividerAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = leftSectionWidth)
                    .width(1.dp)
                    .fillMaxHeight()
                    .graphicsLayer { alpha = dividerAlpha }
                    .background(GrimoireColors.BorderBronze)
            )
        }

        // --- Middle Section: Tabs + Draggable Area ---
        val targetTabsStartPadding = if (isExplorerVisible) 261.dp else (220.dp + 6.dp)
        val tabsStartPadding by animateDpAsState(
            targetValue = targetTabsStartPadding,
            animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
            label = "TabsStartPadding"
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = tabsStartPadding, end = 124.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val minDragWidth = 60.dp
            val maxTabsWidth = (maxWidth - minDragWidth).coerceAtLeast(0.dp)

            if (tabs.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Scrollable row of open file tabs, taking only the width it needs up to maxTabsWidth
                    LazyRow(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = maxTabsWidth)
                            .wrapContentWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(tabs, key = { it.filePath }) { tab ->
                            TabItem(
                                tab = tab,
                                isActive = tab.filePath == activeTabPath,
                                onSelect = { onTabSelect(tab.filePath) },
                                onClose = { onTabClose(tab.filePath) },
                                onPopOut = { onTabPopOut(tab.filePath) },
                                modifier = Modifier.animateItem().fillMaxHeight(),
                            )
                        }
                    }

                    // Remaining empty space (at least 60dp, or larger if fewer tabs) is draggable
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .windowDraggable(
                                window = window,
                                windowState = windowState,
                                onDoubleTap = toggleMaximize
                            )
                    )
                }
            } else {
                // When no tabs are open, entire middle space is cleanly draggable
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowDraggable(
                            window = window,
                            windowState = windowState,
                            onDoubleTap = toggleMaximize
                        )
                )
            }
        }

        // --- Right Section: Window Controls (Permanently Pinned to CenterEnd!) ---
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

        // Bottom hairline border
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(GrimoireColors.BorderBronze)
        )
    }
}
