package de.luca.dungeon_master_manager.ui.map

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import de.luca.dungeon_master_manager.data.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Data
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Rect
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
        if (!file.exists() || !file.isFile) return@withContext null

        val bitmap = if (isSvg(file)) {
            rasterizeSvg(file, maxDimension)
        } else {
            decodeWithImageIO(file, maxDimension) ?: decodeWithCompose(file, maxDimension)
        }

        if (bitmap != null) {
            thumbnailCache[key] = bitmap
        }
        bitmap
    }

    private fun decodeWithImageIO(file: File, maxDimension: Int): ImageBitmap? {
        return try {
            val src = ImageIO.read(file) ?: return null
            val w = src.width
            val h = src.height
            if (maxDimension <= 0 || (w <= maxDimension && h <= maxDimension)) {
                return src.toComposeImageBitmap()
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
            try {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                g2d.drawImage(src, 0, 0, targetW, targetH, null)
            } finally {
                g2d.dispose()
            }

            scaled.toComposeImageBitmap()
        } catch (_: Throwable) {
            null
        }
    }

    private fun decodeWithCompose(file: File, maxDimension: Int): ImageBitmap? {
        return try {
            val rawBmp = file.inputStream().use { stream ->
                loadImageBitmap(stream)
            }
            val maxOrig = maxOf(rawBmp.width, rawBmp.height)
            if (maxDimension <= 0 || maxOrig <= maxDimension) {
                return rawBmp
            }

            val scale = maxDimension.toFloat() / maxOrig
            val targetW = (rawBmp.width * scale).toInt().coerceAtLeast(1)
            val targetH = (rawBmp.height * scale).toInt().coerceAtLeast(1)

            val skiaImage = SkiaImage.makeFromBitmap(rawBmp.asSkiaBitmap())
            val surface = Surface.makeRasterN32Premul(targetW, targetH)
            try {
                surface.canvas.drawImageRect(
                    skiaImage,
                    Rect.makeWH(rawBmp.width.toFloat(), rawBmp.height.toFloat()),
                    Rect.makeWH(targetW.toFloat(), targetH.toFloat()),
                    FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR),
                    null,
                    false
                )
                surface.makeImageSnapshot().use { it.toComposeImageBitmap() }
            } finally {
                surface.close()
                skiaImage.close()
            }
        } catch (_: Throwable) {
            null
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
