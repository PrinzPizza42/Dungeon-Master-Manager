package de.luca.dungeon_master_manager.data

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val isBatterySaverMode: Boolean = false,
    val editorFontSize: Int = 16
)
