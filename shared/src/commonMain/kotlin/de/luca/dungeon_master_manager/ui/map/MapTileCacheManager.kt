package de.luca.dungeon_master_manager.ui.map

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.skia.*
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

data class MapTileState(
    val manifest: MapPyramidManifest? = null,
    val lod0Bitmap: ImageBitmap? = null,
    val isGenerating: Boolean = false,
    val generationProgress: Float = 1.0f,
    val cacheDir: File? = null
)

class TileMemoryCache(private val maxSizeBytes: Long = 96 * 1024 * 1024) {
    private val lock = Any()
    private val map = object : LinkedHashMap<TileKey, ImageBitmap>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<TileKey, ImageBitmap>?): Boolean {
            return currentBytes > maxSizeBytes
        }
    }
    private var currentBytes: Long = 0

    operator fun get(key: TileKey): ImageBitmap? = synchronized(lock) {
        map[key]
    }

    fun put(key: TileKey, bitmap: ImageBitmap) = synchronized(lock) {
        val size = (bitmap.width * bitmap.height * 4).toLong()
        val old = map.put(key, bitmap)
        if (old != null) {
            currentBytes -= (old.width * old.height * 4).toLong()
        }
        currentBytes += size

        // Evict if over budget
        val it = map.entries.iterator()
        while (currentBytes > maxSizeBytes && it.hasNext()) {
            val entry = it.next()
            val entrySize = (entry.value.width * entry.value.height * 4).toLong()
            currentBytes -= entrySize
            it.remove()
        }
    }

    fun clear() = synchronized(lock) {
        map.clear()
        currentBytes = 0
    }
}

object MapTileCacheManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    val memoryCache = TileMemoryCache()

    private val activeStates = ConcurrentHashMap<String, MutableStateFlow<MapTileState>>()
    private val inFlightLoads = ConcurrentHashMap<TileKey, Deferred<ImageBitmap?>>()

    // Limited parallelism for background slicing so laptop CPU remains responsive
    @OptIn(ExperimentalCoroutinesApi::class)
    private val tilingDispatcher = Dispatchers.IO.limitedParallelism(2)

    fun getCacheDir(projectPath: String, sourceFile: File): File {
        val baseDir = if (projectPath.isNotBlank()) {
            File(projectPath, ".map-cache")
        } else {
            File(System.getProperty("user.home"), ".dungeon-master-manager/.global/map-cache")
        }
        val pathKey = try { sourceFile.canonicalPath } catch (_: Exception) { sourceFile.absolutePath }
        val hash = sha256(pathKey).take(12)
        val safeName = sourceFile.nameWithoutExtension.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(baseDir, "${safeName}_$hash")
    }

    private fun sha256(str: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(str.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun getValidManifest(cacheDir: File, sourceFile: File): MapPyramidManifest? {
        val manifestFile = File(cacheDir, "manifest.json")
        if (!manifestFile.exists()) return null
        return try {
            val manifest = json.decodeFromString<MapPyramidManifest>(manifestFile.readText())
            if (manifest.sourceFileSize == sourceFile.length() &&
                manifest.sourceLastModified == sourceFile.lastModified() &&
                File(cacheDir, "lod0.${manifest.tileExtension}").exists()
            ) {
                manifest
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getOrLoadState(
        projectPath: String,
        sourceFile: File,
        coroutineScope: CoroutineScope
    ): StateFlow<MapTileState> {
        val cacheDir = getCacheDir(projectPath, sourceFile)
        val stateKey = cacheDir.absolutePath

        val flow = activeStates.computeIfAbsent(stateKey) {
            MutableStateFlow(MapTileState(cacheDir = cacheDir))
        }

        // Check if already populated
        val current = flow.value
        if (current.manifest != null && current.lod0Bitmap != null && !current.isGenerating) {
            return flow.asStateFlow()
        }

        coroutineScope.launch(Dispatchers.IO) {
            val existingManifest = getValidManifest(cacheDir, sourceFile)
            if (existingManifest != null) {
                // Instant load: load lod0 and manifest directly from disk
                val lod0File = File(cacheDir, "lod0.${existingManifest.tileExtension}")
                val lod0Bitmap = try {
                    lod0File.inputStream().use { loadImageBitmap(it) }
                } catch (_: Exception) {
                    null
                }

                if (lod0Bitmap != null) {
                    flow.value = MapTileState(
                        manifest = existingManifest,
                        lod0Bitmap = lod0Bitmap,
                        isGenerating = false,
                        generationProgress = 1.0f,
                        cacheDir = cacheDir
                    )
                    return@launch
                }
            }

            // Cache miss or outdated: Generate
            generatePyramid(projectPath, sourceFile, cacheDir, flow)
        }

        return flow.asStateFlow()
    }

    private suspend fun generatePyramid(
        projectPath: String,
        sourceFile: File,
        cacheDir: File,
        flow: MutableStateFlow<MapTileState>
    ) = withContext(tilingDispatcher) {
        flow.value = flow.value.copy(isGenerating = true, generationProgress = 0f)

        cacheDir.mkdirs()
        val tilesRootDir = File(cacheDir, "tiles").also { it.mkdirs() }

        // Read source bytes for Skia decode
        val sourceBytes = sourceFile.readBytes()
        val srcImage = try {
            Image.makeFromEncoded(sourceBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            flow.value = flow.value.copy(isGenerating = false)
            return@withContext
        }

        try {
            val origW = srcImage.width
            val origH = srcImage.height
            val hasAlpha = srcImage.colorInfo.alphaType != ColorAlphaType.OPAQUE
            val tileExt = if (hasAlpha) "png" else "jpg"
            val format = if (hasAlpha) EncodedImageFormat.PNG else EncodedImageFormat.JPEG
            val quality = if (hasAlpha) 100 else 90

            // 1. Calculate LOD0
            val maxDim = maxOf(origW, origH)
            val isSmall = maxDim <= LOD0_MAX_DIMENSION

            val lod0Scale = if (isSmall) 1.0f else (LOD0_MAX_DIMENSION.toFloat() / maxDim)
            val lod0W = (origW * lod0Scale).toInt().coerceAtLeast(1)
            val lod0H = (origH * lod0Scale).toInt().coerceAtLeast(1)

            // Generate LOD0 using Skia Surface with Linear Mipmap
            val lod0Bitmap: ImageBitmap = if (isSmall) {
                srcImage.toComposeImageBitmap()
            } else {
                val surface = Surface.makeRasterN32Premul(lod0W, lod0H)
                try {
                    surface.canvas.drawImageRect(
                        srcImage,
                        Rect.makeWH(origW.toFloat(), origH.toFloat()),
                        Rect.makeWH(lod0W.toFloat(), lod0H.toFloat()),
                        FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR),
                        null,
                        false
                    )
                    val snapshot = surface.makeImageSnapshot()
                    val bmp = snapshot.toComposeImageBitmap()
                    snapshot.close()
                    bmp
                } finally {
                    surface.close()
                }
            }

            // Save lod0 to disk
            val lod0File = File(cacheDir, "lod0.$tileExt")
            val lod0Data = if (isSmall) {
                srcImage.encodeToData(format, quality)
            } else {
                val s = Surface.makeRasterN32Premul(lod0W, lod0H)
                try {
                    s.canvas.drawImageRect(
                        srcImage,
                        Rect.makeWH(origW.toFloat(), origH.toFloat()),
                        Rect.makeWH(lod0W.toFloat(), lod0H.toFloat()),
                        FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR),
                        null,
                        false
                    )
                    val snap = s.makeImageSnapshot()
                    val d = snap.encodeToData(format, quality)
                    snap.close()
                    d
                } finally {
                    s.close()
                }
            }
            if (lod0Data != null) {
                lod0File.writeBytes(lod0Data.bytes)
                lod0Data.close()
            }

            // Build levels list
            val levels = mutableListOf<LodLevelSpec>()
            // Level 0: overview
            levels.add(
                LodLevelSpec(
                    level = 0,
                    scaleFactor = lod0Scale,
                    width = lod0W,
                    height = lod0H,
                    cols = 1,
                    rows = 1
                )
            )

            if (!isSmall) {
                // Calculate pyramid factors ending at 1.0f (native resolution)
                val pyramidFactors = mutableListOf<Float>()
                pyramidFactors.add(1.0f)
                var factor = 0.5f
                while (factor > lod0Scale * 1.25f) {
                    pyramidFactors.add(factor)
                    factor /= 2.0f
                }
                pyramidFactors.reverse() // from lowest detail (just above LOD0) to 1.0f (native)

                for ((idx, f) in pyramidFactors.withIndex()) {
                    val lvl = idx + 1
                    val w = (origW * f).toInt().coerceAtLeast(1)
                    val h = (origH * f).toInt().coerceAtLeast(1)
                    val cols = ceil(w.toDouble() / TILE_SIZE).toInt()
                    val rows = ceil(h.toDouble() / TILE_SIZE).toInt()
                    levels.add(
                        LodLevelSpec(
                            level = lvl,
                            scaleFactor = f,
                            width = w,
                            height = h,
                            cols = cols,
                            rows = rows
                        )
                    )
                }
            }

            val manifest = MapPyramidManifest(
                schemaVersion = 1,
                sourceRelativePath = if (projectPath.isNotBlank()) {
                    try { sourceFile.relativeTo(File(projectPath)).path } catch (_: Exception) { sourceFile.name }
                } else sourceFile.name,
                sourceFileSize = sourceFile.length(),
                sourceLastModified = sourceFile.lastModified(),
                originalWidth = origW,
                originalHeight = origH,
                hasAlpha = hasAlpha,
                tileExtension = tileExt,
                tileSize = TILE_SIZE,
                levels = levels
            )

            // Phase 1 COMPLETE: Emit immediate preview!
            flow.value = MapTileState(
                manifest = manifest,
                lod0Bitmap = lod0Bitmap,
                isGenerating = !isSmall,
                generationProgress = if (isSmall) 1.0f else 0.05f,
                cacheDir = cacheDir
            )

            if (isSmall) {
                // Write manifest and we're done
                File(cacheDir, "manifest.json").writeText(json.encodeToString(manifest))
                return@withContext
            }

            // Phase 2: Slice sector tiles in background
            val tileLevels = levels.filter { it.level > 0 }
            val totalTiles = tileLevels.sumOf { it.cols * it.rows }
            var tilesDone = 0

            for (lvlSpec in tileLevels) {
                val lvlDir = File(tilesRootDir, "z${lvlSpec.level}").also { it.mkdirs() }
                val isNative = lvlSpec.scaleFactor == 1.0f

                // If downscaled, we render the scaled image into a surface
                val lvlSurface = if (!isNative) {
                    Surface.makeRasterN32Premul(lvlSpec.width, lvlSpec.height).also { s ->
                        s.canvas.drawImageRect(
                            srcImage,
                            Rect.makeWH(origW.toFloat(), origH.toFloat()),
                            Rect.makeWH(lvlSpec.width.toFloat(), lvlSpec.height.toFloat()),
                            FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR),
                            null,
                            false
                        )
                    }
                } else null

                val lvlImage = lvlSurface?.makeImageSnapshot() ?: srcImage

                for (r in 0 until lvlSpec.rows) {
                    for (c in 0 until lvlSpec.cols) {
                        val tileX = c * TILE_SIZE
                        val tileY = r * TILE_SIZE
                        val tileW = minOf(TILE_SIZE, lvlSpec.width - tileX)
                        val tileH = minOf(TILE_SIZE, lvlSpec.height - tileY)

                        val tileFile = File(lvlDir, "r${r}_c${c}.$tileExt")
                        if (!tileFile.exists() || tileFile.length() == 0L) {
                            val tileSurface = Surface.makeRasterN32Premul(tileW, tileH)
                            try {
                                tileSurface.canvas.drawImageRect(
                                    lvlImage,
                                    Rect.makeXYWH(tileX.toFloat(), tileY.toFloat(), tileW.toFloat(), tileH.toFloat()),
                                    Rect.makeWH(tileW.toFloat(), tileH.toFloat()),
                                    SamplingMode.DEFAULT,
                                    null,
                                    false
                                )
                                val tileSnap = tileSurface.makeImageSnapshot()
                                val data = tileSnap.encodeToData(format, quality)
                                if (data != null) {
                                    val tmpFile = File(lvlDir, "r${r}_c${c}.tmp")
                                    tmpFile.writeBytes(data.bytes)
                                    tmpFile.renameTo(tileFile)
                                    data.close()
                                }
                                tileSnap.close()
                            } finally {
                                tileSurface.close()
                            }
                        }

                        tilesDone++
                        if (tilesDone % 4 == 0 || tilesDone == totalTiles) {
                            val progress = 0.05f + (tilesDone.toFloat() / totalTiles) * 0.95f
                            flow.value = flow.value.copy(generationProgress = progress)
                        }
                    }
                }

                if (lvlSurface != null) {
                    lvlImage.close()
                    lvlSurface.close()
                }
            }

            // Save finalized manifest.json
            File(cacheDir, "manifest.json").writeText(json.encodeToString(manifest))

            flow.value = flow.value.copy(
                isGenerating = false,
                generationProgress = 1.0f
            )
        } finally {
            srcImage.close()
        }
    }

    suspend fun getTileBitmapAsync(
        cacheDir: File,
        manifest: MapPyramidManifest,
        key: TileKey
    ): ImageBitmap? {
        memoryCache[key]?.let { return it }

        val deferred = inFlightLoads.computeIfAbsent(key) {
            CoroutineScope(Dispatchers.IO).async {
                val tileFile = File(cacheDir, "tiles/z${key.level}/r${key.row}_c${key.col}.${manifest.tileExtension}")
                if (!tileFile.exists()) return@async null

                try {
                    tileFile.inputStream().use { stream ->
                        val bmp = loadImageBitmap(stream)
                        memoryCache.put(key, bmp)
                        bmp
                    }
                } catch (_: Exception) {
                    null
                } finally {
                    inFlightLoads.remove(key)
                }
            }
        }

        return deferred.await()
    }
}
