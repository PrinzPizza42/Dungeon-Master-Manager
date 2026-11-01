package de.luca.dungeon_master_manager.ui.map

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.luca.dungeon_master_manager.data.AppSettingsRepository
import de.luca.dungeon_master_manager.data.MapMarker
import de.luca.dungeon_master_manager.ui.components.EntityImageLoader
import de.luca.dungeon_master_manager.ui.components.rememberEntityImage
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import de.luca.dungeon_master_manager.ui.theme.GrimoireDivider
import de.luca.dungeonmastermanager.database.Entity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Cursor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class MarkerRenderData(
    val marker: MapMarker,
    val color: Color,
    val iconBitmap: ImageBitmap?,
    val tokenBitmap: ImageBitmap?,
    val labelText: String?,
    val entityName: String?,
    val linkedEntity: Entity?
)

@Composable
fun MapMarkersCanvas(
    markers: List<MapMarker>,
    projectEntities: List<Entity>,
    globalEntities: List<Entity>,
    mapWidthPx: Float,
    mapHeightPx: Float,
    scaleProvider: () -> Float,
    onOpenEntityPopout: (String) -> Unit,
    onEditMarker: (MapMarker) -> Unit,
    onDeleteMarker: (MapMarker) -> Unit,
    addPinMode: Boolean = false,
    onAddPin: ((x: Float, y: Float) -> Unit)? = null,
    projectPath: String = "",
    onHoverMarker: ((MarkerRenderData?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val baseMarkerSizePx = with(density) { 24.dp.toPx() }
    val baseRadiusPx = baseMarkerSizePx / 2f
    val baseTokenSizePx = with(density) { 29.dp.toPx() }
    val baseTokenRadiusPx = baseTokenSizePx / 2f

    val appSettings by AppSettingsRepository.settings.collectAsState()
    val isBatterySaver = appSettings.isBatterySaverMode

    val infiniteTransition = rememberInfiniteTransition(label = "MarkerHaloPulse")
    val pulseFraction by if (!isBatterySaver) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "MarkerHaloFraction"
        )
    } else {
        remember { mutableStateOf(0.35f) }
    }

    // O(1) Entity Map
    val entityById = remember(projectEntities, globalEntities) {
        val map = HashMap<String, Entity>()
        for (e in globalEntities) map[e.id] = e
        for (e in projectEntities) map[e.id] = e
        map
    }

    // Reactive cache triggers when icons/tokens load
    var iconsLoadedTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(markers, entityById, projectPath) {
        var neededReload = false
        for (marker in markers) {
            val iconName = marker.iconFileName
            if (iconName != null && MarkerIconCache.getIconSync(iconName) == null) {
                MarkerIconCache.getIcon(iconName)
                neededReload = true
            }
            if (marker.isEntityLinked && marker.entityId != null && marker.iconFileName.isNullOrBlank()) {
                val ent = entityById[marker.entityId]
                if (ent != null) {
                    val imgFile = EntityImageLoader.resolveEntityImageFile(ent, projectPath)
                    if (imgFile != null && EntityImageLoader.getImageSync(imgFile, maxDimension = 128) == null) {
                        launch(Dispatchers.IO) {
                            EntityImageLoader.loadImage(imgFile, maxDimension = 128)
                            iconsLoadedTrigger++
                        }
                    }
                }
            }
        }
        if (neededReload) {
            iconsLoadedTrigger++
        }
    }

    val renderDataList = remember(markers, entityById, iconsLoadedTrigger, projectPath) {
        markers.map { marker ->
            var markerColor = Color.Red
            var entityName: String? = null
            var linkedEnt: Entity? = null

            if (marker.isEntityLinked && marker.entityId != null) {
                val ent = entityById[marker.entityId]
                if (ent != null) {
                    linkedEnt = ent
                    entityName = ent.name
                    if (ent.color != null) {
                        try {
                            markerColor = Color(ent.color.toULong())
                        } catch (_: Exception) {}
                    }
                }
            } else if (marker.colorULong != null) {
                try {
                    markerColor = Color(marker.colorULong.toULong())
                } catch (_: Exception) {}
            }

            val iconBmp = marker.iconFileName?.let { MarkerIconCache.getIconSync(it) }
            val tokenBmp = if (marker.iconFileName.isNullOrBlank() && linkedEnt != null) {
                val imgFile = EntityImageLoader.resolveEntityImageFile(linkedEnt, projectPath)
                EntityImageLoader.getImageSync(imgFile, maxDimension = 128)
            } else null

            val labelText = if (marker.isEntityLinked) entityName else marker.title

            MarkerRenderData(
                marker = marker,
                color = markerColor,
                iconBitmap = iconBmp,
                tokenBitmap = tokenBmp,
                labelText = labelText,
                entityName = entityName,
                linkedEntity = linkedEnt
            )
        }
    }

    var hoveredMarker by remember { mutableStateOf<MarkerRenderData?>(null) }
    var contextMenuMarker by remember { mutableStateOf<MarkerRenderData?>(null) }
    var emptySpaceContextMenuPos by remember { mutableStateOf<Offset?>(null) }

    val cursorModifier = if (addPinMode) {
        Modifier.pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)))
    } else Modifier

    Box(modifier = modifier.then(cursorModifier)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(renderDataList, mapWidthPx, mapHeightPx, addPinMode) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pos = event.changes.firstOrNull()?.position ?: continue
                            val currentScale = scaleProvider()
                            val markerAdaptiveScale = (currentScale.pow(-0.85f)).coerceIn(0.02f, 2.0f)
                            val hitRadius = (baseRadiusPx + with(density) { 6.dp.toPx() }) * markerAdaptiveScale

                            fun findMarkerAt(p: Offset): MarkerRenderData? {
                                if (mapWidthPx <= 0f || mapHeightPx <= 0f) return null
                                // Topmost marker first
                                for (i in renderDataList.indices.reversed()) {
                                    val r = renderDataList[i]
                                    val mx = r.marker.x * mapWidthPx
                                    val my = r.marker.y * mapHeightPx
                                    val radius = if (r.tokenBitmap != null) {
                                        (baseTokenRadiusPx + with(density) { 6.dp.toPx() }) * markerAdaptiveScale
                                    } else {
                                        hitRadius
                                    }
                                    val dist = sqrt((p.x - mx) * (p.x - mx) + (p.y - my) * (p.y - my))
                                    if (dist <= radius) return r
                                }
                                return null
                            }

                            when (event.type) {
                                PointerEventType.Move -> {
                                    if (!addPinMode && contextMenuMarker == null) {
                                        val found = findMarkerAt(pos)
                                        if (hoveredMarker?.marker?.id != found?.marker?.id) {
                                            hoveredMarker = found
                                            onHoverMarker?.invoke(found)
                                        }
                                    } else {
                                        hoveredMarker = null
                                        onHoverMarker?.invoke(null)
                                    }
                                }

                                PointerEventType.Exit -> {
                                    hoveredMarker = null
                                    onHoverMarker?.invoke(null)
                                }

                                PointerEventType.Press -> {
                                    if (addPinMode && onAddPin != null) {
                                        if (mapWidthPx > 0f && mapHeightPx > 0f) {
                                            val x = (pos.x / mapWidthPx).coerceIn(0f, 1f)
                                            val y = (pos.y / mapHeightPx).coerceIn(0f, 1f)
                                            onAddPin(x, y)
                                            event.changes.forEach { it.consume() }
                                        }
                                    } else {
                                        val found = findMarkerAt(pos)
                                        if (found != null) {
                                            if (event.buttons.isSecondaryPressed) {
                                                contextMenuMarker = found
                                                emptySpaceContextMenuPos = null
                                                hoveredMarker = null
                                                onHoverMarker?.invoke(null)
                                                event.changes.forEach { it.consume() }
                                            } else if (event.buttons.isPrimaryPressed) {
                                                hoveredMarker = null
                                                onHoverMarker?.invoke(null)
                                                if (found.marker.isEntityLinked && found.marker.entityId != null) {
                                                    val ent = entityById[found.marker.entityId]
                                                    if (ent != null) {
                                                        onOpenEntityPopout(ent.name)
                                                    }
                                                } else {
                                                    onEditMarker(found.marker)
                                                }
                                                event.changes.forEach { it.consume() }
                                            }
                                        } else if (event.buttons.isSecondaryPressed && onAddPin != null) {
                                            if (mapWidthPx > 0f && mapHeightPx > 0f) {
                                                val x = (pos.x / mapWidthPx).coerceIn(0f, 1f)
                                                val y = (pos.y / mapHeightPx).coerceIn(0f, 1f)
                                                emptySpaceContextMenuPos = Offset(x, y)
                                                contextMenuMarker = null
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            if (mapWidthPx <= 0f || mapHeightPx <= 0f) return@Canvas
            val currentScale = scaleProvider()
            val markerAdaptiveScale = (currentScale.pow(-0.85f)).coerceIn(0.02f, 2.0f)
            val currentRadius = baseRadiusPx * markerAdaptiveScale
            val currentSize = baseMarkerSizePx * markerAdaptiveScale

            for (r in renderDataList) {
                val cx = r.marker.x * mapWidthPx
                val cy = r.marker.y * mapHeightPx

                val isFocused = hoveredMarker?.marker?.id == r.marker.id || contextMenuMarker?.marker?.id == r.marker.id
                if (isFocused) {
                    val baseTargetRadius = if (r.tokenBitmap != null) {
                        (baseTokenSizePx * markerAdaptiveScale) / 2f
                    } else {
                        currentRadius
                    }
                    val haloRadius = baseTargetRadius + (3.5f + 5f * pulseFraction) * markerAdaptiveScale
                    val haloAlpha = (0.25f + 0.35f * (1f - pulseFraction)).coerceIn(0.1f, 0.7f)

                    // Soft ambient gold fill
                    drawCircle(
                        color = GrimoireColors.GoldContainer.copy(alpha = haloAlpha * 0.35f),
                        radius = haloRadius,
                        center = Offset(cx, cy)
                    )
                    // Pulsing gold halo border
                    drawCircle(
                        color = GrimoireColors.GoldPrimary.copy(alpha = haloAlpha),
                        radius = haloRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = (2.2f * markerAdaptiveScale).coerceIn(1.2f, 4f))
                    )
                }

                if (r.tokenBitmap != null) {
                    val tokenSize = baseTokenSizePx * markerAdaptiveScale
                    val tokenRadius = tokenSize / 2f
                    val clipOval = Path().apply {
                        addOval(Rect(cx - tokenRadius, cy - tokenRadius, cx + tokenRadius, cy + tokenRadius))
                    }
                    clipPath(clipOval) {
                        drawImage(
                            image = r.tokenBitmap,
                            dstOffset = IntOffset((cx - tokenRadius).roundToInt(), (cy - tokenRadius).roundToInt()),
                            dstSize = IntSize(tokenSize.roundToInt().coerceAtLeast(1), tokenSize.roundToInt().coerceAtLeast(1))
                        )
                    }
                    // Border ring in entity color:
                    drawCircle(
                        color = r.color,
                        radius = tokenRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = (2.5f * markerAdaptiveScale).coerceIn(1.5f, 5f))
                    )
                    // Outer dark rim for contrast:
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.45f),
                        radius = tokenRadius + (1f * markerAdaptiveScale),
                        center = Offset(cx, cy),
                        style = Stroke(width = (1f * markerAdaptiveScale).coerceIn(0.8f, 2f))
                    )
                } else {
                    val icon = r.iconBitmap
                    if (icon != null) {
                        val half = currentSize / 2f
                        drawImage(
                            image = icon,
                            dstOffset = IntOffset((cx - half).toInt(), (cy - half).toInt()),
                            dstSize = IntSize(currentSize.toInt().coerceAtLeast(1), currentSize.toInt().coerceAtLeast(1))
                        )
                    } else {
                        drawCircle(
                            color = r.color,
                            radius = currentRadius,
                            center = Offset(cx, cy)
                        )
                    }
                }
            }
        }

        // Context menu overlay (at most 1)
        if (contextMenuMarker != null) {
            val target = contextMenuMarker!!
            val cx = target.marker.x * mapWidthPx
            val cy = target.marker.y * mapHeightPx
            val menuX = with(density) { cx.toDp() }
            val menuY = with(density) { cy.toDp() }

            Box(modifier = Modifier.offset(x = menuX, y = menuY)) {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { contextMenuMarker = null }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            val m = target.marker
                            contextMenuMarker = null
                            onEditMarker(m)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            val m = target.marker
                            contextMenuMarker = null
                            onDeleteMarker(m)
                        }
                    )
                    if (target.marker.isEntityLinked && target.marker.entityId != null) {
                        val ent = entityById[target.marker.entityId]
                        if (ent != null) {
                            DropdownMenuItem(
                                text = { Text("Open entity") },
                                onClick = {
                                    contextMenuMarker = null
                                    onOpenEntityPopout(ent.name)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Empty space context menu to add marker directly via right click
        if (emptySpaceContextMenuPos != null && onAddPin != null) {
            val p = emptySpaceContextMenuPos!!
            val cx = p.x * mapWidthPx
            val cy = p.y * mapHeightPx
            val menuX = with(density) { cx.toDp() }
            val menuY = with(density) { cy.toDp() }

            Box(modifier = Modifier.offset(x = menuX, y = menuY)) {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { emptySpaceContextMenuPos = null }
                ) {
                    DropdownMenuItem(
                        text = { Text("Add Pin here") },
                        onClick = {
                            emptySpaceContextMenuPos = null
                            onAddPin(p.x, p.y)
                        }
                    )
                }
            }
        }
    }
}
