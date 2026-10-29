package de.luca.dungeon_master_manager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.luca.dungeon_master_manager.data.EditorTab
import de.luca.dungeon_master_manager.data.FileExtensions
import de.luca.dungeon_master_manager.data.FileNode
import de.luca.dungeon_master_manager.data.FileService
import de.luca.dungeon_master_manager.data.MapData
import de.luca.dungeon_master_manager.data.ProjectInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

enum class ConflictAction { OVERWRITE, RENAME, SKIP }
data class ConflictDecision(val action: ConflictAction, val applyToAll: Boolean)

data class FileConflictPrompt(
    val fileName: String,
    val targetFolderDisplayName: String,
    val remainingFilesCount: Int,
    val onDecision: (ConflictDecision) -> Unit,
    val onCancelBatch: () -> Unit,
)

data class ExplorerDropNotice(
    val message: String,
    val isWarning: Boolean = true,
)

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

    data class PoppedOutTab(val tab: EditorTab, val focusTrigger: Int = 0)
    private val _poppedOutTabs = MutableStateFlow<List<PoppedOutTab>>(emptyList())
    val poppedOutTabs: StateFlow<List<PoppedOutTab>> = _poppedOutTabs

    private val _activeTabPath = MutableStateFlow<String?>(null)
    val activeTabPath: StateFlow<String?> = _activeTabPath

    // File content for active tab
    private val _activeFileContent = MutableStateFlow<String?>(null)
    val activeFileContent: StateFlow<String?> = _activeFileContent

    private val _isLoadingContent = MutableStateFlow(false)
    val isLoadingContent: StateFlow<Boolean> = _isLoadingContent

    private val _entityColors = MutableStateFlow<Map<String, String>>(emptyMap())
    val entityColors: StateFlow<Map<String, String>> = _entityColors

    // Conflict & Drop notices
    private val _pendingConflict = MutableStateFlow<FileConflictPrompt?>(null)
    val pendingConflict: StateFlow<FileConflictPrompt?> = _pendingConflict

    private val _dropNotice = MutableStateFlow<ExplorerDropNotice?>(null)
    val dropNotice: StateFlow<ExplorerDropNotice?> = _dropNotice

    private var noticeDismissJob: Job? = null

    init {
        loadFileTree()
        loadEntityColors()
    }

    fun showDropNotice(message: String, isWarning: Boolean = true) {
        noticeDismissJob?.cancel()
        _dropNotice.value = ExplorerDropNotice(message, isWarning)
        noticeDismissJob = viewModelScope.launch {
            delay(4500)
            _dropNotice.value = null
        }
    }

    fun dismissDropNotice() {
        noticeDismissJob?.cancel()
        _dropNotice.value = null
    }

    fun expandTargetDir(targetDirPath: String) {
        val pathsToAdd = mutableSetOf<String>()
        var current: File? = File(targetDirPath)
        val rootCanonical = try { File(project.path).canonicalFile } catch (_: Exception) { File(project.path) }
        while (current != null) {
            pathsToAdd.add(current.absolutePath)
            if (try { current.canonicalFile == rootCanonical } catch (_: Exception) { false }) break
            current = current.parentFile
        }
        _expandedPaths.value = _expandedPaths.value + pathsToAdd
    }

    fun loadEntityColors() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
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

            // Clean up any open tabs or popped-out tabs whose files no longer exist on disk
            val missingTabs = _openTabs.value.filterNot { File(it.filePath).exists() }
            for (missing in missingTabs) {
                closeTab(missing.filePath)
            }
            val missingPopped = _poppedOutTabs.value.filterNot { File(it.tab.filePath).exists() }
            for (missing in missingPopped) {
                closePoppedOutTab(missing.tab.filePath)
            }
        }
    }

    fun onDragTransferCompleted(filePath: String) {
        viewModelScope.launch {
            delay(150)
            loadFileTree()
            if (!File(filePath).exists()) {
                closeTab(filePath)
                closePoppedOutTab(filePath)
            }
        }
    }

    fun toggleExpanded(path: String) {
        _expandedPaths.value = if (path in _expandedPaths.value) {
            _expandedPaths.value - path
        } else {
            _expandedPaths.value + path
        }
    }

    fun popOutTab(filePath: String) {
        val tab = _openTabs.value.find { it.filePath == filePath } ?: return
        closeTab(filePath)
        val poppedIndex = _poppedOutTabs.value.indexOfFirst { it.tab.filePath == filePath }
        if (poppedIndex != -1) {
            val old = _poppedOutTabs.value[poppedIndex]
            val newList = _poppedOutTabs.value.toMutableList()
            newList[poppedIndex] = old.copy(focusTrigger = old.focusTrigger + 1)
            _poppedOutTabs.value = newList
            return
        }
        _poppedOutTabs.value = _poppedOutTabs.value + PoppedOutTab(tab)
    }

    fun openAsWindow(node: FileNode) {
        if (node.isDirectory) return

        val actualPath = if (node.isVirtualHeading) {
            node.path.substringBefore("::heading::")
        } else {
            node.path
        }
        val actualName = if (node.isVirtualHeading) actualPath.substringAfterLast(File.separator) else node.name
        val actualExtension = if (node.isVirtualHeading) "md" else node.extension

        // Check if already popped out, focus it if so
        val poppedIndex = _poppedOutTabs.value.indexOfFirst { it.tab.filePath == actualPath }
        if (poppedIndex != -1) {
            val old = _poppedOutTabs.value[poppedIndex]
            val newList = _poppedOutTabs.value.toMutableList()
            newList[poppedIndex] = old.copy(focusTrigger = old.focusTrigger + 1)
            _poppedOutTabs.value = newList
            return
        }

        // Close it from main tabs if it's there
        closeTab(actualPath)

        val tab = EditorTab(
            filePath = actualPath,
            fileName = actualName,
            extension = actualExtension,
        )
        _poppedOutTabs.value = _poppedOutTabs.value + PoppedOutTab(tab)
    }

    fun closePoppedOutTab(filePath: String) {
        _poppedOutTabs.value = _poppedOutTabs.value.filter { it.tab.filePath != filePath }
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
        val actualName = if (node.isVirtualHeading) actualPath.substringAfterLast(File.separator) else node.name
        val actualExtension = if (node.isVirtualHeading) "md" else node.extension

        // Check if it's already popped out
        val poppedIndex = _poppedOutTabs.value.indexOfFirst { it.tab.filePath == actualPath }
        if (poppedIndex != -1) {
            val old = _poppedOutTabs.value[poppedIndex]
            val newList = _poppedOutTabs.value.toMutableList()
            newList[poppedIndex] = old.copy(focusTrigger = old.focusTrigger + 1)
            _poppedOutTabs.value = newList
            return
        }

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

    private var saveJob: Job? = null

    /** Update file content and auto-save after debounce */
    fun updateActiveFileContent(newContent: String) {
        val activePath = _activeTabPath.value ?: return
        val ext = activePath.substringAfterLast('.', "").lowercase()
        if (!FileExtensions.isEditableText(ext)) return

        _activeFileContent.value = newContent

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500) // Debounce 500ms
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

    fun onEntityDeleted(entityId: String) {
        loadEntityColors()
        val activePath = _activeTabPath.value
        if (activePath != null && activePath.endsWith(".map")) {
            val currentContent = _activeFileContent.value
            if (!currentContent.isNullOrBlank()) {
                try {
                    val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
                    val mapData = json.decodeFromString<MapData>(currentContent)
                    val remainingMarkers = mapData.markers.filterNot { it.entityId == entityId }
                    if (remainingMarkers.size != mapData.markers.size) {
                        saveJob?.cancel()
                        val updatedJson = json.encodeToString(mapData.copy(markers = remainingMarkers))
                        _activeFileContent.value = updatedJson
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                fileService.writeFile(activePath, updatedJson)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun loadFileContent(filePath: String) {
        val ext = filePath.substringAfterLast('.', "").lowercase()
        if (!FileExtensions.isEditableText(ext)) {
            // Binary or non-editable files are not read into text memory
            _activeFileContent.value = ""
            _isLoadingContent.value = false
            return
        }

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
                val folderPath = "${project.path}${File.separator}$folderName"
                val newFile = File(folderPath, safeName)
                
                if (!newFile.exists()) {
                    newFile.parentFile?.mkdirs()
                    newFile.createNewFile()
                    
                    if (safeName.endsWith(".md")) {
                        fileService.writeFile(newFile.absolutePath, "# ${safeName.removeSuffix(".md")}\n\n")
                    } else if (safeName.endsWith(".map")) {
                        fileService.writeFile(newFile.absolutePath, "{\"imagePath\": \"\", \"markers\": []}")
                    } else {
                        fileService.writeFile(newFile.absolutePath, "")
                    }
                    
                    loadFileTree()
                    
                    // Switch to the newly created file tab
                    val newNode = FileNode(
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

    fun deleteFile(node: FileNode) {
        viewModelScope.launch {
            try {
                val file = File(node.path)
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

    fun renameFile(node: FileNode, newName: String) {
        viewModelScope.launch {
            try {
                val oldFile = File(node.path)
                val newFile = File(oldFile.parentFile, newName)
                if (oldFile.renameTo(newFile)) {
                    loadFileTree()
                    closeTab(node.path)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun moveFile(node: FileNode, newParentFolderName: String) {
        val targetDirPath = if (newParentFolderName.isBlank() || newParentFolderName == "/") {
            project.path
        } else {
            "${project.path}${File.separator}$newParentFolderName"
        }
        moveInternalFile(node.path, targetDirPath)
    }

    fun moveInternalFile(sourceFilePath: String, targetDirPath: String) {
        viewModelScope.launch {
            val sourceFile = File(sourceFilePath)
            if (!sourceFile.exists() || sourceFile.isDirectory) return@launch

            val targetDir = File(targetDirPath)
            val isSameDir = try {
                sourceFile.canonicalFile.parentFile == targetDir.canonicalFile
            } catch (_: Exception) {
                false
            }
            if (isSameDir) return@launch // No-op

            // Flush any unsaved changes if this was the active tab
            if (_activeTabPath.value == sourceFilePath) {
                saveJob?.cancel()
                _activeFileContent.value?.let { content ->
                    val ext = sourceFilePath.substringAfterLast('.', "").lowercase()
                    if (FileExtensions.isEditableText(ext)) {
                        try {
                            fileService.writeFile(sourceFilePath, content)
                        } catch (_: Exception) {}
                    }
                }
            }

            var destination = File(targetDir, sourceFile.name)
            val targetFolderDisplayName = if (try { targetDir.canonicalFile == File(project.path).canonicalFile } catch(_: Exception) { false }) {
                "Projekt-Root"
            } else {
                targetDir.name
            }

            if (destination.exists()) {
                val deferred = CompletableDeferred<ConflictDecision?>()
                _pendingConflict.value = FileConflictPrompt(
                    fileName = sourceFile.name,
                    targetFolderDisplayName = targetFolderDisplayName,
                    remainingFilesCount = 0,
                    onDecision = { decision ->
                        _pendingConflict.value = null
                        deferred.complete(decision)
                    },
                    onCancelBatch = {
                        _pendingConflict.value = null
                        deferred.complete(null)
                    }
                )
                val result = deferred.await() ?: return@launch
                when (result.action) {
                    ConflictAction.SKIP -> return@launch
                    ConflictAction.RENAME -> {
                        destination = fileService.generateUniqueTargetFile(targetDir, sourceFile.name)
                    }
                    ConflictAction.OVERWRITE -> {
                        // Will overwrite
                    }
                }
            }

            try {
                fileService.moveFileOnDisk(sourceFile, destination, overwrite = true)
                
                val oldPath = sourceFile.absolutePath
                val newPath = destination.absolutePath

                // If overwriting an existing file that had an open tab (different from source), close that old tab
                if (oldPath != newPath) {
                    _openTabs.value = _openTabs.value.filterNot { it.filePath == newPath && it.filePath != oldPath }
                }

                // Update open tabs
                _openTabs.value = _openTabs.value.map { tab ->
                    if (tab.filePath == oldPath) {
                        tab.copy(filePath = newPath, fileName = destination.name, extension = destination.extension.lowercase())
                    } else tab
                }

                // Update active tab path
                if (_activeTabPath.value == oldPath) {
                    _activeTabPath.value = newPath
                }

                // Update popped out tabs
                _poppedOutTabs.value = _poppedOutTabs.value.map { popped ->
                    if (popped.tab.filePath == oldPath) {
                        popped.copy(tab = popped.tab.copy(filePath = newPath, fileName = destination.name, extension = destination.extension.lowercase()))
                    } else popped
                }

                loadFileTree()
                expandTargetDir(targetDirPath)
            } catch (e: Exception) {
                e.printStackTrace()
                showDropNotice("Fehler beim Verschieben: ${e.message}")
            }
        }
    }

    fun importExternalFiles(rawFiles: List<File>, targetDirPath: String) {
        viewModelScope.launch {
            val targetDir = File(targetDirPath)
            val validation = fileService.validateExternalDrop(rawFiles, targetDir)

            if (validation.validFiles.isEmpty()) {
                if (validation.skippedDirectories > 0 && rawFiles.all { it.isDirectory }) {
                    showDropNotice("Ordner können nicht per Drag & Drop importiert werden.")
                } else if (validation.skippedDotFiles > 0 && rawFiles.all { it.name.startsWith(".") }) {
                    showDropNotice("Versteckte Systemdateien können nicht importiert werden.")
                } else if (validation.skippedSameDirectory > 0) {
                    // Same directory no-op
                } else {
                    showDropNotice("Keine gültigen Dateien zum Importieren gefunden.")
                }
                return@launch
            }

            val targetFolderDisplayName = if (try { targetDir.canonicalFile == File(project.path).canonicalFile } catch(_: Exception) { false }) {
                "Projekt-Root"
            } else {
                targetDir.name
            }

            var applyToAllDecision: ConflictAction? = null
            val successfullyImported = mutableListOf<File>()

            for ((index, sourceFile) in validation.validFiles.withIndex()) {
                var destination = File(targetDir, sourceFile.name)

                if (destination.exists()) {
                    val actionToTake = if (applyToAllDecision != null) {
                        applyToAllDecision
                    } else {
                        val deferred = CompletableDeferred<ConflictDecision?>()
                        val remainingCount = validation.validFiles.size - 1 - index
                        _pendingConflict.value = FileConflictPrompt(
                            fileName = sourceFile.name,
                            targetFolderDisplayName = targetFolderDisplayName,
                            remainingFilesCount = remainingCount,
                            onDecision = { decision ->
                                _pendingConflict.value = null
                                deferred.complete(decision)
                            },
                            onCancelBatch = {
                                _pendingConflict.value = null
                                deferred.complete(null)
                            }
                        )
                        val result = deferred.await()
                        if (result == null) {
                            // User cancelled remainder of batch
                            break
                        }
                        if (result.applyToAll) {
                            applyToAllDecision = result.action
                        }
                        result.action
                    }

                    when (actionToTake) {
                        ConflictAction.SKIP -> {
                            continue
                        }
                        ConflictAction.RENAME -> {
                            destination = fileService.generateUniqueTargetFile(targetDir, sourceFile.name)
                        }
                        ConflictAction.OVERWRITE -> {
                            // Will overwrite
                        }
                    }
                }

                try {
                    fileService.copyFile(sourceFile, destination, overwrite = true)
                    successfullyImported.add(destination)

                    // If overwritten file was active tab, reload content
                    if (_activeTabPath.value == destination.absolutePath) {
                        saveJob?.cancel()
                        loadFileContent(destination.absolutePath)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (successfullyImported.isNotEmpty()) {
                loadFileTree()
                expandTargetDir(targetDirPath)

                if (validation.skippedDirectories > 0) {
                    showDropNotice("${successfullyImported.size} Datei(en) importiert – ${validation.skippedDirectories} Ordner übersprungen.", isWarning = true)
                } else if (validation.skippedDotFiles > 0) {
                    showDropNotice("${successfullyImported.size} Datei(en) importiert – ${validation.skippedDotFiles} Systemdatei(en) übersprungen.", isWarning = true)
                }

                // If exactly 1 file was imported, open it
                if (successfullyImported.size == 1) {
                    val single = successfullyImported.first()
                    openFile(FileNode(name = single.name, path = single.absolutePath, isDirectory = false))
                }
            } else if (validation.skippedDirectories > 0) {
                showDropNotice("Ordner können nicht per Drag & Drop importiert werden.")
            }
        }
    }
}
