package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.luca.dungeon_master_manager.data.EntityPopoutRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

import androidx.compose.ui.text.withStyle

@Composable
fun EntityPopoutScreen(
    request: EntityPopoutRequest,
    onClose: () -> Unit,
    onOpenEntityPopout: (EntityPopoutRequest) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val viewModel = remember(request.projectPath) { de.luca.dungeon_master_manager.viewmodel.EntityViewModel(
        request.projectPath,
        null
    ) }
    var showEditDialog by remember { mutableStateOf(false) }

    val projectEnts by viewModel.projectEntities.collectAsState()
    val globalEnts by viewModel.globalEntities.collectAsState()

    var targetEntityId by remember { mutableStateOf<String?>(null) }
    var isGlobalEntity by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }

    val entity = remember(projectEnts, globalEnts, targetEntityId, initialized, request.entityName) {
        if (!initialized) {
            if (projectEnts.isEmpty() && globalEnts.isEmpty()) return@remember null // Still loading
            
            var found = projectEnts.find { it.name == request.entityName }
            var isGlobal = false
            if (found == null) {
                found = globalEnts.find { it.name == request.entityName }
                if (found != null) isGlobal = true
            }
            
            if (found != null) {
                targetEntityId = found.id
                isGlobalEntity = isGlobal
            }
            initialized = true
            found
        } else {
            val list = if (isGlobalEntity) globalEnts else projectEnts
            list.find { it.id == targetEntityId }
        }
    }

    var fileContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(entity?.notesFilePath, initialized) {
        if (!initialized) return@LaunchedEffect
        
        if (entity != null && !entity.notesFilePath.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                val rootPath = if (isGlobalEntity) {
                    File(System.getProperty("user.home"), ".dungeon-master-manager/.global/Notes").absolutePath
                } else {
                    request.projectPath
                }
                
                val file = File(rootPath, entity.notesFilePath!!)
                if (file.exists()) {
                    fileContent = file.readText()
                } else {
                    fileContent = null
                }
            }
        }
        isLoading = false
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (entity == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Entity '${request.entityName}' not found in project database.")
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Toolbar
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp), 
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val textColor = MaterialTheme.colorScheme.onSurface
                        val entityColor = remember(entity!!.color) {
                            if (entity!!.color != null) {
                                try { androidx.compose.ui.graphics.Color(entity!!.color!!.toULong()) } catch(e: Exception) { textColor }
                            } else {
                                textColor
                            }
                        }
                        
                        Text(
                            text = androidx.compose.ui.text.buildAnnotatedString {
                                append("Entity: ")
                                withStyle(androidx.compose.ui.text.SpanStyle(color = entityColor)) {
                                    append(entity!!.name)
                                }
                                append(" (${entity!!.type})")
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                        Button(onClick = { showEditDialog = true }) {
                            Text("Edit")
                        }
                    }
                }
                
                HorizontalDivider()
                
                if (fileContent != null) {
                    // Editor
                    val projectEnts by viewModel.projectEntities.collectAsState()
                    val globalEnts by viewModel.globalEntities.collectAsState()
                    val entitiesList = remember(projectEnts, globalEnts) {
                        val list = mutableListOf<de.luca.dungeonmastermanager.database.Entity>()
                        list.addAll(globalEnts)
                        list.addAll(projectEnts)
                        list
                    }
                    
                    Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                        MarkdownEditor(
                            content = fileContent!!,
                            onContentChange = { newContent ->
                                fileContent = newContent
                                // Save file
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        val file = File(request.projectPath, entity!!.notesFilePath!!)
                                        file.writeText(newContent)
                                    }
                                }
                            },
                            onOpenEntityPopout = { nestedEntityName ->
                                onOpenEntityPopout(EntityPopoutRequest(nestedEntityName, request.projectPath))
                            },
                            entities = entitiesList
                        )
                    }
                } else {
                    Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No notes file linked to this entity, or file does not exist.")
                    }
                }
            }
        }
    }

    if (showEditDialog && entity != null) {
        CreateEntityDialog(
            isGlobal = isGlobalEntity,
            viewModel = viewModel,
            initialEntity = entity,
            onDismiss = { showEditDialog = false },
            onCreate = { name, type, color, notesPath ->
                viewModel.updateEntity(isGlobalEntity, entity!!.id, name, type, color, notesPath)
                showEditDialog = false
            }
        )
    }
}
