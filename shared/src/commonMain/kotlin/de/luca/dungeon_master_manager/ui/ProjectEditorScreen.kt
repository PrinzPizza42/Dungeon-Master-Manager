package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import de.luca.dungeon_master_manager.data.AppSettingsRepository
import de.luca.dungeon_master_manager.data.CreatableFileType
import de.luca.dungeon_master_manager.data.EntityPopoutRequest
import de.luca.dungeon_master_manager.data.FileExtensions
import kotlinx.coroutines.launch
import de.luca.dungeon_master_manager.ui.components.EditorUnifiedTopBar
import de.luca.dungeon_master_manager.ui.components.ExplorerDropPayload
import de.luca.dungeon_master_manager.ui.components.GrimoireTitleBar
import de.luca.dungeon_master_manager.ui.components.GrimoireWindowFrame
import de.luca.dungeon_master_manager.ui.components.extractExplorerPayload
import de.luca.dungeon_master_manager.ui.components.isExplorerFileDrag
import de.luca.dungeon_master_manager.ui.theme.GrimoireColors
import de.luca.dungeon_master_manager.ui.theme.GrimoireDivider
import de.luca.dungeon_master_manager.ui.theme.GrimoireTheme
import de.luca.dungeon_master_manager.viewmodel.ConflictAction
import de.luca.dungeon_master_manager.viewmodel.ConflictDecision
import de.luca.dungeon_master_manager.viewmodel.ExplorerDropNotice
import de.luca.dungeon_master_manager.viewmodel.ProjectEditorViewModel
import java.awt.Cursor
import java.awt.Window
import java.io.File

/**
 * Main project editor screen with sidebar file tree, tab bar, and content area.
 */
@Composable
fun ProjectEditorScreen(
    viewModel: ProjectEditorViewModel,
    applicationScope: androidx.compose.ui.window.ApplicationScope?,
    onBackToLauncher: () -> Unit,
    onOpenEntityPopout: (EntityPopoutRequest) -> Unit,
    window: Window? = null,
    windowState: androidx.compose.ui.window.WindowState? = null,
    onCloseRequest: () -> Unit = {},
    onTitleChange: (String) -> Unit = {},
) {
    val fileTree by viewModel.fileTree.collectAsState()
    val expandedPaths by viewModel.expandedPaths.collectAsState()
    val openTabs by viewModel.openTabs.collectAsState()
    val activeTabPath by viewModel.activeTabPath.collectAsState()
    val fileContent by viewModel.activeFileContent.collectAsState()
    val isLoadingContent by viewModel.isLoadingContent.collectAsState()
    val pendingConflict by viewModel.pendingConflict.collectAsState()
    val dropNotice by viewModel.dropNotice.collectAsState()

    val activeTab = openTabs.find { it.filePath == activeTabPath }

    // Dynamic hierarchical title update
    val currentTitle = remember(viewModel.project.config.name, activeTab?.fileName) {
        if (activeTab != null) {
            "${activeTab.fileName} - ${viewModel.project.config.name} - Dungeon Master Manager"
        } else {
            "${viewModel.project.config.name} - Dungeon Master Manager"
        }
    }
    LaunchedEffect(currentTitle) {
        onTitleChange(currentTitle)
    }

    if (window != null) {
        DisposableEffect(window) {
            val focusListener = object : java.awt.event.WindowFocusListener {
                override fun windowGainedFocus(e: java.awt.event.WindowEvent?) {
                    viewModel.loadFileTree()
                }
                override fun windowLostFocus(e: java.awt.event.WindowEvent?) {}
            }
            window.addWindowFocusListener(focusListener)
            onDispose {
                window.removeWindowFocusListener(focusListener)
            }
        }
    }
    
    val entityViewModel = remember(viewModel.project.path) { 
        de.luca.dungeon_master_manager.viewmodel.EntityViewModel(viewModel.project.path, viewModel) 
    }
    var showEntityDialog by remember { mutableStateOf(false) }

    val projectEnts by entityViewModel.projectEntities.collectAsState()
    val globalEnts by entityViewModel.globalEntities.collectAsState()
    val entities = remember(projectEnts, globalEnts) {
        val list = mutableListOf<de.luca.dungeonmastermanager.database.Entity>()
        list.addAll(globalEnts)
        list.addAll(projectEnts)
        list
    }

    val settings by AppSettingsRepository.settings.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isExplorerVisible by remember(settings.isExplorerSidebarVisible) {
        mutableStateOf(settings.isExplorerSidebarVisible)
    }

    val toggleExplorer = {
        val next = !isExplorerVisible
        isExplorerVisible = next
        coroutineScope.launch {
            AppSettingsRepository.setExplorerSidebarVisible(next)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(GrimoireColors.Background)) {
        // Unified Top Titlebar (replaces old EditorToolbar and TabBar)
        if (window != null && windowState != null) {
            EditorUnifiedTopBar(
                window = window,
                projectName = viewModel.project.config.name,
                tabs = openTabs,
                activeTabPath = activeTabPath,
                onTabSelect = { viewModel.selectTab(it) },
                onTabClose = { viewModel.closeTab(it) },
                onTabPopOut = { viewModel.popOutTab(it) },
                onBack = onBackToLauncher,
                windowState = windowState,
                onCloseRequest = onCloseRequest,
                isExplorerVisible = isExplorerVisible,
                onToggleExplorer = { toggleExplorer() }
            )
        }

        // Main content: sidebar + editor
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar
            if (isExplorerVisible) {
                EditorSidebar(
                    fileTree = fileTree,
                    expandedPaths = expandedPaths,
                    activeFilePath = activeTabPath,
                    projectPath = viewModel.project.path,
                    dropNotice = dropNotice,
                    onDismissDropNotice = { viewModel.dismissDropNotice() },
                    onNodeClick = { viewModel.openFile(it) },
                    onCreateFile = { folder, file -> viewModel.createNewFile(folder, file) },
                    onRenameFile = { node, newName -> viewModel.renameFile(node, newName) },
                    onMoveFile = { node, folder -> viewModel.moveFile(node, folder) },
                    onDeleteFile = { node -> viewModel.deleteFile(node) },
                    onPopOutClick = { node -> viewModel.openAsWindow(node) },
                    onExternalFilesDropped = { files, targetDir -> viewModel.importExternalFiles(files, targetDir) },
                    onInternalFileMoved = { sourcePath, targetDir -> viewModel.moveInternalFile(sourcePath, targetDir) },
                    onDragTransferCompleted = { filePath -> viewModel.onDragTransferCompleted(filePath) },
                    onOpenNativeExplorer = {
                        try {
                            val file = java.io.File(viewModel.project.path)
                            if (java.awt.Desktop.isDesktopSupported()) {
                                java.awt.Desktop.getDesktop().open(file)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onManageEntities = { showEntityDialog = true },
                    onToggleCollapse = { toggleExplorer() },
                    modifier = Modifier.width(260.dp).fillMaxHeight(),
                )

                VerticalDivider(color = GrimoireColors.BorderBronze)
            }

            // Content Area (Active tab or Empty state)
            Box(modifier = Modifier.fillMaxSize()) {
                if (openTabs.isNotEmpty()) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        ContentArea(
                            activeTab = activeTab,
                            fileContent = fileContent,
                            isLoading = isLoadingContent,
                            onContentChange = { viewModel.updateActiveFileContent(it) },
                            onOpenEntityPopout = { entityName ->
                                onOpenEntityPopout(EntityPopoutRequest(entityName, viewModel.project.path))
                            },
                            onOpenImageInTab = { relativeOrAbsolutePath ->
                                val file = resolveImageFile(relativeOrAbsolutePath, viewModel.project.path)
                                if (file != null && file.exists() && file.isFile) {
                                    viewModel.openFile(
                                        de.luca.dungeon_master_manager.data.FileNode(
                                            name = file.name,
                                            path = file.absolutePath,
                                            isDirectory = false
                                        )
                                    )
                                }
                            },
                            entities = entities,
                            projectPath = viewModel.project.path
                        )
                    }
                } else {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize().background(GrimoireColors.Background),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No tab open",
                                style = MaterialTheme.typography.titleMedium,
                                color = GrimoireColors.ParchmentMuted,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Select a file from the explorer to view or edit",
                                style = MaterialTheme.typography.bodySmall,
                                color = GrimoireColors.ParchmentDim,
                            )
                        }
                    }
                }

                // Docked Flap to Show Explorer when hidden (mirroring Timeline Sidebar flap)
                if (!isExplorerVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 48.dp, start = 0.dp)
                    ) {
                        IconButton(
                            onClick = { toggleExplorer() },
                            modifier = Modifier
                                .size(24.dp)
                                .background(GrimoireColors.SurfaceCard, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                .border(1.dp, GrimoireColors.BorderBronze, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Show Explorer",
                                tint = GrimoireColors.GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEntityDialog) {
        EntityManagerDialog(
            isGlobalOnly = false,
            viewModel = entityViewModel,
            onDismiss = { showEntityDialog = false },
            projectPath = viewModel.project.path,
            onOpenEntityPopout = onOpenEntityPopout
        )
    }

    if (pendingConflict != null) {
        val prompt = pendingConflict!!
        var applyToAll by remember(prompt.fileName) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { prompt.onCancelBatch() },
            title = {
                Text("Namenskonflikt", style = MaterialTheme.typography.titleLarge, color = GrimoireColors.GoldPrimary)
            },
            containerColor = GrimoireColors.SurfaceCard,
            shape = RoundedCornerShape(8.dp),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "In '${prompt.targetFolderDisplayName}' existiert bereits eine Datei namens '${prompt.fileName}'.",
                        color = GrimoireColors.ParchmentLight,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Wie möchtest du mit diesem Konflikt verfahren?",
                        color = GrimoireColors.ParchmentMuted,
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (prompt.remainingFilesCount > 0) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { applyToAll = !applyToAll }
                        ) {
                            Checkbox(
                                checked = applyToAll,
                                onCheckedChange = { applyToAll = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = GrimoireColors.GoldPrimary,
                                    uncheckedColor = GrimoireColors.BorderBronze,
                                    checkmarkColor = GrimoireColors.Background
                                )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Für alle weiteren ${prompt.remainingFilesCount} Dateien anwenden",
                                style = MaterialTheme.typography.bodySmall,
                                color = GrimoireColors.ParchmentLight
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            prompt.onDecision(
                                ConflictDecision(
                                    action = ConflictAction.RENAME,
                                    applyToAll = applyToAll
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GrimoireColors.GoldPrimary,
                            contentColor = GrimoireColors.Background
                        )
                    ) {
                        Text("Umbenennen")
                    }
                    Button(
                        onClick = {
                            prompt.onDecision(
                                ConflictDecision(
                                    action = ConflictAction.OVERWRITE,
                                    applyToAll = applyToAll
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GrimoireColors.CrimsonError,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Überschreiben")
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            prompt.onDecision(
                                ConflictDecision(
                                    action = ConflictAction.SKIP,
                                    applyToAll = applyToAll
                                )
                            )
                        }
                    ) {
                        Text("Überspringen", color = GrimoireColors.ParchmentMuted)
                    }
                    TextButton(onClick = { prompt.onCancelBatch() }) {
                        Text("Abbrechen", color = GrimoireColors.ParchmentDim)
                    }
                }
            }
        )
    }

    val poppedOutTabs by viewModel.poppedOutTabs.collectAsState()
    if (applicationScope != null) {
        with(applicationScope) {
            for (poppedOut in poppedOutTabs) {
                key(poppedOut.tab.filePath) {
                    val detachedState = androidx.compose.ui.window.rememberWindowState(width = 800.dp, height = 600.dp)
                    val detachedTitle = "${poppedOut.tab.fileName} - ${viewModel.project.config.name}"
                    androidx.compose.ui.window.Window(
                        onCloseRequest = { viewModel.closePoppedOutTab(poppedOut.tab.filePath) },
                        title = detachedTitle,
                        state = detachedState,
                        undecorated = true,
                    ) {
                        val detachedWindow = this.window
                        GrimoireTheme {
                            GrimoireWindowFrame(
                                window = detachedWindow,
                                windowState = detachedState,
                                focusTrigger = poppedOut.focusTrigger,
                                minWidth = 400.dp,
                                minHeight = 300.dp
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    GrimoireTitleBar(
                                        window = detachedWindow,
                                        title = detachedTitle,
                                        windowState = detachedState,
                                        onCloseRequest = { viewModel.closePoppedOutTab(poppedOut.tab.filePath) },
                                        subtitle = viewModel.project.config.name
                                    )
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        StandaloneFileEditorWindow(
                                            tab = poppedOut.tab,
                                            projectPath = viewModel.project.path,
                                            entities = entities,
                                            onOpenEntityPopout = { entityName ->
                                                onOpenEntityPopout(EntityPopoutRequest(entityName, viewModel.project.path))
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
    }
}

@Composable
fun StandaloneFileEditorWindow(
    tab: de.luca.dungeon_master_manager.data.EditorTab,
    projectPath: String,
    entities: List<de.luca.dungeonmastermanager.database.Entity>,
    onOpenEntityPopout: (String) -> Unit
) {
    var fileContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val isEditable = FileExtensions.isEditableText(tab.extension)
    
    LaunchedEffect(tab.filePath) {
        isLoading = true
        if (isEditable) {
            fileContent = de.luca.dungeon_master_manager.data.FileService().readFile(tab.filePath)
        } else {
            fileContent = ""
        }
        isLoading = false
    }

    var contentToSave by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(contentToSave) {
        contentToSave?.let { content ->
            kotlinx.coroutines.delay(500)
            if (isEditable) {
                de.luca.dungeon_master_manager.data.FileService().writeFile(tab.filePath, content)
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        ContentArea(
            activeTab = tab,
            fileContent = fileContent,
            isLoading = isLoading,
            onContentChange = { 
                fileContent = it
                contentToSave = it 
            },
            onOpenEntityPopout = onOpenEntityPopout,
            entities = entities,
            projectPath = projectPath
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun EditorSidebar(
    fileTree: de.luca.dungeon_master_manager.data.FileNode?,
    expandedPaths: Set<String>,
    activeFilePath: String?,
    projectPath: String,
    dropNotice: ExplorerDropNotice?,
    onDismissDropNotice: () -> Unit,
    onNodeClick: (de.luca.dungeon_master_manager.data.FileNode) -> Unit,
    onCreateFile: (folderName: String, fileName: String) -> Unit,
    onRenameFile: (de.luca.dungeon_master_manager.data.FileNode, String) -> Unit,
    onMoveFile: (de.luca.dungeon_master_manager.data.FileNode, String) -> Unit,
    onDeleteFile: (de.luca.dungeon_master_manager.data.FileNode) -> Unit,
    onPopOutClick: (de.luca.dungeon_master_manager.data.FileNode) -> Unit,
    onExternalFilesDropped: (List<File>, String) -> Unit,
    onInternalFileMoved: (String, String) -> Unit,
    onDragTransferCompleted: (String) -> Unit = {},
    onOpenNativeExplorer: () -> Unit,
    onManageEntities: () -> Unit,
    onToggleCollapse: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showNewFileDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<de.luca.dungeon_master_manager.data.FileNode?>(null) }
    var fileToMove by remember { mutableStateOf<de.luca.dungeon_master_manager.data.FileNode?>(null) }
    var fileToDelete by remember { mutableStateOf<de.luca.dungeon_master_manager.data.FileNode?>(null) }

    var hoveredDropFolderPath by remember { mutableStateOf<String?>(null) }
    val isRootHovered = hoveredDropFolderPath == projectPath

    val rootDropTarget = remember(projectPath) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                hoveredDropFolderPath = null
                val payload = event.extractExplorerPayload() ?: return false
                when (payload) {
                    is ExplorerDropPayload.ExternalFiles -> {
                        onExternalFilesDropped(payload.files, projectPath)
                        return true
                    }
                    is ExplorerDropPayload.InternalFile -> {
                        onInternalFileMoved(payload.data.filePath, projectPath)
                        return true
                    }
                }
            }

            override fun onEntered(event: DragAndDropEvent) {
                hoveredDropFolderPath = projectPath
            }

            override fun onExited(event: DragAndDropEvent) {
                if (hoveredDropFolderPath == projectPath) {
                    hoveredDropFolderPath = null
                }
            }

            override fun onEnded(event: DragAndDropEvent) {
                hoveredDropFolderPath = null
            }
        }
    }

    Surface(
        modifier = modifier,
        color = GrimoireColors.SurfaceLow,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sidebar header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "EXPLORER",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = GrimoireColors.ParchmentMuted,
                    letterSpacing = 1.sp
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onOpenNativeExplorer,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 28.dp, minHeight = 28.dp).padding(end = 4.dp)
                    ) {
                        Text("📁", fontSize = 14.sp)
                    }
                    TextButton(
                        onClick = { showNewFileDialog = true },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 28.dp, minHeight = 28.dp).padding(end = 4.dp)
                    ) {
                        Text("+", fontSize = 16.sp, color = GrimoireColors.GoldPrimary, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = onToggleCollapse,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Hide Explorer",
                            tint = GrimoireColors.ParchmentMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Drop Notice Banner (if any)
            dropNotice?.let { notice ->
                val containerColor = if (notice.isWarning) GrimoireColors.ErrorContainer.copy(alpha = 0.85f) else GrimoireColors.GoldContainer
                val textColor = if (notice.isWarning) GrimoireColors.OnErrorContainer else GrimoireColors.GoldBright
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(containerColor)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = notice.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onDismissDropNotice,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                    ) {
                        Text("✕", fontSize = 12.sp, color = textColor)
                    }
                }
            }

            HorizontalDivider(color = GrimoireColors.BorderBronze)

            if (fileTree != null) {
                val scrollState = rememberScrollState()
                val rootBorderModifier = if (isRootHovered) {
                    Modifier
                        .border(1.dp, GrimoireColors.GoldPrimary, RoundedCornerShape(4.dp))
                        .background(GrimoireColors.GoldContainer.copy(alpha = 0.25f))
                } else {
                    Modifier
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(rootBorderModifier)
                        .dragAndDropTarget(
                            shouldStartDragAndDrop = { event -> event.isExplorerFileDrag() },
                            target = rootDropTarget
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        FileTreeView(
                            rootNode = fileTree,
                            expandedPaths = expandedPaths,
                            activeFilePath = activeFilePath,
                            hoveredDropFolderPath = hoveredDropFolderPath,
                            onDropTargetHoverChange = { folder, isEntered ->
                                if (isEntered) {
                                    hoveredDropFolderPath = folder
                                } else if (hoveredDropFolderPath == folder) {
                                    hoveredDropFolderPath = null
                                }
                            },
                            onPayloadDropped = { payload, targetFolderPath ->
                                when (payload) {
                                    is ExplorerDropPayload.ExternalFiles -> {
                                        onExternalFilesDropped(payload.files, targetFolderPath)
                                        true
                                    }
                                    is ExplorerDropPayload.InternalFile -> {
                                        onInternalFileMoved(payload.data.filePath, targetFolderPath)
                                        true
                                    }
                                }
                            },
                            onDragTransferCompleted = onDragTransferCompleted,
                            onNodeClick = onNodeClick,
                            onRenameClick = { fileToRename = it },
                            onMoveClick = { fileToMove = it },
                            onDeleteClick = { fileToDelete = it },
                            onPopOutClick = onPopOutClick,
                        )
                    }

                    if (isRootHovered) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = GrimoireColors.GoldContainer,
                            border = BorderStroke(1.dp, GrimoireColors.BorderGold)
                        ) {
                            Text(
                                text = "📥 Im Projekt-Root ablegen",
                                color = GrimoireColors.GoldBright,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = GrimoireColors.GoldPrimary)
                }
            }

            // Entities button at the bottom of the Explorer sidebar
            HorizontalDivider(color = GrimoireColors.BorderBronze)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = onManageEntities,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrimoireColors.GoldContainer,
                        contentColor = GrimoireColors.GoldBright
                    ),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, GrimoireColors.BorderGold),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                ) {
                    Text(
                        "❖ Entities",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showNewFileDialog && fileTree != null) {
        val folders: List<String> = remember(fileTree) {
            val list = fileTree.children.filter { it.isDirectory }.map { it.name }
            if (list.isEmpty()) listOf("") else list
        }
        val defaultFolder: String = remember(folders, activeFilePath) {
            val activeDirName = activeFilePath?.let { File(it).parentFile?.name }
            if (activeDirName != null && folders.contains(activeDirName)) {
                activeDirName
            } else {
                folders.firstOrNull() ?: ""
            }
        }
        var selectedFolder by remember { mutableStateOf(defaultFolder) }
        var selectedFileType by remember {
            mutableStateOf(FileExtensions.defaultFileTypeForFolder(defaultFolder))
        }
        var fileName by remember { mutableStateOf("") }
        var expandedFolderDropdown by remember { mutableStateOf(false) }
        var expandedTypeDropdown by remember { mutableStateOf(false) }

        val finalFileName = FileExtensions.sanitizeFileName(fileName, selectedFileType.extension)
        val canCreate = finalFileName.isNotBlank() && selectedFolder.isNotBlank()

        fun doCreate() {
            if (canCreate) {
                onCreateFile(selectedFolder, finalFileName)
                showNewFileDialog = false
            }
        }

        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedFileType.icon, fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
                    Text(
                        "New File",
                        style = MaterialTheme.typography.titleLarge,
                        color = GrimoireColors.GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = GrimoireColors.SurfaceCard,
            shape = RoundedCornerShape(10.dp),
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(min = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. File Name field with extension suffix
                    Column {
                        Text(
                            "Name",
                            style = MaterialTheme.typography.labelMedium,
                            color = GrimoireColors.ParchmentMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = fileName,
                            onValueChange = { fileName = it },
                            placeholder = {
                                Text(
                                    when (selectedFileType.extension) {
                                        "md" -> "e.g. Session 1, Chapter 2, Quest Log"
                                        "map" -> "e.g. Castle Dungeon, Tavern, Overworld"
                                        "txt" -> "e.g. scratchpad, notes, summary"
                                        "json" -> "e.g. encounter_tables, config"
                                        else -> "e.g. my_file"
                                    },
                                    color = GrimoireColors.ParchmentMuted.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            suffix = {
                                Text(
                                    ".${selectedFileType.extension}",
                                    color = GrimoireColors.GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { doCreate() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onPreviewKeyEvent {
                                    if ((it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyUp) {
                                        doCreate()
                                        true
                                    } else false
                                },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GrimoireColors.GoldPrimary,
                                unfocusedBorderColor = GrimoireColors.BorderBronze,
                                focusedTextColor = GrimoireColors.ParchmentLight,
                                unfocusedTextColor = GrimoireColors.ParchmentLight,
                                cursorColor = GrimoireColors.GoldPrimary,
                            )
                        )
                    }

                    // 2. File Type Dropdown
                    Column {
                        Text(
                            "File Type",
                            style = MaterialTheme.typography.labelMedium,
                            color = GrimoireColors.ParchmentMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = expandedTypeDropdown,
                            onExpandedChange = { expandedTypeDropdown = !expandedTypeDropdown },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = "${selectedFileType.icon}  ${selectedFileType.name}",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeDropdown)
                                },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                                    .fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GrimoireColors.GoldPrimary,
                                    unfocusedBorderColor = GrimoireColors.BorderBronze,
                                    focusedTextColor = GrimoireColors.ParchmentLight,
                                    unfocusedTextColor = GrimoireColors.ParchmentLight,
                                )
                            )
                            DropdownMenu(
                                expanded = expandedTypeDropdown,
                                onDismissRequest = { expandedTypeDropdown = false },
                                modifier = Modifier
                                    .background(GrimoireColors.SurfaceCard)
                                    .border(1.dp, GrimoireColors.BorderBronze, RoundedCornerShape(4.dp))
                            ) {
                                FileExtensions.CREATABLE_FILE_TYPES.forEach { fileType ->
                                    val isSelected = fileType.extension == selectedFileType.extension
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Text(fileType.icon, fontSize = 20.sp)
                                        },
                                        text = {
                                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                                Text(
                                                    fileType.name,
                                                    color = if (isSelected) GrimoireColors.GoldPrimary else GrimoireColors.ParchmentLight,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    fileType.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = GrimoireColors.ParchmentMuted,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedFileType = fileType
                                            expandedTypeDropdown = false
                                        },
                                        modifier = Modifier.background(
                                            if (isSelected) GrimoireColors.GoldContainer.copy(alpha = 0.25f) else Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 3. Destination Folder Dropdown
                    Column {
                        Text(
                            "Destination Folder",
                            style = MaterialTheme.typography.labelMedium,
                            color = GrimoireColors.ParchmentMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = expandedFolderDropdown,
                            onExpandedChange = { expandedFolderDropdown = !expandedFolderDropdown },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = if (selectedFolder.isNotBlank()) "📁  $selectedFolder" else "(Project Root)",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFolderDropdown)
                                },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                                    .fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GrimoireColors.GoldPrimary,
                                    unfocusedBorderColor = GrimoireColors.BorderBronze,
                                    focusedTextColor = GrimoireColors.ParchmentLight,
                                    unfocusedTextColor = GrimoireColors.ParchmentLight,
                                )
                            )
                            DropdownMenu(
                                expanded = expandedFolderDropdown,
                                onDismissRequest = { expandedFolderDropdown = false },
                                modifier = Modifier
                                    .background(GrimoireColors.SurfaceCard)
                                    .border(1.dp, GrimoireColors.BorderBronze, RoundedCornerShape(4.dp))
                            ) {
                                folders.forEach { folder ->
                                    val isSelected = folder == selectedFolder
                                    DropdownMenuItem(
                                        leadingIcon = { Text("📁", fontSize = 16.sp) },
                                        text = {
                                            Text(
                                                folder,
                                                color = if (isSelected) GrimoireColors.GoldPrimary else GrimoireColors.ParchmentLight,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedFolder = folder
                                            expandedFolderDropdown = false
                                            if (fileName.isBlank()) {
                                                selectedFileType = FileExtensions.defaultFileTypeForFolder(folder)
                                            }
                                        },
                                        modifier = Modifier.background(
                                            if (isSelected) GrimoireColors.GoldContainer.copy(alpha = 0.25f) else Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 4. Live Preview Box
                    val previewPath = if (finalFileName.isNotBlank()) {
                        if (selectedFolder.isNotBlank()) "$selectedFolder/$finalFileName" else finalFileName
                    } else {
                        if (selectedFolder.isNotBlank()) "$selectedFolder/<name>.${selectedFileType.extension}" else "<name>.${selectedFileType.extension}"
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GrimoireColors.SurfaceLow,
                        border = BorderStroke(1.dp, GrimoireColors.BorderBronze.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Will create: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = GrimoireColors.ParchmentMuted
                            )
                            Text(
                                previewPath,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (finalFileName.isNotBlank()) GrimoireColors.GoldBright else GrimoireColors.ParchmentMuted
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { doCreate() },
                    enabled = canCreate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrimoireColors.GoldPrimary,
                        contentColor = GrimoireColors.Background,
                        disabledContainerColor = GrimoireColors.SurfaceLow,
                        disabledContentColor = GrimoireColors.ParchmentMuted.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Create File", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancel", color = GrimoireColors.ParchmentMuted)
                }
            }
        )
    }

    if (fileToRename != null) {
        val targetNode = fileToRename!!
        val isDirectory = targetNode.isDirectory
        val extension = targetNode.extension
        val initialBaseName = remember(targetNode) {
            if (!isDirectory && extension.isNotEmpty()) {
                targetNode.name.removeSuffix(".$extension")
            } else {
                targetNode.name
            }
        }
        var newBaseName by remember(targetNode) { mutableStateOf(initialBaseName) }
        val finalRenameName = if (!isDirectory && extension.isNotEmpty()) {
            FileExtensions.sanitizeFileName(newBaseName, extension)
        } else {
            newBaseName.trim()
        }
        val canRename = finalRenameName.isNotBlank() && finalRenameName != targetNode.name

        fun doRename() {
            if (canRename) {
                onRenameFile(targetNode, finalRenameName)
                fileToRename = null
            }
        }

        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isDirectory) "📁" else (FileExtensions.CREATABLE_FILE_TYPES.firstOrNull { it.extension == extension }?.icon ?: "📄"),
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        if (isDirectory) "Rename Folder" else "Rename File",
                        style = MaterialTheme.typography.titleLarge,
                        color = GrimoireColors.GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = GrimoireColors.SurfaceCard,
            shape = RoundedCornerShape(10.dp),
            text = {
                Column(modifier = Modifier.fillMaxWidth().widthIn(min = 360.dp)) {
                    OutlinedTextField(
                        value = newBaseName,
                        onValueChange = { newBaseName = it },
                        label = { Text(if (isDirectory) "Folder Name" else "File Name", color = GrimoireColors.ParchmentMuted) },
                        suffix = if (!isDirectory && extension.isNotEmpty()) {
                            {
                                Text(
                                    ".$extension",
                                    color = GrimoireColors.GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { doRename() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onPreviewKeyEvent {
                                if ((it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyUp) {
                                    doRename()
                                    true
                                } else false
                            },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GrimoireColors.GoldPrimary,
                            unfocusedBorderColor = GrimoireColors.BorderBronze,
                            focusedTextColor = GrimoireColors.ParchmentLight,
                            unfocusedTextColor = GrimoireColors.ParchmentLight,
                            cursorColor = GrimoireColors.GoldPrimary,
                        )
                    )
                    if (!isDirectory && extension.isNotEmpty() && finalRenameName.isNotBlank() && finalRenameName != targetNode.name) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "New name: $finalRenameName",
                            style = MaterialTheme.typography.bodySmall,
                            color = GrimoireColors.GoldBright
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { doRename() },
                    enabled = canRename,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrimoireColors.GoldPrimary,
                        contentColor = GrimoireColors.Background,
                        disabledContainerColor = GrimoireColors.SurfaceLow,
                        disabledContentColor = GrimoireColors.ParchmentMuted.copy(alpha = 0.5f)
                    )
                ) { Text("Rename", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) {
                    Text("Cancel", color = GrimoireColors.ParchmentMuted)
                }
            }
        )
    }

    if (fileToMove != null && fileTree != null) {
        val folders = fileTree.children.filter { it.isDirectory }.map { it.name }
        var selectedFolder by remember { mutableStateOf(folders.firstOrNull() ?: "") }
        AlertDialog(
            onDismissRequest = { fileToMove = null },
            title = { Text("Move File", style = MaterialTheme.typography.titleLarge, color = GrimoireColors.GoldPrimary) },
            containerColor = GrimoireColors.SurfaceCard,
            shape = RoundedCornerShape(8.dp),
            text = {
                Column {
                    Text("Move '${fileToMove!!.name}' to:", color = GrimoireColors.ParchmentLight)
                    Spacer(Modifier.height(8.dp))
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                            .verticalScroll(scrollState)
                    ) {
                        folders.forEach { folder ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFolder = folder }
                                    .padding(vertical = 4.dp)
                                ) {
                                RadioButton(
                                    selected = (selectedFolder == folder),
                                    onClick = { selectedFolder = folder },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = GrimoireColors.GoldPrimary,
                                        unselectedColor = GrimoireColors.BorderBronze
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(folder, color = GrimoireColors.ParchmentLight)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedFolder.isNotBlank()) {
                            onMoveFile(fileToMove!!, selectedFolder)
                            fileToMove = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrimoireColors.GoldPrimary,
                        contentColor = GrimoireColors.Background
                    )
                ) { Text("Move") }
            },
            dismissButton = {
                TextButton(onClick = { fileToMove = null }) { 
                    Text("Cancel", color = GrimoireColors.ParchmentMuted) 
                }
            }
        )
    }

    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete File", style = MaterialTheme.typography.titleLarge, color = GrimoireColors.GoldPrimary) },
            containerColor = GrimoireColors.SurfaceCard,
            shape = RoundedCornerShape(8.dp),
            text = { Text("Are you sure you want to delete '${fileToDelete!!.name}'? This cannot be undone.", color = GrimoireColors.ParchmentLight) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFile(fileToDelete!!)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GrimoireColors.CrimsonError)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) { 
                    Text("Cancel", color = GrimoireColors.ParchmentMuted) 
                }
            }
        )
    }
}
