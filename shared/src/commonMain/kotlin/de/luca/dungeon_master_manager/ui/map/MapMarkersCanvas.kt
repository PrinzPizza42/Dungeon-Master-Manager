package de.luca.dungeon_master_manager.ui.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
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
import androidx.compose.ui.graphics.PathEffect
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

private data class DropAnimState(
    val pos: Offset,
    val anim: Animatable<Float, AnimationVector1D>
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
    highlightedMarkerId: String? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val baseMarkerSizePx = with(density) { 24.dp.toPx() }
    val baseRadiusPx = baseMarkerSizePx / 2f
    val baseTokenSizePx = with(density) { 29.dp.toPx() }
    val baseTokenRadiusPx = baseTokenSizePx / 2f
    val baseHitExtraPx = with(density) { 6.dp.toPx() }
    val baseTokenBorderWidthPx = with(density) { 2.5.dp.toPx() }
    val baseTokenRimWidthPx = with(density) { 1.dp.toPx() }
    val baseTokenRimOffsetPx = with(density) { 0.8.dp.toPx() }
    val baseHaloBorderWidthPx = with(density) { 2.dp.toPx() }
    val baseHaloOffsetMinPx = with(density) { 3.5.dp.toPx() }
    val baseHaloOffsetRangePx = with(density) { 5.dp.toPx() }
    val baseDotRimWidthPx = with(density) { 1.dp.toPx() }

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

    val coroutineScope = rememberCoroutineScope()
    var hoveredMarker by remember { mutableStateOf<MarkerRenderData?>(null) }
    var contextMenuMarker by remember { mutableStateOf<MarkerRenderData?>(null) }
    var emptySpaceContextMenuPos by remember { mutableStateOf<Offset?>(null) }

    var ghostCursorPos by remember { mutableStateOf<Offset?>(null) }
    var dropAnimation by remember { mutableStateOf<DropAnimState?>(null) }

    LaunchedEffect(addPinMode) {
        if (!addPinMode) {
            ghostCursorPos = null
        }
    }

    val cursorModifier = if (addPinMode) {
        Modifier.pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)))
    } else Modifier

    Box(modifier = modifier.then(cursorModifier)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(renderDataList, mapWidthPx, mapHeightPx, addPinMode, dropAnimation != null) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pos = event.changes.firstOrNull()?.position ?: continue
                            val currentScale = scaleProvider()
                            val markerAdaptiveScale = (currentScale.pow(-0.85f)).coerceIn(0.02f, 2.0f)
                            val hitRadius = (baseRadiusPx + baseHitExtraPx) * markerAdaptiveScale
                            val tokenHitRadius = (baseTokenRadiusPx + baseHitExtraPx) * markerAdaptiveScale

                            fun findMarkerAt(p: Offset): MarkerRenderData? {
                                if (mapWidthPx <= 0f || mapHeightPx <= 0f) return null
                                // Topmost marker first
                                for (i in renderDataList.indices.reversed()) {
                                    val r = renderDataList[i]
                                    val mx = r.marker.x * mapWidthPx
                                    val my = r.marker.y * mapHeightPx
                                    val radius = if (r.tokenBitmap != null) {
                                        tokenHitRadius
                                    } else {
                                        hitRadius
                                    }
                                    val dist = sqrt((p.x - mx) * (p.x - mx) + (p.y - my) * (p.y - my))
                                    if (dist <= radius) return r
                                }
                                return null
                            }

                            when (event.type) {
                                PointerEventType.Move, PointerEventType.Scroll -> {
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

                                    if (addPinMode && dropAnimation == null) {
                                        ghostCursorPos = pos
                                    } else {
                                        ghostCursorPos = null
                                    }
                                }

                                PointerEventType.Exit -> {
                                    hoveredMarker = null
                                    onHoverMarker?.invoke(null)
                                    ghostCursorPos = null
                                }

                                PointerEventType.Press -> {
                                    if (addPinMode && onAddPin != null && dropAnimation == null) {
                                        if (mapWidthPx > 0f && mapHeightPx > 0f && event.buttons.isPrimaryPressed) {
                                            val clickPos = pos
                                            val x = (clickPos.x / mapWidthPx).coerceIn(0f, 1f)
                                            val y = (clickPos.y / mapHeightPx).coerceIn(0f, 1f)
                                            ghostCursorPos = null
                                            event.changes.forEach { it.consume() }

                                            coroutineScope.launch {
                                                val anim = Animatable(0f)
                                                dropAnimation = DropAnimState(clickPos, anim)
                                                anim.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                                                )
                                                onAddPin(x, y)
                                                dropAnimation = null
                                            }
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

            val tokenBorderWidth = baseTokenBorderWidthPx * markerAdaptiveScale
            val tokenRimWidth = baseTokenRimWidthPx * markerAdaptiveScale
            val tokenRimOffset = baseTokenRimOffsetPx * markerAdaptiveScale
            val haloBorderWidth = baseHaloBorderWidthPx * markerAdaptiveScale
            val dotRimWidth = baseDotRimWidthPx * markerAdaptiveScale

            for (r in renderDataList) {
                val cx = r.marker.x * mapWidthPx
                val cy = r.marker.y * mapHeightPx

                val isFocused = hoveredMarker?.marker?.id == r.marker.id ||
                    contextMenuMarker?.marker?.id == r.marker.id ||
                    highlightedMarkerId == r.marker.id
                if (isFocused) {
                    val baseTargetRadius = if (r.tokenBitmap != null) {
                        (baseTokenSizePx * markerAdaptiveScale) / 2f
                    } else {
                        currentRadius
                    }
                    val haloRadius = baseTargetRadius + (baseHaloOffsetMinPx + baseHaloOffsetRangePx * pulseFraction) * markerAdaptiveScale
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
                        style = Stroke(width = haloBorderWidth)
                    )

                    // Extra beacon pulse for sidebar-selected marker
                    if (highlightedMarkerId == r.marker.id) {
                        val beaconRadius = baseTargetRadius + (baseHaloOffsetMinPx + baseHaloOffsetRangePx * 2.2f * pulseFraction) * markerAdaptiveScale
                        val beaconAlpha = (0.75f * (1f - pulseFraction)).coerceIn(0.1f, 0.85f)
                        drawCircle(
                            color = GrimoireColors.GoldBright.copy(alpha = beaconAlpha),
                            radius = beaconRadius,
                            center = Offset(cx, cy),
                            style = Stroke(width = haloBorderWidth * 1.5f)
                        )
                    }
                }

                val isHidden = r.marker.isHidden
                val markerAlpha = if (isHidden) 0.35f else 1.0f
                val dashEffect = if (isHidden) PathEffect.dashPathEffect(floatArrayOf(4f * markerAdaptiveScale, 3f * markerAdaptiveScale)) else null

                if (r.tokenBitmap != null) {
                    val tokenSize = baseTokenSizePx * markerAdaptiveScale
                    val tokenRadius = tokenSize / 2f

                    // Subtle background fill for transparent token images:
                    drawCircle(
                        color = GrimoireColors.SurfaceLow.copy(alpha = 0.5f * markerAlpha),
                        radius = tokenRadius,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = r.color.copy(alpha = 0.2f * markerAlpha),
                        radius = tokenRadius,
                        center = Offset(cx, cy)
                    )

                    val clipOval = Path().apply {
                        addOval(Rect(cx - tokenRadius, cy - tokenRadius, cx + tokenRadius, cy + tokenRadius))
                    }
                    clipPath(clipOval) {
                        drawImage(
                            image = r.tokenBitmap,
                            dstOffset = IntOffset((cx - tokenRadius).roundToInt(), (cy - tokenRadius).roundToInt()),
                            dstSize = IntSize(tokenSize.roundToInt().coerceAtLeast(1), tokenSize.roundToInt().coerceAtLeast(1)),
                            alpha = markerAlpha
                        )
                    }
                    // Border ring in entity color:
                    drawCircle(
                        color = r.color.copy(alpha = markerAlpha),
                        radius = tokenRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = tokenBorderWidth, pathEffect = dashEffect)
                    )
                    // Outer dark rim for contrast:
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.45f * markerAlpha),
                        radius = tokenRadius + tokenRimOffset,
                        center = Offset(cx, cy),
                        style = Stroke(width = tokenRimWidth)
                    )
                } else {
                    val icon = r.iconBitmap
                    if (icon != null) {
                        val half = currentSize / 2f
                        drawImage(
                            image = icon,
                            dstOffset = IntOffset((cx - half).toInt(), (cy - half).toInt()),
                            dstSize = IntSize(currentSize.toInt().coerceAtLeast(1), currentSize.toInt().coerceAtLeast(1)),
                            alpha = markerAlpha
                        )
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.45f * markerAlpha),
                            radius = currentRadius + tokenRimOffset,
                            center = Offset(cx, cy),
                            style = Stroke(width = tokenRimWidth, pathEffect = dashEffect)
                        )
                    } else {
                        drawCircle(
                            color = r.color.copy(alpha = markerAlpha),
                            radius = currentRadius,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.45f * markerAlpha),
                            radius = currentRadius,
                            center = Offset(cx, cy),
                            style = Stroke(width = dotRimWidth, pathEffect = dashEffect)
                        )
                    }
                }
            }

            // Ghost Pin Preview under Cursor (Add Pin Mode)
            if (addPinMode && ghostCursorPos != null && dropAnimation == null) {
                val gPos = ghostCursorPos!!
                val ghostRadius = currentRadius * 1.25f

                // Pulsing ambient gold glow around ghost pin
                val ghostHaloRadius = ghostRadius + (baseHaloOffsetMinPx + baseHaloOffsetRangePx * pulseFraction) * markerAdaptiveScale
                val ghostHaloAlpha = (0.35f + 0.35f * (1f - pulseFraction)).coerceIn(0.15f, 0.75f)

                drawCircle(
                    color = GrimoireColors.GoldContainer.copy(alpha = ghostHaloAlpha * 0.35f),
                    radius = ghostHaloRadius,
                    center = gPos
                )
                drawCircle(
                    color = GrimoireColors.GoldPrimary.copy(alpha = ghostHaloAlpha * 0.75f),
                    radius = ghostHaloRadius,
                    center = gPos,
                    style = Stroke(width = haloBorderWidth * 1.15f)
                )

                // Ghost Pin Body (Red with 65% alpha)
                drawCircle(
                    color = Color.Red.copy(alpha = 0.65f),
                    radius = ghostRadius,
                    center = gPos
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.5f),
                    radius = ghostRadius,
                    center = gPos,
                    style = Stroke(width = dotRimWidth * 1.25f)
                )
                // Center precision dot
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = ghostRadius * 0.22f,
                    center = gPos
                )
            }

            // Drop Animation Transition (Ghost Fades Out & Shrinks -> Real Marker Fades In & Pops + Golden Ripple)
            if (dropAnimation != null) {
                val drop = dropAnimation!!
                val t = drop.anim.value
                val dPos = drop.pos

                // 1. Ghost Pin Fade-out & Shrink (1.25x -> 1.0x, 0.65f -> 0f)
                val ghostScale = 1.25f - 0.25f * t
                val ghostAlpha = (0.65f * (1f - t)).coerceIn(0f, 1f)
                val ghostR = currentRadius * ghostScale
                if (ghostAlpha > 0.01f) {
                    drawCircle(
                        color = Color.Red.copy(alpha = ghostAlpha),
                        radius = ghostR,
                        center = dPos
                    )
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.5f * (1f - t)),
                        radius = ghostR,
                        center = dPos,
                        style = Stroke(width = dotRimWidth * ghostScale)
                    )
                }

                // 2. Real Marker Fade-in & Pop (0.8x -> 1.0x, 0f -> 1f)
                val markerScale = 0.8f + 0.2f * t
                val markerAlpha = t.coerceIn(0f, 1f)
                val markerR = currentRadius * markerScale
                drawCircle(
                    color = Color.Red.copy(alpha = markerAlpha),
                    radius = markerR,
                    center = dPos
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.45f * markerAlpha),
                    radius = markerR,
                    center = dPos,
                    style = Stroke(width = dotRimWidth * markerScale)
                )

                // 3. Golden Ripple Shockwave Expanding Outward
                val maxRippleExtra = with(density) { 18.dp.toPx() } * markerAdaptiveScale
                val rippleRadius = currentRadius + maxRippleExtra * t
                val rippleAlpha = (0.85f * (1f - t * t)).coerceIn(0f, 1f)
                val rippleStrokeWidth = (with(density) { 2.dp.toPx() } * markerAdaptiveScale * (1f - 0.4f * t)).coerceAtLeast(0.5f)

                drawCircle(
                    color = GrimoireColors.GoldContainer.copy(alpha = rippleAlpha * 0.35f),
                    radius = rippleRadius * 0.92f,
                    center = dPos
                )
                drawCircle(
                    color = GrimoireColors.GoldPrimary.copy(alpha = rippleAlpha),
                    radius = rippleRadius,
                    center = dPos,
                    style = Stroke(width = rippleStrokeWidth)
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
