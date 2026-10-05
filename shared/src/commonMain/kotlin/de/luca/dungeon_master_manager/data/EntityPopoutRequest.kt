package de.luca.dungeon_master_manager.data

data class EntityPopoutRequest(
    val entityName: String,
    val projectPath: String // We need the project path to know which DB to query, or it can be a global entity if path is something specific
)
