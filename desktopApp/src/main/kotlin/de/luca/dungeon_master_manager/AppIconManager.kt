package de.luca.dungeon_master_manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Taskbar
import java.awt.Window
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Manages desktop window and OS taskbar icons for Dungeon Master Manager.
 *
 * Supports:
 * - Loading from classpath resource: `/icons/app_icon.png`
 * - Loading from local project / working directory: `app_icon.png` or `icon.png`
 * - Fallback: dynamically generated high-resolution Grimoire D20 heraldic icon
 * - Multi-resolution icon list (16x16 up to 256x256) for crisp rendering on HiDPI / taskbar / Alt+Tab
 * - Native Taskbar / Dock integration via [java.awt.Taskbar]
 */
object AppIconManager {
    private const val RESOURCE_PATH = "/icons/app_icon.png"
    private val CANDIDATE_FILES = listOf(
        "app_icon.png",
        "icon.png",
        "app-icon.png",
        "icons/app_icon.png",
        "desktopApp/src/main/resources/icons/app_icon.png"
    )

    val primaryImage: BufferedImage by lazy {
        loadIconImage()
    }

    val iconImages: List<BufferedImage> by lazy {
        generateMultiResolutionList(primaryImage)
    }

    val appIconPainter: Painter by lazy {
        BitmapPainter(primaryImage.toComposeImageBitmap())
    }

    @Composable
    fun rememberAppIconPainter(): Painter {
        return remember { appIconPainter }
    }

    /**
     * Applies the application icon to the given [Window] and system [Taskbar].
     */
    fun applyToWindow(window: Window) {
        try {
            // Apply multi-resolution list to the window (used for window decorations, Alt+Tab, and taskbar)
            window.iconImages = iconImages
        } catch (_: Exception) {
            try {
                if (window is java.awt.Frame) {
                    window.iconImage = primaryImage
                }
            } catch (_: Exception) {}
        }

        // Apply to OS Taskbar / Dock if supported (e.g. macOS Dock, Windows Taskbar)
        try {
            if (Taskbar.isTaskbarSupported()) {
                val taskbar = Taskbar.getTaskbar()
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.iconImage = primaryImage
                }
            }
        } catch (_: Exception) {
            // Ignored if unsupported on the current platform / window manager
        }
    }

    private fun loadIconImage(): BufferedImage {
        // 1. Try external files (useful for direct user replacement without rebuilding)
        for (candidate in CANDIDATE_FILES) {
            val file = File(candidate)
            if (file.exists() && file.isFile) {
                try {
                    val img = ImageIO.read(file)
                    if (img != null) return img
                } catch (e: Exception) {
                    println("[AppIconManager] Warning: Failed to read external icon from $candidate: ${e.message}")
                }
            }
        }

        // 2. Try classpath resource
        try {
            val stream = AppIconManager::class.java.getResourceAsStream(RESOURCE_PATH)
                ?: Thread.currentThread().contextClassLoader.getResourceAsStream("icons/app_icon.png")
            if (stream != null) {
                stream.use { s ->
                    val img = ImageIO.read(s)
                    if (img != null) return img
                }
            }
        } catch (e: Exception) {
            println("[AppIconManager] Warning: Failed to read classpath icon from $RESOURCE_PATH: ${e.message}")
        }

        // 3. Fallback: generate a high-resolution, beautiful Grimoire D&M placeholder icon
        return generatePlaceholderIcon(256)
    }

    private fun generateMultiResolutionList(base: BufferedImage): List<BufferedImage> {
        val sizes = listOf(16, 24, 32, 48, 64, 128, 256)
        return sizes.map { size ->
            if (base.width == size && base.height == size) {
                base
            } else {
                scaleImage(base, size, size)
            }
        }
    }

    private fun scaleImage(src: BufferedImage, width: Int, height: Int): BufferedImage {
        val dest = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = dest.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.drawImage(src, 0, 0, width, height, null)
        g2d.dispose()
        return dest
    }

    /**
     * Generates a sleek, high-quality Grimoire D&M placeholder icon:
     * - Dark rounded fantasy background (#14120F)
     * - Bronze/Gold ornamental concentric borders (#C8AA6E)
     * - Golden 20-sided D20 die shape with heraldic "DM" insignia
     */
    fun generatePlaceholderIcon(size: Int = 256): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val s = size.toFloat()
        val corner = s * 0.22f

        // 1. Dark background plate
        g.color = Color(20, 18, 15) // Deep Grimoire dark
        g.fill(RoundRectangle2D.Float(s * 0.04f, s * 0.04f, s * 0.92f, s * 0.92f, corner, corner))

        // 2. Outer Bronze/Gold Border
        g.color = Color(200, 170, 110) // Grimoire GoldPrimary
        g.stroke = BasicStroke(s * 0.035f)
        g.draw(RoundRectangle2D.Float(s * 0.05f, s * 0.05f, s * 0.90f, s * 0.90f, corner * 0.9f, corner * 0.9f))

        // 3. Inner Subtle Border
        g.color = Color(140, 110, 65, 160) // Bronze Subdued
        g.stroke = BasicStroke(s * 0.015f)
        g.draw(RoundRectangle2D.Float(s * 0.10f, s * 0.10f, s * 0.80f, s * 0.80f, corner * 0.7f, corner * 0.7f))

        // 4. Hexagonal D20 Silhouette
        val centerX = s * 0.5f
        val centerY = s * 0.5f
        val radius = s * 0.32f

        val d20Path = Path2D.Float()
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60 - 30).toDouble())
            val px = (centerX + radius * Math.cos(angle)).toFloat()
            val py = (centerY + radius * Math.sin(angle)).toFloat()
            if (i == 0) d20Path.moveTo(px, py) else d20Path.lineTo(px, py)
        }
        d20Path.closePath()

        // Fill D20 facet background
        g.color = Color(35, 30, 24)
        g.fill(d20Path)

        // Draw D20 facet lines (from center triangle to vertices)
        val innerRadius = radius * 0.52f
        val innerPts = Array(3) { i ->
            val angle = Math.toRadians((i * 120 - 90).toDouble())
            Point2D.Float(
                (centerX + innerRadius * Math.cos(angle)).toFloat(),
                (centerY + innerRadius * Math.sin(angle)).toFloat()
            )
        }

        // Inner triangle
        val innerTri = Path2D.Float()
        innerTri.moveTo(innerPts[0].x, innerPts[0].y)
        innerTri.lineTo(innerPts[1].x, innerPts[1].y)
        innerTri.lineTo(innerPts[2].x, innerPts[2].y)
        innerTri.closePath()
        g.color = Color(48, 40, 30)
        g.fill(innerTri)

        // Facet wireframe
        g.color = Color(200, 170, 110, 200)
        g.stroke = BasicStroke(s * 0.02f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(d20Path)
        g.draw(innerTri)

        // Connect outer hex vertices to inner triangle
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60 - 30).toDouble())
            val px = (centerX + radius * Math.cos(angle)).toFloat()
            val py = (centerY + radius * Math.sin(angle)).toFloat()
            val innerTarget = innerPts[i / 2]
            g.drawLine(px.toInt(), py.toInt(), innerTarget.x.toInt(), innerTarget.y.toInt())
        }

        // 5. Central Heraldic Diamond Crest in Glowing Gold
        val crest = Path2D.Float()
        val crW = s * 0.09f
        val crH = s * 0.13f
        val crY = centerY + s * 0.01f
        crest.moveTo(centerX, crY - crH)
        crest.lineTo(centerX + crW, crY)
        crest.lineTo(centerX, crY + crH)
        crest.lineTo(centerX - crW, crY)
        crest.closePath()

        g.color = Color(245, 215, 140)
        g.fill(crest)

        g.color = Color(255, 240, 190)
        g.stroke = BasicStroke(s * 0.012f)
        g.draw(crest)

        // Inner gem dot
        g.color = Color(20, 18, 15)
        val dotR = s * 0.025f
        g.fill(java.awt.geom.Ellipse2D.Float(centerX - dotR, crY - dotR, dotR * 2, dotR * 2))

        g.dispose()
        return image
    }
}
