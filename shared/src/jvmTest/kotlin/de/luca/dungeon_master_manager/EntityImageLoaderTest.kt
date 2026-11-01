package de.luca.dungeon_master_manager

import androidx.compose.ui.graphics.toComposeImageBitmap
import de.luca.dungeon_master_manager.ui.components.EntityImageLoader
import de.luca.dungeonmastermanager.database.Entity
import kotlinx.coroutines.runBlocking
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.*

class EntityImageLoaderTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("dmm_entity_image_test").toFile()
        EntityImageLoader.clear()
    }

    @AfterTest
    fun tearDown() {
        EntityImageLoader.clear()
        tempDir.deleteRecursively()
    }

    private fun isComposeBitmapAvailable(): Boolean {
        return try {
            val bi = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            bi.toComposeImageBitmap()
            true
        } catch (_: Throwable) {
            false
        }
    }

    @Test
    fun testResolveEntityImageFile() {
        val projectDir = File(tempDir, "my_project").apply { mkdirs() }
        val projImage = File(projectDir, "hero.png").apply { writeText("img") }

        val entityWithProjImage = Entity(
            id = "hero-1",
            name = "Hero",
            type = "NPC",
            color = null,
            notesFilePath = null,
            imageFilePath = "hero.png"
        )
        val resolved = EntityImageLoader.resolveEntityImageFile(entityWithProjImage, projectDir.absolutePath)
        assertNotNull(resolved)
        assertEquals(projImage.absolutePath, resolved.absolutePath)

        val entityWithoutImage = Entity(
            id = "villain-1",
            name = "Villain",
            type = "NPC",
            color = null,
            notesFilePath = null,
            imageFilePath = null
        )
        assertNull(EntityImageLoader.resolveEntityImageFile(entityWithoutImage, projectDir.absolutePath))

        val entityBlankImage = Entity(
            id = "villain-2",
            name = "Villain",
            type = "NPC",
            color = null,
            notesFilePath = null,
            imageFilePath = "   "
        )
        assertNull(EntityImageLoader.resolveEntityImageFile(entityBlankImage, projectDir.absolutePath))
    }

    @Test
    fun testSupportedExtensions() {
        assertTrue("png" in EntityImageLoader.SUPPORTED_IMAGE_EXTENSIONS)
        assertTrue("jpg" in EntityImageLoader.SUPPORTED_IMAGE_EXTENSIONS)
        assertTrue("jpeg" in EntityImageLoader.SUPPORTED_IMAGE_EXTENSIONS)
        assertTrue("webp" in EntityImageLoader.SUPPORTED_IMAGE_EXTENSIONS)
        assertTrue("svg" in EntityImageLoader.SUPPORTED_IMAGE_EXTENSIONS)
    }

    @Test
    fun testInvalidOrMissingFileReturnsNull() = runBlocking {
        val missing = File(tempDir, "non_existent.png")
        assertNull(EntityImageLoader.loadImage(missing))
        assertNull(EntityImageLoader.getImageSync(missing))

        val corrupt = File(tempDir, "corrupt.jpg")
        corrupt.writeText("not an image")
        assertNull(EntityImageLoader.loadImage(corrupt))
    }

    @Test
    fun testLargePortraitImageSubsampledDownscaling() = runBlocking {
        if (!isComposeBitmapAvailable()) {
            println("Skipping Compose ImageBitmap test in headless environment without libGL")
            return@runBlocking
        }

        // Create a large portrait image (2000 x 3000 pixels)
        val largePortraitFile = File(tempDir, "large_hero_portrait.jpg")
        val origW = 2000
        val origH = 3000
        val bi = BufferedImage(origW, origH, BufferedImage.TYPE_INT_RGB)
        val g = bi.createGraphics()
        g.color = Color.DARK_GRAY
        g.fillRect(0, 0, origW, origH)
        g.color = Color.ORANGE
        g.fillOval(500, 200, 1000, 1200)
        g.dispose()
        ImageIO.write(bi, "jpg", largePortraitFile)

        // Load thumbnail with maxDimension = 512
        val thumbnail = EntityImageLoader.loadImage(largePortraitFile, maxDimension = 512)
        assertNotNull(thumbnail, "Thumbnail must not be null")

        // Max dimension should be <= 512
        assertTrue(thumbnail.width <= 512, "Width ${thumbnail.width} should be <= 512")
        assertTrue(thumbnail.height <= 512, "Height ${thumbnail.height} should be <= 512")
        assertEquals(512, thumbnail.height, "Portrait height should equal maxDimension 512")

        // Proportions: 2000 / 3000 = ~0.666 -> width should be ~341
        val expectedWidth = (origW * (512f / origH)).toInt()
        assertEquals(expectedWidth, thumbnail.width, "Thumbnail width should preserve aspect ratio")

        // Check that sync cache has it
        val syncThumbnail = EntityImageLoader.getImageSync(largePortraitFile, maxDimension = 512)
        assertNotNull(syncThumbnail, "Synchronous cache lookup must find the loaded thumbnail")
        assertEquals(thumbnail, syncThumbnail)
    }

    @Test
    fun testInvalidationAndClear() = runBlocking {
        if (!isComposeBitmapAvailable()) {
            println("Skipping Compose ImageBitmap test in headless environment without libGL")
            return@runBlocking
        }

        val file = File(tempDir, "portrait.png")
        val bi = BufferedImage(200, 300, BufferedImage.TYPE_INT_ARGB)
        ImageIO.write(bi, "png", file)

        val thumb128 = EntityImageLoader.loadImage(file, maxDimension = 128)
        assertNotNull(thumb128)
        assertNotNull(EntityImageLoader.getImageSync(file, maxDimension = 128))

        // Invalidate file
        EntityImageLoader.invalidate(file)
        assertNull(EntityImageLoader.getImageSync(file, maxDimension = 128))

        // Re-load and then clear all
        EntityImageLoader.loadImage(file, maxDimension = 128)
        assertNotNull(EntityImageLoader.getImageSync(file, maxDimension = 128))
        EntityImageLoader.clear()
        assertNull(EntityImageLoader.getImageSync(file, maxDimension = 128))
    }
}
