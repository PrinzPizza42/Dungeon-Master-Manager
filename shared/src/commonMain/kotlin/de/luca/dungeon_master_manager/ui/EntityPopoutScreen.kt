package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import de.luca.dungeon_master_manager.data.EntityPopoutRequest
import de.luca.dungeon_master_manager.data.ProjectRepository
import de.luca.dungeon_master_manager.ui.components.GrimoireTitleBar
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import de.luca.dungeon_master_manager.ui.theme.GrimoireDivider
import de.luca.dungeon_master_manager.ui.theme.GrimoireTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.awt.Window

@Composable
fun EntityPopoutScreen(
    request: EntityPopoutRequest,
    onClose: () -> Unit,
    onOpenEntityPopout: (EntityPopoutRequest) -> Unit,
    window: Window? = null,
    windowState: androidx.compose.ui.window.WindowState? = null,
    title: String = request.entityName
) {
    GrimoireTheme {
        val coroutineScope = rememberCoroutineScope()
        val viewModel = remember(request.projectPath) {
            de.luca.dungeon_master_manager.viewmodel.EntityViewModel(
                request.projectPath.ifBlank { null },
                null
            )
        }
        var showEditDialog by remember { mutableStateOf(false) }
        var showLinkFileDialog by remember { mutableStateOf(false) }

        val projectEnts by viewModel.projectEntities.collectAsState()
        val globalEnts by viewModel.globalEntities.collectAsState()

        var targetEntityId by remember { mutableStateOf<String?>(null) }
        var isGlobalEntity by remember { mutableStateOf(false) }
        var initialized by remember { mutableStateOf(false) }

        val entity = remember(projectEnts, globalEnts, targetEntityId, initialized, request.entityName) {
            val pMatch = if (request.projectPath.isNotBlank()) {
                projectEnts.find { 
                    if (targetEntityId != null) it.id == targetEntityId else it.name == request.entityName 
                }
            } else null

            if (pMatch != null) {
                isGlobalEntity = false
                pMatch
            } else {
                val gMatch = globalEnts.find { 
                    if (targetEntityId != null) it.id == targetEntityId else it.name == request.entityName 
                }
                if (gMatch != null) {
                    isGlobalEntity = true
                    gMatch
                } else null
            }
        }

        fun getNotesBaseDir(isGlobal: Boolean): File {
            return if (isGlobal) {
                ProjectRepository.globalNotesDir
            } else {
                File(request.projectPath)
            }
        }

        var fileContent by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(entity?.notesFilePath, isGlobalEntity) {
            isLoading = true
            if (entity?.notesFilePath != null) {
                withContext(Dispatchers.IO) {
                    val baseDir = getNotesBaseDir(isGlobalEntity)
                    val file = File(baseDir, entity.notesFilePath)
                    if (file.exists() && file.isFile) {
                        fileContent = file.readText()
                    } else {
                        fileContent = null
                    }
                }
            } else {
                fileContent = null
            }
            isLoading = false
            initialized = true
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (window != null && windowState != null) {
                    GrimoireTitleBar(
                        window = window,
                        title = title,
                        windowState = windowState,
                        onCloseRequest = onClose
                    )
                }

                if (isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GrimoireColors.GoldPrimary)
                    }
                } else if (entity == null) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Entity '${request.entityName}' not found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Toolbar
                    Surface(color = GrimoireColors.Surface) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val textColor = MaterialTheme.colorScheme.onSurface
                            val entityColor = remember(entity.color) {
                                if (entity.color != null) {
                                    try {
                                        Color(entity.color.toULong())
                                    } catch (e: Exception) {
                                        textColor
                                    }
                                } else {
                                    textColor
                                }
                            }

                            Text(
                                text = buildAnnotatedString {
                                    append("Entity: ")
                                    withStyle(SpanStyle(color = entityColor)) {
                                        append(entity.name)
                                    }
                                    withStyle(SpanStyle(color = GrimoireColors.ParchmentMuted)) {
                                        append(" (${entity.type})")
                                    }
                                },
                                style = MaterialTheme.typography.titleLarge
                            )
                            Button(
                                onClick = { showEditDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GrimoireColors.GoldPrimary,
                                    contentColor = GrimoireColors.Background
                                )
                            ) {
                                Text("Edit")
                            }
                        }
                    }

                    GrimoireDivider()

                    if (fileContent != null) {
                        // Editor
                        val entitiesList = remember(projectEnts, globalEnts) {
                            val list = mutableListOf<de.luca.dungeonmastermanager.database.Entity>()
                            list.addAll(globalEnts)
                            list.addAll(projectEnts)
                            list
                        }

                        Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                            MarkdownEditor(
                                content = fileContent ?: "",
                                onContentChange = { newContent ->
                                    fileContent = newContent
                                    // Save file
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            val notesPath = entity.notesFilePath
                                            if (notesPath != null) {
                                                val baseDir = getNotesBaseDir(isGlobalEntity)
                                                val file = File(baseDir, notesPath)
                                                file.writeText(newContent)
                                            }
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    "No notes file linked to this entity, or file does not exist.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val newPath = viewModel.createTemplateFile(
                                                    isGlobal = isGlobalEntity,
                                                    entityName = entity.name,
                                                    entityType = entity.type
                                                )
                                                viewModel.updateEntity(
                                                    isGlobal = isGlobalEntity,
                                                    id = entity.id,
                                                    name = entity.name,
                                                    type = entity.type,
                                                    color = entity.color,
                                                    notesFilePath = newPath
                                                )
                                                val baseDir = getNotesBaseDir(isGlobalEntity)
                                                val file = File(baseDir, newPath)
                                                if (file.exists() && file.isFile) {
                                                    fileContent = file.readText()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GrimoireColors.GoldPrimary,
                                            contentColor = GrimoireColors.Background
                                        )
                                    ) {
                                        Text("+ Create Notes File")
                                    }

                                    OutlinedButton(
                                        onClick = { showLinkFileDialog = true },
                                        border = BorderStroke(1.dp, GrimoireColors.BorderBronze),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GrimoireColors.GoldPrimary)
                                    ) {
                                        Text("Link Existing File")
                                    }
                                }
                            }
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
                    targetEntityId = entity.id
                    viewModel.updateEntity(isGlobalEntity, entity.id, name, type, color, notesPath)
                    showEditDialog = false
                }
            )
        }

        if (showLinkFileDialog && entity != null) {
            NotesFilePickerDialog(
                isGlobal = isGlobalEntity,
                entityName = entity.name,
                entityType = entity.type,
                viewModel = viewModel,
                onDismiss = { showLinkFileDialog = false },
                onFileSelected = { selectedPath ->
                    targetEntityId = entity.id
                    viewModel.updateEntity(
                        isGlobal = isGlobalEntity,
                        id = entity.id,
                        name = entity.name,
                        type = entity.type,
                        color = entity.color,
                        notesFilePath = selectedPath
                    )
                    showLinkFileDialog = false
                }
            )
        }
    }
}
