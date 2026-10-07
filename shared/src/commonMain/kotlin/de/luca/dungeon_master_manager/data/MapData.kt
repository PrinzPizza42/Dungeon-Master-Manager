package de.luca.dungeon_master_manager.data

import kotlinx.serialization.Serializable

@Serializable
data class MapMarker(
    val id: String, // UUID
    val x: Float, // Normalized 0.0 to 1.0
    val y: Float, // Normalized 0.0 to 1.0
    val isEntityLinked: Boolean,
    
    // For Entity-Linked
    val entityId: String? = null,
    
    // For Standalone
    val title: String? = null,
    val notes: String? = null,
    val colorULong: String? = null, // e.g. "4294901760" for Color.value
    
    // Shared
    val iconFileName: String? = null // e.g. "skull.png". If null, render as dot.
)

@Serializable
data class MapData(
    val imagePath: String, // Relative to project root
    val markers: List<MapMarker> = emptyList()
)
