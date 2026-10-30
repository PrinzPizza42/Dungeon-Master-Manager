package de.luca.dungeon_master_manager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.luca.dungeon_master_manager.data.DatabaseHelper
import de.luca.dungeon_master_manager.data.ProjectRepository
import de.luca.dungeonmastermanager.database.Entity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList

import de.luca.dungeon_master_manager.data.MapData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class EntityViewModel(private val projectPath: String?, private val viewModel: ProjectEditorViewModel?) : ViewModel() {

    private val _projectEntities = MutableStateFlow<List<Entity>>(emptyList())
    val projectEntities: StateFlow<List<Entity>> = _projectEntities

    private val _globalEntities = MutableStateFlow<List<Entity>>(emptyList())
    val globalEntities: StateFlow<List<Entity>> = _globalEntities

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded

    init {
        val globalDb = DatabaseHelper.globalDatabase
        viewModelScope.launch(Dispatchers.IO) {
            var globalLoaded = false
            var projectLoaded = projectPath == null

            launch {
                globalDb.entityQueries.selectAll().asFlow().mapToList(Dispatchers.IO).collect {
                    _globalEntities.value = it
                    globalLoaded = true
                    if (projectLoaded) _isLoaded.value = true
                }
            }

            if (projectPath != null) {
                val projectDb = DatabaseHelper.getProjectDatabase(projectPath)
                launch {
                    projectDb.entityQueries.selectAll().asFlow().mapToList(Dispatchers.IO).collect {
                        _projectEntities.value = it
                        projectLoaded = true
                        if (globalLoaded) _isLoaded.value = true
                    }
                }
            }
        }
    }

    fun addEntity(isGlobal: Boolean, name: String, type: String, color: String?, notesFilePath: String?, imageFilePath: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = if (isGlobal) DatabaseHelper.globalDatabase else DatabaseHelper.getProjectDatabase(projectPath!!)
                db.entityQueries.insertEntity(UUID.randomUUID().toString(), name, type, color, notesFilePath, imageFilePath)
                println("Entity inserted successfully: $name")
            } catch (e: Exception) {
                System.err.println("Failed to insert entity: ${e.message}")
                e.printStackTrace()
            }
            viewModel?.loadEntityColors()
        }
    }

    fun updateEntity(isGlobal: Boolean, id: String, name: String, type: String, color: String?, notesFilePath: String?, imageFilePath: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = if (isGlobal) DatabaseHelper.globalDatabase else DatabaseHelper.getProjectDatabase(projectPath!!)
            db.entityQueries.updateEntity(name, type, color, notesFilePath, imageFilePath, id)
            viewModel?.loadEntityColors()
        }
    }

    fun deleteEntity(isGlobal: Boolean, id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = if (isGlobal) DatabaseHelper.globalDatabase else DatabaseHelper.getProjectDatabase(projectPath!!)
            db.entityQueries.deleteEntity(id)
            deleteMarkersForEntity(isGlobal, id)
            viewModel?.onEntityDeleted(id)
        }
    }

    private fun deleteMarkersForEntity(isGlobal: Boolean, entityId: String) {
        val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
        val projectDirs = mutableSetOf<File>()

        if (projectPath != null) {
            projectDirs.add(File(projectPath))
        }

        if (isGlobal) {
            val baseDir = ProjectRepository.baseDir
            if (baseDir.exists() && baseDir.isDirectory) {
                baseDir.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.forEach {
                    projectDirs.add(it)
                }
            }
        }

        for (dir in projectDirs) {
            if (!dir.exists()) continue
            val mapFiles = dir.walkTopDown()
                .onEnter { it == dir || !it.name.startsWith(".") }
                .filter { it.isFile && it.extension == "map" && !it.name.startsWith(".") }
            for (mapFile in mapFiles) {
                try {
                    val text = mapFile.readText()
                    if (text.isNotBlank()) {
                        val mapData = json.decodeFromString<MapData>(text)
                        val remainingMarkers = mapData.markers.filterNot { it.entityId == entityId }
                        if (remainingMarkers.size != mapData.markers.size) {
                            val updatedMapData = mapData.copy(markers = remainingMarkers)
                            mapFile.writeText(json.encodeToString(updatedMapData))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun resolveNonConflictingFile(parentDir: File, baseName: String, extension: String): File {
        var candidate = File(parentDir, "$baseName.$extension")
        var counter = 1
        while (candidate.exists()) {
            candidate = File(parentDir, "${baseName}_$counter.$extension")
            counter++
        }
        return candidate
    }

    fun resolveEntityImageFile(imagePath: String?, isGlobal: Boolean): File? {
        if (imagePath.isNullOrBlank()) return null
        val baseDir = if (isGlobal) {
            ProjectRepository.globalImagesDir
        } else {
            if (projectPath.isNullOrBlank()) null else File(projectPath)
        } ?: return null
        val file = File(baseDir, imagePath)
        return if (file.exists() && file.isFile) file else null
    }

    fun copyEntity(entity: Entity, toGlobal: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            var targetNotesPath = entity.notesFilePath
            if (!entity.notesFilePath.isNullOrBlank()) {
                val sourceRootDir = if (toGlobal) {
                    if (projectPath != null) File(projectPath) else null
                } else {
                    ProjectRepository.globalNotesDir
                }

                val targetRootDir = if (toGlobal) {
                    ProjectRepository.globalNotesDir
                } else {
                    if (projectPath != null) File(projectPath) else null
                }

                if (sourceRootDir != null && targetRootDir != null) {
                    val sourceFile = File(sourceRootDir, entity.notesFilePath)
                    if (sourceFile.exists() && sourceFile.isFile) {
                        val relativeParent = File(entity.notesFilePath).parentFile?.path ?: "${entity.type}s"
                        val targetDir = File(targetRootDir, relativeParent).also { it.mkdirs() }
                        val baseName = sourceFile.nameWithoutExtension
                        val targetFile = resolveNonConflictingFile(targetDir, baseName, sourceFile.extension.ifBlank { "md" })
                        sourceFile.copyTo(targetFile, overwrite = false)
                        targetNotesPath = if (relativeParent.isBlank() || relativeParent == ".") {
                            targetFile.name
                        } else {
                            "$relativeParent${File.separator}${targetFile.name}"
                        }
                    }
                }
            }

            var targetImagePath = entity.imageFilePath
            if (!entity.imageFilePath.isNullOrBlank()) {
                val sourceFile = resolveEntityImageFile(entity.imageFilePath, isGlobal = !toGlobal)
                if (sourceFile != null && sourceFile.exists()) {
                    val targetDir = if (toGlobal) {
                        ProjectRepository.globalImagesDir.also { it.mkdirs() }
                    } else {
                        if (projectPath != null) File(projectPath, "Images${File.separator}Entities").also { it.mkdirs() } else null
                    }
                    if (targetDir != null) {
                        val baseName = sourceFile.nameWithoutExtension
                        val ext = sourceFile.extension.ifBlank { "png" }
                        val targetFile = resolveNonConflictingFile(targetDir, baseName, ext)
                        sourceFile.copyTo(targetFile, overwrite = false)
                        targetImagePath = if (toGlobal) {
                            targetFile.name
                        } else {
                            "Images${File.separator}Entities${File.separator}${targetFile.name}"
                        }
                    }
                }
            }

            val destDb = if (toGlobal) DatabaseHelper.globalDatabase else DatabaseHelper.getProjectDatabase(projectPath!!)
            destDb.entityQueries.insertEntity(UUID.randomUUID().toString(), entity.name, entity.type, entity.color, targetNotesPath, targetImagePath)

            if (!toGlobal) {
                viewModel?.loadFileTree()
            }
            viewModel?.loadEntityColors()
        }
    }

    fun importExternalMdFile(sourceFile: File, isGlobal: Boolean, entityType: String): String {
        val rootPath = if (isGlobal) {
            ProjectRepository.globalNotesDir
        } else {
            if (projectPath != null) File(projectPath) else return ""
        }

        val pluralType = "${entityType}s"
        val typeFolder = File(rootPath, pluralType).also { it.mkdirs() }

        val baseName = sourceFile.nameWithoutExtension.ifBlank { "imported_note" }
        val ext = sourceFile.extension.ifBlank { "md" }
        val targetFile = resolveNonConflictingFile(typeFolder, baseName, ext)

        sourceFile.copyTo(targetFile, overwrite = false)

        if (!isGlobal) {
            viewModel?.loadFileTree()
        }

        return "$pluralType${File.separator}${targetFile.name}"
    }

    fun importEntityImage(sourceFile: File, isGlobal: Boolean, entityName: String): String {
        val targetDir = if (isGlobal) {
            ProjectRepository.globalImagesDir.also { it.mkdirs() }
        } else {
            if (projectPath != null) {
                File(projectPath, "Images${File.separator}Entities").also { it.mkdirs() }
            } else return ""
        }

        val safeName = entityName.replace(Regex("[^a-zA-Z0-9.-]"), "_").ifBlank { "entity_image" }
        val ext = sourceFile.extension.ifBlank { "png" }
        val targetFile = resolveNonConflictingFile(targetDir, safeName, ext)

        sourceFile.copyTo(targetFile, overwrite = false)

        if (!isGlobal) {
            viewModel?.loadFileTree()
        }

        return if (isGlobal) {
            targetFile.name
        } else {
            "Images${File.separator}Entities${File.separator}${targetFile.name}"
        }
    }

    fun getAvailableMdFiles(isGlobal: Boolean, entityType: String): List<String> {
        val rootPath = if (isGlobal) {
            ProjectRepository.globalNotesDir
        } else {
            if (projectPath != null) File(projectPath) else return emptyList()
        }

        if (!rootPath.exists()) return emptyList()

        val rootAbsPath = rootPath.absolutePath
        val allMdFiles = rootPath.walkTopDown()
            .onEnter { it == rootPath || !it.name.startsWith(".") }
            .filter { it.isFile && it.extension.equals("md", ignoreCase = true) }
            .map { it.absolutePath.removePrefix(rootAbsPath).removePrefix(File.separator) }
            .toList()

        // Sort by relevance (files in the folder matching the entity type go first)
        val pluralType = "${entityType}s"
        val typeFolder = "$pluralType${File.separator}"
        val typeFolderSlash = "$pluralType/"
        return allMdFiles.sortedByDescending { it.startsWith(typeFolder, ignoreCase = true) || it.startsWith(typeFolderSlash, ignoreCase = true) }
    }

    fun createTemplateFile(isGlobal: Boolean, entityName: String, entityType: String): String {
        val rootPath = if (isGlobal) {
            ProjectRepository.globalNotesDir
        } else {
            if (projectPath != null) File(projectPath) else return ""
        }

        val pluralType = "${entityType}s"
        val typeFolder = File(rootPath, pluralType).also { it.mkdirs() }

        // Sanitize name for file
        val safeName = entityName.replace(Regex("[^a-zA-Z0-9.-]"), "_").ifBlank { "unnamed" }
        val file = resolveNonConflictingFile(typeFolder, safeName, "md")

        val template = """
            # $entityName
            
            ## Description
            
            ## Details
            
        """.trimIndent()
        file.writeText(template)

        if (!isGlobal) {
            viewModel?.loadFileTree()
        }

        return "$pluralType${File.separator}${file.name}"
    }
}
