package de.luca.dungeon_master_manager

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf

fun main() = application {
    val openEntityWindows = remember { mutableStateListOf<de.luca.dungeon_master_manager.data.EntityPopoutRequest>() }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Dungeon-Master-Manager",
    ) {
        App(
            onOpenEntityPopout = { request ->
                if (!openEntityWindows.contains(request)) {
                    openEntityWindows.add(request)
                }
            }
        )
    }

    for (request in openEntityWindows) {
        Window(
            onCloseRequest = { openEntityWindows.remove(request) },
            title = request.entityName,
        ) {
            de.luca.dungeon_master_manager.ui.EntityPopoutScreen(
                request = request,
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