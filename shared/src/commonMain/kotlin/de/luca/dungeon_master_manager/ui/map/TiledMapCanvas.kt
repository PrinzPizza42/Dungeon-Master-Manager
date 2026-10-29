package de.luca.dungeon_master_manager.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.floor

@Composable
fun TiledMapCanvas(
    tileState: MapTileState,
    mapWidthPx: Float,
    mapHeightPx: Float,
    scaleProvider: () -> Float,
    offsetProvider: () -> Offset,
    viewportSize: IntSize,
    modifier: Modifier = Modifier
) {
    val lod0 = tileState.lod0Bitmap
    if (lod0 == null) {
        Box(
            modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp))
        }
        return
    }

    val manifest = tileState.manifest
    val cacheDir = tileState.cacheDir

    var tilesLoadedVersion by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Keep track of requested keys to avoid duplicate launches
    val requestedKeys = remember { mutableSetOf<TileKey>() }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Reference tilesLoadedVersion so Canvas redraws when a new tile arrives
        val _version = tilesLoadedVersion

        if (mapWidthPx <= 0f || mapHeightPx <= 0f) return@Canvas

        val currentScale = scaleProvider()
        val renderedW = mapWidthPx * currentScale
        val isOverview = manifest == null || manifest.levels.size <= 1 || renderedW <= lod0.width * 1.05f

        if (isOverview || cacheDir == null) {
            // Draw LOD 0 overview in 1 single draw call (Zero cache-thrashing when zoomed out!)
            drawImage(
                image = lod0,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
            )
            return@Canvas
        }

        // Choose optimal pyramid level
        val tileLevels = manifest.levels.filter { it.level > 0 }
        val chosenLevel = tileLevels.firstOrNull { it.width >= renderedW * 0.85f } ?: tileLevels.last()

        val vw = viewportSize.width.toFloat()
        val vh = viewportSize.height.toFloat()
        val pan = offsetProvider()

        val mapLeft = vw / 2f + pan.x - (mapWidthPx * currentScale) / 2f
        val mapTop = vh / 2f + pan.y - (mapHeightPx * currentScale) / 2f
        val mapTotalW = mapWidthPx * currentScale
        val mapTotalH = mapHeightPx * currentScale

        val preloadNormX = (manifest.tileSize.toFloat() / chosenLevel.width) * 0.5f
        val preloadNormY = (manifest.tileSize.toFloat() / chosenLevel.height) * 0.5f

        val normLeft = (((0f - mapLeft) / mapTotalW) - preloadNormX).coerceIn(0f, 1f)
        val normRight = (((vw - mapLeft) / mapTotalW) + preloadNormX).coerceIn(0f, 1f)
        val normTop = (((0f - mapTop) / mapTotalH) - preloadNormY).coerceIn(0f, 1f)
        val normBottom = (((vh - mapTop) / mapTotalH) + preloadNormY).coerceIn(0f, 1f)

        val firstCol = floor(normLeft * chosenLevel.width / manifest.tileSize).toInt().coerceIn(0, chosenLevel.cols - 1)
        val lastCol = floor((normRight * chosenLevel.width - 0.0001f) / manifest.tileSize).toInt().coerceIn(0, chosenLevel.cols - 1)
        val firstRow = floor(normTop * chosenLevel.height / manifest.tileSize).toInt().coerceIn(0, chosenLevel.rows - 1)
        val lastRow = floor((normBottom * chosenLevel.height - 0.0001f) / manifest.tileSize).toInt().coerceIn(0, chosenLevel.rows - 1)

        val cacheId = cacheDir.name

        for (r in firstRow..lastRow) {
            for (c in firstCol..lastCol) {
                val tileW = minOf(manifest.tileSize, chosenLevel.width - c * manifest.tileSize)
                val tileH = minOf(manifest.tileSize, chosenLevel.height - r * manifest.tileSize)

                val dstLeft = (c * manifest.tileSize.toFloat() / chosenLevel.width) * mapWidthPx
                val dstTop = (r * manifest.tileSize.toFloat() / chosenLevel.height) * mapHeightPx
                val dstW = (tileW.toFloat() / chosenLevel.width) * mapWidthPx
                val dstH = (tileH.toFloat() / chosenLevel.height) * mapHeightPx

                val key = TileKey(cacheId, chosenLevel.level, r, c)
                val tileBmp = MapTileCacheManager.memoryCache[key]

                if (tileBmp != null) {
                    drawImage(
                        image = tileBmp,
                        dstOffset = IntOffset(dstLeft.toInt(), dstTop.toInt()),
                        dstSize = IntSize(dstW.toInt().coerceAtLeast(1), dstH.toInt().coerceAtLeast(1))
                    )
                } else {
                    // Fallback to corresponding slice from lod0
                    val srcLeft = (c * manifest.tileSize.toFloat() / chosenLevel.width) * lod0.width
                    val srcTop = (r * manifest.tileSize.toFloat() / chosenLevel.height) * lod0.height
                    val srcW = (tileW.toFloat() / chosenLevel.width) * lod0.width
                    val srcH = (tileH.toFloat() / chosenLevel.height) * lod0.height

                    drawImage(
                        image = lod0,
                        srcOffset = IntOffset(srcLeft.toInt().coerceIn(0, lod0.width - 1), srcTop.toInt().coerceIn(0, lod0.height - 1)),
                        srcSize = IntSize(srcW.toInt().coerceAtLeast(1), srcH.toInt().coerceAtLeast(1)),
                        dstOffset = IntOffset(dstLeft.toInt(), dstTop.toInt()),
                        dstSize = IntSize(dstW.toInt().coerceAtLeast(1), dstH.toInt().coerceAtLeast(1))
                    )

                    // Request async load if not already in flight
                    if (requestedKeys.add(key)) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val loaded = MapTileCacheManager.getTileBitmapAsync(cacheDir, manifest, key)
                            if (loaded != null) {
                                tilesLoadedVersion++
                            }
                        }
                    }
                }
            }
        }
    }
}
