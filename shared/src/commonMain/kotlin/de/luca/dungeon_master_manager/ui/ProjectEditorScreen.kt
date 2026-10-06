package de.luca.dungeon_master_manager.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.luca.dungeon_master_manager.viewmodel.ProjectEditorViewModel

/**
 * Main project editor screen with sidebar file tree, tab bar, and content area.
 */
import de.luca.dungeon_master_manager.data.EntityPopoutRequest
import java.awt.Cursor

@Composable
fun ProjectEditorScreen(
    viewModel: ProjectEditorViewModel,
    onBackToLauncher: () -> Unit,
    onOpenEntityPopout: (EntityPopoutRequest) -> Unit,
) {
    val fileTree by viewModel.fileTree.collectAsState()
    val expandedPaths by viewModel.expandedPaths.collectAsState()
    val openTabs by viewModel.openTabs.collectAsState()
    val activeTabPath by viewModel.activeTabPath.collectAsState()
    val fileContent by viewModel.activeFileContent.collectAsState()
    val isLoadingContent by viewModel.isLoadingContent.collectAsState()

    val activeTab = openTabs.find { it.filePath == activeTabPath }
    
    val entityViewModel = remember(viewModel.project.path) { de.luca.dungeon_master_manager.viewmodel.EntityViewModel(viewModel.project.path, viewModel) }
    var showEntityDialog by remember { mutableStateOf(false) }

    val projectEnts by entityViewModel.projectEntities.collectAsState()
    val globalEnts by entityViewModel.globalEntities.collectAsState()
    val entityColors = remember(projectEnts, globalEnts) {
        val map = mutableMapOf<String, String>()
        globalEnts.forEach { if (it.color != null) map[it.name] = it.color }
        projectEnts.forEach { if (it.color != null) map[it.name] = it.color }
        map
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top toolbar
        EditorToolbar(
            projectName = viewModel.project.config.name,
            onBack = onBackToLauncher,
            onManageEntities = { showEntityDialog = true }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Main content: sidebar + editor
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar
            EditorSidebar(
                fileTree = fileTree,
                expandedPaths = expandedPaths,
                activeFilePath = activeTabPath,
                onNodeClick = { viewModel.openFile(it) },
                onCreateFile = { folder, file -> viewModel.createNewFile(folder, file) },
                onRenameFile = { node, newName -> viewModel.renameFile(node, newName) },
                onMoveFile = { node, folder -> viewModel.moveFile(node, folder) },
                onDeleteFile = { node -> viewModel.deleteFile(node) },
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
                modifier = Modifier.width(250.dp).fillMaxHeight(),
            )

            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Editor area (tabs + content)
            Column(modifier = Modifier.fillMaxSize()) {
                // Tab bar
                TabBar(
                    tabs = openTabs,
                    activeTabPath = activeTabPath,
                    onTabSelect = { viewModel.selectTab(it) },
                    onTabClose = { viewModel.closeTab(it) },
                )

                if (openTabs.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                // Content area
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ContentArea(
                        activeTab = activeTab,
                        fileContent = fileContent,
                        isLoading = isLoadingContent,
                        onContentChange = { viewModel.updateActiveFileContent(it) },
                        onOpenEntityPopout = { entityName ->
                            onOpenEntityPopout(EntityPopoutRequest(entityName, viewModel.project.path))
                        },
                        entityColors = entityColors,
                        projectPath = viewModel.project.path
                    )
                }
            }
        }
    }

    if (showEntityDialog) {
        de.luca.dungeon_master_manager.ui.EntityManagerDialog(
            isGlobalOnly = false,
            viewModel = entityViewModel,
            onDismiss = { showEntityDialog = false }
        )
    }
}

@Composable
private fun EditorToolbar(
    projectName: String,
    onBack: () -> Unit,
    onManageEntities: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text("← Back")
            }
            Spacer(Modifier.width(12.dp))
            Text(
                projectName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.weight(1f))
            Button(onClick = onManageEntities) {
                Text("Entities")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EditorSidebar(
    fileTree: de.luca.dungeon_master_manager.data.FileNode?,
    expandedPaths: Set<String>,
    activeFilePath: String?,
    onNodeClick: (de.luca.dungeon_master_manager.data.FileNode) -> Unit,
    onCreateFile: (folderName: String, fileName: String) -> Unit,
    onRenameFile: (de.luca.dungeon_master_manager.data.FileNode, String) -> Unit,
    onMoveFile: (de.luca.dungeon_master_manager.data.FileNode, String) -> Unit,
    onDeleteFile: (de.luca.dungeon_master_manager.data.FileNode) -> Unit,
    onOpenNativeExplorer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showNewFileDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<de.luca.dungeon_master_manager.data.FileNode?>(null) }
    var fileToMove by remember { mutableStateOf<de.luca.dungeon_master_manager.data.FileNode?>(null) }
    var fileToDelete by remember { mutableStateOf<de.luca.dungeon_master_manager.data.FileNode?>(null) }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sidebar header
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Explorer",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                
                Row {
                    TextButton(
                        onClick = onOpenNativeExplorer,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 32.dp, minHeight = 32.dp).padding(end = 4.dp)
                    ) {
                        Text("📁", fontSize = 16.sp)
                    }
                    TextButton(
                        onClick = { showNewFileDialog = true },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
                    ) {
                        Text("+", fontSize = 18.sp)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (fileTree != null) {
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    FileTreeView(
                        rootNode = fileTree,
                        expandedPaths = expandedPaths,
                        activeFilePath = activeFilePath,
                        onNodeClick = onNodeClick,
                        onRenameClick = { fileToRename = it },
                        onMoveClick = { fileToMove = it },
                        onDeleteClick = { fileToDelete = it },
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }

    if (showNewFileDialog && fileTree != null) {
        var fileName by remember { mutableStateOf("") }
        val folders = fileTree.children.filter { it.isDirectory }.map { it.name }
        var selectedFolder by remember { mutableStateOf(folders.firstOrNull() ?: "") }

        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("New File") },
            text = {
                Column {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("File Name (e.g. tavern or tavern.md)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Select Folder:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    val scrollState = rememberScrollState()
                    var expandedDropdown by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ){
                        OutlinedTextField(
                            value = selectedFolder,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                        )
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            content = {
                                folders.forEach { folder ->
                                    DropdownMenuItem(
                                        text = {Text(folder)},
                                        onClick = {
                                            selectedFolder = folder
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            },
                            scrollState = scrollState
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fileName.isNotBlank() && selectedFolder.isNotBlank()) {
                            onCreateFile(selectedFolder, fileName)
                            showNewFileDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (fileToRename != null) {
        var newName by remember { mutableStateOf(fileToRename!!.name) }
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onRenameFile(fileToRename!!, newName)
                            fileToRename = null
                        }
                    }
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) { Text("Cancel") }
            }
        )
    }

    if (fileToMove != null && fileTree != null) {
        val folders = fileTree.children.filter { it.isDirectory }.map { it.name }
        var selectedFolder by remember { mutableStateOf(folders.firstOrNull() ?: "") }
        AlertDialog(
            onDismissRequest = { fileToMove = null },
            title = { Text("Move File") },
            text = {
                Column {
                    Text("Move '${fileToMove!!.name}' to:")
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
                                    onClick = { selectedFolder = folder }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(folder)
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
                    }
                ) { Text("Move") }
            },
            dismissButton = {
                TextButton(onClick = { fileToMove = null }) { Text("Cancel") }
            }
        )
    }

    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete File") },
            text = { Text("Are you sure you want to delete '${fileToDelete!!.name}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFile(fileToDelete!!)
                        fileToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
