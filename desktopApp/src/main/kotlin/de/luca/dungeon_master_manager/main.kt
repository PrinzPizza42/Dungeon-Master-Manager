package de.luca.dungeon_master_manager

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import de.luca.dungeon_master_manager.data.EntityPopoutRequest
import de.luca.dungeon_master_manager.ui.components.GrimoireWindowFrame
import de.luca.dungeon_master_manager.ui.theme.GrimoireTheme
import java.io.File

private data class OpenEntityPopoutWindow(
    val request: EntityPopoutRequest,
    val focusTrigger: Int = 0
)

fun main() = application {
    val openEntityWindows = remember { mutableStateListOf<OpenEntityPopoutWindow>() }
    var mainWindowTitle by remember { mutableStateOf("Dungeon Master Manager") }
    val mainWindowState = rememberWindowState(width = 1200.dp, height = 800.dp)

    val handleOpenEntityPopout: (EntityPopoutRequest) -> Unit = { request ->
        val existingIndex = openEntityWindows.indexOfFirst { it.request == request }
        if (existingIndex != -1) {
            val existing = openEntityWindows[existingIndex]
            openEntityWindows[existingIndex] = existing.copy(focusTrigger = existing.focusTrigger + 1)
        } else {
            openEntityWindows.add(OpenEntityPopoutWindow(request))
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = mainWindowTitle,
        state = mainWindowState,
        undecorated = true,
    ) {
        GrimoireWindowFrame(
            window = window,
            windowState = mainWindowState,
            minWidth = 640.dp,
            minHeight = 480.dp
        ) {
            App(
                applicationScope = this@application,
                window = window,
                windowState = mainWindowState,
                onCloseRequest = ::exitApplication,
                onTitleChange = { mainWindowTitle = it },
                onOpenEntityPopout = handleOpenEntityPopout
            )
        }
    }

    for (entry in openEntityWindows) {
        key(entry.request) {
            val entityState = rememberWindowState(width = 750.dp, height = 580.dp)
            val campaignName = if (entry.request.projectPath.isNotBlank()) {
                File(entry.request.projectPath).name
            } else "Global"
            val entityTitle = "${entry.request.entityName} - Entity ($campaignName)"

            Window(
                onCloseRequest = { openEntityWindows.removeAll { it.request == entry.request } },
                title = entityTitle,
                state = entityState,
                undecorated = true,
            ) {
                GrimoireTheme {
                    GrimoireWindowFrame(
                        window = window,
                        windowState = entityState,
                        focusTrigger = entry.focusTrigger,
                        minWidth = 400.dp,
                        minHeight = 300.dp
                    ) {
                        de.luca.dungeon_master_manager.ui.EntityPopoutScreen(
                            request = entry.request,
                            window = window,
                            windowState = entityState,
                            title = entityTitle,
                            onClose = { openEntityWindows.removeAll { it.request == entry.request } },
                            onOpenEntityPopout = handleOpenEntityPopout
                        )
                    }
                }
            }
        }
    }
}
