package de.luca.dungeon_master_manager.data

import kotlinx.serialization.Serializable

/**
 * Stored as `.project` JSON dotfile in each project folder.
 */
@Serializable
data class ProjectConfig(
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val description: String = "",
)
