package de.luca.dungeon_master_manager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.*
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.input.key.onKeyEvent
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
import de.luca.dungeon_master_manager.data.KeybindingAction
import de.luca.dungeon_master_manager.data.KeybindingRegistry
import kotlinx.coroutines.launch
import de.luca.dungeon_master_manager.ui.components.EditorUnifiedTopBar
import de.luca.dungeon_master_manager.ui.components.ExplorerDropPayload
import de.luca.dungeon_master_manager.ui.components.GrimoireShortcutDisplay
import de.luca.dungeon_master_manager.ui.components.GrimoireTitleBar
import de.luca.dungeon_master_manager.ui.components.GrimoireWindowFrame
import de.luca.dungeon_master_manager.ui.components.extractExplorerPayload
import de.luca.dungeon_master_manager.ui.components.isExplorerFileDrag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import de.luca.dungeon_master_manager.data.NewItemMode
import de.luca.dungeon_master_manager.ui.components.ExplorerResizeHandle
import de.luca.dungeon_master_manager.ui.components.collectAllFolderOptions
import kotlin.math.roundToInt
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
    val targetMapMarkerId by viewModel.targetMapMarkerId.collectAsState()

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

    LaunchedEffect(viewModel, window, windowState) {
        de.luca.dungeon_master_manager.data.GlobalFileOpener.openFileRequests.collect { file ->
            if (file.exists() && file.isFile) {
                viewModel.openFile(
                    de.luca.dungeon_master_manager.data.FileNode(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = false
                    )
                )
                if (windowState != null && windowState.isMinimized) {
                    windowState.isMinimized = false
                }
                window?.toFront()
                window?.requestFocus()
            }
        }
    }

    val settings by AppSettingsRepository.settings.collectAsState()
    val isFileSearchOpen by viewModel.isFileSearchOpen.collectAsState()
    val searchShortcut = settings.shortcuts[KeybindingAction.SEARCH_FILES]
        ?: KeybindingRegistry.defaultShortcuts()[KeybindingAction.SEARCH_FILES]
        ?: "Ctrl+F"
    val toggleExplorerShortcut = settings.shortcuts[KeybindingAction.TOGGLE_EXPLORER]
        ?: KeybindingRegistry.defaultShortcuts()[KeybindingAction.TOGGLE_EXPLORER]
        ?: "Ctrl+B"
    val closeTabShortcut = settings.shortcuts[KeybindingAction.CLOSE_TAB]
        ?: KeybindingRegistry.defaultShortcuts()[KeybindingAction.CLOSE_TAB]
        ?: "Ctrl+W"
    val nextTabShortcut = settings.shortcuts[KeybindingAction.NEXT_TAB]
        ?: KeybindingRegistry.defaultShortcuts()[KeybindingAction.NEXT_TAB]
        ?: "Ctrl+Tab"
    val prevTabShortcut = settings.shortcuts[KeybindingAction.PREV_TAB]
        ?: KeybindingRegistry.defaultShortcuts()[KeybindingAction.PREV_TAB]
        ?: "Ctrl+Shift+Tab"
    val openSettingsShortcut = settings.shortcuts[KeybindingAction.OPEN_SETTINGS]
        ?: KeybindingRegistry.defaultShortcuts()[KeybindingAction.OPEN_SETTINGS]
        ?: "Ctrl+,"
    val openEntitiesShortcut = settings.shortcuts[KeybindingAction.OPEN_ENTITIES]
        ?: KeybindingRegistry.defaultShortcuts()[KeybindingAction.OPEN_ENTITIES]
        ?: "Ctrl+E"
    val newFileShortcut = settings.shortcuts[KeybindingAction.NEW_FILE]
        ?: KeybindingRegistry.defaultShortcuts()[KeybindingAction.NEW_FILE]
        ?: "Ctrl+N"

    var triggerNewFileDialog by remember { mutableStateOf(0) }
    var showEntityDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (window != null) {
        DisposableEffect(
            window,
            isFileSearchOpen,
            searchShortcut,
            closeTabShortcut,
            nextTabShortcut,
            prevTabShortcut,
            openSettingsShortcut,
            openEntitiesShortcut,
            newFileShortcut,
            openTabs,
            activeTabPath
        ) {
            val focusListener = object : java.awt.event.WindowFocusListener {
                override fun windowGainedFocus(e: java.awt.event.WindowEvent?) {
                    viewModel.loadFileTree()
                }
                override fun windowLostFocus(e: java.awt.event.WindowEvent?) {}
            }
            window.addWindowFocusListener(focusListener)

            val keyDispatcher = java.awt.KeyEventDispatcher { awtEvent ->
                if (!window.isActive) return@KeyEventDispatcher false
                if (awtEvent.id == java.awt.event.KeyEvent.KEY_PRESSED) {
                    if (isFileSearchOpen) return@KeyEventDispatcher false

                    if (KeybindingRegistry.matchesAwtEvent(awtEvent, searchShortcut)) {
                        viewModel.openFileSearch()
                        return@KeyEventDispatcher true
                    }
                    if (KeybindingRegistry.matchesAwtEvent(awtEvent, closeTabShortcut)) {
                        activeTabPath?.let { viewModel.closeTab(it) }
                        return@KeyEventDispatcher true
                    }
                    if (KeybindingRegistry.matchesAwtEvent(awtEvent, nextTabShortcut) && openTabs.size > 1) {
                        val currentIndex = openTabs.indexOfFirst { it.filePath == activeTabPath }
                        val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % openTabs.size
                        viewModel.selectTab(openTabs[nextIndex].filePath)
                        return@KeyEventDispatcher true
                    }
                    if (KeybindingRegistry.matchesAwtEvent(awtEvent, prevTabShortcut) && openTabs.size > 1) {
                        val currentIndex = openTabs.indexOfFirst { it.filePath == activeTabPath }
                        val prevIndex = if (currentIndex <= 0) openTabs.size - 1 else currentIndex - 1
                        viewModel.selectTab(openTabs[prevIndex].filePath)
                        return@KeyEventDispatcher true
                    }
                    if (KeybindingRegistry.matchesAwtEvent(awtEvent, openSettingsShortcut)) {
                        showSettingsDialog = true
                        return@KeyEventDispatcher true
                    }
                    if (KeybindingRegistry.matchesAwtEvent(awtEvent, openEntitiesShortcut)) {
                        showEntityDialog = true
                        return@KeyEventDispatcher true
                    }
                    if (KeybindingRegistry.matchesAwtEvent(awtEvent, newFileShortcut)) {
                        triggerNewFileDialog++
                        return@KeyEventDispatcher true
                    }
                }
                false
            }
            java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyDispatcher)

            onDispose {
                window.removeWindowFocusListener(focusListener)
                java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyDispatcher)
            }
        }
    }
    
    val entityViewModel = remember(viewModel.project.path) { 
        de.luca.dungeon_master_manager.viewmodel.EntityViewModel(viewModel.project.path, viewModel) 
    }

    val projectEnts by entityViewModel.projectEntities.collectAsState()
    val globalEnts by entityViewModel.globalEntities.collectAsState()
    val entities = remember(projectEnts, globalEnts) {
        val list = mutableListOf<de.luca.dungeonmastermanager.database.Entity>()
        list.addAll(globalEnts)
        list.addAll(projectEnts)
        list
    }

    val coroutineScope = rememberCoroutineScope()
    var isExplorerVisible by remember(settings.isExplorerSidebarVisible) {
        mutableStateOf(settings.isExplorerSidebarVisible)
    }
    val density = LocalDensity.current
    var isResizingSidebar by remember { mutableStateOf(false) }
    var sidebarWidth by remember(settings.explorerSidebarWidth) {
        mutableStateOf(settings.explorerSidebarWidth.dp)
    }

    LaunchedEffect(settings.explorerSidebarWidth) {
        if (!isResizingSidebar) {
            sidebarWidth = settings.explorerSidebarWidth.dp
        }
    }

    val toggleExplorer = {
        val next = !isExplorerVisible
        isExplorerVisible = next
        coroutineScope.launch {
            AppSettingsRepository.setExplorerSidebarVisible(next)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (!isFileSearchOpen) {
                    if (KeybindingRegistry.matchesShortcut(event, searchShortcut)) {
                        viewModel.openFileSearch()
                        return@onPreviewKeyEvent true
                    }
                    if (KeybindingRegistry.matchesShortcut(event, closeTabShortcut)) {
                        activeTabPath?.let { viewModel.closeTab(it) }
                        return@onPreviewKeyEvent true
                    }
                    if (KeybindingRegistry.matchesShortcut(event, nextTabShortcut) && openTabs.size > 1) {
                        val currentIndex = openTabs.indexOfFirst { it.filePath == activeTabPath }
                        val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % openTabs.size
                        viewModel.selectTab(openTabs[nextIndex].filePath)
                        return@onPreviewKeyEvent true
                    }
                    if (KeybindingRegistry.matchesShortcut(event, prevTabShortcut) && openTabs.size > 1) {
                        val currentIndex = openTabs.indexOfFirst { it.filePath == activeTabPath }
                        val prevIndex = if (currentIndex <= 0) openTabs.size - 1 else currentIndex - 1
                        viewModel.selectTab(openTabs[prevIndex].filePath)
                        return@onPreviewKeyEvent true
                    }
                    if (KeybindingRegistry.matchesShortcut(event, openSettingsShortcut)) {
                        showSettingsDialog = true
                        return@onPreviewKeyEvent true
                    }
                    if (KeybindingRegistry.matchesShortcut(event, openEntitiesShortcut)) {
                        showEntityDialog = true
                        return@onPreviewKeyEvent true
                    }
                    if (KeybindingRegistry.matchesShortcut(event, newFileShortcut)) {
                        triggerNewFileDialog++
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
            .onKeyEvent { event ->
                if (!isFileSearchOpen) {
                    if (KeybindingRegistry.matchesShortcut(event, toggleExplorerShortcut)) {
                        toggleExplorer()
                        return@onKeyEvent true
                    }
                }
                false
            }
    ) {
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
            // Sidebar with smooth horizontal slide
            AnimatedVisibility(
                visible = isExplorerVisible,
                enter = expandHorizontally(
                    animationSpec = tween(190, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Start
                ) + fadeIn(animationSpec = tween(140)),
                exit = shrinkHorizontally(
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Start
                ) + fadeOut(animationSpec = tween(120))
            ) {
                Row(modifier = Modifier.width(sidebarWidth).fillMaxHeight()) {
                    EditorSidebar(
                        fileTree = fileTree,
                        expandedPaths = expandedPaths,
                        activeFilePath = activeTabPath,
                        projectPath = viewModel.project.path,
                        dropNotice = dropNotice,
                        onDismissDropNotice = { viewModel.dismissDropNotice() },
                        onNodeClick = { viewModel.openFile(it) },
                        onCreateFile = { folder, file -> viewModel.createNewFile(folder, file) },
                        onCreateFolder = { parentFolder, folderName -> viewModel.createNewFolder(parentFolder, folderName) },
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
                        onOpenSearch = { viewModel.openFileSearch() },
                        searchShortcut = searchShortcut,
                        triggerNewFileDialog = triggerNewFileDialog,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )

                    ExplorerResizeHandle(
                        isDragging = isResizingSidebar,
                        onDragStart = { isResizingSidebar = true },
                        onDragEnd = {
                            isResizingSidebar = false
                            coroutineScope.launch {
                                AppSettingsRepository.setExplorerSidebarWidth(sidebarWidth.value.roundToInt())
                            }
                        },
                        onHorizontalDrag = { dragAmountPx ->
                            val deltaDp = with(density) { dragAmountPx.toDp() }
                            sidebarWidth = (sidebarWidth + deltaDp).coerceIn(
                                AppSettingsRepository.MIN_EXPLORER_SIDEBAR_WIDTH.dp,
                                AppSettingsRepository.MAX_EXPLORER_SIDEBAR_WIDTH.dp
                            )
                        }
                    )
                }
            }

            // Content Area (Active tab or Empty state)
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
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
                            projectPath = viewModel.project.path,
                            targetMapMarkerId = targetMapMarkerId,
                            onMarkerFocused = { viewModel.clearTargetMapMarkerId() },
                            onSave = { viewModel.saveActiveFileImmediately() }
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
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isExplorerVisible,
                    enter = fadeIn(animationSpec = tween(150, delayMillis = 80)),
                    exit = fadeOut(animationSpec = tween(100)),
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

        AnimatedVisibility(
            visible = isFileSearchOpen,
            enter = fadeIn(animationSpec = tween(140)),
            exit = fadeOut(animationSpec = tween(100))
        ) {
            FileSearchOverlay(
                searchService = viewModel.searchService,
                openTabPaths = openTabs.map { it.filePath },
                onClose = { viewModel.closeFileSearch() },
                onOpenFile = { node -> viewModel.openFile(node) },
                onOpenHeading = { path, idx -> viewModel.openHeading(path, idx) },
                onOpenEntity = { entityName ->
                    onOpenEntityPopout(EntityPopoutRequest(entityName, viewModel.project.path))
                },
                onCreateNewNote = { noteName ->
                    viewModel.createNewFile("Story", noteName)
                },
                onOpenMapMarker = { mapPath, markerId ->
                    viewModel.openMapAtMarker(mapPath, markerId)
                },
                onPopOutFile = { node -> viewModel.openAsWindow(node) },
                onExecuteCommand = { cmdId ->
                    when (cmdId) {
                        "cmd_new_note" -> viewModel.createNewFile("Story", "New_Note.md")
                        "cmd_new_map" -> viewModel.createNewFile("Maps", "New_Map.map")
                        "cmd_open_explorer" -> {
                            try {
                                val file = java.io.File(viewModel.project.path)
                                if (java.awt.Desktop.isDesktopSupported()) {
                                    java.awt.Desktop.getDesktop().open(file)
                                }
                            } catch (_: Exception) {}
                        }
                        "cmd_open_settings" -> { showSettingsDialog = true }
                        "cmd_toggle_battery" -> {
                            coroutineScope.launch {
                                AppSettingsRepository.setBatterySaverMode(!settings.isBatterySaverMode)
                            }
                        }
                    }
                },
                onInsertLinkToActiveEditor = { title, relPath, _ ->
                    viewModel.showDropNotice("Link kopiert: [$title]($relPath)", isWarning = false)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(onDismiss = { showSettingsDialog = false })
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
    onCreateFolder: (parentFolderName: String, folderName: String) -> Unit,
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
    onOpenSearch: () -> Unit = {},
    searchShortcut: String = "Ctrl+F",
    triggerNewFileDialog: Int = 0,
    modifier: Modifier = Modifier,
) {
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newItemMode by remember { mutableStateOf(NewItemMode.FILE) }
    var targetFolderForNewItem by remember { mutableStateOf<String?>(null) }
    var fileToRename by remember { mutableStateOf<de.luca.dungeon_master_manager.data.FileNode?>(null) }
    var fileToMove by remember { mutableStateOf<de.luca.dungeon_master_manager.data.FileNode?>(null) }
    var fileToDelete by remember { mutableStateOf<de.luca.dungeon_master_manager.data.FileNode?>(null) }

    LaunchedEffect(triggerNewFileDialog) {
        if (triggerNewFileDialog > 0) {
            newItemMode = NewItemMode.FILE
            targetFolderForNewItem = null
            showNewFileDialog = true
        }
    }

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
                        onClick = {
                            targetFolderForNewItem = null
                            newItemMode = NewItemMode.FOLDER
                            showNewFileDialog = true
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 28.dp, minHeight = 28.dp).padding(end = 4.dp)
                    ) {
                        Text("📁+", fontSize = 13.sp, color = GrimoireColors.GoldPrimary, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            targetFolderForNewItem = null
                            newItemMode = NewItemMode.FILE
                            showNewFileDialog = true
                        },
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

            // Search Bar Trigger Button
            val searchInteractionSource = remember { MutableInteractionSource() }
            val isSearchHovered by searchInteractionSource.collectIsHoveredAsState()
            val searchBorderColor by animateColorAsState(
                targetValue = if (isSearchHovered) GrimoireColors.GoldMuted else GrimoireColors.BorderBronze,
                animationSpec = tween(120, easing = FastOutSlowInEasing),
                label = "SidebarSearchBorder"
            )
            val searchBgColor by animateColorAsState(
                targetValue = if (isSearchHovered) GrimoireColors.SurfaceHover else GrimoireColors.SurfaceCard,
                animationSpec = tween(120, easing = FastOutSlowInEasing),
                label = "SidebarSearchBg"
            )
            val searchIconRotation by animateFloatAsState(
                targetValue = if (isSearchHovered) -10f else 0f,
                animationSpec = tween(120, easing = FastOutSlowInEasing),
                label = "SidebarSearchIconRot"
            )
            val searchIconScale by animateFloatAsState(
                targetValue = if (isSearchHovered) 1.15f else 1f,
                animationSpec = tween(120, easing = FastOutSlowInEasing),
                label = "SidebarSearchIconScale"
            )
            val searchTextColor by animateColorAsState(
                targetValue = if (isSearchHovered) GrimoireColors.ParchmentLight else GrimoireColors.ParchmentDim,
                animationSpec = tween(120, easing = FastOutSlowInEasing),
                label = "SidebarSearchText"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(searchBgColor)
                    .border(1.dp, searchBorderColor, RoundedCornerShape(6.dp))
                    .hoverable(searchInteractionSource)
                    .clickable(
                        interactionSource = searchInteractionSource,
                        indication = null
                    ) { onOpenSearch() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "🔍",
                            fontSize = 12.sp,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .graphicsLayer {
                                    rotationZ = searchIconRotation
                                    scaleX = searchIconScale
                                    scaleY = searchIconScale
                                }
                        )
                        Text(
                            text = "Dateien suchen...",
                            style = MaterialTheme.typography.bodySmall,
                            color = searchTextColor,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    GrimoireShortcutDisplay(shortcutString = searchShortcut)
                }
            }

            Spacer(Modifier.height(4.dp))

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
                            onNewFileClick = { node ->
                                val rel = try {
                                    File(node.path).relativeTo(File(projectPath)).path.replace('\\', '/')
                                } catch (_: Exception) {
                                    node.name
                                }
                                targetFolderForNewItem = rel
                                newItemMode = NewItemMode.FILE
                                showNewFileDialog = true
                            },
                            onNewFolderClick = { node ->
                                val rel = try {
                                    File(node.path).relativeTo(File(projectPath)).path.replace('\\', '/')
                                } catch (_: Exception) {
                                    node.name
                                }
                                targetFolderForNewItem = rel
                                newItemMode = NewItemMode.FOLDER
                                showNewFileDialog = true
                            },
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
        val folderOptions = remember(fileTree, projectPath) {
            collectAllFolderOptions(fileTree, projectPath)
        }
        val defaultFolder: String = remember(folderOptions, activeFilePath, targetFolderForNewItem) {
            if (targetFolderForNewItem != null) {
                targetFolderForNewItem!!
            } else {
                val activeRelDir = activeFilePath?.let { path ->
                    try {
                        val parent = File(path).parentFile
                        val root = File(projectPath)
                        if (parent == root) "" else parent.relativeTo(root).path.replace('\\', '/')
                    } catch (_: Exception) {
                        null
                    }
                }
                if (activeRelDir != null && folderOptions.any { it.relativePath == activeRelDir }) {
                    activeRelDir
                } else {
                    folderOptions.firstOrNull { it.relativePath.isNotEmpty() }?.relativePath ?: ""
                }
            }
        }
        var selectedFolder by remember { mutableStateOf(defaultFolder) }
        var selectedFileType by remember {
            mutableStateOf(FileExtensions.defaultFileTypeForFolder(defaultFolder.substringAfterLast('/')))
        }
        var itemName by remember { mutableStateOf("") }
        var expandedFolderDropdown by remember { mutableStateOf(false) }
        var expandedTypeDropdown by remember { mutableStateOf(false) }

        val finalFileName = FileExtensions.sanitizeFileName(itemName, selectedFileType.extension)
        val sanitizedFolderName = itemName.trim().trim('/', '\\')

        val canCreate = when (newItemMode) {
            NewItemMode.FILE -> finalFileName.isNotBlank()
            NewItemMode.FOLDER -> sanitizedFolderName.isNotBlank() && !sanitizedFolderName.contains("..")
        }

        fun doCreate() {
            if (canCreate) {
                when (newItemMode) {
                    NewItemMode.FILE -> onCreateFile(selectedFolder, finalFileName)
                    NewItemMode.FOLDER -> onCreateFolder(selectedFolder, sanitizedFolderName)
                }
                showNewFileDialog = false
            }
        }

        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (newItemMode == NewItemMode.FILE) selectedFileType.icon else "📁",
                            fontSize = 22.sp,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Text(
                            if (newItemMode == NewItemMode.FILE) "New File" else "New Folder",
                            style = MaterialTheme.typography.titleLarge,
                            color = GrimoireColors.GoldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Segmented tabs: File / Folder
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GrimoireColors.SurfaceLow, RoundedCornerShape(8.dp))
                            .border(1.dp, GrimoireColors.BorderBronze.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { newItemMode = NewItemMode.FILE },
                            shape = RoundedCornerShape(6.dp),
                            color = if (newItemMode == NewItemMode.FILE) GrimoireColors.GoldContainer else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📄", fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "File",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (newItemMode == NewItemMode.FILE) FontWeight.Bold else FontWeight.Medium,
                                    color = if (newItemMode == NewItemMode.FILE) GrimoireColors.GoldBright else GrimoireColors.ParchmentMuted
                                )
                            }
                        }

                        Surface(
                            onClick = { newItemMode = NewItemMode.FOLDER },
                            shape = RoundedCornerShape(6.dp),
                            color = if (newItemMode == NewItemMode.FOLDER) GrimoireColors.GoldContainer else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📁", fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Folder",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (newItemMode == NewItemMode.FOLDER) FontWeight.Bold else FontWeight.Medium,
                                    color = if (newItemMode == NewItemMode.FOLDER) GrimoireColors.GoldBright else GrimoireColors.ParchmentMuted
                                )
                            }
                        }
                    }
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
                    // 1. Name field
                    Column {
                        Text(
                            if (newItemMode == NewItemMode.FILE) "Name" else "Folder Name",
                            style = MaterialTheme.typography.labelMedium,
                            color = GrimoireColors.ParchmentMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            placeholder = {
                                Text(
                                    if (newItemMode == NewItemMode.FILE) {
                                        when (selectedFileType.extension) {
                                            "md" -> "e.g. Session 1, Chapter 2, Quest Log"
                                            "map" -> "e.g. Castle Dungeon, Tavern, Overworld"
                                            "txt" -> "e.g. scratchpad, notes, summary"
                                            "json" -> "e.g. encounter_tables, config"
                                            else -> "e.g. my_file"
                                        }
                                    } else {
                                        "e.g. Chapters, Dungeons, Quests, NPCs"
                                    },
                                    color = GrimoireColors.ParchmentMuted.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            suffix = if (newItemMode == NewItemMode.FILE) {
                                {
                                    Text(
                                        ".${selectedFileType.extension}",
                                        color = GrimoireColors.GoldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else null,
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

                    // 2. File Type Dropdown (Only for File mode)
                    if (newItemMode == NewItemMode.FILE) {
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
                    }

                    // 3. Destination Folder Dropdown
                    Column {
                        Text(
                            if (newItemMode == NewItemMode.FILE) "Destination Folder" else "Create in Folder",
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
                                value = if (selectedFolder.isNotBlank()) "📁  $selectedFolder" else "📦  (Project Root)",
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
                                    .heightIn(max = 280.dp)
                            ) {
                                folderOptions.forEach { option ->
                                    val isSelected = option.relativePath == selectedFolder
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Text(if (option.depth == 0) "📦" else "📁", fontSize = 16.sp)
                                        },
                                        text = {
                                            Row(
                                                modifier = Modifier.padding(start = (option.depth * 14).dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (option.depth > 0) {
                                                    Text("↳ ", color = GrimoireColors.BorderBronze, fontSize = 12.sp)
                                                }
                                                Text(
                                                    option.displayName,
                                                    color = if (isSelected) GrimoireColors.GoldPrimary else GrimoireColors.ParchmentLight,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedFolder = option.relativePath
                                            expandedFolderDropdown = false
                                            if (newItemMode == NewItemMode.FILE && itemName.isBlank()) {
                                                selectedFileType = FileExtensions.defaultFileTypeForFolder(option.displayName)
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
                    val previewPath = if (newItemMode == NewItemMode.FILE) {
                        if (finalFileName.isNotBlank()) {
                            if (selectedFolder.isNotBlank()) "$selectedFolder/$finalFileName" else finalFileName
                        } else {
                            if (selectedFolder.isNotBlank()) "$selectedFolder/<name>.${selectedFileType.extension}" else "<name>.${selectedFileType.extension}"
                        }
                    } else {
                        if (sanitizedFolderName.isNotBlank()) {
                            if (selectedFolder.isNotBlank()) "$selectedFolder/$sanitizedFolderName/" else "$sanitizedFolderName/"
                        } else {
                            if (selectedFolder.isNotBlank()) "$selectedFolder/<folder_name>/" else "<folder_name>/"
                        }
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
                                color = if (canCreate) GrimoireColors.GoldBright else GrimoireColors.ParchmentMuted
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
                    Text(
                        if (newItemMode == NewItemMode.FILE) "Create File" else "Create Folder",
                        fontWeight = FontWeight.Bold
                    )
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
        val folderOptions = remember(fileTree, projectPath) {
            collectAllFolderOptions(fileTree, projectPath)
        }
        var selectedFolder by remember {
            mutableStateOf(folderOptions.firstOrNull { it.relativePath.isNotEmpty() }?.relativePath ?: "")
        }
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
                            .heightIn(max = 220.dp)
                            .verticalScroll(scrollState)
                    ) {
                        folderOptions.forEach { option ->
                            val isSelected = selectedFolder == option.relativePath
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFolder = option.relativePath }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedFolder = option.relativePath },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = GrimoireColors.GoldPrimary,
                                        unselectedColor = GrimoireColors.BorderBronze
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Row(
                                    modifier = Modifier.padding(start = (option.depth * 14).dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (option.depth > 0) {
                                        Text("↳ ", color = GrimoireColors.BorderBronze, fontSize = 12.sp)
                                    }
                                    Text(
                                        if (option.depth == 0) "📦 (Project Root)" else "📁 ${option.displayName}",
                                        color = if (isSelected) GrimoireColors.GoldBright else GrimoireColors.ParchmentLight,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMoveFile(fileToMove!!, selectedFolder)
                        fileToMove = null
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
