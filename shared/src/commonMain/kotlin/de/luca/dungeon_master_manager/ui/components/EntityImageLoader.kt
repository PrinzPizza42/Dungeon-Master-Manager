package de.luca.dungeon_master_manager.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import de.luca.dungeon_master_manager.ui.map.MarkerIconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap

object EntityImageLoader {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    val SUPPORTED_IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "svg")

    suspend fun loadImage(file: File?): ImageBitmap? = withContext(Dispatchers.IO) {
        if (file == null || !file.exists() || !file.isFile) return@withContext null
        val key = "${file.absolutePath}:${file.lastModified()}"
        cache[key]?.let { return@withContext it }

        try {
            val bitmap = if (file.extension.equals("svg", ignoreCase = true)) {
                MarkerIconCache.rasterizeSvg(file, targetDimension = 512)
            } else {
                FileInputStream(file).use { stream ->
                    loadImageBitmap(stream)
                }
            }
            if (bitmap != null) {
                cache[key] = bitmap
            }
            bitmap
        } catch (_: Exception) {
            null
        }
    }
}

@Composable
fun rememberEntityImage(file: File?): ImageBitmap? {
    var bitmap by remember(file?.absolutePath, file?.lastModified()) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(file?.absolutePath, file?.lastModified()) {
        bitmap = EntityImageLoader.loadImage(file)
    }

    return bitmap
}
