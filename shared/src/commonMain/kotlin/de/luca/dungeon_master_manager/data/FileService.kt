package de.luca.dungeon_master_manager.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Service for reading the project file tree and file contents.
 */
class FileService {

    /**
     * Build a recursive [FileNode] tree for the given project directory.
     * Excludes dotfiles/dotfolders
     */
    suspend fun buildFileTree(projectPath: String): FileNode = withContext(Dispatchers.IO) {
        val root = File(projectPath)
        buildNode(root)
    }

    private fun buildNode(file: File): FileNode {
        val children = if (file.isDirectory) {
            file.listFiles()
                ?.filter { !it.name.startsWith(".") }
                ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
                ?.map { buildNode(it) }
                ?: emptyList()
        } else if (file.extension == "md" && file.parentFile?.name == "Story") {
            // Extract headings for outline
            try {
                file.readLines()
                    .filter { it.startsWith("# ") }
                    .mapIndexed { index, heading ->
                        FileNode(
                            name = heading.removePrefix("# ").trim(),
                            path = "${file.absolutePath}::heading::$index",
                            isDirectory = false,
                            isVirtualHeading = true,
                        )
                    }
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        return FileNode(
            name = file.name,
            path = file.absolutePath,
            isDirectory = file.isDirectory,
            children = children,
        )
    }

    /** Read the text content of a file. */
    suspend fun readFile(path: String): String = withContext(Dispatchers.IO) {
        File(path).readText()
    }

    /** Write text content to a file. */
    suspend fun writeFile(path: String, content: String) = withContext(Dispatchers.IO) {
        File(path).writeText(content)
    }
}
