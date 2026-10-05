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

class EntityViewModel(private val projectPath: String?, private val viewModel: ProjectEditorViewModel?) : ViewModel() {

    private val _projectEntities = MutableStateFlow<List<Entity>>(emptyList())
    val projectEntities: StateFlow<List<Entity>> = _projectEntities

    private val _globalEntities = MutableStateFlow<List<Entity>>(emptyList())
    val globalEntities: StateFlow<List<Entity>> = _globalEntities

    init {
        val globalDb = DatabaseHelper.globalDatabase
        viewModelScope.launch {
            globalDb.entityQueries.selectAll().asFlow().mapToList(Dispatchers.IO).collect {
                _globalEntities.value = it
            }
        }

        if (projectPath != null) {
            val projectDb = DatabaseHelper.getProjectDatabase(projectPath)
            viewModelScope.launch {
                projectDb.entityQueries.selectAll().asFlow().mapToList(Dispatchers.IO).collect {
                    _projectEntities.value = it
                }
            }
        }
    }

    fun addEntity(isGlobal: Boolean, name: String, type: String, color: String?, notesFilePath: String?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val db = if (isGlobal) DatabaseHelper.globalDatabase else DatabaseHelper.getProjectDatabase(projectPath!!)
                db.entityQueries.insertEntity(name, type, color, notesFilePath)
            }
        }
    }

    fun updateEntity(isGlobal: Boolean, id: Long, name: String, type: String, color: String?, notesFilePath: String?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val db = if (isGlobal) DatabaseHelper.globalDatabase else DatabaseHelper.getProjectDatabase(projectPath!!)
                db.entityQueries.updateEntity(name, type, color, notesFilePath, id)
            }
        }
    }

    fun deleteEntity(isGlobal: Boolean, id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val db = if (isGlobal) DatabaseHelper.globalDatabase else DatabaseHelper.getProjectDatabase(projectPath!!)
                db.entityQueries.deleteEntity(id)
            }
        }
    }

    fun copyEntity(entity: Entity, toGlobal: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val destDb = if (toGlobal) DatabaseHelper.globalDatabase else DatabaseHelper.getProjectDatabase(projectPath!!)
                destDb.entityQueries.insertEntity(entity.name, entity.type, entity.color, entity.notesFilePath)
            }
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
