package de.luca.dungeon_master_manager.ui.map

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

object MarkerIconCache {
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
    private val thumbnailCache = ConcurrentHashMap<String, ImageBitmap>()

    private val iconsDir: File by lazy {
        File(System.getProperty("user.home"), ".dungeon-master-manager/.global/Icons").also { it.mkdirs() }
    }

    suspend fun getIcon(iconFileName: String): ImageBitmap? = withContext(Dispatchers.IO) {
        iconCache[iconFileName]?.let { return@withContext it }
        val iconFile = File(iconsDir, iconFileName)
        if (!iconFile.exists()) return@withContext null
        try {
            iconFile.inputStream().use { stream ->
                val bitmap = loadImageBitmap(stream)
                iconCache[iconFileName] = bitmap
                bitmap
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

    fun clear() {
        iconCache.clear()
        thumbnailCache.clear()
    }
}
