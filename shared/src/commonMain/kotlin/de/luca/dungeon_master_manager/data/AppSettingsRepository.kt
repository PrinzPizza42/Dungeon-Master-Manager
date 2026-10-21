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

    suspend fun setBatterySaverMode(enabled: Boolean) {
        updateSettings { it.copy(isBatterySaverMode = enabled) }
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
