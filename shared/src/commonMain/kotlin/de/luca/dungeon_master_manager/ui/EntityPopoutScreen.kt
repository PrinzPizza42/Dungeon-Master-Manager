package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.luca.dungeon_master_manager.data.EntityPopoutRequest
import de.luca.dungeon_master_manager.data.ProjectRepository
import de.luca.dungeon_master_manager.ui.components.GrimoireTitleBar
import de.luca.dungeon_master_manager.ui.components.rememberEntityImage
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import de.luca.dungeon_master_manager.ui.theme.GrimoireDivider
import de.luca.dungeon_master_manager.ui.theme.GrimoireTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.awt.Window

@OptIn(ExperimentalMaterial3Api::class)
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
        var selectedTabIndex by remember { mutableStateOf(0) }

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

                            val headerImageFile = remember(entity.imageFilePath, isGlobalEntity) {
                                viewModel.resolveEntityImageFile(entity.imageFilePath, isGlobalEntity)
                            }
                            val headerImageBitmap = rememberEntityImage(headerImageFile, maxDimension = 128)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                // Permanent Portrait Avatar Token
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(GrimoireColors.SurfaceLow)
                                        .border(2.dp, entityColor, CircleShape)
                                        .clickable { selectedTabIndex = 1 },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (headerImageBitmap != null) {
                                        Image(
                                            bitmap = headerImageBitmap,
                                            contentDescription = entity.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            alignment = Alignment.TopCenter
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(entityColor.copy(alpha = 0.18f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = entity.name.trim().take(1).uppercase().ifBlank { "?" },
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = entityColor
                                            )
                                        }
                                    }
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = entity.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = entityColor
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(entityColor.copy(alpha = 0.18f), RoundedCornerShape(4.dp))
                                                .border(1.dp, entityColor.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = entity.type.uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp,
                                                color = entityColor
                                            )
                                        }
                                    }
                                    if (isGlobalEntity) {
                                        Text(
                                            text = "Globales Entity",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GrimoireColors.ParchmentMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

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

                    PrimaryTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = GrimoireColors.SurfaceLow,
                        contentColor = GrimoireColors.GoldPrimary,
                        indicator = {
                            TabRowDefaults.PrimaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                                color = GrimoireColors.GoldPrimary
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = {
                                Text(
                                    "Notizen",
                                    color = if (selectedTabIndex == 0) GrimoireColors.GoldPrimary else GrimoireColors.ParchmentMuted,
                                    fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = {
                                Text(
                                    "Portrait / Bild",
                                    color = if (selectedTabIndex == 1) GrimoireColors.GoldPrimary else GrimoireColors.ParchmentMuted,
                                    fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }

                    GrimoireDivider()

                    if (selectedTabIndex == 0) {
                        // --- NOTES TAB ---
                        if (fileContent != null) {
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
                                                    targetEntityId = entity.id
                                                    viewModel.updateEntity(
                                                        isGlobal = isGlobalEntity,
                                                        id = entity.id,
                                                        name = entity.name,
                                                        type = entity.type,
                                                        color = entity.color,
                                                        notesFilePath = newPath,
                                                        imageFilePath = entity.imageFilePath
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
                    } else {
                        // --- PORTRAIT / IMAGE TAB ---
                        val imageFile = remember(entity.imageFilePath, isGlobalEntity) {
                            viewModel.resolveEntityImageFile(entity.imageFilePath, isGlobalEntity)
                        }
                        val imageBitmap = rememberEntityImage(imageFile, maxDimension = 1600)

                        Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (imageBitmap != null) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val entityColor = remember(entity.color) {
                                        if (entity.color != null) {
                                            try { Color(entity.color.toULong()) } catch (_: Exception) { GrimoireColors.GoldPrimary }
                                        } else GrimoireColors.GoldPrimary
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .fillMaxWidth(0.9f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(2.dp, entityColor, RoundedCornerShape(8.dp))
                                            .background(GrimoireColors.SurfaceCard),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            bitmap = imageBitmap,
                                            contentDescription = entity.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                val file = pickExternalImageFile()
                                                if (file != null && file.exists()) {
                                                    val importedPath = viewModel.importEntityImage(file, isGlobalEntity, entity.name)
                                                    targetEntityId = entity.id
                                                    viewModel.updateEntity(
                                                        isGlobal = isGlobalEntity,
                                                        id = entity.id,
                                                        name = entity.name,
                                                        type = entity.type,
                                                        color = entity.color,
                                                        notesFilePath = entity.notesFilePath,
                                                        imageFilePath = importedPath
                                                    )
                                                }
                                            },
                                            border = BorderStroke(1.dp, GrimoireColors.BorderBronze),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GrimoireColors.GoldPrimary)
                                        ) {
                                            Text("Change Image...")
                                        }

                                        TextButton(
                                            onClick = {
                                                targetEntityId = entity.id
                                                viewModel.updateEntity(
                                                    isGlobal = isGlobalEntity,
                                                    id = entity.id,
                                                    name = entity.name,
                                                    type = entity.type,
                                                    color = entity.color,
                                                    notesFilePath = entity.notesFilePath,
                                                    imageFilePath = null
                                                )
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = GrimoireColors.CrimsonError)
                                        ) {
                                            Text("Remove Image")
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(
                                        "No portrait or image set for this entity.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Button(
                                        onClick = {
                                            val file = pickExternalImageFile()
                                            if (file != null && file.exists()) {
                                                val importedPath = viewModel.importEntityImage(file, isGlobalEntity, entity.name)
                                                targetEntityId = entity.id
                                                viewModel.updateEntity(
                                                    isGlobal = isGlobalEntity,
                                                    id = entity.id,
                                                    name = entity.name,
                                                    type = entity.type,
                                                    color = entity.color,
                                                    notesFilePath = entity.notesFilePath,
                                                    imageFilePath = importedPath
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GrimoireColors.GoldPrimary,
                                            contentColor = GrimoireColors.Background
                                        )
                                    ) {
                                        Text("+ Select Image...")
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
                onCreate = { name, type, color, notesPath, imagePath ->
                    targetEntityId = entity.id
                    viewModel.updateEntity(isGlobalEntity, entity.id, name, type, color, notesPath, imagePath)
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
                        notesFilePath = selectedPath,
                        imageFilePath = entity.imageFilePath
                    )
                    showLinkFileDialog = false
                }
            )
        }
    }
}

private fun pickExternalImageFile(): File? {
    val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Select Entity Image", java.awt.FileDialog.LOAD)
    dialog.setFilenameFilter { _, name ->
        val lower = name.lowercase()
        lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp") || lower.endsWith(".svg")
    }
    dialog.isVisible = true
    val dir = dialog.directory
    val file = dialog.file
    return if (dir != null && file != null) File(dir, file) else null
}
