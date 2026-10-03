package de.luca.dungeon_master_manager

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Dungeon-Master-Manager",
    ) {
        App()
    }
}