package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.imageio.ImageIO

/**
 * Interactive image viewer supporting zoom (mouse wheel + buttons), pan/drag,
 * fit-to-screen, 1:1 original size, and file information.
 */
@Composable
fun ImageViewer(
    filePath: String,
    fileName: String,
    modifier: Modifier = Modifier,
) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var fileSizeString by remember { mutableStateOf("") }
    var reloadTrigger by remember { mutableStateOf(0) }

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var hasAutoFitted by remember { mutableStateOf(false) }

    LaunchedEffect(filePath, reloadTrigger) {
        isLoading = true
        errorMessage = null
        hasAutoFitted = false
        withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (!file.exists()) {
                errorMessage = "File not found: $filePath"
                isLoading = false
                return@withContext
            }

            val bytes = file.length()
            fileSizeString = when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
                else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            }

            var loaded: ImageBitmap? = null
            try {
                file.inputStream().use {
                    loaded = loadImageBitmap(it)
                }
            } catch (e: Exception) {
                // Fallback to Java ImageIO
                try {
                    val bufferedImage = ImageIO.read(file)
                    if (bufferedImage != null) {
                        loaded = bufferedImage.toComposeImageBitmap()
                    }
                } catch (e2: Exception) {
                    errorMessage = "Failed to load image: ${e.message}"
                }
            }

            if (loaded != null) {
                imageBitmap = loaded
                errorMessage = null
            } else if (errorMessage == null) {
                errorMessage = "Unable to decode image format."
            }
            isLoading = false
        }
    }

    fun fitToScreen(bitmap: ImageBitmap, viewSize: IntSize) {
        if (viewSize.width <= 0 || viewSize.height <= 0) return
        val padding = 48f
        val availW = (viewSize.width - padding).coerceAtLeast(20f)
        val availH = (viewSize.height - padding).coerceAtLeast(20f)
        val scaleW = availW / bitmap.width
        val scaleH = availH / bitmap.height
        val fitScale = minOf(scaleW, scaleH)
        // If image is smaller than viewport, keep 1.0f scale, otherwise scale down to fit
        scale = if (bitmap.width <= availW && bitmap.height <= availH) 1f else fitScale.coerceIn(0.05f, 25f)
        offsetX = 0f
        offsetY = 0f
    }

    // Auto-fit on first load once viewport size and bitmap are available
    LaunchedEffect(imageBitmap, viewportSize) {
        val bmp = imageBitmap
        if (bmp != null && viewportSize.width > 0 && !hasAutoFitted) {
            fitToScreen(bmp, viewportSize)
            hasAutoFitted = true
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🖼 $fileName",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (imageBitmap != null) {
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "${imageBitmap!!.width} × ${imageBitmap!!.height} px",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        fileSizeString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (imageBitmap != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Zoom Out
                        OutlinedButton(
                            onClick = {
                                scale = (scale / 1.25f).coerceIn(0.05f, 25f)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("-", fontSize = 16.sp)
                        }

                        // Zoom percent
                        TextButton(
                            onClick = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("${(scale * 100).toInt()}%", fontSize = 12.sp)
                        }

                        // Zoom In
                        OutlinedButton(
                            onClick = {
                                scale = (scale * 1.25f).coerceIn(0.05f, 25f)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("+", fontSize = 16.sp)
                        }

                        Spacer(Modifier.width(4.dp))

                        // 1:1 original
                        OutlinedButton(
                            onClick = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("1:1", fontSize = 12.sp)
                        }

                        // Fit to screen
                        OutlinedButton(
                            onClick = {
                                imageBitmap?.let { fitToScreen(it, viewportSize) }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Fit", fontSize = 12.sp)
                        }

                        if (offsetX != 0f || offsetY != 0f) {
                            // Center position
                            OutlinedButton(
                                onClick = {
                                    offsetX = 0f
                                    offsetY = 0f
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Center", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .onSizeChanged { viewportSize = it }
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    }
                }

                errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { reloadTrigger++ }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                imageBitmap != null -> {
                    val bitmap = imageBitmap!!
                    val density = LocalDensity.current
                    val imageWidthDp = with(density) { bitmap.width.toDp() }
                    val imageHeightDp = with(density) { bitmap.height.toDp() }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    if (viewportSize.width > 0) {
                                        val oldScale = scale
                                        val newScale = (scale * zoom).coerceIn(0.05f, 25f)
                                        val actualZoom = newScale / oldScale

                                        val cx = viewportSize.width / 2f
                                        val cy = viewportSize.height / 2f

                                        scale = newScale
                                        offsetX = offsetX + pan.x + (centroid.x - cx - offsetX) * (1 - actualZoom)
                                        offsetY = offsetY + pan.y + (centroid.y - cy - offsetY) * (1 - actualZoom)
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Scroll && viewportSize.width > 0) {
                                            val delta = event.changes.first().scrollDelta.y
                                            val cursorPosition = event.changes.first().position
                                            val factor = if (delta > 0) 0.85f else 1.15f
                                            val oldScale = scale
                                            val newScale = (scale * factor).coerceIn(0.05f, 25f)
                                            val actualZoom = newScale / oldScale

                                            val cx = viewportSize.width / 2f
                                            val cy = viewportSize.height / 2f

                                            scale = newScale
                                            offsetX = offsetX + (cursorPosition.x - cx - offsetX) * (1 - actualZoom)
                                            offsetY = offsetY + (cursorPosition.y - cy - offsetY) * (1 - actualZoom)

                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                                .requiredSize(imageWidthDp, imageHeightDp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = fileName,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
