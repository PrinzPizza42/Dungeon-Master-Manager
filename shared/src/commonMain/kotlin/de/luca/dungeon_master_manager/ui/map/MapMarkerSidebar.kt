package de.luca.dungeon_master_manager.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.luca.dungeonmastermanager.database.Entity
import de.luca.dungeon_master_manager.data.MapMarker
import de.luca.dungeon_master_manager.ui.components.EntityImageLoader
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import de.luca.dungeon_master_manager.ui.theme.GrimoireDivider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

val MAP_MARKER_SIDEBAR_WIDTH = 350.dp

enum class MarkerFilterCategory(val label: String) {
    ALL("All"),
    ENTITIES("Entities"),
    PINS("Pins")
}

enum class MarkerSortOrder(val label: String) {
    VISIBLE_FIRST("Visible First"),
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)")
}

data class SidebarMarkerItem(
    val marker: MapMarker,
    val title: String,
    val subtitle: String?,
    val entityTypeName: String?,
    val color: Color,
    val iconBitmap: ImageBitmap?,
    val tokenBitmap: ImageBitmap?,
    val linkedEntity: Entity?,
    val isEntityLinked: Boolean,
    val isHidden: Boolean
)

@Composable
fun MapMarkerSidebar(
    markers: List<MapMarker>,
    projectEntities: List<Entity>,
    globalEntities: List<Entity>,
    projectPath: String,
    highlightedMarkerId: String?,
    onSelectMarker: (MapMarker) -> Unit,
    onEditMarker: (MapMarker) -> Unit,
    onOpenEntityPopout: ((String) -> Unit)?,
    onUpdateMarker: (MapMarker) -> Unit,
    onUpdateMarkers: (List<MapMarker>) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MarkerFilterCategory.ALL) }
    var sortOrder by remember { mutableStateOf(MarkerSortOrder.VISIBLE_FIRST) }
    var isSortMenuOpen by remember { mutableStateOf(false) }

    // O(1) entity lookup map
    val entityById = remember(projectEntities, globalEntities) {
        val map = HashMap<String, Entity>()
        for (e in globalEntities) map[e.id] = e
        for (e in projectEntities) map[e.id] = e
        map
    }

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

    // Build items with icons and colors
    val allItems = remember(markers, entityById, iconsLoadedTrigger, projectPath) {
        markers.map { marker ->
            var markerColor = Color.Red
            var linkedEnt: Entity? = null
            var entityName: String? = null

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

            val title = if (marker.isEntityLinked) {
                entityName ?: "Unknown Entity"
            } else {
                marker.title.takeUnless { it.isNullOrBlank() } ?: "Custom Pin"
            }

            val subtitle = if (marker.isEntityLinked) {
                linkedEnt?.type
            } else {
                marker.notes?.takeUnless { it.isBlank() }
            }

            SidebarMarkerItem(
                marker = marker,
                title = title,
                subtitle = subtitle,
                entityTypeName = linkedEnt?.type,
                color = markerColor,
                iconBitmap = iconBmp,
                tokenBitmap = tokenBmp,
                linkedEntity = linkedEnt,
                isEntityLinked = marker.isEntityLinked,
                isHidden = marker.isHidden
            )
        }
    }

    // Filter items based on category chip and search query
    val filteredItems = remember(allItems, searchQuery, selectedCategory) {
        val query = searchQuery.trim().lowercase()
        allItems.filter { item ->
            val matchesCategory = when (selectedCategory) {
                MarkerFilterCategory.ALL -> true
                MarkerFilterCategory.ENTITIES -> item.isEntityLinked
                MarkerFilterCategory.PINS -> !item.isEntityLinked
            }
            if (!matchesCategory) return@filter false

            if (query.isEmpty()) return@filter true

            item.title.lowercase().contains(query) ||
                (item.subtitle?.lowercase()?.contains(query) == true) ||
                (item.marker.notes?.lowercase()?.contains(query) == true) ||
                (item.entityTypeName?.lowercase()?.contains(query) == true)
        }
    }

    // Group items into categories with sorting
    val groupedByCategory = remember(filteredItems, sortOrder) {
        val groups = LinkedHashMap<String, MutableList<SidebarMarkerItem>>()
        for (item in filteredItems) {
            val categoryName = if (item.isEntityLinked) {
                item.linkedEntity?.type?.takeUnless { it.isBlank() } ?: "Entity"
            } else {
                "Pins"
            }
            groups.getOrPut(categoryName) { mutableListOf() }.add(item)
        }

        groups.mapValues { (_, list) ->
            when (sortOrder) {
                MarkerSortOrder.VISIBLE_FIRST -> list.sortedWith(
                    compareBy<SidebarMarkerItem> { it.marker.isHidden }.thenBy { it.title.lowercase() }
                )
                MarkerSortOrder.NAME_ASC -> list.sortedBy { it.title.lowercase() }
                MarkerSortOrder.NAME_DESC -> list.sortedByDescending { it.title.lowercase() }
            }
        }
    }

    // Accordion expansion state per category
    var expandedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var hasInitializedCategories by remember { mutableStateOf(false) }

    LaunchedEffect(groupedByCategory.keys) {
        if (!hasInitializedCategories && groupedByCategory.isNotEmpty()) {
            expandedCategories = groupedByCategory.keys.toSet()
            hasInitializedCategories = true
        } else if (searchQuery.isNotBlank()) {
            // Keep matching categories open while searching
            expandedCategories = expandedCategories + groupedByCategory.keys
        }
    }

    val allMarkersCount = markers.size
    val visibleMarkersCount = markers.count { !it.isHidden }
    val allMarkersHidden = markers.isNotEmpty() && visibleMarkersCount == 0

    Surface(
        modifier = modifier
            .width(MAP_MARKER_SIDEBAR_WIDTH)
            .fillMaxHeight(),
        color = GrimoireColors.SurfaceCard,
        border = BorderStroke(1.dp, GrimoireColors.BorderBronze),
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    tint = GrimoireColors.GoldPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Map Markers",
                    color = GrimoireColors.GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = GrimoireColors.BronzeContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$visibleMarkersCount/$allMarkersCount",
                        color = GrimoireColors.GoldBright,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.weight(1f))

                // Master Toggle All Eye
                if (markers.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            val newHiddenState = !allMarkersHidden
                            val updated = markers.map { it.copy(isHidden = newHiddenState) }
                            onUpdateMarkers(updated)
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (allMarkersHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (allMarkersHidden) "Show all markers on map" else "Hide all markers on map",
                            tint = if (allMarkersHidden) GrimoireColors.ParchmentDim else GrimoireColors.GoldPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Sidebar",
                        tint = GrimoireColors.ParchmentMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            GrimoireDivider(thickness = 1.dp)

            // Search & Filter Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var isSearchFocused by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(GrimoireColors.SurfaceLow, RoundedCornerShape(6.dp))
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isSearchFocused) GrimoireColors.GoldPrimary else GrimoireColors.BorderBronze
                            ),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchFocused) GrimoireColors.GoldPrimary else GrimoireColors.ParchmentMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search markers...",
                                    color = GrimoireColors.ParchmentDim,
                                    fontSize = 13.sp
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = GrimoireColors.ParchmentLight,
                                    fontSize = 13.sp
                                ),
                                cursorBrush = SolidColor(GrimoireColors.GoldPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isSearchFocused = it.isFocused }
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = GrimoireColors.ParchmentMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Category Filter Chips + Sort Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MarkerFilterCategory.entries.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            val count = when (cat) {
                                MarkerFilterCategory.ALL -> allItems.size
                                MarkerFilterCategory.ENTITIES -> allItems.count { it.isEntityLinked }
                                MarkerFilterCategory.PINS -> allItems.count { !it.isEntityLinked }
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = {
                                    Text(
                                        text = "${cat.label} ($count)",
                                        fontSize = 10.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GrimoireColors.GoldPrimary,
                                    selectedLabelColor = GrimoireColors.Background,
                                    containerColor = GrimoireColors.SurfaceLow,
                                    labelColor = GrimoireColors.ParchmentLight
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) GrimoireColors.GoldPrimary else GrimoireColors.BorderBronze
                                ),
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }

                    // Sort Dropdown Button
                    Box {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .border(BorderStroke(1.dp, GrimoireColors.BorderBronzeSubtle), RoundedCornerShape(4.dp))
                                .clickable { isSortMenuOpen = true }
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            color = GrimoireColors.SurfaceLow
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort order",
                                    tint = GrimoireColors.GoldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = when (sortOrder) {
                                        MarkerSortOrder.VISIBLE_FIRST -> "Visible"
                                        MarkerSortOrder.NAME_ASC -> "A–Z"
                                        MarkerSortOrder.NAME_DESC -> "Z–A"
                                    },
                                    color = GrimoireColors.ParchmentLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isSortMenuOpen,
                            onDismissRequest = { isSortMenuOpen = false },
                            modifier = Modifier.background(GrimoireColors.SurfaceCard)
                        ) {
                            MarkerSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            order.label,
                                            color = if (sortOrder == order) GrimoireColors.GoldPrimary else GrimoireColors.ParchmentLight,
                                            fontSize = 12.sp,
                                            fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        sortOrder = order
                                        isSortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            GrimoireDivider(thickness = 1.dp)

            // Markers Accordion List
            if (groupedByCategory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (markers.isEmpty()) "No markers placed on this map yet." else "No markers match your search.",
                        color = GrimoireColors.ParchmentMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    groupedByCategory.forEach { (categoryName, categoryItems) ->
                        val isExpanded = expandedCategories.contains(categoryName)
                        val catVisibleCount = categoryItems.count { !it.marker.isHidden }
                        val catTotalCount = categoryItems.size
                        val allCategoryHidden = catVisibleCount == 0
                        val allCategoryVisible = catVisibleCount == catTotalCount

                        // Category Accordion Header
                        item(key = "header_$categoryName") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(BorderStroke(1.dp, GrimoireColors.BorderBronzeSubtle), RoundedCornerShape(6.dp))
                                    .clickable {
                                        expandedCategories = if (isExpanded) {
                                            expandedCategories - categoryName
                                        } else {
                                            expandedCategories + categoryName
                                        }
                                    },
                                color = GrimoireColors.SurfaceLow,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = GrimoireColors.GoldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = categoryName,
                                        color = GrimoireColors.GoldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        color = GrimoireColors.BronzeContainer.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "$catVisibleCount/$catTotalCount",
                                            color = if (allCategoryHidden) GrimoireColors.ParchmentDim else GrimoireColors.GoldBright,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }

                                    Spacer(Modifier.weight(1f))

                                    // Category Eye Toggle (Show/Hide all in category)
                                    IconButton(
                                        onClick = {
                                            val targetHiddenState = !allCategoryHidden
                                            val updated = categoryItems.map { it.marker.copy(isHidden = targetHiddenState) }
                                            onUpdateMarkers(updated)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (allCategoryHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (allCategoryHidden) "Show all in $categoryName" else "Hide all in $categoryName",
                                            tint = when {
                                                allCategoryHidden -> GrimoireColors.ParchmentDim
                                                allCategoryVisible -> GrimoireColors.GoldPrimary
                                                else -> GrimoireColors.GoldBright.copy(alpha = 0.8f)
                                            },
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Category Items (when expanded)
                        if (isExpanded) {
                            items(categoryItems, key = { it.marker.id }) { item ->
                                val isHighlighted = highlightedMarkerId == item.marker.id
                                val isHidden = item.marker.isHidden

                                val itemBgColor = when {
                                    isHighlighted -> GrimoireColors.GoldContainer.copy(alpha = 0.25f)
                                    isHidden -> GrimoireColors.SurfaceLow.copy(alpha = 0.45f)
                                    else -> GrimoireColors.SurfaceLow
                                }
                                val itemBorderColor = when {
                                    isHighlighted -> GrimoireColors.GoldPrimary
                                    isHidden -> GrimoireColors.BorderBronzeSubtle.copy(alpha = 0.4f)
                                    else -> GrimoireColors.BorderBronzeSubtle
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(itemBgColor, RoundedCornerShape(6.dp))
                                        .border(BorderStroke(1.dp, itemBorderColor), RoundedCornerShape(6.dp))
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { onSelectMarker(item.marker) }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left Avatar / Icon
                                    val iconAlpha = if (isHidden) 0.4f else 1.0f
                                    Box(
                                        modifier = Modifier.size(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (item.tokenBitmap != null) {
                                            Image(
                                                bitmap = item.tokenBitmap,
                                                contentDescription = item.title,
                                                alpha = iconAlpha,
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .border(1.5.dp, item.color.copy(alpha = iconAlpha), CircleShape)
                                            )
                                        } else if (item.iconBitmap != null) {
                                            Image(
                                                bitmap = item.iconBitmap,
                                                contentDescription = item.title,
                                                alpha = iconAlpha,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(item.color.copy(alpha = iconAlpha))
                                                    .border(1.dp, Color.Black.copy(alpha = 0.5f * iconAlpha), CircleShape)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // Middle Info Block
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = item.title,
                                                color = when {
                                                    isHighlighted -> GrimoireColors.GoldBright
                                                    isHidden -> GrimoireColors.ParchmentDim
                                                    else -> GrimoireColors.ParchmentLight
                                                },
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )

                                            if (isHidden) {
                                                Surface(
                                                    color = GrimoireColors.SurfaceCard,
                                                    shape = RoundedCornerShape(3.dp),
                                                    border = BorderStroke(0.5.dp, GrimoireColors.BorderBronzeSubtle)
                                                ) {
                                                    Text(
                                                        text = "Hidden",
                                                        color = GrimoireColors.ParchmentDim,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (item.entityTypeName != null) {
                                                Surface(
                                                    color = item.color.copy(alpha = if (isHidden) 0.1f else 0.2f),
                                                    shape = RoundedCornerShape(3.dp),
                                                    border = BorderStroke(0.5.dp, item.color.copy(alpha = if (isHidden) 0.3f else 0.6f))
                                                ) {
                                                    Text(
                                                        text = item.entityTypeName,
                                                        color = item.color.copy(alpha = if (isHidden) 0.5f else 1f),
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                    )
                                                }
                                            } else if (!item.subtitle.isNullOrBlank()) {
                                                Text(
                                                    text = item.subtitle,
                                                    color = GrimoireColors.ParchmentDim,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // Coords hint
                                            Text(
                                                text = "${(item.marker.x * 100).toInt()}% · ${(item.marker.y * 100).toInt()}%",
                                                color = GrimoireColors.ParchmentDim.copy(alpha = 0.7f),
                                                fontSize = 9.sp
                                            )
                                        }
                                    }

                                    // Right Action Icons
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        // Visibility Toggle Eye
                                        IconButton(
                                            onClick = {
                                                onUpdateMarker(item.marker.copy(isHidden = !item.marker.isHidden))
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (isHidden) "Show on Map" else "Hide on Map",
                                                tint = if (isHidden) GrimoireColors.ParchmentDim else GrimoireColors.GoldPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        if (item.isEntityLinked && item.linkedEntity != null && onOpenEntityPopout != null) {
                                            IconButton(
                                                onClick = { onOpenEntityPopout(item.linkedEntity.name) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.OpenInFull,
                                                    contentDescription = "Open Popout",
                                                    tint = GrimoireColors.GoldPrimary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { onEditMarker(item.marker) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Marker",
                                                tint = GrimoireColors.ParchmentMuted,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
