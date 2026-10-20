package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.luca.dungeon_master_manager.colorElement
import de.luca.dungeon_master_manager.ui.theme.GrimoireCard
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import de.luca.dungeon_master_manager.ui.theme.GrimoireDivider
import de.luca.dungeonmastermanager.database.Entity
import de.luca.dungeon_master_manager.viewmodel.EntityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityManagerDialog(
    viewModel: EntityViewModel,
    onDismiss: () -> Unit,
    isGlobalOnly: Boolean = false
) {
    val projectEntities by viewModel.projectEntities.collectAsState()
    val globalEntities by viewModel.globalEntities.collectAsState()

    var selectedTab by remember { mutableStateOf(if (isGlobalOnly) 1 else 0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var entityToEdit by remember { mutableStateOf<Entity?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.85f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(10.dp),
            color = GrimoireColors.Surface,
            border = BorderStroke(1.dp, GrimoireColors.BorderBronze)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Entity Bestiary & Dramatis Personae",
                        style = MaterialTheme.typography.titleLarge,
                        color = GrimoireColors.GoldPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GrimoireColors.ParchmentMuted
                        )
                    }
                }

                if (!isGlobalOnly) {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = GrimoireColors.SurfaceLow,
                        contentColor = GrimoireColors.GoldPrimary,
                        indicator = {
                            TabRowDefaults.PrimaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(selectedTab),
                                color = GrimoireColors.GoldPrimary
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    "Project Entities",
                                    color = if (selectedTab == 0) GrimoireColors.GoldPrimary else GrimoireColors.ParchmentMuted,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    "Global Entities",
                                    color = if (selectedTab == 1) GrimoireColors.GoldPrimary else GrimoireColors.ParchmentMuted,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                GrimoireDivider()

                // Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val entitiesToShow = if (selectedTab == 0) projectEntities else globalEntities
                    val isShowingGlobal = selectedTab == 1

                    if (entitiesToShow.isEmpty()) {
                        Text(
                            "No entities recorded yet.",
                            modifier = Modifier.align(Alignment.Center),
                            color = GrimoireColors.ParchmentMuted
                        )
                    } else {
                        val grouped = entitiesToShow.groupBy { it.type }
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
                            grouped.forEach { (type, entities) ->
                                item {
                                    Text(
                                        text = type.uppercase(),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = GrimoireColors.GoldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                                    )
                                }
                                items(entities) { entity ->
                                    EntityRow(
                                        entity = entity,
                                        isGlobal = isShowingGlobal,
                                        canCopy = !isGlobalOnly,
                                        onDelete = { viewModel.deleteEntity(isShowingGlobal, entity.id) },
                                        onCopy = { viewModel.copyEntity(entity, !isShowingGlobal) },
                                        onEdit = { entityToEdit = it }
                                    )
                                }
                            }
                        }
                    }
                }

                GrimoireDivider()

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { 
                            entityToEdit = null
                            showCreateDialog = true 
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GrimoireColors.GoldPrimary,
                            contentColor = GrimoireColors.Background
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("+ Create Entity", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCreateDialog || entityToEdit != null) {
        CreateEntityDialog(
            isGlobal = selectedTab == 1,
            viewModel = viewModel,
            initialEntity = entityToEdit,
            onDismiss = { 
                showCreateDialog = false
                entityToEdit = null 
            },
            onCreate = { name, type, color, notesPath ->
                if (entityToEdit != null) {
                    viewModel.updateEntity(selectedTab == 1, entityToEdit!!.id, name, type, color, notesPath)
                } else {
                    viewModel.addEntity(selectedTab == 1, name, type, color, notesPath)
                }
                showCreateDialog = false
                entityToEdit = null
            }
        )
    }
}

@Composable
private fun EntityRow(
    entity: Entity,
    isGlobal: Boolean,
    canCopy: Boolean,
    onDelete: (Entity) -> Unit,
    onCopy: (Entity) -> Unit,
    onEdit: (Entity) -> Unit
) {
    GrimoireCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(6.dp),
        borderHoverColor = GrimoireColors.GoldPrimary
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val colorVal = try { Color(entity.color?.toULong() ?: 0xFFFFFFFFuL) } catch(e: Exception) { GrimoireColors.GoldPrimary }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(colorVal, shape = RoundedCornerShape(3.dp))
                    .border(1.dp, GrimoireColors.BorderBronze, RoundedCornerShape(3.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entity.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = GrimoireColors.ParchmentLight
                )
                if (!entity.notesFilePath.isNullOrBlank()) {
                    Text(
                        text = "Notes: ${entity.notesFilePath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrimoireColors.ParchmentMuted
                    )
                }
            }
            TextButton(onClick = { onEdit(entity) }) {
                Text("Edit", color = GrimoireColors.GoldPrimary)
            }
            if (canCopy) {
                TextButton(onClick = { onCopy(entity) }) {
                    Text(if (isGlobal) "Copy to Campaign" else "Copy to Global", color = GrimoireColors.ParchmentLight)
                }
            }
            TextButton(
                onClick = { onDelete(entity) },
                colors = ButtonDefaults.textButtonColors(contentColor = GrimoireColors.CrimsonError)
            ) {
                Text("Delete")
            }
        }
    }
}

@Composable
fun CreateEntityDialog(
    isGlobal: Boolean,
    viewModel: EntityViewModel,
    initialEntity: Entity? = null,
    onDismiss: () -> Unit,
    onCreate: (name: String, type: String, color: String, notesPath: String) -> Unit
) {
    var name by remember { mutableStateOf(initialEntity?.name ?: "") }
    var type by remember { mutableStateOf(initialEntity?.type ?: "NPC") }
    var color by remember { mutableStateOf(initialEntity?.color ?: Color.Red.value.toString()) }
    var notesPath by remember { mutableStateOf(initialEntity?.notesFilePath ?: "") }
    var showFileSelector by remember { mutableStateOf(false) }
    val showColorPicker = remember { mutableStateOf(false) }

    val types = listOf("NPC", "PC", "Place", "Item", "Monster")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "New ${if (isGlobal) "Global " else ""}Entity",
                style = MaterialTheme.typography.titleLarge,
                color = GrimoireColors.GoldPrimary
            ) 
        },
        containerColor = GrimoireColors.SurfaceCard,
        shape = RoundedCornerShape(8.dp),
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = GrimoireColors.ParchmentMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GrimoireColors.GoldPrimary,
                        unfocusedBorderColor = GrimoireColors.BorderBronze,
                        focusedTextColor = GrimoireColors.ParchmentLight,
                        unfocusedTextColor = GrimoireColors.ParchmentLight,
                    )
                )
                Spacer(Modifier.height(10.dp))
                Text("Type:", style = MaterialTheme.typography.labelMedium, color = GrimoireColors.ParchmentMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    types.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GrimoireColors.GoldPrimary,
                                selectedLabelColor = GrimoireColors.Background,
                                containerColor = GrimoireColors.SurfaceLow,
                                labelColor = GrimoireColors.ParchmentLight
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = type == t,
                                borderColor = if (type == t) GrimoireColors.GoldPrimary else GrimoireColors.BorderBronze
                            )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Color:", style = MaterialTheme.typography.labelMedium, color = GrimoireColors.ParchmentMuted)
                colorElement(
                    Color(color.toULong()),
                    showColorPicker,
                    onClick = { inputColor ->
                        color = inputColor.value.toString()
                    }
                )
                Spacer(Modifier.height(16.dp))
                
                Text("Notes File:", style = MaterialTheme.typography.labelMedium, color = GrimoireColors.ParchmentMuted)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = notesPath,
                        onValueChange = { notesPath = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("No file selected", color = GrimoireColors.ParchmentDim) },
                        readOnly = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GrimoireColors.GoldPrimary,
                            unfocusedBorderColor = GrimoireColors.BorderBronze,
                            focusedTextColor = GrimoireColors.ParchmentLight,
                            unfocusedTextColor = GrimoireColors.ParchmentLight,
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { showFileSelector = true },
                        border = BorderStroke(1.dp, GrimoireColors.BorderBronze),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GrimoireColors.GoldPrimary)
                    ) {
                        Text("Browse")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, type, color, notesPath)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GrimoireColors.GoldPrimary,
                    contentColor = GrimoireColors.Background
                )
            ) { Text(if (initialEntity != null) "Save" else "Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Cancel", color = GrimoireColors.ParchmentMuted) 
            }
        }
    )

    if (showFileSelector) {
        val availableFiles = remember(isGlobal, type) { viewModel.getAvailableMdFiles(isGlobal, type) }
        
        AlertDialog(
            onDismissRequest = { showFileSelector = false },
            title = { 
                Text(
                    "Select Notes File",
                    style = MaterialTheme.typography.titleLarge,
                    color = GrimoireColors.GoldPrimary
                ) 
            },
            containerColor = GrimoireColors.SurfaceCard,
            shape = RoundedCornerShape(8.dp),
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    Button(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        onClick = {
                            val newPath = viewModel.createTemplateFile(isGlobal, name.ifBlank { "Unnamed" }, type)
                            notesPath = newPath
                            showFileSelector = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GrimoireColors.GoldContainer,
                            contentColor = GrimoireColors.GoldBright
                        ),
                        border = BorderStroke(1.dp, GrimoireColors.BorderGold)
                    ) {
                        Text("+ Create New Template File in ${type}s/")
                    }
                    
                    GrimoireDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    if (availableFiles.isEmpty()) {
                        Text("No markdown files found.", color = GrimoireColors.ParchmentMuted)
                    } else {
                        val typePrefix = "${type}s${java.io.File.separator}"
                        LazyColumn {
                            items(availableFiles) { file ->
                                val isRelevant = file.startsWith(typePrefix)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            notesPath = file
                                            showFileSelector = false
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = file,
                                        color = if (isRelevant) GrimoireColors.GoldPrimary else GrimoireColors.ParchmentLight,
                                        fontWeight = if (isRelevant) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFileSelector = false }) { 
                    Text("Close", color = GrimoireColors.ParchmentMuted) 
                }
            }
        )
    }
}
