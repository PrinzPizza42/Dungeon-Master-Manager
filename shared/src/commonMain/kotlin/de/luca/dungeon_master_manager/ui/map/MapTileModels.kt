package de.luca.dungeon_master_manager.ui.map

import kotlinx.serialization.Serializable

const val TILE_SIZE = 1024
const val LOD0_MAX_DIMENSION = 2048

@Serializable
data class LodLevelSpec(
    val level: Int,
    val scaleFactor: Float,
    val width: Int,
    val height: Int,
    val cols: Int,
    val rows: Int
)

@Serializable
data class MapPyramidManifest(
    val schemaVersion: Int = 1,
    val sourceRelativePath: String,
    val sourceFileSize: Long,
    val sourceLastModified: Long,
    val originalWidth: Int,
    val originalHeight: Int,
    val hasAlpha: Boolean,
    val tileExtension: String,
    val tileSize: Int = TILE_SIZE,
    val levels: List<LodLevelSpec>
)

data class TileKey(
    val cacheId: String,
    val level: Int,
    val row: Int,
    val col: Int
)
