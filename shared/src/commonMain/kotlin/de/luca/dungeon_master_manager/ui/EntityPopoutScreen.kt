package de.luca.dungeon_master_manager.ui

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
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import de.luca.dungeon_master_manager.ui.theme.GrimoireDivider
import de.luca.dungeon_master_manager.ui.theme.GrimoireTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun EntityPopoutScreen(
    request: EntityPopoutRequest,
    onClose: () -> Unit,
    onOpenEntityPopout: (EntityPopoutRequest) -> Unit
) {
    GrimoireTheme {
        val coroutineScope = rememberCoroutineScope()
        val viewModel = remember(request.projectPath) {
            de.luca.dungeon_master_manager.viewmodel.EntityViewModel(
                request.projectPath,
                null
            )
        }
        var showEditDialog by remember { mutableStateOf(false) }

        val projectEnts by viewModel.projectEntities.collectAsState()
        val globalEnts by viewModel.globalEntities.collectAsState()

        var targetEntityId by remember { mutableStateOf<String?>(null) }
        var isGlobalEntity by remember { mutableStateOf(false) }
        var initialized by remember { mutableStateOf(false) }

        val entity = remember(projectEnts, globalEnts, targetEntityId, initialized, request.entityName) {
            val pMatch = projectEnts.find { 
                if (targetEntityId != null) it.id.toString() == targetEntityId else it.name == request.entityName 
            }
            if (pMatch != null) {
                isGlobalEntity = false
                pMatch
            } else {
                val gMatch = globalEnts.find { 
                    if (targetEntityId != null) it.id.toString() == targetEntityId else it.name == request.entityName 
                }
                if (gMatch != null) {
                    isGlobalEntity = true
                    gMatch
                } else null
            }
        }

        var fileContent by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(entity?.notesFilePath) {
            isLoading = true
            if (entity?.notesFilePath != null) {
                withContext(Dispatchers.IO) {
                    val file = File(request.projectPath, entity.notesFilePath)
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GrimoireColors.GoldPrimary)
                }
            } else if (entity == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Entity '${request.entityName}' not found in project database.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
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
                                                val file = File(request.projectPath, notesPath)
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
                            Text(
                                "No notes file linked to this entity, or file does not exist.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    viewModel.updateEntity(isGlobalEntity, entity.id, name, type, color, notesPath)
                    showEditDialog = false
                }
            )
        }
    }
}
