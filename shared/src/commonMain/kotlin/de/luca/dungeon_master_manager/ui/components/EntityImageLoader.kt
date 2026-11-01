package de.luca.dungeon_master_manager.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import de.luca.dungeon_master_manager.data.ProjectRepository
import de.luca.dungeon_master_manager.ui.map.MarkerIconCache
import de.luca.dungeonmastermanager.database.Entity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import javax.imageio.ImageIO

object EntityImageLoader {
    private const val MAX_CACHE_ENTRIES = 160

    private val cacheLock = Any()
    private val cache = object : LinkedHashMap<String, ImageBitmap>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean {
            return size > MAX_CACHE_ENTRIES
        }
    }

    val SUPPORTED_IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "svg")

    fun resolveEntityImageFile(entity: Entity, projectPath: String?): File? {
        val relPath = entity.imageFilePath
        if (relPath.isNullOrBlank()) return null

        if (!projectPath.isNullOrBlank()) {
            val projFile = File(projectPath, relPath)
            if (projFile.exists() && projFile.isFile) return projFile
        }

        val globalFile = File(ProjectRepository.globalImagesDir, relPath)
        if (globalFile.exists() && globalFile.isFile) return globalFile

        val direct = File(relPath)
        if (direct.isAbsolute && direct.exists() && direct.isFile) return direct

        return null
    }

    private fun cacheKey(file: File, maxDimension: Int): String {
        return "${file.absolutePath}:${file.lastModified()}:$maxDimension"
    }

    fun getImageSync(file: File?, maxDimension: Int = 512): ImageBitmap? {
        if (file == null || !file.exists() || !file.isFile) return null
        val exactKey = cacheKey(file, maxDimension)
        synchronized(cacheLock) {
            cache[exactKey]?.let { return it }
            // If another resolution exists in cache, return it rather than returning null
            val prefix = "${file.absolutePath}:${file.lastModified()}:"
            for ((key, bmp) in cache) {
                if (key.startsWith(prefix)) {
                    return bmp
                }
            }
        }
        return null
    }

    suspend fun loadImage(file: File?, maxDimension: Int = 512): ImageBitmap? = withContext(Dispatchers.IO) {
        if (file == null || !file.exists() || !file.isFile) return@withContext null
        val exactKey = cacheKey(file, maxDimension)
        synchronized(cacheLock) {
            cache[exactKey]?.let { return@withContext it }
        }

        val bitmap = decodeThumbnail(file, maxDimension)
        if (bitmap != null) {
            synchronized(cacheLock) {
                cache[exactKey] = bitmap
            }
        }
        bitmap
    }

    private fun decodeThumbnail(file: File, maxDimension: Int): ImageBitmap? {
        val ext = file.extension.lowercase()

        // 1. Vector SVG rendering
        if (ext == "svg") {
            return MarkerIconCache.rasterizeSvg(file, targetDimension = maxDimension)
        }

        // 2. Subsampled decoding via ImageIO for JPEG/PNG and standard formats
        try {
            val iis = ImageIO.createImageInputStream(file)
            if (iis != null) {
                val readers = ImageIO.getImageReaders(iis)
                if (readers.hasNext()) {
                    val reader = readers.next()
                    try {
                        reader.input = iis
                        val origW = reader.getWidth(0)
                        val origH = reader.getHeight(0)
                        val maxOrig = maxOf(origW, origH)

                        val subsample = if (maxDimension > 0 && maxOrig > maxDimension) {
                            (maxOrig / maxDimension).coerceAtLeast(1)
                        } else 1

                        val param = reader.defaultReadParam
                        if (subsample > 1) {
                            param.setSourceSubsampling(subsample, subsample, 0, 0)
                        }

                        val readImage = reader.read(0, param)
                        if (readImage != null) {
                            val curMax = maxOf(readImage.width, readImage.height)
                            val finalImage = if (maxDimension > 0 && curMax > maxDimension) {
                                val scale = maxDimension.toFloat() / curMax
                                val targetW = (readImage.width * scale).toInt().coerceAtLeast(1)
                                val targetH = (readImage.height * scale).toInt().coerceAtLeast(1)
                                val hasAlpha = readImage.colorModel?.hasAlpha() ?: true
                                val targetType = if (hasAlpha) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
                                val scaled = BufferedImage(targetW, targetH, targetType)
                                val g = scaled.createGraphics()
                                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                                g.drawImage(readImage, 0, 0, targetW, targetH, null)
                                g.dispose()
                                scaled
                            } else {
                                readImage
                            }
                            return finalImage.toComposeImageBitmap()
                        }
                    } finally {
                        reader.dispose()
                        iis.close()
                    }
                } else {
                    iis.close()
                }
            }
        } catch (_: Throwable) {
            // Fallback below
        }

        // 3. Fallback: Compose loadImageBitmap with optional Skia downscale
        try {
            val rawBmp = FileInputStream(file).use { stream ->
                loadImageBitmap(stream)
            }
            val maxOrig = maxOf(rawBmp.width, rawBmp.height)
            if (maxDimension <= 0 || maxOrig <= maxDimension) {
                return rawBmp
            }

            // Downscale raw Compose bitmap using Skia surface
            val scale = maxDimension.toFloat() / maxOrig
            val targetW = (rawBmp.width * scale).toInt().coerceAtLeast(1)
            val targetH = (rawBmp.height * scale).toInt().coerceAtLeast(1)
            val skiaBmp = rawBmp.asSkiaBitmap()
            val skiaImage = org.jetbrains.skia.Image.makeFromBitmap(skiaBmp)
            val surface = Surface.makeRasterN32Premul(targetW, targetH)
            surface.canvas.drawImageRect(
                skiaImage,
                Rect.makeWH(targetW.toFloat(), targetH.toFloat()),
                null
            )
            return surface.makeImageSnapshot().toComposeImageBitmap()
        } catch (_: Throwable) {
            return null
        }
    }

    fun clear() {
        synchronized(cacheLock) {
            cache.clear()
        }
    }

    fun invalidate(file: File) {
        val prefix = "${file.absolutePath}:"
        synchronized(cacheLock) {
            val it = cache.entries.iterator()
            while (it.hasNext()) {
                if (it.next().key.startsWith(prefix)) {
                    it.remove()
                }
            }
        }
    }
}

@Composable
fun rememberEntityImage(file: File?, maxDimension: Int = 512): ImageBitmap? {
    var bitmap by remember(file?.absolutePath, file?.lastModified(), maxDimension) {
        mutableStateOf(EntityImageLoader.getImageSync(file, maxDimension))
    }

    LaunchedEffect(file?.absolutePath, file?.lastModified(), maxDimension) {
        if (bitmap == null) {
            bitmap = EntityImageLoader.loadImage(file, maxDimension)
        }
    }

    return bitmap
}
