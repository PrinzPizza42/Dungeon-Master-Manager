package de.luca.dungeon_master_manager.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.pow
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import de.luca.dungeon_master_manager.colorElement
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import de.luca.dungeon_master_manager.data.AppSettingsRepository
import de.luca.dungeon_master_manager.ui.theme.GrimoireDeskSurface
import de.luca.dungeon_master_manager.ui.theme.GrimoireAmbientChamber
import dungeon_master_manager.shared.generated.resources.Res
import dungeon_master_manager.shared.generated.resources.tabletop_hero
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import de.luca.dungeon_master_manager.ui.theme.TabletopPropsDeskLayer
import de.luca.dungeon_master_manager.ui.theme.TabletopPropsMapAccentsLayer
import de.luca.dungeon_master_manager.ui.theme.RealisticTableDropShadow
import de.luca.dungeon_master_manager.ui.theme.TabletopFurPelt
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import de.luca.dungeon_master_manager.ui.theme.MapBoundaryFogOverlay
import de.luca.dungeon_master_manager.ui.theme.FogOfWarSurface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import de.luca.dungeon_master_manager.data.MapData
import de.luca.dungeon_master_manager.data.MapMarker
import de.luca.dungeon_master_manager.random
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

private const val MIN_MAP_ZOOM = 0.48f
private const val MAX_MAP_ZOOM = 50.0f

@Composable
fun MapViewer(
    projectPath: String,
    content: String,
    onContentChange: (String) -> Unit,
    onOpenEntityPopout: (String) -> Unit
) {
    val json = remember { Json { ignoreUnknownKeys = true } }
    
    var mapData by remember(content) {
        mutableStateOf(
            try {
                if (content.isBlank()) MapData("") else json.decodeFromString(content)
            } catch (e: Exception) {
                MapData("")
            }
        )
    }

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var targetScale by remember { mutableStateOf(1f) }
    var targetOffsetX by remember { mutableStateOf(0f) }
    var targetOffsetY by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    var zoomJob by remember { mutableStateOf<Job?>(null) }

    val viewModel = remember(projectPath) { de.luca.dungeon_master_manager.viewmodel.EntityViewModel(
        projectPath,
        null
    ) }
    val projectEnts by viewModel.projectEntities.collectAsState()
    val globalEnts by viewModel.globalEntities.collectAsState()
    val isLoaded by viewModel.isLoaded.collectAsState()
    
    // Auto-save function
    fun saveMap() {
        try {
            onContentChange(json.encodeToString(mapData))
        } catch(e: Exception) { e.printStackTrace() }
    }

    LaunchedEffect(isLoaded, projectEnts, globalEnts) {
        if (isLoaded) {
            val validMarkers = mapData.markers.filterNot { marker ->
                marker.isEntityLinked && (marker.entityId == null || (projectEnts.none { it.id == marker.entityId } && globalEnts.none { it.id == marker.entityId }))
            }
            if (validMarkers.size != mapData.markers.size) {
                mapData = mapData.copy(markers = validMarkers)
                saveMap()
            }
        }
    }

    val visibleMarkers = remember(mapData.markers, projectEnts, globalEnts, isLoaded) {
        if (!isLoaded) {
            mapData.markers
        } else {
            mapData.markers.filterNot { marker ->
                marker.isEntityLinked && (marker.entityId == null || (projectEnts.none { it.id == marker.entityId } && globalEnts.none { it.id == marker.entityId }))
            }
        }
    }

    if (mapData.imagePath.isBlank()) {
        ImageSelector(projectPath) { selectedRelativePath ->
            mapData = mapData.copy(imagePath = selectedRelativePath)
            saveMap()
        }
        return
    }

    val imageFile = File(projectPath, mapData.imagePath)
    if (!imageFile.exists()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Image not found: ${mapData.imagePath}", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { 
                    mapData = mapData.copy(imagePath = "")
                    saveMap()
                }) {
                    Text("Select another image")
                }
            }
        }
        return
    }

    var imageBitmap by remember(imageFile.absolutePath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(imageFile.absolutePath) {
        try {
            imageFile.inputStream().use {
                imageBitmap = loadImageBitmap(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var addPinMode by remember { mutableStateOf(false) }
    val appSettings by AppSettingsRepository.settings.collectAsState()
    val isEcoMode = appSettings.isBatterySaverMode

    var viewportSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    Box(modifier = Modifier.fillMaxSize().clipToBounds().onSizeChanged { viewportSize = it }) {
        // Ambient background: Full stone chamber in normal mode, clean solid slate in Battery Saver mode
        if (!isEcoMode) {
            GrimoireAmbientChamber(
                modifier = Modifier.fillMaxSize(),
                panOffset = androidx.compose.ui.geometry.Offset(offsetX, offsetY),
                zoomScale = scale
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(GrimoireColors.Background))
        }

        if (imageBitmap != null) {
            var showMarkerDialog by remember { mutableStateOf<MapMarker?>(null) }

            val density = androidx.compose.ui.platform.LocalDensity.current
            val imgW = imageBitmap!!.width.toFloat()
            val imgH = imageBitmap!!.height.toFloat()

            // Viewport-normalized base display size for the map:
            // At zoom = 1.0f, the map occupies ~72% of the viewport dimensions, maintaining its native aspect ratio.
            val vw = if (viewportSize.width > 0) viewportSize.width.toFloat() else 1920f
            val vh = if (viewportSize.height > 0) viewportSize.height.toFloat() else 1080f
            val targetMapW = vw * 0.72f
            val targetMapH = vh * 0.72f
            val fitScale = minOf(targetMapW / imgW, targetMapH / imgH)
            val baseMapWidth = imgW * fitScale
            val baseMapHeight = imgH * fitScale

            val baseMapWidthDp = with(density) { baseMapWidth.toDp() }
            val baseMapHeightDp = with(density) { baseMapHeight.toDp() }

            // Grand War Table dimensions: Generously frames the map with ample desk margins
            val marginX = maxOf(baseMapWidth * 0.30f, with(density) { 180.dp.toPx() })
            val marginY = maxOf(baseMapHeight * 0.30f, with(density) { 140.dp.toPx() })
            val tableWidth = baseMapWidth + marginX * 2f
            val tableHeight = baseMapHeight + marginY * 2f
            val tableWidthDp = with(density) { tableWidth.toDp() }
            val tableHeightDp = with(density) { tableHeight.toDp() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, _, _ ->
                            if (!addPinMode && viewportSize.width > 0) {
                                zoomJob?.cancel() // Cancel any ongoing zoom animation immediately on drag
                                val effectiveW = if (!isEcoMode) tableWidth else baseMapWidth
                                val effectiveH = if (!isEcoMode) tableHeight else baseMapHeight
                                val maxPanX = (viewportSize.width / 2f) + (effectiveW * scale / 2f) - 100f
                                val maxPanY = (viewportSize.height / 2f) + (effectiveH * scale / 2f) - 100f

                                var nextX = offsetX + pan.x
                                var nextY = offsetY + pan.y
                                if (maxPanX > 0) nextX = nextX.coerceIn(-maxPanX, maxPanX)
                                if (maxPanY > 0) nextY = nextY.coerceIn(-maxPanY, maxPanY)

                                offsetX = nextX
                                offsetY = nextY
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
                                if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Scroll) {
                                    if (!addPinMode && viewportSize.width > 0) {
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
                                        val newTarget = (targetScale * factor).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)

                                        if (newTarget != oldTarget) {
                                            val actualZoom = newTarget / oldTarget
                                            val cx = viewportSize.width / 2f
                                            val cy = viewportSize.height / 2f
                                            var newTargetX = targetOffsetX + (cursorPosition.x - cx - targetOffsetX) * (1 - actualZoom)
                                            var newTargetY = targetOffsetY + (cursorPosition.y - cy - targetOffsetY) * (1 - actualZoom)

                                            val effectiveW = if (!isEcoMode) tableWidth else baseMapWidth
                                            val effectiveH = if (!isEcoMode) tableHeight else baseMapHeight
                                            val maxPanX = (viewportSize.width / 2f) + (effectiveW * newTarget / 2f) - 100f
                                            val maxPanY = (viewportSize.height / 2f) + (effectiveH * newTarget / 2f) - 100f
                                            if (maxPanX > 0) newTargetX = newTargetX.coerceIn(-maxPanX, maxPanX)
                                            if (maxPanY > 0) newTargetY = newTargetY.coerceIn(-maxPanY, maxPanY)

                                            targetScale = newTarget
                                            targetOffsetX = newTargetX
                                            targetOffsetY = newTargetY

                                            zoomJob?.cancel()
                                            zoomJob = coroutineScope.launch {
                                                val startScale = scale
                                                val startX = offsetX
                                                val startY = offsetY
                                                val anim = Animatable(0f)
                                                anim.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
                                                ) {
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
                    }
            ) {
                // Grand War Table (Normal) or Pure Map Frame (Battery Saver)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .size(
                            if (!isEcoMode) tableWidthDp else baseMapWidthDp,
                            if (!isEcoMode) tableHeightDp else baseMapHeightDp
                        )
                ) {
                    if (!isEcoMode) {
                        // 0. Fur Pelt Rug on stone floor peeking out from under the table
                        TabletopFurPelt(
                            tableWidth = tableWidthDp,
                            tableHeight = tableHeightDp
                        )

                        // 1. Realistic Deep Table Drop Shadow on Chamber Stone Floor
                        RealisticTableDropShadow(
                            shape = RoundedCornerShape(10.dp)
                        )

                        // 2. Grand War Table Surface (Cropped pure wood & carved border)
                        Image(
                            painter = painterResource(Res.drawable.tabletop_hero),
                            contentDescription = "Grand War Table",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }

                    // 3. Map Parchment & Accents Container
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(baseMapWidthDp, baseMapHeightDp)
                            .then(
                                if (isEcoMode) {
                                    Modifier
                                        .background(GrimoireColors.SurfaceCard, RoundedCornerShape(6.dp))
                                        .border(BorderStroke(1.dp, GrimoireColors.BorderBronze), RoundedCornerShape(6.dp))
                                } else Modifier
                            )
                    ) {
                        if (!isEcoMode) {
                            // 0. Tabletop Props on the wood desk (Dice, Coins, Quill)
                            TabletopPropsDeskLayer(
                                mapWidth = baseMapWidthDp,
                                mapHeight = baseMapHeightDp
                            )

                            // 1. Soft Parchment Drop Shadow on the wooden desk
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        shadowElevation = 18.dp.toPx()
                                        shape = RoundedCornerShape(8.dp)
                                        clip = false
                                    }
                            )
                        }

                    // 2. Crisp Parchment Map Image
                    Image(
                        bitmap = imageBitmap!!, 
                        contentDescription = "Map",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .pointerInput(addPinMode) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (addPinMode && event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                                        val position = event.changes.first().position
                                        val x = (position.x / baseMapWidth).coerceIn(0f, 1f)
                                        val y = (position.y / baseMapHeight).coerceIn(0f, 1f)
                                        
                                        val newMarker = MapMarker(
                                            id = UUID.randomUUID().toString(),
                                            x = x,
                                            y = y,
                                            isEntityLinked = false
                                        )
                                        showMarkerDialog = newMarker
                                        addPinMode = false
                                    }
                                }
                            }
                        }
                    )

                    if (!isEcoMode) {
                        // Tabletop Accents holding down the parchment map (Compass, Wax Seal)
                        TabletopPropsMapAccentsLayer(
                            mapWidth = baseMapWidthDp,
                            mapHeight = baseMapHeightDp
                        )
                    }

                    // Render markers
                    visibleMarkers.forEach { marker ->
                        val pxX = marker.x * baseMapWidth
                        val pxY = marker.y * baseMapHeight

                        var rightClickMenuOpened by remember { mutableStateOf(false) }
                        
                        var markerColor = Color.Red
                        if (marker.isEntityLinked && marker.entityId != null) {
                            val ent = projectEnts.find { it.id == marker.entityId } ?: globalEnts.find { it.id == marker.entityId }
                            if (ent?.color != null) {
                                try { markerColor = Color(ent.color.toULong()) } catch(e: Exception) {}
                            }
                        } else if (marker.colorULong != null) {
                            try { markerColor = Color(marker.colorULong.toULong()) } catch(e: Exception) {}
                        }
                        
                        // Adaptive scale multiplier: gently dampens pin scaling (~18.5dp at min zoom to ~31.2dp at max zoom)
                        val markerAdaptiveScale = (scale.pow(-0.85f)).coerceIn(0.02f, 2.0f)

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = with(androidx.compose.ui.platform.LocalDensity.current) { pxX.toDp() } - 12.dp,
                                    y = with(androidx.compose.ui.platform.LocalDensity.current) { pxY.toDp() } - 12.dp
                                )
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = markerAdaptiveScale
                                    scaleY = markerAdaptiveScale
                                }
                                .pointerInput(marker) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                                                if (event.buttons.isSecondaryPressed) {
                                                    rightClickMenuOpened = true
                                                    event.changes.forEach { it.consume() }
                                                } else if (event.buttons.isPrimaryPressed) {
                                                    if (marker.isEntityLinked && marker.entityId != null) {
                                                        val ent = projectEnts.find { it.id == marker.entityId } ?: globalEnts.find { it.id == marker.entityId }
                                                        if (ent != null) {
                                                            onOpenEntityPopout(ent.name)
                                                        }
                                                    } else {
                                                        showMarkerDialog = marker
                                                    }
                                                    event.changes.forEach { it.consume() }
                                                }
                                            }
                                        }
                                    }
                                }
                        ) {
                            var isHovered by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Enter) isHovered = true
                                        else if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Exit) isHovered = false
                                    }
                                }
                            }) {
                                if (marker.iconFileName != null) {
                                    val iconFile = File(File(System.getProperty("user.home"), ".dungeon-master-manager/.global/Icons"), marker.iconFileName!!)
                                    if (iconFile.exists()) {
                                        ThumbnailImage(file = iconFile, modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small))
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize().background(markerColor, shape = CircleShape))
                                    }
                                } else {
                                    Box(modifier = Modifier.fillMaxSize().background(markerColor, shape = CircleShape))
                                }
                            }
                            
                            if (isHovered) {
                                val ent = if (marker.isEntityLinked && marker.entityId != null) {
                                    projectEnts.find { it.id == marker.entityId } ?: globalEnts.find { it.id == marker.entityId }
                                } else null
                                val labelText = if (marker.isEntityLinked) ent?.name else marker.title
                                if (!labelText.isNullOrBlank()) {
                                    Surface(
                                        modifier = Modifier
                                            .offset(x = 24.dp, y = (-12).dp)
                                            .wrapContentSize(unbounded = true)
                                            .widthIn(max = 150.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(labelText, modifier = Modifier.padding(4.dp), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = rightClickMenuOpened,
                                onDismissRequest = { rightClickMenuOpened = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = {
                                        rightClickMenuOpened = false
                                        showMarkerDialog = marker
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        rightClickMenuOpened = false
                                        mapData = mapData.copy(markers = mapData.markers.filter { it.id != marker.id })
                                        saveMap()
                                    }
                                )
                                if(marker.isEntityLinked && marker.entityId != null) {
                                    val ent = projectEnts.find { it.id == marker.entityId } ?: globalEnts.find { it.id == marker.entityId }
                                    if (ent != null) {
                                        DropdownMenuItem(
                                            text = { Text("Open entity") },
                                            onClick = {
                                                rightClickMenuOpened = false
                                                onOpenEntityPopout(ent.name)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
            
            if (showMarkerDialog != null) {
                MapMarkerDialog(
                    initialMarker = showMarkerDialog!!,
                    projectEntities = projectEnts,
                    globalEntities = globalEnts,
                    onDismiss = { showMarkerDialog = null },
                    onSave = { updatedMarker ->
                        val existingIndex = mapData.markers.indexOfFirst { it.id == updatedMarker.id }
                        val newMarkers = mapData.markers.toMutableList()
                        if (existingIndex >= 0) {
                            newMarkers[existingIndex] = updatedMarker
                        } else {
                            newMarkers.add(updatedMarker)
                        }
                        mapData = mapData.copy(markers = newMarkers)
                        saveMap()
                        showMarkerDialog = null
                    },
                    onDelete = { markerToDelete ->
                        mapData = mapData.copy(markers = mapData.markers.filter { it.id != markerToDelete.id })
                        saveMap()
                        showMarkerDialog = null
                    }
                )
            }


        }

        // Toolbar
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val factor = 1.25f
                    val oldTarget = targetScale
                    val newTarget = (targetScale * factor).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)
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
                            anim.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
                            ) {
                                scale = startScale + (targetScale - startScale) * value
                                offsetX = startX + (targetOffsetX - startX) * value
                                offsetY = startY + (targetOffsetY - startY) * value
                            }
                        }
                    }
                }) { Text("+") }
                Button(onClick = {
                    val factor = 0.80f
                    val oldTarget = targetScale
                    val newTarget = (targetScale * factor).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)
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
                            anim.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
                            ) {
                                scale = startScale + (targetScale - startScale) * value
                                offsetX = startX + (targetOffsetX - startX) * value
                                offsetY = startY + (targetOffsetY - startY) * value
                            }
                        }
                    }
                }) { Text("-") }
                Button(onClick = { addPinMode = !addPinMode }, colors = ButtonDefaults.buttonColors(containerColor = if (addPinMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)) {
                    Text(if (addPinMode) "Click Map to Place" else "Add Pin")
                }
            }
        }
    }
}

@Composable
fun ImageSelector(projectPath: String, onSelect: (String) -> Unit) {
    var images by remember { mutableStateOf<List<File>>(emptyList()) }
    LaunchedEffect(projectPath) {
        val root = File(projectPath)
        images = root.walkTopDown()
            .filter { it.isFile && (it.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp")) }
            .toList()
    }
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.fillMaxWidth(0.8f).fillMaxHeight(0.8f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Map Image", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                if (images.isEmpty()) {
                    Text("No images found in project folder.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(images) { img ->
                            val relative = img.relativeTo(File(projectPath)).path
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(relative) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ThumbnailImage(file = img, modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.small))
                                Spacer(Modifier.width(16.dp))
                                Text(relative)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThumbnailImage(file: File, modifier: Modifier = Modifier) {
    var bitmap by remember(file.absolutePath) { mutableStateOf<ImageBitmap?>(null) }
    
    LaunchedEffect(file.absolutePath) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                file.inputStream().use {
                    bitmap = androidx.compose.ui.res.loadImageBitmap(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!, 
            contentDescription = null, 
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    } else {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}


@Composable
fun MapMarkerDialog(
    initialMarker: MapMarker,
    projectEntities: List<de.luca.dungeonmastermanager.database.Entity>,
    globalEntities: List<de.luca.dungeonmastermanager.database.Entity>,
    onDismiss: () -> Unit,
    onSave: (MapMarker) -> Unit,
    onDelete: (MapMarker) -> Unit
) {
    var isEntityLinked by remember { mutableStateOf(initialMarker.isEntityLinked) }
    
    // Find initial entity to display its name
    val initEnt = if (initialMarker.entityId != null) {
        projectEntities.find { it.id == initialMarker.entityId } ?: globalEntities.find { it.id == initialMarker.entityId }
    } else null
    
    var entitySearch by remember { mutableStateOf(initEnt?.name ?: "") }
    var selectedEntityId by remember { mutableStateOf(initialMarker.entityId) }
    var title by remember { mutableStateOf(initialMarker.title ?: "") }
    var notes by remember { mutableStateOf(initialMarker.notes ?: "") }
    var iconFileName by remember { mutableStateOf(initialMarker.iconFileName) }
    var color by remember { mutableStateOf(
        initialMarker.colorULong ?: Color.random().value.toString()
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismissRequest@{ onDismiss() },
        title = { Text(if (initialMarker.title.isNullOrBlank() && initialMarker.entityId.isNullOrBlank()) "New Map Marker" else "Edit Marker") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isEntityLinked, onCheckedChange = { isEntityLinked = it })
                    Text("Link to Entity")
                }
                
                if (isEntityLinked) {
                    var expanded by remember { mutableStateOf(false) }
                    val allEntities = remember(projectEntities, globalEntities) { projectEntities + globalEntities }
                    val filteredEntities = remember(entitySearch, allEntities) {
                        if (entitySearch.isBlank()) allEntities 
                        else allEntities.filter { it.name.contains(entitySearch, ignoreCase = true) }
                    }
                    
                    Box {
                        OutlinedTextField(
                            value = entitySearch,
                            onValueChange = { 
                                entitySearch = it 
                                expanded = true
                                // If they type and it doesn't match exactly, we clear the ID so they have to select one
                                val perfectMatch = allEntities.find { e -> e.name.equals(it, ignoreCase = true) }
                                selectedEntityId = perfectMatch?.id
                            },
                            label = { Text("Search Entity") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) expanded = true
                                }
                        )
                        DropdownMenu(
                            expanded = expanded && filteredEntities.isNotEmpty(),
                            onDismissRequest = { expanded = false },
                            properties = androidx.compose.ui.window.PopupProperties(focusable = false),
                            modifier = Modifier.fillMaxWidth(0.5f)
                        ) {
                            filteredEntities.take(5).forEach { ent ->
                                DropdownMenuItem(
                                    text = { Text("${ent.name} (${ent.type})") },
                                    onClick = {
                                        entitySearch = ent.name
                                        selectedEntityId = ent.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Marker Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                }

                if(!isEntityLinked) {
                    Spacer(Modifier.height(16.dp))
                    Text("Marker Icon", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    var availableIcons by remember { mutableStateOf<List<File>>(emptyList()) }

                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val iconsDir = File(System.getProperty("user.home"), ".dungeon-master-manager/.global/Icons")
                            iconsDir.mkdirs()
                            availableIcons = iconsDir.listFiles()?.filter { it.isFile && it.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp") }?.sortedBy { it.name } ?: emptyList()
                        }
                    }

                    val showColorPicker = remember { mutableStateOf(false) }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            // Use only color
                            val parsedColor = try {
                                Color(color.toULong())
                            } catch (e: Exception) {
                                Color.White
                            }

                            colorElement(
                                currentColor = parsedColor,
                                showPopup = showColorPicker,
                                onClick = { inputColor ->
                                    color = inputColor.value.toString()
                                }
                            )
                        }

                        // Use icon
                        items(availableIcons) { iconFile ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(if (iconFileName == iconFile.name) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { iconFileName = iconFile.name }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ThumbnailImage(file = iconFile, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(initialMarker.copy(
                        isEntityLinked = isEntityLinked,
                        entityId = if (isEntityLinked) selectedEntityId else null,
                        title = if (!isEntityLinked) title else null,
                        notes = if (!isEntityLinked) notes else null,
                        iconFileName = iconFileName,
                        colorULong = color
                    ))
                },
                enabled = if (isEntityLinked) selectedEntityId != null else true
            ) {
                Text("Save Marker")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDelete(initialMarker) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text("Delete")
            }
        }
    )
}
