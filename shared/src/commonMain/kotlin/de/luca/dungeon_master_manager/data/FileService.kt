package de.luca.dungeon_master_manager.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

data class ExternalDropValidation(
    val validFiles: List<File>,
    val skippedDirectories: Int,
    val skippedDotFiles: Int,
    val skippedSameDirectory: Int
)

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

    /**
     * Validates and partitions raw files dropped from the OS.
     */
    fun validateExternalDrop(rawFiles: List<File>, targetDir: File): ExternalDropValidation {
        var skippedDirs = 0
        var skippedDots = 0
        var skippedSameDir = 0
        val validFiles = mutableListOf<File>()

        val canonicalTarget = try { targetDir.canonicalFile } catch (_: Exception) { targetDir.absoluteFile }

        for (file in rawFiles) {
            if (file.isDirectory) {
                skippedDirs++
            } else if (file.name.startsWith(".")) {
                skippedDots++
            } else {
                val isSame = try {
                    file.canonicalFile.parentFile == canonicalTarget
                } catch (_: Exception) {
                    false
                }
                if (isSame) {
                    skippedSameDir++
                } else if (file.isFile) {
                    validFiles.add(file)
                }
            }
        }

        return ExternalDropValidation(
            validFiles = validFiles,
            skippedDirectories = skippedDirs,
            skippedDotFiles = skippedDots,
            skippedSameDirectory = skippedSameDir
        )
    }

    /**
     * Generates a unique target file name in the format "name (1).ext", "name (2).ext", etc.
     */
    fun generateUniqueTargetFile(targetDir: File, originalName: String): File {
        val hasExt = originalName.contains('.')
        val ext = if (hasExt) originalName.substringAfterLast('.') else ""
        val base = if (hasExt) originalName.substringBeforeLast('.') else originalName

        var counter = 1
        while (true) {
            val candidateName = if (hasExt && ext.isNotEmpty()) "$base ($counter).$ext" else "$base ($counter)"
            val candidate = File(targetDir, candidateName)
            if (!candidate.exists()) {
                return candidate
            }
            counter++
        }
    }

    /**
     * Copies a file to the target location.
     */
    suspend fun copyFile(source: File, target: File, overwrite: Boolean) = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        if (overwrite) {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } else {
            Files.copy(source.toPath(), target.toPath())
        }
    }

    /**
     * Moves a file on disk to the target location.
     */
    suspend fun moveFileOnDisk(source: File, target: File, overwrite: Boolean) = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        if (overwrite) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } else {
            Files.move(source.toPath(), target.toPath())
        }
    }
}
