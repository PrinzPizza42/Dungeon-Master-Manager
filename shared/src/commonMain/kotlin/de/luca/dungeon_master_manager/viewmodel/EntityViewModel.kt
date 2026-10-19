package de.luca.dungeon_master_manager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.luca.dungeon_master_manager.data.DatabaseHelper
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
        viewModelScope.launch {
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

    fun addEntity(isGlobal: Boolean, name: String, type: String, color: String?, notesFilePath: String?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val db = if (isGlobal) DatabaseHelper.globalDatabase else DatabaseHelper.getProjectDatabase(projectPath!!)
                    db.entityQueries.insertEntity(UUID.randomUUID().toString(), name, type, color, notesFilePath)
                    println("Entity inserted successfully: $name")
                } catch (e: Exception) {
                    System.err.println("Failed to insert entity: ${e.message}")
                    e.printStackTrace()
                }
            }
            viewModel?.loadEntityColors()
        }
    }

    fun updateEntity(isGlobal: Boolean, id: String, name: String, type: String, color: String?, notesFilePath: String?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val db = if (isGlobal) DatabaseHelper.globalDatabase else DatabaseHelper.getProjectDatabase(projectPath!!)
                db.entityQueries.updateEntity(name, type, color, notesFilePath, id)
            }
            viewModel?.loadEntityColors()
        }
    }

    fun deleteEntity(isGlobal: Boolean, id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val db = if (isGlobal) DatabaseHelper.globalDatabase else DatabaseHelper.getProjectDatabase(projectPath!!)
                db.entityQueries.deleteEntity(id)
                deleteMarkersForEntity(isGlobal, id)
            }
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
            val home = System.getProperty("user.home")
            val baseDir = File(home, ".dungeon-master-manager")
            if (baseDir.exists() && baseDir.isDirectory) {
                baseDir.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.forEach {
                    projectDirs.add(it)
                }
            }
        }

        for (dir in projectDirs) {
            if (!dir.exists()) continue
            val mapFiles = dir.walkTopDown().filter { it.isFile && it.extension == "map" && !it.name.startsWith(".") }
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

    fun copyEntity(entity: Entity, toGlobal: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val destDb = if (toGlobal) DatabaseHelper.globalDatabase else DatabaseHelper.getProjectDatabase(projectPath!!)
                destDb.entityQueries.insertEntity(UUID.randomUUID().toString(), entity.name, entity.type, entity.color, entity.notesFilePath)
            }
            viewModel?.loadEntityColors()
        }
    }

    fun getAvailableMdFiles(isGlobal: Boolean, entityType: String): List<String> {
        val rootPath = if (isGlobal) {
            java.io.File(System.getProperty("user.home"), ".dungeon-master-manager/.global/Notes").absolutePath
        } else {
            projectPath ?: return emptyList()
        }

        val rootFile = java.io.File(rootPath)
        if (!rootFile.exists()) return emptyList()

        val allMdFiles = rootFile.walkTopDown()
            .filter { it.isFile && it.extension == "md" }
            .map { it.absolutePath.removePrefix(rootPath).removePrefix(java.io.File.separator) }
            .toList()

        // Sort by relevance (files in the folder matching the entity type go first)
        val pluralType = "${entityType}s"
        val typeFolder = "$pluralType${java.io.File.separator}"
        return allMdFiles.sortedByDescending { it.startsWith(typeFolder) }
    }

    fun createTemplateFile(isGlobal: Boolean, entityName: String, entityType: String): String {
        val rootPath = if (isGlobal) {
            java.io.File(System.getProperty("user.home"), ".dungeon-master-manager/.global/Notes").absolutePath
        } else {
            projectPath ?: return ""
        }

        val pluralType = "${entityType}s"
        val typeFolder = java.io.File(rootPath, pluralType)
        if (!typeFolder.exists()) {
            typeFolder.mkdirs()
        }

        // Sanitize name for file
        val safeName = entityName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val file = java.io.File(typeFolder, "$safeName.md")

        if (!file.exists()) {
            val template = """
                # $entityName
                
                ## Description
                
                ## Details
                
            """.trimIndent()
            file.writeText(template)
        }

        viewModel?.loadFileTree()

        return "$pluralType${java.io.File.separator}${file.name}"
    }
}
