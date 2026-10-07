package de.luca.dungeon_master_manager.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import de.luca.dungeon_master_manager.colorElement
import de.luca.dungeon_master_manager.data.MapData
import de.luca.dungeon_master_manager.data.MapMarker
import de.luca.dungeon_master_manager.random
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

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
    val animatedScale = animateFloatAsState(
        targetValue = scale
    )
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val animatedOffsetX = animateFloatAsState(targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
    )
    val animatedOffsetY = animateFloatAsState(targetValue = offsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
    )

    val viewModel = remember(projectPath) { de.luca.dungeon_master_manager.viewmodel.EntityViewModel(
        projectPath,
        null
    ) }
    val projectEnts by viewModel.projectEntities.collectAsState()
    val globalEnts by viewModel.globalEntities.collectAsState()
    
    // Auto-save function
    fun saveMap() {
        try {
            onContentChange(json.encodeToString(mapData))
        } catch(e: Exception) { e.printStackTrace() }
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

    var viewportSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    Box(modifier = Modifier.fillMaxSize().onSizeChanged { viewportSize = it }) {
        if (imageBitmap != null) {
            var showMarkerDialog by remember { mutableStateOf<MapMarker?>(null) }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            if (!addPinMode && viewportSize.width > 0) {
                                val oldScale = scale
                                val newScale = (scale * zoom).coerceIn(0.1f, 5f)
                                val actualZoom = newScale / oldScale

                                val cx = viewportSize.width / 2f
                                val cy = viewportSize.height / 2f

                                scale = newScale
                                // Pan the map plus compensate for the zoom shifting
                                offsetX = offsetX + pan.x + (centroid.x - cx - offsetX) * (1 - actualZoom)
                                offsetY = offsetY + pan.y + (centroid.y - cy - offsetY) * (1 - actualZoom)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Scroll) {
                                    if (!addPinMode && viewportSize.width > 0) {
                                        // Extract the cursor location to use as the focal point
                                        val cursorPosition = event.changes.first().position
                                        val delta = event.changes.first().scrollDelta.y

                                        val oldScale = scale
                                        val newScale = (scale * if (delta > 0) 0.9f else 1.1f).coerceIn(0.1f, 5f)
                                        val actualZoom = newScale / oldScale

                                        val cx = viewportSize.width / 2f
                                        val cy = viewportSize.height / 2f

                                        scale = newScale
                                        // Shift the map to compensate for the zoom shifting
                                        offsetX = offsetX + (cursorPosition.x - cx - offsetX) * (1 - actualZoom)
                                        offsetY = offsetY + (cursorPosition.y - cy - offsetY) * (1 - actualZoom)

                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                    }
            ) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val imageWidthDp = with(density) { imageBitmap!!.width.toDp() }
                val imageHeightDp = with(density) { imageBitmap!!.height.toDp() }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer(
                            scaleX = animatedScale.value,
                            scaleY = animatedScale.value,
                            translationX = animatedOffsetX.value,
                            translationY = animatedOffsetY.value
                        )
                        .requiredSize(imageWidthDp, imageHeightDp)
                ) {
                    Image(
                        bitmap = imageBitmap!!, 
                        contentDescription = "Map",
                        modifier = Modifier.fillMaxSize().pointerInput(addPinMode) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (addPinMode && event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                                        val position = event.changes.first().position
                                        val x = (position.x / imageBitmap!!.width).coerceIn(0f, 1f)
                                        val y = (position.y / imageBitmap!!.height).coerceIn(0f, 1f)
                                        
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
                    
                    // Render markers
                    mapData.markers.forEach { marker ->
                        val pxX = marker.x * imageBitmap!!.width
                        val pxY = marker.y * imageBitmap!!.height

                        var rightClickMenuOpened by remember { mutableStateOf(false) }
                        
                        var markerColor = Color.Red
                        if (marker.isEntityLinked && marker.entityName != null) {
                            val ent = projectEnts.find { it.name == marker.entityName } ?: globalEnts.find { it.name == marker.entityName }
                            if (ent?.color != null) {
                                try { markerColor = Color(ent.color.toULong()) } catch(e: Exception) {}
                            }
                        } else if (marker.colorULong != null) {
                            try { markerColor = Color(marker.colorULong.toULong()) } catch(e: Exception) {}
                        }
                        
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = with(androidx.compose.ui.platform.LocalDensity.current) { pxX.toDp() } - 12.dp,
                                    y = with(androidx.compose.ui.platform.LocalDensity.current) { pxY.toDp() } - 12.dp
                                )
                                .size(24.dp)
                                .pointerInput(marker) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                                                if (event.buttons.isSecondaryPressed) {
                                                    rightClickMenuOpened = true
                                                    event.changes.forEach { it.consume() }
                                                } else if (event.buttons.isPrimaryPressed) {
                                                    if (marker.isEntityLinked && marker.entityName != null) {
                                                        onOpenEntityPopout(marker.entityName)
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
                                val labelText = if (marker.isEntityLinked) marker.entityName else marker.title
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
                                if(marker.isEntityLinked && marker.entityName != null) {
                                    DropdownMenuItem(
                                        text = { Text("Open entity") },
                                        onClick = {
                                            rightClickMenuOpened = false
                                            onOpenEntityPopout(marker.entityName)
                                        }
                                    )
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
                    val oldScale = scale
                    val newScale = (scale * 1.2f).coerceIn(0.1f, 5f)
                    val actualZoom = newScale / oldScale
                    scale = newScale
                    offsetX *= actualZoom
                    offsetY *= actualZoom
                }) { Text("+") }
                Button(onClick = {
                    val oldScale = scale
                    val newScale = (scale / 1.2f).coerceIn(0.1f, 5f)
                    val actualZoom = newScale / oldScale
                    scale = newScale
                    offsetX *= actualZoom
                    offsetY *= actualZoom
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
    var entityName by remember { mutableStateOf(initialMarker.entityName ?: "") }
    var title by remember { mutableStateOf(initialMarker.title ?: "") }
    var notes by remember { mutableStateOf(initialMarker.notes ?: "") }
    var iconFileName by remember { mutableStateOf(initialMarker.iconFileName) }
    var color by remember { mutableStateOf(
        initialMarker.colorULong ?: Color.random().value.toString()
        )
    }
    
    // TODO: Icon Selection
    
    AlertDialog(
        onDismissRequest = onDismissRequest@{ onDismiss() },
        title = { Text(if (initialMarker.title.isNullOrBlank() && initialMarker.entityName.isNullOrBlank()) "New Map Marker" else "Edit Marker") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isEntityLinked, onCheckedChange = { isEntityLinked = it })
                    Text("Link to Entity")
                }
                
                if (isEntityLinked) {
                    var expanded by remember { mutableStateOf(false) }
                    val allEntities = remember(projectEntities, globalEntities) { projectEntities + globalEntities }
                    val filteredEntities = remember(entityName, allEntities) {
                        if (entityName.isBlank()) allEntities 
                        else allEntities.filter { it.name.contains(entityName, ignoreCase = true) }
                    }
                    
                    Box {
                        OutlinedTextField(
                            value = entityName,
                            onValueChange = { 
                                entityName = it 
                                expanded = true
                            },
                            label = { Text("Search Entity") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = expanded && filteredEntities.isNotEmpty(),
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.5f)
                        ) {
                            filteredEntities.take(5).forEach { ent ->
                                DropdownMenuItem(
                                    text = { Text("${ent.name} (${ent.type})") },
                                    onClick = {
                                        entityName = ent.name
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
            Button(onClick = {
                onSave(initialMarker.copy(
                    isEntityLinked = isEntityLinked,
                    entityName = if (isEntityLinked) entityName else null,
                    title = if (!isEntityLinked) title else null,
                    notes = if (!isEntityLinked) notes else null,
                    iconFileName = iconFileName,
                    colorULong = color
                ))
            }) {
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
