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

fun main() = application {
    val openEntityWindows = remember { mutableStateListOf<EntityPopoutRequest>() }
    var mainWindowTitle by remember { mutableStateOf("Dungeon Master Manager") }
    val mainWindowState = rememberWindowState(width = 1200.dp, height = 800.dp)

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
                onOpenEntityPopout = { request ->
                    if (!openEntityWindows.contains(request)) {
                        openEntityWindows.add(request)
                    }
                }
            )
        }
    }

    for (request in openEntityWindows) {
        key(request) {
            val entityState = rememberWindowState(width = 750.dp, height = 580.dp)
            val campaignName = if (request.projectPath.isNotBlank()) {
                File(request.projectPath).name
            } else "Global"
            val entityTitle = "${request.entityName} - Entity ($campaignName)"

            Window(
                onCloseRequest = { openEntityWindows.remove(request) },
                title = entityTitle,
                state = entityState,
                undecorated = true,
            ) {
                GrimoireTheme {
                    GrimoireWindowFrame(
                        window = window,
                        windowState = entityState,
                        minWidth = 400.dp,
                        minHeight = 300.dp
                    ) {
                        de.luca.dungeon_master_manager.ui.EntityPopoutScreen(
                            request = request,
                            window = window,
                            windowState = entityState,
                            title = entityTitle,
                            onClose = { openEntityWindows.remove(request) },
                            onOpenEntityPopout = { newRequest ->
                                if (!openEntityWindows.contains(newRequest)) {
                                    openEntityWindows.add(newRequest)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
