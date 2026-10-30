package de.luca.dungeon_master_manager

import androidx.compose.ui.graphics.asSkiaBitmap
import de.luca.dungeon_master_manager.data.DefaultMarkerIcons
import de.luca.dungeon_master_manager.ui.map.MarkerIconCache
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Data
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.*

class MarkerIconCacheTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("dmm_marker_test").toFile()
        MarkerIconCache.clear()
    }

    @AfterTest
    fun tearDown() {
        MarkerIconCache.clear()
        tempDir.deleteRecursively()
    }

    @Test
    fun testSkiaSvgDomBasic() {
        val svgXml = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">
                <circle cx="50" cy="50" r="40" fill="#EF4444" stroke="#1E293B" stroke-width="4"/>
            </svg>
        """.trimIndent()
        val data = Data.makeFromBytes(svgXml.toByteArray(Charsets.UTF_8))
        val dom = SVGDOM(data)
        assertNotNull(dom)
        val surface = Surface.makeRasterN32Premul(64, 64)
        dom.setContainerSize(64f, 64f)
        dom.render(surface.canvas)
        val image = surface.makeImageSnapshot()
        assertEquals(64, image.width)
        assertEquals(64, image.height)
    }

    @Test
    fun testEnsureDefaultIcons() {
        DefaultMarkerIcons.ensureDefaultIcons(tempDir)
        val files = tempDir.listFiles()?.filter { it.extension.lowercase() == "svg" } ?: emptyList()
        assertTrue(files.size >= 11, "Expected 11 default icons, found: ${files.size}")

        // Ensure each file is valid and non-empty
        for (f in files) {
            assertTrue(f.length() > 50, "File ${f.name} should not be empty")
            val dom = SVGDOM(Data.makeFromBytes(f.readBytes()))
            assertNotNull(dom, "Should parse ${f.name}")
        }
    }

    @Test
    fun testRasterizeSvgCentering() {
        val svgXml = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">
                <circle cx="50" cy="50" r="10" fill="#EF4444"/>
            </svg>
        """.trimIndent()
        val file = File(tempDir, "center.svg")
        file.writeText(svgXml)
        
        val bitmap = MarkerIconCache.rasterizeSvg(file, 128)
        assertNotNull(bitmap)
        val skiaBmp = bitmap.asSkiaBitmap()
        val centerPixel = skiaBmp.getColor(64, 64)
        assertNotEquals(0, centerPixel, "Center pixel (64, 64) must not be empty!")
    }

    @Test
    fun testRasterizeSvgInvalid() {
        val badFile = File(tempDir, "bad.svg")
        badFile.writeText("Not a valid SVG file", Charsets.UTF_8)
        val bitmap = MarkerIconCache.rasterizeSvg(badFile, 64)
        assertNull(bitmap)
    }

    @Test
    fun testGetThumbnailSvg() = runBlocking {
        val svgFile = File(tempDir, "thumb.svg")
        svgFile.writeText(
            """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 50"><circle cx="25" cy="25" r="20" fill="#10B981"/></svg>""",
            Charsets.UTF_8
        )
        val thumb = MarkerIconCache.getThumbnail(svgFile, 48)
        assertNotNull(thumb)
        assertEquals(48, thumb.width)
        assertEquals(48, thumb.height)
    }

    @Test
    fun testGetThumbnailRasterPng() = runBlocking {
        val pngFile = File(tempDir, "test.png")
        val bi = BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB)
        val g = bi.createGraphics()
        g.color = java.awt.Color.RED
        g.fillRect(0, 0, 200, 100)
        g.dispose()
        ImageIO.write(bi, "png", pngFile)

        val thumb = MarkerIconCache.getThumbnail(pngFile, 50)
        assertNotNull(thumb)
        assertEquals(50, thumb.width)
        assertEquals(25, thumb.height)
    }

    @Test
    fun testSupportedExtensions() {
        assertTrue("png" in MarkerIconCache.SUPPORTED_EXTENSIONS)
        assertTrue("jpg" in MarkerIconCache.SUPPORTED_EXTENSIONS)
        assertTrue("jpeg" in MarkerIconCache.SUPPORTED_EXTENSIONS)
        assertTrue("webp" in MarkerIconCache.SUPPORTED_EXTENSIONS)
        assertTrue("svg" in MarkerIconCache.SUPPORTED_EXTENSIONS)
    }
}
