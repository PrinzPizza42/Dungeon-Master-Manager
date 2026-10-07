package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import de.luca.dungeonmastermanager.database.Entity
import de.luca.dungeon_master_manager.viewmodel.EntityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityManagerDialog(
    viewModel: EntityViewModel,
    onDismiss: () -> Unit,
    isGlobalOnly: Boolean = false // If true, only shows Global entities (for Launcher screen)
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
            modifier = Modifier.fillMaxWidth(0.8f).fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Entity Manager", style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                if (!isGlobalOnly) {
                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                            Text("Project Entities", modifier = Modifier.padding(16.dp))
                        }
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                            Text("Global Entities", modifier = Modifier.padding(16.dp))
                        }
                    }
                }

                // Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val entitiesToShow = if (selectedTab == 0) projectEntities else globalEntities
                    val isShowingGlobal = selectedTab == 1

                    if (entitiesToShow.isEmpty()) {
                        Text(
                            "No entities found.",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Group by type
                        val grouped = entitiesToShow.groupBy { it.type }
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            grouped.forEach { (type, entities) ->
                                item {
                                    Text(
                                        type,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 8.dp)
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

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = { 
                        entityToEdit = null
                        showCreateDialog = true 
                    }) {
                        Text("Create Entity")
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
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val colorVal = try { Color(entity.color?.toULong() ?: 0xFFFFFFFFuL) } catch(e: Exception) { Color.White }
            Box(modifier = Modifier.size(16.dp).background(colorVal, shape = MaterialTheme.shapes.small))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entity.name, fontWeight = FontWeight.Bold)
                if (!entity.notesFilePath.isNullOrBlank()) {
                    Text("Notes: ${entity.notesFilePath}", style = MaterialTheme.typography.bodySmall)
                }
            }
            TextButton(onClick = { onEdit(entity) }) {
                Text("Edit")
            }
            if (canCopy) {
                TextButton(onClick = { onCopy(entity) }) {
                    Text(if (isGlobal) "Copy to Project" else "Copy to Global")
                }
            }
            TextButton(onClick = { onDelete(entity) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
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
        title = { Text("New ${if(isGlobal) "Global " else ""}Entity") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("Type:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    types.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Color:", style = MaterialTheme.typography.labelMedium)
                colorElement(
                    Color(color.toULong()),
                    showColorPicker,
                    onClick = { inputColor ->
                        color = inputColor.value.toString()
                    }
                )
                Spacer(Modifier.height(16.dp))
                
                Text("Notes File:", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = notesPath,
                        onValueChange = { notesPath = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("No file selected") },
                        readOnly = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { showFileSelector = true }) {
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
                }
            ) { Text(if (initialEntity != null) "Save" else "Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showFileSelector) {
        val availableFiles = remember(isGlobal, type) { viewModel.getAvailableMdFiles(isGlobal, type) }
        
        AlertDialog(
            onDismissRequest = { showFileSelector = false },
            title = { Text("Select Notes File") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    Button(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        onClick = {
                            val newPath = viewModel.createTemplateFile(isGlobal, name.ifBlank { "Unnamed" }, type)
                            notesPath = newPath
                            showFileSelector = false
                        }
                    ) {
                        Text("+ Create New Template File in ${type}s/")
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    if (availableFiles.isEmpty()) {
                        Text("No markdown files found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        // Display files with priority coloring for those in the matching type folder
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
                                        color = if (isRelevant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isRelevant) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFileSelector = false }) { Text("Close") }
            }
        )
    }
}
