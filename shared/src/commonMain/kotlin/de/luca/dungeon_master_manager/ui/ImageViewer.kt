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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import kotlin.math.pow
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.imageio.ImageIO

/**
 * Interactive image viewer supporting zoom (mouse wheel + buttons), pan/drag,
 * fit-to-screen, 1:1 original size, and file information.
 */
private const val MIN_IMAGE_ZOOM = 0.05f
private const val MAX_IMAGE_ZOOM = 50.0f

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
    var targetScale by remember { mutableStateOf(1f) }
    var targetOffsetX by remember { mutableStateOf(0f) }
    var targetOffsetY by remember { mutableStateOf(0f) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var hasAutoFitted by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var zoomJob by remember { mutableStateOf<Job?>(null) }

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

    fun fitToScreen(bitmap: ImageBitmap, viewSize: IntSize, animate: Boolean = false) {
        if (viewSize.width <= 0 || viewSize.height <= 0) return
        val padding = 48f
        val availW = (viewSize.width - padding).coerceAtLeast(20f)
        val availH = (viewSize.height - padding).coerceAtLeast(20f)
        val scaleW = availW / bitmap.width
        val scaleH = availH / bitmap.height
        val fitScale = minOf(scaleW, scaleH)
        val target = if (bitmap.width <= availW && bitmap.height <= availH) 1f else fitScale.coerceIn(MIN_IMAGE_ZOOM, MAX_IMAGE_ZOOM)
        targetScale = target
        targetOffsetX = 0f
        targetOffsetY = 0f
        if (animate) {
            zoomJob?.cancel()
            zoomJob = coroutineScope.launch {
                val startScale = scale
                val startX = offsetX
                val startY = offsetY
                val anim = Animatable(0f)
                anim.animateTo(1f, tween(140, easing = FastOutSlowInEasing)) {
                    scale = startScale + (targetScale - startScale) * value
                    offsetX = startX + (0f - startX) * value
                    offsetY = startY + (0f - startY) * value
                }
            }
        } else {
            scale = target
            offsetX = 0f
            offsetY = 0f
        }
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
                                val factor = 0.80f
                                val oldTarget = targetScale
                                val newTarget = (targetScale * factor).coerceIn(MIN_IMAGE_ZOOM, MAX_IMAGE_ZOOM)
                                if (newTarget != oldTarget) {
                                    val actualZoom = newTarget / oldTarget
                                    val newTargetX = targetOffsetX * actualZoom
                                    val newTargetY = targetOffsetY * actualZoom
                                    targetScale = newTarget
                                    targetOffsetX = newTargetX
                                    targetOffsetY = newTargetY
                                    zoomJob?.cancel()
                                    zoomJob = coroutineScope.launch {
                                        val startScale = scale
                                        val startX = offsetX
                                        val startY = offsetY
                                        val anim = Animatable(0f)
                                        anim.animateTo(1f, tween(140, easing = FastOutSlowInEasing)) {
                                            scale = startScale + (targetScale - startScale) * value
                                            offsetX = startX + (targetOffsetX - startX) * value
                                            offsetY = startY + (targetOffsetY - startY) * value
                                        }
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("-", fontSize = 16.sp)
                        }

                        // Zoom percent (Click to reset 1:1)
                        TextButton(
                            onClick = {
                                targetScale = 1f
                                targetOffsetX = 0f
                                targetOffsetY = 0f
                                zoomJob?.cancel()
                                zoomJob = coroutineScope.launch {
                                    val startScale = scale
                                    val startX = offsetX
                                    val startY = offsetY
                                    val anim = Animatable(0f)
                                    anim.animateTo(1f, tween(140, easing = FastOutSlowInEasing)) {
                                        scale = startScale + (1f - startScale) * value
                                        offsetX = startX + (0f - startX) * value
                                        offsetY = startY + (0f - startY) * value
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("${(scale * 100).toInt()}%", fontSize = 12.sp)
                        }

                        // Zoom In
                        OutlinedButton(
                            onClick = {
                                val factor = 1.25f
                                val oldTarget = targetScale
                                val newTarget = (targetScale * factor).coerceIn(MIN_IMAGE_ZOOM, MAX_IMAGE_ZOOM)
                                if (newTarget != oldTarget) {
                                    val actualZoom = newTarget / oldTarget
                                    val newTargetX = targetOffsetX * actualZoom
                                    val newTargetY = targetOffsetY * actualZoom
                                    targetScale = newTarget
                                    targetOffsetX = newTargetX
                                    targetOffsetY = newTargetY
                                    zoomJob?.cancel()
                                    zoomJob = coroutineScope.launch {
                                        val startScale = scale
                                        val startX = offsetX
                                        val startY = offsetY
                                        val anim = Animatable(0f)
                                        anim.animateTo(1f, tween(140, easing = FastOutSlowInEasing)) {
                                            scale = startScale + (targetScale - startScale) * value
                                            offsetX = startX + (targetOffsetX - startX) * value
                                            offsetY = startY + (targetOffsetY - startY) * value
                                        }
                                    }
                                }
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
                                targetScale = 1f
                                targetOffsetX = 0f
                                targetOffsetY = 0f
                                zoomJob?.cancel()
                                zoomJob = coroutineScope.launch {
                                    val startScale = scale
                                    val startX = offsetX
                                    val startY = offsetY
                                    val anim = Animatable(0f)
                                    anim.animateTo(1f, tween(140, easing = FastOutSlowInEasing)) {
                                        scale = startScale + (1f - startScale) * value
                                        offsetX = startX + (0f - startX) * value
                                        offsetY = startY + (0f - startY) * value
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("1:1", fontSize = 12.sp)
                        }

                        // Fit to screen
                        OutlinedButton(
                            onClick = {
                                imageBitmap?.let { fitToScreen(it, viewportSize, animate = true) }
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
                                    targetOffsetX = 0f
                                    targetOffsetY = 0f
                                    zoomJob?.cancel()
                                    zoomJob = coroutineScope.launch {
                                        val startX = offsetX
                                        val startY = offsetY
                                        val anim = Animatable(0f)
                                        anim.animateTo(1f, tween(140, easing = FastOutSlowInEasing)) {
                                            offsetX = startX + (0f - startX) * value
                                            offsetY = startY + (0f - startY) * value
                                        }
                                    }
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
                                detectTransformGestures { _, pan, _, _ ->
                                    if (viewportSize.width > 0) {
                                        zoomJob?.cancel() // Cancel ongoing zoom animation immediately on drag
                                        offsetX += pan.x
                                        offsetY += pan.y
                                        targetScale = scale
                                        targetOffsetX = offsetX
                                        targetOffsetY = offsetY
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Scroll && viewportSize.width > 0) {
                                            val cursorPosition = event.changes.first().position
                                            val rawDelta = event.changes.first().scrollDelta.y

                                            // If reversing direction mid-flight, reset target to current visible values
                                            if ((rawDelta > 0 && targetScale > scale) || (rawDelta < 0 && targetScale < scale)) {
                                                targetScale = scale
                                                targetOffsetX = offsetX
                                                targetOffsetY = offsetY
                                            }

                                            val absDelta = kotlin.math.abs(rawDelta).coerceIn(0.5f, 5f)
                                            val factor = if (rawDelta > 0) (1f / 1.18f).pow(absDelta) else 1.18f.pow(absDelta)
                                            val oldTarget = targetScale
                                            val newTarget = (targetScale * factor).coerceIn(MIN_IMAGE_ZOOM, MAX_IMAGE_ZOOM)

                                            if (newTarget != oldTarget) {
                                                val actualZoom = newTarget / oldTarget
                                                val cx = viewportSize.width / 2f
                                                val cy = viewportSize.height / 2f
                                                val newTargetX = targetOffsetX + (cursorPosition.x - cx - targetOffsetX) * (1 - actualZoom)
                                                val newTargetY = targetOffsetY + (cursorPosition.y - cy - targetOffsetY) * (1 - actualZoom)

                                                targetScale = newTarget
                                                targetOffsetX = newTargetX
                                                targetOffsetY = newTargetY

                                                zoomJob?.cancel()
                                                zoomJob = coroutineScope.launch {
                                                    val startScale = scale
                                                    val startX = offsetX
                                                    val startY = offsetY
                                                    val anim = Animatable(0f)
                                                    anim.animateTo(1f, tween(140, easing = FastOutSlowInEasing)) {
                                                        scale = startScale + (targetScale - startScale) * value
                                                        offsetX = startX + (targetOffsetX - startX) * value
                                                        offsetY = startY + (targetOffsetY - startY) * value
                                                    }
                                                }
                                            }
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
