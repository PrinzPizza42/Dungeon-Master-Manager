package de.luca.dungeon_master_manager

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import de.luca.dungeon_master_manager.ui.map.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.math.floor
import kotlin.test.*

class MapTileEngineTest {

    @Test
    fun testCacheDirResolution() {
        val tempProj = Files.createTempDirectory("dmm_proj_test").toFile()
        try {
            val mapFile = File(tempProj, "Maps/epic_dungeon.png")
            val cacheDir = MapTileCacheManager.getCacheDir(tempProj.absolutePath, mapFile)
            assertTrue(cacheDir.absolutePath.startsWith(File(tempProj, ".map-cache").absolutePath))
            assertTrue(cacheDir.name.startsWith("epic_dungeon_"))
        } finally {
            tempProj.deleteRecursively()
        }
    }

    @Test
    fun testTileMemoryCacheEviction() {
        // 1MB limit for testing
        val cache = TileMemoryCache(maxSizeBytes = 1024 * 1024)

        // 512x512 bitmap = 512 * 512 * 4 = 1,048,576 bytes (~1MB)
        val img1 = BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB).toComposeImageBitmap()
        val img2 = BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB).toComposeImageBitmap()

        val key1 = TileKey("map1", 1, 0, 0)
        val key2 = TileKey("map1", 1, 0, 1)

        cache.put(key1, img1)
        assertNotNull(cache[key1])

        // Putting second image exceeds 1MB limit, should evict key1
        cache.put(key2, img2)
        assertNotNull(cache[key2])
        assertNull(cache[key1])
    }

    @Test
    fun testPyramidGenerationAndManifest() = runBlocking {
        val tempProj = Files.createTempDirectory("dmm_pyramid_test").toFile()
        try {
            val mapsDir = File(tempProj, "Maps").also { it.mkdirs() }
            val mapFile = File(mapsDir, "large_world.png")

            // Create a 2400x1600 image (exceeds 2048)
            val buffered = BufferedImage(2400, 1600, BufferedImage.TYPE_INT_ARGB)
            val g = buffered.createGraphics()
            g.color = Color.BLUE
            g.fillRect(0, 0, 2400, 1600)
            g.color = Color.RED
            g.fillRect(200, 200, 400, 400)
            g.dispose()
            ImageIO.write(buffered, "png", mapFile)

            val stateFlow = MapTileCacheManager.getOrLoadState(tempProj.absolutePath, mapFile, this)

            // Wait for generation to complete
            var state = stateFlow.first { it.manifest != null && it.lod0Bitmap != null }
            assertNotNull(state.lod0Bitmap)
            assertNotNull(state.manifest)

            val manifest = state.manifest!!
            assertEquals(2400, manifest.originalWidth)
            assertEquals(1600, manifest.originalHeight)
            assertTrue(manifest.hasAlpha)
            assertEquals("png", manifest.tileExtension)

            // LOD 0 should be scaled down to <= 2048
            assertTrue(manifest.levels.isNotEmpty())
            val lod0 = manifest.levels.first { it.level == 0 }
            assertTrue(lod0.width <= 2048)
            assertTrue(lod0.height <= 2048)

            // Wait until full generation finishes
            state = stateFlow.first { !it.isGenerating && it.generationProgress >= 1.0f }
            val cacheDir = state.cacheDir!!
            assertTrue(File(cacheDir, "manifest.json").exists())
            assertTrue(File(cacheDir, "lod0.png").exists())

            // Verify tile files were created for higher level
            val highestLevel = manifest.levels.maxByOrNull { it.level }!!
            val tileDir = File(cacheDir, "tiles/z${highestLevel.level}")
            assertTrue(tileDir.exists())
            assertTrue(File(tileDir, "r0_c0.png").exists())

            // Verify valid manifest is returned on immediate reload
            val reloadedManifest = MapTileCacheManager.getValidManifest(cacheDir, mapFile)
            assertNotNull(reloadedManifest)
            assertEquals(manifest.sourceFileSize, reloadedManifest.sourceFileSize)

            // Modify file length / timestamp -> should invalidate manifest
            mapFile.setLastModified(mapFile.lastModified() + 5000)
            val invalidated = MapTileCacheManager.getValidManifest(cacheDir, mapFile)
            assertNull(invalidated)
        } finally {
            tempProj.deleteRecursively()
        }
    }

    @Test
    fun testViewportCullingMath() {
        // Map 4096x2048, Tile size 1024 => 4 cols, 2 rows
        val totalW = 4096
        val totalH = 2048
        val tileSize = 1024
        val cols = 4
        val rows = 2

        // Visible range covering top-left corner only (normLeft=0, normRight=0.25, normTop=0, normBottom=0.5)
        val normLeft = 0.0f
        val normRight = 0.25f
        val normTop = 0.0f
        val normBottom = 0.5f

        val firstCol = floor(normLeft * totalW / tileSize).toInt().coerceIn(0, cols - 1)
        val lastCol = floor((normRight * totalW - 0.0001f) / tileSize).toInt().coerceIn(0, cols - 1)
        val firstRow = floor(normTop * totalH / tileSize).toInt().coerceIn(0, rows - 1)
        val lastRow = floor((normBottom * totalH - 0.0001f) / tileSize).toInt().coerceIn(0, rows - 1)

        assertEquals(0, firstCol)
        assertEquals(0, lastCol) // Only col 0
        assertEquals(0, firstRow)
        assertEquals(0, lastRow) // Only row 0

        // Visible range covering center
        val centerNormLeft = 0.3f
        val centerNormRight = 0.7f
        val centerFirstCol = floor(centerNormLeft * totalW / tileSize).toInt().coerceIn(0, cols - 1)
        val centerLastCol = floor((centerNormRight * totalW - 0.0001f) / tileSize).toInt().coerceIn(0, cols - 1)

        assertEquals(1, centerFirstCol)
        assertEquals(2, centerLastCol)
    }
}
