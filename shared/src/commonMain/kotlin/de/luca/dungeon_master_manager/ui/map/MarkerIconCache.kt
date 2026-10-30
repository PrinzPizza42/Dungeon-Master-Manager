package de.luca.dungeon_master_manager.ui.map

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import de.luca.dungeon_master_manager.data.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Data
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

object MarkerIconCache {
    val SUPPORTED_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "svg")

    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
    private val thumbnailCache = ConcurrentHashMap<String, ImageBitmap>()

    val iconsDir: File
        get() = ProjectRepository.globalIconsDir

    fun isSvg(file: File): Boolean = file.extension.lowercase() == "svg"

    fun rasterizeSvg(file: File, targetDimension: Int = 128): ImageBitmap? {
        return try {
            val bytes = file.readBytes()
            val data = Data.makeFromBytes(bytes)
            val dom = SVGDOM(data)
            val root = dom.root
            val vb = root?.viewBox
            val naturalW = if (vb != null && vb.width > 0f) vb.width
                else (root?.width?.value?.takeIf { it > 0f } ?: 100f)
            val naturalH = if (vb != null && vb.height > 0f) vb.height
                else (root?.height?.value?.takeIf { it > 0f } ?: 100f)

            val surface = Surface.makeRasterN32Premul(targetDimension, targetDimension)
            val canvas = surface.canvas

            val scale = minOf(targetDimension.toFloat() / naturalW, targetDimension.toFloat() / naturalH)
            val scaledW = naturalW * scale
            val scaledH = naturalH * scale
            val dx = (targetDimension - scaledW) / 2f
            val dy = (targetDimension - scaledH) / 2f

            canvas.save()
            canvas.translate(dx, dy)
            canvas.scale(scale, scale)
            dom.setContainerSize(naturalW, naturalH)
            dom.render(canvas)
            canvas.restore()

            surface.makeImageSnapshot().toComposeImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getIcon(iconFileName: String): ImageBitmap? = withContext(Dispatchers.IO) {
        iconCache[iconFileName]?.let { return@withContext it }
        val iconFile = File(iconsDir, iconFileName)
        if (!iconFile.exists()) return@withContext null

        try {
            if (isSvg(iconFile)) {
                val bitmap = rasterizeSvg(iconFile, 128)
                if (bitmap != null) {
                    iconCache[iconFileName] = bitmap
                }
                bitmap
            } else {
                iconFile.inputStream().use { stream ->
                    val bitmap = loadImageBitmap(stream)
                    iconCache[iconFileName] = bitmap
                    bitmap
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getIconSync(iconFileName: String): ImageBitmap? {
        return iconCache[iconFileName]
    }

    suspend fun getThumbnail(file: File, maxDimension: Int = 128): ImageBitmap? = withContext(Dispatchers.IO) {
        val key = "${file.absolutePath}_${file.lastModified()}_$maxDimension"
        thumbnailCache[key]?.let { return@withContext it }
        if (!file.exists()) return@withContext null

        if (isSvg(file)) {
            val bmp = rasterizeSvg(file, maxDimension)
            if (bmp != null) {
                thumbnailCache[key] = bmp
            }
            return@withContext bmp
        }

        try {
            val src = ImageIO.read(file) ?: return@withContext null
            val w = src.width
            val h = src.height
            if (w <= maxDimension && h <= maxDimension) {
                val bmp = src.toComposeImageBitmap()
                thumbnailCache[key] = bmp
                return@withContext bmp
            }

            val scale = maxDimension.toFloat() / maxOf(w, h)
            val targetW = (w * scale).toInt().coerceAtLeast(1)
            val targetH = (h * scale).toInt().coerceAtLeast(1)

            val scaled = BufferedImage(
                targetW,
                targetH,
                if (src.colorModel.hasAlpha()) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
            )
            val g2d = scaled.createGraphics()
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2d.drawImage(src, 0, 0, targetW, targetH, null)
            g2d.dispose()

            val bmp = scaled.toComposeImageBitmap()
            thumbnailCache[key] = bmp
            bmp
        } catch (_: Exception) {
            try {
                file.inputStream().use { stream ->
                    val bmp = loadImageBitmap(stream)
                    thumbnailCache[key] = bmp
                    bmp
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    fun invalidate(iconFileName: String) {
        iconCache.remove(iconFileName)
        val file = File(iconsDir, iconFileName)
        thumbnailCache.keys.removeIf { it.startsWith(file.absolutePath) }
    }

    fun clear() {
        iconCache.clear()
        thumbnailCache.clear()
    }
}
