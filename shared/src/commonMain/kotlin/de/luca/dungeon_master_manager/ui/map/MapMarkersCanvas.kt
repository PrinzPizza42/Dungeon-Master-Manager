package de.luca.dungeon_master_manager.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.luca.dungeon_master_manager.data.MapMarker
import de.luca.dungeonmastermanager.database.Entity
import java.awt.Cursor
import kotlin.math.pow
import kotlin.math.sqrt

data class MarkerRenderData(
    val marker: MapMarker,
    val color: Color,
    val iconBitmap: ImageBitmap?,
    val labelText: String?,
    val entityName: String?
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
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val baseMarkerSizePx = with(density) { 24.dp.toPx() }
    val baseRadiusPx = baseMarkerSizePx / 2f

    // O(1) Entity Map
    val entityById = remember(projectEntities, globalEntities) {
        val map = HashMap<String, Entity>()
        for (e in globalEntities) map[e.id] = e
        for (e in projectEntities) map[e.id] = e
        map
    }

    // Reactive cache triggers when icons load
    var iconsLoadedTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(markers) {
        var neededReload = false
        for (marker in markers) {
            val iconName = marker.iconFileName
            if (iconName != null && MarkerIconCache.getIconSync(iconName) == null) {
                MarkerIconCache.getIcon(iconName)
                neededReload = true
            }
        }
        if (neededReload) {
            iconsLoadedTrigger++
        }
    }

    val renderDataList = remember(markers, entityById, iconsLoadedTrigger) {
        markers.map { marker ->
            var markerColor = Color.Red
            var entityName: String? = null

            if (marker.isEntityLinked && marker.entityId != null) {
                val ent = entityById[marker.entityId]
                if (ent != null) {
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
            val labelText = if (marker.isEntityLinked) entityName else marker.title

            MarkerRenderData(
                marker = marker,
                color = markerColor,
                iconBitmap = iconBmp,
                labelText = labelText,
                entityName = entityName
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
                                    val dist = sqrt((p.x - mx) * (p.x - mx) + (p.y - my) * (p.y - my))
                                    if (dist <= hitRadius) return r
                                }
                                return null
                            }

                            when (event.type) {
                                PointerEventType.Move -> {
                                    if (!addPinMode) {
                                        val found = findMarkerAt(pos)
                                        if (hoveredMarker?.marker?.id != found?.marker?.id) {
                                            hoveredMarker = found
                                        }
                                    } else {
                                        hoveredMarker = null
                                    }
                                }

                                PointerEventType.Exit -> {
                                    hoveredMarker = null
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
                                                event.changes.forEach { it.consume() }
                                            } else if (event.buttons.isPrimaryPressed) {
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

        // Hover label overlay (at most 1)
        if (hoveredMarker != null && !hoveredMarker!!.labelText.isNullOrBlank()) {
            val r = hoveredMarker!!
            val cx = r.marker.x * mapWidthPx
            val cy = r.marker.y * mapHeightPx
            val currentScale = scaleProvider()
            val markerAdaptiveScale = (currentScale.pow(-0.85f)).coerceIn(0.02f, 2.0f)
            val markerOffsetPx = with(density) { 14.dp.toPx() } * markerAdaptiveScale
            val verticalOffsetPx = with(density) { 12.dp.toPx() } * markerAdaptiveScale

            val labelX = with(density) { (cx + markerOffsetPx).toDp() }
            val labelY = with(density) { (cy - verticalOffsetPx).toDp() }

            Surface(
                modifier = Modifier
                    .offset(x = labelX, y = labelY)
                    .graphicsLayer {
                        scaleX = markerAdaptiveScale
                        scaleY = markerAdaptiveScale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .wrapContentSize(unbounded = true)
                    .widthIn(max = 160.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                shadowElevation = 0.dp
            ) {
                Text(
                    text = r.labelText!!,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall
                )
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
