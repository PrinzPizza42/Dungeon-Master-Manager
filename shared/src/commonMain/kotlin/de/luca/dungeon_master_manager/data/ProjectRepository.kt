package de.luca.dungeon_master_manager.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ProjectRepository {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val baseDir: File
        get() {
            val home = System.getProperty("user.home")
            return File(home, ".dungeon-master-manager").also { it.mkdirs() }
        }

    /** Ensure global directories exist. */
    private fun ensureGlobalDirs() {
        File(baseDir, ".global").mkdirs()
        File(baseDir, ".global/marker-icons").mkdirs()
    }

    /** Default subfolders created for every new project. */
    private val defaultFolders = listOf(
        "Story", "Players", "Places", "NPCs", "Items", "Monsters", "Maps", "Notes"
    )

    /** Scan for all projects that contain a valid `.project` file. */
    suspend fun listProjects(): List<ProjectInfo> = withContext(Dispatchers.IO) {
        ensureGlobalDirs()
        baseDir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.mapNotNull { dir ->
                val configFile = File(dir, ".project")
                if (configFile.exists()) {
                    try {
                        val config = json.decodeFromString<ProjectConfig>(configFile.readText())
                        ProjectInfo(config, dir.absolutePath)
                    } catch (_: Exception) {
                        null
                    }
                } else null
            }
            ?.sortedByDescending { it.config.createdAt }
            ?: emptyList()
    }

    /** Create a new project folder with default structure. */
    suspend fun createProject(name: String): ProjectInfo = withContext(Dispatchers.IO) {
        val projectDir = File(baseDir, name)
        require(!projectDir.exists()) { "Project '$name' already exists" }
        projectDir.mkdirs()

        // Create default subfolders
        defaultFolders.forEach { File(projectDir, it).mkdirs() }

        // Starter story file
        File(projectDir, "Story/main.md").writeText("# Kapitel 1\n\nDein Abenteuer beginnt...\n")

        // Write .project config
        val config = ProjectConfig(name = name)
        File(projectDir, ".project").writeText(json.encodeToString(config))

        ProjectInfo(config, projectDir.absolutePath)
    }

    /** Delete a project and all its contents. */
    suspend fun deleteProject(projectInfo: ProjectInfo) = withContext(Dispatchers.IO) {
        File(projectInfo.path).deleteRecursively()
    }
}
