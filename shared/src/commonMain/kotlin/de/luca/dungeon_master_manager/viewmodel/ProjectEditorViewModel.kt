package de.luca.dungeon_master_manager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.luca.dungeon_master_manager.data.EditorTab
import de.luca.dungeon_master_manager.data.FileNode
import de.luca.dungeon_master_manager.data.FileService
import de.luca.dungeon_master_manager.data.ProjectInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProjectEditorViewModel(
    val project: ProjectInfo,
) : ViewModel() {
    private val fileService = FileService()

    // File tree
    private val _fileTree = MutableStateFlow<FileNode?>(null)
    val fileTree: StateFlow<FileNode?> = _fileTree

    private val _expandedPaths = MutableStateFlow<Set<String>>(emptySet())
    val expandedPaths: StateFlow<Set<String>> = _expandedPaths

    // Tabs
    private val _openTabs = MutableStateFlow<List<EditorTab>>(emptyList())
    val openTabs: StateFlow<List<EditorTab>> = _openTabs

    private val _activeTabPath = MutableStateFlow<String?>(null)
    val activeTabPath: StateFlow<String?> = _activeTabPath

    // File content for active tab
    private val _activeFileContent = MutableStateFlow<String?>(null)
    val activeFileContent: StateFlow<String?> = _activeFileContent

    private val _isLoadingContent = MutableStateFlow(false)
    val isLoadingContent: StateFlow<Boolean> = _isLoadingContent

    private val _entityColors = MutableStateFlow<Map<String, String>>(emptyMap())
    val entityColors: StateFlow<Map<String, String>> = _entityColors

    init {
        loadFileTree()
        loadEntityColors()
    }

    fun loadEntityColors() {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val db = de.luca.dungeon_master_manager.data.DatabaseHelper.getProjectDatabase(project.path)
                val globalDb = de.luca.dungeon_master_manager.data.DatabaseHelper.globalDatabase
                
                val projectEntities = db.entityQueries.selectAll().executeAsList()
                val globalEntities = globalDb.entityQueries.selectAll().executeAsList()
                
                val colorMap = mutableMapOf<String, String>()
                // Global entities lower priority than project entities (project overwrites global)
                globalEntities.forEach { entity ->
                    if (entity.color != null) colorMap[entity.name] = entity.color
                }
                projectEntities.forEach { entity ->
                    if (entity.color != null) colorMap[entity.name] = entity.color
                }
                
                _entityColors.value = colorMap
            }
        }
    }

    fun loadFileTree() {
        viewModelScope.launch {
            val tree = fileService.buildFileTree(project.path)
            _fileTree.value = tree
            // Auto-expand the root
            _expandedPaths.value = _expandedPaths.value + tree.path
        }
    }

    fun toggleExpanded(path: String) {
        _expandedPaths.value = if (path in _expandedPaths.value) {
            _expandedPaths.value - path
        } else {
            _expandedPaths.value + path
        }
    }

    /** Open a file in a tab (or switch to it if already open). */
    fun openFile(node: FileNode) {
        if (node.isDirectory) {
            toggleExpanded(node.path)
            return
        }

        val actualPath = if (node.isVirtualHeading) {
            node.path.substringBefore("::heading::")
        } else {
            node.path
        }
        val actualName = if (node.isVirtualHeading) actualPath.substringAfterLast(java.io.File.separator) else node.name
        val actualExtension = if (node.isVirtualHeading) "md" else node.extension

        // Add tab if not already open
        val existingTab = _openTabs.value.find { it.filePath == actualPath }
        if (existingTab == null) {
            _openTabs.value = _openTabs.value + EditorTab(
                filePath = actualPath,
                fileName = actualName,
                extension = actualExtension,
            )
        }

        // Switch to this tab
        selectTab(actualPath)
    }

    /** Switch to an already-open tab. */
    fun selectTab(filePath: String) {
        _activeTabPath.value = filePath
        loadFileContent(filePath)
    }

    /** Close a tab. If it was active, switch to an adjacent tab. */
    fun closeTab(filePath: String) {
        val tabs = _openTabs.value
        val index = tabs.indexOfFirst { it.filePath == filePath }
        if (index == -1) return

        val newTabs = tabs.filterIndexed { i, _ -> i != index }
        _openTabs.value = newTabs

        // If active tab is closed, pick a new one
        if (_activeTabPath.value == filePath) {
            val newActive = when {
                newTabs.isEmpty() -> null
                index < newTabs.size -> newTabs[index].filePath
                else -> newTabs.last().filePath
            }
            _activeTabPath.value = newActive
            if (newActive != null) {
                loadFileContent(newActive)
            } else {
                _activeFileContent.value = null
            }
        }
    }

    private var saveJob: kotlinx.coroutines.Job? = null

    /** Update file content and auto-save after debounce */
    fun updateActiveFileContent(newContent: String) {
        _activeFileContent.value = newContent
        val activePath = _activeTabPath.value ?: return

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500) // Debounce 500ms
            try {
                fileService.writeFile(activePath, newContent)
                // If its a story file, reload tree to update outline
                if (activePath.contains("Story")) {
                    loadFileTree()
                }
            } catch (e: Exception) {
                // Ignore for now
            }
        }
    }

    private fun loadFileContent(filePath: String) {
        viewModelScope.launch {
            _isLoadingContent.value = true
            try {
                _activeFileContent.value = fileService.readFile(filePath)
            } catch (e: Exception) {
                _activeFileContent.value = "Error reading file: ${e.message}"
            } finally {
                _isLoadingContent.value = false
            }
        }
    }

    /** Create a new file in the specified root folder and open it */
    fun createNewFile(folderName: String, fileName: String) {
        viewModelScope.launch {
            try {
                val safeName = if (fileName.contains(".")) fileName else "$fileName.md"
                val folderPath = "${project.path}${java.io.File.separator}$folderName"
                val newFile = java.io.File(folderPath, safeName)
                
                if (!newFile.exists()) {
                    newFile.parentFile?.mkdirs()
                    newFile.createNewFile()
                    
                    if (safeName.endsWith(".md")) {
                        fileService.writeFile(newFile.absolutePath, "# ${safeName.removeSuffix(".md")}\n\n")
                    } else if (safeName.endsWith(".map")) {
                        fileService.writeFile(newFile.absolutePath, "{\"imagePath\": \"\", \"markers\": []}")
                    }
                    
                    loadFileTree()
                    
                    // Switch to the newly created file tab
                    val newNode = de.luca.dungeon_master_manager.data.FileNode(
                        name = newFile.name,
                        path = newFile.absolutePath,
                        isDirectory = false
                    )
                    openFile(newNode)
                }
            } catch (e: Exception) {
                // Ignore for now
            }
        }
    }

    fun deleteFile(node: de.luca.dungeon_master_manager.data.FileNode) {
        viewModelScope.launch {
            try {
                val file = java.io.File(node.path)
                if (file.exists()) {
                    file.deleteRecursively()
                    loadFileTree()
                    closeTab(node.path)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun renameFile(node: de.luca.dungeon_master_manager.data.FileNode, newName: String) {
        viewModelScope.launch {
            try {
                val oldFile = java.io.File(node.path)
                val newFile = java.io.File(oldFile.parentFile, newName)
                if (oldFile.renameTo(newFile)) {
                    loadFileTree()
                    closeTab(node.path)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun moveFile(node: de.luca.dungeon_master_manager.data.FileNode, newParentFolderName: String) {
        viewModelScope.launch {
            try {
                val oldFile = java.io.File(node.path)
                val newParent = java.io.File("${project.path}${java.io.File.separator}$newParentFolderName")
                if (!newParent.exists()) newParent.mkdirs()
                
                val newFile = java.io.File(newParent, oldFile.name)
                if (oldFile.renameTo(newFile)) {
                    loadFileTree()
                    closeTab(node.path)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
