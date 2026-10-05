package de.luca.dungeon_master_manager.data

/**
 * Runtime representation of a discovered project (config + folder path).
 */
data class ProjectInfo(
    val config: ProjectConfig,
    val path: String,
)
