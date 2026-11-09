package de.luca.dungeon_master_manager.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object AppSettingsRepository {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val mutex = Mutex()

    private val settingsFile: File
        get() {
            val home = System.getProperty("user.home")
            val baseDir = File(home, ".dungeon-master-manager").also { it.mkdirs() }
            return File(baseDir, "settings.json")
        }

    private val _settings = MutableStateFlow(loadSettingsSync())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettingsSync(): AppSettings {
        return try {
            val file = settingsFile
            if (file.exists()) {
                json.decodeFromString<AppSettings>(file.readText())
            } else {
                AppSettings()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AppSettings()
        }
    }

    const val DEFAULT_EXPLORER_SIDEBAR_WIDTH = 260
    const val MIN_EXPLORER_SIDEBAR_WIDTH = 180
    const val MAX_EXPLORER_SIDEBAR_WIDTH = 650

    const val DEFAULT_EDITOR_FONT_SIZE = 16
    const val MIN_EDITOR_FONT_SIZE = 10
    const val MAX_EDITOR_FONT_SIZE = 32

    const val ENTITY_VIEW_MODE_GRID = "GRID"
    const val ENTITY_VIEW_MODE_LIST = "LIST"

    suspend fun setBatterySaverMode(enabled: Boolean) {
        updateSettings { it.copy(isBatterySaverMode = enabled) }
    }

    suspend fun setEditorFontSize(fontSize: Int) {
        val clamped = fontSize.coerceIn(MIN_EDITOR_FONT_SIZE, MAX_EDITOR_FONT_SIZE)
        updateSettings { it.copy(editorFontSize = clamped) }
    }

    suspend fun setEntityViewMode(mode: String) {
        val validMode = if (mode == ENTITY_VIEW_MODE_LIST) ENTITY_VIEW_MODE_LIST else ENTITY_VIEW_MODE_GRID
        updateSettings { it.copy(entityViewMode = validMode) }
    }

    suspend fun setEditorTimelineVisible(visible: Boolean) {
        updateSettings { it.copy(isEditorTimelineVisible = visible) }
    }

    suspend fun setExplorerSidebarVisible(visible: Boolean) {
        updateSettings { it.copy(isExplorerSidebarVisible = visible) }
    }

    suspend fun setExplorerSidebarWidth(width: Int) {
        val clamped = width.coerceIn(MIN_EXPLORER_SIDEBAR_WIDTH, MAX_EXPLORER_SIDEBAR_WIDTH)
        updateSettings { it.copy(explorerSidebarWidth = clamped) }
    }

    fun getShortcut(actionId: String): String {
        return _settings.value.shortcuts[actionId]
            ?: KeybindingRegistry.ALL.find { it.id == actionId }?.defaultShortcut
            ?: ""
    }

    suspend fun setShortcut(actionId: String, shortcut: String) {
        updateSettings { current ->
            val updated = current.shortcuts.toMutableMap()
            updated[actionId] = shortcut
            current.copy(shortcuts = updated)
        }
    }

    suspend fun reassignShortcut(targetActionId: String, conflictingActionId: String, shortcut: String) {
        updateSettings { current ->
            val updated = current.shortcuts.toMutableMap()
            updated[targetActionId] = shortcut
            updated[conflictingActionId] = ""
            current.copy(shortcuts = updated)
        }
    }

    suspend fun clearShortcut(actionId: String) {
        setShortcut(actionId, "")
    }

    suspend fun resetShortcut(actionId: String) {
        val default = KeybindingRegistry.ALL.find { it.id == actionId }?.defaultShortcut ?: return
        setShortcut(actionId, default)
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _settings.value
            val updated = transform(current)
            _settings.value = updated
            try {
                settingsFile.writeText(json.encodeToString(updated))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
